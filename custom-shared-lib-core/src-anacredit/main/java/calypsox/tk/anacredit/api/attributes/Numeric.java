package calypsox.tk.anacredit.api.attributes;

import calypsox.tk.anacredit.api.ParseUtil;
import com.calypso.tk.core.Util;

public class Numeric extends Attribute {

	public Numeric(int size) {
		super (DataType.NUMERIC, size);
	}
	
	@Override
	public String formatValue(Object o) throws Exception {
		String value = ParseUtil.EMPTY_SPACE;
		if (o instanceof Integer || o instanceof Double) {
			value = ParseUtil.formatUnsignedNumber(Double.parseDouble(o.toString()), getSize(), getDecimals(), "");
		}
		if (o instanceof String) {
			if (Util.isEmpty(o.toString())) {
				//format field with blanks hole size;
				value = ParseUtil.formatStringWithBlankOnRight( ParseUtil.EMPTY_SPACE, getSize());
			} else  {
				value = ParseUtil.formatUnsignedNumber(Double.parseDouble(o.toString()), getSize(), getDecimals(), "");
			}
		}
		
		return value;
	}

	
}
