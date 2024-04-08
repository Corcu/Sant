package calypsox.apps.reporting;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BODisponibleOnlyBooksFilter implements BOPositionFilter {
    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {
        applyBookFilter(positions);
        return positions;
    }
    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
        return null;
    }

    private void applyBookFilter(InventorySecurityPositionArray positions){
        List<String> excludeBooksList = DomainValues.values(BODisponiblePositionFilter.BOOKS_FILTER);

        if(!Util.isEmpty(positions.getInventorySecurityPositions())){
            List<InventorySecurityPosition> filteredPositionList = Arrays.stream(positions.getInventorySecurityPositions())
                    .parallel()
                    .filter(Objects::nonNull)
                    .filter(position -> {
                        Book book = position.getBook();
                        return null!=book && !excludeBooksList.contains(book.getName());
                    }).collect(Collectors.toList());
            positions.clear();
            positions.addAll(new InventorySecurityPositionArray(filteredPositionList));
        }
    }
}
