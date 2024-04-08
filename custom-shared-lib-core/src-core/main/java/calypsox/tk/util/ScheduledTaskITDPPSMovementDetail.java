package calypsox.tk.util;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.FileUtility;
import calypsox.util.ForexClearFileReader;
import calypsox.util.ForexClearSTUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Creates a margin call cash trade for each movement in the file.
 */
public class ScheduledTaskITDPPSMovementDetail extends ScheduledTask {

    private static final long serialVersionUID = 123L;

    private DSConnection dsCon;

    // Logs
    protected LogGeneric logGen = new LogGeneric();
    protected String fileName = "";

    // Logs

    public String getTaskInformation() {
        return "This report shows the total amount of PPS (Protected Payment System) calls and pays broken down into the individual movements by currency throughout the day";
    }

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

        ArrayList<String> errors = new ArrayList<>();

        // Init values
        dsCon = ds;
        String separator = getAttribute(ForexClearSTUtil.FIELD_SEPARATOR);
        fileName = getAttribute(ForexClearSTUtil.FILE_NAME);
        final String path = getAttribute(ForexClearSTUtil.FILE_PATH);
        final JDate date = this.getValuationDatetime().getJDate(TimeZone.getDefault());

        fileName = ForexClearSTUtil.getFileName(date, fileName);

        // Check atributes
        ForexClearSTUtil.checkAtributes(separator, path, fileName, date, errors);
        if (separator.equalsIgnoreCase("\\t")) {
            separator = "\t";
        }
        if (!errors.isEmpty()) {
            for (String msg : errors) Log.error(this, msg);
            return false;
        }

        // Logs
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
        // Logs

