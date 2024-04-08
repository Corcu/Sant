/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.gestorstp;

import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.bo.mapping.CRESTParser;
import calypsox.tk.bo.swift.CRESTMessage;
import calypsox.util.SantDomainValuesUtil;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.ExternalMessageHandler;
import com.calypso.tk.bo.ExternalMessageParser;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class implements CRESTGestorSTPIncomingMessageEngine for processing SWIFT message from CREST,
 * MT536, MT537, MT544, MT545, MT546, MT547, MT548, MT564, MT566, MT578, MT940, MT941
 *
 * @author Carlos Corcuera
 * @version 1.0
 * @date 09/02/2023
 */

public class CRESTGestorSTPIncomingMessageEngine extends GestorSTPIncomingMessageEngine {

    public static String ENGINE_NAME_CREST = "SANT_CREST_GestorSTPIncomingMessageEngine";

    private static ExternalMessageParser parser;

    private static final String SWIFT_PARSER_TYPE = "SWIFT_PARSER_TYPE";

    private static final String ROLLBACK_DV = "EnableGestorSTPRollback";

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public CRESTGestorSTPIncomingMessageEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
        final String SwifthParser = getEngineParam(SWIFT_PARSER_TYPE, SWIFT_PARSER_TYPE, "SWIFT");
        parser = SwiftParserUtil.getParser(SwifthParser);
    }

    /**
     * Name of the engine that offers this service
     */
    @Override
    public String getEngineName() {
        return ENGINE_NAME_CREST;
    }

    /**
     * Same method that the parent but without response part
     *
     * @param externalMessage received and passed by the newMessage Method
     * @return if the incoming message has been handle
     */
    @Override
    public boolean handleIncomingMessage(final ExternalMessage externalMessage) {

        Log.info(GestorSTPIncomingMessageEngine.class, "Received message:" + externalMessage.getText() + "\n");

        Pattern pattern = Pattern.compile("<Msg.+?</Msg>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(externalMessage.getText());

        while (matcher.find()) {

            String textCrestMessage = matcher.group();
            JMSQueueMessage crestExternalMessage = new JMSQueueMessage();
            crestExternalMessage.setReference(null);
            crestExternalMessage.setCorrelationId(null);
            crestExternalMessage.setText(textCrestMessage);

            final ExternalMessage message = cleanSWIFTMessage(crestExternalMessage);

            if (message == null || Util.isEmpty(externalMessage.getText())) {
                Log.info(GestorSTPIncomingMessageEngine.class, "Message receive is null or empty");
                return false;
            }

            boolean messageProcessedOk = false;
            ExternalMessageHandler handler = SwiftParserUtil.getHandler("SWIFT", message.getType());
            /*
             * core logic, for MT910 & MT900 messages, check MT9X0MessageProcessor
             * classes in package calypsox.tk.util.swiftparser
             */

            Vector<SwiftFieldMessage> messageFields = ((CRESTMessage) message).getFields();

            try {
                if (messageFields != null) {
                    setSplitRelatedMessage(messageFields, message);
                }
            } catch (Exception e) {
                Log.error(this, e + ": " + e.getMessage(), e);
            }

            try {
                if (handler != null) {
                    messageProcessedOk = handler.handleExternalMessage(message, null, null, null, super.getDS(), null);
                } else {
                    messageProcessedOk = SwiftParserUtil.processExternalMessage(message, null, null, null, super.getDS(),
                            null);
                }
            } catch (MessageParseException e) {
                Log.info(GestorSTPIncomingMessageEngine.class, "Parsing error message = " + externalMessage.getText());
                rollbackMsg();
                return false;
            } catch (Exception exc) {
                rollbackMsg();
                return false;
            }

            Log.info(GestorSTPIncomingMessageEngine.class, messageProcessedOk ? "OK" : "ERROR");
        }

        return true;
    }

    /**
     * @param externalMessage
     * @return message with SWIFT end of line format
     */
    private ExternalMessage cleanSWIFTMessage(final ExternalMessage externalMessage) {

        String incomingMessageText = externalMessage.getText();
        incomingMessageText = GestorSTPUtil.fixSwiftEndOfLineCharacters(incomingMessageText);

        try {
            ExternalMessageParser parser = new CRESTParser();
            return parser.readExternal(incomingMessageText, "CREST");

        } catch (MessageParseException e) {
            Log.error(this, e + ": " + e.getMessage(), e);
            return null;
        }
    }

    public void setSplitRelatedMessage(Vector<SwiftFieldMessage> messageFields, ExternalMessage message) {
        String tagRela = ":RELA//CYO";
        String tagRelaSplit = ":RELA//NONREF";
        String tagPrevSplit = ":PREV//";
        String tagSemeSplit = ":SEME//";
        long relatedMessageId = -1;
        int numeroCampo = -1;
        int splitNum = -1;
        for (int i = 0; i < messageFields.size(); i++) {
            String campo = messageFields.get(i).getValue();
            if (campo != null && campo.equals(tagRelaSplit)) {
                splitNum = i;
            }
            if (campo != null && campo.startsWith(tagRela)) {
                numeroCampo = i;
                String relatedMessageField = messageFields.get(numeroCampo).getValue();
                String relatedMessageValue = relatedMessageField.split("CYO")[1];
                relatedMessageId = getNumeric(relatedMessageValue);
                messageFields.get(numeroCampo).setValue(":RELA//CYO" + relatedMessageId);
                i = messageFields.size();
            } else if (campo != null && campo.startsWith(tagPrevSplit)) {
                numeroCampo = i;
                String relatedMessageField = messageFields.get(numeroCampo).getValue();

                if (relatedMessageField.startsWith(":PREV//CYO")) {
                    String relatedMessageValue = relatedMessageField.split("CYO")[1];
                    relatedMessageId = getNumeric(relatedMessageValue);

                } else {
                    String relatedMessageValue = relatedMessageField.split("PREV//")[1];
                    try {
                        String query = "SELECT LINKED_ID FROM MESS_ATTRIBUTES MA, BO_MESSAGE BM WHERE MA.MESSAGE_ID = BM.MESSAGE_ID AND BM.TEMPLATE_NAME = 'MT548' AND MA.ATTR_VALUE = '" + relatedMessageValue + "'";
                        Vector<Vector<String>> messageSplitRela = (Vector<Vector<String>>) getDS().getRemoteAccess().executeSelectSQL(query, null);
                        relatedMessageId = (Long) (messageSplitRela.get(messageSplitRela.size() - 1).toArray()[0]);

                    } catch (CalypsoServiceException e) {
                        e.printStackTrace();
                    }
                }

                for (int j = 0; j < messageFields.size(); j++) {
                    String campoSplit = messageFields.get(j).getValue();
                    if (splitNum > 0) {
                        messageFields.get(splitNum).setValue(":RELA//CYO" + relatedMessageId);
                        j = messageFields.size();
                    } else if (campoSplit != null && campoSplit.startsWith(tagRelaSplit)) {
                        numeroCampo = j;
                        messageFields.get(numeroCampo).setValue(":RELA//CYO" + relatedMessageId);
                        j = messageFields.size();
                        i = messageFields.size();
                    }

                }
            } else if (campo != null && campo.startsWith(tagRelaSplit)) {
                numeroCampo = i;
                for (int l = 0; l < messageFields.size(); l++) {
                    String relatedMessageField = messageFields.get(l).getValue();

                    if (relatedMessageField.startsWith(tagSemeSplit)) {

                        String relatedMessageValue = relatedMessageField.split("SEME//")[1];

                        try {
                            for (int k = 2; k < relatedMessageValue.length(); k++) {
                                relatedMessageValue = relatedMessageValue.substring(0, relatedMessageValue.length() - k);
                                String query = "SELECT LINKED_ID FROM MESS_ATTRIBUTES MA, BO_MESSAGE BM WHERE MA.MESSAGE_ID = BM.MESSAGE_ID AND BM.TEMPLATE_NAME = 'MT548' AND MA.ATTR_VALUE = '" + relatedMessageValue + "'";
                                Vector<Vector<String>> messageSplitRela = (Vector<Vector<String>>) getDS().getRemoteAccess().executeSelectSQL(query, null);
                                relatedMessageId = (Long) (messageSplitRela.get(messageSplitRela.size() - 1).toArray()[0]);
                                if (relatedMessageId > 0) {
                                    k = relatedMessageValue.length();
                                    messageFields.get(numeroCampo).setValue(":RELA//CYO" + relatedMessageId);
                                    l = messageFields.size();
                                    i = messageFields.size();
                                }

                            }
                        } catch (CalypsoServiceException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    private void rollbackMsg() {
        if (SantDomainValuesUtil.getBooleanDV(ROLLBACK_DV)) {
            try {
                this.getIEAdapter().rollback();
            } catch (ConnectException ex) {
                Log.error(this.getEngineName(), "Error during queue session rollback() ", ex);
            }
        }
    }

    protected static long getNumeric(String ref) {
        if (!ref.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < ref.length(); i++) {
                if (Character.isDigit(ref.charAt(i))) {
                    sb.append(ref.charAt(i));
                }
            }

            if (sb.length() > 0) {
                try {
                    return Long.parseLong(sb.toString());
                } catch (Exception var4) {

                }
            }
        }

        return 0L;
    }

}
