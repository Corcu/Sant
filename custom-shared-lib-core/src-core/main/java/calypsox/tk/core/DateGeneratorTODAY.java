/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.core;

import java.util.Vector;

import com.calypso.tk.core.DateGenerator;
import com.calypso.tk.core.DateRule;
import com.calypso.tk.core.JDate;

public class DateGeneratorTODAY implements DateGenerator {

	@Override
	public Vector<JDate> generate(final DateRule drule, final JDate from, final JDate to) {
		final Vector<JDate> v = new Vector<JDate>();
		v.add(JDate.getNow());
		return v;
	}

	@Override
	public Vector<JDate> generate(final DateRule drule, final JDate from, final int count) {
		final Vector<JDate> v = new Vector<JDate>();
		v.add(JDate.getNow());
		return v;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public JDate next(final DateRule drule, final JDate date) {
		final boolean isBusiness = drule.getBusCalB();
		if (isBusiness) {
			final Vector holidays = drule.getHolidays();
			return JDate.getNow().addBusinessDays(1, holidays);
		}
		return JDate.getNow().addDays(1);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public JDate previous(final DateRule drule, final JDate date) {
		final boolean isBusiness = drule.getBusCalB();
		if (isBusiness) {
			final Vector holidays = drule.getHolidays();
			return JDate.getNow().addBusinessDays(-1, holidays);
		}
		return JDate.getNow().addDays(-1);
	}

}
