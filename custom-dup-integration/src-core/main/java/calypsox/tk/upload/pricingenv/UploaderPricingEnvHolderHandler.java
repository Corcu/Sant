package calypsox.tk.upload.pricingenv;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.upload.util.PricingEnvHolder;

/**
 * @author paisanu
 * Creates a new PricingEnvHolder config inside JVM's PricingEnvHolder singleton
 * forcing the use of OFFICIAL_ACCOUNTING P.E
 */
public class UploaderPricingEnvHolderHandler {

    /**
     * Init a new PricingEnvHolder config with key=Uploader and peName="OFFICIAL_ACCOUNTING"
     */
    public static void initOficcialAccPricingEnvHolder(){
        initPricingEnvHolder(getOfficialAccountingStr(),getUploaderStr());
    }

    /**
     * Looks for the given PEHolder config and in case of not being initialized set the param PE to it
     * @param uploaderEnv
     * @param holderConfigName
     */
    public static void initPricingEnvHolder(String pricingEnvName, String holderConfigName){
            try {
                PricingEnvHolder holder = PricingEnvHolder.getInstance(holderConfigName);
                if(holder!=null&&Util.isEmpty(holder.getPricingEnvName())){
                    PricingEnv uploaderEnv = PricingEnv.loadPE(pricingEnvName, new JDatetime());
                    holder.setPricingEnv(uploaderEnv);
                }
            } catch (Exception exc) {
                Log.error(UploaderPricingEnvHolderHandler.class.getSimpleName(),exc.getCause());
            }
    }

    private static String getOfficialAccountingStr(){
        return "OFFICIAL_ACCOUNTING";
    }
    public static String getUploaderStr(){
        return "Uploader";
    }
}
