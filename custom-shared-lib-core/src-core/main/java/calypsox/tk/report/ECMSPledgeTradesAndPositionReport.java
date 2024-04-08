package calypsox.tk.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;

import calypsox.util.binding.CustomBindVariablesUtil;

public class ECMSPledgeTradesAndPositionReport extends ECMSDisponibilidadReport {
	private static final String ECMS_POS_REPORT_TRADE_STATUS = "ECMS_POS_REPORT_TRADE_STATUS";
	private static final long serialVersionUID = 4049405665681396328L;
	private static final String ERROR = "ERROR";
	private static final String EXPORTED = "EXPORTED";
	private static final String PROCESSING_ORG = "ProcessingOrg";
	private static final String INV_POS_BOOKLIST = "INV_POS_BOOKLIST";
	private static final String AGENT_ID = "AGENT_ID";
	private static final String STRING_SEP = ",";
	private static final String ACCOUNT_ID = "ACCOUNT_ID";
	private static final String ACCOUNT = "ACCOUNT";
	private static final int MAX_NUMBER_OF_SECCODES = 10;
	private static final String SEC_CODE_VALUE = "SEC_CODE_VALUE";
	private static final String PAY = "PAY";
	private static final String RECEIVE = "RECEIVE";
	private static final String ISIN = "ISIN";
	public static final String TOTAL_AGENT = "TOTAL_AGENT";
	private static final String TOTAL_ASSETS = "TOTAL_ASSETS";
	public static final String QT_PRICE = "Price";
	public static final String QS_OFFICIAL = "OFFICIAL";
	public static final String QS_DIRTY_PRICE = "DirtyPrice";
	public static final String QT_DIRTY_PRICE = "DirtyPrice";
	public static final String PIGNORACION = "Pignoracion";
	public static final String DESPIGNORACION = "Despignoracion";

	public static final String TRANSFER_ARRAY = "TransferArray";
	public static final String Y_VALUE = "Y";
	public static final String IS_ECMS_PLEDGE = "isECMSPledge";
	
