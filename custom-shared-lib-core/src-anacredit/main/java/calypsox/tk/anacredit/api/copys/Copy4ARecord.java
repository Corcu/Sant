package calypsox.tk.anacredit.api.copys;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.items.AnacreditPersonaOperacionesItem;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;

import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;

public class Copy4ARecord {

	private TreeMap<Copy4AColumns , String> _parsedValues = new TreeMap<>();
	private TreeMap<Copy4AColumns , Object> _keeper = new TreeMap<>();
	private boolean _isOK  = true;

	private void initDefaults() {
        setValue(   Copy4AColumns.ID_ENTIDAD, AnacreditConstants.STR_ENTIDAD_0049);
		setValue(   Copy4AColumns.GRUPO_TITULARES_MANCOMUNADOS, 0);
		setValue(   Copy4AColumns.PORCENTAJE_PARTICIPACION_, 100);
		setValue(   Copy4AColumns.GRADO_RELEVANCIA_GARANTE, 0);

	}

	public Copy4ARecord() {
		init();
		initDefaults();
	}

	public boolean isOK() {
		return _isOK ;
	}

	private void init() {
		for (Copy4AColumns col: Copy4AColumns.values()) {
			_parsedValues.putIfAbsent(col, "");
		}
	}

	public void initializeFromCopy3Record(JDate valDate, Copy3Record copy3Record) {

		setValue(Copy4AColumns.FECHA_ALTA_RELACION, valDate);
		setValue(Copy4AColumns.FECHA_DATOS, valDate);
		setValue(Copy4AColumns.FECHA_BAJA_RELACION, copy3Record.getValue(Copy3Columns.FECHA_VENCIMIENTO));
		setValue(Copy4AColumns.ID_CONTRATO, copy3Record.getValue(Copy3Columns.ID_CONTRATO));
	}


	public boolean setValue(Copy4AColumns column, Object value) {

		String parsedValue = column.parseValue(value);
		if (null == parsedValue  || parsedValue.length() == 0) {
		} else {
			_parsedValues.put(column, parsedValue);
			return true;
		}
		return false;
	}

	public Object getValue(Copy4AColumns column) {
		Object value = _parsedValues.get(column);
		if (null == value) {
			return "";
		}
		return value;
	}

	public void keep(Copy4AColumns column, Object value)  {
		_keeper.put(column, value);
	}

	public Object retrieve(Copy4AColumns column)  {
		return _keeper.get(column);
	}

	public String getLine() {
		StringBuilder sb = new StringBuilder();
		for (Copy4AColumns column : Copy4AColumns.values()) {
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
}
