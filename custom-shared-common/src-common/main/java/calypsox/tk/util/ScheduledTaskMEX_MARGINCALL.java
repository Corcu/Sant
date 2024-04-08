package calypsox.tk.util;

import calypsox.util.FileUtility;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteReferenceData;
import com.calypso.tk.service.RemoteTrade;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;
import org.jfree.util.Log;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduledTaskMEX_MARGINCALL extends ScheduledTask {

    private static final String DESCRIPTION = "ScheduledTask to load Margin Call linked to mexican sec lending trades";

    private static final String FILEPATH = "File path";
    private static final String LOGPATH = "Log path";
    private static final String STARTFILENAME = "Start file name";
    private static final String SEPARATOR = "Separator";
    private String file = "";
    private String separator = "";
    private StringBuilder log;

    private static SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM/yyyy");

    private static final int PROCESS_DATE = 0;
    private static final int PROCESSING_ORG = 1;
    private static final int EXTERNAL_REF_PDFV = 2;
    private static final int TYPE = 3;
    private static final int ISIN = 4;
    private static final int NOMINAL = 5;
    private static final int DIRTYPRICE = 6;
    private static final int CCY = 7;
    private static final int SETTLEMENT_DATE = 8;


    @Override
    public String getTaskInformation() {
        return DESCRIPTION;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.add(attribute(FILEPATH));
        attributeList.add(attribute(STARTFILENAME));
        attributeList.add(attribute(SEPARATOR));
        attributeList.add(attribute(LOGPATH));
        return attributeList;
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {

        HashMap<Long, Trade> marginCalls = getTradesMap(ds);

        final String path = getAttribute(FILEPATH);
        final String logPath = getAttribute(LOGPATH);
        final String startFileName = getAttribute(STARTFILENAME);
        final String date = CollateralUtilities.getValDateString(this.getValuationDatetime());
        final ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName + date);
        this.log = new StringBuilder();
        // We check if the number of matches is 1.
        if (files.size() != 1) {
            log.append("There are 0 or more than 1 file in the path");
            return false;
        }
        this.separator = getAttribute(SEPARATOR);
        this.file = files.get(0);
        final String filePath = path + this.file;
        FileUtility.copyFileToDirectory(filePath, path + "/copy/");

        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            boolean isHeader = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String processDate = line.split(this.separator)[PROCESS_DATE];
                String po = line.split(this.separator)[PROCESSING_ORG];
                String externalRef = line.split(this.separator)[EXTERNAL_REF_PDFV];
                String type = line.split(this.separator)[TYPE];
                String isin = line.split(this.separator)[ISIN];
                String nominal = line.split(this.separator)[NOMINAL];
                String dirtyPrice = line.split(this.separator)[DIRTYPRICE];
                String ccy = line.split(this.separator)[CCY];
                String settlementDate = line.split(this.separator)[SETTLEMENT_DATE];

                Trade childTrade = getTradeByTkwAndIsin(externalRef, isin, ds);
                if (childTrade == null) {
                    createMarginCall(externalRef, isin, processDate, settlementDate, po,
                            type, nominal, dirtyPrice, ccy, ds);
                } else {
                    executeActionForTrade(externalRef, isin, processDate, settlementDate,
                            po, type, nominal, dirtyPrice, ccy, childTrade, ds);
                    //delete from the map the childTrade
                    marginCalls.remove(childTrade.getLongId());
                }
            }
            bufferedReader.close();
            fileReader.close();
        } catch (Exception e) {
            log.append("Error found: " + e);
            Log.error(this, e);
        }
        File f = new File(filePath);
        f.delete();
        cancelMarginCalls(marginCalls, ds);
        generateLog(this.log.toString(), logPath);
        return true;
    }

    private void createMarginCall(String secLendingId, String isin, String processDate,
                                  String settlementDate, String poLine, String type,
                                  String nominal, String dirtyPrice,
                                  String ccy, DSConnection ds) {
        Trade t = new Trade();
        try {
            RemoteReferenceData rrd = ds.getRemoteReferenceData();
            //Add shortname in the le
            t.setAction(Action.NEW);
            t.setTradeDate(new JDatetime(getJDate(processDate)));
            t.setSettleDate(getJDate(settlementDate));
            t.addKeyword("secLendingId", secLendingId);
            MarginCall marginCall = new MarginCall();
            LegalEntity po = rrd.getLegalEntity(poLine);
            marginCall.setOrdererLeId(po.getId());
            marginCall.setSubType(type);
            t.setProduct(marginCall);
            copyDataFromSecLending(secLendingId, t, ds);
            marginCall.setOrdererRole("ProcessingOrg");
            if (type.equals("SECURITY")) {
                marginCall.setSubType(type);
                marginCall.setFlowType("SECURITY");
                Product p = ds.getRemoteProduct().getProductByCode("ISIN", isin);
                if (p == null) {
                    this.log.append("Product with this ISIN: " + isin + " does not exist in sistem");
                    return;
                }
                marginCall.setSecurity(p);
                //TODO Confirmar si debe setearse con la divisa del security
                //t.setSettleCurrency(p.getCurrency());
                //t.setTradeCurrency(p.getCurrency());
                if (p instanceof Bond) {
                    Bond b = (Bond) p;
                    double faceValue = b.getFaceValue();
                    marginCall.setPrincipal(faceValue);
                    t.setQuantity(Double.parseDouble(nominal) / b.getFaceValue());
                    t.setTradePrice(Double.parseDouble(dirtyPrice) / 100);
                } else if (p instanceof Equity)  {
                    Equity b = (Equity) p;
                    t.setQuantity(Double.parseDouble(nominal));
                    t.setTradePrice(Double.parseDouble(dirtyPrice) / 100);
                }     else {
                    t.setTradePrice(Double.parseDouble(dirtyPrice));
                }
            } else {
                marginCall.setFlowType("COLLATERAL");
                marginCall.setPrincipal(Double.parseDouble(nominal));
                t.setTradeCurrency(ccy);
                t.setSettleCurrency(ccy);
            }
            long trade_id = ds.getRemoteTrade().save(t);
            this.log.append("Trade id " + trade_id + " with SecLedingId " + secLendingId + " saved" + "\n");
        } catch (CalypsoServiceException e) {
            if (isin != null && !isin.isEmpty()) {
                this.log.append("Error when trying saved trade with ISIN " + isin + " and SecLedingId" + secLendingId + " error:" + e + "\n");
            } else {
                this.log.append("Error when trying saved trade with SecLedingId" + secLendingId + " error:" + e + "\n");
            }
        }
    }

    private void copyDataFromSecLending(String secLendingId, Trade childTrade, DSConnection ds) {
        try {
            TradeArray trades = ds.getRemoteTrade().getTradesByExternalRef(secLendingId);
            if (trades == null || trades.isEmpty() || !(trades.get(0).getProduct() instanceof SecLending)) {
                log.append("The sec lending trade "+ secLendingId + " does not exist in the environment");
                return;
            }
            SecLending sec = (SecLending) trades.get(0).getProduct();
            MarginCall marginCall = (MarginCall) childTrade.getProduct();
            if (sec.getCollaterals() == null || sec.getCollaterals().isEmpty()) {
                log.append("The sec lending trade "+ secLendingId + " has not collaterals");
                return;
            }
            MarginCallConfig config = getMarginCallContract(trades.get(0), ds);
            if (config == null) {
                log.append("The sec lending trade "+ secLendingId + " has not margin call contract defined");
                return;
            }
            marginCall.setLinkedLongId(config.getId());
            childTrade.setCounterParty(config.getLegalEntity());
            childTrade.setBook(config.getBook());
            //if (marginCall.getFlowType().equals("SECURITY")) {
            //    marginCall.setOrdererLeId(inventory.getBook().getLegalEntity().getId());
            //} else {

            //}
        } catch (CalypsoServiceException e) {
            this.log.append("Error when trying obtain trades with SecLendingId " + secLendingId + " error:" + e + "\n");
        }
    }

    private MarginCallConfig getMarginCallContract(Trade trade, DSConnection ds) {
        SecLending sec = (SecLending) trade.getProduct();
        int configId = sec.getMarginCallContractId(trade);
        try {
            return ds.getRemoteReferenceData().getMarginCallConfig(configId);
        } catch (CalypsoServiceException e) {
            this.log.append("Error when trying obtain MarginCallConfig with configId " + configId + " error:" + e + "\n");
        }
        return null;
    }

    //just 1 margin call per sec lending and isin.

    private Trade getTradeByTkwAndIsin(String externalRef, String isin, DSConnection ds) {
        TradeArray trades = null;
        try {
            trades = ds.getRemoteTrade().getTrades("", getQuery(externalRef, isin), null, null);
        } catch (CalypsoServiceException e) {
            this.log.append("Error when trying obtain Trades with SecLendingId " + externalRef + " error:" + e + "\n");
        }
        if(trades.isEmpty()){
            return null;
        }
        if (isin.isEmpty()) {
            return getCashTrade(trades);
        } else {
            return getSecurityTrade(trades, isin);
        }
    }

    private Trade getSecurityTrade(TradeArray trades, String isin) {
        for (int i = 0; i < trades.getTrades().length; i++) {
            MarginCall margin = (MarginCall) trades.get(i).getProduct();
            if (margin.getSecurity() != null &&
                    margin.getSecurity().getSecCode("ISIN").equals(isin)) {
                return trades.get(i);
            }
        }
        return null;
    }

    private Trade getCashTrade(TradeArray trades) {
        TradeArray finalTrade = new TradeArray();
        for (int i = 0; i < trades.getTrades().length; i++) {
            MarginCall margin = (MarginCall) trades.get(i).getProduct();
            if (margin.getFlowType().equals("COLLATERAL")) {
                return trades.get(i);
            }
        }
        return null;
    }

    private String getQuery(String externalRef, String isin) {
        StringBuilder query = new StringBuilder();
        query.append("TRADE_ID IN (SELECT TRADE_ID FROM TRADE_KEYWORD WHERE KEYWORD_NAME='secLendingId' AND KEYWORD_VALUE='");
        query.append(externalRef);
        query.append("')");
        query.append(" AND TRADE_STATUS <> 'CANCELED'");
        return query.toString();
    }

    private JDate getJDate(String dateLine) {
        try {
            Date date = timeFormat.parse(dateLine);
            return JDate.valueOf(date);
        } catch (ParseException e) {
            this.log.append("Error when trying obtain getJDate  error:" + e + "\n");
        }
        return null;
    }

    private void executeActionForTrade(String externalRef, String isin, String processDate,
                                       String settlementDate, String po, String type,
                                       String nominal, String dirtyPrice, String ccy,
                                       Trade childTrade, DSConnection ds) {
        if (checkFields(isin, nominal, childTrade)) { //Is an AMEND
            amendChildTrade(processDate, settlementDate, childTrade, ds);
        } else { //is CANCEL and Reissue
            cancelChildTrade(childTrade, ds);
            createMarginCall(externalRef, isin, processDate, settlementDate, po,
                    type, nominal, dirtyPrice, ccy, ds);
        }

    }

    // TODO Definir los campos que deben compararse. PTE respuesta Alex de momento a false para forzar la generacion del trade
    private boolean checkFields(String isin, String nominal, Trade trade) {
        MarginCall margin = (MarginCall) trade.getProduct();
        double tradeNominal = margin.getPrincipal();
        if(!isin.isEmpty()){
            tradeNominal = margin.getPrincipal() * trade.getQuantity();
        }
        return  tradeNominal == Double.parseDouble(nominal) &&
                (isin.isEmpty() || margin.getSecurity().getSecCode("ISIN").equals(isin));
    }

    private void amendChildTrade(String processDate, String settlementDate, Trade childTrade, DSConnection ds) {
        childTrade.setTradeDate(new JDatetime(getJDate(processDate)));
        childTrade.setSettleDate(getJDate(settlementDate));
        childTrade.setAction(Action.AMEND);
        try {
            ds.getRemoteTrade().save(childTrade);
            this.log.append("Trade " + childTrade.getLongId() + " amended");
        } catch (CalypsoServiceException e) {
            this.log.append("Error when trying execute amend action in trade with id " + childTrade.getLongId() + "Error: " + e + "\n");
        }
    }

    private void cancelChildTrade(Trade childTrade, DSConnection ds) {
        childTrade.setAction(Action.CANCEL);
        try {
            ds.getRemoteTrade().save(childTrade);
            this.log.append("Trade " + childTrade.getLongId() + " canceled" + "\n");
        } catch (CalypsoServiceException e) {
            this.log.append("Error when trying execute cancel action in trade with id " + childTrade.getLongId() + "\n");
        }
    }

    private HashMap<Long, Trade> getTradesMap(DSConnection ds) {
        HashMap<Long, Trade> map = new HashMap<>();
        TradeFilter tf = null;
        try {
            tf = ds.getRemoteReferenceData().getTradeFilter(this._tradeFilter);
            if (tf != null) {
                TradeArray trades = ds.getRemoteTrade().getTrades(tf, new JDatetime());
                ;
                for (Trade t : trades.getTrades()) {
                    map.put(t.getLongId(), t);
                }
            }
        } catch (CalypsoServiceException e) {
            this.log.append("Error when trying obtain trades by  tradefilter: " + this._tradeFilter + "error:" + e + "\n");
        }
        return map;
    }

    private void cancelMarginCalls(HashMap<Long, Trade> marginCalls, DSConnection ds) {
        RemoteTrade rt = ds.getRemoteTrade();
        for (Map.Entry<Long, Trade> entry : marginCalls.entrySet()) {
            entry.getValue().setAction(Action.CANCEL);
            try {
                rt.save(entry.getValue());
                this.log.append("Trade " + entry.getValue().getLongId() + " canceled" + "\n");
            } catch (CalypsoServiceException e) {
                this.log.append("Error when trying execute cancel action in trade with id " + entry.getValue().getLongId() + "Error: " + e + "\n");
            }
        }
    }

    private void generateLog(String message, String fileName) {
        ///calypso_interfaces/mic/?fileName=ccct_sent_messages_${date:now:yyyyMMdd}
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd");
        String date = dateFormater.format(new Date());
        String extension = ".log";
        String fullFileName = fileName + date + extension;
        try {
            File file = new File(fullFileName);
            FileWriter w = new FileWriter(fullFileName, true);
            w.write(message + "\n");
            w.close();
        } catch (IOException e) {
            Log.error(this, e);
        }
    }

}
