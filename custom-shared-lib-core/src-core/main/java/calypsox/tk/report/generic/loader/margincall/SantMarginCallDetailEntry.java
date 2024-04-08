/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader.margincall;

import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;

public class SantMarginCallDetailEntry {

	private final MarginCallDetailEntryDTO detailEntry;
	private final SantMarginCallEntry santEntry;
	private final CollateralConfig marginCallConfig;
	private Trade trade;

	public SantMarginCallDetailEntry(final MarginCallDetailEntryDTO detailEntry, final SantMarginCallEntry santEntry,
			final CollateralConfig marginCallConfig) {
		this.detailEntry = detailEntry;
		this.santEntry = santEntry;
		this.marginCallConfig = marginCallConfig;
	}

	public MarginCallDetailEntryDTO getDetailEntry() {
		return this.detailEntry;
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
