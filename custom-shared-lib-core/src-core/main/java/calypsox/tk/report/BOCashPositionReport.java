/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import java.util.List;
import java.util.Vector;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.util.InventoryPositionArray;

public class BOCashPositionReport extends com.calypso.tk.report.BOCashPositionReport {

    private static final long serialVersionUID = -5551092099143279891L;

    @Override
    protected InventoryPositionArray<?> load(String where, String from, @SuppressWarnings("rawtypes") Vector errorMsgs, List<CalypsoBindVariable> bindVariables) {
        // We need to refresh PE every time
        if (getPricingEnv() != null) {
            PricingEnv relloadedPE = AppUtil.loadPE(getPricingEnv().getName(), getValuationDatetime());
            setPricingEnv(relloadedPE);
        }

        return super.load(where, from, errorMsgs, null);
    }

}