        // Read file
        ForexClearFileReader fReader =
                new ForexClearFileReader(path, fileName, date, separator, errors);
        if (!errors.isEmpty()) {
            for (String err : errors) Log.error(this, err);
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
        if (!copyFile(path, fileName)) { // CR27 Add !
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

        ArrayList<Integer> lineArray = filterLines(fReader);

        // Contract
        CollateralConfig contract = null;
        // Process file
        int position = 0;
        for (int i = 0; i < lineArray.size(); i++) {
            position = lineArray.get(i);

            // Logs
            this.logGen.incrementTotal();
            // Logs
            // Currency
            String currency = fReader.getValue(ForexClearSTUtil.CURRENCY, position);

            if (Util.isEmpty(currency)) {
                errors.add("Currency is empty (line " + position + ")");
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(
                        this.getClass().getSimpleName(),
                        fileName,
                        String.valueOf(this.logGen.getNumberTotal()),
                        "0",
                        "Currency",
                        "",
                        String.valueOf(position));
                continue;
            }

            ForexClearSTUtil.initLegalEntities(dsCon, errors);
            if (!errors.isEmpty()) {
                for (String msg : errors) Log.error(this, msg);
            }
            if (null == contract) {
                contract = findContract(currency, errors);
            }

            if (contract == null) {
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(
                        this.getClass().getSimpleName(),
                        fileName,
                        String.valueOf(this.logGen.getNumberTotal()),
                        "0",
                        "CONTRACT for CCY: " + currency,
                        "",
                        String.valueOf(position));
                continue;
            }

            // Principal (?pay or call?)
            String ppspay = fReader.getValue(ForexClearSTUtil.PPSPAY, position);
            String ppscall = fReader.getValue(ForexClearSTUtil.PPSCALL, position);
            Double principal = null;
            if (!Util.isEmpty(ppspay) && !Util.isEmpty(ppscall)) {
                errors.add("Ppspay and Ppscall are not empty (line " + position + ")");
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(
                        this.getClass().getSimpleName(),
                        fileName,
                        String.valueOf(this.logGen.getNumberTotal()),
                        "0",
                        "Ppspay and Ppscall",
                        "",
                        String.valueOf(position));
                continue;
            } else if (!Util.isEmpty(ppspay)) {
                try {
                    principal = Double.valueOf(ppspay);
                    principal = -principal;
                } catch (NumberFormatException e) {
                    Log.error(this, e);
                    this.logGen.incrementRecordErrors();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(
                            this.getClass().getSimpleName(),
                            fileName,
                            String.valueOf(this.logGen.getNumberTotal()),
                            "0",
                            "Ppspay",
                            "",
                            String.valueOf(position));
                    continue;
                }
            } else if (!Util.isEmpty(ppscall)) {
                try {
                    principal = Double.valueOf(ppscall);
                } catch (NumberFormatException e) {
                    Log.error(this, e);
                    this.logGen.incrementRecordErrors();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(
                            this.getClass().getSimpleName(),
                            fileName,
                            String.valueOf(this.logGen.getNumberTotal()),
                            "0",
                            "Ppscall",
                            "",
                            String.valueOf(position));
                    continue;
                }
            } else {
                errors.add("Ppspay and Ppscall are empty (line " + position + ")");
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(
                        this.getClass().getSimpleName(),
                        fileName,
                        String.valueOf(this.logGen.getNumberTotal()),
                        "0",
                        "Ppspay and Ppscall",
                        "",
                        String.valueOf(position));
                continue;
            }

            if (isNotSavedTradeWithData(getValuationDatetime(), principal, currency)) {

                // create
                Trade tradeFin = createTrade(position, principal, currency, contract, fReader, errors);

                if (!ForexClearSTUtil.checkAndSaveTrade(position, tradeFin, errors)) {
                    // Logs
                    this.logGen.incrementRecordErrors();
                    this.logGen.setErrorSavingTrade(
                            this.getClass().getSimpleName(),
                            fileName,
                            String.valueOf(position),
                            "",
                            String.valueOf(position));
                    // Logs
                } else {
                    this.logGen.incrementOK();
                    this.logGen.setOkLine(
                            this.getClass().getSimpleName(),
                            fileName,
                            position,
                            String.valueOf(tradeFin.getLongId()));
                }
            }
        } // for Process file

        // Revert Trades
        ArrayList<Trade> revertTradesList = getTradesToRevertFromYesterday(errors);
        // Do not revert trades already reverted
        Set<Integer> alreadyRevertedTrades = getTradesAlreadyReverted();
        revertTradesList = filterOutAlreadyRevertedTrades(revertTradesList, alreadyRevertedTrades);

        Trade revertedTrade = null;
        for (Trade trade : revertTradesList) {
            revertedTrade = revertTrade(trade);
            saveRevertedTrade(revertedTrade, errors);
        }

        // Print errors
        for (String err : errors) Log.error(this, err);

        // post process
        try {
            ForexClearFileReader.postProcess(errors.isEmpty(), date, fileName, path);
        } catch (Exception e1) {
            Log.error(this, e1); // sonar
            this.logGen.incrementError();
            this.logGen.setErrorMovingFile(this.getClass().getSimpleName(), fileName);
        }

        // Logs
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
        // Logs

        // Los errores no impiden que la ST termine correctamente.
        return true;
    }

    /**
     * createTrade.
     *
     * @param i         int
     * @param principal double
     * @param currency  String
     * @param contract  CollateralConfig
     * @param fReader   ForexClearFileReader
     * @param errors    ArrayList<String>
     * @return Trade
     */
    private Trade createTrade(
            int i,
            double principal,
            String currency,
            CollateralConfig contract,
            ForexClearFileReader fReader,
            ArrayList<String> errors) {
        // Create margin call
        MarginCall mcall = new MarginCall();
        mcall.setPrincipal(principal);
        mcall.setCurrencyCash(currency);
        mcall.setFlowType(ForexClearSTUtil.COLLATERAL);
        mcall.setLinkedLongId(contract.getId());
       // mcall.setOrdererLeId(ForexClearSTUtil.processingOrg.getId());

        // Create trade
        Trade trade = new Trade();
        trade.setProduct(mcall);
        if (principal < 0) {
            trade.setQuantity(-1);
        } else {
            trade.setQuantity(1);
        }
        trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, contract.getId());
        trade.setTradeDate(this.getValuationDatetime());
        trade.setAction(Action.NEW);
        trade.setStatus(Status.S_NONE);

        trade.setTradeCurrency(currency);
        trade.setSettleCurrency(currency);
        trade.setCounterParty(ForexClearSTUtil.counterParty);
        trade.setBook(contract.getBook());
        trade.setTraderName(ForexClearSTUtil.NONE);
        trade.setSalesPerson(ForexClearSTUtil.NONE);
        trade.setSettleDate(this.getValuationDatetime().getJDate(null));
        ((MarginCall)trade.getProduct()).setOrdererRole("Client");
        ((MarginCall)trade.getProduct()).setOrdererLeId(trade.getCounterParty().getId());
        trade.addKeyword(
                CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE, ForexClearSTUtil.IM_VM_ITD);
        trade.addKeyword(
                CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT, ForexClearSTUtil.REPORT_33A);

        return trade;
    }

