/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.SantHaircutByIsinReportTemplate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Vector;

public class SantHaircutByIsinReportTemplatePanel extends SantGenericReportTemplatePanel {

    private static final long serialVersionUID = 123L;

    protected SantProcessDatePanel processDatePanel;
    private SantChooseButtonPanel isinPanel;
    private Vector<String> isinIds;

    @Override
    public void setValDatetime(JDatetime valDatetime) {

        this.processDatePanel.setValDatetime(valDatetime);

    }

    @SuppressWarnings("unchecked")
    @Override
    protected void loadStaticData() {

        super.loadStaticData();
        try {
            this.isinIds = DSConnection.getDefault().getRemoteProduct().getSecCodeValues("ISIN", null, null);
        } catch (RemoteException e) {
            Log.error(this, "Error getting ISINs", e);
        }

    }

    @Override
    protected void buildControlsPanel() {

        super.buildControlsPanel();

        // value date
        this.processDatePanel = new SantProcessDatePanel("Value Date");
        this.processDatePanel.setPreferredSize(new Dimension(70, 24), new Dimension(215, 24));
        this.processDatePanel.removeDateLabel();

        // isin selector
        Collections.sort(this.isinIds);
        this.isinPanel = new SantChooseButtonPanel("ISIN", this.isinIds);

    }

    @Override
    protected Border getMasterPanelBorder() {

        final TitledBorder titledBorder = BorderFactory.createTitledBorder("Haircut By ISIN");
        titledBorder.setTitleColor(Color.BLUE);

        return titledBorder;

    }

    @Override
    public ReportTemplate getTemplate() {

        ReportTemplate template = super.getTemplate();
        this.processDatePanel.read(this.reportTemplate);
        // isin value/s
        String value = this.isinPanel.getValue();
        this.reportTemplate.put(SantHaircutByIsinReportTemplate.ISIN_ID, value);

        return template;
    }

    @Override
    public void setTemplate(ReportTemplate template) {

        super.setTemplate(template);
        this.processDatePanel.setTemplate(template);
        this.processDatePanel.write(template);
        // isin value/s
        this.isinPanel.setValue(template, SantHaircutByIsinReportTemplate.ISIN_ID);

    }

    @Override
    protected JPanel getNorthPanel() {

        return this.processDatePanel;

    }

    @Override
    protected JPanel getColumn1Panel() {

        final JPanel column1Panel = new JPanel();
        column1Panel.setLayout(new GridLayout(3, 1));
        column1Panel.add(new JLabel());
        // add agreement selector
        column1Panel.add(this.poAgrPanel);

        return column1Panel;
    }

    @Override
    protected JPanel getColumn2Panel() {

        final JPanel column2Panel = new JPanel();
        column2Panel.setLayout(new GridLayout(3, 1));
        column2Panel.add(new JLabel());
        // add agreement selector
        column2Panel.add(this.agreementNamePanel);

        return column2Panel;

    }

    @Override
    protected JPanel getColumn3Panel() {

        final JPanel column3Panel = new JPanel();
        column3Panel.setLayout(new GridLayout(3, 1));
        column3Panel.add(new JLabel());
        // add isin selector
        column3Panel.add(this.isinPanel);

        return column3Panel;

    }

}
