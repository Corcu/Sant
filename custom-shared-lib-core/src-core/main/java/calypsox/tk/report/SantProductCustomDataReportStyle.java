/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.product.BondCustomData;
import calypsox.tk.product.EquityCustomData;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.flow.CashFlowCoupon;
import com.calypso.tk.product.flow.CashFlowOptionCoupon;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.service.DSConnection;

import java.security.InvalidParameterException;
import java.util.TimeZone;
import java.util.Vector;

public class SantProductCustomDataReportStyle extends ReportStyle {

    private static final long serialVersionUID = 1L;

    // report columns definition
    public static final String HAIRCUT_ECB = "Haircut Ecb";
    public static final String HAIRCUT_SWISS = "Haircut Swiss";
    public static final String HAIRCUT_BOE = "Haircut Boe";
    public static final String HAIRCUT_FED = "Haircut Fed";
    public static final String HAIRCUT_EUREX = "Haircut Eurex";

    public static final String ACTIVE_AVAILABLE_QTY = "Active Available Qty";
    public static final String RATE = "Rate";
    public static final String QUANTITY_ON_LOAN = "Quantity On Loan";
    public static final String EXP_DATE_TYPE = "Expired Date Type";
    public static final String EXP_DATE = "Expired Date";
    public static final String LAST_UPDATE = "Last Update";

    public static final String SANT_EX_DATE_COUPON = "Sant.Sant_Ex_Date_Coupon";

    // Bond flow fields
    public static final String BOND_FLOW_FIELD_ISIN = "Security Code (ISIN)";
    public static final String BOND_FLOW_FIELD_PMT_BEGIN = "Pmt Begin";
    public static final String BOND_FLOW_FIELD_PMT_END = "Pmt End";
    public static final String BOND_FLOW_FIELD_PMT_DT = "Pmt Dt";
    public static final String BOND_FLOW_FIELD_EX_DIVIDEND = "Ex-Dividend";
    public static final String BOND_FLOW_FIELD_PMT_AMT = "Pmt Amt";
    public static final String BOND_FLOW_FIELD_NOTIONAL = "Notional";
    public static final String BOND_FLOW_FIELD_RATE = "Coupon Rate";
    public static final String BOND_FLOW_FIELD_SPREAD = "Spread";
    public static final String BOND_FLOW_FIELD_RESET = "Reset";
    public static final String BOND_FLOW_FIELD_POOL_FACTOR = "Pool Factor";


    public static boolean isProductCustoDataColumn(String columnName) {
        if (HAIRCUT_ECB.equals(columnName) || HAIRCUT_SWISS.equals(columnName) || HAIRCUT_BOE.equals(columnName)
                || HAIRCUT_FED.equals(columnName) || HAIRCUT_EUREX.equals(columnName)
                || ACTIVE_AVAILABLE_QTY.equals(columnName) || RATE.equals(columnName)
                || QUANTITY_ON_LOAN.equals(columnName) || EXP_DATE_TYPE.equals(columnName)
                || EXP_DATE.equals(columnName) || LAST_UPDATE.equals(columnName)
                || BOND_FLOW_FIELD_ISIN.equals(columnName) || BOND_FLOW_FIELD_PMT_BEGIN.equals(columnName)
                || BOND_FLOW_FIELD_PMT_END.equals(columnName) || BOND_FLOW_FIELD_PMT_DT.equals(columnName)
                || BOND_FLOW_FIELD_EX_DIVIDEND.equals(columnName) || BOND_FLOW_FIELD_PMT_AMT.equals(columnName)
                || BOND_FLOW_FIELD_NOTIONAL.equals(columnName) || BOND_FLOW_FIELD_RATE.equals(columnName)
                || BOND_FLOW_FIELD_SPREAD.equals(columnName) || BOND_FLOW_FIELD_RESET.equals(columnName)
                || BOND_FLOW_FIELD_POOL_FACTOR.equals(columnName)) {
            return true;
        }
        return false;
    }

