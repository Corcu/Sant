/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

public class BOSecurityPositionReportTemplate extends com.calypso.tk.report.BOSecurityPositionReportTemplate {

    private static final long serialVersionUID = 1L;

    public static final String DIRTY_PRICE_PROPERTY = "QuotePrice";
    //same property as CollateralConfigReport
    public static final String COLLATERAL_CONFIG_PROPERTY = "MarginCallConfig";

    public static final String PRICING_ENV_PROPERTY = "PRICING_ENV_BOSS";

    public static final String MARKET_VALUE_PROPERTY = "MarketValue";

    public static final String MARKET_VALUE_EUR_PROPERTY = "MarketValueEUR";

    public static final String MARKET_VALUE_EUR_NOFIXING_PROPERTY = "MarketValueEURNoFixing";

    public static final String NOMINAL_PROPERTY = "Nominal";

    public static final String INFORMES_INT_NOMINAL_PROPERTY = "InformesInternosNominal";
    public static final String INFORMES_INT_MVALUE_PROPERTY = "InformesInternosMarketValue";
    public static final String INFORMES_INT_MVALUE_EUR_NOFIXING_PROPERTY = "InformesInternosMValueEUR";
    public static final String INFORMES_INT_PRECIO_PROPERTY = "InformesInternosPrecio";
    public static final String INFORMES_INT_FXRATE_PROPERTY = "InformesInternosFXRate";


    public static final String FX_RATE_NAME_PROPERTY = "FX Fixing Name";

    public static final String QUOTE_SET_PROPERTY = "QuoteSet";
    public static final String REPORT_TEMPLATE_PROPERTY = "ReportTemplate";
    public static final String END_DATE_MINUS1_PROPERTY = "End Date -1";
    public static final String END_DATE_PROPERTY = "End Date";
}

