package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.CalypsoTreeNode;
import com.calypso.apps.util.TreeList;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.product.Security;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

public class ECMSPledgeTradesAndPositionReportStyle extends ECMSDisponibilidadReportStyle {

	private static final long serialVersionUID = 3180465163986769046L;
	private static TradeReportStyle tradeReportStyle = null;
	private static TransferReportStyle transferReportStyle = null;

	public static final String ECMS_STATUS = ECMSPledgeTradesAndPositionReport.TRANSFER
			+ ECMSPledgeTradesAndPositionReport.POINT + "ECMS_Status";
	public static final String ECMS_THEORETICAL_VALUE = BOPositionReport.TRADE + ECMSPledgeTradesAndPositionReport.POINT
			+ "Theoretical Value";
	public static final String ECMS_PLEDGED_ECB = BOPositionReport.TRADE + ECMSPledgeTradesAndPositionReport.POINT
			+ "Pledged ECB";
	public static final String ECMS_OPERATION_TYPE = BOPositionReport.TRADE + ECMSPledgeTradesAndPositionReport.POINT
			+ "OperationType";
	public static final String ECMS_VALUE_PLEDGED_ECB = BOPositionReport.TRADE + ECMSPledgeTradesAndPositionReport.POINT
			+ "Value Pledged ECB";
	public static final String ECMS_OTHER_PLEDGES = BOPositionReport.TRADE + ECMSPledgeTradesAndPositionReport.POINT
			+ "Other Pledges";
	public static final String ECMS_BENEFICIARY_OTHER_PLEDGES = BOPositionReport.TRADE
			+ ECMSPledgeTradesAndPositionReport.POINT + "Beneficiary Other Pledges";
	public static final String TOTAL_AGENT = "ECMS.Total assets by agent";
	
	private static final String PAY = "PAY";
	private static final String RECEIVE = "RECEIVE";

	@Override
	@SuppressWarnings({ "rawtypes" })
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
		TransferArray transfers = row.getProperty(ECMSPledgeTradesAndPositionReport.TRANSFER_ARRAY);
		BOTransfer firstTransfer = null;
		if (!Util.isEmpty(transfers)) {
			for (BOTransfer boTransfer : transfers) {
				if ((firstTransfer == null && boTransfer.getStatus().equals(Status.S_CANCELED))
						|| ((firstTransfer == null || firstTransfer.getStatus().equals(Status.S_CANCELED))
								&& !boTransfer.getStatus().equals(Status.S_CANCELED))) {
					firstTransfer = boTransfer;
				}
			}
		}
		Trade trade = row.getProperty(BOPositionReport.TRADE);
		JDatetime valDateTime = ReportRow.getValuationDateTime(row);
		JDate valDate = JDate.valueOf(valDateTime);
		
