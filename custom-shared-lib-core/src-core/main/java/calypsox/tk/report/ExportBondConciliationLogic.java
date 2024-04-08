package calypsox.tk.report;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.TreeMap;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Rate;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.product.PoolFactorEntry;
import com.calypso.tk.service.DSConnection;

/**
 * Class with the necessary logic to retrieve the different values for the Bonds Conciliation report.
 * 
 * @author Juan Angel Torija
 */
public class ExportBondConciliationLogic {

	public static final String YES = "Yes";
	public static final String NO = "No";
	public static final String NO_CHANGE = "NO CHANGE";
	public static final String BLANK = "";

	public ExportBondConciliationLogic() {
	}

	public String getRateIndexFactor(final Bond bond) {

		if (bond.getFixedB() == false) {
			return String.valueOf(bond.getIndexFactor());
		} else if ((bond.getFixedB() == true) && (bond.getFlipperDate() != null)) {
			if (Math.abs(bond.getIndexFactor()) >= 0.00000) {
				return String.valueOf(bond.getIndexFactor() / 10);
			} else {
				return "";
			}
		} else {
			return "";
		}

	}

	public Amount getSpread(final Bond bond) {

		if (Math.abs(bond.getRateIndexSpread()) >= 0.000000) {
			String spreadS = String.valueOf(bond.getRateIndexSpread());
			BigDecimal spread = new BigDecimal(spreadS);
			BigDecimal newSpread = spread.multiply(new BigDecimal("10000.0"));
			return new Amount(newSpread.doubleValue());
		} else {
			return new Amount();
		}
	}
	
	public Rate getRate(final Bond bond){
		return new Rate(bond.getCoupon());
	}
	
	public Amount getTotalIssued(final Bond bond){
		 return new Amount(bond.getTotalIssued());
	}

	public String getPaymentRule(final Bond bond) {

		return bond.getCouponPeriodRule().toString();

	}

	public String getRedemptionCurrency(final Bond bond) {

		return bond.getRedemCurrency();

	}

	public String getResetHolidays(final Bond bond) {

		return bond.getResetHolidays().toString();

	}

	public String getRedemptionPrice(final Bond bond) {

		double redemptionPrice = 0;
		if (Math.abs(bond.getRedemptionPrice()) < 0.00000) {
			redemptionPrice = redemptionPrice * 100;
		}
		return String.valueOf(redemptionPrice);

	}

	public String getMinPurchaseAmount(final Bond bond) {

		return String.valueOf(bond.getMinPurchaseAmt());

	}

	public String getSettleDays(final Bond bond) {

		return String.valueOf(bond.getSettleDays());

	}

	public String getCuponCurrency(final Bond bond) {

		return bond.getCouponCurrency();

	}

	public String getResetDays(final Bond bond) {

		return String.valueOf(bond.getResetDays());

	}

	public String getResetBusLag(final Bond bond) {

		return String.valueOf(bond.getResetBusLagB());

	}

	public String getResetInArrear(final Bond bond) {

		return String.valueOf(bond.getResetInArrearB());

	}

	public String getPaymentLag(final Bond bond) {

		return String.valueOf(bond.getResetSamplingCutOffLag());

	}

	public String getFrecuency(final Bond bond) {

		return bond.getCouponFrequency().toString();

	}

	public String getRecordDays(final Bond bond) {

		if (bond.getRecordDays() > 0) {
			return String.valueOf(bond.getRecordDays());
		} else {
			return "";
		}

	}

	public String getDayCount(final Bond bond) {

		return bond.getDaycount().toString();
	}

	public BondAssetBacked getPoolFactorEfectiveDate(final Bond bond) {
		//JRL & JTD 11/04/2016 Migration 14.4
		if(bond.getSecCodes()!=null && bond.getSecCodes().get("ISIN") != null){
		BondAssetBacked abs = new BondAssetBacked();
			Product p = BOCache.getExchangeTradedProductByKey(DSConnection.getDefault(), "ISIN", bond.getSecCodes().get("ISIN").toString());
			if ((p != null) && (p instanceof BondAssetBacked)) {
				abs = (BondAssetBacked) p;
				return abs;
			}
		}
		return null;
	}

	public String getPoolFactorKnowDate(final Bond bond) {

		return bond.getDaycount().toString();
	}

