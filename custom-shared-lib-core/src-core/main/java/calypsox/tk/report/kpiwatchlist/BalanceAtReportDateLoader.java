/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.kpiwatchlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import calypsox.util.SantReportingUtil;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

public class BalanceAtReportDateLoader {

	private Thread innerThread;
	private final JDate processDate;
	private final Set<Integer> contractIds;

	private final Map<Integer, Double> balanceMapAtReportDate = new HashMap<Integer, Double>();

	public BalanceAtReportDateLoader(Set<Integer> contractIds, JDate processDate) {
		this.contractIds = contractIds;
		this.processDate = processDate;
		load();
	}

	private void load() {
		this.innerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				loadData();
			}

		});
		this.innerThread.start();
	}

	private void loadData() {
		if (this.contractIds.size() == 0) {
			return;
		}
		List<Integer> contractIdList = new ArrayList<Integer>(this.contractIds);

		List<List<Integer>> contractIdSubList = new ArrayList<List<Integer>>();

		final int SQL_IN_ITEM_COUNT = 999;
		int start = 0;

		for (int i = 0; i <= (contractIdList.size() / SQL_IN_ITEM_COUNT); i++) {
			int end = (i + 1) * SQL_IN_ITEM_COUNT;
			if (end > contractIdList.size()) {
				end = contractIdList.size();
			}
			final List<Integer> subList = contractIdList.subList(start, end);

			start = end;
			contractIdSubList.add(subList);
		}

		StringBuilder sqlQuery = new StringBuilder();

		buildQuery(sqlQuery, contractIdSubList);

		try {
			Vector<?> v = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).executeSelectSQL(
					sqlQuery.toString());
			buildBalanceMapAtReportDate(v);
		} catch (Exception e) {
			Log.error(this, "Cannot Load Last Balance", e);
		}

	}

	private void buildBalanceMapAtReportDate(Vector<?> v) {

		if (v.size() <= 2) {
			return;
		}

		for (int i = 2; i < v.size(); i++) {
			Vector<?> row = (Vector<?>) v.get(i);
			int contractId = Integer.valueOf((String) row.get(0));
			double prevCashMargin = Double.valueOf((String) row.get(1));
			double prevSechMargin = Double.valueOf((String) row.get(2));

			this.balanceMapAtReportDate.put(contractId, prevCashMargin + prevSechMargin);
		}

	}

	private void buildQuery(StringBuilder sqlQuery, List<List<Integer>> contractIdSubList) {

		sqlQuery.append(" SELECT mcc_id, prev_cash_margin,  prev_security_margin FROM margin_call_entries ");
		for (int i = 0; i < contractIdSubList.size(); i++) {
			if (i == 0) {
				sqlQuery.append(" WHERE ");
			} else {
				sqlQuery.append(" OR ");
			}
			sqlQuery.append("  ( mcc_id IN (");
			sqlQuery.append(Util.collectionToString(contractIdSubList.get(i)));
			sqlQuery.append(") ");
			sqlQuery.append(" AND  process_datetime = (SELECT MAX(process_datetime) from margin_call_entries  WHERE TRUNC(process_datetime) = ");
			sqlQuery.append(Util.date2SQLString(this.processDate));
			sqlQuery.append("  AND mcc_id IN (");
			sqlQuery.append(Util.collectionToString(contractIdSubList.get(i)));
			sqlQuery.append(")) ");
			sqlQuery.append(") ");
		}
		sqlQuery.append(" ORDER BY mcc_id ASC,  TRUNC(process_datetime) DESC ");
	}

	public Map<Integer, Double> getBalanceMapAtReportDate() {
		return this.balanceMapAtReportDate;
	}

	public boolean isProcessing() {
		return this.innerThread.isAlive();
	}

}
