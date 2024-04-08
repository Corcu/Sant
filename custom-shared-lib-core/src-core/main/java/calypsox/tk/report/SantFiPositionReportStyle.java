package calypsox.tk.report;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.BOSecurityPositionReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.service.DSConnection;

public class SantFiPositionReportStyle extends ReportStyle {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String PROCESS_DATE = "Process Date";
	public static final String PORTFOLIO = "Folder";
	public static final String COUNTERPARTY = "CounterParty";
	public static final String COUNTERPARTY_NAME = "CounterParty Name";
	public static final String BUY_SELL = "Buy/Sell";
	public static final String BOND = "Bond";
	public static final String BOND_NAME = "Bond Name";
	public static final String CURRENCY = "Currency";
	public static final String FACE_AMOUNT = "Face Amount";
	public static final String DIRTY_PRICE = "Dirty Price";
	public static final String MATURITY_DATE = "Maturity Date";
	public static final String FRECUENCIA_CUPON = "Frecuencia de Cupon";
	public static final String CORTE_CUPON = "Corte Cupon";
	public static final String STATUS = "Status";

	private static final String BUY = "Buy";
	private static final String SELL = "Sell";
	public static final String[] DEFAULTS_COLUMNS = {};

	private BOSecurityPositionReportStyle boSecurityPositionStyle = null;
	private static JDate processDate = null;

	public SantFiPositionReportStyle() {
		this.boSecurityPositionStyle = new BOSecurityPositionReportStyle();
	}

	// AAP MIG 14.4
	public static void setProcessDate(JDate processDate) {
		SantFiPositionReportStyle.processDate = processDate;
	}

	/**
	 * Calculate and get the result for every column of a row.
	 */
	@SuppressWarnings({ "rawtypes" })
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final InventorySecurityPosition inventory = (InventorySecurityPosition) row.getProperty("Default");

