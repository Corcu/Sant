package calypsox.tk.secfinance.actions;

import java.util.Locale;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.DisplayValue;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.fieldentry.FieldEntry;
import com.calypso.tk.util.fieldentry.FieldEntry.InputValidationException;

public class SBLReturnEntry extends com.calypso.tk.secfinance.actions.SBLReturnEntry {
	
	
	@SuppressWarnings("rawtypes")
	public Object stringToType(FieldEntry fieldEntry, Class type, String valString) throws InputValidationException {
	    if (Amount.class.equals(type) || type == DisplayValue.class || Double.class.equals(type)) {
	    	Double d =Util.stringToNumber(valString,Locale.US);
	    	valString = Util.numberToString(d);
	    }
		
		return super.stringToType(fieldEntry, type, valString);
	}
	


}