    // report columns definition0
    @Override
    public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
            throws InvalidParameterException {

        Product product = getProduct(row);

        // Bond flows fields
        if (product instanceof Bond) {
            Bond bond = (Bond) product;
            CashFlow cashFlow = null;
            try {
                cashFlow = getFlows(bond, row);
            }catch (Exception e){
                Log.debug(this, e);
            }
            if (BOND_FLOW_FIELD_ISIN.equals(columnName)) {
                String isin = null;
                try {
                    isin = bond.getSecCode("ISIN");
                }catch (NullPointerException e){
                    Log.debug(this, e);
                }
                return isin;

            } else if (BOND_FLOW_FIELD_PMT_BEGIN.equals(columnName)) {
                if (cashFlow == null){
                    return null;
                }
                if (cashFlow instanceof CashFlowCoupon) {
                    return ((CashFlowCoupon) cashFlow).getStartDate();
                } else if (cashFlow instanceof CashFlowOptionCoupon) {
                    return ((CashFlowOptionCoupon) cashFlow).getStartDate();
                } else {
                    return cashFlow.getStartDate();
                }


                //Bond bond = (Bond) product;
                //try {
                //    CashFlowSet cfs = bond.generateFlows(new JDate());
                //    if(cfs != null){
                //        CashFlow cashFlow = getEnclosingCashFlow(cfs);
                //        return cashFlow!=null ? cashFlow.getStartDate() : null;
                //    }
                //} catch (FlowGenerationException e) {
                //    Log.error(this.getClass().getSimpleName(), "Could not generate CashFlows for product Bond with id: "+ product.getId(), e);
                //}
            } else if (BOND_FLOW_FIELD_PMT_END.equals(columnName)) {
                if (cashFlow == null){
                    return null;
                }
                if (cashFlow instanceof CashFlowCoupon) {
                    return ((CashFlowCoupon) cashFlow).getEndDate();
                } else if (cashFlow instanceof CashFlowOptionCoupon) {
                    return ((CashFlowOptionCoupon) cashFlow).getEndDate();
                } else {
                    return cashFlow.getEndDate();
                }


                //Bond bond = (Bond) product;
                //try {
                //    CashFlowSet cfs = bond.generateFlows(new JDate());
                //    if(cfs != null){
                //        CashFlow cashFlow = getEnclosingCashFlow(cfs);
                //        return cashFlow!=null ? cashFlow.getEndDate() : null;
                //    }
                //} catch (FlowGenerationException e) {
                //    Log.error(this.getClass().getSimpleName(), "Could not generate CashFlows for product Bond with id: "+ product.getId(), e);
                //}
            } else if (BOND_FLOW_FIELD_PMT_DT.equals(columnName)) {
                if (cashFlow == null){
                    return null;
                }
                if (cashFlow instanceof CashFlowCoupon) {
                    return ((CashFlowCoupon) cashFlow).getDate();
                } else if (cashFlow instanceof CashFlowOptionCoupon) {
                    return ((CashFlowOptionCoupon) cashFlow).getDate();
                } else {
                    return cashFlow.getDate();

                }


                //Bond bond = (Bond) product;
                //try {
                //    CashFlowSet cfs = bond.generateFlows(new JDate());
                //    if(cfs != null){
                //        CashFlow cashFlow = getEnclosingCashFlow(cfs);
                //        return cashFlow!=null ? cashFlow.getDate() : null;
                //    }
                //} catch (FlowGenerationException e) {
                //    Log.error(this.getClass().getSimpleName(), "Could not generate CashFlows for product Bond with id: "+ product.getId(), e);
                //}
            } else if (BOND_FLOW_FIELD_EX_DIVIDEND.equals(columnName)) {
                if (cashFlow == null){
                    return null;
                }
                if (cashFlow instanceof CashFlowCoupon) {
                    return ((CashFlowCoupon) cashFlow).getExDividendDate();
                } else if (cashFlow instanceof CashFlowOptionCoupon) {
                    return ((CashFlowOptionCoupon) cashFlow).getExDividendDate();
                }


                //Bond bond = (Bond) product;
                //try {
                //    CashFlowSet cfs = bond.generateFlows(new JDate());
                //    if(cfs != null){
                //        CashFlow cashFlow = getEnclosingCashFlow(cfs);
                //        return cashFlow!=null ? cashFlow.getEndDate() : null;
                //    }
                //} catch (FlowGenerationException e) {
                //    Log.error(this.getClass().getSimpleName(), "Could not generate CashFlows for product Bond with id: "+ product.getId(), e);
                //}
            } else if (BOND_FLOW_FIELD_PMT_AMT.equals(columnName)) {
                if (cashFlow == null){
                    return null;
                }
                if (cashFlow instanceof CashFlowCoupon) {
                    return ((CashFlowCoupon) cashFlow).getAmount();
                } else if (cashFlow instanceof CashFlowOptionCoupon) {
                    return ((CashFlowOptionCoupon) cashFlow).getAmount();
                } else {
                    return cashFlow.getAmount();
                }

            }
            //Bond bond = (Bond) product;
            //try {
            //    CashFlowSet cfs = bond.generateFlows(new JDate());
            //    if(cfs != null){
            //        CashFlow cashFlow = getEnclosingCashFlow(cfs);
            //        if(cashFlow != null) {
            //            PricingEnv env = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("DirtyPrice");
            //            cashFlow.calculate(cfs, env.getQuoteSet(), getValDate(row));
            //            return cashFlow != null ? cashFlow.getAmount() : null;
            //        }
            //        return null;
            //    }
            //} catch (FlowGenerationException | CalypsoServiceException e) {
            //    Log.error(this.getClass().getSimpleName(), "Could not generate CashFlows for product Bond with id: "+ product.getId(), e);
            //}
            else if (BOND_FLOW_FIELD_NOTIONAL.equals(columnName)) {
                if (cashFlow == null){
                    return null;
                }
                if (cashFlow instanceof CashFlowCoupon) {
                    return ((CashFlowCoupon) cashFlow).getNotional();
                } else if (cashFlow instanceof CashFlowOptionCoupon) {
                    return ((CashFlowOptionCoupon) cashFlow).getNotional();
                }

                return null;

                //Bond bond = (Bond) product;
                //return bond.getTotalIssued();
            } else if (BOND_FLOW_FIELD_RATE.equals(columnName)) {
                if (cashFlow == null){
                    return null;
                }
                if (cashFlow instanceof CashFlowCoupon) {
                    return ((CashFlowCoupon) cashFlow).getRate() * 100;
                } else if (cashFlow instanceof CashFlowOptionCoupon) {
                    return ((CashFlowOptionCoupon) cashFlow).getRate() * 100;
                }


            } else if (BOND_FLOW_FIELD_SPREAD.equals(columnName)) {
                if (cashFlow == null){
                    return null;
                }
                if (cashFlow instanceof CashFlowCoupon) {
                    return ((CashFlowCoupon) cashFlow).getSpread() * 100;
                } else if (cashFlow instanceof CashFlowOptionCoupon) {
                    return ((CashFlowOptionCoupon) cashFlow).getSpread() * 100;
                }


                //Bond bond = (Bond) product;
                //if(!bond.getFixedB()) {
                //    double spread = bond.getRateIndexSpread();
                //    return spread != 0 ? spread*100 : "0";
                //}
                //return "";
            } else if (BOND_FLOW_FIELD_RESET.equals(columnName)) {
                if (cashFlow == null){
                    return null;
                }
                if (cashFlow instanceof CashFlowCoupon) {
                    return ((CashFlowCoupon) cashFlow).getResetDate();
                } else if (cashFlow instanceof CashFlowOptionCoupon) {
                    return ((CashFlowOptionCoupon) cashFlow).getResetDate();
                }

                //Bond bond = (Bond) product;
                //if(!bond.getFixedB()) {
                //    try {
                //        CashFlowSet cfs = bond.generateFlows(new JDate());
                //        if(cfs != null){
                //            CashFlow cashFlow = getEnclosingCashFlow(cfs);
                //            return cashFlow!=null ? cashFlow.getStartDate().addBusinessDays((-1)*bond.getResetDays(),bond.getResetHolidays()) : null;
                //        }
                //    } catch (FlowGenerationException e) {
                //        Log.error(this.getClass().getSimpleName(), "Could not generate CashFlows for product Bond with id: "+ product.getId(), e);
                //    }
                //}
                //return "";
            } else if (BOND_FLOW_FIELD_POOL_FACTOR.equals(columnName)) {
               /*
                Log.system("BondInventario","Extracting Pool Factor from Bond");
                CashFlow cashFlow = getFlows(bond, row);
                if (cashFlow instanceof CashFlowCoupon) {
                    return cashFlow != null ? ((CashFlowCoupon) cashFlow).getPoolFactor() : null;
                } else if (cashFlow instanceof CashFlowOptionCoupon) {
                    return cashFlow != null ? ((CashFlowOptionCoupon) cashFlow).getPoolFactor() : null;
                }
                return null;*/
               return bond.getCurrentFactor(this.getValDate(row));

            }

        }
        if ((product == null) || (product.getCustomData() == null)) {
            return null;
        }

        if (product instanceof Bond) {
            // get custom data
            BondCustomData bondCustomData = (BondCustomData) product.getCustomData();
            // process rows of the report
            if (HAIRCUT_ECB.equals(columnName)) {
                return format(bondCustomData.getHaircut_ecb(), 5);
            } else if (HAIRCUT_SWISS.equals(columnName)) {
                return format(bondCustomData.getHaircut_swiss(), 5);
            } else if (HAIRCUT_BOE.equals(columnName)) {
                return format(bondCustomData.getHaircut_boe(), 5);
            } else if (HAIRCUT_FED.equals(columnName)) {
                return format(bondCustomData.getHaircut_fed(), 5);
            } else if (HAIRCUT_EUREX.equals(columnName)) {
                return format(bondCustomData.getHaircut_eurex(), 5);
            } else if (ACTIVE_AVAILABLE_QTY.equals(columnName)) {
                if (bondCustomData.getActive_available_qty() != null) {
                    return format(bondCustomData.getActive_available_qty(), 5);
                }
            } else if (RATE.equals(columnName)) {
                if (bondCustomData.getFee() != null) {
                    return format(bondCustomData.getFee(), 5);
                }
            } else if (QUANTITY_ON_LOAN.equals(columnName)) {
                return format(bondCustomData.getQty_on_loan(), 5);
            } else if (EXP_DATE_TYPE.equals(columnName)) {
                return bondCustomData.getExpired_date_type();
            } else if (EXP_DATE.equals(columnName)) {
                return bondCustomData.getExpired_date();
            } else if (LAST_UPDATE.equals(columnName)) {
                return bondCustomData.getLast_update();
            } else if (SANT_EX_DATE_COUPON.equals(columnName)) {
                Bond bond = (Bond) product;
                JDate nextCouponDate = bond.getNextCouponDate(getValDate(row));
                if ((nextCouponDate != null) && nextCouponDate.after(JDate.getNow())) {
                    int exDividendDays = (bond.getExdividendDays() <= 0) ? 0 : bond.getExdividendDays();
                    boolean exdividendDayBusB = bond.getExdividendDayBusB();
                    if (exdividendDayBusB) {
                        return nextCouponDate.addBusinessDays(-1 * exDividendDays, bond.getHolidays());
                    } else {
                        return nextCouponDate.addDays(-1 * exDividendDays);
                    }
                }
            }
        }
        if (product instanceof Equity) {
            // get custom data
            EquityCustomData bondCustomData = (EquityCustomData) product.getCustomData();
            // process rows of the report
            if (ACTIVE_AVAILABLE_QTY.equals(columnName)) {
                return format(bondCustomData.getActive_available_qty(), 5);
            } else if (RATE.equals(columnName)) {
                return format(bondCustomData.getFee(), 5);
            } else if (QUANTITY_ON_LOAN.equals(columnName)) {
                return format(bondCustomData.getQty_on_loan(), 5);
            } else if (EXP_DATE_TYPE.equals(columnName)) {
                return bondCustomData.getExpired_date_type();
            } else if (EXP_DATE.equals(columnName)) {
                return bondCustomData.getExpired_date();
            } else if (LAST_UPDATE.equals(columnName)) {
                return bondCustomData.getLast_update();
            }
        }

        // column not found
        return null;
    }