	// ECMS Statuses
	public static final String PROCESSED = "Processed";
	public static final String PART_PROCESSED = "Partial Processed";	
	public static final String CANCELED = "Canceled";
	public static final String IN_PROGRESS = "In Progress";
	public static final String TRANSFER = "Transfer";
	public static final String POINT = ".";
	private final String OTHER_PLEDGE_DOMAIN_VALUE_ACCOUNT = "ECMS_OTHER_PLEDGE_ACCOUNTS";
	private final String ECMS_PLEDGE_DOMAIN_VALUE_ACCOUNT = "ECMS_PLEDGE_ACCOUNTS";
	private final String DV_SEPARATOR = ";";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ReportOutput load(Vector errorMsgs) {
		DefaultReportOutput dro = new DefaultReportOutput(this);
		String secCodes = this.getReportTemplate().get(SEC_CODE_VALUE);
		if (!Util.isEmpty(secCodes) && Util.stringToList(secCodes).size() <= MAX_NUMBER_OF_SECCODES) {
			setAccountIdsByName();
			ReportOutput out = super.load(errorMsgs);
			ArrayList<String> accountsFinal = new ArrayList<>();
			accountsFinal = getEcmsAccounts();
			
			ArrayList<ReportRow> arr = new ArrayList<>();
			Map<String, Double> totalAgent = new HashMap<>();
			if (out instanceof DefaultReportOutput) {
				List<String> lstStatus = LocalCache.getDomainValues(DSConnection.getDefault(), ECMS_POS_REPORT_TRADE_STATUS);				
				if(Util.isEmpty(lstStatus)) {
					lstStatus = new ArrayList<>();
					lstStatus.add(Status.CANCELED);
					lstStatus.add(Status.VERIFIED);
					lstStatus.add(EXPORTED);
					lstStatus.add(ERROR);
				}
				ReportRow[] invRows = ((DefaultReportOutput) out).getRows();
				ReportRow[] invRowsFull = invRows;
				if (!Util.isEmpty((String) this.getReportTemplate().get(AGENT_ID))
						|| !Util.isEmpty((String) this.getReportTemplate().get(INV_POS_BOOKLIST))
						|| !Util.isEmpty((String) this.getReportTemplate().get(ACCOUNT))
						|| !Util.isEmpty((String) this.getReportTemplate().get(PROCESSING_ORG))) {
					try {

						ECMSDisponibilidadReport auxRep = new ECMSDisponibilidadReport();
						ReportTemplate template = (ReportTemplate) this.getReportTemplate().clone();
						template.put(AGENT_ID,"");
						template.put(INV_POS_BOOKLIST,"");
						template.remove(ACCOUNT);
						template.put(ACCOUNT_ID,"");
						template.put(PROCESSING_ORG,"");
						auxRep.setReportTemplate(template);
						ReportOutput outPut = auxRep.load(errorMsgs);
						if (outPut instanceof DefaultReportOutput) {
							invRowsFull = ((DefaultReportOutput) outPut).getRows();
						}
					} catch (CloneNotSupportedException e) {
						Log.error(this,
								"Error getting full report rows to calculate total amounts. Calculating with filtered information. ",
								e);
					}
				}
				HashMap<String, ReportRow> rowsFull = new HashMap<String, ReportRow>();
				for (ReportRow reportRow : invRowsFull) {
					Inventory inventory = reportRow.getProperty(ReportRow.INVENTORY);
					String isin = inventory.getProduct()!=null?inventory.getProduct().getSecCode(ISIN):"";		
					String po = (inventory.getBook()!=null && inventory.getBook().getLegalEntity()!=null)?inventory.getBook().getLegalEntity().getCode():"";
					String agent = inventory.getAgent()!=null?inventory.getAgent().getCode():"";
					String acc = inventory.getAccount()!=null?inventory.getAccount().getName():"";
					String book = inventory.getBook()!=null?inventory.getBook().getName():"";
					if(!accountsFinal.contains(acc)) {
						if (totalAgent.containsKey(isin + po + agent)) {
							double assets = reportRow.getProperty(TOTAL_ASSETS);
							double total = assets + totalAgent.get(isin + po + agent);
							totalAgent.put(isin + po + agent, total);
						} else {
							totalAgent.put(isin + po + agent, reportRow.getProperty(TOTAL_ASSETS));
						}
					}
					
					rowsFull.put(isin + po + book + agent + acc, reportRow);

				}


				ArrayList<String> filterAccounts = new ArrayList<>();
				Vector<String> pledgeAccounts = LocalCache.getDomainValues(DSConnection.getDefault(), ECMS_PLEDGE_DOMAIN_VALUE_ACCOUNT);
				for(String account: pledgeAccounts){
			           
					filterAccounts.add(account.split(DV_SEPARATOR)[1]);  
		            
		        }

				for (ReportRow reportRow : invRows) {
					Inventory inventory = reportRow.getProperty(ReportRow.INVENTORY);
					
					String isin = inventory.getProduct()!=null?inventory.getProduct().getSecCode(ISIN):"";		
					String po = (inventory.getBook()!=null && inventory.getBook().getLegalEntity()!=null)?inventory.getBook().getLegalEntity().getCode():"";
					String agent = inventory.getAgent()!=null?inventory.getAgent().getCode():"";
					String acc = inventory.getAccount()!=null?inventory.getAccount().getName():"";
					String book = inventory.getBook()!=null?inventory.getBook().getName():"";
					ReportRow reportRowFull = rowsFull.get(isin + po + book + agent + acc);
					reportRow.getProperties().putAll(reportRowFull.getProperties());
					//Include Trade rows if the account is not a ECMS account
					if(!filterAccounts.contains(acc)) {
						arr.addAll(getTradeRows(reportRow, lstStatus, filterAccounts));
					} else {
						arr.add(reportRow);
					}
				}
				for (ReportRow reportRow : arr) {
					Inventory inventory = reportRow.getProperty(ReportRow.INVENTORY);
					String isin = inventory.getProduct()!=null?inventory.getProduct().getSecCode(ISIN):"";	
					String po = (inventory.getBook()!=null && inventory.getBook().getLegalEntity()!=null)?inventory.getBook().getLegalEntity().getCode():"";
					String agent = inventory.getAgent()!=null?inventory.getAgent().getCode():"";
					if (!totalAgent.containsKey(isin + po + agent)) {
						totalAgent.put(isin + po + agent, reportRow.getProperty(TOTAL_ASSETS));
					}
					reportRow.setProperty(TOTAL_AGENT, totalAgent.get(isin + po + agent));
				}
				ReportRow[] rowsAdjusted = new ReportRow[arr.size()];
				rowsAdjusted = arr.toArray(rowsAdjusted);
				dro.setRows(rowsAdjusted);
			}
		} else {
			Log.error(this,
					"Maximum number of security codes has been reached. Please modify the filters and relaunch the request.");
			errorMsgs.add(
					"Maximum number of security codes has been reached. Please modify the filters and relaunch the request.");
			return null;
		}
		return dro;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void getAdditionalColumns(Map columnMetaData) {
	}

	private void setAccountIdsByName() {
		String accNames = this.getReportTemplate().get(ACCOUNT);
		List<String> accList = Util.stringToList(accNames);
		StringBuilder lstIdAcc = new StringBuilder();
		for (String acc : accList) {
			try {
				@SuppressWarnings("unchecked")
				List<Account> accList2 = this.getDSConnection().getRemoteAccounting().getAccountsByName(acc);
				for (Account accObj : accList2) {
					if (lstIdAcc.length() > 0) {
						lstIdAcc.append(STRING_SEP);
					}
					lstIdAcc.append(accObj.getId());
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, "Error getting Account Id from account name.", e);
			}
		}
		if (lstIdAcc.length() > 0) {
			this.getReportTemplate().put(ACCOUNT_ID, lstIdAcc.toString());
		}else if(!Util.isEmpty(accNames)){
			this.getReportTemplate().put(ACCOUNT_ID, "-1");
		}
	}

	private List<ReportRow> getTradeRows(ReportRow reportRow,List<String> lstStatus, ArrayList<String> filterAccounts) {
		List<ReportRow> retRows = new ArrayList<>();
		TradeArray tarr = getPledgedTradesForInventory(reportRow, lstStatus);
		if (tarr != null && !tarr.isEmpty()) {
			for (Trade trade : tarr.getTrades()) {
				ReportRow row = reportRow.clone();
				row.setProperty(TRADE, trade);
				TransferArray ta = getECMSTransfersForTrade(trade, filterAccounts);
				row.setProperty(TRANSFER_ARRAY, ta);
				retRows.add(row);
			}
		} else {
			retRows.add(reportRow);
		}
		return retRows;
	}

	private TransferArray getECMSTransfersForTrade(Trade trade, ArrayList<String> filterAccounts) {
		try {
			TransferArray ta = this.getDSConnection().getRemoteBackOffice().getBOTransfers(trade.getLongId(),true);
			TransferArray resul = new TransferArray();
			for (BOTransfer boTransfer : ta) {
				Account acc = BOCache.getAccount(DSConnection.getDefault(), boTransfer.getGLAccountNumber());
				if (filterAccounts != null && filterAccounts.contains(acc.getName())) {
					resul.add(boTransfer);
				}
			}
			return resul;
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error getting pledged trades.", e);
			return null;
		}

	}

	private TradeArray getPledgedTradesForInventory(ReportRow reportRow,List<String> lstStatus) {
		StringBuilder sbWhere = new StringBuilder();
		Inventory inventory = reportRow.getProperty(ReportRow.INVENTORY);
		List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(Product.MARGINCALL);
		sbWhere.append("TRADE.TRADE_ID=BO_TRANSFER.TRADE_ID AND TRADE.PRODUCT_ID=PRODUCT_DESC.PRODUCT_ID AND ");
		sbWhere.append("TRADE.TRADE_ID=TRADE_KEYWORD.TRADE_ID AND PRODUCT_DESC.PRODUCT_TYPE=? AND ");
		sbWhere.append("TRADE.TRADE_STATUS IN (");
		sbWhere.append(CustomBindVariablesUtil.collectionToPreparedInString(lstStatus, bindVariables));
		sbWhere.append(") AND TRADE_KEYWORD.KEYWORD_NAME=? AND ");
		sbWhere.append("TRADE_KEYWORD.KEYWORD_VALUE=? AND ? IN(BO_TRANSFER.EXT_AGENT_LE_ID,INT_AGENT_LE_ID) AND ");
		sbWhere.append("BO_TRANSFER.BOOK_ID=? AND BO_TRANSFER.PRODUCT_ID=? AND BO_TRANSFER.GL_ACCOUNT_ID =? ");
		CustomBindVariablesUtil.addNewBindVariableToList(IS_ECMS_PLEDGE, bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(Y_VALUE, bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(inventory.getAgentId(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(inventory.getBookId(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(inventory.getProduct().getId(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(inventory.getAccountId(), bindVariables);

		try {
			return this.getDSConnection().getRemoteTrade().getTrades("TRADE, BO_TRANSFER, PRODUCT_DESC, TRADE_KEYWORD",
					sbWhere.toString(), null, bindVariables);
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error getting pledged trades.", e);
			return null;
		}
	}
	
	private ArrayList<String> getEcmsAccounts() {
		ArrayList<String> accountsFinal = new ArrayList<>();
		Vector<String> pledgeAccounts = LocalCache.getDomainValues(DSConnection.getDefault(), ECMS_PLEDGE_DOMAIN_VALUE_ACCOUNT);
		Vector<String> other_pledges = LocalCache.getDomainValues(DSConnection.getDefault(), OTHER_PLEDGE_DOMAIN_VALUE_ACCOUNT);
		
		for(String account: pledgeAccounts){
           
            accountsFinal.add(account.split(DV_SEPARATOR)[1]);  
        }
		for(String account: other_pledges){
	           
            accountsFinal.add(account.split(DV_SEPARATOR)[1]);  
        }

		return accountsFinal;
	}
}
