package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.TimeZone;
import java.util.Vector;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;

public class EquityBOSecurityPositionDisponibleReportStyle extends BOSecurityPositionReportStyle {

	public static final String PROCESS_DATE = "PROCESSDATE";
	public static final String ENTITY = "ENTITY";
	public static final String ACCOUNTING_CENTER = "ACCOUNTING_CENTER";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    @Override
    @SuppressWarnings("rawtypes")
    public Object getColumnValue(ReportRow row, String columnId, Vector errors)
            throws InvalidParameterException {
    	
    	JDatetime valuationDatetime = null != row ? (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME) : null;
        JDate jDate = null != valuationDatetime ? valuationDatetime.getJDate(TimeZone.getDefault()) : null;
        
        if(PROCESS_DATE.equalsIgnoreCase(columnId)){
            return jDate;
        }
            
		if (columnId.equals(ENTITY)) {
			String idEntidad = "0049";
			Book book = row.getProperty(ReportRow.BOOK);
			if(book!=null) {
				String entity = BOCreUtils.getInstance().getEntity(book.getName());
				idEntidad = BOCreUtils.getInstance().getEntityCod(entity, false);
			}
			return idEntidad;
		}
		if (columnId.equals(ACCOUNTING_CENTER)) {
			String idCentroContable = "1999";
			Book book = row.getProperty(ReportRow.BOOK);
			Product product = row.getProperty(ReportRow.PRODUCT);
			if (product!=null && book!=null) {
				String entity = BOCreUtils.getInstance().getEntity(book.getName());
				idCentroContable = BOCreUtils.getInstance().getCentroContable(product, entity, false);
			}
			return idCentroContable;
		}
    	return super.getColumnValue(row, columnId, errors);
    	
    }
	
	 private String getAttFromLE(LegalEntity entity, String atttributeName){
	        if(null!=entity && !Util.isEmpty(atttributeName)){
	            Collection<LegalEntityAttribute> attributes = entity.getLegalEntityAttributes();
	            if(!Util.isEmpty(attributes)){
	                for(LegalEntityAttribute att : attributes){
	                    if(att.getAttributeType().equalsIgnoreCase(atttributeName)){
	                        return att.getAttributeValue();
	                    }
	                }
	            }
	        }
	        return "";
	    }

	
 

}
