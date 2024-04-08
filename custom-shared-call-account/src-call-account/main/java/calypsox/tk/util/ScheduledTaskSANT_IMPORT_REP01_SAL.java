package calypsox.tk.util;

import calypsox.tk.lch.util.LCHFileReader;
import calypsox.tk.lch.util.LchBean;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ScheduledTaskSANT_IMPORT_REP01_SAL extends ScheduledTask {

    private static final String ATT_FILE_PATH = "File Path";
    private static final String ATT_FILE_NAME = "File Name";
    private static final String ATT_LOAD_TRADES = "Load Trades";
    private static final String ATT_SAVE_TRADES = "Save Trades";
    private static final String ATT_MOVE_TO_COPY = "Move to Copy";
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    //Att
    private String attFilePath;
    private String attFileName;
    private Integer loadTradesNum;
    private Integer saveTradesNum;
    private Boolean moveToCopy;

    private static final String LCH_REF = "LchMatchedTradeRef";
    private static final String USI_VALUE = "Cleared_USI_Value";
    private static final String UTI_VALUE = "Cleared_UTI_Value";
    private static final String USI_PREFIX = "Cleared_USI_Prefix";
    private static final String UTI_PREFIX = "Cleared_UTI_Prefix";
    private static final String CURRENCY = "Currency";

    private static final String USI_REFERENCE = "USI_REFERENCE";
    private static final String UTI_REFERENCE = "UTI_REFERENCE";

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(ATT_FILE_PATH).description("Lch File Path"));
        attributeList.add(attribute(ATT_FILE_NAME).description("Lch File Name"));

        attributeList.add(attribute(ATT_LOAD_TRADES).description("Set load trades by ").integer());
        attributeList.add(attribute(ATT_SAVE_TRADES).description("Set save trades by ").integer());
        attributeList.add(attribute(ATT_MOVE_TO_COPY).description("Move file to copy ").booleanType());

        return attributeList;
    }

    @Override
    public String getTaskInformation() {
        return null;
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        init();
        //Read lch file
        final List<HashMap<String, String>> lchFile = LCHFileReader.getInstance().readFile(attFilePath, attFileName, moveToCopy);

        if (Util.isEmpty(lchFile)) {
            Log.warn(this, "SAL REP01 file empty.");
            return false;
        }

        final Map<String, LchBean> lchBeans = lchFile.stream()
                .filter(line -> line.containsKey(LCH_REF))
                .filter(line -> line.containsKey(USI_VALUE) && !Util.isEmpty(line.get(USI_VALUE)))
                .filter(line -> line.containsKey(UTI_VALUE) && !Util.isEmpty(line.get(UTI_VALUE)))
                .filter(line -> line.containsKey(USI_PREFIX) && !Util.isEmpty(line.get(USI_PREFIX)))
                .filter(line -> line.containsKey(UTI_PREFIX) && !Util.isEmpty(line.get(UTI_PREFIX)))
                .filter(line -> line.containsKey(CURRENCY))
                .collect(Collectors.toMap(file -> file.get(USI_PREFIX).trim() + file.get(USI_VALUE).trim(),
                        file -> new LchBean(file.get(UTI_PREFIX).trim() + file.get(UTI_VALUE).trim(),
                                file.get(USI_PREFIX).trim() + file.get(USI_VALUE).trim(),
                                file.get(LCH_REF).trim(), file.get(CURRENCY).trim()), (address1, address2) -> {
                            Log.system(ScheduledTaskSANT_IMPORT_REP01_SAL.class.getName(), "Duplicate key found!");
                            return address1;
                        }
                ));

        /**
         * 1. Load trades by USI_REFERENCE
         */
        TradeArray tradeArrayByUsi = loadTrades(lchBeans, USI_REFERENCE, LchBean.USI);
        matchTades(lchBeans, tradeArrayByUsi, LchBean.USI, USI_REFERENCE);

        /**
         * 2. Load trades by UTI_REFERENCE
         */
        final Map<String, LchBean> lefts = lchBeans.entrySet().stream()
                .filter(lch -> lch.getValue().getTrade() == null)
                .filter(lch -> !Util.isEmpty(lch.getValue().getUti()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toMap(
                        LchBean::getUti, value -> value));


        TradeArray tradeArrayByUti = loadTrades(lefts, UTI_REFERENCE, LchBean.UTI);
        matchTades(lefts, tradeArrayByUti, LchBean.UTI, UTI_REFERENCE);

        /**
         * 3. Save trades (lchBeans mantiene la referncia a los objetos de lefts)
         */
        List<LchBean> tradesToSave = lchBeans.entrySet().stream()
                .filter(lch -> lch.getValue().getTrade() != null)
                .map(Map.Entry::getValue).collect(Collectors.toList());

        Log.system(ScheduledTaskSANT_IMPORT_REP01_SAL.class.getName(), ("Trades to save: " + tradesToSave.size()));

        try {
            saveTades(tradesToSave);
        } catch (InterruptedException e) {
            Log.error(this, "Cannot save trades: " + e);
        }

        return true;
    }

    private void init() {
        attFilePath = !Util.isEmpty(getAttribute(ATT_FILE_PATH)) ? getAttribute(ATT_FILE_PATH) : "";
        attFileName = !Util.isEmpty(getAttribute(ATT_FILE_NAME)) ? getAttribute(ATT_FILE_NAME) : "";
        loadTradesNum = getIntegerAttribute(ATT_LOAD_TRADES, 999);
        saveTradesNum = getIntegerAttribute(ATT_SAVE_TRADES, 999);
        moveToCopy = getBooleanAttribute(ATT_MOVE_TO_COPY, true);
    }

    /**
     * @param listLchBeans
     * @param tradeKeyword
     * @param type
     * @return
     */
    private TradeArray loadTrades(Map<String, LchBean> listLchBeans, String tradeKeyword, String type) {
        Log.system(ScheduledTaskSANT_IMPORT_REP01_SAL.class.getName(), "Loading trades by " + type + ". " + listLchBeans.size());

        TradeArray trades = new TradeArray();
        List<LchBean> lchBeans = new ArrayList<>(listLchBeans.values());

        if (!Util.isEmpty(lchBeans)) {
            int size = loadTradesNum;
            for (int start = 0; start < lchBeans.size(); start += size) {
                int end = Math.min(start + size, lchBeans.size());

                StringBuilder where = new StringBuilder();
                where.append(" TRADE.TRADE_STATUS NOT IN ('CANCELED') ");
                where.append(" AND TRADE_KEYWORD.KEYWORD_NAME LIKE ");
                where.append("'" + tradeKeyword + "'");
                where.append(" AND TRADE_KEYWORD.KEYWORD_VALUE IN (");

                final List<String> listReferences = lchBeans.subList(start, end).stream()
                        .map(b -> b.getType(type)).collect(Collectors.toList());

                String references = "";
                if (!Util.isEmpty(listReferences)) {
                    references = String.join("','", listReferences);
                }
                where.append("'" + references + "'");
                where.append(")");
                where.append(" AND TRADE_KEYWORD.TRADE_ID = TRADE.TRADE_ID");

                try {
                    trades.addAll(getTrades(where.toString()));
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Cannot load references: " + references + " " + e);
                }
            }
        }

        return trades;
    }

    /**
     * @param lchBeans
     * @param tradearray
     * @param type
     * @param keyword
     */
    private void matchTades(Map<String, LchBean> lchBeans, TradeArray tradearray, String type, String keyword) {
        Log.system(ScheduledTaskSANT_IMPORT_REP01_SAL.class.getName(), "Matching trades founds by " + type + ": " + tradearray.size());
        List<String> references = new ArrayList<>();

        for (Trade trade : tradearray.getTrades()) {
            if (lchBeans.containsKey(trade.getKeywordValue(keyword))) {
                LchBean lchBean = lchBeans.get(trade.getKeywordValue(keyword));
                if (!references.contains(trade.getKeywordValue(keyword))) {
                    if (lchBean != null) {
                        trade.addKeyword("SWAPAGENT_REFERENCE", lchBean.getLchRef());
                        lchBean.setTrade(trade);
                    } else {
                        Log.system(ScheduledTaskSANT_IMPORT_REP84_SAL.class.getName(), "Trade " + trade.getLongId() + " not found on file. ");
                    }
                    references.add(trade.getKeywordValue(keyword));
                } else {
                    Log.system(ScheduledTaskSANT_IMPORT_REP84_SAL.class.getName(), "Duplicate trade with same " + keyword + ": " + trade.getKeywordValue(keyword) + " tradeId: " + trade.getLongId());
                }
            }
        }
    }

    private TradeArray getTrades(String where) throws CalypsoServiceException {
        return DSConnection.getDefault().getRemoteTrade().getTrades("trade_keyword", where, "", null);
    }

    /**
     * @param tradesToSave
     * @throws InterruptedException
     */
    private void saveTades(List<LchBean> tradesToSave) throws InterruptedException {
        Log.system(ScheduledTaskSANT_IMPORT_REP01_SAL.class.getName(), "Saving " + tradesToSave.size() + " trades.");

        if (!Util.isEmpty(tradesToSave)) {
            ExecutorService exec = Executors.newFixedThreadPool(NUM_CORES);
            try {
                int size = saveTradesNum;
                for (int start = 0; start < tradesToSave.size(); start += size) {
                    int end = Math.min(start + size, tradesToSave.size());
                    final List<Trade> trades = tradesToSave.subList(start, end).stream().map(LchBean::getTrade).collect(Collectors.toList());
                    exec.execute(
                            new Runnable() {
                                public void run() {
                                    try {
                                        DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(trades));
                                    } catch (CalypsoServiceException e) {
                                        Log.error(this, "Cannot save trades: " + e);
                                    } catch (InvalidClassException e) {
                                        Log.error(this, "Cannot convert to ExternalArray: " + e);
                                    }
                                }
                            });
                }
            } finally {
                exec.shutdown();
                exec.awaitTermination(20, TimeUnit.MINUTES);
            }
            Log.system(ScheduledTaskSANT_IMPORT_REP01_SAL.class.getName(), "All trades updated.");
        }
    }

}
