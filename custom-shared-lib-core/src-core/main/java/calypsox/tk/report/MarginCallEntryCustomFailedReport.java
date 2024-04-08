package calypsox.tk.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;

import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;

import calypsox.util.binding.CustomBindVariablesUtil;



public class MarginCallEntryCustomFailedReport extends MarginCallEntryReport{


	private static final long serialVersionUID = 3091855985584204006L;
	
	public static final String SECURITY     = "SECURITY";
	public static final String SDFILTER     = "LIQUIDACIONES_TRANSFER_REPORT_BIS";
	public static final String DV_MC_STATUS = "SwapOne.MC_STATUS";
	
	@SuppressWarnings({ "rawtypes" })
	@Override
	public ReportOutput load(Vector errorMsgs) {
		DefaultReportOutput dro = new DefaultReportOutput(this);
		ReportOutput out = super.load(errorMsgs);
		Map<String, String> marginCallStatus = getMarginCallStatusRow();
		
		ReportRow[] invRows = ((DefaultReportOutput) out).getRows();
		ArrayList<ReportRow> arr = new ArrayList<>();
		
		if (out instanceof DefaultReportOutput) {
					
			for (ReportRow reportRow : invRows) {
				MarginCallEntryDTO entry = (MarginCallEntryDTO)reportRow.getProperty("Default");
				CollateralConfig config = this.getMarginCallConfig(entry);
				int contractId = entry.getCollateralConfigId();
				reportRow = getTransferRows(reportRow,contractId);
				reportRow.setProperty("MarginCallStatus", marginCallStatus);
				reportRow.setProperty("MarginCallConfig", config);
				
				arr.add(reportRow);
			}
			ReportRow[] rowsAdjusted = new ReportRow[arr.size()];
			rowsAdjusted = arr.toArray(rowsAdjusted);
			dro.setRows(rowsAdjusted);
		}
		
		return dro;
	}
	
	
	private ReportRow getTransferRows(ReportRow reportRow, int contractId) {
		
		TradeArray tarr = getTradesForCollateral(contractId);
		TransferArray ta;
		ReportRow row = reportRow.clone();
		int numberOfShapes = 0;
		double pendingNominal = 0;
		double pendingValue = 0;
		JDate firstTransferDate = JDate.getNow();
		StaticDataFilter sdFilter = BOCache.getStaticDataFilter(this.getDSConnection(), SDFILTER);

		if (tarr != null && !tarr.isEmpty()) {
			for (Trade trade : tarr.getTrades()) {
				try {
					ta = this.getDSConnection().getRemoteBackOffice().getBOTransfers(trade.getLongId(), true);
				} catch (CalypsoServiceException e) {
					Log.error(this, "Error getting failed transfers.", e);
					return reportRow;
				}
				if (ta != null && !ta.isEmpty()) {
					// Recorre las transfers del array para pasarlo por el SDFilter
					for (BOTransfer transfer : ta.getTransfers()) {
						if (Boolean.TRUE.equals(checkIfTransferFailed(trade, transfer, sdFilter))) {

							numberOfShapes++;
							pendingNominal += transfer.getNominalAmount();
							pendingValue += trade.getTradePrice() * transfer.getNominalAmount();
							if (transfer.getValueDate().before(firstTransferDate)) {
								firstTransferDate = transfer.getValueDate();
							}

						}

					}
				}

			}
			if (numberOfShapes != 0) {
				row.setProperty("FirstTranferDate", firstTransferDate);
				row.setProperty("PendingNominal", pendingNominal);
				row.setProperty("PendingValue", pendingValue);
				row.setProperty("NumberOfShapes", numberOfShapes);
			}

		} else {
			return reportRow;
		}
		return row;
	}

	private TradeArray getTradesForCollateral(int contractId) {

		StringBuilder sbWhere = new StringBuilder();
		List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(contractId);
		sbWhere.append("TRADE.PRODUCT_ID=PRODUCT_SIMPLEXFER.PRODUCT_ID  AND PRODUCT_SIMPLEXFER.LINKED_ID = ?  ");
		sbWhere.append("AND PRODUCT_SIMPLEXFER.PRODUCT_ID =PRODUCT_DESC.PRODUCT_ID AND TRADE.PRODUCT_ID =PRODUCT_DESC.PRODUCT_ID ");
		sbWhere.append("AND TRADE.TRADE_ID = BO_TRANSFER.TRADE_ID AND BO_TRANSFER.VALUE_DATE <= ? " );
		sbWhere.append("AND BO_TRANSFER.TRANSFER_STATUS IN ('VERIFIED','FAILED') ");
		sbWhere.append("AND TRANSFER_TYPE = 'SECURITY' ");
		sbWhere.append("AND PRODUCT_DESC.PRODUCT_TYPE = 'MarginCall'");
		
		CustomBindVariablesUtil.addNewBindVariableToList(JDate.getNow(), bindVariables);

		try {
			return this.getDSConnection().getRemoteTrade().getTrades("TRADE,PRODUCT_SIMPLEXFER, BO_TRANSFER, PRODUCT_DESC",
					sbWhere.toString(), null, bindVariables);
		} catch (CalypsoServiceException e) {
			Log.system("MarginCallEntryCustomFailed", "Error getting collateral trades.", e);
			return TradeArray.EMPTY_TRADE_ARRAY;
		}
	}
	
	private Boolean checkIfTransferFailed(Trade trade, BOTransfer transfer, StaticDataFilter sdFilter) {
		return (transfer.getStatus().equals(Status.S_FAILED) || transfer.getStatus().equals(Status.S_VERIFIED)
				&& (transfer.getValueDate().before(JDate.getNow()) || transfer.getValueDate().equals(JDate.getNow()))  && transfer.getTransferType().equals(SECURITY) 
				&& sdFilter.accept(trade, transfer));

	}
	
	private Map<String, String>  getMarginCallStatusRow() {
		
		return  DomainValues.valuesComment(DV_MC_STATUS);
		
	}
	
	private CollateralConfig getMarginCallConfig(MarginCallEntryDTO entry) {
		return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), entry.getCollateralConfigId());
	}
	
	

	
}
