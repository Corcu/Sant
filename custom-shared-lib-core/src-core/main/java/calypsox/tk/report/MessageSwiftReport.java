package calypsox.tk.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.sql.SQLQuery;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.util.MessageArray;


public class MessageSwiftReport extends com.calypso.tk.report.MessageReport {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	
	/** The constant */
	 static final String SWIFT_CONTRACT_ID = "SWIFT_CONTRACT_ID";
   
    
    
	public SQLQuery buildQuery(Vector errorMsgs) {
		try {
			
			SQLQuery query =  this.buildQuery(this.isArchived());
			
			String contractId = getAttSwiftContractID();
			if(StringUtils.isNotBlank(contractId)){
				String clause = " (bo_message.template_name IN ('MT527BONYCollateral','MT527ClearstreamCollateral','MT527EuroclearCollateral','MT527JPMorganCollateral','MT569') AND ( EXISTS "
						+ "  (SELECT 1 " + "  FROM mess_attributes "
						+ "  WHERE mess_attributes.message_id  = bo_message.message_id "
						+ "  AND (attr_name                    ='contractId' "
						+ "  OR ATTR_NAME                      = 'marginCallConfigId' "
						+ "  OR ATTR_NAME                      = 'MissingIsinContractID') "
						+ "  AND (attr_value                   = '"+contractId+"') "
						+ "  ) )) OR (bo_message.template_name ='MT558' AND bo_message.LINKED_ID IN "
						+ "  (SELECT msg2.message_id " + "  FROM bo_message msg2, " + "    mess_attributes att2 "
						+ "  WHERE att2.message_id   = msg2.message_id "
						+ "  AND msg2.template_name IN ('MT527BONYCollateral','MT527ClearstreamCollateral','MT527EuroclearCollateral','MT527JPMorganCollateral','MT569') "
						+ "  AND (att2.attr_name     ='contractId' "
						+ "  OR att2.ATTR_NAME       = 'marginCallConfigId' "
						+ "  OR att2.ATTR_NAME       = 'MissingIsinContractID') "
						+ "  AND (att2.attr_value    = '"+contractId+"') " + "  )) ";
				if(!query.getWhereClauseList().isEmpty()){
					String actClause = (String)query.getWhereClauseList().get(0);
					String newClause = "("+actClause+") AND ("+clause+")";
					query.getWhereClauseList().set(0,newClause);
				}else{
					query.getWhereClauseList().add(clause);
				}
				
			}
			
			
			return query;
		} catch (Exception arg2) {
			Log.error(this, arg2);

			return null;
		}
	}
    
    /**
     * New DefaultReportOutput
     * @return DefaultReportOutput
     */
    public DefaultReportOutput newDefaultReportOutput() {
        return new DefaultReportOutput(this);
    }
    
    /**
     * Get Filtered
     * @return List<ReportRow>
     */
    private List<ReportRow> getFiltered(ReportRow[] rowsOutput, MessageArray message) {
  
    	List<ReportRow> result = new ArrayList<ReportRow>() ;
    	int tam = rowsOutput.length;
    	for(int i=0;i<tam;i++ ){
    		ReportRow row = rowsOutput[i];
    		
    		if(row.getProperty("BOMessage") instanceof BOMessage) {
    		    BOMessage messages = (BOMessage)row.getProperty("BOMessage");
    			 if(message.indexOf(messages) != -1){
    				 result.add(row);
    			 }
    		}
    	}
    	
		return result;
	}

	/**
     * Gets the rows.
     *
     * @param trades the trades
     * @return the rows
     */    
    public List<ReportRow> getRows(final List<ReportRow> reportRows) {
    	
        List<ReportRow> rows = new ArrayList<ReportRow>();
        for (int i = 0; i < reportRows.size(); i++) {
            ReportRow rowPro = reportRows.get(i);
            BOMessage messasge = rowPro.getProperty("BOMessage");
            
            String swiftContractID = getAttSwiftContractID();
            
            MessageSwiftReportRow row = new MessageSwiftReportRow(messasge, rowPro, swiftContractID);   
            rows.add(row);
        }

        return rows;
    }
    
    /**
     * Get AttToDate.
     * @return Date
     */
    public String getAttSwiftContractID() {
    	String ret = null;
    	
    	String contractId =(String)((MessageSwiftReportTemplate) getReportTemplate()).getAttributes().get("SwiftContractId");
    	
    	if(StringUtils.isNotBlank(contractId)){
    		ret = contractId; 
    	}
    	
        return ret;
    }

    /**
     * Written for Junits
     *
     * @param output the DefaultReportOutput
     * @param rowArray the ReportRow[]
     */
    public DefaultReportOutput setRows(final DefaultReportOutput output, final List<ReportRow> rowArray) {
        output.setRows(rowArray.toArray(new ReportRow[rowArray.size()]));
        return output;
    }
}