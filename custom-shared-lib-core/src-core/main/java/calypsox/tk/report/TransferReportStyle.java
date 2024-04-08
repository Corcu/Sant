/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.security.InvalidParameterException;
import java.util.*;

import static calypsox.tk.report.TransferReportTemplate.*;

public class TransferReportStyle extends com.calypso.tk.report.TransferReportStyle {

	private static final long serialVersionUID = 1L;

	public static final String PROCESS_DATE = "PROCESSDATE";
	public static final String ACCOUNTING_CENTER = "ACCOUNTING_CENTER";
	public static final String ENTITY = "ENTITY";

	private static final String TRADE = "Trade";
	private static final String CSD = "CSD";
	private static final String IM = "IM";
	private static final String VM = "VM";

	private static final String TOMADO = "TOMADO";
	private static final String PRESTADO = "PRESTADO";

	public static final String LE_ATTR_PARTENON_ACCOUNTING_ID = "PartenonAccountingID";

	public static final String CA_BO_REFERENCE = "CA_BO_REFERENCE";
	public static final String NUMBER_OF_NOTIFICATIONS = "Number Of Notifications";
	public static final String REFERENCED_MUREX_ID = "REFERENCED_MUREX_ID";
	public static final String CA_PAYMENT_DATE = "CA_PAYMENT_DATE";
	public static final String CA_ANNOUNCEMENT_DATE = "CA_ANNOUNCEMENT_DATE";
	public static final String VAL_DATE="VD";
	public static final String RERATE_METHOD ="ReRateMethod";

	public static final String GL_ACCOUNT_ID ="GLAccount Id";
	public static final String GL_ACCOUNT_NAME ="GLAccount Name";
	
	public static final String MC_CONTRACT_TYPE = "MarginCall_ContractType";


	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

    	JDatetime valuationDatetime = null != row ? (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME) : null;
        JDate jDate = null != valuationDatetime ? valuationDatetime.getJDate(TimeZone.getDefault()) : null;

