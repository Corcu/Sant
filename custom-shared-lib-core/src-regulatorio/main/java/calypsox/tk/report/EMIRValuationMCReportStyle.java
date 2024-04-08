package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.MarginCallEntryBaseReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import calypsox.regulation.util.EmirUtil;

public class EMIRValuationMCReportStyle extends MarginCallEntryReportStyle {

	public static final String EMIR_PREFIX = "EMIR.";

	private static final long serialVersionUID = -994003393467858501L;
	private final com.calypso.tk.report.MarginCallEntryReportStyle marginCallEntryReportStyle = new com.calypso.tk.report.MarginCallEntryReportStyle();
	private final MarginCallEntryBaseReportStyle marginCallEntryBaseReportStyle = new MarginCallEntryBaseReportStyle();
	private final CollateralConfigReportStyle collateralConfigReportStyle = new CollateralConfigReportStyle();

	private static final String MCE_PREFIX = "MarginCallEntry.";
	private static final String MCEB_PREFIX = "MarginCallEntryBase.";
	private static final String CC_PREFIX = "CollateralConfig.";
	private static final String CONTRACT_CURRENCY = "Contract Currency";
	private static final String GLOBAL_REQUIRED_MRG = "Global Required Mrg";
	private static final String DAILY_TOTAL_MRG = "Total Prev Mrg";

	private static final String COL = "COL";
	private static final String COLLATERAL = "Collateral";

	private static final DecimalFormat df = new DecimalFormat("##0.00", new DecimalFormatSymbols(Locale.ENGLISH));

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		if (row != null) {
			if (columnName.contains(EMIR_PREFIX)) {
				final String newProductColumnName = getEMIRColumnName(EMIR_PREFIX, columnName);
				if (!Util.isEmpty(newProductColumnName)) {
					return getEmirColumnValue(newProductColumnName, row, errors);
				}
			} else if (columnName.contains(MCE_PREFIX)) {
				final String newProductColumnName = MarginCallEntryReportStyle.getColumnName(MCE_PREFIX, columnName);
				if (!Util.isEmpty(newProductColumnName)) {
					return this.marginCallEntryReportStyle.getColumnValue(row, newProductColumnName, errors);
				} else {
					return this.marginCallEntryReportStyle.getColumnValue(row, columnName, errors);
				}
			} else if (columnName.contains(CC_PREFIX)) {
				final String newProductColumnName = CollateralConfigReportStyle.getColumnName(CC_PREFIX, columnName);
				if (!Util.isEmpty(newProductColumnName)) {
					return this.collateralConfigReportStyle.getColumnValue(row, newProductColumnName, errors);
				} else {
					return this.collateralConfigReportStyle.getColumnValue(row, columnName, errors);
				}
			} else if (columnName.contains(MCEB_PREFIX)) {
				final String newProductColumnName = MarginCallEntryBaseReportStyle.getColumnName(MCEB_PREFIX,
						columnName);
				if (!Util.isEmpty(newProductColumnName)) {
					return this.marginCallEntryBaseReportStyle.getColumnValue(row, newProductColumnName, errors);
				} else {
					return this.marginCallEntryBaseReportStyle.getColumnValue(row, columnName, errors);
				}
			} else {
				return super.getColumnValue(row, columnName, errors);
			}
		}

