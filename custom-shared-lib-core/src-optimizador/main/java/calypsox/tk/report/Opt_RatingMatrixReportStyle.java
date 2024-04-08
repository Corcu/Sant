/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Vector;

import calypsox.tk.report.Opt_RatingMatrixReport.MATRIX_TYPE;

import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;

/**
 * Style for Rating Matrix for PO & CTPY: threshold, MTA & IA.
 * 
 * @author Olivier Asuncion & Guillermo Solano
 * 
 */
public class Opt_RatingMatrixReportStyle extends CollateralConfigReportStyle {

	public static final String AGREEMENT_NAME = "Agreement Name";
	public static final String OWNER = "Owner";
	public static final String OWNERNAME = "Owner Name";
	public static final String COUNTERPARTY = "Counterparty";
	public static final String COUNTERPARTY_NAME = "Counterparty Name";

	// PO threshold columns
	public static final String THRESHOLD_TYPE_PO = "Threshold Type PO";
	public static final String THRESHOLD_VALUE_PO = "Threshold Value PO";
	public static final String THRESHOLD_CURRENCY_PO = "Threshold Currency PO";
	public static final String THRESHOLD_PERCENTAGE_PO = "Threshold Percentage PO";
	public static final String THRESHOLD_RATING_PO = "Threshold Rating PO";
	public static final String THRESHOLD_RATING_MATRIX_PO = "Rating Matrix Global Rating PO";

	// LE threshold columns
	public static final String THRESHOLD_TYPE_CPTY = "Threshold Type CPTY";
	public static final String THRESHOLD_VALUE_CPTY = "Threshold Value CPTY";
	public static final String THRESHOLD_CURRENCY_CPTY = "Threshold Currency CPTY";
	public static final String THRESHOLD_PERCENTAGE_CPTY = "Threshold Percentage CPTY";
	public static final String THRESHOLD_RATING_CPTY = "Threshold Rating CPTY";
	public static final String THRESHOLD_RATING_MATRIX_CPTY = "Rating Matrix Global Rating CPTY";

	// PO MTA columns
	public static final String MTA_TYPE_PO = "Minimum Transfer Amount Type PO";
	public static final String MTA_VALUE_PO = "Minimum Transfer Amount Value PO";
	public static final String MTA_CURRENCY_PO = "Minimum Transfer Amount Currency PO";
	public static final String MTA_PERCENTAGE_PO = "Minimum Transfer Amount Percentage PO";
	public static final String MTA_RATING_PO = "Minimum Transfer Amount Rating PO";
	public static final String MTA_RATING_MATRIX_PO = "Rating Matrix Global Rating MTA PO";

	// LE MTA columns
	public static final String MTA_TYPE_CPTY = "Minimum Transfer Amount Type CPTY";
	public static final String MTA_VALUE_CPTY = "Minimum Transfer Amount Value CPTY";
	public static final String MTA_CURRENCY_CPTY = "Minimum Transfer Amount Currency CPTY";
	public static final String MTA_PERCENTAGE_CPTY = "Minimum Transfer Amount Percentage CPTY";
	public static final String MTA_RATING_CPTY = "Minimum Transfer Amount Rating CPTY";
	public static final String MTA_RATING_MATRIX_CPTY = "Rating Matrix Global Rating MTA CPTY";

	// PO IA columns
	public static final String IA_TYPE_PO = "Independent Amount Trade Type PO";
	public static final String IA_VALUE_PO = "Independent Amount Trade Value PO";
	public static final String IA_CURRENCY_PO = "Independent Amount Currency PO";
	public static final String IA_RATING_DIRECTION_PO = "Independent Amount Contract Rating Direction PO";
	public static final String IA_DIRECTION_PO = "Independent Amount Contract Direction PO";
	public static final String IA_RATING_MATRIX_PO = "Rating Matrix Global Rating IA PO";

	// LE IA columns
	public static final String IA_TYPE_CPTY = "Independent Amount Trade Type CPTY";
	public static final String IA_VALUE_CPTY = "Independent Amount Trade Value CPTY";
	public static final String IA_CURRENCY_CPTY = "Independent Amount Currency CPTY";
	public static final String IA_RATING_DIRECTION_CPTY = "Independent Amount Contract Rating Direction CPTY";
	public static final String IA_DIRECTION_CPTY = "Independent Amount Contract Direction CPTY";
	public static final String IA_RATING_MATRIX_CPTY = "Rating Matrix Global Rating IA CPTY";

	private static final long serialVersionUID = 123L;

