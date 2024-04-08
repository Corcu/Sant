/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader.margincall;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteTrade;

import java.rmi.RemoteException;
import java.util.*;

public class SantMarginCallDetailEntryBuilder {

    private final List<SantMarginCallDetailEntry> santDetails = new ArrayList<SantMarginCallDetailEntry>();
    private Map<Integer, CollateralConfig> contractsMap;

    public List<SantMarginCallDetailEntry> getSantDetails() {
        return this.santDetails;
    }

    public void build(final List<MarginCallEntryDTO> entries, final Map<Integer, CollateralConfig> contractsMap,
                      final ReportTemplate template) {
        this.contractsMap = contractsMap;
        final Set<Integer> tradeIds = new HashSet<Integer>();

        // PO deal
        final String poDealStr = (String) template.get(SantGenericTradeReportTemplate.OWNER_DEALS);
        Vector<Integer> poDealIds = null;
        if (!Util.isEmpty(poDealStr)) {
            poDealIds = Util.string2IntVector(poDealStr);
        }

        for (final MarginCallEntryDTO entry : entries) {
            buildDetails(entry, tradeIds, poDealIds);
        }

    }

    private void buildDetails(final MarginCallEntryDTO entry, final Set<Integer> tradeIds, Vector<Integer> poDealIds) {
        // it sounds costly in terms of performance but it's not when trades are in the server cache
        RemoteTrade remoteTrade = DSConnection.getDefault().getRemoteTrade();

        SantMarginCallEntry santEntry = new SantMarginCallEntry(entry);
        CollateralConfig mcc = this.contractsMap.get(entry.getCollateralConfigId());
        for (MarginCallDetailEntryDTO detail : santEntry.getIncludedDetailEntries()) {
            SantMarginCallDetailEntry santDetail = new SantMarginCallDetailEntry(detail, santEntry, mcc);

            Trade trade = getTrade(detail.getTradeId(), remoteTrade, poDealIds);
            if (trade == null) {
                continue;
            }
            santDetail.setTrade(trade);
            this.santDetails.add(santDetail);
        }
    }

    private Trade getTrade(long tradeId, RemoteTrade remoteTrade, Vector<Integer> poDealIds) {
        try {
            Trade trade = remoteTrade.getTrade(tradeId);
            if (trade == null) {
                return null;
            }
            if (!Util.isEmpty(poDealIds) && !poDealIds.contains(trade.getBook().getProcessingOrgBasedId())) {
                return null;
            }
            return trade;
        } catch (RemoteException e) {
            Log.error(this, "Cannot load trade, id = " + tradeId, e);
            return null;
        }

    }
}
