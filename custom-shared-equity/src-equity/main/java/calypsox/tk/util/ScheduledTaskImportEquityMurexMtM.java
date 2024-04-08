package calypsox.tk.util;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ScheduledTaskImportEquityMurexMtM extends ScheduledTask{

    public static final String SCHEDULED_TASK = "ScheduledTaskImportEquityMurexMtM";
    public static final String FILE_NAME = "File Name";
    public static final String FILE_PATH = "File Path";
    public static final String MX_TRADE_KEYWORD = "Murex Trade Keyword";
    private boolean returnExit = true;
    protected String fileName = "";
    protected String path = "";
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static int SQL_GET_SIZE = 999;
    String TRADE_FILTER = "ALL";


    @Override
    public String getTaskInformation() {
        return "Import BRS MtM from Data Lake";
    }


    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {

        path = getAttribute(FILE_PATH);
        final JDate date = this.getValuationDatetime().getJDate(TimeZone.getDefault());
        fileName = getAttribute(FILE_NAME);
        fileName = getFileName(date, fileName);
        String mxTradeKw = getAttribute(MX_TRADE_KEYWORD);

        List<String> lines = readFile();
        Map<String, MTMData> mtmAllDataMap = parseLines(lines);
        TradeArray tradeArray = loadTradesByKeyword(this.getDSConnection(), mtmAllDataMap, mxTradeKw);

        if (!Util.isEmpty(tradeArray)) {
            matchTades(mtmAllDataMap, tradeArray, mxTradeKw);
            final List<MTMData> finalList = mtmAllDataMap.entrySet().stream()
                    .filter(ent -> ent.getValue().getTrade() != null)
                    .map(Map.Entry::getValue).collect(Collectors.toList());
            createAndSavePlMarks(finalList, tradeArray);
            Log.info(this, "All trades saved.");
        }

        return this.returnExit;
    }


    /**
     * @param ds
     * @param
     * @return
     */
    private TradeArray loadTradesByKeyword(DSConnection ds, Map<String, MTMData> mtmAllDataMap, String mxTradeKw) {
        TradeArray trades = new TradeArray();

        if (!Util.isEmpty(mtmAllDataMap)) {
            List<MTMData> mtmDataList = new ArrayList<>(mtmAllDataMap.values());
            int size = SQL_GET_SIZE;
            Log.info(this, "Loading trades by Trade Keyword");
            for (int start = 0; start < mtmDataList.size(); start += size) {
                int end = Math.min(start + size, mtmDataList.size());

                final List<String> listReferences = mtmDataList.subList(start, end).stream().map(MTMData::getMxID).collect(Collectors.toList());
                String references = String.join("','", listReferences);
                try {
                    StringBuilder where = new StringBuilder();
                    where.append(" TRADE.TRADE_STATUS NOT IN ('CANCELED') ");
                    where.append(" AND TRADE_KEYWORD.KEYWORD_NAME = ");
                    where.append("'" + mxTradeKw + "'");
                    where.append(" AND TRADE_KEYWORD.KEYWORD_VALUE IN (");
                    where.append("'" + references + "'");
                    where.append(")");
                    where.append(" AND TRADE_KEYWORD.TRADE_ID = TRADE.TRADE_ID");

                    trades.addAll(ds.getRemoteTrade().getTrades("trade_keyword",where.toString() ,"" ,null));

                } catch (CalypsoServiceException e) {
                    Log.info("Cannot get trades for ", e);
                }
            }
        }
        Log.info(this, trades.size() + " Trades loaded.");

        return trades;
    }


    /**
     * @param mtmAllDataMap
     * @param tradearray
     * @param keyword
     */
    private void matchTades(Map<String, MTMData> mtmAllDataMap, TradeArray tradearray, String keyword) {
        Log.info(this, "Matching trades.");

        List<String> references = new ArrayList<>();
        for (Trade trade : tradearray.getTrades()) {
            if (!references.contains(trade.getKeywordValue(keyword))) {
                MTMData mtmData = mtmAllDataMap.get(trade.getKeywordValue(keyword));
                if (mtmData != null) {
                    mtmData.setTrade(trade);
                } else {
                    Log.info(this, "Trade " + trade.getLongId() + " not found on file. ");
                }
                references.add(trade.getKeywordValue(keyword));
            } else {
                Log.info(this, "Duplicate trade with same STM_REFERENCE: " + trade.getKeywordValue(keyword) + " tradeId: " + trade.getLongId());
            }

        }
    }


    /**
     * @param
     * @return
     */
    private void createAndSavePlMarks(List<MTMData> mtmAllData, TradeArray tradeArray) {
        List<PLMark> plMarks = new ArrayList<>();

        Vector holidays = getHolidays();
        if(Util.isEmpty(holidays)){
            holidays = new Vector();
            holidays.add("SYSTEM");
        }

        HashMap<String, Double> fxRates = new HashMap<String, Double>();

        TradeFilter tradeFilter =  BOCache.getTradeFilter(DSConnection.getDefault(), TRADE_FILTER);
        if (tradeFilter == null) {
            Log.error(this, "Error retrieving Trade Filter "+ TRADE_FILTER);
            return;
        }

        String pricingEnv = getPricingEnv();
        TradeArray tradesToSave = new TradeArray();
        if (!Util.isEmpty(mtmAllData)) {
            Log.info(this, mtmAllData.size() + " Matched trades.");

            for (MTMData mtmData : mtmAllData) {
                Trade trade = mtmData.getTrade();
                if (trade == null) {
                    continue;
                }

                long mirrorTradeId = trade.getMirrorTradeLongId();
                if (mirrorTradeId > 0L) {
                    Trade mirrorTrade = getTradeWithId(tradeArray, mirrorTradeId);
                    if (trade.getLongId() < mirrorTradeId) {
                        addPLMarkForTrade(plMarks, trade, mtmData, tradesToSave, pricingEnv, holidays, tradeFilter, fxRates, 1);
                        if (mirrorTrade != null) {
                            addPLMarkForTrade(plMarks, mirrorTrade, mtmData, tradesToSave, pricingEnv, holidays, tradeFilter, fxRates, -1);
                        }
                    }
                    else {
                        addPLMarkForTrade(plMarks, trade, mtmData, tradesToSave, pricingEnv, holidays, tradeFilter, fxRates, -1);
                        if (mirrorTrade != null) {
                            addPLMarkForTrade(plMarks, mirrorTrade, mtmData, tradesToSave, pricingEnv, holidays, tradeFilter, fxRates, 1);
                        }
                    }
                }
                else {
                    addPLMarkForTrade(plMarks, trade, mtmData, tradesToSave, pricingEnv, holidays, tradeFilter, fxRates, 1);
                }
            }
        }

        try {
            savePLMarks(plMarks);
        } catch (InterruptedException e) {
            Log.error(this, "Error saving PLMarks. " + e);
        }
    }


    private Trade getTradeWithId(TradeArray tradeArray, long id) {
        for (Trade trade : tradeArray.asList()) {
            if (trade.getLongId() == id) {
                return trade;
            }
        }

        return null;
    }


    private void addPLMarkForTrade(List<PLMark> plMarks, Trade trade, MTMData mtmData, TradeArray tradesToSave, String pricingEnv, Vector holidays, TradeFilter tradeFilter, HashMap<String, Double> fxRates, int sign) {
        PLMark plMark = new PLMark();
        plMark.setTradeId(trade.getLongId());
        Book book = BOCache.getBook(DSConnection.getDefault(), trade.getBookId());
        if (book != null) {
            plMark.setBookId(book.getId());
        }
        plMark.setValDate(mtmData.getDate());
        plMark.setPricingEnvName(pricingEnv);
        plMark.setType("PL");
        plMark.addPLMarkValue(createPLMarkValue("MTM_FULL_LAGO", mtmData.getMtmCcy(), mtmData.getMtmCcy(), mtmData.getMtmValue() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue("MTM_MARKET_VALUE", mtmData.getMtmCcy(), mtmData.getMtmCcy(), mtmData.getMtmMarketValue() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue("MTM_FUTURE_CASH", mtmData.getMtmCcy(), mtmData.getMtmCcy(), mtmData.getMtmFutureCash() * sign, "NPV"));
        plMarks.add(plMark);
    }


    private PLMarkValue createPLMarkValue(String name, String ccy, String ccy2, double mtm, String type) {
        PLMarkValue npvPriceMarkValue = new PLMarkValue();
        npvPriceMarkValue.setMarkName(name);
        npvPriceMarkValue.setMarkValue(mtm);
        npvPriceMarkValue.setCurrency(ccy);
        npvPriceMarkValue.setOriginalCurrency(ccy2);
        npvPriceMarkValue.setAdjustmentType(type);
        return npvPriceMarkValue;
    }


    /**
     * @param plMarks
     * @throws InterruptedException
     */
    private void savePLMarks(List<PLMark> plMarks) throws InterruptedException {
        if (!Util.isEmpty(plMarks)) {
            int size = SQL_GET_SIZE;
            Log.info(this, "Saving " + plMarks.size() + " PLMarks.");
            ExecutorService exec = Executors.newFixedThreadPool(NUM_CORES);
            try {
                for (int start = 0; start < plMarks.size(); start += size) {
                    int end = Math.min(start + size, plMarks.size());
                    List<PLMark> plMarksToSave = new ArrayList<>(plMarks.subList(start, end));
                    exec.execute(
                            new Runnable() {
                                public void run() {
                                    try {
                                        DSConnection.getDefault().getRemoteMark().saveMarksWithAudit(plMarksToSave, true);
                                    } catch (PersistenceException e) {
                                        Log.error(this, "Cannot save PLMarks. " + e);
                                    }
                                }
                            });
                }
            } finally {
                exec.shutdown();
                exec.awaitTermination(40, TimeUnit.MINUTES);
            }
        }
    }


    public static String getFileName(JDate date, String name) {
        String year = String.format("%04d", date.getYear());
        String month = String.format("%02d", date.getMonth());
        String day = String.format("%02d", date.getDayOfMonth());
        final String fileName = name + year + month + day + ".DAT";

        return fileName;
    }


    /**
     * @return Lines of the file
     */
    private List<String> readFile() {
        final List<String> lines = new ArrayList<String>();

        BufferedReader inputFileStream = null;

        if(!Util.isEmpty(fileName) && !Util.isEmpty(path)) {
            try {
                // We read the file.
                inputFileStream = new BufferedReader(new FileReader(path + fileName));
                String line;
                while (inputFileStream.ready()) {
                    line = inputFileStream.readLine();
                    lines.add(line);
                }

                Log.info(Log.CALYPSOX, "Finished reading process of import MTM");

            } catch (final FileNotFoundException e) {

                Log.error("Error: File didn't found", e);
                this.returnExit = false;

            } catch (final Exception e) {
                Log.error("Reading Error", e);
                this.returnExit = false;

            } finally {
                try {
                    if (inputFileStream != null) {
                        inputFileStream.close();
                    }
                } catch (final Exception e) {
                    Log.error("File Loader", e);
                }
            }
        }

        return lines;
    }


    private Map<String, MTMData> parseLines(List<String> lines) {
        Map<String, MTMData> mtmAllDataMap = new HashMap<String, MTMData>();

        if(!Util.isEmpty(lines)) {
            for (final String line : lines) {
                // All lines less header (first one)
                if (!line.isEmpty()) {
                    try {
                        final String[] fields = line.split(";");
                        String mxID = fields[4];
                        MTMData mtmData = mtmAllDataMap.get(mxID);
                        if (mtmData == null) {
                            mtmData = new MTMData();
                            mtmData.setDate(stringToDate(fields[0]));
                            mtmData.setMtmCcy(fields[8]);
                            mtmData.setMtmValue(Double.valueOf(fields[14]));
                            mtmData.setMtmMarketValue(Double.valueOf(fields[17]));
                            mtmData.setMtmFutureCash(Double.valueOf(fields[16]));
                            mtmData.setMxID(mxID);
                        }
                        mtmAllDataMap.put(mtmData.getMxID(), mtmData);
                    } catch (Exception e) {
                        Log.error(this, "Cannot set line: " + line + "Error: " + e);
                    }
                }
            }
        }
        return mtmAllDataMap;
    }


    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute(FILE_NAME));
        attributeList.add(attribute(FILE_PATH));
        attributeList.add(attribute(MX_TRADE_KEYWORD));
        return attributeList;
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Vector<String> getAttributeDomain(final String attr, final Hashtable currentAttr) {
        return super.getAttributeDomain(attr, currentAttr);
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean isValidInput(final Vector messages) {
        super.isValidInput(messages);
        return messages.isEmpty();
    }


    public static JDate stringToDate(String datetime) {
        String dFormat ="yyyyMMdd";
        SimpleDateFormat format = new SimpleDateFormat(dFormat);
        try {
            return JDate.valueOf(format.parse(datetime));
        } catch (ParseException e) {
            Log.warn(Log.LOG, "Error parsing string to JDatetime ("+dFormat+")" + e.toString());
            return null;
        }
    }


    private class MTMData{

        JDate date;
        String mxID;
        Trade trade = null;
        double mtmValue;
        double mtmMarketValue;
        double mtmFutureCash;
        String mtmCcy;

        public JDate getDate() {
            return date;
        }

        public void setDate(JDate date) {
            this.date = date;
        }

        public String getMxID() {
            return mxID;
        }

        public void setMxID(String mxID) {
            this.mxID = mxID;
        }

        public Trade getTrade() {
            return trade;
        }

        public void setTrade(Trade trade) {
            this.trade = trade;
        }

        public double getMtmValue() {
            return mtmValue;
        }

        public void setMtmValue(double mtmValue) {
            this.mtmValue = mtmValue;
        }

        public double getMtmMarketValue() {
            return mtmMarketValue;
        }

        public void setMtmMarketValue(double mtmMarketValue) { this.mtmMarketValue = mtmMarketValue; }

        public double getMtmFutureCash() {
            return mtmFutureCash;
        }

        public void setMtmFutureCash(double mtmFutureCash) { this.mtmFutureCash = mtmFutureCash; }

        public String getMtmCcy() {	return mtmCcy; }

        public void setMtmCcy(String mtmCcy) {
            this.mtmCcy = mtmCcy;
        }

    }


}
