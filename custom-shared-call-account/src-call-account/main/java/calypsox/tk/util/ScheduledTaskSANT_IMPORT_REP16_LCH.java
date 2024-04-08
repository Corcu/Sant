package calypsox.tk.util;

import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.lch.util.LCHFileReader;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.ScheduledTask;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author acd
 *
 * This ST load import File REP0000016 from LCH
 * 1. SUM DailyChangeInNPV values by currency.
 * 2. Load STM Contracts.
 * 3. Load NettepPosition Cash + Security from STM contracts
 * 4. Create MarginCall DailyChangeInNPV - NettedPossition
 *
 */
public class ScheduledTaskSANT_IMPORT_REP16_LCH extends ScheduledTask {

    private RemoteSantCollateralService remoteSantColService = null;

    //ST Attributes Names
    private static final String ATT_FILE_PATH = "File Path";
    private static final String ATT_FILE_NAME = "File Name";
    private static final String ATT_VALUE_COLUMN = "Column NPV";
    private static final String ATT_CURRENCY_COLUMN = "Trade Keyword";
    private static final String ATT_MOVE_TO_COPY = "Move to Copy";
    private static final String DEFAULT_CASH_BOOK_OUT = "BOOK_CASH_OUT";
    private static final String AD_STM = "STM";
    private static final String STM_VALUE = "True";
    private static final String NONE = "NONE";

    //Default Values
    private static final String NPV = "DailyChangeInNPV";
    private static final String LCH_CURRENCY = "Currency";

    //Att
    private String attFilePath;
    private String attFileName;
    private Boolean moveToCopy;

    private String attNpvColumn;
    private String attCurrencyColumn;


    @Override
    public String getTaskInformation() {
        return "Import file REP16a from LCH";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(ATT_FILE_PATH).description("Lch File Path"));
        attributeList.add(attribute(ATT_FILE_NAME).description("Lch File Name"));
        attributeList.add(attribute(ATT_MOVE_TO_COPY).description("Move file to copy ").booleanType());
        attributeList.add(attribute(ATT_VALUE_COLUMN).description("Select 'Value Column' for generate MarginCall"));
        attributeList.add(attribute(ATT_CURRENCY_COLUMN).description("Select 'Currency Column' for generate MarginCall"));

