package calypsox.tk.util.lakemtm;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.util.ScheduledTaskImportLakeMtM;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.util.TradeArray;

/**
 * @author aalonsop
 */
public class CollateralMTMPLMarkBuilder extends DefaultMTMPLMarkBuilder{


    @Override
    void addPLMarkValues(PLMark plMark, ScheduledTaskImportLakeMtM.MTMData mtmData, int sign) {
        plMark.addPLMarkValue(createPLMarkValue(SantPricerMeasure.S_MARGIN_CALL, mtmData.getBaseCCY(), mtmData.getBaseCCY(), mtmData.getBaseMtM() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue(PricerMeasure.S_NPV, mtmData.getBaseCCY(), mtmData.getBaseCCY(), mtmData.getBaseMtM() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue(SantPricerMeasure.S_NPV_LEG1, mtmData.getBaseCCY(),mtmData.getBaseCCY(), mtmData.getBaseMtM() * sign, "NPV"));
        plMark.addPLMarkValue(createPLMarkValue(SantPricerMeasure.S_NPV_LEG2,mtmData.getBaseCCY(), mtmData.getBaseCCY(), mtmData.getBaseMtM() * sign, "NPV"));
    }

    @Override
    void updateTradeKwds(Trade trade, CollateralConfig collConfig, TradeArray tradesToSave, TradeFilter tradeFilter, String tradeAction){

    }

}
