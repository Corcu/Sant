package calypsox.tk.util;

import calypsox.tk.util.lakemtm.LakeMTMFileParser;
import calypsox.tk.util.lakemtm.LakeMTMPLMarkBuilder;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;
import com.calypso.ui.component.condition.ConditionTree;
import com.calypso.ui.component.condition.ConditionTreeNode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ScheduledTaskImportLakeMtM extends ScheduledTask {

    public static final String FILE_NAME = "File Name";
    public static final String FILE_PATH = "File Path";
    public static final String MX_TRADE_KEYWORD = "Murex Trade Keyword";
    public static final String PRODUCT_GRP_FILTERING = "Product Group Filtering";
    public static final String PRODUCT_TYP_FILTERING = "Product Type Filtering";
    public static final String PRODUCT_FAM_FILTERING = "Product Family Filtering";
    public static final String TRADE_ACTION = "Action to Apply";

    protected String fileName = "";
    protected String path = "";
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static final int SQL_GET_SIZE = 999;

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

        String productGroup = getAttribute(PRODUCT_GRP_FILTERING);
        String productType = getAttribute(PRODUCT_TYP_FILTERING);
        String productFam = getAttribute(PRODUCT_FAM_FILTERING);
        String idColumn=getAttribute("Field Lago for ID");

        LakeMTMFileParser fileParser=new LakeMTMFileParser(fileName,path,idColumn,productGroup,productType,productFam);
        Map<String, MTMData> mtmAllDataMap =fileParser.parse();
        if(fileParser.hasErrors()||mtmAllDataMap.isEmpty()){
            return false;
        }

        TradeFilter tradeFilter = Optional.ofNullable(getTradeFilter())
                .map(filter -> BOCache.getTradeFilter(DSConnection.getDefault(), filter)).orElse(null);

        processTrades(tradeFilter, mtmAllDataMap, mxTradeKw);


        return true;
    }

    private void processTrades(TradeFilter tradeFilter, Map<String, MTMData> mtmAllDataMap, String mxTradeKw) {
        if (tradeFilter != null) {
            try {
                TradeFilter clonedFilter = (TradeFilter) tradeFilter.clone();
                TradeArray tradeArray = loadTradesByK(this.getDSConnection(), mtmAllDataMap, mxTradeKw, clonedFilter);

                if (!Util.isEmpty(tradeArray)) {
                    matchTades(mtmAllDataMap, tradeArray, mxTradeKw);
                    final List<MTMData> finalList = mtmAllDataMap.entrySet().stream()
                            .filter(ent -> ent.getValue().getTrade() != null)
                            .map(Map.Entry::getValue)
                            .collect(Collectors.toList());

                    createAndSavePlMarks(finalList, tradeArray);


                    Log.info(this.getClass().getSimpleName(), "All trades saved.");
                }
            } catch (CloneNotSupportedException exc) {
                Log.error(this.getClass().getSimpleName(), exc.getMessage(), exc.getCause());
            }
        } else {
            Log.error(this.getClass().getSimpleName(), "A TradeFilter must be set to execute the ScheduledTask");
        }
    }

    /**
     * @param ds
     * @param
     * @return
     */
    private TradeArray loadTradesByK(DSConnection ds, Map<String, MTMData> mtmAllDataMap, String mxTradeKw, TradeFilter tradeFilter) {
        TradeArray trades = new TradeArray();
        if (!Util.isEmpty(mtmAllDataMap) || tradeFilter != null) {
            List<MTMData> mtmDataList = new ArrayList<>(mtmAllDataMap.values());
            int size = SQL_GET_SIZE;
            Log.info(this.getClass().getSimpleName(), "Loading trades by Trade Keyword");
            for (int start = 0; start < mtmDataList.size(); start += size) {
                int end = Math.min(start + size, mtmDataList.size());
                final List<String> listReferences = mtmDataList.subList(start, end).stream().map(MTMData::getMxID).collect(Collectors.toList());
                try {
                    setTFKeywordCriteria(tradeFilter, mxTradeKw, listReferences);
                    trades.addAll(ds.getRemoteTrade().getTrades(tradeFilter, getValuationDatetime()));
                } catch (CalypsoServiceException e) {
                    Log.info("Cannot get trades for ", e);
                }
            }
        }
        Log.info(this.getClass().getSimpleName(), trades.size() + " Trades loaded.");

        return trades;
    }

    private void setTFKeywordCriteria(TradeFilter tradeFilter, String mxTradeKw, List<String> listReferences) {
        ConditionTree tree = new ConditionTree();
        ConditionTreeNode node = tree.getRoot();
        node.setOperatorId("ConditionRoot");
        ConditionTreeNode childNode = new ConditionTreeNode();
        node.setOperands(new Object[]{childNode});
        childNode.setOperatorId("&&");
        ConditionTreeNode comparatorNode = new ConditionTreeNode();
        childNode.setOperands(new Object[]{comparatorNode});
        comparatorNode.setOperatorId("{}");
        comparatorNode.setPropertyId(mxTradeKw);
        comparatorNode.setOperands(listReferences.toArray());
        tradeFilter.setConditionTree(tree);
    }

    /**
     * @param mtmAllDataMap
     * @param tradearray
     * @param keyword
     */
    private void matchTades(Map<String, MTMData> mtmAllDataMap, TradeArray tradearray, String keyword) {
        Log.info(this.getClass().getSimpleName(), "Matching trades.");

        List<String> references = new ArrayList<>();
        for (Trade trade : tradearray.getTrades()) {
            if (!references.contains(trade.getKeywordValue(keyword))) {
                MTMData mtmData = mtmAllDataMap.get(trade.getKeywordValue(keyword));
                if (mtmData != null) {
                    mtmData.setTrade(trade);
                } else {
                    Log.info(this.getClass().getSimpleName(), "Trade " + trade.getLongId() + " not found on file. ");
                }
                references.add(trade.getKeywordValue(keyword));
            } else {
                Log.info(this.getClass().getSimpleName(), "Duplicate trade with same STM_REFERENCE: " + trade.getKeywordValue(keyword) + " tradeId: " + trade.getLongId());
            }

        }
    }


    /**
     * @param
     * @return
     */
    private void createAndSavePlMarks(List<MTMData> mtmAllData, TradeArray tradeArray) {
        List<PLMark> plMarks = new ArrayList<>();

        HashMap<String, Double> fxRates = new HashMap<>();

        TradeFilter tradeFilter = BOCache.getTradeFilter(DSConnection.getDefault(), getTradeFilter());
        if (tradeFilter == null) {
            Log.error(this.getClass().getSimpleName(), "Error retrieving Trade Filter " + getTradeFilter());
            return;
        }

        String pricingEnv = getPricingEnv();
        TradeArray tradesToSave = new TradeArray();
        if (!Util.isEmpty(mtmAllData)) {
            Log.info(this.getClass().getSimpleName(), mtmAllData.size() + " Matched trades.");

            for (MTMData mtmData : mtmAllData) {
                Trade trade = mtmData.getTrade();
                if (trade == null) {
                    continue;
                }

                long mirrorTradeId = trade.getMirrorTradeLongId();
                LakeMTMPLMarkBuilder plMarkBuilder = LakeMTMPLMarkBuilder.getInstance(trade);
                String tradeAction = getAttribute(TRADE_ACTION);
                if (mirrorTradeId > 0L) {
                    Trade mirrorTrade = getTradeWithId(tradeArray, mirrorTradeId);
                    if (trade.getLongId() < mirrorTradeId) {
                        plMarkBuilder.addPLMarkForTrade(plMarks, trade, mtmData, tradesToSave, tradeAction, pricingEnv, tradeFilter, fxRates, 1);
                        if (mirrorTrade != null) {
                            plMarkBuilder.addPLMarkForTrade(plMarks, mirrorTrade, mtmData, tradesToSave, tradeAction, pricingEnv, tradeFilter, fxRates, -1);
                        }
                    } else {
                        plMarkBuilder.addPLMarkForTrade(plMarks, trade, mtmData, tradesToSave, tradeAction, pricingEnv, tradeFilter, fxRates, -1);
                        if (mirrorTrade != null) {
                            plMarkBuilder.addPLMarkForTrade(plMarks, mirrorTrade, mtmData, tradesToSave, tradeAction, pricingEnv, tradeFilter, fxRates, 1);
                        }
                    }
                } else {
                    plMarkBuilder.addPLMarkForTrade(plMarks, trade, mtmData, tradesToSave, tradeAction, pricingEnv, tradeFilter, fxRates, 1);
                }
            }
        }

        try {
            savePLMarks(plMarks);
            DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(tradesToSave.toVector()));
        } catch (InterruptedException e) {
            Log.error(this.getClass().getSimpleName(), "Error saving PLMarks. " + e);
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(), "Error saving Trades: " + e);
        } catch (InvalidClassException e) {
            Log.error(this.getClass().getSimpleName(), "Error : " + e);
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
            Log.info(this.getClass().getSimpleName(), "Saving " + plMarks.size() + " PLMarks.");
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
                                        Log.error(this.getClass().getSimpleName(), "Cannot save PLMarks. " + e);
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
        return name + year + month + day + ".DAT";
    }


    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(FILE_NAME));
        attributeList.add(attribute(FILE_PATH));
        attributeList.add(attribute(MX_TRADE_KEYWORD));
        attributeList.add(attribute(PRODUCT_GRP_FILTERING));
        attributeList.add(attribute(PRODUCT_TYP_FILTERING));
        attributeList.add(attribute(PRODUCT_FAM_FILTERING));
        attributeList.add(attribute(TRADE_ACTION));
        attributeList.add(attribute("Field Lago for ID"));

        return attributeList;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Vector<String> getAttributeDomain(final String attr, final Hashtable currentAttr) {
        return super.getAttributeDomain(attr, currentAttr);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean isValidInput(final Vector messages) {
        super.isValidInput(messages);

        return messages.isEmpty();
    }

    public static JDate stringToDate(String datetime) {
        String dFormat = "yyyy-MM-dd";
        SimpleDateFormat format = new SimpleDateFormat(dFormat);
        try {
            return JDate.valueOf(format.parse(datetime));
        } catch (ParseException e) {
            Log.warn(Log.LOG, "Error parsing string to JDatetime (" + dFormat + ")" + e.toString());
            return null;
        }
    }

    public static class MTMData {
        JDate date;
        String book;
        String mxID;
        Trade trade = null;
        double baseMtM;
        String baseCCY;
        double currencyMtM;
        double line1MTM;
        String line1CCY;
        double line2MTM;
        String line2CCY;

        public String getBook() {
            return book;
        }

        public void setBook(String book) {
            this.book = book;
        }

        public Trade getTrade() {
            return trade;
        }

        public void setTrade(Trade trade) {
            this.trade = trade;
        }

        public double getCurrencyMtM() {
            return currencyMtM;
        }

        public void setCurrencyMtM(double currencyMtM) {
            this.currencyMtM = currencyMtM;
        }

        public String getBaseCCY() {
            return baseCCY;
        }

        public void setBaseCCY(String baseCCY) {
            this.baseCCY = baseCCY;
        }

        public double getBaseMtM() {
            return baseMtM;
        }

        public void setBaseMtM(double baseMtM) {
            this.baseMtM = baseMtM;
        }

        public double getLine1MTM() {
            return line1MTM;
        }

        public void setLine1MTM(double mtm) {
            this.line1MTM = mtm;
        }

        public String getLine1CCY() {
            return line1CCY;
        }

        public void setLine1CCY(String ccy) {
            this.line1CCY = ccy;
        }

        public double getLine2MTM() {
            return line2MTM;
        }

        public void setLine2MTM(double mtm) {
            this.line2MTM = mtm;
        }

        public String getLine2CCY() {
            return line2CCY;
        }

        public void setLine2CCY(String ccy) {
            this.line2CCY = ccy;
        }

        public String getMxID() {
            return mxID;
        }

        public void setMxID(String mxID) {
            this.mxID = mxID;
        }

        public JDate getDate() {
            return date;
        }

        public void setDate(JDate date) {
            this.date = date;
        }
    }

}
