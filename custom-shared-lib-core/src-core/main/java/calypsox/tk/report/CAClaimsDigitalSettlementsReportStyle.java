package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Amount;
import com.calypso.tk.report.ReportRow;
/**
 * 
 * @author x957355
 *
 */
public class CAClaimsDigitalSettlementsReportStyle extends TransferReportStyle{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4689548656625498571L;
	
	public static final String BALANCE ="Balance";
	public static final String NETTED = "isNettedPayment"; 

	
	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors) {

		if (row == null) {
			return null;
		}
		Object rst = null;
		
		if(columnName.equals(BALANCE)){
			Double posicionDisponible = row.getProperty(CAClaimsDigitalSettlementsReport.PROPERTY_POSITION);
			if (posicionDisponible != null) {
				rst = new Amount(posicionDisponible);
			}
		} else if(columnName.equals(NETTED)) {
			BOTransfer transfer = (BOTransfer)row.getProperty("BOTransfer");
			if(transfer!=null) {
				rst = transfer.getNettingType() != null && !transfer.getNettingType().isEmpty() && transfer.getTradeLongId() == 0;
			}
			
		} else {
			rst = super.getColumnValue(row, columnName, errors);
		}
		
		return rst;
	}
}
