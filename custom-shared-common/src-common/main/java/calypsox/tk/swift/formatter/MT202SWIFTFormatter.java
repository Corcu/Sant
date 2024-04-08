package calypsox.tk.swift.formatter;

import calypsox.tk.bo.swift.SantanderSwiftUtil;
import calypsox.tk.bo.util.PaymentsHubUtil;
import calypsox.tk.swift.formatter.common.NoTransferUpdateSWIFTFormatter;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.swift.TagValue;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;


public class MT202SWIFTFormatter extends com.calypso.tk.swift.formatter.MT202SWIFTFormatter {

    /**
     * Formatter with methods that do not look for transfers in BDD or cache
     */
    private final NoTransferUpdateSWIFTFormatter noUpdateXferFormatter = new NoTransferUpdateSWIFTFormatter();

    /**
     * Return the MessageAttribute TRN.
     *
     * @param message       a message
     * @param trade         a trade
     * @param sender        sender
     * @param rec           receiver
     * @param transferRules transferRules
     * @param transfer      transfer
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
     * @return the message attribute TRN
     */
    @SuppressWarnings("rawtypes")
    public String parseSANT_MSG_TRN(final BOMessage message, final Trade trade,
                                    final LEContact sender, final LEContact rec,
                                    final Vector transferRules, final BOTransfer transfer,
                                    final String format, final DSConnection dsCon) {
        return SantanderSwiftUtil.parseSANT_MSG_TRN(
                message, sender, rec, transfer, dsCon);
    }

    /**
     * Returns a String formatted with PO agent BIC and account number.
     *
     * @param message       a message
     * @param trade         a trade
     * @param sender        sender
     * @param rec           receiver
     * @param transferRules transferRules
     * @param transfer      transfer
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
     * @return 53A: a String formatted with PO agent BIC and account number..
     */
    @SuppressWarnings("rawtypes")
    public String parseSANT_PO_DELIVERY_AGENT(final BOMessage message, final Trade trade,
                                              final LEContact sender, final LEContact rec,
                                              final Vector transferRules, final BOTransfer transfer,
                                              final String format, final DSConnection dsCon) {

        final String Valortag = SantanderSwiftUtil.parseSANT_PO_DELIVERY_AGENT(message, trade, sender, rec, transferRules, transfer, format, dsCon);
        //forzamos siempre Opcion A para campo 53a
        TagValue tagValue = new TagValue();
        tagValue.setOption("A");
        this.setTagValue(tagValue);
        return Valortag;
    }

    /**
     * Return the MessageAttribute SENDER TO RECEIVER INFO.
     *
     * @param message       a message
     * @param trade         a trade
     * @param sender        sender
     * @param rec           receiver
     * @param transferRules transferRules
     * @param transfer      transfer
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
     * @return the message attribute BNF/ “CLAIM ISIN XXXXX”
     */
    @SuppressWarnings("rawtypes")
    public String parseSANT_ADDITIONAL_INFO(final BOMessage message, final Trade trade,
                                            final LEContact sender, final LEContact rec,
                                            final Vector transferRules, final BOTransfer transfer,
                                            final String format, final DSConnection dsCon) {

        String additionalInfo = parseADDITIONAL_INFO(message, trade, sender, rec, transferRules, transfer, format, dsCon);
        String santAdditionalInfo = SantanderSwiftUtil.parseSANT_ADDITIONAL_INFO(message, trade, sender, rec, transferRules, transfer, format, dsCon);
        if (Util.isEmpty(santAdditionalInfo)) {
            return additionalInfo;
        } else {
            return SantanderSwiftUtil.parseSANT_ADDITIONAL_INFO(message, trade, sender, rec, transferRules, transfer, format, dsCon) + " " + additionalInfo;
        }
    }

    @Override
    public boolean ordererNotPO(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        boolean outDefault = super.ordererNotPO(message, trade, sender, rec, transferRules, transfer, con);
        return trade.getProduct() != null ? trade.getProduct() instanceof Bond ? true : outDefault : outDefault;
    }

    @Override
    public String parseSEND_BENEFICIARY(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection dsCon) {
        String ss = super.parseSEND_BENEFICIARY(message, trade, sender, rec, transferRules, transfer, format, dsCon);
        return trade.getProduct() != null ? trade.getProduct() instanceof Bond ? SantanderSwiftUtil.parseSANT_PO_BENEFICIARY(message, trade, transfer, dsCon) : ss : ss;
    }

    @Override
    protected SwiftMessage generateSwift(PricingEnv env, BOMessage message, Trade trade, BOTransfer transfer, DSConnection dsCon)
            throws MessageFormatException {
        if (PaymentsHubUtil.isPHOrigin(message) && PaymentsHubUtil.hasCustomSDIs(transfer)) {
            //Legacy info PH has custom SDIs update transfer SDI form msg
            transfer = PaymentsHubUtil.getBOTransfer(transfer.getLongId());
        }
        return super.generateSwift(env, message, trade, transfer, dsCon);
    }


    @Override
    public String parseCPTY_INTERMEDIARY2(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        if (PaymentsHubUtil.isPHOrigin(message) && PaymentsHubUtil.hasCustomSDIs(transfer)) {
            //No update transfer use current custom SDI config
            return noUpdateXferFormatter.parseCPTY_INTERMEDIARY2(message, trade, sender, rec, transferRules, transfer, format, con);
        }
        return super.parseCPTY_INTERMEDIARY2(message, trade, sender, rec, transferRules, transfer, format, con);
    }

    @Override
    public String parseCPTY_INTERMEDIARY(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection dsCon) {
        if (PaymentsHubUtil.isPHOrigin(message) && PaymentsHubUtil.hasCustomSDIs(transfer)) {
            //No update transfer use current custom SDI config
            return noUpdateXferFormatter.parseCPTY_INTERMEDIARY(message, trade, sender, rec, transferRules, transfer, format, dsCon);
        }
        return super.parseCPTY_INTERMEDIARY(message, trade, sender, rec, transferRules, transfer, format, dsCon);
    }

    @Override
    public String parseCPTY_RECEIVING_AGENT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        if (PaymentsHubUtil.isPHOrigin(message) && PaymentsHubUtil.hasCustomSDIs(transfer)) {
            //No update transfer use current custom SDI config
            return noUpdateXferFormatter.parseCPTY_RECEIVING_AGENT(message, trade, sender, rec, transferRules, transfer, format, con);
        }
        return super.parseCPTY_RECEIVING_AGENT(message, trade, sender, rec, transferRules, transfer, format, con);
    }

    @Override
    public String parseCPTY_BENEFICIARY(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        if (PaymentsHubUtil.isPHOrigin(message) && PaymentsHubUtil.hasCustomSDIs(transfer)) {
            //No update transfer use current custom SDI config
            return noUpdateXferFormatter.parseCPTY_BENEFICIARY(message, trade, sender, rec, transferRules, transfer, format, con);
        }
        return super.parseCPTY_BENEFICIARY(message, trade, sender, rec, transferRules, transfer, format, con);
    }
}
