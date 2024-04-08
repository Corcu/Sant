package calypsox.apps.reporting;

import javax.swing.JLabel;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.User;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;

/**
 * BrsAuditReportTemplatePanel
 * 
 */
@SuppressWarnings("serial")
public class BrsMifidReportTemplatePanel extends ReportTemplatePanel {

    private JLabel filterNameLabel = new JLabel();
    private CalypsoComboBox filterNameComboBox = new CalypsoComboBox();
    private ReportTemplate template;

    /**
     * TradeFilterReportTemplatePanel
     */
    public BrsMifidReportTemplatePanel() {
        final int widthLayout = 500;
        final int heightLayout = 30;
        setSize(widthLayout, heightLayout);
        this.filterNameLabel.setText(TradeReportTemplate.TRADE_FILTER);
        add(this.filterNameLabel);
        add(this.filterNameComboBox);
        AppUtil.set(this.filterNameComboBox,
                AccessUtil.getAllNames(User.TRADE_FILTER));
        this.template = null;
    }

    @Override
    public ReportTemplate getTemplate() {
        final String tradeFilterName = (String) this.filterNameComboBox
                .getSelectedItem();
        this.template.put(TradeReportTemplate.TRADE_FILTER, tradeFilterName);
        return this.template;
    }

    @Override
    public void setTemplate(final ReportTemplate template) {
        this.template = template;
        final String tradeFilter = (String) this.template
                .get(TradeReportTemplate.TRADE_FILTER);
        if (Util.isEmpty(tradeFilter)) {
            this.filterNameComboBox.setSelectedItem("ALL");
        } else {
            this.filterNameComboBox.setSelectedItem(tradeFilter);
        }
    }
	
	
}
