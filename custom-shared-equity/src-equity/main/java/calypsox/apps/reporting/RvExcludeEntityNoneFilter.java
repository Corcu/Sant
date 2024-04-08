/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting;

import java.util.Vector;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Log;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;


@SuppressWarnings("deprecation")
public class RvExcludeEntityNoneFilter implements BOPositionFilter {
    private static final String RV_FILTER_AGENT = "RVFilterAgent";
    private static final String RV_FILTER_BOOK = "RVFilterBook";

    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {
        Vector<String> excludeAgent = LocalCache.getDomainValues(DSConnection.getDefault(), RV_FILTER_AGENT);
        if (excludeAgent == null) {
        	excludeAgent = new Vector<String>();
        } 
        
        Vector<String> excludeBookList = LocalCache.getDomainValues(DSConnection.getDefault(), RV_FILTER_BOOK);
        if (excludeBookList == null) {
        	excludeBookList = new Vector<String>();
        }
        
        InventorySecurityPositionArray positionList = new InventorySecurityPositionArray();
        for (int i = 0; i < positions.size(); i++) {
            InventorySecurityPosition pos = positions.get(i);
            if (!isAgentFiltered(pos, excludeAgent) && !isBookFiltered(pos, excludeBookList)) {
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
        if ((pos.getProduct() instanceof Equity) && ((pos.getAgent() == null) || (pos.getAgent() != null && excludeAgentList.contains(pos.getAgent().getCode())))) {
            return true;
        }
        return false;
    }

    private boolean isBookFiltered(InventorySecurityPosition pos, Vector<String> excludeBookList) {
        if ((pos.getProduct() instanceof Equity) && ((pos.getBook() == null) || (pos.getBook() != null && excludeBookList.contains(pos.getBook().getAuthName())))) {
            return true;
        }
        return false;
    }
}