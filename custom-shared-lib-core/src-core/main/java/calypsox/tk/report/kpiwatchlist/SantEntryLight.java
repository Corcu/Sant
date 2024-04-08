/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.kpiwatchlist;

import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.core.JDatetime;

public class SantEntryLight {

	private int contractId;
	private int entryId;
	private JDatetime processDate;
	private final Vector<String> holidays;

	public SantEntryLight(Vector<?> results, Vector<String> holidays) {
		this.holidays = holidays;
		build(results);
	}

	private void build(Vector<?> row) {
		this.contractId = Integer.valueOf((String) row.get(0));
		this.entryId = Integer.valueOf((String) row.get(1));
		this.processDate = (JDatetime) row.get(2);
	}

	public int getContractId() {
		return this.contractId;
	}

	public boolean isSameDay(SantEntryLight newest) {
		return this.processDate.getJDate(TimeZone.getDefault()).equals(newest.processDate.getJDate(TimeZone.getDefault()));
	}

	public boolean isPreviousDay(SantEntryLight newest) {
		return this.processDate.getJDate(TimeZone.getDefault()).equals(newest.processDate.getJDate(TimeZone.getDefault()).addBusinessDays(-1, this.holidays));
	}

	public boolean isSameContract(SantEntryLight newest) {
		return this.contractId == newest.contractId;
	}

	public int getEntryId() {
		return this.entryId;
	}

	public JDatetime getProcessDate() {
		return this.processDate;
	}
}
