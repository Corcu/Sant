/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.collateral.MarginCallDetailEntry;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.util.SantEquitySwapUtil;
import calypsox.util.collateral.CollateralUtilities;

public class MarginCallDetailReportStyle extends com.calypso.tk.report.MarginCallDetailReportStyle {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public final static String ABS_POOL_FACTOR_TODAY = com.calypso.tk.report.BondReportStyle.ABS_POOL_FACTOR_TODAY;

    // Dynamic margin columns
    public final static String NET_EXPOSURE = "Net Exposure";
    private final static String DM_PREFIX = "DM ";
    public final static String DM_PERCENTAGE = DM_PREFIX + "Percentage";
    public final static String DM_NPV_LEG1 = DM_PREFIX + "Npv Leg1";
    public final static String DM_NPV_LEG2 = DM_PREFIX + "Npv Leg2";
    public final static String DM_NPV = DM_PREFIX + "Npv";
    public final static String DM_NPV_BASE = DM_PREFIX + "Npv Base";
    public final static String DM_VM = DM_PREFIX + "VM";
    public final static String DM_IA = DM_PREFIX + "IA";
    public final static String DM_IA_BASE = DM_PREFIX + "IA Base";
    public final static String DM_IA_SETTLE = DM_PREFIX + "IA Settle";

    public final static String DM_IA_SETTLE_BASE = DM_PREFIX + "IA Settle Base";
    public final static String DM_NET_BALANCE = DM_PREFIX + "Net Balance";

    private final static String CORE_PREFIX = "Core ";
    public final static String CORE_NPV = CORE_PREFIX + "Npv";
    public final static String CORE_NPV_BASE = CORE_PREFIX + "Npv Base";
    public final static String CORE_NPV_LEG1 = CORE_PREFIX + "Npv Leg1";
    public final static String CORE_NPV_LEG2 = CORE_PREFIX + "Npv Leg2";
    public final static String CORE_EXPOSURE = CORE_PREFIX + "Exposure";
    public final static String CORE_EXP_DIFF = CORE_PREFIX + "Exposure Diff.";
    public final static String NET_COLLATERAL_BALANCE = "Net Collateral Balance";
    public final static String CLOSING_PRICE = "Closing Price";

    private final static String SAN_PRICING_ENV_CORE_CALC = "SAN_PRICING_ENV_CORE_CALC";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        MarginCallDetailEntry mcde = row.getProperty(ReportRow.MARGIN_CALL_DETAIL_ENTRY);
        MarginCallEntry mce = row.getProperty(ReportRow.MARGIN_CALL_ENTRY);

