package calypsox.tk.bo.accounting;

import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.core.*;
import com.calypso.tk.event.LiquidationEvent;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventPositionValuation;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.mo.*;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.PLPositionArray;
import com.calypso.tk.util.TradeOpenQuantityArray;

/**
 * Created by x379335 on 28/07/2020.
 */
public class EquityAccountingHandler extends com.calypso.tk.bo.accounting.EquityAccountingHandler {
    public void getUNREALIZED_PL(Trade trade, PSEvent event, AccountingEventConfig eventConfig,
                                 Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
    	if (event instanceof PSEventPositionValuation) {
    		PSEventPositionValuation eventPos = (PSEventPositionValuation)event;
    		
    		if (eventPos.getPositionAggregationId() != 0 || eventPos.getAmount() == 0.0) {
    			return;
    		}
    		
    		try {
				PLPosition plPos = DSConnection.getDefault().getRemoteTrade().getPLPosition(eventPos.getPositionLongId());
				
				try {
					Vector v = new Vector();
					v.addElement(eventPos.getValuationDate());
					PLPositionArray allPLPositions = new PLPositionArray();
					allPLPositions.add(plPos);
					Map<JDatetime, PLPositionArray> myMap = PLPositionUtil.buildPositionByDates(allPLPositions, v, true, false, false);
					allPLPositions = myMap.get(eventPos.getValuationDate());
					plPos = allPLPositions.get(0);
				} catch (PositionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				TradeOpenQuantityArray toqA = plPos.getOpenPositions();
				for (int i = toqA.size() - 1; i >= 0; i--) {
					TradeOpenQuantity toq = toqA.get(i);
					if (toq.getSettleDate().after(eventPos.getValuationDate().getJDate(TimeZone.getDefault()))) {
						toqA.remove(i);
					}
				}
				
				double unrealized = plPos.getUnrealized(pricingEnv, eventPos.getValuationDate());
				
		    	BOPosting accEvent = new BOPosting(eventConfig);
		    	accEvent.setEffectiveDate(eventPos.getValuationDate().getJDate(TimeZone.getDefault()));
		    	accEvent.setAmount(unrealized);
		    	accEvent.setCurrency(eventPos.getCurrency());
		    	accountingEvents.add(accEvent);
			} catch (CalypsoServiceException e) {
				Log.error(this, "Error retrieving PL Position : " + e.toString());
			} catch (PricerException e) {
				Log.error(this, "Error retrieving Unrealized from PL Position : " + e.toString());
			}
    	}
    }

    @Override
	public void getNOM_FULL_REV(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		Vector<String> rvCreNomFullRevActivate = LocalCache.getDomainValues(DSConnection.getDefault(), "RVCreNomFullRevActivate");
		if(!Util.isEmpty(rvCreNomFullRevActivate) && Boolean.parseBoolean(rvCreNomFullRevActivate.get(0))){
			BOPosting accEvent = new BOPosting(eventConfig);
			LiquidatedPosition position = null;
			if (event instanceof LiquidationEvent) {
				position = ((LiquidationEvent)event).getPosition();
				double amount = 0.0D;
				if (this.isLiquidatedPosition(position)) {
					amount = position.getFirstPrice() * position.getQuantity();
					amount = amount / trade.getProduct().getPrincipal(trade.getSettleDate()) * trade.getProduct().getPrincipal(position.getRealizedDate());
				} else {
					if (!position.isAmortization()) {
						return;
					}
					amount = position.getCAAmount() * trade.getProduct().getPrincipal(position.getRealizedDate());
				}

				accEvent.setAmount(amount);
				accEvent.setEffectiveDate(position.getRealizedDate());
				accEvent.setCurrency(trade.getProduct().getCurrency());
				accountingEvents.addElement(accEvent);
			}
		}else {
			super.getNOM_FULL_REV(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
		}
	}

    public String getUNREALIZED_PL_Description() {
        String descr = "Unrealized_PL";
        return descr;
    }


    @Override
	public void getCOT_REV(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		BOPosting accEvent = new BOPosting(eventConfig);
		accEvent.setAmount(this.getSettlementAmount(trade));
		if (trade.getAlternateDate()!=null && (!trade.getSettleDate().equals(trade.getAlternateDate()))) {
			accEvent.setEffectiveDate(trade.getSettleDate());
		}
		else{
			accEvent.setEffectiveDate(trade.getAlternateDate());
		}
		accEvent.setCurrency(trade.getProduct().getCurrency());
		accountingEvents.addElement(accEvent);
	}


}
