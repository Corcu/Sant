package calypsox.apps.reporting;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.util.InventorySecurityPositionArray;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * This filter exclude books defined on the following domain values (BODisponibleExcludeDestinationBooks and BODisponibleExcludeBooks)
 */
public class BODisponibleDestinationBooksFilter extends BODisponibleBooksFilter{
    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {
        super.filterSecurity(positions);
        applyDestinationBookFilter(positions);
        return positions;
    }

    private void applyDestinationBookFilter(InventorySecurityPositionArray positions){
        if(!Util.isEmpty(positions.getInventorySecurityPositions())){
            List<InventorySecurityPosition> filteredPositionList = Arrays.stream(positions.getInventorySecurityPositions())
                    .filter(Objects::nonNull)
                    .filter(this::checkDestinationBook)
                    .collect(Collectors.toList());
            positions.clear();
            positions.addAll(new InventorySecurityPositionArray(filteredPositionList));
        }
    }
    private boolean checkDestinationBook(InventorySecurityPosition position){
        if(position.getBook()!=null && position.getBook().getLegalEntity()!=null){
            Vector<LegalEntityAttribute> legalEntityAttributes = (Vector<LegalEntityAttribute>) position.getBook().getLegalEntity().getLegalEntityAttributes();
            String destinationBookName = legalEntityAttributes.stream()
                    .filter(att -> att.getAttributeType().equalsIgnoreCase("DestinationBook"))
                    .map(LegalEntityAttribute::getAttributeValue).findFirst().orElse("");
            return !destinationBookName.equalsIgnoreCase(position.getBook().getName());
        }
        return true;
    }

}
