package calypsox.tk.upload.uploader;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.Pair;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwappableLeg;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.upload.jaxb.CalypsoCashFlow;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.Cashflow;
import com.calypso.tk.upload.jaxb.Column;
import com.calypso.tk.util.CurrencyUtil;

/**
 * custom class to handle different cash flow number and order from Murex
 * (Primary leg)
 * 
 * @author CedricAllain
 *
 */
public class UploadCalypsoCashFlowTrade extends com.calypso.tk.upload.uploader.UploadCalypsoCashFlowTrade {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String LOG_CATEGORY = "UploadCalypsoCashFlowTrade";

	public static final String PRIMARY_LEG = "Primary";
	public static final String HOLIDAY_CODE = "SYSTEM";

	public static final String COLUMN_TYPE = "Type";
	public static final String COLUMN_CURRENCY = "Currency";
	public static final String COLUMN_INTEREST_AMOUNT = "Interest Amt";
	public static final String COLUMN_PAYMENT_DATE = "Pmt Dt";
	public static final String COLUMN_PAYMENT_BEGIN = "Pmt Begin";
	public static final String COLUMN_PAYMENT_END = "Pmt End";
	public static final String COLUMN_DELETION_EVENT_ID = "DeletionEventId";
	public static final String COLUMN_INSERTION_ID = "InsertionId";
	
	public static final String COLUMN_NOTIONAL = "Notional";

	public static final String DATE_FORMAT = "yyyy-MM-dd";
	
	public static final String MATCHING_DATE_MAX_OFFSET_DV = "BRS.CashFlows.MatchingDateMaxOffset";
	public static final Integer MatchingDateMaxOffsetDefault = 3;

	protected Vector<Fee> fees = new Vector<Fee>();

	public Vector<Fee> getFees() {
		return fees;
	}

	public void addAllFlowsAsFee(CalypsoObject jaxbObject, Vector<BOException> errors) {

		CalypsoCashFlow jaxbCalypsoCashFlow = (CalypsoCashFlow) jaxbObject;

		if (isCustomCashFlowsPresent(jaxbCalypsoCashFlow.getCashFlows())) {
			Iterator<Cashflow> customFlowIterator = jaxbCalypsoCashFlow.getCashFlows().getCashFlow().iterator();

			while (customFlowIterator.hasNext()) {
				Cashflow cashflow = customFlowIterator.next();
				String legType = cashflow.getLegType();
				addAsFee(cashflow,!PRIMARY_LEG.equals(legType));
				customFlowIterator.remove();
			}
		}
	}
	
	
	public void upload(CalypsoObject jaxbObject, Vector<BOException> errors) {
		if (product instanceof PerformanceSwap) {
			PerformanceSwap perfSwap = (PerformanceSwap) product;
			PerformanceSwappableLeg primaryLeg = perfSwap.getPrimaryLeg();
			CalypsoCashFlow jaxbCalypsoCashFlow = (CalypsoCashFlow) jaxbObject;
			if (isCustomCashFlowsPresent(jaxbCalypsoCashFlow.getCashFlows())) {
				CashFlowSet sortedFlows = sortCashFlows(primaryLeg.getFlows());
				handleInsertionDeletionId(jaxbCalypsoCashFlow);
				matchCashFlows(jaxbCalypsoCashFlow, sortedFlows);
				handleSingleFlows(jaxbCalypsoCashFlow);
			}
			
		}
		super.upload(jaxbObject, errors);

	}
	
	/**
	 * bug with lockColumn and single flow
	 * @param cashFlow
	 */
	public void handleSingleFlows(CalypsoCashFlow cashFlow) {
		int primaryLegCount = 0;
		int secondaryLegCount = 0;
		for(Cashflow cashflow : cashFlow.getCashFlows().getCashFlow()) {
			if(cashflow.getLegType().equals(PRIMARY_LEG))
				primaryLegCount++;
			else
				secondaryLegCount++;
		}
		
		if(primaryLegCount==1 || secondaryLegCount==1 ) {
			if(cashFlow.getCashFlows().getLockColumns()!=null)
				cashFlow.getCashFlows().getLockColumns().clear();
		}
		
	}
	
