package calypsox.tk.report;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.LegalEntityAttribute;

public class ExportLegalEntityLogic {
	private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

	public ExportLegalEntityLogic() {
	}

	@SuppressWarnings("unused")
	private ExportLegalEntityItem getExportLegalEntityItem(final Vector<String> errors) {
		return null;
	}

	/**
	 * Method to create the row to retrieve to the caller method.
	 * 
	 * @param processingOrg
	 *            Processing Org. to include in the file to export.
	 * @param errorMsgs
	 *            Vector of errors.
	 * @param le
	 *            Legal Entity used to show it in the file exported.
	 * @param valDate
	 *            Valuation Date used in the file as the correct date to export.
	 * @return The particular row with the values from the system.
	 */
	public static Vector<ExportLegalEntityItem> getReportRows(final String processingOrg,
			final Vector<String> errorMsgs, final LegalEntity le, final JDate valDate) {
		final Vector<ExportLegalEntityItem> reportRows = new Vector<ExportLegalEntityItem>();
		final ExportLegalEntityItem rowCreated = ExportLegalEntityLogic
				.getExportLegalEntity(processingOrg, le, valDate);
		if (null != rowCreated) { // If the result row is equals to NULL, we
			// don't add this row to the report.
			reportRows.add(rowCreated);
		}

		return reportRows;
	}

	/**
	 * Method to generate the item with the particular row with system values.
	 * 
	 * @param processingOrg
	 *            Processing Or. to include in the file to export.
	 * @param le
	 *            Legal Entity used to show it in the file exported.
	 * @param valDate
	 *            Valuation Date used in the file as the correct date to export.
	 * @return The item with the particular row retrieved.
	 */
	// BAU 5.5 - New column - export cpty susi repo
	// public static ExportLegalEntityItem getExportLegalEntity(final String processingOrg, final LegalEntity le,
	// final JDate valDate, boolean overnight) {
	//
	// final ExportLegalEntityItem leItem = new ExportLegalEntityItem();
	//
	// leItem.setCounterpartyDescription(le.getName());
	// leItem.setGenerationDate(getGenerationDate(valDate));
	// leItem.setCounterparty(le.getAuthName());
	// leItem.setProcessingOrg(processingOrg);
	//
	// for (LegalEntityAttribute leAttribute : (List<LegalEntityAttribute>) le.getLegalEntityAttributes()) {
	//
	// if (leAttribute.getAttributeType().equals("Overnight")) {
	//
	// leItem.setOvernight(leAttribute.getAttributeValue());
	// continue;
	// }
	// }
	//
	// if (Util.isEmpty(leItem.getOvernight())) {
	// leItem.setOvernight("N");
	// }
	//
	// return leItem;
	// }
	/**
	 * Method to generate the item with the particular row with system values.
	 * 
	 * @param processingOrg
	 *            Processing Or. to include in the file to export.
	 * @param le
	 *            Legal Entity used to show it in the file exported.
	 * @param valDate
	 *            Valuation Date used in the file as the correct date to export.
	 * @return The item with the particular row retrieved.
	 */
	public static ExportLegalEntityItem getExportLegalEntity(final String processingOrg, final LegalEntity le,
			final JDate valDate) {

		final ExportLegalEntityItem leItem = new ExportLegalEntityItem();

		leItem.setCounterpartyDescription(le.getName());
		leItem.setGenerationDate(getGenerationDate(valDate));
		leItem.setCounterparty(le.getAuthName());
		leItem.setProcessingOrg(processingOrg);
		String[] leAtributes = getLegalAttributesData(le);
		leItem.setOvernight(leAtributes[0]);// overnight
		leItem.setEffectiveDate(leAtributes[1]);// EffectiveDate
		return leItem;
	}

	// BAU 5.5 y 6.1 - GSM New columns overnight y effectiveDate
	@SuppressWarnings("unchecked")
	private static String[] getLegalAttributesData(final LegalEntity le) {

		String v[] = { "N", "ValueDate" };

		boolean overnight = false;  
		boolean effectiveDate = false;

		// FIX in case a LE does NOT have attributes
		if ((le == null) || (le.getLegalEntityAttributes() == null)) {
			if (le != null) {
				Log.error(ExportLegalEntityLogic.class, le.getName() + " does not have LE attributes configured");
			}
			return v;
		}

		for (LegalEntityAttribute leAttribute : (List<LegalEntityAttribute>) le.getLegalEntityAttributes()) {

			if (overnight = leAttribute.getAttributeType().equals("Overnight")) {
				v[0] = leAttribute.getAttributeValue();
				if (effectiveDate) {
					break;
				}
			}

			if (effectiveDate = leAttribute.getAttributeType().equals("EffectiveDate")) {
				v[1] = leAttribute.getAttributeValue();
				if (overnight) {
					break;
				}
			}
		}
		return v;
	}

	/**
	 * The generation date for the export (today if the parameter is NULL, or the date passed in the method).
	 * 
	 * @param valDate
	 *            Valuation Date used in the file as the correct date to export.
	 * @return Today, in the format dd/mm/yyyy.
	 */
	private static String getGenerationDate(final JDate valDate) {
		if (null == valDate) {
			return simpleDateFormat.format(JDate.getNow().getDate(TimeZone.getDefault()));
		} else {
			return simpleDateFormat.format(valDate.getDate(TimeZone.getDefault()));
		}
	}

}
