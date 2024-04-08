package calypsox.tk.bo.accounting;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.keyword.sql.TradeKeywordWhereClauseBuilder;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.CorporateActionHandlerUtil;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.WithholdingTaxConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TradeArray;

public class CAAccountingHandler extends com.calypso.tk.bo.accounting.CAAccountingHandler {

    @Override
    public void getDIVIDEND(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        this.getCF_EVENT_TYPE(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer, "DIVIDEND", 0);

    }

	@SuppressWarnings("unchecked")
	public void getCST_VERIFIED(Trade trade, PSEvent event, AccountingEventConfig eventConfig,
			Vector<?> accountingEvents,
			AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		try {
			if (event instanceof PSEventTransfer && "ALL".equals(eventConfig.getEventProperty())) {
				Vector<AccountingEventConfig> vEvCfg = new Vector<AccountingEventConfig>();
				AccountingEventConfig cfg = (AccountingEventConfig) eventConfig.clone();
				cfg.setEventProperty("UNNET");
				vEvCfg.add(cfg);
				Vector<String> excp = new Vector<String>();
				accountingEvents.addAll(generateTransferAccounting((PSEventTransfer) event, vEvCfg, accountingEvents,
						((PSEventTransfer) event).getBoTransfer(), trade, rule, excp, pricingEnv));
				vEvCfg.clear();
				cfg = (AccountingEventConfig) eventConfig.clone();
				cfg.setEventProperty("PAYMENT");
				vEvCfg.add(cfg);
				accountingEvents.addAll(generateTransferAccounting((PSEventTransfer) event, vEvCfg, accountingEvents,
						((PSEventTransfer) event).getBoTransfer(), trade, rule, excp, pricingEnv));
				if (!Util.isEmpty(excp)) {
					for (String err : excp) {
						Log.error(this, err);
					}
				}
			}
		} catch (Exception e) {
			Log.error(CAAccountingHandler.class.getName(), e);
		}
	}
    @Override
    public void getFULL_DIVIDEND(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.FULL_DIVIDEND, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    @Override
    public void getWITHHOLDINGTAX(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.WITHHOLDINGTAX, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    @Override
    public void getNET_WITHHOLDINGTAX(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.NET_WITHHOLDINGTAX, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    @Override
    public void getRECLAIM_TAX(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.RECLAIM_TAX, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    @Override
    public void getRECLAIM_TAX_XD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.RECLAIM_TAX_XD, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    @Override
    public void getRECLAIM_TAX_RD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.RECLAIM_TAX_RD, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);


    }

    @Override
    public void getRECLAIM_PROVISION(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.RECLAIM_PROVISION, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    @Override
    public void getWITHHOLDINGTAX_XD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.WITHHOLDINGTAX_XD, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    @Override
    public void getNET_WITHHOLDINGTAX_XD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.NET_WITHHOLDINGTAX_XD, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    @Override
    public void getFULL_DIVIDEND_XD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.FULL_DIVIDEND_XD, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    @Override
    public void getREALIZED_PL_CA(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.REALIZED_PL_CA, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    @Override
    public void getREALIZED_PL_CA_XD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.REALIZED_PL_CA_XD, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    public void getDIVIDEND_RD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
            this.getCF_EVENT_TYPE(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer, "DIVIDEND", 0);
            Vector accountingEventsfinal = new Vector();
            if(!Util.isEmpty(accountingEvents)){
                Object accEvent = accountingEvents.get(0);
                for(Object eve : accountingEvents){
                    if(eve instanceof BOPosting){
                        if("DIVIDEND_RD".equalsIgnoreCase(((BOPosting) eve).getEventType())){
                            CA ca = CorporateActionHandlerUtil.getCA(trade);
                            if(null!=ca){
                                ((BOPosting) eve).setEffectiveDate(ca.getRecordDate());
                                accountingEventsfinal.add(eve);
                            }
                        }else{
                            accountingEventsfinal.add(eve);
                        }
                    }
                }
                accountingEvents.clear();
                accountingEvents.addAll(accountingEventsfinal);
            }
    }

    public void getFULL_DIVIDEND_RD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.FULL_DIVIDEND_RD, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    public void getWITHHOLDINGTAX_RD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.WITHHOLDINGTAX_RD, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    public void getNET_WITHHOLDINGTAX_RD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.NET_WITHHOLDINGTAX_RD, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    public void getREALIZED_PL_CA_RD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.REALIZED_PL_CA_RD, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    public void getDIV_CLAIM_PL(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.DIV_CLAIM_PL, trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    public void getDIV_CLAIM_PL_RD(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        customGenerateWithholdingTaxAccounting(WhtAccEvent.DIV_CLAIM_PL_RD , trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);

    }

    private void customGenerateWithholdingTaxAccounting(WhtAccEvent accEventType, Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
    CA ca = CorporateActionHandlerUtil.getCA(trade);
    BOPosting accEvent = new BOPosting(eventConfig);
    accEvent.setCurrency(trade.getSettleCurrency());
    accEvent.setProductId(ca.getUnderlyingSecurityId());
    accEvent.setAmount(ca.calcGrossAmount(trade));
    WithholdingTaxConfig wtc = null;
    double wtcRate = 0.0D;
    if (Util.isNonZeroNumber(ca.getWithholdingTaxRate())) {
        wtcRate = ca.getWithholdingTaxRate();
    } else {
        List<CalypsoBindVariable> bindVariables = new ArrayList();
        if (accEventType != WhtAccEvent.REALIZED_PL_CA && accEventType != WhtAccEvent.REALIZED_PL_CA_XD) {
            wtc = BOCache.getWithholdingTaxConfig(DSConnection.getDefault(), (int)trade.getKeywordAsLongId("WithholdingTaxConfigId"));
        } else {
            StringBuffer sb = new StringBuffer(TradeKeywordWhereClauseBuilder.eq("trade.trade_id", "CAReference", Integer.toString(ca.getId()), bindVariables, new TradeKeywordWhereClauseBuilder.Option[0]));
            TradeArray trades = null;

            try {
                trades = DSConnection.getDefault().getRemoteTrade().getTrades("trade", sb.toString(), "", bindVariables);
            } catch (RemoteException var20) {
                Log.error("CAAccountingHandler", var20);
            }

            if (trades != null && !trades.isEmpty()) {
                Iterator iter = trades.iterator();

                while(iter.hasNext()) {
                    Trade t = (Trade)iter.next();
                    wtc = BOCache.getWithholdingTaxConfig(DSConnection.getDefault(), (int)t.getKeywordAsLongId("WithholdingTaxConfigId"));
                    if (wtc != null) {
                        break;
                    }
                }
            }
        }

        if (wtc == null) {
            if (WhtAccEvent.FULL_DIVIDEND.equals(accEventType)
                    || WhtAccEvent.FULL_DIVIDEND_XD.equals(accEventType)
                    || WhtAccEvent.FULL_DIVIDEND_RD.equals(accEventType)
                    || WhtAccEvent.DIV_CLAIM_PL.equals(accEventType)
                    || WhtAccEvent.DIV_CLAIM_PL_RD.equals(accEventType)) {

                if (WhtAccEvent.FULL_DIVIDEND.equals(accEventType)) {
                    accEvent.setEffectiveDate(ca.getValueDate());
                } else if (WhtAccEvent.FULL_DIVIDEND_XD.equals(accEventType)) {
                    accEvent.setEffectiveDate(ca.getExDate());
                }else if (WhtAccEvent.FULL_DIVIDEND_RD.equals(accEventType)){
                    accEvent.setEffectiveDate(ca.getRecordDate());
                }else if (WhtAccEvent.DIV_CLAIM_PL.equals(accEventType)){
                    accEvent.setAmount(getDivClaim(trade,ca));
                    accEvent.setEffectiveDate(ca.getValueDate());
                }else if (WhtAccEvent.DIV_CLAIM_PL_RD.equals(accEventType)){
                    accEvent.setAmount(getDivClaim(trade,ca));
                    accEvent.setEffectiveDate(ca.getRecordDate());
                }

                if ((accEventType == WhtAccEvent.DIV_CLAIM_PL
                        || accEventType == WhtAccEvent.DIV_CLAIM_PL_RD) && accEvent.getAmount() == 0.0D) {
                    return;
                }

                accountingEvents.add(accEvent);
            }

            return;
        }

        wtcRate = wtc.getWHTRate();
    }

    if (accEventType == WhtAccEvent.RECLAIM_PROVISION) {
        if (wtc.getTaxAuthorityLe() == null) {
            return;
        }

        LegalEntityAttribute provisionable = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, wtc.getTaxAuthorityLe().getId(), "TaxAuthority", "PROVISIONABLE");
        if (provisionable == null || provisionable != null && !Util.isTrue(provisionable.getAttributeValue())) {
            return;
        }
    }

    if ((accEventType == WhtAccEvent.RECLAIM_TAX || accEventType == WhtAccEvent.RECLAIM_TAX_XD || accEventType == WhtAccEvent.RECLAIM_TAX_RD || accEventType == WhtAccEvent.RECLAIM_PROVISION
            || accEventType == WhtAccEvent.NET_WITHHOLDINGTAX_XD
            || accEventType == WhtAccEvent.REALIZED_PL_CA || accEventType == WhtAccEvent.REALIZED_PL_CA_XD) && (wtc == null || wtc != null && !wtc.hasReclaimRate())) {
        return;
    }

    double divReq = trade.getKeywordAsDouble("ContractDivRate");
    JDate effectiveDate = ca.getValueDate();
    double amount = accEvent.getAmount();
    switch(accEventType) {
        case FULL_DIVIDEND_XD:
            effectiveDate = ca.getExDate();
            break;
        case RECLAIM_TAX:
            amount *= wtc.getReclaimRate();
            amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
            break;
        case RECLAIM_TAX_XD:
            amount *= wtc.getReclaimRate();
            amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
            effectiveDate = ca.getExDate();
            break;
        case RECLAIM_TAX_RD:
            amount *= wtc.getReclaimRate();
            amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
            effectiveDate = ca.getRecordDate();
            break;
        case RECLAIM_PROVISION:
            amount = trade.getQuantity() * (ca.getAmount() - ca.getTaxFreeAmount()) * wtc.getReclaimRate() * -1.0D;
            amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
            effectiveDate = ca.getExDate();
            List<String> taxReclaimDate = LocalCache.getDomainValues(DSConnection.getDefault(), "TaxReclaimDate");
            if (!Util.isEmpty(taxReclaimDate)) {
                if ("RecordDate".equals(taxReclaimDate.get(0))) {
                    effectiveDate = ca.getRecordDate();
                } else if ("ValueDate".equals(taxReclaimDate.get(0))) {
                    effectiveDate = ca.getValueDate();
                }
            }
            break;
        case WITHHOLDINGTAX:
            amount *= wtcRate;
            amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
            break;
        case WITHHOLDINGTAX_RD:
            amount *= wtcRate;
            amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
            break;
        case WITHHOLDINGTAX_XD:
            amount *= wtcRate;
            effectiveDate = ca.getExDate();
            amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
            break;
        case NET_WITHHOLDINGTAX:
            amount *= wtcRate - wtc.getReclaimRate();
            amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
            break;
        case NET_WITHHOLDINGTAX_RD:
            amount *= wtcRate - wtc.getReclaimRate();
            amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
            break;
        case NET_WITHHOLDINGTAX_XD:
            amount *= wtcRate - wtc.getReclaimRate();
            amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
            effectiveDate = ca.getExDate();
            break;
        case REALIZED_PL_CA:
            amount -= WithholdingTaxConfig.roundWithholdingTaxAmount(amount * (1.0D - (wtcRate - wtc.getReclaimRate())) / divReq, wtc);
            break;
        case REALIZED_PL_CA_RD:
            amount -= WithholdingTaxConfig.roundWithholdingTaxAmount(amount * (1.0D - (wtcRate - wtc.getReclaimRate())) / divReq, wtc);
            break;
        case REALIZED_PL_CA_XD:
            amount -= WithholdingTaxConfig.roundWithholdingTaxAmount(amount * (1.0D - (wtcRate - wtc.getReclaimRate())) / divReq, wtc);
            effectiveDate = ca.getExDate();
            break;
    }

    if ((accEventType == WhtAccEvent.REALIZED_PL_CA
            || accEventType == WhtAccEvent.REALIZED_PL_CA_XD
            || accEventType == WhtAccEvent.DIV_CLAIM_PL
            || accEventType == WhtAccEvent.DIV_CLAIM_PL_RD) && amount == 0.0D) {
        return;
    }

    accEvent.setAmount(amount);
    if(accEventType == WhtAccEvent.DIVIDEND_RD
            || accEventType == WhtAccEvent.WITHHOLDINGTAX_RD
            || accEventType == WhtAccEvent.NET_WITHHOLDINGTAX_RD
            || accEventType == WhtAccEvent.REALIZED_PL_CA_RD
            || accEventType == WhtAccEvent.FULL_DIVIDEND_RD
            || accEventType == WhtAccEvent.DIV_CLAIM_PL_RD){
        effectiveDate = ca.getRecordDate();
    }

    accEvent.setEffectiveDate(effectiveDate);

    accountingEvents.add(accEvent);
}

    private static enum WhtAccEvent {
        RECLAIM_TAX,
        RECLAIM_TAX_XD,
        RECLAIM_TAX_RD,
        RECLAIM_PROVISION,
        WITHHOLDINGTAX,
        WITHHOLDINGTAX_XD,
        NET_WITHHOLDINGTAX,
        NET_WITHHOLDINGTAX_XD,
        FULL_DIVIDEND,
        FULL_DIVIDEND_XD,
        REALIZED_PL_CA,
        REALIZED_PL_CA_XD,
        DIVIDEND_RD,
        FULL_DIVIDEND_RD,
        WITHHOLDINGTAX_RD,
        NET_WITHHOLDINGTAX_RD,
        REALIZED_PL_CA_RD,
        DIV_CLAIM_PL,
        DIV_CLAIM_PL_RD;

        private WhtAccEvent() {
        }
    }

    private double getDivClaim(Trade trade, CA ca){
        double amount = 0.0;
        if(null!=trade && null!=ca){
            double ca_unit_amount = ca.getAmount();
            double quantity = trade.getQuantity();
            double settlementAmount = ca.calcSettlementAmount(trade);
            amount = settlementAmount - (quantity * ca_unit_amount);

        }
        return amount;
    }

    public boolean isPay(Trade trade) {
        if (trade.getProduct() != null) {
            if (trade.getQuantity() < 0.0D){
                //"Pay"
                return true;
            }
            if (trade.getQuantity() > 0.0D){
                //"Receive"
                return false;
            }
        }
        return true;
    }


}
