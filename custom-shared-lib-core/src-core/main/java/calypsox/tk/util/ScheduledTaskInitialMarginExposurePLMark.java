package calypsox.tk.util;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.ForexClearFileReader;
import calypsox.util.ForexClearSTUtil;
import com.calypso.tk.collateral.CollateralManager;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.MarginCallReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class ScheduledTaskInitialMarginExposurePLMark extends ScheduledTask {

    private static final long serialVersionUID = 123L;

    private static final String DESCRIPTION = "Initial And Variation Margin";
    // Logs
    protected LogGeneric logGen = new LogGeneric();
    protected String fileName = "";
    // Logs

    private CollateralConfig defaultContract = null;
    private ForexClearFileReader file = null;
    private List<MarginCallEntry> listMarginCallEntry = null;

    // Setter/Getter ListMarginCallEntry
    public List<MarginCallEntry> getListMarginCallEntry() {
        return this.listMarginCallEntry;
    }

    public void setListMarginCallEntry(List<MarginCallEntry> listMarginCallEntry) {
        this.listMarginCallEntry = listMarginCallEntry;
    }

    /**
     * Devuelve la descripcion de la Scheduled Task
     */
    public String getTaskInformation() {
        return DESCRIPTION;
    }

    /**
     * Devuelve la lista de los atributos de la Scheduled Task
     */
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute(ForexClearSTUtil.FIELD_SEPARATOR));
        attributeList.add(attribute(ForexClearSTUtil.FILE_NAME));
        attributeList.add(attribute(ForexClearSTUtil.FILE_PATH));
        // Logs
        attributeList.add(attribute(ForexClearSTUtil.SUMMARY_LOG));
        attributeList.add(attribute(ForexClearSTUtil.DETAILED_LOG));
        attributeList.add(attribute(ForexClearSTUtil.FULL_LOG));
        attributeList.add(attribute(ForexClearSTUtil.STATIC_DATA_LOG));
        // Logs
        return attributeList;
    }

    public boolean process(DSConnection ds, PSConnection ps) {
        // Atributos inicializados del ST
        String separator = getAttribute(ForexClearSTUtil.FIELD_SEPARATOR);
        fileName = getAttribute(ForexClearSTUtil.FILE_NAME);
        final String path = getAttribute(ForexClearSTUtil.FILE_PATH);
        final JDate date = this.getValuationDatetime().getJDate(TimeZone.getDefault());
        ArrayList<String> error = new ArrayList<>();

        fileName = ForexClearSTUtil.getFileName(date, fileName);

        ForexClearSTUtil.checkAtributes(separator, path, fileName, date, error);
        if (separator.equalsIgnoreCase("\\t")) {
            separator = "\t";
        }
        if (!error.isEmpty()) {
            for (String msg : error) Log.error(this, msg);
            return false;
        }

        // Logs
        startLogs(date);
        // Logs

        // Lectura del fichero
        file = new ForexClearFileReader(path, fileName, date, separator, error);
        if (!error.isEmpty()) {
            for (String msg : error) Log.error(this, msg);
            // Logs
            this.logGen.incrementError();
            this.logGen.setErrorNumberOfFiles(this.getClass().getSimpleName(), fileName);
            ForexClearSTUtil.returnErrorLog(
                    logGen,
                    false,
                    date,
                    fileName,
                    path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            // Logs
            return false;
        }

        // copy
        if (!ForexClearFileReader.copyFile(path, fileName)) {
            Log.error(this, "ERROR: Failed to copy file");
            this.logGen.incrementError();
            this.logGen.setErrorMovingFile(this.getClass().getSimpleName(), fileName);
            ForexClearSTUtil.returnErrorLog(
                    logGen,
                    false,
                    date,
                    fileName,
                    path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            return false;
        }

        // PRE: el archivo debe contener alguna fila
        if (file.getLinesSize() == 0) {
            Log.error(this, "El fichero esta vacio");
            // Logs
            this.logGen.incrementError();
            this.logGen.setErrorNumberOfFiles(this.getClass().getSimpleName(), fileName);
            ForexClearSTUtil.returnErrorLog(
                    logGen,
                    false,
                    date,
                    fileName,
                    path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            // Logs
            return false;
        }

        List<Integer> linesList = new ArrayList<Integer>();
        int row = 0;
        while (row < file.getLinesSize()) {

            // Get the lines from file according to: line 'H' and Contracttype
            // 'O'
            final String account = this.file.getValue(ForexClearSTUtil.ACCOUNT, row);
            final String contractType = this.file.getValue(ForexClearSTUtil.CONTRACTTYPE, row);

            if (ForexClearSTUtil.ACCOUNT_H.equals(account)
                    && ForexClearSTUtil.CONTRACTTYPE_O.equals(contractType)) {
                linesList.add(row);
            }

            row++;
        }

        if (linesList.isEmpty()) {
            for (String msg : error) Log.error(this, msg);
            // Logs
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(
                    this.getClass().getSimpleName(),
                    fileName,
                    String.valueOf(this.logGen.getNumberTotal()),
                    "0",
                    ForexClearSTUtil.INITMARGIN,
                    "",
                    String.valueOf(0));
            ForexClearSTUtil.returnErrorLog(
                    logGen,
                    false,
                    date,
                    fileName,
                    path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            // Logs
            return false;
        }

        // Init Legal Entities
        ForexClearSTUtil.initLegalEntities(ds, error);

        // Get the contract
        this.defaultContract =
                ForexClearSTUtil.findContract(
                        ds, ForexClearSTUtil.CCY_EUR, ForexClearSTUtil.CSA, ForexClearSTUtil.IM);

        // Get the MarginCallEntry using PRICE WORKER
        setListMarginCallEntry(getPriceWorker(this.defaultContract.getId()));

        for (Integer line : linesList) {
            int linea = line.intValue();

            if (this.defaultContract == null) {
                Log.error(this, "ERROR: No se encuentra el contrato");
                // Logs
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(
                        this.getClass().getSimpleName(),
                        fileName,
                        String.valueOf(this.logGen.getNumberTotal()),
                        "0",
                        "CONTRACT",
                        "",
                        "");
                ForexClearSTUtil.returnErrorLog(
                        logGen,
                        false,
                        date,
                        fileName,
                        path,
                        getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                        this.getClass().getSimpleName());
                // Logs
                return false;
            }

            // Get the trade Nominal
            Double tradeNominal = getTradeNominal(ds, linea, error);

            if (tradeNominal != null) {
                // Create the MarginCall CASH Trade and asign the trade Nominal
                // to product
                Trade trade = this.createTrade(linea, this.defaultContract, error);

                MarginCall mc = (MarginCall) trade.getProduct();
                mc.setPrincipal(tradeNominal.doubleValue());
                // Check direction of principal to set quantity
                if (mc.getPrincipal() < 0) {
                    trade.setQuantity(-1);
                } else {
                    trade.setQuantity(1);
                }

                if (!ForexClearSTUtil.checkAndSaveTrade(linea, trade, error)) {
                    // Logs
                    this.logGen.incrementRecordErrors();
                    this.logGen.setErrorSavingTrade(
                            this.getClass().getSimpleName(),
                            fileName,
                            String.valueOf(linea),
                            "",
                            String.valueOf(linea));
                    // Logs
                } else {
                    this.logGen.incrementOK();
                    this.logGen.setOkLine(
                            this.getClass().getSimpleName(), fileName, linea, String.valueOf(trade.getLongId()));
                }
            }

            for (String msg : error) Log.error(this, msg);
        }

        // post process
        try {
            ForexClearFileReader.postProcess(error.isEmpty(), date, fileName, path);
        } catch (Exception e1) {
            Log.error(this, e1); // sonar
            this.logGen.incrementError();
            this.logGen.setErrorMovingFile(this.getClass().getSimpleName(), fileName);
        }

        // Logs
        stopLogs();
        // Logs

        return error.isEmpty();
    }

    /**
     * Get the trade nominal.
     *
     * @param ds
     * @param cc
     * @param linea
     * @param error
     * @return
     */
    private Double getTradeNominal(DSConnection ds, final int linea, ArrayList<String> error) {
        Double tradeNominal = null;
        final Double initMarginValue = getInitMarginForexClear(ds, linea);

        if (initMarginValue != null) {
            final Double dailycurrentSecurityPosition =
                    getCurrentSecurityPositionForexClear(getListMarginCallEntry(), error);

            final double prevTotalMargina =
                    getPreviousTotalMarginForexClear(getListMarginCallEntry(), error);

            double prevCash = getPrevCash(getListMarginCallEntry());
            // Condition
            // Use absolute amount for Current Security Position and Initial Margin
            if (Math.abs(prevTotalMargina) + Math.abs(dailycurrentSecurityPosition)
                    < Math.abs(initMarginValue)) {

                tradeNominal = initMarginValue - prevTotalMargina - dailycurrentSecurityPosition;

            } else if (!(Math.abs(prevTotalMargina)
                    + Math.abs(dailycurrentSecurityPosition)
                    - Math.abs(initMarginValue)
                    < 1.0)
                    && prevCash != 0.0) {
                tradeNominal = initMarginValue - prevTotalMargina - dailycurrentSecurityPosition;

                tradeNominal = Math.min(tradeNominal, Math.abs(prevCash));
            }
        }

        //  The TradeNominal is in EUR. It is not necessary to convert to USD
        //        // Convert trade nominal to trade currency
        //        if (tradeNominal != null) {
        //            try {
        //                final String tradeCcy = this.file
        //                        .getValue(ForexClearSTUtil.CURRENCY, linea);
        //                final JDate valDate = new JDatetime(getValuationDatetime(false))
        //                        .getJDate(TimeZone.getDefault());
        //                final PricingEnv pricingEnv = ds.getRemoteMarketData()
        //                        .getPricingEnv(getPricingEnv());
        //
        //                tradeNominal = Double.valueOf(CollateralUtilities
        //                        .convertCurrency(ForexClearSTUtil.CCY_EUR,
        //                                tradeNominal.doubleValue(), tradeCcy,
        // valDate.addBusinessDays(-1,
        // LocalCache.getCurrencyDefault(ForexClearSTUtil.CCY_EUR).getDefaultHolidays()),
        //                                pricingEnv));
        //            } catch (CalypsoServiceException e) {
        //                String errorMessage = String.format(
        //                        "Could not retrive Pricing Env \"%s\"",
        //                        getPricingEnv());
        //                Log.error(this, errorMessage, e);
        //            } catch (MarketDataException e) {
        //                Log.error(this, "Could not convert amount", e);
        //            }
        //        }

        return tradeNominal;
    }

    private Double getPrevCash(final List<MarginCallEntry> listMarginCall) {
        for (MarginCallEntry entry : listMarginCall) {
            return entry.getPreviousCashMargin();
        }
        return null;
    }

    /**
     * Get the Previous Total Margin (Cash and Security)
     *
     * @param listMarginCall
     * @param error
     * @return
     */
    private Double getPreviousTotalMarginForexClear(
            final List<MarginCallEntry> listMarginCall, ArrayList<String> error) {
        for (MarginCallEntry entry : listMarginCall) {
            return entry.getPreviousTotalMargin();
        }
        return null;
    }

    /**
     * Get the Current Security Position.
     *
     * @param listMarginCall
     * @param error
     * @return
     */
    private Double getCurrentSecurityPositionForexClear(
            final List<MarginCallEntry> listMarginCall, ArrayList<String> error) {
        // TODO eso es la suma de todos los Margin Call Trades de tipo Security
        // marcados con el keyword ForexClearReport "36A"

        for (MarginCallEntry entry : listMarginCall) {
            return entry.getDailySecurityMargin();
        }
        return null;
    }

    /**
     * Get the Init Margin Value from file according to: line 'H' and Contracttype 'O'
     *
     * @param linea
     * @return
     */
    private Double getInitMarginForexClear(final DSConnection dsCon, final int linea) {
        Double initMarginValue = new Double(0);

        final String iM = this.file.getValue(ForexClearSTUtil.INITMARGIN, linea);
        try {
            initMarginValue = Double.valueOf(iM);

            final String ccy = this.file.getValue(ForexClearSTUtil.CURRENCY, linea);
            initMarginValue = convertToEurCcy(dsCon, ccy, initMarginValue);
        } catch (NumberFormatException e) {
            Log.error(this, e);
            this.logGen.incrementError();
            return null;
        }

        return initMarginValue;
    }

    /**
     * Convert any amount in a specific ccy into EUR.
     *
     * @param dsCon
     * @param ccy
     * @param amount
     * @return
     */
    private Double convertToEurCcy(final DSConnection dsCon, final String ccy, final Double amount) {
        Double rst;
        if (!ForexClearSTUtil.CCY_EUR.equals(ccy)) {
            try {
                final JDate valDate =
                        new JDatetime(getValuationDatetime(false)).getJDate(TimeZone.getDefault());
                final PricingEnv pricingEnv = dsCon.getRemoteMarketData().getPricingEnv(getPricingEnv());
                rst =
                        CollateralUtilities.convertCurrency(
                                ccy,
                                amount,
                                ForexClearSTUtil.CCY_EUR,
                                valDate.addBusinessDays(
                                        -1,
                                        LocalCache.getCurrencyDefault(ForexClearSTUtil.CCY_EUR).getDefaultHolidays()),
                                pricingEnv);
            } catch (CalypsoServiceException | MarketDataException e) {
                Log.error(this, e);
                this.logGen.incrementError();
                return null;
            }
        } else {
            rst = amount;
        }
        return rst;
    }

    private MarginCall createMarginCall(int linea, CollateralConfig cc) {

        // Crear Margin Call
        MarginCall mc = new MarginCall();
        mc.setSecurity(null);
        mc.setFlowType(ForexClearSTUtil.COLLATERAL);
        mc.setOrdererLeId(ForexClearSTUtil.processingOrg.getId());
        mc.setLinkedLongId(cc.getId());

        // final String specificCurrency = this.file
        // .getValue(ForexClearSTUtil.CURRENCY, linea);
        //  if (Util.isEmpty(specificCurrency)) {
        //  return null;
        //  }
        //  mc.setCurrencyCash(specificCurrency);

        //  Always EUR Ccy
        mc.setCurrencyCash(ForexClearSTUtil.EUR_CURRENCY);

        return mc;
    }

    /**
     * Crear un trade a partir de los datos del fichero y el contrato
     *
     * @param product
     * @param line
     * @param fila
     * @param error
     * @return
     */
    private Trade createTrade(int linea, CollateralConfig cc, ArrayList<String> error) {

        MarginCall mc = this.createMarginCall(linea, cc);
        if (mc == null) {
            error.add("ERROR: No se ha podido crear el Margin Call ");
            return null;
        }

        Trade trade = new Trade();
        trade.setProduct(mc);
        trade.setTraderName(ForexClearSTUtil.NONE);
        trade.setSalesPerson(ForexClearSTUtil.NONE);
        trade.setBook(cc.getBook());
        trade.setEnteredUser(DSConnection.getDefault().getUser());
        trade.setTradeDate(this.getValuationDatetime());
        trade.setSettleDate(this.getValuationDatetime().getJDate(null));
        trade.setAction(Action.NEW);
        trade.setCounterParty(ForexClearSTUtil.counterParty);
        trade.setAdjustmentType(ForexClearSTUtil.BUYSELL);

        // final String currency = this.file.getValue(ForexClearSTUtil.CURRENCY, linea);
        //  Always is EUR
        final String currency = ForexClearSTUtil.EUR_CURRENCY;
        trade.setTradeCurrency(currency);
        trade.setSettleCurrency(currency);
        ((MarginCall)trade.getProduct()).setOrdererRole("Client");
        ((MarginCall)trade.getProduct()).setOrdererLeId(trade.getCounterParty().getId());
        trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, cc.getId());

        // ForexClear - Trade Keywords
        addForexClearKeywords(trade);

        // ForexClear - End

        return trade;
    }

    /**
     * Add the ForexClear Trade Keywords.
     *
     * @param trade
     * @param description
     */
    private void addForexClearKeywords(final Trade trade) {
        trade.addKeyword(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE, ForexClearSTUtil.IM);
        trade.addKeyword(
                CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT, ForexClearSTUtil.REPORT_21);
    }

    private List<MarginCallEntry> getPriceWorker(final int contractID) {

        List<Integer> mccIDs = new ArrayList<Integer>();
        mccIDs.add(contractID);

        final JDate processDate =
                new JDatetime(getValuationDatetime(false)).getJDate(TimeZone.getDefault());
        final List<MarginCallEntry> listMarginCall = priceWorker(mccIDs, processDate);

        return listMarginCall;
    }

    private List<MarginCallEntry> priceWorker(List<Integer> mccIDs, JDate processDate) {
        MarginCallReportTemplate template = new MarginCallReportTemplate();

        template.put(MarginCallReportTemplate.PROCESS_DATE, processDate);
        template.put(MarginCallReportTemplate.IS_VALIDATE_SECURITIES, Boolean.FALSE);
        template.put(MarginCallReportTemplate.IS_CHECK_ALLOCATION_POSITION, Boolean.FALSE);
        template.put(MarginCallReportTemplate.CONTRACT_TYPES, ForexClearSTUtil.CSA);

        ExecutionContext context =
                ExecutionContext.getInstance(
                        ServiceRegistry.getDefaultContext(),
                        ServiceRegistry.getDefaultExposureContext(),
                        template);

        MarginCallConfigFilter mccFilter = new MarginCallConfigFilter();
        mccFilter.setContractIds(mccIDs);

        context.setProcessDate(processDate);
        context.setFilter(mccFilter);

        final CollateralManager marginCallManager = CollateralManager.getInstance(context);
        List<MarginCallEntry> entries = marginCallManager.createEntries(context.getFilter(), new ArrayList<>());

        CollateralTaskWorker rePriceTaskWorker = CollateralTaskWorker.getInstance(CollateralTaskWorker.TASK_REPRICE, context, entries);
        rePriceTaskWorker.process();

        // TODO
        // entries.get(0).getDailySecurityMargin(); // Current Security Position
        // entries.get(0).getPreviousTotalMargin(); // Prev Total Margin

        return entries;
    }

    private void startLogs(final JDate date) {
        String time = "";
        synchronized (ForexClearSTUtil.timeFormat) {
            time = ForexClearSTUtil.timeFormat.format(date.getDate());
        }
        this.logGen.generateFiles(
                getAttribute(ForexClearSTUtil.DETAILED_LOG),
                getAttribute(ForexClearSTUtil.FULL_LOG),
                getAttribute(ForexClearSTUtil.STATIC_DATA_LOG),
                time);
        try {
            this.logGen.initializeFiles(this.getClass().getSimpleName());
        } catch (IOException e1) {
            this.logGen.incrementError();
            this.logGen.setErrorCreatingLogFile(this.getClass().getSimpleName(), fileName);
            Log.error(this, e1);
        }
    }

    private void stopLogs() {
        try {
            this.logGen.feedGenericLogProcess(
                    fileName,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName(),
                    this.logGen.getNumberTotal() - 1);
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            this.logGen.closeLogFiles();
        } catch (final IOException e) {
            Log.error(this, e); // sonar
        }
    }

    // private double getPreviousTotalMarginForexClear(final CollateralConfig
    // cc) {
    // double prevTotalMargin = 0.0;
    // final List<Integer> mccID = new ArrayList<Integer>();
    // mccID.add(cc.getId());
    // // get entry
    // try {
    // final List<MarginCallEntryDTO> entries =
    // CollateralManagerUtil.loadMarginCallEntriesDTO(mccID, new
    // JDatetime(getValuationDatetime(false)).getJDate(TimeZone.getDefault()));
    // if ((entries != null) && (entries.size() > 0)) {
    // // Prev Total Margin
    // prevTotalMargin = entries.get(0).getPreviousTotalMargin();
    // }
    // } catch (RemoteException e) {
    // Log.error(this, "Cannot get marginCallEntry for the contract = " +
    // cc.getId(), e);
    // }
    // return prevTotalMargin;
    // }

}
