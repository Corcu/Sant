package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.GlobalRatingConfiguration;
import com.calypso.tk.report.CollateralConfigReport;
import com.calypso.tk.report.CollateralConfigReportTemplate;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.util.SantMarginCallConfigUtil;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.ELBEandKGRutilities;

/**
 * Optimization Extraction of Rating Matrix for PO & CTPY: threshold, MTA & IA.
 * 
 * @author Olivier Asuncion & Guillermo Solano
 * 
 */
public class Opt_RatingMatrixReport extends CollateralConfigReport {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = 123L;
	/**
	 * Collateral Config ReportRow keyword
	 */
	public static final String COLLATERAL_CONFIG = ReportRow.MARGIN_CALL_CONFIG;
	/**
	 * PO & CTPY rating matrices ReportRow keyword
	 */
	public static final String RATING_MATRICES = "RatingMatrices";

	/**
	 * enum: types of rating matrixes
	 */
	public enum MATRIX_TYPE {
		PO_THRESHOLD, PO_MTA, PO_IA, CTPY_THRESHOLD, CTPY_MTA, CTPY_IA;
	};

	/**
	 * Empty matrix word
	 */
	private final static String EMPTY_MATRIX = "";
	/**
	 * Inside Matrix new column/field separator
	 */
	private final static String MATRIX_COLUMN_SEPARATOR = "#";
	/**
	 * Inside Matrix new row separator
	 */
	private final static String MATRIX_LINE_SEPARATOR = ";";
	/**
	 * Inside Matrix, if both are selected, will separate amount from percentage inside the same field
	 */
	private final static String MATRIX_BOTH_SEPARATOR = "/";
	/**
	 * Percentage mark
	 */
	private final static String PERCENTAGE_SYMBON = "%";

	@SuppressWarnings("unchecked")
	private static Vector<String> holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
	private static JDate valueDate = JDate.getNow().addBusinessDays(-1, holidays);

	private GlobalRatingConfiguration globalRatingConfig = null;
	private MarginCallCreditRatingConfiguration poRatingConfig = null;
	private MarginCallCreditRatingConfiguration ctpyRatingConfig = null;

	/**
	 * Override method load to generate the report.
	 * 
	 * @param errorsMsgs
	 *            passed by parameter
	 * @return the ReportOutput to generate the report
	 */
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") Vector errorMsgs) {

		/*
		 * retrieve the OPEN Collateral Contracts
		 */
		getReportTemplate().put(CollateralConfigReportTemplate.PROCESSING_ORG, CollateralConfig.ALL);
		getReportTemplate().put(CollateralConfigReportTemplate.ROLE, CollateralConfig.ALL);
		getReportTemplate().put(CollateralConfigReportTemplate.CONTRACT_TYPE, CollateralConfig.ALL);
		getReportTemplate().put(CollateralConfigReportTemplate.STATUS, CollateralConfig.OPEN);
		getReportTemplate().put(CollateralConfigReportTemplate.DISCOUNT_CURRENCY, CollateralConfig.ALL);

		final StandardReportOutput output = new StandardReportOutput(this);
		final DefaultReportOutput defOutput = ((DefaultReportOutput) super.load(errorMsgs));
		// Collateral configs rows from core MCC report
		ReportRow[] rows = defOutput.getRows();

		/*
		 * PO Rating Matrix: For each contract, if PO has threshold type equal to BOTH or CREDIT RATING, attach Rating
		 * threshold Matrix. If PO has MTA equal to BOTH or CREDIT RATING, attach Rating MTA Matrix. If PO has IA equal
		 * to DEFAULT or ALWAYS, attach Rating IA Matrix. The same logic is applied to the CTPY of the contract.
		 */
		List<ReportRow> rowsList = new ArrayList<ReportRow>(rows.length);

		for (ReportRow colConfigRow : rows) {

			final CollateralConfig config = (CollateralConfig) colConfigRow.getProperty(COLLATERAL_CONFIG);

			// GSM 20/07/15. SBNA Multi-PO filter
			if (CollateralUtilities.filterPoByTemplate(getReportTemplate(), config)) {
				continue;
			}

			if (config == null) {
				continue;
			}
			
			if (KGR_Collateral_MarginCallReport.CSA_FACADE.equals(config.getContractType())){
				continue;
			}

			final Map<MATRIX_TYPE, String> ratingMatrix = buildMatrixMap(config);
			final ReportRow newRow = new ReportRow(config, COLLATERAL_CONFIG);
			newRow.setProperty(RATING_MATRICES, ratingMatrix);
			rowsList.add(newRow);

		}

