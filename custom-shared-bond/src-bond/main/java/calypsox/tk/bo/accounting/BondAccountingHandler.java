package calypsox.tk.bo.accounting;

import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;

import java.util.TimeZone;
import java.util.Vector;

/**
 * Created by x379335.
 */
public class BondAccountingHandler extends com.calypso.tk.bo.accounting.BondAccountingHandler{

	@Override
	public void getCOT(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		BOPosting accEvent = new BOPosting(eventConfig);

		if (isBondForward(trade)) {
			accEvent.setAmount(-trade.getQuantity() * trade.getProduct().getPrincipal(trade.getSettleDate()));
		} else {
			accEvent.setAmount(this.getSettlementAmount(trade, pricingEnv));
		}
		accEvent.setEffectiveDate(this.getActiveDate(trade));
		accEvent.setCurrency(trade.getProduct().getCurrency());
		accountingEvents.addElement(accEvent);
	}

	@Override
	public void getCOT_REV(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		BOPosting accEvent = new BOPosting(eventConfig);
		if (isBondForwardCash(trade)) {
			accEvent.setAmount(-trade.getQuantity() * trade.getProduct().getPrincipal(trade.getSettleDate()));
		} else {
			accEvent.setAmount(this.getSettlementAmount(trade, pricingEnv));
		}
		JDate termDate = trade.getTerminationDate();
		if (termDate != null) {
			accEvent.setEffectiveDate(termDate);
		} else if (trade.getAlternateDate() != null) {
			accEvent.setEffectiveDate(trade.getAlternateDate());
		} else {
			JDate cotRevDate = trade.getKeywordAsJDate("COT_REV_DATE");
			if (cotRevDate != null) {
				accEvent.setEffectiveDate(cotRevDate);
			} else {
				accEvent.setEffectiveDate(trade.getSettleDate());
			}
		}
		accEvent.setCurrency(trade.getProduct().getCurrency());
		accountingEvents.addElement(accEvent);
	}

	public void getBOOKING(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		BOPosting accEvent = new BOPosting(eventConfig);
		accEvent.setAmount(this.getSettlementAmount(trade, pricingEnv));
		accEvent.setBookingDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
		accEvent.setCurrency(trade.getTradeCurrency());
		accEvent.setEffectiveDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
		accountingEvents.addElement(accEvent);
	}

	public void getMATURITY(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		BOPosting accEvent = new BOPosting(eventConfig);
		accEvent.setBookingDate(trade.getSettleDate());
		accEvent.setAmount(this.getSettlementAmount(trade, pricingEnv));
		accEvent.setCurrency(trade.getTradeCurrency());
		accEvent.setEffectiveDate(trade.getSettleDate());
		accountingEvents.addElement(accEvent);

	}

	public void getALLOCATED(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer){
		CreAllocatableAccHandler.getALLOCATED(trade,event,eventConfig,accountingEvents,rule,pricingEnv,pricer);
	}

	private boolean isBondForward(Trade trade){
		return "true".equalsIgnoreCase(trade.getKeywordValue("BondForward"));
	}

	private boolean isBondForwardCash(Trade trade){
		return isBondForward(trade) && "Cash".equals(trade.getKeywordValue("BondForwardType"));
	}
}
