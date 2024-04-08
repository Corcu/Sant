package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TransferReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import calypsox.apps.reporting.PirumTransferReportTemplatePanel;
import calypsox.tk.util.CalypsoObjectUtil;

public class PirumTransferReport extends Report {

	private static final long serialVersionUID = 3338034850955955520L;

	private static final String LOG_CATEGORY = PirumTransferReport.class.getSimpleName();

	public static final String DN_ACCEPT_XFER_STATUS = "PirumAcceptTransferStatus";
	public static final String DN_ACCEPT_XFER_STATUS_ONLINE = "PirumAcceptTransferStatusOnline";
	public static final String DN_ACCEPT_XFER_TYPE = "PirumAcceptTransferType";
	public static final String DN_ACCEPT_XFER_TYPE_ONLINE = "PirumAcceptTransferTypeOnline";

	public static final String DN_ACCEPT_PROCESSING_ORG = "PirumAcceptProcessingOrgName";
	public static final String DN_REJECT_TRADE_STATUS = "PirumRejectTradeStatus";
	public static final String DN_REJECT_TRADE_STATUS_ONLINE = "PirumRejectTradeStatusOnline";
	public static final String DN_REJECT_CPTY_NAME = "PirumRejectCptyName";

	private boolean _countOnly = false;
	private final Map<Long, Trade> tradeMap = new HashMap<>();
	private boolean isOnline = false;

