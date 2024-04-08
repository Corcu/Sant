package calypsox.tk.pricer;

import calypsox.tk.util.ScheduledTaskImportRepoMtM;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.helper.RemoteAPI;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMark;

import java.util.TimeZone;

/**
 * @author anonimous
 */
public class RepoMTM_NET_MUREXCalculator implements MTM_NET_MUREXCalculator{

    public void calculate(Trade trade, JDatetime valDatetime, PricingEnv env, Pricer pricer, PricerMeasure measureToCalculate) throws PricerException {
        JDate valeDate = valDatetime.getJDate(TimeZone.getDefault());
        String pmCurrency = measureToCalculate.getCurrency();
        PricerMeasure pmAccrual = pricer.price(trade, valDatetime, env, PricerMeasure.ACCRUAL_FIRST);

        RemoteMark remoteMark = DSConnection.getDefault().getRemoteMark();
        PLMark plMark = null;
        try {
            plMark = RemoteAPI.getMark(remoteMark, "PL", trade.getLongId(), null, env.getName(), valeDate);
        } catch (PersistenceException e) {
            Log.error(this, "Error : " + e);
        }
        if (plMark == null) {
            try {
                plMark = RemoteAPI.getMark(remoteMark, "NONE", trade.getLongId(), null, env.getName(), valeDate);
            } catch (PersistenceException e) {
                Log.error(this, "Error : " + e);
            }
            if (plMark == null) {
                measureToCalculate.setValue(Double.NaN);
                throw new PricerException("Could not find PLMark for Trade " + trade.getLongId());
            }
        }

        // Using ScheduledTaskImportRepoMtM global vars because ST and PricerMeasure are directly connected
        double plmMarketValueManValue = 0.0D;
        PLMarkValue plmMarketValueMan = CollateralUtilities.retrievePLMarkValue(plMark, ScheduledTaskImportRepoMtM.MARKETVALUEMAN_PLMARKNAME);
        if (plmMarketValueMan == null) {
            Log.error(this, "No PLMark " + ScheduledTaskImportRepoMtM.MARKETVALUEMAN_PLMARKNAME + " found for Trade " + trade.getLongId() + " on date " + valeDate);
        }
        else {
            plmMarketValueManValue = plmMarketValueMan.getMarkValue();
            pmCurrency = plmMarketValueMan.getCurrency();
        }

        double plmBuySellCashValue = 0.0D;
        PLMarkValue plmBuySellCash = CollateralUtilities.retrievePLMarkValue(plMark, ScheduledTaskImportRepoMtM.BUYSELLCASH_PLMARKNAME);
        if (plmBuySellCash == null) {
            Log.error(this, "No PLMark " + ScheduledTaskImportRepoMtM.BUYSELLCASH_PLMARKNAME + " found for Trade " + trade.getLongId() + " on date " + valeDate);
        }
        else {
            plmBuySellCashValue = plmBuySellCash.getMarkValue();
            // both PLMarkValues should have same CCY so no need to check that here
        }

        measureToCalculate.setValue(plmMarketValueManValue + plmBuySellCashValue - pmAccrual.getValue());
        measureToCalculate.setCurrency(pmCurrency);
    }
}
