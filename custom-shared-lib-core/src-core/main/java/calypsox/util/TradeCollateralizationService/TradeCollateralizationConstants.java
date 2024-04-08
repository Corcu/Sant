/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util.TradeCollateralizationService;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import calypsox.util.TradeInterfaceUtils;

/**
 * This class contains al the enums and constants used in the TradeCollateralization service.
 * 
 * @author Guillermo Solano
 * 
 */
public class TradeCollateralizationConstants {
	public static String ENGINE_NAME = "SANT_PhoenixDFAEngine";
	
	// DV names constants
	public final static String UPI_CATALOGUE_DV_NAME = "UPI"; // +ColExpProductsMapping
	public final static String MX_CATALOGUE_DV_NAME = "MX"; // +ColExpProductsMapping
	public final static String CALYPSO_CATALOGUE_DV_NAME = "CALYPSO"; // +ColExpProductsMapping
	public final static String GBO_CATALOGUE_DV_NAME = "GBO"; // +ColExpProductsMapping
	public final static String PHOENIX_CATALOGUE_DV_NAME = "Phoenix"; // +ColExpProductsMapping

	// public constant
	/* determines the minimum Threshold to consider in a MCContract for the FULL_PARTIAL_COLLATERALIZED answer */
	public final static Double MINIMUM_THRESHOLD_TO_CONSIDER = 0.0;
	// private constant
	public final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
	// patterns to check the format of input fields
	public final static Pattern datePattern = Pattern.compile("\\d{2}/\\d{2}/[1-3]\\d{3}");

	// class constants
	public final static String BO_REFERENCE_KEYWORD = TradeInterfaceUtils.TRD_IMP_FIELD_BO_REFERENCE;
	public final static String BO_SYSTEM_KEYWORD = TradeInterfaceUtils.TRD_IMP_FIELD_BO_SYSTEM;
	public final static String BOOK_ALIAS_LONG = "ALIAS_BOOK_GBO_LONG";
	// GSM: 13/06/2013 to add short name
	public final static String BOOK_SHORT_ALIAS = "ALIAS_BOOK_GBO";
	public final static String EMPTY = "";
	public final static String NEW_LINE = "\n";

	/*
	 * OUTPUT response messages. Enum that defines collaterization grades or errors produced during the trade generation
	 * or matching. It contains all the possible responses sent to the SourceSystem
	 */
	// Enum defined as: "message" and value response
	public enum RESPONSES {

		/* ERRORS RESPONSES */
		ERR_INPUT_FIELD_MISSING(" Input mandatory field is missing.", -1, "ERR_INPUT_FIELD_MISSING"), // input fields errors
		ERR_INPUT_FORMAT_INCORRECT(" Field has an incorrect format.", -2, "ERR_INPUT_FORMAT_INCORRECT"),
		ERR_MORE_THAN_ONE_CONTRACT_FOUND(" More than one applicable MarginCall Contracts found for the trade", -3, "ERR_MORE_THAN_ONE_CONTRACT_FOUND"),
		ERR_EXCEPTION_OCCURRED(" Exception occurred: ", -4, "ERR_EXCEPTION_OCCURRED"),
		ERR_NO_BOOK_FOUND(" The " + BOOK_SHORT_ALIAS + " is not found.", -5, "ERR_NO_BOOK_FOUND"),
		ERR_FORMAT_FIELD(" Format error", -6, "ERR_FORMAT_FIELD"),
		ERR_PO_DIFFERENT_CONTRACT(" PO does NOT belong to the Mrg contract", -7, "ERR_PO_DIFFERENT_CONTRACT"),
		ERR_PO_NOT_FOUND(" PO NOT found in the system", -8, "ERR_PO_NOT_FOUND"),
		ERR_PRODUCT_NOT_FOUND(" Product not found on the system", -9, "ERR_PRODUCT_NOT_FOUND"),

		/* ERRORS TO BE LOGGED */
		ERR_UPI_DV_MAPPING_MISSING("The mapping is missing in the system. Contact Calypso Admin for help", -101, "ERR_UPI_DV_MAPPING_MISSING"),

		// TRADES GRADES COLLATERALIZATION RESPONSES
		CONTRACT_NOT_FOUND("No applicable MarginCall Contract found for the trade ", 3, "CONTRACT_NOT_FOUND"),
		UNCOLLATERALIZED("There is NO matching between a Collateral Agreement and the trade", 3, "Uncollateralized"),
		ONE_WAY_COLLATERALIZED("Collateral Agreement Match. Unilateral in favour of Santader or CPTY", 0, "OneWay"),
		PARTIAL_COLLATERALIZED("Collateral Agreement Match. Bilateral contract HAS threshold", 1, "Partially"),
		FULL_COLLATERALIZED("Collateral Agreement Match. Bilateral contract HAS NOT threshold", 2, "Fully");

		private final String description;
		private final Integer descriptionValue;
		private final String name;

		private RESPONSES(String description, Integer v, String name) {
			this.description = description;
			this.descriptionValue = v;
			this.name = name;
		}

		public String getDescription() {
			return this.description;
		}

