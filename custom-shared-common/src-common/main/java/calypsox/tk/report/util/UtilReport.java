package calypsox.tk.report.util;

import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;

import com.calypso.tk.product.*;
import com.calypso.tk.product.flow.CashFlowCoupon;
import com.calypso.tk.product.flow.CashFlowDividend;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.product.flow.CashFlowPriceChange;
import com.calypso.tk.product.flow.CashFlowResettablePrincipal;
import com.calypso.tk.product.flow.CashFlowSimple;
import com.calypso.tk.product.util.NotionalDate;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.refdata.FdnCurrencyPair;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TradePrice;

import calypsox.tk.core.SantPricerMeasure;

/**
 * <p>
 * Title: UtilReport
 * </p>
 * <p>
 * Description: Utilities for use in Report classes
 * </p>
 * .
 *
 * @author Silviano Espejo
 * @version 1.0
 */

public class UtilReport {
    private UtilReport() {
        // nothing to do
    }

    private static final String DATEFORMAT = "dd/MM/yyyy";
    private static final int SQL_IN_TRADES_LIMIT = 950;

    // CAL_SUP_101
    /** Control format. */
    public static final String CONTROL_FORMAT = "%1$08d";

    // CAL_REP_164
    /** TLM eol char. */
    public static final String TLM_EOL_CHAR = "#$";

    /** TLM eol char with hash. */
    public static final String TLM_EOL_CHAR_HASH = "$";

    /**
     * Gets the value without zeros on left.
     *
     * @param valueIn
     *            The value with the original length
     * @param length
     *            The length we need (total length including integer part,
     *            decimal point and decimal places)
     * @param decimalLength
     *            the decimal length
     * @return the value without zeros on left
     * @deprecated use String.format on the ReportStyle Return a String with the
     *             value specified.If valueIn is a date this method must not be
     *             called The dates must be formatted in the
     *             template!!!!!!!!!!!!!!!!!!!!! If valueIn is a String, it adds
     *             at the end white spaces until the length If valueIn is a
     *             number, it convert to String and add 0's to the rigth until
     *             the length
     */
    @Deprecated
    public static String getValueWithoutZerosOnLeft(final Object valueIn,
                                                    final int length, final int decimalLength) {
        Log.debug("calypsox.tk.report.util.UtilReport",
                "UtilReport.getValue Start with valueIn=" + valueIn
                        + " and length=" + length + " and decimalLength="
                        + decimalLength);
        // String valueOut = null;

        // Numbers
        // String formatToUse="%."+decimalLength+"f%n";
        final String formatToUse = "%." + decimalLength + "f";
        final String stringValue = String.format(formatToUse, valueIn);
        // valueOut = fill(stringValue, '0', length, false);

        Log.debug("calypsox.tk.report.util.UtilReport",
                "UtilReport.getValue End returning " + stringValue);
        return stringValue;
    }

    /**
     * Obtain date control.
     *
     * @param date
     *            the date
     * @return the string
     */
    public static String obtainDateControl(final JDate date) {
        final StringBuffer fecha = new StringBuffer();
        // JDate jdateBusqueda =
        // JDate.valueOf(Calendar.getInstance().getTime());
        // Date d = jdateBusqueda.getDate();
        // d.setDate(jdateBusqueda.getDate().getDate() - 1);
        // jdateBusqueda = jdateBusqueda.valueOf(d);
        final String fechaaux = Util.dateToString(date, DATEFORMAT);
        fecha.append(fechaaux.substring(6));
        fecha.append(fechaaux.substring(3, 5));
        fecha.append(fechaaux.substring(0, 2));

        return fecha.toString();
    }

    /**
     * This method retreives NPV* TradePrices the passed in valDate.
     *
     * @param ds
     *            the ds
     * @param trades
     *            the trades
     * @param valDate
     *            the val date
     * @return the trade prices
     * @throws RemoteException
     *             the remote exception
     */
    public static ArrayList<TradePrice> getTradePrices(final DSConnection ds,
                                                       final TradeArray trades, final JDate valDate)
            throws RemoteException {
        return getTradePrices(ds, trades, valDate, null);
    }

