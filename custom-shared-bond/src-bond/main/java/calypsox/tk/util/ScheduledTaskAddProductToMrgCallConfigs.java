package calypsox.tk.util;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class ScheduledTaskAddProductToMrgCallConfigs extends ScheduledTask {

    public static final String PRODUCT_TYPE_TO_ADD = "Product Type to add";
    public static final String TARGET_COLLATERAL_CONFIG_TYPE = "Collateral Config type";

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        boolean res=true;
        String collateralConfigType=getAttribute(TARGET_COLLATERAL_CONFIG_TYPE);
        String productTypeToAdd=getAttribute(PRODUCT_TYPE_TO_ADD);
        MarginCallConfigFilter contractFilter = new MarginCallConfigFilter();
        Vector<String> types=new Vector<>();
        if(!Util.isEmpty(collateralConfigType)) {
            types.add(collateralConfigType);
        }
        contractFilter.setContractTypes(types);
        List<CollateralConfig> configs=null;
        try {
          configs = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfigs(contractFilter, null);
        } catch (CollateralServiceException exc) {
            Log.error(this,exc.getCause());
        }
            if(configs!=null) {
                Log.system(this.getClass().getSimpleName(), configs.size()+" contracts loaded");
                for (CollateralConfig collateralConfig : configs) {
                    Vector<String> productList = collateralConfig.getProductList();
                    if (productList != null&&!productList.contains(productTypeToAdd)) {
                        productList.add(productTypeToAdd);
                        Collections.sort(productList);
                        collateralConfig.setProductList(productList);
                        Log.system(this.getClass().getSimpleName(), "Adding "+productTypeToAdd+" productType to "+collateralConfig.getName());
                        try {
                            ServiceRegistry.getDefault().getCollateralDataServer().save(collateralConfig);
                            Log.system(this.getClass().getSimpleName(), "Contract Updated: "+collateralConfig.getName());
                        } catch (CollateralServiceException exc) {
                           Log.error(this,"Couldnt save contract"+ collateralConfig.getName(),exc.getCause());
                        }
                    }
                }
            }
//
        return res;
    }

    private Vector<String> buildFinalProductList(Vector<String> productList,String productTypeToAdd){
        if(productList!=null && !productList.contains(productTypeToAdd)){
            productList.add(productTypeToAdd);
            Collections.sort(productList);
        }
        return productList;
    }
    @Override
    public Vector getDomainAttributes() {
        final Vector result = super.getDomainAttributes();
        result.add(PRODUCT_TYPE_TO_ADD);
        result.add(TARGET_COLLATERAL_CONFIG_TYPE);
        return result;
    }

    @Override
    public String getTaskInformation() {
        return "Adds ProductType's attribute value as a new domain product for target CollateralConfigs";
    }
}
