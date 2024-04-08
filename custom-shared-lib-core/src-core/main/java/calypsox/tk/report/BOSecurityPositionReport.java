/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;
import com.calypso.tk.report.inventoryposition.InventoryKey;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.InventoryPositionArray;

import java.security.InvalidParameterException;
import java.util.*;

import static calypsox.tk.report.BOSecurityPositionReportTemplate.*;

public class BOSecurityPositionReport extends com.calypso.tk.report.BOSecurityPositionReport {

    private static final String LOG_CATEGORY = BOSecurityPositionReport.class.getName();

    private static final long serialVersionUID = 1373155839304048874L;

    // Constants
    protected static final String PRICING_ENV = "PricingEnvName";
    public static final String CORE_FLAG = "Core Flag";
    protected static final String EUR_NAME = "EUR";
    protected static final String BALANCE = "Balance";
    private static final String DIRTY_PRICE_STR = "DirtyPrice";

    private long top = 0L;

    @SuppressWarnings({"rawtypes"})
    @Override
    public ReportOutput load(Vector errorMsgs) {
        PricingEnv pricingEnv = Optional.ofNullable(getPricingEnv())
                .orElseGet(() -> {
                    PricingEnv defaultEnv=PricingEnv.loadPE(DIRTY_PRICE_STR, this.getValuationDatetime());
                    this._pricingEnv=defaultEnv;
                    return defaultEnv;
                });
        QuoteSet quoteSet = pricingEnv.getQuoteSet();

        top = System.currentTimeMillis();
        Log.system(LOG_CATEGORY, "Start core loading of inventory positions");
        DefaultReportOutput reportOutput = (DefaultReportOutput) super.load(errorMsgs);
        Log.system(LOG_CATEGORY, "End core loading of inventory positions. TIME=" + (System.currentTimeMillis() - top)
                + " in milliseconds");
        ReportRow[] rows = initReportRows(reportOutput);

        Vector holidays = getHolidays();
        JDate endDate = getEndDate(getReportTemplate(), getValDate());
        top = System.currentTimeMillis();
        Log.system(LOG_CATEGORY, "Start custom inventory positions enhancement");
        for (int i = 0; i < rows.length; i++) {

            ReportRow row = rows[i];

            Inventory inventory = row.getProperty(ReportRow.INVENTORY);
            row.setProperty(COLLATERAL_CONFIG_PROPERTY, getCollateralConfig(inventory));
            row.setProperty(PRICING_ENV_PROPERTY, pricingEnv);
            row.setProperty(QUOTE_SET_PROPERTY, quoteSet);
            row.setProperty(BOSecurityPositionReportStyle.HOLIDAYS, holidays);
            row.setProperty(REPORT_TEMPLATE_PROPERTY, getReportTemplate());
            row.setProperty(END_DATE_PROPERTY, endDate);
            row.setProperty(END_DATE_MINUS1_PROPERTY,
                    endDate.addBusinessDays(-1, row.getProperty(BOSecurityPositionReportStyle.HOLIDAYS)));
            Product product = inventory.getProduct();
            row.setProperty(BOSecurityPositionReportStyle.INV_PRODUCT, product);
            row.setProperty(BOSecurityPositionReportStyle.ROW_NUMBER, i + 1);

        }
        Log.system(LOG_CATEGORY, "Stop custom inventory positions enhancement. TIME="
                + (System.currentTimeMillis() - top) + " in milliseconds");
        return reportOutput;
    }

    private ReportRow[] initReportRows(DefaultReportOutput reportOutput) {
        ReportRow[] rows = new ReportRow[0];
        if (reportOutput != null) {
            rows = reportOutput.getRows();
        }
        return rows;
    }

    @Override
    public JDatetime getValuationDatetime() {
        // check if core flag is set
        if (checkCoreFlag()) {
            return super.getValuationDatetime();
        } else {
            JDatetime valDateTime = super.getValuationDatetime();
            if (valDateTime == null) {
                valDateTime = new JDatetime();
            }

            Vector<String> holidays = Util.string2Vector("SYSTEM");
            JDate valDate = valDateTime.getJDate(TimeZone.getDefault());
            valDate = valDate.addBusinessDays(-1, holidays);

            return new JDatetime(valDate, valDateTime.getField(Calendar.HOUR_OF_DAY),
                    valDateTime.getField(Calendar.MINUTE), valDateTime.getField(Calendar.SECOND),
                    TimeZone.getDefault());
        }
    }

    /**
     * Builds the equivalence of the CCY of the Product with the EUR fixing. Based
     * of this fixing, calculates value of market value in EUR
     *
     * @param row          with current position
     * @param quotesLoader with Fixing FX
     */
    private void buildMarketValueEURAndFXFix(ReportRow row, SantGenericQuotesLoader quotesLoader, Inventory inventory,
                                             final Product product) {

        Double marketValue = row.getProperty(MARKET_VALUE_PROPERTY);

        if (marketValue == null || inventory == null || product == null)
            return;

        // market value fixing for products which ccy is different to EUR
        if (product.getCurrency().equals(EUR_NAME)) {

            row.setProperty(FX_RATE_NAME_PROPERTY, EUR_NAME);
            row.setProperty(MARKET_VALUE_EUR_PROPERTY, marketValue);

        } else {

            // marketValue in EUR, we require the daily (D-1) fixing
            final QuoteValue qvFXfix = quotesLoader.fetchFXQuoteValue(EUR_NAME, product.getCurrency());
            if (QuoteValue.isNull(qvFXfix) || QuoteValue.isNull(qvFXfix.getClose())) {
                Log.error(this, "Not FX Fixing found for FX.EUR." + product.getCurrency());
                return;
            }

            final String fxFixing = buildFXName(product, qvFXfix);
            row.setProperty(FX_RATE_NAME_PROPERTY, fxFixing);

            // precio*nominal* D-1 FX.EUR fixing
            Double priceValueEur = marketValue * qvFXfix.getClose();

            row.setProperty(MARKET_VALUE_EUR_PROPERTY, priceValueEur);
        }
    }

