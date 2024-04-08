/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader.margincall;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallAllocationFacade;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SantMarginCallAllocationBuilder {

    private final List<SantMarginCallAllocationEntry> santAllocations = new ArrayList<SantMarginCallAllocationEntry>();

    public List<SantMarginCallAllocationEntry> getSantAllocations() {
        return this.santAllocations;
    }

    /**
     * When buildDummy is true and the entry does not have any allocation - then a dummy allocation is created to
     * represent the contract - Used for SantKPIDailyTask and SantDailyTask
     *
     * @param entries
     * @param contractsMap
     * @param buildDummy
     * @throws Exception
     */
    public void build(final List<MarginCallEntryDTO> entries, final Map<Integer, CollateralConfig> contractsMap,
                      ReportTemplate template, boolean buildDummy) throws Exception {
        this.santAllocations.clear();
        if (!buildDummy) {
            build(entries, contractsMap, template);
        } else {
            buildDummy(entries, contractsMap, template);
        }

    }

    private void buildDummy(final List<MarginCallEntryDTO> entries, final Map<Integer, CollateralConfig> contractsMap,
                            ReportTemplate template) throws Exception {
        List<SantMarginCallAllocationEntry> santLocalAllocations = new ArrayList<SantMarginCallAllocationEntry>();
        for (final MarginCallEntryDTO entry : entries) {
            santLocalAllocations.clear();
            santLocalAllocations.addAll(buildAllocations(entry.getCashAllocations(), entry, contractsMap, template));
            santLocalAllocations
                    .addAll(buildAllocations(entry.getSecurityAllocations(), entry, contractsMap, template));
            if (santLocalAllocations.size() == 0) {
                // Create Dummy
                CollateralConfig mcc = filterMarginCallContract(contractsMap.get(entry.getCollateralConfigId()),
                        template);
                if (mcc == null) {
                    continue;
                }
                santLocalAllocations.add(new SantMarginCallAllocationEntry(null, new SantMarginCallEntry(entry), mcc));
            }
            this.santAllocations.addAll(santLocalAllocations);
        }

    }

    public void build(final List<MarginCallEntryDTO> entries, final Map<Integer, CollateralConfig> contractsMap,
                      ReportTemplate template) throws Exception {
        for (final MarginCallEntryDTO entry : entries) {
            this.santAllocations.addAll(buildAllocations(entry.getCashAllocations(), entry, contractsMap, template));
            this.santAllocations
                    .addAll(buildAllocations(entry.getSecurityAllocations(), entry, contractsMap, template));
        }

    }

    private List<SantMarginCallAllocationEntry> buildAllocations(
            final List<? extends MarginCallAllocationFacade> allocations, final MarginCallEntryDTO entry,
            Map<Integer, CollateralConfig> contractsMap, ReportTemplate template) throws Exception {
        final SantMarginCallEntry santEntry = new SantMarginCallEntry(entry);

        List<SantMarginCallAllocationEntry> santLocalAllocations = new ArrayList<>();

        for (final MarginCallAllocationFacade allocation : allocations) {

            Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(allocation.getTradeId());

            if ((trade == null) || (trade.getStatus().getStatus().equals(Status.CANCELED))) {
                continue;
            }
            CollateralConfig mcc = filterMarginCallContract(contractsMap.get(entry.getCollateralConfigId()), template);
            if (mcc == null) {
                continue;
            }

            final SantMarginCallAllocationEntry santAlloc = new SantMarginCallAllocationEntry(allocation, santEntry,
                    contractsMap.get(entry.getCollateralConfigId()));
            santAlloc.setTrade(trade);
            santLocalAllocations.add(santAlloc);
        }

        return santLocalAllocations;
    }

    private CollateralConfig filterMarginCallContract(CollateralConfig mcc, ReportTemplate template) {
        String baseCurrency = (String) template.get(SantGenericTradeReportTemplate.BASE_CURRENCY);
        String economicSector = (String) template.get(SantGenericTradeReportTemplate.ECONOMIC_SECTOR);
        String headCloneIndicator = (String) template.get(SantGenericTradeReportTemplate.HEAD_CLONE_INDICATOR);

        if (!Util.isEmpty(baseCurrency) && !baseCurrency.equals(mcc.getCurrency())) {
            return null;
        }
        if (!Util.isEmpty(economicSector)
                && !economicSector.equals(mcc
                .getAdditionalField(CollateralStaticAttributes.MCC_ADD_FIELD_ECONOMIC_SECTOR))) {
            return null;
        }
        if (!Util.isEmpty(headCloneIndicator)
                && !headCloneIndicator.equals(mcc
                .getAdditionalField(CollateralStaticAttributes.MCC_ADD_FIELD_HEAD_CLONE))) {
            return null;
        }
        return mcc;
    }

}
