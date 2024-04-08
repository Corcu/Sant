package calypsox.tk.swift.formatter;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * Class to customize the contents of several fields for Triparty messages.
 */
public class MT527EuroclearCollateralSWIFTFormatter
        extends com.calypso.tk.bo.swift.MT527EuroclearCollateralSWIFTFormatter {
    public String parseEXPOSURE_TYPE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                     Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return CustomMT527CollateralSWIFTFormatter.getInstance().parseEXPOSURE_TYPE(message, trade, sender, rec,
                transferRules, transfer, dsCon);
    }

    public String parsePRINCIPAL_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                        Vector transferRules, BOTransfer transfer, String format, DSConnection con) throws MessageFormatException {
        String value = CustomMT527CollateralSWIFTFormatter.getInstance().parsePRINCIPAL_AMOUNT(message, trade, sender,
                rec, transferRules, transfer, format, con);
        if (Util.isEmpty(value)) {
            value = super.parsePRINCIPAL_AMOUNT(message, trade, sender, rec, transferRules, transfer, format, con);
        }

        return value;
    }

    /**
     * MUST BE REMOVED AFTER CALYPSO FIX
     */
    public String parseELIGIBILITY(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                   Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String es = getEligibilitySet(message, dsCon);

        return ":ELIG/ECLR/" + es;
    }

    public String parseMESSAGE_SUBACTION(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                         Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        String value = CustomMT527CollateralSWIFTFormatter.getInstance().parseMESSAGE_SUBACTION(message, trade, sender, rec, transferRules, transfer, dsCon);
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

}
