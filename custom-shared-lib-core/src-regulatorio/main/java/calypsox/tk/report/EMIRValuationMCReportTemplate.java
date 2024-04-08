package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

public class EMIRValuationMCReportTemplate extends SantGenericTradeReportTemplate{
	
	private static final String _TITLE = "EMIR - Valuation MarginCall Report";

	public static final String GROUPING_REPORT_IDS = "GROUPING_REPORT_IDS";
	public static final String GROUPING_REPORT_NAMES = "GROUPING_REPORT_NAMES";
	public static final String MARGIN_CALL_CONTRACTS_ID = "MARGIN_CALL_CONTRACTS_ID";
	public static final String CONTRACT_TYPE = "CONTRACT_TYPE";
	public static final String SUBMITTER_REPORT = "SUBMITTER_REPORT";
	public static final String REPLACE_OWNER_NAME = "REPLACE_OWNER_NAME";
	public static final String REPLACE_OWNER_ID = "REPLACE_OWNER_ID";
	
	private static final long serialVersionUID = 9152093326426380475L;
	
	protected static final String[] DEFAULT_COLUMNS = {EMIRReportLogic.VERSION_COLUMN,
			EMIRReportLogic.MESSAGEID_COLUMN,
			EMIRReportLogic.ACTION_COLUMN, 
			EMIRReportLogic.LEIPREFIX_COLUMN,
			EMIRReportLogic.LEIVALUE_COLUMN,
			EMIRReportLogic.TRADEPARTYPREF1_COLUMN,
			EMIRReportLogic.TRADEPARTYVAL1_COLUMN,
			EMIRReportLogic.COLLATERALPORTFOLIOCODE_COLUMN,
			EMIRReportLogic.COLLATERALPORTFOLIOIND_COLUMN,
			EMIRReportLogic.SENDTO_COLUMN, 
			EMIRReportLogic.OTHERPARTYTYPEID_COLUMN,
			EMIRReportLogic.OTHERPARTYID_COLUMN,
			EMIRReportLogic.COLLATERALIZED_COLUMN,
			EMIRReportLogic.LEVEL_COLUMN, //14 NEW VAL LAYOUT
			EMIRReportLogic.RESERVEDPARTICIPANTUSE1}; // GLCS value
	
	@Override
    public void setDefaults() {
		super.setDefaults();
        put("Title", _TITLE);
        
        StringBuilder aux = new StringBuilder();
        for(int i = 0; i < DEFAULT_COLUMNS.length; i++){
        	DEFAULT_COLUMNS[i] = aux.append(EMIRValuationMCReportStyle.EMIR_PREFIX).append(DEFAULT_COLUMNS[i]).toString();
        	aux = new StringBuilder();
        }
        
		setColumns(DEFAULT_COLUMNS);
	}
	
}