		if (GL_ACCOUNT_ID.equalsIgnoreCase(columnName)) {
			return Optional.ofNullable(row).map(r -> r.getProperty("BOTransfer")).filter(BOTransfer.class::isInstance).map(BOTransfer.class::cast).map(BOTransfer::getGLAccountNumber).orElse(0);
		}else if (columnName.equals(IM_VM)) {
			if(null!=row && row.getProperty(TRADE)!=null){
				Trade trade = (Trade) row.getProperty(TRADE);
				if(trade!=null && trade.getProduct()!=null){
					if(trade.getProduct() instanceof MarginCall){
						MarginCall marginCall = (MarginCall) trade.getProduct();
						if(marginCall!=null){
							try {
								CollateralConfig collateralConfig = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(marginCall.getMarginCallId());
								return getIMorVM(collateralConfig);
							} catch (CollateralServiceException e) {
								Log.error(this, "Error extracting the CollateralConfig: " + e.getMessage());
								Log.error(this,e);//Sonar
							}
						}
					}
				}
			}
			return "";
		} else  if(TOMADO_PRESTADO.equalsIgnoreCase(columnName)){
			Trade trade = null;
			if (row != null) {
				trade = row.getProperty("Trade");
			}
			if (trade != null) {
				return (trade.getQuantity() > 0 ? TOMADO : PRESTADO);
			}
		}  else  if(SIGNO_COMISION.equalsIgnoreCase(columnName)) {
			return getSignoComision(row, errors);
		}
		else if (columnName.equals(ACCOUNTING_CENTER)) {
	    	Trade trade = row.getProperty(ReportRow.TRADE);
			if (trade != null) {
			    String partenonAccountingID = trade.getKeywordValue(LE_ATTR_PARTENON_ACCOUNTING_ID);
			    if((partenonAccountingID != null && partenonAccountingID.length() == 21)) {
			    	return partenonAccountingID.substring(4, 8);
			    }
			}
			return "";
	    }
	    else if(PROCESS_DATE.equalsIgnoreCase(columnName)){
	    	return jDate;
	    }
	    else if (columnName.equals(ENTITY)) {
			Trade trade = row.getProperty(ReportRow.TRADE);
			if (trade != null) {
				String partenonAccountingID = trade.getKeywordValue(LE_ATTR_PARTENON_ACCOUNTING_ID);
				if((partenonAccountingID != null && partenonAccountingID.length() == 21)) {
					return partenonAccountingID.substring(0, 4);
				}
			}
	    	BOTransfer transfer = (BOTransfer)row.getProperty(ReportRow.TRANSFER);
	    	Book book = BOCache.getBook(DSConnection.getDefault(), transfer.getBookId());
			String entity = BOCreUtils.getInstance().getEntity(book.getName());
			return BOCreUtils.getInstance().getEntityCod(entity, false);
	    }
		else if (columnName.equals(CA_BO_REFERENCE)) {
			BOTransfer xfer = (BOTransfer)row.getProperty(ReportRow.TRANSFER);
			try{
				Trade caTrade = DSConnection.getDefault().getRemoteTrade().getTrade(xfer.getTradeLongId());
				if (caTrade!=null){
					if (caTrade.getProduct() instanceof CA) {
						String caSource = caTrade.getKeywordValue("CASource");
						String caSourceProductType = caTrade.getKeywordValue("CASourceProductType");
						if (!Util.isEmpty(caSource) && !Util.isEmpty(caSourceProductType)) {
							Long tradeId = Long.parseLong(caSource);
							Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
							if (trade != null) {
								return trade.getKeywordValue("PartenonAccountingID");
							}
						}
					}
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, "Could not get the trade with id: " + xfer.getLongId());
			}
			return "";
		}
		else if (columnName.equals(NUMBER_OF_NOTIFICATIONS)) {
			int count = 0;
			BOTransfer xfer = (BOTransfer) row.getProperty(ReportRow.TRANSFER);
			try{
				if(xfer!=null) {
					Trade caTrade = DSConnection.getDefault().getRemoteTrade().getTrade(xfer.getTradeLongId());
					if (caTrade != null) {
						if (caTrade.getProduct() instanceof CA) {
							final MessageArray messages = DSConnection.getDefault().getRemoteBO().getTransferMessages(xfer.getLongId());
							if (messages != null && messages.size() > 0) {
								for (int i = 0; i < messages.size(); i++) {
									BOMessage message = messages.get(i);
									if ("CA_NOTIF".equalsIgnoreCase(message.getMessageType()) && "SENT".equalsIgnoreCase(message.getStatus().getStatus())) {
										count++;
									}
								}
							}
						}
					}
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, "Could not get the messages from transfer id: " + xfer.getLongId());
			}
			return String.valueOf(count);
		}
		else if (columnName.equals(REFERENCED_MUREX_ID)) {
			BOTransfer xfer = (BOTransfer)row.getProperty(ReportRow.TRANSFER);
			try{
				Trade caTrade = DSConnection.getDefault().getRemoteTrade().getTrade(xfer.getTradeLongId());
				if (caTrade!=null){
					if (caTrade.getProduct() instanceof CA) {
						String caSource = caTrade.getKeywordValue("CASource");
						String caSourceProductType = caTrade.getKeywordValue("CASourceProductType");
						if (!Util.isEmpty(caSource) && !Util.isEmpty(caSourceProductType)) {
							Long tradeId = Long.parseLong(caSource);
							Trade refTrade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
							if (refTrade != null) {
								String refTradeMurexId = refTrade.getKeywordValue("MurexRootContract");
								return refTradeMurexId;
							}
						}
					}
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, "Could not get the trade with id: " + xfer.getTradeLongId());
			}
			return "";
		}
		else if (columnName.equals(CA_PAYMENT_DATE)) {
			BOTransfer xfer = (BOTransfer)row.getProperty(ReportRow.TRANSFER);
			try{
				Trade caTrade = DSConnection.getDefault().getRemoteTrade().getTrade(xfer.getTradeLongId());
				if (caTrade!=null){
					if (caTrade.getProduct() instanceof CA) {
						CA ca = (CA) caTrade.getProduct();
						return ca.getFinalPaymentMaturityDate();
					}
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, "Could not get the trade with id: " + xfer.getTradeLongId());
			}
			return "";
		}
		else if (columnName.equals(CA_ANNOUNCEMENT_DATE)) {
			BOTransfer xfer = (BOTransfer)row.getProperty(ReportRow.TRANSFER);
			try{
				Trade caTrade = DSConnection.getDefault().getRemoteTrade().getTrade(xfer.getTradeLongId());
				if (caTrade!=null){
					if (caTrade.getProduct() instanceof CA) {
						CA ca = (CA) caTrade.getProduct();
						return ca.getAnnounceDate();
					}
				}
			} catch (CalypsoServiceException e) {
				Log.error(this, "Could not get the trade with id: " + xfer.getTradeLongId());
			}
			return "";
		} else if(VAL_DATE.equalsIgnoreCase(columnName)){
			BOTransfer transfer = (BOTransfer)row.getProperty(ReportRow.TRANSFER);
			if(transfer!=null ){
				JDate settleDate = transfer.getValueDate();
				JDateFormat format = new JDateFormat("yyyyMMdd");
				return format.format(settleDate);
			}
			return "";
		} else if (CUSTOM_NOMINAL.equals(columnName)){
			SignedAmount nominal = (SignedAmount) super.getColumnValue(row, "Nominal", errors);
			return parseNominalAmount(nominal);
		}
		else if (GL_ACCOUNT_NAME.equals(columnName)){
			Account accountFromGLAccountNumber = getAccountFromGLAccountNumber(row);
			return null!=accountFromGLAccountNumber ? accountFromGLAccountNumber.getName() : "";
		}if (columnName.equals(RERATE_METHOD)) {
			if(null!=row){
				if(row.getProperty(TRADE)!=null){
					return Optional.ofNullable(row.getProperty(TRADE)).map(Trade.class::cast).map(t -> t.getKeywordValue(RERATE_METHOD)).orElse("");
				}else {
					final Integer legalEntityId = Optional.ofNullable(row.getProperty(ReportRow.TRANSFER)).map(BOTransfer.class::cast).map(BOTransfer::getExternalLegalEntityId).orElse(-1);
					return Optional.ofNullable(BOCache.getLegalEntityAttributes(DSConnection.getDefault(), legalEntityId))
							.orElse(new Vector<>()).stream()
							.filter(att -> att.getAttributeType().equalsIgnoreCase("RERATE_METHOD"))
							.findFirst().map(LegalEntityAttribute::getAttributeValue).orElse("");

				}
			}
			return "";
		} else if (columnName.equals(MC_CONTRACT_TYPE)) {
			Trade trade = row.getProperty(TRADE);
			if(trade != null && trade.getProduct() != null) {
				MarginCall mc = (MarginCall) trade.getProduct();
				return Optional.ofNullable(mc).map(t -> t.getMarginCallConfig()).map(k -> k.getContractType()).orElse("");
			}
			return "";
		}

