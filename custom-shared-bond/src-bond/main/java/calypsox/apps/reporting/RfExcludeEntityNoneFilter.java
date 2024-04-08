package calypsox.apps.reporting;


import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;
import java.util.Vector;


@SuppressWarnings("deprecation")
public class RfExcludeEntityNoneFilter implements BOPositionFilter {


    private static final String RF_FILTER_AGENT = "RVFilterAgent";
    private static final String RF_FILTER_BOOK = "RVFilterBook";


    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {
        Vector<String> excludeAgent = LocalCache.getDomainValues(DSConnection.getDefault(), RF_FILTER_AGENT);
        if (excludeAgent == null) {
            excludeAgent = new Vector<String>();
        }

        Vector<String> excludeBookList = LocalCache.getDomainValues(DSConnection.getDefault(), RF_FILTER_BOOK);
        if (excludeBookList == null) {
            excludeBookList = new Vector<String>();
        }

        InventorySecurityPositionArray positionList = new InventorySecurityPositionArray();
        for (int i = 0; i < positions.size(); i++) {
            InventorySecurityPosition pos = positions.get(i);
            if (!isAgentFiltered(pos, excludeAgent) && !isBookFiltered(pos, excludeBookList) && isBond(pos)) {
                positionList.add(pos);
            }
            else {
                StringBuilder strBld = new StringBuilder();
                strBld.append("Position is filtered: ");
                strBld.append(pos.toString());
                strBld.append(" - Agent = ");
                if (pos.getAgent() != null) {
                    strBld.append(pos.getAgent().getCode());
                }
                else {
                    strBld.append("NULL");
                }
                strBld.append(", Book = ");
                if (pos.getBook() != null) {
                    strBld.append(pos.getBook().getAuthName());
                }
                else {
                    strBld.append("NULL");
                }
                Log.info(this, strBld.toString());
            }
        }
        return positionList;
    }


    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
        return positions;
    }


    private boolean isAgentFiltered(InventorySecurityPosition pos, Vector<String> excludeAgentList) {
        if ((pos.getAgent() == null) || (pos.getAgent() != null && excludeAgentList.contains(pos.getAgent().getCode()))) {
            return true;
        }
        return false;
    }


    private boolean isBookFiltered(InventorySecurityPosition pos, Vector<String> excludeBookList) {
        if ((pos.getBook() == null) || (pos.getBook() != null && excludeBookList.contains(pos.getBook().getAuthName()))) {
            return true;
        }
        return false;
    }


    private boolean isBond(InventorySecurityPosition pos){
        String prodType = pos.getProduct().getType();
        if((pos.getProduct() instanceof Bond) || (!Util.isEmpty(prodType) && ("Bond".equalsIgnoreCase(prodType) || "G.Bonds".equalsIgnoreCase(prodType)))) {
            return true;
        }
        else{
            return false;
        }
    }


}
