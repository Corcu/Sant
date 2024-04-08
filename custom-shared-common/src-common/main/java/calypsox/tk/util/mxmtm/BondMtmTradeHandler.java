package calypsox.tk.util.mxmtm;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.util.TradeArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author aalonsop
 */
public class BondMtmTradeHandler extends MxMtmTradeHandler{

    @Override
    public void addPLMarkValues(PLMark plMark, MxMTMData mtmData, int sign) {
        double fullMtmValue=mtmData.getMarketvalueMan()+mtmData.getFutureCash();
        plMark.addPLMarkValue(createPLMarkValue("MTM_NET_MUREX", mtmData.getCurrency(), mtmData.getCurrency(), fullMtmValue * sign));
    }

    @Override
    public void matchTades(Map<String, MxMTMData> mtmAllDataMap, TradeArray tradearray, String keyword) {
        Log.info(this, "Matching trades.");

        List<String> references = new ArrayList<>();
        for (Trade trade : tradearray.getTrades()) {
            String contractID = trade.getKeywordValue(keyword);
            if (!references.contains(contractID)) {
                MxMTMData mtmData = mtmAllDataMap.get(contractID);
                if (mtmData != null) {
                    mtmData.setTrade(trade);
                    references.add(contractID);
                } else {
                    Log.info(this, "Trade " + trade.getLongId() + " not found on file. ");
                }
            } else {
                Log.info(this, "Duplicate trade with same REFERENCE: " + contractID + " tradeId: " + trade.getLongId());
            }
        }
    }


    @Override
    public Map<String, MxMTMData> parseLines(List<String> lines) {
        Map<String, MxMTMData> mtmAllDataMap = new HashMap<>();

        if (!Util.isEmpty(lines)) {
            for (final String line : lines) {
                if (!line.isEmpty()) {
                    try {
                        final String[] fields = line.split(FILE_SEPARATOR);

                        MxMTMData mtmData = new MxMTMData();

                        mtmData.setProcessDate(stringToDate(fields[BondFileColumnPositions.PROCESSDATE.position]));
                        mtmData.setContractID(fields[BondFileColumnPositions.CONTRACTID.position]);
                        mtmData.setDealID(fields[BondFileColumnPositions.DEALID.position]);
                        mtmData.setBlockNumber(Integer.parseInt(fields[BondFileColumnPositions.BLOCKNUMBER.position]));
                        mtmData.setFrontID(Long.parseLong(fields[BondFileColumnPositions.FRONTID.position]));
                        mtmData.setDealGRID(Integer.parseInt(fields[BondFileColumnPositions.DEALGRID.position]));
                        mtmData.setCurrency(fields[BondFileColumnPositions.CURRENCY.position]);
                        mtmData.setDummy("");
                        mtmData.setInterestCash(Double.parseDouble(fields[BondFileColumnPositions.INTERESTCASH.position]));
                        mtmData.setBuySellCash(Double.parseDouble(fields[BondFileColumnPositions.BUYSELLCASH.position]));
                        mtmData.setTaxComCash(Double.parseDouble(fields[BondFileColumnPositions.TAXCOMCASH.position]));
                        mtmData.setCarryAcc(Double.parseDouble(fields[BondFileColumnPositions.CARRYACC.position]));
                        mtmData.setMarketvalueMan(Double.parseDouble(fields[BondFileColumnPositions.MARKETVALUEMAN.position]));
                        mtmData.setEntity(fields[BondFileColumnPositions.ENTITY.position]);
                        mtmData.setProcessID(fields[BondFileColumnPositions.PROCESSID.position]);
                        mtmData.setGeneratorID(Integer.parseInt(fields[BondFileColumnPositions.GENERATORID.position]));
                        mtmData.setCompraVenta(fields[BondFileColumnPositions.COMPRAVENTA.position]);
                        mtmData.setPastCash(Double.parseDouble(fields[BondFileColumnPositions.PASTCASH.position]));
                        mtmData.setFutureCash(Double.parseDouble(fields[BondFileColumnPositions.FUTURECASH.position]));

                        String key = mtmData.getDealID();
                        if (mtmAllDataMap.containsKey(key)) {
                            MxMTMData previousMtmData = mtmAllDataMap.get(key);
                            int dealGRID = mtmData.getDealGRID();
                            if (dealGRID > previousMtmData.getDealGRID()) {
                                mtmAllDataMap.replace(key, previousMtmData, mtmData);
                            }
                        } else {
                            mtmAllDataMap.put(key, mtmData);
                        }
                    } catch (Exception e) {
                        Log.error(this, "Cannot set line: " + line + "Error: " + e);
                    }
                }
            }
        }
        return mtmAllDataMap;
    }


    enum BondFileColumnPositions{

        PROCESSDATE(0),
        CONTRACTID(1),
        DEALID(2),
        BLOCKNUMBER(3),
        FRONTID(4),
        DEALGRID(5),
        CURRENCY(6),
        INTERESTCASH(7),
        BUYSELLCASH(8),
        TAXCOMCASH(9),
        CARRYACC(10),
        MARKETVALUEMAN(11),
        ENTITY(12),
        PROCESSID(13),
        GENERATORID(14),
        COMPRAVENTA(15),
        PASTCASH(16),
        FUTURECASH(17);

        int position;

        BondFileColumnPositions(int columnPosition){
            this.position=columnPosition;
        }
    }
}
