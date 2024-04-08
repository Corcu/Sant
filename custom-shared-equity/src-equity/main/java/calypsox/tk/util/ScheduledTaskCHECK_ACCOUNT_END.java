package calypsox.tk.util;


import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.ScheduledTask;
import java.util.ArrayList;
import java.util.List;

public class ScheduledTaskCHECK_ACCOUNT_END extends ScheduledTask {


    private static final long serialVersionUID = -1L;
    private static final String TASK_INFORMATION = "Checks if the Cres sent to mic has come back.";
    private static final String INITIAL_SLEEP_TIME = "INITIAL SLEEP TIME";
    private static final String CRE_PRODUCT_TYPE = "PRODUCT TYPE";
    private static final String CRE_EVENT_TYPES = "EVENT TYPES";
    private static final String NUMBER_OF_RETRIES = "NUMBER OF RETRIES";
    private static final String SLEEP_TIME = "SLEEP TIME";
    private static final String EMPTY_SPACE = "";


    public ScheduledTaskCHECK_ACCOUNT_END() {
        super();
    }


    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        final List<AttributeDefinition> attrDefList = new ArrayList<>();
        final AttributeDefinition initialSleepTime = attribute(INITIAL_SLEEP_TIME);
        final AttributeDefinition productType = attribute(CRE_PRODUCT_TYPE);
        final AttributeDefinition eventType = attribute(CRE_EVENT_TYPES);
        final AttributeDefinition numRetries = attribute(NUMBER_OF_RETRIES);
        final AttributeDefinition sleepTime = attribute(SLEEP_TIME);
        attrDefList.add(productType);
        attrDefList.add(eventType);
        attrDefList.add(initialSleepTime);
        attrDefList.add(numRetries);
        attrDefList.add(sleepTime);
        return attrDefList;
    }


    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }


    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        final boolean rst = true;
        final String[] eventTypes = getAttribute(CRE_EVENT_TYPES).split(",");
        final String productType = getAttribute(CRE_PRODUCT_TYPE);
        final long initialSleepTime = Long.parseLong(getAttribute(INITIAL_SLEEP_TIME));
        final int numRetries = Integer.parseInt(getAttribute(NUMBER_OF_RETRIES));
        final long sleepTime = Long.parseLong(getAttribute(SLEEP_TIME));
        JDatetime valDatetime = this.getValuationDatetime();
        JDate valDate = JDate.valueOf(valDatetime, this._timeZone);

        CreArray creArray = getCres(ds, productType, eventTypes, valDate);

        sleep(1,initialSleepTime);
        for(int i=0; i<numRetries; i++){
            sleep(numRetries, sleepTime);
            if(checkAllCresAreBack(creArray)) {
                return true;
            }
        }

        // publish task in case of arrive until here


        return true;
    }


    private boolean checkAllCresAreBack(CreArray creArray){
        Boolean allProcessed = true;
        for (BOCre cre : creArray) {
            allProcessed &= isCreProcessed(cre);
        }
        return allProcessed;
    }


    private boolean isCreProcessed(BOCre cre){
        String sentAttrStatus = cre.getAttributeValue("Cre Sent Status");
        return !Util.isEmpty(sentAttrStatus) || "OK".equalsIgnoreCase(sentAttrStatus);
    }


    private CreArray getCres(DSConnection ds, String productType, String[] eventTypes, JDate valDate){
        CreArray creArray = null;
        try {
            
            // From Clause
            StringBuilder fromClause = new StringBuilder();
            fromClause.append("cre_attribute");
            
            // Where Clause
            StringBuilder whereClause = new StringBuilder();
            whereClause.append("bo_cre_type IN (");
            for (int i=0; i < eventTypes.length; i++) {
                whereClause.append("'" + eventTypes[i] + "'");
                if((i != eventTypes.length-1)){
                    whereClause.append(",");
                }
                else{
                    whereClause.append(")");
                }
            }
            whereClause.append(" AND bo_cre_id = cre_attribute.cre_id");
            whereClause.append(" AND cre_attribute.attribute_name = 'AccountProductType'");
            whereClause.append(" AND cre_attribute.attribute_name = '" + productType + "'");
            whereClause.append(" AND sent_status = 'SENT'");
            whereClause.append(" AND sent_date IS NOT NULL");
            whereClause.append(" AND sent_date = " + Util.date2SQLString(valDate));
            whereClause.append(" AND trunc(effective_date) = " + Util.date2SQLString(valDate));
            
            creArray = DSConnection.getDefault().getRemoteBO().getBOCres(null, whereClause.toString(), null);
        
        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve CREs. " + e.toString());
            return null;
        }
        if (creArray == null || creArray.isEmpty()) {
            return null;
        }
        return creArray;
    }


    private void sleep(int numRetries, long sleepTime){
        for(int i=0; i<numRetries; i++){
            try {
                wait(sleepTime);
            } catch (InterruptedException e) {
                Log.error(this, e);
            }
        }
    }


}
