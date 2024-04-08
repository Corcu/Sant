package calypsox.tk.util;


import calypsox.util.TradeInterfaceUtils;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 1. Load
 */
public class ScheduledTaskSANT_FIX_POSITION_FB extends ScheduledTask {

    public static final String ATT_FILE_PATH = "File Path";
    public static final String ATT_FILE_NAME = "File Name";
    public static final String ATT_DEFAULT_SEPARATOR = ";";
    public static final String ATT_FILE_SEPARATOR = "File Separator";

    String fileSeparator = ATT_DEFAULT_SEPARATOR;

    @Override
    public String getTaskInformation() {
        return "Fix Position, Header: ContractName,ContractId,Currency,Account,Amount,ValDate";
    }


    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute(ATT_FILE_PATH).description("File Path."));
        attributeList.add(attribute(ATT_FILE_NAME).description("File Name."));
        attributeList.add(attribute(ATT_FILE_SEPARATOR).description("File Separator."));

        return attributeList;
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {

        List<Bean> beansToProcess = generateBeans(readFile());

        if(!Util.isEmpty(beansToProcess)){
            loadTradesForBean(beansToProcess);
            Log.system(this.getClass().getName(),"Processing Beans..");
            beansToProcess.forEach(this::processBean);
            Log.system(this.getClass().getName(),"End Process.");
        }

        return super.process(ds, ps);
    }


    /**
     * @param beansToProcess
     */
    private void loadTradesForBean(List<Bean> beansToProcess){
        Log.system(this.getClass().getName(),"Loading MarginCall Trades.");
        beansToProcess.forEach(bean -> {
            //Load last MarginCall from Contract ids
            Trade trade = getMarginCallTrade(bean.getContractId(),bean.getCurrency(),getValuationDatetime().getJDate(TimeZone.getDefault()));
            if(null!=trade){
                bean.setTradeToClone(trade);
            }else {
                Log.system(this.getClass().getName(),"No trade found for contract + "+ bean.getContractId() +" Currency: "+ bean.getCurrency() + " on date: " + getValuationDatetime().getJDate(TimeZone.getDefault()));
            }
        });
    }


    /**
     * @param fileLines
     * @return
     */
    private List<Bean> generateBeans(List<String[]> fileLines){
        Log.system(this.getClass().getName(),"Generating Beans.");
        List<Bean> beans = new ArrayList<>();
        fileLines.forEach(line -> {
            if(line.length>=5){
                Bean bean = new Bean();
                bean.setContractName(line[0]);
                bean.setContractId(line[1]);
                bean.setCurrency(line[2]);
                bean.setAccount(line[3]);
                bean.setAmount(Double.valueOf(line[4]));
                bean.setValueDate(JDate.valueOf(line[5]));
                //TODO set line
                bean.setLine(0);
                beans.add(bean);
            }
        });
        return beans;
    }

    /**
     *  Process beans
     * @param bean
     */
    public void processBean(Bean bean){
        try {
            Trade newTradeToSave = createNewTrade(bean);
            if(Optional.ofNullable(newTradeToSave).isPresent()){
                Long newTradeId = saveTrade(newTradeToSave);
                Trade tradeSaved = DSConnection.getDefault().getRemoteTrade().getTrade(newTradeId);
                if (CollateralUtilities.isTradeActionApplicable(tradeSaved,Action.valueOf("APPROVE"))){
                    tradeSaved.setAction(Action.valueOf("APPROVE"));
                    newTradeId = saveTrade(tradeSaved);
                    Log.system(this.getClass().getName(), "Action APPROVE is applied to the new trade " + newTradeId);
                }
                //TODO Need wait for trade save and create new Xfers (probarlo quizas es demasiado)
                Thread.sleep(10000);
                processXfers(bean,newTradeId);

            }
        } catch (Exception e) {
           Log.error(this,"Error creating new Trade: " + e);
        }

    }

    /**
     * Create and set values on new MarginCall Trade
     * @param bean
     * @return
     */
    private Trade createNewTrade(Bean bean){
        Trade newTradeToSave = null;
        try {
            if(Optional.ofNullable(bean.getTradeToClone()).isPresent()){
                newTradeToSave = (Trade) bean.getTradeToClone().clone();
                newTradeToSave.setLongId(0);
                newTradeToSave.setAction(Action.NEW);
                newTradeToSave.setStatus(Status.S_NONE);
                newTradeToSave.addKeyword("CouponType","Branch");
                newTradeToSave.addKeyword("ClientPositionAdjustment", "Y");

                MarginCall marginCall = null;

                    marginCall = (MarginCall) newTradeToSave.getProduct().clone();

                marginCall.setId(0);
                //Set new creation and value date
                newTradeToSave.setSettleDate(bean.getValueDate());
                newTradeToSave.setTradeDate(bean.getValueDate().getJDatetime());
                //set new amount
                marginCall.setPrincipal(bean.getAmount());
                newTradeToSave.setProduct(marginCall);
            }
        } catch (Exception e) {
            Log.error(this,"Error creating new Trade: " + e);
        }
        return newTradeToSave;
    }

    /**
     * Apply actions on Xfers
     * @param bean
     * @param newTradeId
     */
    private void processXfers(Bean bean, Long newTradeId){
        try {
            final TransferArray transfers = DSConnection.getDefault().getRemoteBO()
                    .getTransfers(null, "trade_id = " + newTradeId, null);
            BOTransfer[] boTransfers = transfers.getTransfers();
            boolean clientTransfer = false;
            for (BOTransfer xfer: boTransfers){
                if(xfer.isClientTransfer()
                        && xfer.getGLAccountNumber()==Integer.parseInt(bean.getAccount())){
                    xfer.setAction(Action.SETTLE);
                    saveXfer(xfer);
                    clientTransfer = true;
                    Log.system(this.getClass().getName(),"Trade id "+ newTradeId + " from contract " + bean.getContractId()
                            + ". Action SETTLED has applied to the client transfer: " + xfer.getLongId());
                }else {
                    if (xfer.getNettedTransferLongId()>0){
                        try {
                            BOTransfer xferNetted = DSConnection.getDefault().getRemoteBO().getBOTransfer(xfer.getNettedTransferLongId());
                            xferNetted.setAction(Action.CANCEL);
                            saveXfer(xferNetted);
                            Log.system(this.getClass().getName(), "Trade id " + newTradeId + " from contract " + bean.getContractId()
                                    + ". Action CANCELED has applied to the MarginCall transfer: " + xferNetted.getLongId());
                        } catch (CalypsoServiceException e) {
                            Log.error(this.getClass().getName(),"No netted transfer for" + xfer.getLongId() + "-- Trade id "+ newTradeId + " from contract " + bean.getContractId()
                                    + "for transfer: " + xfer.getLongId());
                        }
                    } else {
                        xfer.setAction(Action.CANCEL);
                        saveXfer(xfer);
                        Log.system(this.getClass().getName(), "Trade id " + newTradeId + " from contract " + bean.getContractId()
                                + ". Action CANCELED has applied to the MarginCall transfer: " + xfer.getLongId());

                    }
                }

                }
            if (clientTransfer==false){
                try {
                    Trade tradeToCancel = DSConnection.getDefault().getRemoteTrade().getTrade(newTradeId);
                    tradeToCancel.setAction(Action.CANCEL);
                    saveTrade(tradeToCancel);
                    Log.system(this.getClass().getName(), "The account " + bean.getAccount()
                            + " for contract id" + bean.getContractId() + " is not OK. Trade " +tradeToCancel.getLongId() + " has been canceled");
                } catch (CalypsoServiceException e) {
                    e.printStackTrace();
                }
            }

        } catch (CalypsoServiceException e) {
            Log.error(this,"Something went wrong while applying actions to the transfers of Trade id "+ newTradeId + " from contract " + bean.getContractId() +":"  + e);
        }
    }

    /**
     * Load MarginCall Trade for Contract/Currency
     * @param contractIds
     * @param currency
     * @param valDate
     * @return
     */
    private Trade getMarginCallTrade(String contractIds,String currency,JDate valDate) {
        TradeArray existingTrades = null;
        if(!Util.isEmpty(currency) && !Util.isEmpty(contractIds)){
            try {
                existingTrades = DSConnection
                        .getDefault()
                        .getRemoteTrade()
                        .getTrades(
                                "trade, product_desc, product_simplexfer ",
                                "trade.product_id=product_desc.product_id "
                                        + " AND product_simplexfer.product_id = product_desc.product_id"
                                        + " AND product_desc.product_type = 'MarginCall'"
                                        + " AND trade.TRADE_STATUS IN ('EXPORTED')"
                                        + " AND product_desc.PRODUCT_SUB_TYPE = 'COLLATERAL'"
                                        + " AND product_desc.CURRENCY = '"+currency+"'"
                                        + " AND product_simplexfer.LINKED_ID IN ("+contractIds+")"
                                        + " AND trunc(trade.entered_date) >= "+Util.date2SQLString(valDate.addDays(-10))
                                , "trade.trade_id DESC", null);
            } catch (RemoteException e) {
                Log.error(TradeInterfaceUtils.class, "Error loading trades for contract: " + contractIds +" ." + e);
            }
        }

        if(existingTrades!=null && !Util.isEmpty(existingTrades.getTrades())){
            return existingTrades.get(0);
        }

        return null;
    }


    /**
     *
     * Read fix position File: Header: ContractName,ContractId,Currency,Account,Amount,ValDate
     * @return
     */
    private List<String[]> readFile(){
        List<String> allLines = new ArrayList<>();
        String filePath = getAttribute(ATT_FILE_PATH);
        String fileName = getAttribute(ATT_FILE_NAME);
        fileSeparator = getAttribute(ATT_FILE_SEPARATOR)!=null ? getAttribute(ATT_FILE_SEPARATOR) : this.fileSeparator;

        String file = getFile(filePath, fileName);
        List<String[]> lines = new ArrayList<>();

        Log.system(this.getClass().getName(),"Reading File: " + file);

        try {
            allLines = Files.readAllLines(Paths.get(filePath+file));
        } catch (IOException e) {
            Log.error(this,"Error reading file: " + filePath+file + " : " + e);
        }
        if(!Util.isEmpty(allLines)){
            Supplier<Stream<String>> supplier = allLines::stream;
            try {
                final List<String> headers = Arrays.stream(supplier.get().findFirst()
                        .filter(l -> l.contains(fileSeparator))
                        .map(line -> line.split(fileSeparator))
                        .orElseThrow(Exception::new))
                        .collect(Collectors.toList());
                //Read rest of lines from file
                lines = supplier
                        .get().skip(1) //Skip the headers line
                        .filter(l -> l.contains(fileSeparator))
                        .map(line -> line.split(fileSeparator))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                Log.error(this,"Error reading file. : " + e);
            }
        }

        return lines;
    }

    /**
     * Get list of files from directory and return the first file with contains te fileName
     * @param filePath
     * @param fileName
     * @return
     */
    private String getFile(String filePath,String fileName){
        String fileN = "";
        try {
            final Optional<Path> first = Files.list(Paths.get(filePath))
                    .filter(file -> file.getFileName().toString().contains(fileName))
                    .findFirst();
            if(first.isPresent()){
                fileN = first.get().getFileName().toString();
            }
        } catch (IOException e) {
            Log.error(this,"Cannot get list of files form route: " + filePath);
        }
        return fileN;
    }

    /**
     * Save xfers
     * @param xfer
     */
    private void saveXfer(BOTransfer xfer){
        try {
            long none = DSConnection.getDefault().getRemoteBO().save(xfer, 0, "None");
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error saving BoTransfer: " + e);
        }
    }

    /**
     * Save Trade
     * @param trade
     * @return
     */
    private Long saveTrade(Trade trade){
        long save = 0L;
        try {
            if(trade!=null){
                save = DSConnection.getDefault().getRemoteTrade().save(trade);

            }
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error saving new Trade: " + e);
        }
        return save;
    }


    private class Bean {
        String contractName;
        String contractId;
        String currency;
        String account;
        Double amount;
        Trade tradeToClone;
        JDate valueDate;
        int line;


        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public Trade getTradeToClone() {
            return tradeToClone;
        }

        public void setTradeToClone(Trade tradeToClone) {
            this.tradeToClone = tradeToClone;
        }

        public String getContractName() {
            return contractName;
        }

        public void setContractName(String contractName) {
            this.contractName = contractName;
        }

        public String getContractId() {
            return contractId;
        }

        public void setContractId(String contractId) {
            this.contractId = contractId;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public JDate getValueDate() {
            return valueDate;
        }

        public void setValueDate(JDate valueDate) {
            this.valueDate = valueDate;
        }

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }
    }
}