	protected void handleInsertionDeletionId(CalypsoCashFlow customCF) {
		
		Iterator<Cashflow> customFlowIterator = customCF.getCashFlows().getCashFlow().iterator();
		
		while (customFlowIterator.hasNext()) {
			Cashflow customFlow = customFlowIterator.next();
			if((hasCashInsertionId(customFlow) || hasCashDeletionEventId(customFlow)) && customFlow.getLegType().equals(PRIMARY_LEG)) {
				addAsFee(customFlow);
				customFlowIterator.remove();
			}
			removeColumn(customFlow, COLUMN_DELETION_EVENT_ID);
			removeColumn(customFlow, COLUMN_INSERTION_ID);
		}
		
		
	}

	/**
	 * matching the cashflows of the primary leg based on cashflow type. non
	 * matching flow will go into fees.
	 * 
	 * @param customCF
	 * @param coreCF
	 */

	protected void matchCashFlows(CalypsoCashFlow customCF, CashFlowSet coreCF) {
		

		Iterator<Cashflow> customFlowIterator = customCF.getCashFlows().getCashFlow().iterator();
		Pair<Cashflow, CashFlow> startingCashFlow = findStartingFlows(customCF, coreCF);
		
		ArrayList<Cashflow> leadingFlows = new ArrayList<Cashflow>();
		
		if(startingCashFlow!=null) {
			Cashflow startingCustomFlow = startingCashFlow.first();
			CashFlow startingCoreFlow = startingCashFlow.second();
			
			boolean startingCFFound = false;
			Cashflow customFlow = null;
			
			int nbCashFlows =0;
	
			while (customFlowIterator.hasNext() && !startingCFFound) {
				customFlow = customFlowIterator.next();
				if (customFlow.getLegType().equals(PRIMARY_LEG)) {
					if (startingCustomFlow == customFlow) {
						startingCFFound = true;
					} else {
						addAsFee(customFlow);
						customFlowIterator.remove();
					}
				}
	
			}
	
			
			
			Iterator<CashFlow> coreFlowIterator = coreCF.iterator();
			startingCFFound = false;
			CashFlow coreFlow = null;
	
			while (coreFlowIterator.hasNext() && customFlowIterator.hasNext() && nbCashFlows<coreCF.size()) {
				coreFlow = coreFlowIterator.next();
				if (startingCFFound) {
					boolean matchingCustomFlowFound = false;
					while (customFlowIterator.hasNext() && !matchingCustomFlowFound) {
						customFlow = customFlowIterator.next();
						if (customFlow.getLegType().equals(PRIMARY_LEG)) {
							if (coreFlow.getType().equals(getCashFlowType(customFlow))) {
								matchingCustomFlowFound = true;
								nbCashFlows++;
							} else {
								addAsFee(customFlow);
								customFlowIterator.remove();
							}
						}
					}
				}
				if (startingCoreFlow == coreFlow) {
					startingCFFound = true;
				}
				if(!startingCFFound) {
					nbCashFlows++;
					leadingFlows.add(createFlow(startingCustomFlow,coreFlow));
				}
			}

		}
		
		while(customFlowIterator.hasNext()) {
			Cashflow customFlow = customFlowIterator.next();
			if (customFlow.getLegType().equals(PRIMARY_LEG)) {
				addAsFee(customFlow);
				customFlowIterator.remove();
			}
		}
		
		if(customCF.getCashFlows().getCashFlow().size()>0) {
			customCF.getCashFlows().getCashFlow().addAll(0, leadingFlows);
		}
			

	}
	
	protected Cashflow createFlow(Cashflow startingCustomFlow, CashFlow cashFlow) {
		Cashflow newFlow = new Cashflow();
		Column columnFlowType = new Column();
		columnFlowType.setName(COLUMN_TYPE);
		columnFlowType.setValue(cashFlow.getType());
		Column columnLegPmtDate = new Column();
		columnLegPmtDate.setName(COLUMN_PAYMENT_DATE);
		columnLegPmtDate.setValue(jdateToXMLString(cashFlow.getDate()));
		Column columnManualAmt = new Column();
		columnManualAmt.setName("Manual Amt");
		columnManualAmt.setValue("TRUE");
		Column columnInterestAmt = new Column();
		columnInterestAmt.setName(COLUMN_INTEREST_AMOUNT);
		columnInterestAmt.setValue("0");
		Column columnNotional = new Column();
		columnNotional.setName(COLUMN_NOTIONAL);
		columnNotional.setValue(getNotional(startingCustomFlow));
		Column columnPmtBegin = new Column();
		columnPmtBegin.setName(COLUMN_PAYMENT_BEGIN);
		columnPmtBegin.setValue(getPaymentBegin(startingCustomFlow));
		Column columnPmtEnd = new Column();
		columnPmtEnd.setName(COLUMN_PAYMENT_END);
		columnPmtEnd.setValue(getPaymentEnd(startingCustomFlow));
		
		newFlow.setLegType(PRIMARY_LEG);
		newFlow.getColumn().add(columnFlowType);
		newFlow.getColumn().add(columnLegPmtDate);
		newFlow.getColumn().add(columnManualAmt);
		newFlow.getColumn().add(columnInterestAmt);
		newFlow.getColumn().add(columnNotional);
		newFlow.getColumn().add(columnPmtBegin);
		newFlow.getColumn().add(columnPmtEnd);
		
		return newFlow;
		
	}
	