		return super.getColumnValue(row, columnName, errors);
	}

	private Object getEmirColumnValue(String newProductColumnName, ReportRow row, Vector errors) {
		if (newProductColumnName.equals(EMIRReportLogic.EXTERNAL_ID)) {
			return EMIRReportLogic.getCOLLATERALPORTFOLIOCODE(row);
		}else if(newProductColumnName.equals(EMIRReportLogic.SOURCE_SYSTEM)){
			return EMIRReportLogic.getSourceSystem(row);
		}else if(newProductColumnName.equals(EMIRReportLogic.MESSAGE)){
			return getMessage();
		}else if(newProductColumnName.equals(EMIRReportLogic.ACTIVITY_OLD)){//activity
			return getOldActivity(row);
		}else if(newProductColumnName.equals(EMIRReportLogic.TRANSACTION_TYPE)){
			return EMIRReportLogic.getTransactionType();
		}else if(newProductColumnName.equals(EMIRReportLogic.PRODUCT)){
			return getProduct();
		}else if (newProductColumnName.equals(EMIRReportLogic.VERSION_COLUMN)) {
			return EMIRReportLogic.getVERSION(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.MESSAGETYPE_COLUMN)) {
			return EMIRReportLogic.getMESSAGETYPE_VALUE(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.ACTION_COLUMN)) {
			return EMIRReportLogic.getACTION(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.LEIPREFIX_COLUMN)) {
			return EMIRReportLogic.getLEIPREFIX(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.LEIVALUE_COLUMN)) {
			return EMIRReportLogic.getLEIVALUE(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.TRADEPARTYPREF1_COLUMN)) {
			return EMIRReportLogic.getTRADEPARTYPREF1(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.TRADEPARTYVAL1_COLUMN)) {
			return EMIRReportLogic.getTRADEPARTYVAL1(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.COLLATERALPORTFOLIOCODE_COLUMN)) {
			return EMIRReportLogic.getCOLLATERALPORTFOLIOCODE(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.COLLATERALPORTFOLIOIND_COLUMN)) {
			return EMIRReportLogic.getCOLLATERALPORTFOLIOIND(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.SENDTO_COLUMN)) {
			return EMIRReportLogic.getSENDTO(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.TRADEPARTY1REPOBLIGATION_COLUMN)) {
			return EMIRReportLogic.getTRADEPARTY1REPOBLIGATION(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.OTHERPARTYTYPEID_COLUMN)) {
			return EMIRReportLogic.getOTHERPARTYTYPEID(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.OTHERPARTYID_COLUMN)) {
			return EMIRReportLogic.getOTHERPARTYID(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.COLLATERALIZED_COLUMN)) {
			return EMIRReportLogic.getCOLLATERALIZED(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.LEVEL_COLUMN)) {
			return EMIRReportLogic.getLEVEL(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.COLLATERALPORTFOLIOINDICATOR_OLD_COLUMN)) {
			return EMIRReportLogic.getCOLLATERALPORTFOLIOINDICATOR_OLD(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.COMMENT_COLUMN)) {
			return EMIRReportLogic.getCOMMENT(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.EXECUTIONAGENTPARTY1PREFIX_COLUMN)) {
			return EMIRReportLogic.getEXECUTIONAGENTPARTY1PREFIX(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.EXECUTIONAGENTPARTYVALUE1_COLUMN)) {
			return EMIRReportLogic.getEXECUTIONAGENTPARTYVALUE1(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.MESSAGEID_COLUMN)) {
			return EMIRReportLogic.getMESSAGEID(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.CURRENCYCOLLATERALVALUE_COLUMN)) {
			return EMIRReportLogic.getCURRENCYCOLLATERALVALUE(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.COLLATERALVALUATIONDATETIME_COLUMN)) {
			return EMIRReportLogic.getCOLLATERALVALUATIONDATETIME(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.COLLATERALREPORTINGDATE_COLUMN)) {
			return EMIRReportLogic.getCOLLATERALREPORTINGDATE(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.VALUEOFTHECOLLATERAL_COLUMN)) {
			return EMIRReportLogic.getVALUEOFTHECOLLATERAL(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.ACTION_TYPE_PARTY_1)) {
			return EMIRReportLogic.getACTIONTYPEPARTY1();
		} else if (newProductColumnName.equals(EMIRReportLogic.INITIALMARGINPOSTED)) {
			return getINITIALMARGINPOSTED(row, errors);
		} else if (newProductColumnName.equals(EMIRReportLogic.CURROFINITIALMARGINPOSTED)) {
			return getIMContractCurrency(row, errors,true);
		} else if (newProductColumnName.equals(EMIRReportLogic.INITIALMARGINRECEIVED)) {
			return getINITIALMARGINRECEIVED(row, errors);
		} else if (newProductColumnName.equals(EMIRReportLogic.CURROFINITIALMARGINRECEIVED)) {
			return getIMContractCurrency(row, errors,false);
		} else if (newProductColumnName.equals(EMIRReportLogic.VARIATIONMARGINPOSTED)) {
			return getVARIATIONMARGINPOSTED(row, errors);
		} else if (newProductColumnName.equals(EMIRReportLogic.CURROFVARIATIONMARGINPOSTED)) {
			return getContractCurrency(EMIRReportLogic.VARIATIONMARGINPOSTED, row, errors);
		} else if (newProductColumnName.equals(EMIRReportLogic.VARMARGINRECEIVED)) {
			return getVARIATIONMARGINRECEIVED(row, errors);
		} else if (newProductColumnName.equals(EMIRReportLogic.CURROFVARMARGINRECEIVED)) {
			return getContractCurrency(EMIRReportLogic.VARMARGINRECEIVED, row, errors);
		} else if (newProductColumnName.equals(EMIRReportLogic.EXCESSCOLLPOSTED)) {
			return getEXCESSCOLLPOSTED(row, errors);
		} else if (newProductColumnName.equals(EMIRReportLogic.CURROFEXCESSCOLLPOSTED)) {
			return getContractCurrency(EMIRReportLogic.EXCESSCOLLPOSTED, row, errors);
		} else if (newProductColumnName.equals(EMIRReportLogic.EXCESSCOLLRECEIVED)) {
			return getEXCESSCOLLRECEIVED(row, errors);
		} else if (newProductColumnName.equals(EMIRReportLogic.CURROFEXCESSCOLLRECEIVED)) {
			return getContractCurrency(EMIRReportLogic.EXCESSCOLLRECEIVED, row, errors);
		}else if (newProductColumnName.equals(EMIRReportLogic.RESERVEDPARTICIPANTUSE1)) { // GLCS value
			return EMIRReportLogic.getValMCRESERVEDPARTICIPANTUSE1(row);
		}else {
			return "";
		}
	}

	@Override
	public TreeList getTreeList() {
		TreeList treeList = new TreeList();
		addSubTreeList(treeList, new Vector<String>(), MCE_PREFIX, this.marginCallEntryReportStyle.getTreeList());
		addSubTreeList(treeList, new Vector<String>(), MCEB_PREFIX, this.marginCallEntryBaseReportStyle.getTreeList());
		addSubTreeList(treeList, new Vector<String>(), CC_PREFIX, this.collateralConfigReportStyle.getTreeList());

		TreeList emir = new TreeList();
		emir.add(EMIRReportLogic.EXTERNAL_ID);
		emir.add(EMIRReportLogic.SOURCE_SYSTEM);
		emir.add(EMIRReportLogic.MESSAGE);
		emir.add(EMIRReportLogic.ACTIVITY_OLD);
		emir.add(EMIRReportLogic.TRANSACTION_TYPE);
		emir.add(EMIRReportLogic.PRODUCT);

		emir.add(EMIRReportLogic.VERSION_COLUMN);
		emir.add(EMIRReportLogic.MESSAGETYPE_COLUMN);
		emir.add(EMIRReportLogic.ACTION_COLUMN);
		emir.add(EMIRReportLogic.LEIPREFIX_COLUMN);
		emir.add(EMIRReportLogic.LEIVALUE_COLUMN);
		emir.add(EMIRReportLogic.TRADEPARTYPREF1_COLUMN);
		emir.add(EMIRReportLogic.TRADEPARTYVAL1_COLUMN);
		emir.add(EMIRReportLogic.COLLATERALPORTFOLIOCODE_COLUMN);
		emir.add(EMIRReportLogic.COLLATERALPORTFOLIOIND_COLUMN);
		emir.add(EMIRReportLogic.SENDTO_COLUMN);
		emir.add(EMIRReportLogic.TRADEPARTY1REPOBLIGATION_COLUMN);
		emir.add(EMIRReportLogic.OTHERPARTYTYPEID_COLUMN);
		emir.add(EMIRReportLogic.OTHERPARTYID_COLUMN);
		emir.add(EMIRReportLogic.COLLATERALIZED_COLUMN);
		emir.add(EMIRReportLogic.LEVEL_COLUMN);
		emir.add(EMIRReportLogic.INITIALMARGINPOSTED);
		emir.add(EMIRReportLogic.CURROFINITIALMARGINPOSTED);
		emir.add(EMIRReportLogic.INITIALMARGINRECEIVED);
		emir.add(EMIRReportLogic.CURROFINITIALMARGINRECEIVED);
		emir.add(EMIRReportLogic.VARIATIONMARGINPOSTED);
		emir.add(EMIRReportLogic.CURROFVARIATIONMARGINPOSTED);
		emir.add(EMIRReportLogic.VARMARGINRECEIVED);
		emir.add(EMIRReportLogic.CURROFVARMARGINRECEIVED);
		emir.add(EMIRReportLogic.EXCESSCOLLPOSTED);
		emir.add(EMIRReportLogic.CURROFEXCESSCOLLPOSTED);
		emir.add(EMIRReportLogic.EXCESSCOLLRECEIVED);
		emir.add(EMIRReportLogic.CURROFEXCESSCOLLRECEIVED);
		emir.add(EMIRReportLogic.ACTION_TYPE_PARTY_1);
		emir.add(EMIRReportLogic.RESERVEDPARTICIPANTUSE1); // GLCS value

		// old
		emir.add(EMIRReportLogic.COLLATERALPORTFOLIOINDICATOR_OLD_COLUMN);
		emir.add(EMIRReportLogic.COMMENT_COLUMN);
		emir.add(EMIRReportLogic.EXECUTIONAGENTPARTY1PREFIX_COLUMN);
		emir.add(EMIRReportLogic.EXECUTIONAGENTPARTYVALUE1_COLUMN);
		emir.add(EMIRReportLogic.MESSAGEID_COLUMN);
		emir.add(EMIRReportLogic.CURRENCYCOLLATERALVALUE_COLUMN);
		emir.add(EMIRReportLogic.COLLATERALVALUATIONDATETIME_COLUMN);
		emir.add(EMIRReportLogic.COLLATERALREPORTINGDATE_COLUMN);
		emir.add(EMIRReportLogic.VALUEOFTHECOLLATERAL_COLUMN);
		// old - end
		addSubTreeList(treeList, new Vector<String>(), EMIR_PREFIX, emir);

		return treeList;
	}

	public static String getEMIRColumnName(String prefix, String columnId) {
		if ((prefix != null) && (!columnId.startsWith(prefix)))
			return null;
		if ((prefix != null) && (columnId.length() <= prefix.length()))
			return null;
		String realName = prefix != null ? columnId.substring(prefix.length()) : columnId;
		return realName;
	}

	private String getMessage() {
		return COL;
	}

	private String getProduct() {
		return COLLATERAL;
	}

	private String getOldActivity(ReportRow row){
		Object o = row.getProperty("Default");
		if(o instanceof MarginCallEntryDTO){
			MarginCallEntryDTO mcd = (MarginCallEntryDTO) o;
			int mccId = mcd.getCollateralConfigId();
			MarginCallConfig mcc = BOCache.getMarginCallConfig(DSConnection.getDefault(), mccId);
			String aggreementStatus = mcc.getAgreementStatus();
			if (!Util.isEmpty(aggreementStatus)) {
				if (aggreementStatus.equals("CLOSED")) {
					return "CAN";
				}
				if (aggreementStatus.equals("OPEN")) {
					return "NEW";
				}
			}
		}
		return "";
	}

	private String getINITIALMARGINPOSTED(ReportRow row, Vector errors) {
		CollateralConfig config = EMIRReportLogic.getCollateralConfig(row);
		if (isIMColumnNeeded(config,true)) {
			return df.format(0.0D);
		}
		return "";
	}

	private String getINITIALMARGINRECEIVED(ReportRow row, Vector errors) {
		CollateralConfig config = EMIRReportLogic.getCollateralConfig(row);
		if (isIMColumnNeeded(config,false)) {
			return df.format(0.0D);
		}
		return "";
	}

	private String getVARIATIONMARGINPOSTED(ReportRow row, Vector errors) {
		CollateralConfig config = EMIRReportLogic.getCollateralConfig(row);
		if (config != null && EMIRReportLogic.isContractTypeCSA(config)) {
			double requiredMargin = getDoubleValueOfField(GLOBAL_REQUIRED_MRG, row, errors);
			if (requiredMargin < 0) {
				return df.format(Math.abs(getDoubleValueOfField(DAILY_TOTAL_MRG, row, errors)));
			}
		}
		return "";
	}

	private String getVARIATIONMARGINRECEIVED(ReportRow row, Vector errors) {
		CollateralConfig config = EMIRReportLogic.getCollateralConfig(row);
		if (config != null && EMIRReportLogic.isContractTypeCSA(config)) {
			double requiredMargin = getDoubleValueOfField(GLOBAL_REQUIRED_MRG, row, errors);
			if (requiredMargin >= 0) {
				return df.format(Math.abs(getDoubleValueOfField(DAILY_TOTAL_MRG, row, errors)));
			}
		}
		return "";
	}

	private String getContractCurrency(String field, ReportRow row, Vector errors) {
		Object o = getEmirColumnValue(field, row, errors);
		if (o instanceof String) {
			String columnValue = (String) o;
			if (!Util.isEmpty(columnValue)) {
				return (String) marginCallEntryReportStyle.getColumnValue(row, CONTRACT_CURRENCY, errors);
			}
		}
		return "";
	}

	private String getEXCESSCOLLPOSTED(ReportRow row, Vector errors) {
		CollateralConfig config = EMIRReportLogic.getCollateralConfig(row);
		Object o = row.getProperty(EmirUtil.MARGIN_CALL_ENTRY_NAME);
		MarginCallEntry entry = null;
		if (o instanceof MarginCallEntry) {
			entry = (MarginCallEntry) o;
		}

		if (config != null && entry != null) {

			double remainigMrg = entry.getRemainingMargin();
			if (EMIRReportLogic.isContractTypeCSD(config) && EMIRReportLogic.isContractNameIncludePO(config)
					&& remainigMrg > 0) {
				return df.format(remainigMrg);
			} else if (EMIRReportLogic.isContractTypeCSA(config) && remainigMrg > 0) {
				double globalRequiredMrg = getDoubleValueOfField(GLOBAL_REQUIRED_MRG, row, errors);
				if (globalRequiredMrg < 0) {
					return df.format(remainigMrg);
				}
			}
		}
		return "";
	}

	private String getEXCESSCOLLRECEIVED(ReportRow row, Vector errors) {
		CollateralConfig config = EMIRReportLogic.getCollateralConfig(row);
		Object o = row.getProperty(EmirUtil.MARGIN_CALL_ENTRY_NAME);
		MarginCallEntry entry = null;
		if (o instanceof MarginCallEntry) {
			entry = (MarginCallEntry) o;
		}
		if (config != null && entry != null) {
			double remainigMrg = entry.getRemainingMargin();
			if (EMIRReportLogic.isContractTypeCSD(config) && EMIRReportLogic.isContractNameIncludeCPTYorCTPY(config)
					&& remainigMrg < 0) {
				return df.format(Math.abs(remainigMrg));
			} else if (EMIRReportLogic.isContractTypeCSA(config) && remainigMrg < 0) {
				double globalRequiredMrg = getDoubleValueOfField(GLOBAL_REQUIRED_MRG, row, errors);
				if (globalRequiredMrg > 0) {
					return df.format(Math.abs(remainigMrg));
				}
			}
		}
		return "";
	}

	private double getDoubleValueOfField(String field, ReportRow row, Vector errors) {
		Object o = marginCallEntryReportStyle.getColumnValue(row, field, errors);
		if (o instanceof Amount) {
			Amount fieldAmount = (Amount) o;
			return fieldAmount.get();
		}
		return 0.0D;
	}

	/**
	 * Collateral Valuation- IM fields DDR v1.1
	 * */
	private enum IMCollateralDegree{
		FULLY(true,true),
		ONEWAY(true,false),
		PARTIALLY(false,false);

		boolean isIMPostedNeeded;
		boolean isIMReceivedNeeded;

		IMCollateralDegree(boolean imPosted, boolean imReceived){
			isIMPostedNeeded=imPosted;
			isIMReceivedNeeded=imReceived;
		}

		boolean getBooleanValue(boolean isPosted){
			boolean res=isIMReceivedNeeded;
			if (isPosted) {
				res = isIMPostedNeeded;
			}
			return res;
		}
	}

	/**
	 * Collateral Valuation- IM fields DDR v1.1
 	 * @param cConfig
	 * @param isPosted
	 * @return true if column needed
	 */
	private boolean isIMColumnNeeded(CollateralConfig cConfig,boolean isPosted){
		String emirCollValueStr="EMIR_COLLATERAL_VALUE";
		String collDegree= Optional.ofNullable(cConfig).map(cc->cc.getAdditionalField(emirCollValueStr)).orElse("");
		boolean res = false;
		if(!Util.isEmpty(collDegree)) {
			try {
				res = IMCollateralDegree.valueOf(collDegree.toUpperCase()).getBooleanValue(isPosted);
			} catch (IllegalArgumentException | NullPointerException exc) {
				Log.error(this.getClass().getSimpleName(), exc.getMessage());
			}
		}
		return res;
	}

	/**
	 * Collateral Valuation- IM fields DDR v1.1
	 */
	private String getIMContractCurrency(ReportRow row,Vector errors,boolean isPosted) {
		String res="";
		CollateralConfig config = EMIRReportLogic.getCollateralConfig(row);
		if(isIMColumnNeeded(config,isPosted)) {
			res = (String) marginCallEntryReportStyle.getColumnValue(row, CONTRACT_CURRENCY, errors);
		}
		return res;
	}
}
