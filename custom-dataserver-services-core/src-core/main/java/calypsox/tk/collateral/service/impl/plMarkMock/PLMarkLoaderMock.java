/**
 *
 */
package calypsox.tk.collateral.service.impl.plMarkMock;

import com.calypso.tk.core.Status;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.JResultSet;
import com.calypso.tk.core.sql.SQLObjectPersistor;
import com.calypso.tk.marketdata.PLMark;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author ?? & aalonsop (revision)
 * @version 2.0 Generics added 
 *
 */
public class PLMarkLoaderMock extends SQLObjectPersistor<PLMark> {

//    /private static final String TABLE_NAME = "pl_mark";

    public static final String TABLE_NAME_HISTORY = "PL_MARK_HIST";

    public static final String COLUMN_LIST = "mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status";

    public static final String SELECT_BY_ID = "SELECT mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status FROM PL_MARK_HIST where mark_id = ?";

    public static final String INSERT = "INSERT INTO PL_MARK_HIST (mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String DELETE = " DELETE FROM PL_MARK_HIST where mark_id = ? AND version_num = ?";

    public static final String SELECT_BY_TRADE_ENV_VALDATE = "SELECT mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status FROM PL_MARK_HIST WHERE trade_id = ? AND sub_id = ? AND pricing_env_name = ? AND valuation_date = ?";

    public static final String SELECT_BY_TRADE_ENV_VALDATE_WITH_NULL_SUBID = "SELECT mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status FROM PL_MARK_HIST WHERE trade_id = ? AND sub_id IS NULL AND pricing_env_name = ? AND valuation_date = ?";

    public static final String SELECT_BY_ENV_AND_DATE = "SELECT mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status FROM PL_MARK_HIST WHERE pricing_env_name = ? AND valuation_date = ?";

    public static final String SELECT_BY_TRADE_IDS_ENV_AND_DATE = "SELECT mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status FROM PL_MARK_HIST WHERE pricing_env_name = ? AND valuation_date = ? and trade_ids IN ";

    public static final String SELECT_BY_NO_WHERE = "SELECT mark_id, trade_id, pricing_env_name, valuation_date, position_or_trade, position_or_trade_version, entered_datetime, update_datetime, version_num, entered_user, sub_id, book_id, position_time, market_time, comments, status FROM PL_MARK_HIST ";

    public static final String QUALIFIED_COLUMN_LIST = "PL_MARK_HIST.mark_id, PL_MARK_HIST.trade_id, PL_MARK_HIST.pricing_env_name, PL_MARK_HIST.valuation_date, PL_MARK_HIST.position_or_trade, PL_MARK_HIST.position_or_trade_version, PL_MARK_HIST.entered_datetime, PL_MARK_HIST.update_datetime, PL_MARK_HIST.version_num, PL_MARK_HIST.entered_user, PL_MARK_HIST.sub_id, PL_MARK_HIST.book_id, PL_MARK_HIST.position_time, PL_MARK_HIST.market_time, PL_MARK_HIST.comments, PL_MARK_HIST.status";

    public static final String SELECT_LATEST_MARK_BY_TRADE_ENV_VALDATE_WITH_NULL_SUBID = " SELECT PL_MARK_HIST.mark_id  FROM PL_MARK_HIST WHERE PL_MARK_HIST.trade_id = ? AND PL_MARK_HIST.sub_id IS NULL AND PL_MARK_HIST.pricing_env_name = ? AND PL_MARK_HIST.valuation_date <= ?  ORDER BY PL_MARK_HIST.valuation_date DESC";

    public static final String SELECT_LATEST_MARK_BY_TRADE_ENV_VALDATE = " SELECT PL_MARK_HIST.mark_id  FROM PL_MARK_HIST, pl_mark_value_hist  WHERE PL_MARK_HIST.trade_id = ? AND PL_MARK_HIST.sub_id = ? AND PL_MARK_HIST.pricing_env_name = ? AND PL_MARK_HIST.valuation_date <= ?  ORDER BY PL_MARK_HIST.valuation_date DESC";