	protected String jdateToXMLString(JDate date) {
		DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		return df.format(date.getDate());
	}

	protected Integer getMatchingDateMaxOffset() {
		Vector<String> maxDateOffsets = LocalCache.getDomainValues(DSConnection.getDefault(),
				MATCHING_DATE_MAX_OFFSET_DV);
		if (maxDateOffsets != null && maxDateOffsets.size() > 0) {
			return Integer.parseInt(maxDateOffsets.get(0));
		}

		return MatchingDateMaxOffsetDefault;
	}

	/**
	 * Define cashflow to start the matching from (Murex doesn't necessarily send
	 * past cash flows)
	 * 
	 * @param customCF
	 * @param coreCF
	 * @return a pair with starting custom and core cashflow (1st matched cash
	 *         flows)
	 */
	protected Pair<Cashflow, CashFlow> findStartingFlows(CalypsoCashFlow customCF, CashFlowSet coreCF) {

		Integer matchingDateMaxOffset = getMatchingDateMaxOffset();

		Double currentMinDateDiff = null;
		Cashflow startCustomCashFlow = null;
		CashFlow startCoreCashFlow = null;

		Iterator<Cashflow> customFlowIterator = customCF.getCashFlows().getCashFlow().iterator();

		JDate coreFlowDate = null;

		while (customFlowIterator.hasNext()) {

			Cashflow customFlow = customFlowIterator.next();

			if (customFlow.getLegType().equals(PRIMARY_LEG)) {
				CashFlow foundCoreFlow = null;
				JDate customFlowDate = getCashFlowPaymentDate(customFlow);
				JDate startDate = customFlowDate.addBusinessDays(-matchingDateMaxOffset,
						Util.string2Vector(HOLIDAY_CODE));
				JDate endDate = customFlowDate.addBusinessDays(matchingDateMaxOffset, Util.string2Vector(HOLIDAY_CODE));

				Iterator<CashFlow> coreFlowIterator = coreCF.iterator();

				Double dateDiff = null;
				while (coreFlowIterator.hasNext() && (coreFlowDate == null || coreFlowDate.lte(endDate))) {
					CashFlow coreFlow = coreFlowIterator.next();
					coreFlowDate = coreFlow.getDate();
					if (getCashFlowType(customFlow).equals(coreFlow.getType()) && (coreFlowDate.gte(startDate))
							&& coreFlowDate.lte(endDate)) {
						Double incomingDateDiff = Math.abs(JDate.diff(customFlowDate, coreFlowDate));
						if (incomingDateDiff == 0)
							return new Pair<Cashflow, CashFlow>(customFlow, coreFlow);
						if (dateDiff == null || dateDiff > incomingDateDiff) {
							dateDiff = incomingDateDiff;
							foundCoreFlow = coreFlow;
						}
					}
				}

				if (dateDiff != null && (currentMinDateDiff == null || currentMinDateDiff > dateDiff)) {
					currentMinDateDiff = dateDiff;
					startCoreCashFlow = foundCoreFlow;
					startCustomCashFlow = customFlow;
				}
			}

		}

		if(startCustomCashFlow==null || startCoreCashFlow==null) {
			return null;
		}
		
		return new Pair<Cashflow, CashFlow>(startCustomCashFlow, startCoreCashFlow);

	}
	
