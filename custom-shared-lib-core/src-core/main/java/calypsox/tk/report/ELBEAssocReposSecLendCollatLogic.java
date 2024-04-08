package calypsox.tk.report;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

/**
 * Class with the necessary logic to retrieve the different values for the Repos report.
 * 
 * @author David Porras Mart?nez
 */
public class ELBEAssocReposSecLendCollatLogic {

	private static final String MC_CONTRACT_NUMBER = "MC_CONTRACT_NUMBER";
	private static final String COD_LAYOUT = "049";
	private static final String SOURCE_APP = "015";
	private static final String BLANK = "";
	private static final String DEFAULT_FRONT_ID = "0";
	private static String fechaExt;
	private static JDate fechaOp;

	public ELBEAssocReposSecLendCollatLogic() {
	}

	public String getCodLayout() {
		return COD_LAYOUT;
	}

	public String getSourceApp() {
		return SOURCE_APP;
	}

	public String getFrontID(final Trade trade, final DSConnection dsConn) {// TODO
		// get marginCall contract
		final String mccID = trade.getKeywordValue(MC_CONTRACT_NUMBER);
		if ((mccID != null) && !(mccID.equals(BLANK))) {
			CollateralConfig mcc;
			try {
				mcc = ServiceRegistry.getDefault().getCollateralDataServer()
						.getMarginCallConfig(Integer.valueOf(mccID));
				if (mcc != null) {
					String externalReference = trade.getExternalReference();
					if (externalReference == null) {
						externalReference = DEFAULT_FRONT_ID;
					}
					return externalReference;
				}
			} catch (final NumberFormatException e) {
				Log.error(this, e); //sonar
			} catch (final RemoteException e) {
				Log.error(this, e); //sonar
			}

		}
		return BLANK;
	}

	public String getCollatID(final Trade trade, final DSConnection dsConn) {// TODO
		// get marginCall contract
		final String mccID = trade.getKeywordValue(MC_CONTRACT_NUMBER);
		if ((mccID != null) && !(mccID.equals(BLANK))) {
			CollateralConfig mcc;
			try {
				mcc = ServiceRegistry.getDefault().getCollateralDataServer()
						.getMarginCallConfig(Integer.valueOf(mccID));
				if (mcc != null) {
					return mcc.getName();
				}
			} catch (final NumberFormatException e) {
				Log.error(this, e); //sonar
			} catch (final RemoteException e) {
				Log.error(this, e); //sonar
			}

		}
		return BLANK;
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
			Log.error(ELBEAssocReposSecLendCollatLogic.class, e); //sonar
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
	public static ELBEAssocReposSecLendCollatItem getReportRows(final Trade trade, final String fecha,
			final JDate fecha2, final DSConnection dsConn, final Vector<String> errorMsgs) {
		final ELBEAssocReposSecLendCollatLogic verifiedRow = new ELBEAssocReposSecLendCollatLogic();
		fechaExt = fecha;
		fechaOp = fecha2;
		ELBEAssocReposSecLendCollatItem  rowCreated = verifiedRow.getELBEAssocReposSecLendCollatItem(trade, dsConn, errorMsgs);

		return rowCreated;
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
	private ELBEAssocReposSecLendCollatItem getELBEAssocReposSecLendCollatItem(final Trade trade,
			final DSConnection dsConn, final Vector<String> errors) {
		final ELBEAssocReposSecLendCollatItem elbeAssocReposSecLendCollatItem = new ELBEAssocReposSecLendCollatItem();

		elbeAssocReposSecLendCollatItem.setCodLayout(getCodLayout());
		elbeAssocReposSecLendCollatItem.setExtractDate(fechaExt);
		elbeAssocReposSecLendCollatItem.setPosTransDate(convertDate(fechaOp.toString(), "dd/MM/yyyy"));
		elbeAssocReposSecLendCollatItem.setSourceApp(getSourceApp());
		elbeAssocReposSecLendCollatItem.setFrontID(getFrontID(trade, dsConn));
		elbeAssocReposSecLendCollatItem.setCollatID(getCollatID(trade, dsConn));

		String murexTradeId = trade.getKeywordValue("MurexTradeID");
		if (!Util.isEmpty(murexTradeId)){
			elbeAssocReposSecLendCollatItem.setFrontOfficeReference(murexTradeId);
		} else {
			elbeAssocReposSecLendCollatItem.setFrontOfficeReference(elbeAssocReposSecLendCollatItem.getFrontID());
		}

		return elbeAssocReposSecLendCollatItem;

	}

}
