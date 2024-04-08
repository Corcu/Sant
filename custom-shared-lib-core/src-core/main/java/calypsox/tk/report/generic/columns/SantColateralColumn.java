/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.columns;

import java.util.Vector;

import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecLending;

public class SantColateralColumn extends SantColumn {

    private Object initialColateral;

    private Product underlying;

    private Object principal;

    public SantColateralColumn(final Trade trade) {
	build(trade);

    }

    @SuppressWarnings("unchecked")
    private void build(final Trade trade) {
	if (trade == null) {
	    return;
	}
	final Product p = trade.getProduct();

	if (p instanceof SecLending) {
	    final SecLending secLending = (SecLending) p;

	    // Sec Vs Cash
	    final Vector<Collateral> leftCollaterals = secLending
		    .getLeftCollaterals();
	    if (leftCollaterals.size() > 0) {
		this.underlying = leftCollaterals.get(0);
	    }

	    final Vector<Collateral> rightCollaterals = secLending
		    .getRightCollaterals();

	    for (final Collateral c : rightCollaterals) {
		if (c.getMoneyMarket() != null) {
		    final String ccy = c.getMoneyMarket().getCurrency();
		    final double amount = c.getMoneyMarket().getPrincipal();
		    this.initialColateral = getSignedAmount(amount, ccy);
		    this.principal = this.initialColateral;
		}
	    }
	    // Sec Vs Collateral Pool
	    if (this.initialColateral == null) {
		this.initialColateral = 0;
		this.principal = getSignedAmount(
			secLending.getSecuritiesMoneyValue(),
			secLending.getCurrency());
	    }
	} else if (p instanceof Repo) {
	    final Repo repo = (Repo) p;
	    this.underlying = repo.getUnderlyingProduct();
	    this.initialColateral = 0;
	    this.principal = getSignedAmount(repo.getPrincipal(),
		    repo.getCurrency());
	}
    }

    public Object getInitialColateral() {
	return this.initialColateral;
    }

    public String getUnderLyingDescrition() {
	if (this.underlying == null) {
	    return null;
	}
	return this.underlying.getDescription();
    }

    public String getUnderlyingQuoteName() {
	if (this.underlying == null) {
	    return null;
	}
	return this.underlying.getDescription();
    }

    public Object getPrincipal() {
	return this.principal;
    }
}
