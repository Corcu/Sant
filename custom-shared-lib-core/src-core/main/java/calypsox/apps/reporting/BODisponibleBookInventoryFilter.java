package calypsox.apps.reporting;

import com.calypso.tk.bo.Inventory;
import com.calypso.tk.refdata.DomainValues;

import java.util.List;
import java.util.Optional;

public class BODisponibleBookInventoryFilter extends BODisponibleBondInventoryFilter {
    @Override
    public boolean accept(Inventory inv) {
        List<String> excludeBooksList = DomainValues.values("BODisponibleExcludeBooks");
        boolean acceptedBook = Optional.ofNullable(inv).map(Inventory::getBook)
                .filter(book -> !excludeBooksList.contains(book.getName())).isPresent();
        return acceptedBook && super.accept(inv);
    }

}