        return attributeList;
    }


    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        //Init attributes
        init();

        //Read lch file
        final List<HashMap<String, String>> lchFile = LCHFileReader.getInstance().readFile(attFilePath,attFileName,moveToCopy);

        if(Util.isEmpty(lchFile)){
            Log.warn(this,"LCH REP16a file empty.");
            return false;
        }

        //Load Contracts
        HashMap<String,List<CollateralConfig>> contracts = getCollateralConfigsByAF();

        if(Util.isEmpty(contracts)){
            Log.warn(this,"No STM contracts found");
            return false;
        }

        List<CollateralConfig> configs = getContracts(contracts);

        //Load NettedPossition
        final HashMap<Integer, Double> nettedPossition = loadNettedPossition(configs);

        final HashMap<String, Double> stringDoubleHashMap = groupByCurrency(lchFile);

        createMarginCalls(stringDoubleHashMap,contracts,nettedPossition);

        return true;
    }



    /**
     * This method return list of STM contract group by Currency
     * Filter: AdditionalFiled STM : YES
     */
    private HashMap<String,List<CollateralConfig>> getCollateralConfigsByAF(){
        List<CollateralConfig> marginCallConfigByAF;
        HashMap<String,String> aditionalFields = new HashMap<>();
        aditionalFields.put(AD_STM,STM_VALUE);

        HashMap<String,List<CollateralConfig>> contracts = new HashMap<>();
        try {
            marginCallConfigByAF = remoteSantColService.getMarginCallConfigByAdditionalField(aditionalFields);
            //Group contracts by Contract currency
            if( !Util.isEmpty(marginCallConfigByAF)){
                for(CollateralConfig config : marginCallConfigByAF){
                    if(contracts.containsKey(config.getCurrency())){
                        Log.info(this,"More than one STM contract found for currency: " + config.getCurrency());
                        contracts.get(config.getCurrency()).add(config);
                    }else{
                        List<CollateralConfig> stmContracts = new ArrayList<>();
                        stmContracts.add(config);
                        contracts.put(config.getCurrency(),stmContracts);
                    }
                }
            }
        } catch (PersistenceException e) {
            Log.error(this,"Cannot load STM Contracts: " + e );
        }

        return contracts;
    }


    private List<CollateralConfig> getContracts(HashMap<String,List<CollateralConfig>> contracts){
        List<CollateralConfig> configs = new ArrayList<>();
        for(Map.Entry<String, List<CollateralConfig>> configs1 : contracts.entrySet()){
            configs.add(configs1.getValue().get(0));
        }
        return configs;
    }

    /**
     * Group lines of REP16 LCh file by currency.
     * @param lchFile
     * @return
     */
    private HashMap<String, Double> groupByCurrency(List<HashMap<String, String>> lchFile){
        HashMap<String, Double> group = new HashMap<>();
        HashMap<String, Double> result = new HashMap<>();

        for(HashMap<String, String> line : lchFile){
            if(line.containsKey(attNpvColumn) && line.containsKey(attCurrencyColumn)){
                String currency = line.get(attCurrencyColumn);
                Double value = convertToDouble(line.get(attNpvColumn));

                if(group.containsKey(currency)){
                    Double npv = group.get(currency);
                    npv += value;
                    group.put(currency,npv);
                }else {
                    group.put(currency, value);
                }

            }
        }

//        try {
//            if(!Util.isEmpty(group)){
//                //Remove 0.0
//                result = (HashMap<String, Double>) group.entrySet()
//                        .stream()
//                        .filter(e -> e.getValue() != 0)
//                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//            }
//        }catch (Exception e){
//            Log.error(this,"Cannot cast to HashMap. " + e);
//        }

        return group;
    }

    /**
     * Create one Margin Call per currency.
     * @param netos
     * @param contracts
     * @return
     */
    private boolean createMarginCalls(HashMap<String, Double> netos,
                                      HashMap<String,List<CollateralConfig>> contracts,
                                      HashMap<Integer, Double> nettedPossition){

        for (Map.Entry<String, List<CollateralConfig>> entry : contracts.entrySet()) {

            CollateralConfig contract = entry.getValue().get(0);
            if(netos.containsKey(contract.getCurrency()) && nettedPossition.containsKey(contract.getId())){

                //Principal = STMNPV - Netted Position.
                Double value = netos.get(contract.getCurrency()) - nettedPossition.get(contract.getId());

                final Double principal = roundPrincipal(contract.getCurrency(), value);
                if(principal!=0.0){
                    MarginCall mc = new MarginCall();
                    mc.setSecurity(null);
                    mc.setFlowType("COLLATERAL");
                    mc.setOrdererLeId(contract.getLegalEntityId());
                    mc.setPrincipal(principal);

                    mc.setCurrencyCash(contract.getCurrency());
                    mc.setLinkedLongId(contract.getId());
                    mc.setOrdererRole(contract.getOrdererRole());

                    Trade trade = new Trade();
                    trade.setTradeCurrency(contract.getCurrency());
                    trade.setSettleCurrency(contract.getCurrency());
                    trade.setCounterParty(contract.getLegalEntity());
                    trade.setTraderName(NONE);
                    trade.setSalesPerson(NONE);
                    trade.setAction(Action.NEW);
                    trade.setStatus(Status.S_NONE);
                    if (mc.getPrincipal() < 0) {
                        trade.setQuantity(-1);
                    } else {
                        trade.setQuantity(1);
                    }
                    //Add book to trade
                    if(null!=contract.getDefaultBook(DEFAULT_CASH_BOOK_OUT)){
                        trade.setBook(contract.getDefaultBook(DEFAULT_CASH_BOOK_OUT));
                    }else{
                        Log.error(this,"Cannot get 'Outgoing Cash Book' please update the contract: " + contract.getId() + " -Name: " +contract.getName() );
                    }
                    trade.setTradeDate(JDate.getNow().getJDatetime(TimeZone.getDefault()));
                    trade.setSettleDate(JDate.getNow());

                    //Add keywords:
                    trade.addKeyword("STM","true" );

                    //Add MarginCall to trade
                    trade.setProduct(mc);

                    //Save MarginCall

                    saveTrade(trade);
                }else{
                    Log.warn(this,"The MarginCall has not been created for currency: " + contract.getCurrency()
                            + " and Value: " + netos.get(contract.getCurrency())+
                            " check default currency config.");
                }
            }
        }

        return true;
    }

    private void saveTrade(Trade trade){
        if(trade!=null){
            try {
                long id = DSConnection.getDefault().getRemoteTrade().save(trade);
                if(id!=0){
                    Log.system(this.getClass().getName(),"MarginCall Created id: "+ id + " -Currency: " + trade.getTradeCurrency() + " -Principal: " + trade.getProduct().getPrincipal() + "\n" );
                }
            } catch (CalypsoServiceException e) {
                Log.error(this,"Cannot save trade: " + e );
            }
        }
    }

    private Double roundPrincipal(String curency, Double value){
        return CurrencyUtil.roundAmount(value, curency);
    }




    private Double convertToDouble(String value){
        if(!Util.isEmpty(value)){
            try {
                return Double.parseDouble(value);
            }catch (Exception e){
                Log.error(this,"Cannot convert: " + value + " to double." );
            }
        }

        return 0.0;
    }


    /**
     * Load Netted Possition for STM contracts
     * @param collect
     * @return
     */
    private HashMap<Integer,Double> loadNettedPossition(List<CollateralConfig> collect){
        JDate valDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        HashMap<Integer,Double> netted = new HashMap<>();
        final List<Integer> contractsIds = collect.stream().map(CollateralConfig::getId).collect(Collectors.toList());
        try {
            final List<MarginCallEntryDTO> marginCallEntryDTOS = ServiceRegistry.getDefault().getCollateralServer().loadEntries(contractsIds, valDate, ServiceRegistry.getDefaultContext().getId());
            if(!Util.isEmpty(marginCallEntryDTOS)){
                for (MarginCallEntryDTO entryDTO : marginCallEntryDTOS){
                    double value = (entryDTO.getPreviousCashPosition() != null) ? entryDTO.getPreviousCashPosition().getValue() : 0.0;
                    value += (entryDTO.getPreviousSecurityPosition()!= null) ? entryDTO.getPreviousSecurityPosition().getValue() : 0.0;
                    netted.put(entryDTO.getCollateralConfigId(),value);
                }
            }
        } catch (CollateralServiceException e) {
            Log.error(this,"Cannot load Entries for contracts. " + contractsIds);
        }

        return netted;
    }

    /**
     * Init attributes
     */
    private void init(){

        //Init remote services
        this.remoteSantColService = DSConnection.getDefault()
                .getRMIService("baseSantCollateralService", RemoteSantCollateralService.class);

        attFilePath = !Util.isEmpty(getAttribute(ATT_FILE_PATH)) ? getAttribute(ATT_FILE_PATH) : "";
        attFileName = !Util.isEmpty(getAttribute(ATT_FILE_NAME)) ? getAttribute(ATT_FILE_NAME) : "";

        attNpvColumn = !Util.isEmpty(getAttribute(ATT_VALUE_COLUMN)) ? getAttribute(ATT_VALUE_COLUMN) : NPV;
        attCurrencyColumn = !Util.isEmpty(getAttribute(ATT_CURRENCY_COLUMN)) ? getAttribute(ATT_CURRENCY_COLUMN) : LCH_CURRENCY;
        moveToCopy = getBooleanAttribute(ATT_MOVE_TO_COPY, true);

    }


}
