/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader;

import java.util.Map;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.mo.TradeFilterPager;
import com.calypso.tk.report.ReportTemplate;

public class SantPortfolioTradesThreadLoader extends SantEnableThreadLoader<Integer, Trade> {

	private final int numThread;
	private final TradeFilterPager tradeFilterPager;

	public SantPortfolioTradesThreadLoader(boolean enableThreading, ReportTemplate template,
			TradeFilterPager tradeFilterPager, int numThread) {
		super(template, enableThreading, null);
		this.tradeFilterPager = tradeFilterPager;
		this.numThread = numThread;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void loadData() {
		try {
			this.dataList.addAll(getTradeFilterPager().getPage(getNumThread()));
		} catch (Exception e) {
			Log.error(this, e);
		}
	}

	public int getNumThread() {
		return this.numThread;
	}

	public TradeFilterPager getTradeFilterPager() {
		return this.tradeFilterPager;
	}

	@Override
	protected Map<Integer, Trade> getDataMapFromDataList() {
		return super.getDataAsMap();
	}
}
