package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

public class SantSecuritiesFlowReportTemplate extends SantGenericTradeReportTemplate {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5974000418220336862L;
	public static final String SISTEMA_ORIGEN = "SantSecuritiesFlow.SISTEMA_ORIGEN";
	public static final String BUY_SELL = "SantSecuritiesFlow.Buy/Sell";
	public static final String BOND_DIRTY_PRICE = "SantSecuritiesFlow.Dirty Price";
	public static final String FACE_AMOUNT = "SantSecuritiesFlow.Face Amount";
	public static final String BOND_NAME = "SantSecuritiesFlow.Bond Name";
	public static final String FRECUENCIA_CUPON = "SantSecuritiesFlow.Frecuencia de Cupon";
	public static final String MATURITY_DATE = "SantSecuritiesFlow.Maturity Date";
	public static final String CORTE_CUPON = "SantSecuritiesFlow.Corte Cupon";

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(SantSecuritiesFlowReportStyle.DEFAULTS_COLUMNS);

	}
}
