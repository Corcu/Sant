package calypsox.tk.util;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventTime;
import com.calypso.tk.event.PSEventValuation;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ScheduledTaskEOD_TRADE_VALUATION;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.ValuationUtil;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

public class ScheduledTaskREPO_EOD_TRADE_VALUATION extends ScheduledTaskEOD_TRADE_VALUATION {
	public static final String PRICING_ENV_PLMARK_PARAM = "PricingEnv for PLMarks";
	private final String COLLATERAL_EXCLUDE_KWD="CollateralExclude";
	private final String PM_FOR_VALUATION = PricerMeasure.S_MARGIN_CALL;
	HashMap<Long,CollateralConfig> contracts = new HashMap<>();

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
					Log.warn(this, "No Pricer Measure for Trade, ignoring Trade " + trade.getLongId());
					continue;
				}
				if (pms != null && pms.length != 1) {
					Log.warn(this, "More than one Pricer Measure, will only use " + PM_FOR_VALUATION + " to create PL Marks.");
				}
				boolean wantedPMFound = false;
				for (PricerMeasure pm : pms) {
					if (pm.getName().equals(PM_FOR_VALUATION)) {
						wantedPMFound = true;
					}
				}
				if (!wantedPMFound) {
					Log.warn(this, PM_FOR_VALUATION + " Pricer Measure not found, ignoring Trade. " + trade.getLongId());
					continue;
				}

				if (!(trade.getProduct() instanceof Repo)) {
					Log.warn(this, "Trade is not a Repo, ignoring Trade. " + trade.getLongId());
					continue;
				}
				Repo repo = (Repo) trade.getProduct();

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
				CollateralConfig marginCallConfig = contracts.get(trade.getLongId());

				if (marginCallConfig == null) {
					Log.error(this, "No Margin Call Config found. Ignoring Trade " + trade.getLongId());
					continue;
				}

				PLMarkValue plMarkValue = null;
				for (PricerMeasure pm : pms) {
					if (!pm.getName().equals(PM_FOR_VALUATION)) {
						continue;
					}

					// NPV PL Mark is a copy of Pricer Measure calculated
					plMarkValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_NPV, pm.getCurrency(), pm.getValue(), "");
					plMark.addPLMarkValue(plMarkValue);
					
					// MARGIN_CALL is calculated via PM
					plMarkValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_MARGIN_CALL, pm.getCurrency(), pm.getValue(), "");
					plMark.addPLMarkValue(plMarkValue);

					// Conversion for NPV_BASE
					double baseMtmValue = pm.getValue();
					String baseMtMCcy = pm.getCurrency();
					if (marginCallConfig != null && !marginCallConfig.getCurrency().equals(baseMtMCcy)) {
						JDate fxdate = valueDate; 

						String fxRateKey = fxdate.toString() + baseMtMCcy + marginCallConfig.getCurrency();
						Double fxRate = 0.0;
						if (!fxRates.containsKey(fxRateKey)) {
							fxRate = CollateralUtilities.getFXRate(fxdate, baseMtMCcy, marginCallConfig.getCurrency());
							fxRates.put(fxRateKey, fxRate);
						} else {
							fxRate = fxRates.get(fxRateKey);
						}

						if (fxRate > 0.0) {
							baseMtMCcy = marginCallConfig.getCurrency();
							baseMtmValue = baseMtmValue * fxRate;
						}
					}
					plMarkValue = CollateralUtilities.buildPLMarkValue(SantPricerMeasure.S_NPV_BASE, baseMtMCcy, baseMtmValue, "");
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

	private double getHaircut(Repo repo){
		double haircut=0.0D;
		Vector collaterals = repo.getCollaterals();
		if (!Util.isEmpty(collaterals) && collaterals.get(0) instanceof Collateral) {
			haircut = ((Collateral) collaterals.get(0)).getHaircut();
			haircut = 1 + haircut;
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
		if(trade.getProduct() instanceof Repo){
			curr= Optional.ofNullable(((Repo) trade.getProduct()).getSecurity())
					.map(Product::getCurrency).orElse(curr);
		}
		return curr;
	}

	/**
	 * Remove Interal Trades (mirror book is present)
	 * @param valuationUtil
	 */
	private void filterInteralRepoTrades(ValuationUtil valuationUtil){
		final Vector trades = valuationUtil.getTrades();
		final List<Trade> filteredTrades = Arrays.stream(trades.toArray()).map(Trade.class::cast).filter(t -> !isInternal(t)).collect(Collectors.toList());
		valuationUtil.setTrades(new TradeArray(filteredTrades));
	}

	protected boolean handleTradeValuation(DSConnection ds, PSConnection ps, Task task, long maxWaitTime, TaskArray tasks) {
		JDatetime valDatetime = this.getValuationDatetime2(this.getCurrentDate());
		JDatetime undoDatetime = this.getUndoTime() > 0 ? this.getUndoTime(this.getCurrentDate()) : null;
		if (Log.isCategoryLogged(Log.OLD_TRACE)) {
			Log.debug(Log.OLD_TRACE, "ScheduledTask TradeValuation " + this.getId() + " ValDatetime: " + valDatetime + " UndoDatetime: " + undoDatetime + " PE: " + this._pricingEnv);
		}

		boolean checkMktDataOnlyB = this.getBooleanAttribute("Check Market Data Only");
		String keepCF = this.getAttribute("KEEP_CASH_FLOWS");
		boolean keepCFB = keepCF != null ? Boolean.parseBoolean(keepCF) : false;
		ValuationUtil valuationUtil = new ValuationUtil();
		valuationUtil.setTimeout(maxWaitTime);
		if (!valuationUtil.setPricingEnv(this._pricingEnv, valDatetime, ds)) {
			Log.error(this, "Invalid pricingEnv " + this._pricingEnv);
			return false;
		} else if (this._tradeFilter == null) {
			task.setComment("Trade Filter is not defined");
			return false;
		} else if (!valuationUtil.setTradeFilter(this._tradeFilter, ds)) {
			task.setComment("Error Loading Trade Filter " + this._tradeFilter + " For:" + this);
			return false;
		} else {
			if (this.preLoadProducts()) {
				TradeFilter tf = valuationUtil.getTradeFilter();
				if (tf != null) {
					try {
						BOCache.getProductsFromPLPosition(ds, tf, true);
					} catch (Exception var35) {
						Log.error(this, var35);
					}
				}
			}

			String v = this.getAttribute("STATIC DATA FILTER");
			if (v != null && v.trim().length() > 0) {
				valuationUtil.setStaticDataFilter(v, ds);
			}

			boolean removePosition = this.getIsRemovePosition();
			boolean includeMatured = this.getMaturedTrades();
			if (includeMatured && removePosition) {
				task.setComment("Include Matured Trade and Remove Position are not compatible.Remove Position must be false when select Matured Trade" + this);
				return false;
			} else if (!valuationUtil.loadTrades(valDatetime, undoDatetime, removePosition, this.getUseTradeDatePosition(), this.getEOD(), true, includeMatured, ds)) {
				task.setComment("Error Loading Trades for Trade Filter " + this._tradeFilter + " For:" + this);
				return false;
			} else if (valuationUtil.getNumberOfTrades() == 0) {
				task.setComment("Nothing to process for TRADE VALUATION FOR: " + this);
				return true;
			} else {

				filterInteralRepoTrades(valuationUtil);

				v = this.getAttribute("PRELOAD_POSTINGS");
				if (v != null && (v.toLowerCase().equals("y") || v.toLowerCase().equals("true") || v.toLowerCase().equals("yes"))) {
					try {
						ds.getRemoteAccounting().ensurePostingsCached(valuationUtil.getTradeFilter());
						ds.getRemoteAccounting().ensureCresCached(valuationUtil.getTradeFilter());
					} catch (Exception var34) {
						Log.error(this, var34);
					}
				}

				if (this.getPricerMeasures() != null && this.getPricerMeasures().size() != 0) {
					valuationUtil.setPricerMeasure(this.getPricerMeasures());
					valuationUtil.setDailyPM(this.isDailyPM());
					Hashtable exceptions = new Hashtable();
					String disp = this.getAttribute("DISPATCHER");
					if (com.calypso.tk.core.Util.isEmpty(disp)) {
						disp = null;
					}

					String stradePerJob = this.getAttribute("DISPATCHER_TRADE_PER_JOB");
					int jobPerTrade = stradePerJob != null ? Integer.parseInt(stradePerJob) : 0;
					int eventsPerPublish = this.getEventsPerPublish();
					JDatetime eventDatetime = valDatetime;
					if (!this.isValDateEOM()) {
						eventDatetime = this.getValuationDatetime();
					}

					DSConnection rwDs = null;

					try {
						rwDs = getReadWriteDS(ds);
					} catch (Exception var33) {
						Log.error(this, var33);
					}

					if (rwDs == null || checkMktDataOnlyB) {
						eventsPerPublish = 0;
					}

					int eventsPerArray = this.getEventsPerArray();
					boolean useEventArray = this.usePSEventArray();
					if (!this.postSQLLoadedFiltering(valuationUtil, ds, tasks)) {
						task.setComment("No Eligible Trades Selected for TRADE VALUATION: " + this);
						return false;
					} else {

						preloadAndValidateContracts(valuationUtil);

						boolean allOk = valuationUtil.priceTrades(valDatetime, exceptions, this.getAttribute("VALUATION"), this.getAttribute("VALCCY"), disp, jobPerTrade, keepCFB, eventsPerPublish, useEventArray, eventsPerArray, eventDatetime, undoDatetime, rwDs);
						if (exceptions.size() > 0) {
							StringBuffer buffer = new StringBuffer();
							int counter = 0;

							String message;
							for(Enumeration e = exceptions.keys(); e.hasMoreElements(); buffer.append(message)) {
								Trade trade = (Trade)e.nextElement();
								message = (String)exceptions.get(trade);
								addException(message, trade, tasks);
								if (counter > 0) {
									buffer.append(System.getProperty("line.separator"));
								}
							}

							Log.error(this, buffer.toString());
							task.setComment("Error Pricing Trade for " + this);
							int commentId = 0;
							List messages = new ArrayList(valuationUtil.items.keySet());
							if (!com.calypso.tk.core.Util.isEmpty(messages)) {
								List messagese = new ArrayList();
								Iterator var30 = messages.iterator();

								while(var30.hasNext()) {
									Object key = var30.next();
									String value = (String)valuationUtil.items.get(key);
									messagese.add(key + ", " + value);
								}

								commentId = this.saveGenericComment(messagese).getId();
							}

							task.setLinkId((long)commentId);
						}

						if (eventsPerPublish <= 0 && !checkMktDataOnlyB) {
							boolean publishedOK = this.publishTradeEvents(valuationUtil, ds, ps, task, tasks, valDatetime, undoDatetime);
							allOk = allOk && !this.containError(valuationUtil);
							return allOk && publishedOK;
						} else {
							return allOk;
						}
					}
				} else {
					task.setComment("No Measures Selected for TRADE VALUATION: " + this);
					return false;
				}
			}
		}
	}


	/**
	 * @param valuationUtil
	 */
	private void preloadAndValidateContracts(ValuationUtil valuationUtil){
		ArrayList<String> errorMsgs = new ArrayList<String>();
		List<Trade> finalTrades = new ArrayList<>();
		final List<Trade> trades = valuationUtil.getTrades();
		for(Trade trade : trades){
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
			if(null!=marginCallConfig){
				if("REAL_SETTLEMENT".equalsIgnoreCase(marginCallConfig.getEffDateType())){
					JDate today = this.getValuationDatetime2(this.getCurrentDate()).getJDate(TimeZone.getDefault());
					JDate settleDate = Optional.ofNullable(trade.getSettleDate()).map(settle -> settle.addBusinessDays(-1,Util.string2Vector("SYSTEM"))).orElse(null);
					JDate maturityDate = Optional.ofNullable(getMaturityDate(((Repo) trade.getProduct()))).map(mat -> mat.addBusinessDays(-1, Util.string2Vector("SYSTEM"))).orElse(null);
					if(null!=settleDate && today.gte(settleDate) && null!=maturityDate && today.lte(maturityDate)){
						trade.removeKeyword("MARGIN_CALL_CONFIG_ID");
					}
				}
				contracts.put(trade.getLongId(),marginCallConfig);
				finalTrades.add(trade);
			} else {
				Log.error(this, "No Margin Call Config found. Ignoring Trade " + trade.getLongId());
			}
		}
		valuationUtil.setTrades(new TradeArray(finalTrades));
	}

	private boolean isInternal(Trade trade){
		return Optional.ofNullable(trade.getMirrorBook()).isPresent();
	}
	private JDate getMaturityDate(Repo repo){
		JDate endDate = null;
		if(null!=repo){
			if (!repo.getMaturityType().equalsIgnoreCase("OPEN")) {
				endDate = repo.getEndDate();
			} else if (repo.getSecurity() instanceof Bond) {
				Bond bond = (Bond) repo.getSecurity();
				endDate = bond.getEndDate();
			}
		}
		return endDate;
	}

	void addException(String message, Trade trade, TaskArray tasks) {
		Task task = new Task();
		task.setObjectLongId((long)this.getId());
		task.setEventClass("Exception");
		task.setNewDatetime(this.getValuationDatetime2(this.getCurrentDate()));
		task.setUnderProcessingDatetime(this.getDatetime());
		task.setUndoTradeDatetime(this.getUndoDatetime());
		task.setDatetime(this.getDatetime());
		task.setPriority(1);
		task.setId(0L);
		task.setStatus(0);
		task.setSource(this.getType());
		task.setAttribute("ScheduledTask Id=" + String.valueOf(this.getId()));
		task.setComment(this.toString() + " " + trade.getLongId() + " " + trade.getProduct().getType() + " " + message);
		task.setEventType("EX_" + this.getExceptionString());
		tasks.add(task);
	}
}
