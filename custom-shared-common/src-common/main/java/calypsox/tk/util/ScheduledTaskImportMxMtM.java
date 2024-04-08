package calypsox.tk.util;

import calypsox.tk.util.mxmtm.MxMTMData;
import calypsox.tk.util.mxmtm.MxMtmTradeHandler;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author aalonsop
 */
public class ScheduledTaskImportMxMtM extends ScheduledTask {

    public static final String FILE_NAME_PREFIX = "Files Name Prefix";
    public static final String FILE_PATH = "Files Path";
    public static final String FILE_DATE_FORMAT = "File Date Format";
    public static final String MX_TRADE_KEYWORD = "Murex Trade Keyword";
    public static final String PRODUCT_TYPE = "Product Type";

    private boolean returnExit = true;

    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static final int SQL_GET_SIZE = 999;

    @Override
    public String getTaskInformation() {
        return "Import Murex MtM";
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        String path = getAttribute(FILE_PATH);
        String fileName = getAttribute(FILE_NAME_PREFIX);
        String fileDateFormat = getAttribute(FILE_DATE_FORMAT);
        if (Util.isEmpty(fileDateFormat)) {
            fileDateFormat = "yyyyMMdd";
        }
        String mxTradeKw = getAttribute(MX_TRADE_KEYWORD);

        JDate fileDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        SimpleDateFormat df = new SimpleDateFormat(fileDateFormat);
        String fileDateS = df.format(fileDate.getDate());
        List<File> filesToProcess = getFilesToProcess(path, fileName, fileDateS);

        List<String> lines = new ArrayList<>();
        if (filesToProcess.size() > 0) {
            for (File fileToProcess : filesToProcess) {
                lines.addAll(readFile(fileToProcess));
            }
        }

        MxMtmTradeHandler mtmHandler=MxMtmTradeHandler.getInstance(getAttribute(PRODUCT_TYPE));
        Map<String, MxMTMData> mtmAllDataMap = mtmHandler.parseLines(lines);

        TradeArray tradeArray = loadTradesByK(this.getDSConnection(), mtmAllDataMap, mxTradeKw);

        if (!Util.isEmpty(tradeArray)) {
            mtmHandler.matchTades(mtmAllDataMap, tradeArray, mxTradeKw);
            final List<MxMTMData> finalList = mtmAllDataMap.values().stream()
                    .filter(mxMTMData -> mxMTMData.getTrade() != null)
                    .collect(Collectors.toList());

            if (!Util.isEmpty(finalList)) {
                createAndSavePlMarks(finalList, tradeArray,mtmHandler);
            }
            else {
                Log.error(this, "No PLMark to be created : empty list after filtering.");
            }


            Log.info(this, "All trades saved.");
        }

        return this.returnExit;
    }

    /**
     * @param ds
     * @param
     * @return
     */
    private TradeArray loadTradesByK(DSConnection ds, Map<String, MxMTMData> mtmAllDataMap, String mxTradeKw) {
        TradeArray trades = new TradeArray();

        if (!Util.isEmpty(mtmAllDataMap)) {
            List<MxMTMData> mtmDataList = new ArrayList<>(mtmAllDataMap.values());
            int size = SQL_GET_SIZE;
            Log.info(this, "Loading trades by Trade Keyword");
            for (int start = 0; start < mtmDataList.size(); start += size) {
                int end = Math.min(start + size, mtmDataList.size());

                final List<String> listReferences = mtmDataList.subList(start, end).stream().map(MxMTMData::getDealID).collect(Collectors.toList());
                String references = String.join("','", listReferences);
                StringBuilder where = new StringBuilder();
                try {
                    where.append(" TRADE_KEYWORD.KEYWORD_NAME = ");
                    where.append("'" + mxTradeKw + "'");
                    where.append(" AND TRADE_KEYWORD.KEYWORD_VALUE IN (");
                    where.append("'" + references + "'");
                    where.append(")");
                    where.append(" AND TRADE_KEYWORD.TRADE_ID = TRADE.TRADE_ID");

                    trades.addAll(ds.getRemoteTrade().getTrades("trade_keyword", where.toString(), "", null));
                } catch (CalypsoServiceException e) {
                    Log.info("Cannot get trades for ", e);
                }
            }
        }
        Log.info(this, trades.size() + " Trades loaded before filtering.");

        if (!Util.isEmpty(getTradeFilter())) {
            Log.info(this, "Filtering based on Trade Filter " + getTradeFilter());
            TradeFilter tradeFilter = BOCache.getTradeFilter(DSConnection.getDefault(), getTradeFilter());
            if (tradeFilter != null) {
                for (int i = trades.size() - 1; i >= 0; i--) {
                    Trade currentTrade = trades.get(i);
                    if (!tradeFilter.accept(currentTrade)) {
                        Log.info(this, "Trade is not accepted by Trade Filter : " + currentTrade.getLongId());
                        trades.remove(i);
                    }
                }
            }
        }
        else {
            Log.info(this, "No Trade Filter configured, no post-filtering.");
        }

        Log.info(this, trades.size() + " Trades loaded after filtering.");

        return trades;
    }

