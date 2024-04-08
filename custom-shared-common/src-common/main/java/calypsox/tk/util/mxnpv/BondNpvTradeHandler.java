package calypsox.tk.util.mxnpv;

import calypsox.tk.util.ScheduledTaskImportMxNpv;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.FX;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dmenendd
 */
public class BondNpvTradeHandler extends MxNpvTradeHandler{

    @Override
    public void addPLMarkValues(PLMark plMark, MxNPVData npvData, int sign) {
        if (npvData.getTrade().getTradeCurrency().equalsIgnoreCase(npvData.getCurrency())){
            plMark.addPLMarkValue(createPLMarkValue("NPV_LEG1", npvData.getCurrency(), npvData.getCurrency(), npvData.getNpv()));
        } else if (npvData.getTrade().getSettleCurrency().equalsIgnoreCase(npvData.getCurrency())){
            plMark.addPLMarkValue(createPLMarkValue("NPV_LEG2", npvData.getCurrency(), npvData.getCurrency(), npvData.getNpv()));
        }
    }

    @Override
    public void matchTrades(Map<String, MxNPVData> npvAllDataMap, TradeArray tradearray, String keyword) {
        Log.info(this, "Matching trades.");

        List<String> references = new ArrayList<>();
        for (Trade trade : tradearray.getTrades()) {
            String contractIDLEG1 = trade.getKeywordValue(keyword) + trade.getTradeCurrency();
            String contractIDLEG2 = trade.getKeywordValue(keyword) + trade.getSettleCurrency();
            if (!references.contains(contractIDLEG1)) {
                MxNPVData npvData = npvAllDataMap.get(contractIDLEG1);
                if (npvData != null) {
                    npvData.setTrade(trade);
                    references.add(contractIDLEG1);
                }
                MxNPVData npvData2 = npvAllDataMap.get(contractIDLEG2);
                if (npvData2 != null) {
                    npvData2.setTrade(trade);
                    references.add(contractIDLEG2);
                } else {
                    Log.info(this, "Trade " + trade.getLongId() + " not found on file. ");
                }
            } else {
                Log.info(this, "Duplicate trade with same REFERENCE: " + contractIDLEG1 + " tradeId: " + trade.getLongId());
            }
        }
    }


    @Override
    public Map<String, MxNPVData> parseLines(List<String> lines) {
        Map<String, MxNPVData> npvAllDataMap = new HashMap<>();
        TradeArray trades =  new TradeArray();

        if (!Util.isEmpty(lines)) {
            for (final String line : lines) {
                if (!line.isEmpty()) {
                    try {
                        final String[] fields = line.split(FILE_SEPARATOR);

                        MxNPVData npvData = new MxNPVData();

                        npvData.setProcessDate(stringToDate(fields[BondFileColumnPositions.PROCESSDATE.position]));
                        npvData.setContractID(formatContractID(fields[BondFileColumnPositions.CONTRACTID.position]));
                        npvData.setCurrency(fields[BondFileColumnPositions.CURRENCY.position]);
                        npvData.setNpv(Double.parseDouble(fields[BondFileColumnPositions.NPV.position]));

                        trades = DSConnection.getDefault().getRemoteTrade().getTradesByKeywordNameAndValue("MurexRootContract", npvData.getContractID());

                        if (!Util.isEmpty(trades)) {
                            for (int j = 0; j < trades.size(); j++) {
                                if (trades.get(j).getProduct() instanceof FX && !ScheduledTaskImportMxNpv.CANCELED.equalsIgnoreCase(String.valueOf(trades.get(j).getStatus()))) {
                                    npvAllDataMap.put(customMxGlobalID(trades.get(j).getKeywordValue("Mx Global ID")) + npvData.getCurrency(), npvData);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.error(this, "Cannot set line: " + line + "Error: " + e);
                    }
                }
            }
        }
        return npvAllDataMap;
    }

    /**
     *
     * @param mxGlobalId
     * @return
     */
    public String customMxGlobalID(String mxGlobalId) {
        String mxGlobalIdFormat = mxGlobalId;
        if (StringUtils.startsWith(mxGlobalId, "TOM")){
            mxGlobalIdFormat = StringUtils.substring(mxGlobalId, 0, mxGlobalId.length() - 4);
        }
        return mxGlobalIdFormat;
    }

    /**
     *
     * @param contractID
     * @return
     */
    public String formatContractID(String contractID) {
        return StringUtils.startsWith(contractID, "MX-") ? StringUtils.substring(contractID, 3) : StringUtils.substring(contractID, 2);
    }



    enum BondFileColumnPositions{

        PROCESSDATE(0),
        CONTRACTID(1),
        CURRENCY(2),
        NPV(3);

        int position;

        BondFileColumnPositions(int columnPosition){
            this.position=columnPosition;
        }
    }
}
