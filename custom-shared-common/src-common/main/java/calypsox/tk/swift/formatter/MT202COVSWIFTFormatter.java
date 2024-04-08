package calypsox.tk.swift.formatter;

import calypsox.tk.bo.swift.SantanderSwiftUtil;
import calypsox.tk.bo.util.PaymentsHubUtil;
import calypsox.tk.swift.formatter.common.NoTransferUpdateSWIFTFormatter;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.swift.TagValue;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Vector;


public class MT202COVSWIFTFormatter extends com.calypso.tk.swift.formatter.MT202COVSWIFTFormatter {

    /**
     * Formatter with methods that do not look for transfers in BDD or cache
     */
    private final NoTransferUpdateSWIFTFormatter noUpdateXferFormatter = new NoTransferUpdateSWIFTFormatter();

    /**
     * Fixed text
     */
    private static final String TEXT = "/BNF/IN COVER OF DIRECT SWIFT";

    /**
     * Return the MessageAttribute TRN.
     *
     * @param message       a message.
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
     * Return the MessageAttribute TRN of the message linked if exists.
     *
     * @param message       message to be formatter
     * @param trade         trade related to this message if applicable
     * @param sender        sender of the message
     * @param rec           receiver of the message
     * @param transferRules transfer rules
     * @param transfer      transfer related to this message if applicable
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
     * @return the formatted swift tag
     */
    @SuppressWarnings("rawtypes")
    public String parseSANT_COVER_LINKED_MSG_TRN(final BOMessage message, final Trade trade, final LEContact sender,
                                                 final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
                                                 final DSConnection dsCon) {
        // String for the templates name.
        String templateNameMsg = null;
        // String to return.
        String strReturned = null;
        // Array of messages for the same transfer that the MT202COV message.
        BOMessage[] msgArray = null;

        // Retrieves transfer ID for the message.
        final long msgTransferID = message.getTransferLongId();

        // From the DataBase, retrieves the messages MT103 in the same transfer
        // that the MT202COV message.
        try {
            msgArray = dsCon.getRemoteBO().getTransferMessages(msgTransferID).getMessages();

            if ((null != msgArray) && (msgArray.length > 0)) {
                for (int i = 0; (i < msgArray.length) && (null != msgArray[i]); i++) {
                    // Checks if the type message = MT103.
                    templateNameMsg = msgArray[i].getTemplateName();
                    if (templateNameMsg.equals("MT103")) {
                        // strReturned = TransactionReferenceNumber
                        // .getTRN(msgArray[i]);
                        // CAL_BO_146
                        // strReturned = TemplateUtil.getInstance()
                        // .getLinkedMessageTRN(msgArray[i],
                        // TemplateConstant.MESSAGE_NONREF);

                        // CAL_368_
                        // Method parseSANT_COVER_LINKED_MSG_TRN is only used
                        // for parsing field 21 in MT202COV messages. Use method
                        // getTRN to get the reference number from their linked
                        // MT103 message. Using method getLinkedMessageTRN
                        // results in field 21 having the value NONREF.
                        
                    	//strReturned = SantanderSwiftUtil.getTRN(msgArray[i]);
                    	strReturned = SantanderSwiftUtil.parseSANT_MSG_TRN(msgArray[i], sender, rec, transfer, dsCon);
                    }
                }
            }
        } catch (final RemoteException e) {
            Log.error(this, "Error retrieving messages from the DataBase with the transfer ID " + msgTransferID, e);

            strReturned = SantanderSwiftUtil.COV202_NONREF;
        }

        return strReturned;
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
     * returns additional info as a fixed text.
     *
     * @param message       message to be formatter
     * @param trade         trade related to this message if applicable
     * @param sender        sender of the message
     * @param rec           receiver of the message
     * @param transferRules transferrules
     * @param transfer      transfer related to this message if applicable
     * @param format        format of this keyword as defined in the template
     * @param dsCon         ds connection
     * @return the formatted swift tag
     */
    @SuppressWarnings("rawtypes")
    public String parseSANT_ADDITIONAL_INFO(final BOMessage message, final Trade trade, final LEContact sender,
                                            final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
                                            final DSConnection dsCon) {
        String sToReturn = MT202COVSWIFTFormatter.TEXT;

        return sToReturn;
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
    public boolean isAgentNotBeneficiaryOrReceiver(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        if (PaymentsHubUtil.isPHOrigin(message) && PaymentsHubUtil.hasCustomSDIs(transfer)) {
            //No update transfer use current custom SDI config
            return noUpdateXferFormatter.isAgentNotBeneficiaryOrReceiver(message, trade, sender, rec, transferRules, transfer, con);
        }
        return super.isAgentNotBeneficiaryOrReceiver(message, trade, sender, rec, transferRules, transfer, con);
    }
}
