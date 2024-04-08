package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;

public class MarginCallEntryCustomFailedReportStyle extends MarginCallEntryReportStyle{


	private static final long serialVersionUID = 7809298377293561605L;
	
	//COLUMNS
	public static final  String NUMBER_OF_SHAPES = "Number of shapes (days)";
	public static final  String STATUS_OF_MC = "Status of MC";
	public static final  String FIRST_UNSETTLED_TRADE = "First Unsettled trade";
	public static final  String PENDING_VALUE = "Pending Value";
	public static final  String PENDING_NOMINAL ="Pending Nominal";
	public static final  String DEPARTAMENTO ="Department";
	public static final  String ENTIDAD = "Entity";
	
	@Override
	@SuppressWarnings({ "rawtypes" })
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
		
		Object rst = null;
		if(columnId.equals(NUMBER_OF_SHAPES)) {
			rst= row.getProperty("NumberOfShapes")!=null?row.getProperty("NumberOfShapes"):0;

		} else if(columnId.equals(STATUS_OF_MC)) {
			MarginCallEntryDTO entry = (MarginCallEntryDTO)row.getProperty("Default");
			Map<String,String> MCStatus = row.getProperty("MarginCallStatus");
			String status = entry.getStatus();
			rst = MCStatus.get(status);
			
			
		} else if (columnId.equals(FIRST_UNSETTLED_TRADE)) {
			rst = row.getProperty("FirstTranferDate")!=null?row.getProperty("FirstTranferDate"):null;	
		} else if (columnId.equals(PENDING_VALUE)) {
			rst = row.getProperty("PendingValue")!=null?row.getProperty("PendingValue"):0;
		} else if (columnId.equals(PENDING_NOMINAL)) {
			rst = row.getProperty("PendingNominal")!=null?row.getProperty("PendingNominal"):0;
			
		} else if (columnId.equals(DEPARTAMENTO)) {
			CollateralConfig config = (CollateralConfig)row.getProperty("MarginCallConfig");
			rst = config.getBook().getAttribute("Desk");			
		} else if (columnId.equals(ENTIDAD)) {
			CollateralConfig config = (CollateralConfig)row.getProperty("MarginCallConfig");
			rst = config.getProcessingOrg().getCode();
		} else {
			rst = super.getColumnValue(row, columnId, errors);
		}
		
		return rst;
	}
	

}
