package calypsox.tk.anacredit.api;

import java.text.DecimalFormatSymbols;
import java.util.TreeMap;

import calypsox.tk.anacredit.api.copys.Copy3Columns;
import com.calypso.apps.cws.presentation.format.JDateFormat;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;

public class ParseUtil {

	public static final String EMPTY_SPACE = "";

	public static String formatStringWithBlankOnRight(final String value,
			final int length) {

		if(!Util.isEmpty(value)){
			final String pattern = "%-" + length + "." + length + "s";
			return String.format(pattern, value).substring(0, length);
		}
		return formatBlank("",length );
	}


	public static String formatBlank(String value, int length){
		final StringBuilder str = new StringBuilder();

		for (int i = 0; i < length; ++i) {
			str.append(' ');
		}
		return str.toString();
	}

	public static String formatUnsignedNumber(final double value,int length,
											  final int decimals, String separator) {
		if (decimals != 0) {
			if(com.calypso.tk.core.Util.isEmpty(separator)){
				length = length + 1;
			}
			final String pattern = "%0" + (length) + "." + decimals + "f";
			final DecimalFormatSymbols symbols = new DecimalFormatSymbols();

			return String.format(pattern, Math.abs(value))
					.replace(symbols.getDecimalSeparator() + "", separator);
		}

		final String pattern = "%0" + (length) + "." + decimals + "f";
		return String.format(pattern, Math.abs(value));
	}


	public static String formatDate(JDate date, int length) {
		if(null!=date){
			JDateFormat format = new JDateFormat("yyyyMMdd");
			return format.format(date);
		}
		return formatBlank("", length);
	}

	public static String getLine(TreeMap<Copy3Columns , String> parsedValues) {
		StringBuilder sb = new StringBuilder();
		for (Copy3Columns column : Copy3Columns.values()) {
			String value = "";
			if (column.isFiller()) {
				value = column.parseValue("");
			} else {
				value = parsedValues.get(column);
			}
			sb.append(value);
		}
		return sb.append("\n").toString();
	}

}
