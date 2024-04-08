package calypsox.apps.reporting;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Vector;

public class SantAgreementParametersReportTemplatePanel extends SantGenericReportTemplatePanel {

    private static final String BLANK = "";

    private static final long serialVersionUID = 32L;

    protected SantComboBoxPanel<Integer, String> directionComboBoxPanel;
    protected SantComboBoxPanel<Integer, String> poEligibleSecurityIndComboBoxPanel;

    protected SantProcessDatePanel processDatePanel;

    @Override
    protected Border getMasterPanelBorder() {
        final TitledBorder titledBorder = BorderFactory.createTitledBorder("Agreement Parameters");
        titledBorder.setTitleColor(Color.BLUE);
        return titledBorder;
    }

    @Override
    protected Dimension getPanelSize() {
        return new Dimension(0, 170);
    }

    @Override
    protected void buildControlsPanel() {
        super.buildControlsPanel();
        this.directionComboBoxPanel = new SantComboBoxPanel<>("Direction", getDirectionVect());
        this.poEligibleSecurityIndComboBoxPanel = new SantComboBoxPanel<>("EligibleSec Ind",
                getPOEligibleSecurityIndicatorVect());
        this.processDatePanel = new SantProcessDatePanel("Process Date");
        this.processDatePanel.setPreferredSize(new Dimension(80, 24), new Dimension(215, 24));
        this.processDatePanel.removeDateLabel();
    }

    @Override
    public void setValDatetime(JDatetime valDatetime) {
        this.processDatePanel.setValDatetime(valDatetime);
    }

    @Override
    protected Component getNorthPanel() {
        return this.processDatePanel;
    }

    @Override
    protected JPanel getColumn1Panel() {
        final JPanel column1Panel = new JPanel();
        column1Panel.setLayout(new GridLayout(4, 1));
        column1Panel.add(this.poAgrPanel);
        column1Panel.add(this.cptyPanel);

        return column1Panel;
    }

    @Override
    protected JPanel getColumn2Panel() {
        final JPanel column2Panel = new JPanel();
        column2Panel.setLayout(new GridLayout(4, 1));

        column2Panel.add(this.agreementNamePanel);
        column2Panel.add(this.agreementTypePanel);
        column2Panel.add(this.directionComboBoxPanel);
        column2Panel.add(this.poEligibleSecurityIndComboBoxPanel);
        return column2Panel;
    }

    @Override
    protected JPanel getColumn3Panel() {
        final JPanel column3Panel = new JPanel();
        column3Panel.setLayout(new GridLayout(4, 1));
        column3Panel.add(this.economicSectorPanel);
        column3Panel.add(this.headCloneIndicatorPanel);
        column3Panel.add(this.baseCcyPanel);
        return column3Panel;
    }

    private Vector<String> getDirectionVect() {
        Vector<String> vect = new Vector<>();
        vect.add(CollateralConfig.NET_BILATERAL);
        vect.add(CollateralConfig.NET_UNILATERAL);
        return vect;
    }

    private Vector<String> getPOEligibleSecurityIndicatorVect() {
        Vector<String> vect = new Vector<>();
        vect.add(BLANK);
        vect.add(CollateralConfig.BOTH);
        vect.add(CollateralConfig.CASH);
        vect.add(CollateralConfig.SECURITY);
        return vect;
    }

    @Override
    public ReportTemplate getTemplate() {
        final ReportTemplate template = super.getTemplate();
        template.put(SantGenericTradeReportTemplate.PO_ELIGIBLE_SEC_IND,
                this.poEligibleSecurityIndComboBoxPanel.getValue());
        template.put(SantGenericTradeReportTemplate.AGREEMENT_DIRECTION, this.directionComboBoxPanel.getValue());

        this.processDatePanel.read(this.reportTemplate);

        return template;
    }

    @Override
    public void setTemplate(final ReportTemplate template) {
        super.setTemplate(template);
        String s = this.reportTemplate.get(SantGenericTradeReportTemplate.PO_ELIGIBLE_SEC_IND);
        if (!Util.isEmpty(s)) {
            this.poEligibleSecurityIndComboBoxPanel.setValue(s);
        }

        s = this.reportTemplate.get(SantGenericTradeReportTemplate.AGREEMENT_DIRECTION);
        if (!Util.isEmpty(s)) {
            this.directionComboBoxPanel.setValue(s);
        }

        this.processDatePanel.setTemplate(template);
        this.processDatePanel.write(template);

    }

}
