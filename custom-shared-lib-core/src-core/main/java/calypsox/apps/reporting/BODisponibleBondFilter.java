package calypsox.apps.reporting;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
/**
 * @author acd
 */
public class BODisponibleBondFilter implements BOPositionFilter {

    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {
        applyFilters(positions);
        return positions;
    }

    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
        return null;
    }

    private void applyFilters(InventorySecurityPositionArray positions){
        if(!Util.isEmpty(positions.getInventorySecurityPositions())){
            List<InventorySecurityPosition> filteredPositionList = Arrays.stream(positions.getInventorySecurityPositions())
                    .filter(Objects::nonNull)
                    .filter(position -> {
                        Product product = position.getProduct();
                        return product instanceof Bond || (product!=null && "Bond".equalsIgnoreCase(product.getProductFamily()));
                    }).collect(Collectors.toList());
            positions.clear();
            positions.addAll(new InventorySecurityPositionArray(filteredPositionList));
        }
    }
}
