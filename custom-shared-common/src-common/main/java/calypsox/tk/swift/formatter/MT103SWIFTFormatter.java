package calypsox.tk.swift.formatter;

import calypsox.tk.bo.swift.SantanderSwiftUtil;
import calypsox.tk.bo.util.PaymentsHubUtil;
import calypsox.tk.swift.formatter.common.NoTransferUpdateSWIFTFormatter;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.bo.swift.TagValue;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;


public class MT103SWIFTFormatter extends
        com.calypso.tk.swift.formatter.MT103SWIFTFormatter {
    private static final String SENDER_SWIFT = "SWIFT";
    /**
     * Attribute filled when a 103 has cover message related
     */
    private static final String ATTRIBUTE_COVER_MESSAGE = "HasCoverMessage";

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
     * @return the message attribute TRN..
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
     * Return the Charges details code. By default "OUR".
     *
     * @param message       a message
     * @param trade         a trade
     * @param sender        sender
     * @param rec           receiver
     * @param transferRules transferRules
     * @param transfer      transfer
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
     * @return "OUR"
     */
    @SuppressWarnings("rawtypes")
    public String parseSANT_DETAILS_CHARGES(final BOMessage message, final Trade trade,
                                            final LEContact sender, final LEContact rec,
                                            final Vector transferRules, final BOTransfer transfer,
                                            final String format, final DSConnection dsCon) {

        return SantanderSwiftUtil.parseSANT_DETAILS_CHARGES(message, trade, sender, rec, transferRules, transfer, format, dsCon);

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
    protected SwiftMessage generateSwift(PricingEnv env, BOMessage message, Trade trade, BOTransfer transfer, DSConnection dsCon)
            throws MessageFormatException {
        if(PaymentsHubUtil.isPHOrigin(message) && PaymentsHubUtil.hasCustomSDIs(transfer)){
            //Legacy info PH has custom SDIs update transfer SDI for msg
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

    @Override
    public boolean hasIntermediary2(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        if (PaymentsHubUtil.isPHOrigin(message) && PaymentsHubUtil.hasCustomSDIs(transfer)) {
            //No update transfer use current custom SDI config
            return noUpdateXferFormatter.hasIntermediary2(message, trade, sender, rec, transferRules, transfer, con);
        }
        return super.hasIntermediary2(message, trade, sender, rec, transferRules, transfer, con);
    }

    @Override
    public boolean hasIntermediary(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        if (PaymentsHubUtil.isPHOrigin(message) && PaymentsHubUtil.hasCustomSDIs(transfer)) {
            //No update transfer use current custom SDI config
            return noUpdateXferFormatter.hasIntermediary(message, trade, sender, rec, transferRules, transfer, con);
        }
        return super.hasIntermediary(message, trade, sender, rec, transferRules, transfer, con);
    }

}  //fin clase