		public Integer getResponseValue() {
			return this.descriptionValue;
		}
		
		public String getName() {
			return this.name;
		}
	}

	/* INPUT Message FIELDS defined for a SIMULATED trade of the DoddFrank Service. */
	// enum defined as fieldName: String field Name + Mandatory_field: boolean + type + position in the array
	public enum DFA_INPUT_SIMULATED_FIELDS {
		BO_EXTERNAL_REFERENCE("boExternalReference", false, "String", 0), //
		BO_SOURCE_SYSTEM("boSourceSystem", false, "String", 1),
		PROCESSING_ORG("processingOrg", true, "String", 2),
		COUNTERPARTY("counterParty", true, "String", 3),
		PRODUCT_TYPE("productType", false, "String", 4),
		START_DATE("startDate", false, "Date", 5),
		END_DATE("endDate", false, "Date", 6),
		VALUE_DATE("valueDate", true, "Date", 7),
		CURRENCY("currency", true, "String", 8),
		MCC_PROCESSING_DATE("processingDate", false, "Date", 9),
		MCC_VALUATION_DATE("valuationDate", false, "Date", 10),
		// For Phoenix
		FO_EXTERNAL_REFERENCE("foExternalReference", false, "String", -1), 
		FO_SOURCE_SYSTEM("foSourceSystem", false, "String", -1),
		INSTRUMENT("instrument", false, "String", -1),
		TIPOLOGY("tipology", false, "String", -1),
		ID("id", false, "String", -1),
		;

		private final String field_name;
		private final boolean mandatory;
		private final String type;
		private final int position;

		DFA_INPUT_SIMULATED_FIELDS(String f, boolean m, String t, int pos) {
			this.field_name = f;
			this.mandatory = m;
			this.type = t;
			this.position = pos;

		}

		public boolean isDate() {
			return this.type.equals("Date");
		}

		public String getFieldName() {
			return this.field_name;
		}

		public boolean isMandatory() {
			return this.mandatory;
		}

		public int getPosition() {
			return this.position;
		}

		/* returns a sorted map of the inputs */
		public static Map<Integer, DFA_INPUT_SIMULATED_FIELDS> inputFieldsMapSorted() {
			Map<Integer, DFA_INPUT_SIMULATED_FIELDS> m = new TreeMap<Integer, DFA_INPUT_SIMULATED_FIELDS>();
			for (DFA_INPUT_SIMULATED_FIELDS input : DFA_INPUT_SIMULATED_FIELDS.values()) {
				m.put(input.getPosition(), input);
			}
			return m;
		}

		/* represents the expected input String */
		public static String toStringSortedAndDescription() {
			Map<Integer, DFA_INPUT_SIMULATED_FIELDS> m = inputFieldsMapSorted();
			StringBuffer sb = new StringBuffer();
			for (Integer pos : m.keySet()) {
				DFA_INPUT_SIMULATED_FIELDS i = m.get(pos);
				sb.append(i.field_name).append(": ");
				sb.append("type: ").append(i.type).append(" - ");
				sb.append("mandatory: ").append(i.mandatory ? "yes" : "no").append(" - ");
				sb.append("position: ").append(i.position).append(" ");
				sb.append("|").append(" ");

			}
			sb.delete(sb.length() - 2, sb.length() - 1);
			return sb.toString();
		}
	};

	/* INPUT Message FIELDS defined for a TRADE IN THE SYSTEM for the DoddFrank Service. */
	// enum defined as fieldName: String field Name + Mandatory_field: boolean + type + position in the array
	public enum DFA_INPUT_TRADE_FIELDS {

		EXTERNAL_REFERENCE("externalReference", true, "String", 0), //
		SOURCE_SYSTEM("sourceSystem", true, "String", 1),
		VALUE_DATE("valueDate", false, "Date", 2);

		private final String field_name;
		private final boolean mandatory;
		private final String type;
		private final int position;

		DFA_INPUT_TRADE_FIELDS(String f, boolean m, String t, int pos) {
			this.field_name = f;
			this.mandatory = m;
			this.type = t;
			this.position = pos;

		}

		public boolean isDate() {
			return this.type.equals("Date");
		}

		public String getFieldName() {
			return this.field_name;
		}

		public boolean isMandatory() {
			return this.mandatory;
		}

		public int getPosition() {
			return this.position;
		}

		/* returns a sorted map of the inputs */
		public static Map<Integer, DFA_INPUT_TRADE_FIELDS> inputFieldsMapSorted() {
			Map<Integer, DFA_INPUT_TRADE_FIELDS> m = new TreeMap<Integer, DFA_INPUT_TRADE_FIELDS>();
			for (DFA_INPUT_TRADE_FIELDS input : DFA_INPUT_TRADE_FIELDS.values()) {
				m.put(input.getPosition(), input);
			}
			return m;
		}

