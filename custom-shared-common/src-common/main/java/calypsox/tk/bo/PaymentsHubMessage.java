package calypsox.tk.bo;


import java.util.ArrayList;
import java.util.List;
import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.restservices.paymentshub.model.PaymentsHubInput;
import com.santander.restservices.paymentshub.model.submodel.CreditTransferInstruction;
import com.santander.restservices.paymentshub.model.submodel.GroupHeader;
import com.santander.restservices.paymentshub.model.submodel.SupplementaryData;


public class PaymentsHubMessage {

    private final static String LOG_CATEGORY = PaymentsHubMessage.class.getSimpleName();

    private PAYMENTHUBMSGMessageFormatter formatter;
    private PaymentsHubInput paymentRequest;
    private BOMessage boMessage;
    private Trade trade;
    private BOTransfer boTransfer;
    private LEContact sender;
    private LEContact receiver;
    private PricingEnv pricingEnv;
    private String paymentsHubText;
    private String templateName;
    private String gateway;
    private final ObjectMapper mapper;


    public PaymentsHubMessage(final BOMessage boMessage) {
        this.boMessage = boMessage;
        mapper = new ObjectMapper();
        configureMapper();
        init(boMessage);
    }


    private void configureMapper() {
        // Don't show nulls
        getMapper().setSerializationInclusion(Include.NON_NULL);
    }


    private void init(final BOMessage boMessage) {
        // Set Trade
        setTrade(PaymentsHubUtil.getTrade(getBOMessage().getTradeLongId()));
        // Set Transfer
        setBOTransfer(PaymentsHubUtil.getBOTransfer(boMessage.getTransferLongId()));
        // Set Sender
        setSender(PaymentsHubUtil.getContactById(boMessage.getSenderContactId(), DSConnection.getDefault()));
        // Set Receiver
        setReceiver(PaymentsHubUtil.getContactById(boMessage.getReceiverContactId(), DSConnection.getDefault()));
        // Set Template
        setTemplateName(boMessage.getTemplateName());
        // Set Formatter
        setFormatter(findMessageFormatter());
        // Set Gateway
        setGateway(boMessage.getGateway());
        // PaymentsHubText
        setPaymentsHubText(null);
        //Origin PH for SWIFT generate custom SDI or not
        PaymentsHubUtil.setPHOrigin(boMessage);
    }


    /**
     * Build the PaymentsHub Message
     */
    public void build(final boolean newDocument) {
        String textMessage = "";
        AdviceDocument adviceDocument = null;
        if (!newDocument) {
            // Check LatestAdviceDocument
            adviceDocument = PaymentsHubUtil.getLatestAdviceDocument(getBOMessage().getLongId());
        }
        if (adviceDocument != null) {
            final StringBuffer buff = PaymentsHubUtil.getAdviceDocumentAsStringBuffer(adviceDocument);
            textMessage = (buff != null) ? buff.toString() : "";
            // Parse advice document to PaymentsHubMessage
            if (!parsePaymentsHubText(textMessage)) {
                final String msg = String.format("Could not parse PaymentsHubInput data from BOMessage [%d] ", getBOMessage().getLongId());
                Log.error(LOG_CATEGORY, msg);
            }
        } else {
            // Build PaymentsHubInput
            final PaymentsHubInput input = buildPaymentRequest();
            setPaymentRequest(input);
            // Build PaymentsHub Text
            textMessage = getPaymentsHubTextMessage(input);
            setPaymentsHubText(textMessage);
        }
    }


    /**
     * Find MessageFormatter.
     */
    private PAYMENTHUBMSGMessageFormatter findMessageFormatter() {
        final MessageFormatter formatter = PAYMENTHUBMSGMFSelector.findMessageFormatter(getBOMessage());
        if (formatter != null && formatter instanceof PAYMENTHUBMSGMessageFormatter) {
            return (PAYMENTHUBMSGMessageFormatter) formatter;
        }
        return null;
    }


    /**
     * Build the getPaymentsHubTextMessage - Object to Json.
     *
     * @param input
     * @return
     */
    private String getPaymentsHubTextMessage(final PaymentsHubInput input) {
        return getPaymentsHubTextMessage(input, false);
    }


    /**
     * Build the getPaymentsHubTextMessage - Object to Json.
     *
     * @param input
     * @param isPrettyFormat
     * @return
     */
    private String getPaymentsHubTextMessage(final PaymentsHubInput input, final boolean isPrettyFormat) {
        String debug = "";
        String paymentsHubInputStr = "";
        if (input != null) {
            try {
                // Converting the Java object into a JSON string
                if (!isPrettyFormat) {
                    paymentsHubInputStr = getMapper().writeValueAsString(input);
                } else {
                    paymentsHubInputStr = getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(input);
                }
            } catch (final JsonProcessingException e) {
                debug = "Error converting the Object to JSON string.";
                Log.error(LOG_CATEGORY, debug, e);
            }
        } else {
            debug = "Error converting the Object to JSON string. The input is null.";
            Log.debug(LOG_CATEGORY, debug);
        }
        return paymentsHubInputStr;
    }


    /**
     * Get PaymentRequest with DefaultPrettyPrinter.
     *
     * @return
     */
    public String getTexMessageWithDefaultPrettyPrinter() {
        String debug = "";
        String textMessagePretty = "";
        if (getPaymentRequest() != null) {
            textMessagePretty = getPaymentsHubTextMessage(getPaymentRequest(), true);
        } else {
            debug = "Error getting the PaymentRequest. It is not possible show the TextMessage with DefaultPrettyPrinter.";
            Log.error(LOG_CATEGORY, debug);
        }
        return textMessagePretty;
    }