		if (columnId.equals(ECMS_OPERATION_TYPE)) {
			if (trade != null && trade.getProduct() != null) {
				return trade.getProduct().getBuySell(trade) > 0 ? ECMSPledgeTradesAndPositionReport.DESPIGNORACION
						: ECMSPledgeTradesAndPositionReport.PIGNORACION;
			} else {
				return null;
			}
		} else if (columnId.equals(ECMS_THEORETICAL_VALUE)) {
			return getTheoreticalValue(trade, row,  valDate, valDateTime, errors);
		} else if (columnId.equals(ECMS_PLEDGED_ECB)) {
			if(!Util.isEmpty(transfers)) {
				return getPledgedECB(trade,transfers);
			} else {
				return null;
			}
		} else if (columnId.equals(ECMS_VALUE_PLEDGED_ECB)) {
			return getPledgedECBValue(trade,transfers, valDate, valDateTime, errors);
		} else if (columnId.equals(ECMS_STATUS)) {
			return getXferECMSStatus(transfers);
		} else if (TOTAL_AGENT.equals(columnId)) {
			double nominal = row.getProperty(ECMSPledgeTradesAndPositionReport.TOTAL_AGENT);
			Product product = row.getProperty(INV_PRODUCT);
			if (product instanceof Bond) {
				Bond b = (Bond) product;
				double princ = b.getPrincipal(valDate);
				nominal = nominal * princ;
			}
			Object retVal = super.getColumnValue(row, CURRENCY, errors);
			if(retVal instanceof String) {
				return new Amount(nominal,retVal.toString());
			}else {
				return new Amount(nominal);
			}
	    } else if (columnId.startsWith(BOPositionReport.TRADE + ECMSPledgeTradesAndPositionReport.POINT)) {
			ReportRow rowTr = new ReportRow(trade);
			return getTradeReportStyle().getColumnValue(rowTr,
					columnId.replaceFirst(BOPositionReport.TRADE + ECMSPledgeTradesAndPositionReport.POINT, ""),
					errors);
		} else if (columnId
				.startsWith(ECMSPledgeTradesAndPositionReport.TRANSFER + ECMSPledgeTradesAndPositionReport.POINT)) {
			if (firstTransfer != null) {
				ReportRow rowTr = new ReportRow(firstTransfer);
				return getTradeReportStyle().getColumnValue(rowTr,
						columnId.replaceFirst(
								ECMSPledgeTradesAndPositionReport.TRANSFER + ECMSPledgeTradesAndPositionReport.POINT,
								""),
						errors);
			}
			return null;
		} else if (columnId.equals("Dates")) {
			return null;
		}
		Object retVal2 = super.getColumnValue(row, columnId, errors);
		if (retVal2 instanceof Double) {
			Object retVal = super.getColumnValue(row, CURRENCY, errors);
			if(retVal instanceof String) {
				return new Amount((Double)retVal2,retVal.toString());
			}else {
				return new Amount((Double)retVal2);
			}			
		}
		return retVal2;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Amount getTheoreticalValue(Trade trade,ReportRow row, JDate valDate, JDatetime valDateTime, Vector errors) {
		if (trade != null) {
			QuoteValue qv = new QuoteValue();
			if (((MarginCall) trade.getProduct()).getSecurity() instanceof Bond) {
				qv.setQuoteSetName(ECMSPledgeTradesAndPositionReport.QS_DIRTY_PRICE);
				qv.setQuoteType(ECMSPledgeTradesAndPositionReport.QT_DIRTY_PRICE);
			} else if (((MarginCall) trade.getProduct()).getSecurity() instanceof Equity) {
				qv.setQuoteSetName(ECMSPledgeTradesAndPositionReport.QS_OFFICIAL);
				qv.setQuoteType(ECMSPledgeTradesAndPositionReport.QT_PRICE);
			}
			qv.setName(((MarginCall) trade.getProduct()).getSecurity().getQuoteName());
			qv.setDate(valDate);
			qv.setDatetime(valDateTime);
			try {
				qv = DSConnection.getDefault().getRemoteMarketData().getLatestQuoteValue(qv);
			} catch (CalypsoServiceException e) {
				errors.add("Error loading last market ISIN price. " + e.toString());
				Log.error(this, "Error loading last market price for ISIN. ", e);
				return null;
			}
			if (qv != null) {
				double price = qv.getClose();
				LegalEntity legalEntity = LegalEntity
						.valueOf(((Security) ((MarginCall) trade.getProduct()).getSecurity()).getIssuerId());
				double haircut = 0;
				if (legalEntity != null && legalEntity.getCode().equals(trade.getBook().getLegalEntity().getCode())) {
					String ecmsOwenHaircut = ((MarginCall) trade.getProduct()).getSecurity()
							.getSecCode("ECMS_Haircut_Owen_Use");
					haircut = Util.istringToRate(ecmsOwenHaircut);
				} else {
					String ecmsHaircut = ((MarginCall) trade.getProduct()).getSecurity().getSecCode("ECMS_Haircut");
					haircut = Util.istringToRate(ecmsHaircut);
					
				}
				if (((MarginCall) trade.getProduct()).getSecurity() instanceof Bond) {
					double principal = row.getProperty("Principal");
					double quantity = trade.getQuantity();
					return new Amount(quantity * principal * (price - haircut),trade.getTradeCurrency());
					
				} else if (((MarginCall) trade.getProduct()).getSecurity() instanceof Equity) {
					double nominal = trade.computeNominal(valDate);
					return new Amount(nominal * price * (1 - haircut),trade.getTradeCurrency());
				} else {
					return new Amount(0,trade.getTradeCurrency());
				}
				
				
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private String getXferECMSStatus(TransferArray transfers) {
		if (!Util.isEmpty(transfers)) {
			int settled=0;
			int canceled=0;
			int others=0;
			for (BOTransfer boTransfer : transfers) {
				if (boTransfer.getStatus().equals(Status.S_SETTLED)) {
					settled++;
				} else if (boTransfer.getStatus().equals(Status.S_CANCELED)) {
					canceled++;
				} else if(!boTransfer.getStatus().equals(Status.S_SPLIT)){
					others++;
				}
			}
			if (settled>0 && others==0) {
				return ECMSPledgeTradesAndPositionReport.PROCESSED;
			} else if (settled>0 && others>0) {
				return ECMSPledgeTradesAndPositionReport.PART_PROCESSED;
			} else if (settled==0 && others==0 && canceled>0) {
				return ECMSPledgeTradesAndPositionReport.CANCELED;
			} else {
				return ECMSPledgeTradesAndPositionReport.IN_PROGRESS;
			}
		}		
		return null;
	}

	@Override
	public TreeList getTreeList() {
		TreeList treeList = super.getTreeList();
		TreeList tradeList = getTradeReportStyle().getTreeList();
		Vector<String> types = new Vector<>();
		types.add(BOPositionReport.TRADE);
		this.copyTreeList(treeList, tradeList, types, false, (CalypsoTreeNode) null,
				BOPositionReport.TRADE + ECMSPledgeTradesAndPositionReport.POINT);
		types = new Vector<>();
		types.add(ECMSPledgeTradesAndPositionReport.TRANSFER);
		this.copyTreeList(treeList, tradeList, types, false, (CalypsoTreeNode) null,
				ECMSPledgeTradesAndPositionReport.TRANSFER + ECMSPledgeTradesAndPositionReport.POINT);
		return super.getTreeList();
	}

	protected static TradeReportStyle getTradeReportStyle() {
		if (tradeReportStyle == null) {
			tradeReportStyle = getReportStyle(BOPositionReport.TRADE);
		}
		return tradeReportStyle;
	}

	protected static TransferReportStyle getTransferReportStyle() {
		if (transferReportStyle == null) {
			transferReportStyle = getReportStyle(ECMSPledgeTradesAndPositionReport.TRANSFER);
		}
		return transferReportStyle;
	}
	
	private Amount getPledgedECB(Trade trade,TransferArray transfers) {
		Double nominal = 0d;
		for (BOTransfer boTransfer : transfers) {
			if(boTransfer.getStatus().equals(Status.S_SETTLED)) {
				if(boTransfer.getLinkedLongId() >= 0) {
					nominal = nominal + boTransfer.getNominalAmount();
				} else {
					nominal = nominal - boTransfer.getNominalAmount();
				}

			}
		}
		
		return new Amount(nominal,trade.getTradeCurrency());
	}
	
@SuppressWarnings({ "unchecked", "rawtypes" })
private Amount getPledgedECBValue(Trade trade,TransferArray transfers, JDate valDate, JDatetime valDateTime, Vector errors) {
		
		if(trade != null ) {
			Amount pledgedECB = getPledgedECB(trade, transfers);
			if(pledgedECB.equals(new Amount(0,trade.getTradeCurrency()))) {
				return pledgedECB;
			}
			QuoteValue qv = new QuoteValue();
			if (((MarginCall) trade.getProduct()).getSecurity() instanceof Bond) {
				qv.setQuoteSetName(ECMSPledgeTradesAndPositionReport.QS_DIRTY_PRICE);
				qv.setQuoteType(ECMSPledgeTradesAndPositionReport.QT_DIRTY_PRICE);
			} else if (((MarginCall) trade.getProduct()).getSecurity() instanceof Equity) {
				qv.setQuoteSetName(ECMSPledgeTradesAndPositionReport.QS_OFFICIAL);
				qv.setQuoteType(ECMSPledgeTradesAndPositionReport.QT_PRICE);
			}
			qv.setName(((MarginCall) trade.getProduct()).getSecurity().getQuoteName());
			qv.setDate(valDate);
			qv.setDatetime(valDateTime);
			try {
				qv = DSConnection.getDefault().getRemoteMarketData().getLatestQuoteValue(qv);
			} catch (CalypsoServiceException e) {
				errors.add("Error loading last market ISIN price. " + e.toString());
				Log.error(this, "Error loading last market price for ISIN. ", e);
				return null;
			}
			if (qv != null) {
				double price = qv.getClose();
				LegalEntity legalEntity = LegalEntity
						.valueOf(((Security) ((MarginCall) trade.getProduct()).getSecurity()).getIssuerId());
				double haircut = 0;
				if (legalEntity != null && legalEntity.getCode().equals(trade.getBook().getLegalEntity().getCode())) {
					String ecmsOwenHaircut = ((MarginCall) trade.getProduct()).getSecurity()
							.getSecCode("ECMS_Haircut_Owen_Use");
					haircut = Util.istringToRate(ecmsOwenHaircut);
				} else {
					String ecmsHaircut = ((MarginCall) trade.getProduct()).getSecurity().getSecCode("ECMS_Haircut");
					haircut = Util.istringToRate(ecmsHaircut);
					
				}
				if (((MarginCall) trade.getProduct()).getSecurity() instanceof Bond) {
					return new Amount(pledgedECB.get() * (price - haircut),trade.getTradeCurrency());
					
				} else if (((MarginCall) trade.getProduct()).getSecurity() instanceof Equity) {
					return new Amount(pledgedECB.get() * price * (1 - haircut),trade.getTradeCurrency());
				} else {
					return new Amount(0,trade.getTradeCurrency());
				}
				
			} else {
				return null;
			}
		} else {
			return null;
		}
		
	}
}
