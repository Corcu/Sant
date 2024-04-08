package calypsox.tk.report;

import java.util.Vector;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

@SuppressWarnings("serial")
public class SantLegChangeControlReportTemplate extends SantGenericTradeReportTemplate {

	@Override
	public void setDefaults() {
		super.setDefaults();
		final Vector<String> columns = new Vector<String>();

		columns.addElement(SantLegChangeControlReportStyle.REPORT_DATE);
		columns.addElement(SantLegChangeControlReportStyle.OWNER);
		columns.addElement(SantLegChangeControlReportStyle.GLCS);
		columns.addElement(SantLegChangeControlReportStyle.COLLATERAL_DESCR);
		columns.addElement(SantLegChangeControlReportStyle.COLL_AGREE_TYPE);
		columns.addElement(SantLegChangeControlReportStyle.COLL_BASE_CCY);
		columns.addElement(SantLegChangeControlReportStyle.ELIGIBLE_COLLATERAL);
		columns.addElement(SantLegChangeControlReportStyle.CCY_ASSET);
		columns.addElement(SantLegChangeControlReportStyle.CPTY_CCY);
		columns.addElement(SantLegChangeControlReportStyle.CCY_ASSET_CCY);
		columns.addElement(SantLegChangeControlReportStyle.INITIAL_POSITION);
		columns.addElement(SantLegChangeControlReportStyle.INITIAL_CCY);
		columns.addElement(SantLegChangeControlReportStyle.MOVEMENT_AMOUNT);
		columns.addElement(SantLegChangeControlReportStyle.CCY_MOVEMENT);
		columns.addElement(SantLegChangeControlReportStyle.FINAL_SITUATION);
		columns.addElement(SantLegChangeControlReportStyle.MOVEMENT_CCY);

		setColumns(columns.toArray(new String[columns.size()]));

	}
}
