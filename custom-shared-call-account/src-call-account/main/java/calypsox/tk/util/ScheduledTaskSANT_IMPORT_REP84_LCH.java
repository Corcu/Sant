package calypsox.tk.util;

import calypsox.tk.lch.util.LCHFileReader;
import calypsox.tk.lch.util.LchBean;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author acd
 *
 * 1. Load LCH Ref from file just for STM lines
 * 2. Load trades by in Calypso STM_REFERENCE (LCHRef)
 * 3. Create PLMark for loaded trades with TradeLevelNPV from LCH file
 * 4. Add keyword STM:true for loaded trades
 *
 */
public class ScheduledTaskSANT_IMPORT_REP84_LCH extends ScheduledTask {


    private static final String KEYWORD_STM_REFERENCE = "STM_REFERENCE";
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    //ST Attributes Names
    private static final String ATT_FILE_PATH = "File Path";
    private static final String ATT_FILE_NAME = "File Name";
    private static final String ATT_MOVE_TO_COPY = "Move to Copy";
    private static final String ATT_REF_COLUMN = "LCH Column Reference";
    private static final String ATT_TRADE_CLASS_COLUMN = "LCH Column Trade Type";
    private static final String ATT_TRADE_TYPE = "LCH Trade Type";
    private static final String ATT_TARDE_KEYWORD = "Trade Keyword";
    private static final String ATT_TARDE_KEYWORD_VALUE = "Trade Keyword Value";

    private static final String ATT_LOAD_TRADE = "Load Trade";
    private static final String ATT_SAVE_TRADE = "Save Trade";
    private static final String ATT_SAVE_PLMARK = "Save PLMark";


    private String attFilePath;
    private String attFileName;
    private String pricingEnv;
    private Vector holidays;
    private Boolean moveToCopy;

    private Integer loadTradeNum;
    private Integer saveTradeNum;
    private Integer savePlMarkNum;

    private String attRefColumn;
    private static final String LCH_TRADE_REF = "LchMatchedTradeRef";

    private String attTradeTypeColumn;
    private static final String TRADE_CLASSIFICATION = "VMTradeClassification";

    private String attTradeType;
    private static final String TRADE_TYPE = "STM";

    //Attributes
    private String attTradeSTMKeyword = "STM";
    private String attTradeKeywordValue;

    @Override
    public String getTaskInformation() {
        return "Import file REP84 from LCH";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(ATT_FILE_PATH).description("Lch File Path"));
        attributeList.add(attribute(ATT_FILE_NAME).description("Lch File Name"));
        attributeList.add(attribute(ATT_MOVE_TO_COPY).description("Move file to copy ").booleanType());

        attributeList.add(attribute(ATT_LOAD_TRADE).description("Set load trades by").integer());
        attributeList.add(attribute(ATT_SAVE_TRADE).description("Set save trades by").integer());
        attributeList.add(attribute(ATT_SAVE_PLMARK).description("Set save PLMark").integer());

        attributeList.add(attribute(ATT_REF_COLUMN).description("Select 'Ref Column' for find trade by keyword in Calypso"));
        attributeList.add(attribute(ATT_TRADE_CLASS_COLUMN).description("Select 'TradeClassification' in LCH file"));
        attributeList.add(attribute(ATT_TRADE_TYPE).description("Select trade type STM/CTM"));
        attributeList.add(attribute(ATT_TARDE_KEYWORD).description("Define Trade Keyword Name"));
        attributeList.add(attribute(ATT_TARDE_KEYWORD_VALUE).description("Set Trade Keyword Value"));

        return attributeList;
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        //Init attributes
        init();

        //Read lch file
        final List<HashMap<String, String>> lchFile = LCHFileReader.getInstance().readFile(attFilePath,attFileName,true);

        if (Util.isEmpty(lchFile)) {
            Log.warn(this, "LCH REP84 file empty.");
            return false;
        }

