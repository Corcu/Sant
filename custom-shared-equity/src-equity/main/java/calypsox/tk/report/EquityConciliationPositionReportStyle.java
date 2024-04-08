package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.Inventory;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.report.BOSecurityPositionReportStyle;
import com.calypso.tk.report.ReportRow;

public class EquityConciliationPositionReportStyle extends BOSecurityPositionReportStyle {
    public static final String FHCONCILIA = "FHCONCILIA";
    private static final String SALDO = "SALDO";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        JDatetime valuationDatetime = null != row ? (JDatetime) row.getProperty("ValuationDatetime") : null;
        JDate jDate = null != valuationDatetime ? valuationDatetime.getJDate(TimeZone.getDefault()) : null;
        
        if (FHCONCILIA.equalsIgnoreCase(columnId)) {
            return jDate;
        }
        else if (SALDO.equalsIgnoreCase(columnId)) {
        	HashMap<JDate, Vector<Inventory>> positions = (HashMap)row.getProperty("POSITIONS");
            if (positions != null && positions.size() > 0) {
            	JDate firsPosDate = (JDate)positions.keySet().toArray()[0];
                String firstPosDateString = formatSaldoDate(firsPosDate);
            	
            	Amount saldo = (Amount)super.getColumnValue(row, firstPosDateString, errors);
                return formatNumeric(saldo.get(), '.', 2);
            }
            
            return null;
        }
        else {
            return super.getColumnValue(row, columnId, errors);
        }
    }

	public String formatSaldoDate(JDate date) {
    	String pattern = "dd-MMM-yyyy";
    	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    	
    	return simpleDateFormat.format(date.getDate()).toLowerCase();
    }
    
    public Object formatNumeric(double number, char decimalSeparator, int numDecimals) {
    	String myDecimalFormat = String.format("0.%0" + numDecimals + "d", 0);
    	DecimalFormat df = new DecimalFormat(myDecimalFormat);
    	df.setGroupingUsed(false);
    	DecimalFormatSymbols newSymbols = new DecimalFormatSymbols();
    	newSymbols.setDecimalSeparator(decimalSeparator);
    	df.setDecimalFormatSymbols(newSymbols);
    	return df.format(number);
    }

	@Override
	public void precalculateColumnValues(ReportRow row, String[] columns, Vector errors) {
		super.precalculateColumnValues(row, columns, errors);
		
		columns[columns.length - 1] = SALDO;
	}
}
