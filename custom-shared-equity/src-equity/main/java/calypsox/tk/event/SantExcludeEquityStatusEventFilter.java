/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.event;
import java.util.Vector;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;


public class SantExcludeEquityStatusEventFilter implements EventFilter {


    public static final String DOMAIN_EXCLUDE_EQUITY_STATUS = "ExcludeEquityStatusLiquidationEngine";


    @Override
    public boolean accept(final PSEvent event) {
        Vector<String> equityFilterStatus = LocalCache
                .getDomainValues(DSConnection.getDefault(), DOMAIN_EXCLUDE_EQUITY_STATUS);

        if (event instanceof PSEventTrade) {
            final PSEventTrade tradeEvent = (PSEventTrade) event;
            if ("Equity".equalsIgnoreCase(tradeEvent.getTrade().getProductType())
                    && equityFilterStatus.contains(tradeEvent.getTrade().getStatus().getStatus())) {
                return false;
            }
        }

        return true;
    }


}
