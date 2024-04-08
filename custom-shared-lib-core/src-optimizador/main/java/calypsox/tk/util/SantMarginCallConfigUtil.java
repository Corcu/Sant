package calypsox.tk.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.CVA_ThresholdOptParamItem;
import calypsox.tk.report.riskvalues.RiskValuesItem;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.ELBEandKGRutilities;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.GlobalRating;
import com.calypso.tk.refdata.GlobalRatingConfiguration;
import com.calypso.tk.refdata.GlobalRatingValue;
import com.calypso.tk.service.DSConnection;

public class SantMarginCallConfigUtil {
	
	private static final String PERCENT = "PERCENT";

	
	public static String getMTARatingMatrixPO(final CollateralConfig marginCall, Vector<CreditRating> creditRatings,
			final PricingEnv pricingEnv, JDate valueDate) throws Exception {

		MarginCallCreditRatingConfiguration mCRatingsAll = loadMCRatingsPO(marginCall, valueDate);

		GlobalRatingConfiguration globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
				.loadDefaultGlobalRatingConfiguration();

		StringBuilder mtaRatings = new StringBuilder();
		String peName = "";
		if (pricingEnv != null) {
			peName = pricingEnv.getName();
		}
		AppUtil.loadPE(peName, new JDatetime(valueDate, TimeZone.getDefault()));

		List<RiskValuesItem> allItems = new ArrayList<RiskValuesItem>();
		
		

		if ((mCRatingsAll != null) && !Util.isEmpty(mCRatingsAll.getRatings())
				&& (marginCall.getEligibleAgencies() != null)) {
			// get margin call rating lines ratings
			List<MarginCallCreditRating> mcRatings = mCRatingsAll.getRatings();

			// get priority of rating matrix line according to lower/higher rating, depending on threshold rating
			// direction
			int ThresholdRequiredRatingPriority = getRequiredRatingPriority(marginCall, mcRatings,
					marginCall.getProcessingOrg(), valueDate, marginCall.getPoThresholdRatingDirection());

			// process margin call rating lines
			for (MarginCallCreditRating mcRating : mcRatings) {

				RiskValuesItem item = buildItem(marginCall);

				// set active threshold
				if ((ThresholdRequiredRatingPriority != -1)
						&& (mcRating.getPriority() == ThresholdRequiredRatingPriority)) {
					item.setActive(true);
				}

				// set fitch's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.FITCH)) {
					String fitchRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.FITCH, mcRating.getPriority());
					if (!Util.isEmpty(fitchRatingValue)) {
						item.setFitchMCrating(fitchRatingValue);
					}
				}

				// set SP's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.SNP)) {
					String snpRatingValue = getGlobalRatingValue(globalRatingConfig, CollateralStaticAttributes.SNP,
							mcRating.getPriority());
					if (!Util.isEmpty(snpRatingValue)) {
						item.setSnpMCrating(snpRatingValue);
					}
				}