	/**
	 * Method used to add the rows in the report generated.
	 * 
	 * @param bond
	 *            Bond.
	 * @param dsConn
	 *            Database connection.
	 * @param errorMsgs
	 *            Vector with the different errors occurred.
	 * @return Vector with the rows added.
	 */
	public static Vector<ExportBondConciliationItem> getReportRows(final Bond bond, final String date,
			final DSConnection dsConn, final Vector<String> errorMsgs) {

		final Vector<ExportBondConciliationItem> reportRows = new Vector<ExportBondConciliationItem>();
		final ExportBondConciliationLogic verifiedRow = new ExportBondConciliationLogic();
		ExportBondConciliationItem rowCreated = null;

		rowCreated = verifiedRow.getExportBondConciliationItem(bond, dsConn, errorMsgs);
		if (null != rowCreated) { // If the result row is equals to NULL, we
			// don't add this row to the report.
			reportRows.add(rowCreated);
		}

		return reportRows;
	}

	/**
	 * Method that retrieve row by row from Calypso, to insert in the vector with the result to show.
	 * 
	 * @param bond
	 *            Bond.
	 * @param dsConn
	 *            Database connection.
	 * @param errors
	 *            Vector with the different errors occurred.
	 * @return The row retrieved from the system, with the necessary information.
	 * @throws RemoteException
	 */
	@SuppressWarnings("rawtypes")
	private ExportBondConciliationItem getExportBondConciliationItem(final Bond bond, final DSConnection dsConn,
			final Vector<String> errors) {

		final ExportBondConciliationItem exp_BondConciliationItem = new ExportBondConciliationItem();
		BondAssetBacked abs = new BondAssetBacked();
		TreeMap poolFactor;
		// Set values
		if (bond != null) {

			exp_BondConciliationItem.setSpread(getSpread(bond));
			exp_BondConciliationItem.setRate(getRate(bond));
			exp_BondConciliationItem.setTotalIssued(getTotalIssued(bond));
			exp_BondConciliationItem.setRateIndexFactor(getRateIndexFactor(bond));
			exp_BondConciliationItem.setPaymentRule(getPaymentRule(bond));
			exp_BondConciliationItem.setRedemptionCurrency(getRedemptionCurrency(bond));
			exp_BondConciliationItem.setResetHolidays(getResetHolidays(bond));
			exp_BondConciliationItem.setRedemptionPrice(getRedemptionPrice(bond));
			exp_BondConciliationItem.setMinPurchaseAmount(getMinPurchaseAmount(bond));
			exp_BondConciliationItem.setSettleDays(getSettleDays(bond));
			exp_BondConciliationItem.setCuponCurrency(getCuponCurrency(bond));
			exp_BondConciliationItem.setResetDays(getResetDays(bond));
			exp_BondConciliationItem.setResetBusLag(getResetBusLag(bond));
			exp_BondConciliationItem.setResetInArrear(getResetInArrear(bond));
			exp_BondConciliationItem.setPaymentLag(getPaymentLag(bond));
			exp_BondConciliationItem.setFrecuency(getFrecuency(bond));
			exp_BondConciliationItem.setRecordDays(getRecordDays(bond));
			exp_BondConciliationItem.setDayCount(getDayCount(bond));
			abs = getPoolFactorEfectiveDate(bond);
			if (abs != null) {
				poolFactor = abs.getPoolFactorSchedule();
				if (poolFactor.lastEntry() != null) {
					PoolFactorEntry poolFactorEntry = (PoolFactorEntry) poolFactor.lastEntry().getValue();
					poolFactorEntry.getPoolFactor();
					exp_BondConciliationItem.setPoolFactorEfectiveDate(String.valueOf(poolFactorEntry
							.getEffectiveDate()));
				}
			}
			if (bond.getFlipperDate() == null) {
				exp_BondConciliationItem.setFlipper(NO);
				exp_BondConciliationItem.setFlipperDate(BLANK);
				exp_BondConciliationItem.setFlipperFrequency(BLANK);
			} else {
				exp_BondConciliationItem.setFlipper(YES);
				exp_BondConciliationItem.setFlipperDate(bond.getFlipperDate().toString());
				if (bond.getFlipperFrequency() == null) {
					exp_BondConciliationItem.setFlipperFrequency(NO_CHANGE);
				} else {
					exp_BondConciliationItem.setFlipperFrequency(bond.getFlipperFrequency().toString());
				}
			}

			return exp_BondConciliationItem;
		}

		return null;
	}

}
