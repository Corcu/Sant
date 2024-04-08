/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader.margincall;

import com.calypso.tk.collateral.MarginCallAllocationFacade;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;

public class SantMarginCallAllocationEntry {

	private final MarginCallAllocationFacade allocation;
	private final SantMarginCallEntry santEntry;
	private final CollateralConfig marginCallConfig;
	private Trade trade;

	public SantMarginCallAllocationEntry(final MarginCallAllocationFacade allocation,
			final SantMarginCallEntry santEntry, final CollateralConfig marginCallConfig) {
		this.allocation = allocation;
		this.santEntry = santEntry;
		this.marginCallConfig = marginCallConfig;
	}

	public boolean isDummy() {
		return (this.trade == null) && (this.allocation == null);
	}

	public MarginCallAllocationFacade getAllocation() {
		return this.allocation;
	}

	public CollateralConfig getMarginCallConfig() {
		return this.marginCallConfig;
	}

	public void setTrade(final Trade trade) {
		this.trade = trade;
	}

	public Trade getTrade() {
		return this.trade;
	}

	public SantMarginCallEntry getSantEntry() {
		return this.santEntry;
	}

}
