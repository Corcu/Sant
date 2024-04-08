/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.notification;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;

import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.DisplayValue;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Rate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.InterestBearingEntry;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.BOPositionUtil;

public class SantStatementHelper {

	private static final String DISPLAYABLE_MISSING_VALUE = "-";

	private static int ENTRY_NB_COL = 9;

	private static final String ENTRY_DATE = "Date";
	private static final String ENTRY_MOV = "Movement";
	private static final String ENTRY_CURRENCY = "Currency";
	private static final String ENTRY_PRINCIPAL = "Principal";
	private static final String ENTRY_RATE = "Rate";
	private static final String ENTRY_SPREAD = "Spread";
	private static final String ENTRY_ADJUST_RATE = "Adjusted Rate";
	private static final String ENTRY_DAILY_ACCRUAL = "Daily Accrual";
	private static final String ENTRY_TOTAL_ACCRUAL = "Total Accrual";

	private static final List<String> INT_TABLE_HEADER = Arrays.asList(ENTRY_DATE, ENTRY_MOV, ENTRY_CURRENCY,
			ENTRY_PRINCIPAL, ENTRY_RATE, ENTRY_SPREAD, ENTRY_ADJUST_RATE, ENTRY_DAILY_ACCRUAL, ENTRY_TOTAL_ACCRUAL);

	private final SantInterestNotificationCache cache;

	public SantStatementHelper(SantInterestNotificationCache cache) {
		this.cache = cache;
	}

	@SuppressWarnings("unchecked")
	public String getDetails(Trade trade, DateFormat df) {
		// structure for the interest table
		final HashMap<JDate, List<Object>> interestTable = new HashMap<JDate, List<Object>>();

		initIterestTable(interestTable, trade, this.cache.getDs(), df, ENTRY_NB_COL);

		List<InterestBearing> ibList = this.cache.getInterestBearings(trade);
		double previousPositionAmount = 0.0d;
		double totalAccrual = 0.0;

		for (InterestBearing interestBearing : ibList) {

			boolean firstRow = true;

			final Vector<InterestBearingEntry> entries = interestBearing.getEntries();

			final Map<JDate, InterestPosEntry> mergedEntries = new SantInterestPosEntryBuilder().mergeEntries(entries);

			final SortedSet<JDate> sortedEntriesDates = new TreeSet<JDate>(mergedEntries.keySet());

			double movementAmount = 0;

			for (final JDate d : sortedEntriesDates) {

				final List<Object> interestRow = interestTable.get(d);
				if (interestRow == null) {
					continue;
				}
				interestRow.clear();

				final InterestPosEntry entry = mergedEntries.get(d);

				final InterestBearingEntry interestEntry = entry.getInterest();
				final InterestBearingEntry positionEntry = entry.getPos();
				final InterestBearingEntry adjustmentEntry = entry.getAdjustment();

				// Previous
				final InterestPosEntry previousEntry = mergedEntries.get(d.addDays(-1));

				InterestBearingEntry partialSettleEntry = null;
				if (previousEntry != null) {
					partialSettleEntry = previousEntry.getPartialSettle();
					if (partialSettleEntry != null) {
						totalAccrual = 0.0d;
					}
				}

				double interestAmount = 0.0d;
				double positionAmount = 0.0d;
				double adjustmentAmount = 0.0d;
				if (interestEntry != null) {
					interestAmount = interestEntry.getAmount() + interestEntry.getAdjustment();
					totalAccrual += interestAmount;
				}
				if (positionEntry != null) {
					positionAmount = positionEntry.getAmount();
				}
				if (adjustmentEntry != null) {
					if (adjustmentEntry.getValueDate().equals(interestBearing.getStartDate())) {
						// DO NOTHING
					} else if (adjustmentEntry.getValueDate().equals(interestBearing.getEndDate())) {
						// DO NOTHING
					} else {
						adjustmentAmount = adjustmentEntry.getAmount();
						totalAccrual += adjustmentAmount;
					}
				}

				if ((interestEntry == null) || (positionEntry == null)) {
					continue;
				}

				if (firstRow) {
					@SuppressWarnings("deprecation")
					final InventoryCashPosition invCashPosition = BOPositionUtil.getCashPosition(
							this.cache.getAccount(trade).getCurrency(), BOPositionUtil.CLIENT, BOPositionUtil.ACTUAL,
							BOPositionUtil.SETTLE_DATE, d, 0, this.cache.getAccount(trade).getId(),
							DSConnection.getDefault(), null);
					if (invCashPosition != null) {
						if (invCashPosition.getPositionDate().equals(d)) {
							movementAmount = invCashPosition.getDailyChange();
						} else {
							movementAmount = 0;
						}
					} else {
						movementAmount = 0;
					}
				} else {
					movementAmount = positionAmount - previousPositionAmount;
				}

				interestRow.add(df.format(interestEntry.getEntryDate().getDate(TimeZone.getDefault())).toUpperCase());
				interestRow.add(new Amount(movementAmount, 2));
				interestRow.add(interestBearing.getCurrency());
				interestRow.add(new Amount(positionEntry.getAmount(), 2));
				interestRow.add(new Rate(interestEntry.getRate()));
				interestRow.add(new Rate(interestEntry.getSpread()));
				interestRow.add(new Rate(interestEntry.getRate() + interestEntry.getSpread()));
				interestRow.add(new Amount(interestEntry.getAmount(), 2));

				interestRow.add(new Amount(totalAccrual, 2));

				previousPositionAmount = positionAmount;
				firstRow = false;
			}

		}

		StringBuilder sb = new StringBuilder();
		int tableSize = interestTable.size();
		int startIndex = 0;
		int endIndex = 0;

		boolean oneTableOnly = false;

		// first page
		if (tableSize < 40) {
			endIndex = tableSize;
			oneTableOnly = true;
		} else {
			endIndex = 40;
		}
		sb.append(generateInterestTableHTML(interestTable, startIndex, endIndex, oneTableOnly));

		startIndex = endIndex;

		while (startIndex < tableSize) {
			if ((tableSize - startIndex) > 70) {
				endIndex += 70;
			} else {
				endIndex = tableSize;
			}
			sb.append(generateInterestTableHTML(interestTable, startIndex, endIndex, oneTableOnly));
			startIndex = endIndex;

		}

		return sb.toString();
	}

