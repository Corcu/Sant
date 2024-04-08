package calypsox.tk.util;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventTime;
import com.calypso.tk.event.PSEventValuation;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ScheduledTaskEOD_TRADE_VALUATION;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.ValuationUtil;

import java.rmi.RemoteException;
import java.util.*;

public class ScheduledTaskSL_EOD_TRADE_VALUATION extends ScheduledTaskEOD_TRADE_VALUATION {
	public static final String PRICING_ENV_PLMARK_PARAM = "PricingEnv for PLMarks";
	private final String COLLATERAL_EXCLUDE_KWD="CollateralExclude";
	
	@Override
    public String getTaskInformation() {
        return "Compute Valuation with CORE, and use it to make PLMarks";
    }
	
	@Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute(PRICING_ENV_PLMARK_PARAM));

        return attributeList;
    }
	
	@Override
	public Vector getDomainAttributes() {
		Vector v = new Vector();
		v.addElement(PRICING_ENV_PLMARK_PARAM);
		v.addAll(super.getDomainAttributes());
		return v;
	}

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean isValidInput(final Vector messages) {
        boolean ret = super.isValidInput(messages);
        
        if (Util.isEmpty(getAttribute(PRICING_ENV_PLMARK_PARAM))) {
        	messages.addElement("Must select " + PRICING_ENV_PLMARK_PARAM);
        	ret = false;
        }

        return ret;
    }
	
	@Override
	protected boolean publishTradeEvents(ValuationUtil valuationUtil, DSConnection ds, PSConnection ps, Task task,
			TaskArray tasks, JDatetime valDateTime, JDatetime undoDatetime) {
		boolean ret = this.createAndPublishEvents(valuationUtil, ds, ps, task, tasks, valDateTime, undoDatetime);
		
		JDate valueDate = getValuationDatetime().getJDate(TimeZone.getDefault());
		
		List<PLMark> plMarks = new ArrayList<>();
		String pricingEnv =  getAttribute(PRICING_ENV_PLMARK_PARAM);
		if (Util.isEmpty(pricingEnv)) {
			pricingEnv = "DirtyPrice";
		}
		HashMap<String, Double> fxRates = new HashMap<String, Double>();
		Vector<Trade> trades = valuationUtil.getTrades();
		for (Trade trade : trades) {
			if (!isInternal(trade)&&isCollateralizable(trade)) {
				PricerMeasure[] pms = valuationUtil.getTradeMeasures(trade);
				if (pms == null) {
					Log.error(this, "No Pricer Measure for Trade, ignoring Trade " + trade.getLongId());
					continue;
				}
				if (pms != null && pms.length != 1) {
					Log.warn(this, "More than one Pricer Measure, will only use NPV_COLLAT to create PL Marks.");
				}
				boolean npvCollatPMFound = false;
				for (PricerMeasure pm : pms) {
					if (pm.getName().equals(PricerMeasure.S_NPV_COLLAT)) {
						npvCollatPMFound = true;
					}
				}
				if (!npvCollatPMFound) {
					Log.error(this, "NPV_COLLAT Pricer Measure not found, ignoring Trade. " + trade.getLongId());
					continue;
				}

				if (!(trade.getProduct() instanceof SecLending)) {
					Log.error(this, "Trade is not a SecLending, ignoring Trade. " + trade.getLongId());
					continue;
				}
				SecLending sl = (SecLending) trade.getProduct();

				PLMark plMark = null;
				try {
					plMark = CollateralUtilities.createPLMarkIfNotExists(
							trade, DSConnection.getDefault(),
							pricingEnv, valueDate);
				} catch (RemoteException e) {
					Log.error(this, "Error retrieving PLMark : " + e.toString());
					ret = false;
					continue;
				}

				if (plMark == null) {
					Log.error(this, "Could not retrieve/create PLMark for Trade " + trade.getLongId());
					ret = false;
					continue;
				}

				ArrayList<String> errorMsgs = new ArrayList<String>();
				CollateralConfig marginCallConfig = null;
				try {
					int mccId = trade.getKeywordAsInt("MARGIN_CALL_CONFIG_ID");
					if (mccId > 0) {
						marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);
					} else {
						marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);
					}
				} catch (RemoteException e) {
					Log.error(this, "Could not retrieve Margin Call Config for Trade " + trade.getLongId() + " - " + e.toString());
				}
				
				if (marginCallConfig == null) {
					Log.error(this, "No Margin Call Config found. Ignoring Trade " + trade.getLongId());
					continue;
				}

				PLMarkValue plMarkValue = null;
				for (PricerMeasure pm : pms) {
					if (!pm.getName().equals(PricerMeasure.S_NPV_COLLAT)) {
						continue;
					}

					// NPV PL Mark is a copy of Pricer Measure calculated
					//plMarkValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_NPV, pm.getCurrency(), pm.getValue(), "");
					plMarkValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_NPV, sl.getSecurity().getCurrency(), -pm.getValue(), "");
					plMark.addPLMarkValue(plMarkValue);


					// Conversion for NPV_BASE
					double baseMtmValue = pm.getValue();
					//String baseMtmCcy = pm.getCurrency();
					String baseMtmCcy = sl.getSecurity().getCurrency();
					if (marginCallConfig != null && !marginCallConfig.getCurrency().equals(baseMtmCcy)) {
						JDate fxdate = valueDate; //.addBusinessDays(-1, holidays);

						String fxRateKey = fxdate.toString() + baseMtmCcy + marginCallConfig.getCurrency();
						Double fxRate = 0.0;
						if (!fxRates.containsKey(fxRateKey)) {
							fxRate = CollateralUtilities.getFXRate(fxdate, baseMtmCcy, marginCallConfig.getCurrency());
							fxRates.put(fxRateKey, fxRate);
						} else {
							fxRate = fxRates.get(fxRateKey);
						}

						if (fxRate > 0.0) {
							baseMtmCcy = marginCallConfig.getCurrency();
							baseMtmValue = baseMtmValue * fxRate;
						}
					}
					plMarkValue = CollateralUtilities.buildPLMarkValue(SantPricerMeasure.S_NPV_BASE, baseMtmCcy, -baseMtmValue, "");
					plMark.addPLMarkValue(plMarkValue);

					// Haircut for MARGIN_CALL
					double haircutValue = getHaircut(sl);
					//if (null!=marginCallConfig && haircutValue <= 0.0) {
					if (null!=marginCallConfig && isContractHaircutNeeded(haircutValue)) {
						// If no haircut from product, search it in additional fields of contract
						//haircutValue = CollateralUtilities.getProductHaircut(marginCallConfig, sl, valueDate);
						String haircutPercent = marginCallConfig.getAdditionalField(CollateralStaticAttributes.MCC_HAIRCUT);
						if (haircutPercent != null) {
							try {
								haircutValue = Double.parseDouble(haircutPercent)/100;
							} catch (NumberFormatException e) {
								haircutValue = 0.0;
								Log.error(this, CollateralStaticAttributes.MCC_HAIRCUT + " has a non-numeric format in contract " + marginCallConfig.getId() + " - " + e.toString());
							}
						}
					}
					Double plMarkValueAmount = pm.getValue();
					if (haircutValue > 0.0) {
						plMarkValueAmount = pm.getValue() * haircutValue;
					}
					plMarkValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_MARGIN_CALL, sl.getSecurity().getCurrency(), -plMarkValueAmount, "");
					plMark.addPLMarkValue(plMarkValue);
				}

				plMarks.add(plMark);
				}
			}
				if (plMarks.size() > 0) {
					try {
						CollateralUtilities.savePLMarks(plMarks);
					} catch (InterruptedException e) {
						Log.error(this, "Error : " + e.toString());
						ret = false;
					}
				}
		return ret;
	}

	private boolean isContractHaircutNeeded(double haircutValue){
		return Double.valueOf(haircutValue).equals(1.0D) || Double.valueOf(haircutValue).equals(0.0D);
	}
	private boolean isCollateralizable(Trade trade){
		return Optional.ofNullable(trade).map(t->t.getKeywordValue(COLLATERAL_EXCLUDE_KWD))
				.map(value->!Boolean.parseBoolean(value)).orElse(true);
	}

	private double getHaircut(SecLending sl){
		double haircut=0.0D;
		Vector collaterals=sl.getCollaterals();
		if (!Util.isEmpty(collaterals) && collaterals.get(0) instanceof Collateral) {
			haircut = ((Collateral) collaterals.get(0)).getHaircut();
			haircut=1+haircut;
		}
		return haircut;
	}

	private boolean createAndPublishEvents(ValuationUtil valuationUtil, DSConnection ds, PSConnection ps, Task task, TaskArray tasks, JDatetime valDateTime, JDatetime undoDatetime) {
		Vector events = new Vector();
		JDatetime valTimeAsOf = valDateTime;
		if (!this.isValDateEOM()) {
			valDateTime = this.getValuationDatetime();
		}

		Vector trades = valuationUtil.getTrades();

		for (Object o : trades) {
			Trade trade = (Trade) o;
			PSEventValuation event = this.createTradeEvent();
			event.setTrade(trade);
			event.setQuantity(trade.getQuantity());
			event.setTradeLongId(trade.getLongId());
			event.setCurrency(getCurrency(trade));
			event.setValuationDate(valDateTime);
			event.setValuationDateAsOf(valTimeAsOf);
			event.setSettleDate(trade.getSettleDate());
			event.setProductId(trade.getProduct().getId());
			event.setUndoDatetime(undoDatetime);
			event.setTradeVersion(trade.getVersion());
			Vector pmv = new Vector();
			PricerMeasure[][] subMeaures = valuationUtil.getTradeSubMeasures(trade);
			event.setSubMeasures(subMeaures);
			PricerMeasure[] measures=valuationUtil.getTradeMeasures(trade);
			if (measures == null) {
				Log.warn(this, "Null measures for Trade " + trade.getLongId());
			} else {
				for (PricerMeasure measure : measures) {
					pmv.addElement(measure);
				}

				event.setMeasures(pmv);
				event.setPricingEnvName(this._pricingEnv);
				events.addElement(event);
			}

			Vector dpmv = new Vector();
			PricerMeasure[] dailymeasures = valuationUtil.getTradeDailyMeasures(trade);
			if (dailymeasures != null) {
				for (int j = 0; j < dailymeasures.length; ++j) {
					dpmv.addElement(dailymeasures[j]);
				}

				event.setDailyMeasures(dpmv);
			}
		}

		PSEventTime evtime = new PSEventTime();
		evtime.setTime(System.currentTimeMillis());
		evtime.setComment("START");

		try {
			if (ps != null) {
				ps.publish(evtime);
			}
		} catch (Exception var23) {
			Log.error(this, var23);
		}

		try {
			Log.debug(this, "Starting Saving Events " + events.size());
			long before = System.currentTimeMillis();
			getReadWriteDS(ds).getRemoteTrade().saveAndPublish(events, this.usePSEventArray(), this.getEventsPerArray());
			long after = System.currentTimeMillis();
			Log.debug(this, "Finished Saving Events. Time Required" + (after - before));
		} catch (Exception var22) {
			Log.error(this, var22);
			task.setComment("Error Saving Events for " + this);
			return false;
		}

		evtime = new PSEventTime();
		evtime.setTime(System.currentTimeMillis());
		evtime.setComment("END");

		try {
			if (ps != null) {
				ps.publish(evtime);
			}
		} catch (Exception var21) {
			Log.error(this, var21);
		}

		return true;
	}

	private String getCurrency(Trade trade){
		String curr=trade.getProduct().getCurrency();
		if(trade.getProduct() instanceof SecLending){
			curr= Optional.ofNullable(((SecLending) trade.getProduct()).getSecurity())
					.map(Product::getCurrency).orElse(curr);
		}
		return curr;
	}

	private boolean isInternal(Trade trade){
		return Optional.ofNullable(trade.getMirrorBook()).isPresent();
	}
}
