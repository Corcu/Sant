/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.notification;

import com.calypso.tk.product.InterestBearingEntry;

public class InterestPosEntry {

	protected InterestBearingEntry pos;
	protected InterestBearingEntry interest;
	protected InterestBearingEntry adjustment;
	protected InterestBearingEntry partialSettle;

	public InterestBearingEntry getPos() {
		return this.pos;
	}

	public void setPos(final InterestBearingEntry pos) {
		this.pos = pos;
	}

	public InterestBearingEntry getInterest() {
		return this.interest;
	}

	public void setInterest(final InterestBearingEntry interest) {
		this.interest = interest;
	}

	public void setAdjustment(InterestBearingEntry adjustment) {
		this.adjustment = adjustment;
	}

	public InterestBearingEntry getAdjustment() {
		return this.adjustment;
	}

	public InterestBearingEntry getPartialSettle() {
		return this.partialSettle;
	}

	public void setPartialSettle(InterestBearingEntry partialSettle) {
		this.partialSettle = partialSettle;
	}
}
