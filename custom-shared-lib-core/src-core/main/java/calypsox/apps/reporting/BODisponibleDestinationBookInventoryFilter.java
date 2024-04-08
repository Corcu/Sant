package calypsox.apps.reporting;

import com.calypso.tk.bo.Inventory;
import com.calypso.tk.refdata.LegalEntityAttribute;

import java.util.Vector;

public class BODisponibleDestinationBookInventoryFilter extends BODisponibleBookInventoryFilter {
    @Override
    public boolean accept(Inventory inv) {
        return checkDestinationBook(inv) && super.accept(inv);
    }

    private boolean checkDestinationBook(Inventory position){
        if(position.getBook()!=null && position.getBook().getLegalEntity()!=null){
            Vector<LegalEntityAttribute> legalEntityAttributes = (Vector<LegalEntityAttribute>) position.getBook().getLegalEntity().getLegalEntityAttributes();
            String destinationBookName = legalEntityAttributes.stream()
                    .filter(att -> att.getAttributeType().equalsIgnoreCase("DestinationBook"))
                    .map(LegalEntityAttribute::getAttributeValue).findFirst().orElse("");
            return !destinationBookName.equalsIgnoreCase(position.getBook().getName());
        }
        return false;
    }
}
