package calypsox.tk.anacredit.loader;

import calypsox.tk.anacredit.util.AnacreditMapper;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.collateral.dto.MarginCallPositionDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.InterestBearingEntry;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AnacreditLoaderUtil {
    public static final String MC_CONTRACT_NUMBER = "MC_CONTRACT_NUMBER";
    public static final String INVENTORY_CASH_POS = "INVENTORY_CASH_POS";

    /**
     *  Load Inventory Cash Positions
     * @param
     * @param rows
     * @param valDate
     * @param errors
     *
     */
    public static void getInvLastCashPosition(List<CollateralConfig> contracts, ReportRow[] rows, JDate valDate, Vector<String> errors) {
        HashMap<String, InventoryCashPosition> invCashMap =  loadInventoryCashMapByConfig(contracts, valDate);
        // match map against rows
        Arrays.asList(rows).stream().forEach(reportRow -> {
            MarginCallPositionDTO marginCallPosition  =  reportRow.getProperty("Default");
            marginCallPosition.getAccountId();
            CollateralConfig config = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), marginCallPosition.getMarginCallConfigId());
            if (null != config)   {
                Account account = loadAccount(String.valueOf(config.getId()), marginCallPosition.getCurrency());
                Book book = getBook(marginCallPosition);
                String bookAccountingLink = getBookAccountingLink(book);
                if(account!=null && book!=null){
                    String positionKey = account.getLongId()+"_"+config.getId() +"_"+ marginCallPosition.getCurrency()+"_"+bookAccountingLink;// Contract * ccy;
                    if (!Util.isEmpty(positionKey)) {
                        InventoryCashPosition invCashPos = invCashMap.get(positionKey);
                        reportRow.setProperty(INVENTORY_CASH_POS, invCashPos);
                        reportRow.setProperty("F_ACCOUNT",account);
                        reportRow.setProperty("F_BOOK",book);
                    }
                }
            }
        });
    }

    private static HashMap<String, InventoryCashPosition> loadInventoryCashMapByConfig(List<CollateralConfig> contracts, JDate valDate) {
        Log.system("AnacreditLoaderUtil", "### Extracting Inventory Cash operations");
        Instant start = Instant.now();
        HashMap<String, InventoryCashPosition> invCashPositions = new HashMap<>();
        for (CollateralConfig config : contracts) {
            //CollateralConfig marginCall = row.getContract();
            //position_date D-1
            //date_type = 'TRADE' || SETTLE

            StringBuilder where = new StringBuilder("internal_external = 'MARGIN_CALL' ");
            where.append(" AND position_date <= " + Util.date2SQLString(valDate));
            where.append(" AND position_type =  'ACTUAL' AND date_type = 'SETTLE' AND config_id = 0");
            where.append(" AND mcc_id = " + config.getId());

            try {
                Vector<InventoryCashPosition> inventoryCashPos = DSConnection.getDefault().getRemoteInventory().getLastInventoryCashPositions(where.toString(),null);
                if (!Util.isEmpty(inventoryCashPos)) {
                    for (InventoryCashPosition newPos : inventoryCashPos) {
                        Account account = loadAccount(String.valueOf(config.getId()), newPos.getCurrency());
                        if(account!=null && newPos.getBook()!=null){
                            String accoutingLink = newPos.getBook().getAccountingBook().getName();
                            String key = account.getLongId()+"_"+config.getId() +"_"+newPos.getCurrency()+"_"+getAccLinkMap(accoutingLink);
                            if(newPos.getTotal()!=0.0){
                                if (!invCashPositions.containsKey(key)) {
                                    invCashPositions.put(key, newPos);
                                    continue;
                                }
                                // compute position
                                invCashPositions.get(key).addTotalPosition(newPos);
                            }
                        }
                    }
                }
            } catch (Exception var5) {
                Log.error(Log.CALYPSOX, var5);
            }
        }
        Log.system("AnacreditLoaderUtil", "### Extracting Inventory Cash operations finished in " + (Duration.between(start, Instant.now())) + " sec.");
        return invCashPositions;
    }

    /**
     * @param marginCalls
     * @param valDate
     * @throws CalypsoServiceException
     */
    public static TradeArray getInterestBearings(List<CollateralConfig> marginCalls, JDate valDate){
        long time = System.currentTimeMillis();
        TradeArray trades = new TradeArray();
        String from = "product_desc, product_int_bearing, trade_keyword";
        StringBuilder where = new StringBuilder();
        where.append("trade.trade_id = trade_keyword.trade_id");
        where.append(" AND trade.product_id = product_desc.product_id");
        where.append(" AND trade_status != 'CANCELED' " +
                " AND product_desc.product_type = 'InterestBearing' " +
                " AND product_desc.product_id = product_int_bearing.product_id " +
                " AND product_int_bearing.end_date >= ");
        where.append(Util.date2SQLString(valDate));
        where.append(" AND product_int_bearing.start_date <= ");
        where.append(Util.date2SQLString(valDate));
        where.append(" AND trade_keyword.keyword_name LIKE 'MC_CONTRACT_NUMBER'");
        List<String> strings = contractListToIdString(marginCalls);
        String orderBy = " trade.trade_id DESC";

        for (String inClause : strings) {
            StringBuilder resultWhere = new StringBuilder().append(where).append(" AND trade_keyword.keyword_value IN ( ")
                    .append(inClause).append(" )");

            try {
                trades.addAll(DSConnection.getDefault().getRemoteTrade().getTrades(from, resultWhere.toString(), orderBy, null));
                Log.system("AnacreditLoaderUtil", "Load Interest Bearing. Query executed in:" + String.valueOf((System.currentTimeMillis() - time)/100) + " s.");
            } catch (CalypsoServiceException e) {
                Log.error(Log.CALYPSOX,"Error loading logs: " + e);
            }
        }

        return trades;
    }

    /**
     * @param contracts
     * @return List of contracts ids separate by 999
     */
    public static List<String> contractListToIdString(List<CollateralConfig> contracts) {
        StringBuilder builder = new StringBuilder();
        List<String> inClauses = new ArrayList<String>();
        int i = 0;
        for (CollateralConfig contract : contracts) {
            builder.append(contract.getId() + ",");
            if (i++ >= 998) {
                builder.deleteCharAt(builder.lastIndexOf(","));
                inClauses.add(builder.toString());
                builder.setLength(0);
                i = 0;
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.lastIndexOf(","));
            inClauses.add(builder.toString());
        }
        return inClauses;
    }

     /**
     * @param mcContracts
     * @return Load MarginCall trades for contracts
     */
    public static TradeArray loadMarginCallCashByMC(List<CollateralConfig> mcContracts, JDate valDate){
        TradeArray trades = new TradeArray();
        String from  = "product_desc, product_simplexfer";
        int size = 999;
        for (int start = 0; start < mcContracts.size(); start += size) {
            int end = Math.min(start + size, mcContracts.size());

            StringBuilder where = new StringBuilder();

            where.append(" trade.product_id = product_desc.product_id ");
            where.append(" AND product_desc.product_type = 'MarginCall' ");
            where.append(" AND product_desc.product_sub_type = 'COLLATERAL' ");
            where.append(" AND trade.quantity = -1 "); //SOLO PAY
            where.append(" AND trunc(trade.settlement_date) > " + Util.date2SQLString(valDate));
            where.append(" AND trunc(trade.trade_date_time) <= " + Util.date2SQLString(valDate));
            where.append(" AND trade.trade_status <> 'CANCELED'");
            where.append(" AND product_simplexfer.product_id = product_desc.product_id");
            where.append(" AND product_simplexfer.linked_id IN (");

            final List<String> listReferences = mcContracts.subList(start, end).stream()
                    .map(b -> String.valueOf(b.getId())).collect(Collectors.toList());

            String ids = "";
            if (!Util.isEmpty(listReferences)) {
                ids = String.join("','", listReferences);
            }
            where.append("'" + ids + "'");
            where.append(")");

            try {
                trades.addAll(DSConnection.getDefault().getRemoteTrade().getTrades(from, where.toString(), null, null));
            } catch (CalypsoServiceException e) {
                Log.error(Log.CALYPSOX, "Cannot load references: " + ids + " " + e);
            }
        }

        return trades;
    }

    public static Map<Integer, CollateralConfig> buildContractsMap(List<CollateralConfig> marginCallConfigByAdditionalField) {
        return marginCallConfigByAdditionalField.stream()
                .collect(Collectors.toMap(CollateralConfig::getId, bean -> bean, (address1, address2) -> {
                    Log.system(AnacreditMapper.class.getName(), "Duplicate contract found! for: 1: "+address1.getId() +" 2:" + address2.getId());
                    return address1;
                }));
    }


    /**
     * @param interestBearing
     * @param valDate
     * @return
     */
    public static Double  calculateInterestDaily(InterestBearing interestBearing, JDate valDate) {

        Double sumInterst = 0.0;
        if(null!=interestBearing && null!=valDate){
            JDate system = valDate.addBusinessDays(1, Util.string2Vector("SYSTEM"));
            if(valDate.getMonth() != system.getMonth()){
                int monthLength = valDate.getMonthLength();
                valDate = valDate.addDays(monthLength-valDate.getDayOfMonth());
            }

            Vector<InterestBearingEntry> entries = (Vector<InterestBearingEntry>) interestBearing.getEntries();
            List<InterestBearingEntry> collect = entries.stream().filter(interest -> InterestBearingEntry.POSITION.equalsIgnoreCase(interest.getEntryType())).collect(Collectors.toList());

            if(!Util.isEmpty(entries)){
                for(InterestBearingEntry entry : collect){
                    JDate entryDate = entry.getEntryDate();
                    if(entryDate.lte(valDate) && InterestBearingEntry.POSITION.equalsIgnoreCase(entry.getEntryType()) &&
                            entry.getAmount()<0){
                        InterestBearingEntry interes=interestBearing.getEntry("INTEREST",entryDate );
                        if (interes!=null)
                            sumInterst = sumInterst + interes.getAmount();

                        InterestBearingEntry ajuste=interestBearing.getEntry("ADJUSTMENT",entryDate );
                        if (ajuste!=null)
                            sumInterst += ajuste.getAmount();
                    }
                }
            }
        }

        return sumInterst;
    }

    public  static ConcurrentHashMap<String, ReportRow> reportRowsToMap(ReportRow[] rows) {
        ConcurrentHashMap<String, ReportRow> map = new ConcurrentHashMap<>();
        Arrays.asList(rows).stream().forEach(reportRow -> {
            MarginCallPositionDTO pos  = (MarginCallPositionDTO) reportRow.getProperty("Default");
            Book f_book = getRowBook(reportRow);
            Account f_account = getRowAcc(reportRow);
            if (pos!=null && f_book!=null && f_account!=null) {
                map.putIfAbsent(f_account.getLongId()+"_"+pos.getMarginCallConfigId()+"_"+pos.getCurrency()+"_"+getAccLinkMap(f_book.getAccountingBook().getName()), reportRow);
            }
        });
        return map;
    }

    private static Account getRowAcc(ReportRow row){
        Object f_account = row.getProperty("F_ACCOUNT");
        if(f_account instanceof Account){
            return (Account) f_account;
        }
        return null;
    }

    private static Book getRowBook(ReportRow row){
        Object f_book = row.getProperty("F_BOOK");
        if(f_book instanceof Book){
            return (Book) f_book;
        }
        return null;
    }

    public static Book getBook( MarginCallPositionDTO mcef){
        if(mcef!=null){
            mcef.getBookId();
            Book book = BOCache.getBook(DSConnection.getDefault(), mcef.getBookId());
            return book;
        }
        return null;
    }

    public static String getBookAccountingLink( Book book){
        if(book!=null){
            String name = book.getAccountingBook().getName();
            return getAccLinkMap(name);
        }
        return "";
    }


    public static String getAccLinkMap(String accountingLink){
        if(accountingLink.equalsIgnoreCase("Disponible para la venta")){
            return "08_01";
        }else if(accountingLink.equalsIgnoreCase("Inventario Terceros")
                || accountingLink.equalsIgnoreCase("Inversion a vencimiento")
                || accountingLink.equalsIgnoreCase("Inversion crediticia")
                || accountingLink.equalsIgnoreCase("NONE")){
            return "06_01";
        }else if(accountingLink.equalsIgnoreCase("Negociacion")){
            return "02_01";
        }else if(accountingLink.equalsIgnoreCase("Otros a valor razonable")){
            return "41_01";
        }
        return "";
    }

    public static String generateTradeKey(Trade trade){
        if(null!=trade){
            String contractId = trade.getKeywordValue(MC_CONTRACT_NUMBER);
            return generateKey(contractId,trade.getTradeCurrency(),trade.getBook());
        }
        return "";
    }


    public static String generateKey(String contractId, String currency, Book book){
        Account account = loadAccount(String.valueOf(contractId), currency);
        if(account!=null && book!=null) {
            String accLinkMap = AnacreditMapper.getTipoCartera(book);
            return account.getLongId() + "_" + contractId + "_" + currency + "_" + accLinkMap;
        }
        return "";
    }


    public static Account loadAccount(String id, String currency){
        Account account = null;
        final List<Account> accounts = BOCache.getAccountByAttribute(DSConnection.getDefault(), "MARGIN_CALL_CONTRACT", id);
        account = accounts.stream().filter(acc -> acc.getCurrency().equalsIgnoreCase(currency)).findFirst().orElse(null);
        return account;
    }


}
