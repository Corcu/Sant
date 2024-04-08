/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.refdata;

import static calypsox.tk.core.CollateralStaticAttributes.FITCH;
import static calypsox.tk.core.CollateralStaticAttributes.MOODY;
import static calypsox.tk.core.CollateralStaticAttributes.SC;
import static calypsox.tk.core.CollateralStaticAttributes.SNP;

import java.util.List;
import java.util.Vector;

import calypsox.tk.bo.CustomClientCacheImpl;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.HedgeRelationship;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.ProductCreditRating;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.GlobalRating;
import com.calypso.tk.refdata.GlobalRatingConfiguration;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

@SuppressWarnings("deprecation")
public class SantCreditRatingStaticDataFilter implements StaticDataFilterInterface {

	private static final String ISSUER_RATING_EQV_SP = "Issuer Rating Eqv S&P";
	private static final String ISSUER_RATING_EQV_MOODY = "Issuer Rating Eqv Moody";
	private static final String ISSUER_RATING_EQV_FITCH = "Issuer Rating Eqv Fitch";

	private static final String ISSUE_RATING_EQV_SP = "Issue Rating Eqv S&P";
	private static final String ISSUE_RATING_EQV_MOODY = "Issue Rating Eqv Moody";
	private static final String ISSUE_RATING_EQV_FITCH = "Issue Rating Eqv Fitch";

	private static final String LOWEST_ISSUER_RATING_EQV_SP = "Issuer LOWEST Rating Eqv S&P";
	private static final String LOWEST_ISSUER_RATING_EQV_MOODY = "Issuer LOWEST Rating Eqv Moody";
	private static final String LOWEST_ISSUER_RATING_EQV_FITCH = "Issuer LOWEST Rating Eqv Fitch";

	private static final String LOWEST_ISSUE_RATING_EQV_SP = "Issue LOWEST Rating Eqv S&P";
	private static final String LOWEST_ISSUE_RATING_EQV_MOODY = "Issue LOWEST Rating Eqv Moody";
	private static final String LOWEST_ISSUE_RATING_EQV_FITCH = "Issue LOWEST Rating Eqv Fitch";

	private static final String HIGHEST_ISSUER_RATING_EQV_SP = "Issuer HIGHEST Rating Eqv S&P";
	private static final String HIGHEST_ISSUER_RATING_EQV_MOODY = "Issuer HIGHEST Rating Eqv Moody";
	private static final String HIGHEST_ISSUER_RATING_EQV_FITCH = "Issuer HIGHEST Rating Eqv Fitch";

	private static final String HIGHEST_ISSUE_RATING_EQV_SP = "Issue HIGHEST Rating Eqv S&P";
	private static final String HIGHEST_ISSUE_RATING_EQV_MOODY = "Issue HIGHEST Rating Eqv Moody";
	private static final String HIGHEST_ISSUE_RATING_EQV_FITCH = "Issue HIGHEST Rating Eqv Fitch";

	private static final String ISSUER_CREDITRATING_FITCH = "Issuer.CreditRating.Fitch";
	private static final String ISSUER_CREDITRATING_SP = "Issuer.CreditRating.S&P";
	private static final String ISSUER_CREDITRATING_MOODY = "Issuer.CreditRating.Moody";
	private static final String ISSUER_CREDITRATING_SC = "Issuer.CreditRating.SC";

	private static final String ISSUER_CREDITRATING_FITCH_LATEST = "Issuer.CreditRating.Fitch.Latest";
	private static final String ISSUER_CREDITRATING_SP_LATEST = "Issuer.CreditRating.S&P.Latest";
	private static final String ISSUER_CREDITRATING_MOODY_LATEST = "Issuer.CreditRating.Moody.Latest";
	private static final String ISSUER_CREDITRATING_SC_LATEST = "Issuer.CreditRating.SC.Latest";

	private static final String ISSUER_CREDITRATING_LATEST_SUFFIX = ".Latest";

