package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.pricer.PricerRepo;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.util.PricerMeasureUtility;

public class MICBarridoReport extends TradeReport {

	private static String REPO = "Repo";
	private static String BOND = "Bond";
	private static String MTM_NET_MUREX = "MTM_NET_MUREX";

	@Override
	public ReportOutput load(Vector errorMsgs) {
		DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);

		String products[] = getReportTemplate().getAttributes().get("ProductFamily").toString().split(",");
		HashMap<Long, ReportRow> reportsRepoRows = new HashMap<>();
		HashMap<Long, ReportRow> reportsBondRows = new HashMap<>();

		if (null != output) {
			ArrayList<ReportRow> rows = new ArrayList<>();
			Arrays.asList(products).forEach(p -> {
				if (p.equals(REPO)) {
					groupTrades(output.getRows(), reportsRepoRows, REPO);
				} else if (p.equals(BOND)) {
					groupTrades(output.getRows(), reportsBondRows, BOND);
				}
			});

			rows.addAll(addRepoRows(reportsRepoRows, output));
			rows.addAll(addBondRows(reportsBondRows, output));

			ReportRow[] arrayRows = rows.toArray(new ReportRow[0]);
			output.setRows(arrayRows);
		}

		return output;
	}

	private ArrayList<ReportRow> addRepoRows(HashMap<Long, ReportRow> repoRows, DefaultReportOutput output) {

		repoRows.forEach((tradeID, row) -> {
			Double pValue = 0D;
			PLMark plMark = new PLMark();
			Trade trade = null;
			try {
				trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeID);
				pValue = calculatePricerMeasure(MTM_NET_MUREX, trade, _pricingEnv);
			} catch (CalypsoServiceException e) {
				Log.warn(this, e.getCause());
			}

			plMark.setType(MTM_NET_MUREX);
			row.setProperty("PLMarkRepo", pValue);
			row.setProperty("VD", getValDate());
		});

		ArrayList<ReportRow> rows = new ArrayList<>();

		for (ReportRow v : repoRows.values()) {
			rows.add(v);
		}

		return rows;
	}

	protected Double calculatePricerMeasure(String pmName, Trade trade, PricingEnv env) {
		double pricerMeasureAmt = 0.0D;
		Pricer pricer = new PricerRepo();
		PricerMeasure measure = PricerMeasureUtility.makeMeasure(pmName);
		try {
			pricer.price(trade, getValuationDatetime(), env, new PricerMeasure[] { measure });
			pricerMeasureAmt = Optional.ofNullable(measure).map(PricerMeasure::getValue).orElse(0.0D);
		} catch (PricerException | NullPointerException exc) {
			Log.warn(this, exc.getCause());
		}
		return pricerMeasureAmt;
	}

	private ArrayList<ReportRow> addBondRows(HashMap<Long, ReportRow> bondRows, DefaultReportOutput output) {

		HashMap<Long, List<PLMark>> plMarksList = loadPLMarks(bondRows, getValDate());
		bondRows.forEach((tradeID, row) -> {
			List<PLMark> marks = plMarksList.get(tradeID);
			if (!Util.isEmpty(marks)) {

				for (PLMark mark : marks) {
					for (PLMarkValue v : mark.getPLMarkValuesByName(MTM_NET_MUREX)) {
						PLMark plMark = mark;
						row.setProperty("PLMark", plMark);
					}
				}

			}
			row.setProperty("VD", getValDate());
		});

		ArrayList<ReportRow> rows = new ArrayList<>();

		for (ReportRow v : bondRows.values()) {
			rows.add(v);
		}

		return rows;
	}

	private void groupTrades(ReportRow[] rows, HashMap<Long, ReportRow> reportsRows, String type) {
		Arrays.stream(rows).forEach(row -> {
			Trade trade = Optional.ofNullable(row.getProperty("Trade")).filter(obj -> obj instanceof Trade)
					.map(Trade.class::cast).orElse(null);
			if (null != trade && trade.getProductType().equals(type)) {
				reportsRows.put(trade.getLongId(), row);
			}
		});
	}

	private HashMap<Long, List<PLMark>> loadPLMarks(HashMap<Long, ReportRow> reportRows, JDate valDate) {
		PricingEnv pricingEnv = getPricingEnv();
		HashMap<Long, List<PLMark>> groupedPLMarks = new HashMap<>();

		if (null != valDate && null != pricingEnv) {
			Set<Long> longs = reportRows.keySet();
			final ArrayList<Long> tradeIdsList = new ArrayList<Long>(longs);
			final int SQL_IN_ITEM_COUNT = 999;
			int start = 0;

			try {
				if (!tradeIdsList.isEmpty()) {
					for (int i = 0; i <= (tradeIdsList.size() / SQL_IN_ITEM_COUNT); i++) {
						int end = (i + 1) * SQL_IN_ITEM_COUNT;
						if (end > tradeIdsList.size()) {
							end = tradeIdsList.size();
						}
						final List<Long> subList = tradeIdsList.subList(start, end);
						start = end;

						List<CalypsoBindVariable> bindVariables = new ArrayList<>();
						String whereClause = generateWhere(subList, valDate);

						RemoteMarketData api = getDSConnection().getRemoteMarketData();
						Collection<PLMark> plMarks = api.getPLMarks(whereClause, bindVariables);

						plMarks.forEach(plMark -> {
							long tradeLongId = plMark.getTradeLongId();
							if (groupedPLMarks.containsKey(tradeLongId)) {
								groupedPLMarks.get(tradeLongId).add(plMark);
							} else {
								List<PLMark> plmarks = new ArrayList<>();
								plmarks.add(plMark);
								groupedPLMarks.put(tradeLongId, plmarks);
							}
						});
					}
				}
			} catch (CalypsoServiceException e) {
				throw new RuntimeException(e);
			}
		}

		return groupedPLMarks;
	}

	private String generateWhere(List<Long> subList, JDate valDate) {
		StringBuilder whereClause = new StringBuilder();
		whereClause.append(" trade_id IN " + Util.collectionToSQLString(subList));
		whereClause.append(" AND mark_type IN ('PL','NONE') ");
		whereClause.append(" AND trunc(valuation_date) BETWEEN "
				+ Util.date2SQLString(valDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"))) + " AND "
				+ Util.date2SQLString(valDate));
		return whereClause.toString();
	}
}
