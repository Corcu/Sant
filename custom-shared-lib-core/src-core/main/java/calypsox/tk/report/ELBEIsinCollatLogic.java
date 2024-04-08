package calypsox.tk.report;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.CollateralManagerUtil;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

/**
 * Class with the necessary logic to retrieve the different values for the
 * ELBEAgreements report.
 * 
 * @author David Porras Martinez
 */
public class ELBEIsinCollatLogic {

	private static final String COD_LAYOUT = "050";
	private static final String SOURCE_APP = "015";
	private static final String BLANK = "";
	private static final String ISIN = "ISIN";
	
	private static final String CONTRACT_TYPE_CSD = "CSD";
	private static final String CONTRACT_VALUE_IM = "IM";
	private static final String CONTRACT_VALUE_VM = "VM";

	private static String fechaExt;
	private static JDate fechaOp;
	private static boolean old;

	public ELBEIsinCollatLogic() {
	}

	@SuppressWarnings("unused")
	private ELBEAgreementsExtractionItem getELBEAgreementsExtractionItem(
			final Vector<String> errors) {
		return null;
	}

	public String getCodLayout() {
		return COD_LAYOUT;
	}

	public String getSourceApp() {
		return SOURCE_APP;
	}

	public String getClaveColat(final CollateralConfig marginCall) {
		return marginCall.getName();
	}

	// GSM: 08/09/2014. Accept equities
	public String getIsinTitulo(final Product product) {

		if (product instanceof Bond) {

			final Bond bond = (Bond) product;
			if (bond != null) {
				return bond.getSecCode(ISIN);
			}

		} else if (product instanceof Equity) {

			final Equity equity = (Equity) product;
			if (equity != null) {
				return equity.getSecCode(ISIN);
			}
		} else {
			Log.error(this, "Product not accepted, id " + product.getId());
		}

		return BLANK;
	}

	// GSM: 04/07/14. Added Asset type for MMOO
	/**
	 * @param secPos
	 * @return B if product underlying is Bond, E if is Equity
	 */
	private String getAssetType(final Product product) {

		if (product == null) {
			return "";
		}

		if (product.getProductFamily().equalsIgnoreCase(Product.BOND)) {
			return "B";
		} else if (product.getProductFamily().equalsIgnoreCase(Product.EQUITY)) {
			return "E";
		}
		return "";
	}

	public String getSenal(final CollateralConfig marginCall,
			final DSConnection dsConn, final PricingEnv pricingEnv) {

		final double balance = getBalanceStockCCY(marginCall, dsConn,
				pricingEnv);
		if (balance >= 0) {
			return "C";
		}
		if (balance < 0) {
			return "P";
		}
		return BLANK;
	}

	public String getMonedaBase(final CollateralConfig marginCall) {
		return marginCall.getCurrency();

	}

	public String getDivisa(final InventorySecurityPosition secPos) {
		final Bond bond = (Bond) secPos.getProduct();
		if (bond != null) {
			return bond.getCurrency();
		}
		return "";

	}

	public double getGrossExpoDivisa(final CollateralConfig marginCall,
			final DSConnection dsConn) {
		final List<Integer> mccID = new ArrayList<Integer>();
		mccID.add(marginCall.getId());
		double total = 0.00;
		try {
			// if there was any movement, we have one entry per contract/date
			final List<MarginCallEntryDTO> entries = CollateralManagerUtil
					.loadMarginCallEntriesDTO(mccID, fechaOp);

			if ((entries != null) && (entries.size() > 0)) {
				total = entries.get(0).getNetBalance();
			}

		} catch (final RemoteException e) {
			Log.error(this, "Cannot get marginCallEntry for the contract", e);
			// e.printStackTrace();
		}
		return total;

	}

	/**
	 * Retrieve date converted in dd/mm/yyyy format.
	 * 
	 * @param date
	 *            String with the date, format String with the previous format
	 * @return String with the date in ddMMyyyy fomat.
	 */
	public static String changeFormatDate(final String date,
			final String prevFormat) {

		final SimpleDateFormat sdf = new SimpleDateFormat(prevFormat);
		Date d = null;
		try {
			d = sdf.parse(date);
		} catch (final ParseException e) {
			Log.error(ELBEIsinCollatLogic.class, e); //sonar
		}
		final SimpleDateFormat sdf2 = new SimpleDateFormat("ddMMyyyy");
		final String stringDate = sdf2.format(d);

		return stringDate;//

	}

	public double getBalanceStockCCY(final CollateralConfig marginCall,
			final DSConnection dsConn, final PricingEnv pricingEnv) {
		final List<Integer> mccID = new ArrayList<Integer>();
		mccID.add(marginCall.getId());
		double total = 0.00;
		try {
			//   if there was any movement, we have one entry per contract/date
			final List<MarginCallEntryDTO> entries = CollateralManagerUtil
					.loadMarginCallEntriesDTO(mccID, fechaOp);

			if ((entries != null) && (entries.size() > 0)) {
				total = entries.get(0).getPreviousSecurityMargin();
			}

		} catch (final RemoteException e) {
			Log.error(this, "Cannot get marginCallEntry for the contract", e);
		}
		return total;

	}