	/**
	 * sort flow in Date / Type order (same order than screen and custom flow for data uploader)
	 * @param inputCashFlowSet
	 * @return
	 */
	public CashFlowSet sortCashFlows(CashFlowSet inputCashFlowSet) {
		CashFlowSet sortedSet = new CashFlowSet();
		List<CashFlow> cashFlows = Arrays.asList(inputCashFlowSet.getFlows());
		
		Collections.sort(cashFlows ,new Comparator<CashFlow>() {
			@Override
			public int compare(CashFlow o1, CashFlow o2) {
				int c=0;
				c = o1.getDate().compareTo(o2.getDate());
				if(c!=0) return c;
				
				c = o1.getType().compareTo(o2.getType());
				if(c!=0) return c;
					
				return c;
				
			}
		});
		
		for(CashFlow cashFlow : cashFlows)
			sortedSet.add(cashFlow);
		
		return sortedSet;
		
	}
	
	protected void addAsFee(Cashflow cashFlow) {
		addAsFee(cashFlow, false);
	}

	protected void addAsFee(Cashflow cashFlow, boolean reverseAmount) {
		String currency = getCashFlowCurrency(cashFlow);
		Double interestAmount = getCashFlowInterestAmount(cashFlow);
		if(interestAmount==null)
			return;
		interestAmount = CurrencyUtil.roundAmount(interestAmount, currency);
		if(reverseAmount)
			interestAmount = -interestAmount;
		if(interestAmount!=0.0d) {
			Fee fee = new Fee();
			fee.setAmount(interestAmount);
			fee.setType(getCashFlowType(cashFlow));
			fee.setCurrency(getCashFlowCurrency(cashFlow));
			fee.setDate(getCashFlowPaymentDate(cashFlow));
			fee.setStartDate(stringToJDate(getPaymentBegin(cashFlow)));
			fee.setEndDate(stringToJDate(getPaymentEnd(cashFlow)));
			fees.add(fee);
		}
	}

	protected static String getCashFlowType(Cashflow cashFlow) {
		return getColumnValue(cashFlow, COLUMN_TYPE);
	}
	
	protected static String getNotional(Cashflow cashFlow) {
		return getColumnValue(cashFlow, COLUMN_NOTIONAL);
	}
	
	protected static String getPaymentBegin(Cashflow cashFlow) {
		return getColumnValue(cashFlow, COLUMN_PAYMENT_BEGIN);
	}
	
	protected static String getPaymentEnd(Cashflow cashFlow) {
		return getColumnValue(cashFlow, COLUMN_PAYMENT_END);
	}
	
	protected static String getCashFlowCurrency(Cashflow cashFlow) {
		return getColumnValue(cashFlow, COLUMN_CURRENCY);
	}

	protected static Double getCashFlowInterestAmount(Cashflow cashFlow) {
		String value = getColumnValue(cashFlow, COLUMN_INTEREST_AMOUNT);
		if(value==null)
			return null;
		return Double.parseDouble(value);
	}

	protected static JDate getCashFlowPaymentDate(Cashflow cashFlow) {
		String value = getColumnValue(cashFlow, COLUMN_PAYMENT_DATE);
		if(value==null)
			return null;
		return stringToJDate(value);
	}
	
	protected static String getCashInsertionId(Cashflow cashFlow) {
		return getColumnValue(cashFlow, COLUMN_INSERTION_ID);
	}
	
	protected static Boolean hasCashInsertionId(Cashflow cashFlow) {
		return getCashInsertionId(cashFlow)==null?false:true;
	}
	
	protected static String getCashDeletionId(Cashflow cashFlow) {
		return getColumnValue(cashFlow, COLUMN_DELETION_EVENT_ID);
	}
	
	protected static Boolean hasCashDeletionEventId(Cashflow cashFlow) {
		return getCashDeletionId(cashFlow)==null?false:true;
	}

	public static String getColumnValue(Cashflow cf, String columnName) {
		for (Column c : cf.getColumn()) {
			if (c.getName().trim().equals(columnName))
				return c.getValue();
		}
		return null;

	}
	
	public static void removeColumn(Cashflow cf, String columnName) {
		Iterator<Column> it = cf.getColumn().iterator();
		while(it.hasNext()) {
			Column c = it.next();
			if (c.getName().trim().equals(columnName))
				it.remove();
		}

	}

	public static JDate stringToJDate(String source) {
		DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		try {
			return JDate.valueOf(df.parse(source));
		} catch (ParseException e) {
			Log.error(LOG_CATEGORY, e);
			return null;
		}
	}

}
