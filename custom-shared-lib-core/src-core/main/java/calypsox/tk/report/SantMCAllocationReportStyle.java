package calypsox.tk.report;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.collateral.CashAllocationFacade;
import com.calypso.tk.collateral.MarginCallAllocationFacade;
import com.calypso.tk.collateral.SecurityAllocationFacade;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.core.DisplayValue;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Rate;
import com.calypso.tk.core.RoundingMethod;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.MarginCallAllocationBaseReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

/**
 * ReportStyle class for allocation list
 * 
 * @author various
 * @version 2.0
 * @date 28/06/2016
 * 
 */
@SuppressWarnings("rawtypes")
public class SantMCAllocationReportStyle extends MarginCallAllocationBaseReportStyle {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String POSITION_ACTION = "PARTY";
	public static final String VALUE_DATE = "VALUE DATE";
	public static final String ASSET = "ELIGIBLE COLLATERAL";
	public static final String CURRENCY = "ELIGIBLE CURRENCY";
	public static final String NOMINAL = "NOMINAL";
	public static final String UNIT_PRICE = "UNIT PRICE";
	public static final String MARKET_VALUE = "MARKET VALUE";
	public static final String VALUATION_PERCENTAGE = "VALUATION PERCENTAGE";
	public static final String ADJUSTED_VALUE = "ADJUSTED VALUE";
	
	// MarginCallMessageFormatter MEX
	public static final String POSITION_ACTION_MEX = "MOVIMIENTO";
	public static final String VALUE_DATE_MEX = "FECHA VALOR";
	public static final String ASSET_MEX = "GARANTIA";
	public static final String CURRENCY_MEX = "MONEDA";
	public static final String NOMINAL_MEX = "NOCIONAL";
	public static final String UNIT_PRICE_MEX = "PRECIO";
	public static final String MARKET_VALUE_MEX = "VALOR DE MERCADO";
	public static final String VALUATION_PERCENTAGE_MEX = "PORCENTAJE DE VALORACION";
	public static final String ADJUSTED_VALUE_MEX = "VALOR AJUSTADO";

	public static final String POSITION_ACTION_DELIVER = "DELIVERS";
	public static final String POSITION_ACTION_RECEIVE = "RECEIVES";
	public static final String PRODUCT_CODE_ISIN = "ISIN";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.report.MarginCallAllocationBaseReportStyle#getColumnValue
	 * (com.calypso.tk.collateral.SecurityAllocationFacade, java.lang.String, java.util.Vector)
	 */
	@Override
	public Object getColumnValue(SecurityAllocationFacade allocation, String columnName, Vector errors)
			throws InvalidParameterException {

		// get the Security product
		Product allocProduct = null;
		@SuppressWarnings("unused")
		Product entry = null;
		try {
			allocProduct = DSConnection.getDefault().getRemoteProduct().getProduct(allocation.getProductId());
			entry = DSConnection.getDefault().getRemoteProduct().getProduct(allocation.getProductId());

		} catch (RemoteException e) {
			Log.error(this, e);
			return "";
		}

		if (UNIT_PRICE.equals(columnName) || UNIT_PRICE_MEX.equals(columnName)) {
			DisplayValue displayValue = allocProduct.getPriceDisplayValue();
			if (displayValue != null) {
				// try {
				int numDec = 0;
				SecurityAllocationDTO secAlloc = (SecurityAllocationDTO) allocation;
				double dirtyPrice = secAlloc.getCleanPrice() + secAlloc.getAccrual();

				if (allocProduct instanceof Bond) {
					numDec = ((Bond) allocProduct).getPriceDecimals();
					if (numDec != 0) {
						numDec += 2;
						int accrDec = ((Bond) allocProduct).getAccrualRounding() + 2;
						if (accrDec > numDec) {
							numDec = accrDec;
						}
					}
				}

				numDec = (numDec == 0 ? Util.getBondPriceDecimals() + 2 : numDec);
				displayValue.set(RoundingMethod.roundNearest(dirtyPrice, numDec));
				// } catch (RemoteException e) {
				// Log.error(this, e);
				// return null;
				// }
			}
			return displayValue;

			// return super.getColumnValue(allocation,
			// MarginCallAllocationBaseReportStyle.MARKET_PRICE, errors);
			// if (allocProduct != null && allocProduct instanceof Bond) {
			// return new Amount(((Bond) allocProduct).getDirtyPriceBase(),
			// getRoundingUnit(allocation));
			// }
			// return "-";
		} else if (POSITION_ACTION.equals(columnName) || POSITION_ACTION_MEX.equals(columnName)) {
			CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
					allocation.getCollateralConfigId());
			String deliverReceive = (allocation.getQuantity() < 0 ? POSITION_ACTION_DELIVER : POSITION_ACTION_RECEIVE);
			return marginCallConfig.getProcessingOrg().getName() + " " + deliverReceive;
			// return (allocation.getQuantity() < 0 ? POSITION_ACTION_DELIVER :
			// POSITION_ACTION_RECEIVE);
		} else if (ASSET.equals(columnName) || ASSET_MEX.equals(columnName)) {
			String asset = "";
			if (allocProduct != null) {
				String isinCode = allocProduct.getSecCode(PRODUCT_CODE_ISIN);
				asset = allocProduct.getType() + (isinCode != null ? " - " + isinCode : "");
			}
			return asset;
		}
		return super.getColumnValue(allocation, columnName, errors);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.report.MarginCallAllocationBaseReportStyle#getColumnValue
	 * (com.calypso.tk.collateral.CashAllocationFacade, java.lang.String, java.util.Vector)
	 */
	@Override
	public Object getColumnValue(CashAllocationFacade allocation, String columnName, Vector errors)
			throws InvalidParameterException {
		if (UNIT_PRICE.equals(columnName) || UNIT_PRICE_MEX.equals(columnName)) {
			return super.getBaseColumnValue(allocation, MarginCallAllocationBaseReportStyle.FX_RATE, errors);
			// return super.getBaseColumnValue(allocation,
			// MarginCallAllocationBaseReportStyle.FX_RATE, errors);
		} else if (POSITION_ACTION.equals(columnName) || POSITION_ACTION_MEX.equals(columnName)) {
			CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
					allocation.getCollateralConfigId());
			String deliverReceive = (allocation.getPrincipal() < 0 ? POSITION_ACTION_DELIVER : POSITION_ACTION_RECEIVE);
			return marginCallConfig.getProcessingOrg().getName() + " " + deliverReceive;
			// return (allocation.getPrincipal() < 0 ? POSITION_ACTION_DELIVER :
			// POSITION_ACTION_RECEIVE);
		} else if (ASSET.equals(columnName) || ASSET_MEX.equals(columnName)) {
			return super.getColumnValue(allocation, MarginCallAllocationBaseReportStyle.UNDERLYING_TYPE, errors);
		}

