package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.MarginCallDetailEntryReportStyle;
import com.calypso.tk.report.MarginCallEntryBaseReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import calypsox.regulation.util.EmirUtil;
import calypsox.tk.util.SantTradeKeywordUtil;

public class EMIRLinkingMarginCallDetailEntryReportStyle extends MarginCallDetailEntryReportStyle {

	/** Serial Version UID */
	private static final long serialVersionUID = 1L;
	
	private final MarginCallDetailEntryReportStyle marginCallDetailEntryReportStyle = new MarginCallDetailEntryReportStyle();
	private final CollateralConfigReportStyle collateralConfigReportStyle = new CollateralConfigReportStyle();
	private final TradeReportStyle tradeReportStyle = new TradeReportStyle();
	
	private static final String MCDE_PREFIX = "MarginCallDetailEntry.";
	private static final String COLLATERALCONFIG_PREFIX = "CollateralConfig.";
	private static final String TRADE_PREFIX = "Trade.";
	private static final String CLK = "CLK";
	private static final String COL_LINKING = "ColLinking";
	

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		if (row != null) {
			if (columnName.contains(EMIRValuationMCReportStyle.EMIR_PREFIX)) {
				final String newProductColumnName = getEMIRColumnName(EMIRValuationMCReportStyle.EMIR_PREFIX, columnName);
				if (!Util.isEmpty(newProductColumnName)) {
					return getEmirColumnValue(newProductColumnName, row);
				}
			}else if (columnName.contains(MCDE_PREFIX)) {
				row.setProperty("Default", row.getProperty(EmirUtil.MARGIN_CALL_DETAIL_ENTRY_NAME));
				final String newProductColumnName = MarginCallEntryReportStyle.getColumnName(MCDE_PREFIX, columnName);
				if (!Util.isEmpty(newProductColumnName)) {
					return this.marginCallDetailEntryReportStyle.getColumnValue(row, newProductColumnName, errors);
				} else {
					return this.marginCallDetailEntryReportStyle.getColumnValue(row, columnName, errors);
				}
			}else if(columnName.contains(COLLATERALCONFIG_PREFIX)){
				row.setProperty("Default", row.getProperty(EmirUtil.MARGIN_CALL_ENTRY_NAME));
				final String newProductColumnName = MarginCallEntryBaseReportStyle.getColumnName(COLLATERALCONFIG_PREFIX, columnName);
				if (!Util.isEmpty(newProductColumnName)) {
					return this.collateralConfigReportStyle.getColumnValue(row, newProductColumnName, errors);
				} else {
					return this.collateralConfigReportStyle.getColumnValue(row, columnName, errors);
				}
			}else if(columnName.contains(TRADE_PREFIX)){
				row.setProperty("Default", row.getProperty(EmirUtil.TRADE_NAME));
				final String newProductColumnName = MarginCallEntryBaseReportStyle.getColumnName(TRADE_PREFIX, columnName);
				if (!Util.isEmpty(newProductColumnName)) {
					return this.tradeReportStyle.getColumnValue(row, newProductColumnName, errors);
				} else {
					return this.tradeReportStyle.getColumnValue(row, columnName, errors);
				}
			}
			else{
				row.setProperty("Default", row.getProperty(EmirUtil.MARGIN_CALL_DETAIL_ENTRY_NAME));
				return super.getColumnValue(row, columnName, errors);
			}
		}
		
