/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import calypsox.tk.report.SantTradeBrowserItem;
import calypsox.tk.report.SantTradeBrowserReport;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

import static calypsox.tk.core.CollateralStaticAttributes.MC_CONTRACT_NUMBER;

public class SantTradesLoader {

    @SuppressWarnings("rawtypes")
    public Collection<SantTradeBrowserItem> getTradeBrowserItems(final String type, final String subType,
                                                                 final StringBuffer whereTemplate, final Vector<String> fromTemplate) {

        Collection<SantTradeBrowserItem> tradeBrowserItems = null;
        try {
            tradeBrowserItems = getTradeBrowserItems(type, subType, whereTemplate, fromTemplate, new Vector());

        } catch (final RemoteException e) {
            Log.error(SantTradeBrowserReport.class, "Error loading MTM Audit Items", e);
        }

        return tradeBrowserItems;
    }

    @SuppressWarnings("rawtypes")
    private Collection<SantTradeBrowserItem> getTradeBrowserItems(final String type, final String subType,
                                                                  final StringBuffer whereTemplate, final Vector<String> fromTemplate, final Vector errormsgs)
            throws RemoteException {

        final String where = buildWhere(type, subType, whereTemplate);
        final String from = buildFrom(fromTemplate);

        return loadTradeBrowserItems(from, where);

    }

    private String buildWhere(final String type, final String subType, final StringBuffer whereTemplate) {
        final StringBuffer where = new StringBuffer("");

        if (!"".equals(type)) {
            where.append(" trade.product_id=product_desc.product_id AND product_desc.product_type = '" + type + "'"
                    + whereTemplate);

            if (!"".equals(subType)) {
                where.append(" AND product_desc.product_sub_type = '" + subType + "'" + whereTemplate);
            }
        } else {
            if (!"".equals(subType)) {
                where.append(" trade.product_id=product_desc.product_id AND product_desc.product_sub_type = '"
                        + subType + "'" + whereTemplate);
            }
        }

        return where.toString();
    }

    private String buildFrom(final Vector<String> fromTemplate) {
        final StringBuffer from = new StringBuffer();

        from.append(" trade");
        from.append(", product_desc, ");

        for (final String fromTable : fromTemplate) {
            from.append(fromTable).append(",");
        }

        return from.toString().substring(0, from.length() - 1);
    }

    private Collection<SantTradeBrowserItem> loadTradeBrowserItems(final String from, final String where)
            throws RemoteException {

        final HashMap<Long, SantTradeBrowserItem> tradeItemsMap = new HashMap<>();

        final HashMap<Integer, CollateralConfig> finalContractsMap = new HashMap<>();

        final TradeArray trades = DSConnection.getDefault().getRemoteTrade().getTrades(from, where, "trade.trade_id", null);

        if (!Util.isEmpty(trades)) {
            for (int i = 0; i < trades.size(); i++) {
                final SantTradeBrowserItem tradeItem = new SantTradeBrowserItem();
                final Trade trade = trades.get(i);
                tradeItem.setTrade(trade);

                final int contractId = trade.getKeywordAsInt(MC_CONTRACT_NUMBER);
                if (contractId != 0) {
                    final CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(
                            DSConnection.getDefault(), contractId);
                    finalContractsMap.put(contractId, marginCallConfig);
                    tradeItem.setMarginCall(marginCallConfig);
                }
                tradeItemsMap.put(trade.getLongId(), tradeItem);
            }
        }

        return tradeItemsMap.values();
    }

    @SuppressWarnings("unused")
    private boolean checkIsMrgCallCriteriaSpecified(final String valAgent, final Vector<String> agrOwnerIds,
                                                    final String agrType) {
        boolean isMrgCallCriteriaSpecified = false;
        if (!Util.isEmpty(valAgent) || !Util.isEmpty(agrOwnerIds)) {
            isMrgCallCriteriaSpecified = true;
        }
        return isMrgCallCriteriaSpecified;
    }

    public CollateralConfig getMarginCallConfig(final long id) {
        CollateralConfig marginCall = null;
        final List<String> from = new ArrayList<>();
        from.add("margin_call_entries");
        from.add("margin_call_allocation");

        List<MarginCallEntryDTO> entries = null;
        try {
            String where = "margin_call_allocation.mc_entry_id = margin_call_entries.id"
                    + " AND margin_call_allocation.trade_id=" + id;
            entries = new SantMarginCallEntryLoader().loadMarginCallEntriesDTO(from, where, true);
        } catch (final CollateralServiceException exc) {
            final String message = "Couldn't find the entries of the trade id=" + id;
            Log.error(message, exc.getCause());
            Log.error(this, exc);//Sonar
        }

        if (!Util.isEmpty(entries) && entries.get(0) != null) {
            marginCall = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), entries.get(0)
                    .getCollateralConfigId());
        }

        return marginCall;
    }

}