    private JDate getValDate(ReportRow row) {
        JDatetime valDatetime = (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME);
        if (valDatetime == null) {
            valDatetime = new JDatetime();
        }
        JDate valDate = null;
        PricingEnv env = (PricingEnv) row.getProperty(ReportRow.PRICING_ENV);
        if (env != null) {
            valDate = valDatetime.getJDate(env.getTimeZone());
        } else {
            valDate = valDatetime.getJDate(TimeZone.getDefault());
        }
        return valDate;
    }

    private Amount format(Double value, int decimalDigits) {
        if (value == null) {
            return null;
        } else {
            return new Amount(value, decimalDigits);
        }
    }

    public Product getProduct(ReportRow row) {
        Product product = (Product) row.getProperty(ReportRow.PRODUCT);
        if (product != null) {
            return product;
        }

        Inventory inventory = (Inventory) row.getProperty(ReportRow.INVENTORY);
        if ((inventory != null) && (inventory instanceof InventorySecurityPosition)) {
            int securityId = ((InventorySecurityPosition) inventory).getSecurityId();
            return BOCache.getExchangedTradedProduct(DSConnection.getDefault(), securityId);
        }

        return null;

    }

    boolean isBond(Product product) {
        if ((product != null) && (product instanceof Bond)) {
            return true;
        } else {
            return false;
        }
    }

