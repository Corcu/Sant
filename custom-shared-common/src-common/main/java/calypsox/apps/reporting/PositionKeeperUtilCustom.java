package calypsox.apps.reporting;

import calypsox.tk.report.PositionKeeperReportTemplate;
import com.calypso.apps.reporting.PositionConfigTabs;
import com.calypso.apps.reporting.PositionKeeperUtil;
import com.calypso.engine.position.LiquidationUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.mo.*;
import com.calypso.tk.mo.PositionAggregation;
import com.calypso.tk.mo.PLPosition.PLPositionKey;
import com.calypso.tk.mo.TradeOpenQuantity.LiquidableStatus;
import com.calypso.tk.mo.liquidation.loader.DefaultLiquidatedPositionLoader;
import com.calypso.tk.mo.liquidation.loader.LiquidatedPositionCriteriaBuilder;
import com.calypso.tk.mo.liquidation.loader.PLPositionAggregationParameter;
import com.calypso.tk.mo.liquidation.openquantity.loader.TradeOpenQuantityCriteriaBuilder;
import com.calypso.tk.mo.liquidation.plposition.loader.PLPositionCriteriaBuilder;
import com.calypso.tk.mo.liquidation.plposition.loader.PLPositionLoader;
import com.calypso.tk.mo.liquidation.rebuild.PLPositionRebuildContext;
import com.calypso.tk.mo.liquidation.rebuild.PLPositionRebuildOption;
import com.calypso.tk.mo.liquidation.rebuild.PLPositionRebuildResult;
import com.calypso.tk.product.*;
import com.calypso.tk.product.commodities.CommodityUtil;
import com.calypso.tk.product.util.PortfolioSwapUtil;
import com.calypso.tk.refdata.*;
import com.calypso.tk.report.PosAggregationFilterDescriptor;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.liqposition.LiquidationAggregationFilterDescriptor;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.RemoteProduct;
import com.calypso.tk.util.*;
import com.calypso.tk.util.LogHelper.Monitor;

import java.rmi.RemoteException;
import java.util.*;


/**
 * 
 * Based on PositionKeeperJFrame
 * removed link to awt class to be able to launch it from Scheduled tasks.
 * 
 * @author CedricAllain
 *
 */

@SuppressWarnings({ "rawtypes", "unchecked","deprecation", "unused" })
public class PositionKeeperUtilCustom {

	static final String ANY_BOOK_NAME = "_ANY_";
	static final int COL_ATTRIBUTE = 0;
	static final int COL_PRODUCT_ID = 1;
	static final int COL_LIQ_CONFIG = 63;

	public ArrayList<HashMap<String, Object>> result = new ArrayList<HashMap<String, Object>>();

	public void initDomains() {
		
		try {
			try {
				Vector v = AccessUtil.getAllNames(User.TRADE_FILTER);
				v = AccessUtil.getAllNames(User.BOOK_HIERARCHY);
				if (v == null)
					v = new Vector();
				v.insertElementAt("", 0);
				v = DSConnection.getDefault().getRemoteReferenceData().getBookAttributeNames();
				v.addElement(BookAttribute.LEGAL_ENTITY);
				v.addElement(BookAttribute.BOOK_NAME);
				v.addElement(BookAttribute.ACTIVITY);
				v.addElement(BookAttribute.LOCATION);
				v.addElement(BookAttribute.ACCOUNTING_BOOK);
				// AppUtil.set(aggregateChoice, v, true, null);
				// aggregateChoice.setSelectedItem(BookAttribute.BOOK_NAME);
				UserDefaults ud = DSConnection.getDefault().getUserDefaults();
				if (ud != null) {
					if (ud.getBookHierarchyName() != null)
						orgChoice = ud.getBookHierarchyName();
				}
			} catch (Exception e) {
				Log.error(Log.GUI, e);
			}
			String name = orgChoice;
			_orgStructure = null;
			if (name != null && name.length() > 0) {
				try {
					_orgStructure = DSConnection.getDefault().getRemoteReferenceData().getBookHierarchy(name);
				} catch (Exception e) {
					Log.error(this, e);
				}
			}

			setTolerance();
			// load user config
			loadTabs();
		} catch (Exception e) {
			Log.error(this, e);
		}

	}

	public void newPLPosition(PLPosition position) {
		if (_portfolio == null)
			return;
		if (_product != null) {
			if (position.getProductId() != _product.getId())
				return;
		}
		if (!_portfolio.accept(position))
			return;
		/*
		 * * Refresh Open Positions
		 */
		TradeOpenQuantityArray openPositions = null;
		try {
			TradeOpenQuantityCriteriaBuilder criteria = TradeOpenQuantityCriteriaBuilder.create().status()
					.ne(LiquidableStatus.Canceled)
					.posKey(new PLPositionKey(position.getProductId(), position.getBookId(),
							position.getLiquidationConfig(), position.getPositionAggregationId()));
			openPositions = DSConnection.getDefault().getRemoteLiquidation().getTradeOpenQuantity(criteria);
		} catch (Exception e) {
			Log.error(this, e);
		}
		if (position.getLiquidationInfo() == null) {
			LiquidationInfo liqInfo = BOCache.getLiquidationInfo(DSConnection.getDefault(), position);
			if (liqInfo != null)
				position.setLiquidationInfo(liqInfo);
		}
		position.setOpenPositions(openPositions);
		position.setAsOfDate(getDatetime());
		position.setAveragePrice(position.computeAveragePrice());
		if (includeFee) {
			if (position.isFeePosition()) {
				PLPosition parentPosition = new PLPosition();
				parentPosition.setBookId(position.getBookId());
				parentPosition.setPositionAggregationId(position.getPositionAggregationId());
				parentPosition.setLiquidationConfig(position.getLiquidationConfig());
				CA ca = (CA) position.getProduct();
				parentPosition.setProduct(ca.getUnderlying());
				int index = _allPLPositions.indexOf(parentPosition);
				if (index < 0) {
					// _allPLPositions.add(position);
					return;
				} else {
					parentPosition = _allPLPositions.elementAt(index);
					parentPosition.addFeePosition(position);
					position = parentPosition;
				}
			}
		}
		// (re)load and set liquidation array on new position
		try {
			LiquidatedPositionArray liqArray = PLPositionUtil.loadLiqPos(position, getDatetime(), null,
					needsLiquidatedPositions(), posBySettleDate);
			position.setLiqArray(liqArray);
		} catch (Exception e) {
			Log.error(this, e);
		}
		int index = _allPLPositions.indexOf(position);
		if (index < 0)
			_allPLPositions.add(position);
		else
			_allPLPositions.set(index, position);
		if (!accept(position)) {
			// remove rejected position if necessary
			removePLPosition(position, true, -1);
			return;
		}
		index = _PLPositions.indexOf(position);
		if (index < 0) {
			index = _PLPositions.size();
			_PLPositions.add(position);
		} else
			_PLPositions.set(index, position);
		Vector tabs = new Vector();
		PLPositionArray positions = addAggregatedPositions(position, tabs);
		JDatetime datetime = getDatetime();
		if (datetime == null)
			datetime = new JDatetime();
		JDate date = datetime.getJDate();
		PricingEnv env = getPricingEnv();
		addPLPosition(tabs, positions, -1, datetime, date, env, true);
	}

