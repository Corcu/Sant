package calypsox.tk.collateral.service.impl.plMark;

import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.AuthorizableSQL;
import com.calypso.tk.core.sql.JResultSet;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.refdata.sql.DomainValuesSQL;
import com.calypso.tk.util.PLMarkSet;

import java.sql.*;
import java.util.*;

/**
 * @author ?? & aalonsop (Refactor)
 * @version 2.0 Refactored all generic and deprecated warnings
 */
public class PLMarkHistSQL extends AuthorizableSQL<PLMark> {

    private static PLMarkLoader _plMarkLoader = new PLMarkLoader();
    private static PLMarkValueLoader _markValueLoader = new PLMarkValueLoader();

    public PLMarkHistSQL() {
    }

    public static PLMark get(final long markId) throws PersistenceException {
        final PLMark pLMark = (PLMark) _plMarkLoader.loadFromDB(PLMarkLoader.SELECT_BY_ID,
                new Object[]{markId});
        loadPLMarkValues(pLMark);

        return pLMark;
    }

    public static PLMark get(final long tradeId, final String subId, final String pricingEnvName,
                             final JDate valuationDate) throws PersistenceException {
        PLMark pLMark = getFromCache(
                new String(tradeId + "_" + subId + "_" + pricingEnvName + "_" + valuationDate.toString()));
        if (pLMark != null) {
            return pLMark;
        }

        Object[] values = null;

        if ((subId == null) || ((isOracle()) && (subId.equals("")))) {
            values = new Object[]{tradeId, pricingEnvName, valuationDate};
            pLMark = _plMarkLoader.loadFromDB(PLMarkLoader.SELECT_BY_TRADE_ENV_VALDATE_WITH_NULL_SUBID,
                    values);
        } else {
            values = new Object[]{tradeId, subId, pricingEnvName, valuationDate};
            pLMark = _plMarkLoader.loadFromDB(PLMarkLoader.SELECT_BY_TRADE_ENV_VALDATE, values);
        }
        loadPLMarkValues(pLMark);

        return pLMark;
    }

    private static PLMark get(final int markId, final String markName, final String currency, final String markType)
            throws PersistenceException {
        final PLMark pLMark = (PLMark) _plMarkLoader.loadFromDB(
                "SELECT mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status FROM SANT_PL_MARK_HIST where mark_id = ?",
                new Object[]{new Integer(markId)});
        loadPLMarkValue(pLMark, markName, currency, markType);
        return pLMark;
    }

    public static Set<PLMark> load(final Map<Integer, HashSet<String>> tradeMap, final String pricingEnvName,
                                   final JDate valuationDate) throws PersistenceException {
        final HashSet<Integer> markIdSet = new HashSet<>();
        final HashMap<Integer, PLMark> pLMarkMap = new HashMap<>();
        @SuppressWarnings("unchecked") final List<String> listStringsOfIntegers = returnStringsOfIntegers(tradeMap.keySet());
        List<PLMark> listResultDB;
        for (int i = 0; i < listStringsOfIntegers.size(); i++) {
            final String query = "SELECT mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status FROM SANT_PL_MARK_HIST  WHERE pricing_env_name = "
                    + string2SQLString(pricingEnvName) + " " + "AND valuation_date = " + date2String(valuationDate)
                    + " " + " and trade_id IN (" + listStringsOfIntegers.get(i) + ")";

            listResultDB = _plMarkLoader.listFromDB(query, null);

            for (final Object oResult : listResultDB) {
                final PLMark pLMark = (PLMark) oResult;
                markIdSet.add(Integer.valueOf(pLMark.getId()));
                pLMarkMap.put(Integer.valueOf(pLMark.getId()), pLMark);
            }
        }
        loadPLMarkValues(markIdSet, pLMarkMap);
        final PLMarkSet<PLMark> pLMarkSet = new PLMarkSet<PLMark>();
        for (final Iterator<Integer> it = pLMarkMap.keySet().iterator(); it.hasNext(); ) {
            pLMarkSet.add(pLMarkMap.get(it.next()));
        }
        return pLMarkSet;
    }

    private static void loadPLMarkValues(final PLMark pLMark) throws PersistenceException {
        if (pLMark == null) {
            return;
        }
        final String query = "SELECT mark_id, mark_name, mark_value, adj_value, currency, display_class, display_digits, is_adjusted, adj_type, adj_comment, mark_type FROM SANT_PL_MARK_VALUE_HIST  WHERE mark_id = "
                + pLMark.getId();

        final List<PLMarkValue> listResultDB = _markValueLoader.listFromDB(query, null);
        for (PLMarkValue pLMarkValue : listResultDB) {
            if (!Util.isEmpty(pLMarkValue.getMarkName())) {
                pLMark.addPLMarkValue(pLMarkValue);
            }
        }
    }