		LegalEntity agent = null;
		if (inventory != null) {
			agent = inventory.getAgent();
		}
		if (columnName.equals(PROCESS_DATE)) {
			// AAP MIG 14.4
			return processDate;
		} else if (columnName.equals(PORTFOLIO)) {
			return this.boSecurityPositionStyle.getColumnValue(row, BOSecurityPositionReportStyle.BOOK, errors);
		} else if (columnName.equals(COUNTERPARTY)) {
			if (agent != null) {
				return agent.getAuthName();
			} else {
				return "";
			}
		} else if (columnName.equals(COUNTERPARTY_NAME)) {
			if (agent != null) {
				return agent.getName();
			} else {
				return "";
			}
		} else if (columnName.equals(BUY_SELL)) {
			return getDirection(inventory.getTotal());
		} else if (columnName.equals(BOND)) {
			return this.boSecurityPositionStyle.getColumnValue(row, "PRODUCT_CODE.ISIN", errors);
		} else if (columnName.equals(BOND_NAME)) {
			return this.boSecurityPositionStyle.getColumnValue(row, "Name", errors);
		} else if (columnName.equals(CURRENCY)) {
			return this.boSecurityPositionStyle.getColumnValue(row, columnName, errors);
		} else if (columnName.equals(FACE_AMOUNT)) {
			// AAP BUG, FACE AMOUNT IS ALWAYS 0
			Bond security = (Bond) inventory.getProduct();
			Vector<InventorySecurityPosition> posVector = new Vector<>();
			posVector.addElement(inventory);
			// SpecificInventoryPositionValues.SpecificInventoryPositionValueContext
			// posContext =
			// (SpecificInventoryPositionValues.SpecificInventoryPositionValueContext)
			// row
			// .getProperty("SpecificInventoryPosition");
			
			//Amount total = (Amount) boSecurityPositionStyle.getColumnValue(row, "22-mar-2016", new Vector<>());
			Double quantity=(double) row.getProperty("FRITANGA");
			return formatValueIfAmount(new Amount(security.getFaceValue()
					* quantity));
		} else if (columnName.equals(DIRTY_PRICE)) {
			return formatValueIfAmount(getDirtyPrice(row));
		} else if (columnName.equals(STATUS)) {

			if ("FAILED".equals(inventory.getPositionType())) {
				return "In Transit";
			} else if ("ACTUAL".equals(inventory.getPositionType())) {
				return "Held";
			} else {
				return inventory.getPositionType();
			}
		} else if (columnName.equals(MATURITY_DATE)) {
			return this.boSecurityPositionStyle.getColumnValue(row, columnName, errors);
		} else if (columnName.equals(FRECUENCIA_CUPON)) {
			return this.boSecurityPositionStyle.getColumnValue(row, "Coupon Frequency", errors);
		} else if (columnName.equals(CORTE_CUPON)) {
			return this.boSecurityPositionStyle.getColumnValue(row, "Next Coupon Date", errors);
		}
		// else if (columnName.equals(AGREEMENT_NAME)) {
		// if (marginCallAgreement != null) {
		// return marginCallAgreement.getName();
		// } else {
		// return "";
		// }
		// } else if (columnName.equals(AGREEMENT_DESCRIPTION)) {
		// if (marginCallAgreement != null) {
		// return marginCallAgreement.getDescription();
		// } else {
		// return "";
		// }
		// }
		else {
			return this.boSecurityPositionStyle.getColumnValue(row, columnName, errors);
		}

	}

	@SuppressWarnings("unchecked")
	private Amount getDirtyPrice(ReportRow row) {
		Amount value = new Amount();

		final InventorySecurityPosition inventory = (InventorySecurityPosition) row.getProperty("Default");

		try {
			String quoteName = (String) this.boSecurityPositionStyle.getColumnValue(row, "Quote Name",
					new Vector<String>());
			// AAP MIG 14.4 In v12 inventory.getPositionDate is D and in v14
			// it's D-1
			Vector<QuoteValue> quotes = DSConnection.getDefault().getRemoteMarketData()
					.getQuoteValues(inventory.getPositionDate(), "DirtyPrice");

			for (QuoteValue quote : quotes) {
				if (quote.getName().equals(quoteName) && quote.getQuoteType().equals("DirtyPrice")) {
					value.set(quote.getClose());
				}
			}
		} catch (RemoteException e) {
			Log.error("Coudn't get the quotes", e.getCause());
			Log.error(this, e); //sonar
		}

		value.set(value.get() * 100.0);
		return value;
	}

	private Object getDirection(final double faceAmount) {
		String result = "";

		if (faceAmount < 0) {
			result = SantFiPositionReportStyle.SELL;
		} else {
			result = SantFiPositionReportStyle.BUY;
		}
		return result;
	}

	/**
	 * Format the values to retrieve the data in the specified format. 2
	 * decimals and no separator in thousands
	 * 
	 * @param value
	 *            value to format
	 * @return value formatted
	 */
	private String formatValueIfAmount(final Object value) {
		if (value instanceof Amount) {
			final NumberFormat numberFormatter = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.ENGLISH));

			final String numberString = numberFormatter.format(((Amount) value).get());

			return numberString;
		}

		return value.toString();
	}

	@Override
	public TreeList getTreeList() {
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();

		treeList.add(this.boSecurityPositionStyle.getTreeList());

		treeList.add("SantFiPosition", PROCESS_DATE);
		treeList.add("SantFiPosition", PORTFOLIO);
		treeList.add("SantFiPosition", COUNTERPARTY);
		treeList.add("SantFiPosition", COUNTERPARTY_NAME);
		treeList.add("SantFiPosition", BUY_SELL);
		treeList.add("SantFiPosition", BOND);
		treeList.add("SantFiPosition", BOND_NAME);
		treeList.add("SantFiPosition", BOSecurityPositionReportStyle.CURRENCY);
		treeList.add("SantFiPosition", FACE_AMOUNT);
		treeList.add("SantFiPosition", STATUS);
		treeList.add("SantFiPosition", DIRTY_PRICE);
		treeList.add("SantFiPosition", BOSecurityPositionReportStyle.MATURITY_DATE);
		treeList.add("SantFiPosition", FRECUENCIA_CUPON);
		treeList.add("SantFiPosition", CORTE_CUPON);
		// treeList.add("SantFiPosition", AGREEMENT_NAME);
		// treeList.add("SantFiPosition", AGREEMENT_DESCRIPTION);

		return treeList;
	}
}
