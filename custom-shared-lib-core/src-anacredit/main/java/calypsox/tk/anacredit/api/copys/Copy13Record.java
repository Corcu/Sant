package calypsox.tk.anacredit.api.copys;

import calypsox.tk.anacredit.api.AnacreditConstants;
import com.calypso.tk.core.Util;

import java.util.TreeMap;

/**
 * Copy11 - Importes - Saldos
 */
public class Copy13Record {

	private TreeMap<Copy13Columns , String> _parsedValues = new TreeMap<>();
	private TreeMap<Copy13Columns , Object> _keeper = new TreeMap<>();
	private boolean _isOK  = true;

	private void initDefaults() {
		setValue(	Copy13Columns.ID_ENTIDAD, AnacreditConstants.STR_ENTIDAD_0049);
	}

	public Copy13Record() {
		init();
		initDefaults();
	}

	public String getLine() {
		StringBuilder sb = new StringBuilder();
		for (Copy13Columns column : Copy13Columns.values()) {
			String value = "";
			if (column.isFiller()) {
				value = column.parseValue("");
			} else {
				value = _parsedValues.get(column);
				if (Util.isEmpty(value))  {
					value = column.parseValue("");
				}
			}
			sb.append(value);
		}
		return sb.toString();
	}

	
	public boolean isOK() {
		return _isOK ;
	}

	private void init() {
		for (Copy13Columns col: Copy13Columns.values()) {
			_parsedValues.putIfAbsent(col, "");
		}
	}
	
	public boolean setValue(Copy13Columns column, Object value) {

		String parsedValue = column.parseValue(value);

		if (null == parsedValue  || parsedValue.length() == 0) {
			System.out.println("Return value is empty : " + column.toString());
		} else {
			_parsedValues.put(column, parsedValue);
			return true;
		}
		return false;
	}

	public Object getValue(Copy13Columns column) {
		Object value = _parsedValues.get(column);
		if (null == value) {
			return "";
		}
		return value;
	}

	public void keep(Copy13Columns column, Object value)  {
		_keeper.put(column, value);
	}

	public Object retrieve(Copy13Columns column)  {
		return _keeper.get(column);
	}

}