	// Default columns.
	public static final String[] DEFAULTS_COLUMNS = { AGREEMENT_NAME, OWNER, OWNERNAME, COUNTERPARTY,
			COUNTERPARTY_NAME, THRESHOLD_TYPE_PO, THRESHOLD_VALUE_PO, THRESHOLD_CURRENCY_PO, THRESHOLD_PERCENTAGE_PO,
			THRESHOLD_RATING_PO, THRESHOLD_RATING_MATRIX_PO, THRESHOLD_TYPE_CPTY, THRESHOLD_VALUE_CPTY,
			THRESHOLD_CURRENCY_CPTY, THRESHOLD_PERCENTAGE_CPTY, THRESHOLD_RATING_CPTY, THRESHOLD_RATING_MATRIX_CPTY,
			MTA_TYPE_PO, MTA_VALUE_PO, MTA_CURRENCY_PO, MTA_PERCENTAGE_PO, MTA_RATING_PO, MTA_RATING_MATRIX_PO,
			MTA_TYPE_CPTY, MTA_VALUE_CPTY, MTA_CURRENCY_CPTY, MTA_PERCENTAGE_CPTY, MTA_RATING_CPTY,
			MTA_RATING_MATRIX_CPTY, IA_TYPE_PO, IA_VALUE_PO, IA_CURRENCY_PO, IA_RATING_DIRECTION_PO, IA_DIRECTION_PO,
			IA_RATING_MATRIX_PO, IA_TYPE_CPTY, IA_VALUE_CPTY, IA_CURRENCY_CPTY, IA_RATING_DIRECTION_CPTY,
			IA_DIRECTION_CPTY, IA_RATING_MATRIX_CPTY };

