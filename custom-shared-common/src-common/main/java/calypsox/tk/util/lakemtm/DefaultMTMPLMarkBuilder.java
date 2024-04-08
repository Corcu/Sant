package calypsox.tk.util.lakemtm;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.util.ScheduledTaskImportLakeMtM;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.util.TradeArray;

import java.util.HashMap;

/**
 * @author aalonsop
 */
public class DefaultMTMPLMarkBuilder extends LakeMTMPLMarkBuilder {


     void addNPVBasePLMarkValue(PLMark plMark, Trade trade, ScheduledTaskImportLakeMtM.MTMData mtmData, CollateralConfig cc, HashMap<String, Double> fxRates, int sign) {
        double baseMtmValue = mtmData.getBaseMtM();
        String baseMtmCcy = mtmData.getBaseCCY();

        CollateralConfig collConfig = getCollateralConfig(trade);
        if (collConfig != null && !collConfig.getCurrency().equals(mtmData.getBaseCCY())) {

            JDate fxdate = mtmData.getDate();

            String fxRateKey = fxdate.toString() + mtmData.getBaseCCY() + collConfig.getCurrency();
            Double fxRate = 0.0D;
            if (!fxRates.containsKey(fxRateKey)) {
                fxRate = CollateralUtilities.getFXRate(fxdate, mtmData.getBaseCCY(), collConfig.getCurrency());
                fxRates.put(fxRateKey, fxRate);
            } else {
                fxRate = fxRates.get(fxRateKey);
            }

            if (fxRate > 0.0) {
                baseMtmCcy = collConfig.getCurrency();
                baseMtmValue = mtmData.getBaseMtM() * fxRate;
            }
        }
        plMark.addPLMarkValue(createPLMarkValue(SantPricerMeasure.S_NPV_BASE, baseMtmCcy, baseMtmCcy, baseMtmValue * sign, "NPV"));
    }

    void updateTradeKwds(Trade trade, CollateralConfig collConfig, TradeArray tradesToSave, TradeFilter tradeFilter, String tradeAction) {
        if (tradeFilter.accept(trade)) {
            Trade tradetoSave = trade.clone();
            if (collConfig == null) {
                tradetoSave.setInternalReference("");
                tradetoSave.removeKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER);

            } else {
                tradetoSave.setInternalReference(String.valueOf(collConfig.getId()));
                tradetoSave.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, collConfig.getId());

            }
            tradetoSave.setAction(Action.valueOf(tradeAction));
            tradetoSave.addKeywordAsLong("BO_REFERENCE", trade.getLongId());
            tradesToSave.add(tradetoSave);
        }
    }

    @Override
     void addPLMarkValues(PLMark plMark, ScheduledTaskImportLakeMtM.MTMData mtmData, int sign) {
        plMark.addPLMarkValue(createPLMarkValue("MTM_MARKET_VALUE", mtmData.getBaseCCY(), mtmData.getBaseCCY(), mtmData.getBaseMtM() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue("MTM_FULL_LAGO_BASE", mtmData.getBaseCCY(), mtmData.getBaseCCY(), mtmData.getBaseMtM() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue("MTM_FULL_LAGO", mtmData.getBaseCCY(), mtmData.getBaseCCY(), mtmData.getBaseMtM() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue(SantPricerMeasure.S_MARGIN_CALL, mtmData.getBaseCCY(), mtmData.getBaseCCY(), mtmData.getBaseMtM() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue(PricerMeasure.S_NPV, mtmData.getBaseCCY(), mtmData.getBaseCCY(), mtmData.getBaseMtM() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue(SantPricerMeasure.S_NPV_LEG1, mtmData.getLine1CCY(), mtmData.getLine1CCY(), mtmData.getLine1MTM() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue(SantPricerMeasure.S_NPV_LEG2, mtmData.getLine2CCY(), mtmData.getLine2CCY(), mtmData.getLine2MTM() * sign, "NPV"));
    }
}
