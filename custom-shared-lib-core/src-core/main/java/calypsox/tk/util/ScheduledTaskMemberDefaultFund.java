package calypsox.tk.util;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.ForexClearFileReader;
import calypsox.util.ForexClearSTUtil;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.collateral.CollateralManager;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.MarginCallReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ScheduledTaskMemberDefaultFund extends ScheduledTask {

    private ForexClearFileReader file = null;
    private static final long serialVersionUID = 123L;

    // Logs
    protected LogGeneric logGen = new LogGeneric();
    protected String fileName = "";

    // Logs

    @Override
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

    @Override
    public String getTaskInformation() {
        return "This report displays the Default Fund contribution by Clearing Member";
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) { // M?todo que
        // contiene la
        // l?gica de
        // la
        // ScheduledTask

        ArrayList<String> error = new ArrayList<String>();

        String separator = getAttribute(ForexClearSTUtil.FIELD_SEPARATOR);
        fileName = getAttribute(ForexClearSTUtil.FILE_NAME);
        final String path = getAttribute(ForexClearSTUtil.FILE_PATH);
        final JDate date = this.getValuationDatetime()
                .getJDate(TimeZone.getDefault());

        fileName = ForexClearSTUtil.getFileName(date, fileName);

        ForexClearSTUtil.checkAtributes(separator, path, fileName, date, error);
        if (separator.equalsIgnoreCase("\\t")) {
            separator = "\t";
        }
        if (!error.isEmpty()) {
            for (String msg : error)
                Log.error(this, msg);
            return false;
        }

        // Logs
        String time = "";
        synchronized (ForexClearSTUtil.timeFormat) {
            time = ForexClearSTUtil.timeFormat.format(date.getDate());
        }
        this.logGen.generateFiles(getAttribute(ForexClearSTUtil.DETAILED_LOG),
                getAttribute(ForexClearSTUtil.FULL_LOG),
                getAttribute(ForexClearSTUtil.STATIC_DATA_LOG), time);
        try {
            this.logGen.initializeFiles(this.getClass().getSimpleName());
        } catch (IOException e1) {
            this.logGen.incrementError();
            this.logGen.setErrorCreatingLogFile(this.getClass().getSimpleName(),
                    fileName);
            Log.error(this, e1);
        }
        // Logs

        file = new ForexClearFileReader(path, fileName, date, separator, error);
        if (!error.isEmpty()) {
            for (String msg : error)
                Log.error(this, msg);
            // Logs
            this.logGen.incrementError();
            this.logGen.setErrorNumberOfFiles(this.getClass().getSimpleName(),
                    fileName);
            ForexClearSTUtil.returnErrorLog(logGen, false, date, fileName, path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            // Logs
            return false;
        }

        // copy
        if (!ForexClearFileReader.copyFile(path, fileName)) {
            Log.error(this, "ERROR: Failed to copy file");
            this.logGen.incrementError();
            this.logGen.setErrorMovingFile(this.getClass().getSimpleName(),
                    fileName);
            ForexClearSTUtil.returnErrorLog(logGen, false, date, fileName, path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            return false;
        }

        // PRE: el archivo debe contener alguna fila
        if (file.getLinesSize() < 1) {
            Log.error(this, "El archivo NO contiene al menos 1 l?nea");
            // Logs
            this.logGen.incrementError();
            this.logGen.setErrorNumberOfLines(this.getClass().getSimpleName(),
                    fileName);
            ForexClearSTUtil.returnErrorLog(logGen, false, date, fileName, path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            // Logs
            return false;
        }

        ForexClearSTUtil.initLegalEntities(ds, error);
        if (!error.isEmpty()) {
            for (String msg : error)
                Log.error(this, msg);
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(
                    this.getClass().getSimpleName(), fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "0",
                    "PO or CTPY", "", "");
            ForexClearSTUtil.returnErrorLog(logGen, false, date, fileName, path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            return false;
        }
        this.logGen.incrementTotal();

        ArrayList<Integer> lineArray = filterLines();

        CollateralConfig cc = findContract(ds, error, lineArray);

        // Price contract
        getPriceWorker(cc.getId());

        if (!error.isEmpty()) {
            for (String msg : error)
                Log.error(this, msg);
            // Logs
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(
                    this.getClass().getSimpleName(), fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "0",
                    "CONTRACT", "", "");
            // Logs
        }

        int position = 0;

        for (int i = 0; i < lineArray.size(); i++) {
            position = lineArray.get(i);
            // Logs
            this.logGen.incrementTotal();
            // Logs
            String currency = this.file.getValue(ForexClearSTUtil.CURRENCY,
                    position);
            if (Util.isEmpty(currency)) {
                error.add("ERROR: El valor de la divisa en la linea ("
                        + position + ") esta vacia");

            } else if (isOnValidDate(position)) {
                Trade trade = createMarginCallCashTrade(cc, error, position);
                if (trade != null) {
                    if (!ForexClearSTUtil.checkAndSaveTrade(0, trade, error)) {
                        // Logs
                        this.logGen.incrementRecordErrors();
                        this.logGen.setErrorSavingTrade(
                                this.getClass().getSimpleName(), fileName, "1",
                                "", "1");
                        // Logs
                    } else {
                        this.logGen.incrementOK();
                        this.logGen.setOkLine(this.getClass().getSimpleName(),
                                fileName, 0, "");
                    }
                }
            }
        }

        for (String msg : error)
            Log.error(this, msg);

        // post process
        try {
            ForexClearFileReader.postProcess(error.isEmpty(), date, fileName,
                    path);
        } catch (Exception e1) {
            Log.error(this, e1); // sonar
            this.logGen.incrementError();
            this.logGen.setErrorMovingFile(this.getClass().getSimpleName(),
                    fileName);
        }

        // Logs
        try {
            this.logGen.feedGenericLogProcess(fileName,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName(),
                    this.logGen.getNumberTotal() - 1);
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            this.logGen.closeLogFiles();
        } catch (final IOException e) {
            Log.error(this, e); // sonar
        }
        // Logs

        return error.isEmpty();
    }

    /**
     * createMarginCallCashTrade.
     *
     * @param cc    CollateralConfig
     * @param error ArrayList<String>
     * @param fila
     * @return Trade
     */
    private Trade createMarginCallCashTrade(CollateralConfig cc,
                                            ArrayList<String> error, int fila) {

        double previousCashPosition = getPreviousCashPosition(cc.getId());
        MarginCall mc = createMarginCall(cc.getId(), previousCashPosition,
                fila);
        if (mc == null) {
            error.add(
                    "ERROR: No se ha creado el Margin Call Security Trade, el motivo es que no existe divisa ");
            return null;
        }
        // Create trade only if the principal is not zero
        Trade trade = null;
        if (mc.getPrincipal() != 0.0) {
            trade = new Trade();
            trade.setProduct(mc);
            trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER,
                    cc.getId());

            // Check direction of principal to set quantity
            if (mc.getPrincipal() < 0) {
                trade.setQuantity(-1);
            } else {
                trade.setQuantity(1);
            }
            // Principal value should be signed.

            trade.setTradeCurrency(file.getValue(ForexClearSTUtil.CURRENCY, 0));
            trade.setSettleCurrency(
                    file.getValue(ForexClearSTUtil.CURRENCY, 0));
            trade.setCounterParty(ForexClearSTUtil.counterParty);
            trade.setAction(Action.NEW);
            trade.setStatus(Status.S_NONE);
            trade.setBook(cc.getBook());
            trade.setTradeDate(this.getValuationDatetime());
            trade.setSettleDate(this.getValuationDatetime().getJDate(null));
            trade.setTraderName(ForexClearSTUtil.NONE);
            trade.setSalesPerson(ForexClearSTUtil.NONE);
            ((MarginCall)trade.getProduct()).setOrdererRole("Client");
            ((MarginCall)trade.getProduct()).setOrdererLeId(trade.getCounterParty().getId());
            trade.addKeyword(
                    CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE,
                    ForexClearSTUtil.DF);
            trade.addKeyword(
                    CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT,
                    ForexClearSTUtil.REPORT_32);
        }

        return trade;
    }

    /**
     * createMarginCall.
     *
     * @param ccId                 int
     * @param previousCashPosition double
     * @param fila                 int
     * @return MarginCall
     */
    private MarginCall createMarginCall(int ccId, double previousCashPosition,
                                        int fila) {

        // Crear Margin Call
        MarginCall mc = new MarginCall();
        String specificCurrency = file.getValue(ForexClearSTUtil.CURRENCY,
                fila);
        if (Util.isEmpty(specificCurrency)) {
            return null;
        }
        String nominal = file.getValue(ForexClearSTUtil.REQMDFCONT, fila);

        if (Util.isEmpty(nominal)) {
            Log.error(this,
                    "No se puede crear el Margin Call Security Trade por motivo de que falta el Price en fila "
                            + fila);
            return null;
        }
        double valorNominal = 0.0;
        try {
            valorNominal = Double.valueOf(nominal);

        } catch (NumberFormatException e) {
            Log.error(this, e);
            return null;
        }

        valorNominal = Math.abs(previousCashPosition) - valorNominal;
        mc.setPrincipal(valorNominal);
        mc.setCurrencyCash(specificCurrency);
        mc.setLinkedLongId(ccId);
        mc.setFlowType(ForexClearSTUtil.COLLATERAL);
      //  mc.setOrdererLeId(ForexClearSTUtil.processingOrg.getId());

        return mc;
    }

    /**
     * getPreviousCashPosition.
     *
     * @param contractId int
     * @return double
     */
    private double getPreviousCashPosition(int contractId) {
        // 15538304
        double result = 0.0;

        List<Integer> arrIds = new ArrayList<Integer>();
        arrIds.add(contractId);
        try {
            final List<MarginCallEntryDTO> entries = CollateralManagerUtil
                    .loadMarginCallEntriesDTO(arrIds,
                            new JDatetime(getValuationDatetime(false))
                                    .getJDate(TimeZone.getDefault()));
            if ((entries != null) && (entries.size() > 0)) {
                MarginCallEntryDTO vo = entries.get(0);
                if (vo.getPreviousCashPosition() != null) {
                    result = vo.getPreviousCashPosition().getValue();
                } else {
                    result = vo.getPreviousCashMargin();
                }
            }
        } catch (RemoteException e) {
            Log.error(this, "Cannot get marginCallEntry for the contract = "
                    + contractId, e);
        }

        return result;
    }

    /**
     * filterLines.
     *
     * @return ArrayList<Integer>
     */
    private ArrayList<Integer> filterLines() {
        ArrayList<Integer> lineasArray = new ArrayList<Integer>();

        for (int linea = 0; linea < this.file.getLinesSize(); linea++) {

            String currency = this.file.getValue(ForexClearSTUtil.CURRENCY,
                    linea);
            if (!Util.isEmpty(currency)
                    && currency.equals(ForexClearSTUtil.USD_CURRENCY)) {
                lineasArray.add(linea);

            }

        }
        return lineasArray;
    }

    /**
     * findContract.
     *
     * @param ds        DSConnection
     * @param error     ArrayList<String>
     * @param lineArray ArrayList<Integer>
     * @return CollateralConfig
     */
    private CollateralConfig findContract(DSConnection ds,
                                          ArrayList<String> error, ArrayList<Integer> lineArray) {
        CollateralConfig cc = null;
        int linea = 0;
        for (int i = 0; i < lineArray.size(); i++) {
            linea = lineArray.get(i);
            String divisa = this.file.getValue(ForexClearSTUtil.CURRENCY,
                    linea);
            if (ForexClearSTUtil.USD_CURRENCY.equals(divisa)) {
                cc = ForexClearSTUtil.findContract(ds, divisa,
                        ForexClearSTUtil.CSA, ForexClearSTUtil.DF);

                break;

            } else {
                error.add("ERROR: El valor de la divisa en la linea (" + linea
                        + ") no es USD");
            }

        }

        if (cc == null) {
            error.add("ERROR: No se ha encontrado el contrato con la divisa ");

        }
        return cc;
    }

    private List<MarginCallEntry> getPriceWorker(final int contractID) {

        List<Integer> mccIDs = new ArrayList<>();
        mccIDs.add(contractID);

        final JDate processDate = new JDatetime(getValuationDatetime(false))
                .getJDate(TimeZone.getDefault());
        final List<MarginCallEntry> listMarginCall = priceWorker(mccIDs,
                processDate);

        return listMarginCall;
    }

    public List<MarginCallEntry> priceWorker(List<Integer> mccIDs,
                                             JDate processDate) {
        MarginCallReportTemplate template = new MarginCallReportTemplate();

        template.put(MarginCallReportTemplate.PROCESS_DATE, processDate);
        template.put(MarginCallReportTemplate.IS_VALIDATE_SECURITIES,
                Boolean.FALSE);
        template.put(MarginCallReportTemplate.IS_CHECK_ALLOCATION_POSITION,
                Boolean.FALSE);
        template.put(MarginCallReportTemplate.CONTRACT_TYPES,
                ForexClearSTUtil.CSA);

        ExecutionContext context = ExecutionContext.getInstance(
                ServiceRegistry.getDefaultContext(),
                ServiceRegistry.getDefaultExposureContext(), template);

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

    private boolean isOnValidDate(int position) {
        boolean isOnValidDate = false;

        String startDateStr = this.file
                .getValue(ForexClearSTUtil.COLUMN_NAME_START_DATE, position);
        String endDateStr = this.file
                .getValue(ForexClearSTUtil.COLUMN_NAME_END_DATE, position);
        if (Util.isEmpty(startDateStr)) {
            Log.error(this, String.format("Column \"%s\" cannot be empty",
                    ForexClearSTUtil.COLUMN_NAME_START_DATE));
        }
        if (Util.isEmpty(endDateStr)) {
            Log.error(this, String.format("Column \"%s\" cannot be empty",
                    ForexClearSTUtil.COLUMN_NAME_END_DATE));
        }
        if (!Util.isEmpty(startDateStr) && !Util.isEmpty(endDateStr)) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        ForexClearSTUtil.DATE_FORMAT);

                Date startDate = dateFormat.parse(startDateStr);
                JDate startJDate = JDate.valueOf(startDate);

                Date endDate = dateFormat.parse(endDateStr);
                JDate endJDate = JDate.valueOf(endDate);

                JDate valuationDate = this.getValuationDatetime()
                        .getJDate(null);

                isOnValidDate = valuationDate.gte(startJDate)
                        && valuationDate.before(endJDate);
            } catch (ParseException e) {
                Log.error(this, "Could not parse date", e);
            }
        }

        return isOnValidDate;
    }

}