        if (columnId.equals(ABS_POOL_FACTOR_TODAY)) {
            if (mcde.getProductType().equals(Repo.REPO)) {
                Product product = null;
                Trade trade = null;
                try {
                    product = DSConnection.getDefault().getRemoteProduct().getProduct(mcde.getProductId());
                    trade = DSConnection.getDefault().getRemoteTrade().getTrade(mcde.getTradeId());
                } catch (RemoteException re) {
                    Log.error(this, "Cannot load the product/trade linked to the MarginCallDetailEntry " + mcde.getId()
                            + " : " + re.getMessage());
                }
                return getABSPoolFactor(product, JDate.getNow(), trade);
            } else {
                return null;
            }
        } else if (CLOSING_PRICE.equals(columnId)){
            JDatetime valDateTime = row.getProperty("ValuationDatetime");
            Trade trade = null;
            try {
                trade = DSConnection.getDefault().getRemoteTrade().getTrade(mcde.getTradeId());
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
            return CollateralUtilities.formatAmount(getQuotes(trade, valDateTime), null);
        } else if (PRODUCT_TYPE.equals(columnId) || BondReportStyle.PRODUCT_SUBTYPE.equals(columnId)) {
            if (isBond(mcde)) {
                return "BondForward";
            } else if (isEquityCO2(mcde)) {
                return "EquityCO2";
            } else if (isEquityVCO2(mcde)) {
                return "EquityVCO2";
            }
        } else if (MarginCallDetailReportStyle.START_DATE.equals(columnId)) {
            if (isBond(mcde) || isEquityCO2(mcde) || isEquityVCO2(mcde)) {
                return super.getColumnValue(row, MarginCallDetailReportStyle.SETTLE_DATE, errors);
            }
        } else if (columnId.startsWith(DM_PREFIX)
                || columnId.startsWith(CORE_PREFIX)
                || NET_EXPOSURE.equals(columnId) || NET_COLLATERAL_BALANCE.equals(columnId)) {
            try {
                double marginCallValue = mcde.getMarginCallValue();
                double independentAmount = mcde.getIndependentAmount() != null
                        ? !Double.isNaN(mcde.getIndependentAmount().getValue()) ? mcde.getIndependentAmount().getValue() : 0d : 0d;
                switch (columnId) {
                    case NET_EXPOSURE:
                        Amount vm_dm = getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_VARIATION_MARGIN_DM);
                        Amount ia_settle_base = getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_IA_SETTLEMENT_BASE_DM);
                        double vm_dm_value = vm_dm != null ? vm_dm.get() : 0.0d;
                        double ia_settle_base_value = ia_settle_base != null ? ia_settle_base.get() : 0.0d;
                        return new Amount(vm_dm_value + ia_settle_base_value, 2);
                    case DM_PERCENTAGE:
                        return getIACustomLeverage(mcde);
                    case DM_NPV:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_NPV_DM);
                    case DM_NPV_BASE:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_NPV_BASE_DM);
                    case DM_NPV_LEG1:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_NPV_LEG_1_DM);
                    case DM_NPV_LEG2:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_NPV_LEG_2_DM);
                    case DM_VM:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_VARIATION_MARGIN_DM);
                    case DM_IA:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_INDEPENDENT_AMOUNT_DM);
                    case DM_IA_BASE:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE_DM);
                    case DM_IA_SETTLE:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_IA_SETTLEMENT_DM);
                    case DM_IA_SETTLE_BASE:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_IA_SETTLEMENT_BASE_DM);
                    case DM_NET_BALANCE:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_MARGIN_CALL_DM);
                    case CORE_NPV:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_NPV);
                    case CORE_NPV_BASE:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_NPV_BASE);
                    case CORE_NPV_LEG1:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_NPV_LEG1);
                    case CORE_NPV_LEG2:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_NPV_LEG2);
                    case CORE_EXPOSURE:
                        return getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_MARGIN_CALL);
                    case CORE_EXP_DIFF:
                        Amount coreVal = getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_MARGIN_CALL);
                        return new Amount((marginCallValue + independentAmount) - (coreVal != null ? coreVal.get() : 0d), 2);
                    case NET_COLLATERAL_BALANCE:
                        Amount independentAmountBaseDM = getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE_DM);
                        Amount dmVM = getMainPLMarkValueAmount(mce, mcde, SantPricerMeasure.S_VARIATION_MARGIN_DM);
                        return independentAmountBaseDM != null && dmVM != null ? new Amount(independentAmountBaseDM.get() + dmVM.get(), 2) : null;
                }
            } catch (Exception e) {
                Log.error(this, e);
                errors.add(e.getMessage());
            }
        }
        Object ret=super.getColumnValue(row, columnId, errors);
        if(ret instanceof Book) {
			return ret.toString();
        }
        return ret;

    }

    protected Object getABSPoolFactor(Product product, JDate date, Trade trade) {
        Repo repo = null;
        if (product instanceof Repo) {
            repo = (Repo) product;
            if (repo.getSecurity() != null) {
                return BondReportStyle.getABSPoolFactor(repo.getSecurity(), date);
            }
        }
        return null;
    }

    protected String getPoolFactorType(Product product) {
        Repo repo = null;
        SecLending secLending = null;
        if (product instanceof Repo) {
            repo = (Repo) product;
            if (repo.getSecurity() instanceof BondAssetBacked) {
                BondAssetBacked bab = (BondAssetBacked) repo.getSecurity();
                return bab.getPoolFactorType();
            }
        } else if (product instanceof SecLending) {
            secLending = (SecLending) product;
            if (secLending.getSecurity() instanceof BondAssetBacked) {
                BondAssetBacked bab = (BondAssetBacked) secLending.getSecurity();
                return bab.getPoolFactorType();
            }
        } else {
            if (product instanceof BondAssetBacked) {
                return ((BondAssetBacked) product).getPoolFactorType();
            }
        }
        return "";
    }

    private boolean isBond(MarginCallDetailEntry mcde) {
        return Optional.ofNullable(mcde)
                .map(MarginCallDetailEntry::getProductType)
                .map(type -> type.equals(Bond.class.getSimpleName()))
                .orElse(false);
    }


    private boolean isEquityCO2(MarginCallDetailEntry mcde) {
        Boolean equityCO2 = false;
        Product product = null;
        try {
            product = DSConnection.getDefault().getRemoteProduct().getProduct(mcde.getProductId());
        } catch (RemoteException re) {
            Log.error(this, "Cannot load the product linked to the MarginCallDetailEntry " + mcde.getId() + " : " + re.getMessage());
        }
        if (product instanceof Equity && "CO2".equalsIgnoreCase(product.getSecCode("EQUITY_TYPE"))) {
            equityCO2 = true;
        }
        return equityCO2;
    }

    private boolean isEquityVCO2(MarginCallDetailEntry mcde) {
        Boolean equityCO2 = false;
        Product product = null;
        try {
            product = DSConnection.getDefault().getRemoteProduct().getProduct(mcde.getProductId());
        } catch (RemoteException re) {
            Log.error(this, "Cannot load the product linked to the MarginCallDetailEntry " + mcde.getId() + " : " + re.getMessage());
        }
        if (product instanceof Equity && "VCO2".equalsIgnoreCase(product.getSecCode("EQUITY_TYPE"))) {
            equityCO2 = true;
        }
        return equityCO2;
    }

    private Amount getIACustomLeverage(MarginCallDetailEntry mcde) throws Exception {
        Trade t = DSConnection.getDefault().getRemoteTrade().getTrade(mcde.getTradeId());
        if (t != null) {
            SantEquitySwapUtil.LegInfo legInfo = SantEquitySwapUtil.extractIsinLeg(t);
            if (legInfo != null && t.getCounterParty() != null) {
                return new Amount(SantEquitySwapUtil.getPercentage(legInfo, t.getCounterParty().getId()));
            }
        }
        return null;
    }


    private Optional<PLMark> getMainPLMark(MarginCallEntry mce, MarginCallDetailEntry mcde) throws Exception {
        String pricingEnvName = mce.getCollateralConfig().getPricingEnvName();
        return getRelatedPLMark(mce, mcde, pricingEnvName);
    }

    private Optional<PLMark> getRelatedPLMark(MarginCallEntry mce, MarginCallDetailEntry mcde, String pricingEnv) throws Exception {
        return !Util.isEmpty(pricingEnv)
                ? Optional.ofNullable(DSConnection.getDefault().getRemoteMark()
                .getMark("PL", mcde.getTradeId(), null, pricingEnv, mce.getValueDate()))
                : Optional.empty();
    }

    private Optional<PLMarkValue> getMainPLMarkValue(MarginCallEntry mce, MarginCallDetailEntry mcde, String markValueName) throws Exception {
        return getMainPLMark(mce, mcde)
                .map(plMark -> plMark.getPLMarkValueByName(markValueName));
    }

    private Amount getMainPLMarkValueAmount(MarginCallEntry mce, MarginCallDetailEntry mcde, String markValueName) throws Exception {
        return getMainPLMarkValue(mce, mcde, markValueName)
                .map(value -> new Amount(value.getMarkValue(), 2))
                .orElse(null);
    }

    private double getQuotes(final Trade trade, JDatetime valDateTime) {
        Product product = trade.getProduct();

        String quoteName = "";
        String quoteType = "";
        String quoteSetName = "";
        if (!(product instanceof SecFinance)){
            return 0.00;
        }

        quoteName = ((SecFinance) product).getSecurity().getQuoteName();
        quoteType = ((SecFinance) product).getSecurity().getQuoteType();
        quoteSetName = ((SecFinance) product).getSecurity().getType().equals("Bond") ? "DirtyPrice" : "OFFICIAL";

        if (quoteName == null || quoteType == null) {
            return 0.00;
        }

        QuoteValue quoteValue = new QuoteValue();
        quoteValue.setName(quoteName);
        quoteValue.setDate(valDateTime.getJDate(TimeZone.getDefault()));
        quoteValue.setQuoteType(quoteType);
        quoteValue.setQuoteSetName(quoteSetName);

        try {
            quoteValue = DSConnection.getDefault().getRemoteMarketData().getQuoteValue(quoteValue);
        } catch (CalypsoServiceException e) {
            Log.error(this, e);
            return 0.00;
        }

        return quoteValue != null ? quoteValue.getClose() : 0.00;
    }
}