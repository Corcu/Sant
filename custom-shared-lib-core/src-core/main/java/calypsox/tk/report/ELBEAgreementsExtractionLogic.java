package calypsox.tk.report;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.commons.beanutils.BeanUtils;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.dto.CashAllocationDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.PreviousPositionDTO;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.collateral.dto.SecurityPositionDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.GlobalRating;
import com.calypso.tk.refdata.GlobalRatingConfiguration;

import calypsox.util.ELBEandKGRutilities;
import calypsox.util.collateral.CollateralManagerUtil;
import calypsox.util.collateral.CollateralUtilities;

/**
 * Class with the necessary logic to retrieve the different values for the
 * ELBEAgreements report.
 *
 * @author David Porras Martinez
 */
public class ELBEAgreementsExtractionLogic {

	private static final String FITCH = "Fitch";
	private static final String SYP = "S&P";
	private static final String MOODY = "Moody";
	private static final String HIGHER = "HIGHER";
	private static final String LOWER = "LOWER";
	private static final String COD_LAYOUT = "005";
	private static final String SOURCE_APP = "015";
	private static final String MARGIN_CALL = "Margin Call";
	private static final String DEFINE = "Define";
	private static final String BLANK = "";
	private static final String COLLECT = "C";
	private static final String PAY = "P";
	private static final String EURO = "EUR";
	private static final String PART_EXECUTED = "PART_EXECUTED";
	private static final String FULL_EXECUTED = "FULL_EXECUTED";
	private static final String PRICED_NO_CALL = "PRICED_NO_CALL";
	/* Constant */
	private static final String CANNOT_GET_MARGINCALLRATING_FOR_CONTRANT = "Cannot get MarginCallCreditRating for contract";
	/* Constant */
	private static final String AGENCY = "agency";
	/* Constant */
	private static final String RATING = "rating";
	/* Constant */
	private static final String EQUAL = "=";
	/* Constant */
	private static final String COMMA = ",";
	/* Constant */
	public static final String NEW = "New Method";
	/* Constant */
	public static final String MOODYS = "MOODYS - ";
	/* Constant */
	public static final String STANDARD = "STANDARD & POORS - ";

	private static String fechaExt;
	private static JDate processDate;
	private static JDate valueDate;
	@SuppressWarnings("unused")
	private double grossExposureCCY;
	private static double fxRateCcyEur;
	private static GlobalRatingConfiguration globalRatingConfig = null;

	/**
	 * Constructor empty
	 * 
	 * public ELBEAgreementsExtractionLogic() {
	 * 
	 * }
	 */

	@SuppressWarnings("unused")
	private ELBEAgreementsExtractionItem getELBEAgreementsExtractionItem(final List<String> errors) {
		return null;
	}

	/**
	 * Get required rating depending on direction from a list of credit ratings
	 * (Rating Stuff)
	 * 
	 * @param creditRatings
	 * @param direction
	 * @return
	 */
	public CreditRating getRequiredRating(List<CreditRating> creditRatings, String direction) {

		CreditRating requiredRating = null;
		GlobalRating globalRating = null;
		int priorityComparator = -1;

		if (!Util.isEmpty(direction)) {
			// initialize with first element values
			globalRating = ELBEandKGRutilities.getAgencyGlobalRating(globalRatingConfig,
					creditRatings.get(0).getAgencyName());
			if (globalRating != null) {
				priorityComparator = ELBEandKGRutilities.getAgencyRatingPriority(globalRating,
						creditRatings.get(0).getRatingValue());
				if (priorityComparator != -1) {
					requiredRating = creditRatings.get(0);
					// compare and get
					for (int i = 1; i < creditRatings.size(); i++) {
						globalRating = ELBEandKGRutilities.getAgencyGlobalRating(globalRatingConfig,
								creditRatings.get(i).getAgencyName());
						if (globalRating != null) {
							int priority = ELBEandKGRutilities.getAgencyRatingPriority(globalRating,
									creditRatings.get(i).getRatingValue());
							if (priority != -1) {
								if (direction.equals(HIGHER) && (priority < priorityComparator)) {
									requiredRating = creditRatings.get(i);
									priorityComparator = priority;
								}
								if (direction.equals(LOWER) && (priority > priorityComparator)) {
									requiredRating = creditRatings.get(i);
									priorityComparator = priority;
								}
							}
						}
					}
				}
			}
		}

		return requiredRating;
	}

	/**
	 * Get equivalent rating for a given credit rating and required agency
	 * 
	 * @param creditRating
	 * @param agency
	 * @return
	 */
	public String getAgencyEquivalentRatingValue(CreditRating creditRating, String agency) {

		if (creditRating.getAgencyName().equals(agency)) {
			return creditRating.getRatingValue();
		}

		GlobalRating actualGlobalRating = ELBEandKGRutilities.getAgencyGlobalRating(globalRatingConfig,
				creditRating.getAgencyName());
		GlobalRating equivalentGlobalRating = ELBEandKGRutilities.getAgencyGlobalRating(globalRatingConfig, agency);

		if ((actualGlobalRating != null) && (equivalentGlobalRating != null)) {
			int priority = ELBEandKGRutilities.getAgencyRatingPriority(actualGlobalRating,
					creditRating.getRatingValue());
			if (priority != -1) {
				String value = ELBEandKGRutilities.getAgencyRatingValue(equivalentGlobalRating, priority);
				if (value != null) {
					return value;
				}
			}
		}
		return BLANK;
	}