    /**
     * @param
     * @return
     */
    private void createAndSavePlMarks(List<MxMTMData> mtmAllData, TradeArray tradeArray, MxMtmTradeHandler mtmTradeHandler) {
        List<PLMark> plMarks = new ArrayList<>();

        Vector<String> holidays = getHolidays();
        if (Util.isEmpty(holidays)) {
            holidays = new Vector<>();
            holidays.add("SYSTEM");
        }

        TradeArray tradesToSave = new TradeArray();
        if (!Util.isEmpty(mtmAllData)) {
            Log.info(this, mtmAllData.size() + " Matched trades.");

            for (MxMTMData mtmData : mtmAllData) {
                Trade trade = mtmData.getTrade();
                if (trade == null) {
                    continue;
                }

                long mirrorTradeId = trade.getMirrorTradeLongId();
                if (mirrorTradeId > 0L) {
                    Trade mirrorTrade = getTradeWithId(tradeArray, mirrorTradeId);
                    if (trade.getLongId() < mirrorTradeId) {
                        mtmTradeHandler.addPLMarkForTrade(plMarks, trade, mtmData, this._pricingEnv, 1);
                        if (mirrorTrade != null) {
                            mtmTradeHandler.addPLMarkForTrade(plMarks, mirrorTrade, mtmData, this._pricingEnv, -1);
                        }
                    } else {
                        mtmTradeHandler.addPLMarkForTrade(plMarks, trade, mtmData, this._pricingEnv, -1);
                        if (mirrorTrade != null) {
                            mtmTradeHandler.addPLMarkForTrade(plMarks, mirrorTrade, mtmData, this._pricingEnv, 1);
                        }
                    }
                } else {
                    mtmTradeHandler.addPLMarkForTrade(plMarks, trade, mtmData, this._pricingEnv, 1);
                }
            }
        }

        try {
            savePLMarks(plMarks);
            if (!Util.isEmpty(tradesToSave)) {
                DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(tradesToSave.toVector()));
            }
        } catch (InterruptedException e) {
            Log.error(this, "Error saving PLMarks. " + e);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error saving Trades: " + e);
        } catch (InvalidClassException e) {
            Log.error(this, "Error : " + e);
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

    /**
     * @return Lines of the file
     */
    private List<String> readFile(File file) {
        final List<String> lines = new ArrayList<>();

        BufferedReader inputFileStream = null;

        if (file != null) {
            try {
                // We read the file.
                inputFileStream = new BufferedReader(new FileReader(file));
                String line;
                while (inputFileStream.ready()) {
                    line = inputFileStream.readLine();
                    lines.add(line);
                }

                Log.info(Log.CALYPSOX, "Finished reading file" + file.getName());

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


    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute(FILE_NAME_PREFIX));
        attributeList.add(attribute(FILE_PATH));
        attributeList.add(attribute(FILE_DATE_FORMAT));
        attributeList.add(attribute(MX_TRADE_KEYWORD));
        attributeList.add(attribute(PRODUCT_TYPE));

        return attributeList;
    }

    @Override
    public Vector<String> getAttributeDomain(final String attr, final Hashtable currentAttr) {
        return super.getAttributeDomain(attr, currentAttr);
    }

    @Override
    public boolean isValidInput(final Vector messages) {
        super.isValidInput(messages);

        return messages.isEmpty();
    }

    public static JDate stringToDate(String datetime) {
        JDate date=null;
        String dFormat = "dd/MM/yyyy";
        SimpleDateFormat format = new SimpleDateFormat(dFormat);
        try {
            date=JDate.valueOf(format.parse(datetime));
        } catch (ParseException e) {
            Log.warn(Log.LOG, "Error parsing string to JDatetime (" + dFormat + ")" + e);
        }
        return date;
    }

    private static List<File> getFilesToProcess(final String rootPath, final String filePrefix, final String fileDate) {
        File folder = new File(rootPath);
        File[] listOfFiles = folder.listFiles();

        List<File> foundFiles = new ArrayList<File>();
        if(listOfFiles!=null) {
            for (File currentFile : listOfFiles) {
                if (currentFile.isFile()) {
                    if (currentFile.getName().startsWith(filePrefix) && currentFile.getName().contains(fileDate)) {
                        foundFiles.add(currentFile);
                    }
                }
            }
        }
        return foundFiles;
    }
}
