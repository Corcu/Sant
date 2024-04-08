/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.SantMCConfigFilteringUtil;
import calypsox.util.collateral.SantMarginCallEntryUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.filter.CollateralFilterProxy;
import com.calypso.tk.collateral.filter.impl.CachedCollateralFilterProxy;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.manager.worker.impl.LoadTaskWorker;
import com.calypso.tk.collateral.optimization.candidat.CollateralCandidate;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.RemoteCollateralServer;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.BOCashPositionReport;
import com.calypso.tk.report.BOSecurityPositionReport;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SA CCR Balances (or new IRIS balances files). This report provides collateral balances and other information. Positions are built from BOPosition (core).
 * It includes columns for Collateral Config, Margin Call Entries and some custom columns, extracted from positions.
 *
 * @author Guillermo Solano
 * @version 1.1
 * @Date 30/12/2016
 */
public class SACCRBalancesReport extends SantReport {


    /**
     * Generated Serial UID
     */
    private static final long serialVersionUID = 4738602175553031820L;


    /*
     * Alternative configurations by DV
     */
    public enum CONFIGURATIONS {
        SKIP_ERRORS("No_Errors"), //will disable errors
        DISABLE_THREADS("No_Threads"), //does not recover MCEntry & Haircut
        CLEAN_PRICE("Clean_Price_Quote"), //in case a clean price PE is changed
        MATURITY_RANGE("Maturity_Range"), //allows to add new values in range
        ALTERNATIVE_MCE_RECOVER("Alternative_MCEntry"), //recovers MC Entries like CM instead of DS service
        NUMBER_DECIMALS("Number_Decimal"); //default 4, allows to precise with more decimals


        private String name;

        CONFIGURATIONS(final String n) {
            this.name = n;
        }

        public String getName() {
            return name;
        }
    }

    //Constants
    private static final String PRICING_ENV = "PricingEnvName";

    private static final String EUR_NAME = "EUR";

    private static final String CASH = "Cash";

    private static final String BALANCE = "Balance";

    //variables
    /*
     * Process Date, introduced by date in Panel
     */
    private JDate processDate;

    /*
     * Process date -1 business day
     */
    private JDate valueDate;

    /*
     *  Process date + Matury offset to business days, determined by Maturity offset in panel.
     *  If empty, default is 7
     */
    private JDate collateralMaturityDate;

    /*
     * Saves DV parameterization of used
     */
    private static Map<String, String> dvConfiguration = null;

    /*
     * Static constant to define how many decimals should be use
     */
    private static Integer DECIMALS_POSITIONS = null;

    /*
     * Maps Entries found in MCEntry thread, to cache previos calls to collateral Service
     */
    private Map<Integer, MarginCallEntryDTO> mcEntryMap = null;

    /*
     * Stores the clean Price Env. By default is "CleanPrice", but it can be configured by DV.
     */
    private PricingEnv cleanPricePricingEnv = null;

    /*
     * Keeps track of any error produced in independent Threads
     */
    private List<String> threadsErrors = Collections.synchronizedList(new ArrayList<String>());

    /*
     * super.getReportTemplate, allows threads to read it.
     */
    private ReportTemplate reportTemplate = null;

    /*
     * Allowed contracts types if filter has been enabled in panel
     */
    private Set<String> allowedContractTypes = null;