    /**
     * This method retreives NPV* TradePrices between startDate and endDate
     * passed in.
     *
     * @param ds
     *            the ds
     * @param trades
     *            the trades
     * @param valDate
     *            the val date
     * @param startDate
     *            the start date
     * @return the trade prices
     * @throws RemoteException
     *             the remote exception
     */
    public static ArrayList<TradePrice> getTradePrices(final DSConnection ds,
                                                       final TradeArray trades, final JDate valDate, final JDate startDate)
            throws RemoteException {
        final ArrayList<TradePrice> tradePrices = new ArrayList<TradePrice>();

        final StringBuffer whereClause = new StringBuffer();
        if (startDate == null) {
            whereClause.append("trunc(valuation_date)=").append(
                    Util.date2SQLString(valDate));
        } else {
            whereClause.append("trunc(valuation_date) between ")
                    .append(Util.date2SQLString(startDate)).append(" AND ")
                    .append(Util.date2SQLString(valDate));
        }

        whereClause.append(" AND MEASURE_ID in (")
                //.append(SantPricerMeasure.MTM_FULL_LAGO_BASE).append(',')
                //.append(SantPricerMeasure.MTM_FULL_LAGO).append(',')
                .append(SantPricerMeasure.NPV).append(',')
                .append(SantPricerMeasure.NPV_LEG1).append(',')
                .append(SantPricerMeasure.NPV_LEG2)
                //.append(',')
                //.append(SantPricerMeasure.MIS_NPV).append(',')
                //.append(SantPricerMeasure.MIS_NPV_LEG1).append(',')
                //.append(SantPricerMeasure.MIS_NPV_LEG2)
                .append(") AND TRADE_ID IN (");

        if (trades.size() > 0) {
            final ArrayList<String> idsSQLStrList = getTradeIdsForSQLInClause(trades);
            for (final String idsSQLStr : idsSQLStrList) {
                final String where = whereClause + idsSQLStr + ")";
                @SuppressWarnings("unchecked")
                final ArrayList<TradePrice> tempList = ds.getRemoteTrade()
                        .getTradePrices(null, where, null);
                tradePrices.addAll(tempList);
            }
        }

        return tradePrices;
    }

    /**
     * This method returns a csv of Trade Ids as a String with 950 Trades. There
     * is a limit of 1000 ids in the SQL IN clause so this method is useful in
     * which case.
     *
     * @param trades
     *            the trades
     * @return an Array list of Strings. Each String contains less than or equal
     *         to 950 trade ids seperated by Comma.
     */
    public static ArrayList<String> getTradeIdsForSQLInClause(
            final TradeArray trades) {
        final ArrayList<String> sqlInStrList = new ArrayList<String>();

        if (trades.size() > 0) {
            StringBuffer sqlInStr = new StringBuffer(" ");

            for (int i = 0; i < trades.size(); i++) {
                sqlInStr.append(trades.get(i).getLongId());
                if ((i == (trades.size() - 1))
                        || (((i + 1) % SQL_IN_TRADES_LIMIT) == 0)) {
                    // SQL_IN_TRADES_LIMIT reached or end of tradeArray reached
                    sqlInStrList.add(sqlInStr.toString());
                    // System.out.println("****" + sqlInStr);
                    sqlInStr = new StringBuffer(" ");
                } else if (i < (trades.size() - 1)) {
                    sqlInStr.append(", ");
                }
            }

        }

        return sqlInStrList;
    }

    /**
     * Splits a list of identifiers into several lists that are fit to be used
     * in an SQL IN clause. The returned list will contain one or several String
     * objects, each one with a list of identifiers, like "(1, 2, 3, ..., 500)".
     *
     * @param ids
     *            List of identifiers
     * @return A list of Strings to be used in an SQL IN clause.
     */
    // CAL_SUP_096
    public static List<String> idsListToSqlInClause(final List<Long> ids) {
        final List<String> sqlInStrList = new ArrayList<String>();

        // CAL_402_
        if (!Util.isEmpty(ids)) {
            StringBuilder sqlInStr = new StringBuilder();

            for (int iId = 0; iId < ids.size(); iId++) {
                if (sqlInStr.length() == 0) {
                    sqlInStr.append('(');
                }
                sqlInStr.append(ids.get(iId));
                if ((iId == (ids.size() - 1))
                        || (((iId + 1) % SQL_IN_TRADES_LIMIT) == 0)) {
                    sqlInStr.append(')');
                    sqlInStrList.add(sqlInStr.toString());
                    sqlInStr = new StringBuilder();
                } else if (iId < (ids.size() - 1)) {
                    sqlInStr.append(", ");
                }
            }

        }

        return sqlInStrList;
    }

