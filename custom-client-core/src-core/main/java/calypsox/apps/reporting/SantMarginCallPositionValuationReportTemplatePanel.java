/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
/**
 *
 */
package calypsox.apps.reporting;

import calypsox.apps.reporting.util.ValueComparator;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantLegalEntityPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.loader.AgreementLoader;
import com.calypso.apps.reporting.MarginCallPositionValuationReportTemplatePanel;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import static calypsox.tk.report.SantMarginCallPositionValuationReportTemplate.OWNER_AGR;
import static calypsox.tk.report.SantMarginCallPositionValuationReportTemplate.SECURITIES;

/**
 * Panel SantMarginCallPositionValuationReport, for MMOO Conciliation (and other
 * MCs valuations)
 *
 * @author aela & Guillermo Solano * @date 27/03/2015
 * @version 2.0. Added custom filters: process date, contract, owner & ISIN
 *
 */
public class SantMarginCallPositionValuationReportTemplatePanel extends MarginCallPositionValuationReportTemplatePanel {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Process Date
     */
    private SantProcessDatePanel proccessDatePanel;

    /**
     * Agreement panel
     */
    protected SantChooseButtonPanel agreementNamePanel;

    /**
     * Owner Panel
     */
    protected SantLegalEntityPanel poAgrPanel;

    /**
     * Security panel (ISINs)
     */
    private SantChooseButtonPanel bondsPanel;

    /**
     * List of Isins
     */
    private Vector<String> bondsIsin;

    /**
     * Map of contracts by ids
     */
    private Map<Integer, String> marginCallContractIdsMap;

    /**
     * Constructors: builds the core panels Contract & Position and adds the
     * custom panels
     */
    public SantMarginCallPositionValuationReportTemplatePanel() {

        super();
        setPreferredSize(new Dimension(905, 200));
        this.setSize(new Dimension(905, 200));
        loadStaticData();
        buildControlsPanel();
    }

    /**
     * Builds the custom panels: Agreement & Process. Agreement: filters by
     * owner & contract name. Process: Process date y Security Isin
     */
    private void buildControlsPanel() {

        // agreement panel
        final JPanel agreementPanel = new JPanel();
        agreementPanel.setBorder(new TitledBorder(new EtchedBorder(1, null, null), "Agreement", 4, 2, null, null));
        agreementPanel.setBounds(895, 5, 300, 117);

        // po owner
        this.poAgrPanel = new SantLegalEntityPanel(LegalEntity.PROCESSINGORG, "Owner (Agr):", false, true, true, true);

        // contract selector
        ValueComparator bvc = new ValueComparator(this.marginCallContractIdsMap);
        Map<Integer, String> sortedMap = new TreeMap<Integer, String>(bvc);
        sortedMap.putAll(this.marginCallContractIdsMap);
        this.agreementNamePanel = new SantChooseButtonPanel("Agr Name:", sortedMap.values());

        // add agrement panel
        agreementPanel.add(this.poAgrPanel);
        agreementPanel.add(this.agreementNamePanel);
        super.add(agreementPanel);

        // process panel
        final JPanel processPanel = new JPanel();
        processPanel.setBorder(new TitledBorder(new EtchedBorder(1, null, null), "Process", 4, 2, null, null));
        processPanel.setBounds(5, 120, 700, 75);

        // process date
        this.proccessDatePanel = new SantProcessDatePanel("Process Date:");
        this.proccessDatePanel.removeDateLabel();
        this.proccessDatePanel.setPreferredSize(new Dimension(100, 24), new Dimension(215, 24));
        this.proccessDatePanel.customInitDomains("PROCESS_DATE", "PROCESS_PLUS", "PROCESS_TENOR");

        // securities
        Collections.sort(this.bondsIsin);
        this.bondsPanel = new SantChooseButtonPanel("Sec. ISINs:", this.bondsIsin);

        // add process panel
        processPanel.add(this.proccessDatePanel);
        processPanel.add(this.bondsPanel);
        super.add(processPanel);
    }

    /**
     * Recovers agreements & ISINs securities to show each list in the GUI
     */
    @SuppressWarnings("unchecked")
    protected void loadStaticData() {

        this.marginCallContractIdsMap = new AgreementLoader().load();
        try {
            this.bondsIsin = DSConnection.getDefault().getRemoteProduct().getSecCodeValues("ISIN", null, null);
        } catch (RemoteException e) {
            final StringBuffer message = new StringBuffer("Couldn't load securities");
            Log.error(message, e.getCause());
            Log.error(this, e); //sonar
        }
    }

    /**
     * @return report template
     */
    @Override
    public ReportTemplate getTemplate() {

        ReportTemplate reportTemplate = super.getTemplate();
        if (!Util.isEmpty(this.poAgrPanel.getLE())) {
            reportTemplate.put(OWNER_AGR, this.poAgrPanel.getLEIdsStr());
        } else {
            reportTemplate.remove(OWNER_AGR);
        }
        reportTemplate.put(SantGenericTradeReportTemplate.AGREEMENT_ID,
                getMultipleKey(this.agreementNamePanel.getValue(), this.marginCallContractIdsMap));

        this.proccessDatePanel.read(reportTemplate);
        reportTemplate.put(SECURITIES, this.bondsPanel.getValue());

        return reportTemplate;
    }

    /**
     * sets template
     *
     * @param report
     *            template
     */
    @Override
    public void setTemplate(final ReportTemplate template) {

        super.setTemplate(template);
        this.poAgrPanel.setValue(template, OWNER_AGR);
        this.agreementNamePanel.setValue(template, SantGenericTradeReportTemplate.AGREEMENT_ID,
                this.marginCallContractIdsMap);
        this.proccessDatePanel.setTemplate(template);
        this.proccessDatePanel.write(template);
        template.put(SECURITIES, template.get(SECURITIES));
    }

    /**
     * Forces to recover template before loading report
     */
    @Override
    public void callBeforeLoad(final ReportPanel panel) {
        ReportTemplate template = panel.getTemplate();
        template.callBeforeLoad();
    }

    protected Object getMultipleKey(final String value, final Map<Integer, String> map) {
        final Vector<String> agreementNames = Util.string2Vector(value);
        final Vector<Integer> agreementIds = new Vector<>();
        for (final String agreementName : agreementNames) {
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

    // Panel Testing
    // public static void main(final String... pepe) throws ConnectException {
    // final String args[] = { "-env", "dev4-local", "-user", "nav_it_sup_tec",
    // "-password", "calypso" };
    // ConnectionUtil.connect(args, "MMOO_Reconciliation");
    // final JFrame frame = new JFrame();
    // frame.setTitle("MMOO_Reconciliation");
    // frame.setContentPane(new
    // SantMarginCallPositionValuationReportTemplatePanel());
    // frame.setVisible(true);
    // frame.setSize(new Dimension(1273, 307));
    // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // }

}
