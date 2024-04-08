package com.calypso.tk.util;


import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEventValuation;
import com.calypso.tk.pricer.PricerStructuredProduct;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.StructuredProduct;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import java.util.Hashtable;
import java.util.Vector;


public class MultiCcyValuationUtil extends ValuationUtil {


    @Override
    public boolean priceTrades(JDatetime valDatetime, Hashtable exceptions, String attribute, String inputCcy, String dispatcherConfigName, int tradePerJob, boolean keepCF, int eventsPerPublish, boolean useEventArray, int eventsPerArray, JDatetime eventDatetime, JDatetime undoDatetime, DSConnection ds) {
        if (dispatcherConfigName != null) {
            return this.priceTradesUsingDispatcher(valDatetime, exceptions, attribute, inputCcy, dispatcherConfigName, tradePerJob, eventsPerPublish, useEventArray, eventsPerArray, eventDatetime, undoDatetime);
        } else {
            this._tradePricingMeasures = new Hashtable();
            this._tradePricingSubMeasures = new Hashtable();
            if (this._dailyPM) {
                this._tradePricingDailyMeasures = new Hashtable();
                this._tradePricingSubDailyMeasures = new Hashtable();
            }
            JDate valDate = this.getJDate(valDatetime);
            Vector excludedPMForValuations = LocalCache.getDomainValues(ds != null ? ds : DSConnection.getDefault(), "ExcludedPMForValuation");
            boolean saveQuoteSetCacheB = this._env.getQuoteSet().getCacheNotFound();
            try {
                this._env.getQuoteSet().setCacheNotFound(true);
                PricerMeasure[] templates = createMeasure(this._pricerMeasures);
                boolean error = false;
                boolean isTradeCcyB = false;
                if (attribute.equals("TRADE")) {
                    isTradeCcyB = true;
                }
                Vector events = new Vector();
                int countEvents = 0;
                this.items = new Hashtable();
                for(int i = 0; i < this._trades.size(); ++i) {
                    Trade trade = this._trades.get(i);
                    if (trade.getProduct() instanceof Equity && !Util.isEmpty(attribute) && "MULTICCY".equalsIgnoreCase(attribute)) {
                        Equity equity = (Equity) trade.getProduct();
                        Equity equityClone = (Equity) equity.clone();
                        equityClone.setCurrency(trade.getSettleCurrency());
                        trade.setProduct(equityClone);
                    }
                    PricerMeasure[] measures;
                    if (Util.isEmpty(excludedPMForValuations)) {
                        measures = copyMeasures(templates);
                    } else {
                        measures = copyMeasures(templates, trade, ds);
                    }
                    JDate yesterdayDate = null;
                    JDatetime yesterdayTime = null;
                    PricerMeasure[] dailymeasures;
                    if (this._dailyPM) {
                        yesterdayDate = this.getYesterdayDate(valDate, this._trades.get(i).getBook());
                        yesterdayTime = this.getYesterdayTime(valDatetime, this._trades.get(i).getBook());
                        if (Util.isEmpty(excludedPMForValuations)) {
                            dailymeasures = copyMeasures(templates);
                        } else {
                            dailymeasures = copyMeasures(templates, trade, ds);
                        }
                        for(int j = 0; j < dailymeasures.length; ++j) {
                            dailymeasures[j].setValue(0.0D);
                        }
                    } else {
                        dailymeasures = null;
                    }
                    for(int j = 0; j < measures.length; ++j) {
                        measures[j].setValue(0.0D);
                    }
                    Pricer pricer = this._env.getPricerConfig().getPricerInstance(trade.getProduct());
                    PricerMeasure[][] subMeasures = (PricerMeasure[][])null;
                    PricerMeasure[][] subDailyMeasures = (PricerMeasure[][])null;
                    try {
                        if (pricer != null) {
                            if (trade.getProduct() instanceof StructuredProduct && pricer instanceof PricerStructuredProduct) {
                                pricer.getMissingMarketDataItems(trade, this._env, valDate, this.items);
                                subMeasures = ((PricerStructuredProduct)pricer).priceSubProducts(trade, valDatetime, this._env, measures);
                                if (this._dailyPM && trade.getTradeDate().lte(yesterdayTime)) {
                                    pricer.getMissingMarketDataItems(trade, this._env, yesterdayDate, this.items);
                                    subDailyMeasures = ((PricerStructuredProduct)pricer).priceSubProducts(trade, yesterdayTime, this._env, dailymeasures);
                                }
                            } else {
                                pricer.getMissingMarketDataItems(trade, this._env, valDate, this.items);
                                pricer.price(trade, valDatetime, this._env, measures);
                                if (this._dailyPM && trade.getTradeDate().lte(yesterdayTime)) {
                                    pricer.getMissingMarketDataItems(trade, this._env, yesterdayDate, this.items);
                                    pricer.price(trade, yesterdayTime, this._env, dailymeasures);
                                }
                            }
                        } else {
                            error = true;
                            Log.info(Log.OLD_TRACE, "Pricer not found for : " + trade.getProduct().getType());
                            if (exceptions != null) {
                                exceptions.put(trade, "Pricer not found for : ");
                            }
                        }
                        if (!isTradeCcyB) {
                            measures = convertTradeMeasures(trade, getValCcy(trade, attribute, inputCcy, this._env), valDate, valDatetime, exceptions, this._env, measures);
                            if (this._dailyPM) {
                                dailymeasures = convertTradeMeasures(trade, getValCcy(trade, attribute, inputCcy, this._env), yesterdayDate, yesterdayTime, exceptions, this._env, dailymeasures);
                            }
                        }
                        if (this._dailyPM) {
                            for(int j = 0; j < dailymeasures.length; ++j) {
                                PricerMeasure dm = dailymeasures[j];
                                if (dm.isAdditive()) {
                                    dm.setValue(measures[j].getValue() - dm.getValue());
                                }
                            }
                        }
                        if (measures != null) {
                            this._tradePricingMeasures.put(trade, measures);
                        }
                        if (dailymeasures != null) {
                            this._tradePricingDailyMeasures.put(trade, dailymeasures);
                        }
                        if (subMeasures != null) {
                            this._tradePricingSubMeasures.put(trade, subMeasures);
                        }
                        if (subDailyMeasures != null) {
                            this._tradePricingSubDailyMeasures.put(trade, subDailyMeasures);
                        }
                        if (eventsPerPublish > 0) {
                            PSEventValuation event = new PSEventValuation();
                            event.setTrade(trade);
                            event.setQuantity(trade.getQuantity());
                            event.setTradeLongId(trade.getLongId());
                            event.setCurrency(getValCcy(trade, attribute, inputCcy, this._env));
                            event.setValuationDate(eventDatetime);
                            event.setValuationDateAsOf(valDatetime);
                            event.setSettleDate(trade.getSettleDate());
                            event.setProductId(trade.getProduct().getId());
                            event.setUndoDatetime(undoDatetime);
                            event.setTradeVersion(trade.getVersion());
                            Vector pmv = new Vector();
                            event.setSubMeasures(subMeasures);
                            if (measures != null) {
                                for(int j = 0; j < measures.length; ++j) {
                                    PricerMeasure m = measures[j];
                                    pmv.addElement(m);
                                }
                                event.setMeasures(pmv);
                                event.setPricingEnvName(this._env.getName());
                                events.addElement(event);
                                ++countEvents;
                            } else {
                                Log.error(this, "priceTrades(): Null measures for Trade id = " + trade.getLongId());
                            }
                            Vector dpmv = new Vector();
                            if (dailymeasures != null) {
                                for(int j = 0; j < dailymeasures.length; ++j) {
                                    PricerMeasure m = dailymeasures[j];
                                    dpmv.addElement(m);
                                }
                                event.setDailyMeasures(dpmv);
                            }
                        }
                    } catch (Exception var49) {
                        Log.error(Log.OLD_TRACE, (String)null, var49);
                        if (exceptions != null) {
                            exceptions.put(trade, "Pricing " + var49.getMessage());
                        }
                        this.items.putAll(exceptions);
                    } finally {
                        if (!keepCF && !trade.getProduct().getCustomFlowsB()) {
                            trade.getProduct().setFlows((CashFlowSet)null);
                        }
                    }
                    if (eventsPerPublish > 0 && (countEvents >= eventsPerPublish || i == this._trades.size() - 1)) {
                        try {
                            ds.getRemoteTrade().saveAndPublish(events, useEventArray, eventsPerArray);
                            events = new Vector();
                            countEvents = 0;
                        } catch (Exception var48) {
                            Log.error(Log.OLD_TRACE, (String)null, var48);
                        }
                    }
                    trade.getProduct().compressFlows();
                }
                if (error) {
                    boolean var54 = false;
                    return var54;
                }
            } catch (Exception var51) {
                boolean var18 = false;
                return var18;
            } finally {
                this._env.getQuoteSet().setCacheNotFound(saveQuoteSetCacheB);
            }
            return true;
        }
    }


}
