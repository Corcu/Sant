package calypsox.tk.bo;


import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;


public class PaymentHubCOVTemplateSelector extends PaymentHubTemplateSelector {


    /**
     * Instantiates a new payment hub template selector.
     */
    public PaymentHubCOVTemplateSelector() {
        // do nothing
    }


    /**
     * Financial Institution Cover Credit Transfer Template
     *
     * @return
     */
    protected String getFICTCOVType() {
        return PHConstants.MESSAGE_PH_FICTCOV;
    }


    @Override
    public String getTemplate(final Trade trade, final BOMessage boMessage, final String name) {
        if ((boMessage == null) || Util.isEmpty(boMessage.getMessageType())) {
            Log.info(this, "Null Message");
            return null;
        }
        final DSConnection dsCon = DSConnection.getDefault();
        BOTransfer xfer = null;
        try {
            xfer = PaymentsHubUtil.getBOTransfer(boMessage.getTransferLongId());
        } catch (final Exception e) {
            Log.error(this.getClass().getName(), "Could not get transfer", e);
        }
        // Get Template by StaticDataFilter Exceptions.
        String template = getSDFExceptionTemplate(trade, xfer, boMessage, PHConstants.COV_SDF_EXCEPTION_NAMES, dsCon);
        if (Util.isEmpty(template)) {
            if (xfer == null) {
                template = getTypeFICT();
            } else {
                if (xfer.getPayReceiveType().equals(PHConstants.XFER_TYPE_RECEIVE)) {
                    template = getTypeNTR();
                } else {
                    final TradeTransferRule tradeXferRule = xfer.toTradeTransferRule();
                    if (tradeXferRule == null) {
                        template = getTypeFICT();
                    } else {
                        final boolean flagFinancial = isFinantial(dsCon, boMessage, xfer);
                        if (!flagFinancial) {
                            final boolean flagCover = SwiftUtil.isCoverMessage(boMessage, tradeXferRule, dsCon);
                            if (flagCover) {
                                template = getFICTCOVType();
                            } else {
                                if (SwiftUtil.isCoverMessageRequired(boMessage, tradeXferRule, dsCon, null)) {
                                    boMessage.setAttribute(PHConstants.MSG_ATTRIBUTE_COVER_MESSAGE, Boolean.TRUE.toString());
                                }
                                template = getTypeFICCT();
                            }
                        } else {
                            template = getTypeFICT();
                        }
                    }
                }
            }
        }
        return template;
    }


}