    private static void loadPLMarkValues(final List<PLMark> listPLMark) throws PersistenceException {
        final HashSet<Integer> markIdSet = new HashSet<>();
        final HashMap<Integer, PLMark> pLMarkMap = new HashMap<>();
        for (final PLMark pLMark : listPLMark) {
            markIdSet.add(Integer.valueOf(pLMark.getId()));
            pLMarkMap.put(Integer.valueOf(pLMark.getId()), pLMark);
        }
        loadPLMarkValues(markIdSet, pLMarkMap);
    }

    private static void loadPLMarkValues(final Set<Integer> markIdSet, final Map<Integer, PLMark> pLMarkMap)
            throws PersistenceException {
        @SuppressWarnings("unchecked") final List<String> listMarkId = returnStringsOfIntegers(markIdSet);
        for (int i = 0; i < listMarkId.size(); i++) {
            final String query = "SELECT mark_id, mark_name, mark_value, adj_value, currency, display_class, display_digits, is_adjusted, adj_type, adj_comment FROM SANT_PL_MARK_VALUE_HIST  WHERE mark_id IN ("
                    + listMarkId.get(i) + ")";

            final List<PLMarkValue> listResultDB = _markValueLoader.listFromDB(query, null);
            for (PLMarkValue pLMarkValue : listResultDB) {
                final PLMark pLMark = pLMarkMap.get(Integer.valueOf(pLMarkValue.getMarkId()));
                if (!Util.isEmpty(pLMarkValue.getMarkName())) {
                    pLMark.addPLMarkValue(pLMarkValue);
                }
            }
        }
    }

    private static void loadPLMarkValue(final PLMark pLMark, final String markName, final String currency,
                                        final String markType) throws PersistenceException {
        if (pLMark == null) {
            return;
        }
        final Object[] values = {Integer.valueOf(pLMark.getId()), currency, markName, markType};
        final PLMarkValue listResultDB = (PLMarkValue) _markValueLoader.loadFromDB(PLMarkValueLoader.SELECT_BY_VALUES,
                values);

        if (!Util.isEmpty(listResultDB.getMarkName())) {
            pLMark.addPLMarkValue(listResultDB);
        }
    }

    public static Collection<PLMark> get(final String pricingEnvName, final JDate valuationDate)
            throws PersistenceException {
        final List<PLMark> listResultDB = _plMarkLoader.listFromDB(PLMarkLoader.SELECT_BY_ENV_AND_DATE,
                new Object[]{pricingEnvName, valuationDate});

        loadPLMarkValues(listResultDB);
        return listResultDB;
    }

    public static int count(final String clause) throws PersistenceException {
        String query = "SELECT count(*) FROM SANT_PL_MARK_HIST";
        if (!Util.isEmpty(clause)) {
            query = query + " WHERE " + clause;
        }
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        int i = 0;
        try {
            connection = ioSQL.getConnection();
            statement = ioSQL.newStatement(connection);
            resultSet = statement.executeQuery(query);
            resultSet.next();
            i = resultSet.getInt(1);
            resultSet.close();
        } catch (final SQLException sqlException) {
            Log.error(Log.SQL, sqlException);
            throw new PersistenceException(sqlException);
        } finally {
            ioSQL.close(statement);
            ioSQL.releaseConnection(connection);
        }

        return i;
    }

    public static Collection<PLMark> get(final String clause) throws PersistenceException {
        List<PLMark> listResultDB = null;
        String query = "SELECT mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status FROM SANT_PL_MARK_HIST ";
        if (!Util.isEmpty(clause)) {
            query = query + " WHERE " + clause;
        }
        listResultDB = _plMarkLoader.listFromDB(query, new Object[0]);
        loadPLMarkValues(listResultDB);
        return listResultDB;
    }

    public static Vector<Integer> getPLMarkIds(final Connection connection, final String clause)
            throws PersistenceException {
        final Vector<Integer> vPLMarksIds = new Vector<>();
        Statement statement = null;
        JResultSet jResultSet = null;
        try {
            statement = ioSQL.newStatement(connection);
            final StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("SELECT mark_id FROM ").append(PLMarkLoader.TABLE_NAME_HISTORY);
            if ((clause != null) && (clause.trim().length() > 0)) {
                stringBuffer.append(" WHERE ").append(clause);
            }
            jResultSet = new JResultSet(statement.executeQuery(stringBuffer.toString()));
            while (jResultSet.next()) {
                vPLMarksIds.add(Integer.valueOf(jResultSet.getInt(1)));
            }
            jResultSet.close();
        } catch (final SQLException sqlException) {
            Log.error(Log.SQL, "Exception while getPLMarkIds", sqlException);
            throw new PersistenceException(sqlException);
        } finally {
            ioSQL.close(statement);
        }
        return vPLMarksIds;
    }

