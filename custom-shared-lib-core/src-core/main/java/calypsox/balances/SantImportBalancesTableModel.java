package calypsox.balances;

import com.calypso.apps.util.TableModelUtil;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;

/**
 * @author epalaobe
 *
 */
public class SantImportBalancesTableModel  extends TableModelUtil {

	private static final long serialVersionUID = 1492502007554129449L;
	
	public static final int PO_OWNER_COL_NUM = 0;
	public static final int COUNTERPARTY_COL_NUM = 1;
	public static final int CONTRACT_NAME_COL_NUM = 2;
	public static final int ISIN_COL_NUM = 3;
	public static final int NOMINAL_COL_NUM = 4;
	public static final int FATHER_ID_COL_NUM = 5;
	public static final int VALUE_DATE_COL_NUM = 6;
	public static final int SEND_MOVEMENT_COL_NUM = 7;
	public static final int TYPE_COL_NUM = 8;
	public static final int CURRENCY_COL_NUM = 9;
	public static final int ID_TRADE_NUM = 10;
	
	protected static final String ID_TRADE_COL = "Trade Id";
	protected static final String PO_OWNER_COL = "Owner";
	protected static final String COUNTERPARTY_COL = "Cpty";
	protected static final String CONTRACT_NAME_COL = "ContractName";
	protected static final String ISIN_COL = "ISIN";
	protected static final String NOMINAL_COL = "Nominal";
	protected static final String FATHER_ID_COL = "Contracts Re-calculation";
	protected static final String VALUE_DATE_COL = "Value Date";
	protected static final String SEND_MOVEMENT_COL = "Send"; //default false
	protected static final String TYPE_COL = "Type";
	protected static final String CURRENCY_COL = "Currency";

	protected static final int TOTAL_COLUMNS = 11;
	
	public static final String COLLATERAL_CONFIG_NOT_FOUND = "CollateralConfig Not Found";

	protected SantImportBalancesTableModel() {
		this(0);
	}

	public SantImportBalancesTableModel(final int rows) {
		super(TOTAL_COLUMNS, rows);
		
		setColumnName(PO_OWNER_COL_NUM, PO_OWNER_COL);
		setColumnName(COUNTERPARTY_COL_NUM, COUNTERPARTY_COL);
		setColumnName(CONTRACT_NAME_COL_NUM, CONTRACT_NAME_COL);
		setColumnName(ISIN_COL_NUM, ISIN_COL);
		setColumnName(NOMINAL_COL_NUM, NOMINAL_COL);
		setColumnName(FATHER_ID_COL_NUM, FATHER_ID_COL);
		setColumnName(VALUE_DATE_COL_NUM, VALUE_DATE_COL);
		setColumnName(SEND_MOVEMENT_COL_NUM, SEND_MOVEMENT_COL);
		setColumnName(TYPE_COL_NUM, TYPE_COL);
		setColumnName(CURRENCY_COL_NUM, CURRENCY_COL);
		setColumnName(ID_TRADE_NUM, ID_TRADE_COL);

		
		setColumnClass(PO_OWNER_COL_NUM, String.class);
		setColumnClass(COUNTERPARTY_COL_NUM, String.class);
		setColumnClass(CONTRACT_NAME_COL_NUM, String.class);
		setColumnClass(ISIN_COL_NUM, String.class);
		setColumnClass(NOMINAL_COL_NUM, Double.class);
		setColumnClass(FATHER_ID_COL_NUM, String.class);
		setColumnClass(VALUE_DATE_COL_NUM, JDate.class);
		setColumnClass(SEND_MOVEMENT_COL_NUM, String.class);
		setColumnClass(TYPE_COL_NUM, String.class);
		setColumnClass(CURRENCY_COL_NUM, String.class);
		setColumnClass(ID_TRADE_NUM, Integer.class);


		setColumnEditable(PO_OWNER_COL_NUM, true);
		setColumnEditable(COUNTERPARTY_COL_NUM, true);
		setColumnEditable(CONTRACT_NAME_COL_NUM, true);
		setColumnEditable(ISIN_COL_NUM, true);
		setColumnEditable(NOMINAL_COL_NUM, true);
		setColumnEditable(FATHER_ID_COL_NUM, true);
		setColumnEditable(VALUE_DATE_COL_NUM, true);
		setColumnEditable(SEND_MOVEMENT_COL_NUM, true);
		setColumnEditable(TYPE_COL_NUM, true);
		setColumnEditable(CURRENCY_COL_NUM, true);
		setColumnEditable(ID_TRADE_NUM, false);

	}

	@Override
	public void newValueAt(final int row, final int column, final Object value) {

		boolean isConversionRequired = false;

		if (isConversionRequired) {
			refresh();
		}
	}
	
	public void setCollateralConfigInfoToCells(final int row, String contractName) {
		String processingOrg = COLLATERAL_CONFIG_NOT_FOUND;
		String counterParty = COLLATERAL_CONFIG_NOT_FOUND;
		CollateralConfig collateralConfig = null;
		try {
			collateralConfig = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfigByCode(null, contractName);
			if (collateralConfig != null) {
				processingOrg = getLegalEntityCode(collateralConfig.getProcessingOrg());
				counterParty = getLegalEntityCode(collateralConfig.getLegalEntity());

				setValueNoCheck(row, PO_OWNER_COL_NUM, processingOrg);
				setValueNoCheck(row, COUNTERPARTY_COL_NUM, counterParty);
			} else {
				setValueNoCheck(row, PO_OWNER_COL_NUM, "");
				setValueNoCheck(row, COUNTERPARTY_COL_NUM, "");
			}
		} catch (Exception exc) {
			Log.warn(SantImportBalancesTableModel.class, exc);
		}
	}

	private String getLegalEntityCode(LegalEntity legalEntity){
		if(legalEntity!=null){
			return legalEntity.getCode();
		}
		return "";
	}
}
