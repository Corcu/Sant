package calypsox.apps.reporting;

import calypsox.tk.report.BODisponibleSecurityPositionReportTemplate;
import calypsox.tk.report.TransferReportTemplate;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.reporting.ReportUtil;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.gui.ReportPanelDefinition;
import com.calypso.tk.report.gui.ReportWindowDefinition;
import com.calypso.tk.service.DSConnection;
import com.jidesoft.dialog.AbstractPage;
import com.jidesoft.docking.DockableFrame;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author acd
 */
public class BODisponibleSecurityPositionReportWindowHandler extends BOPositionReportWindowHandler {
    public ReportPanel transferPanel;
    static final String TRANSFER_FRAME_NAME = "Real Position";
    static final String TRANSFER_DEFAULT_TEMPLATE = "BOPosition Not Settled Movements";


    /**
     * Add a new report definition whit TransferReport
     *
     * @param reportType
     * @return
     */
    @Override
    public ReportWindowDefinition defaultReportWindowDefinition(String reportType) {
        ReportWindowDefinition winDef = super.defaultReportWindowDefinition(reportType);
        winDef.setUseBookHierarchy(false);
        winDef.setUsePricingEnv(true);
        winDef.addPanelDefinition(new ReportPanelDefinition(winDef, "BODisponibleSecurityPosition", "BODisponibleSecurityPosition"));
        winDef.addPanelDefinition(new ReportPanelDefinition(winDef, "Transfer", TRANSFER_FRAME_NAME));
        //Set Template
        ReportUtil.save(winDef);
        return winDef;
    }

    /**
     * Call before report loaded to merge the two templates
     *
     * @param panel
     */
    @Override
    public void callBeforeLoad(ReportPanel panel) {
        super.callBeforeLoad(panel);
        int numberOfTemplatePanels = null!=panel ? panel.getReportWindow().getNumberOfTemplatePanels() : 0;
        if(numberOfTemplatePanels>1){
            ReportTemplate boPositionTemplate = null;
            ReportTemplate transferTemplate = null;
            int transferTemplateIndex = 1;

            for(int i = 0;i<numberOfTemplatePanels;i++){
                ReportTemplate template = panel.getReportWindow().getReportPanel(i).getTemplate();
                if(template instanceof BODisponibleSecurityPositionReportTemplate){
                    boPositionTemplate = template;
                }
                if(template instanceof TransferReportTemplate){
                    transferTemplateIndex = i;
                    if(template.getTemplateName() != null){
                        transferTemplate = template;
                    }else {
                        transferTemplate = getTransferReportTemplate();
                    }
                }
            }

            if(null!=boPositionTemplate && null!=transferTemplate){
                Attributes positionAttributes = boPositionTemplate.getAttributes();
                Attributes attributes = transferTemplate.getAttributes();

                attributes.add("StartDate","");
                attributes.add("StartTenor","");
                attributes.add("StartPlus","");
                attributes.add("EndDate",positionAttributes.get("EndDate"));
                attributes.add("EndPlus",positionAttributes.get("EndPlus"));
                attributes.add("EndTenor",positionAttributes.get("EndTenor"));
                attributes.add("Book",positionAttributes.get("INV_POS_BOOKLIST"));
                attributes.add("PoAgent",positionAttributes.get("AGENT_ID"));
                attributes.add("SecCode",positionAttributes.get("SEC_CODE"));
                attributes.add("SecCodeValue",positionAttributes.get("SEC_CODE_VALUE"));
                Optional.ofNullable(positionAttributes.get("ACCOUNT_ID")).ifPresent(obj -> {
                    String glAccountsNames = getGlAccountsNames(String.valueOf(obj));
                    attributes.add("GLAccount",glAccountsNames);
                });
                transferTemplate.setAttributes(attributes);
                panel.getReportWindow().getReportPanel(transferTemplateIndex).setTemplate(transferTemplate);
                if(null!=transferPanel){
                    transferPanel.setTemplate(transferTemplate);
                }
            }
        }
    }

