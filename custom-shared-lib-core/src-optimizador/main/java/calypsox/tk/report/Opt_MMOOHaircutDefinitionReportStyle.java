package calypsox.tk.report;

import static calypsox.tk.report.Opt_MMOOHaircutDefinitionReportTemplate.CURRENCY;
import static calypsox.tk.report.Opt_MMOOHaircutDefinitionReportTemplate.QUOTE;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.report.QuoteReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import calypsox.util.collateral.CollateralUtilities;

/**
 * Style for the report of MMOO Haircuts quotes
 * 
 * @author Guillermo Solano
 * @version 1.0
 *
 */
public class Opt_MMOOHaircutDefinitionReportStyle extends ReportStyle {


	/**
	 * Constants
	 */
	private static final long serialVersionUID = 8391747494595306953L;
	public static final String DATE = "Fecha";
	public static final String QUOTE_SET = "Quote_Set";
	public static final String ISIN = "ISIN";
	public static final String HAIRCUT = "haircut";
	public static final String RAW_HAIRCUT = "Raw_Haircut";
	public static final String PERCENTAGE_HAIRCUT = "Percentage_Haircut";
	public static final String PRODUCT_CURRENCY = "Currency";

	/**
	 * Default columns
	 */
	public static final String[] DEFAULTS_COLUMNS = { DATE, QUOTE_SET, ISIN, HAIRCUT };

	/**
	 * Style main method
	 */
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, @SuppressWarnings("rawtypes") final Vector errors)
			throws InvalidParameterException {
		
		final QuoteValue quote = (QuoteValue) row .getProperty(QUOTE);
		final String isin = (String) row .getProperty(ISIN);
		final String ccy = (String) row .getProperty(CURRENCY);


		if (columnName.equals(DATE)) {
			return quote.getDate().toString();
			
		} else if (columnName.equals(QUOTE_SET)){			
			return quote.getQuoteSetName();
			
			
		} else if (columnName.equals(ISIN)) {
			return isin;
			
			
		} else if (columnName.equals(HAIRCUT)) {
			return CollateralUtilities.formatNumber(quote.getClose());
		
		
		} else if (columnName.equals(RAW_HAIRCUT)){
			return quote.getClose();
			
		} else if (columnName.equals(PERCENTAGE_HAIRCUT)){
			//JRL 20/04/2016 Migration 14.4
			return CollateralUtilities.formatNumber(Math.abs(quote.getClose())*100);
			
		} else if (columnName.equals(PRODUCT_CURRENCY)){
			return ccy;
		
		}
		final QuoteReportStyle quoteStyle = new QuoteReportStyle();
		return quoteStyle.getColumnValue(row, columnName, errors);
	}
			


	@SuppressWarnings("deprecation")
	@Override
	public TreeList getTreeList() {

		if (this._treeList != null) {
			return this._treeList;
		}
		final TreeList treeList = super.getTreeList();
		final QuoteReportStyle quoteStyle = new QuoteReportStyle();
		treeList.add(quoteStyle.getTreeList());
		return treeList;
	}
	

}