    boolean isEquity(Product product) {
        if ((product != null) && (product instanceof Equity)) {
            return true;
        } else {
            return false;
        }
    }


    private CashFlow getEnclosingCashFlow(CashFlowSet cashFlowSet) {
        CashFlow cashFlow = (CashFlow) cashFlowSet.findEnclosingCashFlow(JDate.getNow(), CashFlow.INTEREST);
        if (cashFlow == null) {
            cashFlow = (CashFlow) cashFlowSet.findEnclosingCashFlow(JDate.getNow(), CashFlow.PRINCIPAL);
        }
        return cashFlow;
    }

    private CashFlow getFlows(Product product, ReportRow row) {
        if (row.getProperty("CASHFLOWS") != null) return row.getProperty("CASHFLOWS");

        Bond bond = (Bond) product;
        if (bond.getFlows() == null) {
            CashFlowSet cfs = null;
            try {
                cfs = bond.generateFlows(new JDate());
                if (cfs != null) {
                    CashFlow cashFlow = getEnclosingCashFlow(cfs);
                    if (cashFlow != null) {
                        PricingEnv env = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("DirtyPrice");
                        cashFlow.calculate(cfs, env.getQuoteSet(), getValDate(row));
                        row.setProperty("CASHFLOWS", cashFlow);
                        return cashFlow;
                    }
                }
            } catch (FlowGenerationException e) {
                e.printStackTrace();
            } catch (CalypsoServiceException e) {
                e.printStackTrace();
            }

        } else {
            CashFlow cashFlow = getEnclosingCashFlow(bond.getFlows());
            if (cashFlow != null) {
                try {
                    PricingEnv env = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("DirtyPrice");
                    cashFlow.calculate(bond.getFlows(), env.getQuoteSet(), getValDate(row));
                    row.setProperty("CASHFLOWS", cashFlow);
                    return cashFlow;
                } catch (FlowGenerationException | CalypsoServiceException e) {
                    e.printStackTrace();
                }
                return cashFlow;
            }
        }
        return null;
    }

}
