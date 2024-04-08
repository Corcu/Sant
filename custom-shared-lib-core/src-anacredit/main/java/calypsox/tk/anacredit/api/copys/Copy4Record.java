package calypsox.tk.anacredit.api.copys;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.attributes.FILLER;
import com.calypso.tk.core.Util;

import java.util.TreeMap;

/**
 * Copy4 - Importes - Saldos
 */
public class Copy4Record {

	private TreeMap<Copy4Columns , String> _parsedValues = new TreeMap<>();
	private TreeMap<Copy4Columns , Object> _keeper = new TreeMap<>();
	private boolean _isOK  = true;

	private void initDefaults() {

		setValue(	Copy4Columns.ID_ENTIDAD, AnacreditConstants.STR_ENTIDAD_0049);
		setValue(	Copy4Columns.ID_CENTRO_CONTABLE, AnacreditConstants.STR_ID_CENTRO_CONTABLE);
		setValue(	Copy4Columns.APLICACION_ORIGEN, AnacreditConstants.STR_ORIGEN_A003);
	}

	public Copy4Record() {
		init();
		initDefaults();
	}

	public void initializeFromCopy3(Copy3Record copy3Record) {
		setValue(Copy4Columns.FECHA_EXTRACION, copy3Record.getValue(Copy3Columns.FECHA_EXTRACCION));
		setValue(Copy4Columns.ID_CONTRATO, copy3Record.getValue(Copy3Columns.ID_CONTRATO));
		setValue(Copy4Columns.ID_CENTRO_CONTABLE, copy3Record.getValue(Copy3Columns.ID_CENTRO_CONTABLE));
		setValue(Copy4Columns.PROVINCIA_NEGOCIO, copy3Record.getValue(Copy3Columns.PROVINCIA_NEGOCIO));
		setValue(Copy4Columns.PAIS_NEGOCIO, copy3Record.getValue(Copy3Columns.PAIS_NEGOCIO));
		setValue(Copy4Columns.FILLER_TIPO_CARTEIRA, new FILLER(2));
		setValue(Copy4Columns.MONEDA, copy3Record.getValue(Copy3Columns.MONEDA));
		setValue(Copy4Columns.PRODUCTO_AC, copy3Record.getValue(Copy3Columns.PRODUCTO_AC));
		setValue(Copy4Columns.ID_PLAN_CONTABLE, AnacreditConstants.EMPTY_STRING);
		setValue(Copy4Columns.TIPO_CARTERA_IRFS9, copy3Record.getValue(Copy3Columns.TIPO_CARTERA_IFRS9));

	}

	public String getLine() {
		StringBuilder sb = new StringBuilder();
		for (Copy4Columns column : Copy4Columns.values()) {
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
		for (Copy4Columns col: Copy4Columns.values()) {
			_parsedValues.putIfAbsent(col, "");
		}
	}
	
	public boolean setValue(Copy4Columns column, Object value) {

		String parsedValue = column.parseValue(value);

		if (null == parsedValue  || parsedValue.length() == 0) {
			System.out.println("Return value is empty : " + column.toString());
		} else {
			_parsedValues.put(column, parsedValue);
			return true;
		}
		return false;
	}

	public Object getValue(Copy4Columns column) {
		Object value = _parsedValues.get(column);
		if (null == value) {
			return "";
		}
		return value;
	}

	public void keep(Copy4Columns column, Object value)  {
		_keeper.put(column, value);
	}

	public Object retrieve(Copy4Columns column)  {
		return _keeper.get(column);
	}

}