	/**
	 * Method that calulate Active Rating
	 * 
	 * @param item
	 * @param marginCall
	 * @param thresCreditRating
	 * @param iaCreditRating
	 * @param agencies
	 */
	public void calculateActiveRating(ELBEAgreementsExtractionItem item, final CollateralConfig marginCall,
			CreditRating thresCreditRating, CreditRating iaCreditRating, List<String> agencies) {

		String activeRating = "";

		// check if owner threshold or ia depends on rating
		if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.GLOBAL_RATING)
				|| ELBEandKGRutilities.isIADependingOnRating(marginCall)) {

			CreditRating creditRating = null;
			// use appropriate rating
			if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.GLOBAL_RATING)) {
				creditRating = thresCreditRating;
			} else {
				creditRating = iaCreditRating;
			}

			activeRating = getActiveRating(creditRating, agencies);
		}

		item.setActiveRating(CollateralUtilities.fillWithBlanks(activeRating, 60));

	}

	//
	//
	/**
	 * Method get Active Rating that set rating passed value and values for
	 * equivalents ratings in agencies given
	 * 
	 * @param creditRating
	 * @param agencies
	 * @return
	 */
	public String getActiveRating(CreditRating creditRating, List<String> agencies) {

		StringBuilder activeRating = new StringBuilder("");
		String value;

		if (Util.isEmpty(agencies)) {
			// moody
			value = getAgencyEquivalentRatingValue(creditRating, MOODY);
			activeRating.append(MOODYS + value + " | ");
			// sp
			value = getAgencyEquivalentRatingValue(creditRating, SYP);
			activeRating.append(STANDARD + value);
		} else {
			// caso especial 3 agencias, solo mostrar Moody y SP
			if (agencies.size() == 3) {
				// moody
				value = getAgencyEquivalentRatingValue(creditRating, MOODY);
				activeRating.append(MOODYS + value + " | ");
				// sp
				value = getAgencyEquivalentRatingValue(creditRating, SYP);
				activeRating.append(STANDARD + value);
			} else {
				String moody = null;
				String sp = null;
				String fitch = null;
				for (int i = 0; i < agencies.size(); i++) {
					if (agencies.get(i).equals(MOODY)) {
						moody = getAgencyEquivalentRatingValue(creditRating, MOODY);
					}
					if (agencies.get(i).equals(SYP)) {
						sp = getAgencyEquivalentRatingValue(creditRating, SYP);
					}
					if (agencies.get(i).equals(FITCH)) {
						fitch = getAgencyEquivalentRatingValue(creditRating, FITCH);
					}
				}
				if (moody != null) {
					activeRating.append(MOODYS + moody);
					if (sp != null) {
						activeRating.append(" | STANDARD & POORS - " + sp);
					} else if (fitch != null) {
						activeRating.append(" | FITCH - " + fitch);
					}
				} else if (sp != null) {
					activeRating.append(STANDARD + sp);
					if (fitch != null) {
						activeRating.append(" | FITCH - " + fitch);
					}
				} else if (fitch != null) {
					activeRating.append("FITCH - " + fitch);
				}
			}
		}

		return activeRating.toString();
	}

	/**
	 * Method Get contract threshold
	 * 
	 * @param marginCall
	 * @param creditRating
	 * @return double
	 */
	public double getThresholdCcy(final CollateralConfig marginCall, CreditRating creditRating) {

		// GLOBAL RATING
		if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.GLOBAL_RATING)) {

			MarginCallCreditRating mccCreditRating = null;
			try {
				mccCreditRating = CollateralUtilities.getLatestMCCreditRating(marginCall.getPoRatingsConfigId(),
						creditRating.getRatingValue(), creditRating.getAgencyName(), valueDate);
			} catch (Exception e) {
				Log.error(CANNOT_GET_MARGINCALLRATING_FOR_CONTRANT + EQUAL + marginCall.getId() + COMMA + BLANK + AGENCY
						+ EQUAL + creditRating.getAgencyName() + COMMA + BLANK + RATING + EQUAL
						+ creditRating.getRatingValue(), e);
			}
			if (mccCreditRating != null) {
				return ELBEandKGRutilities.getThresholdDependingOnRating(marginCall, mccCreditRating,
						marginCall.getPoRatingsConfigId(), processDate, valueDate);
			}

		}
		// AMOUNT
		if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.AMOUNT)) {
			return ELBEandKGRutilities.getThresholdDependingOnAmount(marginCall, "PO", valueDate);
		}
		// MC_PERCENT
		// MIG_V14
		// PERCENT
		if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.PERCENT)) {
			return ELBEandKGRutilities.getThresholdDependingOnPercent(marginCall, "PO", processDate);
		}
		// BOTH
		if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, "BOTH")) {
			return ELBEandKGRutilities.getThresholdDependingOnBoth(marginCall, "PO", processDate, valueDate);
		}

		return 0.00;

	}

	// Set threshold notches
	public void calculateThresholdNotches(ELBEAgreementsExtractionItem item, CollateralConfig marginCall,
			CreditRating creditRating, double thresActive) {
		// threshold notches
		if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.GLOBAL_RATING)) {
			// notch 1
			CreditRating crNotch1 = getCreditRatingNotch(creditRating, globalRatingConfig, 1);
			if (crNotch1 != null) {
				setThresholdNotch1Stuff(item, marginCall, crNotch1, thresActive);
			}
			// notch 2
			CreditRating crNotch2 = getCreditRatingNotch(creditRating, globalRatingConfig, 2);
			if (crNotch2 != null) {
				setThresholdNotch2Stuff(item, marginCall, crNotch2, thresActive);
			}
			// notch 3
			CreditRating crNotch3 = getCreditRatingNotch(creditRating, globalRatingConfig, 3);
			if (crNotch3 != null) {
				setThresholdNotch3Stuff(item, marginCall, crNotch3, thresActive);
			}
		} else {
			setNonRatingDependingThresholdStuff(item);
		}
	}

	// Set threshold for 1 notch: get threshold from 1notch rating, impact and
	// 1notch rating values
	public void setThresholdNotch1Stuff(ELBEAgreementsExtractionItem item, CollateralConfig marginCall,
			CreditRating creditRating, double thresActive) {

		double thresActiveDown = Math.abs(getThresholdCcy(marginCall, creditRating));

		// threshold active down 1 notch ccy
		item.setThresholdDown1notch(thresActiveDown);
		item.setThresholdActiveDown1notchCCY(CollateralUtilities.fillWithZeros(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities.formatNumber(thresActiveDown), ",", "."), 18));

		// threshold active down 1 notch eur
		item.setThresholdActiveDown1notchEUR(
				CollateralUtilities.fillWithZeros(
						CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
								.formatNumber(thresActiveDown * ELBEAgreementsExtractionLogic.fxRateCcyEur), ",", "."),
						18));

		// impactos
		item.setImpactDown1notchCCY(
				CollateralUtilities.convertToReportDecimalFormat(
						CollateralUtilities.fillWithZeros(
								CollateralUtilities.formatNumber(Math.abs(thresActive - thresActiveDown)), 18),
						",", "."));
		item.setImpactDown1notchEUR(
				CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities.fillWithZeros(
						CollateralUtilities.formatNumber(
								(Math.abs(thresActive - thresActiveDown)) * ELBEAgreementsExtractionLogic.fxRateCcyEur),
						18), ",", "."));

	}

	// Set threshold for 2 notch: get threshold from 2notch rating, impact and
	// 2notch rating values
	public void setThresholdNotch2Stuff(ELBEAgreementsExtractionItem item, CollateralConfig marginCall,
			CreditRating creditRating, double thresActive) {

		double thresActiveDown = Math.abs(getThresholdCcy(marginCall, creditRating));

		// threshold active down 2 notch ccy
		item.setThresholdDown2notch(thresActiveDown);
		item.setThresholdActiveDown2notchCCY(CollateralUtilities.fillWithZeros(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities.formatNumber(thresActiveDown), ",", "."), 18));

		// threshold active down 2 notch eur
		item.setThresholdActiveDown2notchEUR(
				CollateralUtilities.fillWithZeros(
						CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
								.formatNumber(thresActiveDown * ELBEAgreementsExtractionLogic.fxRateCcyEur), ",", "."),
						18));

		// impactos
		item.setImpactDown2notchCCY(
				CollateralUtilities.convertToReportDecimalFormat(
						CollateralUtilities.fillWithZeros(
								CollateralUtilities.formatNumber(Math.abs(thresActive - thresActiveDown)), 18),
						",", "."));
		item.setImpactDown2notchEUR(
				CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities.fillWithZeros(
						CollateralUtilities.formatNumber(
								(Math.abs(thresActive - thresActiveDown)) * ELBEAgreementsExtractionLogic.fxRateCcyEur),
						18), ",", "."));

	}

	// Set threshold for 3 notch: get threshold from 3notch rating, impact and
	// 3notch rating values
	public void setThresholdNotch3Stuff(ELBEAgreementsExtractionItem item, CollateralConfig marginCall,
			CreditRating creditRating, double thresActive) {

		double thresActiveDown = Math.abs(getThresholdCcy(marginCall, creditRating));

		// threshold active down 3 notch ccy
		item.setThresholdDown3notch(thresActiveDown);
		item.setThresholdActiveDown3notchCCY(CollateralUtilities.fillWithZeros(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities.formatNumber(thresActiveDown), ",", "."), 18));

		// threshold active down 3 notch eur
		item.setThresholdActiveDown3notchEUR(
				CollateralUtilities.fillWithZeros(
						CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
								.formatNumber(thresActiveDown * ELBEAgreementsExtractionLogic.fxRateCcyEur), ",", "."),
						18));

		// impactos
		item.setImpactDown3notchCCY(
				CollateralUtilities.convertToReportDecimalFormat(
						CollateralUtilities.fillWithZeros(
								CollateralUtilities.formatNumber(Math.abs(thresActive - thresActiveDown)), 18),
						",", "."));
		item.setImpactDown3notchEUR(
				CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities.fillWithZeros(
						CollateralUtilities.formatNumber(
								(Math.abs(thresActive - thresActiveDown)) * ELBEAgreementsExtractionLogic.fxRateCcyEur),
						18), ",", "."));

	}

	// Get contract IA - (taking in account po and cpty sides)
	public double getIndAmountCcy(final CollateralConfig marginCall, CreditRating indAmountRequiredRatingOwner,
			CreditRating indAmountRequiredRatingCpty, PricingEnv pricingEnv) {

		MarginCallCreditRating mccCreditRating = null;
		double ownerIA = 0.0;
		double cptyIA = 0.0;

		if (ELBEandKGRutilities.isIADependingOnRating(marginCall)
				&& !ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {
			try {
				mccCreditRating = CollateralUtilities.getLatestMCCreditRating(marginCall.getPoRatingsConfigId(),
						indAmountRequiredRatingOwner.getRatingValue(), indAmountRequiredRatingOwner.getAgencyName(),
						valueDate);
			} catch (Exception e) {
				Log.error(CANNOT_GET_MARGINCALLRATING_FOR_CONTRANT + EQUAL + marginCall.getId() + COMMA + BLANK + AGENCY
						+ EQUAL + indAmountRequiredRatingOwner.getAgencyName() + COMMA + BLANK + RATING + EQUAL
						+ indAmountRequiredRatingOwner.getRatingValue(), e);
				return 0.0;
			}
			if (mccCreditRating != null) {
				return -1 * ELBEandKGRutilities.getIndAmountDependingOnRating(marginCall, mccCreditRating,
						marginCall.getPoRatingsConfigId(), processDate, valueDate);
			}
		} else if (!ELBEandKGRutilities.isIADependingOnRating(marginCall)
				&& ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {
			try {
				mccCreditRating = CollateralUtilities.getLatestMCCreditRating(marginCall.getLeRatingsConfigId(),
						indAmountRequiredRatingCpty.getRatingValue(), indAmountRequiredRatingCpty.getAgencyName(),
						valueDate);
			} catch (Exception e) {
				Log.error(CANNOT_GET_MARGINCALLRATING_FOR_CONTRANT + EQUAL + marginCall.getId() + COMMA + BLANK + AGENCY
						+ EQUAL + indAmountRequiredRatingCpty.getAgencyName() + COMMA + BLANK + RATING + EQUAL
						+ indAmountRequiredRatingCpty.getRatingValue(), e);
				return 0.0;
			}
			if (mccCreditRating != null) {
				return Math.abs(ELBEandKGRutilities.getIndAmountDependingOnRating(marginCall, mccCreditRating,
						marginCall.getLeRatingsConfigId(), processDate, valueDate));
			}
		} else if (ELBEandKGRutilities.isIADependingOnRating(marginCall)
				&& ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {
			// OWNER
			try {
				mccCreditRating = CollateralUtilities.getLatestMCCreditRating(marginCall.getPoRatingsConfigId(),
						indAmountRequiredRatingOwner.getRatingValue(), indAmountRequiredRatingOwner.getAgencyName(),
						valueDate);
			} catch (Exception e) {
				Log.error(CANNOT_GET_MARGINCALLRATING_FOR_CONTRANT + EQUAL + marginCall.getId() + COMMA + BLANK + AGENCY
						+ EQUAL + indAmountRequiredRatingOwner.getAgencyName() + COMMA + BLANK + RATING + EQUAL
						+ indAmountRequiredRatingOwner.getRatingValue(), e);
				return 0.0;
			}
			if (mccCreditRating != null) {
				ownerIA = ELBEandKGRutilities.getIndAmountDependingOnRating(marginCall, mccCreditRating,
						marginCall.getPoRatingsConfigId(), processDate, valueDate);
			}
			// CPTY
			try {
				mccCreditRating = CollateralUtilities.getLatestMCCreditRating(marginCall.getLeRatingsConfigId(),
						indAmountRequiredRatingCpty.getRatingValue(), indAmountRequiredRatingCpty.getAgencyName(),
						valueDate);
			} catch (Exception e) {
				Log.error("Cannot get MarginCallCreditRating for contract=" + marginCall.getId() + ", agency="
						+ indAmountRequiredRatingCpty.getAgencyName() + ", rating="
						+ indAmountRequiredRatingCpty.getRatingValue(), e);
				return 0.0;
			}
			if (mccCreditRating != null) {
				cptyIA = ELBEandKGRutilities.getIndAmountDependingOnRating(marginCall, mccCreditRating,
						marginCall.getLeRatingsConfigId(), processDate, valueDate);
			}
			return cptyIA - ownerIA;
		} else if (!ELBEandKGRutilities.isIADependingOnRating(marginCall)
				&& !ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {
			double iaValue = ELBEandKGRutilities.getContractIA(marginCall);
			String iaCcy = ELBEandKGRutilities.getContractIAccy(marginCall);
			return iaValue
					* CollateralUtilities.getFXRatebyQuoteSet(valueDate, marginCall.getCurrency(), iaCcy, pricingEnv);
		}

		return 0.00;

	}

	/**
	 * Methos getIndAmountActivo BAU - put as IA activo netted value from CM, used
	 * in Daily Task
	 * 
	 * @param entries
	 * @return
	 */
	public double getIndAmountActivo(final List<MarginCallEntryDTO> entries, final boolean marginCallEmpty) {

		double total = 0.00;

		if ((marginCallEmpty) && (!entries.isEmpty())) {
			total = entries.get(0).getIndependentAmount();
		}

		return total;

	}

	/**
	 * Method Calculate IANotches that Set IA notches
	 * 
	 * @param elbeAgreeExtItem
	 * @param marginCall
	 * @param indAmountRequiredRatingOwner
	 * @param indAmountRequiredRatingCpty
	 * @param iaActiveCcy
	 * @param pricingEnv
	 */
	public void calculateIANotches(ELBEAgreementsExtractionItem elbeAgreeExtItem, CollateralConfig marginCall,
			CreditRating indAmountRequiredRatingOwner, CreditRating indAmountRequiredRatingCpty, double iaActiveCcy,
			PricingEnv pricingEnv) {

		CreditRating crNotchOwner = null;
		double iaNotch;

		if (ELBEandKGRutilities.isIADependingOnRating(marginCall)
				&& !ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {

			// notch 1
			crNotchOwner = getCreditRatingNotch(indAmountRequiredRatingOwner, globalRatingConfig, 1);
			if (crNotchOwner != null) {
				iaNotch = getIndAmountCcy(marginCall, crNotchOwner, null, pricingEnv);
				setIANotch1Stuff(elbeAgreeExtItem, iaNotch, iaActiveCcy);
			}
			// notch 2
			crNotchOwner = getCreditRatingNotch(indAmountRequiredRatingOwner, globalRatingConfig, 2);
			if (crNotchOwner != null) {
				iaNotch = getIndAmountCcy(marginCall, crNotchOwner, null, pricingEnv);
				setIANotch2Stuff(elbeAgreeExtItem, iaNotch, iaActiveCcy);
			}
			// notch 3
			crNotchOwner = getCreditRatingNotch(indAmountRequiredRatingOwner, globalRatingConfig, 3);
			if (crNotchOwner != null) {
				iaNotch = getIndAmountCcy(marginCall, crNotchOwner, null, pricingEnv);
				setIANotch3Stuff(elbeAgreeExtItem, iaNotch, iaActiveCcy);
			}

		} else if (!ELBEandKGRutilities.isIADependingOnRating(marginCall)
				&& ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {

			// impact is zero (because there's no owner rating notches)
			setIANotch1Stuff(elbeAgreeExtItem, 0.0, 0.0);
			setIANotch2Stuff(elbeAgreeExtItem, 0.0, 0.0);
			setIANotch3Stuff(elbeAgreeExtItem, 0.0, 0.0);

		} else if (ELBEandKGRutilities.isIADependingOnRating(marginCall)
				&& ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {

			// notch 1
			crNotchOwner = getCreditRatingNotch(indAmountRequiredRatingOwner, globalRatingConfig, 1);
			if (crNotchOwner != null) {
				iaNotch = getIndAmountCcy(marginCall, crNotchOwner, indAmountRequiredRatingCpty, pricingEnv);
				setIANotch1Stuff(elbeAgreeExtItem, iaNotch, iaActiveCcy);
			}
			// notch 2
			crNotchOwner = getCreditRatingNotch(indAmountRequiredRatingOwner, globalRatingConfig, 2);
			if (crNotchOwner != null) {
				iaNotch = getIndAmountCcy(marginCall, crNotchOwner, indAmountRequiredRatingCpty, pricingEnv);
				setIANotch2Stuff(elbeAgreeExtItem, iaNotch, iaActiveCcy);
			}
			// notch 3
			crNotchOwner = getCreditRatingNotch(indAmountRequiredRatingOwner, globalRatingConfig, 3);
			if (crNotchOwner != null) {
				iaNotch = getIndAmountCcy(marginCall, crNotchOwner, indAmountRequiredRatingCpty, pricingEnv);
				setIANotch3Stuff(elbeAgreeExtItem, iaNotch, iaActiveCcy);
			}
		} else if (!ELBEandKGRutilities.isIADependingOnRating(marginCall)
				&& !ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {

			// impacts empty
			setNonRatingDependingIAstuff(elbeAgreeExtItem);

		}
	}

	// Set IA for 1 notch
	public void setIANotch1Stuff(ELBEAgreementsExtractionItem item, double iaNotch, double iaActive) {

		// independent amount down 1 notch ccy
		item.setIADown1notchCCY(CollateralUtilities.fillWithZeros(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities.formatNumber(iaNotch - iaActive), ",", "."), 18));

		// independent amount down 1 notch eur
		item.setIADown1notchEUR(CollateralUtilities.fillWithZeros(
				CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
						.formatNumber((iaNotch - iaActive) * ELBEAgreementsExtractionLogic.fxRateCcyEur), ",", "."),
				18));

	}

	// Set IA for 2 notch
	public void setIANotch2Stuff(ELBEAgreementsExtractionItem item, double iaNotch, double iaActive) {

		// independent amount down 2 notch ccy
		item.setIADown2notchCCY(CollateralUtilities.fillWithZeros(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities.formatNumber(iaNotch - iaActive), ",", "."), 18));
		// independent amount down 2 notch eur
		item.setIADown2notchEUR(CollateralUtilities.fillWithZeros(
				CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
						.formatNumber((iaNotch - iaActive) * ELBEAgreementsExtractionLogic.fxRateCcyEur), ",", "."),
				18));

	}

	/**
	 * Method set IANotch3Stuff that Set IA for 3 notch: get IA from 3notch rating
	 * 
	 * @param item
	 * @param iaNotch
	 * @param iaActive
	 */
	public void setIANotch3Stuff(ELBEAgreementsExtractionItem item, double iaNotch, double iaActive) {

		// independent amount down 3 notch ccy
		item.setIADown3notchCCY(CollateralUtilities.fillWithZeros(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities.formatNumber(iaNotch - iaActive), ",", "."), 18));

		// independent amount down 3 notch eur
		item.setIADown3notchEUR(CollateralUtilities.fillWithZeros(
				CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
						.formatNumber((iaNotch - iaActive) * ELBEAgreementsExtractionLogic.fxRateCcyEur), ",", "."),
				18));

	}

	/**
	 * Method getCreditRatingNotch that Create a credit rating x notch form a given
	 * credit rating
	 * 
	 * @param creditRating
	 * @param globalRatingConfig
	 * @param notch
	 * @return
	 */
	public CreditRating getCreditRatingNotch(CreditRating creditRating, GlobalRatingConfiguration globalRatingConfig,
			int notch) {

		String notchValue = null;
		String name = "";
		if (creditRating != null) {
			notchValue = getRatingNotchValue(creditRating, globalRatingConfig, notch);
			name = creditRating.getAgencyName();
		}

		if (!Util.isEmpty(notchValue)) {
			CreditRating crNotch = new CreditRating();
			crNotch.setAgencyName(name);
			crNotch.setRatingValue(notchValue);
			return crNotch;
		}

		return null;
	}

	/**
	 * Method getRatingNotchValue that Get credit rating x notch value from a given
	 * credit rating
	 * 
	 * @param creditRating
	 * @param globalRatingConfig
	 * @param notch
	 * @return ratingNotchValue
	 */
	public String getRatingNotchValue(CreditRating creditRating, GlobalRatingConfiguration globalRatingConfig,
			int notch) {

		GlobalRating globalRating = ELBEandKGRutilities.getAgencyGlobalRating(globalRatingConfig,
				creditRating.getAgencyName());
		if (globalRating == null) {
			Log.error(this, "No Ratings exists for Agency=" + creditRating.getAgencyName());
			return null;
		}
		int actualPriority = ELBEandKGRutilities.getAgencyRatingPriority(globalRating, creditRating.getRatingValue());
		if (actualPriority == -1) {
			Log.error(this, "No priority exists for rating=" + creditRating.getRatingValue());
			return null;
		}
		int notchPriority = actualPriority + notch;
		return ELBEandKGRutilities.getAgencyRatingValue(globalRating, notchPriority);
	}

	/**
	 * Method calulateRatingNotches that Get rating notches
	 * 
	 * @param item
	 * @param marginCall
	 * @param thresCreditRating
	 * @param iaCreditRating
	 * @param agencies
	 */
	public void calculateRatingNotches(ELBEAgreementsExtractionItem item, CollateralConfig marginCall,
			CreditRating thresCreditRating, CreditRating iaCreditRating, List<String> agencies) {

		CreditRating creditRating = null;

		if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.GLOBAL_RATING)
				|| ELBEandKGRutilities.isIADependingOnRating(marginCall)) {

			if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.GLOBAL_RATING)) {
				creditRating = thresCreditRating;
			} else {
				creditRating = iaCreditRating;
			}

			// notch 1
			CreditRating crNotch1 = getCreditRatingNotch(creditRating, globalRatingConfig, 1);
			if (crNotch1 != null) {
				// rating down 1
				item.setRatDown1notch(CollateralUtilities.fillWithBlanks(getActiveRating(crNotch1, agencies), 60));
			}
			// notch 2
			CreditRating crNotch2 = getCreditRatingNotch(creditRating, globalRatingConfig, 2);
			if (crNotch2 != null) {
				// rating down 2
				item.setRatDown2notch(CollateralUtilities.fillWithBlanks(getActiveRating(crNotch2, agencies), 60));
			}
			// notch 3
			CreditRating crNotch3 = getCreditRatingNotch(creditRating, globalRatingConfig, 3);
			if (crNotch3 != null) {
				// rating down 3
				item.setRatDown3notch(CollateralUtilities.fillWithBlanks(getActiveRating(crNotch3, agencies), 60));
			}
		} else {
			item.setRatDown1notch(CollateralUtilities.fillWithBlanks(BLANK, 60));
			item.setRatDown2notch(CollateralUtilities.fillWithBlanks(BLANK, 60));
			item.setRatDown3notch(CollateralUtilities.fillWithBlanks(BLANK, 60));
		}

	}

	/**
	 * Method getCodLayout
	 * 
	 * @return codLayout
	 */
	public String getCodLayout() {
		return COD_LAYOUT;
	}

	/**
	 * Method getSourceApp
	 * 
	 * @return source_app
	 */
	public String getSourceApp() {
		return SOURCE_APP;
	}

	/**
	 * Method getValAgent
	 * 
	 * @param marginCall
	 * @return valAgent
	 */
	public String getValAgent(final CollateralConfig marginCall) {
		final LegalEntity po = marginCall.getProcessingOrg();
		if (po != null) {
			return po.getAuthName();
		}
		return BLANK;
	}

	/**
	 * Method getColAgreement that get colAgreement
	 * 
	 * @param marginCall
	 * @return colAgreement
	 */
	public String getColAgreement(final CollateralConfig marginCall) {
		final LegalEntity le = marginCall.getLegalEntity();
		if (le != null) {
			return le.getName();
		}
		return BLANK;
	}

	/**
	 * Method getShortName that get Short Name
	 * 
	 * @param marginCall
	 * @return shortName
	 */
	public String getShortname(final CollateralConfig marginCall) {
		String name = marginCall.getName();
		if (name == null) {
			name = BLANK;
		}
		return name;
	}

	/**
	 * Method getBases
	 * 
	 * @param marginCall
	 * @return baseCCY
	 */
	public String getBaseCCY(final CollateralConfig marginCall) {
		return marginCall.getCurrency();
	}

	/**
	 * Method getContracType
	 * 
	 * @param marginCall
	 * @return contranctType
	 */
	public String getContractType(final CollateralConfig marginCall) {
		return marginCall.getContractType();
	}

	/**
	 * Method getActiveValues
	 * 
	 * @param marginCall
	 * @return activeValues
	 */
	public String getActiveValues(final CollateralConfig marginCall) {
		if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.GLOBAL_RATING)) {
			return "R";
		} else {
			return "F";
		}
	}

	/**
	 * Method getIndAmoutAcitveValues
	 * 
	 * @param marginCall
	 * @return indAmountActiveValuesS
	 */

	public String getIndAmountActiveValues(final CollateralConfig marginCall) {
		if (ELBEandKGRutilities.isIADependingOnRating(marginCall)) {
			return "R";
		} else {
			return "F";
		}
	}

	/**
	 * Method getBalanceCCY
	 * 
	 * @param marginCall
	 * @param dsConn
	 * @param define
	 * @param entries
	 * @return balanceCCY
	 */
	public static double getBalanceCCY(boolean define, final List<MarginCallEntryDTO> entries,
			final boolean marginCallEmpty) {
		return getBalanceCashCCY(define, entries, marginCallEmpty)
				+ getBalanceStockCCY(define, entries, marginCallEmpty);
	}

	/**
	 * Method getHaircut
	 * 
	 * @param marginCall
	 * @param dsConn
	 * @param entries
	 * @return haircut
	 */
	public String getHaircut(final List<MarginCallEntryDTO> entries, final boolean marginCallEmpty) {
		StringBuilder haircutList = new StringBuilder();
		List<SecurityPositionDTO> securityPositions = getSecurities(entries, marginCallEmpty);
		if ((securityPositions != null) && (!securityPositions.isEmpty())) {
			for (int i = 0; i < securityPositions.size(); i++) {
				SecurityPositionDTO securityPosition = securityPositions.get(i);
				if ((securityPosition != null) && (securityPosition.getContractValue() != 0)) {
					// not include zero positions
					Double haircutValue = 100 - (securityPosition.getHaircut() * 100);
					if (haircutList.length() > 0) {
						haircutList.append("|");
					}
					haircutList.append(haircutValue.toString());
				}
			}
		}
		return haircutList.toString();
	}

	/**
	 * Get Securities
	 * 
	 * @param dsConn
	 * @param date
	 * @param entries
	 * @return
	 */
	public List<SecurityPositionDTO> getSecurities(final List<MarginCallEntryDTO> entries,
			final boolean marginCallEmpty) {
		List<SecurityPositionDTO> sucurities = null;

		if ((marginCallEmpty) && (!entries.isEmpty())) {
			// get security positions
			sucurities = entries.get(0).getPreviousSecurityPosition() != null
					? entries.get(0).getPreviousSecurityPosition().getPositions()
					: null;
		}
		return sucurities;
	}

	/**
	 * Method getDSSecurity
	 * 
	 * @param marginCall
	 * @param dsConn
	 * @param entries
	 * @return DSSecurity
	 */
	public String getDSSecurity(final List<MarginCallEntryDTO> entries, final boolean marginCallEmpty) {
		StringBuilder dsSec = new StringBuilder();
		Product product;
		List<SecurityPositionDTO> securityPositions = getSecurities(entries, marginCallEmpty);
		if ((securityPositions != null) && (!securityPositions.isEmpty())) {
			for (int i = 0; i < securityPositions.size(); i++) {
				SecurityPositionDTO securityPosition = securityPositions.get(i);
				// not include zero positions
				if (securityPosition != null && securityPosition.getContractValue() != 0) {
					// GSM: 08/09/14. Adaptation to accept equities too
					product = securityPosition.getProduct();
					if (product != null) {
						if (dsSec.length() > 0) {
							dsSec.append("|");
						}
						dsSec.append(product.getName());
					}
				}
			}
		}
		return dsSec.toString();
	}

	/**
	 * Method getSign
	 * 
	 * @param dsConn
	 * @param pricingEnv
	 * @return sign
	 */
	public String getSign(final double balanceCCYTrue, final double balanceCCYFalse) {
		if ((balanceCCYFalse + balanceCCYTrue) >= 0) {
			return COLLECT;
		} else {
			return PAY;
		}
	}

	/**
	 * Method getBalanceCashCCY
	 * 
	 * @param define
	 * @param entries
	 * @return BalanceCashCCy
	 */
	public static double getBalanceCashCCY(boolean define, final List<MarginCallEntryDTO> entries,
			final boolean marginCallEmpty) {

		Double total = 0.00;
		List<CashAllocationDTO> cl;

		if ((marginCallEmpty) && (!entries.isEmpty())) {
			if (!define) {
				total = entries.get(0).getPreviousCashMargin();
			}
			// cash
			cl = entries.get(0).getCashAllocations();
			if ((cl != null) && (!cl.isEmpty())) {
				total = calculateTotal(total, cl, define);

			}
		}

		return total;
	}

	/**
	 * Method Calculate Total
	 * 
	 * @param total
	 * @param cl
	 * @param define
	 * @return
	 */
	private static double calculateTotal(double total, List<CashAllocationDTO> cl, boolean define) {
		if (!define) {
			for (int i = 0; i < cl.size(); i++) {
				if (!cl.get(i).getSettlementDate().after(processDate)) {
					total += cl.get(i).getContractValue();
				}
			}
		}
		if (define) {
			for (int i = 0; i < cl.size(); i++) {
				if (cl.get(i).getSettlementDate().after(processDate)) {
					total += cl.get(i).getContractValue();
				}
			}
		}
		return total;
	}

	/**
	 * Method getBalanceCashEUR
	 * 
	 * @param marginCall
	 * @param fxrate
	 * @param dsConn
	 * @param define
	 * @param entries
	 * @return BalanceCashEUR
	 */
	public static double getBalanceCashEUR(double fxrate, boolean define, final List<MarginCallEntryDTO> entries,
			final boolean marginCallEmpty) {
		return getBalanceCashCCY(define, entries, marginCallEmpty) * fxrate;
	}

	/**
	 * Method getBalanceStockCCY
	 * 
	 * @param define
	 * @param entries
	 * @return getBalanceStockCCY
	 */
	public static double getBalanceStockCCY(boolean define, final List<MarginCallEntryDTO> entries,
			final boolean marginCallEmpty) {

		double total = 0.00;
		PreviousPositionDTO<SecurityPositionDTO> previousSecurityPosition;

		if ((marginCallEmpty) && (!entries.isEmpty())) {

			if (!define) {
				previousSecurityPosition = entries.get(0).getPreviousSecurityPosition();
				if (null != previousSecurityPosition) {
					total = getPreviousSecurityPosition(previousSecurityPosition, total);
				}
			}
			// securities
			List<SecurityAllocationDTO> sl = entries.get(0).getSecurityAllocations();

			if ((sl != null) && !sl.isEmpty()) {
				if (!define) {
					total = getSecurityPositionDefineFalse(sl, total);
				} else {
					total = getSecurityPositionDefineTrue(sl, total);
				}
			}
		}
		return total;
	}

	/**
	 * Method getSecurityPositionDefineTrue
	 * 
	 * @param sl
	 * @param total
	 * @return total
	 */
	private static double getSecurityPositionDefineTrue(List<SecurityAllocationDTO> sl, double total) {

		for (int j = 0; j < sl.size(); j++) {
			if (sl.get(j).getSettlementDate().after(processDate))
				total += sl.get(j).getContractValue();
		}

		return total;
	}

	/**
	 * Method getSecurityPositionDefineFalse
	 * 
	 * @param sl
	 * @param total
	 * @return total
	 */
	private static double getSecurityPositionDefineFalse(List<SecurityAllocationDTO> sl, double total) {

		for (int j = 0; j < sl.size(); j++) {
			if (!sl.get(j).getSettlementDate().after(processDate))
				total += sl.get(j).getContractValue();
		}
		return total;
	}

	/**
	 * Method getPreviousSecurityPosition
	 * 
	 * @param previousSecurityPosition
	 * @param total
	 * @return total
	 */
	private static double getPreviousSecurityPosition(PreviousPositionDTO<SecurityPositionDTO> previousSecurityPosition,
			double total) {
		Double value;
		double rate;

		for (SecurityPositionDTO position : previousSecurityPosition.getPositions()) {
			value = position.getValue();
			rate = position.getFxRate();
			total += value * rate;
		}
		return total;
	}

	/**
	 * Method getBalanceStockEUR
	 * 
	 * @param fxrate
	 * @param dsConn
	 * @param define
	 * @param entries
	 * @return
	 */
	public static double getBalanceStockEUR(double fxrate, boolean define, final List<MarginCallEntryDTO> entries,
			final boolean marginCallEmpty) {
		return getBalanceStockCCY(define, entries, marginCallEmpty) * fxrate;
	}

	/**
	 * Method getStatus
	 * 
	 * @param dsConn
	 * @param entries
	 * @return status
	 */
	public String getStatus(final List<MarginCallEntryDTO> entries, final boolean marginCallEmpty) {

		if ((marginCallEmpty) && (!entries.isEmpty())) {
			String status = entries.get(0).getStatus();
			if ((status.equals(PART_EXECUTED)) || (status.equals(FULL_EXECUTED)) || (status.equals(PRICED_NO_CALL))) {
				return "V";
			} else {
				return "P";
			}
		}

		return "P";
	}

	/**
	 * Method getGrossExposureCCY
	 * 
	 * @param entries
	 * @return GrossExposureCCY
	 */
	public double getGrossExposureCCY(List<MarginCallEntryDTO> entries, final boolean marginCallEmpty) {

		double total = 0.00;

		if ((marginCallEmpty) && (!entries.isEmpty())) {
			total = entries.get(0).getNetBalance();
		}
		return total;
	}

	/**
	 * Method getGrossExposureEUR
	 * 
	 * @param marginCall
	 * @param dsConn
	 * @param pricingEnv
	 * @param entries
	 * @return GrossExposureEUR
	 */
	public double getGrossExposureEUR(final CollateralConfig marginCall, final PricingEnv pricingEnv,
			List<MarginCallEntryDTO> entries, final boolean marginCallEmpty) {
		return getGrossExposureCCY(entries, marginCallEmpty)
				* CollateralUtilities.getFXRatebyQuoteSet(processDate, marginCall.getCurrency(), EURO, pricingEnv);
	}

	/**
	 * Method getMarginCallCCY
	 * 
	 * @param dsConn
	 * @param entries
	 * @return MarginCallCCY
	 */
	public double getMarginCallCCY(final List<MarginCallEntryDTO> entries, final boolean marginCallEmpty) {
		double total = 0.00;

		if ((marginCallEmpty) && (!entries.isEmpty())) {
			total = entries.get(0).getGlobalRequiredMargin();
		}

		return total;
	}

	/**
	 * Method getMarginCallCCY2
	 * 
	 * @param dsConn
	 * @param entries
	 * @return MarginCallCCY2
	 */
	public static double getMarginCallCCY2(final List<MarginCallEntryDTO> entries, final boolean marginCallEmpty) {
		double total = 0.00;

		if ((marginCallEmpty) && (!entries.isEmpty())) {
			// cash
			List<CashAllocationDTO> cl = entries.get(0).getCashAllocations();
			if ((cl != null) && (!cl.isEmpty())) {
				for (int i = 0; i < cl.size(); i++) {
					total += cl.get(i).getContractValue();
				}
			}
			// securities
			List<SecurityAllocationDTO> sl = entries.get(0).getSecurityAllocations();
			if ((sl != null) && (!sl.isEmpty())) {
				for (int j = 0; j < sl.size(); j++) {
					total += sl.get(j).getContractValue();
				}
			}
		}
		return total;
	}

	/**
	 *
	 * Method getMarginCallCCY3
	 *
	 * @param entries
	 * @param marginCallEmpty
	 * @param marginCall
	 * @return
	 */

	/*
	[BAU] Margin Call column fixed. Theoretical Outstanding Margin Column in case of Triparty with Net Exposure as Calculation Type.
	 */

	public static double getMarginCallCCY3(final List<MarginCallEntryDTO> entries, final boolean marginCallEmpty, final CollateralConfig marginCall){
		double total = 0.00;

		if ((marginCallEmpty) && (!entries.isEmpty())) {
			MarginCallEntryDTO entry = entries.get(0);

			if(marginCall != null && marginCall.isTriParty() && "Net Exposure".equalsIgnoreCase(marginCall.getAdditionalField("TRIPARTY_CALCULATION_TYPE"))){
				double totalTheoreticalTotalPreviousPosition = entry.getPreviousTheoreticalCashMargin() + entry.getPreviousTheoreticalSecurityMargin();
				boolean isDefaultOrValueDate = "POSITION_DATE_DEFAULT".equals(marginCall.getPositionDateType()) || "POSITION_DATE_VALUE".equals(marginCall.getPositionDateType());
				boolean isProcessOrLastKnownDate =  "POSITION_DATE_PROCESS".equals(marginCall.getPositionDateType()) || "POSITION_DATE_LAST_KNOWN".equals(marginCall.getPositionDateType());

				if(isDefaultOrValueDate){
					total = entry.getTheoreticalNetBalance() - (totalTheoreticalTotalPreviousPosition + entry.getDailyTotalMargin());
				}else if(isProcessOrLastKnownDate){
					total = entry.getTheoreticalNetBalance() - totalTheoreticalTotalPreviousPosition;
				}
			}else{
				total = entry.getGlobalRequiredMargin();
			}

		}

		return total;
	}

	/**
	 * Method setNonRatingDependingThresholdStuff
	 * 
	 * @param item
	 */
	public static void setNonRatingDependingThresholdStuff(ELBEAgreementsExtractionItem item) {
		// notch 1
		item.setThresholdActiveDown1notchCCY(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setThresholdActiveDown1notchEUR(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setImpactDown1notchCCY(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setImpactDown1notchEUR(CollateralUtilities.fillWithBlanks(BLANK, 18));
		// notch 2
		item.setThresholdActiveDown2notchCCY(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setThresholdActiveDown2notchEUR(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setImpactDown2notchCCY(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setImpactDown2notchEUR(CollateralUtilities.fillWithBlanks(BLANK, 18));
		// notch 3
		item.setThresholdActiveDown3notchCCY(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setThresholdActiveDown3notchEUR(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setImpactDown3notchCCY(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setImpactDown3notchEUR(CollateralUtilities.fillWithBlanks(BLANK, 18));
	}

	/**
	 * Method setNonRatingDependingIAstuff
	 * 
	 * @param item
	 */
	public static void setNonRatingDependingIAstuff(ELBEAgreementsExtractionItem item) {
		// notch 1
		item.setIADown1notchCCY(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setIADown1notchEUR(CollateralUtilities.fillWithBlanks(BLANK, 18));
		// notch 2
		item.setIADown2notchCCY(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setIADown2notchEUR(CollateralUtilities.fillWithBlanks(BLANK, 18));
		// notch 3
		item.setIADown3notchCCY(CollateralUtilities.fillWithBlanks(BLANK, 18));
		item.setIADown3notchEUR(CollateralUtilities.fillWithBlanks(BLANK, 18));
	}

	/**
	 * Method used to get GlobalRating
	 */
	public static void getGloablRatingConfiguration() {
		if (globalRatingConfig == null) {
			try {
				globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
						.loadDefaultGlobalRatingConfiguration();
			} catch (RemoteException e) {
				Log.error(ELBEAgreementsExtractionItem.class, "Cannot get GlobalRatingConfiguration");
			}
		}
	}

	/**
	 * Method used to add the rows in the report generated.
	 *
	 * @param trade     Trade associated with the Repo object.
	 * @param dsConn    Database connection.
	 * @param errorMsgs Vector with the different errors occurred.
	 * @return Vector with the rows added.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<ELBEAgreementsExtractionItem> getReportRows(final CollateralConfig marginCall,
			final PricingEnv pricingEnv, final String date, final JDate date2, List stHolidays,
			final List<String> errorMsgs, boolean empty) {
		final ArrayList<ELBEAgreementsExtractionItem> reportRows = new ArrayList<>();
		final ELBEAgreementsExtractionLogic verifiedRow = new ELBEAgreementsExtractionLogic();
		ELBEAgreementsExtractionItem rowCreated = null;
		processDate = date2;
		fechaExt = date;
		List<MarginCallEntryDTO> entries = updateMarginCallEntriesDTO(marginCall);
		boolean marginCallEmpty = !Util.isEmpty(entries);
		final double balanceCCYTrue = getBalanceCCY(true, entries, marginCallEmpty);
		final double balanceCCYFalse = getBalanceCCY(false, entries, marginCallEmpty);	
		valueDate = processDate.addBusinessDays(-1, stHolidays);

		// marginCall row
		rowCreated = verifiedRow.getELBEAgreementsExtractionItem(marginCall, pricingEnv, entries, marginCallEmpty,
				balanceCCYTrue, balanceCCYFalse);
		if (null != rowCreated) {
			reportRows.add(rowCreated);
			if (!empty) {
				// define row
				ELBEAgreementsExtractionItem defineRow = changeToDefineRow(rowCreated, entries, marginCallEmpty,
						balanceCCYTrue, balanceCCYFalse);
				reportRows.add(defineRow);
			}
		}
		return reportRows;
	}

	/**
	 * Method that update MarginCallEntriesDTO
	 * 
	 * @param marginCall
	 */
	private static List<MarginCallEntryDTO> updateMarginCallEntriesDTO(CollateralConfig marginCall) {
		List<MarginCallEntryDTO> entries = null;

		final List<Integer> mccID = new ArrayList<>();

		mccID.add(marginCall.getId());
		try {
			// if there was any movement, we have one entry per contract/date
			entries = CollateralManagerUtil.loadMarginCallEntriesDTO(mccID, processDate);

		} catch (final RemoteException excep) {
			Log.error("Cannot get entries for marginCall Id " + marginCall.getId(), excep);
		}
		return entries;
	}

	/**
	 * Method that retrieve row by row from Calypso, to insert in the vector with
	 * the result to show.
	 *
	 * @param trade  Trade associated with the Repo object.
	 * @param dsConn Database connection.
	 * @param errors Vector with the different errors occurred.
	 * @return The row retrieved from the system, with the necessary information.
	 */
	private ELBEAgreementsExtractionItem getELBEAgreementsExtractionItem(final CollateralConfig marginCall,
			final PricingEnv pricingEnv, final List<MarginCallEntryDTO> entries, final boolean marginCallEmpty,
			final double balanceCCYTrue, final double balanceCCYFalse) {
		final ELBEAgreementsExtractionItem elbeAgreeExtItem = new ELBEAgreementsExtractionItem();
		Vector<String> agencies = null;
		List<CreditRating> ownerCreditRatings = null;
		List<CreditRating> cptyCreditRatings = null;
		CreditRating thresholdRequiredRating = null;
		CreditRating indAmountRequiredRating = null;
		CreditRating cptyIndAmountRequiredRating = null;
		CreditRating mtaRequiredRating = null;
		ELBEAgreementsExtractionLogic.fxRateCcyEur = CollateralUtilities.getFXRatebyQuoteSet(valueDate,
				marginCall.getCurrency(), EURO, pricingEnv);
		this.grossExposureCCY = getGrossExposureCCY(entries, marginCallEmpty);

		if (ELBEandKGRutilities.isELBEcontractDependingOnRating(marginCall)) {

			// get global rating config
			getGloablRatingConfiguration();
			if (globalRatingConfig == null) {
				return null;
			}

			// get contract rating agencies
			agencies = marginCall.getEligibleAgencies();

			if (ELBEandKGRutilities.isOwnerELBEcontractDependingOnRating(marginCall)) {
				// get agencies ratings for owner
				MarginCallCreditRatingConfiguration mccRatingConfigOwner = null;
				try {
					mccRatingConfigOwner = CollateralUtilities
							.getMCRatingConfiguration(marginCall.getPoRatingsConfigId());
				} catch (Exception e) {
					Log.error("Cannot get PO ratingMatrix for contract = " + marginCall.getName(), e);
					return null;
				}
				ownerCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(marginCall, agencies,
						marginCall.getProcessingOrg().getId(), valueDate, mccRatingConfigOwner.getRatingType());
				if (Util.isEmpty(ownerCreditRatings)) {
					Log.error(this, "Cannot get contract agencies ratings for owner");
					return null;
				}

				// threshold depends on rating
				if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.GLOBAL_RATING)) {
					// get threshold required rating
					thresholdRequiredRating = getRequiredRating(ownerCreditRatings,
							marginCall.getPoThresholdRatingDirection());
					if (thresholdRequiredRating == null) {
						Log.error(this, "Cannot get threshold required rating for contract = " + marginCall.getName());
						return null;
					}
				}

				// IA depends on rating
				if (ELBEandKGRutilities.isIADependingOnRating(marginCall)) {
					// get owner IA required rating
					indAmountRequiredRating = getRequiredRating(ownerCreditRatings,
							marginCall.getPoIARatingDirection());
					if (indAmountRequiredRating == null) {
						Log.error(this, "Cannot get IA required rating for owner");
						return null;
					}
				}

				// MTA depends on rating
				if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.GLOBAL_RATING)) {
					// get MTA required rating
					mtaRequiredRating = getRequiredRating(ownerCreditRatings, marginCall.getPoMTARatingDirection());
					if (mtaRequiredRating == null) {
						Log.error(this, "Cannot get MTA required rating for contract = " + marginCall.getName());
						return null;
					}
				}
			}

			if (ELBEandKGRutilities.isCptyELBEcontractDependingOnRating(marginCall)) {
				// get agencies ratings for cpty
				MarginCallCreditRatingConfiguration mccRatingConfigCpty = null;
				try {
					mccRatingConfigCpty = CollateralUtilities
							.getMCRatingConfiguration(marginCall.getLeRatingsConfigId());
				} catch (Exception e) {
					Log.error("Cannot get Cpty ratingMatrix for contract = " + marginCall.getName(), e);
					return null;
				}
				// get ratings for cpty
				cptyCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(marginCall, agencies,
						marginCall.getLegalEntity().getId(), valueDate, mccRatingConfigCpty.getRatingType());
				if (Util.isEmpty(cptyCreditRatings)) {
					Log.error(this, "Cannot get contract agencies ratings for cpty");
					return null;
				}

				// get cpty IA required rating
				cptyIndAmountRequiredRating = getRequiredRating(cptyCreditRatings, marginCall.getLeIARatingDirection());
				if (cptyIndAmountRequiredRating == null) {
					Log.error(this, "Cannot get IA required rating for cpty");
					return null;
				}
			}
		}

		// cod layout
		elbeAgreeExtItem.setCodLayout(getCodLayout());

		// extract date
		elbeAgreeExtItem.setExtractDate(fechaExt);

		// operation date
		elbeAgreeExtItem.setPosTransDate(changeFormatDate(processDate.toString(), "dd/MM/yyyy"));

		// source app
		elbeAgreeExtItem.setSourceApp(getSourceApp());

		// valuation agent
		elbeAgreeExtItem.setValAgent(CollateralUtilities.fillWithBlanks(getValAgent(marginCall), 30));

		// owner
		elbeAgreeExtItem.setOwner(CollateralUtilities.fillWithBlanks(getValAgent(marginCall), 30));

		// col agreement
		elbeAgreeExtItem.setColAgreement(CollateralUtilities.fillWithBlanks(getColAgreement(marginCall), 80));

		// shortname
		elbeAgreeExtItem.setShortname(CollateralUtilities.fillWithBlanks(getShortname(marginCall), 30));

		// base ccy
		elbeAgreeExtItem.setBaseCCY(getBaseCCY(marginCall));

		// fixing
		elbeAgreeExtItem.setFixing(CollateralUtilities.convertToReportDecimalFormat(
				formatFixing(
						CollateralUtilities.getFXRatebyQuoteSet(valueDate, marginCall.getCurrency(), EURO, pricingEnv)),
				",", "."));

		// contract type
		elbeAgreeExtItem.setContractType(CollateralUtilities.fillWithBlanks(getContractType(marginCall), 10));

		// active values
		elbeAgreeExtItem.setActiveValues(CollateralUtilities.fillWithBlanks(getActiveValues(marginCall), 1));

		// ia active values
		elbeAgreeExtItem.setIaActiveValues(CollateralUtilities.fillWithBlanks(getIndAmountActiveValues(marginCall), 1));

		// BAU - get active rating taking in account owne threshold and IA
		// active rating
		calculateActiveRating(elbeAgreeExtItem, marginCall, thresholdRequiredRating, indAmountRequiredRating, agencies);

		// BAU - process rating notches
		// rating notches
		calculateRatingNotches(elbeAgreeExtItem, marginCall, thresholdRequiredRating, indAmountRequiredRating,
				agencies);

		// processing MTA
		calculateMTANotches(elbeAgreeExtItem, marginCall, mtaRequiredRating);

		// treshold avtive ccy
		double thresActiveCcy;
		thresActiveCcy = Math.abs(getThresholdCcy(marginCall, thresholdRequiredRating));
		elbeAgreeExtItem.setThresholdActiveCCY(CollateralUtilities.fillWithZeros(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities.formatNumber(thresActiveCcy), ",", "."), 18));

		// treshold avtive eur
		elbeAgreeExtItem
				.setThresholdActiveEUR(CollateralUtilities.fillWithZeros(
						CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
								.formatNumber(thresActiveCcy * ELBEAgreementsExtractionLogic.fxRateCcyEur), ",", "."),
						18));

		// BAU - only process threshold fields depending on rating, not process
		// rating notches
		// threshold notches
		calculateThresholdNotches(elbeAgreeExtItem, marginCall, thresholdRequiredRating, thresActiveCcy);

		// BAU - new method to get ind amount activo
		// independent amount ccy
		double iAactiveCcy = getIndAmountActivo(entries, marginCallEmpty);

		elbeAgreeExtItem.setIAActiveCCY(CollateralUtilities.fillWithZeros(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities.formatNumber(iAactiveCcy), ",", "."), 18));

		// independent amount eur
		elbeAgreeExtItem
				.setIAActiveEUR(CollateralUtilities.fillWithZeros(
						CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
								.formatNumber(iAactiveCcy * ELBEAgreementsExtractionLogic.fxRateCcyEur), ",", "."),
						18));

		// BAU - only process IA fields depending on rating, not process rating
		// notches
		// IA notches
		calculateIANotches(elbeAgreeExtItem, marginCall, indAmountRequiredRating, cptyIndAmountRequiredRating,
				iAactiveCcy, pricingEnv);

		// balance ccy
		elbeAgreeExtItem.setBalanceCCY(CollateralUtilities.fillWithZeros(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities.formatNumber(balanceCCYFalse), ",", "."), 18));

		// balance eur
		elbeAgreeExtItem
				.setBalanceEUR(CollateralUtilities.fillWithZeros(
						CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
								.formatNumber(balanceCCYFalse * ELBEAgreementsExtractionLogic.fxRateCcyEur), ",", "."),
						18));

		// haircut
		elbeAgreeExtItem.setHaircut(CollateralUtilities.fillWithBlanks(getHaircut(entries, marginCallEmpty), 100));

		// dssecurity
		elbeAgreeExtItem.setDSecurity(CollateralUtilities.fillWithBlanks(getDSSecurity(entries, marginCallEmpty), 100));

		// sign
		elbeAgreeExtItem.setSign(getSign(balanceCCYTrue, balanceCCYFalse));

		// balance cash ccy

		elbeAgreeExtItem.setBalanceCashCCY(CollateralUtilities.fillWithZeros(
				CollateralUtilities.convertToReportDecimalFormat(
						CollateralUtilities.formatNumber(getBalanceCashCCY(false, entries, marginCallEmpty)), ",", "."),
				18));

		// balance cash eur
		elbeAgreeExtItem
				.setBalanceCashEUR(
						CollateralUtilities.fillWithZeros(CollateralUtilities.convertToReportDecimalFormat(
								CollateralUtilities.formatNumber(getBalanceCashEUR(
										ELBEAgreementsExtractionLogic.fxRateCcyEur, false, entries, marginCallEmpty)),
								",", "."), 18));

		// balance stock ccy
		elbeAgreeExtItem
				.setBalanceStockCCY(
						CollateralUtilities.fillWithZeros(
								CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
										.formatNumber(getBalanceStockCCY(false, entries, marginCallEmpty)), ",", "."),
								18));

		// balance stock eur
		elbeAgreeExtItem
				.setBalanceStockEUR(
						CollateralUtilities.fillWithZeros(CollateralUtilities.convertToReportDecimalFormat(
								CollateralUtilities.formatNumber(getBalanceStockEUR(
										ELBEAgreementsExtractionLogic.fxRateCcyEur, false, entries, marginCallEmpty)),
								",", "."), 18));

		// status
		elbeAgreeExtItem.setStatus(CollateralUtilities.fillWithBlanks(getStatus(entries, marginCallEmpty), 1));

		// event
		elbeAgreeExtItem.setEvent(CollateralUtilities.fillWithBlanks(MARGIN_CALL, 20));

		// gross exposure ccy
		double grossExp = getGrossExposureCCY(entries, marginCallEmpty);
		elbeAgreeExtItem.setGrossExposure(grossExp);
		elbeAgreeExtItem.setGrossExposureCCY(CollateralUtilities.fillWithZeros(
				CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities.formatNumber(grossExp), ",", "."),
				18));

		// gross exposure eur
		elbeAgreeExtItem.setGrossExposureEUR(CollateralUtilities.fillWithZeros(
				CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
						.formatNumber(getGrossExposureEUR(marginCall, pricingEnv, entries, marginCallEmpty)), ",", "."),
				18));

		// margin call ccy

		/*
		elbeAgreeExtItem
				.setMarginCallCCY(CollateralUtilities.fillWithZeros(
						CollateralUtilities.convertToReportDecimalFormat(
								CollateralUtilities.formatNumber(getMarginCallCCY(entries, marginCallEmpty)), ",", "."),
						18));

		 */

		elbeAgreeExtItem
				.setMarginCallCCY(CollateralUtilities.fillWithZeros(
						CollateralUtilities.convertToReportDecimalFormat(
								CollateralUtilities.formatNumber(getMarginCallCCY3(entries, marginCallEmpty, marginCall)), ",", "."),
						18));


		elbeAgreeExtItem.setMarginCallConfig(marginCall);

		return elbeAgreeExtItem;
	}

	/**
	 * Method changeTodefineRow that only for DEFINE rows
	 * 
	 * @param mcRow
	 * @param marginCall
	 * @param dsConn
	 * @param entries
	 * @return
	 */
	private static ELBEAgreementsExtractionItem changeToDefineRow(ELBEAgreementsExtractionItem mcRow,
			final List<MarginCallEntryDTO> entries, final boolean marginCallEmpty, final double balanceCCYTrue,
			final double balanceCCYFalse) {

		ELBEAgreementsExtractionItem defineRow = new ELBEAgreementsExtractionItem();
		try {
			defineRow = (ELBEAgreementsExtractionItem) BeanUtils.cloneBean(mcRow);
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException
				| NoSuchMethodException excp) {

			Log.info(excp.getClass(), excp.getMessage());
			return null;

		}

		// change caidas
		setNonRatingDependingThresholdStuff(defineRow);
		setNonRatingDependingIAstuff(defineRow);

		// change sign
		defineRow.setSign(" ");

		// change status
		defineRow.setStatus(CollateralUtilities.fillWithBlanks("V", 1));

		// change event
		defineRow.setEvent(CollateralUtilities.fillWithBlanks(DEFINE, 20));

		// change margin call ccy
		defineRow.setMarginCallCCY(CollateralUtilities.fillWithZeros(CollateralUtilities.convertToReportDecimalFormat(
				CollateralUtilities.formatNumber(getMarginCallCCY2(entries, marginCallEmpty)), ",", "."), 18));

		// change balance stock ccy
		defineRow.setBalanceStockCCY(CollateralUtilities.fillWithZeros(
				CollateralUtilities.convertToReportDecimalFormat(
						CollateralUtilities.formatNumber(getBalanceStockCCY(true, entries, marginCallEmpty)), ",", "."),
				18));

		// change balance cash ccy
		defineRow.setBalanceCashCCY(CollateralUtilities.fillWithZeros(
				CollateralUtilities.convertToReportDecimalFormat(
						CollateralUtilities.formatNumber(getBalanceCashCCY(true, entries, marginCallEmpty)), ",", "."),
				18));

		// change balance ccy

		defineRow.setBalanceCCY(CollateralUtilities.fillWithZeros(CollateralUtilities
				.convertToReportDecimalFormat(CollateralUtilities.formatNumber(balanceCCYTrue), ",", "."), 18));

		// change balance eur
		defineRow
				.setBalanceEUR(CollateralUtilities.fillWithZeros(
						CollateralUtilities.convertToReportDecimalFormat(CollateralUtilities
								.formatNumber(balanceCCYTrue * ELBEAgreementsExtractionLogic.fxRateCcyEur), ",", "."),
						18));

		// balance cash eur
		defineRow.setBalanceCashEUR(CollateralUtilities.fillWithZeros(CollateralUtilities.convertToReportDecimalFormat(
				CollateralUtilities.formatNumber(
						getBalanceCashEUR(ELBEAgreementsExtractionLogic.fxRateCcyEur, true, entries, marginCallEmpty)),
				",", "."), 18));

		// balance stock eur
		defineRow.setBalanceStockEUR(CollateralUtilities.fillWithZeros(CollateralUtilities.convertToReportDecimalFormat(
				CollateralUtilities.formatNumber(
						getBalanceStockEUR(ELBEAgreementsExtractionLogic.fxRateCcyEur, true, entries, marginCallEmpty)),
				",", "."), 18));

		return defineRow;

	}

	/**
	 * Retrieve date converted in ddMMyyyy format.
	 *
	 * @param date String with the date, format String with the previous format
	 * @return String with the date in ddMMyyyy format.
	 */
	public static String changeFormatDate(final String date, final String prevFormat) {

		final SimpleDateFormat sdf = new SimpleDateFormat(prevFormat);
		Date d = null;
		try {
			d = sdf.parse(date);
		} catch (final ParseException e) {
			Log.error("Cannot parse date", e);
		}
		final SimpleDateFormat sdf2 = new SimpleDateFormat("ddMMyyyy");
		return sdf2.format(d);

	}

	/**
	 * Method used to formated number
	 * 
	 * @param number
	 * @return
	 */
	public static String formatFixing(final double number) {
		final NumberFormat numberFormatter = new DecimalFormat("#000.000000");
		return numberFormatter.format(number);
	}

	/**
	 * Method used to calculate MTANotches
	 * 
	 * @param item
	 * @param marginCall
	 * @param mtaRequiredRating
	 * @param agencies
	 */
	private void calculateMTANotches(ELBEAgreementsExtractionItem item, CollateralConfig marginCall,
			CreditRating mtaRequiredRating) {
		CreditRating creditRating = null;

		if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.GLOBAL_RATING)) {
			creditRating = mtaRequiredRating;

			if (creditRating == null) {
				return;
			}
			// notch 1
			CreditRating crNotch1 = getCreditRatingNotch(creditRating, globalRatingConfig, 1);
			if (crNotch1 != null) {
				// MTA active down 1 notch ccy
				item.setMTADown1notch(Math.abs(getMTA(marginCall, crNotch1)));
			}
			// notch 2
			CreditRating crNotch2 = getCreditRatingNotch(creditRating, globalRatingConfig, 2);
			if (crNotch2 != null) {
				// MTA active down 2 notch ccy
				item.setMTADown2notch(Math.abs(getMTA(marginCall, crNotch2)));
			}
			// notch 3
			CreditRating crNotch3 = getCreditRatingNotch(creditRating, globalRatingConfig, 3);
			if (crNotch3 != null) {
				// MTA active down 3 notch ccy
				item.setMTADown3notch(Math.abs(getMTA(marginCall, crNotch3)));
			}
		}
	}

	/**
	 * Method used to get MTA
	 * 
	 * @param marginCall
	 * @param creditRating
	 * @return
	 */
	public double getMTA(final CollateralConfig marginCall, CreditRating creditRating) {
		// GLOBAL RATING
		if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.GLOBAL_RATING)) {

			MarginCallCreditRating mccCreditRating = null;
			try {
				mccCreditRating = CollateralUtilities.getLatestMCCreditRating(marginCall.getPoRatingsConfigId(),
						creditRating.getRatingValue(), creditRating.getAgencyName(), valueDate);
			} catch (Exception e) {
				Log.error("Cannot get MarginCallCreditRating for contract=" + marginCall.getId() + ", agency="
						+ creditRating.getAgencyName() + ", rating=" + creditRating.getRatingValue(), e);
			}
			if (mccCreditRating != null) {
				return ELBEandKGRutilities.getMtaDependingOnRating(marginCall, mccCreditRating,
						marginCall.getPoRatingsConfigId(), processDate, valueDate);
			}

		}
		// AMOUNT
		if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.AMOUNT)) {
			return ELBEandKGRutilities.getMtaDependingOnAmount(marginCall, "PO", valueDate);
		}
		// MC_PERCENT
		// MIG_V14

		// PERCENT
		if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.PERCENT)) {
			return ELBEandKGRutilities.getMtaDependingOnPercent(marginCall, "PO", processDate);
		}
		// BOTH
		if (ELBEandKGRutilities.isMTADependingOn(marginCall, "BOTH")) {
			return ELBEandKGRutilities.getThresholdDependingOnBoth(marginCall, "PO", processDate, valueDate);
		}
		return 0.00;
	}

}
