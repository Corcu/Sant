package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.ReportRow;

public class EquityIssuerReportStyle extends EquityReportStyle {

	public static final String ISSUER_EXTERNAL_REF = "Issuer External Ref";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
		Equity equity = (Equity)row.getProperty(ReportRow.DEFAULT);
		
		if (columnId.equals(ISSUER_EXTERNAL_REF)) {
			LegalEntity issuer = equity.getIssuer();
			if(issuer!=null)
				return equity.getIssuer().getExternalRef();
			else 
				return "";
		}
		
		return super.getColumnValue(row, columnId, errors);
		
	}

}
