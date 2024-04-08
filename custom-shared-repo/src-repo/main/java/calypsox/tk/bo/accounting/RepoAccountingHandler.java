package calypsox.tk.bo.accounting;

import java.util.Vector;

import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;

public class RepoAccountingHandler extends com.calypso.tk.bo.accounting.RepoAccountingHandler {

	public void getRL_PRINCIPAL(Trade trade, PSEvent event, AccountingEventConfig eventConfig,
			Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		if (!(event instanceof PSEventTransfer)) {
			return;
		}
		PSEventTransfer eventXFer = (PSEventTransfer)event;
		
		getPRINCIPAL(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
		
		for (int i = accountingEvents.size() - 1; i >= 0; i--) {
			BOPosting posting = (BOPosting)accountingEvents.get(i);
			
			BOTransfer xfer = eventXFer.getBoTransfer();
			if (!posting.getEffectiveDate().equals(xfer.getValueDate())) {
				accountingEvents.remove(i);
				continue;
			}
			else {
				posting.setTransferLongId(eventXFer.getBoTransfer().getLongId());
			}
			
			if (posting.getAmount() == 0.0d) {
				accountingEvents.remove(i);
				continue;
			}
		}
	}
	

	public String getRL_PRINCIPAL_Description() {
		String descr = "RL_PRINCIPAL";
		return descr;
	} 

	public void getRL_INTEREST(Trade trade, PSEvent event, AccountingEventConfig eventConfig,
			Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		if (!(event instanceof PSEventTransfer)) {
			return;
		}
		PSEventTransfer eventXFer = (PSEventTransfer)event;
		BOTransfer xfer = eventXFer.getBoTransfer();
		 

		JDate now = JDate.getNow();
		CashFlowSet cfs = null;
		try {
			cfs = trade.getProduct().getFlows(trade, now, true, -1, true);
			if (cfs == null) {        return;      }
			trade.getProduct().calculate(cfs, pricingEnv, now);
		}   
		catch (Exception e) {
			Log.error(this, e);    
		}
		
		for (int i = 0; i < cfs.size(); i++)    {
			CashFlow cf = (CashFlow)cfs.elementAt(i);
			if (cf.isInterest() && cf.getDate().equals(xfer.getValueDate())) {
				if (cf.getAmount() == 0.0d) {
					continue;
				}
				BOPosting accEvent = null;
				accEvent = new BOPosting(eventConfig);
				                   
				accEvent.setCurrency(cf.getCurrency());
				accEvent.setProductId(trade.getProductId());
				
				accEvent.setEffectiveDate(cf.getDate());
				accEvent.setAmount(cf.getAmount());
				
				accEvent.setTransferLongId(xfer.getLongId());
				
				accountingEvents.addElement(accEvent);
				
				break; 
			}
		}
	}

	public String getRL_INTEREST_Description() {
		String descr = "RL_INTEREST";
		return descr;
	}


	@Override
	public void getNOMINAL(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents,
			AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		super.getNOMINAL(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
		
		Repo repo = (Repo)trade.getProduct();
		Product product = repo.getSecurity();
		
		for (int i = 0; i < accountingEvents.size(); i++) {
			BOPosting currentPosting = (BOPosting)accountingEvents.get(i);
			if (currentPosting.getEventType().equals("NOMINAL")) {
				setFactoredAmount(currentPosting, product);
			}
		}
	}

	@Override
	public void getNOMINAL_REV(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents,
			AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		super.getNOMINAL_REV(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
		
		Repo repo = (Repo)trade.getProduct();
		Product product = repo.getSecurity();
		
		for (int i = 0; i < accountingEvents.size(); i++) {
			BOPosting currentPosting = (BOPosting)accountingEvents.get(i);
			if (currentPosting.getEventType().equals("NOMINAL_REV")) {
				setFactoredAmount(currentPosting, product);
			}
		}
	}

	
	private void setFactoredAmount(BOPosting posting, Product product) {
		double factor = ((Bond)product).getCurrentFactor(posting.getEffectiveDate());
		posting.setAmount(posting.getAmount() * factor);
	}
}