	/**
	 * Override method to retrieve, cell by cell, the different columns of the report
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		if (row == null) {
			return null;
		}

		final CollateralConfig mcc = (CollateralConfig) row.getProperty(ReportRow.MARGIN_CALL_CONFIG);
		final Map<MATRIX_TYPE, String> ratingMatrix = (Map<MATRIX_TYPE, String>) row
				.getProperty(Opt_RatingMatrixReport.RATING_MATRICES);

		if (columnName.compareTo(AGREEMENT_NAME) == 0) {
			return mcc.getName();

		} else if (columnName.compareTo(OWNER) == 0) {
			return mcc.getProcessingOrg().getCode();

		} else if (columnName.compareTo(OWNERNAME) == 0) {
			return mcc.getProcessingOrg().getName();

		} else if (columnName.compareTo(COUNTERPARTY) == 0) {
			return mcc.getLegalEntity().getCode();

		} else if (columnName.compareTo(COUNTERPARTY_NAME) == 0) {
			return mcc.getLegalEntity().getName();

			// PO threshold columns
		} else if (columnName.equals(THRESHOLD_TYPE_PO)) {
			return mcc.getPoNewThresholdType();

		} else if (columnName.equals(THRESHOLD_VALUE_PO)) {
			return mcc.getPoNewThresholdAmount();

		} else if (columnName.equals(THRESHOLD_CURRENCY_PO)) {
			return mcc.getPoNewThresholdCurrency();

		} else if (columnName.equals(THRESHOLD_PERCENTAGE_PO)) {
			return mcc.getPoNewThresholdPercentage();

		} else if (columnName.equals(THRESHOLD_RATING_PO)) {
			return mcc.getPoThresholdRatingDirection();

		} else if (columnName.equals(THRESHOLD_RATING_MATRIX_PO)) {

			return ratingMatrix.get(MATRIX_TYPE.PO_THRESHOLD);

			// LE threshold columns
		} else if (columnName.equals(THRESHOLD_TYPE_CPTY)) {
			return mcc.getLeNewThresholdType();

		} else if (columnName.equals(THRESHOLD_VALUE_CPTY)) {
			return mcc.getLeNewThresholdAmount();

		} else if (columnName.equals(THRESHOLD_CURRENCY_CPTY)) {
			return mcc.getLeNewThresholdCurrency();

		} else if (columnName.equals(THRESHOLD_PERCENTAGE_CPTY)) {
			return mcc.getLeNewThresholdPercentage();

		} else if (columnName.equals(THRESHOLD_RATING_CPTY)) {
			return mcc.getLeThresholdRatingDirection();

		} else if (columnName.equals(THRESHOLD_RATING_MATRIX_CPTY)) {

			return ratingMatrix.get(MATRIX_TYPE.CTPY_THRESHOLD);
		}

		// PO MTA columns
		else if (columnName.equals(MTA_TYPE_PO)) {
			return mcc.getPoMTAType();
		}

		else if (columnName.equals(MTA_VALUE_PO)) {
			return mcc.getPoMTAAmount();
		}

		else if (columnName.equals(MTA_CURRENCY_PO)) {
			return mcc.getPoMTACurrency();
		}

		else if (columnName.equals(MTA_PERCENTAGE_PO)) {
			return mcc.getPoMTAPercentage();
		}

		else if (columnName.equals(MTA_RATING_PO)) {
			return mcc.getPoMTARatingDirection();
		}

		else if (columnName.equals(MTA_RATING_MATRIX_PO)) {

			return ratingMatrix.get(MATRIX_TYPE.PO_MTA);
		}

		// LE MTA columns
		else if (columnName.equals(MTA_TYPE_CPTY)) {
			return mcc.getLeMTAType();
		}

		else if (columnName.equals(MTA_VALUE_CPTY)) {
			return mcc.getLeMTAAmount();
		}

		else if (columnName.equals(MTA_CURRENCY_CPTY)) {
			return mcc.getLeMTACurrency();
		}

		else if (columnName.equals(MTA_PERCENTAGE_CPTY)) {
			return mcc.getLeMTAPercentage();
		}

		else if (columnName.equals(MTA_RATING_CPTY)) {
			return mcc.getLeMTARatingDirection();
		}

		else if (columnName.equals(MTA_RATING_MATRIX_CPTY)) {

			return ratingMatrix.get(MATRIX_TYPE.CTPY_MTA);
		}

		// PO IA columns

		else if (columnName.equals(IA_TYPE_PO)) {
			return mcc.getPoIAType();
		}

		else if (columnName.equals(IA_VALUE_PO)) {
			return mcc.getPoIAAmount();
		}

		else if (columnName.equals(IA_CURRENCY_PO)) {
			return mcc.getPoIACurrency();
		}

		else if (columnName.equals(IA_RATING_DIRECTION_PO)) {
			return mcc.getPoIARatingDirection();
		}

		else if (columnName.equals(IA_DIRECTION_PO)) {
			return mcc.getPoIADirection();
		}

		else if (columnName.equals(IA_RATING_MATRIX_PO)) {

			return ratingMatrix.get(MATRIX_TYPE.PO_IA);
		}

		// LE IA columns

		else if (columnName.equals(IA_TYPE_CPTY)) {
			return mcc.getLeIAType();
		}

		else if (columnName.equals(IA_VALUE_CPTY)) {
			return mcc.getLeIAAmount();
		}

		else if (columnName.equals(IA_CURRENCY_CPTY)) {
			return mcc.getLeIACurrency();
		}

		else if (columnName.equals(IA_RATING_DIRECTION_CPTY)) {
			return mcc.getLeIARatingDirection();
		}

		else if (columnName.equals(IA_DIRECTION_CPTY)) {
			return mcc.getLeIADirection();
		}

		else if (columnName.equals(IA_RATING_MATRIX_CPTY)) {

			return ratingMatrix.get(MATRIX_TYPE.CTPY_IA);
		}

		// other MCC columns
		return super.getColumnValue(row, columnName, errors);

		/* OLD CODE */

