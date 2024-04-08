package calypsox.tk.report;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

/**
 * Class with the necessary logic to retrieve the different values for the Repos report.
 * 
 * @author David Porras Mart?nez
 */
public class ELBEAssocDerivCollatLogic {

	private static final String BO_SYSTEM = "BO_SYSTEM";
	private static final String FO_SYSTEM = "FO_SYSTEM";
	private static final String CALYPSO = "CALYPSO";
	private static final String BO_REFERENCE = "BO_REFERENCE";
	private static final String COD_LAYOUT = "022";
	private static final String SOURCE_APP = "015";
	private static final String BLANK = "";
	private static final String DEFAULT_ID = "0";
	private static String fechaExt;
	private static JDate fechaOp;
	
	// MISAssocDerivCollat
	private static final String DV_MISASSOCDERIVCOLLAT_BOSYSTEM_VALUES = "MISAssocDerivCollat_BoSystemValues";
	// MISAssocDerivCollat - End

	public ELBEAssocDerivCollatLogic() {
	}

	public String getCodLayout() {
		return COD_LAYOUT;
	}

	public String getSourceApp() {
		return SOURCE_APP;
	}

	public String getFrontID(final Trade trade, final DSConnection dsConn) { // TODO
		String boSystem = trade.getKeywordValue(BO_SYSTEM);
		if (!Util.isEmpty(boSystem)) {
			if (!boSystem.equals(CALYPSO)) {
				return getBoReferenceFromTrade(trade);
			} else {
				return getExtReferenceFromTrade(trade);
			}
		}
		return DEFAULT_ID;
	}

	public String getCollatID(final Trade trade, final DSConnection dsConn) {
		// get marginCall contract
		CollateralConfig mcc = getContractFromTrade(trade);
		if (mcc != null) {
			return mcc.getName();
		}
		return BLANK;
	}

	public CollateralConfig getContractFromTrade(Trade trade) {
		String internalRef = trade.getInternalReference();
		if (!Util.isEmpty(internalRef)) {
			try {
				return ServiceRegistry.getDefault().getCollateralDataServer()
						.getMarginCallConfig(Integer.valueOf(internalRef));
			} catch (NumberFormatException e) {
				Log.error(this, e.getMessage(), e);
			} catch (RemoteException e) {
				Log.error(this, e.getMessage(), e);
			}
		}
		return null;
	}

	public String getBoReferenceFromTrade(Trade trade) {
		String boRef = trade.getKeywordValue(BO_REFERENCE);
		if (!Util.isEmpty(boRef)) {
			return boRef;
		} else {
			return DEFAULT_ID;
		}
	}

	public String getExtReferenceFromTrade(Trade trade) {
		String extRef = trade.getExternalReference();
		if (!Util.isEmpty(extRef)) {
			return trade.getKeywordValue(FO_SYSTEM) + extRef;
		} else {
			return DEFAULT_ID;
		}
	}

	/**
	 * Retrieve date converted in dd/mm/yyyy format.
	 * 
	 * @param date
	 *            String with the date, format String with the previous format
	 * @return String with the date in ddMMyyyy fomat.
	 */
	public static String convertDate(final String date, final String prevFormat) {

		final SimpleDateFormat sdf = new SimpleDateFormat(prevFormat);
		Date d = null;
		try {
			d = sdf.parse(date);
		} catch (final ParseException e) {
			Log.error(ELBEAssocDerivCollatLogic.class, e); //sonar
		}
		final SimpleDateFormat sdf2 = new SimpleDateFormat("ddMMyyyy");
		final String stringDate = sdf2.format(d);

		return stringDate;

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
	public static ELBEAssocDerivCollatItem getReportRows(final Trade trade, final String fecha,
			final JDate fecha2, final DSConnection dsConn, final Vector<String> errorMsgs) {
		final Vector<ELBEAssocDerivCollatItem> reportRows = new Vector<ELBEAssocDerivCollatItem>();

		final ELBEAssocDerivCollatLogic verifiedRow = new ELBEAssocDerivCollatLogic();
		ELBEAssocDerivCollatItem rowCreated = null;
		fechaExt = fecha;
		fechaOp = fecha2;

		return verifiedRow.getELBEAssocDerivCollatItem(trade, dsConn, errorMsgs);
	}

	/**
	 * Method that retrieve row by row from Calypso, to insert in the vector with the result to show.
	 * 
	 * @param trade
	 *            Trade associated with the Repo object.
	 * @param dsConn
	 *            Database connection.
	 * @param errors
	 *            Vector with the different errors occurred.
	 * @return The row retrieved from the system, with the necessary information.
	 */
	private ELBEAssocDerivCollatItem getELBEAssocDerivCollatItem(final Trade trade, final DSConnection dsConn,
			final Vector<String> errors) {
		final ELBEAssocDerivCollatItem elbeAssocDerivColatItem = new ELBEAssocDerivCollatItem();

		elbeAssocDerivColatItem.setCodLayout(getCodLayout());
		elbeAssocDerivColatItem.setExtractDate(fechaExt);
		elbeAssocDerivColatItem.setPosTransDate(convertDate(fechaOp.toString(), "dd/MM/yyyy"));
		elbeAssocDerivColatItem.setSourceApp(getSourceApp());
		elbeAssocDerivColatItem.setFrontID(CollateralUtilities.fillWithBlanks(getFrontID(trade, dsConn), 40));
		elbeAssocDerivColatItem.setCollatID(CollateralUtilities.fillWithBlanks(getCollatID(trade, dsConn), 30));

		// MISAssocDerivCollat
		elbeAssocDerivColatItem.setFrontIDMIS(CollateralUtilities.fillWithBlanks(getFrontID_MIS(trade, dsConn), 40));

		String bo_reference = trade.getKeywordValue("BO_REFERENCE");
		String murexTradeId = trade.getKeywordValue("MurexTradeID");
		if (!Util.isEmpty(bo_reference)){
			elbeAssocDerivColatItem.setBoReferenceCustom(bo_reference);
		} else if (!Util.isEmpty(murexTradeId)){
			elbeAssocDerivColatItem.setBoReferenceCustom(murexTradeId);
		}

		return elbeAssocDerivColatItem;
	}

	// MISAssocDerivCollat

	public String getFrontID_MIS(final Trade trade, final DSConnection dsConn) {
		ArrayList<String> listValues = new ArrayList<String>();
		boolean isMISBoSystem = false;
		String boSystem = trade.getKeywordValue(BO_SYSTEM);

		if (!Util.isEmpty(boSystem)) {

			// Get DV values MISAssocDerivCollat_BoSystemValues
			listValues = getMISBoSystemValuesFromDV(dsConn);

			isMISBoSystem = listValues.contains(boSystem);

			if (isMISBoSystem) {
				return getExtReferenceFromTrade(trade);
			} else {
				return getBoReferenceFromTrade(trade);
			}
		}
		return DEFAULT_ID;
	}

	/**
	 * Get values MISAssocDerivCollat BoSystem from DomainValues
	 * @param dsConn
	 * @return
	 */
	private ArrayList<String> getMISBoSystemValuesFromDV(final DSConnection dsConn) {
		ArrayList<String> list = new ArrayList<String>();
		Vector<String> domainValues = LocalCache.getDomainValues(dsConn,
				DV_MISASSOCDERIVCOLLAT_BOSYSTEM_VALUES);

		if (!Util.isEmpty(domainValues)) {
			list.addAll(domainValues);
		}

		return list;
	}

	// MISAssocDerivCollat - End
	
}