    /**
     * Main method, call by super class. Some exceptions & control are in super class.
     *
     * @param errors
     * @return rows for securities positions
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput loadReport(Vector errors) {

        //Verified mandatory fields and initialize variables
        if (!mandatoryFields(errors))
            return null;

        final DefaultReportOutput reportOutput = new DefaultReportOutput(this);

        //recovers FX fixing quotes
        final SantGenericQuotesLoader fxQuotesLoader = new SantGenericQuotesLoader(true, buildFXWhereClause());
        // securities report retrieved in a thread - Equities
        final SecurityPositionThread secPosThread = new SecurityPositionThread(errors);
        //recover cash position
        final CashPositionThread cashThread = new CashPositionThread(errors);
        //recovers DV configurations
        final DomainValuesThread dvThread = new DomainValuesThread();

        // start threads
        dvThread.start();
        fxQuotesLoader.load();
        secPosThread.start();
        cashThread.start();

        // join: wait all threads till last one finishes
        try {
            dvThread.join();
            secPosThread.join();
            cashThread.join();
            fxQuotesLoader.join();

        } catch (InterruptedException e) {
            errors.add("FAIL: Thread Exception during execution" + e.getLocalizedMessage());
            Log.error(this, e);
        }

        //if used, stores configuration in DV Named SACCRBalancesReport
        dvConfiguration = dvThread.getDomainValuesAndComments();

        // recover rows from positions to re-process
        ReportRow[] secRows = secPosThread.getSecuritiesPositionsRows();
        ReportRow[] cashRows = cashThread.getCashPositionsRows();
        final List<ReportRow> rowsList = new ArrayList<ReportRow>(secRows.length + cashRows.length);

        // concat rows
        ReportRow[] rows = new ReportRow[secRows.length + cashRows.length];
        System.arraycopy(secRows, 0, rows, 0, secRows.length);
        System.arraycopy(cashRows, 0, rows, secRows.length, cashRows.length);

        //reset, it will be checked again during processing
        errors.clear();
        recoverCleanPriceEnvironment(errors);
        //in case haircut & MC Entries are not required, allows to increase performance by disable these threads
        boolean noThreads = Util.isTrue(dvConfiguration.get(CONFIGURATIONS.DISABLE_THREADS.name));
        MarginCallEntryThread mcEntryThread = null;
        ProductHaircutThread haircutThread = null;

        if ((rows != null) && (rows.length > 0)) {

            for (int i = 0; i < rows.length; i++) {

                final ReportRow currentRow = rows[i];

                final Inventory inventory = (Inventory) currentRow.getProperty(ReportRow.INVENTORY);

                CollateralConfig mcConfig = getCollateralConfig(inventory);
                if ((mcConfig == null || inventory == null) || filterContractType(mcConfig))
                    continue;

                if (!noThreads) {
                    //Thread to recover MCEntries
                    mcEntryThread = new MarginCallEntryThread(mcConfig);
                    mcEntryThread.start();
                    //thread to recover Product Haircut
                    haircutThread = new ProductHaircutThread(mcConfig, inventory);
                    haircutThread.start();
                }

                // build custom columns
                currentRow.setProperty(SACCRBalancesReportTemplate.COLLATERAL_CONFIG, mcConfig);
                currentRow.setProperty(SACCRBalancesReportTemplate.PRICING_ENV_PROPERTY, getPricingEnv());
                buildQuotePrices(currentRow, inventory, errors);
                currentRow.setProperty(SACCRBalancesReportTemplate.NOMINAL, getNominal(currentRow));
                currentRow.setProperty(SACCRBalancesReportTemplate.MARKET_VALUATION, getMarketValue(currentRow));
                buildProductFXName(currentRow, mcConfig, fxQuotesLoader);
                currentRow.setProperty(SACCRBalancesReportTemplate.COLLATERAL_CONFIG_TYPE, getCollateralConfigType(mcConfig));
                currentRow.setProperty(SACCRBalancesReportTemplate.COLLATERAL_MOVEMENT_TYPE, getCollateralMovementType(inventory));
                buildCollateralDates(currentRow);


                try {
                    if (!noThreads) {
                        //recover threads calculated values
                        mcEntryThread.join();
                        haircutThread.join();
                        currentRow.setProperty(SACCRBalancesReportTemplate.MARGIN_CALL_ENTRY, mcEntryThread.getMarginCallEntry());
                        currentRow.setProperty(SACCRBalancesReportTemplate.HAIRCUT, haircutThread.getProductHaircut());
                    }

                } catch (InterruptedException e) {
                    Log.error(this, e);
                }

                // add row to output list
                rowsList.add(currentRow);
            }

            //add threadErrors produced to general errors list
            errors.addAll(threadsErrors);
        }

        //if option activate, dont care errors occurred, it will send an OK to Scheduler
        if (Util.isTrue(dvConfiguration.get(CONFIGURATIONS.SKIP_ERRORS.name)))
            errors.clear();

        reportOutput.setRows(rowsList.toArray(new ReportRow[0]));
        return reportOutput;
    }

    /**
     * @param mcConfig to filter
     * @return true if the contract type is used and is not included
     */
    private boolean filterContractType(final CollateralConfig mcConfig) {

        if (this.allowedContractTypes == null || this.allowedContractTypes.isEmpty())
            return false;

        return !this.allowedContractTypes.contains(mcConfig.getContractType());
    }

    /*
     * Override to disable end date, NOT USED.
     */
    @Override
    protected boolean checkProcessEndDate() {
        return false;
    }

