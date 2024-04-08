package calypsox.apps.collateral.manager.controller;

import calypsox.tk.collateral.manager.worker.impl.RePriceTaskWorker;
import com.calypso.apps.collateral.manager.MarginCallDesktop;
import com.calypso.apps.collateral.manager.controller.BaseMarginCallDesktopController;
import com.calypso.apps.collateral.manager.controller.MarginCallDesktopController;
import com.calypso.tk.collateral.BilateralEntry;
import com.calypso.tk.collateral.ExposureGroupEntry;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.MarginCallEntryUtil;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.MarginCallReportTemplate;

import javax.swing.*;
import java.util.*;

public class CustomMarginCallDesktopController extends MarginCallDesktopController {
    private MarginCallViewerController viewerController = null;

    public CustomMarginCallDesktopController(MarginCallDesktop window) {
        super(window);
    }

    @Override
    public MarginCallViewerController getViewerController() {
        if (this.viewerController == null) {

            viewerController = new MarginCallViewerController(this);
        }
        return viewerController;
    }

    @Override
    protected void priceSelected() {
        List<MarginCallEntry> selectedEntries = this.getSelectedMarginCallEntries();
        if (!Util.isEmpty(selectedEntries)) {
            this.applyActionOnSelected("Price Selected", selectedEntries);
        } else {
            this.priceAll();
        }

        this.getDesktop().getDockingManager().showFrame("DOCK_KEY_MARGIN_CALL_ENTRY_VIEW_V1.0");
    }

    @Override
    protected void priceAll() {
        String action = "Price";
        MarginCallConfigFilter filter = null;
        if (this.executionContext != null) {
            filter = this.executionContext.getFilter();
        }

        if (this.checkCollateralContext()) {
            List<MarginCallEntry> entries = this.getAllMarginCallEntries();
            if (!Util.isEmpty(entries) && filter != null && filter.equals(this.executionContext.getFilter())) {
                this.applyActionOnSelected("Price Selected", entries);
            } else {
                entries = new ArrayList<>();
                this.clear();
                CollateralTaskWorker worker = new RePriceTaskWorker(this.executionContext,entries);
                boolean indeterminate = entries.size() == 1;
                this.startDialog(worker, indeterminate);
                List<MarginCallEntry> entrieToDisplay = this.expandEntryToDisplay(entries);
                this.getDesktop().getMarginCallEntryEventView().registerBaseEntries(this.getExecutionContext().getProcessDate(), entrieToDisplay);
                this.display(entrieToDisplay);
                this.getDesktop().getDockingManager().showFrame("DOCK_KEY_MARGIN_CALL_ENTRY_VIEW_V1.0");
            }
        }

    }

    @Override
    protected void applyActionOnSelected(String action, List<MarginCallEntry> selectedEntries) {
        MarginCallReportTemplate template = this.getReportTemplate();
        List<MarginCallEntry> entries = template.getEntries();
        CollateralTaskWorker task;
        if(action.equals("Price Selected")||action.equals("RePrice")) {
            task = new RePriceTaskWorker(this.executionContext, selectedEntries);
        }else{
             task = CollateralTaskWorker.getInstance(action, this.getExecutionContext(), selectedEntries);
        }

        boolean indeterminate = entries.size() == 1;
        this.startDialog(task, indeterminate);
        this.getDesktop().getMarginCallEntryEventView().refreshEntryEvents(selectedEntries);
        this.setNeedRefresh(true);// 987
        SwingUtilities.invokeLater(new BaseMarginCallDesktopController.RefreshWorker(selectedEntries));
    }

    private List<MarginCallEntry> expandEntryToDisplay(List<MarginCallEntry> entries) {
        List<MarginCallEntry> result = new ArrayList<>(entries.size());
        Set<Integer> nonIdependentConfigIds = new HashSet<>();
        Iterator<MarginCallEntry> entryIterator = entries.iterator();

        MarginCallEntry entry;
        while(entryIterator.hasNext()) {
            entry = entryIterator.next();
            if (MarginCallEntryUtil.isBilateralEntry(entry) && !entry.getCollateralConfig().isIndependentWorkflow()) {
                nonIdependentConfigIds.add(entry.getCollateralConfigId());
                result.addAll(((BilateralEntry)entry).getExposureGroupEntries());
            }
        }
        entryIterator = entries.iterator();
        while(true) {
            do {
                if (!entryIterator.hasNext()) {
                    return result;
                }
                entry = entryIterator.next();
            } while(MarginCallEntryUtil.isExposureGroupEntry(entry) && nonIdependentConfigIds.contains(((ExposureGroupEntry)entry).getMasterConfigId()));

            result.add(entry);
        }
    }
}