		return super.getColumnValue(row, columnName, errors);
	}
	
	private Object getEmirColumnValue(String newProductColumnName, ReportRow row){
		 if(newProductColumnName.equals(EMIRReportLogic.SOURCE_SYSTEM)){
			return EMIRReportLogic.getSourceSystem(row);
		} else if (newProductColumnName.equals(EMIRReportLogic.MESSAGE)) {
			return getMessage();
		}else if (newProductColumnName.equals(EMIRReportLogic.ACTIVITY_OLD)) {
			return EMIRReportLogic.getACTIVITY(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.TRANSACTION_TYPE)) {
			return EMIRReportLogic.getTransactionType();
		}else if (newProductColumnName.equals(EMIRReportLogic.PRODUCT)) {
			return getProduct();
		}else if (newProductColumnName.equals(EMIRReportLogic.ACTION_COLUMN)) {
			return EMIRReportLogic.getACTION(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.COLLATERALIZED_COLUMN)) {
			return EMIRReportLogic.getCOLLATERALIZED(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.COLLATERALPORTFOLIOCODE_COLUMN)) {
			return EMIRReportLogic.getCOLLATERALPORTFOLIOCODE(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.COLLATERALPORTFOLIOIND_COLUMN)) {
			return EMIRReportLogic.getCOLLATERALPORTFOLIOIND(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.LEIPREFIX_COLUMN)) {
			return EMIRReportLogic.getLEIPREFIX(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.LEIVALUE_COLUMN)) {
			return EMIRReportLogic.getLEIVALUE(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.LEVEL_COLUMN)) {
			return EMIRReportLogic.getLEVEL(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.MESSAGEID_COLUMN)) {
			return EMIRReportLogic.getMESSAGEID(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.OTHERPARTYID_COLUMN)) {
			return row.getProperty(EMIRReportLogic.OTHERPARTYID_COLUMN);
		}else if (newProductColumnName.equals(EMIRReportLogic.OTHERPARTYTYPEID_COLUMN)) {
			return EMIRReportLogic.getOTHERPARTYTYPEID(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.SENDTO_COLUMN)) {
			return EMIRReportLogic.getSENDTO(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.TRADEPARTY1REPOBLIGATION_COLUMN)) {
			return EMIRReportLogic.getTRADEPARTY1REPOBLIGATION(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.TRADEPARTYPREF1_COLUMN)) {
			return EMIRReportLogic.getTRADEPARTYPREF1(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.TRADEPARTYVAL1_COLUMN)) {
			return row.getProperty(EMIRReportLogic.TRADEPARTYVAL1_COLUMN);
		}else if (newProductColumnName.equals(EMIRReportLogic.VERSION_COLUMN)) {
			return EMIRReportLogic.getVERSION(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.TRADEPARTYTRANSACTIONID1_COLUMN)) {
			return EMIRReportLogic.getTradeKeyword(row, SantTradeKeywordUtil.BO_REFERENCE);
		}else if (newProductColumnName.equals(EMIRReportLogic.USIPREFIX_COLUMN)) {
			return EMIRReportLogic.getPrefixFromKeyword(row, SantTradeKeywordUtil.USI_REFERENCE, true);
		}else if (newProductColumnName.equals(EMIRReportLogic.USIVALUE_COLUMN)) {
			return EMIRReportLogic.getPrefixFromKeyword(row, SantTradeKeywordUtil.USI_REFERENCE, false);
		}else if (newProductColumnName.equals(EMIRReportLogic.UTIVALUE_COLUMN)) {
			return EMIRReportLogic.getPrefixFromKeyword(row, SantTradeKeywordUtil.UTI_REFERENCE, false);
		}else if (newProductColumnName.equals(EMIRReportLogic.UTIPREFIX_COLUMN)) {
			return EMIRReportLogic.getPrefixFromKeyword(row, SantTradeKeywordUtil.UTI_REFERENCE, true);
		}else if (newProductColumnName.equals(EMIRReportLogic.COMMENT_COLUMN)) {
			return EMIRReportLogic.getCOMMENT(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.MESSAGETYPE_COLUMN)) {
			return EMIRReportLogic.getMESSAGETYPE_Link(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.EXECUTIONAGENTPARTY1PREFIX_COLUMN)) {
			return EMIRReportLogic.getEXECUTIONAGENTPARTY1PREFIX(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.EXECUTIONAGENTPARTYVALUE1_COLUMN)) {
			return EMIRReportLogic.getEXECUTIONAGENTPARTYVALUE1(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.ACTIVITY_COLUMN)) {
			return EMIRReportLogic.getACTIVITY(row);
		}else if (newProductColumnName.equals(EMIRReportLogic.ACTION_TYPE_PARTY_1)) {
			return EMIRReportLogic.getACTIONTYPEPARTY1();
		}else if (newProductColumnName.equals(EMIRReportLogic.RESERVEDPARTICIPANTUSE1)) { // GLCS value
			return EMIRReportLogic.getRESERVEDPARTICIPANTUSE1(row);
		}else{
			return "";
		}
	}
	
	@Override
	public TreeList getTreeList() {
		TreeList treeList = new TreeList();
		addSubTreeList(treeList, new Vector<String>(), MCDE_PREFIX, this.marginCallDetailEntryReportStyle.getTreeList());
		addSubTreeList(treeList, new Vector<String>(), COLLATERALCONFIG_PREFIX, this.collateralConfigReportStyle.getTreeList());
		addSubTreeList(treeList, new Vector<String>(), TRADE_PREFIX, this.tradeReportStyle.getTreeList());
		
		TreeList emir = new TreeList();
		
		emir.add(EMIRReportLogic.SOURCE_SYSTEM);
		emir.add(EMIRReportLogic.MESSAGE);
		emir.add(EMIRReportLogic.ACTIVITY_OLD);
		emir.add(EMIRReportLogic.TRANSACTION_TYPE);
		emir.add(EMIRReportLogic.PRODUCT);
		
		emir.add(EMIRReportLogic.VERSION_COLUMN);//1
		emir.add(EMIRReportLogic.MESSAGEID_COLUMN); //2
		emir.add(EMIRReportLogic.ACTION_COLUMN); //3
		emir.add(EMIRReportLogic.LEIPREFIX_COLUMN); //4
		emir.add(EMIRReportLogic.LEIVALUE_COLUMN); //5
		emir.add(EMIRReportLogic.TRADEPARTYPREF1_COLUMN); //6 
		emir.add(EMIRReportLogic.TRADEPARTYVAL1_COLUMN); // 7
		emir.add(EMIRReportLogic.UTIPREFIX_COLUMN); //8
		emir.add(EMIRReportLogic.UTIVALUE_COLUMN); //9
		emir.add(EMIRReportLogic.USIPREFIX_COLUMN); //10
		emir.add(EMIRReportLogic.USIVALUE_COLUMN); //11
		emir.add(EMIRReportLogic.TRADEPARTYTRANSACTIONID1_COLUMN); //12
		emir.add(EMIRReportLogic.COLLATERALPORTFOLIOCODE_COLUMN); //13
		emir.add(EMIRReportLogic.COLLATERALIZED_COLUMN); //14
		emir.add(EMIRReportLogic.SENDTO_COLUMN); //15
		emir.add(EMIRReportLogic.TRADEPARTY1REPOBLIGATION_COLUMN); //16
		emir.add(EMIRReportLogic.OTHERPARTYTYPEID_COLUMN); //17
		emir.add(EMIRReportLogic.OTHERPARTYID_COLUMN); //18
		emir.add(EMIRReportLogic.COLLATERALPORTFOLIOIND_COLUMN);//19
		emir.add(EMIRReportLogic.LEVEL_COLUMN); //20
		emir.add(EMIRReportLogic.ACTION_TYPE_PARTY_1);
		emir.add(EMIRReportLogic.RESERVEDPARTICIPANTUSE1); // GLCS value
		//old report
		emir.add(EMIRReportLogic.COMMENT_COLUMN);
		emir.add(EMIRReportLogic.MESSAGETYPE_COLUMN);
		emir.add(EMIRReportLogic.EXECUTIONAGENTPARTY1PREFIX_COLUMN);
		emir.add(EMIRReportLogic.EXECUTIONAGENTPARTYVALUE1_COLUMN);
		emir.add(EMIRReportLogic.ACTIVITY_COLUMN);
		addSubTreeList(treeList, new Vector<String>(), EMIRValuationMCReportStyle.EMIR_PREFIX, emir);
		
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
		return CLK;
	}
	
	private String getProduct(){
		return COL_LINKING;
	}

}
