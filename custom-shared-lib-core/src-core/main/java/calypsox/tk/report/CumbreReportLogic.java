package calypsox.tk.report;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.report.BOPositionReport;
import org.jfree.util.Log;

import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import calypsox.util.collateral.CollateralUtilities;

import static calypsox.tk.report.BOSecurityPositionReportTemplate.NOMINAL_PROPERTY;

public class CumbreReportLogic {

	private static final String ISIN = "ISIN";
	private static final String BALANCE_BRANCH = "000001";
	private static final String MOVEMENT_BRANCH = "0001";
	private static final String DATE_FORMAT = "dd/MM/yyyy";
	private static final String MOV_DATE_FORMAT = "1YYMMdd";
	private static final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
	private static final String FILE_IDENTIFIER = "02";
	private static final String EUR = "EUR";
	private static final String TESOR = "TESOR";
	private static final String ZERO = "0";
	private static final String SIGOMLIQ = "SIGOMLIQ  ";
	private static final String NOMINAL = "Nominal";

	private static final String DV_CUMBRE_ACCOUNTS = "CumbreAccounts";
	private static final String DV_FIRST_LINE_ACCOUNT = "FIRST_LINE_ACCOUNT";
	private static final String DV_SECOND_LINE_ACCOUNT = "SECOND_LINE_ACCOUNT";
	
	private static String firstLineAccount = "";
	private static String secondLineAccount = "";
	
	private static boolean initAccount = true;
	
	private CumbreReportLogic() {		// Sonar: Add a private constructor to hide the implicit public one in utility classes (classes which are collections of static members).
	}
	
	/**
	 * @return process date of the report with format DD/MM/YYYY
	 */
	protected static String getBalanceProcessDate(final JDate valDate) {
		return formatter.format(valDate.getDate(TimeZone.getDefault()));
	}
	
	protected static String getMovementProcessDate(final JDate valDate){
		StringBuilder processDate = new StringBuilder(formatter.format(valDate.getDate(TimeZone.getDefault())));
		processDate.append(appendChar(ZERO, 25));
		return processDate.toString();
	}
	
	public static String getValueDate(final JDate valDate){
		SimpleDateFormat valDateFormat = new SimpleDateFormat(MOV_DATE_FORMAT);
		return valDateFormat.format(valDate.getDate(TimeZone.getDefault()));
	}

	protected static String getFileIdentifier(){
		return FILE_IDENTIFIER;
	}
	
	/**
	 * @return always '000001'
	 */
	protected static String getBalanceBranchNumber() {
		return BALANCE_BRANCH;
	}
	
	/**
	 * @return ALWAYS '0001'
	 */
	protected static String getMovementBranchNumber() {
		return MOVEMENT_BRANCH;
	}
	
	protected static String getSigomliq(){
		return SIGOMLIQ;
	}
	

	/**
	 * @return always EUR
	 */
	protected static String getAccountCcyCode(){		// creo que no es necesario este metodo
		return EUR;
	}
	
	/**
	 * @return TESOR + 15 zeros
	 */
	protected static String getTesorWithZeros(boolean addSpaces){
		StringBuilder tesor = new StringBuilder(TESOR);
		tesor.append(appendChar(ZERO, 15));
		if(addSpaces){
			tesor.append(appendChar(" ", 15));
		}
		return tesor.toString();
	}
	
	/**
	 * @param contract
	 * @return
	 */
	protected static String getCptyShortName(final MarginCallConfig contract, final boolean addZeros) {
		StringBuilder cpty = new StringBuilder();
		String cptyShortName = contract.getLegalEntity().getCode();
		if (addZeros) {
			cpty.append(appendChar(ZERO, 10 - cptyShortName.length()));
		}
		cpty.append(cptyShortName);
		return cpty.toString();
	}

