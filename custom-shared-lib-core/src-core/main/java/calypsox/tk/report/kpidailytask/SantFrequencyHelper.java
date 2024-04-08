/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.kpidailytask;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class SantFrequencyHelper {

	private static final String DAILY = "Daily";
	private static final String MONDAY = "Monday";
	private static final String TUESDAY = "Tuesday";
	private static final String WEDNESDAY = "Wednesday";
	private static final String THURSDAY = "Thursday";
	private static final String FRIDAY = "Friday";
	private static final String FIRST_DAY_OF_MONTH = "First day of month";
	private static final String MONTHLY_10_OF_EVERY_MONTH = "Monthly, 10 of every month";
	private static final String MONTHLY_15_OF_EVERY_MONTH = "Monthly, 15 of every month";
	private static final String BIANNUAL_20_FEBRUARY_AND_AUGUST = "Biannual, 20 February and August";
	private static final String FORTNIGHTLY_5_AND_20_OF_EVERY_MONTH = "Fortnightly, 5 and 20 of every month";
	private static final String FORTNIGHTLY_10_AND_25_OF_EVERY_MONTH = "Fortnightly, 10 and 25 of every month";
	private static final String FORTNIGHTLY_1_AND_15_OF_EVERY_MONTH = "Fortnightly, 1 and 15 of every month";
	private static final String SECOND_TUESDAY_OF_EVERY_MONTH = "Second Tuesday of every month";
	private static final String FORTNIGHTLY_TUESDAYS_2_AND_4_OF_EVERY_MONTH = "Fortnightly, Tuesdays 2 and 4 of every month";

	private static final String FREQUENCIES_ADDITIONAL_FIELD = "mccAdditionalField.FREQUENCY";

	private Map<String, String> frequenciesLabels = new HashMap<String, String>();

	public SantFrequencyHelper(DSConnection ds) {
		initAdditFieldsFrequenciesDomainValues(ds);
	}

	public String getFrequency(String key) {
		return this.frequenciesLabels.get(key);
	}

	public String getMarginCallCalculation(String frequency, JDate reportDate) {
		if (frequency == null) {
			return "";
		}
		if (frequency.equals(DAILY)) {
			return "Yes";
		}
		// "Monday", "Monday Manual"
		else if ((frequency.startsWith(MONDAY)) && (reportDate.getDayOfWeek() == 2)) {
			return "Yes";
		}
		// Tuesday, Tuesday Manual
		else if ((frequency.startsWith(TUESDAY)) && (reportDate.getDayOfWeek() == 3)) {
			return "Yes";
		}
		// "Wednesday", "Wednesday Manual"
		else if ((frequency.startsWith(WEDNESDAY)) && (reportDate.getDayOfWeek() == 4)) {
			return "Yes";
		} else if (frequency.startsWith(THURSDAY) && (reportDate.getDayOfWeek() == 5)) {
			return "Yes";
		} else if (frequency.startsWith(FRIDAY) && (reportDate.getDayOfWeek() == 6)) {
			return "Yes";
		}
		// "First day of month", "First day of month, but manual calculation"
		else if (frequency.startsWith(FIRST_DAY_OF_MONTH) && (reportDate.getDayOfMonth() == 1)) {
			return "Yes";
		}
		// "Monthly, 15 of every month"
		else if (frequency.startsWith(MONTHLY_15_OF_EVERY_MONTH) && (reportDate.getDayOfMonth() == 15)) {
			return "Yes";
		}
		// "Monthly, 10 of every month"
		else if (frequency.startsWith(MONTHLY_10_OF_EVERY_MONTH) && (reportDate.getDayOfMonth() == 10)) {
			return "Yes";
		}
		// Biannual, 20 February and August
		else if (frequency.startsWith(BIANNUAL_20_FEBRUARY_AND_AUGUST)
				&& (reportDate.equals(JDate.valueOf(reportDate.getYear(), 2, 20)) || reportDate.equals(JDate.valueOf(
						reportDate.getYear(), 8, 20)))) {
			return "Yes";
		}
		// Fortnightly, 1 and 15 of every month
		else if (frequency.startsWith(FORTNIGHTLY_1_AND_15_OF_EVERY_MONTH)
				&& ((reportDate.getDayOfMonth() == 1) || (reportDate.getDayOfMonth() == 15))) {
			return "Yes";
		}
		// Fortnightly, 10 and 25 of every month
		else if (frequency.equals(FORTNIGHTLY_10_AND_25_OF_EVERY_MONTH)
				&& ((reportDate.getDayOfMonth() == 10) || (reportDate.getDayOfMonth() == 25))) {
			return "Yes";
		}
		// Fortnightly, 5 and 20 of every month
		else if (frequency.startsWith(FORTNIGHTLY_5_AND_20_OF_EVERY_MONTH)
				&& ((reportDate.getDayOfMonth() == 5) || (reportDate.getDayOfMonth() == 20))) {
			return "Yes";
		}
		// Second Tuesday of every month
		else if (frequency.startsWith(SECOND_TUESDAY_OF_EVERY_MONTH)) {
			int firstTuesdayOfMonth = 0;
			JDate tempDate = JDate.valueOf(reportDate.getYear(), reportDate.getMonth(), 1);
			while (tempDate.getDayOfWeek() != JDate.TUESDAY) {
				tempDate = tempDate.addDays(1);
			}
			firstTuesdayOfMonth = tempDate.getDayOfMonth();
			if (reportDate.getDayOfMonth() == (firstTuesdayOfMonth + 7)) {
				return "Yes";
			}
		}
		// Fortnightly, Tuesdays 2 and 4 of every month
		else if (frequency.startsWith(FORTNIGHTLY_TUESDAYS_2_AND_4_OF_EVERY_MONTH)) {
			int firstTuesdayOfMonth = 0;
			JDate tempDate = JDate.valueOf(reportDate.getYear(), reportDate.getMonth(), 1);
			while (tempDate.getDayOfWeek() != JDate.TUESDAY) {
				tempDate = tempDate.addDays(1);
			}
			firstTuesdayOfMonth = tempDate.getDayOfMonth();
			if (reportDate.getDayOfMonth() == (firstTuesdayOfMonth + 7)) {
				return "Yes";
			} else if (reportDate.getDayOfMonth() == (firstTuesdayOfMonth + 21)) {
				return "Yes";
			}
		}
		return "No";

	}

	public void initAdditFieldsFrequenciesDomainValues(final DSConnection dsConn) {
		this.frequenciesLabels = (this.frequenciesLabels != null ? this.frequenciesLabels
				: new HashMap<String, String>());
		if (this.frequenciesLabels.size() == 0) {
			Vector<String> domainValueFrequencies = LocalCache.getDomainValues(dsConn, FREQUENCIES_ADDITIONAL_FIELD);
			if (Util.isEmpty(domainValueFrequencies)) {
				return;
			}
			for (String dv : domainValueFrequencies) {
				if (Util.isEmpty(dv)) {
					continue;
				}
				final String domainComment = LocalCache.getDomainValueComment(dsConn, FREQUENCIES_ADDITIONAL_FIELD, dv);
				this.frequenciesLabels.put(dv, domainComment);
			}
		}
	}

}
