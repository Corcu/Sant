package calypsox.tk.report;

import com.calypso.tk.report.*;

import java.util.Vector;

/**
 * Initial Margin. Qef Margin Call Entry selector to send Msg to QEF
 *
 * @author xIS15793
 */
public class InitialMarginContractConfigReport extends MarginCallEntryReport
        implements InitialMarginContractConfigConstants {

    /**
     * serialVersion id
     */
    private static final long serialVersionUID = -8400164165915183868L;

    private DefaultReportOutput reportOutput = null;

    public DefaultReportOutput getReportOutput() {
        return this.reportOutput;
    }

    public void setReportOutput(DefaultReportOutput reportOutput) {
        this.reportOutput = reportOutput;
    }

    @Override
    public ReportOutput load(@SuppressWarnings("rawtypes") Vector errorMsgs) {
        DefaultReportOutput tmpReportOutput = (DefaultReportOutput) super.load(errorMsgs);
        if ((tmpReportOutput == null) || (tmpReportOutput.getRows() == null)) {
            return tmpReportOutput;
        }
        setReportOutput(tmpReportOutput);
        return tmpReportOutput;
    }

    @Override
    public ReportTemplate getReportTemplate() {
        if (this.reportOutput != null) {
            ReportStyle reportStyle = this.reportOutput.getStyle();
            if (reportStyle != null) {
                reportStyle.setProperty(InitialMarginContractConfigConstants.QEF_UNSELECT_ALL,
                        this._reportTemplate.get(InitialMarginContractConfigConstants.QEF_UNSELECT_ALL));
                reportStyle.setProperty(InitialMarginContractConfigConstants.QEF_SELECT_ALL,
                        this._reportTemplate.get(InitialMarginContractConfigConstants.QEF_SELECT_ALL));
            }
        }
        ReportTemplate reportTemplate = super.getReportTemplate();
        // if ((reportTemplate != null) && (reportTemplate.getColumns() != null)) {
        // if (!Arrays.asList(reportTemplate.getColumns()).contains(ID_MARGIN_CALL)) {
        // Vector<String> columns = new Vector<String>();
        // columns.addAll(Arrays.asList(reportTemplate.getColumns()));
        // columns.addElement(ID_MARGIN_CALL);
        // reportTemplate.setColumns(columns.toArray(new String[columns.size()]));
        // }
        // }
        return reportTemplate;
    }
}