    /**
     * Gets the par tipo.
     *
     * @param trades
     *            the trades
     * @return the par tipo
     */
    public static ArrayList<String> getParTipo(final TradeArray trades) {
        final ArrayList<String> toret = new ArrayList<String>();

        for (int i = 0; i < trades.size(); i++) {
            final Trade trade = trades.get(i);
            final Product prod = trade.getProduct();
            if ((prod != null) && (prod instanceof FX)) {
                final FX fx = (FX) prod;
                final CurrencyPair cp = fx.getCurrencyPair();
                if (cp != null) {
                    toret.add(cp.getPrimaryCode() + cp.getQuotingCode());
                } else {
                    toret.add("");
                }
            }
        }

        return toret;
    }

    /**
     * Gets the sentido.
     *
     * @param trade
     *            the trade
     * @return the sentido
     */
    public static String getSentido(final Trade trade) {
        String toret = "";

        final Status status = trade.getStatus();
        if (status != null) {
            final String stor = status.getStatus();
            if ("RATE_RESET".equals(stor)) {
                final Product prod = trade.getProduct();
                if ((prod != null) && (prod instanceof FXNDF)) {
                    final FXNDF ndf = (FXNDF) prod;
                    final double amount = ndf.getSettlementAmount(trade,
                            trade.getSettleDate());
                    if (amount > 0) {
                        toret = "COMPRA";
                    } else {
                        toret = "VENTA";
                    }
                } else {
                    toret = "-";
                }
            } else {
                toret = "-";
            }
        } else {
            toret = "-";
        }

        return toret;
    }

    /**
     * Gets the sentido2.
     *
     * @param trade
     *            the trade
     * @return the sentido2
     */
    public static String getSentido2(final Trade trade) {
        String toret = "";

        final Product p = trade.getProduct();
        if (p instanceof FXSwap) {
            final FXSwap sw = (FXSwap) p;
            if (sw.getPrimaryAmount(trade).get() < 0) {
                toret = "VENTA";
            } else {
                toret = "COMPRA";
            }
        } else if (p instanceof FX) {
            final FX fx = (FX) p;
            if (fx.getPrimaryAmount(trade).get() < 0) {
                toret = "VENTA";
            } else {
                toret = "COMPRA";
            }
        }

        return toret;
    }

    // CAL_SUP_101
    /**
     * Generates a control line for reports based on the number of rows and the
     * valuation date.
     *
     * @param numberOfRows
     *            Number of rows in the report
     * @param valuationDatetime
     *            Valuation date of the report
     * @return A StringBuilder containing the control line
     */
    public static StringBuilder getControlLine(final int numberOfRows,
                                               final JDate valuationDatetime) {
        final StringBuilder line = new StringBuilder("*****");

        final String nlines = getLines(numberOfRows);

        final String controlDate = UtilReport
                .obtainDateControl(valuationDatetime);

        line.append(nlines);
        line.append(controlDate);
        return line;
    }

    // CAL_SUP_101
    private static String getLines(final int numberOfRows) {
        final String nlines = String.format(CONTROL_FORMAT, numberOfRows);
        return nlines;
    }



