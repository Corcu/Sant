package calypsox.tk.util.lakemtm;

import calypsox.tk.util.ScheduledTaskImportLakeMtM;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 */
public abstract class LakeMTMPLMarkBuilder {

    public static LakeMTMPLMarkBuilder getInstance(Trade trade) {
        LakeMTMPLMarkBuilder instance;
        if (Optional.ofNullable(trade).map(Trade::getProduct).map(p -> p instanceof Bond).orElse(false)) {
            instance = new BondMTMPLMarkBuilder();
        } else {
            instance = new DefaultMTMPLMarkBuilder();
        }
        return instance;
    }


    public void addPLMarkForTrade(List<PLMark> plMarks, Trade trade, ScheduledTaskImportLakeMtM.MTMData mtmData, String pricingEnv, HashMap<String, Double> fxRates, int sign) {
        addPLMarkForTrade(plMarks,trade,mtmData,null,"",pricingEnv,null,fxRates,sign);
    }

    public void addPLMarkForTrade(List<PLMark> plMarks, Trade trade, ScheduledTaskImportLakeMtM.MTMData mtmData, TradeArray tradesToSave, String tradeAction, String pricingEnv, TradeFilter tradeFilter, HashMap<String, Double> fxRates, int sign) {
        PLMark plMark = new PLMark();
        plMark.setTradeId(trade.getLongId());
        Book book = BOCache.getBook(DSConnection.getDefault(), mtmData.getBook());
        if (book != null) {
            plMark.setBookId(book.getId());

            plMark.setValDate(mtmData.getDate());
            plMark.setPricingEnvName(pricingEnv);
            plMark.setType("PL");

            addPLMarkValues(plMark, mtmData, sign);

            CollateralConfig cc=getCollateralConfig(trade);
            addNPVBasePLMarkValue(plMark, trade, mtmData,cc,fxRates, sign);
            updateTradeKwds(trade,cc,tradesToSave,tradeFilter,tradeAction);

            plMarks.add(plMark);
        } else {
            Log.system(this.getClass().getSimpleName(), "Couldn't add PLMark for trade:" + trade.getLongId() + " cause parsed book " + mtmData.getBook() + " does not exists");
        }
    }

    abstract void addPLMarkValues(PLMark plMark, ScheduledTaskImportLakeMtM.MTMData mtmData, int sign);

    /**
     * Conversion for NPV_BASE
     *
     * @param plMark
     * @param trade
     * @param mtmData
     * @param tradesToSave
     * @param tradeAction
     * @param tradeFilter
     * @param fxRates
     * @param sign
     */
    abstract void addNPVBasePLMarkValue(PLMark plMark, Trade trade, ScheduledTaskImportLakeMtM.MTMData mtmData, CollateralConfig cc,HashMap<String, Double> fxRates, int sign);


    abstract void updateTradeKwds(Trade trade, CollateralConfig collConfig, TradeArray tradesToSave, TradeFilter tradeFilter, String tradeAction);


    PLMarkValue createPLMarkValue(String name, String ccy, String ccy2, double mtm, String type) {
        PLMarkValue npvPriceMarkValue = new PLMarkValue();
        npvPriceMarkValue.setMarkName(name);
        npvPriceMarkValue.setMarkValue(mtm);
        npvPriceMarkValue.setCurrency(ccy);
        npvPriceMarkValue.setOriginalCurrency(ccy2);
        npvPriceMarkValue.setAdjustmentType(type);
        return npvPriceMarkValue;
    }

    CollateralConfig getCollateralConfig(Trade trade) {
        CollateralConfig marginCallConfig = null;
        int mccId = 0;

        try {
            mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");

        } catch (Exception e) {
            Log.error(this.getClass().getSimpleName(), e); // sonar
        }

        if (mccId == 0) {
            ArrayList<String> errorMsgs = new ArrayList<String>();
            try {
                marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);
        }

        return marginCallConfig;
    }

}