				// set Moody's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.MOODY)) {
					String moodyRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.MOODY, mcRating.getPriority());
					if (!Util.isEmpty(moodyRatingValue)) {
						item.setMoodyMCrating(moodyRatingValue);
					}
				}

				item.setMCRatingCcy(marginCall.getCurrency());

				allItems.add(item);
			}

			String ratingValue;

			if (allItems.get(0).getMoodyMCrating() != null) {
				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {						
						ratingValue = mCRatingsAll.getRatings().get(j).getMta();
						mtaRatings.append(allItems.get(j).getMoodyMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} else if (allItems.get(0).getSnpMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getMta();
						mtaRatings.append(allItems.get(j).getSnpMCrating() + " - " + ratingValue + " ; ");
					}
				}				

			} else if (allItems.get(0).getFitchMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getMta();
						mtaRatings.append(allItems.get(j).getFitchMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} 
		}
		return mtaRatings.toString();
	}	
	
	public static String getIARatingMatrixPO(final CollateralConfig marginCall, Vector<CreditRating> creditRatings,
			final PricingEnv pricingEnv, JDate valueDate) throws Exception {

		MarginCallCreditRatingConfiguration mCRatingsAll = loadMCRatingsPO(marginCall, valueDate);

		GlobalRatingConfiguration globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
				.loadDefaultGlobalRatingConfiguration();

		StringBuilder iaRatings = new StringBuilder();
		String peName = "";
		if (pricingEnv != null) {
			peName = pricingEnv.getName();
		}
		AppUtil.loadPE(peName, new JDatetime(valueDate, TimeZone.getDefault()));

		List<RiskValuesItem> allItems = new ArrayList<RiskValuesItem>();
		
		

		if ((mCRatingsAll != null) && !Util.isEmpty(mCRatingsAll.getRatings())
				&& (marginCall.getEligibleAgencies() != null)) {
			// get margin call rating lines ratings
			List<MarginCallCreditRating> mcRatings = mCRatingsAll.getRatings();

			// get priority of rating matrix line according to lower/higher rating, depending on threshold rating
			// direction
			int ThresholdRequiredRatingPriority = getRequiredRatingPriority(marginCall, mcRatings,
					marginCall.getProcessingOrg(), valueDate, marginCall.getPoThresholdRatingDirection());

			// process margin call rating lines
			for (MarginCallCreditRating mcRating : mcRatings) {

				RiskValuesItem item = buildItem(marginCall);

				// set active threshold
				if ((ThresholdRequiredRatingPriority != -1)
						&& (mcRating.getPriority() == ThresholdRequiredRatingPriority)) {
					item.setActive(true);
				}

				// set fitch's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.FITCH)) {
					String fitchRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.FITCH, mcRating.getPriority());
					if (!Util.isEmpty(fitchRatingValue)) {
						item.setFitchMCrating(fitchRatingValue);
					}
				}

				// set SP's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.SNP)) {
					String snpRatingValue = getGlobalRatingValue(globalRatingConfig, CollateralStaticAttributes.SNP,
							mcRating.getPriority());
					if (!Util.isEmpty(snpRatingValue)) {
						item.setSnpMCrating(snpRatingValue);
					}
				}

				// set Moody's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.MOODY)) {
					String moodyRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.MOODY, mcRating.getPriority());
					if (!Util.isEmpty(moodyRatingValue)) {
						item.setMoodyMCrating(moodyRatingValue);
					}
				}

				item.setMCRatingCcy(marginCall.getCurrency());

				allItems.add(item);
			}

			String ratingValue;

			if (allItems.get(0).getMoodyMCrating() != null) {
				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {						
						ratingValue = mCRatingsAll.getRatings().get(j).getIndependentAmount();
						iaRatings.append(allItems.get(j).getMoodyMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} else if (allItems.get(0).getSnpMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getIndependentAmount();
						iaRatings.append(allItems.get(j).getSnpMCrating() + " - " + ratingValue + " ; ");
					}
				}				

			} else if (allItems.get(0).getFitchMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getIndependentAmount();
						iaRatings.append(allItems.get(j).getFitchMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} 
		}
		return iaRatings.toString();
	}
	
	public static String getRatingMatrixPO(final CollateralConfig marginCall, Vector<CreditRating> creditRatings,
			final PricingEnv pricingEnv, JDate valueDate) throws Exception {

		MarginCallCreditRatingConfiguration mCRatingsAll = loadMCRatingsPO(marginCall, valueDate);

		GlobalRatingConfiguration globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
				.loadDefaultGlobalRatingConfiguration();

		StringBuilder ratings = new StringBuilder();
		String peName = "";
		if (pricingEnv != null) {
			peName = pricingEnv.getName();
		}
		AppUtil.loadPE(peName, new JDatetime(valueDate, TimeZone.getDefault()));

		List<RiskValuesItem> allItems = new ArrayList<RiskValuesItem>();
		
		

		if ((mCRatingsAll != null) && !Util.isEmpty(mCRatingsAll.getRatings())
				&& (marginCall.getEligibleAgencies() != null)) {
			// get margin call rating lines ratings
			List<MarginCallCreditRating> mcRatings = mCRatingsAll.getRatings();

			// get priority of rating matrix line according to lower/higher rating, depending on threshold rating
			// direction
			int ThresholdRequiredRatingPriority = getRequiredRatingPriority(marginCall, mcRatings,
					marginCall.getProcessingOrg(), valueDate, marginCall.getPoThresholdRatingDirection());

			// process margin call rating lines
			for (MarginCallCreditRating mcRating : mcRatings) {

				RiskValuesItem item = buildItem(marginCall);

				// set active threshold
				if ((ThresholdRequiredRatingPriority != -1)
						&& (mcRating.getPriority() == ThresholdRequiredRatingPriority)) {
					item.setActive(true);
				}

				// set fitch's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.FITCH)) {
					String fitchRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.FITCH, mcRating.getPriority());
					if (!Util.isEmpty(fitchRatingValue)) {
						item.setFitchMCrating(fitchRatingValue);
					}
				}

				// set SP's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.SNP)) {
					String snpRatingValue = getGlobalRatingValue(globalRatingConfig, CollateralStaticAttributes.SNP,
							mcRating.getPriority());
					if (!Util.isEmpty(snpRatingValue)) {
						item.setSnpMCrating(snpRatingValue);
					}
				}

				// set Moody's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.MOODY)) {
					String moodyRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.MOODY, mcRating.getPriority());
					if (!Util.isEmpty(moodyRatingValue)) {
						item.setMoodyMCrating(moodyRatingValue);
					}
				}

				item.setMCRatingCcy(marginCall.getCurrency());

				allItems.add(item);
			}

			String ratingValue;

			if (allItems.get(0).getMoodyMCrating() != null) {
				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {						
						ratingValue = mCRatingsAll.getRatings().get(j).getThreshold();
						ratings.append(allItems.get(j).getMoodyMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} else if (allItems.get(0).getSnpMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getThreshold();
						ratings.append(allItems.get(j).getSnpMCrating() + " - " + ratingValue + " ; ");
					}
				}				

			} else if (allItems.get(0).getFitchMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getThreshold();
						ratings.append(allItems.get(j).getFitchMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} 
		}
		return ratings.toString();
	}	
	
	public static String getRatingMatrixCpty(final CollateralConfig marginCall, Vector<CreditRating> creditRatings,
			final PricingEnv pricingEnv, JDate valueDate) throws Exception {

		MarginCallCreditRatingConfiguration mCRatingsAll = loadMCRatingsCpty(marginCall, valueDate);

		GlobalRatingConfiguration globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
				.loadDefaultGlobalRatingConfiguration();

		StringBuilder ratings = new StringBuilder();
		String peName = "";
		if (pricingEnv != null) {
			peName = pricingEnv.getName();
		}
		AppUtil.loadPE(peName, new JDatetime(valueDate, TimeZone.getDefault()));

		List<RiskValuesItem> allItems = new ArrayList<RiskValuesItem>();
		
		

		if ((mCRatingsAll != null) && !Util.isEmpty(mCRatingsAll.getRatings())
				&& (marginCall.getEligibleAgencies() != null)) {
			// get margin call rating lines ratings
			List<MarginCallCreditRating> mcRatings = mCRatingsAll.getRatings();

			// get priority of rating matrix line according to lower/higher rating, depending on threshold rating
			// direction
			int ThresholdRequiredRatingPriority = getRequiredRatingPriority(marginCall, mcRatings,
					marginCall.getLegalEntity(), valueDate, marginCall.getLeThresholdRatingDirection());

			// process margin call rating lines
			for (MarginCallCreditRating mcRating : mcRatings) {

				RiskValuesItem item = buildItem(marginCall);

				// set active threshold
				if ((ThresholdRequiredRatingPriority != -1)
						&& (mcRating.getPriority() == ThresholdRequiredRatingPriority)) {
					item.setActive(true);
				}

				// set fitch's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.FITCH)) {
					String fitchRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.FITCH, mcRating.getPriority());
					if (!Util.isEmpty(fitchRatingValue)) {
						item.setFitchMCrating(fitchRatingValue);
					}
				}

				// set SP's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.SNP)) {
					String snpRatingValue = getGlobalRatingValue(globalRatingConfig, CollateralStaticAttributes.SNP,
							mcRating.getPriority());
					if (!Util.isEmpty(snpRatingValue)) {
						item.setSnpMCrating(snpRatingValue);
					}
				}

				// set Moody's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.MOODY)) {
					String moodyRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.MOODY, mcRating.getPriority());
					if (!Util.isEmpty(moodyRatingValue)) {
						item.setMoodyMCrating(moodyRatingValue);
					}
				}

				item.setMCRatingCcy(marginCall.getCurrency());

				allItems.add(item);
			}

			String ratingValue;

			if (allItems.get(0).getMoodyMCrating() != null) {
				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {						
						ratingValue = mCRatingsAll.getRatings().get(j).getThreshold();
						ratings.append(allItems.get(j).getMoodyMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} else if (allItems.get(0).getSnpMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getThreshold();
						ratings.append(allItems.get(j).getSnpMCrating() + " - " + ratingValue + " ; ");
					}
				}				

			} else if (allItems.get(0).getFitchMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getThreshold();
						ratings.append(allItems.get(j).getFitchMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} 
		}
		return ratings.toString();
	}	
	
	public static String getIARatingMatrixCpty(final CollateralConfig marginCall, Vector<CreditRating> creditRatings,
			final PricingEnv pricingEnv, JDate valueDate) throws Exception {

		MarginCallCreditRatingConfiguration mCRatingsAll = loadMCRatingsCpty(marginCall, valueDate);

		GlobalRatingConfiguration globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
				.loadDefaultGlobalRatingConfiguration();

		StringBuilder iaRatings = new StringBuilder();
		String peName = "";
		if (pricingEnv != null) {
			peName = pricingEnv.getName();
		}
		AppUtil.loadPE(peName, new JDatetime(valueDate, TimeZone.getDefault()));

		List<RiskValuesItem> allItems = new ArrayList<RiskValuesItem>();
		
		

		if ((mCRatingsAll != null) && !Util.isEmpty(mCRatingsAll.getRatings())
				&& (marginCall.getEligibleAgencies() != null)) {
			// get margin call rating lines ratings
			List<MarginCallCreditRating> mcRatings = mCRatingsAll.getRatings();

			// get priority of rating matrix line according to lower/higher rating, depending on threshold rating
			// direction
			int ThresholdRequiredRatingPriority = getRequiredRatingPriority(marginCall, mcRatings,
					marginCall.getLegalEntity(), valueDate, marginCall.getLeThresholdRatingDirection());

			// process margin call rating lines
			for (MarginCallCreditRating mcRating : mcRatings) {

				RiskValuesItem item = buildItem(marginCall);

				// set active threshold
				if ((ThresholdRequiredRatingPriority != -1)
						&& (mcRating.getPriority() == ThresholdRequiredRatingPriority)) {
					item.setActive(true);
				}

				// set fitch's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.FITCH)) {
					String fitchRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.FITCH, mcRating.getPriority());
					if (!Util.isEmpty(fitchRatingValue)) {
						item.setFitchMCrating(fitchRatingValue);
					}
				}

				// set SP's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.SNP)) {
					String snpRatingValue = getGlobalRatingValue(globalRatingConfig, CollateralStaticAttributes.SNP,
							mcRating.getPriority());
					if (!Util.isEmpty(snpRatingValue)) {
						item.setSnpMCrating(snpRatingValue);
					}
				}

				// set Moody's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.MOODY)) {
					String moodyRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.MOODY, mcRating.getPriority());
					if (!Util.isEmpty(moodyRatingValue)) {
						item.setMoodyMCrating(moodyRatingValue);
					}
				}

				item.setMCRatingCcy(marginCall.getCurrency());

				allItems.add(item);
			}

			String ratingValue;

			if (allItems.get(0).getMoodyMCrating() != null) {
				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {						
						ratingValue = mCRatingsAll.getRatings().get(j).getIndependentAmount();
						iaRatings.append(allItems.get(j).getMoodyMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} else if (allItems.get(0).getSnpMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getIndependentAmount();
						iaRatings.append(allItems.get(j).getSnpMCrating() + " - " + ratingValue + " ; ");
					}
				}				

			} else if (allItems.get(0).getFitchMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getIndependentAmount();
						iaRatings.append(allItems.get(j).getFitchMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} 
		}
		return iaRatings.toString();
	}	
	
	public static String getMTARatingMatrixCpty(final CollateralConfig marginCall, Vector<CreditRating> creditRatings,
			final PricingEnv pricingEnv, JDate valueDate) throws Exception {

		MarginCallCreditRatingConfiguration mCRatingsAll = loadMCRatingsCpty(marginCall, valueDate);

		GlobalRatingConfiguration globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
				.loadDefaultGlobalRatingConfiguration();

		StringBuilder mtaRatings = new StringBuilder();
		String peName = "";
		if (pricingEnv != null) {
			peName = pricingEnv.getName();
		}
		AppUtil.loadPE(peName, new JDatetime(valueDate, TimeZone.getDefault()));

		List<RiskValuesItem> allItems = new ArrayList<RiskValuesItem>();
		
		

		if ((mCRatingsAll != null) && !Util.isEmpty(mCRatingsAll.getRatings())
				&& (marginCall.getEligibleAgencies() != null)) {
			// get margin call rating lines ratings
			List<MarginCallCreditRating> mcRatings = mCRatingsAll.getRatings();

			// get priority of rating matrix line according to lower/higher rating, depending on threshold rating
			// direction
			int ThresholdRequiredRatingPriority = getRequiredRatingPriority(marginCall, mcRatings,
					marginCall.getLegalEntity(), valueDate, marginCall.getLeThresholdRatingDirection());

			// process margin call rating lines
			for (MarginCallCreditRating mcRating : mcRatings) {

				RiskValuesItem item = buildItem(marginCall);

				// set active threshold
				if ((ThresholdRequiredRatingPriority != -1)
						&& (mcRating.getPriority() == ThresholdRequiredRatingPriority)) {
					item.setActive(true);
				}

				// set fitch's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.FITCH)) {
					String fitchRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.FITCH, mcRating.getPriority());
					if (!Util.isEmpty(fitchRatingValue)) {
						item.setFitchMCrating(fitchRatingValue);
					}
				}

				// set SP's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.SNP)) {
					String snpRatingValue = getGlobalRatingValue(globalRatingConfig, CollateralStaticAttributes.SNP,
							mcRating.getPriority());
					if (!Util.isEmpty(snpRatingValue)) {
						item.setSnpMCrating(snpRatingValue);
					}
				}

				// set Moody's active rating
				if (marginCall.getEligibleAgencies().contains(CollateralStaticAttributes.MOODY)) {
					String moodyRatingValue = getGlobalRatingValue(globalRatingConfig,
							CollateralStaticAttributes.MOODY, mcRating.getPriority());
					if (!Util.isEmpty(moodyRatingValue)) {
						item.setMoodyMCrating(moodyRatingValue);
					}
				}

				item.setMCRatingCcy(marginCall.getCurrency());

				allItems.add(item);
			}

			String ratingValue;

			if (allItems.get(0).getMoodyMCrating() != null) {
				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {						
						ratingValue = mCRatingsAll.getRatings().get(j).getMta();
						mtaRatings.append(allItems.get(j).getMoodyMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} else if (allItems.get(0).getSnpMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getMta();
						mtaRatings.append(allItems.get(j).getSnpMCrating() + " - " + ratingValue + " ; ");
					}
				}				

			} else if (allItems.get(0).getFitchMCrating() != null) {

				for (int j = 0; j < allItems.size(); j++) {
					if (allItems.get(j) != null) {
						ratingValue = mCRatingsAll.getRatings().get(j).getMta();
						mtaRatings.append(allItems.get(j).getFitchMCrating() + " - " + ratingValue + " ; ");
					}
				}
				

			} 
		}
		return mtaRatings.toString();
	}	
	
	private static MarginCallCreditRatingConfiguration loadMCRatingsCpty(CollateralConfig contract, JDate processDate)
			throws RemoteException {

		if (contract.getLeRatingsConfigId() != 0) {
			MarginCallCreditRatingConfiguration mcRatingConfig = getLatestMarginCallRatings(
					contract.getLeRatingsConfigId(), processDate);
			if (mcRatingConfig != null) {
				return mcRatingConfig;
			}
		}
		return null;
	}
	
	private static MarginCallCreditRatingConfiguration loadMCRatingsPO(CollateralConfig contract, JDate processDate)
			throws RemoteException {

		if (contract.getPoRatingsConfigId() != 0) {
			MarginCallCreditRatingConfiguration mcRatingConfig = getLatestMarginCallRatings(
					contract.getPoRatingsConfigId(), processDate);
			if (mcRatingConfig != null) {
				return mcRatingConfig;
			}
		}
		return null;
	}
	
	// get priority of rating matrix line corresponding to lower/higher rating value (according to rating direction
		// received)
		@SuppressWarnings("unchecked")
		public static int getRequiredRatingPriority(CollateralConfig contract, List<MarginCallCreditRating> mcRatings,
				LegalEntity le, JDate valDate, String direction) throws Exception {

			if (Util.isEmpty(direction)) {
				return -1;
			}

			final int leID = le.getId();
			final DSConnection dsConn = DSConnection.getDefault();
			int currentPriority = -1;

			// get last rating values for LE
			Vector<CreditRating> leRatings = dsConn
					.getRemoteMarketData()
					.getRatings(
							null,
							"credit_rating.legal_entity_id = "
									+ leID
									+ " and credit_rating.rating_type = 'Current' "
									+ "AND as_of_date in (select max(cr2.as_of_date) from credit_rating cr2  where cr2.rating_agency_name=credit_rating.rating_agency_name "
									+ "and cr2.legal_entity_id=credit_rating.legal_entity_id and cr2.rating_type = credit_rating.rating_type "
									+ " AND as_of_date<=" + Util.date2SQLString(valDate)
									+ " ) order by trunc(updated_datetime) desc ");

			if (Util.isEmpty(leRatings)) {
				return -1;
			}

			// get only elegible agencies rating values
			leRatings = CollateralUtilities.getEligibleAgenciesOnly(leRatings, contract.getEligibleAgencies());

			GlobalRatingConfiguration globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
					.loadDefaultGlobalRatingConfiguration();

			// get lower/higher
			for (CreditRating leRating : leRatings) {

				List<GlobalRating> leGlobalRatings = globalRatingConfig.getGlobalRating(CreditRating.CURRENT,
						leRating.getAgencyName(), CreditRating.ANY);

				int leRatingPriority = ELBEandKGRutilities.getAgencyRatingPriority(leGlobalRatings.get(0),
						leRating.getRatingValue());

				// initial case
				if (currentPriority == -1) {
					currentPriority = leRatingPriority;
					// rest of iterations
				} else {
					// same priority
					if (currentPriority == leRatingPriority) {
						continue;
					} else {
						// different priority
						if ("HIGHER".equals(direction)) {
							if (leRatingPriority < currentPriority) {
								currentPriority = leRatingPriority;
							}
						}
						if ("LOWER".equals(direction)) {
							if (leRatingPriority > currentPriority) {
								currentPriority = leRatingPriority;
							}
						}
					}
				}
			}

			return currentPriority;
		}

		public void setThresholdCpty(CVA_ThresholdOptParamItem marginCallItem, CollateralConfig marginCall,
				double thresholdCpty) {
			String value = CollateralUtilities.formatNumber(Math.abs(thresholdCpty));
			if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, PERCENT)) {
				if (value.contains(",")) {
					marginCallItem.setThresholdValueCpty(value.replace(',', '.'));
				}
			} else {
				if (value.contains(".")) {
					marginCallItem.setThresholdValueCpty(value.substring(0, value.indexOf('.')));
				}
				if (value.contains(",")) {
					marginCallItem.setThresholdValueCpty(value.substring(0, value.indexOf(',')));
				}
			}
		}
		
		private static  RiskValuesItem buildItem(CollateralConfig contract) {
			RiskValuesItem item = new RiskValuesItem();
			item.setContractId(contract.getId());
			item.setContractName(contract.getName());
			item.setLEFullName(contract.getLegalEntity().getName());
			item.setDeliveryRoundingPO(contract.getPoRoundingFigure());
			item.setReturnRoundingLE(contract.getLeReturnRoundingFigure());

			return item;
		}
		
		public static String getGlobalRatingValue(GlobalRatingConfiguration globalRatingConfig, String ratingAgency, int priority)
				throws Exception {
			if (!Util.isEmpty(ratingAgency)) {

				List<GlobalRating> globalRatings = globalRatingConfig.getGlobalRating(CreditRating.CURRENT, ratingAgency,
						CreditRating.ANY);
				if (!Util.isEmpty(globalRatings)) {
					GlobalRating rating = globalRatings.get(0);
					GlobalRatingValue globalRatingValue = rating.getGlobalRatingValue(priority);

					if (globalRatingValue != null) {
						return globalRatingValue.getValue();
					}
				}
			}
			return "";
		}
		
		// Get rating matrix matching with id and date received (get all margin call rating lines according to Moody scale)
		public static MarginCallCreditRatingConfiguration getLatestMarginCallRatings(int ratingConfigId, JDate date)
				throws RemoteException {

			MarginCallCreditRatingConfiguration marginCallCreditRatingConfig = ServiceRegistry.getDefault()
					.getCollateralServer().getMarginCallCreditRatingById(ratingConfigId);

			if (marginCallCreditRatingConfig == null) {
				return null;
			}

			List<MarginCallCreditRating> finalRatings = new ArrayList<MarginCallCreditRating>();

			GlobalRatingConfiguration globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
					.loadDefaultGlobalRatingConfiguration();

			// get Moodys scale
			List<GlobalRating> spGlobalRatings = globalRatingConfig.getGlobalRating(CreditRating.CURRENT,
					CollateralStaticAttributes.MOODY, CreditRating.ANY);
			List<GlobalRatingValue> ratingValues = null;
			if (!Util.isEmpty(spGlobalRatings)) {
				GlobalRating spRating = spGlobalRatings.get(0);
				if (spRating != null) {
					ratingValues = spRating.getRatingValues();
				}
			}

			// Get margin call rating line for each scale value
			if (ratingValues != null) {
				for (GlobalRatingValue ratingValue : ratingValues) {
					if (!Util.isEmpty(ratingValue.getValue())) {
						MarginCallCreditRating mcCreditrating = new MarginCallCreditRating();
						mcCreditrating.setMarginCallCreditRatingId(ratingConfigId);
						mcCreditrating.setPriority(ratingValue.getPriority());
						MarginCallCreditRating mcRatingLine = ServiceRegistry.getDefault().getCollateralServer()
								.getLatestMarginCallCreditRating(mcCreditrating, date);
						if (mcRatingLine != null) {
							finalRatings.add(mcRatingLine);
						} else {
							finalRatings.add(mcCreditrating);
						}
					}
				}
			}

			marginCallCreditRatingConfig.setRatings(new Vector<MarginCallCreditRating>(finalRatings));
			return marginCallCreditRatingConfig;
		}
	
}