    /**
     * @param product
     * @param qvFix
     * @return String as FX.EUR.CCY=xx.xx with the fixing of yesterday close
     */
    private String buildFXName(final Product product, final QuoteValue qvFix) {

        return "FX.EUR." + product.getCurrency() + "=" + qvFix.getClose();
    }

    /**
     * @param inventory
     * @return MC Contract
     */
    private CollateralConfig getCollateralConfig(final Inventory inventory) {

        if (inventory == null) {
            throw new InvalidParameterException("Invalid row. Cannot locate Inventory object");
        }
        return (inventory.getMarginCallConfigId() == 0) ? null
                : CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                inventory.getMarginCallConfigId());
    }

    /**
     * @return where to recover FX based on the price Env configured
     */
    private List<String> buildFXWhereClause() {

        final String priceEnvironment = getPriceEnvironment();
        final JDate quoteDate = getEndDate(getReportTemplate(), getValDate()).addBusinessDays(-1, getHolidays());
        // two calls might seem weird: in case FX are in other env than OFFICIAL we
        // recover it from here. Otherwise from official
        // in case FX in in both env it will prevails the configured one.
        final String body = " and quote_name like 'FX.%' and length(quote_name) = 10 and TRUNC(quote_date) = ";
        final StringBuilder specificPE = new StringBuilder(" quote_set_name= '").append(priceEnvironment).append("'")
                .append(body).append(Util.date2SQLString(quoteDate));

        final StringBuilder officialPE = new StringBuilder(" quote_set_name= 'OFFICIAL'").append(body)
                .append(Util.date2SQLString(quoteDate));

        return Arrays.asList(new String[]{specificPE.toString(), officialPE.toString()});
    }

    /**
     * @return PE of the template or, if not set up, of the ST. In any other case,
     * OFFICIAL
     */
    private String getPriceEnvironment() {

        final ReportTemplate template = getReportTemplate();

        if (super.getPricingEnv() != null) {
            return super.getPricingEnv().getName();
        }

        if ((template != null) && (template.getAttributes() != null)
                && (template.getAttributes().get(PRICING_ENV) != null)) {
            return template.getAttributes().get(PRICING_ENV);
        }

        // error
        final String pricingEnvName = "OFFICIAL";
        Log.error(this, "PricingEnv not set up in the report. Using as default " + pricingEnvName);
        return pricingEnvName;
    }

    /**
     * @param template
     * @param valDate
     * @return start date from template
     */
    protected JDate getStartDate(ReportTemplate template, JDate valDate) {
        return getDate(template, valDate, TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS,
                TradeReportTemplate.START_TENOR);
    }

    /**
     * @param template
     * @param valDate
     * @return end date from template
     */
    protected JDate getEndDate(ReportTemplate template, JDate valDate) {
        return getDate(template, valDate, TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
                TradeReportTemplate.END_TENOR);
    }

    private boolean checkCoreFlag() {
        final Boolean flag = (Boolean) getReportTemplate().get(CORE_FLAG);
        if (flag != null) {
            return flag;
        }
        return false;
    }

    protected Vector getHolidays() {
        Vector holidays = new Vector<>();
        if (getReportTemplate().getHolidays() != null) {
            holidays = getReportTemplate().getHolidays();
        } else {
            holidays.add("SYSTEM");
        }
        return holidays;
    }

    /**
     * A workaround to be sure Product Cache warm-up will be done before executing
     * BOPositionReprt.preparePositionFilters => getUnderlyerRoundingDigits =>
     * getProduct(id) N times To get this correctly placed, it should be done in
     * core Calypso => A P2 SF case to be opened?
     */
    @Override
    protected HashMap<InventoryKey, InventoryPositionArray> trimPositions(InventoryPositionArray positions) {
        if (null != positions && !positions.isEmpty()) {
            Set<Integer> productIds = new HashSet<>(positions.size());
            for (int i = 0; i < positions.size(); i++) {
                productIds.add(((com.calypso.tk.bo.InventorySecurityPosition) positions.get(i)).getSecurityId());
            }
            top = System.currentTimeMillis();
            Log.system(LOG_CATEGORY, "Start loading products from list of inventory positions");
            BOCache.getExchangeTradedProductsByIds(DSConnection.getDefault(), productIds);
            Log.system(LOG_CATEGORY, "Stop loading products from list of inventory positions. TIME="
                    + (System.currentTimeMillis() - top) + " in milliseconds");
        }
        return super.trimPositions(positions);
    }

}
