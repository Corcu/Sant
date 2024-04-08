/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.CollateralCacheUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

import java.util.Vector;

@SuppressWarnings("deprecation")
public class RvExcludeBookFilter implements BOPositionFilter {

    public static final String RV_FILTER_BOOK = "RVFilterBook";

    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {
        Vector<String> excludeBookList = LocalCache.getDomainValues(DSConnection.getDefault(), RV_FILTER_BOOK);
        if (Util.isEmpty(excludeBookList)) {
        	return positions;
        }
        
        InventorySecurityPositionArray positionList = new InventorySecurityPositionArray();
        for (int i = 0; i < positions.size(); i++) {
            InventorySecurityPosition pos = positions.get(i);
            if (!isBookFiltered(pos, excludeBookList)) {
                positionList.add(pos);
            }
        }
        return positionList;
    }


    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
        return positions;
    }


    private boolean isBookFiltered(InventorySecurityPosition pos, Vector<String> excludeBookList) {
        if ((pos.getProduct() instanceof Equity) && ((pos.getBook() == null) || (pos.getBook() != null && excludeBookList.contains(pos.getBook().getAuthName())))) {
            return true;
        }
        return false;
    }


}