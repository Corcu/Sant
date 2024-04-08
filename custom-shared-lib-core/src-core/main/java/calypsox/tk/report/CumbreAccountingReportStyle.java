package calypsox.tk.report;

import java.util.Arrays;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class CumbreAccountingReportStyle extends ReportStyle {
	
	//comun
	public static final String ACCOUNT = "Account number";

	//balance report
	public static final String BALANCE_PROCESS_DATE = "Balance Process Date";
	public static final String BALANCE_BRANCH = "Balance Branch Number";
	public static final String CPTY_NAME = "Counterparty Short Name";
	public static final String BALANCE_REF = "Balance Reference";
	public static final String BALANCE_CCY = "Balance CCY";
	public static final String BALANCE_AMOUNT = "Balance Amount";
	public static final String CCY_1 = "CCY_1";
	public static final String AMOUNT_1 = "Amount 1 in EUR";
	public static final String CCY_2 = "CCY_2";
	public static final String AMOUNT_2 = "Amount 2 in EUR";
	public static final String ADD_FIELD_1 = "Additional field 1";
	public static final String ADD_FIELD_2 = "Additional field 2";
	public static final String ADD_FIELD_3 = "Additional field 3";
	public static final String ADD_FIELD_4 = "Additional field 4";
	public static final String ADD_FIELD_5 = "Additional field 5";
	public static final String ACCOUNT_DOMM = "Balance Account number";

	
	//movement report
	public static final String FILE_IDENTIFIER = "Movement File Identifier";
	public static final String MOVEMENT_BRANCH = "Movement Branch Number";
	
	public static final String VALUE_DATE = "Value date";
	public static final String TESOR_1 = "Tesor 1";
	public static final String MOVEMENT_AMOUNT = "Movement Amount (singless)";
	public static final String MOVEMENT_TYPE = "Movement type";
	public static final String MOVEMENT_CCY_CODE= "Movement Ccy Code";
	public static final String MOVEMENT_VALUE= "Value";
	public static final String SIGOMLIQ= "SIGOMLIQ";
	public static final String MOVEMENT_REF = "Movement Reference";
	public static final String TESOR_2 = "Tesor 2";
	public static final String MOVEMENT_PROCESS_DATE = "Movement Process Date";
	

	
	//default columns for balance report
	private static final String[] DEFAULTS_COLUMNS = { BALANCE_PROCESS_DATE, BALANCE_BRANCH, CPTY_NAME, ACCOUNT, BALANCE_REF,
			BALANCE_CCY, BALANCE_AMOUNT, CCY_1, AMOUNT_1, CCY_2, AMOUNT_2, ADD_FIELD_1, ADD_FIELD_2, ADD_FIELD_3,
			ADD_FIELD_4, ADD_FIELD_5 };

	
	/** UID */
	private static final long serialVersionUID = 1L;

	private static final String EUR = "EUR";

	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

		final CumbreAccountingReportBean balanceBean = row.getProperty(CumbreAccountingReportBean.CUMBRE_ACCOUNTING_REPORT_BEAN);
		boolean isLode = balanceBean.getIsLode();

		if (columnName == null)		
			columnName = "";		// 'switch' would generate a nullpointer exception if the argument was null, so we convert null into ""
		switch (columnName) {
		
			case BALANCE_PROCESS_DATE:	return balanceBean.getProcessDate();
			case BALANCE_BRANCH:		return balanceBean.getBalanceBranch();
			case CPTY_NAME:				return balanceBean.getCptyShortName();
			case ACCOUNT:				return balanceBean.getAccountNumber();
			case ACCOUNT_DOMM:			return CumbreReportLogic.getAccountNumberDOMM(balanceBean.getAccountNumber());
			case BALANCE_REF:			return balanceBean.getBalanceReference();
			case BALANCE_CCY:			return balanceBean.getBalanceCcy();
			case BALANCE_AMOUNT:		return balanceBean.getBalanceAmount();
			case CCY_1:					return isLode ? balanceBean.getBalanceCcy() : EUR;
			case CCY_2:					return EUR;
			case AMOUNT_1:
			case AMOUNT_2:				return balanceBean.getAmount();
			case ADD_FIELD_1:
			case ADD_FIELD_2:
			case ADD_FIELD_3:
			case ADD_FIELD_4:			return getZeros(35);
			case ADD_FIELD_5:			return getZeros(10);
			
			//movement report
			case FILE_IDENTIFIER:		return CumbreReportLogic.getFileIdentifier();
			case MOVEMENT_BRANCH:		return CumbreReportLogic.getMovementBranchNumber();
			case MOVEMENT_TYPE:			return balanceBean.getMovementType();
			case VALUE_DATE:			return balanceBean.getValueDate();
			case TESOR_1:				return CumbreReportLogic.getTesorWithZeros(false);
			case MOVEMENT_AMOUNT:		return balanceBean.getMovementAmount();
			case MOVEMENT_VALUE:		return balanceBean.getMovementValue();
			case SIGOMLIQ:				return CumbreReportLogic.getSigomliq();
			case MOVEMENT_REF:			return balanceBean.getMovementRef();
			case TESOR_2:				return CumbreReportLogic.getTesorWithZeros(true);
			case MOVEMENT_PROCESS_DATE:	return balanceBean.getMovementProcessDate();
			
			default:					return "";
		}

	}

	/**
	 * 
	 * @param zerosNumber
	 * @return String with zeros
	 */
	private String getZeros(int zerosNumber) {
		StringBuilder zerosBuilder = new StringBuilder();
		for (int i = 0; i < zerosNumber; i++) {
			zerosBuilder.append("0");
		}
		return zerosBuilder.toString();
	}
	
	/** Required by Sonar to make DEFAULTS_COLUMNS private */
	protected static String[] getCumbreDefaultColumns() {
		return Arrays.copyOf(DEFAULTS_COLUMNS, DEFAULTS_COLUMNS.length);	// copy to avoid sonar warning "Returning 'DEFAULTS_COLUMNS' may expose an internal array"
	}
	


}