	/**
	 * @return account number
	 */
	protected static String getAccountNumber(boolean isFirstLine) {
		if(initAccount) {
			firstLineAccount = LocalCache.getDomainValueComment(DSConnection.getDefault(),DV_CUMBRE_ACCOUNTS, DV_FIRST_LINE_ACCOUNT);
			secondLineAccount = LocalCache.getDomainValueComment(DSConnection.getDefault(),DV_CUMBRE_ACCOUNTS, DV_SECOND_LINE_ACCOUNT);
			initAccount = false;
		}

		if(firstLineAccount!=null && isFirstLine) {
			return firstLineAccount;
		}else if(secondLineAccount!=null && !isFirstLine) {
			return secondLineAccount;
		}
		
		return "";
	}
	
	protected static String getAccountNumberDOMM(String accountNumber){
		String number = "0000000";
		String zeros = "00000000000000000000000";
		return accountNumber != null ? zeros+accountNumber : zeros+number;
	}

	protected static String getBalanceReference(final MarginCallConfig contract, final Inventory inventory, int totalLength) {
	StringBuilder balanceRef = new StringBuilder(getCptyShortName(contract, false));
	balanceRef.append(getISIN(inventory));
	balanceRef.append(getBalanceCcy(inventory));
	String valueCutTo20 = (balanceRef.length() > 20) 
							? balanceRef.substring(0, 20)		// start index is inclusive, end index is exclusive
							: balanceRef.toString();
	return appendChar(ZERO, totalLength - valueCutTo20.length()).concat(valueCutTo20);
}

	/**
	 * @param inventory
	 * @return balance currency
	 */
	protected static String getBalanceCcy(final Inventory inventory) {
		return inventory.getSettleCurrency();
	}

	protected static Double getNominal(final ReportRow row, Inventory inventory) {

			Map positions = row.getProperty(BOPositionReport.POSITIONS);
			BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");
			if (inventory instanceof InventorySecurityPosition && positions != null && context != null) {
				Vector<InventorySecurityPosition> datedPositions = (Vector<InventorySecurityPosition>) positions.get(context.endDate);
				if (!com.calypso.tk.core.Util.isEmpty(datedPositions)) {
					return InventorySecurityPosition.getTotalSecurity(datedPositions, BOSecurityPositionReport.BALANCE);
				}
			}
			return null;
		}

