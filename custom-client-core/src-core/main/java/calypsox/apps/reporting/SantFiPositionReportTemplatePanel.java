package calypsox.apps.reporting;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.*;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.BOPositionReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Vector;

public class SantFiPositionReportTemplatePanel extends SantGenericReportTemplatePanel {

    private static final long serialVersionUID = -2509019658734835894L;

    private SantStartEndDatePanel nextPaymentDatePanel;
    private SantProcessDatePanel proccessDatePanel;
    private SantStartEndDatePanel maturityDatePanel;
    private SantComboBoxPanel<Integer, String> buySellPanel;
    private Vector<String> buySellOptions;
    private SantChooseButtonPanel bondsPanel;
    private Vector<String> bondsIsin;

    public SantFiPositionReportTemplatePanel() {
        setPanelVisibility();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void buildControlsPanel() {

        super.buildControlsPanel();

        try {

            this.bondsIsin = DSConnection.getDefault().getRemoteProduct().getSecCodeValues("ISIN", null, null);

        } catch (final RemoteException e) {

            final StringBuffer message = new StringBuffer("Couldn't load the bonds");
            Log.error(message, e.getCause());
            Log.error(this, e); //sonar
        }

    }

    @Override
    protected void init() {
        super.init();
        removeAll();

        buildControlsPanel();

        final JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BorderLayout());
        masterPanel.setBorder(getMasterPanelBorder());

        add(masterPanel);

        final JPanel datePanel = new JPanel();
        datePanel.setLayout(new GridLayout(3, 1));

        final JPanel nextPaymentPanelSet = new JPanel();
        nextPaymentPanelSet.setLayout(new GridLayout(1, 1));
        this.nextPaymentDatePanel = new SantStartEndDatePanel("Next Payment");
        this.nextPaymentDatePanel.setPreferredSize(new Dimension(100, 24), new Dimension(215, 24), new Dimension(215,
                24));
        this.nextPaymentDatePanel.customInitDomains("NextPaymentStartRange", "NextPaymentStartRangePlus",
                "NextPaymentStartRangeTenor", "NextPaymentEndRange", "NextPaymentEndRangePlus",
                "NextPaymentEndRangeTenor");
        nextPaymentPanelSet.add(this.nextPaymentDatePanel);
        datePanel.add(nextPaymentPanelSet);

        final JPanel maturityPanelSet = new JPanel();
        maturityPanelSet.setLayout(new GridLayout(1, 1));
        this.maturityDatePanel = new SantStartEndDatePanel("Maturity");
        this.maturityDatePanel.setPreferredSize(new Dimension(100, 24), new Dimension(215, 24), new Dimension(215, 24));
        this.maturityDatePanel.customInitDomains("MaturityStartRange", "MaturityStartRangePlus",
                "MaturityStartRangeTenor", "MaturityEndRange", "MaturityEndRangePlus", "MaturityEndRangeTenor");
        maturityPanelSet.add(this.maturityDatePanel);
        datePanel.add(maturityPanelSet);

        final JPanel processDatePanelSet = new JPanel();
        processDatePanelSet.setLayout(new GridLayout(1, 1));
        this.proccessDatePanel = new SantProcessDatePanel("Process Date");
        this.proccessDatePanel.removeDateLabel();
        this.proccessDatePanel.setPreferredSize(new Dimension(100, 24), new Dimension(215, 24));
        this.proccessDatePanel.customInitDomains("PROCESS_DATE", "PROCESS_PLUS", "PROCESS_TENOR");
        processDatePanelSet.add(this.proccessDatePanel);
        datePanel.add(processDatePanelSet);
        masterPanel.add(datePanel, BorderLayout.NORTH);

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1, 3));
        final JPanel column1Panel = getColumn1Panel();
        final JPanel column2Panel = getColumn2Panel();
        final JPanel column3Panel = getColumn3Panel();
        mainPanel.add(column1Panel);
        mainPanel.add(column2Panel);
        mainPanel.add(column3Panel);

        masterPanel.add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    protected Border getMasterPanelBorder() {
        final TitledBorder titledBorder = BorderFactory.createTitledBorder("Sant Fi Position");
        titledBorder.setTitleColor(Color.BLUE);
        return titledBorder;
    }

    private void setPanelVisibility() {
        hideAllPanels();
        this.processStartEndDatePanel.setVisible(true);
        this.poDealPanel.setVisible(true);
        this.portfolioPanel.setVisible(true);
        this.cptyPanel.setVisible(true);
        this.agreementNamePanel.setVisible(true);
        this.agreementTypePanel.setVisible(true);
        this.poAgrPanel.setVisible(true);
    }

    @Override
    protected JPanel getColumn1Panel() {
        final JPanel column1panel = new JPanel();
        column1panel.removeAll();

        column1panel.setLayout(new GridLayout(3, 1));
        column1panel.add(this.poDealPanel);

        this.cptyPanel = new SantLegalEntityPanel(LegalEntity.COUNTERPARTY, "CounterParty", false, true, true, true);
        column1panel.add(this.cptyPanel);
        return column1panel;
    }

    @Override
    protected JPanel getColumn2Panel() {
        final JPanel column2panel = new JPanel();
        column2panel.removeAll();
        column2panel.setLayout(new GridLayout(3, 1));
        // 31/07/15. SBNA Multi-PO filter. add PO filter
        column2panel.add(this.poAgrPanel);
        column2panel.add(this.portfolioPanel);

        return column2panel;
    }

    @Override
    protected JPanel getColumn3Panel() {
        final JPanel column3panel = new JPanel();
        column3panel.removeAll();
        column3panel.setLayout(new GridLayout(3, 1));

        this.buySellOptions = new Vector<String>();
        this.buySellOptions.add("");
        this.buySellOptions.add("Buy");
        this.buySellOptions.add("Sell");
        this.buySellPanel = new SantComboBoxPanel<Integer, String>("Buy/Sell", this.buySellOptions);
        this.buySellPanel.setValue("");
        column3panel.add(this.buySellPanel);

        Collections.sort(this.bondsIsin);
        this.bondsPanel = new SantChooseButtonPanel("Bonds", this.bondsIsin);
        column3panel.add(this.bondsPanel);
        return column3panel;
    }

    @Override
    public ReportTemplate getTemplate() {
        super.getTemplate();

        if (!Util.isEmpty(this.poDealPanel.getLE())) {
            final String shortName = this.poDealPanel.getTextField().getText();
            this.reportTemplate.put("ProcessingOrg", shortName);
        } else {
            this.reportTemplate.remove("ProcessingOrg");
        }

        this.reportTemplate.put("Buy/Sell", this.buySellPanel.getValue());

        if (!Util.isEmpty(this.cptyPanel.getLE())) {
            final String shortName = this.cptyPanel.getTextField().getText();
            //v14 Mig - GSM fix to get id
            if (!Util.isEmpty(shortName)) {
                final LegalEntity cpty = BOCache.getLegalEntity(DSConnection.getDefault(), shortName);
                if (cpty != null)
                    this.reportTemplate.put(BOPositionReportTemplate.AGENT_ID, cpty.getAuthName());
            }

        } else {
            this.reportTemplate.remove("AGENT_ID");
        }

        final String value = this.portfolioPanel.getValue();
        this.reportTemplate.put("INV_POS_BOOKLIST", value);

        this.reportTemplate.put("POSITION_DATE", "Trade");
        this.reportTemplate.put("POSITION_CLASS", "Margin_Call");
        this.reportTemplate.put("POSITION_TYPE", "ALL");
        this.reportTemplate.put("AGREGATION", "Book/Agent/Account");
        this.reportTemplate.put("CASH_SECURITY", "Both");
        this.reportTemplate.put("INV_POS_MOVE", "Balance");

        this.nextPaymentDatePanel.read(this.reportTemplate);
        this.proccessDatePanel.read(this.reportTemplate);

        this.maturityDatePanel.read(this.reportTemplate);

        this.reportTemplate.put("Bonds", this.bondsPanel.getValue());

        return this.reportTemplate;
    }

    @Override
    public void setTemplate(final ReportTemplate template) {
        super.setTemplate(template);
        this.reportTemplate = template;
        if (!Util.isEmpty((String) this.reportTemplate.get("ProcessingOrg"))) {
            final LegalEntity legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(),
                    this.reportTemplate.get("ProcessingOrg").toString());

            if (legalEntity != null) {
                this.poDealPanel.setLE(legalEntity.getAuthName());
            }
        }

        if (!Util.isEmpty((String) this.reportTemplate.get("AGENT_ID"))) {
            final LegalEntity legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(),
                    this.reportTemplate.get("AGENT_ID").toString());

            if (legalEntity != null) {
                this.cptyPanel.setLE(legalEntity.getAuthName());
            }
        }

        this.portfolioPanel.setValue(this.reportTemplate, "INV_POS_BOOKLIST");

        this.nextPaymentDatePanel.setTemplate(this.reportTemplate);
        this.nextPaymentDatePanel.write(this.reportTemplate);

        this.proccessDatePanel.setTemplate(this.reportTemplate);
        this.proccessDatePanel.write(this.reportTemplate);

        this.maturityDatePanel.setTemplate(this.reportTemplate);
        this.maturityDatePanel.write(this.reportTemplate);

        this.reportTemplate.put("Bonds", template.get("Bonds"));
        this.reportTemplate.put("Buy/Sell", template.get("Buy/Sell"));
    }

    public static void main(final String... args) throws ConnectException {
        ConnectionUtil.connect(args, "SantFIPositionReportTemplate");
        final JFrame frame = new JFrame();
        frame.setTitle("SantFIPositionReportTemplate");
        frame.setContentPane(new SantFiPositionReportTemplatePanel());
        frame.setVisible(true);
        frame.setSize(new Dimension(1273, 307));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}