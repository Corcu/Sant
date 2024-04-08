package calypsox.tk.bo.handler;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.DOMUtility;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.List;
import java.util.TimeZone;

public class KondorPlusIncomingMessageHandler implements MessageHandler {
    private static final String KONDOR_GROUP_LIST_ERRORRETURNED = "KONDOR_GROUP_LIST_ERRORRETURNED";
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
    private static final String TIME_OUT = "TIME_OUT";
    /*
     * Sample ACK message ================== <?xml version="1.0" encoding="UTF-8"?> <collateralTransferReturnStatus
     * xsi:noNamespaceSchemaLocation="KondorPlusReturnStatus.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
     * <messageId>0</messageId> <status>OK</status> <errorMesage>OK</errorMesage> </collateralTransferReturnStatus>
     *
     * Nack Message ============ <?xml version="1.0" encoding="UTF-8"?> <collateralTransferReturnStatus
     * xsi:noNamespaceSchemaLocation="KondorPlusReturnStatus.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
     * <messageId>0</messageId> <status>FAIL</status> <errorMesage>Failed to Process</errorMesage>
     * </collateralTransferReturnStatus>
     *
     * Time Out Message ============ <?xml version="1.0" encoding="UTF-8"?> <collateralTransferReturnStatus
     * xsi:noNamespaceSchemaLocation="KondorPlusReturnStatus.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
     * <messageId>0</messageId> <status>TIME_OUT</status> <errorMesage>Time out in the process</errorMesage>
     * </collateralTransferReturnStatus>
     */
    private int messageId;
    private String ackNackResponse;
    private String errorMessage;
    private int transactionId;
    private String contractId;
    private String dealId;
    private String lotSize;

    @Override
    public BOMessage parseMessage(String messageStr) throws Exception {
        String subject = "";

        if (Util.isEmpty(messageStr)) {
            Log.error("KondorPlusIncomingMessageHandler", "Cannot process message as it is either null or empty.");
        }

        parseXml(messageStr);
        BOMessage boMessage = new BOMessage();

        if (this.messageId != 0) {
            boMessage.setLongId(this.messageId);
            boMessage.setTradeLongId(this.transactionId);
        } else {
            throw new Exception("Message Id is not valid");
        }

        if (this.ackNackResponse.equals(CollateralStaticAttributes.OK)) {
            boMessage.setAction(Action.ACK);
        } else if (this.ackNackResponse.equals(CollateralStaticAttributes.FAIL)) {
            boMessage.setAction(Action.NACK);
            subject = "[Kondor+ Interface] ERROR: Fail returned";
        } else if (this.ackNackResponse.equals(CollateralStaticAttributes.TIME_OUT)) {
            boMessage.setAction(Action.valueOf(TIME_OUT));
            subject = "[Kondor+ Interface] ERROR: Time out returned";
        } else if (this.ackNackResponse.equals(CollateralStaticAttributes.ERROR_AC)) {
            boMessage.setAction(Action.NACK);
            subject = "[Kondor+ Interface] ERROR: Error inserting AccountClosing in Murex ";
        } else {
            throw new Exception("Unknown response code received");
        }

        if (this.errorMessage != null) {
            boMessage.setDescription(this.errorMessage);
            generateMessageToSendByEmail(boMessage, subject, KONDOR_GROUP_LIST_ERRORRETURNED, DSConnection.getDefault());
        } else {
            // If the response if FAIL then error message is mandatory
            if (this.ackNackResponse.equals(CollateralStaticAttributes.FAIL)) {
                throw new Exception("Error message must be specified when the status is FAIL");
            }
        }

        return boMessage;
    }

    private void parseXml(String xmlMessage) throws Exception {
        ByteArrayInputStream xmlStream = new ByteArrayInputStream(xmlMessage.getBytes());
        Document document = DOMUtility.createDOMDocument(xmlStream);

        Node node1 = document.getFirstChild();
        String prefix = node1.getPrefix();
        if (prefix != null) {
            prefix += ":";
        } else {
            prefix = "";
        }

        NodeList childNodes = node1.getChildNodes();

        for (int j = 0; j < childNodes.getLength(); j++) {
            Node child = childNodes.item(j);

            System.err.println(child.getNodeName() + " = " + child.getTextContent());
            if (child.getNodeName().equals(prefix + "transactionId")) {
                this.transactionId = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equals(prefix + "messageId")) {
                this.messageId = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equals(prefix + "status")) {
                this.ackNackResponse = child.getTextContent();
            } else if (child.getNodeName().equals(prefix + "errorDescription")) {
                this.errorMessage = child.getTextContent();
            } else if (child.getNodeName().equals(prefix + "contractId") && !child.getTextContent().isEmpty()) {
                this.contractId = child.getTextContent();
            } else if (child.getNodeName().equals(prefix + "dealId") && !child.getTextContent().isEmpty()) {
                this.dealId = child.getTextContent();
            } else if (child.getNodeName().equals(prefix + "lotSize") && !child.getTextContent().isEmpty()) {
                this.lotSize = child.getTextContent();
            }
        }
    }

