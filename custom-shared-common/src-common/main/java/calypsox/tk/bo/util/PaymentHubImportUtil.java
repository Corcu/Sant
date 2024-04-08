package calypsox.tk.bo.util;


import calypsox.tk.event.PSEventPaymentsHubImport;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TransferArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.*;


public class PaymentHubImportUtil {


    private static String KEY_SEPARATOR = "-";
    static private PaymentHubImportUtil instance = null;
    private static Map<String, String> actions = new HashMap<String, String>();
    private static Map<String, String> attributeValues = new HashMap<String, String>();
    public static final String DOMAIN_PH_CALLBACK_ACTION = "PHCallBackAction";
    public static final String DOMAIN_PH_CALLBACK_RESPONSES = "PHCallBackResponses";
    static {
        actions.put("RECEIVED-RECEIVED", Action.S_UPDATE);
        actions.put("PENDINGAUTHORIZATION-STPSTOP", Action.S_UPDATE);
        actions.put("PENDINGRELEASE-STPSTOP", Action.S_UPDATE);
        actions.put("P.SETTLEMENT-RELEASED", Action.S_UPDATE);
        actions.put("P.SETTLEMENT-ANALYSISAML", Action.S_UPDATE);
        actions.put("P.SETTLEMENT-ERRORAML", Action.S_UPDATE);
        actions.put("P.SETTLEMENT-ANALYSISEU847", Action.S_UPDATE);
        actions.put("P.SETTLEMENT-WAITINGACK", Action.S_UPDATE);
        actions.put("P.SETTLEMENT-ACK", Action.S_ACK);
        actions.put("SETTLED-MATCHED", Action.S_ACK);
        actions.put("SETTLED-SETTLED", Action.S_ACK);
        actions.put("CANCEL-MANUALREJECTION", Action.S_CANCEL);
        actions.put("CANCEL-NACK", Action.S_NACK);
        actions.put("CANCEL-MARKETREJECTION", Action.S_CANCEL);
        actions.put("CANCEL-BLOCKEDEU847", Action.S_NACK);
        actions.put("CANCEL-BLOCKEDAML", Action.S_NACK);
        attributeValues.put("RECEIVED-RECEIVED", "Received in PH with errors");
        attributeValues.put("PENDINGAUTHORIZATION-STPSTOP", "Received in PH pending first step 4 eyes check");
        attributeValues.put("PENDINGRELEASE-STPSTOP", "Received in PH pending second step 4 eyes check");
        attributeValues.put("P.SETTLEMENT-RELEASED", "Received in PH ISOCORE");
        attributeValues.put("P.SETTLEMENT-ANALYSISAML", "KO response screening tool");
        attributeValues.put("P.SETTLEMENT-ERRORAML", "Technical KO screening tool send");
        attributeValues.put("P.SETTLEMENT-ANALYSISEU847", "KO response EU847 tool");
        attributeValues.put("P.SETTLEMENT-WAITINGACK", "Waiting ACK/NACK");
        attributeValues.put("P.SETTLEMENT-ACK", "ACK");
        attributeValues.put("SETTLED-MATCHED", "ACK MT900");
        attributeValues.put("SETTLED-SETTLED", "ACK");
        attributeValues.put("CANCEL-NACK", "NACK in Swift");
        attributeValues.put("CANCEL-BLOCKEDEU847", "Rejected EU847 tool");
        attributeValues.put("CANCEL-BLOCKEDAML", "Rejected screening tool");
        attributeValues.put("CANCEL-MANUALREJECTION", "Manual cancel in PH");
        attributeValues.put("CANCEL-MARKETREJECTION", "Market rejection");
    }


    private PaymentHubImportUtil() {
    }


    public static PaymentHubImportUtil getInstance() {
        if (PaymentHubImportUtil.instance == null) {
            PaymentHubImportUtil.instance = new PaymentHubImportUtil();
        }
        return PaymentHubImportUtil.instance;
    }


    public static void setInstance(PaymentHubImportUtil mockInstance) {
        PaymentHubImportUtil.instance = mockInstance;
    }


