package calypsox.tk.bo;


import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubUtil;
import calypsox.tk.swift.formatter.MT202SWIFTFormatter;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;


public class PHFICTMessageFormatter extends PAYMENTHUB_PAYMENTMSGMessageFormatter {


    private static final String MESSAGE_TYPE = "General Financial Institution Transfer";


    public PHFICTMessageFormatter() {
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
     * Get Clearing and settlement time
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CLEARING_TIME(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // Settlement Time Request 13CTimeIndication
        String clsTm = xfer.getAttribute(KEYWORD_13CTIME_INDICATION);
        clsTm = Util.isEmpty(clsTm) ? (trade != null) ? trade.getKeywordValue(KEYWORD_13CTIME_INDICATION) : clsTm : clsTm;
        return clsTm;
    }


    /**
     * Get cdtrAgt bicfi
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_CREDITOR_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if (!PaymentsHubUtil.isMutuactivo(xfer)) {
            // Get BOTransfer field 'Receiver.Agent.Swift'
            return super.parseSANT_CREDITOR_AGENT_BICFI(boMessage, xfer, trade, dsCon);
        }
        return null;
    }


    /**
     * Get Debtor Agent Bicfi. dbtrAgt: bicfi
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if (PaymentsHubUtil.isCHAPS(xfer) || PaymentsHubUtil.isMutuactivo(xfer)) {
            // Get Payer.Agent.Swift
            final String payerAgentSwift = PaymentsHubUtil.getBOTransferPayerAgentSwift(trade, xfer);
            return !Util.isEmpty(payerAgentSwift) ? payerAgentSwift : null;
        }
        return null;
    }


    /**
     * Get Senders Correspondent Agent block. sndrsCorrespdntAgt
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_SENDS_CORRESPDNT_AGT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String payerAgentSwift = null;
        if (!PaymentsHubUtil.isCHAPS(xfer) && !PaymentsHubUtil.isMutuactivo(xfer)) {
            // Get Payer.Agent.Swift
            payerAgentSwift = PaymentsHubUtil.getBOTransferPayerAgentSwift(trade, xfer);
        }
        return !Util.isEmpty(payerAgentSwift) ? payerAgentSwift : null;
    }


    /**
     * Get Senders Correspondent Agent Account block. sndrsCorrespdntAgtAcc
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_SENDS_CORRESPDNT_AGT_ACC(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String payerAgentAccount = null;
        if (!PaymentsHubUtil.isTARGET2(xfer) && !PaymentsHubUtil.isMutuactivo(xfer)) {
            // Get Payer.Agent.Account
            payerAgentAccount = PaymentsHubUtil.getBOTransferPayerAgentAccount(trade, xfer);
        }
        return !Util.isEmpty(payerAgentAccount) ? payerAgentAccount : null;
    }


    /**
     * Get instrForNxtAgt
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_INSTR_FOR_NEXT_AGENT(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        String instrForNxtAgt =null;
        if (xfer != null) {
                // As field 72 from MT202
            instrForNxtAgt =getField72(boMessage, xfer, trade, dsCon);
        }
        return !Util.isEmpty(instrForNxtAgt) ? instrForNxtAgt : null;
    }


    /**
     * Get Filed 72 as MT202.
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    private String getField72(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
       return new MT202SWIFTFormatter()
               .parseSANT_ADDITIONAL_INFO(boMessage,trade,null,null,null,xfer,null,dsCon);
    }


    /**
     * Get intrmyAgt
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_INTERMEDIARY_AGENT_BICFI(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        if (!PaymentsHubUtil.isMutuactivo(xfer)) {
            // Get Receiver.Intermediary.Swift
            return super.parseSANT_INTERMEDIARY_AGENT_BICFI(boMessage, xfer, trade, dsCon);
        }
        return null;
    }


    /**
     * Get Debtor Account. dbtrAcct
     *
     * @param boMessage
     * @param xfer
     * @param trade
     * @param dsCon
     * @return
     */
    @Override
    public String parseSANT_DEBTOR_ACCOUNT_BICFI(BOMessage boMessage, BOTransfer xfer, Trade trade, DSConnection dsCon) {
        return null;
    }


    // ----------------------------------------------------------------- //
    // ---------------------- SUPPLEMENTARY DATA ----------------------- //
    // ----------------------------------------------------------------- //


    @Override
    public String parseSANT_LEGACY_INFO(final BOMessage boMessage, final BOTransfer xfer, final Trade trade, final DSConnection dsCon) {
        // MT202
        String legacyInfo = null;
        final PricingEnv env = getPricingEnv() != null ? getPricingEnv() : PaymentsHubUtil.getPricingEnv(dsCon);
        try {
            // Get legacyInfo
            // Template Selector
            final String template = PaymentsHubUtil.getLegacyTemplateSelector(boMessage, PHConstants.LEGACY_TEMPLATE_MT202);
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
