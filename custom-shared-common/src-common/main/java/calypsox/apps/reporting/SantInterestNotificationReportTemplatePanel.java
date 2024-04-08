/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting;

import calypsox.apps.reporting.util.ValueComparator;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantStartEndDatePanel;
import calypsox.apps.reporting.util.loader.MarginCallConfigLightLoader;
import calypsox.tk.core.MarginCallConfigLight;
import calypsox.tk.report.SantInterestNotificationReportTemplate;
import calypsox.tk.report.loader.CallAccountLoader;
import calypsox.tk.report.loader.IndexLoader;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.FontChooser;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEventDomainChange;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.ReportTemplateName;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.*;
import java.util.Map.Entry;

public class SantInterestNotificationReportTemplatePanel extends ReportTemplatePanel implements ActionListener {

    private static final long serialVersionUID = 4763089345432328644L;
    // for Report template
    public static final String CALL_ACCOUNT_ID = "CallAccountIds";
    public static final String AGREEMENT_ID = "AGREEMENT_ID";
    public static final String AGREEMENT_TYPE = "AGREEMENT_TYPE";
    public static final String RATE_INDEX = "RATE_INDEX";
    public static final String ACCOUNT_MAP = "AccountMap";
    public static final String CONTRACT_MAP = "ContractMap";
    public static final String CURRENCY = "Currency";
    public static final String ROW_DATA = "AccountRowData";
    public static final String PROCESSING_ORG_NAMES = "ProcessingOrg";

    protected Map<RateIndex, String> rateIndexMap;

    protected Map<Integer, String> marginCallContractIdsMap;

    protected Map<Integer, Account> callAccountsMap;

    protected Map<Integer, MarginCallConfigLight> mccLightsMap;

    protected Map<Integer, String> callAccountNamesMap;

    protected SantChooseButtonPanel agreementTypePanel;

    protected SantStartEndDatePanel processStartEndDatePanel;

    protected SantChooseButtonPanel rateIndexPanel;

    protected SantChooseButtonPanel agreementNamePanel;

    protected SantChooseButtonPanel callAccountPanel;

    protected SantChooseButtonPanel poAgrPanel;

    protected PaymentButtonPanel processPaymentPanel;

    protected SantChooseButtonPanel ccyPanel;

    private ReportTemplate reportTemplate;

    public SantInterestNotificationReportTemplatePanel() {
        loadstaticData();
        init();
    }

    private void loadstaticData() {

        this.rateIndexMap = new IndexLoader().load();

        final MarginCallConfigLightLoader mccLightLoader = new MarginCallConfigLightLoader();
        this.marginCallContractIdsMap = mccLightLoader.load();

        final CallAccountLoader accountLoader = new CallAccountLoader();
        this.callAccountNamesMap = accountLoader.load();

        this.callAccountsMap = accountLoader.get();
        this.mccLightsMap = mccLightLoader.get();
    }

    protected void init() {
        buildControlsPanel();

        setSize(0, 150);
        final JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BorderLayout());
        masterPanel.setBorder(getMasterPanelBorder());
        add(masterPanel, BorderLayout.CENTER);

