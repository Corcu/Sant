package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.BOPositionReportTemplate;

public class SantFiPositionReportTemplate extends BOPositionReportTemplate {

	/**
     * 
     */
	private static final long serialVersionUID = -4257895219796783965L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		final Vector<String> columns = new Vector<String>();
		columns.addElement(SantFiPositionReportStyle.PROCESS_DATE);
		columns.addElement(SantFiPositionReportStyle.PORTFOLIO);
		columns.addElement(SantFiPositionReportStyle.COUNTERPARTY);
		columns.addElement(SantFiPositionReportStyle.COUNTERPARTY_NAME);
		columns.addElement(SantFiPositionReportStyle.BUY_SELL);
		columns.addElement(SantFiPositionReportStyle.BOND);
		columns.addElement(SantFiPositionReportStyle.BOND_NAME);
		columns.addElement(SantFiPositionReportStyle.CURRENCY);
		columns.addElement(SantFiPositionReportStyle.STATUS);
		columns.addElement(SantFiPositionReportStyle.FACE_AMOUNT);
		columns.addElement(SantFiPositionReportStyle.DIRTY_PRICE);
		columns.addElement(SantFiPositionReportStyle.MATURITY_DATE);
		columns.addElement(SantFiPositionReportStyle.FRECUENCIA_CUPON);
		columns.addElement(SantFiPositionReportStyle.CORTE_CUPON);

		setColumns(columns.toArray(new String[columns.size()]));
	}

	@Override
	public void callBeforeLoad() {
		// DO NOTHING - to avoid date columns default calypso behaviour for BO
		// Position
	}

}