		return super.getColumnValue(allocation, columnName, errors);
	}

	@Override
	public Object getColumnValue(MarginCallAllocationFacade allocation, String columnName, Vector errors)
			throws InvalidParameterException {

		if (VALUE_DATE.equals(columnName) || VALUE_DATE_MEX.equals(columnName)) {
			return super.getBaseColumnValue(allocation, MarginCallAllocationBaseReportStyle.SETTLEMENT_DATE, errors);
		} else if (CURRENCY.equals(columnName) || CURRENCY_MEX.equals(columnName)) {
			return super.getBaseColumnValue(allocation, MarginCallAllocationBaseReportStyle.CURRENCY, errors);
		} else if (NOMINAL.equals(columnName) || NOMINAL_MEX.equals(columnName)) {
			return super.getColumnValue(allocation, MarginCallAllocationBaseReportStyle.NOMINAL, errors);
		} else if (MARKET_VALUE.equals(columnName) || MARKET_VALUE_MEX.equals(columnName)) {
			// return super.getBaseColumnValue(allocation,
			// MarginCallAllocationBaseReportStyle.BASE_VALUE, errors);
			return super.getBaseColumnValue(allocation, MarginCallAllocationBaseReportStyle.VALUE, errors);
			// Amount price = (Amount) getColumnValue(allocation, UNIT_PRICE,
			// errors);
			// Amount nominal = (Amount) getColumnValue(allocation, NOMINAL,
			// errors);
			// Amount marketvalue = new Amount();
			// if (nominal != null && price != null) {
			// marketvalue = new Amount(price.get() * nominal.get() / 100,
			// getRoundingUnit(allocation));
			// }
			// return marketvalue;
		} else if (VALUATION_PERCENTAGE.equals(columnName) || VALUATION_PERCENTAGE_MEX.equals(columnName)) {
			// get the haircut vaule
			// TODO There is no logic applied regarding the type of the haircut: regular, reverse !!!!
			return new Rate(1 + allocation.getHaircut());
			// return super.getBaseColumnValue(allocation, MarginCallAllocationBaseReportStyle.HAIRCUT, errors);
		} else if (ADJUSTED_VALUE.equals(columnName) || ADJUSTED_VALUE_MEX.equals(columnName)) {
			return super.getBaseColumnValue(allocation, MarginCallAllocationBaseReportStyle.CONTRACT_VALUE, errors);
			// // TODO cache already calculated values
			// Amount marketValue = (Amount) getColumnValue(allocation,
			// MARKET_VALUE, errors);
			// Rate hairCut = (Rate) getColumnValue(allocation,
			// VALUATION_PERCENTAGE, errors);
			// Amount adjustedValue = new Amount();
			// if (marketValue != null && hairCut != null) {
			// adjustedValue = new Amount(marketValue.get() * (1 + hairCut.get()
			// / 100), getRoundingUnit(allocation));
			// }
			//
			// return adjustedValue;
		}
		return super.getColumnValue(allocation, columnName, errors);
	}

}
