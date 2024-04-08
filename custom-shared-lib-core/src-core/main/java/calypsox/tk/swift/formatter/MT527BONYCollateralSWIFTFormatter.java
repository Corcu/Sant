package calypsox.tk.swift.formatter;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.swift.TagValue;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Vector;

/**
 * Class to customize the contents of several fields for Triparty messages.
 */
public class MT527BONYCollateralSWIFTFormatter extends
        com.calypso.tk.bo.swift.MT527BONYCollateralSWIFTFormatter {
    private static final String BONY_EXPOSURE_TYPE_TAG = ":22F:";
    private static final String BONY_EXPOSURE_TYPE_OPTION = "F";
    private static final String BONY_EXPOSURE_TYPE_VALUE = ":COLA/BNYM/TRCS";
    private static final String EXPOSURE_TYPE_TAG = "EXPOSURE_TYPE_TAG";

    /**
     * This method returns a specific format and value for BONY messages.
     */

    public String parseEXPOSURE_TYPE(BOMessage message, Trade trade,
                                     LEContact sender, LEContact rec, Vector transferRules,
                                     BOTransfer transfer, DSConnection dsCon) {

        TagValue exposureTypeTagValue = getTagValue(message);

        setTagValue(exposureTypeTagValue);

        // check contract first
        String exposureTypeValue = CustomMT527CollateralSWIFTFormatter
                .getExpTypeValue(message);
        if (!Util.isEmpty(exposureTypeValue)) {
            exposureTypeTagValue.setValue(exposureTypeValue);
            return exposureTypeValue;
        }

        return BONY_EXPOSURE_TYPE_VALUE;
    }


    private TagValue getTagValue(BOMessage message) {
        if (message != null) {
            String attribute = message.getAttribute("marginCallConfigId");
            if (!Util.isEmpty(attribute)) {
                CollateralConfig contract = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                        Integer.parseInt(attribute));
                if (null != contract && !Util.isEmpty(contract.getAdditionalField(EXPOSURE_TYPE_TAG))) {
                    String tagOption = contract.getAdditionalField(EXPOSURE_TYPE_TAG);
                    String tag = ":22" + tagOption + ":";
                    return new TagValue(tag,
                            tagOption, BONY_EXPOSURE_TYPE_VALUE, 0);
                }
                Log.error(CustomMT527CollateralSWIFTFormatter.class, "Could not load contract: " + attribute);
            }
        }

        //retrun default TagValue
        return new TagValue(BONY_EXPOSURE_TYPE_TAG, BONY_EXPOSURE_TYPE_OPTION, BONY_EXPOSURE_TYPE_VALUE, 0);
    }


    public String parsePRINCIPAL_AMOUNT(BOMessage message, Trade trade,
                                        LEContact sender, LEContact rec, Vector transferRules,
                                        BOTransfer transfer, String format, DSConnection con)
            throws MessageFormatException {
        String value = CustomMT527CollateralSWIFTFormatter.getInstance()
                .parsePRINCIPAL_AMOUNT(message, trade, sender, rec,
                        transferRules, transfer, format, con);
        if (Util.isEmpty(value)) {
            value = super.parsePRINCIPAL_AMOUNT(message, trade, sender, rec,
                    transferRules, transfer, format, con);
        }

        return value;
    }

    public String parseMESSAGE_SUBACTION(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                         Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        String value = CustomMT527CollateralSWIFTFormatter.getInstance().parseMESSAGE_SUBACTION(message, trade, sender,
                rec, transferRules, transfer, dsCon);
        if (Util.isEmpty(value)) {
            value = super.parseMESSAGE_SUBACTION(message, trade, sender, rec, transferRules, transfer, dsCon);
        }
        return value;

    }

    public String parseINSTRUCTION_TYPE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                        Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String value = CustomMT527CollateralSWIFTFormatter.getInstance().parseINSTRUCTION_TYPE(message, trade, sender, rec, transferRules, transfer, dsCon);
        if (Util.isEmpty(value)) {
            value = super.parseINSTRUCTION_TYPE(message, trade, sender, rec, transferRules, transfer, dsCon);
        }
        return value;
    }

    public String parseCLIENT_TRADE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                       Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String value = CustomMT527CollateralSWIFTFormatter.getInstance().parseCLIENT_TRADE_ID(message, trade, sender, rec, transferRules, transfer, dsCon);
        if (Util.isEmpty(value)) {
            value = super.parseCLIENT_TRADE_ID(message, trade, sender, rec, transferRules, transfer, dsCon);
        }
        return value;
    }

    public String parseSEND_TRADE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                     Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String value = CustomMT527CollateralSWIFTFormatter.getInstance().parseSEND_TRADE_ID(message, trade, sender, rec, transferRules, transfer, dsCon);
        if (Util.isEmpty(value)) {
            value = super.parseSEND_TRADE_ID(message, trade, sender, rec, transferRules, transfer, dsCon);
        }
        return value;
    }

    public boolean hasEligibilitySet(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con){
        if (message != null) {
            String attribute = message.getAttribute("marginCallConfigId");
            if (!Util.isEmpty(attribute)){
                CollateralConfig contract = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), Integer.parseInt(attribute));
                if (contract != null  && !Util.isEmpty(contract.getTripartyEligibility())) {
                    return true;
                }
            }
        }

        return false;
    }
}
