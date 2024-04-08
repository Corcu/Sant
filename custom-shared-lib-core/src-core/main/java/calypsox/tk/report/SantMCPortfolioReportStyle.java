package calypsox.tk.report;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.report.quotes.FXQuoteHelper;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.MarginCallDetailEntryDTOReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

/**
 * ReportStyle class for the portfolio valuation report
 *
 * @author aela
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SantMCPortfolioReportStyle extends MarginCallDetailEntryDTOReportStyle {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // public static final String COLLATERAL_AGREE = "Collateral Agree";
    // public static final String COLLATERAL_AGREE_TYPE =
    // "Collateral Agree Type";
    // public static final String COUNTERPARTY = "CounterParty";
    // public static final String TRADE_ID = "Trade id";
    // public static final String FRONT_OFFICE_ID = "Front office id";
    // public static final String CLOSE_OF_BUSINESS = "Close of business";
    // public static final String STRUCTURE = "Strcuture";
    // public static final String TRADE_DATE = "Trade Date";
    // public static final String VALUE_DATE = "Value Date";
    // public static final String MATURITY_DATE = "Maturity Date";
    // public static final String VALUATION_AGENT = "Valuation Agent";
    // public static final String PORTFOLIO = "Portfolio";
    // public static final String OWNER = "Owner";
    // public static final String DEAL_OWNER = "Deal Owner";
    // public static final String INSTRUMENT = "Instrument";
    // public static final String UNDERLYING = "Underlying";
    // public static final String PRINCIPAL = "Principal";
    // public static final String PRINCIPAL_CCY = "Principal ccy";
    // public static final String INDEPENDENT_AMOUNT = "Independent Amount";
    // public static final String BUY_SELL = "Buy Sell";
    // public static final String BASE_CCY = "Base ccy";
    // public static final String MTM_BASE_CCY = "MTM base ccy";
    // public static final String PRINCIPAL_2 = "Principal 2";
    // public static final String PRINCIPAL_CCY_2 = "Principal 2 CCY";
    // public static final String RATE_2 = "Rate 2";
    // public static final String RATE = "Rate";
    public static final String COLLATERAL_AGREE = "COLLATERAL_AGREE";
    public static final String COLLATERAL_AGREE_TYPE = "COLLATERAL_AGREE_TYPE";
    public static final String COUNTERPARTY = "COUNTERPARTY";
    public static final String TRADE_ID = "TRADE_ID";
    public static final String FRONT_OFFICE_ID = "FRONT_OFFICE_ID";
    public static final String CLOSE_OF_BUSINESS = "CLOSE_OF_BUSINESS";
    public static final String STRUCTURE = "STRUCTURE";
    public static final String TRADE_DATE = "TRADE_DATE";
    public static final String VALUE_DATE = "VALUE_DATE";
    public static final String MATURITY_DATE = "MATURITY_DATE";
    public static final String VALUATION_AGENT = "VALUATION_AGENT";
    public static final String PORTFOLIO = "PORTFOLIO";
    public static final String OWNER = "OWNER";
    public static final String DEAL_OWNER = "DEAL_OWNER";
    public static final String INSTRUMENT = "INSTRUMENT";
    public static final String UNDERLYING = "UNDERLYING";
    public static final String PRINCIPAL = "PRINCIPAL";
    public static final String PRINCIPAL_CCY = "PRINCIPAL_CCY";
    public static final String INDEPENDENT_AMOUNT = "INDEPENDENT_AMOUNT";
    public static final String BUY_SELL = "BUY_SELL";
    public static final String BASE_CCY = "BASE_CCY";
    public static final String MTM_BASE_CCY = "MTM_BASE_CCY";
    public static final String PRINCIPAL_2 = "PRINCIPAL_2";
    public static final String PRINCIPAL_CCY_2 = "PRINCIPAL_CCY_2";
    public static final String RATE_2 = "RATE_2";
    public static final String RATE = "RATE";
    public static final String SANT_EXPOSURE = "Sant Exposure";

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.report.MarginCallDetailEntryDTOReportStyle#getColumnValue (com.calypso.tk.report.ReportRow,
     * java.lang.String, java.util.Vector)
     */
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        if (row == null) {
            return null;
        }
        if (columnName == null) {
            return null;
        }

        MarginCallDetailEntryDTO detailEntryDTO = (MarginCallDetailEntryDTO) row.getProperty(ReportRow.DEFAULT);
        if (detailEntryDTO == null) {
            return null;
        }

        MarginCallConfig config = (MarginCallConfig) row.getProperty("MARGIN_CALL_CONFIG");
        if (config == null) {
            return null;
        }

        Trade trade = (Trade) row.getProperty("TRADE");
        if (trade == null) {
            return null;
        }

        PricingEnv pricingEnv = (PricingEnv) row.getProperty("PRICING_ENV");
        FXQuoteHelper.setPricingEnv(pricingEnv);

        Product product = trade.getProduct();
        boolean isCollateralExposure = (product instanceof CollateralExposure);

        if (columnName.equals(COLLATERAL_AGREE)) {
            return config.getName();
        } else if (columnName.equals(COLLATERAL_AGREE_TYPE)) {
            return config.getContractType();
        } else if (columnName.equals(COUNTERPARTY)) {
            return trade.getCounterParty().getName();
        } else if (columnName.equals(TRADE_ID)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            return trade.getLongId();
        } else if (columnName.equals(FRONT_OFFICE_ID)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            return trade.getExternalReference();
        } else if (columnName.equals(CLOSE_OF_BUSINESS)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            return detailEntryDTO.getValueDatetime().getJDate(TimeZone.getDefault());
        } else if (columnName.equals(STRUCTURE)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            return trade.getKeywordValue("STRUCTURE_ID");
        } else if (columnName.equals(TRADE_DATE)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            return detailEntryDTO.getTradeDate().getJDate(TimeZone.getDefault());
        } else if (columnName.equals(VALUE_DATE)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            return detailEntryDTO.getSettleDate();
        } else if (columnName.equals(MATURITY_DATE)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            if (trade.getMaturityDate() == null) {
                return "Open";
            } else {
                return trade.getMaturityDate();
            }
        } else if (columnName.equals(VALUATION_AGENT)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            String valuationAgent = config.getProcessingOrg().getName();
            LegalEntity valAgent = BOCache.getLegalEntity(DSConnection.getDefault(), config.getValuationAgentId());
            if (valAgent != null) {
                valuationAgent = valAgent.getName();
            }
            return valuationAgent;
        } else if (columnName.equals(PORTFOLIO)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            return trade.getBook().getName();
        } else if (columnName.equals(OWNER)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            return config.getProcessingOrg().getName();
        } else if (columnName.equals(DEAL_OWNER)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }
            return trade.getBook().getLegalEntity().getName();
        } else if (columnName.equals(INSTRUMENT)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "";
            }

            if (isCollateralExposure) {
                return trade.getProductSubType();
            } else {
                return trade.getProductType();
            }
            //
            // return trade.getProductSubType();
        } else if (columnName.equals(UNDERLYING)) {
            if ("CONTRACT_IA".equals(trade.getProductSubType())) {
                return "Legal Agreement Independent Amount";
            }
            return getUnderlyingName(product);
            // String underlyingName = null;
            // if (isCollateralExposure) {
            // underlyingName = (String) ((CollateralExposure) product)
            // .getAttribute(TradeInterfaceUtils.COL_CTX_PROP_UNDERLYING);
            // if (Util.isEmpty(underlyingName)) {
            // underlyingName = (String) ((CollateralExposure) product)
            // .getAttribute(TradeInterfaceUtils.COL_CTX_PROP_UNDERLYING_1);
            // }
            // return underlyingName;
            // }
            //
            // Product underlying = trade.getProduct().getUnderlyingProduct();
            // return (underlying == null ? "" : underlying.getSubType());
            // Product underlying = trade.getProduct().getUnderlyingProduct();
            // String underlyingName = (underlying == null ? "" : underlying.getSubType());
            // return underlyingName;

        } else if (columnName.equals(PRINCIPAL)) {
            if (product != null) {
                if (product instanceof Repo) {
                    return ((Repo) product).getNominal(detailEntryDTO.getProcessDatetime().getJDate(TimeZone.getDefault()));
                } else if (product instanceof SecLending) {
                    return ((SecLending) product).getSecuritiesNominalValue(detailEntryDTO.getProcessDatetime()
                            .getJDate(TimeZone.getDefault()));
                } else if (isCollateralExposure) {
                    return ((CollateralExposure) product).getPrincipal();
                }
                return 0;
            }
        } else if (columnName.equals(PRINCIPAL_2)) {
            if (isCollateralExposure) {
                return ((CollateralExposure) product).getAttribute(TradeInterfaceUtils.COL_CTX_PROP_NOMINAL_2);
            }
            return null;
        } else if (columnName.equals(PRINCIPAL_CCY)) {
            return getColumnValue(detailEntryDTO, CURRENCY, errors);
        } else if (columnName.equals(PRINCIPAL_CCY_2)) {
            if (isCollateralExposure) {
                return ((CollateralExposure) product).getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_2);
            }
            return null;
        } else if (columnName.equals(INDEPENDENT_AMOUNT)) {
            PricerMeasure pm = detailEntryDTO.getIndependentAmount();
            return getPricerMeasureValue(pm);
        } else if (columnName.equals(BUY_SELL)) {
            if (product != null) {
                if (product instanceof Repo) {
                    return ((Repo) product).getDirection();
                } else if (product instanceof SecLending) {
                    return ((SecLending) product).getDirection();
                } else if (isCollateralExposure) {
                    return ((CollateralExposure) product).getDirection(trade);
                }
                return null;
            }
        } else if (columnName.equals(BASE_CCY)) {
            return config.getCurrency();
            // PricerMeasure pm =
            // detailEntryDTO.getMeasure(PricerMeasure.MARGIN_CALL);
            // if (pm != null) {
            // return pm.getCurrency();
            // }
            // return null;
        } else if (columnName.equals(MTM_BASE_CCY)) {
            return getPLMarkValue(config, trade, detailEntryDTO, SantPricerMeasure.S_NPV_BASE, detailEntryDTO
                    .getValueDatetime().getJDate(TimeZone.getDefault()));
            // PricerMeasure pm = getPricerMeasureFromEntry(detailEntryDTO, SantPricerMeasure.S_NPV_BASE);
            // return getPricerMeasureValue(pm);
        } else if (columnName.equals(RATE)) {
            if (pricingEnv != null) {
                String ccy1 = trade.getTradeCurrency();
                // get the FX rate for the leg1 against the base currency
                if (isCollateralExposure) {
                    CollateralExposure pColCtx = (CollateralExposure) product;
                    ccy1 = (String) pColCtx.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_1);
                    if (Util.isEmpty(ccy1)) {
                        ccy1 = trade.getTradeCurrency();
                    }
                }
                try {
                    if (Util.isEmpty(ccy1)) {
                        return null;
                    }
                    if (config.getCurrency().equals(ccy1)) {
                        return CollateralUtilities.formatFXQuote(1.0d, ccy1, config.getCurrency());
                    }
                    QuoteValue quote = FXQuoteHelper.getMarketConventionFXQuote(ccy1, config.getCurrency(),
                            detailEntryDTO.getValueDatetime().getJDate(TimeZone.getDefault()));
                    if (quote != null) {
                        return CollateralUtilities.formatFXQuote(quote.getClose(), ccy1, config.getCurrency());
                    }
                } catch (MarketDataException e) {
                    Log.error(this, e);
                    return null;
                }
            }
            return null;

        } else if (columnName.equals(RATE_2)) {
            if ((pricingEnv != null) && isCollateralExposure) {
                String ccy2 = (String) ((CollateralExposure) product)
                        .getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_2);
                try {
                    if (Util.isEmpty(ccy2)) {
                        return null;
                    }
                    if (config.getCurrency().equals(ccy2)) {
                        return CollateralUtilities.formatFXQuote(1.0d, ccy2, config.getCurrency());
                    }
                    QuoteValue quote = FXQuoteHelper.getMarketConventionFXQuote(ccy2, config.getCurrency(),
                            detailEntryDTO.getValueDatetime().getJDate(TimeZone.getDefault()));
                    if (quote != null) {
                        return CollateralUtilities.formatFXQuote(quote.getClose(), ccy2, config.getCurrency());
                    }
                } catch (MarketDataException e) {
                    Log.error(this, e);
                    return null;
                }
            }
            return null;
        } else if (columnName.equals(SANT_EXPOSURE)) {

            Double currentPlMark = getPLMarkValue(config, trade, detailEntryDTO, SantPricerMeasure.S_NPV_BASE,
                    detailEntryDTO.getValueDatetime().getJDate(TimeZone.getDefault()));

            JDate previousValuationDate = Holiday.getCurrent().addBusinessDays(
                    detailEntryDTO.getValueDatetime().getJDate(TimeZone.getDefault()),
                    DSConnection.getDefault().getUserDefaults().getHolidays(), -1);

            Double previousPlMark = getPLMarkValue(config, trade, detailEntryDTO, SantPricerMeasure.S_NPV_BASE,
                    previousValuationDate);
            if ((currentPlMark != null) && (previousPlMark != null)) {
                return (currentPlMark - previousPlMark);
            }

            return null;
        }
        return null;
    }

    /**
     * @param pm
     * @return the value of the given pricer measure
     */
    private Double getPricerMeasureValue(PricerMeasure pm) {
        if (pm != null) {
            double pmv = pm.getValue();
            return (Double.isNaN(pmv) ? 0.0 : pmv);
        }
        return 0.0;
    }

    // private Double getFxRate(String ccy1, String ccy2, JDate valueDate) {
    // try {
    // if (Util.isEmpty(ccy2ccy1))
    // return null;
    // QuoteValue quote = princingEnv.getFXQuote(ccy1, config.getCurrency(),
    // valueDate);
    // if (quote != null && !Double.isNaN(quote.getClose())) {
    // return quote.getClose();
    // }
    // } catch (MarketDataException e) {
    // Log.error(this, e);
    // return null;
    // }
    // }

    /**
     * @param detailEntry
     * @param pmName
     * @return
     */
    @SuppressWarnings("unused")
    private PricerMeasure getPricerMeasureFromEntry(MarginCallDetailEntryDTO detailEntry, String pmName) {
        PricerMeasure pm = null;
        List<PricerMeasure> pms = detailEntry.getMeasures();
        if (!Util.isEmpty(pms) && !Util.isEmpty(pmName)) {
            for (PricerMeasure tmpPM : pms) {
                if (pmName.equals(tmpPM.getName())) {
                    pm = tmpPM;
                    break;
                }
            }
        }
        return pm;
    }

    /**
     * @param product
     * @return
     */
    private String getUnderlyingName(Product product) {

        if ((product instanceof CollateralExposure)) {
            CollateralExposure collatExpo = (CollateralExposure) product;
            if (CollateralUtilities.isTwoLegsProductType(((CollateralExposure) product).getUnderlyingType())) {
                return (String) collatExpo.getAttribute("UNDERLYING_1");
            } else {
                return (String) collatExpo.getAttribute("UNDERLYING");
            }
        } else if (product instanceof Repo) {
            try {
                Product secUnderlying = DSConnection.getDefault().getRemoteProduct()
                        .getProduct(((Repo) product).getUnderlyingSecurityId());
                return secUnderlying.getDescription();
            } catch (Exception e) {
                Log.error(this, "Cannot retrieve security", e);
            }
        } else if (product instanceof SecLending) {
            SecLending secLending = (SecLending) product;
            final Vector<Collateral> leftCollaterals = secLending.getLeftCollaterals();
            if (leftCollaterals.size() > 0) {
                return leftCollaterals.get(0).getDescription();
            }
        }
        return null;
    }

    private Double getPLMarkValue(MarginCallConfig config, Trade trade, MarginCallDetailEntryDTO detailEntryDTO,
                                  String pricerMeasure, JDate date) {
        PLMarkValue plMarkValue = null;
        try {
            plMarkValue = CollateralUtilities.retrievePLMarkValue(trade, DSConnection.getDefault(), pricerMeasure,
                    config.getPricingEnvName(), date);
        } catch (RemoteException e) {
            Log.error(this, e);
            return null;

        }
        if (plMarkValue != null) {
            return plMarkValue.getMarkValue();
        }

        return null;

    }
}