	protected static double getBalanceAmount(Inventory inventory, JDate valDate, PricingEnv pricingEnv, ReportRow row) {

		double amount = getNominal(row, inventory);
		if (inventory instanceof InventorySecurityPosition) {
			Product product = ((InventorySecurityPosition) inventory).getProduct();
			double dirtyPrice = getDirtyPriceCumbre(product, valDate, pricingEnv);
			
			if (product instanceof Bond) {
				amount = amount * ((Bond) product).getFaceValue() * dirtyPrice / 100;
			}else if(product instanceof BondAssetBacked) {
				amount = amount * ((Bond) product).getPoolFactor(valDate) * ((Bond) product).getFaceValue() * dirtyPrice / 100;
			}
			
			else if (product instanceof Equity) {
				amount = amount * getDirtyPriceCumbre(product, valDate, pricingEnv);
			}
		}

		return amount;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static double getDirtyPriceCumbre(Product product, JDate valDate, PricingEnv pricingEnv) {
		if ( product == null || pricingEnv == null || "".equals(pricingEnv.getQuoteSetName()) )
			return 0.00;				// to avoid Sonar warning on method complexity
		
		String isin = product.getSecCode(ISIN);
		String quoteName;
		String quotesetName = pricingEnv.getQuoteSetName();

		try {
			quoteName = CollateralUtilities.getQuoteNameFromISIN(isin, valDate);

			if (!Util.isEmpty(quoteName)) {
				Vector<QuoteValue> vQuotes;
				if (product instanceof Bond) {
					String clausule = "quote_name = " + "'" + quoteName + "' AND trunc(quote_date) = "
							+ Util.date2SQLString(valDate) + " AND quote_set_name = '" + quotesetName + "'";
					vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
					if ( (vQuotes != null) && (!vQuotes.isEmpty()) && !Double.isNaN(vQuotes.get(0).getClose()) )
						return vQuotes.get(0).getClose() * 100;
				} else if (product instanceof Equity) {
					String clausule = "quote_name = " + "'" + quoteName + "' AND trunc(quote_date) = "
							+ Util.date2SQLString(valDate) + " AND quote_set_name = 'OFFICIAL'";
					vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
					if ( (vQuotes != null) && (!vQuotes.isEmpty()) && !Double.isNaN(vQuotes.get(0).getClose()) )
						return vQuotes.get(0).getClose();
				}
			}
		} catch (RemoteException e) {
			Log.error("Cannot retrieve dirty price", e);
		}

		return 0.00;
	}

	protected static String getConvertedAmount(final Inventory inventory, double balance, JDate valDate,
			PricingEnv pricingEnv, int length, boolean addSign) {
		return getConvertedAmount(inventory, balance, valDate, pricingEnv, length, addSign, null);
	}
	
	protected static String getConvertedAmount(final Inventory inventory, double balance, JDate valDate,
			PricingEnv pricingEnv, int length, boolean addSign, Integer numOfDecimals)  {
		String balanceCcy = inventory.getSettleCurrency();
		double convertedAmount = balance;
		
		// Get OFFICIAL quoteSet
		QuoteSet officialQuoteSet = null;
		try {
			officialQuoteSet = DSConnection.getDefault().getRemoteMarketData().getQuoteSet("OFFICIAL");
		}
		catch (CalypsoServiceException e) {
			Log.error(CumbreReportLogic.class, e);
		}
		
		// Convert value
		if (officialQuoteSet != null) {
			QuoteSet originalQuoteSet = pricingEnv.getQuoteSet();
			pricingEnv.setQuoteSet(officialQuoteSet);				// set OFFICIAL quoteSet in pricingEnv

			if ("EUR".equalsIgnoreCase(balanceCcy)) {
				// convertedAmount
			} else {
				try {
					convertedAmount = CollateralUtilities.convertCurrency(balanceCcy, balance, "EUR", valDate, pricingEnv);
				} catch (MarketDataException e) {
					Log.error(CumbreReportLogic.class, e);
				}
			}
			pricingEnv.setQuoteSet(originalQuoteSet);			// recover original quoteSet
		}
		
		return formatDoubleToString(length, convertedAmount, addSign, numOfDecimals);
	}

	protected static String getAdditionalInfo() {
		return appendChar("0", 35);
	}

	public static String formatDoubleToString( int totalLength, double amount, boolean addSign) {
		return formatDoubleToString(totalLength, amount, addSign, null);
	}

	public static String formatDoubleToString( int totalLength, double amount, boolean addSign, Integer numOfDecimals) {
		double amountAbs = Math.abs(amount);
		String decimalsFormatter = String.format("%%.%df", numOfDecimals);		// = "%." + numOfDecimals + "f"     We format the formatter to avoid Sonar warning: Format specifiers should be used instead of string concatenation.
		String balance = (numOfDecimals == null) 								// convert to string, formatting decimals
				 ? String.valueOf(amountAbs)
				 : String.format(Locale.ROOT, decimalsFormatter, amountAbs);	// "locale.ROOT" to force "." instead of "," as decimal separator
		StringBuilder retVal = new StringBuilder();
		if (addSign) {															// first, add sign, if requested
			retVal.append(getSignOfAmount(amount));
			totalLength--;
		}
		retVal.append(appendChar("0", totalLength - balance.length()));			// add zeros to fill length
		retVal.append(balance);													// add number with decimals

		return retVal.toString();
	}
	
	private static String getSignOfAmount(double amount){
		String sign = "-";
		if(amount >= 0){
			sign = "+";
		}
		return sign;
	}

	/**
	 * @param inventory
	 * @return ISIN value if the position is of the Security type, otherwise
	 *         returns "_"
	 */
	private static final String getISIN(final Inventory inventory) {
		String isin = "_";
		if (inventory instanceof InventorySecurityPosition) {
			isin = inventory.getProduct().getSecCode(ISIN);
		}
		return isin;

	}

	/**
	 * Generate string filled with 'character' with indicated length
	 * @param zerosNumber
	 * @return String with zeros
	 */
	public static String appendChar(String character, int length) {
		StringBuilder zerosBuilder = new StringBuilder();
		if (length > 0) {
			for (int i = 0; i < length; i++) {
				zerosBuilder.append(character);
			}
		}
		return zerosBuilder.toString();
	}
	
}