	/**
	 * Method used to add the rows in the report generated.
	 * 
	 * @param trade
	 *            Trade associated with the Repo object.
	 * @param dsConn
	 *            Database connection.
	 * @param errorMsgs
	 *            Vector with the different errors occurred.
	 * @return Vector with the rows added.
	 */
	public static Vector<ELBEIsinCollatItem> getReportRows(
			final CollateralConfig marginCall,
			final ELBEIsinCollatPositionItem secPos, final DSConnection dsConn,
			final PricingEnv pricingEnv, final String date, final JDate date2,
			final Vector<String> errorMsgs, boolean oldway) {
		final Vector<ELBEIsinCollatItem> reportRows = new Vector<ELBEIsinCollatItem>();
		final ELBEIsinCollatLogic verifiedRow = new ELBEIsinCollatLogic();
		ELBEIsinCollatItem rowCreated = null;
		fechaExt = date;
		fechaOp = date2;
		old = oldway;
		
		
		
		rowCreated = verifiedRow.getELBEIsinCollatItem(marginCall, secPos,
				dsConn, pricingEnv, errorMsgs, old);
		
		if (null != rowCreated) { // If the result row is equals to NULL, we
			// don't add this row to the report.
			reportRows.add(rowCreated);
		}

		return reportRows;
	}
	
	/**
	 * Method that retrieve row by row from Calypso, to insert in the vector
	 * with the result to show.
	 * 
	 * @param trade
	 *            Trade associated with the Repo object.
	 * @param dsConn
	 *            Database connection.
	 * @param errors
	 *            Vector with the different errors occurred.
	 * @return The row retrieved from the system, with the necessary
	 *         information.
	 */
	private ELBEIsinCollatItem getELBEIsinCollatItem(
			final CollateralConfig marginCall,
			final ELBEIsinCollatPositionItem secPos, final DSConnection dsConn,
			final PricingEnv pricingEnv, final Vector<String> errors, boolean old) {
		final ELBEIsinCollatItem elbeIsinCollatItem = new ELBEIsinCollatItem();

		// cod layout
		elbeIsinCollatItem.setCodLayout(getCodLayout());

		// extract date
		elbeIsinCollatItem.setExtractDate(fechaExt);

		// operation date
		// COL_OUT_019
		// Date format fixed
		elbeIsinCollatItem.setPosTransDate(changeFormatDate(fechaOp.toString(),
				"dd/MM/yyyy"));

		// source app
		elbeIsinCollatItem.setSourceApp(getSourceApp());

		// frontID - blank
		elbeIsinCollatItem.setFrontId(BLANK);

		// claveColat;
		elbeIsinCollatItem.setClaveColat(getClaveColat(marginCall));

		// isinTitulo;
		elbeIsinCollatItem.setIsinTitulo(getIsinTitulo(secPos.getProduct()));

		// senal;
		elbeIsinCollatItem.setSenal(getSenal(marginCall, dsConn, pricingEnv));

		// balanTitulosDivisa;
		
		if (old) {
		elbeIsinCollatItem.setBalanTitulosDivisa(CollateralUtilities
				.convertToReportDecimalFormat(
						CollateralUtilities.formatNumber(secPos.getValue()), ",",
						"."));
		}else {
			elbeIsinCollatItem.setBalanTitulosDivisa(CollateralUtilities
					.convertToReportDecimalFormat(
							CollateralUtilities.formatNumber((secPos.getBalanceTitulo())), ",",
							"."));
			
		}
		// monedaBase;
		elbeIsinCollatItem.setMonedaBase(getMonedaBase(marginCall));

		// gross exposure divisa
		elbeIsinCollatItem.setGrossExpoDivisa(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities
						.formatNumber(getGrossExpoDivisa(marginCall, dsConn)),
						",", "."));

		// divisa del ISIN
		if(old) {
			elbeIsinCollatItem.setDivisa(secPos.getCurrency());
		}else {
			elbeIsinCollatItem.setDivisa(getMonedaBase(marginCall));
		}

		// GSM: 04/07/14. Added Asset type for MMOO
		// Tipo de security
		elbeIsinCollatItem.setAssetType(getAssetType(secPos.getProduct()));

		// Margin Type. May 2016
		elbeIsinCollatItem.setMarginType(getMarginType(marginCall));
		
		// CounterParty (legalEntity) short name
		elbeIsinCollatItem.setLegalEntityShortName( marginCall.getLegalEntity().getAuthName() );
		
		return elbeIsinCollatItem;
	}
	
	private String getMarginType(final CollateralConfig marginCall) {

		if (marginCall == null) {
			return "";
		}

		String contractType = marginCall.getContractType();
		
		if(contractType.equalsIgnoreCase(CONTRACT_TYPE_CSD)) {
			return CONTRACT_VALUE_IM;
		} else {
			return CONTRACT_VALUE_VM;
		}
		
	}

}
