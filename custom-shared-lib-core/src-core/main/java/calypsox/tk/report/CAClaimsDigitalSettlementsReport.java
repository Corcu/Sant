package calypsox.tk.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.FilterSet;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventorySecurityPositionArray;

import calypsox.tk.report.restservices.WebServiceReport;
import calypsox.util.binding.CustomBindVariablesUtil;

/**
 * 
 * @author x957355
 *
 */
public class CAClaimsDigitalSettlementsReport extends TransferReport implements WebServiceReport {
	private static final String CPTY_NAME = "CptyName";
	private static final String PO_NAME = "PoName";
	private static final long serialVersionUID = 2397244269468697231L;
	private static final String STRING_SEP = ",";
	private boolean countOnly = false;
	public static final String PROPERTY_POSITION = "PROPERTY_POSITION";
	private static String injectionQuery = null;

	@SuppressWarnings({ "rawtypes" })
	@Override
	public ReportOutput load(Vector errorMsgs) {
		DSConnection dsConn = getDSConnection();
		setCptyIdsByName();
		setPOIdsByFullName();
		DefaultReportOutput dro = (DefaultReportOutput) loadReport(errorMsgs);

		if (!countOnly) {
			ReportRow[] rows = dro.getRows();
			for (int i = 0; i < rows.length; i++) {
				ReportRow row = rows[i];
				row = setRowData(row, dsConn);
				rows[i] = row;
			}
			dro.setRows(rows);
		}

		return dro;

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ReportOutput loadReport(Vector errorMsgs) {
//		injectionQuery = "bo_transfer.transfer_id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY";
//		injectionQuery = null;
		if (!this.countOnly && !Util.isEmpty(injectionQuery)) {
			Hashtable<String, String> fromV = new Hashtable<>();
			List<CalypsoBindVariable> bindVariables = new ArrayList<>();
			String where = null;
			try {
				boolean includeArchived = includeArchived();
				where = this.buildQuery(fromV, includeArchived, bindVariables);
				if (this._reportTemplate != null) {
					String s = this._reportTemplate.<String>get("FilterSet");
					if (s != null && s.trim().length() > 0) {
						FilterSet fs = getDSConnection().getRemoteMarketData().getFilterSet(s);
						if (fs != null) {
							where = buildFSQuery(fs, where, bindVariables, includeArchived);
							
						}
					}
				}
				if(where.contains("product_desc.und_security_id") && !fromV.contains("product_desc")) {
					fromV.put("product_desc", "product_desc");
					where += " and product_desc.product_id=bo_transfer.product_id ";
				}
				String from = getFrom(fromV);
				
				WSTransferReportSelector selector = new WSTransferReportSelector(from, where, injectionQuery,
						includeArchived,
						bindVariables);
				((TransferReportTemplate) this.getReportTemplate()).setTransferSelector(selector);
			} catch (Exception e) {
				Log.error(this, e);
				String msg = Util.getRootCause(e).getMessage();
				if (Util.isEmpty(msg)) {
					errorMsgs.add(e.getMessage());
				} else {
					errorMsgs.add(msg);
				}
				return null;
			}
		}
		ReportOutput out = super.load(errorMsgs);
		if (!this.countOnly && !Util.isEmpty(injectionQuery)) {
			((TransferReportTemplate) this.getReportTemplate()).setTransferSelector(null);
		}
		return out;
	}

	@Override
	public ReportOutput loadFromWS(String query, Vector<String> errorMsgs) {
		injectionQuery = query;
		ReportOutput out = load(errorMsgs);
		injectionQuery = null;
		return out;
	}

	public String buildFSQuery(FilterSet fs, String inputWhere, List<CalypsoBindVariable> whereBindVariables,
			boolean isArchive) throws Exception {
		String where = inputWhere;
		List<CalypsoBindVariable> fsWhereBindVariables = new ArrayList<>();
		String fswhere = null;
		String fsfrom = null;

		JDate valDate = getValDate();

		fsfrom = isArchive ? fs.getArchiveSQLFrom(valDate) : fs.getSQLFrom(valDate);

		fswhere = isArchive ? fs.getArchiveSQLWhere(valDate, fsWhereBindVariables)
				: fs.getSQLWhere(valDate, fsWhereBindVariables);
		if (fswhere != null) {
			if (fswhere.indexOf("product_desc") >= 0) {
				Boolean b = this._reportTemplate.<Boolean>get("ApplyToTrade");
				if (b != null && b.booleanValue()) {
					String tradeTable = isArchive ? "trade_hist" : "trade";
					String productTable = isArchive ? "product_desc_hist" : "product_desc";
					fswhere = fswhere + " AND " + tradeTable + ".product_id = " + productTable + ".product_id";

				}
			}
			if (fswhere.indexOf(".trade_id") >= 0) {
				String xferTable = isArchive ? "bo_transfer_hist" : "bo_transfer";
				String tradeTable = isArchive ? "trade_hist" : "trade";
				if (where != null && where.length() > 0) {
					where = where + " AND " + xferTable + ".trade_id = " + tradeTable + ".trade_id";

				} else {
					where = xferTable + ".trade_id = " + tradeTable + ".trade_id";

				}
			}
			if (!Util.isEmpty(fsfrom) || !Util.isEmpty(fswhere)) {

				String fsw = " (bo_transfer.trade_id IN (SELECT trade.trade_id FROM "
						+ (isArchive ? "book,product_desc_hist,trade_hist" : "book,product_desc,trade");

				if (fsfrom != null)
					fsw = fsw + "," + fsfrom;

				fsw = fsw + " WHERE  book.book_id=trade.book_id  AND product_desc.product_id=trade.product_id ";

				if (fswhere != null)
					fsw = fsw + " AND " + fswhere;

				fsw = fsw + ")";
				if (!Util.isEmpty(where)) {
					where = where + " AND ";
				} else {
					where = "";
				}
				if (isArchive) {
					fsw = Util.replaceString(fsw, "trade.", "trade_hist.");
					fsw = Util.replaceString(fsw, "bo_message.", "bo_message_hist.");
					fsw = Util.replaceString(fsw, "bo_transfer.", "bo_transfer_hist.");
					fsw = Util.replaceString(fsw, "product_desc.", "product_desc_hist.");
					fsw = Util.replaceString(fsw, "trade tradeA", "trade_hist tradeA");
				}
				where = where + fsw;
				where = where + ")";
				whereBindVariables.addAll(fsWhereBindVariables);
			}
		}
		return where;
	}

	/**
	 * Set Custom row properties
	 * 
	 * @param row    ReportRow
	 * @param dsConn DSCOnnection object
	 * @return Added properties row
	 */
	public ReportRow setRowData(ReportRow row, DSConnection dsConn) {
		BOTransfer transfer = row.getProperty(ReportRow.TRANSFER);
		if (transfer == null) {
			transfer = row.getProperty(ReportRow.DEFAULT);
		}
		if (transfer != null) {
			Trade trade = row.getProperty(ReportRow.TRADE);
			JDatetime valDateTime = row.getProperty(ReportRow.VALUATION_DATETIME);
			if (valDateTime == null)
				valDateTime = new JDatetime();
			JDate valDate = (trade != null && trade.getBook() != null) ? trade.getBook().getJDate(valDateTime)
					: valDateTime.getJDate(TimeZone.getDefault());

			Double positionDbl = getPosition(transfer, valDate, dsConn);
			// Save the position row property
			row.setProperty(PROPERTY_POSITION, positionDbl);
		}
		return row;
	}

	/**
	 * Get the inventory postion from the transfer
	 * 
	 * @param transfer BoTransfer Object
	 * @param valDate  Valuation date
	 * @param dsConn   DSConnection object
	 * @return
	 */
	public static InventorySecurityPositionArray getInvPositionsArray(BOTransfer transfer, JDate valDate,
			DSConnection dsConn) {
		int pId = transfer.getProductId();
		StringBuilder where = new StringBuilder();
		StringBuilder from = new StringBuilder();
		where.append(" inv_sec_balance.config_id=?");
		List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(0);
		where.append(" and inv_sec_balance.security_id=?");
		CustomBindVariablesUtil.addNewBindVariableToList(pId, bindVariables);
		where.append(" and inv_sec_balance.INTERNAL_EXTERNAL=?");
		CustomBindVariablesUtil.addNewBindVariableToList(BOPositionReport.INTERNAL.toUpperCase(), bindVariables);
		where.append(" and inv_sec_balance.POSITION_TYPE=?");
		CustomBindVariablesUtil.addNewBindVariableToList(BOPositionReport.ACTUAL.toUpperCase(), bindVariables);
		where.append(" and inv_sec_balance.DATE_TYPE=?");
		CustomBindVariablesUtil.addNewBindVariableToList(BOPositionReport.SETTLE.toUpperCase(), bindVariables);
		where.append(" ");

		try {
			return dsConn.getRemoteInventory().getSecurityPositionsFromTo(from.toString(), where.toString(), valDate,
					valDate, bindVariables);
		} catch (CalypsoServiceException e) {
			Log.error(CAClaimsDigitalSettlementsReport.class, "Error getting security position from Inventory.", e);
		}
		return null;
	}

	public static Double getPosition(BOTransfer transfer, JDate valDate, DSConnection dsConn) {
		Double result = 0.0;
		InventorySecurityPositionArray secPositions = getInvPositionsArray(transfer, valDate, dsConn);
		if (secPositions != null && !secPositions.isEmpty()) {

			for (InventorySecurityPosition inventorySecurityPosition : secPositions) {
				double princ = inventorySecurityPosition.getProduct().getPrincipal(valDate);
				result += inventorySecurityPosition.getTotalSecurity() * princ;
			}
			return result;
		}
		return null;
	}

	/**
	 *
	 */
	@Override
	public Map<?, ?> getPotentialSize() {
		try {
			this._potentialSize = new HashMap<>();
			this.countOnly = true;
			super.getPotentialSize();
		} finally {
			this.countOnly = false;

		}
		return this._potentialSize;
	}

	private void setCptyIdsByName() {
		String cptyNames = this.getReportTemplate().get(CPTY_NAME);
		List<String> cptyList = Util.stringToList(cptyNames);
		StringBuilder lstIdAcc = new StringBuilder();
		for (String s : cptyList) {
			LegalEntity le = BOCache.getLegalEntity(getDSConnection(), s);
			if (lstIdAcc.length() > 0) {
				lstIdAcc.append(STRING_SEP);
			}
			if (le != null) {
				lstIdAcc.append(le.getId());
			} else if (Util.isNumber(s)) {
				lstIdAcc.append(s);
			}
		}
		if (lstIdAcc.length() > 0) {
			this.getReportTemplate().put(CPTY_NAME, lstIdAcc.toString());
		} else if (!Util.isEmpty(cptyNames)) {
			this.getReportTemplate().put(CPTY_NAME, "-1");
		}
	}
	
	private void setPOIdsByFullName() {
		
		String fullName = this.getReportTemplate().get("BANK_NAME");
		if(fullName != null && !fullName.isEmpty()) {
			String where = "LONG_NAME = ? ";
			
			List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(fullName);
			try {
				//Search LE by LONG Name
				Vector<LegalEntity> le = DSConnection.getDefault().getRemoteReferenceData().getAllLE(where, bindVariables);
				StringBuilder lstIdAcc = new StringBuilder();
				for (LegalEntity entity: le) {
					if (lstIdAcc.length() > 0) {
						lstIdAcc.append(STRING_SEP);
					}
					if(entity != null) {
						lstIdAcc.append(entity.getCode());
					}

				}
				this.getReportTemplate().put(PO_NAME, lstIdAcc.toString());
			} catch (CalypsoServiceException e) {
				Log.error(this,"Error getting the Legal Entity", e);
			} catch(Exception e) {
				Log.error(this, e);
			}
		}
		

			
	}
}
