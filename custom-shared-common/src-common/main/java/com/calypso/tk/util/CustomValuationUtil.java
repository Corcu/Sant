package com.calypso.tk.util;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.mo.LiquidationConfig;
import com.calypso.tk.product.FXBased;
import com.calypso.tk.service.DSConnection;



import java.util.HashMap;

public class CustomValuationUtil extends ValuationUtil {

    @Override
    public boolean loadTrades(JDatetime valDatetime, JDatetime undoDatetime, boolean removePosition, boolean useTradeDatePosition, boolean useEOD, boolean checkMaturity, boolean includeMat, DSConnection ds) {
        TradeArray trades = null;// 348


        try {
            DSConnection rods = ScheduledTask.getReadOnlyDS("EOD_VALUATION", ds);
            if (undoDatetime != null) {
                trades = ScheduledTask.getTrades(rods, this._tradeFilter, valDatetime, undoDatetime, includeMat);
            } else {
                trades = ScheduledTask.getTrades(rods, this._tradeFilter, valDatetime, (JDatetime)null, includeMat);
            }

            this._trades = new TradeArray();
            new HashMap();
            boolean includeMatured = includeMat;
            if (this._tradeFilter.getPositionSpec() != null && this._tradeFilter.getPositionSpec().getLiquidationConfig() != null) {
                this.setLiquidationConfig(this._tradeFilter.getPositionSpec().getLiquidationConfig());
            }

            for(int i = 0; i < trades.size(); ++i) {
                Trade trade = trades.get(i);
                if (!trade.getTradeDate().after(valDatetime)) {
                    JDate settleDate;
                    JDatetime settleDateTime;
                    if (!trade.getProduct().hasSecondaryMarket()) {
                        settleDate = trade.getMaturityDateInclFees();
                        if (settleDate != null && !includeMatured) {
                            settleDateTime = trade.getBook().getEODTime(settleDate);
                            if (settleDateTime.before(valDatetime)) {
                                continue;
                            }
                        }

                        if (trade.getTradeDate().after(valDatetime)) {
                            continue;
                        }
                    }

                    JDate matDate;
                    if (removePosition && trade.getProduct().isPositionBased()) {
                        if (useTradeDatePosition) {
                            continue;
                        }

                        if (!trade.getProduct().hasSecondaryMarket() && trade.getProduct().getFinalPaymentMaturityDate() != null && trade.getProduct().getMaturityDate() != null) {
                            settleDateTime = null;
                            matDate = trade.getProduct().getMaturityDate();
                            if (useEOD) {
                                settleDateTime = trade.getBook().getEODTime(matDate);
                            } else {
                                settleDateTime = trade.getBook().getStartOfDayTime(matDate);
                            }

                            if (settleDateTime.lte(valDatetime)) {
                                continue;
                            }
                        } else {
                            boolean openTermProduct = false;
                            if ((trade.getProduct().getFinalPaymentMaturityDate() == null || trade.getProduct().isOpen()) && !trade.getProduct().hasSecondaryMarket() && !(trade.getProduct() instanceof FXBased)) {
                                openTermProduct = true;
                            }

                            settleDateTime = null;
                            if (useEOD) {
                                settleDateTime = trade.getBook().getEODTime(trade.getMinSettleDate());
                            } else {
                                settleDateTime = trade.getBook().getStartOfDayTime(trade.getMinSettleDate());
                            }

                            if (!openTermProduct && settleDateTime.lte(valDatetime)) {
                                continue;
                            }
                        }
                    } else if (checkMaturity && trade.getProduct().getFinalPaymentMaturityDate() != null) {
                        if (!includeMatured) {
                            settleDateTime = null;
                            matDate = trade.getMaturityDateInclFees();
                            if (useEOD) {// 445
                                settleDateTime = trade.getBook().getEODTime(matDate);
                            } else {
                                settleDateTime = trade.getBook().getStartOfDayTime(matDate);
                            }

                            if (settleDateTime.lte(valDatetime)) {
                                continue;
                            }
                        }
                    } else if (checkMaturity && (trade.getProduct().hasSecondaryMarket() || trade.getProduct().getFinalPaymentMaturityDate(trade) == null) && !includeMatured && !this.isBondRelated(trade)) {
                        settleDateTime = null;
                        if (useEOD) {
                            settleDateTime = trade.getBook().getEODTime(trade.getMinSettleDate());
                        } else {
                            settleDateTime = trade.getBook().getStartOfDayTime(trade.getMinSettleDate());
                        }

                        boolean isOpenTerm = trade.getProduct().isOpen();
                        if (settleDateTime.lte(valDatetime) && !isOpenTerm) {
                            continue;
                        }
                    }

                    if (this._dataFilter == null || this._dataFilter.accept(trade)) {
                        this._trades.add(trade);
                        trade.getProduct().compressFlows();
                    }
                }
            }

            return true;
        } catch (Exception var17) {
            return false;
        }
    }
    private void setLiquidationConfig(LiquidationConfig lc) {
        this._lc = lc;
    }
}
