package calypsox.tk.util;

import calypsox.tk.core.KeywordConstantsUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.util.ProcessStatusException;

/**
 * The Class PayplusMessageHandler.
 */
public class PayplusMessageHandler extends SwiftMessageHandler {

    private static final String EMIR_JURISDICTION = "ESMA";

    private static final String DFA_JURISDICTION = "CFTC";

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.SwiftMessageHandler#getParsedMessage(java.lang.String)
     */
    @Override
    public BOMessage getParsedMessage(final String serializedMessage)
            throws ProcessStatusException {
        final SwiftMessage externalMessage = new SwiftMessage();
        externalMessage.parseSwiftText(serializedMessage, false);
        final SwiftFieldMessage tag21 = externalMessage
                .getSwiftField(externalMessage.getFields(), ":21:", null, null);
        final String messageId = tag21.getValue().substring(3);

        final BOMessage result = new BOMessage();

        // CAL_751_
        try {
            final int idValue = Integer.parseInt(messageId);
            final Action action = getAction(externalMessage);

            // USI/UTI Keywords
            // CAL_EMIR_020 y CAL_DODD_098
            final SwiftFieldMessage jurisdiction = externalMessage
                    .getSwiftField(externalMessage.getFields(), ":22L:", null,
                            null);
            final SwiftFieldMessage utiusiPrefix = externalMessage
                    .getSwiftField(externalMessage.getFields(), ":22M:", null,
                            null);
            final SwiftFieldMessage utiusiValue = externalMessage.getSwiftField(
                    externalMessage.getFields(), ":22N:", null, null);
            final SwiftFieldMessage priorUtiusiPrefix = externalMessage
                    .getSwiftField(externalMessage.getFields(), ":22P:", null,
                            null);
            final SwiftFieldMessage priorUtiusiValue = externalMessage
                    .getSwiftField(externalMessage.getFields(), ":22R:", null,
                            null);

            result.setLongId(idValue);
            result.setAction(action);
            result.setDescription(getDescription(externalMessage));

            // UTI Fields
            // CAL_EMIR_020
            if (jurisdiction != null) {
                if (EMIR_JURISDICTION
                        .equalsIgnoreCase(jurisdiction.getValue())) {
                    if ((utiusiValue != null) && (utiusiPrefix != null)) {
                        result.setAttribute(
                                KeywordConstantsUtil.KEYWORD_UTI_TRADE_ID,
                                utiusiPrefix.getValue()
                                        + utiusiValue.getValue());
                    }
                    if ((priorUtiusiValue != null)
                            && (priorUtiusiPrefix != null)) {
                        result.setAttribute(
                                KeywordConstantsUtil.KEYWORD_PRIOR_UTI_TRADE_ID,
                                priorUtiusiPrefix.getValue()
                                        + priorUtiusiValue.getValue());
                    }
                }
                // USI Fields
                // CAL_DODD_098
                if (DFA_JURISDICTION
                        .equalsIgnoreCase(jurisdiction.getValue())) {
                    if ((utiusiValue != null) && (utiusiPrefix != null)) {
                        result.setAttribute(
                                KeywordConstantsUtil.KEYWORD_USI_TRADE_ID,
                                utiusiPrefix.getValue()
                                        + utiusiValue.getValue());
                    }
                    if ((priorUtiusiValue != null)
                            && (priorUtiusiPrefix != null)) {
                        result.setAttribute(
                                KeywordConstantsUtil.KEYWORD_PRIOR_USI_TRADE_ID,
                                priorUtiusiPrefix.getValue()
                                        + priorUtiusiValue.getValue());
                    }
                }
            }
        } catch (final NumberFormatException ex) {
            Log.error(this, "Couldn't parse messageId for tag 21: "
                    + externalMessage.getText());
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.SwiftMessageHandler#getAction(com.calypso.tk.bo.swift
     * .SwiftMessage)
     */
    @Override
    protected Action getAction(final SwiftMessage externalMessage)
            throws ProcessStatusException {
        final SwiftFieldMessage swiftAction = externalMessage
                .getSwiftField(externalMessage.getFields(), ":76:", null, null);

        final String swiftActionValue = swiftAction.getValue();
        Action result = null;

        // CAL_661_
        if (swiftAction.getValue().startsWith("/WTAK/")) {
            result = Action.valueOf("PAYPLUS_WAIT");
        } else if (swiftActionValue.startsWith("/FVAL/")
                || swiftActionValue.startsWith("/RCNF/")
                || swiftActionValue.startsWith("/INVA/")
                || swiftActionValue.startsWith("/RJCT/")) {
            result = Action.NACK;
        } else if (swiftActionValue.startsWith("/IMAT/")
                || swiftActionValue.startsWith("/FMTC/")) {
            result = Action.MATCH;
        } else if (swiftActionValue.startsWith("/IURT/")
                || swiftActionValue.startsWith("/UMTC/")) {
            result = Action.UNMATCH;
        } else if (swiftActionValue.startsWith("/SRST/")
                || swiftActionValue.startsWith("/RSCD001/")
                || swiftActionValue.startsWith("/RSCD002/")) {
            result = Action.ACK;
        } else {
            throw new ProcessStatusException(
                    "No valid action was returned by the JMS queue: '"
                            + swiftActionValue + "'");
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.util.SwiftMessageHandler#getDescription(com.calypso.tk.bo
     * .swift.SwiftMessage)
     */
    @Override
    protected String getDescription(final SwiftMessage externalMessage) {
        final SwiftFieldMessage swiftDescription = externalMessage
                .getSwiftField(externalMessage.getFields(), ":76:", null, null);
        String result = null;
        if (swiftDescription.getValue().equalsIgnoreCase("/FVAL/")) {
            result = "invalid message for CLS";
        } else if (swiftDescription.getValue().startsWith("/RCNF/")) {
            result = "Message rejected with code: "
                    + swiftDescription.getValue().substring(6, 10);
        }
        return result;
    }

}