	private String generateInterestTableHTML(final Map<JDate, List<Object>> interestTable, int startIndex,
			int endEndex, boolean oneTableOnly) {

		String className = "firstPageBorderTable";
		if (!oneTableOnly) {
			className = "borderTable";
		}

		final String tableHeader = "<table class=\"" + className + "\">\n";
		final StringBuilder rows = new StringBuilder();
		final String tableFooter = "</table>";
		rows.append("<thead>");

		rows.append("<tr>");
		for (final String title : INT_TABLE_HEADER) {
			rows.append("<th>");
			rows.append(title);
			rows.append("</th>");
		}
		rows.append("</tr>");
		rows.append("</thead>");

		final List<JDate> days = new ArrayList<JDate>(new TreeSet<JDate>(interestTable.keySet()));
		for (int i = startIndex; i < endEndex; i++) {
			JDate d = days.get(i);
			rows.append("<tr>");
			final List<Object> row = interestTable.get(d);
			for (final Object value : row) {
				if (value instanceof DisplayValue) {
					rows.append("<td align=right>");
				} else {
					rows.append("<td>");
				}
				rows.append(value);
				rows.append("</td>");
			}
			rows.append("</tr>");
		}

		return tableHeader + rows + tableFooter;
	}

	/**
	 * Initiate the interest rate table using the start and end date from the interest bearing product
	 * 
	 * @param interestTable
	 * @param df
	 * @param interestBearingProduct2
	 */
	private void initIterestTable(final Map<JDate, List<Object>> interestTable, final Trade trade, DSConnection ds,
			final DateFormat df, final int colNumber) {
		final JDate startDate = this.cache.getStartDate(trade);
		final JDate endDate = this.cache.getEndDate(trade);
		JDate d = startDate;
		while (d.lte(endDate)) {
			final ArrayList<Object> row = new ArrayList<Object>(colNumber);
			row.add(df.format(d.getDate(TimeZone.getDefault())).toUpperCase());
			for (int j = 1; j < colNumber; j++) {
				row.add(DISPLAYABLE_MISSING_VALUE);
			}
			interestTable.put(d, row);
			d = d.addDays(1);
		}

	}

}
