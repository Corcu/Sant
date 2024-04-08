package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.ReportTemplate;

public class SantTradesPotentialMtmErrorReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 1L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		final Vector<String> columns = new Vector<String>();
		columns.addElement(SantTradesPotentialMtmErrorReportStyle.PROCESS_DATE);
		columns.addElement(SantTradesPotentialMtmErrorReportStyle.REGISTRY);
		columns.addElement(SantTradesPotentialMtmErrorReportStyle.FRONT_ID);
		columns.addElement(SantTradesPotentialMtmErrorReportStyle.CONTRACTO);
		columns.addElement(SantTradesPotentialMtmErrorReportStyle.OWNER);
		columns.addElement(SantTradesPotentialMtmErrorReportStyle.PRODUCTO);
		columns.addElement(SantTradesPotentialMtmErrorReportStyle.MATURITY);
		columns.addElement(SantTradesPotentialMtmErrorReportStyle.ESTRUCTURA);
		columns.addElement(SantTradesPotentialMtmErrorReportStyle.MTM_D_MINUS1);

		setColumns(columns.toArray(new String[columns.size()]));
	}
}