    /**
     * Stores process, value & collateral dates. Iquals for all rows.
     *
     * @param currentRow
     */
    private void buildCollateralDates(ReportRow currentRow) {

        currentRow.setProperty(SACCRBalancesReportTemplate.COLLATERAL_PROCESS_DATE, this.processDate);
        currentRow.setProperty(SACCRBalancesReportTemplate.COLLATERAL_VALUE_DATE, this.valueDate);
        currentRow.setProperty(SACCRBalancesReportTemplate.COLLATERAL_MATURITY_DATE, this.collateralMaturityDate);
    }


    /**
     * @param pos current position
     * @return Cash, Bond or Equity depending of the product
     */
    private String getCollateralMovementType(final Inventory pos) {

        if (pos instanceof InventoryCashPosition) {
            return "Cash";

        } else {
            Product p = pos.getProduct();
            if (p != null) {
                if (p instanceof Bond)
                    return "Bond";
                else if (p instanceof Equity)
                    return "Equity";
            }
        }
        return "";
    }


    /**
     * @param mcConfig
     * @return IM for CSD contract; CSA, ISMA, OSLA, MMOO for VM Contracts
     */
    private String getCollateralConfigType(CollateralConfig mcConfig) {

        final String collateralMarginType = mcConfig.getContractType();

        if (!Util.isEmpty(collateralMarginType)) {
            if (collateralMarginType.equals("CSD")) {
                return "IM";
            }
        }
        return collateralMarginType;
    }

    /**
     * Used to define another cleanPrice Env if somehow changes in a future
     *
     * @param errors
     */
    private void recoverCleanPriceEnvironment(Vector<String> errors) {

        this.cleanPricePricingEnv = null;

        if (dvConfiguration.containsKey(CONFIGURATIONS.CLEAN_PRICE)) {

            Log.info(this, "Using DV Configuration for Clean Price");
            cleanPricePricingEnv = AppUtil.loadPE(dvConfiguration.get(CONFIGURATIONS.CLEAN_PRICE).trim(), getValuationDatetime());
            if (cleanPricePricingEnv == null) {
                final String errorMessage = "Domain value " + CONFIGURATIONS.CLEAN_PRICE + " not properly configured in comment. " +
                        dvConfiguration.get(CONFIGURATIONS.CLEAN_PRICE).trim() + "PE not found. Using CleanPrice as default";
                Log.error(this, errorMessage);
                errors.add(errorMessage);
            } else
                return;
        }

        this.cleanPricePricingEnv = AppUtil.loadPE("CleanPrice", getValuationDatetime());
    }

    /**
     * @param errors
     * @return true if all mandatory filters in panel are included
     */
    private boolean mandatoryFields(final Vector<String> errors) {

        errors.clear();
        this.threadsErrors.clear();
        this.allowedContractTypes = new HashSet<String>();
        this.reportTemplate = super.getReportTemplate();
        this.mcEntryMap = new ConcurrentHashMap<Integer, MarginCallEntryDTO>();

        if (this.reportTemplate == null) {
            errors.add("Template not assign.");
            return false;
        }
        computeProcessDate(errors);

        //"Not settled,Actual"
        if (Util.isEmpty((String) this.reportTemplate.get(BOSecurityPositionReportTemplate.POSITION_TYPE)))
            errors.add("Position Type cannot be empty.");

        //"Nominal"
        if (Util.isEmpty((String) this.reportTemplate.get(BOSecurityPositionReportTemplate.POSITION_VALUE)))
            errors.add("Position Value cannot be empty.");

        //"Trade, Settlement
        if (Util.isEmpty((String) this.reportTemplate.get(BOSecurityPositionReportTemplate.POSITION_DATE)))
            errors.add("Position Date cannot be empty.");

        //Cash, Security, Both
        if (Util.isEmpty((String) this.reportTemplate.get(BOSecurityPositionReportTemplate.CASH_SECURITY)))
            errors.add("Position Date cannot be empty.");

        //recover dates, common for all rows
        Integer collateralMaturityOffset = 7;// by default if empty selection in panel
        final String offset = this.reportTemplate.get(SACCRBalancesReportTemplate.MATURITY_OFFSET);
        if (!Util.isEmpty(offset)) {
            collateralMaturityOffset = Integer.parseInt(offset);
        }

        this.valueDate = this.processDate.addBusinessDays(-1, this.reportTemplate.getHolidays());
        this.collateralMaturityDate = this.processDate.addBusinessDays(collateralMaturityOffset, this.reportTemplate.getHolidays());

        final String contractTypes = this.reportTemplate.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        if (!Util.isEmpty(contractTypes)) {
            this.allowedContractTypes = new HashSet<String>(Util.string2Vector(contractTypes));
        }

        return (Util.isEmpty(errors));
    }

