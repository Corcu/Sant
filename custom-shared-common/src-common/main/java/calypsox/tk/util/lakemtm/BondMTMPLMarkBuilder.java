package calypsox.tk.util.lakemtm;

import calypsox.tk.util.ScheduledTaskImportLakeMtM;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.util.TradeArray;

import java.util.HashMap;

/**
 * @author aalonsop
 */
public class BondMTMPLMarkBuilder extends LakeMTMPLMarkBuilder {
    
    @Override
    public void addPLMarkValues(PLMark plMark, ScheduledTaskImportLakeMtM.MTMData mtmData, int sign) {
        plMark.addPLMarkValue(createPLMarkValue("MTM_FULL_LAGO", mtmData.getBaseCCY(), mtmData.getBaseCCY(), mtmData.getBaseMtM() * sign, "NPV"));
    }

    @Override
    void addNPVBasePLMarkValue(PLMark plMark, Trade trade, ScheduledTaskImportLakeMtM.MTMData mtmData, CollateralConfig cc, HashMap<String, Double> fxRates, int sign) {

    }

    @Override
    void updateTradeKwds(Trade trade, CollateralConfig collConfig, TradeArray tradesToSave, TradeFilter tradeFilter, String tradeAction) {

    }
}
