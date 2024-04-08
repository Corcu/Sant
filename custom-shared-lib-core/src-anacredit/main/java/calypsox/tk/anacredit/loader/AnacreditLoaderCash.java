package calypsox.tk.anacredit.loader;

import calypsox.tk.anacredit.formatter.AnacreditFormatter;
import calypsox.tk.anacredit.formatter.AnacreditFormatterCash;
import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import calypsox.tk.anacredit.processor.AnacreditProcessor;
import calypsox.tk.report.AnacreditAbstractReport;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallPositionDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnacreditLoaderCash extends AnacreditLoader {

    public static final String MC_CONTRACT_NUMBER = "MC_CONTRACT_NUMBER";
    public static final String INTEREST_BEARING = "INTEREST_BEARING";
    private static final String ATTR_CONTRACT_TYPE = "ANACREDIT.Cash.ContractTypes";
    private static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";
    private static final String NEW_CALL_ACCOUNT_CIRCUIT = "NEW_CALL_ACCOUNT_CIRCUIT";
    private static final String ACCOUNTING = "ACCOUNTING";

    private static String _extractionType = "";
    private AnacreditFormatterCash _formatter;


    /**
     * List contracts with migration indicator
     * @param contracts
     * @param valDate
     * @return
     */
    @Override
    public List<CollateralConfig> selectContractsToReport(Map<Integer, CollateralConfig> contracts, JDate valDate) {
        List<CollateralConfig> result = new ArrayList<>();
        contracts.forEach((key, value) -> {
            if (!Util.isEmpty(value.getAdditionalField(ACCOUNTING)))  {
                result.add(value);
            }
        });
        return result;
    }

    /**
     * Load and handle all Data for this extraction
     * @param extractionType
     * @param configs
     * @param rows
     * @param valDate
     * @param pEnv
     * @param errors
     * @return
     */
    @Override
    public List<ReportRow> loadData(String extractionType, List<CollateralConfig> configs, ReportRow[] rows, JDate valDate, PricingEnv pEnv, Vector<String> errors) {
        _extractionType = extractionType;
        ArrayList<ReportRow> result = new ArrayList<>();
        TradeArray tradeArray = AnacreditLoaderUtil.getInterestBearings(configs, valDate);
        //--> no need to check InventoryPosition ???
        AnacreditLoaderUtil.getInvLastCashPosition(configs, rows, valDate, errors);
        //
        if (rows != null) {
            if (!tradeArray.isEmpty()) {
                List<Trade> unmatchedTrades = matchInterestBearingWithPositions(tradeArray, rows, valDate);
                if (unmatchedTrades != null) {
                    unmatchedTrades.stream()
                            .forEach(
                                    trade -> generateOnlyInterestPaymentItems(result, trade, valDate, pEnv, errors));
                }
            }

            // crea las lineas de los contractos que tienen posiciones y interes en el mes
            Arrays.asList(rows).stream()
                    .forEach(
                            reportRow -> createContractItem(result, reportRow, valDate, pEnv, errors));
        }
        ArrayList<ReportRow> interestRows = createOperacionesItem(configs, valDate, pEnv, errors);
        result.addAll(interestRows);
        return result;
    }

    /**
     * Items with only interest payments and NO position at moment
     * @param result
     * @param trade
     * @param valDate
     * @param pEnv
     * @param errors
     */
    private void generateOnlyInterestPaymentItems(ArrayList<ReportRow> result, Trade trade, JDate valDate, PricingEnv pEnv, Vector<String> errors) {
        CollateralConfig config = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), trade.getKeywordAsInt(MC_CONTRACT_NUMBER));
        if (null == config)   {
            return;
        }
        if (isValidContract(config, null)) {
            AnacreditOperacionesItem item = getFormatter()
                    .formatInterestPaymentItem(config, trade , valDate, pEnv, errors);
            if (item != null) {
                ReportRow reportRow = new ReportRow(trade, INTEREST_BEARING);
                reportRow.setProperty(INTEREST_BEARING, trade);
                reportRow.setProperty(MARGIN_CALL_CONTRACT, config);
                addRowData(result, reportRow, item);
            }
        }
    }

    /**
     * MC contracts listed in MC Position report
     * @param result
     * @param reportRow
     * @param valDate
     * @param pEnv
     * @param errors
     */
    private void createContractItem(ArrayList<ReportRow> result, ReportRow reportRow, JDate valDate, PricingEnv pEnv, Vector<String> errors) {
        MarginCallPositionDTO marginCallPosition  =  reportRow.getProperty("Default");
        if (null!=marginCallPosition){
            CollateralConfig config = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), marginCallPosition.getMarginCallConfigId());
            if (null == config)   {
                return;
            }
            if (marginCallPosition instanceof CashPositionDTO) {
                // for some selected contracts there is one entry as Contract
                if (isValidContract(config, marginCallPosition)) {
                    AnacreditOperacionesItem item = getFormatter()
                            .formatContractPositionItem(config, (CashPositionDTO) marginCallPosition ,reportRow, valDate, pEnv, errors);
                    if(validateSaldosOfRow(item,config,errors)){
                        addRowData(result, reportRow, item);
                    }
                }
            }
        }
    }


    private AnacreditFormatterCash getFormatter() {
        if (_formatter==null){
            _formatter = new AnacreditFormatterCash();
        }
        return _formatter;
    }

    /**
     * Operaciones (Trades) for MC
     * @param configs
     * @param valDate
     * @param pEnv
     * @param errors
     * @return
     */
    private  ArrayList<ReportRow> createOperacionesItem(List<CollateralConfig> configs, JDate valDate, PricingEnv pEnv, Vector<String> errors) {
        ArrayList<ReportRow> result = new ArrayList<>();
        Map<Integer, CollateralConfig> contractsMap = AnacreditLoaderUtil.buildContractsMap(configs);
        TradeArray interestTrades = AnacreditLoaderUtil.loadMarginCallCashByMC(configs, valDate);
        for(Trade trade : interestTrades.getTrades()){
            CollateralConfig config = null;
            if(null!=trade.getProduct()
                    && trade.getProduct() instanceof MarginCall){
                Integer id = Math.toIntExact(((MarginCall) trade.getProduct()).getLinkedLongId());
                config = contractsMap.get(id);
            }
            ReportRow reportRow = new ReportRow(trade, INTEREST_BEARING);
            reportRow.setProperty(INTEREST_BEARING, trade);
            reportRow.setProperty(MARGIN_CALL_CONTRACT, config);
            AnacreditOperacionesItem item = getFormatter().formatTradeItem(config, trade, valDate, pEnv, errors);
            addRowData(result, reportRow, item);
        }
        return result;
    }

    //Validate Saldos
    private boolean validateSaldosOfRow(AnacreditOperacionesItem item, CollateralConfig config, Vector<String> errors){
        if(null!=item){
            if (item.getSaldo_deudor_no_ven()!=0.0 ||
                    item.getValor_nominal_d()!=0.0 ||
                    item.getIntereses_devengos()!=0.0 ||
                    item.getSaldo_contingente_d() !=0.0){
                return true;
            }else{
                AnacreditProcessor.createMessage(AnacreditFormatter.LogLevel.WARN, String.valueOf(config.getId()), "Zero balance.", errors);
            }
        }
        return false;
    }


    protected  boolean isValidContract(CollateralConfig config, MarginCallPositionDTO position) {
        return getContractTypes().contains(config.getContractType());
    }

    private static List<String> getContractTypes(){
        List<String> result = LocalCache.getDomainValues(DSConnection.getDefault(), ATTR_CONTRACT_TYPE);
        return !Util.isEmpty(result) ? result : Util.stringToList("OSLA");
    }


    /**
     * @param interestBearing
     * @param valDate
     * @return
     */
    public static List<Trade>  matchInterestBearingWithPositions(TradeArray interestBearing, ReportRow[] rows, JDate valDate) {
        ArrayList<Trade> unmatched = new ArrayList<>();
        Map<String, Trade> trades = new HashMap<>();
        if(!Util.isEmpty(interestBearing)) {
            for(Trade trade : interestBearing.getTrades()){
                if (trade.getTradeDate().after(valDate.getDate())) {
                    continue;
                }
                String key = AnacreditLoaderUtil.generateTradeKey(trade);
                if(!Util.isEmpty(key)){
                    if(trades.containsKey(key)){
                        Trade oldTrade = trades.get(key);
                        if(trade.getLongId()>oldTrade.getLongId()){
                            trades.put(key,trade);
                        }
                    } else {
                        trades.put(key,trade);
                    }
                }
            }
        }

        ConcurrentHashMap<String, ReportRow > rowsMap = AnacreditLoaderUtil.reportRowsToMap(rows);

        // Interest paid over contracts without positions
        trades.forEach((key, interestBearingTrade) -> {
            if (rowsMap.containsKey(key))  {
                ReportRow row = rowsMap.get(key);
                row.setProperty(AnacreditAbstractReport.INTEREST_BEARING, interestBearingTrade);
            } else {
                unmatched.add(interestBearingTrade);
            }
        });
        return unmatched;
    }


}
