package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.SantMarginCallAllocationEntriesLoader;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallAllocationEntry;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

public class SantResultOfMarginCallCalculationReport extends SantReport {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	@Override
	public ReportOutput loadReport(final Vector arg0) {

		try {
			return getReportOutput();

		} catch (final Exception e) {
			Log.error(this, "Cannot load MarginCallEntry", e);
		}

		return null;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	private ReportOutput getReportOutput() throws Exception {

		DefaultReportOutput output = new DefaultReportOutput(this);
		String tradeStatus = (String) getReportTemplate().get(SantGenericTradeReportTemplate.TRADE_STATUS);
		SantMarginCallAllocationEntriesLoader loader = new SantMarginCallAllocationEntriesLoader();
		Collection<SantMarginCallAllocationEntry> santAllocations = loader.loadWithDummy(getReportTemplate(),
				getValDate());

		List<ReportRow> rows = new ArrayList<ReportRow>();
		Trade trade = null;
		for (SantMarginCallAllocationEntry alloc : santAllocations) {
			// filter on trade status				
			if (!Util.isEmpty(tradeStatus)) {
				trade = alloc.getTrade();
				if ((trade != null) && !tradeStatus.contains(trade.getStatus().getStatus())) {
					continue;
				}
			}	
			if (!alloc.isDummy()) {
				alloc.getSantEntry().addAllocationCurrency(alloc.getAllocation().getCurrency());
			}
			ReportRow row = new ReportRow(alloc, "SantMarginCallAllocationEntry");
			rows.add(row);

		}

		output.setRows(rows.toArray(new ReportRow[rows.size()]));
		return output;
	}

	}