    /**
     * Remove TrasnferReport panel and add again just as an extra frame
     *
     * @param reportWindow
     */
    @Override
    public void customizeReportWindow(ReportWindow reportWindow) {
        super.customizeReportWindow(reportWindow);

        modifiedPositionValueOptions(reportWindow);

        if(reportWindow.getDockingManager().getFrame(TRANSFER_FRAME_NAME)!=null){
            reportWindow.getDockingManager().getFrame(TRANSFER_FRAME_NAME).setInitSide(2);
            reportWindow.getDockingManager().getFrame(TRANSFER_FRAME_NAME).setInitMode(4);
        }

        if(reportWindow.getNumberOfTemplatePanels()>1){
            reportWindow.getDockingManager().removeFrame(TRANSFER_FRAME_NAME, true);
            ReportTemplate transferReportTemplate = getTransferReportTemplate();
            if(null!=transferReportTemplate){
                reportWindow.getReportPanel(1).setTemplate(transferReportTemplate);
                reportWindow.addExtraFrame(getTransferFrame(reportWindow));
            }
        }
    }

    /**
     * Add Nominal (Unit Swift Format) to position value ComboBox.
     *
     * @param reportWindow
     */
    private void modifiedPositionValueOptions(ReportWindow reportWindow){
        if (reportWindow != null) {
            List<String> positionValueOptions = Arrays.asList("Quantity", "Nominal", "Nominal (Unfactored)", "Nominal (Unit Swift Format)");
            Optional<ReportPanel> reportPanelOpt = Optional.ofNullable(reportWindow.getReportPanel());
            Component[] components = reportPanelOpt.map(ReportPanel::getTemplatePanel)
                    .map(ReportTemplatePanel::getComponents)
                    .orElse(new Component[0]);

            Arrays.stream(components)
                    .filter(CalypsoComboBox.class::isInstance)
                    .map(CalypsoComboBox.class::cast)
                    .filter(comboBox -> positionValueOptions.contains(comboBox.getSelectedItem()))
                    .findFirst()
                    .ifPresent(calypsoComboBox -> AppUtil.set(calypsoComboBox, positionValueOptions));
        }
    }


    /**
     * Init new transfer report frame on report
     *
     * @param reportWindow
     * @return
     */
    private DockableFrame getTransferFrame(final ReportWindow reportWindow) {
        DockableFrame frame = new DockableFrame(TRANSFER_FRAME_NAME);
        frame.getContentPane().add((Component)new AbstractPage() {
            public void lazyInitialize() {
                setLayout(new BorderLayout(0, 0));
                transferPanel = reportWindow.getReportPanel(1);
                add(transferPanel);
            }
        });
        frame.getContext().setInitMode(2);
        frame.getContext().setInitSide(4);
        frame.getContext().setAutohidable(true);
        frame.getContext().setFloatable(true);
        frame.getContext().setDockable(true);
        frame.getContext().setHidable(true);

        return frame;
    }

    private ReportTemplate getTransferReportTemplate(){
        try {
            return DSConnection.getDefault().getRemoteReferenceData().getReportTemplate(ReportTemplate.getReportName("Transfer"), TRANSFER_DEFAULT_TEMPLATE);
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(),"Error: " + e);
        }
        return null;
    }

    private String getGlAccountsNames(String accounts) {
        try {
            List<Integer> accountIdList = Arrays.stream(accounts.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            if (!accountIdList.isEmpty()) {
                Collection<Account> accountsFromIds = BOCache.getAccountsFromIds(DSConnection.getDefault(), accountIdList, false);
                return accountsFromIds.stream().map(Account::getName).collect(Collectors.joining(","));
            }
        } catch (NumberFormatException e) {
            Log.error(this.getClass().getSimpleName(), "Error: " + e.getMessage());
        }

        return "";
    }

}