		// finally attach the new rows list to the output: collateral config y rating matrices
		output.setRows(rowsList.toArray(new ReportRow[0]));
		return output;
	}

	/**
	 * @param collateral
	 *            config from where the rating matrixs (if required will be built)
	 * @return a map with all the matrix ratings associated
	 */
	// rating types: PO_THRESHOLD, PO_MTA, PO_IA, CTPY_THRESHOLD, CTPY_MTA, CTPY_IA;
	private Map<MATRIX_TYPE, String> buildMatrixMap(final CollateralConfig config) {

		final HashMap<MATRIX_TYPE, String> map = new HashMap<MATRIX_TYPE, String>();
		// Collection<CreditRating> poCreditRatings = null;
		// Collection<CreditRating> ctpyCreditRatings = null;

		for (MATRIX_TYPE type : MATRIX_TYPE.values()) {
			map.put(type, EMPTY_MATRIX);
		}

		// PO Threshold Rating Matrix
		if (poUsesThresholdRating(config)) {
			final String matrix = buildPOThresholdMatrix(config);
			map.put(MATRIX_TYPE.PO_THRESHOLD, matrix);
		}

		// CTPY Threshold Rating Matrix
		if (ctpyUsesThresholdRating(config)) {
			final String matrix = buildctpyThresholdMatrix(config);
			map.put(MATRIX_TYPE.CTPY_THRESHOLD, matrix);
		}

		// PO MTA Rating Matrix
		if (poUsesMTARating(config)) {
			final String matrix = buildPOMTAMatrix(config);
			map.put(MATRIX_TYPE.PO_MTA, matrix);
		}

		// CTPY MTA Rating Matrix
		if (ctpyUsesMTARating(config)) {
			final String matrix = buildctpyMTAMatrix(config);
			map.put(MATRIX_TYPE.CTPY_MTA, matrix);
		}

		// PO IA Rating Matrix
		if (poUsesIARating(config)) {
			final String matrix = buildPOIAMatrix(config);
			map.put(MATRIX_TYPE.PO_IA, matrix);
		}

		// CTPY IA Rating Matrix
		if (ctpyUsesIARating(config)) {
			final String matrix = buildctpyIAMatrix(config);
			map.put(MATRIX_TYPE.CTPY_IA, matrix);
		}

		// clean rating matrices cache
		this.poRatingConfig = null;
		this.ctpyRatingConfig = null;

		return map;
	}

	// ////////////////////
	// / THRESHOLD ///////
	// //////////////////
	/**
	 * @param config
	 * @return true if collateral config PO uses threshold matrix
	 */
	private boolean poUsesThresholdRating(final CollateralConfig config) {

		final String thrType = config.getPoNewThresholdType();
		return (usesCreditRating(thrType));
	}

	/**
	 * 
	 * @param mcc
	 * @param poRatingMatrix
	 * @return
	 */
	private String buildPOThresholdMatrix(final CollateralConfig mcc) {

		final StringBuffer sb = new StringBuffer();
		if (!buildCreditRatings(true, mcc)) {
			return sb.toString();
		}

		// process margin call rating lines
		for (MarginCallCreditRating mcRating : this.poRatingConfig.getRatings()) {

			if ((mcRating.getThreshold() == null) || mcRating.getThreshold().isEmpty()) {
				continue;
			}
			if (mcc.getEligibleAgencies() == null) {
				Log.error(this, "The Elegible Agencies for contract " + mcc.getShortName() + " is empty");
				continue;
			}
					
			// agency rating for prioriy in order of elegible agencies in contract
			sb.append(buildCreditRatingsInAgencyOrder(mcc, mcRating)).append(MATRIX_COLUMN_SEPARATOR);

			// threshold type
			sb.append(mcRating.getThresholdType()).append(MATRIX_COLUMN_SEPARATOR);
			// threshold amount
			sb.append(buildTypeAndPercent(mcRating, mcRating.getThresholdType(), MATRIX_TYPE.PO_THRESHOLD));
			sb.append(MATRIX_COLUMN_SEPARATOR);
			// threshold ccy
			sb.append(this.poRatingConfig.getThresholdCurrency()).append(MATRIX_COLUMN_SEPARATOR);
			// GSM: pending!!
			sb.append(MATRIX_LINE_SEPARATOR);
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * @param config
	 * @return true if collateral config LE uses threshold matrix
	 */
	private boolean ctpyUsesThresholdRating(CollateralConfig config) {

		final String thrType = config.getLeNewThresholdType();
		return (usesCreditRating(thrType));
	}

	/**
	 * 
	 * @param config
	 * @return
	 */
	private String buildctpyThresholdMatrix(CollateralConfig config) {

		final StringBuffer sb = new StringBuffer();
		if (!buildCreditRatings(false, config)) {
			return sb.toString();
		}
		// process margin call rating lines
		for (MarginCallCreditRating mcRating : this.ctpyRatingConfig.getRatings()) {
			if ((mcRating.getThreshold() == null) || mcRating.getThreshold().isEmpty()) {
				continue;
			}
			// agency rating for prioriy in order of elegible agencies in contract
			sb.append(buildCreditRatingsInAgencyOrder(config, mcRating));
			sb.append(MATRIX_COLUMN_SEPARATOR);
			// threshold type
			sb.append(mcRating.getThresholdType()).append(MATRIX_COLUMN_SEPARATOR);
			// threshold amount
			sb.append(buildTypeAndPercent(mcRating, mcRating.getThresholdType(), MATRIX_TYPE.CTPY_THRESHOLD));
			sb.append(MATRIX_COLUMN_SEPARATOR);
			// threshold ccy
			sb.append(this.ctpyRatingConfig.getThresholdCurrency()).append(MATRIX_COLUMN_SEPARATOR);
			// GSM: pending!!
			sb.append(MATRIX_LINE_SEPARATOR);
		}

		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}

		return sb.toString();
	}

	// ////////////////////
	// ///// MTA /////////
	// //////////////////

	/**
	 * 
	 * @param config
	 * @return true if collateral config PO uses MTA matrix
	 */
	private boolean poUsesMTARating(CollateralConfig config) {
		final String thrType = config.getPoMTAType();
		return (usesCreditRating(thrType));
	}

	/**
	 * 
	 * @param config
	 * @return
	 */
	private String buildPOMTAMatrix(CollateralConfig config) {

		final StringBuffer sb = new StringBuffer();
		if (!buildCreditRatings(true, config)) {
			return sb.toString();
		}
		// process margin call rating lines
		for (MarginCallCreditRating mcRating : this.poRatingConfig.getRatings()) {
			// if (mcRating.getMta() == 0.0d) {
			// continue;
			// }
			// agency rating for prioriy in order of elegible agencies in contract

			if (config.getEligibleAgencies() == null) {
				Log.error(this, "The Elegible Agencies for contract " + config.getShortName() + " is empty");
				continue;
			}
			sb.append(buildCreditRatingsInAgencyOrder(config, mcRating)).append(MATRIX_COLUMN_SEPARATOR);
			// threshold type
			sb.append(mcRating.getMtaType()).append(MATRIX_COLUMN_SEPARATOR);
			// threshold amount
			sb.append(buildTypeAndPercent(mcRating, mcRating.getMtaType(), MATRIX_TYPE.PO_MTA));
			sb.append(MATRIX_COLUMN_SEPARATOR);
			// threshold ccy
			sb.append(this.poRatingConfig.getMtaCurrency()).append(MATRIX_COLUMN_SEPARATOR);
			// GSM: pending!!
			sb.append(MATRIX_LINE_SEPARATOR);
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param config
	 * @return true if collateral config LE uses MTA matrix
	 */
	private boolean ctpyUsesMTARating(CollateralConfig config) {

		final String thrType = config.getLeMTAType();
		return (usesCreditRating(thrType));
	}

	/**
	 * 
	 * @param config
	 * @return
	 */
	private String buildctpyMTAMatrix(CollateralConfig config) {

		final StringBuffer sb = new StringBuffer();
		if (!buildCreditRatings(false, config)) {
			return sb.toString();
		}
		// process margin call rating lines
		for (MarginCallCreditRating mcRating : this.ctpyRatingConfig.getRatings()) {
			// if (mcRating.getMta() == 0.0d) {
			// continue;
			// }
			// agency rating for prioriy in order of elegible agencies in contract
			if (config.getEligibleAgencies() == null) {
				Log.error(this, "The Elegible Agencies for contract " + config.getShortName() + " is empty");
				continue;
			}
			sb.append(buildCreditRatingsInAgencyOrder(config, mcRating));
			sb.append(MATRIX_COLUMN_SEPARATOR);
			// threshold type
			sb.append(mcRating.getMtaType()).append(MATRIX_COLUMN_SEPARATOR);
			// threshold amount
			sb.append(buildTypeAndPercent(mcRating, mcRating.getMtaType(), MATRIX_TYPE.PO_MTA));
			sb.append(MATRIX_COLUMN_SEPARATOR);
			// threshold ccy
			sb.append(this.ctpyRatingConfig.getMtaCurrency()).append(MATRIX_COLUMN_SEPARATOR);
			// new line separator
			sb.append(MATRIX_LINE_SEPARATOR);
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	// ////////////////////
	// ////// IA /////////
	// //////////////////

	/**
	 * 
	 * @param config
	 * @return true if collateral config PO uses IA matrix
	 */
	private boolean ctpyUsesIARating(CollateralConfig config) {

		final String ratingDirection = config.getLeIARatingDirection();
		final String thrType = config.getLeIADirection();

		if ((ratingDirection == null) || (thrType == null)) {
			return false;
		}

		return ((!ratingDirection.equals(CollateralConfig.NONE)) && (thrType.equals(CollateralConfig.DEFAULT) || thrType
		        .equals(CollateralConfig.ALWAYS)));
	}

	/**
	 * 
	 * @param config
	 * @return
	 */
	private String buildPOIAMatrix(CollateralConfig config) {

		final StringBuffer sb = new StringBuffer();
		if (!buildCreditRatings(true, config)) {
			return sb.toString();
		}
		// process margin call rating lines
		for (MarginCallCreditRating mcRating : this.poRatingConfig.getRatings()) {
			// if (mcRating.getIndependentAmount() == 0.0d) {
			// continue;
			// }
			// agency rating for prioriy in order of elegible agencies in contract
			sb.append(buildCreditRatingsInAgencyOrder(config, mcRating)).append(MATRIX_COLUMN_SEPARATOR);
			// threshold type
			sb.append(mcRating.getIaType()).append(MATRIX_COLUMN_SEPARATOR);
			// threshold amount
			sb.append(buildTypeAndPercent(mcRating, mcRating.getIaType(), MATRIX_TYPE.PO_IA));
			sb.append(MATRIX_COLUMN_SEPARATOR);
			// threshold ccy
			sb.append(this.poRatingConfig.getIaCurrency()).append(MATRIX_COLUMN_SEPARATOR);
			// GSM: pending!!
			sb.append(MATRIX_LINE_SEPARATOR);
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param config
	 * @return true if collateral config PO uses IA matrix
	 */
	private boolean poUsesIARating(CollateralConfig config) {

		final String ratingDirection = config.getPoIARatingDirection();
		final String thrType = config.getPoIADirection();

		if ((ratingDirection == null) || (thrType == null)) {
			return false;
		}

		return ((!ratingDirection.equals(CollateralConfig.NONE)) && (thrType.equals(CollateralConfig.DEFAULT) || thrType
		        .equals(CollateralConfig.ALWAYS)));
	}

	/**
	 * 
	 * @param config
	 * 
	 */
	private String buildctpyIAMatrix(CollateralConfig config) {

		final StringBuffer sb = new StringBuffer();
		if (!buildCreditRatings(false, config)) {
			return sb.toString();
		}
		// process margin call rating lines
		for (MarginCallCreditRating mcRating : this.ctpyRatingConfig.getRatings()) {
			// if (mcRating.getIndependentAmount() == 0.0d) {
			// continue;
			// }
			// agency rating for prioriy in order of elegible agencies in contract
			sb.append(buildCreditRatingsInAgencyOrder(config, mcRating)).append(MATRIX_COLUMN_SEPARATOR);
			// threshold type
			sb.append(mcRating.getIaType()).append(MATRIX_COLUMN_SEPARATOR);
			// threshold amount
			sb.append(buildTypeAndPercent(mcRating, mcRating.getIaType(), MATRIX_TYPE.PO_IA));
			sb.append(MATRIX_COLUMN_SEPARATOR);
			// threshold ccy
			sb.append(this.ctpyRatingConfig.getIaCurrency()).append(MATRIX_COLUMN_SEPARATOR);
			// GSM: pending!!
			sb.append(MATRIX_LINE_SEPARATOR);
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	// /////////////////////////
	// // Helpful methods /////
	// ///////////////////////

	/*
	 * Helpful methods
	 */
	/**
	 * @param config
	 * @return true if collateral config PO uses threshold matrix
	 */
	private boolean usesCreditRating(final String thrType) {

		if ((thrType == null) || thrType.isEmpty()) {
			return false;
		}
		return (thrType.equals(CollateralConfig.GLOBAL_RATING) || thrType.equals(CollateralConfig.BOTH));
	}

	/**
	 * 
	 * @param po
	 * @param mcc
	 * @param ratingMatrix
	 * @param ratingConfig
	 * @return
	 */
	private boolean buildCreditRatings(boolean po, CollateralConfig mcc) {

		if (po) {
			// if (ratingMatrix == null) {
			// ratingMatrix = readPOCreditRating(mcc);
			// }
			// if (ratingMatrix.isEmpty()) {
			// return false;
			// }
			if (this.poRatingConfig == null) {
				this.poRatingConfig = readPORatingConfig(mcc);
			}
			if (this.poRatingConfig == null) {
				return false;
			}
		}
		// retrieve rating data of CTPY
		else {

			// if (ratingMatrix == null) {
			// ratingMatrix = readCTPYCreditRating(mcc);
			// }
			// if (ratingMatrix.isEmpty()) {
			// return false;
			// }
			if (this.ctpyRatingConfig == null) {
				this.ctpyRatingConfig = readCTPYRatingConfig(mcc);
			}
			if (this.ctpyRatingConfig == null) {
				return false;
			}
		}
		// retrieve DefaultGlobalRatingConfiguration
		if (this.globalRatingConfig == null) {

			try {
				this.globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
				        .loadDefaultGlobalRatingConfiguration();
			} catch (RemoteException e) {
				Log.error(this, "DB Error retrieving Calypso DefaultGlobalRatingConfiguration");
				Log.error(this, e); //sonar
				return false;
			}
		}
		
		return true;
	}

	/**
	 * 
	 * @param contract
	 * @return
	 */
	private MarginCallCreditRatingConfiguration readPORatingConfig(final CollateralConfig contract) {

		if (contract.getPoRatingsConfigId() != 0) {
			MarginCallCreditRatingConfiguration mcRatingConfig;
			try {
				mcRatingConfig = SantMarginCallConfigUtil.getLatestMarginCallRatings(contract.getPoRatingsConfigId(),
				        valueDate);
				if (mcRatingConfig != null) {
					return mcRatingConfig;
				}
			} catch (RemoteException e) {
				Log.error(this, "DB Error retrieving CTPY Rating Configuration for contract " + contract.getId());
				Log.error(this, e); //sonar
			}

		}
		return null;

	}

	/**
	 * @param mcc
	 *            collateral config
	 * @return the credit retings of the PO (owner)
	 */
	@SuppressWarnings("unused")
	private Collection<CreditRating> readPOCreditRating(final CollateralConfig mcc) {

		@SuppressWarnings("unchecked")
		Vector<String> holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
		MarginCallCreditRatingConfiguration mccRatingConfigOwner;

		try {
			mccRatingConfigOwner = CollateralUtilities.getMCRatingConfiguration(mcc.getPoRatingsConfigId());
		} catch (Exception e) {
			Log.error(this, "DB Error retrieving PO Rating configuration for contract " + mcc.getId());
			Log.error(this, e); //sonar
			return new Vector<CreditRating>(0);
		}

		Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(mcc, mcc.getEligibleAgencies(),
		        mcc.getProcessingOrg().getId(), JDate.getNow().addBusinessDays(-1, holidays),
		        mccRatingConfigOwner.getRatingType());

		return creditRatings;
	}

	/**
	 * @param mcc
	 *            collateral config
	 * @return the credit rating of the counterparty
	 */
	@SuppressWarnings("unused")
	private Collection<CreditRating> readCTPYCreditRating(final CollateralConfig mcc) {

		@SuppressWarnings("unchecked")
		Vector<String> holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
		MarginCallCreditRatingConfiguration mccRatingConfigCtpy;

		try {
			mccRatingConfigCtpy = CollateralUtilities.getMCRatingConfiguration(mcc.getLeRatingsConfigId());
		} catch (Exception e) {
			Log.error(this, "DB Error retrieving CTPY Credit Rating for contract " + mcc.getId());
			Log.error(this, e); //sonar
			return new Vector<CreditRating>(0);
		}

		Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(mcc, mcc.getEligibleAgencies(),
		        mcc.getLegalEntity().getId(), JDate.getNow().addBusinessDays(-1, holidays),
		        mccRatingConfigCtpy.getRatingType());

		return creditRatings;
	}

	/**
	 * 
	 * @param contract
	 * @return
	 */
	private MarginCallCreditRatingConfiguration readCTPYRatingConfig(CollateralConfig contract) {

		if (contract.getPoRatingsConfigId() != 0) {
			MarginCallCreditRatingConfiguration mcRatingConfig;
			try {
				mcRatingConfig = SantMarginCallConfigUtil.getLatestMarginCallRatings(contract.getLeRatingsConfigId(),
				        valueDate);
				if (mcRatingConfig != null) {
					return mcRatingConfig;
				}
			} catch (RemoteException e) {
				Log.error(this, "DB Error retrieving CTPY Rating Configuration for contract " + contract.getId());
				Log.error(this, e); //sonar
			}

		}
		return null;
	}

	/**
	 * 
	 * @param mcc
	 * @param po
	 *            CreditRating
	 * @return
	 */
	private StringBuffer buildCreditRatingsInAgencyOrder(final CollateralConfig mcc,
	        final MarginCallCreditRating mcRating) {

		StringBuffer sb = new StringBuffer();
		try {
			for (String agency : mcc.getEligibleAgencies()) {

				String agencyAttribute = "";
				if (agency.equals(CollateralStaticAttributes.FITCH)) {
					agencyAttribute = CollateralStaticAttributes.FITCH;

				} else if (agency.equals(CollateralStaticAttributes.SNP)) {
					agencyAttribute = CollateralStaticAttributes.SNP;

				} else if (agency.equals(CollateralStaticAttributes.MOODY)) {
					agencyAttribute = CollateralStaticAttributes.MOODY;
				}
				if (agencyAttribute.length() > 1) {
					String ratingValue = SantMarginCallConfigUtil.getGlobalRatingValue(this.globalRatingConfig, agencyAttribute, mcRating.getPriority());
					sb.append(ratingValue).append(MATRIX_COLUMN_SEPARATOR);
				}
			}
		} catch (Exception e) {
			Log.error(this, "DB Error getting agency global rating value \n" + e); //sonar
		}

		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}

		return sb;
	}

	/**
	 * 
	 * @param mcRating
	 * @param type
	 * @param t
	 * @return
	 */
	private String buildTypeAndPercent(final MarginCallCreditRating mcRating, final String type, final MATRIX_TYPE t) {

		if (type.equals("NEVER") || (mcRating == null)) {
			return "";
		}

		if (type.equals("AMOUNT")) {
			if (t.equals(MATRIX_TYPE.PO_THRESHOLD) || t.equals(MATRIX_TYPE.CTPY_THRESHOLD)) {
				return mcRating.getThreshold();
			} else if (t.equals(MATRIX_TYPE.PO_MTA) || t.equals(MATRIX_TYPE.CTPY_MTA)) {
				String mta = mcRating.getMta() + "";
				return mta;
			} else if (t.equals(MATRIX_TYPE.PO_IA) || t.equals(MATRIX_TYPE.PO_IA)) {
				String ia = mcRating.getIndependentAmount() + "";
				return ia;
			}
		}
		// other options means percentage
		String amountPercent = "";
		if (t.equals(MATRIX_TYPE.PO_THRESHOLD) || t.equals(MATRIX_TYPE.CTPY_THRESHOLD)) {
			amountPercent = mcRating.getThreshold() + MATRIX_BOTH_SEPARATOR + mcRating.getThresholdPercent()
			        + PERCENTAGE_SYMBON;
		} else if (t.equals(MATRIX_TYPE.PO_MTA) || t.equals(MATRIX_TYPE.CTPY_MTA)) {
			amountPercent = mcRating.getMta() + MATRIX_BOTH_SEPARATOR + mcRating.getMtaPercent() + PERCENTAGE_SYMBON;
		} else if (t.equals(MATRIX_TYPE.PO_IA) || t.equals(MATRIX_TYPE.PO_IA)) {
			amountPercent = mcRating.getIndependentAmount() + MATRIX_BOTH_SEPARATOR + mcRating.getIaPercent()
			        + PERCENTAGE_SYMBON;
		}
		return amountPercent;
	}
}