		/* represents the expected input String */
		public static String toStringSortedAndDescription() {
			Map<Integer, DFA_INPUT_TRADE_FIELDS> m = inputFieldsMapSorted();
			StringBuffer sb = new StringBuffer();
			for (Integer pos : m.keySet()) {
				DFA_INPUT_TRADE_FIELDS i = m.get(pos);
				sb.append(i.field_name).append(": ");
				sb.append("type: ").append(i.type).append(" - ");
				sb.append("mandatory: ").append(i.mandatory ? "yes" : "no").append(" - ");
				sb.append("position: ").append(i.position).append(" ");
				sb.append("|").append(" ");

			}
			sb.delete(sb.length() - 2, sb.length() - 1);
			return sb.toString();
		}
	};

	/*
	 * OUTPUT Message FIELDS defined for the DoddFrank Service. For the simulated or real trade options
	 */
	// enum defined as output fieldName: String output Field name
	public enum DFA_OUTPUT_FIELDS {
		IS_COLLATERALIZED_DEAL("isCollateralizedDeal"),
		FO_EXTERNAL_REFERENCE("FOExternalReference"), 
		FO_SOURCE_SYSTEM("FOSourceSystem"),
		BO_EXTERNAL_REFERENCE(DFA_INPUT_SIMULATED_FIELDS.BO_EXTERNAL_REFERENCE.getFieldName()),
		BO_SOURCE_SYSTEM(DFA_INPUT_SIMULATED_FIELDS.BO_SOURCE_SYSTEM.getFieldName()),
		VALUE_DATE("valueDate"),
		COLLATERAL_PROCESSING_DATE("collaProcessingDate"),
		COLLATERAL_VALUATION_DATE("collaValuationDate"),
		COLLATERAL_NAME("CollateralName"),
		COLLATERAL_TYPE("CollateralType"),
		COLLATERAL_BASE_CURRENCY("baseCollatCurrency"),
		COLLATERAL_OWNER("collateralOwner"),
		COLLATERAL_START_DATE("CollateralStartDate"),
		COLLATERAL_END_DATE("CollateralEndDate"),
		PRODUCT_TYPE("productType"),
		CONTRACT_DIRECTION("ContractDirection"),
		CONTRACT_ID("contractId"),
		// For Phoenix
		CONTRACT_NAME("contractName"),
		IS_TRIPARTY("IsTriparty"),
		TRIPARTY_AGENT("TripartyAgent"),
		COLLATERAL_PORTFOLIO_CODE("CollateralPortfolioCode");

		private final String field_name;

		private DFA_OUTPUT_FIELDS(String field_name) {
			this.field_name = field_name;
		}

		public String getFieldName() {
			return this.field_name;
		}

	}

	/* OUTPUT Message FIELDS defined for the Portfolio reconciliation Service - DFA and EMIR */
	// enum defined as output fieldName: String output Field name
	public enum PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS {
		COLLATERAL_AGREEMENT("CollateralAgree"), // 1
		COLLATERAL_TYPE(DFA_OUTPUT_FIELDS.COLLATERAL_TYPE.getFieldName()), // 2
		COUNTERPARTY("Counterparty"), // 3
		TRADE_ID("TradeId"), // 4
		EXTERNAL_REFERENCE(DFA_INPUT_SIMULATED_FIELDS.BO_EXTERNAL_REFERENCE.getFieldName()), // 5 FO
		CLOSE_OF_BUSINESS("CloseBusiness"), // 6 valuation
		STRUCTURE("Structure"), // 7
		TRADE_DATE("TradeDate"), // 8
		TRADE_VALUE_DATE(DFA_INPUT_SIMULATED_FIELDS.VALUE_DATE.getFieldName()), // 9
		TRADE_MATURITY_DATE("MaturityDate"), // 10
		VALUATION_AGENT("ValuationAgent"), // 11
		PORTFOLIO("Portfolio"), // 12
		OWNER("Owner"), // 13
		DEAL_OWNER("DealOwner"), // 14
		INSTRUMENT("Instrument"), // 15
		UNDERLYING("Underlying"), // 16
		PRINCIPAL_CCY("PrincipalCcy"), // 17
		PRINCIPAL("Principal"), // 18
		PRINCIPAL_2_CCY("Principal2Ccy"), // 19
		PRINCIPAL_2("Principal2"), // 20
		IND_AMOUNT("IndAmount"), // 21
		RATE("Rate"), // 22
		RATE_2("Rate2"), // 23
		BUY_SELL("BuySell"), // 24
		BASE_CCY("baseCcy"), // 25
		MTM_BASE_CCY("mtmBaseCcy"), // 26
		USI("USI"), // 27
		SD_MSP("SD_MSP"), // 28
		US_PERSON("USPerson"), // 29
		DFA("DFA"), // 30
		FC_NFC_NFCPLUS("fc_nfc_nfc+"), // 31
		EMIR("EMIR"), // 32
		// GSM: 22/08/13. Added the 7? field for Port. Reconciliation
		UTI_APPLICABLE("UTI"); // 33

		private final String field_name;

		private PORTFOLIO_RECONCILIATION_OUTPUT_FIELDS(String field_name) {
			this.field_name = field_name;
		}

		public String getFieldName() {
			return this.field_name;
		}

	}

}