    public static final String SELECT_LATEST_BY_TRADE_ENV_VALDATE = " SELECT PL_MARK_HIST.mark_id  FROM PL_MARK_HIST, pl_mark_value_hist  WHERE PL_MARK_HIST.mark_id = pl_mark_value_hist.mark_id  AND PL_MARK_HIST.trade_id = ? AND PL_MARK_HIST.sub_id = ? AND PL_MARK_HIST.pricing_env_name = ? AND PL_MARK_HIST.valuation_date < ? AND pl_mark_value_hist.mark_name = ? AND pl_mark_value_hist.mark_type = ? AND pl_mark_value_hist.currency = ?  ORDER BY PL_MARK_HIST.valuation_date DESC";

    public static final String SELECT_LATEST_BY_TRADE_ENV_VALDATE_WITH_NULL_SUBID = " SELECT PL_MARK_HIST.mark_id  FROM PL_MARK_HIST, pl_mark_value_hist  WHERE PL_MARK_HIST.mark_id = pl_mark_value_hist.mark_id  AND PL_MARK_HIST.trade_id = ? AND PL_MARK_HIST.sub_id IS NULL AND PL_MARK_HIST.pricing_env_name = ? AND PL_MARK_HIST.valuation_date < ? AND pl_mark_value_hist.mark_name = ? AND pl_mark_value_hist.mark_type = ? AND pl_mark_value_hist.currency = ?  ORDER BY PL_MARK_HIST.valuation_date DESC";

    public static final String SELECT_MAX_DATED_MARKS_NULL_SUBID = " SELECT PL_MARK_HIST.mark_id, PL_MARK_HIST.trade_id, PL_MARK_HIST.pricing_env_name, PL_MARK_HIST.valuation_date, PL_MARK_HIST.position_or_trade, PL_MARK_HIST.position_or_trade_version, PL_MARK_HIST.entered_datetime, PL_MARK_HIST.update_datetime, PL_MARK_HIST.version_num, PL_MARK_HIST.entered_user, PL_MARK_HIST.sub_id, PL_MARK_HIST.book_id, PL_MARK_HIST.position_time, PL_MARK_HIST.market_time, PL_MARK_HIST.comments, PL_MARK_HIST.status FROM PL_MARK_HIST WHERE PL_MARK_HIST.sub_id IS NULL AND PL_MARK_HIST.valuation_date= (SELECT max(p.valuation_date) FROM PL_MARK_HIST p WHERE p.trade_id=PL_MARK_HIST.trade_id AND p.pricing_env_name=PL_MARK_HIST.pricing_env_name AND p.sub_id IS NULL GROUP BY p.trade_id,p.pricing_env_name,p.sub_id)";

    public static final String SELECT_LATEST_MARK_BY_TRADE_VALDATE_WITH_NULL_SUBID = " SELECT PL_MARK_HIST.mark_id, PL_MARK_HIST.trade_id, PL_MARK_HIST.pricing_env_name, PL_MARK_HIST.valuation_date, PL_MARK_HIST.position_or_trade, PL_MARK_HIST.position_or_trade_version, PL_MARK_HIST.entered_datetime, PL_MARK_HIST.update_datetime, PL_MARK_HIST.version_num, PL_MARK_HIST.entered_user, PL_MARK_HIST.sub_id, PL_MARK_HIST.book_id, PL_MARK_HIST.position_time, PL_MARK_HIST.market_time, PL_MARK_HIST.comments, PL_MARK_HIST.status FROM PL_MARK_HIST  WHERE PL_MARK_HIST.sub_id IS NULL AND PL_MARK_HIST.valuation_date= (SELECT max(p.valuation_date) FROM PL_MARK_HIST p WHERE p.trade_id = ? AND p.valuation_date <= ? AND p.trade_id=PL_MARK_HIST.trade_id AND p.sub_id IS NULL GROUP BY p.trade_id,p.sub_id)";

    public PLMarkLoaderMock() {
    }