    public static void main(String... args) throws Exception {
        KondorPlusIncomingMessageHandler handler = new KondorPlusIncomingMessageHandler();

        String fileName = "";
        File f = new File(fileName);

        FileReader fis = new FileReader(f);
        @SuppressWarnings("resource")
        BufferedReader reader = new BufferedReader(fis);
        String text = "";
        String line = null;
        while ((line = reader.readLine()) != null) {
            text += line;
        }

        handler.parseXml(text);

        System.out.println("*********** Parsed Fields **************");

        System.out.println("message id = " + handler.messageId);
        System.out.println("AckNack = " + handler.ackNackResponse);
        System.out.println("Error Message = " + handler.errorMessage);
    }

    /**
     * This method generates the message to send by email to the BOUsers, depending on the group list defined in the
     * DomainValues, and send it, using the EmailSender class.
     *
     * @param bomessage Message to get the description.
     * @param subject   Subject for the email.
     * @param groupList Distribution list with all the email address for the BOUsers.
     * @param conn      Connection with the Database.
     */
    public void generateMessageToSendByEmail(BOMessage bomessage, String subject, String groupList, DSConnection conn) {
        List<String> to = null;
        String textBody = "";
        double tradeAmount = 0.0;

        // We send the email only when the result is different of ACK.
        if (!bomessage.getAction().equals(Action.ACK)) {
            // We get all the email address to send the message, in the group defined.
            try {
                // We get the Trade using the BOMessage retrieved.
                Trade trade = conn.getRemoteTrade().getTrade(bomessage.getTradeLongId());
                if (null != trade) {
                    try {
                        MarginCall marginCall = (MarginCall) trade.getProduct();
                        if (null != marginCall.getSecurity()) {
                            tradeAmount = trade.getQuantity() * marginCall.getPrincipal();
                        } else {
                            tradeAmount = marginCall.getPrincipal();
                        }

                        CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                                marginCall.getMarginCallId());
                        textBody = bomessage.getDescription() + " for the message with ID: " + bomessage.getLongId()
                                + "\n\nAdditional Information:\n\nMargin Call Contract Name:\t" + mcc.getAuthName()
                                + "\nCounterparty:\t" + trade.getCounterParty().getAuthName() + "\nBook:\t"
                                + trade.getBook().getAuthName() + "\nTrade Date:\t" + trade.getTradeDate().getJDate(TimeZone.getDefault())
                                + "\nAmount:\t" + tradeAmount + "\nCurrency:\t" + trade.getTradeCurrency();
                    } catch (ClassCastException cce) {
                        Log.error(KondorPlusIncomingMessageHandler.class, cce);
                    }
                }

                // If the description for the message is NULL or Trade is NULL, we specify a new one by default.
                if ((null == trade) || (null == bomessage.getDescription()) || "".equals(bomessage.getDescription())) {
                    textBody = "Error returned from Kondor+ without an specification.";
                }

                to = conn.getRemoteReferenceData().getDomainValues(groupList);
                CollateralUtilities.sendEmail(to, subject, textBody, DEFAULT_FROM_EMAIL);
            } catch (RemoteException e) {
            }
        }
    }

    /**
     * Return the contractId value.
     *
     * @param messageStr
     * @return
     * @throws Exception
     */
    public String parseContractId(String messageStr) throws Exception {
        String contractId = null;

        if (Util.isEmpty(messageStr)) {
            Log.error("KondorPlusIncomingMessageHandler", "Cannot process message as it is either null or empty.");
        }
        if (this.contractId == null) {
            parseXml(messageStr);
        }
        if (this.contractId != null) {
            contractId = this.contractId;
        }
        return contractId;
    }

    /**
     * Return the dealId value.
     *
     * @param messageStr
     * @return
     * @throws Exception
     */
    public String parseDealId(String messageStr) throws Exception {
        String dealId = null;

        if (Util.isEmpty(messageStr)) {
            Log.error("KondorPlusIncomingMessageHandler", "Cannot process message as it is either null or empty.");
        }
        if (this.dealId == null) {
            parseXml(messageStr);
        }
        if (this.dealId != null) {
            dealId = this.dealId;
        }
        return dealId;
    }

    /**
     * Return the LotSize value.
     *
     * @param messageStr
     * @return
     * @throws Exception
     */
    public String parseLotSize(String messageStr) throws Exception {
        String LotSize = null;

        if (Util.isEmpty(messageStr)) {
            Log.error("KondorPlusIncomingMessageHandler", "Cannot process message as it is either null or empty.");
        }
        if (this.lotSize == null) {
            parseXml(messageStr);
        }
        if (this.lotSize != null) {
            LotSize = this.lotSize;
        }
        return LotSize;
    }

    /**
     * Return the Status value of the message.
     *
     * @param messageStr
     * @return
     * @throws Exception
     */
    public String parseStatus(String messageStr) throws Exception {
        String status = null;

        if (Util.isEmpty(messageStr)) {
            Log.error("KondorPlusIncomingMessageHandler", "Cannot process message as it is either null or empty.");
        }
        if (this.ackNackResponse == null) {
            parseXml(messageStr);
        }
        if (this.ackNackResponse != null) {
            status = this.ackNackResponse;
        }
        return status;
    }


}
