/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.calypso.tk.product.CollateralExposure;

public class InstrumentTypeLoader {

	private final List<String> list = new ArrayList<String>();

	public List<String> load() {
		this.list.add("");
		// this.list.addAll(Repo.getRepoTypeChoices());
		// this.list.addAll(SecLending.getSecLendingTypeChoices());
		this.list.addAll(CollateralExposure.getSubTypes(false));
		Collections.sort(this.list);
		return this.list;
	}
}
