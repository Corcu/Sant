package calypsox.tk.util.mxmtm;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.product.Repo;
import com.calypso.tk.util.TradeArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author aalonsop
 */
public class RepoMtmTradeHandler extends MxMtmTradeHandler{

    public static final String MARKETVALUEMAN_PLMARKNAME = "MARKETVALUEMAN";
    public static final String BUYSELLCASH_PLMARKNAME = "BUYSELLCASH";

    @Override
    public void addPLMarkValues(PLMark plMark, MxMTMData mtmData, int sign) {
        plMark.addPLMarkValue(createPLMarkValue(MARKETVALUEMAN_PLMARKNAME, mtmData.getCurrency(), mtmData.getCurrency(), mtmData.getMarketvalueMan() * sign));
        plMark.addPLMarkValue(createPLMarkValue(BUYSELLCASH_PLMARKNAME, mtmData.getCurrency(), mtmData.getCurrency(), mtmData.getBuySellCash() * sign));

    }

    @Override
    public void matchTades(Map<String, MxMTMData> mtmAllDataMap, TradeArray tradearray, String keyword) {
        Log.info(this, "Matching trades.");

        List<String> references = new ArrayList<>();
        for (Trade trade : tradearray.getTrades()) {
            String contractID = trade.getKeywordValue(keyword);
            if (!references.contains(contractID)) {
                StringBuilder identifier = new StringBuilder();
                identifier.append(contractID);
                identifier.append("_");
                if(trade.getProduct() instanceof Repo){
                    Repo repo = (Repo)trade.getProduct();
                    String direction = repo.getDirection(Repo.REPO, repo.getSign());
                    if (direction.equals("Reverse")) {
                        identifier.append("B");
                    } else {
                        identifier.append("S");
                    }
                }
                MxMTMData mtmData = mtmAllDataMap.get(identifier.toString());
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

                        mtmData.setProcessDate(stringToDate(fields[RepoFileColumnPositions.PROCESSDATE.position]));
                        mtmData.setContractID(fields[RepoFileColumnPositions.CONTRACTID.position]);
                        mtmData.setDealID(fields[RepoFileColumnPositions.DEALID.position]);
                        mtmData.setBlockNumber(Integer.parseInt(fields[RepoFileColumnPositions.BLOCKNUMBER.position]));
                        mtmData.setFrontID(Long.parseLong(fields[RepoFileColumnPositions.FRONTID.position]));
                        mtmData.setDealGRID(Integer.parseInt(fields[RepoFileColumnPositions.DEALGRID.position]));
                        mtmData.setCurrency(fields[RepoFileColumnPositions.CURRENCY.position]);
                        mtmData.setDummy(fields[RepoFileColumnPositions.DUMMY.position]);
                        mtmData.setInterestCash(Double.parseDouble(fields[RepoFileColumnPositions.INTERESTCASH.position]));
                        mtmData.setBuySellCash(Double.parseDouble(fields[RepoFileColumnPositions.BUYSELLCASH.position]));
                        mtmData.setTaxComCash(Double.parseDouble(fields[RepoFileColumnPositions.TAXCOMCASH.position]));
                        mtmData.setCarryAcc(Double.parseDouble(fields[RepoFileColumnPositions.CARRYACC.position]));
                        mtmData.setMarketvalueMan(Double.parseDouble(fields[RepoFileColumnPositions.MARKETVALUEMAN.position]));
                        mtmData.setEntity(fields[RepoFileColumnPositions.ENTITY.position]);
                        mtmData.setProcessID(fields[RepoFileColumnPositions.PROCESSID.position]);
                        mtmData.setGeneratorID(Integer.parseInt(fields[RepoFileColumnPositions.GENERATORID.position]));
                        mtmData.setCompraVenta(fields[RepoFileColumnPositions.COMPRAVENTA.position]);
                        mtmData.setPastCash(Double.parseDouble(fields[RepoFileColumnPositions.PASTCASH.position]));
                        mtmData.setFutureCash(Double.parseDouble(fields[RepoFileColumnPositions.FUTURECASH.position]));

                        StringBuilder identifier = new StringBuilder();
                        identifier.append(mtmData.getDealID());
                        identifier.append("_");
                        identifier.append(mtmData.getCompraVenta());
                        mtmAllDataMap.put(identifier.toString(), mtmData);
                    } catch (Exception e) {
                        Log.error(this, "Cannot set line: " + line + "Error: " + e);
                    }
                }
            }
        }
        return mtmAllDataMap;
    }


    enum RepoFileColumnPositions{

        PROCESSDATE(0),
        CONTRACTID(1),
        DEALID(2),
        BLOCKNUMBER(3),
        FRONTID(4),
        DEALGRID(5),
        CURRENCY(6),
        DUMMY(7),
        INTERESTCASH(8),
        BUYSELLCASH(9),
        TAXCOMCASH(10),
        CARRYACC(11),
        MARKETVALUEMAN(12),
        ENTITY(13),
        PROCESSID(14),
        GENERATORID(15),
        COMPRAVENTA(16),
        PASTCASH(17),
        FUTURECASH(18);

        int position;

        RepoFileColumnPositions(int columnPosition){
            this.position=columnPosition;
        }
    }
}