	@Override
	public Map<?, ?> getPotentialSize() {
		try {
			_potentialSize = new HashMap<>();
			_countOnly = true;
			load(new Vector<>());
		} finally {
			_countOnly = false;
		}

		return _potentialSize;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ReportOutput load(final Vector errorMsgsP) {

		try {
			return createReport(errorMsgsP);

		} catch (final RemoteException e) {
			final String error = "Error generating Pirum Report\n";
			Log.error(LOG_CATEGORY, error, e);
			errorMsgsP.add(error + e.getMessage());
		}
		return null;

	}

	/**
	 * Create Report
	 *
	 * @param  ds
	 * @param  errors
	 * @return
	 * @throws CalypsoServiceException
	 */
	private DefaultReportOutput createReport(final List<String> errors)
			throws CalypsoServiceException {

		final DefaultReportOutput output = new StandardReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

		// Get Transfer from database
		final TransferArray xferArray = getBOTransfers();

		if (_countOnly) {
			addPotentialSize(BOTransfer.class.getName(), xferArray.size());
			return null;
		}

		for (final BOTransfer xfer : xferArray) {
			Trade trade = null;

			// Get Trade
			if(!tradeMap.containsKey(xfer.getTradeLongId())) {
				trade = CalypsoObjectUtil.getInstance().getTrade(getDSConnection(), xfer.getTradeLongId());
				tradeMap.put(xfer.getTradeLongId(), trade); //adds the trade into the Map
			} else {
				trade = tradeMap.get(xfer.getTradeLongId());
			}
			// Build the reportRow
			final ReportRow row = new ReportRow(xfer);
			row.setProperty(ReportRow.TRADE, trade);

			// Add row to reportRows
			reportRows.add(row);

		}

		// set report rows
		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
		return output;
	}

	/**
	 * Get Report Transfers
	 *
	 * @return
	 */
	private TransferArray getBOTransfers() {
		final TransferArray xferArray = new TransferArray();

		// get xferId from ReportTemplate
		final List<Long> xferIds = getTransferIdReportTemplate();

		// Check online
		isOnline = getReportTemplate().getBoolean(PirumTransferReportTemplatePanel.IS_ONLINE, false);

		if (!Util.isEmpty(xferIds) && isOnline) {
			// Get the BOTransfer using xferId
			final long xferId = xferIds.get(0);
			try {
				final BOTransfer xfer = getOnlineBOTransfer(getDSConnection(), xferId);
				if (xfer != null) {
					xferArray.add(xfer);
				}

			} catch (final CalypsoServiceException e) {
				final String msgError = String.format("Error getting BOTransfer with id %s.", xferIds);
				Log.error(LOG_CATEGORY, msgError, e);
			}
		} else {
			// Get the BOTransfers using the query
			xferArray.addAll(getBatchBOTransfers());
		}

		return xferArray;
	}

	/**
	 * BOTransfer XferId
	 *
	 * @param  dsCon
	 * @param  xferId
	 * @return
	 * @throws CalypsoServiceException
	 */
	private BOTransfer getOnlineBOTransfer(final DSConnection dsCon, final long xferId) throws CalypsoServiceException {

		// Get ProcessingOrgs
		final List<Integer> pos = getProcessingOrgsFromDV();

		// Get TransferType
		final List<String> xferTypes = getTransferTypesFromDV();

		// Get Transfer Status
		final List<String> xferStatus = getTransferStatusFromDV();

		// Get data (Counterparty and book) from ReportTemplate
		final String cptyCriteria = getCounterPartyCriteria("");
		final String bookCriteria = getBookCriteria("");

		// Build WHERE clause
		final StringBuilder where = new StringBuilder();
		where.append(" transfer_id = ").append(xferId);

		if (!Util.isEmpty(pos)) {
			where.append(" AND int_le_id IN (").append(StringUtils.join(pos, ", ")).append(") ");
		}

		if (!Util.isEmpty(xferTypes)) {
			where.append(" AND transfer_type IN ('").append(StringUtils.join(xferTypes, "', '")).append("') ");
		}

		if (!Util.isEmpty(xferStatus)) {
			where.append(" AND transfer_status IN ('").append(StringUtils.join(xferStatus, "', '")).append("') ");
		}

		if (!Util.isEmpty(cptyCriteria)) {
			where.append(" AND ").append(cptyCriteria);
		}

		if (!Util.isEmpty(bookCriteria)) {
			where.append(" AND ").append(bookCriteria);
		}

		// Get BOTransfers
		TransferArray xferArray = getDSConnection().getRemoteBackOffice().getBOTransfers(where.toString(), null);
		if (xferArray != null && xferArray.size() == 1) {

			// SDFilter TransferArray
			xferArray = filterByStaticDataFilter(xferArray);

			// TradeFilter TransferArray
			xferArray = filterByTradeFilter(xferArray);

			return xferArray != null && !xferArray.isEmpty() ? xferArray.firstElement() : null; // Only one BOTransfer

		} else {
			final String msgError = String.format("Not found any BOTransfer with id %s.", xferId);
			Log.debug(LOG_CATEGORY, msgError);
		}

		return null;
	}

	/**
	 * Get BOTransfer using Query.
	 *
	 * @return
	 */
	private TransferArray getBatchBOTransfers() {

		TransferArray xferArray = new TransferArray();

		// Execute query to get the xfersIds
		final List<Long> xferIds = getBOTransferIds();

		if (!Util.isEmpty(xferIds)) {
			// Get the BOTransfers
			final long[] arrayXferIds = xferIds.stream().mapToLong(l -> l).toArray();
			xferArray = getBOTransfers(arrayXferIds);

			// SDFilter TransferArray
			xferArray = filterByStaticDataFilter(xferArray);

			// TradeFilter TransferArray
			xferArray = filterByTradeFilter(xferArray);

		}

		return xferArray;
	}

	/**
	 * Get Report BOTransfer Ids from Query.
	 *
	 * @return
	 */
	private List<Long> getBOTransferIds() {
		List<Long> xferIds = null;

		// Get query
		final StringBuilder query = getQuery();

		// Execute query
		final Vector<Vector<Object>> rawData = executeSelect(query.toString());
		if (rawData != null) {
			xferIds = getXferIdList(Long.class, rawData);
		}

		return xferIds;
	}

	/**
	 * Execute Select SQL
	 *
	 * @param  type
	 * @param  query
	 * @return
	 */
	protected Vector<Vector<Object>> executeSelect(final String query) {
		Vector<Vector<Object>> rst = new Vector<Vector<Object>>();
		Vector<?> rawResultSet;
		try {
			rawResultSet = getDSConnection().getRemoteAccess().executeSelectSQL(query, null);
			rst = getData(rawResultSet, 0, Object.class);
		} catch (final RemoteException e) {
			Log.error(this, "Cannot retrieve data from database");
			return null;
		}
		return rst;
	}

	/**
	 *
	 * @param  <T>
	 * @param  resultSet
	 * @param  indexRow
	 * @param  type
	 * @return
	 */
	private <T> Vector<Vector<T>> getData(final Vector<?> resultSet, final int indexRow,
			final Class<? extends T> type) {
		final Vector<Vector<T>> rst = new Vector<Vector<T>>();

		if (!Util.isEmpty(resultSet)) {
			for (int iRow = indexRow; iRow < resultSet.size(); iRow++) {
				final Object dataObject = resultSet.get(iRow);
				if (dataObject instanceof Vector<?>) {
					final Vector<T> r = new Vector<T>();
					final Vector<?> rowData = (Vector<?>) dataObject;
					for (final Object columnObject : rowData) {
						r.add(type.cast(columnObject));
					}
					rst.add(r);
				}
			}
		}
		return rst;
	}

	/**
	 * Get the SQL result
	 *
	 * @param  rstXferIdSql
	 * @return
	 */
	private static <T> List<T> getXferIdList(final Class<? extends T> clazz, final Vector<Vector<Object>> rstXferIdSql) {
		final List<T> xferList = new ArrayList<T>();
		if (rstXferIdSql != null && !rstXferIdSql.isEmpty()) {
			for (int iRow = 2; iRow < rstXferIdSql.size(); iRow++) {
				final Vector<Object> rowObject = rstXferIdSql.get(iRow);
				for (final Object columnObject : rowObject) {
					xferList.add(clazz.cast(columnObject));
				}
			}
		}
		return xferList;
	}

	/**
	 * Filter TransferArray by SDFilter
	 *
	 * @param  xferArray
	 * @return
	 */
	private TransferArray filterByStaticDataFilter(final TransferArray xferArray) {

		// Get SDFilter from Template
		final String sdFilterName = (String) getReportTemplate().get(TransferReportTemplate.SD_FILTER);

		if (!Util.isEmpty(sdFilterName)) {
			final TransferArray filterXferArray = new TransferArray();

			// Get SDF (based on the TransferReport class)
			final StaticDataFilter sdFilter = BOCache.getStaticDataFilter(getDSConnection(), sdFilterName);
			if (sdFilter != null) {

				for (final Iterator<BOTransfer> iterator = xferArray.iterator(); iterator.hasNext();) {
					final BOTransfer xfer = iterator.next();
					final Trade trade = xfer.getTradeLongId() > 0 ? getTrade(xfer.getTradeLongId()) : null;

					Product security = null;
					if (trade != null && (xfer.getProductId() == trade.getProduct().getId() || xfer.getProductId() == 0)) {
						if (sdFilter.accept(trade, xfer)) {
							filterXferArray.add(xfer);
						}
					} else if (xfer.getTransferType().equals("SECURITY")) {
						security = BOCache.getExchangedTradedProduct(getDSConnection(), xfer.getProductId());
						if (trade != null) {
							if (sdFilter.accept(trade, trade.getCounterParty(), trade.getRole(), security, xfer, (BOMessage) null)) {
								filterXferArray.add(xfer);
							}
						} else if (sdFilter.accept((Trade) null, (LegalEntity) null, (String) null, security, xfer,
								(BOMessage) null)) {
							filterXferArray.add(xfer);
						}
					} else if (sdFilter.accept(trade, xfer)) {
						filterXferArray.add(xfer);
					}
				}

				return filterXferArray;

			}
		}

		return xferArray;
	}

	/**
	 * Filter by TradeFilter
	 *
	 * @param  xferArray
	 * @return
	 */
	private TransferArray filterByTradeFilter(final TransferArray xferArray) {
		final String tradeFilterName = getReportTemplate().get(PirumTransferReportTemplatePanel.TRADE_FILTER);
		if (!Util.isEmpty(tradeFilterName) && !"ALL".equals(tradeFilterName)) {
			final TransferArray filterXferArray = new TransferArray();
			final TradeFilter tf = BOCache.getTradeFilter(getDSConnection(), tradeFilterName);
			if (tf != null) {

				try {
					final long[] arrayTradeIds = getDSConnection().getRemoteTrade().getTradeIds(tf, getValuationDatetime(),
							false);

					if (arrayTradeIds != null && arrayTradeIds.length > 0) {
						for (final Iterator<BOTransfer> iterator = xferArray.iterator(); iterator.hasNext();) {
							final BOTransfer xfer = iterator.next();
							final boolean isMatch = Arrays.stream(arrayTradeIds).anyMatch(tradeId -> tradeId == xfer.getTradeLongId());
							if (isMatch) {
								filterXferArray.add(xfer);
							}
						}

						return filterXferArray;
					}

				} catch (final CalypsoServiceException e) {
					Log.error(LOG_CATEGORY, "Error getting TradeIds from database.", e);
				}
			}
		}

		return xferArray;
	}

	/**
	 * Get Trade from database
	 *
	 * @param  tradeId
	 * @return
	 */
	private Trade getTrade(final long tradeId) {
		Trade trade = null;
		try {
			// Get Trade
			if (!tradeMap.containsKey(tradeId)) {
				trade = CalypsoObjectUtil.getInstance().getTrade(getDSConnection(), tradeId);
				tradeMap.put(tradeId, trade); // adds the trade into the Map
			} else {
				trade = tradeMap.get(tradeId);
			}
		} catch (final CalypsoServiceException e) {
			Log.error(LOG_CATEGORY, "Error getting Trade " + tradeId + " from database.", e);
		}
		return trade;
	}

	/**
	 * Get BOTransfers from array ids.
	 *
	 * @param  arrayXferIds
	 * @return
	 */
	private TransferArray getBOTransfers(final long[] arrayXferIds) {
		TransferArray xferArray = null;

		try {
			xferArray = getDSConnection().getRemoteBackOffice().getTransfers(arrayXferIds);
		} catch (final CalypsoServiceException e) {
			Log.error(LOG_CATEGORY, "Error getting BOTransfers from array.", e);
		}

		return xferArray;
	}

	/**
	 * Get processingOrgs Ids from DomainValues
	 *
	 * @return
	 */
	private List<Integer> getProcessingOrgsFromDV() {
		final List<Integer> pos = new ArrayList<>();
		// Get POs from DomainValues
		final List<String> values = DomainValues.values(DN_ACCEPT_PROCESSING_ORG);
		if (!Util.isEmpty(values)) {

			for (final String poName : values) {
				final int poId = BOCache.getLegalEntityId(getDSConnection(), poName);
				pos.add(poId);
			}
		}

		return pos;
	}

	/**
	 * Get TransferTypes from DomainValues
	 *
	 * @return
	 */
	private List<String> getTransferTypesFromDV() {
		final List<String> xferTypes = new ArrayList<>();
		// Get XferTypes from DomainValues
		final List<String> values = isOnline ? DomainValues.values(DN_ACCEPT_XFER_TYPE_ONLINE)
				: DomainValues.values(DN_ACCEPT_XFER_TYPE);
		if (!Util.isEmpty(values)) {
			xferTypes.addAll(values);
		}
		return xferTypes;
	}

	/**
	 * Get TransferStatus from DomainValues
	 *
	 * @return
	 */
	private List<String> getTransferStatusFromDV() {
		final List<String> xferStatus = new ArrayList<>();
		// Get Status from DomainValues
		final List<String> values = isOnline ? DomainValues.values(DN_ACCEPT_XFER_STATUS_ONLINE)
				: DomainValues.values(DN_ACCEPT_XFER_STATUS);
		if (!Util.isEmpty(values)) {
			xferStatus.addAll(values);
		}
		return xferStatus;
	}

	/**
	 * Get TradeStatus from DomainValues
	 *
	 * @return
	 */
	private List<String> getTradeStatusFromDV() {
		final List<String> tradeStatus = new ArrayList<>();
		// Get Status from DomainValues
		final List<String> values = isOnline ? DomainValues.values(DN_REJECT_TRADE_STATUS_ONLINE)
				: DomainValues.values(DN_REJECT_TRADE_STATUS);
		if (!Util.isEmpty(values)) {
			tradeStatus.addAll(values);
		}
		return tradeStatus;
	}

	/**
	 * Get rejected Cpty Names from DomainValues
	 *
	 * @return
	 */
	private List<Integer> getCptyNamesFromDV() {
		final List<Integer> pos = new ArrayList<>();
		// Get POs from DomainValues
		final List<String> values = DomainValues.values(DN_REJECT_CPTY_NAME);

		if (!Util.isEmpty(values)) {
			for (final String poName : values) {
				final int poId = BOCache.getLegalEntityId(getDSConnection(), poName);
				pos.add(poId);
			}
		}

		return pos;
	}

	/**
	 * Get Query Repo Vivas
	 *
	 * @param  sqlDate
	 * @param  pos
	 * @param  xferTypes
	 * @param  xferStatus
	 * @param  tradeStatus
	 * @param  xferId
	 * @param  cptyCriteria
	 * @param  bookCriteria
	 * @return
	 */
	private StringBuilder getQueryRepoVivas(final String sqlDate, final List<Integer> pos, final List<String> xferTypes,
			final List<String> xferStatus, final List<String> tradeStatus, final List<Long> xferIds, final String cptyCriteria,
			final String bookCriteria, final List<Integer> rejectCptys) {
		final StringBuilder query = new StringBuilder();
		query.append("   SELECT transfer_id FROM bo_transfer transfer   ");
		query.append("   INNER JOIN trade trade ON trade.trade_id = transfer.trade_id   ");
		query.append("   INNER JOIN product_desc product_desc ON trade.product_id = product_desc.product_id ");
		query.append("   INNER JOIN product_repo product_repo ON product_desc.product_id = product_repo.product_id   ");
		query.append("   WHERE product_desc.product_family IN ('Repo')   ");
		query.append("   AND transfer.is_payment = 0   ");
		query.append("   AND transfer.int_le_id != trade.CPTY_ID   ");
		query.append("   AND trade.CPTY_ID NOT IN (").append(StringUtils.join(rejectCptys, ", ")).append(") ");
		query.append("   AND ((product_repo.maturity_type IN ('TERM', 'EVERGREEN', 'CALLABLE')   ");
		query.append("        AND product_desc.maturity_date >= ").append(sqlDate).append(") ");
		query.append("    OR product_repo.maturity_type = 'OPEN') ");

		if (!Util.isEmpty(pos)) {
			query.append(" AND transfer.int_le_id IN (").append(StringUtils.join(pos, ", ")).append(") ");
		}

		if (!Util.isEmpty(xferTypes)) {
			query.append(" AND transfer.transfer_type IN ('").append(StringUtils.join(xferTypes, "', '")).append("') ");
		}

		if (!Util.isEmpty(xferStatus)) {
			query.append(" AND transfer.transfer_status IN ('").append(StringUtils.join(xferStatus, "', '")).append("') ");
		}

		if (!Util.isEmpty(tradeStatus)) {
			query.append(" AND trade.trade_status NOT IN ('").append(StringUtils.join(tradeStatus, "', '")).append("') ");
		}

		if (!Util.isEmpty(cptyCriteria)) {
			query.append(" AND ").append(cptyCriteria);
		}

		if (!Util.isEmpty(bookCriteria)) {
			query.append(" AND ").append(bookCriteria);
		}

		if (!Util.isEmpty(xferIds)) {
			query.append(" AND ").append(" transfer.transfer_id IN (").append(StringUtils.join(xferIds, ", ")).append(")");
		}
		return query;
	}

	/**
	 * Get Query Repo SETTLED Vivas
	 *
	 * @param sqlDate
	 * @param pos
	 * @param xferTypes
	 * @param xferStatus
	 * @param tradeStatus
	 * @param xferId
	 * @param cptyCriteria
	 * @param bookCriteria
	 * @return
	 */
	private StringBuilder getQueryRepoSettledVivas(final String sqlDate, final List<Integer> pos,
			final List<String> xferTypes, final List<String> xferStatus, final List<String> tradeStatus,
			final List<Long> xferIds, final String cptyCriteria, final String bookCriteria,
			final List<Integer> rejectCptys) {
		final StringBuilder query = new StringBuilder();
		query.append("   (SELECT transfer_id FROM bo_transfer transfer   ");
		query.append("   INNER JOIN trade trade ON trade.trade_id = transfer.trade_id   ");
		query.append(
				"   INNER JOIN product_desc product_desc ON trade.product_id = product_desc.product_id ");
		query.append("   INNER JOIN product_repo product_repo ON product_desc.product_id = product_repo.product_id   ");
		query.append("   WHERE product_desc.product_family IN ('Repo')   ");
		query.append("   AND transfer.is_payment = 0   ");
		query.append("   AND transfer.int_le_id != trade.CPTY_ID   ");
		query.append("   AND trade.CPTY_ID NOT IN (").append(StringUtils.join(rejectCptys, ", ")).append(") ");
		// query.append(" AND transfer.value_date <= ").append(sqlDate).append("+ 7");
		query.append("   AND ((product_repo.maturity_type IN ('TERM', 'EVERGREEN', 'CALLABLE')   ");
		query.append("        AND product_desc.maturity_date >= ").append(sqlDate).append(") ");
		query.append("    OR product_repo.maturity_type = 'OPEN') ");

		if (!Util.isEmpty(pos)) {
			query.append(" AND transfer.int_le_id IN (").append(StringUtils.join(pos, ", ")).append(") ");
		}

		if (!Util.isEmpty(xferTypes)) {
			query.append(" AND transfer.transfer_type IN ('").append(StringUtils.join(xferTypes, "', '")).append("') ");
		}

		query.append(" AND transfer.transfer_status IN ('SETTLED')");

		if (!Util.isEmpty(tradeStatus)) {
			query.append(" AND trade.trade_status NOT IN ('").append(StringUtils.join(tradeStatus, "', '"))
			.append("') ");
		}

		if (!Util.isEmpty(cptyCriteria)) {
			query.append(" AND ").append(cptyCriteria);
		}

		if (!Util.isEmpty(bookCriteria)) {
			query.append(" AND ").append(bookCriteria);
		}

		if (!Util.isEmpty(xferIds)) {
			query.append(" AND ").append(" transfer.transfer_id IN (").append(StringUtils.join(xferIds, ", "))
			.append(")");
		}

		final StringBuilder lastSettledQuery = getQueryLastSettledXferPerTrade(pos, xferTypes, 0);

		query.append(lastSettledQuery);

		return query;
	}

	/**
	 * Get Query SecLending Vivas
	 *
	 * @param  date
	 * @param  pos
	 * @param  xferTypes
	 * @param  xferStatus
	 * @param  tradeStatus
	 * @param  xferId
	 * @param  cptyCriteria
	 * @param  bookCriteria
	 * @return
	 */
	private StringBuilder getQuerySecLendingVivas(final String date, final List<Integer> pos,
			final List<String> xferTypes, final List<String> xferStatus, final List<String> tradeStatus, final List<Long> xferIds,
			final String cptyCriteria,
			final String bookCriteria, final List<Integer> rejectCptys) {
		final StringBuilder query = new StringBuilder();
		query.append("   SELECT transfer_id FROM bo_transfer transfer   ");
		query.append("   INNER JOIN trade trade ON trade.trade_id = transfer.trade_id   ");
		query.append("   INNER JOIN product_desc product_desc ON trade.product_id = product_desc.product_id ");
		query.append("   INNER JOIN product_seclending product_seclending ON product_desc.product_id = product_seclending.product_id   ");
		query.append("   WHERE product_desc.product_family IN ('SecurityLending')   ");
		query.append("   AND transfer.is_payment = 1   ");
		query.append("   AND transfer.int_le_id != trade.CPTY_ID   ");
		query.append("   AND trade.CPTY_ID NOT IN (").append(StringUtils.join(rejectCptys, ", ")).append(") ");
		// query.append(" AND transfer.value_date <= ").append(date).append("+7");
		query.append("   AND ((product_seclending.maturity_type = 'TERM'   ");
		query.append("        AND product_desc.maturity_date >= ").append(date).append(") ");
		query.append("    OR product_seclending.maturity_type = 'OPEN') ");

		if (!Util.isEmpty(pos)) {
			query.append(" AND transfer.int_le_id IN (").append(StringUtils.join(pos, ", ")).append(") ");
		}

		if (!Util.isEmpty(xferTypes)) {
			query.append(" AND transfer.transfer_type IN ('").append(StringUtils.join(xferTypes, "', '")).append("') ");
		}

		if (!Util.isEmpty(xferStatus)) {
			query.append(" AND transfer.transfer_status IN ('").append(StringUtils.join(xferStatus, "', '")).append("') ");
		}

		if (!Util.isEmpty(tradeStatus)) {
			query.append(" AND trade.trade_status NOT IN ('").append(StringUtils.join(tradeStatus, "', '")).append("') ");
		}

		if (!Util.isEmpty(cptyCriteria)) {
			query.append(" AND ").append(cptyCriteria);
		}

		if (!Util.isEmpty(bookCriteria)) {
			query.append(" AND ").append(bookCriteria);
		}

		if (!Util.isEmpty(xferIds)) {
			query.append(" AND ").append(" transfer.transfer_id IN (").append(StringUtils.join(xferIds, ", ")).append(")");
		}
		return query;
	}

	/**
	 * Get Query SecLending SETTLED Vivas
	 *
	 * @param date
	 * @param pos
	 * @param xferTypes
	 * @param xferStatus
	 * @param tradeStatus
	 * @param xferId
	 * @param cptyCriteria
	 * @param bookCriteria
	 * @return
	 */
	private StringBuilder getQuerySecLendingSettledVivas(final String date, final List<Integer> pos,
			final List<String> xferTypes, final List<String> xferStatus, final List<String> tradeStatus,
			final List<Long> xferIds, final String cptyCriteria, final String bookCriteria,
			final List<Integer> rejectCptys) {
		final StringBuilder query = new StringBuilder();
		query.append("   (SELECT transfer_id FROM bo_transfer transfer   ");
		query.append("   INNER JOIN trade trade ON trade.trade_id = transfer.trade_id   ");
		query.append(
				"   INNER JOIN product_desc product_desc ON trade.product_id = product_desc.product_id ");
		query.append(
				"   INNER JOIN product_seclending product_seclending ON product_desc.product_id = product_seclending.product_id   ");
		query.append("   WHERE product_desc.product_family IN ('SecurityLending')   ");
		query.append("   AND transfer.is_payment = 1   ");
		query.append("   AND transfer.int_le_id != trade.CPTY_ID   ");
		query.append("   AND trade.CPTY_ID NOT IN (").append(StringUtils.join(rejectCptys, ", ")).append(") ");
		// query.append(" AND transfer.value_date <= ").append(date).append("+7");
		query.append("   AND ((product_seclending.maturity_type = 'TERM'   ");
		query.append("        AND product_desc.maturity_date >= ").append(date).append(") ");
		query.append("    OR product_seclending.maturity_type = 'OPEN') ");

		if (!Util.isEmpty(pos)) {
			query.append(" AND transfer.int_le_id IN (").append(StringUtils.join(pos, ", ")).append(") ");
		}

		if (!Util.isEmpty(xferTypes)) {
			query.append(" AND transfer.transfer_type IN ('").append(StringUtils.join(xferTypes, "', '")).append("') ");
		}

		query.append(" AND transfer.transfer_status IN ('SETTLED')");

		if (!Util.isEmpty(tradeStatus)) {
			query.append(" AND trade.trade_status NOT IN ('").append(StringUtils.join(tradeStatus, "', '"))
			.append("') ");
		}

		if (!Util.isEmpty(cptyCriteria)) {
			query.append(" AND ").append(cptyCriteria);
		}

		if (!Util.isEmpty(bookCriteria)) {
			query.append(" AND ").append(bookCriteria);
		}

		if (!Util.isEmpty(xferIds)) {
			query.append(" AND ").append(" transfer.transfer_id IN (").append(StringUtils.join(xferIds, ", "))
			.append(")");
		}

		final StringBuilder lastSettledQuery = getQueryLastSettledXferPerTrade(pos, xferTypes, 1);

		query.append(lastSettledQuery);

		return query;
	}


	/**
	 * Get Query Repo and SecLending Vencidas
	 *
	 * @param sqlDate
	 * @param pos
	 * @param xferTypes
	 * @param tradeStatus
	 * @param xferStatus
	 * @param xferId
	 * @param cptyCriteria
	 * @param bookCriteria
	 * @return
	 */
	private StringBuilder getQueryVencidas(final String sqlDate, final List<Integer> pos, final List<String> xferTypes,
			final List<String> tradeStatus, final List<Long> xferIds, final String cptyCriteria,
			final String bookCriteria, final List<Integer> rejectCptys) {

		final StringBuilder query = new StringBuilder();
		query.append("   SELECT transfer_id FROM BO_TRANSFER transfer   ");
		query.append("   INNER JOIN trade trade ON trade.trade_id = transfer.trade_id   ");
		query.append("   INNER JOIN product_desc product_desc ON trade.product_id = product_desc.product_id ");
		query.append("   WHERE   transfer.transfer_status = 'FAILED' ");
		query.append("   AND product_desc.maturity_date <= ").append(sqlDate);
		query.append("   AND transfer.int_le_id != trade.CPTY_ID   ");
		query.append("   AND trade.CPTY_ID NOT IN (").append(StringUtils.join(rejectCptys, ", ")).append(") ");
		query.append("   AND (   ");
		query.append("       product_desc.product_family IN ('Repo')    ");
		query.append("       OR   ");
		query.append("       ( transfer.is_payment = 1 AND product_desc.product_family IN ('SecurityLending') )   ");
		query.append("   )   ");

		if (!Util.isEmpty(pos)) {
			query.append(" AND  transfer.int_le_id IN (").append(StringUtils.join(pos, ", ")).append(") ");
		}

		if (!Util.isEmpty(xferTypes)) {
			query.append(" AND transfer.transfer_type IN ('").append(StringUtils.join(xferTypes, "', '")).append("') ");
		}

		if (!Util.isEmpty(tradeStatus)) {
			query.append(" AND trade.trade_status NOT IN ('").append(StringUtils.join(tradeStatus, "', '")).append("') ");
		}

		if (!Util.isEmpty(cptyCriteria)) {
			query.append(" AND ").append(cptyCriteria);
		}

		if (!Util.isEmpty(bookCriteria)) {
			query.append(" AND ").append(bookCriteria);
		}

		if (!Util.isEmpty(xferIds)) {
			query.append(" AND ").append(" transfer.transfer_id IN ('").append(StringUtils.join(xferIds, "', '")).append("') ");
		}

		return query;
	}

	/**
	 * SQL query to load archived trade
	 */
	private StringBuilder getQuery() {

		// Get ProcessingOrgs
		final List<Integer> processingOrgs = getProcessingOrgsFromDV();

		// Get TransferType
		final List<String> xferTypes = getTransferTypesFromDV();

		// Get Transfer Status
		final List<String> xferStatus = getTransferStatusFromDV();

		// Get Trade Status
		final List<String> tradeStatus = getTradeStatusFromDV();

		// Get data from ReportTemplate
		final List<Long> xferIds = getTransferIdReportTemplate();
		final String cptyCriteria = getCounterPartyCriteria("transfer");
		final String bookCriteria = getBookCriteria("transfer");

		// Get Date
		final String sqlDate = Util.date2SQLString(getValDate());

		// Get Reject Cpty Ids
		final List<Integer> rejectCptys = getCptyNamesFromDV();

		// Get Queries
		final StringBuilder queryRepoVivas = getQueryRepoVivas(sqlDate, processingOrgs, xferTypes, xferStatus, tradeStatus,
				xferIds, cptyCriteria, bookCriteria, rejectCptys);
		final StringBuilder queryRepoSettledVivas = getQueryRepoSettledVivas(sqlDate, processingOrgs, xferTypes,
				xferStatus, tradeStatus, xferIds, cptyCriteria, bookCriteria, rejectCptys);
		final StringBuilder querySecLendingVivas = getQuerySecLendingVivas(sqlDate, processingOrgs, xferTypes, xferStatus,
				tradeStatus, xferIds, cptyCriteria, bookCriteria, rejectCptys);
		final StringBuilder querySecLendingSettledVivas = getQuerySecLendingSettledVivas(sqlDate, processingOrgs,
				xferTypes, xferStatus, tradeStatus, xferIds, cptyCriteria, bookCriteria, rejectCptys);
		final StringBuilder queryVencidas = getQueryVencidas(sqlDate, processingOrgs, xferTypes, tradeStatus,
				xferIds, cptyCriteria, bookCriteria, rejectCptys);

		final StringBuilder query = new StringBuilder();
		query.append(queryRepoVivas);
		query.append(" UNION ");
		query.append(queryRepoSettledVivas);
		query.append(" UNION ");
		query.append(querySecLendingVivas);
		query.append(" UNION ");
		query.append(querySecLendingSettledVivas);
		query.append(" UNION ");
		query.append(queryVencidas);

		return query;
	}

	/**
	 * Get TransferId from ReportTemplate.
	 *
	 * @return
	 */
	private List<Long> getTransferIdReportTemplate() {
		final List<Long> xferIds = new ArrayList<Long>();
		final String xferIdStr = (String) getReportTemplate().get(TransferReportTemplate.XFER_ID);
		if (!Util.isEmpty(xferIdStr)) {
			final String[] parts = xferIdStr.split(",");
			// Parse to long
			for (String xfer : parts) {
				xfer = xfer.trim();
				if (NumberUtils.isParsable(xfer)) {
					xferIds.add(Long.parseLong(xfer));
				}
			}

		}
		return xferIds;
	}

	/**
	 * Get WHERE Clause for exclude the Books
	 *
	 * @return
	 */
	private String getBookCriteria(final String alias) {
		final StringBuilder where = new StringBuilder();

		final String booksName = getReportTemplate().get(TransferReportTemplate.BOOK);

		if (!Util.isEmpty(booksName)) {
			final Vector<?> books = Util.string2Vector(booksName);
			final List<Integer> booksIds = new ArrayList<>();
			for (final Iterator<?> iterator = books.iterator(); iterator.hasNext();) {
				final Object bookName = iterator.next();
				final Book b = BOCache.getBook(getDSConnection(), (String) bookName);
				if (b != null) {
					final int bookId = b.getId();
					booksIds.add(bookId);
				}
			}

			if (!Util.isEmpty(booksIds)) {

				if (!Util.isEmpty(alias)) {
					where.append(alias).append(".");
				}

				where.append("book_id NOT IN (");
				where.append(StringUtils.join(booksIds, "', '"));
				where.append(") ");
			}
		}

		return where.toString();
	}

	/**
	 * Get WHERE Clause for exclude the CounterParties.
	 *
	 * @return
	 */
	private String getCounterPartyCriteria(final String alias) {
		final StringBuilder where = new StringBuilder();

		// Get Counterparties from ReportTemplate
		final String cptysName = getReportTemplate().get(TransferReportTemplate.CPTYNAME);
		if (!Util.isEmpty(cptysName)) {
			int cptyId = 0;

			if (NumberUtils.isParsable(cptysName)) {
				cptyId = Integer.parseInt(cptysName);
			}

			List<Integer> ids = new ArrayList<>();
			boolean oldWay = false;
			if (cptyId <= 0) {
				final LegalEntity le = BOCache.getLegalEntity(this.getDSConnection(), cptysName);
				if (le != null) {
					oldWay = true;
					ids.add(le.getId());
				}
			}

			if (!oldWay) {
				ids = Util.idStringToCollection(cptysName);
			}

			if (ids.size() != 0) {

				if (!Util.isEmpty(alias)) {
					where.append(alias).append(".");
				}

				where.append("ext_le_id NOT IN (");
				where.append(StringUtils.join(ids, "', '"));
				where.append(") ");
			}
		}

		return where.toString();
	}

	/**
	 * Is responsible for obtaining, for the same trade, the last xfer in SETTLED
	 * status
	 *
	 * @return
	 */
	private StringBuilder getQueryLastSettledXferPerTrade(final List<Integer> pos, final List<String> xferTypes,
			final int isPayment) {
		final StringBuilder query = new StringBuilder();

		query.append("INTERSECT\n");
		query.append("SELECT transfer_id FROM BO_TRANSFER t1\n");
		query.append("JOIN(\n");
		query.append("SELECT trade_id, MAX(transfer_id) AS max_xfer\n");
		query.append("FROM bo_transfer transfer\n");
		query.append("WHERE transfer_status = 'SETTLED'\n");
		if (!Util.isEmpty(pos)) {
			query.append(" AND int_le_id IN (").append(StringUtils.join(pos, ", ")).append(") ");
		}
		if (!Util.isEmpty(xferTypes)) {
			query.append(" AND transfer_type IN ('").append(StringUtils.join(xferTypes, "', '")).append("') ");
		}
		query.append("   AND is_payment = " + isPayment);
		query.append("GROUP BY trade_id) ");
		query.append("t2 ON t1.trade_id = t2.trade_id AND t1.transfer_id = t2.max_xfer)");

		return query;
	}

}