    /**
     * @param errors
     * @return true if process date has been read from the template
     */
    private boolean computeProcessDate(final Vector<String> errors) {

        this.processDate = null;
        this.processDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

        if (this.processDate == null) {
            errors.add("Process Start Date cannot be empty.");
            return false;
        }
        return true;
    }

    /**
     * @param row
     * @return nominal of the position. If cash same as Total, if security, recover total Security
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Double getNominal(final ReportRow row) {

        Inventory inventory = row.getProperty(ReportRow.INVENTORY);
        Map positions = row.getProperty(BOPositionReport.POSITIONS);

        if (inventory instanceof InventoryCashPosition) {

            InventoryCashPosition cashPosition = (InventoryCashPosition) inventory;
            return cashPosition.getTotal();
        } else if (inventory instanceof InventorySecurityPosition) {

            BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");

            if (inventory == null || context == null) {
                Log.error(this, "Inventory/context not available for row " + row.toString());
                return null;
            }

            Vector<InventorySecurityPosition> datedPositions = (Vector<InventorySecurityPosition>) positions.get(context.endDate);
            if (Util.isEmpty(datedPositions)) {
                return null;
            }
            Double quantity = InventorySecurityPosition.getTotalSecurity(datedPositions, BALANCE);

            if (!Double.isNaN(quantity) && inventory.getProduct() != null) {
                Product p = inventory.getProduct();
                if (p instanceof Bond) {
                    Bond bond = (Bond) p;
                    return quantity * bond.getFaceValue(); //obtain nominal
                }
                return quantity;
            }

        }
        return null;
    }


    /**
     * @param row with current position
     * @return price of the ISIN Dirty Price x Nominal
     */
    private Double getMarketValue(final ReportRow row) {

        //get Nominal
        Double marketValue = null;
        Inventory inventory = (Inventory) row.getProperty(ReportRow.INVENTORY);

        if (inventory == null) {
            Log.error(this, "Inventory/context not available for row " + row.toString());
            return null;
        }

        final Double nominal = (Double) row.getProperty(SACCRBalancesReportTemplate.NOMINAL);
        if (inventory instanceof InventoryCashPosition) {
            return nominal;
        } else if (inventory instanceof InventorySecurityPosition) {

            final Double priceQuote = (Double) row.getProperty(SACCRBalancesReportTemplate.DIRTY_PRICE_QUOTE);
            final Product product = inventory.getProduct();

            if (product == null) {
                Log.info(this, "Product does NOT exist for position " + row.toString());
                return null;
            }
            if (priceQuote == null || nominal == null) {
                return null;
            }

            if (!Double.isNaN(nominal)) {
                marketValue = nominal * priceQuote;
                return marketValue;
            }
        }
        return null;
    }

    /**
     * @param row          with current position
     * @param mcConfig
     * @param quotesLoader with Fixing FX
     */
    private void buildProductFXName(ReportRow row, CollateralConfig mcConfig, SantGenericQuotesLoader quotesLoader) {

        Inventory inventory = (Inventory) row.getProperty(ReportRow.INVENTORY);

        if (inventory instanceof InventoryCashPosition) {
            row.setProperty(SACCRBalancesReportTemplate.FX_RATE_NAME, CASH);
        } else if (inventory instanceof InventorySecurityPosition && inventory.getProduct() != null) {

            final Product product = inventory.getProduct();
            if (product.getCurrency().equals(EUR_NAME)) {

                row.setProperty(SACCRBalancesReportTemplate.FX_RATE_NAME, EUR_NAME);

            } else {
                //marketValue in EUR, we require the daily (D-1) fixing
                final QuoteValue qvFXfix = quotesLoader.fetchFXQuoteValue(mcConfig.getCurrency(), product.getCurrency());
                if (QuoteValue.isNull(qvFXfix) || QuoteValue.isNull(qvFXfix.getClose())) {
                    Log.error(this, "Not FX Fixing found for FX.EUR." + product.getCurrency());
                    return;
                }

                final String fxFixing = buildFXName(product, qvFXfix);
                row.setProperty(SACCRBalancesReportTemplate.FX_RATE_NAME, fxFixing);

            }
        }
    }


