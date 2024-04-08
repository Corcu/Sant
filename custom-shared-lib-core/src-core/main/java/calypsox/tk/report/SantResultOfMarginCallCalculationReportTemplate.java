package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.ReportTemplate;

public class SantResultOfMarginCallCalculationReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 1L;

	@Override
	public void setDefaults() {
		Vector<String> columns = new Vector<String>();

		columns.addElement(SantResultOfMarginCallCalculationReportStyle.EXPOSURE_COUNTERPARTY);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.COLLATERAL_IN_TRANSIT_CASH);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.COLLATERAL_IN_TRANSIT_BOND);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.FACE_AMOUNT_BOND);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.PRICE_BOND);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.HAIRCUT_BOND);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.VALUEDATE_DEFINE_CASH);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.VALUEDATE_DEFINE_BOND);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.DISPUTE);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.DISPUTE_TYPE);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.DISPUTE_EXPOSURE);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.DISPUTE_DATE);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.MARGIN_CALL_CALCULATION);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.DEALS);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.EFFECTIVE_CURRENCY_1);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.EFFECTIVE_CURRENCY_2);
		columns.addElement(SantResultOfMarginCallCalculationReportStyle.EFFECTIVE_CURRENCY_3);

		setColumns(columns.toArray(new String[columns.size()]));
	}
}
