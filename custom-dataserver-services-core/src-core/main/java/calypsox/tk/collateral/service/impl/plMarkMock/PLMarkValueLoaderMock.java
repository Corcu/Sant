/**
 * 
 */
package calypsox.tk.collateral.service.impl.plMarkMock;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.calypso.tk.core.sql.JResultSet;
import com.calypso.tk.core.sql.SQLObjectPersistor;
import com.calypso.tk.marketdata.PLMarkValue;

/**
 * @author ?? & aalonsop (revision)
 * @version 2.0 Generics added 
 */
public class PLMarkValueLoaderMock extends SQLObjectPersistor<PLMarkValue> {

	public static final String TABLE_NAME = "PL_MARK_VALUE_HIST";

	public static final String TABLE_NAME_HISTORY = "pl_mark_value_hist";

	public static final String COLUMN_LIST = "mark_id, mark_name, mark_value, adj_value, currency, display_class, display_digits, is_adjusted, adj_type, adj_comment, mark_type";

	public static final String INSERT = "INSERT INTO pl_mark_value_hist(mark_id, mark_name, mark_value, adj_value, currency, display_class, display_digits, is_adjusted, adj_type, adj_comment, mark_type) VALUES (?,?,?,?,?,?,?,?,?,?,?)";

	public static final String DELETE = "DELETE FROM pl_mark_value_hist WHERE mark_id = ?";

	public static final String SELECT_BY_NO_WHERE = "SELECT mark_id, mark_name, mark_value, adj_value, currency, display_class, display_digits, is_adjusted, adj_type, adj_comment, mark_type FROM pl_mark_value_hist ";

	public static final String SELECT_BY_VALUES = "SELECT mark_id, mark_name, mark_value, adj_value, currency, display_class, display_digits, is_adjusted, adj_type, adj_comment, mark_type FROM pl_mark_value_hist  WHERE mark_id = ? AND currency = ? AND mark_name = ? AND mark_type = ?";

	public PLMarkValueLoaderMock() {
	}

	@Override
	public PLMarkValue buildObjectFromResultSet(final JResultSet jResultSet) throws SQLException {
		int i = 1;

		final PLMarkValue pLMarkValue = new PLMarkValue();
		pLMarkValue.setMarkId(jResultSet.getInt(i++));
		pLMarkValue.setMarkName(jResultSet.getString(i++));
		pLMarkValue.setOriginalMarkValue(jResultSet.getDouble(i++));
		pLMarkValue.setAdjustmentValue(jResultSet.getDouble(i++));
		pLMarkValue.setCurrency(jResultSet.getString(i++));
		pLMarkValue.setDisplayClassName(jResultSet.getString(i++));
		pLMarkValue.setDisplayDigits(jResultSet.getInt(i++));
		pLMarkValue.setIsAdjusted(jResultSet.getBoolean(i++));
		pLMarkValue.setAdjustmentType(jResultSet.getString(i++));
		pLMarkValue.setAdjustmentComment(jResultSet.getString(i++));
		// pLMarkValue.setMarkType(jResultSet.getString(i++));
		return pLMarkValue;
	}

	@Override
	public void setParametersFromObject(final PLMarkValue object, final PreparedStatement preparedStatement)
			throws SQLException {
		int i = 1;

		final PLMarkValue pLMarkValue = object;
		setParameter(preparedStatement, pLMarkValue.getMarkId(), i++);
		setParameter(preparedStatement, pLMarkValue.getMarkName(), i++);
		setParameter(preparedStatement, pLMarkValue.getOriginalMarkValue(), i++);
		setParameter(preparedStatement, pLMarkValue.getAdjustmentValue(), i++);
		setParameter(preparedStatement, pLMarkValue.getCurrency(), i++);
		setParameter(preparedStatement, pLMarkValue.getDisplayClassName(), i++);
		setParameter(preparedStatement, pLMarkValue.getDisplayDigits(), i++);
		setParameter(preparedStatement, pLMarkValue.isAdjusted(), i++);
		setParameter(preparedStatement, pLMarkValue.getAdjustmentType(), i++);
		setParameter(preparedStatement, pLMarkValue.getAdjustmentComment(), i++);
		// setParameter(preparedStatement, pLMarkValue.getMarkType(), i++);
	}
}
