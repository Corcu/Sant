package calypsox.tk.report;

import java.util.Vector;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class IMAccountingReportStyle extends ReportStyle{
	
	private static final long serialVersionUID = 8326564045034855870L;
	private static final String PROCESS_DATE = "ProcessDate";
	private static final String VALUE_DATE = "ValueDate";
	private static final String TRADE_VALUE_DATE = "Trade ValueDate";
	private static final String MATURITY_DATE = "MaturityDate";
	private static final String REFERENCE = "Reference";
	private static final String BRANCH_OFFICE = "Branchoffice";
	private static final String ACCOUNT = "Account";
	private static final String DIRECTION = "Direction";
	private static final String AMOUNT = "Amount";
	private static final String AMOUNT_EUR = "Amount EUR";
	private static final String CURRENCY = "Currency";
	private static final String ORIGIN_AMOUNT = "Origin Amount";
	private static final String ORIGIN_CURRENCY = "Origin Currency";
	private static final String CPTY_UME = "Cpty Ume";
	private static final String DIRECTION_UME = "Direction Ume";
	private static final String PRODUCT_UME = "Product Ume";
	private static final String TIPOPER_UME = "Tipoper Ume";
	private static final String PRODUCT = "Product";
	private static final String TYPE_ASIENTO = "Tipo Asiento";
	private static final String SEND_MIS = "Send Mis";
	private static final String CONTRACT_ID = "Contract id";
	private static final String SECURITY = "Security";
	private static final String BOOK = "Book";
	 
	 
	 protected static final String[] DEFAULTS_COLUMNS = { PROCESS_DATE, VALUE_DATE, TRADE_VALUE_DATE,
			 MATURITY_DATE, REFERENCE,BRANCH_OFFICE,ACCOUNT,DIRECTION,AMOUNT,AMOUNT_EUR,CURRENCY,
			 ORIGIN_AMOUNT,ORIGIN_CURRENCY,CPTY_UME,DIRECTION_UME,PRODUCT_UME,TIPOPER_UME,
			 PRODUCT,TYPE_ASIENTO,SEND_MIS,CONTRACT_ID,SECURITY,BOOK};

	 
	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector arg2) {

		final IMAccountingReportBean item = (IMAccountingReportBean) row
				.getProperty(IMAccountingReportBean.IM_ACCOUNTING_BEAN);
		
		if (PROCESS_DATE.equals(columnName)) {
			return item.getProccesDate();
		} else if (VALUE_DATE.equals(columnName)) {
			return item.getValueDate();
		}else if (TRADE_VALUE_DATE.equals(columnName)) {
			return item.getValueTradeDate();
		} else if (MATURITY_DATE.equals(columnName)) {
			return item.getMaturityDate();
		}else if (REFERENCE.equals(columnName)) {
			return item.getReference();
		}else if (BRANCH_OFFICE.equals(columnName)) {
			return item.getBranchoffice();
		} else if (ACCOUNT.equals(columnName)) {
			return item.getPo(); // ? 
		}else if (DIRECTION.equals(columnName)) {
			return item.getDirection();
		} else if (AMOUNT.equals(columnName)) {
			return item.getAmount();
		}else if (AMOUNT_EUR.equals(columnName)) {
			return item.getImp_eur();
		} else if (CURRENCY.equals(columnName)) {
			return item.getCurrency();
		}else if (ORIGIN_AMOUNT.equals(columnName)) {
			return item.getOrigne_amount();
		} else if (ORIGIN_CURRENCY.equals(columnName)) {
			return item.getOrigne_currency();
		}else if (CPTY_UME.equals(columnName)) {
			return item.getCpty();
		} else if (DIRECTION_UME.equals(columnName)) {
			return item.getUme_sign();
		}else if (PRODUCT_UME.equals(columnName)) {
			return item.getUme_product();
		}else if (TIPOPER_UME.equals(columnName)) {
			return item.getUme_tipoper();
		}else if (TYPE_ASIENTO.equals(columnName)) {
			return item.getType_seat();
		}else if (SEND_MIS.equals(columnName)) {
			return item.getMis();
		} else if (CONTRACT_ID.equals(columnName)) {
			return item.getContractid();
		}else if (SECURITY.equals(columnName)) {
			return item.getSecurity();
		} else if (BOOK.equals(columnName)) {
			return item.getFolder();
		}else if (PRODUCT.equals(columnName)) {
			return item.getProduct();
		}
		
		return " Blank Column ";
	}
	
	@Override
	public TreeList getTreeList() {
		final TreeList treeList = new TreeList();
		treeList.add(PROCESS_DATE);
		treeList.add(VALUE_DATE);
		treeList.add(TRADE_VALUE_DATE);
		treeList.add(MATURITY_DATE);
		treeList.add(REFERENCE);
		treeList.add(BRANCH_OFFICE);
		treeList.add(ACCOUNT);
		treeList.add(DIRECTION);
		treeList.add(AMOUNT);
		treeList.add(AMOUNT_EUR);
		treeList.add(CURRENCY);
		treeList.add(ORIGIN_AMOUNT);
		treeList.add(ORIGIN_CURRENCY);
		treeList.add(CPTY_UME);
		treeList.add(DIRECTION_UME);
		treeList.add(PRODUCT_UME);
		treeList.add(TIPOPER_UME);
		treeList.add(TYPE_ASIENTO);
		treeList.add(SEND_MIS);
		treeList.add(CONTRACT_ID);
		treeList.add(SECURITY);
		treeList.add(BOOK);
		treeList.add(PRODUCT);
		
		return treeList;
	}
	

}
