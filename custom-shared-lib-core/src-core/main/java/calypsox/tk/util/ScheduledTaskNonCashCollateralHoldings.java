package calypsox.tk.util;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.ForexClearFileReader;
import calypsox.util.ForexClearSTUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class ScheduledTaskNonCashCollateralHoldings extends ScheduledTask {

    private static final long serialVersionUID = 123L;

    private CollateralConfig defaultContract = null;
    private ForexClearFileReader file = null;

    // Logs
    protected LogGeneric logGen = new LogGeneric();
    protected String fileName = "";
    // Logs

    /**
     * Devuelve la descripcion de la Scheduled Task
     */
    public String getTaskInformation() {
        return "Generar? un Margin Call Security Trade por cada movimiento de tipo Security en el fichero";
    }

    /**
     * Devuelve la lista de los atributos de Scheduled Task
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
        final JDate date = this.getValuationDatetime()
                .getJDate(TimeZone.getDefault());
        ArrayList<String> error = new ArrayList<>();

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
        startLogs(date);
        // Logs

        // Lectura del fichero
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
        if (file.getLinesSize() == 0) {
            Log.error(this, "El fichero esta vacio");
            // Logs
            this.logGen.incrementError();
            this.logGen.setErrorNumberOfLines(this.getClass().getSimpleName(),
                    fileName);
            ForexClearSTUtil.returnErrorLog(logGen, false, date, fileName, path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            // Logs
            return true;
        }

        ForexClearSTUtil.initLegalEntities(ds, error);
        if (!error.isEmpty()) {
            for (String msg : error)
                Log.error(this, msg);
            // Logs
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(
                    this.getClass().getSimpleName(), fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "0",
                    "PO or CTPY", "", "");
            ForexClearSTUtil.returnErrorLog(logGen, false, date, fileName, path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            // Logs
            return false; // cuando el resultado es false se introduce al array
            // de fallos
        }

        // Contract [EUR, CSA, IM]
        this.defaultContract = ForexClearSTUtil.findContract(ds,
                ForexClearSTUtil.CCY_EUR, ForexClearSTUtil.CSA,
                ForexClearSTUtil.IM);
        if (this.defaultContract == null) {
            // Logs
            this.logGen.incrementRecordErrors();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(
                    this.getClass().getSimpleName(), fileName,
                    String.valueOf(this.logGen.getNumberTotal()), "0",
                    "CONTRACT for CCY: EUR", "", "");
            // Logs
            error.add("ERROR: no se encuentra el contrato con divisa EUR");
            return false;
        }

        Product product = null;

        // Process file
        for (int line = 0; line < file.getLinesSize(); line++) {

            String accountType = this.file.getValue(ForexClearSTUtil.ACCOUNT,
                    line);
            if (ForexClearSTUtil.ACCOUNT_H.equalsIgnoreCase(accountType)) {
                // Logs
                this.logGen.incrementTotal();
                // Logs

                // If ISIN value is empty in this line, go to the next line
                String isinInFile = file.getValue(ForexClearSTUtil.ISIN, line);
                if (Util.isEmpty(isinInFile)) {
                    // Logs
                    this.logGen.incrementRecordErrors();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(
                            this.getClass().getSimpleName(), fileName,
                            String.valueOf(this.logGen.getNumberTotal()), "0",
                            "ISIN", "", String.valueOf(line));
                    // Logs
                    error.add("ERROR: Campo vacio en la columna ISIN y linea "
                            + line);
                    continue;
                }

                // Get the Security (Bond or Equity)
                product = BOCache.getExchangeTradedProductByKey(
                        DSConnection.getDefault(), ForexClearSTUtil.ISIN,
                        isinInFile);

                // If don't exist BONDs with this ISIN value, go to the next
                // line
                if (product == null) {
                    error.add(
                            "ERROR: No se ha encontrado el security con el ISIN: "
                                    + isinInFile);
                    // Logs
                    this.logGen.incrementRecordErrors();
                    this.logGen.setErrorGettingBond(
                            this.getClass().getSimpleName(), fileName,
                            String.valueOf(this.logGen.getNumberTotal()), "",
                            String.valueOf(line));
                    // Logs
                    this.crearTask(line, isinInFile, ds, error); // create error
                    // Task
                    continue;
                }

                // Create the MarginCall SECURITY Trade.
                Trade trade = this.createTrade(product, line,
                        this.defaultContract, error);

                if (!ForexClearSTUtil.checkAndSaveTrade(line, trade, error)) {
                    // Logs
                    this.logGen.incrementRecordErrors();
                    this.logGen.setErrorSavingTrade(
                            this.getClass().getSimpleName(), fileName,
                            String.valueOf(line), "", String.valueOf(line));
                    // Logs
                } else {
                    this.logGen.incrementOK();
                    this.logGen.setOkLine(this.getClass().getSimpleName(),
                            fileName, line, String.valueOf(trade.getLongId()));
                }
            }
        }
        // Si no ha habido ningun error true, en caso de que haya fallado 1
        // false
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
        stopLogs();
        // Logs

        return error.isEmpty();
    }

    /**
     * Crear un task para que el usuario cree un bono con un ISIN especifico
     *
     * @param lineNumber
     * @param isin
     * @param dsCon
     * @param error
     */
    private void crearTask(int lineNumber, String isin, DSConnection dsCon,
                           ArrayList<String> error) {
        try {
            Task task = new Task();
            task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
            task.setEventType("EX_FOREXCLEAR");
            task.setPriority(Task.PRIORITY_NORMAL);
            task.setDatetime(JDate.getNow().getJDatetime());
            task.setComment("The following line (Row number : "
                    + (lineNumber + 1) + "), Security with ISIN : " + isin
                    + " does not exist.");
            dsCon.getRemoteBO().save(task);
        } catch (CalypsoServiceException e) {
            Log.error(this, e);// sonar 03/01/2018
            error.add(
                    "ERROR - Fallo al crear el Task de error por ISIN no existente, ISIN: "
                            + isin);
        }
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
    private Trade createTrade(Product product, int line,
                              final CollateralConfig contract, ArrayList<String> error) {
        // Create Trade

        final String currency = this.file.getValue(ForexClearSTUtil.CURRENCY,
                line);
        if (Util.isEmpty(currency)) {
            return null;
        }

        MarginCall mc = this.createMarginCall(product, contract, line);
        if (mc == null) {
            error.add(
                    "ERROR: No se ha creado el Margin Call Security Trade, el motivo es que no existe divisa en la fila "
                            + line);
            return null;
        }

        Trade trade = new Trade();
        trade.setProduct(mc);
        trade.setTradeCurrency(currency);
        trade.setSettleCurrency(currency);
        trade.setCounterParty(ForexClearSTUtil.counterParty);
        trade.setTraderName(ForexClearSTUtil.NONE);
        trade.setSalesPerson(ForexClearSTUtil.NONE);
        trade.setAction(Action.NEW);
        trade.setStatus(Status.S_NONE);
        trade.setBook(contract.getBook());
        trade.setTradeDate(this.getValuationDatetime());
        trade.setSettleDate(this.getValuationDatetime().getJDate(null));

        trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER,
                contract.getId());

        // Custodian
        String custodian = this.file.getValue(ForexClearSTUtil.CUSTODIAN, line);
        if (Util.isEmpty(custodian)) {
            error.add(
                    "No se puede crear el Margin Call Security Trade por motivo de que falta el Custodian, en la fila "
                            + line);
            return null;
        }
        trade.addKeyword(ForexClearSTUtil.CUSTODIAN_CTE, custodian);

        // Units - Quantity
        String unit = this.file.getValue(ForexClearSTUtil.UNITS, line);
        double units = 0.0;
        if (Util.isEmpty(unit)) {
            return null;
        }
        try {
            units = Double.valueOf(unit);
        } catch (NumberFormatException e) {
            Log.error(this, e);
            return null;
        }

        if (product instanceof Bond) {
            double facevalue = ((Bond) product).getFaceValue();
            trade.setQuantity((units * facevalue));
        } else {
            trade.setQuantity(units);
        }


        // Price - DirtyPrice
        String price = this.file.getValue(ForexClearSTUtil.PRICE, line);
        double dirtyPrice = 0.0;
        if (Util.isEmpty(price)) {
            return null;
        }

        try {
            dirtyPrice = CollateralUtilities.parseStringAmountToDouble(price);
        } catch (NumberFormatException e) {
            Log.error(this, e);
            return null;
        }
        trade.setTradePrice(dirtyPrice); // If is not Bond or dirtyPrice = 0.0
        if (dirtyPrice != 0.0 && product instanceof Bond) {
            trade.setTradePrice(dirtyPrice / 100);
        }

        // Accrual
        trade.setAccrual(0.0);

        // ForexClear
        addForexClearKeywords(trade, line);
        // ForexClear - End

        return trade;
    }

    /**
     * Crear un Margin Call a partir de la divisa, Processing Org, el id del
     * contrato y el tipo.
     *
     * @param product
     * @param line
     * @param fila
     * @return
     */
    private MarginCall createMarginCall(final Product product,
                                        final CollateralConfig contract, int line) {
        // Create Margin Call
        final String specificCurrency = this.file
                .getValue(ForexClearSTUtil.CURRENCY, line);

        MarginCall mc = new MarginCall();
        mc.setSecurity(product);
        mc.setOrdererLeId(ForexClearSTUtil.processingOrg.getId());
        mc.setCurrencyCash(specificCurrency);
        mc.setLinkedLongId(contract.getId());
        mc.setFlowType(ForexClearSTUtil.SECURITY);

        // Set Pledged Security to TRUE
        mc.setIsPledgeMovementB(true);

        return mc;
    }

    /**
     * Add the ForexClear Trade Keywords.
     *
     * @param trade
     * @param description
     */
    private void addForexClearKeywords(final Trade trade, final int line) {
        trade.addKeyword(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT,
                ForexClearSTUtil.REPORT_36A);
        final String account = this.file.getValue(ForexClearSTUtil.ACCOUNT,
                line);
        if (ForexClearSTUtil.ACCOUNT_H.equalsIgnoreCase(account)) {
            trade.addKeyword(
                    CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE,
                    ForexClearSTUtil.IM);
        }
    }

    private void startLogs(final JDate date) {
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
    }

    private void stopLogs() {
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
    }

}