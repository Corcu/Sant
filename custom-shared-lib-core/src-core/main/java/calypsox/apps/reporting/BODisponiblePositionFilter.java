package calypsox.apps.reporting;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
/**
 * @author acd
 */
public class BODisponiblePositionFilter extends BODisponibleBondFilter {

    public final static String ACCOUNT_FILTER = "BODisponibleExcludeAccounts";
    public final static String ISIN_FILTER = "BODisponibleExcludeIsin";
    public final static String BOOKS_FILTER = "BODisponibleExcludeBooks";
    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {
        super.filterSecurity(positions);
        applyFilters(positions);
        return positions;
    }
    private void applyFilters(InventorySecurityPositionArray positions){
        List<String> excludeAccountList = DomainValues.values(ACCOUNT_FILTER);
        List<String> excludeIsinList = DomainValues.values(ISIN_FILTER);
        List<String> excludeBooksList = DomainValues.values(BOOKS_FILTER);

        if(!Util.isEmpty(positions.getInventorySecurityPositions())){
            List<InventorySecurityPosition> filteredPositionList = Arrays.stream(positions.getInventorySecurityPositions())
                    .filter(Objects::nonNull)
                    .filter(position -> null!=position.getAccount() && !excludeAccountList.contains(position.getAccount().getName()))
                    .filter(position -> null!=position.getProduct() && !excludeIsinList.contains(position.getProduct().getSecCode("ISIN")))
                    .filter(position -> null!=position.getBook() && !excludeBooksList.contains(position.getBook().getName()))
                    .collect(Collectors.toList());
            positions.clear();
            positions.addAll(new InventorySecurityPositionArray(filteredPositionList));
        }
    }

    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
        return positions;
    }
}