		// else if (columnName.equals(RATINGMATRIXIAPO)) {
		// Vector<String> holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
		// MarginCallCreditRatingConfiguration mccRatingConfigOwner;
		// try {
		// mccRatingConfigOwner = CollateralUtilities.getMCRatingConfiguration(mcc.getPoRatingsConfigId());
		//
		// Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(mcc,
		// mcc.getEligibleAgencies(), mcc.getProcessingOrg().getId(),
		// JDate.getNow().addBusinessDays(-1, holidays), mccRatingConfigOwner.getRatingType());
		//
		// return SantMarginCallConfigUtil.getIARatingMatrixPO(
		// mcc,
		// creditRatings,
		// PricingEnv.loadPE(DSConnection.getDefault().getDefaultPricingEnv(),
		// new JDatetime(JDate.getNow())), JDate.getNow().addBusinessDays(-1, holidays));
		// } catch (Exception e) {
		// return null;
		// }
		// }
		// else if (columnName.equals(RATING_MATRIX_IA_CPTY)) {
		// Vector<String> holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
		// MarginCallCreditRatingConfiguration mccRatingConfigCpty;
		// try {
		// mccRatingConfigCpty = CollateralUtilities.getMCRatingConfiguration(mcc.getPoRatingsConfigId());
		//
		// Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(mcc,
		// mcc.getEligibleAgencies(), mcc.getProcessingOrg().getId(),
		// JDate.getNow().addBusinessDays(-1, holidays), mccRatingConfigCpty.getRatingType());
		//
		// return SantMarginCallConfigUtil.getIARatingMatrixCpty(
		// mcc,
		// creditRatings,
		// PricingEnv.loadPE(DSConnection.getDefault().getDefaultPricingEnv(),
		// new JDatetime(JDate.getNow())), JDate.getNow().addBusinessDays(-1, holidays));
		// } catch (Exception e) {
		// return null;
		// }
		// }
		// else if (columnName.equals(RATING_MATRIX_MTA_PO)) {
		// Vector<String> holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
		// MarginCallCreditRatingConfiguration mccRatingConfigOwner;
		// try {
		// mccRatingConfigOwner = CollateralUtilities.getMCRatingConfiguration(mcc.getPoRatingsConfigId());
		//
		// Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(mcc,
		// mcc.getEligibleAgencies(), mcc.getProcessingOrg().getId(),
		// JDate.getNow().addBusinessDays(-1, holidays), mccRatingConfigOwner.getRatingType());
		//
		// return SantMarginCallConfigUtil.getMTARatingMatrixPO(
		// mcc,
		// creditRatings,
		// PricingEnv.loadPE(DSConnection.getDefault().getDefaultPricingEnv(),
		// new JDatetime(JDate.getNow())), JDate.getNow().addBusinessDays(-1, holidays));
		// } catch (Exception e) {
		// return null;
		// }
		// }
		// else if (columnName.equals(RATING_MATRIX_MTA_CPTY)) {
		// Vector<String> holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
		// MarginCallCreditRatingConfiguration mccRatingConfigCpty;
		// try {
		// mccRatingConfigCpty = CollateralUtilities.getMCRatingConfiguration(mcc.getLeRatingsConfigId());
		//
		// Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(mcc,
		// mcc.getEligibleAgencies(), mcc.getProcessingOrg().getId(),
		// JDate.getNow().addBusinessDays(-1, holidays), mccRatingConfigCpty.getRatingType());
		//
		// return SantMarginCallConfigUtil.getMTARatingMatrixCpty(
		// mcc,
		// creditRatings,
		// PricingEnv.loadPE(DSConnection.getDefault().getDefaultPricingEnv(),
		// new JDatetime(JDate.getNow())), JDate.getNow().addBusinessDays(-1, holidays));
		// } catch (Exception e) {
		// return null;
		// }
		// }
		// } else if (columnName.compareTo(RATING_MATRIX_PO) == 0) {
		// Vector<String> holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
		// MarginCallCreditRatingConfiguration mccRatingConfigOwner;
		// try {
		// mccRatingConfigOwner = CollateralUtilities.getMCRatingConfiguration(mcc.getPoRatingsConfigId());
		//
		// Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(mcc,
		// mcc.getEligibleAgencies(), mcc.getProcessingOrg().getId(),
		// JDate.getNow().addBusinessDays(-1, holidays), mccRatingConfigOwner.getRatingType());
		//
		// return SantMarginCallConfigUtil.getRatingMatrixPO(
		// mcc,
		// creditRatings,
		// PricingEnv.loadPE(DSConnection.getDefault().getDefaultPricingEnv(),
		// new JDatetime(JDate.getNow())), JDate.getNow().addBusinessDays(-1, holidays));
		// } catch (Exception e) {
		// return null;
		// }
		// } else if (columnName.compareTo(RATING_MATRIX_CPTY) == 0) {
		// Vector<String> holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
		// MarginCallCreditRatingConfiguration mccRatingConfigCpty;
		// try {
		// mccRatingConfigCpty = CollateralUtilities.getMCRatingConfiguration(mcc.getPoRatingsConfigId());
		//
		// Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(mcc,
		// mcc.getEligibleAgencies(), mcc.getProcessingOrg().getId(),
		// JDate.getNow().addBusinessDays(-1, holidays), mccRatingConfigCpty.getRatingType());
		//
		// return SantMarginCallConfigUtil.getRatingMatrixCpty(
		// mcc,
		// creditRatings,
		// PricingEnv.loadPE(DSConnection.getDefault().getDefaultPricingEnv(),
		// new JDatetime(JDate.getNow())), JDate.getNow().addBusinessDays(-1, holidays));
		// } catch (Exception e) {
		// return null;
		// }
	}
}
