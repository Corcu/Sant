package calypsox.tk.bo.accounting;

import java.util.Vector;

import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CashFlowGeneratorBased;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.product.PerformanceSwappableLeg;
import com.calypso.tk.product.SwapLeg;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.product.flow.CashFlowPriceChange;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;

import calypsox.util.collateral.CollateralUtilities;

@SuppressWarnings("rawtypes")
public class PerformanceSwapAccountingHandler extends com.calypso.tk.bo.accounting.PerformanceSwapAccountingHandler {

    public void getACCRUAL_PAYLEG(Trade trade, PSEvent event, AccountingEventConfig eventConfig,
                                  Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        this.getACCRUAL(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
    }

    public String getACCRUAL_PAYLEG_Description() {
        return this.getACCRUAL_Description();
    }

    public void getACCRUAL_RECLEG(Trade trade, PSEvent event, AccountingEventConfig eventConfig,
                                  Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        this.getACCRUAL(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
    }

    public String getACCRUAL_RECLEG_Description() {
        return this.getACCRUAL_Description();
    }

    public void getMTM_NET(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents,
                           AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        this.getMTM_FULL(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
    }

    public void getCASH_BASE(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer){
        this.getMTM_FULL(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
    }

    public void getMTM_FULL_BASE(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer){
        this.getMTM_FULL(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
    }

    public String getMTM_FULL_BASE_Description() {
        String descr = "Purpose: Book Pricing Measures.\n\n";
        descr = descr + "Amount Booked: Pricing Measure Amount (Valuation Ccy).\n\n";
        descr = descr + "Effective Date: Valuation Date\n";
        descr = descr + "Trigger Events: Trade Valuation,Canceled and Matured Trade.\n\n";
        descr = descr + "Special Features: One Accounting Event is created for each Pricing Measure.\n";
        descr = descr + "If the Trade is Canceled or Matured, the Accounting Event\n";
        descr = descr + "is reversed.\n";
        descr = descr + "MTM_FULL_LAGO_CBASE is provided in order to be able to book different\n";
        descr = descr + "Pricing Measures than the one set for MTM_FULL_LAGO_CBASE\n\n";
        descr = descr + "Booking Type:  Reversal\n";
        descr = descr + "Retro-Activity: ClosingPeriod\n";
        descr = descr + "Event Class: INVENTORY\n";
        return descr;
    }

    public void getMTM_FULL_LAGO(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer){
        this.getMTM_FULL(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
    }

    public String getMTM_FULL_LAGO_Description() {
        String descr = "Purpose: Book Pricing Measures.\n\n";
        descr = descr + "Amount Booked: Pricing Measure Amount (Valuation Ccy).\n\n";
        descr = descr + "Effective Date: Valuation Date\n";
        descr = descr + "Trigger Events: Trade Valuation,Canceled and Matured Trade.\n\n";
        descr = descr + "Special Features: One Accounting Event is created for each Pricing Measure.\n";
        descr = descr + "If the Trade is Canceled or Matured, the Accounting Event\n";
        descr = descr + "is reversed.\n";
        descr = descr + "MTM_FULL_LAGO_CBASE is provided in order to be able to book different\n";
        descr = descr + "Pricing Measures than the one set for MTM_FULL_LAGO_CBASE\n\n";
        descr = descr + "Booking Type:  Reversal\n";
        descr = descr + "Retro-Activity: ClosingPeriod\n";
        descr = descr + "Event Class: INVENTORY\n";
        return descr;
    }

    public String getCASH_BASE_Description() {
        String descr = "Purpose: Book Pricing Measures.\n\n";
        descr = descr + "Amount Booked: Pricing Measure Amount (Valuation Ccy).\n\n";
        descr = descr + "Effective Date: Valuation Date\n";
        descr = descr + "Trigger Events: Trade Valuation,Canceled and Matured Trade.\n\n";
        descr = descr + "Special Features: One Accounting Event is created for each Pricing Measure.\n";
        descr = descr + "If the Trade is Canceled or Matured, the Accounting Event\n";
        descr = descr + "is reversed.\n";
        descr = descr + "CASH_BASE is provided in order to be able to book different\n";
        descr = descr + "Pricing Measures than the one set for MTM_FULL\n\n";
        descr = descr + "Booking Type:  Reversal\n";
        descr = descr + "Retro-Activity: ClosingPeriod\n";
        descr = descr + "Event Class: INVENTORY\n";
        return descr;
    }

    public String getMTM_NET_Description() {
        String descr = "Purpose: Book Pricing Measures.\n\n";
        descr = descr + "Amount Booked: Pricing Measure Amount (Valuation Ccy).\n\n";
        descr = descr + "Effective Date: Valuation Date\n";
        descr = descr + "Trigger Events: Trade Valuation,Canceled and Matured Trade.\n\n";
        descr = descr + "Special Features: One Accounting Event is created for each Pricing Measure.\n";
        descr = descr + "If the Trade is Canceled or Matured, the Accounting Event\n";
        descr = descr + "is reversed.\n";
        descr = descr + "MTM_NET is provided in order to be able to book different\n";
        descr = descr + "Pricing Measures than the one set for MTM_FULL\n\n";
        descr = descr + "Booking Type:  Reversal\n";
        descr = descr + "Retro-Activity: ClosingPeriod\n";
        descr = descr + "Event Class: INVENTORY\n";
        return descr;
    }
    
    public void getTransferPostingInBaseCcy(PSEventTransfer eventTransfer, AccountingEventConfig eventConfig, Vector<BOPosting> accountingEvents, PricingEnv pricingEnv) {

    	BOTransfer transfer = eventTransfer.getBoTransfer();
    	
    	JDate effectiveDate = transfer.getValueDate();
    	
    	String transferCcy = transfer.getSettlementCurrency();
    	String baseCcy = pricingEnv.getBaseCurrency();
    	
        BOPosting accEvent = new BOPosting(eventConfig);
        accEvent.setCurrency(baseCcy);
        accEvent.setProductId(transfer.getProductId());
        accEvent.setEffectiveDate(effectiveDate);
        accEvent.setTransferId(transfer.getLongId());
        accEvent.setDescription(transfer.getTransferType());
        accEvent.setBookingDate(transfer.getBookingDate());
        
        accEvent.setDebitAccountId(transfer.getGLAccountNumber());
        accEvent.setCreditAccountId(transfer.getGLAccountNumber());
        
        if(Status.isCanceled(eventTransfer.getStatus())) {
        	accEvent.setAmount(0.0D);
        }
        else
        {
        	try {
        		Double amount = getQuantityAmount(eventTransfer, transfer);
        		Double convertedAmount = CollateralUtilities.convertCurrency(transferCcy, amount, baseCcy, effectiveDate, pricingEnv);
				accEvent.setAmount(convertedAmount);

			} catch (MarketDataException e) {
				Log.error(this, e);
			}
        }

		if(accEvent.getAmount()!=0.0d)
			accountingEvents.add(accEvent);
    }
    
    public void getTERMINATION_FEE_BASE(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector<BOPosting> accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer){
        if(event instanceof PSEventTransfer)
        	getTransferPostingInBaseCcy((PSEventTransfer)event, eventConfig, accountingEvents, pricingEnv);
    }
    
    public String getTERMINATION_FEE_BASE_Description() {
        String descr = "Termination Fee in base currency";
        return descr;
    }
    
    public void getINTEREST_BASE(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector<BOPosting> accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer){
        if(event instanceof PSEventTransfer)
        	getTransferPostingInBaseCcy((PSEventTransfer)event, eventConfig, accountingEvents, pricingEnv);
    }
     
    public String getINTEREST_BASE_Description() {
        String descr = "Interest in base currency";
        return descr;
    }
    
    
    // HD 193673
    @SuppressWarnings("unchecked")
    public void getCOT_REV(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
   
    	  Vector accEvents = new Vector();
    	  AccountingEventConfig evCf = new AccountingEventConfig();
    	  evCf.setEventType("NOM_CHG");
    	  this.getNOMForAmotization(trade, event, evCf, accEvents, rule, pricingEnv, pricer);
    	  evCf.setEventType("NOM_MAT");
    	  this.getNOMForAmotization(trade, event, evCf, accEvents, rule, pricingEnv, pricer);
    	  for(int i = 0; i < accEvents.size(); ++i) {
    	     BOPosting nomTd = (BOPosting)accEvents.get(i);
    	     nomTd.setEventType(eventConfig.getEventType());
    	     nomTd.setEventConfigId(eventConfig.getId());
    	     nomTd.setAmount(-nomTd.getAmount());
    	   }
    	   accountingEvents.addAll(accEvents);
    }
    
    // HD 193673
    @SuppressWarnings("unchecked")
	public void getCOT(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {

    	 Vector accEvents = new Vector();
    	 AccountingEventConfig evCf = new AccountingEventConfig();
    	 evCf.setEventType("NOM_TD");
    	 this.getNOMForAmotization(trade, event, evCf, accEvents, rule, pricingEnv, pricer);
    	 for(int i = 0; i < accEvents.size(); ++i) {
    	     BOPosting nomTd = (BOPosting)accEvents.get(i);
    	     nomTd.setEventType(eventConfig.getEventType());
    	     nomTd.setEventConfigId(eventConfig.getId());
    	     nomTd.setEffectiveDate(JDate.getNow());
    	 }
    	 accountingEvents.addAll(accEvents);
    }
    
    @SuppressWarnings("unchecked")
	public void getNOMForAmotization(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents,
			AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
		PerformanceSwap swap = (PerformanceSwap) trade.getProduct();
		
		try {
			CashFlowSet cfs = trade.getProduct().getFlows(trade, JDate.getNow(), true, 0, true, pricingEnv);
			trade.getProduct().calculateAll(cfs, pricingEnv, JDate.getNow());
			if (cfs == null) {
				return;
			}

			if (trade.isTerminated()) {
				cfs = setUpTerminationFlows(trade, pricingEnv);
				if (cfs == null) {
					return;
				}
			}
		} catch (Exception var17) {
			Log.error(this, var17);
		}
	
		
		double pos = trade.getQuantity();
		if (pos == 0.0D) 
			pos = trade.getAllocatedQuantity();  
		
		boolean isBuy = (pos >= 0.0D ? true : false);
		PerformanceSwappableLeg leg = null;
		CashFlowSet mySet = null;
		
		// only receive leg generate postings
		if(isBuy) {
			leg = swap.getPrimaryLeg();
			mySet = swap.getPrimaryLegFlows();
		}
		else {
			leg = swap.getSecondaryLeg();
			mySet = swap.getSecondaryLegFlows();
		}
		
		
		CashFlowSet interestCashFlow = getAllCashFlowType(mySet, CashFlow.INTEREST);
		CashFlowSet priceChangeCashFlow = getAllCashFlowType(mySet, CashFlow.PRICE_CHANGE);
		
		// Take the cashflow type witch has the more number of flows.
		if(interestCashFlow.getFlows().length<priceChangeCashFlow.getFlows().length)
			mySet=priceChangeCashFlow;
		else
			mySet=interestCashFlow;
		
		Vector vv = generateNominalEventsForAmortization(trade, pricingEnv, pricer, mySet, leg, eventConfig);
		if (vv != null) {
			String ccy = getCurrency(leg);
			for (int i = 0; i < vv.size(); ++i) {
				BOPosting boPosting = (BOPosting) vv.get(i);
				boPosting.setCurrency(ccy);
			}
			accountingEvents.addAll(vv);
		}

	}
    
    /**
     * filter cash flow based on cashflow type
     * @param cfs
     * @param cashFlowType
     * @return
     */
    protected CashFlowSet getAllCashFlowType(CashFlowSet cfs, String cashFlowType) {
    	CashFlowSet result = new CashFlowSet();
    	for(int i=0; i<cfs.size();i++) {
    		CashFlow cf = cfs.get(i);
    		if(cf.getType().equals(cashFlowType))
    			result.add(cf);
    	}
    	return result;
    	
    }

	private static String getCurrency(PerformanceSwappableLeg leg) {
		return leg instanceof PerformanceSwapLeg ? ((PerformanceSwapLeg) leg).getBaseCurrency()
				: ((SwapLeg) leg).getPrincipalCurrency();
	}

	private Object getParameter(CashFlowGeneratorBased product, String param) {
		return product.getParameter(param);
	}

	public Vector<BOPosting> generateNominalEventsForAmortization(Trade trade, PricingEnv pricingEnv, Pricer pricer,
			CashFlowSet cfs, CashFlowGeneratorBased product, AccountingEventConfig eventConfig) {
		JDate legStartDate = (JDate) getParameter(product, "FirstFullCoupon");
		if (legStartDate == null) {
			legStartDate = (JDate) getParameter(product, "StartDt");
		}
		JDate matDate = (JDate) getParameter(product, "MaturityDt");
		return generateNominalEventsForAmortization(trade, pricingEnv, pricer, cfs, eventConfig, legStartDate, matDate);
	}

	public Vector<BOPosting> generateNominalEventsForAmortization(Trade trade, PricingEnv pricingEnv, Pricer pricer,
			CashFlowSet cfs, AccountingEventConfig eventConfig, JDate legStartDate, JDate matDate) {

		Vector<BOPosting> nomEvents = new Vector<BOPosting>();
		double currentNotional = 0.0d;
		JDate transferDate = trade.getTransferDate();
		CashFlow lastCashFlow = null;
		
		JDate terminationDate = null;
		
		if (trade.getTerminationDate() != null)
			terminationDate=getInactiveTradeDate(trade);
		

		if (cfs != null) {
			for (int i = 0; i < cfs.count(); i++) {
				CashFlow cf = (CashFlow) cfs.elementAt(i);
				if ((cf instanceof CashFlowInterest)||(cf instanceof CashFlowPriceChange)) {
					BOPosting accEvent = new BOPosting(eventConfig);
					double newNotional = 0.0d;
					lastCashFlow = cf;
					if (cf instanceof CashFlowInterest) {
						CashFlowInterest cfi = (CashFlowInterest) cf;
						newNotional = cfi.getNotional();
					}
					if (cf instanceof CashFlowPriceChange) {
						CashFlowPriceChange cfi = (CashFlowPriceChange) cf;
						newNotional = cfi.getNotional();
					}
					
					double notionalDiff = newNotional - currentNotional;
	
					if (notionalDiff != 0.0d) {
						
						accEvent.setAmount(notionalDiff);
						accEvent.setSubId(cf.getSubId());
						accEvent.setEffectiveDate(cf.getStartDate());
						if (cf.getStartDate().before(legStartDate)) {
							accEvent.setEffectiveDate(legStartDate);
						}
						if (transferDate != null && accEvent.getEffectiveDate().before(transferDate)) {
							accEvent.setEffectiveDate(transferDate);
						}
						accEvent.setCurrency(cf.getCurrency());	
					
					
						if(terminationDate==null || terminationDate.after(accEvent.getEffectiveDate())) {
							currentNotional = newNotional;
							nomEvents.addElement(accEvent);
						}
					}

				}
			}
		}
		

		if (eventConfig.getEventType().equals("NOM_TD")) {
			Vector<BOPosting> nomTd = new Vector<BOPosting>();
			if (nomEvents.size() > 0) {
				BOPosting posting = nomEvents.get(0);
				posting.setAmount(posting.getAmount());
				nomTd.add(posting);
				return nomTd;
			}
		}

		if (eventConfig.getEventType().equals("NOM_CHG")) {
			if (nomEvents.size() > 1) {
				nomEvents.remove(0);
				return nomEvents;
			}
		}

		if (eventConfig.getEventType().equals("NOM_MAT")) {
			Vector<BOPosting> nomMat = new Vector<BOPosting>();
			if (lastCashFlow != null) {
				BOPosting accEvent = new BOPosting(eventConfig);
				accEvent.setAmount(-currentNotional);
				accEvent.setSubId(lastCashFlow.getSubId());
				if (terminationDate != null)
					accEvent.setEffectiveDate(terminationDate);
				else
					accEvent.setEffectiveDate(lastCashFlow.getEndDate());
				accEvent.setCurrency(lastCashFlow.getCurrency());
				nomMat.addElement(accEvent);
				return nomMat;
			}
		}

		return new Vector<BOPosting>();

	}
    
}
