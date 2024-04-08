package calypsox.apps.reporting;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import com.calypso.apps.reporting.ReportTemplateDatePanel;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;
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

public class SantSecuritiesFlowReportTemplatePanel extends SantGenericReportTemplatePanel {

    private static final long serialVersionUID = -2509019658734835894L;

    private JLabel tradeDateLabel;
    private ReportTemplateDatePanel tradeStartDatePanel;
    private ReportTemplateDatePanel tradeEndDatePanel;
    private JLabel valueDateLabel;
    private ReportTemplateDatePanel valueStartDatePanel;
    private ReportTemplateDatePanel valueEndDatePanel;
    private SantComboBoxPanel<Integer, String> buySellPanel;
    private Vector<String> buySellOptions;
    private SantChooseButtonPanel bondsPanel;
    // private Map<Integer, String> bondsNames;
    private Vector<String> bondsIsin;

    // private Map<Integer, String> bondsDescription;

    public SantSecuritiesFlowReportTemplatePanel() {
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
        nextPaymentPanelSet.setLayout(new GridLayout(1, 3));
        this.valueDateLabel = new JLabel("        Value Date");
        nextPaymentPanelSet.add(this.valueDateLabel);
        this.valueStartDatePanel = ReportTemplateDatePanel.getStart();
        initDatePanel(this.valueStartDatePanel);
        nextPaymentPanelSet.add(this.valueStartDatePanel);
        this.valueEndDatePanel = ReportTemplateDatePanel.getEnd();
        initDatePanel(this.valueEndDatePanel);
        this.valueEndDatePanel.setDependency(this.valueStartDatePanel);
        nextPaymentPanelSet.add(this.valueEndDatePanel);
        datePanel.add(nextPaymentPanelSet);

        final JPanel processDatePanelSet = new JPanel();
        processDatePanelSet.setLayout(new GridLayout(1, 3));
        this.tradeDateLabel = new JLabel("       Trade Date");
        processDatePanelSet.add(this.tradeDateLabel);
        this.tradeStartDatePanel = ReportTemplateDatePanel.getStart();
        initDatePanel(this.tradeStartDatePanel);
        processDatePanelSet.add(this.tradeStartDatePanel);
        this.tradeEndDatePanel = ReportTemplateDatePanel.getEnd();
        initDatePanel(this.tradeEndDatePanel);
        processDatePanelSet.add(this.tradeEndDatePanel);
        processDatePanelSet.setPreferredSize(new Dimension(230, 24));
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

    private void initDatePanel(final ReportTemplateDatePanel panel) {
        panel.init(TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);
    }

    @Override
    protected Border getMasterPanelBorder() {
        final TitledBorder titledBorder = BorderFactory.createTitledBorder("Securities Flow");
        titledBorder.setTitleColor(Color.BLUE);
        return titledBorder;
    }

    private void setPanelVisibility() {
        hideAllPanels();
        this.processStartEndDatePanel.setVisible(true);
        this.poAgrPanel.setVisible(true);
        this.poDealPanel.setVisible(true);
        this.portfolioPanel.setVisible(true);
        this.cptyPanel.setVisible(true);
        this.baseCcyPanel.setVisible(true);
        this.agreementNamePanel.setVisible(true);
        this.agreementTypePanel.setVisible(true);
    }

    @Override
    protected JPanel getColumn1Panel() {
        final JPanel column1panel = new JPanel();
        column1panel.removeAll();

        column1panel.setLayout(new GridLayout(3, 1));
        column1panel.add(this.poDealPanel);
        column1panel.add(this.poAgrPanel);

        return column1panel;
    }

    @Override
    protected JPanel getColumn2Panel() {
        final JPanel column2panel = new JPanel();
        column2panel.removeAll();
        column2panel.setLayout(new GridLayout(3, 1));
        column2panel.add(this.portfolioPanel);
        column2panel.add(this.cptyPanel);
        column2panel.add(this.baseCcyPanel);

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
        this.reportTemplate = super.getTemplate();

        final String processDate = this.tradeStartDatePanel.getDateText().getText();

        this.reportTemplate.put("PROCESS_DATE", processDate);

        this.reportTemplate.put("TradeDateStartRange",
                Util.stringToJDate(this.tradeStartDatePanel.getDateText().getText()));
        if ((this.tradeStartDatePanel.getPlusChoice() != null)
                && (!"".equals(this.tradeStartDatePanel.getPlusChoice()))) {
            this.reportTemplate.put("TradeDateStartRangeTenor", this.tradeStartDatePanel.getTenorChoice());
            this.reportTemplate.put("TradeDateStartRangePlus", this.tradeStartDatePanel.getPlusChoice());
        } else {
            this.reportTemplate.put("TradeDateStartRangeTenor", "");
            this.reportTemplate.put("TradeDateStartRangePlus", "");
        }

        this.reportTemplate
                .put("TradeDateEndRange", Util.stringToJDate(this.tradeEndDatePanel.getDateText().getText()));
        if ((this.tradeEndDatePanel.getPlusChoice() != null) && (!"".equals(this.tradeEndDatePanel.getPlusChoice()))) {
            this.reportTemplate.put("TradeDateEndRangeTenor", this.tradeEndDatePanel.getTenorChoice());
            this.reportTemplate.put("TradeDateEndRangePlus", this.tradeEndDatePanel.getPlusChoice());
        } else {
            this.reportTemplate.put("TradeDateStartEndTenor", "");
            this.reportTemplate.put("TradeDateStartEndPlus", "");
        }

        this.reportTemplate.put("ValueDateStartRange",
                Util.stringToJDate(this.valueStartDatePanel.getDateText().getText()));
        if ((this.valueStartDatePanel.getPlusChoice() != null)
                && (!"".equals(this.valueStartDatePanel.getPlusChoice()))) {
            this.reportTemplate.put("ValueDateStartRangeTenor", this.valueStartDatePanel.getTenorChoice());
            this.reportTemplate.put("ValueDateStartRangePlus", this.valueStartDatePanel.getPlusChoice());
        } else {
            this.reportTemplate.put("ValueDateStartRangeTenor", "");
            this.reportTemplate.put("ValueDateStartRangePlus", "");
        }

        this.reportTemplate
                .put("ValueDateEndRange", Util.stringToJDate(this.valueEndDatePanel.getDateText().getText()));
        if ((this.valueEndDatePanel.getPlusChoice() != null) && (!"".equals(this.valueEndDatePanel.getPlusChoice()))) {
            this.reportTemplate.put("ValueDateEndRangeTenor", this.valueEndDatePanel.getTenorChoice());
            this.reportTemplate.put("ValueDateEndRangePlus", this.valueEndDatePanel.getPlusChoice());
        } else {
            this.reportTemplate.put("ValueDateEndRangeTenor", "");
            this.reportTemplate.put("ValueDateEndRangePlus", "");
        }

        this.reportTemplate.put("Bonds", this.bondsPanel.getValue());

        this.reportTemplate.put("BuySell", this.buySellPanel.getValue());

        this.reportTemplate.put("StartDate", this.valueStartDatePanel.getDateText().getText());
        this.reportTemplate.put("EndDate", this.valueEndDatePanel.getDateText().getText());

        this.reportTemplate.put("ProcessStartDate", this.tradeStartDatePanel.getDateText().getText());
        this.reportTemplate.put("ProcessEndDate", this.tradeEndDatePanel.getDateText().getText());

        return this.reportTemplate;
    }

    @Override
    public void setTemplate(final ReportTemplate template) {
        this.reportTemplate = template;

        this.tradeStartDatePanel.setDate((JDate) this.reportTemplate.get("TradeDateStartRange"));
        this.tradeStartDatePanel.setTenorChoice((String) this.reportTemplate.get("TradeDateStartRangeTenor"));
        this.tradeStartDatePanel.setPlusChoice((String) this.reportTemplate.get("TradeDateStartRangePlus"));

        this.tradeEndDatePanel.setDate((JDate) this.reportTemplate.get("TradeDateEndRange"));
        this.tradeEndDatePanel.setTenorChoice((String) this.reportTemplate.get("TradeDateEndRangeTenor"));
        this.tradeEndDatePanel.setPlusChoice((String) this.reportTemplate.get("TradeDateEndRangePlus"));

        this.valueStartDatePanel.setDate((JDate) this.reportTemplate.get("ValueDateStartRange"));
        this.valueStartDatePanel.setTenorChoice((String) this.reportTemplate.get("ValueDateStartRangeTenor"));
        this.valueStartDatePanel.setPlusChoice((String) this.reportTemplate.get("ValueDateStartRangePlus"));

        this.valueEndDatePanel.setDate((JDate) this.reportTemplate.get("ValueDateEndRange"));
        this.valueEndDatePanel.setTenorChoice((String) this.reportTemplate.get("ValueDateEndRangeTenor"));
        this.valueEndDatePanel.setPlusChoice((String) this.reportTemplate.get("ValueDateEndRangePlus"));

        this.reportTemplate.put("Bonds", template.get("Bonds"));
        this.reportTemplate.put("BuySell", template.get("BuySell"));
    }

    public static void main(final String... args) throws ConnectException {
        ConnectionUtil.connect(args, "SantSecuritiesFlowReportTemplatePanel");
        final JFrame frame = new JFrame();
        frame.setTitle("SantSecuritiesFlowReportTemplatePanel");
        frame.setContentPane(new SantSecuritiesFlowReportTemplatePanel());
        frame.setVisible(true);
        frame.setSize(new Dimension(1273, 307));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}