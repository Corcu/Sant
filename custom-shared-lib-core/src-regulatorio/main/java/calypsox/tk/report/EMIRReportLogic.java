package calypsox.tk.report;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.regulation.util.EmirUtil;
import calypsox.regulation.util.SantEmirUtil;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class EMIRReportLogic {

	/**
	 * Date format
	 */
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * Date format
	 */
	private static SimpleDateFormat sdfUTC = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

	public static final String MC_CONTRACT_NUMBER = "MC_CONTRACT_NUMBER";
	public static final String LEI = "LEI";
	private static final String CLC = "CLC";
	private static final String LEI_ID = LEI;
	private static final String DTCC_LE_ID = "DTCC_LE_ID";
	private static final String ALL_ROLE = "ALL";
	private static final String CSD = "CSD";
	private static final String CSA = "CSA";
	private static final String PO = "PO";
	private static final String CPTY = "CPTY";
	private static final String CTPY = "CTPY";

	/**
	 * Column names
	 */
	public static final String EXTERNAL_ID = "EXTERNAL_ID";
	public static final String SOURCE_SYSTEM = "SOURCE_SYSTEM";
	public static final String MESSAGE = "MESSAGE";
	public static final String ACTIVITY_OLD = "ACTIVITY_OLD";
	public static final String TRANSACTION_TYPE = "TRANSACTION_TYPE";
	public static final String PRODUCT = "PRODUCT";

	public static final String VERSION_COLUMN = "VERSION";
	public static final String MESSAGETYPE_COLUMN = "MESSAGETYPE";
	public static final String ACTION_COLUMN = "ACTION";
	public static final String LEIPREFIX_COLUMN = "LEIPREFIX";
	public static final String LEIVALUE_COLUMN = "LEIVALUE";
	public static final String TRADEPARTYPREF1_COLUMN = "TRADEPARTYPREF1";
	public static final String TRADEPARTYVAL1_COLUMN = "TRADEPARTYVAL1";
	public static final String COLLATERALPORTFOLIOCODE_COLUMN = "COLLATERALPORTFOLIOCODE";
	public static final String COLLATERALPORTFOLIOIND_COLUMN = "COLLATERALPORTFOLIOINDICATOR";
	public static final String SENDTO_COLUMN = "SENDTO";
	public static final String TRADEPARTY1REPOBLIGATION_COLUMN = "TRADEPARTY1REPOBLIGATION";
	public static final String OTHERPARTYTYPEID_COLUMN = "OTHERPARTYTYPEID";
	public static final String OTHERPARTYID_COLUMN = "OTHERPARTYID";
	public static final String COLLATERALIZED_COLUMN = "COLLATERALIZED";
	public static final String LEVEL_COLUMN = "LEVEL";
	public static final String ACTION_TYPE_PARTY_1 = "ACTIONTYPEPARTY1";

	public static final String UTIPREFIX_COLUMN = "UTIPREFIX";
	public static final String UTIVALUE_COLUMN = "UTI";
	public static final String USIPREFIX_COLUMN = "USIPREFIX";
	public static final String USIVALUE_COLUMN = "USIVALUE";
	public static final String TRADEPARTYTRANSACTIONID1_COLUMN = "TRADEPARTYTRANSACTIONID1";
	public static final String INITIALMARGINPOSTED = "INITIALMARGINPOSTED";
	public static final String CURROFINITIALMARGINPOSTED = "CURROFINITIALMARGINPOSTED";
	public static final String INITIALMARGINRECEIVED = "INITIALMARGINRECEIVED";
	public static final String CURROFINITIALMARGINRECEIVED = "CURROFINITIALMARGINRECEIVED";
	public static final String VARIATIONMARGINPOSTED = "VARIATIONMARGINPOSTED";
	public static final String CURROFVARIATIONMARGINPOSTED = "CURROFVARIATIONMARGINPOSTED";
	public static final String VARMARGINRECEIVED = "VARMARGINRECEIVED";
	public static final String CURROFVARMARGINRECEIVED = "CURROFVARMARGINRECEIVED";
	public static final String EXCESSCOLLPOSTED = "EXCESSCOLLPOSTED";
	public static final String CURROFEXCESSCOLLPOSTED = "CURROFEXCESSCOLLPOSTED";
	public static final String EXCESSCOLLRECEIVED = "EXCESSCOLLRECEIVED";
	public static final String CURROFEXCESSCOLLRECEIVED = "CURROFEXCESSCOLLRECEIVED";

	public static final String RESERVEDPARTICIPANTUSE1 = "RESERVEDPARTICIPANTUSE1"; // GLCS value

	// old
	public static final String COLLATERALPORTFOLIOINDICATOR_OLD_COLUMN = "COLLATERALPORTFOLIOINDICATOR_OLD";
	public static final String COMMENT_COLUMN = "COMMENT";
	public static final String EXECUTIONAGENTPARTY1PREFIX_COLUMN = "EXECUTIONAGENTPARTY1PREFIX";
	public static final String EXECUTIONAGENTPARTYVALUE1_COLUMN = "EXECUTIONAGENTPARTYVALUE1";
	public static final String MESSAGEID_COLUMN = "MESSAGEID";
	public static final String CURRENCYCOLLATERALVALUE_COLUMN = "CURRENCYCOLLATERALVALUE";
	public static final String COLLATERALVALUATIONDATETIME_COLUMN = "COLLATERALVALUATIONDATETIME";
	public static final String COLLATERALREPORTINGDATE_COLUMN = "COLLATERALREPORTINGDATE";
	public static final String VALUEOFTHECOLLATERAL_COLUMN = "VALUEOFTHECOLLATERAL";
	public static final String ACTIVITY_COLUMN = "ACTIVITY";

	/**
	 * Column constant values
	 */
	private static final String VERSION_VALUE = "Coll1.0";
	private static final String MESSAGETYPE_VALUE = "CollateralValue";
	private static final String MESSAGETYPE_LINK = "CollateralLink";
	public static final String ACTION_VALUE_NEW = "New";
	public static final String ACTION_VALUE_MODIF = "Modify";
	private static final String COLLATERALPORTFOLIOIND_VALUE = "Y";
	private static final String SENDTO_VALUE = "DTCCEU";
	private static final String TRADEPARTY1REPOBLIGATION_VALUE = "ESMA";
	private static final String TRD = "TRD";

	/**
	 * 
	 * @param row
	 * @param key
	 * @return
	 */

	public static Object getRowValue(ReportRow row, String key) {
		if (row == null || Util.isEmpty(key))
			return null;
		Object o = row.getProperty(key);
		return o;
	}

	/**
	 * OLD COLUMNS
	 */

	public static String getTransactionType() {
		return TRD;
	}

	public static String getVALUEOFTHECOLLATERAL(ReportRow row) {

		String rst = "";

		SantEmirCacheCloneContracts[] cacheCloneContracts = getCacheCloneContracts(row);

		Object o1 = row.getProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME);
		Object o2 = row.getProperty("Default");

		if (o1 instanceof CollateralConfig && o2 instanceof MarginCallEntryDTO) {
			CollateralConfig col_conf = (CollateralConfig) o1;
			MarginCallEntryDTO dto = (MarginCallEntryDTO) o2;

			double value = 0.0;

			// the value of the head
			if ((dto != null)
					&& !Util.isEmpty(String.valueOf(dto
							.getPreviousTotalMargin()))) {
				value = dto.getPreviousTotalMargin();
			}
			if (cacheCloneContracts != null && cacheCloneContracts.length > 0) {
				int i = 0;
				while (i < cacheCloneContracts.length - 1) {
					// adding the value of its clone contracts
					if (cacheCloneContracts[i].getHeadContractId() == col_conf
							.getId()) {
						value += cacheCloneContracts[i].getValueCloneContract();
					}
					i++;
				}
			}
			// Only retrieved if Total Prev Mrg is negative (the total addition
			// of
			// values of clones contracts and the head one)
			if (value < 0.0) {
				value = -(value);
			} else {
				value = 0.0;
			}

			// VALUEOFTHECOLLATERAL format: 20 integers . 10 decimals
			DecimalFormat df = new DecimalFormat(
					"####################.##########");
			rst = df.format(value);

			if (rst.contains(",")) {
				rst = rst.replace(",", ".").toString();
			}

			return rst;
		}
		return rst;
	}

	public static String getCURRENCYCOLLATERALVALUE(ReportRow row) {
		Object o1 = row.getProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME);
		if (o1 instanceof CollateralConfig) {
			CollateralConfig config = (CollateralConfig) o1;
			return config.getCurrency();
		}
		return null;
	}

	public static String getMESSAGEID(ReportRow row) {
		return null;
	}

	public static String getCOMMENT(ReportRow row) {
		return null;
	}

	public static String getEXECUTIONAGENTPARTY1PREFIX(ReportRow row) {
		return null;
	}

	public static String getEXECUTIONAGENTPARTYVALUE1(ReportRow row) {
		return null;
	}

	public static String getCOLLATERALPORTFOLIOINDICATOR_OLD(ReportRow row) {
		return "true";
	}

	public static String getACTIVITY(ReportRow row) {
		return "NEW";
	}

	@SuppressWarnings("rawtypes")
	public static String getCOLLATERALVALUATIONDATETIME(ReportRow row) {
		Object o = row.getProperty("ProcessDate");
		if (o instanceof JDate) {
			JDate date = (JDate) o;
			final JDate previousDate = date.addBusinessDays(-1, new Vector());
			final JDatetime pDt = previousDate.getJDatetime(TimeZone
					.getDefault());
			final JDatetime pDtAt10am = pDt.add(0, -13, -59, 0, 0);

			if (pDtAt10am != null) {
				return sdfUTC.format(pDtAt10am);
			}
		}
		return null;
	}

	public static String getCOLLATERALREPORTINGDATE(ReportRow row) {
		Object o = row.getProperty("ProcessDate");
		if (o instanceof JDate) {
			JDate date = (JDate) o;
			return sdf.format(date.getJDatetime());
		}
		return null;
	}

	private static SantEmirCacheCloneContracts[] getCacheCloneContracts(
			ReportRow row) {
		Object o1 = row.getProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME);
		Object o2 = row.getProperty(EmirUtil.PROCESSDATE);
		Object o3 = row.getProperty("Default");

		if (o1 instanceof CollateralConfig && o2 instanceof JDate
				&& o3 instanceof MarginCallEntryDTO) {
			CollateralConfig col_conf = (CollateralConfig) o1;
			JDate date = (JDate) o2;
			MarginCallEntryDTO dto = (MarginCallEntryDTO) o3;

			if (!("HEAD".equals(col_conf.getAdditionalField("HEAD_CLONE")))
					&& (!Util.isEmpty(col_conf.getAdditionalField("MCC_HEAD")))) {
				if (((!Util.isEmpty(col_conf
						.getAdditionalField("EMIR_CLONE_VALUE_REPORTABLE"))) && ("YES"
						.equalsIgnoreCase(col_conf
								.getAdditionalField("EMIR_CLONE_VALUE_REPORTABLE"))))
						|| ("WK15".equals(col_conf.getProcessingOrg()
								.getAuthName()))) {

					final List<MarginCallEntryDTO> retriveHeadMCE = SantEmirUtil
							.retriveHeadMCE(new ArrayList<String>(), date,
									col_conf.getAdditionalField("MCC_HEAD"));

					SantEmirCacheCloneContracts[] cacheCloneContracts_ = SantEmirCacheCloneContracts
							.initializeCacheCloneContracts(retriveHeadMCE
									.size());
					int i = 0;
					if (retriveHeadMCE != null) {
						for (MarginCallEntryDTO head_mcd : retriveHeadMCE) {
							cacheCloneContracts_[i].setCloneContractId(dto
									.getCollateralConfigId());
							cacheCloneContracts_[i]
									.setHeadContractName(col_conf
											.getAdditionalField("MCC_HEAD"));
							cacheCloneContracts_[i].setHeadContractId(head_mcd
									.getCollateralConfigId());
							cacheCloneContracts_[i]
									.setValueCloneContract(SantEmirUtil
											.getLogicValueOfTheCollateral(dto));
							i++;
							break;
						}
					}
					return cacheCloneContracts_;
				}
			}
		}
		return null;
	}

	/**
	 * OLD COLUMNS - END
	 */

	public static Object getVERSION(ReportRow row) {
		return VERSION_VALUE;
	}

	public static Object getMESSAGETYPE_VALUE(ReportRow row) {
		return MESSAGETYPE_VALUE;
	}

	public static Object getMESSAGETYPE_Link(ReportRow row) {
		return MESSAGETYPE_LINK;
	}

	public static Object getACTION(ReportRow row) {
		Object o = row.getProperty(EmirUtil.ISDELTA);

		if (o instanceof Boolean) {
			boolean isDelta = (boolean) o;
			if (isDelta) {

				// new logic for delta, calculated in process row
				String value = (String) row.getProperty(EmirUtil.TRADE_ACTION);
				if (!Util.isEmpty(value))
					return value.trim();

				Object o2 = row.getProperty(EmirUtil.TRADE_NAME);
				if (o2 instanceof Trade) {
					Trade trade = (Trade) o2;
					if (trade.getVersion() == 0) {
						return ACTION_VALUE_NEW;
					} else {
						return ACTION_VALUE_MODIF;
					}
				}
			} else {
				return ACTION_VALUE_NEW;
			}
		}

		return ACTION_VALUE_NEW;
	}

	public static Object getLEIPREFIX(ReportRow row) {
		return row
				.getProperty(EMIRLinkingMarginCallDetailEntryReportTemplate.SUBMITTER_REPORT);
	}

	public static Object getLEIVALUE(ReportRow row) {

		String type = row
				.getProperty(EMIRLinkingMarginCallDetailEntryReportTemplate.SUBMITTER_REPORT);

		Object o1 = row.getProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME);
		if (o1 instanceof CollateralConfig && !Util.isEmpty(type)) {
			boolean dtcc = type.trim().equalsIgnoreCase("dtcc") ? true : false;
			CollateralConfig config = (CollateralConfig) o1;
			LegalEntity po = config.getProcessingOrg();
			if (po == null) {
				return null;
			}
			Object o2 = row
					.getProperty(EMIRLinkingMarginCallDetailEntryReportTemplate.REPLACE_OWNER_NAME);
			if (o2 instanceof LegalEntity) {
				LegalEntity replace = (LegalEntity) o2;
				Vector<String> namesLE = row
						.getProperty(EMIRLinkingMarginCallDetailEntryReportTemplate.GROUPING_REPORT_NAMES);
				if (namesLE != null && namesLE.contains(po.getCode())) {
					po = replace;
				}
			}
			LegalEntityAttribute attr;
			if (dtcc) {
				attr = BOCache.getLegalEntityAttribute(
						DSConnection.getDefault(), 0, po.getId(), ALL_ROLE,
						DTCC_LE_ID);
			} else {
				attr = BOCache.getLegalEntityAttribute(
						DSConnection.getDefault(), 0, po.getId(), ALL_ROLE,
						LEI_ID);
			}
			if (attr != null && !Util.isEmpty(attr.getAttributeValue())) {
				return attr.getAttributeValue();
			}
		}
		return null;
	}

	public static Object getTRADEPARTYPREF1(ReportRow row) {
		return getLEIPREFIX(row);
	}

	public static Object getTRADEPARTYVAL1(ReportRow row) {
		return getLEIVALUE(row);
	}

	public static Object getCOLLATERALPORTFOLIOCODE(ReportRow row) {

		Object o = row.getProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME);

		if (o instanceof CollateralConfig) {
			CollateralConfig config = (CollateralConfig) o;
			return config.getId();
		}

		return null;
	}

	public static Object getCOLLATERALPORTFOLIOIND(ReportRow row) {
		return COLLATERALPORTFOLIOIND_VALUE;
	}

	public static Object getSENDTO(ReportRow row) {
		return SENDTO_VALUE;
	}

	public static Object getTRADEPARTY1REPOBLIGATION(ReportRow row) {
		return TRADEPARTY1REPOBLIGATION_VALUE;
	}

	public static Object getOTHERPARTYTYPEID(ReportRow row) {

		Object o1 = row.getProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME);
		if (o1 instanceof CollateralConfig) {
			CollateralConfig config = (CollateralConfig) o1;
			LegalEntity cpty = config.getLegalEntity();
			if (cpty == null) {
				return null;
			}

			LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(
					DSConnection.getDefault(), 0, cpty.getId(), ALL_ROLE,
					LEI_ID);
			if (attr != null && !Util.isEmpty(attr.getAttributeValue())) {
				return LEI;
			} else {
				return CLC;
			}
		}
		return null;
	}

	public static Object getOTHERPARTYID(ReportRow row) {
		Object o1 = row.getProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME);
		if (o1 instanceof CollateralConfig) {
			CollateralConfig config = (CollateralConfig) o1;
			LegalEntity cpty = config.getLegalEntity();
			if (cpty == null) {
				return null;
			}

			LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(
					DSConnection.getDefault(), 0, cpty.getId(), ALL_ROLE,
					LEI_ID);

			if (attr != null && !Util.isEmpty(attr.getAttributeValue())) {
				System.out.println("Contract " + config.getId() + " LE Value" + attr.getAttributeValue());
				return attr.getAttributeValue();
			} else {
				System.out.println("Contract " + config.getId() + " Code" + cpty.getCode());
				return cpty.getCode();
			}
		}
		return null;
	}

	public static Object getCOLLATERALIZED(ReportRow row) {

		Object o = row.getProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME);

		if (o instanceof CollateralConfig) {
			CollateralConfig config = (CollateralConfig) o;
			return config.getAdditionalField(EmirUtil.EMIR_COLLATERAL_VALUE);
		}

		return null;
	}

	static boolean isContractTypeCSD(CollateralConfig config) {
		String contractType = config.getContractType();
		return !Util.isEmpty(contractType)
				&& contractType.equalsIgnoreCase(CSD);
	}

	static boolean isContractTypeCSA(CollateralConfig config) {
		String contractType = config.getContractType();
		return !Util.isEmpty(contractType)
				&& contractType.equalsIgnoreCase(CSA);
	}

	static boolean isContractNameIncludePO(CollateralConfig config) {
		String contractName = config.getName();
		return !Util.isEmpty(contractName) && contractName.contains(PO);
	}

	static boolean isContractNameIncludeCPTYorCTPY(CollateralConfig config) {
		String contractName = config.getName();
		return !Util.isEmpty(contractName)
				&& (contractName.contains(CPTY) || contractName.contains(CTPY));
	}

	static CollateralConfig getCollateralConfig(ReportRow row) {
		Object o = row.getProperty(EmirUtil.MARGIN_CALL_CONFIG_NAME);
		if (o instanceof CollateralConfig) {
			return (CollateralConfig) o;
		}
		return null;
	}

	public static Object getLEVEL(ReportRow row) {
		// return LEVEL_VALUE; changed because of change of requirements
		return "";
	}

	public static String getACTIONTYPEPARTY1() {
		return "V";
	}

	public static String getPrefixFromKeyword(ReportRow row, String keyword,
			boolean isPrefix) {
		String prefix = "";
		String keywordValue = getTradeKeyword(row, keyword);
		if (!Util.isEmpty(keywordValue) && keywordValue.length() >= 10) {
			if (isPrefix) {
				prefix = keywordValue.substring(0, 10);
			} else {
				prefix = keywordValue.substring(10, keywordValue.length());
			}
		}
		return prefix;
	}

	public static String getTradeKeyword(final ReportRow row, String keyword) {
		String keywordValue = "";
		Trade trade = getTradefromRow(row);
		if (trade != null) {
			keywordValue = trade.getKeywordValue(keyword);
		}
		return keywordValue;
	}

	private static Trade getTradefromRow(final ReportRow row) {
		Trade trade = null;
		Object obj = row.getProperty(EmirUtil.TRADE_NAME);
		if (obj instanceof Trade) {
			trade = (Trade) obj;
		}
		return trade;
	}

	public static String getSourceSystem(final ReportRow row) {
		String sourceSystem = "431.4";
		CollateralConfig colConfig = getCollateralConfig(row);
		if (colConfig != null) {
			LegalEntity le = colConfig.getProcessingOrg();
			if (le != null) {
				String code = le.getCode();
				if ("BFOM".equals(code)) {
					sourceSystem = "519.4";
				} else if ("5HSF".equals(code)) {
					sourceSystem = "520.4";
				} else if ("BCHB".equals(code)) {
					sourceSystem = "521.4";
				}
			}
		}
		return sourceSystem;
	}
	
	// GLCS value
	public static String getRESERVEDPARTICIPANTUSE1(ReportRow row) {
		String code = "";
		final Trade trade = getTradefromRow(row);
		
		if (trade != null) {
			final LegalEntity cpty = trade.getCounterParty();
			if(cpty != null) {
				code = cpty.getCode();	
			}
		}
		
		return code;
	}
	
	public static String getValMCRESERVEDPARTICIPANTUSE1(ReportRow row) {
		String code = "";
		final CollateralConfig colConfig = getCollateralConfig(row);
		
		if (colConfig != null ) {
			final LegalEntity le = colConfig.getLegalEntity();
			if(le != null) {
				code = le.getCode();	
			}
		}
		
		return code;
	}


	/*
		Descarta las filas con TRADEPARTYVAL1 = OTHERPARTYID y rellena
		estos campos para las que no son iguales

	 */
	protected static List<ReportRow> discardRows(List<ReportRow> rows){
		List<ReportRow> finalRows = new ArrayList<>();

		if(!Util.isEmpty(rows)){
			for(ReportRow row : rows){
				//TODO refactor...
				String otherparyid = String.valueOf(getOTHERPARTYID(row));
				String tradepartyval = String.valueOf(getLEIVALUE(row));

				if(Util.isEmpty(otherparyid) || Util.isEmpty(tradepartyval)){
					row.setProperty(TRADEPARTYVAL1_COLUMN,tradepartyval);
					row.setProperty(OTHERPARTYID_COLUMN,otherparyid);
					finalRows.add(row);
				}else if(!(otherparyid.equalsIgnoreCase(tradepartyval))){
					row.setProperty(TRADEPARTYVAL1_COLUMN,tradepartyval);
					row.setProperty(OTHERPARTYID_COLUMN,otherparyid);
					finalRows.add(row);
				}

			}
		}

		return finalRows;
	}

}
