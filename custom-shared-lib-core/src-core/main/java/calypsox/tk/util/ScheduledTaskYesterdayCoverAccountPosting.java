package calypsox.tk.util;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.ForexClearFileReader;
import calypsox.util.ForexClearSTUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.IOException;
import java.util.*;

public class ScheduledTaskYesterdayCoverAccountPosting extends ScheduledTask {

    private static final long serialVersionUID = 123L;
    private ForexClearFileReader file = null;

    // Logs
    protected LogGeneric logGen = new LogGeneric();
    protected String fileName = "";
    // Logs

    /**
     * Devuelve la descripcion de la Scheduled Task
     */
    public String getTaskInformation() {
        return "Generar? un Simple Transfer de tipo PAI.";
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

        // ForexClear Process
        startForexClearProcess(ds, error);

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

    private void startForexClearProcess(final DSConnection ds,
                                        ArrayList<String> error) {
        HashMap<Integer, String> paiMap = new HashMap<>();
        HashMap<Integer, CollateralConfig> contracts = new HashMap<>();

        this.findContract(ds, paiMap, contracts, error);
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

        for (Map.Entry<Integer, String> entry : paiMap.entrySet()) {
            int i = 0; // Log
            int numLine = entry.getKey();
            String desc = entry.getValue();

            // Logs
            this.logGen.incrementTotal();
            // Logs
            String currency = this.file.getValue(ForexClearSTUtil.CURRENCY,
                    numLine);
            CollateralConfig cc = contracts.get(numLine);
            if (cc == null) {
                error.add(
                        "ERROR: No se ha encontrado el contrato con la divisa ("
                                + currency + ")");
                // Logs
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(
                        this.getClass().getSimpleName(), fileName,
                        String.valueOf(this.logGen.getNumberTotal()), "0",
                        "CONTRACT FOR CURRENCY:" + currency, "", "");
                // Logs
                continue;
            }

            Trade trade = this.createTrade(numLine, cc, currency, desc, error);

            if (!ForexClearSTUtil.checkAndSaveTrade(numLine, trade, error)) {
                // Logs
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorSavingTrade(this.getClass().getSimpleName(),
                        fileName, String.valueOf(i), "", String.valueOf(i));
                // Logs
            } else {
                this.logGen.incrementOK();
                this.logGen.setOkLine(this.getClass().getSimpleName(), fileName,
                        i, String.valueOf(trade.getLongId())); // poner trade ID
            }
            i++;
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

    private double findPrincipal(int linea, ArrayList<String> error) {
        String debit = this.file.getValue(ForexClearSTUtil.POSTINGDEBIT, linea);
        String credit = this.file.getValue(ForexClearSTUtil.POSTINGCREDIT,
                linea);
        double debito = 0.0;
        double credito = 0.0;
        if (Util.isEmpty(debit)) {
            error.add("El campo de posting debit esta vacio en la linea ("
                    + linea + ")");
            Log.error(this, "El campo de posting debit esta vacio en la linea ("
                    + linea + ")");
            return 0.0;
        }
        try {
            debito = Double.valueOf(debit);
        } catch (NumberFormatException e) {
            Log.error(this, e);
        }
        if (Util.isEmpty(credit)) {
            error.add("El campo de posting credit esta vacio en la linea ("
                    + linea + ")");
            Log.error(this,
                    "El campo de posting credit esta vacio en la linea ("
                            + linea + ")");
            return 0.0;
        }
        try {
            credito = Double.valueOf(credit);
        } catch (NumberFormatException e) {
            Log.error(this, e);
        }
        if (debito != 0.0 && credito != 0.0) {
            error.add(
                    "No pueden estar los dos campos (posting debit y credit) inicializados");
        } else if ((debito == 0.0 && credito == 0.0)) {
            error.add(
                    "No pueden estar los dos campos (posting debit y credit) inicializados a 0");
        } else if (debito == 0.0) {
            return credito;
        } else {
            return debito;
        }
        return 0.0;
    }

    // DDR ForexClear

    private MarginCall createMarginCall(int linea, CollateralConfig cc,
                                        String specificCurrency) {

        // Crear Margin Call
        MarginCall mc = new MarginCall();
        mc.setSecurity(null);
        mc.setFlowType(ForexClearSTUtil.COLLATERAL);
        mc.setOrdererLeId(ForexClearSTUtil.processingOrg.getId());

        ArrayList<String> error = new ArrayList<String>();
        double principal = this.findPrincipal(linea, error);
        if (!Util.isEmpty(error)) {
            for (String msg : error)
                Log.error(this, msg);
            return null;
        }
        mc.setPrincipal(principal);
        mc.setCurrencyCash(specificCurrency);
        mc.setLinkedLongId(cc.getId());

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
    private Trade createTrade(int linea, CollateralConfig cc, String currency,
                              String description, ArrayList<String> error) {

        MarginCall mc = this.createMarginCall(linea, cc, currency);
        if (mc == null) {
            error.add("ERROR: No se ha podido crear el Margin Call ");
            return null;
        }

        Trade trade = new Trade();
        trade.setProduct(mc);
        trade.setTraderName(ForexClearSTUtil.NONE);
        trade.setSalesPerson(ForexClearSTUtil.NONE);
        trade.setBook(cc.getBook());

        trade.setTradeDate(this.getValuationDatetime());
        trade.setSettleDate(this.getValuationDatetime().getJDate(null));
        trade.setAction(Action.NEW);
        trade.setCounterParty(ForexClearSTUtil.counterParty);
        trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER,
                cc.getId());
        trade.setAdjustmentType(ForexClearSTUtil.BUYSELL);
        trade.setTradeCurrency(currency);
        trade.setSettleCurrency(currency);
        trade.setEnteredUser(DSConnection.getDefault().getUser());
        trade.setQuantity(getQuantity(linea, mc.getPrincipal()));
        ((MarginCall) trade.getProduct()).setPrincipal(((MarginCall) trade.getProduct()).getPrincipal() * trade.getQuantity());
        ((MarginCall)trade.getProduct()).setOrdererRole("Client");
        ((MarginCall)trade.getProduct()).setOrdererLeId(trade.getCounterParty().getId());

        // ForexClear - Trade Keywords
        addForexClearKeywords(trade, description);

        return trade;
    }

    // Quantity: if debit/PAY(-) > -1 ; credit/RECEIVE(+) > +1
    private double getQuantity(final int linea, final double principal) {

        final String debit = this.file.getValue(ForexClearSTUtil.POSTINGDEBIT,
                linea);
        final String credit = this.file.getValue(ForexClearSTUtil.POSTINGCREDIT,
                linea);

        if (principal == Double.valueOf(debit)) {
            return -1.0;
        } else if (principal == Double.valueOf(credit)) {
            return 1.0;
        }

        return 0.0;
    }

    /**
     * Add the ForexClear Trade Keywords.
     *
     * @param trade
     * @param description
     */
    private void addForexClearKeywords(final Trade trade,
                                       final String description) {
        trade.addKeyword(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT,
                ForexClearSTUtil.REPORT_22);
        if (description.equals(ForexClearSTUtil.VM_EOD_POST)) {
            trade.addKeyword(
                    CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE,
                    ForexClearSTUtil.VM);
        } else if (description.equals(ForexClearSTUtil.PR_AL_INTST)) {
            trade.addKeyword(
                    CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE,
                    ForexClearSTUtil.PAI);
        }
    }

    /**
     * Filtramos los datos del fichero. Buscamos aquellas líneas del fichero
     * que tengan:
     * <p>
     * - Page 2 - Account H - Description VM EOD Post || PR AL INTST
     *
     * @param ds
     * @param currency
     * @return
     */
    private void findContract(DSConnection ds, HashMap<Integer, String> paiMap,
                              HashMap<Integer, CollateralConfig> contracts,
                              ArrayList<String> error) {
        for (int linea = 0; linea < this.file.getLinesSize(); linea++) {
            String descripcion = this.file
                    .getValue(ForexClearSTUtil.POSTINGDESCRIPTION, linea)
                    .toUpperCase();
            String page = this.file.getValue(ForexClearSTUtil.PAGE, linea);
            String account = this.file.getValue(ForexClearSTUtil.ACCOUNT, linea)
                    .toUpperCase();

            // Page 2 and Account H
            if (!Util.isEmpty(page) && page.equals(ForexClearSTUtil.DOS)
                    && !Util.isEmpty(account)
                    && account.equals(ForexClearSTUtil.ACCOUNT_H)) {
                if (!Util.isEmpty(descripcion)) {
                    if (descripcion.equals(ForexClearSTUtil.VM_EOD_POST)) { // Description
                        // =
                        // VM
                        // EOD
                        // POST
                        addContract(ds, paiMap, contracts, error, linea,
                                descripcion, ForexClearSTUtil.CSA,
                                ForexClearSTUtil.VM);
                    } else if (ForexClearSTUtil.PR_AL_INTST.equals(descripcion)) { // Description
                        // = PR AL
                        // INTST
                        addContract(ds, paiMap, contracts, error, linea,
                                descripcion, ForexClearSTUtil.CSA,
                                ForexClearSTUtil.PAI);
                    }
                }
            }
        }
    }

    /**
     * According to the contractType and ForexClear, find and add the contract.
     *
     * @param ds
     * @param paiMap
     * @param contracts
     * @param error
     * @param linea
     * @param descripcion
     * @param contractType
     * @param forexClear
     */
    private void addContract(DSConnection ds, HashMap<Integer, String> paiMap,
                             HashMap<Integer, CollateralConfig> contracts,
                             ArrayList<String> error, int linea, String descripcion,
                             String contractType, String forexClear) {

        String currency = this.file.getValue(ForexClearSTUtil.CURRENCY, linea);
        if (Util.isEmpty(currency)) {
            error.add("ERROR: El valor de la divisa en la linea (" + linea
                    + ") esta vacia");
        } else {
            CollateralConfig contractAux = ForexClearSTUtil.findContract(ds,
                    currency, contractType, forexClear);
            if (contractAux == null) {
                error.add(
                        "ERROR: No se ha encontrado el contrato con la divisa ("
                                + currency + ")");
            } else {
                paiMap.put(linea, descripcion);
                contracts.put(linea, contractAux);
            }
        }
    }

    // DDR ForexClear - End
}