    /**
     * isNotSavedTradeWithData.
     *
     * @param d         JDatetime
     * @param principal Double
     * @param currency  String
     * @return boolean
     */
    private boolean isNotSavedTradeWithData(JDatetime d, Double principal, String currency) {

        JDate date = d.getJDate(TimeZone.getDefault());
        Double dPrincipal = Double.valueOf(principal);

        final String from = "trade, product_simplexfer, product_desc, trade_keyword ";

        StringBuffer where = new StringBuffer();
        where.append(" trade.product_id = product_simplexfer.product_id ");
        where.append("and product_desc.product_id = product_simplexfer.product_id ");
        where.append("and trade.trade_id = trade_keyword.trade_id ");
        where.append("and product_simplexfer.quantity = '").append(dPrincipal).append("' ");
        where.append("and trade_keyword.keyword_value ='");
        where.append(ForexClearSTUtil.REPORT_33A).append("' ");
        where.append("and product_desc.product_type = 'MarginCall' ");
        where.append("and trade.trade_status not in('CANCELED') ");
        where.append("and product_simplexfer.cash_currency = '").append(currency).append("' ");
        where.append("and TO_CHAR(trade.trade_date_time,'dd/MM/yyyy') = '").append(date).append("'");

        try {
            final TradeArray tradesSelected =
                    DSConnection.getDefault().getRemoteTrade().getTrades(from, where.toString(), null, null);
            return Util.isEmpty(tradesSelected);
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * findContract.
     *
     * @param currency String
     * @param errors   ArrayList<String>
     * @return CollateralConfig
     */
    private CollateralConfig findContract(String currency, ArrayList<String> errors) {

        // Contract for currency
        CollateralConfig contract =
                ForexClearSTUtil.findContract(
                        dsCon, currency, ForexClearSTUtil.CSA, ForexClearSTUtil.IM_VM_ITD + "_" + currency);
        if (contract == null) {
            errors.add("Contract not found for currency '" + currency + "'");
        }

        return contract;
    }

    /**
     * getTradesToRevertFromYesterday.
     *
     * @param errors ArrayList<String>
     * @return Map<String               ,               Trade>
     */
    private ArrayList<Trade> getTradesToRevertFromYesterday(ArrayList<String> errors) {

        final ArrayList<Trade> tradesToRevert = new ArrayList<Trade>();

        final Map<String, String> tradesIds = new HashMap<String, String>();

        try {

            JDatetime yesterday =
                    JDatetime.addBusiness(
                            getValuationDatetime(),
                            -1,
                            0,
                            0,
                            getHolidays(),
                            TimeZone.getDefault(),
                            DateRoll.R_PRECEDING);

            JDate date = yesterday.getJDate(TimeZone.getDefault());

            final String from = "trade, trade_keyword";
            // retrieve only trades required with ForexClearType to value
            // IM_VM_ITD

            final String where1 =
                    " trade.trade_id = trade_keyword.trade_id and "
                            + "trade_keyword.keyword_name='"
                            + CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE
                            + "' and "
                            + "trade_keyword.keyword_value ='"
                            + ForexClearSTUtil.IM_VM_ITD
                            + "' and "
                            + " TO_CHAR(trade.trade_date_time,'dd/MM/yyyy') = "
                            + " '"
                            + date
                            + "' AND trade.trade_status NOT IN ('CANCELED')";

            final TradeArray tradesSelected =
                    DSConnection.getDefault().getRemoteTrade().getTrades(from, where1, null, null);

            Trade t = null;
            for (Object trade : tradesSelected) {
                t = (Trade) trade;

                tradesIds.put(String.valueOf(t.getLongId()), String.valueOf(t.getLongId()));
            }

            // retrieve only trades required with ForexClearReport to value 33a
            final String where2 =
                    " trade.trade_id = trade_keyword.trade_id and "
                            + "trade_keyword.keyword_name='"
                            + CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT
                            + "' and "
                            + "trade_keyword.keyword_value ='"
                            + ForexClearSTUtil.REPORT_33A
                            + "' and "
                            + " TO_CHAR(trade.trade_date_time,'dd/MM/yyyy') = "
                            + " '"
                            + date
                            + "' AND trade.trade_status NOT IN ('CANCELED')";

            final TradeArray tradesSelected2 =
                    DSConnection.getDefault().getRemoteTrade().getTrades(from, where2, null, null);

            // retrieve mix trades required with both conditions
            String key = null;
            for (Object trade : tradesSelected2) {
                t = (Trade) trade;

                key = String.valueOf(t.getLongId());
                if (tradesIds.get(key) != null) {
                    tradesToRevert.add(t);
                }
            }

        } catch (final RemoteException e) {
            errors.add("Error to get Trades to revert '" + e.toString());
        }

        return tradesToRevert;
    }

    private Set<Integer> getTradesAlreadyReverted() {
        Set<Integer> tradesAlreadyReverted = new TreeSet<Integer>();

        StringBuilder from = new StringBuilder();
        from.append("trade_keyword ");
        from.append(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE);
        from.append(", trade_keyword ");
        from.append(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT);
        from.append(", trade_keyword ");
        from.append(CollateralStaticAttributes.KEYWORD_FOREX_REVERTED_TRADE_ID);

        StringBuilder where = new StringBuilder();

        where.append(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE);
        where.append(".trade_id = trade.trade_id AND ");
        where.append(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE);
        where.append(
                ".keyword_name = '" + CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE + "' AND ");
        where.append(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE);
        where.append(".keyword_value = '" + ForexClearSTUtil.IM_VM_ITD + "' AND ");

        where.append(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT);
        where.append(".trade_id = trade.trade_id AND ");
        where.append(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT);
        where.append(
                ".keyword_name = '" + CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT + "' AND ");
        where.append(CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT);
        where.append(".keyword_value = '" + ForexClearSTUtil.REPORT_33A + "' AND ");

        where.append(CollateralStaticAttributes.KEYWORD_FOREX_REVERTED_TRADE_ID);
        where.append(".trade_id = trade.trade_id AND ");
        where.append(CollateralStaticAttributes.KEYWORD_FOREX_REVERTED_TRADE_ID);
        where.append(
                ".keyword_name = '"
                        + CollateralStaticAttributes.KEYWORD_FOREX_REVERTED_TRADE_ID
                        + "' AND ");

        JDatetime valuationDate = getValuationDatetime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        where.append(
                "TRUNC(trade.trade_date_time) = TO_DATE('"
                        + dateFormat.format(valuationDate)
                        + "', 'dd/mm/yyyy')");

        try {
            TradeArray revertingTrades =
                    DSConnection.getDefault()
                            .getRemoteTrade()
                            .getTrades(from.toString(), where.toString(), "trade.trade_id", null);
            for (int iTrade = 0; iTrade < revertingTrades.size(); iTrade++) {
                Trade trade = revertingTrades.get(iTrade);
                int revertedTradeId =
                        trade.getKeywordAsInt(CollateralStaticAttributes.KEYWORD_FOREX_REVERTED_TRADE_ID);
                if (revertedTradeId > 0) {
                    tradesAlreadyReverted.add(revertedTradeId);
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve trades already reverted", e);
        }

        return tradesAlreadyReverted;
    }

    private ArrayList<Trade> filterOutAlreadyRevertedTrades(
            ArrayList<Trade> tradesToRevert, Set<Integer> alreadyRevertedTrades) {
        ArrayList<Trade> filteredTrades = new ArrayList<Trade>();

        for (int iTrade = 0; iTrade < tradesToRevert.size(); iTrade++) {
            Trade trade = tradesToRevert.get(iTrade);
            if (!alreadyRevertedTrades.contains(trade.getLongId())) {
                filteredTrades.add(trade);
            }
        }

        return filteredTrades;
    }

    /**
     * filterLines.
     *
     * @return ArrayList<Integer>
     */
    private ArrayList<Integer> filterLines(ForexClearFileReader fReader) {
        ArrayList<Integer> lineasArray = new ArrayList<Integer>();

        for (int linea = 0; linea < fReader.getLinesSize(); linea++) {

            String account = fReader.getValue(ForexClearSTUtil.ACCOUNT, linea);
            if (!Util.isEmpty(account) && (account.equals(ForexClearSTUtil.ACCOUNT_H))) {
                lineasArray.add(linea);
            }
        }

        return lineasArray;
    }

    /**
     * revertTrade.
     *
     * @param t Trade
     * @return Trade
     */
    private Trade revertTrade(Trade t) {
        Trade newTrade = new Trade();

        MarginCall mc = (MarginCall) t.getProduct();
        newTrade.setProduct(revertMarginCall(mc));

        newTrade.addKeyword(
                CollateralStaticAttributes.MC_CONTRACT_NUMBER,
                getKeywordValue(t, CollateralStaticAttributes.MC_CONTRACT_NUMBER));

        newTrade.addKeyword(
                CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE,
                getKeywordValue(t, CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_TYPE));

        newTrade.addKeyword(
                CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT,
                getKeywordValue(t, CollateralStaticAttributes.KEYWORD_FOREX_CLEAR_REPORT));

        // This keyword indicates the id of the trade being reverted by this new
        // trade
        newTrade.addKeywordAsLong(
                CollateralStaticAttributes.KEYWORD_FOREX_REVERTED_TRADE_ID, t.getLongId());

        newTrade.setTradeCurrency(t.getTradeCurrency());
        newTrade.setSettleCurrency(t.getSettleCurrency());
        newTrade.setCounterParty(t.getCounterParty());
        newTrade.setAction(Action.NEW);
        newTrade.setStatus(Status.S_NONE);
        newTrade.setBook(t.getBook());
        newTrade.setTradeDate(this.getValuationDatetime());
        newTrade.setSettleDate(this.getValuationDatetime().getJDate(null));
        newTrade.setTraderName(t.getTraderName());
        newTrade.setSalesPerson(t.getSalesPerson());
        newTrade.setQuantity(t.getQuantity() * (-1));

        return newTrade;
    }

    /**
     * getKeywordValue.
     *
     * @param t   Trade
     * @param key String
     * @return String
     */
    private String getKeywordValue(Trade t, String key) {
        return t.getKeywordValue(key);
    }

    /**
     * revertMarginCall.
     *
     * @param mc MarginCall
     * @return MarginCall
     */
    private MarginCall revertMarginCall(MarginCall mc) {
        // Create Margin Call
        MarginCall newMc = new MarginCall();

        newMc.setPrincipal(mc.getPrincipal());
        newMc.setCurrencyCash(mc.getCurrencyCash());
        newMc.setLinkedLongId(mc.getLinkedLongId());
        newMc.setFlowType(mc.getFlowType());
        newMc.setOrdererLeId(mc.getOrdererLeId());

        return newMc;
    }

    /**
     * saveRevertedTrade.
     *
     * @param trade  Trade
     * @param errors ArrayList<String>
     */
    private void saveRevertedTrade(Trade trade, ArrayList<String> errors) {
        long idTrade = -1;
        try {
            idTrade = DSConnection.getDefault().getRemoteTrade().save(trade);
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(), e);
            errors.add("Failed to save the Reverted Trade");
        }
        if (idTrade <= 0) {
            errors.add("Failed to save the Reverted Trade");
        } else {
            Log.info(this.getClass().getSimpleName(), "Reverted Trade saved with id " + idTrade);
        }
    }

    private boolean copyFile(String path, String fileName) {
        try {
            JDatetime dateTime = getValuationDatetime();
            SimpleDateFormat format = new SimpleDateFormat("HHmm");

            final String outputFileName =
                    path
                            + File.separator
                            + "copy"
                            + File.separator
                            + format.format(dateTime)
                            + "_"
                            + fileName;
            FileUtility.copyFile(path + File.separator + fileName, outputFileName);

        } catch (IOException e) {
            Log.error(ForexClearFileReader.class, e);
            return false;
        }
        return true;
    }
}