    public PSEventPaymentsHubImport buildEvent(StringBuffer inputJson) {
        PSEventPaymentsHubImport event = new PSEventPaymentsHubImport(inputJson);
        return event;
    }


    public boolean saveBOMessage(StringBuffer inputJson, String engineName) {
        boolean savedOK = false;
        DSConnection ds = DSConnection.getDefault();
        try {
            final BOMessage boMessage = buildBOMessage(ds, inputJson);
            final long messageId = ds.getRemoteBO().save(boMessage, 0, null);
            Log.info(this, "Message saved with id: " + messageId);
            final AdviceDocument document = buildDocument(boMessage, messageId, inputJson);
            final long documentId = ds.getRemoteBO().save(document);
            Log.info(this, "Document for message " + messageId + " saved with id: " + documentId);
            // Apply action SAVE to BOMessage once the document is saved
            savedOK = applySaveAction(ds, messageId);
        } catch (final CalypsoServiceException e) {
            final String errorMessage = String.format("Could not save %s message", PHConstants.PAYMENTHUB_INPUT_MSG_TYPE);
            Log.error(this, errorMessage, e);
            savedOK = false;
        }
        return savedOK;
    }


    public StringBuffer inputStreamToStringBuffer(InputStream inputStream) {
        StringBuffer output = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append('\n');
                }
                output.append(line);
            }
        } catch (IOException e) {
            String errorMessage = "Could not read InputStream as String";
            Log.error(this, errorMessage, e);
        }
        return output;
    }


    private BOMessage buildBOMessage(DSConnection ds, StringBuffer jsonStringBuffer) {
        final BOMessage boMessage = new BOMessage();
        long transferId = 0;
        long tradeId = 0;
        BOTransfer transfer = getTransfer(ds, jsonStringBuffer);
        if (transfer != null) {
            transferId = transfer.getLongId();
            tradeId = transfer.getTradeLongId();
        }
        boMessage.setLongId(0);
        boMessage.setMessageClass(0);
        boMessage.setTransferLongId(transferId);
        boMessage.setTradeLongId(tradeId);
        boMessage.setMessageType(PHConstants.PAYMENTHUB_INPUT_MSG_TYPE);
        boMessage.setAddressMethod(PHConstants.ADDRESS_METHOD_NONE);
        boMessage.setLanguage(null);
        boMessage.setCreationDate(new JDatetime());
        boMessage.setStatus(Status.S_NONE);
        boMessage.setMatchingB(false);
        boMessage.setAction(Action.NEW);
        boMessage.setFormatType(null);
        boMessage.setGateway(PHConstants.GATEWAY_NONE);
        boMessage.setExternalB(true);
        boMessage.setSubAction(Action.NONE);
        boMessage.setFormatType(PHConstants.FORMAT_TYPE_JSON);
        boMessage.setTemplateName(PHConstants.TEMPLATE_NAME_PAYMENTHUB_INPUT);
        return boMessage;
    }


    /**
    private BOMessage getBOMessageFromJson(DSConnection ds, StringBuffer jsonStringBuffer) {
        BOMessage message = null;
        String inputJsonString = jsonStringBuffer.toString();
        PaymentsHubCallback callback = PaymentsHubCallback.parseText(inputJsonString);
        String idEmpotentReference = callback.getIdempotentReference();
        message = getBOMessageFromIdRef(idEmpotentReference);
        return message;
    }
    */


    public BOMessage getBOMessageFromIdRef(String idRef) {
        BOMessage boMessage = null;
        String from = "bo_transfer, xfer_attributes";
        StringBuilder where = new StringBuilder();
        where.append("xfer_attributes.attr_name = 'MessageTrn' AND ");
        where.append("xfer_attributes.attr_value = '" + idRef + "' AND ");
        where.append("xfer_attributes.transfer_id = bo_transfer.transfer_id AND ");
        where.append("bo_message.transfer_id = bo_transfer.transfer_id AND ");
        where.append("bo_message.message_type = 'PAYMENTHUB_PAYMENTMSG' AND ");
        where.append("bo_message.template_name IN ('PH-FICT', 'PH-FICCT') AND ");
        where.append("bo_message.message_status NOT IN ('CANCELED')");
        try {
            MessageArray messages = DSConnection.getDefault().getRemoteBackOffice().getMessages(from, where.toString(), null, null);
            if(null != messages && !Util.isEmpty(messages.getMessages())){
                boMessage = messages.get(0);
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading messages for TRN: " + idRef);
        }
        return boMessage;
    }


    public BOTransfer getBOTransferFromBOMessage(BOMessage message) {
        BOTransfer xfer = null;
        if (message != null) {
            long transferId = message.getTransferLongId();
            try {
                xfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(transferId);
            } catch (CalypsoServiceException e) {
                String errorMessage = String.format("Could not retrieve transfer with id = %d", transferId);
                Log.error(this, errorMessage, e);
            }
        }
        return xfer;
    }


    private long getMessageIdFromTRN(String trn) {
        long messageId = 0;
        String trnToParse = trn.toUpperCase().replaceAll("[A-Z]", "");
        try {
            messageId = Long.parseLong(trnToParse);
        } catch (NumberFormatException e) {
            String errorMessage = String.format("Could not parse \"%s\" as long", trnToParse);
            Log.error(this, errorMessage, e);
            messageId = -1;
        }
        return messageId;
    }


    /**
    public BOTransfer getTransfer(DSConnection ds, StringBuffer jsonStringBuffer) {
        BOTransfer transfer = null;
        String inputJsonString = jsonStringBuffer.toString();
        PaymentsHubCallback callback = PaymentsHubCallback.parseText(inputJsonString);
        String id = callback.getIdempotentReference();

        for (int i=0; i<id.length(); i++) {
            if(id.charAt(i) == '0'){
                id = id.substring(i);
                break;
            }
        }
        for (int i=0; i<id.length(); i++) {
            if(id.charAt(i) != '0'){
                id = id.substring(i);
                break;
            }
        }
        BOMessage msg = null;
        if(!Util.isEmpty(id)) {
            try {
                msg = ds.getRemoteBackOffice().getMessage(Long.parseLong(id));
            } catch (CalypsoServiceException e) {
                Log.error(this, "Could not load message with id: " + id, e);
            }
        }

        try {
            if(msg != null) {
                Long transferId = msg.getTransferLongId();
                if (transferId > 0) {
                    transfer = ds.getRemoteBO().getBOTransfer(transferId);
                }
            }
        } catch (NumberFormatException e) {
            String errorMessage = String.format("Could not parse \"%s\" as Long", id);
            Log.error(this, errorMessage, e);
        } catch (CalypsoServiceException e) {
            String errorMessage = String.format("Could not retrieve transfer with id %s", id);
            Log.error(this, errorMessage, e);
        }
        return transfer;
    }
    */


    public BOTransfer getTransfer(DSConnection ds, StringBuffer jsonStringBuffer) {
        BOTransfer transfer = null;
        String inputJsonString = jsonStringBuffer.toString();
        PaymentsHubCallback callback = PaymentsHubCallback.parseText(inputJsonString);
        String trn = callback.getIdempotentReference();
        transfer = getTransferByAttr(trn);
        return transfer;
    }


    private AdviceDocument buildDocument(BOMessage boMessage, long messageId, StringBuffer jsonStringBuffer) {
        final AdviceDocument document = new AdviceDocument(boMessage, new JDatetime());
        document.setAdviceId(messageId);
        document.setDocument(jsonStringBuffer);
        return document;
    }


    private boolean applySaveAction(DSConnection ds, long messageId) {
        boolean messageSavedOK = false;
        try {
            final BOMessage message = ds.getRemoteBO().getMessage(messageId);
            final BOMessage newMessage = (BOMessage) message.clone();
            newMessage.setAction(PHConstants.MESSAGE_ACTION_SAVE);
            final long newMessageId = ds.getRemoteBO().save(newMessage, 0, null);
            if (newMessageId == messageId) {
                messageSavedOK = true;
                final String infoMessage = String.format("[Message %d] Action SAVE applied correctly", messageId);
                Log.info(this, infoMessage);
            } else {
                final String errorMessage = String.format("[Message %d] Could not apply action SAVE", messageId);
                Log.error(this, errorMessage);
            }
        } catch (final RemoteException e) {
            final String errorMessage = String.format("[Message %d] Could not apply action SAVE", messageId);
            Log.error(this, errorMessage, e);
        } catch (final CloneNotSupportedException e) {
            final String errorMessage = String.format("[Message %d] Could not clone message", messageId);
            Log.error(this, errorMessage, e);
        }
        return messageSavedOK;
    }


    /**
     * Get Action
     *
     * @param statusReceived
     * @param commStatusReceived
     * @return
     */
    public Action getAction(final String statusReceived, final String commStatusReceived) {
        String actionStr = "";
        String key = statusReceived.concat(KEY_SEPARATOR).concat(commStatusReceived);
        key = convertKey(key);
        // From DomainValues
        final Map<String, String> callBackActions = DomainValues.valuesComment(DOMAIN_PH_CALLBACK_ACTION);
        convertMapKeys(callBackActions);
        if (callBackActions != null && !callBackActions.isEmpty() && callBackActions.get(key) != null) {
            actionStr = callBackActions.get(key);
        } else {
            actionStr = actions.get(key);
        }
        return (!Util.isEmpty(actionStr)) ? Action.valueOf(actionStr) : null;
    }


    /**
     * Get 'PHSettlementStatus' value for Xfer attribute
     *
     * @param statusReceived
     * @param commStatusReceived
     * @return
     */
    public String getPHSettlementStatusAttributeValue(final String statusReceived, final String commStatusReceived) {
        String attributeValue = "";
        String key = statusReceived.concat(KEY_SEPARATOR).concat(commStatusReceived);
        key = convertKey(key);
        // From DomainValues
        final Map<String, String> literal = DomainValues.valuesComment(DOMAIN_PH_CALLBACK_RESPONSES);
        convertMapKeys(literal);
        if (literal != null && literal.get(key) != null) {
            attributeValue = literal.get(key);
        } else {
            attributeValue = attributeValues.get(key);
        }
        return (!Util.isEmpty(attributeValue)) ? attributeValue : null;
    }


    private void convertMapKeys(Map<String, String> map) {
        if (map != null) {
            List<String> mapKeys = new ArrayList<String>(map.keySet());
            for (String mapKey : mapKeys) {
                String value = map.get(mapKey);
                map.remove(mapKey);
                String convertedKey = convertKey(mapKey);
                map.put(convertedKey, value);
            }
        }
    }


    private String convertKey(String key) {
        String convertedKey = null;
        if (key != null) {
            convertedKey = key;
            convertedKey = convertedKey.replaceAll(" ", "");
            convertedKey = convertedKey.toUpperCase();
        }

        return convertedKey;
    }


    public boolean consumeEvent(final PSEvent event, String engine) {
        try {
            DSConnection.getDefault().getRemoteTrade().eventProcessed(event.getLongId(), engine);
        } catch (final Exception e) {
            Log.error(this, e);
            return false;
        }
        return true;
    }


    public static BOTransfer getTransferByAttr(final String trn) {
        String fromClause = "xfer_attributes";
        String whereClause = "xfer_attributes.transfer_id=bo_transfer.transfer_id AND xfer_attributes.attr_name='" + PHConstants.XFER_ATTR_PH_MESSAGE_TRN + "' AND xfer_attributes.attr_value='" + trn + "'";
        TransferArray transfers = null;
        try {
            transfers = DSConnection.getDefault().getRemoteBO().getTransfers(fromClause, whereClause, null);
        } catch (CalypsoServiceException e) {
            e.printStackTrace();
        }
        if (!Util.isEmpty(transfers)) {
            return transfers.get(0);
        }
        return null;
    }


}
