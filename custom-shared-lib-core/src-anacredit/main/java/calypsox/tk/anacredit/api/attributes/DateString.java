package calypsox.tk.anacredit.api.attributes;

import calypsox.tk.anacredit.api.ParseUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;

import java.util.TimeZone;

public  final class DateString extends Alpha {
	
	public DateString() {
		super(8);
	}

	@Override
	public String formatValue(Object valObj) {
		
		if (valObj instanceof JDate) {
			return ParseUtil.formatDate((JDate)valObj, getLegth());
		}
		if (valObj instanceof JDatetime) {
			return ParseUtil.formatDate(((JDatetime)valObj).getJDate(TimeZone.getDefault()), getLegth());
		}
		if (valObj instanceof String) {
			return super.formatValue(valObj);
		}
		return null;
		//return super.formatValue(valObj);
	}

}
