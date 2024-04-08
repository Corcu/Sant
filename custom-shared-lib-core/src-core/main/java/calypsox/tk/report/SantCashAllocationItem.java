/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;

public class SantCashAllocationItem {

	private Trade trade = null;
	private CollateralConfig mcc = null;
	private Amount mtm = null;
	private PricingEnv pricingEnv = null;

	public Trade getTrade() {
		return this.trade;
	}

	public void setTrade(Trade trade) {
		this.trade = trade;
	}

	// GSM: 28/06/2013 - deprecated new core.
	public CollateralConfig getCollateralConfig() {
		return this.mcc;
	}

	// GSM: 28/06/2013 - deprecated new core.
	public void setCollateralConfig(CollateralConfig mcc) {
		this.mcc = mcc;
	}

	public Amount getMtm() {
		return this.mtm;
	}

	public void setMtm(Amount mtm) {
		this.mtm = mtm;
	}

	public PricingEnv getPricingEnv() {
		return this.pricingEnv;
	}

	public void setPricingEnv(PricingEnv pricingEnv) {
		this.pricingEnv = pricingEnv;
	}

}