		return super.getColumnValue(row, columnName, errors);
	}

	private Account getAccountFromGLAccountNumber(ReportRow row){
		try {
			int accountId = Optional.ofNullable(row).map(r -> r.getProperty("BOTransfer")).filter(BOTransfer.class::isInstance)
					.map(BOTransfer.class::cast).map(BOTransfer::getGLAccountNumber).orElse(0);
			return BOCache.getAccount(DSConnection.getDefault(), accountId);
		} catch (Exception e) {
			Log.error(this,"Error: " +e.getMessage());
		}
		return null;
	}

	@Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();
		treeList.add(IM_VM);
		treeList.add(TOMADO_PRESTADO);
		treeList.add(SIGNO_COMISION);
		treeList.add(CUSTOM_NOMINAL);
		return treeList;
	}

	private String getSignoComision(ReportRow row, Vector errors) {
		Trade trade = null;
		if (row != null) {
			trade = row.getProperty("Trade");
		}
		if (trade != null) {
			Object value = super.getColumnValue(row, "Current Fee Rate", errors);
			if (value != null && value instanceof Rate) {
				Rate rate = (Rate) value;
				if (trade.getQuantity() > 0)
					if (rate.get()>0) {
						return "COBRO";
					} else {
						return "PAGO";
					}
				else if (rate.get()>0) {
					return "PAGO";
				} else {
					return "COBRO";
				}
			}
		}
		return "";
	}


	private String getIMorVM(CollateralConfig collateralConfig){
		if(collateralConfig!=null){
			if(CSD.equals(collateralConfig.getContractType())){
				return IM;
			}else {
				return VM;
			}
		}
		return "";
	}


	 private String getAttFromLE(LegalEntity entity, String atttributeName){
	        if(null!=entity && !Util.isEmpty(atttributeName)){
	            Collection<LegalEntityAttribute> attributes = entity.getLegalEntityAttributes();
	            if(!Util.isEmpty(attributes)){
	                for(LegalEntityAttribute att : attributes){
	                    if(att.getAttributeType().equalsIgnoreCase(atttributeName)){
	                        return att.getAttributeValue();
	                    }
	                }
	            }
	        }
	        return "";
	    }

	private String parseNominalAmount(SignedAmount nominal) {
		if (null != nominal) {
			return CollateralUtilities.formatNumber(nominal.get(), "#.00#", Locale.US);
		}
		return "";
	}
}
