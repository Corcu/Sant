package calypsox.tk.upload.uploader;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.*;
import com.calypso.tk.upload.util.UploaderTradeUtil;
import com.calypso.tk.util.TaskArray;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.SantReportingUtil;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * custom class to handle different cash flow number and order from Murex
 * (Primary leg). The purpose is to use a custom UploadCalypsoCashFlowTrade
 * class instead of the core one
 *
 * @author CedricAllain
 *
 */
public class UploadCalypsoTradePerformanceSwap
		extends com.calypso.tk.upload.uploader.UploadCalypsoTradePerformanceSwap {

	public static final String CONTRACT_TYPE_CSA = "CSA";
	public static final String KW_REPROCESS_DATE = "ReprocessDate";
	public static final String DATE_PATTERN = "dd/MM/yyyy";
	public static final String KW_EVENT_ACTION = "EventAction";

	private static final long serialVersionUID = 1L;

	@Override
	public void upload(CalypsoObject object, Vector<BOException> errors, Object dbCon, boolean saveToDB1) {
		setStrikeSchedules(calypsoTrade);
		// save custom cashflows and set it to null so core code will not handle it.
		CashFlows cashFlows = calypsoTrade.getCashFlows();
		calypsoTrade.setCashFlows(null);
		cleanFees();
		super.upload(object, errors, dbCon, saveToDB1);
		// set back the custom cash flow to be handled by the custom code.
		calypsoTrade.setCashFlows(cashFlows);
		handleCustomCashFlows(trade, calypsoTrade, errors);
		setKeywords();
		
		ArrayList<String> errorMsgs = new ArrayList<String>();
		CollateralConfig collateralConfig = getCollateralConfig(this.trade, errorMsgs);
		if (collateralConfig != null) {
			trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, collateralConfig.getId());
		}
		else {
			trade.removeKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER);
		}
		if (!Util.isEmpty(errorMsgs)) {
			Task task = new Task();
	        task.setObjectLongId(trade.getLongId());
	        task.setTradeLongId(trade.getLongId());
	        task.setEventClass(PSEventTrade.class.getSimpleName());
	        task.setDatetime(new JDatetime());
	        task.setNewDatetime(task.getDatetime());
	        task.setPriority(Task.PRIORITY_NORMAL);
	        task.setId(0);
	        task.setStatus(Task.NEW);
	        task.setEventType("CHECKED_TRADE");
	        task.setSource("Uploader");
	        
	        StringBuilder sb = new StringBuilder();
	        sb.append(trade.getExternalReference()).append(" - ");
	        for (String errorMsg : errorMsgs) {
	        	sb.append(errorMsg).append("\n");
	        }
	        task.setComment(sb.toString());
	        
	        try {
	        	TaskArray taskArray = new TaskArray();
	        	taskArray.add(task);
	            DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(taskArray, 0, null);
	        } catch (RemoteException e) {
	            Log.error(this, "Trade uploader: Failed to saveAndPublishTasks: " + task, e);
	        }
		}
	}

	private void setStrikeSchedules(CalypsoTrade calypsoTrade){
		SwapLeg secLeg= Optional.ofNullable(calypsoTrade.getProduct()).map(com.calypso.tk.upload.jaxb.Product::getPerformanceSwap)
				.map(com.calypso.tk.upload.jaxb.PerformanceSwap::getSecondaryLeg).map(SecondaryLeg::getSwapLeg).orElse(null);
		if(secLeg !=null){
			List<AmortizationSchedule> amortSched=secLeg.getAmortizationSchedule();
			Iterator<AmortizationSchedule> amortIterator=amortSched.listIterator();
			StrikePriceScheduledAdapter strikeAdapter=new StrikePriceScheduledAdapter();
			while(amortIterator.hasNext()){
				AmortizationSchedule amort=amortIterator.next();
				if(amort.getRate()!=null){
					strikeAdapter.convertAndAddStrikePriceSchedule(amort.getAmortizationDate(),amort.getRate());
					updateCustomFlow(calypsoTrade,amort.getAmortizationDate(),amort.getRate());
					updateAmortSchedule(amortIterator,amort);
				}else if(amort.getSpread()!=null){
					strikeAdapter.convertAndAddStrikePriceSchedule(amort.getAmortizationDate(),amort.getSpread());
					updateCustomFlow(calypsoTrade,amort.getAmortizationDate(),amort.getSpread());
					updateAmortSchedule(amortIterator,amort);

				}
			}
			secLeg.setStrikePriceSchedules(strikeAdapter.getStrikePriceSchedules());
		}
	}

	/**
	 * HotFIX
	 * @param calypsoTrade
	 * @param schedDate
	 * @param schedRate
	 */
	private void updateCustomFlow(CalypsoTrade calypsoTrade, XMLGregorianCalendar schedDate, Double schedRate){
		List<Cashflow> flows=Optional.ofNullable(calypsoTrade.getCashFlows()).map(CashFlows::getCashFlow)
				.orElse(new ArrayList<>());
		if(schedRate!=null && schedDate!=null) {
			for (Cashflow flow : flows) {
				String dateStr = schedDate.toXMLFormat();
				String flowDate = "";
				String flowType = "";
				for (Column column : flow.getColumn()) {
					if ("Pmt Begin".equals(column.getName())) {
						flowDate = column.getValue();
					} else if ("Type".equals(column.getName())) {
						flowType = column.getValue();
					}
				}
				if (dateStr.equals(flowDate) && "INTEREST".equals(flowType)) {
					Column spreadColumn = new Column();
					spreadColumn.setName("Spread");
					spreadColumn.setValue(String.valueOf(schedRate * 100));
					flow.getColumn().add(spreadColumn);
				}
			}
		}
	}
	private void updateAmortSchedule(Iterator<AmortizationSchedule> iterator,AmortizationSchedule amortSchedule){
		if(amortSchedule.getAmortizationAmount()!=null){
			amortSchedule.setRate(null);
			amortSchedule.setSpread(null);
		}else{
			iterator.remove();
		}
	}

	protected void cleanFees () {
		int nbFeePremiumToRemove = 0;
		if(isEventActionCancel()) {
			nbFeePremiumToRemove=1;
		}

		if(trade.getFees()!=null) {
			((Vector<Fee>)trade.getFees()).sort(new Comparator<Fee> () {
				@Override
				public int compare(Fee o1, Fee o2) {
					if(o1==null) {
						if(o2==null)
							return 0;
						return 1;
					}
					if(o2==null) {
						return 0;
					}

					return o2.getDate().compareTo(o1.getDate());
				}

			});
			Iterator<Fee> feeIt = trade.getFees().iterator();
			while(feeIt.hasNext()) {
				Fee fee = feeIt.next();
				if(fee.getType()==null || (!fee.getType().equals("EVENT_PREMIUM"))) {
					feeIt.remove();
				}
				if(fee.getType()!=null && fee.getType().equals("EVENT_PREMIUM") && nbFeePremiumToRemove>0) {
					feeIt.remove();
					nbFeePremiumToRemove--;
				}
			}
		}
	}

	protected void handleCustomCashFlows(Trade trade, CalypsoTrade calypsoTrade, Vector<BOException> errors) {
		if (calypsoTrade.getCashFlows() == null) {
			return;
		}
		UploadCalypsoCashFlowTrade calypsoCashFlowTrade = new UploadCalypsoCashFlowTrade();
		calypsoCashFlowTrade.setProduct(trade.getProduct());

		CalypsoCashFlow jaxbCalypsoCashFlow = new CalypsoCashFlow();

		jaxbCalypsoCashFlow.setCashFlows(calypsoTrade.getCashFlows());

		updateInterestAmtColumnName(jaxbCalypsoCashFlow);

		String mxLastEventValue = getKeyWordValue("MxLastEvent");

		if(mxLastEventValue!=null && mxLastEventValue.contains("EARLY_TERMINATION_TOTAL_RETURN"))
			calypsoCashFlowTrade.addAllFlowsAsFee(jaxbCalypsoCashFlow, errors);
		else
			calypsoCashFlowTrade.upload(jaxbCalypsoCashFlow, errors);

		double pos = trade.getQuantity();
		if (pos == 0.0D)
			pos = trade.getAllocatedQuantity();

		boolean isPay = (pos < 0.0D ? true : false);

		JDate reprocessDate = getReprocessDate(trade);

		for (Fee fee : calypsoCashFlowTrade.getFees()) {
			if(reprocessDate==null || fee.getDate().gte(reprocessDate)) {
				if(trade.getLongId()<=0)
					fee.setTradeLongId(trade.getAllocatedLongSeed());
				else
					fee.setTradeLongId(trade.getLongId());

				if(isPay) {
					fee.setAmount(-fee.getAmount());
				}

				fee.setLegalEntityId(trade.getCounterParty().getId());
				trade.addFee(fee);
			}
		}



		trade.setProduct((Product) calypsoCashFlowTrade.getUploadObject());
	}

	public boolean isEventActionCancel() {
		String eventAction = getKeyWordValue(KW_EVENT_ACTION);
		if(!Util.isEmpty(eventAction) && (eventAction.equals("Cancel") || eventAction.equals("CancelReevent"))) {
			return true;
		}
		return false;
	}


	/**
	 * bug with lockColumn and single flow
	 * @param cashFlow
	 */
	public void updateInterestAmtColumnName(CalypsoCashFlow cashFlow) {

		String bondCouponCcy = null;

		PerformanceSwap perfSwap = (PerformanceSwap)trade.getProduct();
		PerformanceSwapLeg primaryLeg = (PerformanceSwapLeg)perfSwap.getPrimaryLeg();
		Security sec = primaryLeg.getReferenceAsset();
		if (sec instanceof Bond) {
			Bond bond = (Bond) sec;
			bondCouponCcy=bond.getCouponCurrency();
		}

		CurrencyConversionLeg ccyLeg = calypsoTrade.getProduct().getPerformanceSwap().getPrimaryLeg().getCurrencyConversionLeg();
		if(!Util.isEmpty(bondCouponCcy) && ccyLeg!=null) {
			String payCcy = ccyLeg.getPayCurrency();
			if(!Util.isEmpty(payCcy) && !payCcy.equals(bondCouponCcy)) {
				for(Cashflow cashflow : cashFlow.getCashFlows().getCashFlow()) {
					if(cashflow.getLegType().equals(UploadCalypsoCashFlowTrade.PRIMARY_LEG)) {
						for(Column column : cashflow.getColumn()) {
							if("Interest Amt".equals(column.getName())) {
								column.setName("Interest Amt" + " " + payCcy);
							}
						}
					}
				}
			}
		}
	}

	public String getKeyWordValue(String keywordName) {
		if(calypsoTrade.getTradeKeywords()!=null) {
			for(Keyword keyword : calypsoTrade.getTradeKeywords().getKeyword()) {
				if(keyword.getKeywordName().equals(keywordName)) {
					return keyword.getKeywordValue();
				}
			}
		}
		return null;

	}
	protected void setKeywords() {
		trade.addKeyword(CollateralStaticAttributes.CONTRACT_TYPE, CONTRACT_TYPE_CSA);
		trade.addKeyword("NUM_FRONT_ID", calypsoTrade.getExternalReference());
	}
	/**
	 * EMIR : need to keep the Fee known date = fee creation date
	 * @param trade
	 * @param feeVector
	 */
	public void updateFeeKnowDate(Trade trade, Vector<Fee> feeVector) {
		if(trade.getFeesList()!=null && feeVector!=null) {
			for(Fee incomingFee : feeVector) {
				incomingFee.setKnownDate(JDate.getNow());
				for(Fee existingFee : trade.getFeesList()) {
					if(isSameFee(existingFee,incomingFee)) {
						incomingFee.setKnownDate(existingFee.getKnownDate());
					}
				}
			}
		}
	}

	/**
	 * compare fee
	 * @param fee1
	 * @param fee2
	 * @return true if fee are the same except for knownDate and amount
	 */
	public boolean isSameFee(Fee fee1, Fee fee2) {

		try {
			Fee compareFee = (Fee)fee2.clone();
			compareFee.setKnownDate(fee1.getKnownDate());
			compareFee.setAmount(fee1.getAmount());

			return fee1.equals(compareFee);
		} catch (CloneNotSupportedException e) {
			Log.error("UploadCalypsoTradePerformanceSwap", e);
		}
		return false;

	}

	@Override
	public void handleFees(Trade trade, CalypsoTrade calypsoTrade, Connection connection) {
		String action = UploaderTradeUtil.isValidAction(connection, calypsoTrade.getAction());

		String strTradeSource = this.getTradeSource(calypsoTrade);
		Vector<com.calypso.tk.bo.Fee> feeVector = this.getFees(trade, calypsoTrade, connection);

		updateFeeKnowDate(trade, feeVector);

		if (!Util.isEmpty(feeVector)) {
			if (strTradeSource == null) {
				strTradeSource = "DEF";
			}

			String RA = this.replaceOrAppend(strTradeSource, action);


			if ("R".equalsIgnoreCase(RA)) {
				trade.setFees(feeVector);
			} else if ("A".equalsIgnoreCase(RA)) {

				this.appendFee(trade, calypsoTrade, feeVector);
			}
		}

	}

	protected static JDate getReprocessDate(Trade trade) {
		return Util.istringToJDate(trade.getKeywordValue(KW_REPROCESS_DATE), DATE_PATTERN);
	}

	private CollateralConfig getCollateralConfig(Trade trade, ArrayList<String> errorMsgs) {
        CollateralConfig marginCallConfig = null;

        
        try {
            final ArrayList<CollateralConfig> eligibleMarginCallConfigs =
                    SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                            .getEligibleMarginCallConfigs(trade);
            if (Util.isEmpty(eligibleMarginCallConfigs)) {
                errorMsgs.add("No MarginCall Contract found for the Trade.");
            }
            else if (eligibleMarginCallConfigs.size() > 1) {
                errorMsgs.add("More than one MarginCall Contract found for the Trade.");
            }
            
            if (Util.isEmpty(errorMsgs)) {
            	marginCallConfig = eligibleMarginCallConfigs.get(0);
            }
        } catch (RemoteException e) {
        	Log.error(this, "Could not find MarginCall Config : " + e.toString());
        }

        return marginCallConfig;
    }
}