	/**
	 * @param p         the position to build by aggregation
	 * @param positions
	 * @param env
	 * @param datetime
	 * @param pdate
	 * @param previous  - whether we are dealing with today's position or the
	 *                  previous day position.
	 * @return aggregated measures
	 */
	private double[] calculatePLPositionValues(PLPosition p, PLPositionArray positions, PricingEnv env,
			JDatetime datetime, JDate pdate, boolean previous) {
		double unreal = 0.;
		double unrealClean = 0.;
		double pl = 0.;
		double amount = 0.;
		double globalPos = 0.;
		int size = positions.size();
		boolean stateOk = (!previous) || (_previousPLPositionsHash != null);
		if (stateOk) {
			for (int i = 0; i < size; i++) {
				PLPosition tmpPos = positions.elementAt(i);
				if (previous) {
					tmpPos = (PLPosition) _previousPLPositionsHash.get(tmpPos);
				}
				if (tmpPos == null)
					continue;
				p.setRealized(p.getRealized() + tmpPos.getRealized());
				p.setBookId(tmpPos.getBookId());
				p.setProduct(tmpPos.getProduct());
				globalPos += tmpPos.getAmount(datetime);
				if (tmpPos.getOpenPositions() != null) {
					int tmpsize = tmpPos.getOpenPositions().size();
					for (int j = 0; j < tmpsize; j++) {
						p.getOpenPositions().add(tmpPos.getOpenPositions().elementAt(j));
					}
				}
				if (tmpPos.getLiqArray() != null) {
					int tmpsize = tmpPos.getLiqArray().size();
					for (int j = 0; j < tmpsize; j++) {
						p.getLiqArray().add(tmpPos.getLiqArray().elementAt(j));
					}
				}
				if (tmpPos.getQuantity() != 0.) {
//					double avp = (tmpPos.getAveragePrice() * Math.abs(tmpPos.getQuantity()));
//					double oldavp = (p.getAveragePrice() * Math.abs(p.getQuantity()));
					double avp = tmpPos.getAveragePrice() * tmpPos.getQuantity();
					double oldavp = p.getAveragePrice() * p.getQuantity();
					double totalQuantity = tmpPos.getQuantity() + p.getQuantity();
					double avgPrice = Double.NaN;
					if (totalQuantity != 0.) {
						avgPrice = (avp + oldavp) / totalQuantity;
					}
					p.setAveragePrice(avgPrice);
					p.setQuantity(p.getQuantity() + tmpPos.getQuantity());
				}
				if (tmpPos.getFeePositions() != null) {
					int tmpSize = tmpPos.getFeePositions().size();
					for (int j = 0; j < tmpSize; j++) {
						p.addFeePosition(tmpPos.getFeePositions().elementAt(j));
					}
				}
				p.setTotalLiqBuyQuantity(tmpPos.getTotalLiqBuyQuantity() + p.getTotalLiqBuyQuantity());
				p.setTotalLiqSellQuantity(tmpPos.getTotalLiqSellQuantity() + p.getTotalLiqSellQuantity());
				p.setTotalLiqBuyAmount(tmpPos.getTotalLiqBuyAmount() + p.getTotalLiqBuyAmount());
				p.setTotalLiqSellAmount(tmpPos.getTotalLiqSellAmount() + p.getTotalLiqSellAmount());
				p.setTotalLiqBuyAccrual(tmpPos.getTotalLiqBuyAccrual() + p.getTotalLiqBuyAccrual());
				p.setTotalLiqSellAccrual(tmpPos.getTotalLiqSellAccrual() + p.getTotalLiqSellAccrual());
				p.setTotalInterest(tmpPos.getTotalInterest() + p.getTotalInterest());
				p.setTotalPrincipal(tmpPos.getTotalPrincipal() + p.getTotalPrincipal());
				amount += tmpPos.computeAmount(datetime);
				double currentUnreal = 0;
				try {
					if (previous) {
						Double unReal = (Double) _previousPL.get(tmpPos);
						currentUnreal = unReal.doubleValue();
					} else {
						Product pr = tmpPos.getProduct();
						if (pr != null && Util.isEqualStrings(CommodityUtil.getPLPositionProductType(tmpPos),
								CommodityForward.COMMODITY_FORWARD)) {
							currentUnreal = CommodityUtil.computeUnrealize(tmpPos, env, datetime);
						} else {
							currentUnreal = tmpPos.getUnrealized(env, datetime);
						}
					}
					if (p.getSecurity() instanceof Bond) {
						unrealClean = tmpPos.getUnrealized(env, datetime, null, true);
					}
				} catch (Exception e) {
					Log.error(this, e);
				}
				if (p.getProduct() instanceof PortfolioSwapPosition) {
					unreal = PortfolioSwapUtil.computeUnrealize(tmpPos, env, datetime);
				} else {
					unreal += currentUnreal;
				}
				pl += (tmpPos.getRealized() + unreal);
			}
		}
		double principal = p.getProduct().getPrincipal(pdate);
		amount = amount * principal;
		double[] result = new double[6];
		result[0] = unreal;
		result[1] = pl;
		result[2] = amount;
		result[3] = globalPos;
		result[4] = principal;
		result[5] = unrealClean;
		return result;
	}

	// This should be optimized...
	// We should compute it ONCE !
	private boolean includePreviousPL() {
		return _includePreviousPL;
	}

	private void setIncludePreviousPL() {
		Vector cols = getColumnNamesUsed();
		_includePreviousPL = false;
		_useOptimizeLoad = true;
		for (int i = 0; i < cols.size(); i++) {
			String colName = (String) cols.elementAt(i);
			if (colName.indexOf(PositionKeeperUtil.REPOED_POSITION) != -1) {
				_useOptimizeLoad = false;
				break;
			}
		}
		for (int i = 0; i < cols.size(); i++) {
			String colName = (String) cols.elementAt(i);
			if (colName.indexOf("Dly ") != -1) {
				_includePreviousPL = true;
				break;
			}
		}
	}

