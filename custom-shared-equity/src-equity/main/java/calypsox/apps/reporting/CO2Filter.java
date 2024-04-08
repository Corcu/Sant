package calypsox.apps.reporting;


import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;


@SuppressWarnings("deprecation")
public class CO2Filter implements BOPositionFilter {


    public static final String CO2 = "CO2";
    public static final String VCO2 = "VCO2";
    public static final String EQUITY_TYPE = "EQUITY_TYPE";


    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {
        InventorySecurityPositionArray positionList = new InventorySecurityPositionArray();
        for (int i = 0; i < positions.size(); i++) {
            InventorySecurityPosition pos = positions.get(i);
            Product product = pos.getProduct();
            String equityType = product.getSecCode(EQUITY_TYPE);
            if (!Util.isEmpty(equityType) && (CO2.equalsIgnoreCase(equityType) || VCO2.equalsIgnoreCase(equityType))) {
                positionList.add(pos);
            }
        }
        return positionList;
    }


    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
        return positions;
    }


}