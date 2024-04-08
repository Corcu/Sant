/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.riskvalues;

import com.calypso.tk.marketdata.MarginCallCreditRating;

public class MCRatingsPriority {

	private int priority;
	private MarginCallCreditRating moodyRating;
	private MarginCallCreditRating snpRating;

	public int getPriority() {
		return this.priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public MarginCallCreditRating getMoodyRating() {
		return this.moodyRating;
	}

	public void setMoodyRating(MarginCallCreditRating moodyRating) {
		this.moodyRating = moodyRating;
	}

	public MarginCallCreditRating getSnpRating() {
		return this.snpRating;
	}

	public void setSnpRating(MarginCallCreditRating snpRating) {
		this.snpRating = snpRating;
	}
}