    public static String getReferenceType(PerformanceSwappableLeg leg) {
        if (leg instanceof SwapLeg) {
            RateIndex index = getRateIndex(leg);
            if (index == null)
                return null;
            else
                return index.getName() + "-" + index.getTenor();
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) leg;
            Security sec = perfSwapLeg.getReferenceAsset();
            if (sec instanceof Bond) {
                Bond bond = (Bond) sec;
                return bond.getSecCode(SecCode.ISIN);
            }
        }
        return null;
    }

    public static Boolean isFixedInterestType(PerformanceSwappableLeg leg) {
        if (leg instanceof SwapLeg) {
            SwapLeg swapLeg = (SwapLeg) leg;
            if (swapLeg.isFixed())
                return true;
            else
                return false;
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) leg;
            Security sec = perfSwapLeg.getReferenceAsset();
            if (sec instanceof Bond) {
                Bond bond = (Bond) sec;
                if (bond.getFixedB())
                    return true;
            }
            return false;
        }
        return null;
    }


    public static String getInterestype(PerformanceSwappableLeg leg) {
        Boolean isFixed = isFixedInterestType(leg);
        if(isFixed==null)
            return null;

        if(isFixed) {
            return "F";
        }
        else
            return "V";
    }

    public static  RateIndex getRateIndex(PerformanceSwappableLeg leg) {
        if (leg instanceof SwapLeg) {
            RateIndex index = ((SwapLeg) leg).getRateIndex();
            return index;
        }
        return null;
    }

    public static  DayCount getDayCount(PerformanceSwappableLeg leg) {
        if (leg instanceof SwapLeg) {
            SwapLeg swapLeg = (SwapLeg) leg;
            return swapLeg.getDayCount();
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg performanceSwapLeg = (PerformanceSwapLeg) leg;
            return performanceSwapLeg.getDayCount();
        }
        return null;
    }


    public static  Frequency getPmtFrequency(PerformanceSwappableLeg leg) {
        if (leg instanceof SwapLeg) {
            SwapLeg swapLeg = (SwapLeg) leg;
            return swapLeg.getCouponFrequency();
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg performanceSwapLeg = (PerformanceSwapLeg) leg;
            return performanceSwapLeg.getIncomePmtSchedule().getFrequency();
        }
        return null;
    }

    public static  JDate getStartDate(PerformanceSwappableLeg leg) {
        if (leg instanceof SwapLeg) {
            SwapLeg swapLeg = (SwapLeg) leg;
            return swapLeg.getStartDate();
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg performanceSwapLeg = (PerformanceSwapLeg) leg;
            return performanceSwapLeg.getStartDate();
        }
        return null;
    }

    public static  Amount getNominal(PerformanceSwappableLeg leg, JDate asOfDate) {
        String ccy = leg.getCurrency();
        int digits = 2;

        if (leg instanceof SwapLeg) {
            SwapLeg swapLeg = (SwapLeg) leg;
            double nominal = swapLeg.getPrincipal();
            if (ccy != null) {
                nominal = CurrencyUtil.roundAmount(nominal, ccy);
                digits = CurrencyUtil.getRoundingUnit(ccy);
            }
            return new SignedAmount(nominal, digits);
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg performanceSwapLeg = (PerformanceSwapLeg) leg;
            double nominal = performanceSwapLeg.getNotional();

            if (ccy != null) {
                nominal = CurrencyUtil.roundAmount(nominal, ccy);
                digits = CurrencyUtil.getRoundingUnit(ccy);
            }

            return new SignedAmount(nominal, digits);
        }

        return null;
    }


    public static  Integer getAmortizationType(String strAmortizationType) {
        if(strAmortizationType ==null || "Bullet".equals(strAmortizationType)) {
            return 0;
        } else if("Schedule".equals(strAmortizationType)) {
            return 2;
        }
        return 0;
    }

    public static  Integer getAmortization(PerformanceSwappableLeg leg) {
        if (leg instanceof SwapLeg) {
            SwapLeg swapLeg = (SwapLeg) leg;
            return getAmortizationType(swapLeg.getPrincipalStructure());
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg performanceSwapLeg = (PerformanceSwapLeg) leg;
            Security sec = performanceSwapLeg.getReferenceAsset();
            if (sec instanceof Bond) {
                Bond bond = ((Bond) sec);
                if (bond.getAmortizingB() && ! "Bullet".equals(bond.getPrincipalStructure()))
                    return 2;
                else return 0;
            }
        }

        return 0;
    }

    public static  Frequency getPrincipalPmtFrequency(PerformanceSwappableLeg leg, Trade trade) {
        if (leg instanceof SwapLeg) {
            SwapLeg swapLeg = (SwapLeg) leg;
            return swapLeg.getCouponFrequency();
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg performanceSwapLeg = (PerformanceSwapLeg) leg;
            PerformanceSwap product = (PerformanceSwap) trade.getProduct();
            Security sec = performanceSwapLeg.getReferenceAsset();
            if (sec instanceof Bond) {
                return ((Bond) product.getReferenceProduct()).getAmortFrequency();
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static  JDate getNextPrincipalLiquidation(PerformanceSwappableLeg leg, JDate valDate) {
        if(getAmortization(leg)==0)
            return null;
        Vector<NotionalDate> amortSchedule = null;
        if (leg instanceof SwapLeg) {
            SwapLeg swapLeg = (SwapLeg) leg;
            amortSchedule = swapLeg.getAmortSchedule();
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg performanceSwapLeg = (PerformanceSwapLeg) leg;
            Security sec = performanceSwapLeg.getReferenceAsset();
            if (sec instanceof Bond) {
                amortSchedule = ((Bond) sec).getAmortSchedule();
            }
        }
        if(amortSchedule!=null)
        {
            Collections.sort(amortSchedule) ;
            for(NotionalDate notionalDate : amortSchedule) {
                if(notionalDate.getStartDate().gte(valDate)) {
                    return notionalDate.getStartDate();
                }
            }
        }
        return null;
    }

    public static  Rate getCurrentRate(PerformanceSwappableLeg leg, JDate valDate) {

        CashFlowSet flows = leg.getFlows();
        if (flows != null && !flows.isEmpty()) {
            CashFlow cashFlow = (CashFlow) flows.findEnclosingCashFlow(valDate, CashFlow.INTEREST);
            return getRate(cashFlow);

        }
        return null;
    }

    public static  Rate getCurrentSpread(PerformanceSwappableLeg leg, JDate valDate) {

        CashFlowSet flows = leg.getFlows();
        if (flows != null && !flows.isEmpty()) {
            CashFlow cashFlow = (CashFlow) flows.findEnclosingCashFlow(valDate, CashFlow.INTEREST);
            return getSpread(cashFlow);

        }
        return null;
    }

    public static Rate getSpread(CashFlow cf) {
        if (cf instanceof CashFlowInterest) {
            CashFlowInterest cfi = (CashFlowInterest) cf;
            return new Rate(cfi.getSpread(), 6);
        }

        return new Rate(0, 6);
    }

    public static Rate getRate(CashFlow cf) {
        if (cf instanceof CashFlowInterest) {
            CashFlowInterest cfi = (CashFlowInterest) cf;
            return new Rate(cfi.getRate(), Util.getRateDecimals());
        }

        return null;
    }

    public static JDate getNextCashFlowDate(PerformanceSwappableLeg leg, JDate valDate, String flowType) {
        JDate flowDate = null;
        CashFlowSet flows = leg.getFlows();
        if (flows != null && !flows.isEmpty()) {
            CashFlow cashFlow = (CashFlow) flows.findEnclosingCashFlow(valDate, flowType);

            flowDate = cashFlow != null ? cashFlow.getCashFlowDefinition().getDate() : null;
        }
        return flowDate;
    }

    @SuppressWarnings("unchecked")
    public static List<JDate> getAllCashFlowDates(CashFlowSet flows, List<String> flowTypes, String dateType) {
        ArrayList<JDate> allDates = new ArrayList<JDate>();

        if (flows != null)
            for (CashFlow cashFlow : flows) {
                if (flowTypes == null || flowTypes.contains(cashFlow.getType())) {
                    JDate date = getCashFlowDate(cashFlow, dateType);
                    if (date != null)
                        allDates.add(date);
                }
            }

        Collections.sort(allDates);

        return allDates;
    }

    public static List<JDate> getAllCashFlowDates(PerformanceSwappableLeg leg, String flowType, String dateType) {
        ArrayList<String> flowTypes = new ArrayList<String>();
        flowTypes.add(flowType);
        return getAllCashFlowDates(leg.getFlows(), flowTypes, dateType);

    }


    public static JDate getLastPriceFixingDate (PerformanceSwapLeg leg, JDate asOfDate) {
        JDate lastFixingDate = leg.getStartDate();
        double lastFixing = leg.getInitialPrice();
        int numFlows = leg.getFlows().size();
        for(int i = 0; i < numFlows; ++i) {
            CashFlow f = (CashFlow)leg.getFlows().elementAt(i);
            if (leg.isReturnFlow(f)) {
                CashFlowPriceChange flow = (CashFlowPriceChange)f;
                JDate fixingDate = flow.getPriceFixingDate();
                if (fixingDate.lte(asOfDate) && fixingDate.after(lastFixingDate)) {

                    lastFixingDate = fixingDate;
                    lastFixing = flow.getEndPrice();
                }
            }
        }

        return lastFixingDate;
    }

    public static double getLastPriceFixing (PerformanceSwapLeg leg, JDate asOfDate) {
        JDate lastFixingDate = leg.getStartDate();
        double lastFixing = leg.getInitialPrice();
        int numFlows = leg.getFlows().size();
        for(int i = 0; i < numFlows; ++i) {
            CashFlow f = (CashFlow)leg.getFlows().elementAt(i);
            if (leg.isReturnFlow(f)) {
                CashFlowPriceChange flow = (CashFlowPriceChange)f;
                JDate fixingDate = flow.getPriceFixingDate();
                if (fixingDate.lte(asOfDate) && fixingDate.after(lastFixingDate)) {

                    lastFixingDate = fixingDate;
                    lastFixing = flow.getEndPrice();
                }
            }
        }

        return lastFixing;
    }

    public static JDate getCashFlowDate(PerformanceSwappableLeg leg, JDate valDate, String flowType, String dateType,
                                        boolean next) {
        ArrayList<String> flowTypes = new ArrayList<String>();
        flowTypes.add(flowType);
        return getCashFlowDate(leg.getFlows(), valDate, flowTypes, dateType, next);
    }

    public static JDate getCashFlowDate(CashFlowSet flows, JDate valDate, String flowType, String dateType,
                                        boolean next) {
        ArrayList<String> flowTypes = new ArrayList<String>();
        flowTypes.add(flowType);
        return getCashFlowDate(flows, valDate, flowTypes, dateType, next);
    }
    
    public static double getCashFlowNotional(CashFlowSet flows, JDate valDate, String flowType) {
    	
    	CashFlow flow = null;
    	
    	for (CashFlow f: flows) {
    		if (flowType.equals(CashFlow.INTEREST)) {
    			if (valDate.after(f.getStartDate()) && valDate.before(f.getEndDate())) {
    				flow = f;
    				break;
    			}
    		}
    	}
    	
    	if (flow instanceof CashFlowCoupon) {
    		return ((CashFlowCoupon) flow).getNotional();
    	} else if (flow instanceof CashFlowSimple) {
    		return ((CashFlowSimple) flow).getNotional();
    	}
    	
    	return 0;
    	
    }

    public static CashFlow getCashFlow(CashFlowSet flows, JDate valDate, String flowType, String dateType,
                                       boolean next) {
        ArrayList<String> flowTypes = new ArrayList<String>();
        flowTypes.add(flowType);
        return getCashFlow(flows, valDate, flowTypes, dateType, next);
    }


    public static CashFlow getCashFlow(CashFlowSet flows, JDate valDate, List<String> cashFlowTypes,
                                       String dateType, boolean next) {

        ArrayList<CashFlow> allFlows = new ArrayList<CashFlow> ();
        allFlows.addAll(Arrays.asList(flows.getFlows()));


        Iterator<CashFlow> itCF = allFlows.listIterator();
        while(itCF.hasNext()) {
            CashFlow cf = itCF.next();
            if(!cashFlowTypes.contains(cf.getType())) {
                itCF.remove();
            }
        }

        allFlows.sort(new Comparator<CashFlow>() {
            @Override
            public int compare(CashFlow o1, CashFlow o2) {
                JDate dateFlow1 = getCashFlowDate(o1, dateType);
                JDate dateFlow2 = getCashFlowDate(o2, dateType);
                return dateFlow1.compareTo(dateFlow2);
            }

        });




        CashFlow lastCashFlow = null;

        for (CashFlow flow : allFlows) {

            JDate date = getCashFlowDate(flow);

            if (next && date.gte(valDate))
                return flow;
            if (!next) {
                if (date.before(valDate))
                    lastCashFlow = flow;
                else if (date.after(valDate))
                    return lastCashFlow;
                else
                    return flow;
            }
        }

        return lastCashFlow;

    }

    public static JDate getCashFlowDate(CashFlowSet flows, JDate valDate, List<String> cashFlowTypes,
                                        String dateType, boolean next) {
        List<JDate> allDates = getAllCashFlowDates(flows, cashFlowTypes, dateType);

        JDate lastResetDate = null;

        for (JDate date : allDates) {

            if (next && date.gte(valDate))
                return date;
            if (!next) {
                if (date.before(valDate))
                    lastResetDate = date;
                else if (date.after(valDate))
                    return lastResetDate;
                else
                    return date;
            }
        }

        return lastResetDate;

    }

    public static JDate getCashFlowDate(CashFlow cashFlow) {
        return getCashFlowDate(cashFlow, "Date");
    }

    public static JDate getCashFlowDate(CashFlow cashFlow, String dateType) {
        if (dateType.equals("ResetDate")) {
            return getResetDate(cashFlow);
        } else if (dateType.equals("Date")) {
            return cashFlow.getDate();
        }
        return null;

    }

    public static JDate getResetDate(CashFlow cashFlow) {
        if (cashFlow != null) {
            if (cashFlow instanceof CashFlowInterest) {
                CashFlowInterest cfi = (CashFlowInterest) cashFlow;
                return cfi.getResetDate();
            } else if (cashFlow instanceof CashFlowDividend) {
                CashFlowDividend cfd = (CashFlowDividend) cashFlow;
                return cfd.getFXResetDate();
            } else if (cashFlow instanceof CashFlowResettablePrincipal) {
                CashFlowResettablePrincipal cfResettable = (CashFlowResettablePrincipal) cashFlow;
                return cfResettable.getResetDate();
            }

        }

        return null;
    }

    public static Object formatResult(Object o, char decimalSeparator) {
        if(o instanceof Number)
            return formatNumber((Number)o, decimalSeparator);
        if(o instanceof DisplayValue) {
            return formatNumber(((DisplayValue)o).get(), decimalSeparator);
        }if(o instanceof String){

            return ((String) o).replace(String.valueOf(decimalSeparator),"").replace(',',decimalSeparator);
        }

        return o;
    }

    public static Object formatResult(double d, char decimalSeparator) {
        return formatNumber(new Double(d), decimalSeparator);
    }

    public static Object formatNumber(Number number, char decimalSeparator) {
        if(number instanceof Double) {
            DecimalFormat df = new DecimalFormat("0.00");
            df.setGroupingUsed(false);
            DecimalFormatSymbols newSymbols = new DecimalFormatSymbols();
            newSymbols.setDecimalSeparator(decimalSeparator);
            df.setDecimalFormatSymbols(newSymbols);
            if (((Double) number).isNaN()){
                number = 0.0D;
            }
            return df.format(number);
        }
        return number;
    }

    public static Double getFXCurrencyPair(String ccy) {
        Vector<String> currenciesPair = LocalCache.getDomainValues(DSConnection.getDefault(), "CurrencyPairFX");
        for (String currencyPair : currenciesPair) {
            String[] value = currencyPair.split("\\.");
            if (Util.isSame(ccy, value[0])) {
                Optional<CurrencyPair> pair = Optional.ofNullable(CurrencyUtil.getCurrencyDefault(ccy).getCurrencyPair(value[1]));
                return pair.map(FdnCurrencyPair::getFixedRate).orElse(0D);
            }
        }
        return 1D;
    }

    public static String getCurrencyPair(String ccy) {
        Vector<String> currenciesPair = LocalCache.getDomainValues(DSConnection.getDefault(), "CurrencyPairFX");
        for (String currencyPair : currenciesPair) {
            String[] value = currencyPair.split("\\.");
            if (Util.isSame(ccy, value[0])) {
                return value[1];
            }
        }
        return ccy;
    }
}