	private static PLPosition createAggregatedPLPosition(PLPositionArray positions, JDatetime startDate) {
		PLPosition pos = positions.elementAt(0);
		PLPosition p = new PLPosition();
		p.setProduct(pos.getProduct());
		p.setBookId(pos.getBookId());
		p.setPositionAggregationId(pos.getPositionAggregationId());
		p.setPositionLongId(pos.getPositionLongId());
		p.setVersion(pos.getVersion());
		p.setLiquidationConfig(pos.getLiquidationConfig());
		// setting the toqs, etc require a valid key on the position, so we do this last
		// otherwise the key will be invalid
		p.setOpenPositions(new TradeOpenQuantityArray());
		p.setLiqArray(new LiquidatedPositionArray());
		if (LiquidationUtil.isTDWACBasedLiquidation(pos)) {
			// copy transient marks and measures
			List<PLMark> posMarks = pos.getPLMarks();
			if (posMarks != null) {
				List<PLMark> markList = new ArrayList<PLMark>(posMarks.size());
				for (PLMark mark : posMarks) {
					if (mark == null)
						continue;
					try {
						markList.add((PLMark) mark.clone());
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
				}
				p.setPLMarks(markList);
			}
			Map<String, Double> posWacMeasures = pos.getWACMeasures();
			if (posWacMeasures != null) {
				Map<String, Double> wacMeasures = new HashMap<String, Double>();
				for (String key : posWacMeasures.keySet()) {
					wacMeasures.put(new String(key), new Double(posWacMeasures.get(key)));
				}
				p.setWACMeasures(wacMeasures);
			}
		}
		p.setStartDate(startDate);
		return p;
	}
	

	private boolean addPLPosition(Vector tabs, PLPositionArray positions, int index, JDatetime datetime, JDate pdate,
			PricingEnv env, boolean refresh) {
		JDatetime prevDatetime = getPreviousDateTime(datetime);
		if (tabs.size() == 0)
			return false;
		// CAL-272064 - for some columns we will call the position pricer, and for that
		// we need to ensure the position inception date is initialized.
		JDatetime minStartDate = null;
		for (int i = 0; i < positions.size(); i++) {
			PLPosition pos = positions.get(i);
			JDatetime startDate = pos.getStartDate();
			if (minStartDate == null || minStartDate.after(startDate)) {
				minStartDate = startDate;
			}
		}
		PLPosition pos = positions.elementAt(0);
		PLPosition p = createAggregatedPLPosition(positions, minStartDate);
		try {
			String ccy = p.getProduct().getCurrency();
			int rU = CurrencyUtil.getRoundingUnit(ccy);
			double[] newPLValues = null;
			LogHelper.getCurrentMonitor().resumeTask("calculate PLPositionValues");
			try {
				// double quantity = 0.;
				newPLValues = calculatePLPositionValues(p, positions, env, datetime, pdate, false);
			} finally {
				LogHelper.getCurrentMonitor().pauseTask("calculate PLPositionValues");
			}
			// check if this is a zero aggregated position
			if (isZeroPositionForRemoval(p, true)) {
				removePLPosition(pos, false, index);
				return false;
			}
			double unreal = newPLValues[0];
			double pl = newPLValues[1];
			double amount = newPLValues[2];
			double globalPos = newPLValues[3];
			double principal = newPLValues[4];
			double unrealClean = newPLValues[5];
			double unrealPrevious = .0;
			double plPrevious = .0;
			double unrealCleanPrevious = .0;
			// if calculate daily PL
			PLPosition previousPos = null;
			if (includePreviousPL()) {
				LogHelper.getCurrentMonitor().resumeTask("calculate Previous PLPositionValues");
				try {
					previousPos = createAggregatedPLPosition(positions, minStartDate);
					// to rebuild the previous day position, needed for daily fee type values
					double[] previousPLValues = calculatePLPositionValues(previousPos, positions, env, datetime, pdate,
							true);
					unrealPrevious = previousPLValues[0];
					plPrevious = previousPLValues[1];
					unrealCleanPrevious = previousPLValues[5];
				} catch (Exception e) {
					Log.error(this, e);
				} finally {
					LogHelper.getCurrentMonitor().pauseTask("calculate Previous PLPositionValues");
				}
			}
			// calculate pricer measures
			Pricer pricer;
			PricerMeasure[] a = new PricerMeasure[2];
			a[0] = new PricerMeasure(PricerMeasure.DIRTY_PRICE);
			a[1] = new PricerMeasure(PricerMeasure.YIELD);
			Trade trade = p.toTrade();
			JDatetime positionTime = datetime;
			JDate date = pdate;
			trade.setTradeDate(positionTime);
			JDate spotDate = SpotDateCalculatorUtil.getSpotDate(trade, positionTime);
			trade.setSettleDate(spotDate);
			double value = 0;
			double value2 = 0;
			pricer = env.getPricerConfig().getPricerInstance(trade.getProduct());
			try {
				if (pricer != null)
					pricer.price(trade, positionTime, env, a);
				else
					Log.warn(Log.WARN, "Pricer not found for : " + trade.getProduct().getType());
				value = a[0].getValue();
				value2 = a[1].getValue();
			} catch (Exception e) {
			}
			double mktPrice = value;
			double yield = value2;
			double repoedPosition = p.getRepoedPosition(positionTime);
			repoedPosition *= principal;
			String posAttr = getBook(pos.getBookId()).getAttribute(_attributeName);
			// Bug#: 11032
			int rUBaseCcy = CurrencyUtil.getRoundingUnit(env.getBaseCurrency());
			double plBaseCcy = convertCurrency(pl, ccy, env, pdate, p);
			int quoteUsage = env.getParameters().getQuoteUsage(trade.getProductType());
			int instanceToUse = env.getInstance();
			for (int k = 0; k < tabs.size(); k++) {
				TabPLPosition ppl = (TabPLPosition) tabs.elementAt(k);
				if (ppl._name.equals(this.tabName)) {
					Vector cols = ppl._plUtil.getColumnNames();
					QuoteSet set = env.getQuoteSet();
					String setName = env.getQuoteSetName();
					String quoteName = trade.getProduct().getQuoteName();
					double quoteValue;
					QuoteValue qValue = null;
					if (quoteName != null) {
						qValue = set.getQuote(new QuoteValue(setName, quoteName, pdate, null));
						if (p.getProduct() != null && Util.isEqualStrings(CommodityUtil.getPLPositionProductType(p),
								CommodityForward.COMMODITY_FORWARD)) {
							try {
								mktPrice = CommodityUtil.getCommodityForwardLocationAdjMktPrice(p, env, pdate, pdate);
							} catch (PricerException pe) {
								Log.error("PositionKeeperJFrame", pe);
								mktPrice = 0.0;
							}
						}
					}
					// BZ: 19669
					if (qValue != null) {
						quoteValue = (quoteUsage >= 0) ? qValue.getValue(quoteUsage)
								: qValue.getInstanceSide(instanceToUse, QuoteSet.MID);
					} else
						quoteValue = 0;
					// BZ: 19669
					try {
						LogHelper.getCurrentMonitor().resumeTask("PositionKeeperUtil.getValueAt");
						// int rowIndex = getRowIndex(index, pos, model);
						// if (rowIndex < 0)
						// return false;
						// model.putClientProperty(rowIndex, PL_POSITION_KEY, p);
						// model.putClientProperty(rowIndex, PL_POSITION_AGG_ATTRIBUTE, posAttr);

						HashMap<String, Object> rowValues = new HashMap<String, Object>();

						for (int i = 0; i < cols.size(); i++) {
							rowValues.put("" + cols.get(i),
									ppl._plUtil.getValueAt(index, i, p, previousPos, posAttr, rU, principal, amount,
											mktPrice, unreal, unrealPrevious, pl, plPrevious, yield, globalPos,
											repoedPosition, rUBaseCcy, plBaseCcy, quoteValue, unrealCleanPrevious,
											unrealClean, datetime, prevDatetime, env));
							// model.setValueNoCheck(rowIndex, i, ppl._plUtil.getValueAt(index, i, p,
							// previousPos, posAttr, rU, principal, amount, mktPrice, unreal,
							// unrealPrevious, pl, plPrevious, yield, globalPos, repoedPosition, rUBaseCcy,
							// plBaseCcy, quoteValue, unrealCleanPrevious, unrealClean, datetime,
							// prevDatetime, env));
						}
						result.add(rowValues);
					} catch (Exception x) {
						Log.error(Log.GUI, x);
					} finally {
						LogHelper.getCurrentMonitor().pauseTask("PositionKeeperUtil.getValueAt");
					}

				}
			}
		} catch (Exception e) {
			Log.error(Log.GUI, e);
		}
		// TableUtil.adjust(positionTable);
		return true;
	}

	
/*	class FXCache {
		
		HashMap<PricingEnv,HashMap<CurrencyPair,QuoteValue>> fxCache = new HashMap<PricingEnv,HashMap<CurrencyPair,QuoteValue>>();
		
		
		protected QuoteValue getFXQuote(PricingEnv env, String ccy, String baseCurrency, JDate valDate) {
			HashMap<CurrencyPair,QuoteValue> prices = fxCache.get(env);
			
			CurrencyPair ccyPair = null;
			try {
				ccyPair = CurrencyUtil.getCurrencyPair(ccy, baseCurrency, valDate);
			} catch (MarketDataException e1) {
				Log.error(this, e1);
			}

			
			if(prices==null) {
				prices = new HashMap<CurrencyPair,QuoteValue>();
				fxCache.put(env, prices);
			}
			
			QuoteValue quote = prices.get(ccyPair);
			if(quote!=null)
				return quote;
			
			try {
				QuoteValue qv = env.getFXQuote(ccy, baseCurrency, valDate);
				prices.put(ccyPair, qv);
				return qv;
				
			} catch (MarketDataException e) {
				Log.error(this, e);
			}
			
			return null;
		}
	
	}*/
	
	// Bug#: 11032
	protected double convertCurrency(double value, String ccy, PricingEnv env, JDate valDate, PLPosition plPos) {
		String baseCurrency = env.getBaseCurrency();
		if (ccy.equals(baseCurrency))
			return value;
		try {
			//QuoteValue qv = fxCache.getFXQuote(env, ccy, baseCurrency, valDate);
			QuoteValue qv = env.getFXQuote(ccy, baseCurrency, valDate);
			if (qv != null) {
				if (plPos != null && Util.isEqualStrings(CommodityUtil.getPLPositionProductType(plPos),
						CommodityForward.COMMODITY_FORWARD)) {
					return (value * CommodityUtil.fxAdjustPrice(plPos, env, valDate, valDate, qv, baseCurrency));
				}
				return (value * qv.getMid(env.getParameters().getInstance()));
			} else {
				Log.error("PositionKeeperJFrame",
						("Can't find FX Rate " + "for " + baseCurrency + "/" + ccy + " on " + valDate));
				return 0;
			}
		} catch (Exception ex) {
			Log.error("PositionKeeperJFrame", ex);
		}
		return 0;
	}


	protected void initializeFromPLPositionUtil(JDatetime datetime) throws Exception {

		/*
		 * * Rebuild PLPositions in the Past
		 */
		Monitor monitor = LogHelper.startWithCategory("PositionKeeper", "Loading Products");
		monitor.done();
		_portfolio.setValDate(datetime);
		PLPositionCriteriaBuilder criteria = PLPositionCriteriaBuilder.create(_portfolio);
		monitor = LogHelper.startWithCategory("PositionKeeper", "Loading PLPositions");
		_allPLPositions = DSConnection.getDefault().getRemoteLiquidation().getPLPositionsAndProducts(criteria).get();
		monitor.done(_allPLPositions.size() + " positions loaded.");
		if (isRealTimeChangeCheck) {
			monitor = LogHelper.startWithCategory("PositionKeeper", "Loading TradeOpenQuantity");
			loadAndSetOpenPositions(_allPLPositions, _portfolio);
			monitor.done();
			WACMarkCache.loadMarksAndEnableCache(_allPLPositions, datetime);
			if (includeFee) {
				PLPositionUtil.mergePositionsWithFees(_allPLPositions);
			}
			for (int i = 0; i < _allPLPositions.size(); i++) {
				PLPosition plpos = _allPLPositions.get(i);
				if (plpos.getLiquidationInfo() == null) {
					LiquidationInfo liqInfo = BOCache.getLiquidationInfo(DSConnection.getDefault(), plpos);
					if (liqInfo != null)
						plpos.setLiquidationInfo(liqInfo);
				}
				plpos.setAsOfDate(datetime);
				plpos.setAveragePrice(plpos.computeAveragePrice());
			}
			// load and set liquidated positions as they're needed to calculate some columns
			// (accruedRealized, cleanRealized, etc.)
			if (needsLiquidatedPositions()) {
				monitor = LogHelper.startWithCategory("PositionKeeper", "Loading LiquidatedPosition");
				loadAllLiquidatedPositions(_allPLPositions, datetime, _portfolio);
				monitor.done();
			}
			// TDWAC always requires a rebuild (if any prem disc measures needed)
			for (int i = 0; i < _allPLPositions.size(); i++) {
				PLPosition plpos = _allPLPositions.get(i);
				if (LiquidationUtil.isTDWACBasedLiquidation(plpos)) {
					// rebuild to trigger measure derivation
					PLPositionArray positionArray = new PLPositionArray();
					positionArray.add(plpos);
					positionArray.setFees(false);
					PLPositionUtil.rebuildPLPositions(positionArray, datetime, false, null, false);
				}
			}
			if (includePreviousPL()) {
				monitor = LogHelper.startWithCategory("PositionKeeper", "Rebuilding Previous");
				PLPositionArray prevPL = null;
				try {
					prevPL = (PLPositionArray) _allPLPositions.clone();
					prevPL.setFees(false);
				} catch (Exception e) {
					Log.error(this, e);
				}
				PLPositionUtil.rebuildPLPositions(prevPL, getPreviousDateTime(datetime), posBySettleDate, null, false);
				if (includeFee) {
					PLPositionUtil.mergePositionsWithFees(prevPL);
				}
				_previousPLPositions = prevPL;
				_previousPLPositionsHash = Util.toHashtable(prevPL.toVector());
				monitor.done();
				monitor = LogHelper.startWithCategory("PositionKeeper", "Computing Previous Values");
				computePreviousValues(_previousPLPositions, _previousPL, getPreviousEnv(),
						getPreviousDateTime(datetime));
				monitor.done();
			}
		} else {
			// Not Real time, we need to call the PLPositionRebuild, which will take care of
			// loading the PLPosition and LiquidatedPosition
			monitor = LogHelper.startWithCategory("PositionKeeper", "Rebuild PLPosition");
			Vector v = new Vector();
			JDatetime dtprv = getPreviousDateTime(datetime);
			if (includePreviousPL())
				v.addElement(dtprv);
			v.addElement(datetime);
			_allPLPositions.setFees(false);
			Map<JDatetime, PLPositionArray> hash = PLPositionUtil.buildPositionByDates(_allPLPositions, v,
					(_useOptimizeLoad && !includeFee), false, posBySettleDate);
			monitor.done();
			_allPLPositions = hash.get(datetime);
			if (_allPLPositions != null) {
				if (includeFee) {
					PLPositionUtil.mergePositionsWithFees(_allPLPositions);
				}
				if (includePreviousPL()) {
					_previousPLPositions = hash.get(dtprv);
					if (includeFee) {
						PLPositionUtil.mergePositionsWithFees(_previousPLPositions);
					}
					_previousPLPositionsHash = Util.toHashtable(_previousPLPositions.toVector());
					monitor = LogHelper.startWithCategory("PositionKeeper", "Computing Previous Values");
					computePreviousValues(_previousPLPositions, _previousPL, getPreviousEnv(), dtprv);
					monitor.done();
				}
			}
		}
	}

	protected void initializeFromSnapshot(JDatetime datetime) throws Exception {
		LiquidationConfig liqConfig = LiquidationUtil.getLiquidationConfig(_portfolio);
		OptionUtil<PLPositionRebuildOption> rebuildOptions = OptionUtil.get(PLPositionRebuildOption.class)
				.addIf(PLPositionRebuildOption.SettleDatePosition, posBySettleDate)
				.addIf(PLPositionRebuildOption.LoadOnly, isRealTimeChangeCheck)
				.addIf(PLPositionRebuildOption.InMemoryLiquidation, liqConfig.isSimulationSupport())
				.addIf(PLPositionRebuildOption.MergeFees, includeFee)
				.addIf(PLPositionRebuildOption.LoadAllLiquidatedPositions, needsLiquidatedPositions())
				.addIf(PLPositionRebuildOption.GetRepoedPositions, needsRepoedPositions())
				.addIf(PLPositionRebuildOption.AsOf, liqConfig.isUsingSnapshots() && !isRealTimeChangeCheck);
		JDatetime previousPositionDate = getPreviousDateTime(datetime);
		PLPositionRebuildContext positionContext = PLPositionRebuildContext.fromTradeFilter(_portfolio, datetime,
				rebuildOptions.build());
		PLPositionRebuildContext prevPositionContext = null;
		if (includePreviousPL()) {
			OptionUtil<PLPositionRebuildOption> prevRebuildOptions = OptionUtil.get(PLPositionRebuildOption.class)
					.addIf(PLPositionRebuildOption.SettleDatePosition, posBySettleDate)
					.addIf(PLPositionRebuildOption.MergeFees, includeFee)
					.addIf(PLPositionRebuildOption.LoadAllLiquidatedPositions, needsLiquidatedPositions())
					.addIf(PLPositionRebuildOption.GetRepoedPositions, needsRepoedPositions())
					.addIf(PLPositionRebuildOption.AsOf, liqConfig.isUsingSnapshots());
			// Need to create another Context for the rebuild.
			prevPositionContext = PLPositionRebuildContext.fromTradeFilter(_portfolio, previousPositionDate,
					prevRebuildOptions.build());
		}
		/*
		 * * Rebuild PLPositions in the Past
		 */
		Monitor monitor = LogHelper.startWithCategory("PositionKeeper", "Loading Products");
		monitor.done();
		// Request rebuild position based on the positionContext.
		monitor = LogHelper.startWithCategory("PositionKeeper", "Loading Snapshot Positions");
		PLPositionRebuildResult result = PLPositionLoader.loadAndRebuildPLPosition(positionContext);
		monitor.done();
		if (_allPLPositions == null)
			_allPLPositions = new PLPositionArray();
		if (includePreviousPL()) {
			monitor = LogHelper.startWithCategory("PositionKeeper", "Loading Previous Snapshot Positions");
			result.add(previousPositionDate, PLPositionLoader.loadAndRebuildPLPosition(prevPositionContext));
			monitor.done();
		}
		if (isRealTimeChangeCheck) {
			_allPLPositions = result.getPLPositionsOn(datetime);
			if (includePreviousPL()) {
				_previousPLPositions = result.getPLPositionsOn(previousPositionDate);
				_previousPLPositionsHash = Util.toHashtable(_previousPLPositions.toVector());
				computePreviousValues(_previousPLPositions, _previousPL, getPreviousEnv(), previousPositionDate);
			}
		} else {
			_allPLPositions = result.getPLPositionsOn(datetime);
			if (_allPLPositions != null) {
				if (includePreviousPL()) {
					_previousPLPositions = result.getPLPositionsOn(previousPositionDate);
					_previousPLPositionsHash = Util.toHashtable(_previousPLPositions.toVector());
					computePreviousValues(_previousPLPositions, _previousPL, getPreviousEnv(), previousPositionDate);
				}
			}
		}
	}

	protected boolean needsLiquidatedPositions() {
		// load and set liquidated positions as they're needed to calculate some columns
		// (accruedRealized, cleanRealized, etc.)
		return true;
	}

	protected boolean needsRepoedPositions() {
		return !_useOptimizeLoad;
	}

	/**
	 * @return a Modified Trade filter which includes Fees information and
	 *         PositionSpec according to the AggDesc selected.
	 */
	private TradeFilter getUpdatedTradeFilter() {
		LogErrorReporter reporter = new LogErrorReporter();
		LiquidationAggregationFilterDescriptor liqAggDesc = (LiquidationAggregationFilterDescriptor) posAggregationFilterDescriptor;
		TradeFilter tf = PLPositionAggregationParameter.copyForPLPositionLoading(_portfolio, liqAggDesc, reporter);
		if (_product != null) {
			tf.addCriterion(TradeFilter.PRODUCT_ID, Integer.toString(_product.getId()));
		}
		if (includeFee) {
			LiquidationUtil.addPLFeeProductCriteria(tf, _product, reporter);
		}
		return tf;
	}


	protected void filter(PLPositionArray positions) {
		if (positions == null) {
			Log.error(this, "No Positions");
			return;
		}
		_PLPositions = new PLPositionArray();
		int size = positions.size();
		for (int i = 0; i < size; i++) {
			PLPosition position = positions.elementAt(i);
			if (accept(position))
				_PLPositions.add(position);
		}
		showPLPositions(getPLPositions());
	}

	PLPositionArray getPLPositions() {
		return _PLPositions;
	}

	PLPositionArray getAllPLPositions() {
		return _allPLPositions;
	}

	JDatetime getDatetime() {
		return this.valDateTime;
	}

	protected JDatetime getPreviousDateTime(JDatetime datetime) {
		Vector holidays = DSConnection.getDefault().getUserDefaults().getHolidays();
		if (holidays == null) {
			holidays = new Vector();
		}
		TimeZone envTZ = _env.getTimeZone();
		JDate datePrevious = Holiday.getCurrent().addBusinessDays(datetime.getJDate(envTZ), holidays, -1, false);
		return new JDatetime(datePrevious, 23, 59, 0, 0, envTZ);
	}

	void setMarketConfig() {
		JDatetime datetime = getDatetime();
		if (datetime == null)
			return;
		JDate today = JDate.valueOf(datetime);
		try {
			JDatetime datetimePrevious = getPreviousDateTime(datetime);
		} catch (Exception e) {
			Log.error(Log.GUI, e);
		}
	}

	PricingEnv getPricingEnv() {
		JDatetime datetime = getDatetime();
		if (datetime == null)
			return null;
		JDate today = JDate.valueOf(datetime);
		if ((_env == null || _previousEnv == null) || !today.equals(_env.getDate()))
			setMarketConfig();
		return _env;
	}

	PricingEnv getPreviousEnv() {
		return _previousEnv;
	}



	public void showPLPositions(PLPositionArray v) {
		if (v == null)
			v = new PLPositionArray();
		buildAggregatedPLPositions(v);
		PricingEnv env = getPricingEnv();
		QuoteSet qs = null;
		if (env != null)
			qs = env.getQuoteSet();
		boolean saveQuoteSetCacheB = false;
		if (qs != null)
			saveQuoteSetCacheB = qs.getCacheNotFound();
		JDatetime datetime = getDatetime();
		JDate date = datetime.getJDate();
		try {
			if (qs != null)
				qs.setCacheNotFound(true);
			int size = _products.size();
			for (int i = 0; i < size; i++) {
				TabPLPosition ppl = (TabPLPosition) _products.elementAt(i);
				Hashtable aggregatedPLPositions = ppl._plPositions;
				Enumeration e = null;
				e = aggregatedPLPositions.elements();
				int count = 0;
				Vector tabs = new Vector();
				tabs.add(ppl);
				while (e.hasMoreElements()) {
					PLPositionArray aggPositions = (PLPositionArray) e.nextElement();
					if (addPLPosition(tabs, aggPositions, count, datetime, date, env, false) == true) {
						count++;
					}
				}

				// TableUtil.fastAdjust(model);
			}

		} finally {
			if (qs != null)
				qs.setCacheNotFound(saveQuoteSetCacheB);
		}
	}

	protected void buildAggregatedPLPositions(PLPositionArray positions) {
		_aggregatedPLPositions = new Hashtable();
		int size = positions.size();
		for (int i = 0; i < size; i++) {
			PLPosition position = positions.elementAt(i);
			addAggregatedPositions(position, null);
		}
		// filter for tab
		size = _products.size();
		for (int i = 0; i < size; i++) {
			TabPLPosition ppl = (TabPLPosition) _products.elementAt(i);
			ppl.buildAggregatedPLPositions(_aggregatedPLPositions);
		}
	}

	// Note that if tabs is null the aggregation of each TabPLPosition
	// must be done latter (see buildAggregatedPLPositions)
	protected PLPositionArray addAggregatedPositions(PLPosition position, Vector tabs) {
		if (_aggregatedPLPositions == null)
			return null;
		String key = getBook(position.getBookId()).getAttribute(_attributeName) + position.getProductId() + "_"
				+ position.getLiquidationConfig() + "_" + position.getPositionAggregationId();
		PLPositionArray aggPositions = _aggregatedPLPositions.get(key);
		if (aggPositions == null) {
			aggPositions = new PLPositionArray();
			aggPositions.add(position);
			_aggregatedPLPositions.put(key, aggPositions);
		} else {
			if (aggPositions.indexOf(position) >= 0) {
				aggPositions.remove(position);
			}
			aggPositions.add(position);
		}
		if (tabs != null) {
			// puts into tabs all the TabPLPosition that matches
			for (int i = 0; i < _products.size(); i++) {
				TabPLPosition ppl = (TabPLPosition) _products.elementAt(i);
				if (ppl.addAggregatedPositions(key, aggPositions, position))
					tabs.addElement(ppl);
			}
		}
		return aggPositions;
	}

	private void removePLPosition(PLPosition position, boolean fromPLPositions, int displayIndex) {
		// build position key
		String key = getBook(position.getBookId()).getAttribute(_attributeName) + position.getProductId() + "_"
				+ position.getLiquidationConfig() + "_" + position.getPositionAggregationId();
		// remove from _aggregatedPLPositions
		if (_aggregatedPLPositions != null) {
			PLPositionArray aggPositions = _aggregatedPLPositions.get(key);
			if ((aggPositions != null) && aggPositions.indexOf(position) >= 0) {
				_aggregatedPLPositions.remove(key);
			}
		}
		if (fromPLPositions) {
			// remove from _PLPositions
			if ((_PLPositions != null) && _PLPositions.indexOf(position) >= 0) {
				_PLPositions.remove(position);
			}
		}
		// remove from display
		/*
		 * int size = _products.size(); for (int i = 0; i < size; i++) { TabPLPosition
		 * ppl = (TabPLPosition) _products.elementAt(i); int rowIndex =
		 * getRowIndex(displayIndex, position, model); ppl.removeAggregatedPosition(key,
		 * rowIndex); }
		 */
	}

	protected int sign(double d) {
		if (d > 0)
			return 1;
		else
			return -1;
	}

	/*
	 * * List of Function to accept or reject the Positions based * on the hierarchy
	 * selected
	 */
	protected boolean accept(PLPosition position) {
		if (!_isAllBookAvailable) {
			Book bk = getBook(position.getBookId());
			if (bk != null && _availBooks.size() > 0) {
				if (_availBooks.get(bk.getName()) == null)
					return false;
			}
		}
		if (_selectedBookHierarchyNode != null)
			return _selectedBookHierarchyNode.fullAccept(getBook(position.getBookId()));
		return (!isZeroPositionForRemoval(position, false));
	}

	private boolean isZeroPositionForRemoval(PLPosition position, boolean checkOnlyIfAggregationOn) {
		// is option checked
		if (_excludeZeroPositions == 0) {
			return false;
		}
		// is the aggregation on versus do we care only if it's on
		if (checkOnlyIfAggregationOn == _attributeName.equals("BookName")) {
			return false;
		}
		boolean zeroPosition = false;
		switch (_excludeZeroPositions) {
		case 1:
			// exclude when 0 nominal AND 0 realized pl
			if (_zeroPositionTolerance != 0.0) {
				zeroPosition = (Math.abs(position.getQuantity()) <= _zeroPositionTolerance)
						&& (Math.abs(position.getRealized()) <= _zeroPositionTolerance);
			} else {
				zeroPosition = ((position.getQuantity() == 0.) && (position.getRealized() == 0.));
			}
			break;
		case 2:
			// exclude when 0 nominal
			if (_zeroPositionTolerance != 0.0) {
				zeroPosition = (Math.abs(position.getQuantity()) <= _zeroPositionTolerance);
			} else {
				zeroPosition = (position.getQuantity() == 0.);
			}
			break;
		}
		return zeroPosition;
	}

	private void setTolerance() {
		_zeroPositionTolerance = 0.;
	}

	Book getBook(int bid) {
		return BOCache.getBook(DSConnection.getDefault(), bid);
	}

	void setStaticDataFilter(TabPLPosition p, String name) {
		p._filterName = (name == null ? "" : name);
		p._staticDataFilter = null;
		if (p._filterName != null && p._filterName.length() > 0) {
			try {
				p._staticDataFilter = DSConnection.getDefault().getRemoteReferenceData()
						.getStaticDataFilter(p._filterName);
			} catch (Exception e) {
				Log.error(this, e);
			}
		}
	}

	// from PositionConfigTabs
	public void apply() {
		loadTabs();
	}

	public void showPositionKeeperConfigDialog() {
		if (_pkConfigDialog == null)
			_pkConfigDialog = new PositionConfigTabs();
		_pkConfigDialog.load(this, PK_NAME, getAllColumnNames());
		_pkConfigDialog.setVisible(true);
	}

	class TabPLPosition {

		public javax.swing.JScrollPane _scrollPane = null;

		public PositionKeeperUtil _plUtil = null;

		public Hashtable _plPositions = new Hashtable();

		// list of Integers
		Vector _bookIdList = new Vector();

		// list of Strings
		Vector _productTypeList = new Vector();

		// list of Strings
		Vector _contractList = new Vector();

		String _filterName = "";

		StaticDataFilter _staticDataFilter = null;

		String _name;

		int _nbFixedColumns = 0;

		boolean _openTrades = false;

		Vector _models = null;

		public TabPLPosition(String name, int rows, Vector columnNames, int nbFixedColumns, boolean openTrades) {
			_name = name;
			_plUtil = new PositionKeeperUtil();
			_plUtil.setColumnNames(columnNames);
			_nbFixedColumns = nbFixedColumns;
			_openTrades = openTrades;
			if (_nbFixedColumns >= columnNames.size())
				_nbFixedColumns = 0;
		}

		@Override
		public String toString() {
			return _name;
		}

		public Vector getBookIdList() {
			return _bookIdList;
		}

		public Vector getProductTypeList() {
			return _productTypeList;
		}

		public Vector getContractList() {
			return _contractList;
		}

		private boolean isPLPositionIn(PLPosition position) {
			// Check Book
			boolean ok = false;
			int bid = position.getBookId();
			for (int i = 0; i < _bookIdList.size(); i++) {
				if (bid == ((Integer) _bookIdList.elementAt(i)).intValue()) {
					ok = true;
					break;
				}
			}
			if (!ok)
				return false;
			// Check Product Type
			ok = false;
			String type = position.getProduct().getType();
			if (type == null)
				return false;
			for (int i = 0; i < _productTypeList.size(); i++) {
				if (type.equals(_productTypeList.elementAt(i))) {
					ok = true;
					break;
				}
			}
			if (!ok)
				return false;
			// check Contract Type
			if (_contractList != null && _contractList.size() > 0) {
				ok = false;
				Product product = position.getProduct();
				if (product instanceof Future) {
					String contract = ((Future) product).getName();
					for (int i = 0; i < _contractList.size(); i++) {
						if (contract.equals(_contractList.elementAt(i))) {
							ok = true;
							break;
						}
					}
					if (!ok)
						return false;
				} else if (product instanceof FutureOption) {
					FutureOption fo = (FutureOption) product;
					String contract = fo.getOptionContract().getUnderlying().getName();
					for (int i = 0; i < _contractList.size(); i++) {
						if (contract.equals(_contractList.elementAt(i))) {
							ok = true;
							break;
						}
					}
					if (!ok)
						return false;
				}
			}
			// check static data filter
			if (_staticDataFilter != null)
				// Need to be optimized...
				if (!_staticDataFilter.accept(position.toTrade()))
					return false;
			// check open position
			if (_openTrades) {
				TradeOpenQuantityArray array = position.getOpenPositions();
				if (array == null)
					return false;
				int size = 0;
				for (int kk = 0; kk < array.size(); kk++) {
					TradeOpenQuantity toq = array.get(kk);
					if (toq.getOpenQuantity() == 0)
						continue;
					size++;
				}
				if (size == 0)
					return false;
			}
			return true;
		}

		public void buildAggregatedPLPositions(Hashtable all) {
			_plPositions.clear();
			for (Enumeration e = all.keys(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
				PLPositionArray value = (PLPositionArray) all.get(key);
				if (value == null || value.size() == 0)
					continue;
				PLPosition position = value.elementAt(0);
				if (isPLPositionIn(position))
					_plPositions.put(key, value);
			}
		}

		public boolean addAggregatedPositions(String key, PLPositionArray aggPositions, PLPosition position) {
			PLPositionArray value = (PLPositionArray) _plPositions.get(key);
			if (value != null) {
				_plPositions.put(key, aggPositions);
				return true;
			}
			if (isPLPositionIn(position)) {
				_plPositions.put(key, aggPositions);
				return true;
			}
			return false;
		}

		public void removeAggregatedPosition(String key, int index) {
			// remove aggregated position
			PLPositionArray value = (PLPositionArray) _plPositions.get(key);
			if (value != null) {
				_plPositions.remove(key);
			}
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	Vector getTrades() {
		Vector trades = new Vector();
		if (_allPLPositions == null)
			return trades;
		for (int i = 0; i < _allPLPositions.size(); i++) {
			PLPosition plPosition = _allPLPositions.elementAt(i);
			Trade trade = plPosition.toTrade();
			trades.add(trade);
			// System.out.println("Quote " +
			// trade.getProduct().getQuoteName());
		}
		return trades;
	}


	JDatetime getValDate() {
		return this.valDateTime;
	}


	static String PK_NAME = "PositionKeeperTab";

	public Vector<String> getAllColumnNames() {
		if (_allColumnNames != null)
			return _allColumnNames;
		PositionKeeperUtil util = new PositionKeeperUtil();
		// _allColumnNames = util.getAllColumnNames();
		_allColumnNames = util.getColumnNames();
		List l = BOCache.getPositionAggregationConfigs(DSConnection.getDefault());
		if (l == null || l.size() == 0) {
			_allColumnNames.remove(PositionKeeperUtil.LIQ_AGGREGATION);
			_allColumnNames.remove(PositionKeeperUtil.LIQ_AGG_ID);
			_allColumnNames.remove(PositionKeeperUtil.CUSTODIAN);
			_allColumnNames.remove(PositionKeeperUtil.LONG_SHORT);
			_allColumnNames.remove(PositionKeeperUtil.STRATEGY);
			_allColumnNames.remove(PositionKeeperUtil.TRADER);
			_allColumnNames.remove(PositionKeeperUtil.BUNDLE);
		}
		Map<String, LiquidationConfig> liqConfigs = null;
		try {
			liqConfigs = DSConnection.getDefault().getRemoteReferenceData().getLiquidationConfigsByName();
		} catch (RemoteException e) {
			Log.error(this, "Could not load liquidation configs", e);
		}
		if (liqConfigs == null || liqConfigs.size() <= 1) {
			_allColumnNames.remove(PositionKeeperUtil.LIQ_CONFIG);
		}
		return _allColumnNames;
	}

	// Bug#: 15092
	static Vector _onlySecondary = null;

	public static Vector getOnlySecondaryMarketProduct(boolean clone) {
		if (_onlySecondary != null) {
			if (clone)
				return new Vector(_onlySecondary);
			return _onlySecondary;
		}
		_onlySecondary = LocalCache.cloneDomainValues(DSConnection.getDefault(), "productType");
		if (_onlySecondary == null || _onlySecondary.size() == 0)
			return new Vector();
		Vector all = new Vector(_onlySecondary);
		Product p;
		for (int i = 0; i < all.size(); i++) {
			String name = (String) all.get(i);
			try {
				p = (Product) InstantiateUtil.getInstance("tk.product." + name);
				if (p.isPositionProxy() || (p != null && !p.hasSecondaryMarket()))
					_onlySecondary.remove(name);
			} catch (Throwable e) {
			}
		}
		// Remove also "Collateral"
		_onlySecondary.remove("Collateral");
		if (clone)
			return new Vector(_onlySecondary);
		return _onlySecondary;
	}

	public void loadTabs() {
		Vector tabs = new Vector();
		Hashtable tabBooks = new Hashtable();
		Hashtable tabProducts = new Hashtable();
		Hashtable tabContracts = new Hashtable();
		Hashtable tabColumns = new Hashtable();
		Hashtable tabFixedColumns = new Hashtable();
		Hashtable tabFilterName = new Hashtable();
		Hashtable tabOpenTrades = new Hashtable();
		PositionConfigTabs.loadTabs(PK_NAME, getAllColumnNames(), tabs, tabBooks, tabProducts, tabContracts, tabColumns,
				tabFixedColumns, tabFilterName, tabOpenTrades);
		if (tabs.size() == 0) {
			// init defaults
			Vector pt = null;
			Vector contracts = null;
			try {
				// Bug#: 15092
				pt = getOnlySecondaryMarketProduct(false);
				RemoteProduct rp = DSConnection.getDefault().getRemoteProduct();
				Vector cn = rp.getContractNames();
				Vector ocn = rp.getOptionContractNames();
				contracts = new Vector();
				contracts.addAll(cn);
				if (ocn != null) {
					for (int i = 0; i < ocn.size(); i++) {
						String name = (String) ocn.elementAt(i);
						if (!contracts.contains(name))
							contracts.addElement(name);
					}
				}
			} catch (Exception e) {
				Log.error(this, e);
				return;
			}
			if (pt == null)
				return;
			Vector bn = new Vector();
			bn.add(ANY_BOOK_NAME);
			tabs.add("All");
			tabBooks.put("All", bn);
			tabProducts.put("All", pt);
			tabContracts.put("All", contracts);
			tabColumns.put("All", getAllColumnNames());
			PositionConfigTabs.saveTabs(PK_NAME, null, getAllColumnNames(), tabs, tabBooks, tabProducts, tabContracts,
					tabColumns, tabFixedColumns, tabFilterName, tabOpenTrades);
		}

		// Create tabs
		Vector bn = null;
		Vector allColumnList = new Vector();
		Vector reportColumnList = new Vector();
		for (int i = 0; i < tabs.size(); i++) {
			String tabName = (String) tabs.get(i);
			String filterName = (String) tabFilterName.get(tabName);
			String fixedColumns = (String) tabFixedColumns.get(tabName);
			Vector bookNames = (Vector) tabBooks.get(tabName);
			if (bookNames != null && bookNames.contains(ANY_BOOK_NAME)) {
				if (bn == null) {
					try {
						bn = DSConnection.getDefault().getRemoteReferenceData().getBookNames();
					} catch (Exception e) {
						Log.error(this, e);
						return;
					}
				}
				bookNames = bn;
			}
			Vector productTypes = (Vector) tabProducts.get(tabName);
			Vector contractNames = (Vector) tabContracts.get(tabName);
			Vector columnList = (Vector) tabColumns.get(tabName);

			if(domainValues != null && domainValues.contains(report.getReportTemplate().getTemplateName()) &&
					tabName.equalsIgnoreCase(LocalCache.getDomainValueComment(DSConnection.getDefault(), PK_NAME, report.getReportTemplate().getTemplateName())))
				reportColumnList = columnList;

			allColumnList = Util.mergeVectors(allColumnList, columnList);
			String openTrades = (String) tabOpenTrades.get(tabName);
			int nbColumns = 0;
			if (!Util.isEmptyString(fixedColumns)) {
				try {
					nbColumns = Integer.parseInt(fixedColumns);
				} catch (Throwable x) {
				}
			}
			boolean ot = (openTrades == null ? false : openTrades.equals("true"));
			TabPLPosition p = new TabPLPosition(tabName, 0, columnList, nbColumns, ot);
			setStaticDataFilter(p, filterName);
			if (bookNames != null) {
				for (int j = 0; j < bookNames.size(); j++) {
					String book = (String) bookNames.elementAt(j);
					int id = PositionConfigTabs.getBookId(book);
					p._bookIdList.addElement(Integer.valueOf(id));
				}
			}
			p._productTypeList = productTypes;
			p._contractList = contractNames;
			if (p._productTypeList == null)
				p._productTypeList = new Vector();
			_products.addElement(p);
		}
		if(reportColumnList.size() != 0)
			allColumnList = reportColumnList;
		if (allColumnList != null && allColumnList.size() > 0) {
			_columnNamesUsed = allColumnList;
			setIncludePreviousPL();
		}
		showPLPositions(getPLPositions());
	}
	private Vector getColumnNamesUsed() {
		if (_columnNamesUsed != null && _columnNamesUsed.size() > 0)
			return _columnNamesUsed;
		Vector columns = new Vector();
		Vector tabs = new Vector();
		Hashtable tabBooks = new Hashtable();
		Hashtable tabProducts = new Hashtable();
		Hashtable tabContracts = new Hashtable();
		Hashtable tabColumns = new Hashtable();
		Hashtable tabFixedColumns = new Hashtable();
		Hashtable tabFilterName = new Hashtable();
		Hashtable tabOpenTrades = new Hashtable();
		PositionConfigTabs.loadTabs(PK_NAME, getAllColumnNames(), tabs, tabBooks, tabProducts, tabContracts, tabColumns,
				tabFixedColumns, tabFilterName, tabOpenTrades);
		for (int i = 0; i < tabs.size(); i++) {
			String tabName = (String) tabs.get(i);
			Vector columnList = (Vector) tabColumns.get(tabName);
			columns = Util.mergeVectors(columns, columnList);
		}
		_columnNamesUsed = columns;
		return _columnNamesUsed;
	}


	void loadAndSetOpenPositions(PLPositionArray plpos, TradeFilter tf) throws Exception {
		if (plpos.isEmpty())
			return;
		// Instead of doing a Loop on the Entire scope of position_id, we use the
		// TradeFilter to load the TradeOpenQuantity.
		// If the Trade Filter has a ExcludeInactive position on, we will gether too
		// many Liquidated Positions.
		// TODO - we should try to add a subquery on the position_id which reflects the
		// exclude
		TradeOpenQuantityArray openPos = DSConnection.getDefault().getRemoteLiquidation()
				.getTradeOpenQuantity(TradeOpenQuantityCriteriaBuilder.create(tf).status().ne(LiquidableStatus.Canceled)
						.and(TradeOpenQuantityCriteriaBuilder.create().openQuantity().ne(0)
								.or(TradeOpenQuantityCriteriaBuilder.create().openRepoQuantity().ne(0).or(
										TradeOpenQuantityCriteriaBuilder.create().productFamily().eq("REALIZED")))));
		if (openPos == null || openPos.size() == 0)
			return;
		Hashtable<PLPosition.PLPositionKey, PLPosition> h = new Hashtable<PLPosition.PLPositionKey, PLPosition>();
		int size = plpos.size();
		for (int i = 0; i < size; i++) {
			PLPosition pl = plpos.elementAt(i);
			h.put(pl.getKey(), pl);
			pl.setOpenPositions(new TradeOpenQuantityArray());
		}
		size = openPos.size();
		for (int i = 0; i < openPos.size(); i++) {
			TradeOpenQuantity oq = openPos.elementAt(i);
			PLPosition pl = h.get(oq.getPLPositionKey());
			if (pl != null) {
				pl.getOpenPositions().add(oq);
			}
		}
		size = plpos.size();
	}

	// Allows to set a single Tab with required column names for application
	// trying to use PositionKeeperJFrame
	public void setConfigurableSingleTab(Vector columnNames) {
		Vector tabs = new Vector();
		Hashtable tabBooks = new Hashtable();
		Hashtable tabProducts = new Hashtable();
		Hashtable tabContracts = new Hashtable();
		Hashtable tabColumns = new Hashtable();
		Hashtable tabFixedColumns = new Hashtable();
		Hashtable tabFilterName = new Hashtable();
		Hashtable tabOpenTrades = new Hashtable();
		// init defaults
		Vector pt = null;
		Vector contracts = null;
		try {
			pt = getOnlySecondaryMarketProduct(false);
			contracts = DSConnection.getDefault().getRemoteProduct().getContractNames();
		} catch (Exception e) {
			Log.error(this, e);
			return;
		}
		if (pt == null)
			return;
		Vector bn = new Vector();
		bn.add(ANY_BOOK_NAME);
		tabs.add("All");
		tabBooks.put("All", bn);
		tabProducts.put("All", pt);
		tabContracts.put("All", contracts);
		tabColumns.put("All", columnNames);
		PositionConfigTabs.saveTabs(PK_NAME, null, getAllColumnNames(), tabs, tabBooks, tabProducts, tabContracts,
				tabColumns, tabFixedColumns, tabFilterName, tabOpenTrades);
		// Create tabs
		for (int i = 0; i < tabs.size(); i++) {
			String tabName = (String) tabs.get(i);
			int nbColumns = 0;
			boolean ot = false;
			TabPLPosition p = new TabPLPosition(tabName, 0, columnNames, columnNames.size(), ot);
			p._productTypeList = pt;
			p._contractList = contracts;
			if (bn != null) {
				for (int j = 0; j < bn.size(); j++) {
					String book = (String) bn.elementAt(j);
					int id = PositionConfigTabs.getBookId(book);
					p._bookIdList.addElement(Integer.valueOf(id));
				}
			}
			_products.addElement(p);
		}
	}

	protected void computePreviousValues(PLPositionArray previousPositions, Hashtable<PLPosition, Double> cache,
			PricingEnv previousEnv, JDatetime previousDatetime) {
		double unreal = 0.;
		int size = previousPositions.size();
		for (int i = 0; i < size; i++) {
			PLPosition tmpPos = previousPositions.elementAt(i);
			unreal = 0;
			try {
				Product p = tmpPos.getProduct();
				if (p != null && Util.isEqualStrings(CommodityUtil.getPLPositionProductType(tmpPos),
						CommodityForward.COMMODITY_FORWARD)) {
					unreal = CommodityUtil.computeUnrealize(tmpPos, previousEnv, previousDatetime);
				} else {
					unreal = tmpPos.getUnrealized(previousEnv, previousDatetime);
				}
			} catch (Exception e) {
				Log.error(this, e);
			}
			cache.put(tmpPos, Double.valueOf(unreal));
		}
	}

	private String getPositionAggregationName(int liqAggId) {
		PositionAggregation agg = BOCache.getPositionAggregation(DSConnection.getDefault(), liqAggId);
		if (agg != null) {
			int configId = agg.getConfigId();
			if (configId == 0)
				return null;
			PositionAggregationConfig config = BOCache.getPositionAggregationConfig(DSConnection.getDefault(),
					configId);
			if (config != null)
				return config.getName();
		}
		return null;
	}

	/**
	 * Return a boolean to indicate whether the trade filter uses liquidation
	 * aggregation
	 *
	 * @return a boolean
	 */
	private boolean isUsingLiqAggregation() {
		if (_portfolio != null) {
			PositionSpec pSpec = _portfolio.getPositionSpec();
			if (pSpec == null) {
				return false;
			} else {
				if (pSpec.getPositionAggregationConfigId() == 0)
					return false;
				else
					return true;
			}
		}
		return false;
	}

	private boolean isUsingLiqAggOrConfig() {
		boolean useLiqAgg = false;
		if (_portfolio != null) {
			PositionSpec pSpec = _portfolio.getPositionSpec();
			if (pSpec != null && (pSpec.getPositionAggregationConfigId() != 0 || pSpec.getLiquidationConfig() != null))
				useLiqAgg = true;
		}
		return useLiqAgg;
	}

	private boolean isUsingLiqConfig() {
		if (_portfolio != null) {
			PositionSpec pSpec = _portfolio.getPositionSpec();
			if (pSpec == null) {
				return false;
			}
			if (pSpec.getLiquidationConfig() == null)
				return false;
			else
				return true;
		}
		return false;
	}

	/**
	 * Return liquidation aggregation id from trade filter if trade filter uses
	 * poistion spec and the spec uses position aggregation. For all other cases, 0
	 * is returned.
	 *
	 * @return an int of lquidation aggregation id
	 */
	private int getLiqAggId() {
		int liqAggId = 0;
		PositionSpec positionSpec = _portfolio.getPositionSpec();
		if (positionSpec != null)
			liqAggId = positionSpec.getPositionAggregationConfigId();
		return liqAggId;
	}

	private LiquidationConfig getLiqConfig() {
		LiquidationConfig liqConfig = LiquidationConfig.getDEFAULT();
		PositionSpec positionSpec = _portfolio.getPositionSpec();
		if (positionSpec != null)
			liqConfig = positionSpec.getLiquidationConfig();
		return liqConfig;
	}

	private int getLiqConfigId() {
		return getLiqConfig().getId();
	}

	private boolean isAllBookAvailable() {
		if (!AccessUtil.fullAccessB()) {
			User user = AccessUtil.getUser();
			if (user != null) {
				Vector groups = user.getGroups();
				if (groups == null)
					return false;
				for (int j = 0; j < groups.size(); j++) {
					String group = (String) groups.elementAt(j);
					if (AccessUtil.isAllBookAvailable(group))
						return true;
				}
			}
			return false;
		}
		return true;
	}

	private Hashtable getBookMap() {
		Vector books = AccessUtil.getAllBookNames();
		if (Util.isEmpty(books))
			return new Hashtable();
		return Util.toHashtable(books);
	}

	// bulk-load liquidated positions and set them on the PL positions passed in
	private void loadAllLiquidatedPositions(PLPositionArray plPositions, JDatetime datetime, TradeFilter tf)
			throws CalypsoServiceException {
		Set<Long> posIds = new HashSet<Long>();
		Map<Long, PLPosition> posIdMap = new TreeMap<Long, PLPosition>();
		// build list of posIds
		for (int i = 0; i < plPositions.size(); i++) {
			PLPosition pl = plPositions.get(i);
			Long posId = Long.valueOf(pl.getPositionLongId());
			posIds.add(posId);
			// add to map for future retrieval
			posIdMap.put(posId, pl);
			// initialize liqArray on each pl
			LiquidatedPositionArray plLiqArray = pl.getLiqArray();
			if (plLiqArray == null)
				pl.setLiqArray(null);
			else
				plLiqArray.clear();
		}
		// Instead of doing a Loop on the Entire scope of position_id, we use the
		// TradeFilter to load the LiquidatedPosition.
		// If the Trade Filter has a ExcludeInactive position on, we will gether too
		// many Liquidated Positions.
		// TODO - we should try to add a subquery on the position_id which reflects the
		// exclude
		List<LiquidatedPosition> liqPos = DefaultLiquidatedPositionLoader.create()
				.load(LiquidatedPositionCriteriaBuilder.create(tf).deleted(false).orderForUndo());
		// set liquidated position on the corresponding PL position
		for (LiquidatedPosition lp : liqPos) {
			// corresponding PL position
			PLPosition pl = posIdMap.get(lp.getPositionLongId());
			if (pl != null) {
				LiquidatedPositionArray plLiqArray = pl.getLiqArray();
				if (plLiqArray != null) {
					plLiqArray.add(lp);
				}
			}
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	PricingEnv _env;

	PricingEnv _previousEnv;

	String _attributeName;

	int _excludeZeroPositions;

	double _zeroPositionTolerance;

	Hashtable<String, PLPositionArray> _aggregatedPLPositions;

	Vector _books;

	BookHierarchyNode _selectedBookHierarchyNode;

	Hashtable _liquidationBooks;

	protected TradeFilter _portfolio;

	protected Product _product;

	protected PLPositionArray _PLPositions;

	protected BookHierarchy _orgStructure;

	protected PLPositionArray _allPLPositions;

	protected PLPositionArray _previousPLPositions;

	protected Hashtable _previousPLPositionsHash;

	static final String POSITION_KEEPER_HIERARCHY = "PositionKeeperHierarchy";

	static final String PL_POSITION_KEY = "PLPosition";

	static final String PL_POSITION_AGG_ATTRIBUTE = "PLPositionAggAttribute";

	protected boolean _isAllBookAvailable = true;

	protected Hashtable _availBooks = new Hashtable();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	PositionConfigTabs _pkConfigDialog = null;

	Vector _products = new Vector();

	Vector<String> _allColumnNames = null;

	Vector _columnNamesUsed = null;

	boolean _includePreviousPL = false;

	boolean _useOptimizeLoad = true;

	Vector _preloadedTypes = null;

	Hashtable<PLPosition, Double> _previousPL = new Hashtable<PLPosition, Double>();



	/*******************************************/

	protected JDatetime valDateTime;
	protected Boolean posBySettleDate;
	protected Boolean includeFee;
	protected Boolean isRealTimeChangeCheck = false;
	protected String tabName = "All";
	protected String orgChoice = "";
	PosAggregationFilterDescriptor posAggregationFilterDescriptor;
	private Report report = null;
	private Vector domainValues = null;

	/**
	 * build the position
	 * 
	 * @param report
	 */
	public synchronized ArrayList<HashMap<String, Object>> getPositionKeeperContent(Report report) {
		this.report=report;
		if (report != null) {
			updateFromReport(report);
			try {
				domainValues = DSConnection.getDefault().getRemoteReferenceData().getDomainValues(PK_NAME);
			} catch (CalypsoServiceException e) {
				throw new RuntimeException(e);
			}
		}
		initDomains();
		WACMarkCache.setUseCache(true);
		try {

			_portfolio = getUpdatedTradeFilter();

			if (LiquidationUtil.isSnapshotLiquidationConfig((TradeFilter) _portfolio)) {
				initializeFromSnapshot(valDateTime);
			} else {
				initializeFromPLPositionUtil(valDateTime);
			}
		} catch (Exception e) {
			Log.error(this, e);
		}
		PLPositionArray plPositions = getAllPLPositions();
		filter(plPositions);
		return result;
	}

	/**
	 * update the controller criterias from the report criterias.
	 * 
	 * @param report
	 */
	public synchronized void updateFromReport(Report report) {

		ReportTemplate template = report.getReportTemplate();

		setPricingEnv(report.getPricingEnv());

		setValuationDateTime(report.getValuationDatetime());

		String tradeFilter = template.get(PositionKeeperReportTemplate.TRADE_FILTER);
		setTradeFilter(tradeFilter);

		String aggregation = template.get(PositionKeeperReportTemplate.AGGREGATION);
		setAggregation(aggregation);

		int zeroPosition = template.get(PositionKeeperReportTemplate.ZERO_POSITION);
		setZeroPosition(zeroPosition);

		Product product = template.get(PositionKeeperReportTemplate.PRODUCT);
		setProduct(product);

		Boolean posBySettleDate = template.get(PositionKeeperReportTemplate.POSITION_BY_SETTLEDATE);
		setPosBySettleDate(posBySettleDate);

		Boolean includeFee = template.get(PositionKeeperReportTemplate.INCLUDE_FEE);
		setIncludeFees(includeFee);

		PosAggregationFilterDescriptor liquidationKeys = template.get(PositionKeeperReportTemplate.LIQUIDATION_KEYS);
		setPosAggregationFilterDescriptor(liquidationKeys);

	}

	public void setPricingEnv(PricingEnv env) {
		this._env = env;
		this._previousEnv = env;
	}

	public void setValuationDateTime(JDatetime valDatetime) {
		this.valDateTime = valDatetime;
	}

	public void setTradeFilter(String tradeFilter) {

		try {
			_portfolio = (TradeFilter) BOCache.getTradeFilter(DSConnection.getDefault(), tradeFilter).clone();
		} catch (CloneNotSupportedException e) {
			Log.error(this, e);
		}

	}

	public void setAggregation(String aggregation) {
		this._attributeName = aggregation;
	}

	public void setZeroPosition(int zeroPosition) {
		this._excludeZeroPositions = zeroPosition;
	}

	public void setProduct(Product product) {
		this._product = product;
	}

	public void setPosBySettleDate(Boolean posBySettleDate) {
		this.posBySettleDate = posBySettleDate;
	}

	public void setIncludeFees(Boolean includeFee) {
		this.includeFee = includeFee;
	}

	public void setPosAggregationFilterDescriptor(PosAggregationFilterDescriptor posAggregationFilterDescriptor) {
		this.posAggregationFilterDescriptor = posAggregationFilterDescriptor;
	}


}
