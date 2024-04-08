package calypsox.tk.util.swiftparser.util;

import com.calypso.analytics.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;

/**
 * SwiftMessageProcessorUtil
 *
 * @author Ruben Garcia
 */
public class SwiftMessageProcessorUtil {

    /**
     * Attribute that is reported in the ExternalMessage before being processed to indicate the source BOMessage,
     * for deletion/archiving purposes
     */
    public static final String ORIGINAL_MESSAGE_ID = "Original Message ID";

    /**
     * Updates the Original Message ID attribute on the message for traceability to the source message in case of rework.
     * @param message the BOMessage
     * @param swiftMessage the ExternalMessage
     */
    public static void updateBOMessageOriginalID(BOMessage message, SwiftMessage swiftMessage){
        if(message != null && swiftMessage != null && swiftMessage.getProperty(ORIGINAL_MESSAGE_ID) != null){
            message.setAttribute(ORIGINAL_MESSAGE_ID, swiftMessage.getProperty(ORIGINAL_MESSAGE_ID).toString());
        }
    }

    /**
     * Reports the Original Message ID property of the ExternalMessage with the ID of the BOMessage being reprocessed.
     * @param originalMsg the original BOMessage
     * @param externalMessage to reprocess external message
     */
    public static void addExternalMessageOriginalID(BOMessage originalMsg, ExternalMessage externalMessage){
        if(originalMsg != null && externalMessage instanceof SwiftMessage){
            ((SwiftMessage) externalMessage).addProperty(ORIGINAL_MESSAGE_ID, originalMsg.getLongId());
        }
    }

    public static String toDateStr(String dateTagVal) {
        if (!Util.isEmpty(dateTagVal)) {
            try {
                String[] qualifierAndVal = dateTagVal.split("//");
                if (qualifierAndVal.length == 2)
                    return SwiftParserUtil.getCalypsoDate(qualifierAndVal[1]).toString();
            } catch (MessageParseException e) {
                Log.error(SwiftMessageProcessorUtil.class, e);
            }
        }
        return null;
    }
}
