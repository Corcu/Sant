package calypsox.tk.bo.cremapping.event;

import calypsox.tk.core.SantanderUtil;
import com.calypso.tk.bo.*;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class BOCreFXMATURITY extends BOCreFXBOOKING {

    public BOCreFXMATURITY(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected String loadBuyAgent() {
        return loadBuySellAgent(true);
    }
    @Override
    protected String loadBuySettleMethod(){
        return loadBuySellSettleMethod(true);
    }

    @Override
    protected String loadSellAgent(){
        return loadBuySellAgent(false);
    }

    @Override
    protected String loadSellSettleMethod(){
        return loadBuySellSettleMethod(false);
    }

    private TradeTransferRule getXferRule(boolean isBuy) {
        boolean found = false;
        TradeTransferRule xferRule = null;
        Vector<BOException> exc = new Vector<>();
        Vector<TradeTransferRule> xferRules = BOProductHandler.buildTransferRules(this.trade, exc,
                DSConnection.getDefault());
        if (exc.isEmpty()) {

            for (int i = 0; (i < xferRules.size()) && !found; i++) {
                xferRule = xferRules.get(i);
                found = !xferRules.get(i).getTransferType()
                        .contains("FEE")
                        && xferRule.getSettleDate()
                        .equals(this.trade.getMaturityDate())
                        && xferRule.getPayReceive()
                        .equals(isBuy ? "RECEIVE" : "PAY");
            }
        }
        return xferRule;
    }

    private String loadBuySellAgent(boolean isBuy) {
        String rst = "";
        TradeTransferRule xferRule = getXferRule(isBuy);
        if (xferRule!=null) {
            final SettleDeliveryInstruction sdi = BOCache
                    .getSettleDeliveryInstruction(
                            DSConnection.getDefault(),
                            xferRule.getCounterPartySDId());

            if ((sdi != null)) {
                if (isDirectSettleMethod(
                        sdi.getSettlementMethod())) {
                    // CAL_511_
                    final String extRef = this.trade.getCounterParty().getExternalRef();
                    if (!Util.isEmpty(extRef)) {
                        rst = " " + extRef;
                    }
                } else {
                    final LegalEntity agent = BOCache.getLegalEntity(
                            DSConnection.getDefault(),
                            sdi.getAgentId());
                    // CAL_511_
                    if (agent != null) {
                        final String extRef = agent.getExternalRef();
                        if (!Util.isEmpty(extRef)) {
                            rst = " " + extRef;
                        }
                    }
                }
            }
        }

        return rst;
    }

    private String loadBuySellSettleMethod(boolean isBuy){
        String rst = "";
        TradeTransferRule xferRule = getXferRule(isBuy);
        if (xferRule!=null) {
            final SettleDeliveryInstruction sdi = BOCache
                    .getSettleDeliveryInstruction(
                            DSConnection.getDefault(),
                            xferRule.getCounterPartySDId());

            if ((sdi != null)) {
                if (isDirectSettleMethod(
                        sdi.getSettlementMethod())) {
                    rst = "CTA";
                } else if ("SWIFT"
                        .equalsIgnoreCase(sdi.getSettlementMethod())
                        || "CLS".equalsIgnoreCase(
                        sdi.getSettlementMethod()) || "CHAPS".equalsIgnoreCase(
                        sdi.getSettlementMethod())) {
                    rst = "COR";
                } else if ("TARGET2"
                        .equalsIgnoreCase(sdi.getSettlementMethod())) {
                    rst = "TAR";
                }
            }
        }
        return rst;
    }

    private boolean isDirectSettleMethod(final String settleMethod) {
        boolean isDirect = Boolean.FALSE;

        final Vector<String> settleMethods = getDirectSettleMethods();

        if (!Util.isEmpty(settleMethod) && !Util.isEmpty(settleMethods)) {
            for (final String method : settleMethods) {
                if (method.equalsIgnoreCase(settleMethod)) {
                    isDirect = Boolean.TRUE;
                }
            }
        }

        return isDirect;
    }

    private Vector<String> getDirectSettleMethods() {
        final Vector<String> settleMethods = new Vector<String>();

        final Vector domainValues = LocalCache.getDomainValues(
                DSConnection.getDefault(), "settlementMethodDirect");

        if (domainValues != null) {
            for (final Object value : domainValues) {
                settleMethods.add((String) value);
            }
        }

        return settleMethods;
    }

}
