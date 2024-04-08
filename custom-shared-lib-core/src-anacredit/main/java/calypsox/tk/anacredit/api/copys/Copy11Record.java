package calypsox.tk.anacredit.api.copys;

import calypsox.tk.anacredit.api.AnacreditConstants;
import com.calypso.tk.core.Util;

import java.util.TreeMap;

/**
 * Copy11 - Importes - Saldos
 */
public class Copy11Record {

	private TreeMap<Copy11Columns , String> _parsedValues = new TreeMap<>();
	private TreeMap<Copy11Columns , Object> _keeper = new TreeMap<>();
	private boolean _isOK  = true;

	private void initDefaults() {
		setValue(	Copy11Columns.ID_ENTIDAD, AnacreditConstants.STR_ENTIDAD_0049);
	}

	public Copy11Record() {
		init();
		initDefaults();
	}

	public String getLine() {
		StringBuilder sb = new StringBuilder();
		for (Copy11Columns column : Copy11Columns.values()) {
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
		for (Copy11Columns col: Copy11Columns.values()) {
			_parsedValues.putIfAbsent(col, "");
		}
	}
	
	public boolean setValue(Copy11Columns column, Object value) {

		String parsedValue = column.parseValue(value);

		if (null == parsedValue  || parsedValue.length() == 0) {
			System.out.println("Return value is empty : " + column.toString());
		} else {
			_parsedValues.put(column, parsedValue);
			return true;
		}
		return false;
	}

	public Object getValue(Copy11Columns column) {
		Object value = _parsedValues.get(column);
		if (null == value) {
			return "";
		}
		return value;
	}

	public void keep(Copy11Columns column, Object value)  {
		_keeper.put(column, value);
	}

	public Object retrieve(Copy11Columns column)  {
		return _keeper.get(column);
	}

}
