package calypsox.tk.util;

import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;

import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.sql.CollateralConfigFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.util.*;

public class ScheduledTaskSANT_MCC_PRODUCT_TYPE_SETTER extends ScheduledTask {


    @Override
    public String getTaskInformation() {
        return "This scheduledTask sets a products list by contract type";
    }

    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>(super.buildAttributeDefinition());
        attributeList.add(attribute("Product Types"));
        attributeList.add(attribute("Contract Type"));

        return attributeList;
    }

    @Override
    protected boolean process(DSConnection dsConn, PSConnection psConn) {
        String productTypes = getAttribute("Product Types");
        String contract_type = getAttribute("Contract Type");
            try {
                if (!Util.isEmpty(productTypes) && !Util.isEmpty(contract_type)) {

                    String[] products = productTypes.split(";");
                    CollateralConfigFilter collateralConfigFilter = new CollateralConfigFilter();
                    collateralConfigFilter.addContractType(contract_type);
                    List<CollateralConfig> ccs = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfigByFilter(collateralConfigFilter, true);
                    int counter = 0;
                    for (CollateralConfig cc : ccs) {
                        cc.setProductList(new Vector(Arrays.asList(products)));
                        try {
                            ServiceRegistry.getDefault().getCollateralDataServer().save(cc);
                        } catch (CollateralServiceException e) {
                            Log.error(this.getClass().getSimpleName(), "Couldn't save MCContract " + cc.getId());
                        }
                        Log.info(this.getClass().getSimpleName(), counter++ + ": Contract Id " + cc.getId() + " saved; ");
                    }
                }
            } catch (CollateralServiceException exc) {
                Log.error(this.getClass().getSimpleName(), exc.getCause());
            }
        return true;
    }

}
