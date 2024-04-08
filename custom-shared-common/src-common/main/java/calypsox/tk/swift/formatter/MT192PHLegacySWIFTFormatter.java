package calypsox.tk.swift.formatter;


import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.swift.formatter.MT192SWIFTFormatter;

import java.util.Vector;


/**
 * <p>
 * Title: PHFICCTLegacyMT192SWIFTFormatter
 * </p>
 * <p>
 * Description: Put the value of attribute TRN
 * </p>
 * .
 */
public class MT192PHLegacySWIFTFormatter extends MT192SWIFTFormatter {


    private static final String LOG_CATEGORY = MT192PHLegacySWIFTFormatter.class.getSimpleName();
    private BOMessage _message;


    public String parseORIGINAL_MT_AND_DATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return super.parseORIGINAL_MT_AND_DATE(message, trade, sender, rec, transferRules, transfer, dsCon);
    }


    public String parseCOPY_OF_ORIGINAL(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) throws MessageFormatException {
        // Builds the legacyBOMessage with the correct template
        final BOMessage originalLegacyBOMessage = getOriginalMessage(message, dsCon);
        if (originalLegacyBOMessage != null) {
            final SwiftMessage oldSwift = this.getOriginalSwift(message, dsCon);
            final boolean includeTag20 = Defaults.getBooleanProperty(Defaults.INCLUDE_TAG_20, true);
            final SwiftMessage newSwift = getSwiftMessage();
            final Vector<SwiftFieldMessage> fields = oldSwift.getFields();
            for (int i = 0; i < oldSwift.getFields().size(); ++i) {
                final SwiftFieldMessage field = fields.elementAt(i);
                if (includeTag20 || !field.getTAG().equals(":20:") && !field.getTAG().equals(":21:")) {
                    if (message.getSubAction().equals(Action.CANCEL)
                            && Util.isTrue(newSwift.getBOMessage().getAttribute("MEPS"), false) && field.getTAG().equals(":32A:")) {
                        field.setValue(SwiftUtil.getSwiftDate(JDate.getNow(), 6) + field.getValue().substring(6));
                    }
                    String val = field.getValue().replaceAll("\\\\r\\\\n", PHConstants.PATTERN_TO_CHANGE);
                    field.setValue(val);
                    newSwift.getFields().addElement(field);
                }
            }
        }
        return "";
    }

    @Override
    protected SwiftMessage generateSwift(PricingEnv env, BOMessage message, Trade trade, BOTransfer transfer, DSConnection dsCon)
            throws MessageFormatException {
        if(PaymentsHubUtil.isPHOrigin(message) && PaymentsHubUtil.hasCustomSDIs(transfer)){
            //Legacy info PH has custom SDIs update transfer SDI form msg
            transfer = PaymentsHubUtil.getBOTransfer(transfer.getLongId());
        }
        return super.generateSwift(env, message, trade, transfer, dsCon);
    }

/**
    @SuppressWarnings("rawtypes")
    @Override
    public String parseORIGINAL_MT_AND_DATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String result = "";
        // Get Original Message
        final BOMessage originalMsg = getOriginalMessage(message, dsCon);
        if (originalMsg != null) {
            final String type = getTranslateType(originalMsg);
            final long originalMsgId = originalMsg.getLongId();
            final JDatetime creationDate = originalMsg.getCreationDate();
            AdviceDocument adviceDoc = null;
            try {
                adviceDoc = DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(originalMsgId, (JDatetime) null);
            } catch (final Exception var16) {
                Log.error("Swift", "Cannot find the related advice document", var16);
            }
            result = adviceDoc != null && adviceDoc.getSentDate() != null ? type + SwiftMessage.END_OF_LINE + SwiftUtil.getSwiftDate2(adviceDoc.getSentDate().getJDate(TimeZone.getDefault()))
                    : type + SwiftMessage.END_OF_LINE + SwiftUtil.getSwiftDate2(creationDate.getJDate(TimeZone.getDefault()));
        }
        return result;
    }


    @Override
    protected BOMessage getOriginalMessage(final BOMessage boMessage, final DSConnection dsCon) {
        BOMessage originalLegacyBOMessage = null;
        final BOMessage originalMsg = super.getOriginalMessage(boMessage, dsCon);
        if (originalMsg != null) {
            final String template = getTranslateTemplateName(originalMsg.getTemplateName());
            try {
                originalLegacyBOMessage = PaymentsHubUtil.getLegacyMessage(originalMsg, template);
            } catch (final CloneNotSupportedException e) {
                Log.error(LOG_CATEGORY, "Error cloning BOMessage.");
            }
        }
        return originalLegacyBOMessage;
    }


    private static String getTranslateTemplateName(final String originalTemplateName) {
        String template = "";
        switch (originalTemplateName) {
            case PHConstants.MESSAGE_PH_FICCT: // MT103
                template = "MT103";
                break;
            case PHConstants.MESSAGE_PH_FICT: // MT202
                template = "MT202";
                break;
            case PHConstants.MESSAGE_PH_FICTCOV: // MT202COV
                template = "MT202COV";
                break;
            case PHConstants.MESSAGE_PH_NTR: // MT210
                template = "MT210";
                break;
            default:
                template = originalTemplateName;
                break;
        }
        return template;
    }


    private static String getTranslateType(final BOMessage message) {
        String type = getTranslateTemplateName(message.getTemplateName());
        if (!Util.isEmpty(type) && type.length() >= 5) {
            if (type.startsWith("MT")) {
                type = type.substring(2);
            }
            type = type.substring(0, 3);
        }
        return type;
    }
*/

}