    public static PLMark getLatest(final long tradeId, final String subId, final String pricingEnvName,
                                   final JDate valuationDate, final String markName, final String markType, final String currency)
            throws PersistenceException {
        PLMark pLMark = null;
        Object[] values = null;
        int i = 0;
        if ((subId == null) || ((isOracle()) && (subId.equals("")))) {
            values = new Object[]{tradeId, pricingEnvName, valuationDate, markName, markType, currency};
            i = _plMarkLoader.getSingleIntEntry(PLMarkLoader.SELECT_LATEST_BY_TRADE_ENV_VALDATE_WITH_NULL_SUBID, values,
                    0);
        } else {
            values = new Object[]{tradeId, subId, pricingEnvName, valuationDate, markName, markType,
                    currency};
            i = _plMarkLoader.getSingleIntEntry(PLMarkLoader.SELECT_LATEST_BY_TRADE_ENV_VALDATE, values, 0);
        }
        if (i > 0) {
            pLMark = get(i, markName, currency, markType);
        }
        return pLMark;
    }

    public static PLMark getLatest(final long tradeId, final String subId, final String pricingEnvName,
                                   final JDate valuationDate) throws PersistenceException {
        PLMark pLMark = null;
        Object[] values = null;
        if (!Util.isEmpty(pricingEnvName)) {
            int i = 0;
            if ((subId == null) || ((isOracle()) && (subId.equals("")))) {
                values = new Object[]{tradeId, pricingEnvName, valuationDate};
                i = _plMarkLoader.getSingleIntEntry(
                        PLMarkLoader.SELECT_LATEST_MARK_BY_TRADE_ENV_VALDATE_WITH_NULL_SUBID, values, 0);
            } else {
                values = new Object[]{tradeId, subId, pricingEnvName, valuationDate};
                i = _plMarkLoader.getSingleIntEntry(PLMarkLoader.SELECT_LATEST_MARK_BY_TRADE_ENV_VALDATE, values, 0);
            }
            if (i > 0) {
                pLMark = get(i);
            }
        } else {
            List<PLMark> listResultDB = null;
            if ((subId == null) || ((isOracle()) && (subId.equals("")))) {
                values = new Object[]{tradeId, valuationDate};
                listResultDB = _plMarkLoader
                        .listFromDB(PLMarkLoader.SELECT_LATEST_MARK_BY_TRADE_VALDATE_WITH_NULL_SUBID, values);
                loadPLMarkValues(listResultDB);
            }

            if (!Util.isEmpty(listResultDB)) {
                pLMark = (PLMark) listResultDB.get(0);
            }
        }
        return pLMark;
    }

    public static Collection<PLMark> getMaxDatedPLMarks(final String clause, final boolean existsSubId)
            throws PersistenceException {
        List<PLMark> listResultDB = null;
        if (!existsSubId) {

            final StringBuffer stringBuffer = new StringBuffer(PLMarkLoader.SELECT_MAX_DATED_MARKS_NULL_SUBID);
            if (!Util.isEmpty(clause)) {
                stringBuffer.append(" AND " + clause);
            }
            listResultDB = _plMarkLoader.listFromDB(stringBuffer.toString(), null);
            loadPLMarkValues(listResultDB);
        }
        return listResultDB;
    }