        final Map<String, LchBean> lchTradeRefs = lchFile.stream()
                .filter(filter -> filter.containsKey(attTradeTypeColumn))
                .filter(filter -> filter.get(attTradeTypeColumn).equalsIgnoreCase(attTradeType))
                .filter(filter -> filter.containsKey(attRefColumn))
                .filter(filter -> filter.containsKey("TradeLevelNPV"))
                .filter(filter -> filter.containsKey("Currency"))
                .map(ref -> new LchBean(ref.get(attRefColumn), ref.get("TradeLevelNPV"), ref.get("Currency")))
                .collect(Collectors.toMap(LchBean::getLchRef, bean -> bean, (address1, address2) -> {
                    Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "Duplicate key found!");
                    return address1;
                }));

        //Load trades
        final TradeArray tradeArray = loadTradesByK(ds, lchTradeRefs);
        //Match trades by ref
        if (!Util.isEmpty(tradeArray)) {
            matchTades(lchTradeRefs, tradeArray, KEYWORD_STM_REFERENCE);

            final List<LchBean> finalList = lchTradeRefs.entrySet().stream()
                    .filter(lch -> lch.getValue().getTrade() != null)
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            final List<PLMark> plMarks = createPlMarks(finalList);

            try {
                savePLMarks(plMarks);
            } catch (InterruptedException e) {
                Log.error(this, "Thread Error saving PLMarks. " + e);
            }
            final List<Trade> trades = finalList.stream().map(LchBean::getTrade).collect(Collectors.toList());
            addSTMTradeKeywords(trades);
            try {
                saveTrades(trades);
            } catch (InterruptedException e) {
                Log.error(this, "Thread error saving Trades. " + e);
            }
            Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "All trades saved.");
        }

        return true;
    }

    /**
     * @param ds
     * @param lchTradeRefs
     * @return
     */
    private TradeArray loadTradesByK(DSConnection ds, Map<String, LchBean> lchTradeRefs) {
        TradeArray trades = new TradeArray();

        if (!Util.isEmpty(lchTradeRefs)) {
            List<LchBean> beansList = new ArrayList<>(lchTradeRefs.values());
            int size = loadTradeNum;
            Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "Loading trades by " + KEYWORD_STM_REFERENCE);
            for (int start = 0; start < beansList.size(); start += size) {
                int end = Math.min(start + size, beansList.size());

                final List<String> listReferences = beansList.subList(start, end).stream().map(LchBean::getLchRef).collect(Collectors.toList());
                String references = String.join("','", listReferences);
                try {
                    StringBuilder where = new StringBuilder();
                    where.append(" TRADE.TRADE_STATUS NOT IN ('CANCELED') ");
                    where.append(" AND TRADE_KEYWORD.KEYWORD_NAME LIKE ");
                    where.append("'" + KEYWORD_STM_REFERENCE + "'");
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
        Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), trades.size() + " Trades loaded.");
        return trades;
    }

    /**
     * Add TradeKeyword STM : true to trades and Save
     * @param tradeArray
     */
    private void addSTMTradeKeywords(List<Trade> tradeArray) {
        Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "Adding keyword " + attTradeSTMKeyword + ":true to trades.");
        for (Trade trade : tradeArray) {
            trade.addKeyword(attTradeSTMKeyword, attTradeKeywordValue);
        }
    }

    /**
     * @param lchBeans
     * @param tradearray
     * @param keyword
     */
    private void matchTades(Map<String, LchBean> lchBeans, TradeArray tradearray, String keyword) {
        Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "Matching trades.");

        List<String> references = new ArrayList<>();
        for (Trade trade : tradearray.getTrades()) {
            if (!references.contains(trade.getKeywordValue(keyword))) {
                LchBean bean = lchBeans.get(trade.getKeywordValue(keyword));
                if (bean != null) {
                    bean.setTrade(trade);
                } else {
                    Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "Trade " + trade.getLongId() + " not found on file. ");
                }
                references.add(trade.getKeywordValue(keyword));
            } else {
                Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "Duplicate trade with same STM_REFERENCE: " + trade.getKeywordValue(keyword) + " tradeId: " + trade.getLongId());
            }

        }
    }

    /**
     * @param tradesToSave
     */
    private void saveTrades(List<Trade> tradesToSave) throws InterruptedException {
        Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "Saving " + tradesToSave.size() + " trades.");
        if (!Util.isEmpty(tradesToSave)) {
            int size = saveTradeNum;
            ExecutorService exec = Executors.newFixedThreadPool(NUM_CORES);
            try {
                for (int start = 0; start < tradesToSave.size(); start += size) {
                    int end = Math.min(start + size, tradesToSave.size());
                    List<Trade> toSave = new ArrayList<>(tradesToSave.subList(start, end));
                    exec.execute(
                            new Runnable() {
                                public void run() {
                                    try {
                                        DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(toSave));
                                    } catch (InvalidClassException e) {
                                        Log.error(this, "Cannot cast to ExternalArray: " + e);
                                    } catch (CalypsoServiceException e) {
                                        Log.error(this, "Cannot save trades: " + e);
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
     * @param lchBeans
     * @return
     */
    private List<PLMark> createPlMarks(List<LchBean> lchBeans) {
        List<PLMark> plMarks = new ArrayList<>();

        if (!Util.isEmpty(lchBeans)) {
            Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), lchBeans.size() + " Matched trades.");
            for (LchBean bean : lchBeans) {
                PLMark plMark = new PLMark();
                plMark.setTradeId(bean.getTrade().getLongId());
                plMark.setBookId(bean.getTrade().getBookId());
                JDate valDate = getValuationDatetime().getJDate(TimeZone.getDefault()).addBusinessDays(-1, holidays);
                plMark.setValDate(valDate);
                plMark.setPricingEnvName(pricingEnv);

                final PLMarkValue npvPriceMarkValue = new PLMarkValue();
                npvPriceMarkValue.setMarkName("NPV");
                npvPriceMarkValue.setCurrency(bean.getCurrency());
                npvPriceMarkValue.setOriginalCurrency(bean.getCurrency());
                npvPriceMarkValue.setMarkValue(bean.getNpv());
                npvPriceMarkValue.setAdjustmentType("NPV");
                plMark.addPLMarkValue(npvPriceMarkValue);
                plMarks.add(plMark);
            }
        }

        return plMarks;
    }

    /**
     * @param plMarks
     * @throws InterruptedException
     */
    private void savePLMarks(List<PLMark> plMarks) throws InterruptedException {
        if (!Util.isEmpty(plMarks)) {
            int size = savePlMarkNum;
            Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "Saving " + plMarks.size() + " PLMarks.");
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
     * Init attributes
     */
    private void init() {
        pricingEnv = getPricingEnv();
        if (Util.isEmpty(pricingEnv)) {
            Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "Using default PricingEnv: LCH-STM.");
            pricingEnv = "LCH-STM";
        }
        holidays = getHolidays();
        if (Util.isEmpty(holidays)) {
            Log.system(ScheduledTaskSANT_IMPORT_REP84_LCH.class.getName(), "Using default Holidays: SYSTEM.");
            holidays = Util.string2Vector("SYSTEM");
        }

        loadTradeNum = getIntegerAttribute(ATT_LOAD_TRADE, 999);
        saveTradeNum = getIntegerAttribute(ATT_SAVE_TRADE,999 );
        savePlMarkNum = getIntegerAttribute(ATT_SAVE_PLMARK,999 );
        moveToCopy = getBooleanAttribute(ATT_MOVE_TO_COPY, true);

        attFilePath = !Util.isEmpty(getAttribute(ATT_FILE_PATH)) ? getAttribute(ATT_FILE_PATH) : "";
        attFileName = !Util.isEmpty(getAttribute(ATT_FILE_NAME)) ? getAttribute(ATT_FILE_NAME) : "";

        attRefColumn = !Util.isEmpty(getAttribute(ATT_REF_COLUMN)) ? getAttribute(ATT_REF_COLUMN) : LCH_TRADE_REF;
        attTradeTypeColumn = !Util.isEmpty(getAttribute(ATT_TRADE_CLASS_COLUMN)) ? getAttribute(ATT_TRADE_CLASS_COLUMN) : TRADE_CLASSIFICATION;
        attTradeType = !Util.isEmpty(getAttribute(ATT_TRADE_TYPE)) ? getAttribute(ATT_TRADE_TYPE) : TRADE_TYPE;

        attTradeSTMKeyword = !Util.isEmpty(getAttribute(ATT_TARDE_KEYWORD)) ? getAttribute(ATT_TARDE_KEYWORD) : "STM";
        attTradeKeywordValue = !Util.isEmpty(getAttribute(ATT_TARDE_KEYWORD_VALUE)) ? getAttribute(ATT_TARDE_KEYWORD_VALUE) : "true";
    }


}
