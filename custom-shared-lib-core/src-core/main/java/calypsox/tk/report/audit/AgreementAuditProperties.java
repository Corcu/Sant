/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario"," S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.audit;

import java.util.Properties;

public class AgreementAuditProperties {

	private static Properties props = new Properties();

	static {
		props.put("__additionalFields", "ADDITIONALFIELDS");

		props.put("_contractType", "AGREEMENT TYPE");
		props.put("_currency", "BASE CURRENCY");
		// TODO MAPPING TO FIND props.put("", "CALC PERIOD");
		// TODO MAPPING TO FIND props.put("", "CONTRACT TYPE");
		props.put("_leId", "COUNTERPARTY");
		// TODO MAPPING TO FIND props.put("", "DELIVERY ROUNDING");
		props.put("_leRoundingMethod", "DELIVERY ROUNDING (CPTY)");
		props.put("_poRoundingMethod", "DELIVERY ROUNDING (OWNER)");

		props.put("_leMTAAmount", "DELIVERYMTA_CPTY");
		props.put("_poMTAAmount", "DELIVERYMTA_OWNER");
		props.put("_leMTACurrency", "DELIVERYMTA_CPTY_CCY");
		props.put("_poMTACurrency", "DELIVERYMTA_OWNER_CCY");

		props.put("_leCollType", "ELIGIBLE ASSET TYPES");
		props.put("_eligibleCurrencies", "ELIGIBLE_CURRENCIES");
		props.put("eligibilityFilterNames", "ELIGIBLE_SECURITIES");
		// TODO MAPPING TO FIND props.put("", "FACTOR");
		// TODO MAPPING TO FIND props.put("", "INDEPENDENT AMOUNT CPTY");
		// TODO MAPPING TO FIND props.put("", "INDEPENDENT AMOUNT OWNER");
		props.put("_productList", "INSTRUMENTS");
		// TODO MAPPING TO FIND props.put("", "INTEREST TYPE");
		// TODO MAPPING TO FIND props.put("", "LEGAL NAME");
		// TODO MAPPING TO FIND props.put("", "MC VALIDATION");
		props.put("_id", "MC_CONTRACT_INTERNAL_ID");
		props.put("_contractDirection", "ONE WAY");
		props.put("_poId", "OWNER");

		props.put("_additionalPOIds", "OWNERS");
		// TODO MAPPING TO FIND props.put("", "POSTING METHOD");
		// TODO MAPPING TO FIND props.put("", "RATING METHOD");
		props.put("_isRehypothecable", "REHYPOTHECATION");
		// TODO MAPPING TO FIND props.put("", "RETURN ROUNDING");
		props.put("_leReturnRoundingMethod", "RETURN ROUNDING (CPTY)");
		props.put("_poReturnRoundingMethod", "RETURN ROUNDING (OWNER)");
		// TODO MAPPING TO FIND props.put("", "RETURNMTA_CTY");
		// TODO MAPPING TO FIND props.put("", "RETURNMTA_OWNER");
		props.put("_name", "SHORT NAME");
		// TODO MAPPING TO FIND props.put("", "SPLITTING CLAUSE");
		props.put("_agreementStatus", "STATUS");
		props.put("_valuationAgentId", "VALUATION AGENT");

		props.put("_leNewThresholdType", "THRESHOLD_COUNTERPARTY_TYPE");
		props.put("_leNewThresholdAmount", "THRESHOLD_COUNTERPARTY_AMOUNT");
		props.put("_leNewThresholdPercentage", "THRESHOLD_COUNTERPARTY_PERCENTAGE");
		props.put("_leNewThresholdCurrency", "THRESHOLD_COUNTERPARTY_CURRENCY");

		props.put("_poNewThresholdType", "THRESHOLD_OWNER_TYPE");
		props.put("_poNewThresholdAmount", "THRESHOLD_OWNER_AMOUNT");
		props.put("_poNewThresholdPercentage", "THRESHOLD_OWNER_PERCENTAGE");
		props.put("_poNewThresholdCurrency", "THRESHOLD_OWNER_CURRENCY");

		props.put("_disputeToleranceAmount", "TOLERANCE AMOUNT DISPUTE");
		props.put("_disputeContractToleranceAmount", "TOLERANCE AMOUNT DISPUTE CONTRACT");

	}

	public static String getProperty(String key) {
		return props.getProperty(key);
	}

	public static boolean isAuditable(String key) {
		return props.getProperty(key) != null;
	}
}