    @Override
    public PLMark buildObjectFromResultSet(final JResultSet jResultSet)
            throws SQLException {
        final PLMark pLMark = new PLMark();
        int i = 1;
        pLMark.setId(jResultSet.getInt(i++));
        pLMark.setTradeLongId(jResultSet.getInt(i++));
        pLMark.setPricingEnvName(jResultSet.getString(i++));
        pLMark.setValDate(jResultSet.getJDate(i++));
        pLMark.setPositionOrTrade(jResultSet.getString(i++));
        pLMark.setPositionOrTradeVersion(jResultSet.getInt(i++));
        pLMark.setEnteredOn(jResultSet.getJDatetime(i++));
        pLMark.setUpdatedOn(jResultSet.getJDatetime(i++));
        pLMark.setVersion(jResultSet.getInt(i++));
        pLMark.setEnteredBy(jResultSet.getString(i++));
        pLMark.setSubId(jResultSet.getString(i++));
        pLMark.setBookId(jResultSet.getInt(i++));
        pLMark.setPositionTime(jResultSet.getString(i++));
        pLMark.setMarketTime(jResultSet.getString(i++));
        pLMark.setComment(jResultSet.getString(i++));
        final String str = jResultSet.getString(i++);
        Status localStatus = Status.fromString("Open");
        if (!Util.isEmpty(str)) {
            localStatus = Status.fromString(str);
        }
        pLMark.setStatus(localStatus);
        pLMark.setMarkValuesAsList(null);
        return pLMark;
    }

    @Override
    public void setParametersFromObject(final PLMark object,
                                        final PreparedStatement preparedStatement) throws SQLException {
        final PLMark pLMark = object;
        int i = 1;
        setParameter(preparedStatement, pLMark.getId(), i++);
        setParameter(preparedStatement, pLMark.getTradeLongId(), i++);
        setParameter(preparedStatement, pLMark.getPricingEnvName(), i++);
        setParameter(preparedStatement, pLMark.getValDate(), i++);
        setParameter(preparedStatement, pLMark.getPositionOrTrade(), i++);
        setParameter(preparedStatement, pLMark.getPositionOrTradeVersion(),
                i++);
        setParameter(preparedStatement, pLMark.getEnteredOn(), i++);
        setParameter(preparedStatement, pLMark.getUpdatedOn(), i++);
        setParameter(preparedStatement, pLMark.getVersion(), i++);
        setParameter(preparedStatement, pLMark.getEnteredBy(), i++);
        setParameter(preparedStatement, pLMark.getSubId(), i++);
        setParameter(preparedStatement, pLMark.getBookId(), i++);
        setParameter(preparedStatement, pLMark.getPositionTime(), i++);
        setParameter(preparedStatement, pLMark.getMarketTime(), i++);
        setParameter(preparedStatement, pLMark.getComment(), i++);
        setParameter(preparedStatement, pLMark.getStatus().toString(), i++);
    }

    @Override
    public void preSave(final List<PLMark> paramList, final Connection connection)
            throws Exception {
        // final ArrayList localArrayList = new ArrayList();
        // for (final Iterator localIterator1 = paramList
        // .iterator(); localIterator1.hasNext();) {
        // final PLMark localPLMark = (PLMark) localIterator1.next();
        // final Collection localCollection = localPLMark.getMarkValues()
        // .values();
        // final Iterator localIterator2 = localCollection.iterator();
        // while (localIterator2.hasNext()) {
        // final PLMarkValue localPLMarkValue = (PLMarkValue) localIterator2
        // .next();
        // localArrayList.add(localPLMarkValue);
        // }
        // }
        //
        // PLMarkSQL._markValueLoader.saveToDB(localArrayList,
        // connection,
        // "INSERT INTO pl_mark_value(mark_id, mark_name, mark_value,
        // adj_value, currency, display_class, display_digits, is_adjusted,
        // adj_type, adj_comment, mark_type) VALUES
        // (?,?,?,?,?,?,?,?,?,?,?)");
    }

    @Override
    public void postLoad(final List<PLMark> paramList, final Connection connection)
            throws Exception {
    }
}