    public static Integer decimalsPositions4Number() {
        if (DECIMALS_POSITIONS != null)
            return DECIMALS_POSITIONS;

        DECIMALS_POSITIONS = 4;
        String domainName = SACCRBalancesReport.class.getSimpleName() + "." + (SACCRBalancesReport.CONFIGURATIONS.NUMBER_DECIMALS.getName());
        Map<String, String> decimalMap = CollateralUtilities.initDomainValueComments(domainName);
        if (decimalMap.containsKey(domainName)) {
            try {
                DECIMALS_POSITIONS = Integer.parseInt(decimalMap.get(domainName));
            } catch (NumberFormatException e) {
                Log.error(SACCRBalancesReport.class, "Cannot read number in DV " + SACCRBalancesReport.CONFIGURATIONS.NUMBER_DECIMALS.getName());
            }
        }

        return DECIMALS_POSITIONS;
    }

    /**
     * @param currentRow
     * @param inventory
     * @param errors
     * @return close quotePrice for products, recovers clean price (default "CleanPrice") and dirty Price (from template PE selection).
     */
    private Double buildQuotePrices(final ReportRow currentRow, final Inventory inventory, Vector<String> errors) {

        if (inventory instanceof InventorySecurityPosition) {

            if (getPricingEnv() != null && inventory.getProduct() != null) {

                final Product product = inventory.getProduct();
                final QuoteSet quoteSetDirty = getPricingEnv().getQuoteSet();
                final QuoteSet quoteSetClean = this.cleanPricePricingEnv.getQuoteSet();

                //ojo, mirar a fecha valueDate
                final QuoteValue dirtyPriceQuote = quoteSetDirty.getProductQuote(product, this.valueDate, getPricingEnv().getName());
                final QuoteValue cleanPriceQuote = quoteSetClean.getProductQuote(product, this.valueDate, quoteSetClean.getName());

                if ((dirtyPriceQuote != null) && (!Double.isNaN(dirtyPriceQuote.getClose()))) {
                    currentRow.setProperty(SACCRBalancesReportTemplate.DIRTY_PRICE_QUOTE, dirtyPriceQuote.getClose());

                } else {

                    final CollateralConfig contract = (CollateralConfig) currentRow.getProperty(SACCRBalancesReportTemplate.COLLATERAL_CONFIG);
                    final String error = quoteSetDirty.getName() + " - Quote not available for Product ISIN: " +
                            inventory.getProduct().getSecCode("ISIN") + " for date " + this.valueDate.toString()
                            + ". For contract: " + contract.getName();
                    errors.add(error);
                    Log.error(this, error);
                }

                if ((cleanPriceQuote != null) && (!Double.isNaN(cleanPriceQuote.getClose()))) {
                    currentRow.setProperty(SACCRBalancesReportTemplate.CLEAN_PRICE_QUOTE, cleanPriceQuote.getClose());

                } else {

                    final CollateralConfig contract = (CollateralConfig) currentRow.getProperty(SACCRBalancesReportTemplate.COLLATERAL_CONFIG);
                    final String error = quoteSetClean.getName() + " - Quote not available for Product ISIN: " +
                            inventory.getProduct().getSecCode("ISIN") + " for date " + this.valueDate.toString()
                            + ". For contract: " + contract.getName();
                    Log.error(this, error);
                }

            } else {
                final CollateralConfig contract = currentRow.getProperty(SACCRBalancesReportTemplate.COLLATERAL_CONFIG);
                Log.error(this, "error Pricing or product null - " + ". For contract: " + contract.getName());
            }
        }
        return null;
    }

    /**
     * @param product
     * @param qvFix
     * @return String as FX.EUR.CCY=xx.xx with the fixing of yesterday close
     */
    //just for checking
    private String buildFXName(final Product product, final QuoteValue qvFix) {

        return "FX.EUR." + product.getCurrency() + "=" + qvFix.getClose();
    }

    /**
     * @param inventory
     * @return MC Contract of the position
     */
    private CollateralConfig getCollateralConfig(final Inventory inventory) {

        if (inventory == null) {
            throw new InvalidParameterException("Invalid row. Cannot locate Inventory object");
        }
        return (inventory.getMarginCallConfigId() == 0) ? null : CacheCollateralClient.getCollateralConfig(
                DSConnection.getDefault(), inventory.getMarginCallConfigId());
    }

    /**
     * @return where to recover FX based on the price Env configured. Makes two calls, one based in the PE set up in the template and, by defaults, adds "OFFICIAL".
     */
    private List<String> buildFXWhereClause() {

        final String priceEnvironment = getPriceEnvironment();
        //two calls might seem weird: in case FX are in other env than OFFICIAL we recover it from here. Otherwise from official
        //in case FX in in both env it will prevails the configured one.
        final String body = " and quote_name like 'FX.%' and length(quote_name) = 10 and TRUNC(quote_date) = ";
        final StringBuilder specificPE = new StringBuilder(" quote_set_name= '").append(priceEnvironment).append("'").append(body).append(
                Util.date2SQLString(this.valueDate));


        final StringBuilder officialPE = new StringBuilder(" quote_set_name= 'OFFICIAL'").append(body).append(
                Util.date2SQLString(this.valueDate));

        return Arrays.asList(new String[]{specificPE.toString(), officialPE.toString()});
    }