    static boolean allowAudit(final Connection connection) throws PersistenceException {
        final Vector<String> vDomainValues = DomainValuesSQL.getDomains(connection).getDomainValues("classAuditMode");

        if (!vDomainValues.contains("PLMark")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean save(final PLMark authorizable, final Connection connection) throws PersistenceException {
        // save((PLMark) authorizable, connection, true, true, true);
        return true;
    }

    @Override
    public boolean remove(final PLMark authorizable, final Connection connection) throws PersistenceException {
        // return remove((PLMark) authorizable, connection, true);
        return true;
    }

    @Override
    public PLMark find(final long markId, final String ref, final Connection connection) throws PersistenceException {
        return get(markId);
    }

    @Override
    public boolean updateCache(final PLMark authorizable) throws PersistenceException {
        return true;
    }

    @Override
    public boolean removeFromCache(final PLMark authorizable) throws PersistenceException {
        return true;
    }

    public static void putInCache(final PLMark paramPLMark) {
    }

    public static void putInCache(final Vector<PLMark> paramVector) {
    }

    public static PLMark getFromCache(final String paramString) {
        return null;
    }

    public static boolean isPresentInCache(final String paramString) {
        return false;
    }

    public static boolean checkMarkTime(final long tradeId, final String pricingEnvName, final JDate valuationDate,
                                        final JDatetime referenceJDatetime) throws PersistenceException {
        final ArrayList<PLMark> listMarks = new ArrayList<>();

        final String str = "pricing_env_name = " + ioSQL.string2SQLString(pricingEnvName) + " AND valuation_date = "
                + ioSQL.date2String(valuationDate) + " AND trade_id in (" + tradeId + ")";

        listMarks.addAll(get(str));

        for (PLMark pLMark : listMarks) {
            JDatetime jUpdateOn = pLMark.getUpdatedOn();
            if ((jUpdateOn == null) || (jUpdateOn.before(pLMark.getEnteredOn()))) {
                jUpdateOn = pLMark.getEnteredOn();
            }
            if (jUpdateOn.before(referenceJDatetime)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public static boolean bookHasUnlockedMarks(final int bookId, final String pricingEnvName, final JDate valuationDate)
            throws PersistenceException {
        boolean bool = false;

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = ioSQL.getConnection();
            preparedStatement = ioSQL.newPreparedStatement(connection,
                    "SELECT count(*) FROM SANT_PL_MARK_HIST WHERE book_id = ? AND pricing_env_name = ? AND valuation_date = ? AND status != ?");
            int i = 1;
            preparedStatement.setInt(i++, bookId);
            preparedStatement.setString(i++, pricingEnvName);
            preparedStatement.setDate(i++, ioSQL.toSQLDate(valuationDate));
            preparedStatement.setString(i++, "Locked");
            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            bool = resultSet.getInt(1) > 0;
            resultSet.close();
        } catch (final SQLException sqlException) {
            Log.error(Log.SQL, sqlException);
            throw new PersistenceException(sqlException);
        } finally {
            ioSQL.close(preparedStatement);
            ioSQL.releaseConnection(connection);
        }

        return bool;
    }

    public static int countPLMarks(final String clause, final Connection connection) throws PersistenceException {
        int i = 0;
        Statement statement = null;
        JResultSet jResultSet = null;
        try {
            statement = ioSQL.newStatement(connection);
            final StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("SELECT COUNT(*) FROM ").append(PLMarkLoader.TABLE_NAME_HISTORY);
            if ((clause != null) && (clause.trim().length() > 0)) {
                stringBuffer.append(" WHERE ").append(clause);
            }
            jResultSet = new JResultSet(statement.executeQuery(stringBuffer.toString()));
            if (jResultSet.next()) {
                i = jResultSet.getInt(1);
            }
            jResultSet.close();
        } catch (final SQLException sqlException) {
            Log.error(Log.SQL, "Exception while countPLMarks", sqlException);
            throw new PersistenceException(sqlException);
        } finally {
            ioSQL.close(statement);
        }
        return i;
    }

    public static Set<PLMark> loadLatestPLMarks(final List<Integer> listTradeId, final String pricingEnvName,
                                                final JDate valuationDate) throws PersistenceException {
        final HashSet<PLMark> pLMarksSet = new HashSet<>();
        for (Integer tradeId : listTradeId) {
            final PLMark pLMark = getLatest(tradeId.intValue(), null, pricingEnvName, valuationDate);
            if (pLMark != null) {
                pLMarksSet.add(pLMark);
            }
        }
        return pLMarksSet;
    }

    public static String getPLMarkIds(final String clause, final Connection connection) throws PersistenceException {
        final StringBuffer sBufferResultSet = new StringBuffer();
        Statement statement = null;
        JResultSet jResultSet = null;
        try {
            statement = ioSQL.newStatement(connection);
            final StringBuffer sBufferQuery = new StringBuffer();
            sBufferQuery.append("SELECT mark_id FROM ").append(PLMarkLoader.TABLE_NAME_HISTORY);
            if ((clause != null) && (clause.trim().length() > 0)) {
                sBufferQuery.append(" WHERE ").append(clause);
            }
            jResultSet = new JResultSet(statement.executeQuery(sBufferQuery.toString()));
            while (jResultSet.next()) {
                if (sBufferResultSet.length() != 0) {
                    sBufferResultSet.append(",");
                }
                sBufferResultSet.append(jResultSet.getInt(1));
            }
            jResultSet.close();
        } catch (final SQLException sqlException) {
            Log.error(Log.SQL, "Exception while getPLMarkIds", sqlException);
            throw new PersistenceException(sqlException);
        } finally {
            ioSQL.close(statement);
        }
        return sBufferResultSet.toString();
    }
}