	@Override
	public boolean fillTreeList(DSConnection con, TreeList tl) {
		Vector<String> nodes = new Vector<String>();
		nodes.add("SantCreditRating");
		tl.add(nodes, ISSUER_RATING_EQV_SP);
		tl.add(nodes, ISSUER_RATING_EQV_MOODY);
		tl.add(nodes, ISSUER_RATING_EQV_FITCH);

		tl.add(nodes, ISSUE_RATING_EQV_SP);
		tl.add(nodes, ISSUE_RATING_EQV_MOODY);
		tl.add(nodes, ISSUE_RATING_EQV_FITCH);

		tl.add(nodes, LOWEST_ISSUER_RATING_EQV_SP);
		tl.add(nodes, LOWEST_ISSUER_RATING_EQV_MOODY);
		tl.add(nodes, LOWEST_ISSUER_RATING_EQV_FITCH);

		tl.add(nodes, LOWEST_ISSUE_RATING_EQV_SP);
		tl.add(nodes, LOWEST_ISSUE_RATING_EQV_MOODY);
		tl.add(nodes, LOWEST_ISSUE_RATING_EQV_FITCH);

		tl.add(nodes, HIGHEST_ISSUER_RATING_EQV_SP);
		tl.add(nodes, HIGHEST_ISSUER_RATING_EQV_MOODY);
		tl.add(nodes, HIGHEST_ISSUER_RATING_EQV_FITCH);

		tl.add(nodes, HIGHEST_ISSUE_RATING_EQV_SP);
		tl.add(nodes, HIGHEST_ISSUE_RATING_EQV_MOODY);
		tl.add(nodes, HIGHEST_ISSUE_RATING_EQV_FITCH);

		tl.add(nodes, ISSUER_CREDITRATING_FITCH);
		tl.add(nodes, ISSUER_CREDITRATING_SP);
		tl.add(nodes, ISSUER_CREDITRATING_MOODY);
		tl.add(nodes, ISSUER_CREDITRATING_SC);

		tl.add(nodes, ISSUER_CREDITRATING_FITCH_LATEST);
		tl.add(nodes, ISSUER_CREDITRATING_SP_LATEST);
		tl.add(nodes, ISSUER_CREDITRATING_MOODY_LATEST);
		tl.add(nodes, ISSUER_CREDITRATING_SC_LATEST);

		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void getDomainValues(DSConnection con, Vector v) {
		v.add(ISSUER_RATING_EQV_SP);
		v.add(ISSUER_RATING_EQV_MOODY);
		v.add(ISSUER_RATING_EQV_FITCH);

		v.add(ISSUE_RATING_EQV_SP);
		v.add(ISSUE_RATING_EQV_MOODY);
		v.add(ISSUE_RATING_EQV_FITCH);

		v.add(LOWEST_ISSUER_RATING_EQV_SP);
		v.add(LOWEST_ISSUER_RATING_EQV_MOODY);
		v.add(LOWEST_ISSUER_RATING_EQV_FITCH);

		v.add(LOWEST_ISSUE_RATING_EQV_SP);
		v.add(LOWEST_ISSUE_RATING_EQV_MOODY);
		v.add(LOWEST_ISSUE_RATING_EQV_FITCH);

		v.add(HIGHEST_ISSUER_RATING_EQV_SP);
		v.add(HIGHEST_ISSUER_RATING_EQV_MOODY);
		v.add(HIGHEST_ISSUER_RATING_EQV_FITCH);

		v.add(HIGHEST_ISSUE_RATING_EQV_SP);
		v.add(HIGHEST_ISSUE_RATING_EQV_MOODY);
		v.add(HIGHEST_ISSUE_RATING_EQV_FITCH);

		v.add(ISSUER_CREDITRATING_FITCH);
		v.add(ISSUER_CREDITRATING_SP);
		v.add(ISSUER_CREDITRATING_MOODY);
		v.add(ISSUER_CREDITRATING_SC);

		v.add(ISSUER_CREDITRATING_FITCH_LATEST);
		v.add(ISSUER_CREDITRATING_SP_LATEST);
		v.add(ISSUER_CREDITRATING_MOODY_LATEST);
		v.add(ISSUER_CREDITRATING_SC_LATEST);

	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {
		Vector<String> v = new Vector<String>();
		v.addElement(StaticDataFilterElement.S_IN);

		if (attributeName.equals(ISSUER_CREDITRATING_FITCH) || attributeName.equals(ISSUER_CREDITRATING_SP)
				|| attributeName.equals(ISSUER_CREDITRATING_MOODY) || attributeName.equals(ISSUER_CREDITRATING_SC)) {
			v.addElement(StaticDataFilterElement.S_NOT_IN);
		}

		if (attributeName.equals(ISSUER_CREDITRATING_FITCH_LATEST)
				|| attributeName.equals(ISSUER_CREDITRATING_SP_LATEST)
				|| attributeName.equals(ISSUER_CREDITRATING_MOODY_LATEST)
				|| attributeName.equals(ISSUER_CREDITRATING_SC_LATEST)) {
			v.addElement(StaticDataFilterElement.S_NOT_IN);
		}
		return v;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getDomain(DSConnection con, String attributeName) {
		Vector v = new Vector();
		try {
			if (attributeName.endsWith(SNP) || attributeName.endsWith(SNP + ISSUER_CREDITRATING_LATEST_SUFFIX)) {
				v = DSConnection.getDefault().getRemoteReferenceData().getRatingValues()
						.getRatingValues(SNP, CreditRating.CURRENT);
			} else if (attributeName.endsWith(MOODY)
					|| attributeName.endsWith(MOODY + ISSUER_CREDITRATING_LATEST_SUFFIX)) {
				v = DSConnection.getDefault().getRemoteReferenceData().getRatingValues()
						.getRatingValues(MOODY, CreditRating.CURRENT);
			} else if (attributeName.endsWith(FITCH)
					|| attributeName.endsWith(FITCH + ISSUER_CREDITRATING_LATEST_SUFFIX)) {
				v = DSConnection.getDefault().getRemoteReferenceData().getRatingValues()
						.getRatingValues(FITCH, CreditRating.CURRENT);
			} else if (attributeName.endsWith(SC) || attributeName.endsWith(SC + ISSUER_CREDITRATING_LATEST_SUFFIX)) {
				v = DSConnection.getDefault().getRemoteReferenceData().getRatingValues()
						.getRatingValues(SC, CreditRating.CURRENT);
			}
		} catch (Exception exc) {
			Log.error(this, exc); //sonar purpose
		}
		return v;
	}

	
	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {
		if ((product == null) || !(product instanceof Bond)) {
			return null;
		}

		@SuppressWarnings("unchecked")
		Vector<String> filtElementValues = (Vector<String>)element.getValues();
		if (Util.isEmpty(filtElementValues)) {
			return null;
		}

		Bond bond = (Bond) product;

		try {
			if (filterElement.equals(ISSUER_CREDITRATING_FITCH) || filterElement.equals(ISSUER_CREDITRATING_SP)
					|| filterElement.equals(ISSUER_CREDITRATING_MOODY) || filterElement.equals(ISSUER_CREDITRATING_SC)) {

				Vector<CreditRating> issuerRatings = getIssuerRatings(bond.getIssuerId());
				for (CreditRating rating : issuerRatings) {
					if (filterElement.equals(ISSUER_CREDITRATING_FITCH) && FITCH.equals(rating.getAgencyName())) {
						return rating.getRatingValue();
					} else if (filterElement.equals(ISSUER_CREDITRATING_MOODY) && MOODY.equals(rating.getAgencyName())) {
						return rating.getRatingValue();
					} else if (filterElement.equals(ISSUER_CREDITRATING_SP) && SNP.equals(rating.getAgencyName())) {
						return rating.getRatingValue();
					} else if (filterElement.equals(ISSUER_CREDITRATING_SC) && SC.equals(rating.getAgencyName())) {
						return rating.getRatingValue();
					}
				}
			} else if (filterElement.equals(ISSUER_CREDITRATING_FITCH_LATEST)
					|| filterElement.equals(ISSUER_CREDITRATING_SP_LATEST)
					|| filterElement.equals(ISSUER_CREDITRATING_MOODY_LATEST)
					|| filterElement.equals(ISSUER_CREDITRATING_SC_LATEST)) {

				Vector<CreditRating> issuerRatings = getLatestIssuerRatings(bond.getIssuerId());
				for (CreditRating rating : issuerRatings) {
					if (filterElement.equals(ISSUER_CREDITRATING_FITCH_LATEST) && FITCH.equals(rating.getAgencyName())) {
						return rating.getRatingValue();
					} else if (filterElement.equals(ISSUER_CREDITRATING_MOODY_LATEST)
							&& MOODY.equals(rating.getAgencyName())) {
						return rating.getRatingValue();
					} else if (filterElement.equals(ISSUER_CREDITRATING_SP_LATEST)
							&& SNP.equals(rating.getAgencyName())) {
						return rating.getRatingValue();
					} else if (filterElement.equals(ISSUER_CREDITRATING_SC_LATEST) && SC.equals(rating.getAgencyName())) {
						return rating.getRatingValue();
					}
				}
			} else if (filterElement.equals(HIGHEST_ISSUER_RATING_EQV_SP)
					|| filterElement.equals(HIGHEST_ISSUER_RATING_EQV_MOODY)
					|| filterElement.equals(HIGHEST_ISSUER_RATING_EQV_FITCH)) {
				// Highest Issuer
				int highestPriority = getHighestIssuerRatingPriority(bond.getIssuerId());
				if (highestPriority == -1) {
					return null;
				}

				for (String filtElementValue : filtElementValues) {
					int priorityToLookFor = getEquivalentPriorityToLookFor(filterElement, filtElementValue);
					if (highestPriority == priorityToLookFor) {
						return filtElementValue;
					}
				}

			} else if (filterElement.equals(HIGHEST_ISSUE_RATING_EQV_SP)
					|| filterElement.equals(HIGHEST_ISSUE_RATING_EQV_MOODY)
					|| filterElement.equals(HIGHEST_ISSUE_RATING_EQV_FITCH)) {
				// Highest Issue
				int highestPriority = getHighestProductRatingPriority(product.getId());
				if (highestPriority == -1) {
					return null;
				}

				for (String filtElementValue : filtElementValues) {
					int priorityToLookFor = getEquivalentPriorityToLookFor(filterElement, filtElementValue);
					if (highestPriority == priorityToLookFor) {
						return filtElementValue;
					}
				}

			} else if (filterElement.equals(LOWEST_ISSUER_RATING_EQV_SP)
					|| filterElement.equals(LOWEST_ISSUER_RATING_EQV_MOODY)
					|| filterElement.equals(LOWEST_ISSUER_RATING_EQV_FITCH)) {
				// Lowest Issuer
				int highestPriority = getLowestIssuerRatingPriority(bond.getIssuerId());
				if (highestPriority == -1) {
					return null;
				}

				for (String filtElementValue : filtElementValues) {
					int priorityToLookFor = getEquivalentPriorityToLookFor(filterElement, filtElementValue);
					if (highestPriority == priorityToLookFor) {
						return filtElementValue;
					}
				}
			} else if (filterElement.equals(LOWEST_ISSUE_RATING_EQV_SP)
					|| filterElement.equals(LOWEST_ISSUE_RATING_EQV_MOODY)
					|| filterElement.equals(LOWEST_ISSUE_RATING_EQV_FITCH)) {
				// Lowest Issue
				int lowestPriority = getLowestProductRatingPriority(product.getId());
				if (lowestPriority == -1) {
					return null;
				}

				for (String filtElementValue : filtElementValues) {
					int priorityToLookFor = getEquivalentPriorityToLookFor(filterElement, filtElementValue);
					if (lowestPriority == priorityToLookFor) {
						return filtElementValue;
					}
				}
			} else if (filterElement.equals(ISSUE_RATING_EQV_SP) || filterElement.equals(ISSUE_RATING_EQV_MOODY)
					|| filterElement.equals(ISSUE_RATING_EQV_FITCH)) {
				// Issue Rating Check
				Vector<ProductCreditRating> productRatings = getProductRatings(product.getId());
				// Issuer Rating check
				if (Util.isEmpty(productRatings)) {
					return null;
				}
				for (String filtElementValue : filtElementValues) {
					// 1. get the priority that we are looking for
					int priorityToLookFor = getEquivalentPriorityToLookFor(filterElement, filtElementValue);

					if (priorityToLookFor == -1) {// Equivalent Priority not found
						return null;
					}

					if (acceptProductRatings(productRatings, priorityToLookFor)) {
						return filtElementValue;
					}
				}

			} else if (filterElement.equals(ISSUER_RATING_EQV_SP) || filterElement.equals(ISSUER_RATING_EQV_MOODY)
					|| filterElement.equals(ISSUER_RATING_EQV_FITCH)) {
				// Issuer Rating check
				for (String filtElementValue : filtElementValues) {
					// 1. get the priority that we are looking for
					int priorityToLookFor = getEquivalentPriorityToLookFor(filterElement, filtElementValue);
					if (priorityToLookFor == -1) { // Equivalent Priority not found
						return null;
					}

					Vector<CreditRating> issuerRatings = getIssuerRatings(bond.getIssuerId());
					if (acceptIssueRating(issuerRatings, priorityToLookFor)) {
						return filtElementValue;
					}
				}

			}
		} catch (Exception e) {
			Log.info(SantCreditRatingStaticDataFilter.class, e.getLocalizedMessage(), e);
		}

		return null;
	}

	
	public static Vector<ProductCreditRating> getProductRatings(int product_id) throws Exception {
		JDate date = JDate.getNow();

		String where = "as_of_date in ( "
				+ "select max(pr2.as_of_date) from product_credit_rating pr2 where pr2.product_id=" + product_id
				+ " and pr2.rating_type=" + Util.string2SQLString(CreditRating.CURRENT) + " and pr2.as_of_Date<="
				+ Util.date2SQLString(date) + " group by pr2.rating_agency_name )" + "and product_id=" + product_id
				+ " and rating_type=" + Util.string2SQLString(CreditRating.CURRENT);

		Vector<ProductCreditRating> productRatings = DSConnection.getDefault().getRemoteMarketData()
				.getProductRatings(null, where);
		return productRatings;
	}

	
	@SuppressWarnings("unchecked")
	public static Vector<CreditRating> getIssuerRatings(int le_id) throws Exception {
		String where = " credit_rating.legal_entity_id=" + le_id + " and credit_rating.rating_type="
				+ Util.string2SQLString(CreditRating.CURRENT) + " AND credit_rating.debt_seniority='SENIOR_UNSECURED' ";
		return DSConnection.getDefault().getRemoteMarketData().getLatestRatings(null, where, null, JDate.getNow(),null);
	}

	
	@SuppressWarnings("unchecked")
	public static Vector<CreditRating> getLatestIssuerRatings(int le_id) throws Exception {
		String where = " credit_rating.legal_entity_id=" + le_id + " and credit_rating.rating_type="
				+ Util.string2SQLString(CreditRating.CURRENT);
		return DSConnection.getDefault().getRemoteMarketData().getLatestRatings(null, where, null, JDate.getNow(),null);
	}

	public static boolean acceptProductRatings(Vector<ProductCreditRating> productRatings, int priorityToLookFor) {
		return acceptProductRatings(productRatings, priorityToLookFor, "=");
	}

	public static boolean acceptProductRatings(Vector<ProductCreditRating> productRatings, int priorityToLookFor,
			String sign) {
		for (ProductCreditRating productRating : productRatings) {
			return acceptProductRatings(productRating, priorityToLookFor, sign);
		}
		return false;
	}

	/**
	 * Checks if any of the passed in ratings has matched with the priorityToLookFor with the sign passed in.
	 * 
	 * @param productRatings
	 * @param agency
	 * @param priorityToLookFor
	 * @param sign
	 * @return
	 */
	public static boolean acceptProductRatings(ProductCreditRating productRating, int priorityToLookFor, String sign) {

		int priority = -1;
		try {
			priority = getGlobalPriority(productRating.getAgencyName(), productRating.getRatingValue());
		} catch (Exception e) {
			Log.error(SantCreditRatingStaticDataFilter.class, e); //sonar purpose
		}
		if (priority != -1) {
			if ((sign.equals("=") && (priority == priorityToLookFor))
					|| (sign.equals("<=") && (priority >= priorityToLookFor))
					|| (sign.equals(">=") && (priority <= priorityToLookFor))) {
				return true;
			}
		}
		return false;
	}

	public static boolean acceptIssueRating(Vector<CreditRating> issuerRatings, int priorityToLookFor) {
		return acceptIssueRating(issuerRatings, priorityToLookFor, "=");
	}

	public static boolean acceptIssueRating(Vector<CreditRating> issuerRatings, int priorityToLookFor, String sign) {
		for (CreditRating rating : issuerRatings) {
			if (acceptIssueRating(rating, priorityToLookFor, sign)) {
				return true;
			}
		}
		return false;
	}

	public static boolean acceptIssueRating(CreditRating rating, int priorityToLookFor, String sign) {

		int priority = -1;

		try {
			priority = getGlobalPriority(rating.getAgencyName(), rating.getRatingValue());
		} catch (Exception e) {
			Log.error(SantCreditRatingStaticDataFilter.class, e); //sonar purpose
		}

		if (priority != -1) {
			if ((sign.equals("=") && (priority == priorityToLookFor))
					|| (sign.equals("<=") && (priority >= priorityToLookFor))
					|| (sign.equals(">=") && (priority <= priorityToLookFor))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns ProductCreditRating for a given Agency, from a Vector of ProductCreditRatings
	 * 
	 * @param productRatings
	 * @param agency
	 * @return
	 */
	public static String getProductCreditRatingValue(Vector<ProductCreditRating> productRatings, String agency) {
		for (ProductCreditRating rating : productRatings) {
			if (rating.getAgencyName().equals(agency)) {
				return rating.getRatingValue();
			}
		}
		return null;
	}

	public static String getIssuerRatingValue(Vector<CreditRating> issuerRatings, String agency) {
		for (CreditRating rating : issuerRatings) {
			if (rating.getAgencyName().equals(agency)) {
				return rating.getRatingValue();
			}
		}
		return null;
	}

	public static int getHighestProductRatingPriority(int product_id) throws Exception {
		int priority = -1;
		Vector<ProductCreditRating> productRatings = getProductRatings(product_id);

		if (!Util.isEmpty(productRatings)) {
			for (ProductCreditRating rating : productRatings) {
				int tempPriority = getGlobalPriority(rating.getAgencyName(), rating.getRatingValue());
				if (tempPriority != -1) {
					if (priority == -1) {
						priority = tempPriority;
					} else if (tempPriority < priority) {
						priority = tempPriority;
					}
				}
			}
		}
		return priority;
	}

	public static int getLowestProductRatingPriority(int product_id) throws Exception {
		int priority = -1;
		Vector<ProductCreditRating> productRatings = getProductRatings(product_id);

		if (!Util.isEmpty(productRatings)) {
			for (ProductCreditRating rating : productRatings) {
				int tempPriority = getGlobalPriority(rating.getAgencyName(), rating.getRatingValue());
				if (tempPriority > priority) {
					priority = tempPriority;
				}
			}
		}
		return priority;
	}

	public static int getLowestIssuerRatingPriority(int le_id) throws Exception {
		int priority = -1;

		Vector<CreditRating> issuerRatings = getIssuerRatings(le_id);
		for (CreditRating rating : issuerRatings) {
			int tmpPriority = getGlobalPriority(rating.getAgencyName(), rating.getRatingValue());
			if (tmpPriority > priority) {
				priority = tmpPriority;
			}
		}

		return priority;
	}

	public static int getHighestIssuerRatingPriority(int le_id) throws Exception {
		int priority = -1;

		Vector<CreditRating> issuerRatings = getIssuerRatings(le_id);

		for (CreditRating rating : issuerRatings) {
			int tmpPriority = getGlobalPriority(rating.getAgencyName(), rating.getRatingValue());
			if (tmpPriority != -1) {
				if (priority == -1) {
					priority = tmpPriority;
				} else if (tmpPriority < priority) {
					priority = tmpPriority;
				}
			}
		}

		return priority;
	}

	/**
	 * Gives the priority for a given Agency and Rating Value from the Global Credit Rating Matrix
	 * 
	 * @param ratingAgency
	 * @param ratingValue
	 * @return
	 * @throws Exception
	 */
	public static int getGlobalPriority(String ratingAgency, String ratingValue) throws Exception {
		if (Util.isEmpty(ratingAgency) || Util.isEmpty(ratingValue)) {
			return -1;
		}

		GlobalRatingConfiguration globalRatingConfig = CustomClientCacheImpl.getGlobalRatingConfiguration();

		List<GlobalRating> spGlobalRatings = globalRatingConfig.getGlobalRating(CreditRating.CURRENT, ratingAgency,
				CreditRating.ANY);
		if (Util.isEmpty(spGlobalRatings)) {
			Log.info(SantCreditRatingStaticDataFilter.class, "No Rating exists for Agency=" + ratingAgency);
			return -1;
		}

		GlobalRating rating = spGlobalRatings.get(0);
		List<Integer> priorities = rating.getPriorityOf(ratingValue);

		if (Util.isEmpty(priorities)) {
			Log.info(SantCreditRatingStaticDataFilter.class, "No priorities exist for Agency=" + ratingAgency
					+ "; ratingValue=" + ratingValue);
			return -1;
		}
		return priorities.get(0);
	}

	private int getEquivalentPriorityToLookFor(String filterElement, String filtElementValue) throws Exception {
		int priorityToLookFor = -1;
		if (filterElement.endsWith(SNP)) {
			priorityToLookFor = getGlobalPriority(SNP, filtElementValue);
		} else if (filterElement.endsWith(MOODY)) {
			priorityToLookFor = getGlobalPriority(MOODY, filtElementValue);
		} else if (filterElement.endsWith(FITCH)) {
			priorityToLookFor = getGlobalPriority(FITCH, filtElementValue);
		}
		return priorityToLookFor;
	}

	@Override
	public boolean isTradeNeeded(String attributeName) {
		return true;
	}

}