    /**
     * @return PE of the template or, if not set up, of the ST. In any other case, OFFICIAL
     */
    private String getPriceEnvironment() {


        if (super.getPricingEnv() != null) {
            return super.getPricingEnv().getName();
        }

        if ((this.reportTemplate != null) && (this.reportTemplate.getAttributes() != null)
                && (this.reportTemplate.getAttributes().get(PRICING_ENV) != null)) {
            return (String) this.reportTemplate.getAttributes().get(PRICING_ENV);
        }

        //error
        final String pricingEnvName = "OFFICIAL";
        Log.error(this, "PricingEnv not set up in the report. Using as default " + pricingEnvName);
        return pricingEnvName;
    }

////////////////////////////////////////////////////////////////////
/////////////	INDEPENDENT INNERS CLASS THREADS //////////////////	
//////////////////////////////////////////////////////////////////

    /**
     * Thread to run the SecurityPosition Report of the BOPosition
     */
    private class SecurityPositionThread extends Thread {

        private final Vector<String> errors;
        private ReportRow[] secPosRows;

        SecurityPositionThread(Vector<String> errorsL) {
            this.errors = errorsL;
            this.secPosRows = new ReportRow[0];
        }

        /**
         * Main call to recover Security positions, by setting the template
         */
        @Override
        public void run() {

            final String cashSec = reportTemplate.get(BOSecurityPositionReportTemplate.CASH_SECURITY);
            //no process security if selection is cash
            if (!Util.isEmpty(cashSec) && cashSec.equals("Cash"))
                return;

            BOSecurityPositionReportTemplate secPositionTemplate = new BOSecurityPositionReportTemplate();

            secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_DATE, reportTemplate.get(BOSecurityPositionReportTemplate.POSITION_DATE));
            secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_TYPE, reportTemplate.get(BOSecurityPositionReportTemplate.POSITION_TYPE));
            secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_VALUE, reportTemplate.get(BOSecurityPositionReportTemplate.POSITION_VALUE));
            String filterZero = "false";
            if (Util.isTrue(reportTemplate.get(BOPositionReportTemplate.FILTER_ZERO).toString(), false))
                filterZero = "true";
            secPositionTemplate.put(BOPositionReportTemplate.FILTER_ZERO, filterZero);
            String filterMatured = "false";
            if (Util.isTrue(reportTemplate.get(BOPositionReportTemplate.FILTER_MATURED).toString(), false))
                filterMatured = "true";
            secPositionTemplate.put(BOPositionReportTemplate.FILTER_MATURED, filterMatured);
            secPositionTemplate.put(BOPositionReportTemplate.MOVE, reportTemplate.get(BOPositionReportTemplate.MOVE));
            secPositionTemplate.put(BOPositionReportTemplate.SEC_FILTER, reportTemplate.get(BOPositionReportTemplate.SEC_FILTER));
            secPositionTemplate.put(BOPositionReportTemplate.CUSTOM_FILTER, reportTemplate.get(BOPositionReportTemplate.CUSTOM_FILTER));

            //determine
            secPositionTemplate.put(SecurityTemplateHelper.SECURITY_REPORT_TYPE, SecurityTemplateHelper.REPORT_TYPE_BOND);
            secPositionTemplate.put(BOSecurityPositionReportTemplate.CASH_SECURITY, "Security");
            secPositionTemplate.put(BOSecurityPositionReportTemplate.AGGREGATION, "ProcessingOrg");
            secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_CLASS, "Margin_Call");

            final String agreementId = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
            if (!Util.isEmpty(agreementId)) {
                secPositionTemplate.put(BOPositionReportTemplate.CONFIG_ID, agreementId);
            }
            //Multi-PO filter
            final String ownersNames = CollateralUtilities.filterPoNamesByTemplate(getReportTemplate());

            if (!Util.isEmpty(ownersNames)) {
                secPositionTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES, ownersNames);
            }

            BOPositionReport secPositionReport = new BOSecurityPositionReport();
            secPositionReport.setReportTemplate(secPositionTemplate);
            secPositionReport.setPricingEnv(getPricingEnv());
            secPositionReport.setStartDate(processDate);
            secPositionReport.setEndDate(processDate);

            //call
            this.secPosRows = (((DefaultReportOutput) secPositionReport.load(this.errors)).getRows());
        }

        public ReportRow[] getSecuritiesPositionsRows() {
            return this.secPosRows.clone();
        }
    }

    /**
     * Thread to run the CashPosition Report of the BOPosition
     */
    private class CashPositionThread extends Thread {

        private final Vector<String> errors;
        private ReportRow[] cashPosRows;

        CashPositionThread(Vector<String> errorsL) {
            this.errors = errorsL;
            this.cashPosRows = new ReportRow[0];
        }

        /**
         * Main call to recover Cash positions, by setting the template
         */
        @Override
        public void run() {

            final String cashSec = reportTemplate.get(BOSecurityPositionReportTemplate.CASH_SECURITY);
            //no process security if selection is Security
            if (!Util.isEmpty(cashSec) && cashSec.equals("Security"))
                return;

            ReportTemplate cashTemplate = new BOCashPositionReportTemplate();

            cashTemplate.put(BOSecurityPositionReportTemplate.POSITION_DATE, (String) reportTemplate.get(BOSecurityPositionReportTemplate.POSITION_DATE));
            cashTemplate.put(BOSecurityPositionReportTemplate.POSITION_TYPE, (String) reportTemplate.get(BOSecurityPositionReportTemplate.POSITION_TYPE));
            cashTemplate.put(BOSecurityPositionReportTemplate.POSITION_VALUE, (String) reportTemplate.get(BOSecurityPositionReportTemplate.POSITION_VALUE));
            String filterZero = "false";
            if (Util.isTrue(reportTemplate.get(BOPositionReportTemplate.FILTER_ZERO).toString(), false))
                filterZero = "true";
            cashTemplate.put(BOPositionReportTemplate.FILTER_ZERO, filterZero);
            cashTemplate.put(BOPositionReportTemplate.MOVE, (String) reportTemplate.get(BOPositionReportTemplate.MOVE));
            cashTemplate.put(BOPositionReportTemplate.CUSTOM_FILTER, reportTemplate.get(BOPositionReportTemplate.CUSTOM_FILTER));
            //determined
            cashTemplate.put(BOSecurityPositionReportTemplate.POSITION_CLASS, "Margin_Call");
            cashTemplate.put(BOSecurityPositionReportTemplate.CASH_SECURITY, "Cash");
            cashTemplate.put(BOSecurityPositionReportTemplate.AGGREGATION, "ProcessingOrg");


            final String agreementId = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
            if (!Util.isEmpty(agreementId)) {
                cashTemplate.put(BOPositionReportTemplate.CONFIG_ID, agreementId);
            }

            // Multi-PO filter
            final String ownersNames = CollateralUtilities.filterPoNamesByTemplate(getReportTemplate());
            if (!Util.isEmpty(ownersNames)) {
                cashTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES, ownersNames);
            }

            BOPositionReport cashPositionReport = new BOCashPositionReport();

            cashPositionReport.setPricingEnv(getPricingEnv());
            cashPositionReport.setStartDate(processDate);
            cashPositionReport.setEndDate(processDate);

            cashPositionReport.setReportTemplate(cashTemplate);

            this.cashPosRows = (((DefaultReportOutput) cashPositionReport.load(this.errors)).getRows());
        }

        public ReportRow[] getCashPositionsRows() {
            return this.cashPosRows.clone();
        }
    }


    /**
     * Thread to run the read of DomainValues configuration. These configuration tend to be general for all
     */
    private class DomainValuesThread extends Thread {

        private Map<String, String> dvMap;

        DomainValuesThread() {
            this.dvMap = null;
        }

        /**
         * Stores in Map DV Name SACCRBalancesReport and comments if alternative configurations are used.
         */
        @Override
        public void run() {

            String domainName = SACCRBalancesReport.class.getSimpleName();
            this.dvMap = CollateralUtilities.initDomainValueComments(domainName);
            domainName += ".";

            for (CONFIGURATIONS t : SACCRBalancesReport.CONFIGURATIONS.values()) {
                this.dvMap.putAll(CollateralUtilities.initDomainValueComments(domainName + t.getName()));
            }
        }

        public Map<String, String> getDomainValuesAndComments() {
            return this.dvMap;
        }
    }


    /**
     * Thread to recover MarginCallEntries for each contract for the process date given in the panel
     */
    private class MarginCallEntryThread extends Thread {

        private CollateralConfig contract;
        private MarginCallEntryDTO entry;

        MarginCallEntryThread(CollateralConfig currentContract) {
            this.contract = null;
            this.entry = null;
            this.contract = currentContract;
        }

        public MarginCallEntryDTO getMarginCallEntry() {
            return entry;
        }

        /**
         * Recovers MCEntries for the process date. These are cached in case previous search was done.
         */
        @Override
        public void run() {

            List<MarginCallEntryDTO> entries = null;
            try {


                if (mcEntryMap.containsKey(this.contract.getId())) {
                    this.entry = mcEntryMap.get(this.contract.getId());
                    return;
                }

                if (!Util.isTrue(dvConfiguration.get(CONFIGURATIONS.ALTERNATIVE_MCE_RECOVER.name))) {

                    entries = toDTO(loadWorker(Arrays.asList(new Integer[]{this.contract.getId()}), processDate));


                } else {

                    entries = DSConnection.getDefault()
                            .getRemoteService(RemoteCollateralServer.class)
                            .loadEntries(this.contract.getId(), processDate, processDate, TimeZone.getDefault(), 1);
                }


                if (!Util.isEmpty(entries)) {

                    if (entries.size() == 1) {
                        this.entry = entries.get(0);
                        mcEntryMap.put(this.contract.getId(), this.entry);
                    } else
                        threadsErrors.add("FAIL: MORE THAN ONE Entry NOT found for contract " + contract.getName() + " for date " + processDate.toString());
                } else
                    threadsErrors.add("FAIL: Entry NOT found for contract " + contract.getName() + " for date " + processDate.toString());

            } catch (CollateralServiceException e) {
                final String errorMessage = "ERROR: Entry NOT found for contract " + contract.getName() + " for date " + processDate.toString();
                threadsErrors.add(errorMessage);
                Log.error(SACCRBalancesReport.class, errorMessage + " " + e.getLocalizedMessage());
                Log.error(this, e); //sonar
            }
        }

        private List<MarginCallEntryDTO> toDTO(List<MarginCallEntry> entries2) {

            ArrayList<MarginCallEntryDTO> listMCEntryDTO = new ArrayList<MarginCallEntryDTO>();
            for (MarginCallEntry mce : entries2) {
                listMCEntryDTO.add(mce.toDTO());
            }
            return listMCEntryDTO;
        }

        private List<MarginCallEntry> loadWorker(List<Integer> mccIDs, JDate processDate) {
            MarginCallReportTemplate template = new MarginCallReportTemplate();

            template.put(MarginCallReportTemplate.PROCESS_DATE, processDate);
            template.put(MarginCallReportTemplate.IS_VALIDATE_SECURITIES,
                    Boolean.FALSE);
            template.put(MarginCallReportTemplate.IS_CHECK_ALLOCATION_POSITION,
                    Boolean.FALSE);
            // template.put(MarginCallReportTemplate.CONTRACT_TYPES, "CSD,CSA_FACADE");

            ExecutionContext context = ExecutionContext.getInstance(
                    ServiceRegistry.getDefaultContext(),
                    ServiceRegistry.getDefaultExposureContext(), template);

            context.setProcessDate(processDate);
            context.setFilter(SantMCConfigFilteringUtil.getInstance().buildMCConfigFilter(mccIDs));

            List<MarginCallEntry> entries = new ArrayList<>();

            CollateralTaskWorker resulto = new LoadTaskWorker(context,
                    entries);
            resulto.process();
            SantMarginCallEntryUtil.fillLightWeightMarginCallEntry(context, entries);
            return entries;
        }
    }

    /**
     * Thread to recover haircuts for positions of type Security.
     */
    private class ProductHaircutThread extends Thread {

        private CollateralConfig contract;
        private Inventory position;
        private Amount haircut;

        ProductHaircutThread(CollateralConfig currentContract, Inventory currentPosition) {
            this.position = currentPosition;
            this.contract = currentContract;
            this.haircut = null;
        }

        public Amount getProductHaircut() {
            return haircut;
        }

        @Override
        public void run() {

            if (this.position instanceof InventoryCashPosition || this.position == null || this.position.getProduct() == null) {
                return;
            }
            Product product = this.position.getProduct();
            CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
            HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
            HaircutProxy haircutProxy = fact.getProxy(contract.getPoHaircutName());

            Double hc = haircutProxy.getHaircut(this.contract.getCurrency(), new CollateralCandidate(product), valueDate, true, this.contract, "Pay");
            this.haircut = new Amount(hc, decimalsPositions4Number());
        }
    }
///////////// END INNER THREADS //////////////

}


