package calypsox.apps.reporting;

import calypsox.apps.reporting.util.ValueComparator;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.apps.reporting.util.control.SantStartEndDatePanel;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class AccountingNotificationReportTemplatePanel extends SantInterestNotificationReportTemplatePanel {
    SantProcessDatePanel processDatePanel;

    @Override
    protected void init() {
        buildControlsPanel();

        setSize(0, 150);
        final JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BorderLayout());
        masterPanel.setBorder(getMasterPanelBorder());
        add(masterPanel, BorderLayout.CENTER);

        masterPanel.add(this.processDatePanel, BorderLayout.NORTH);

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1, 2));
        masterPanel.add(mainPanel, BorderLayout.CENTER);

        final JPanel column1Panel = getColumn1Panel();
        final JPanel column2Panel = getColumn2Panel();

        mainPanel.add(column1Panel);
        mainPanel.add(column2Panel);

        add(this.processPaymentPanel, BorderLayout.EAST);
    }

    @Override
    protected void buildControlsPanel() {
        this.processDatePanel = new SantProcessDatePanel("Process");
        this.processStartEndDatePanel = new SantStartEndDatePanel("Old");
        this.rateIndexPanel = new SantChooseButtonPanel("Rate Index", this.rateIndexMap.values());
        this.agreementTypePanel = new SantChooseButtonPanel("Agr Type", "legalAgreementType");

        final ValueComparator bvc = new ValueComparator(this.marginCallContractIdsMap);
        final Map<Integer, String> sortedMap = new TreeMap<Integer, String>(bvc);
        sortedMap.putAll(this.marginCallContractIdsMap);
        this.agreementNamePanel = new SantChooseButtonPanel("Agr Name", sortedMap.values());

        this.callAccountPanel = new SantChooseButtonPanel("Call Account", this.callAccountNamesMap.values());

        this.ccyPanel = new SantChooseButtonPanel("Currency", LocalCache.getCurrencies());

        final SortedSet<String> sortedLegalEntities = new TreeSet<String>(
                BOCache.getLegalEntitieNamesForRole(DSConnection.getDefault(), LegalEntity.PROCESSINGORG));
        this.poAgrPanel = new SantChooseButtonPanel("Owner Agr", sortedLegalEntities);
        this.processPaymentPanel = new PaymentButtonPanel("Show Payments", this);

    }

    private JPanel getColumn1Panel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1));
        panel.add(this.agreementNamePanel);
        panel.add(this.agreementTypePanel);
        panel.add(this.poAgrPanel);

        return panel;
    }

    private JPanel getColumn2Panel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1));
        panel.add(this.callAccountPanel);
        panel.add(this.rateIndexPanel);
        panel.add(this.ccyPanel);
        return panel;
    }

    @Override
    public void setTemplate(ReportTemplate template) {
        super.setTemplate(template);
        this.processDatePanel.setTemplate(template);
        this.processDatePanel.write(template);
    }

    @Override
    public ReportTemplate getTemplate() {
        ReportTemplate template = super.getTemplate();
        this.processDatePanel.read(template);
        return template;
    }
}
