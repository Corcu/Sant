package calypsox.tk.bo.accounting;

import java.util.Vector;

import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.accounting.AccountingHandler;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventValuation;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;

public class SecLendingAccountingHandler extends com.calypso.tk.bo.accounting.SecLendingAccountingHandler {

    @Override
    @SuppressWarnings("unchecked")
    public void getCOT(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents,
                       AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        SecFinance secFinance = (SecFinance) trade.getProduct();
        Cash cash = secFinance.getCash();
        if (cash != null) {
            super.getCOT(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
        } else {
            BOPosting accEvent = new BOPosting(eventConfig);
            if (secFinance instanceof SecLending) {
                SecLending secLending = (SecLending) secFinance;
                accEvent.setAmount(secLending.getInitialMarginValue());
                accEvent.setEffectiveDate(this.getActiveDate(trade));
                accEvent.setProductId(trade.getProductId());
                accEvent.setCurrency(secLending.getMarginCallCurrency());
                accountingEvents.addElement(accEvent);
            }
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public void getCOT_REV(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents,
                           AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        SecFinance secFinance = (SecFinance) trade.getProduct();
        Cash cash = secFinance.getCash();
        if (cash != null) {
            super.getCOT_REV(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
        } else {
            BOPosting accEvent = new BOPosting(eventConfig);
            if (secFinance instanceof SecLending) {
                SecLending secLending = (SecLending) secFinance;
                accEvent.setAmount(secLending.getInitialMarginValue());
                accEvent.setEffectiveDate(trade.getSettleDate());
                JDate termDate = trade.getTerminationDate();
                if (termDate != null && termDate.before(accEvent.getEffectiveDate())) {
                    accEvent.setEffectiveDate(termDate);
                }

                JDate transDate = trade.getTransferDate();
                if (transDate != null && transDate.before(accEvent.getEffectiveDate())) {
                    accEvent.setEffectiveDate(transDate);
                }

                accEvent.setProductId(trade.getProductId());
                accEvent.setCurrency(secLending.getMarginCallCurrency());
                accountingEvents.addElement(accEvent);
            }


        }

    }

    @Override
    public void getMTM_FULL(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {

        if (event instanceof PSEventValuation) {// 471

            SecFinance secFinance = (SecFinance) trade.getProduct();
            if (secFinance.getSecurity() instanceof Bond && !"Negociacion".equalsIgnoreCase(trade.getBook().getAccountingBook().getName())) {

                PricerMeasure[] measures = null;// 474
                if (eventConfig.getPricingMeasures() != null) {// 475
                    measures = AccountingHandler.getBOPricingMeasure(trade, eventConfig.getPricingMeasures(), event, pricingEnv, pricer, ((PSEventValuation) event).getValuationDate());// 476 477
                }

                if (measures.length > 0) {// 478
                    for (int i = 0; i < measures.length; ++i) {// 481
                        PricerMeasure measure = measures[i];// 482
                        if (measure != null && measure.getType() >= 0) {// 483
                            BOPosting accEvent = new BOPosting(eventConfig);// 484
                            JDate now = this.getCurrentDate(trade.getTradeCurrency());
                            accEvent.setAmount(secFinance.computeNominal(trade, now));// 485
                            accEvent.setSubId((long) measure.getType());// 486
                            accEvent.setDescription(measure.getName());// 487
                            accEvent.setCurrency(((PSEventValuation) event).getCurrency());// 488
                            accEvent.setEffectiveDate(((PSEventValuation) event).getValuationDate().getJDate(trade.getBook().getLocation()));// 489
                            accountingEvents.addElement(accEvent);// 490
                        }
                    }

                }
            } else {
                super.getMTM_FULL(trade, event, eventConfig, accountingEvents, rule, pricingEnv, pricer);
            }
        }
    }

}
