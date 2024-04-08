package calypsox.tk.bo;


import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;

import java.util.regex.Pattern;


public class PHFICTCOVMessageFormatter extends PAYMENTHUB_PAYMENTMSGMessageFormatter {


    /** Tag 21 */
    private static final String TAG_21 = ":21:";
    private static final String MESSAGE_TYPE = "General Financial Institution Transfer";


    public PHFICTCOVMessageFormatter() {
    }


    // ----------------------------------------------------------------- //
    // -------------------------- GROUP HEADER ------------------------- //
    // ----------------------------------------------------------------- //


    @Override
    public String parseSANT_MESSAGE_TYPE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return MESSAGE_TYPE;
    }


    // ----------------------------------------------------------------- //
    // ------------------ CREDIT TRANSFER INSTRUCTION ------------------ //
    // ----------------------------------------------------------------- //


    /**
     * Get PaymentTypeInformation Code
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_PAYMENT_TYPE_INFO_CODE(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return PHConstants.SSTD;
    }


    /**
     * Get cdtrAgt Party Name -> Not Apply
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_AGENT_PARTY_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null; // N/A
    }


    /**
     * Get cdtrAgtAcc -> Not Apply
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_AGT_ACC(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        return null; // N/A
    }


    // ----------------------------------------------------------------- //
    // ---------------------- SUPPLEMENTARY DATA ----------------------- //
    // ----------------------------------------------------------------- //


    @Override
    public String parseSANT_LEGACY_INFO(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // MT202COV
        String legacyInfo = null;
        final PricingEnv env = getPricingEnv() != null ? getPricingEnv() : PaymentsHubUtil.getPricingEnv(dsCon);
        try {
            // Get legacyInfo
            // Template Selector
            final String template = PaymentsHubUtil.getLegacyTemplateSelector(boMessage, PHConstants.LEGACY_TEMPLATE_MT202COV);
            // Builds the legacyBOMessage with the correct template
            final BOMessage legacyBOMessage = PaymentsHubUtil.getLegacyMessage(boMessage, template);
            // Get legacyInfo
            legacyInfo = PaymentsHubUtil.getLegacyInfo(legacyBOMessage, env, dsCon);
            // Add Tag :21: to Legacy MT202COV
            legacyInfo = addTag21(legacyInfo, boMessage, dsCon);
        } catch (final CloneNotSupportedException e) {
            final String msg = String.format("Error cloning BOMessage [%s]", String.valueOf(boMessage.getLongId()));
            Log.error(this, msg, e);
        }
        return PaymentsHubUtil.adaptNewLineToLegacyInfo(legacyInfo);
    }


    /**
     * Add Tag :21: SANT_COVER_LINKED_MSG_TRN to Legacy Info MT202COV.
     *
     * @param legacyInfo
     * @param boMessagePHFICTCOV
     * @param dsCon
     * @return
     */
    private static String addTag21(final String legacyInfo, final BOMessage boMessagePHFICTCOV, final DSConnection dsCon) {
        String debug = "";
        String legacyInfoMod = new String(legacyInfo);
        // Get PH-FICCT Message
        final BOMessage boMessagePHFICCT = PaymentsHubUtil.getPaymentsHubLinkedMessageByTransfer(PHConstants.MESSAGE_PH_FICCT, boMessagePHFICTCOV, dsCon);
        if (boMessagePHFICCT != null) {
            // Get TRN of BOMessage PHFICCT
            //final String trn = PaymentsHubUtil.getTRN(boMessagePHFICCT);
            final String trn = String.valueOf(boMessagePHFICCT.getTransferLongId());
            if (!Util.isEmpty(trn)) {
                if (legacyInfo.contains(TAG_21)) {
                    final String tagValue = getTagValue(legacyInfo, TAG_21);
                    if (Util.isEmpty(tagValue)) {
                        legacyInfoMod = legacyInfo.replaceAll(TAG_21.concat(tagValue), TAG_21.concat(trn));
                    }
                } else {
                    // LegacyInfo does not contain Tag :21:
                    debug = String.format("LegacyInfo MT202COV from PH-FICTCOV %d does not contain Tag :21:", boMessagePHFICTCOV.getLongId());
                    Log.warn(LOG_CATEGORY, debug);
                }
            }
        } else {
            // No boMessageFICCT found
            debug = String.format("No PH-FICCT message found from PH-FICTCOV %d", boMessagePHFICTCOV.getLongId());
            Log.warn(LOG_CATEGORY, debug);
        }
        return legacyInfoMod;
    }


    /**
     * Get Tag Value from LegacyInfo
     *
     * @param legacyInfo
     * @param tag
     * @return
     */
    private static String getTagValue(final String legacyInfo, final String tag) {
        final String subString = legacyInfo.substring(legacyInfo.indexOf(tag) + tag.length(), legacyInfo.length());
        int pos = 0;
        final String pattern = "^[-}|:]$";
        for (int i = 0; i < subString.length(); i++) {
            final char c = subString.charAt(i);
            if (Pattern.matches(pattern, Character.toString(c))) {
                pos = i;
                break;
            }
        }
        final int indexTagValue = legacyInfo.indexOf(tag) + tag.length();
        return legacyInfo.substring(indexTagValue, pos + indexTagValue).trim();
    }


}
