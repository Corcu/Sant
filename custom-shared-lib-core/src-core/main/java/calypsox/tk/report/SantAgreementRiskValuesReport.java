package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.riskvalues.RiskValuesItem;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.ELBEandKGRutilities;
import calypsox.util.collateral.CollateralManagerUtil;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.GlobalRating;
import com.calypso.tk.refdata.GlobalRatingConfiguration;
import com.calypso.tk.refdata.GlobalRatingValue;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class SantAgreementRiskValuesReport extends SantReport {

	private static final long serialVersionUID = 790458666598235108L;

	public static String SANT_RISK_VALUES_ITEM = "SantRiskValuesItem";

	@Override
	protected ReportOutput loadReport(Vector<String> errorMsgs) {
		try {
			return getReportOutput(errorMsgs);
		} catch (final Exception e) {
			String msg = "Cannot load AgreementRiskValues ";
			Log.error(this, msg, e);
			errorMsgs.add(msg + e.getMessage());
		}

		return null;
	}

	private JDate valDate = null;

	protected JDate getValDateDate() {
		return this.valDate;
	}

	public ReportOutput getReportOutput(Vector<String> errorMsgs) throws Exception {
		final DefaultReportOutput output = new DefaultReportOutput(this);

		String peName = getPricingEnv().getName();
		this.valDate = CollateralUtilities.getMCValDate(getProcessStartDate());
		PricingEnv pricingEnv = AppUtil.loadPE(peName, new JDatetime(this.valDate, TimeZone.getDefault()));

		final List<ReportRow> reportRows = new ArrayList<ReportRow>();

		// Agreement
		final String agreementIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
		// Owner
		// 27/07/15. SBNA Multi-PO filter
		// (String)
		// getReportTemplate().get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
		final String ownerIds = CollateralUtilities.filterPoIdsByTemplate(getReportTemplate());

		// load contracts
		List<CollateralConfig> contracts = loadContracts(agreementIds, ownerIds, errorMsgs);

		// DPM 21/02/2014 - for each contract, load both rating matrix, for po
		// and cpty
		Map<Integer, MarginCallCreditRatingConfiguration> POmCRatingsAll = new HashMap<Integer, MarginCallCreditRatingConfiguration>();
		Map<Integer, MarginCallCreditRatingConfiguration> CPTYmCRatingsAll = new HashMap<Integer, MarginCallCreditRatingConfiguration>();
		loadMCRatings(contracts, POmCRatingsAll, CPTYmCRatingsAll, getValDateDate());

		GlobalRatingConfiguration globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
				.loadDefaultGlobalRatingConfiguration();

		// process contracts
		for (CollateralConfig contract : contracts) {

			// Filtered closed status // JTD 20/03/2014 - added condition
			if (!"CLOSED".equals(contract.getAgreementStatus())) {
				// 1. create RiskValue Item // DPM 21/02/2014 - added condition
				if (!"GLOBAL RATING".equals(contract.getPoMTAType())
						&& !"GLOBAL RATING".equals(contract.getPoNewThresholdType())
						&& !ELBEandKGRutilities.isIADependingOnRating(contract)) {
					continue;
				}

				if (Util.isEmpty(contract.getEligibleAgencies())) {
					continue;
				}

				// DPM 21/02/2014 - added cpty
				MarginCallCreditRatingConfiguration POmcRatingConfig = POmCRatingsAll
						.get(contract.getPoRatingsConfigId());
				MarginCallCreditRatingConfiguration CPTYmcRatingConfig = CPTYmCRatingsAll
						.get(contract.getLeRatingsConfigId());

				// no contract or no rating matrix
				if ((POmcRatingConfig == null) || Util.isEmpty(POmcRatingConfig.getRatings())) {
					RiskValuesItem item = buildItem(contract);
					item.setDeliveryMCRatingMTA(
							SantAgreementParametersReportStyle.formatNumber(contract.getPoMTAAmount()));
					item.setReturnMCRatingMTA(
							SantAgreementParametersReportStyle.formatNumber(contract.getLeMTAAmount()));
					addReportRow(reportRows, item);
				} else {
					// get margin call rating lines ratings
					List<MarginCallCreditRating> POmcRatings = POmcRatingConfig.getRatings();

					// get priority of rating matrix line according to
					// lower/higher rating, depending on mta rating
					// direction
					int MtaRequiredRatingPriority = getRequiredRatingPriority(contract, POmcRatings,
							BOCache.getLegalEntity(getDSConnection(), contract.getPoId()), getValDateDate(),
							contract.getPoMTARatingDirection());

					// get priority of rating matrix line according to
					// lower/higher rating, depending on threshold
					// rating
					// direction
					int ThresholdRequiredRatingPriority = getRequiredRatingPriority(contract, POmcRatings,
							BOCache.getLegalEntity(getDSConnection(), contract.getPoId()), getValDateDate(),
							contract.getPoThresholdRatingDirection());

					// process margin call rating lines (PO=CPTY)
					for (int i = 0; i < POmcRatings.size(); i++) {

						RiskValuesItem item = buildItem(contract);

						MarginCallCreditRating POmcRating = POmcRatings.get(i);

						// set active mta
						if ((MtaRequiredRatingPriority != -1)
								&& (POmcRating.getPriority() == MtaRequiredRatingPriority)) {
							item.setActiveMTA(true);
						}

						// set active threshold
						if ((ThresholdRequiredRatingPriority != -1)
								&& (POmcRating.getPriority() == ThresholdRequiredRatingPriority)) {
							item.setActive(true);
						}

						// set fitch's active rating
						if (contract.getEligibleAgencies().contains(CollateralStaticAttributes.FITCH)) {
							String fitchRatingValue = getGlobalRatingValue(globalRatingConfig,
									CollateralStaticAttributes.FITCH, POmcRating.getPriority());
							if (!Util.isEmpty(fitchRatingValue)) {
								item.setFitchMCrating(fitchRatingValue);
							}
						}

						// set SP's active rating
						if (contract.getEligibleAgencies().contains(CollateralStaticAttributes.SNP)) {
							String snpRatingValue = getGlobalRatingValue(globalRatingConfig,
									CollateralStaticAttributes.SNP, POmcRating.getPriority());
							if (!Util.isEmpty(snpRatingValue)) {
								item.setSnpMCrating(snpRatingValue);
							}
						}

						// set Moody's active rating
						if (contract.getEligibleAgencies().contains(CollateralStaticAttributes.MOODY)) {
							String moodyRatingValue = getGlobalRatingValue(globalRatingConfig,
									CollateralStaticAttributes.MOODY, POmcRating.getPriority());
							if (!Util.isEmpty(moodyRatingValue)) {
								item.setMoodyMCrating(moodyRatingValue);
							}
						}

						// set mta
						String mtaStr = "";
						mtaStr = SantAgreementParametersReportStyle.formatGlobalRatingValue(POmcRating.getMtaType(),
								contract.getCurrency(), POmcRatingConfig.getMtaCurrency(),
								CollateralUtilities.parseStringAmountToDouble(POmcRating.getMta()),
								POmcRating.getMtaPercent(), getValDateDate(), pricingEnv);

						item.setDeliveryMCRatingMTA(mtaStr);
						item.setReturnMCRatingMTA(mtaStr);

						// set IA
						// owner
						// AAP MIG 14.4 Calling parseStringAmountToDouble should
						// fix this
						String indAmountStr = SantAgreementParametersReportStyle.formatGlobalRatingValue(
								POmcRating.getIaType(), contract.getCurrency(), POmcRatingConfig.getIaCurrency(),
								CollateralUtilities.parseStringAmountToDouble(POmcRating.getIndependentAmount()),
								POmcRating.getIaPercent(), getValDateDate(), pricingEnv);

						item.setIndependentAmountPO(indAmountStr);
						// DPM 21/02/2014 - cpty
						if ((CPTYmcRatingConfig != null) && !Util.isEmpty(CPTYmcRatingConfig.getRatings())) {
							List<MarginCallCreditRating> CPTYmcRatings = CPTYmcRatingConfig.getRatings();
							MarginCallCreditRating CPTYmcRating = CPTYmcRatings.get(i);
							indAmountStr = SantAgreementParametersReportStyle.formatGlobalRatingValue(
									CPTYmcRating.getIaType(), contract.getCurrency(),
									CPTYmcRatingConfig.getIaCurrency(),
									CollateralUtilities.parseStringAmountToDouble(CPTYmcRating.getIndependentAmount()), CPTYmcRating.getIaPercent(),
									getValDateDate(), pricingEnv);
							item.setIndependentAmountCPTY(indAmountStr);
						}

						// set threshold
						Double threshold = SantAgreementParametersReportStyle
								.convertThreshold(POmcRating.getThreshold());
						if (threshold == null) {
							threshold = 0.0;
						}

						String thresholdAmountStr = SantAgreementParametersReportStyle.formatGlobalRatingValue(
								POmcRating.getThresholdType(), contract.getCurrency(),
								POmcRatingConfig.getThresholdCurrency(), threshold, POmcRating.getThresholdPercent(),
								getValDateDate(), pricingEnv);

						item.setMCRatingThreshold(thresholdAmountStr);

						item.setMCRatingCcy(contract.getCurrency());

						addReportRow(reportRows, item);
					}

				}
			}
		}

		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
		return output;
	}

	private RiskValuesItem buildItem(CollateralConfig contract) {
		RiskValuesItem item = new RiskValuesItem();
		item.setContractId(contract.getId());
		item.setContractName(contract.getName());
		item.setLEFullName(contract.getLegalEntity().getName());
		item.setDeliveryRoundingPO(contract.getPoRoundingFigure());
		item.setReturnRoundingLE(contract.getLeReturnRoundingFigure());

		return item;
	}

	private void addReportRow(List<ReportRow> reportRows, RiskValuesItem item) {
		ReportRow row = new ReportRow(item);
		row.setProperty(SantAgreementRiskValuesReport.SANT_RISK_VALUES_ITEM, item);
		reportRows.add(row);
	}

	@Override
	protected boolean checkProcessEndDate() {
		return false;
	}

	private List<CollateralConfig> loadContracts(String agreementIds, String ownerIds, Vector<String> errorMsgs)
			throws RemoteException {

		MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();

		if (!Util.isEmpty(agreementIds)) {
			mcFilter.setContractIds(Util.string2IntVector(agreementIds));
		}
		if (!Util.isEmpty(ownerIds)) {
			mcFilter.setProcessingOrgIds(Util.string2IntVector(ownerIds));
		}

		List<CollateralConfig> marginCallConfigs = CollateralManagerUtil.loadCollateralConfigs(mcFilter);

		return marginCallConfigs;
	}

	// DPM 21/02/2014 - get also cpty rating matrix
	// For each contract get rating matrix and save it in a map, identified by
	// contract id
	private void loadMCRatings(List<CollateralConfig> contracts,
			Map<Integer, MarginCallCreditRatingConfiguration> poRatingMatrixMap,
			Map<Integer, MarginCallCreditRatingConfiguration> cptyRatingMatrixMap, JDate processDate)
			throws RemoteException {

		for (CollateralConfig contract : contracts) {
			// owner
			if (poRatingMatrixMap.get(contract.getPoRatingsConfigId()) != null) {
				continue;
			}
			if (contract.getPoRatingsConfigId() != 0) {
				MarginCallCreditRatingConfiguration mcRatingConfig = getLatestMarginCallRatings(
						contract.getPoRatingsConfigId(), processDate);
				if (mcRatingConfig != null) {
					poRatingMatrixMap.put(contract.getPoRatingsConfigId(), mcRatingConfig);
				}
			}
			// cpty
			if (cptyRatingMatrixMap.get(contract.getLeRatingsConfigId()) != null) {
				continue;
			}
			if (contract.getLeRatingsConfigId() != 0) {
				MarginCallCreditRatingConfiguration mcRatingConfig = getLatestMarginCallRatings(
						contract.getLeRatingsConfigId(), processDate);
				if (mcRatingConfig != null) {
					cptyRatingMatrixMap.put(contract.getLeRatingsConfigId(), mcRatingConfig);
				}
			}
		}

	}

	public String getGlobalRatingValue(GlobalRatingConfiguration globalRatingConfig, String ratingAgency, int priority)
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

	// Fixed v4.2
	// Get rating matrix matching with id and date received (get all margin call
	// rating lines according to Moody scale)
	public MarginCallCreditRatingConfiguration getLatestMarginCallRatings(int ratingConfigId, JDate date)
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
		List<GlobalRating> globalRatings = globalRatingConfig.getGlobalRating(CreditRating.CURRENT,
				CollateralStaticAttributes.MOODY, CreditRating.ANY);
		List<GlobalRatingValue> ratingValues = null;
		if (!Util.isEmpty(globalRatings)) {
			GlobalRating spRating = globalRatings.get(0);
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

	// Fixed v4.2
	// get priority of rating matrix line corresponding to lower/higher rating
	// value (according to rating direction
	// received)
	@SuppressWarnings("unchecked")
	public int getRequiredRatingPriority(CollateralConfig contract, List<MarginCallCreditRating> mcRatings,
			LegalEntity le, JDate valDate, String direction) throws Exception {

		if (Util.isEmpty(direction)) {
			return -1;
		}

		final int leID = le.getId();
		final DSConnection dsConn = DSConnection.getDefault();
		int currentPriority = -1;

		// get last rating values for LE
		Vector<CreditRating> leRatings = dsConn.getRemoteMarketData().getRatings(null,
				"credit_rating.legal_entity_id = " + leID + " and credit_rating.rating_type = 'Current' "
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
}