        masterPanel.add(this.processStartEndDatePanel, BorderLayout.NORTH);

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1, 2));
        masterPanel.add(mainPanel, BorderLayout.CENTER);

        final JPanel column1Panel = getColumn1Panel();
        final JPanel column2Panel = getColumn2Panel();

        mainPanel.add(column1Panel);
        mainPanel.add(column2Panel);

        add(this.processPaymentPanel, BorderLayout.EAST);
    }

    protected void buildControlsPanel() {
        this.processStartEndDatePanel = new SantStartEndDatePanel("Process");
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


    protected Border getMasterPanelBorder() {
        //GSM 01/03/16 - Not working title font in java 6, modified to java 7
        TitledBorder border = BorderFactory.createTitledBorder("Interest Notification");//new TitledBorder(BorderFactory.createEtchedBorder(), "");
        border.setTitleFont(FontChooser.F_B);
        border.setTitleColor(Color.BLUE);
        return border;
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
    public void setTemplate(final ReportTemplate template) {

        // GSM 24/02/2016 - check maps have been loaded
        if ((this.rateIndexMap == null) || (this.marginCallContractIdsMap == null)
                || (this.callAccountNamesMap == null) || (this.mccLightsMap == null)) {
            loadstaticData();
        }
        this.reportTemplate = template;
        this.processStartEndDatePanel.setTemplate(template);
        this.processStartEndDatePanel.write(template);

        this.poAgrPanel.setValue(this.reportTemplate, SantInterestNotificationReportTemplate.OWNER_AGR);

        this.agreementNamePanel.setValue(this.reportTemplate, SantInterestNotificationReportTemplate.AGREEMENT_ID,
                this.marginCallContractIdsMap);

        this.agreementTypePanel.setValue(this.reportTemplate, SantInterestNotificationReportTemplate.AGREEMENT_TYPE);

        this.rateIndexPanel.setValue(this.reportTemplate, SantInterestNotificationReportTemplate.RATE_INDEX);

        this.callAccountPanel.setValue(this.reportTemplate, SantInterestNotificationReportTemplate.CALL_ACCOUNT_ID,
                this.callAccountNamesMap);

        this.ccyPanel.setValue(this.reportTemplate, SantInterestNotificationReportTemplate.CURRENCY);
    }

    @Override
    public ReportTemplate getTemplate() {
        this.processStartEndDatePanel.read(this.reportTemplate);

        String value = this.agreementNamePanel.getValue();
        this.reportTemplate.put(SantInterestNotificationReportTemplate.AGREEMENT_ID,
                getMultipleKey(value, this.marginCallContractIdsMap));

        value = this.callAccountPanel.getValue();
        this.reportTemplate.put(SantInterestNotificationReportTemplate.CALL_ACCOUNT_ID,
                getMultipleKey(value, this.callAccountNamesMap));

        this.reportTemplate.put(SantInterestNotificationReportTemplate.AGREEMENT_TYPE,
                this.agreementTypePanel.getValue());

        this.reportTemplate.put(SantInterestNotificationReportTemplate.OWNER_AGR, this.poAgrPanel.getValue());

        this.reportTemplate.put(SantInterestNotificationReportTemplate.RATE_INDEX, this.rateIndexPanel.getValue());

        this.reportTemplate.put(SantInterestNotificationReportTemplate.CURRENCY, this.ccyPanel.getValue());

        this.reportTemplate.put(SantInterestNotificationReportTemplate.ACCOUNT_MAP, this.callAccountsMap);
        this.reportTemplate.put(SantInterestNotificationReportTemplate.CONTRACT_MAP, this.mccLightsMap);

        return this.reportTemplate;
    }

    protected Object getMultipleKey(final String value, final Map<Integer, String> map) {
        final Vector<String> agreementNames = Util.string2Vector(value);
        final Vector<Integer> agreementIds = new Vector<Integer>();
        for (String agreementName : agreementNames) {
            agreementIds.add((Integer) getKey(agreementName, map));
        }
        return Util.collectionToString(agreementIds);
    }

    private Object getKey(final String value, final Map<Integer, String> map) {
        for (final Entry<Integer, String> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                return null;
            }
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!e.getActionCommand().equals("Show Payments")) {
            return;
        }
        ReportWindow reportWin = new ReportWindow("SantInterestPaymentRunner");
        AppUtil.setCalypsoIcon(reportWin);

        ReportTemplateName templateName = new ReportTemplateName("SantInterestPaymentRunnerReportTemplate");
        ReportTemplate template = BOCache.getReportTemplate(DSConnection.getDefault(), "SantInterestPaymentRunner",
                templateName);

        if (template == null) {
            reportWin.setVisible(true);
            return;
        }

        // Cache issue => template is deleted before being re-saved
        try {
            DSConnection.getDefault().getRemoteReferenceData().removeReportTemplate(template);
            PSEventDomainChange event = new PSEventDomainChange(PSEventDomainChange.REPORT_TEMPLATE,
                    PSEventDomainChange.REMOVE, template.getTemplateName(), template.getId());
            event.setInfo(template.getReportType());
            BOCache.newEvent(DSConnection.getDefault(), event);

        } catch (RemoteException error) {
            Log.error(this, "Cannot delete template SantInterestPaymentRunnerReportTemplate", error);
        }

        template.remove(AGREEMENT_ID);
        template.remove(CALL_ACCOUNT_ID);
        template.remove(AGREEMENT_TYPE);
        template.remove(PROCESSING_ORG_NAMES);
        template.remove(RATE_INDEX);
        template.remove(CURRENCY);
        template.remove(RATE_INDEX);
        template.remove(TradeReportTemplate.START_DATE);
        template.remove(TradeReportTemplate.START_PLUS);
        template.remove(TradeReportTemplate.START_TENOR);

        ReportTemplate newTemplate = null;
        try {
            newTemplate = (ReportTemplate) template.clone();
        } catch (CloneNotSupportedException error) {
            Log.error(this, "Cannot clone template SantInterestPaymentRunnerReportTemplate", error);
        }

        newTemplate.put(TradeReportTemplate.START_PLUS, "+");
        newTemplate.put(TradeReportTemplate.START_TENOR, "0D");

        String value = this.agreementNamePanel.getValue();
        newTemplate.put(AGREEMENT_ID,
                getMultipleKey(value, this.marginCallContractIdsMap));

        value = this.callAccountPanel.getValue();
        newTemplate.put(CALL_ACCOUNT_ID,
                getMultipleKey(value, this.callAccountNamesMap));

        newTemplate.put(AGREEMENT_TYPE, this.agreementTypePanel.getValue());
        newTemplate.put(PROCESSING_ORG_NAMES, this.poAgrPanel.getValue());
        newTemplate.put(RATE_INDEX, this.rateIndexPanel.getValue());
        newTemplate.put(CURRENCY, this.ccyPanel.getValue());

        try {
            newTemplate.setId(0);
            DSConnection.getDefault().getRemoteReferenceData().save(newTemplate);
        } catch (RemoteException error) {
            Log.error(this, "Cannot save template SantInterestPaymentRunnerReportTemplate", error);
        }

        reportWin.setVisible(true);
        reportWin.loadTemplate("SantInterestPaymentRunnerReportTemplate");

    }

    public class PaymentButtonPanel extends JButton {

        private static final long serialVersionUID = 6498512575607484715L;

        protected JButton button = new JButton();

        public PaymentButtonPanel(String name, ActionListener l) {
            this.button.addActionListener(l);
            init(name);

        }

        private void init(String name) {
            this.button.setText(name);
            this.button.setActionCommand(name);
            this.button.setFont(new Font(getFont().getName(), Font.BOLD, getFont().getSize()));
            setBackground(Color.BLUE);
            this.button.setForeground(Color.BLUE);
            Dimension dim = new Dimension(140, 50);
            this.button.setMinimumSize(dim);
            this.button.setPreferredSize(dim);
            this.button.setMaximumSize(dim);
            setMargin(new Insets(0, 0, 0, 0));
            add(this.button);
        }

    }
}
