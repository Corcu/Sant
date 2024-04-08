package calypsox.tk.bo;


import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubUtil;
import calypsox.tk.core.SantanderUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;


public class PHNTRMessageFormatter extends PAYMENTHUBMSGMessageFormatter {


    private static final String MESSAGE_TYPE = "Collection Instruction";


    public PHNTRMessageFormatter() {
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
     * Get dbtr -> BOTransfer field 'Payer.Swift' when CPTY is NonFinancial
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get if CounterParty is Financial
        final boolean isFinancial = PaymentsHubUtil.isCounterPartyFinancial(trade, xfer, dsCon);
        if (!isFinancial) {
            // Get BOTransfer field 'Payer.Swift'
            return super.parseSANT_DEBTOR_BICFI(boMessage, xfer, trade, dsCon);
        }
        return null;
    }


    /**
     * Get dbtrAcct -> BOTransfer field 'Payer.Agent.Account' when CPTY is NonFinancial
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_ACCOUNT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get if CounterParty is Financial
        final boolean isFinancial = PaymentsHubUtil.isCounterPartyFinancial(trade, xfer, dsCon);
        if (!isFinancial) {
            // Get BOTransfer field 'Payer.Agent.Account'
            final String payerAgentAccount = PaymentsHubUtil.getBOTransferPayerAgentAccount(trade, xfer);
            return !Util.isEmpty(payerAgentAccount) ? payerAgentAccount : null;
        }
        return null;
    }


    /**
     * Get dbtrAgt bicfi
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get if CounterParty is Financial
        final boolean isFinancial = PaymentsHubUtil.isCounterPartyFinancial(trade, xfer, dsCon);
        if (isFinancial) {
            // Get BOTransfer field 'Payer.Agent.Swift'
            final String payerAgentSwift = PaymentsHubUtil.getBOTransferPayerAgentSwift(trade, xfer);
            return !Util.isEmpty(payerAgentSwift) ? payerAgentSwift : null;
        }
        return null;
    }


    /**
     * Get dbtr: city -> BOTransfer field 'Payer.Code' and get the City when CPTY is NonFinancial.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_CITY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get if CounterParty is Financial
        final boolean isFinancial = PaymentsHubUtil.isCounterPartyFinancial(trade, xfer, dsCon);
        if (!isFinancial) {
            // Get BOTransfer field 'Payer.Code'
            final LegalEntity le = PaymentsHubUtil.getLegalEntity(xfer, XFER_TYPE_PAY);
            final LEContact contact = BOCache.getContact(dsCon, "ALL", le, "Default", "ALL", 0);
            return contact != null && !Util.isEmpty(contact.getCityName()) ? contact.getCityName() : null;
        }
        return null;
    }


    /**
     * Get dbtr: country -> BOTransfer field 'Payer.Country' when CPTY is NonFinancial
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_COUNTRY(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Get if CounterParty is Financial
        final boolean isFinancial = PaymentsHubUtil.isCounterPartyFinancial(trade, xfer, dsCon);
        if (!isFinancial) {
            // Get BOTransfer field 'Payer.Country'
            final String payerCountry = PaymentsHubUtil.getBOTransferPayerCountry(xfer);
            final Country country = BOCache.getCountry(dsCon, payerCountry);
            return (country != null && !Util.isEmpty(country.getISOCode())) ? country.getISOCode() : null;
        }
        return null;
    }


    /**
     * Get intrmyAgt ->
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_INTERMEDIARY_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String rst = null;
        // Check if currency is PLN. If not, do not send this field
        if (xfer != null && SantanderUtil.PLN.equals(xfer.getSettlementCurrency())) {
            // Get ExternalSdi
            final int sdiId = xfer.getExternalSettleDeliveryId();
            // HasIntermediary
            final boolean hasIntermediary = PaymentsHubUtil.hasIntermediary(sdiId, dsCon);
            // HasIntermediary2
            final boolean hasIntermediary2 = PaymentsHubUtil.hasIntermediary2(sdiId, dsCon);
            if (!hasIntermediary && !hasIntermediary2) {
                // BOTransfer "Payer.Agent.Swift"
                rst = PaymentsHubUtil.getBOTransferPayerAgentSwift(trade, xfer);
            } else if (hasIntermediary && !hasIntermediary2) {
                // BOTransfer "Payer.Intermediary.Swift"
                rst = PaymentsHubUtil.getBOTransferPayerIntermediarySwift(trade, xfer);
            } else if (hasIntermediary2) {
                // BOTransfer "Payer.Intermediary2.Swift"
                rst = PaymentsHubUtil.getBOTransferPayerIntermediary2Swift(trade, xfer);
            }
        }
        return rst;
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
     * Get cdtr:instId:pstlAdr:twnNm -> Not Apply
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_CITY(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        return null;
    }


    /**
     * Get cdtr: instId: pstlAdr: ctry -> Not Apply
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_COUNTRY(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        return null;
    }


    /**
     * Get intrmyAgtAcct -> Not Apply
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
     * Get cdtr bicfi -> Not Apply
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
     * Get cdtr name -> Not Apply
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_FULL_NAME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        return null;
    }


    // ----------------------------------------------------------------- //
    // ---------------------- SUPPLEMENTARY DATA ----------------------- //
    // ----------------------------------------------------------------- //


    @Override
    public String parseSANT_LEGACY_INFO(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // MT210
        String legacyInfo = null;
        final PricingEnv env = getPricingEnv() != null ? getPricingEnv() : PaymentsHubUtil.getPricingEnv(dsCon);
        try {
            // Get legacyInfo
            // Template Selector
            final String template = PaymentsHubUtil.getLegacyTemplateSelector(boMessage, PHConstants.LEGACY_TEMPLATE_MT210);
            // Builds the legacyBOMessage with the correct template
            final BOMessage legacyBOMessage = PaymentsHubUtil.getLegacyMessage(boMessage, template);
            // Get legacyInfo
            legacyInfo = PaymentsHubUtil.getLegacyInfo(legacyBOMessage, env, dsCon);
        } catch (final CloneNotSupportedException e) {
            final String msg = String.format("Error cloning BOMessage [%s]", String.valueOf(boMessage.getLongId()));
            Log.error(this, msg, e);
        }
        return PaymentsHubUtil.adaptNewLineToLegacyInfo(legacyInfo);
    }


}
