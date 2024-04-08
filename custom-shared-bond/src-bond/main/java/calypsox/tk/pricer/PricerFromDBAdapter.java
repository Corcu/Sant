package calypsox.tk.pricer;

import calypsox.tk.core.SantPricerMeasure;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.pricer.PricerFromDB;

import java.util.*;

/**
 * @author paisanu
 */
public interface PricerFromDBAdapter {

    /**
     *
     * @param trade
     * @param valDatetime
     * @param env
     * @param measures
     * @return An array containing those measures that weren't priced using PricerFromDB
     * @throws PricerException
     */
    default PricerMeasure[] priceFromDB(Trade trade, JDatetime valDatetime, PricingEnv env, PricerMeasure[] measures) throws PricerException {
        boolean isTargetPricingEnv=Optional.ofNullable(env)
                .map(PricingEnv::getName).map(name->name.equals(getTargetPricingEnvName()))
                .orElse(false);
        if(isTargetPricingEnv) {
            Map<Integer, String> fromDBMeasureTypes = getFromDBMeasures();
            List<PricerMeasure> fromDBMeasures = new ArrayList<>();
            List<PricerMeasure> notPricedMeasures = new ArrayList<>();
            for (PricerMeasure inputMeasure : measures) {
                boolean isFromDBMeasure=Optional.ofNullable(inputMeasure).map(PricerMeasure::getType)
                        .map(fromDBMeasureTypes::get).map(typeStr -> typeStr.equals(inputMeasure.getName()))
                        .orElse(false);
                if (isFromDBMeasure) {
                    fromDBMeasures.add(inputMeasure);
                } else {
                    notPricedMeasures.add(inputMeasure);
                }
            }
            if(!fromDBMeasures.isEmpty()) {
                PricerFromDB pricerFromDB = new PricerFromDB();
                pricerFromDB.price(trade, valDatetime, env, fromDBMeasures.toArray(new PricerMeasure[0]));
            }
            measures=notPricedMeasures.toArray(new PricerMeasure[0]);
        }
        return measures;
    }

    /**
     * @return A list containing PricerMeasure's types that need to be processed by using PricerFromDB.
     */
    default Map<Integer, String> getFromDBMeasures(){
        Map<Integer,String> measureMap=new HashMap<>();
        measureMap.put(PricerMeasure.ACCRUAL,PricerMeasure.S_ACCRUAL);
        measureMap.put(PricerMeasure.INDEPENDENT_AMOUNT,PricerMeasure.S_INDEPENDENT_AMOUNT);
        measureMap.put(SantPricerMeasure.INDEPENDENT_AMOUNT_BASE,SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE);
        measureMap.put(PricerMeasure.NPV,PricerMeasure.S_NPV);
        measureMap.put(SantPricerMeasure.NPV_BASE,SantPricerMeasure.S_NPV_BASE);
        measureMap.put(PricerMeasure.MARGIN_CALL,PricerMeasure.S_MARGIN_CALL);
        return measureMap;
    }

    /**
     * @return PricingEnv's name to be affected
     */
    default String getTargetPricingEnvName(){
        return "DirtyPrice";
    }
}
