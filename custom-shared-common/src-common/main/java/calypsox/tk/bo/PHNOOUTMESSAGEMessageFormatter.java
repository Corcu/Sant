package calypsox.tk.bo;


import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;


public class PHNOOUTMESSAGEMessageFormatter extends PAYMENTHUBMSGMessageFormatter {


    private static final String MESSAGE_TYPE = "Single Customer Credit Transfer";


    public PHNOOUTMESSAGEMessageFormatter() {
    }


    // ----------------------------------------------------------------- //
    // -------------------------- GROUP HEADER ------------------------- //
    // ----------------------------------------------------------------- //


    @Override
    public String parseSANT_MESSAGE_TYPE(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        return MESSAGE_TYPE;
    }


    // ----------------------------------------------------------------- //
    // ------------------ CREDIT TRANSFER INSTRUCTION ------------------ //
    // ----------------------------------------------------------------- //


    /**
     * Get cdtr: country -> BOTransfer field 'Payer.Country'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_COUNTRY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer field 'Payer.Country'
        final String payerCountry = PaymentsHubUtil.getBOTransferPayerCountry(xfer);
        final Country country = BOCache.getCountry(dsCon, payerCountry);
        return (country != null && !Util.isEmpty(country.getISOCode())) ? country.getISOCode() : null;
    }


    /**
     * Get cdtr name -> Get BOTransfer field 'Payer.Full Name'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_FULL_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get BOTransfer field 'Payer.Full Name'
        final String payerFullName = PaymentsHubUtil.getBOTransferPayerFullName(xfer);
        return !Util.isEmpty(payerFullName) ? payerFullName : null;
    }


    /**
     * Get Debtor Account block. dbtrAcct
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_ACCOUNT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String glAccountName = null;
        if (xfer.getPayReceiveType().equals(PHConstants.XFER_TYPE_RECEIVE)) {
            final SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(dsCon, xfer.getExternalSettleDeliveryId());
            if (sdi != null) {
                final int glAccountId = sdi.getGeneralLedgerAccount();
                final com.calypso.tk.refdata.Account glAccount = BOCache.getAccount(dsCon, glAccountId);
                if (glAccount != null) {
                    glAccountName = glAccount.getName();
                }
            }
        }
        return !Util.isEmpty(glAccountName) ? glAccountName : null;
    }


    /**
     * Get Creditor Account block. cdtrAcct
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_ACCOUNT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String glAccountName = null;
        if (xfer.getPayReceiveType().equals(PHConstants.XFER_TYPE_PAY)) {
            final SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(dsCon, xfer.getExternalSettleDeliveryId());
            if (sdi != null) {
                final int glAccountId = sdi.getGeneralLedgerAccount();
                final com.calypso.tk.refdata.Account glAccount = BOCache.getAccount(dsCon, glAccountId);
                if (glAccount != null) {
                    glAccountName = glAccount.getName();
                }
            }
        }
        return !Util.isEmpty(glAccountName) ? glAccountName : null;
    }


    /**
     * Get End To End Identification (endToEndId).
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_END_TO_END_ID(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


    /**
     * Get BOMessage Receiver Address Code
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_INSTD_AGT_BICFI(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        return null;
    }


    /**
     * Get BOMessage Sender Address Code
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_INSTG_AGT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


    /**
     * Get dbtr
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


    /**
     * Get BOTransfer field 'Receiver.Swift'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
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
        return null;
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
        return null;
    }


    /**
     * Get BOTransfer field 'Receiver.Code' and get the City.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_CITY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


    /**
     * Get CreditTransferInstruction -> FinancialInstitution -> Party -> Bicfi.
     *
     * Get BOTransfer field 'Receiver.Agent.Swift'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


    /**
     * Get BOTransfer field 'Receiver.Intermediary.Swift'
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_INTERMEDIARY_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


    /**
     * Get intrmyAgtAcct
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_INTERMEDIARY_AGENT_ACCOUNT(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


    /**
     * Get dbtrAgt
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


    /**
     * Get Sender to Receiver Information block. instrForNxtAgt
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_INSTR_FOR_NEXT_AGENT(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


    // ----------------------------------------------------------------- //
    // ---------------------- SUPPLEMENTARY DATA ----------------------- //
    // ----------------------------------------------------------------- //


    /**
     * Get Delivery Flag -> FALSE
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DELIVERY_FLAG(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return Boolean.FALSE.toString();
    }


    @Override
    public String parseSANT_LEGACY_INFO(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


}