    /**
     * Generate PaymentRequest.
     *
     * Build PaymentsHubInput object.
     *
     * @return
     */
    private PaymentsHubInput buildPaymentRequest() {
        final DSConnection dsCon = DSConnection.getDefault();
        final PaymentsHubInput paymentRequest = new PaymentsHubInput();
        if (getFormatter() != null) {
            // Build paymentRequest
            final List<CreditTransferInstruction> list = new ArrayList<>();
            list.add(getCreditTransferTransactionInformation(dsCon));
            paymentRequest.setGrpHdr(getGroupHeader(dsCon));
            paymentRequest.setCdtTrfTxInf(list);
            paymentRequest.setSplmtryData(getSupplementaryData(dsCon));
        }
        return paymentRequest;
    }


    /**
     * Parse JSON Text to PaymentsHubInput.
     *
     * @param text
     * @return
     */
    public boolean parsePaymentsHubText(final String text) {
        try {
            final PaymentsHubInput paymentRequest = getMapper().readValue(text, PaymentsHubInput.class);
            // Set text
            setPaymentsHubText(text);
            // Set paymentRequest
            setPaymentRequest(paymentRequest);
        } catch (final JsonProcessingException e) {
            final String msg = String.format("Error parsing PaymentsHubInput data.");
            Log.error(PaymentsHubMessage.class, msg, e);
            return false;
        }
        return true;
    }


    // ----------------------------------------------------------------- //
    // -------------------------- GROUP HEADER ------------------------- //
    // ----------------------------------------------------------------- //


    /**
     * Set of characteristics shared by all individual transactions included in the message.
     *
     * @return
     */
    protected GroupHeader getGroupHeader(final DSConnection dsCon) {
        // Build GroupHeader
        final GroupHeader gh = getFormatter().getGroupHeader(getBOMessage(), getBOTransfer(), getTrade(), dsCon);
        return gh;
    }


    // ----------------------------------------------------------------- //
    // ------------------ CREDIT TRANSFER INSTRUCTION ------------------ //
    // ----------------------------------------------------------------- //


    /**
     * Set of elements providing information specific to the individual credit transfer(s).
     *
     * @return
     */
    protected CreditTransferInstruction getCreditTransferTransactionInformation(final DSConnection dsCon) {
        // Build CreditTransferInstruction
        final CreditTransferInstruction creditTransferInstruction = getFormatter().getCreditTransferInstruction(getBOMessage(), getBOTransfer(), getTrade(), dsCon);
        return creditTransferInstruction;
    }


    // ----------------------------------------------------------------- //
    // ---------------------- SUPPLEMENTARY DATA ----------------------- //
    // ----------------------------------------------------------------- //


    /**
     * Additional information that cannot be captured in the structured elements and/or any other
     * specific block.
     *
     * @return
     */
    protected SupplementaryData getSupplementaryData(final DSConnection dsCon) {
        // Build CreditTransferInstruction
        final SupplementaryData supplementaryData = getFormatter().getSupplementaryData(getBOMessage(), getBOTransfer(),
                getTrade(), dsCon);
        return supplementaryData;
    }


    // ----------------------------------------------------------------- //
    // --------------------- SETTERS and GETTERS ----------------------- //
    // ----------------------------------------------------------------- //


    public BOMessage getBOMessage() {
        return boMessage;
    }


    public void setBOMessage(BOMessage boMessage) {
        this.boMessage = boMessage;
        init(boMessage);
    }


    public Trade getTrade() {
        return trade;
    }


    private void setTrade(Trade trade) {
        this.trade = trade;
    }


    public BOTransfer getBOTransfer() {
        return boTransfer;
    }


    private void setBOTransfer(BOTransfer boTransfer) {
        this.boTransfer = boTransfer;
    }


    public LEContact getSender() {
        return sender;
    }


    private void setSender(LEContact sender) {
        this.sender = sender;
    }


    public LEContact getReceiver() {
        return receiver;
    }


    private void setReceiver(LEContact receiver) {
        this.receiver = receiver;
    }


    public PricingEnv getPricingEnv() {
        return pricingEnv;
    }


    public void setPricingEnv(PricingEnv pricingEnv) {
        this.pricingEnv = pricingEnv;
    }


    public void setPaymentsHubText(String paymentsHubText) {
        this.paymentsHubText = paymentsHubText;
    }


    public String getPaymentsHubText() {
        return paymentsHubText;
    }


    private ObjectMapper getMapper() {
        return mapper;
    }


    public PaymentsHubInput getPaymentRequest() {
        return paymentRequest;
    }


    public void setPaymentRequest(PaymentsHubInput paymentRequest) {
        this.paymentRequest = paymentRequest;
    }


    public String getTemplateName() {
        return templateName;
    }


    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }


    public String getGateway() {
        return gateway;
    }


    public void setGateway(String gateway) {
        this.gateway = gateway;
    }


    public PAYMENTHUBMSGMessageFormatter getFormatter() {
        if (formatter == null) {
            formatter = findMessageFormatter();
        }
        return formatter;
    }
    public void setFormatter(PAYMENTHUBMSGMessageFormatter formatter) {
        this.formatter = formatter;
    }


}
