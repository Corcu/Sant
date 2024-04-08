package calypsox.tk.anacredit.api.attributes;

import calypsox.tk.anacredit.api.ParseUtil;
import com.calypso.infra.util.Util;

public class Decimal extends Attribute {

	private DataAttribute _dataAttribute = DataAttribute.NONE;

	public Decimal(int size, int dec) {
		super (DataType.DECIMAL, size);
		setSize(size);
		setDecimals(dec);
		setSigned(false);
	}

	public Decimal(int size, int dec, boolean signed) {
		this(size, dec);
		setSigned(signed);
	}

	public Decimal(int size, int dec, boolean signed, DataAttribute dataAttribute) {
		this(size, dec);
		setSigned(signed);
		_dataAttribute = dataAttribute;
	}
	
	public final String formatValue(Object valueObj) throws Exception {
		String value = "0";
		if (DataAttribute.FORCE_NULL == _dataAttribute)  {
			value = ParseUtil.formatStringWithBlankOnRight(ParseUtil.EMPTY_SPACE, getLegth());
		} else if (valueObj instanceof Integer) {
			value = formatDouble(Double.parseDouble(valueObj.toString()));
		} else if (valueObj instanceof Double) {
			value = formatDouble((Double) valueObj);
		} else if (valueObj instanceof String) {
			if (null != valueObj) {
				if (Util.isEmpty(valueObj.toString()))  {
					value = formatDouble(0.0);
				} else {
 					value = formatDouble(Double.parseDouble(valueObj.toString()));
				}
			}
		}
		return value;
	}
	private String formatDouble(Double dbl) {
		String value;
		value = ParseUtil.formatUnsignedNumber(dbl, getSize()+getDecimals(), getDecimals(), "");

		if ((DataAttribute.SIGNAL_MINUS == _dataAttribute
			|| DataAttribute.SIGNAL_PLUS ==   _dataAttribute)
					&& (dbl != 0.0d )) {

			value =  (DataAttribute.SIGNAL_MINUS == _dataAttribute ? "-" : "+") +  value;
		} else  if (isSigned()) {
			value =  (dbl < 0 ? "-" : "+") + value;
		}
		return value;
	}
}
