package calypsox.tk.util;

import calypsox.tk.csdr.CSDRPenaltyPeriod;
import calypsox.tk.csdr.CSDRSecurityCategory;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.util.*;

/**
 * @author aalonsop
 */
public class ScheduledTaskUpdateCSDRTransfer extends ScheduledTask {

    private final String xferStatus="Transfer Status";
    private final String productType="Product Type";
    private final String startDate="CSDR Transfer From Date";
    private final String actionAttr="Transfer Action to Apply";


    private final String csdrTransferAttr="CSDRFailedTransferMark";
    private Action actionToApply=Action.UPDATE;

    @Override
    public String getTaskInformation() {
        return "Updates CSDR Transfer statuses";
    }


    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        parseActionToApply();

        JDate valueDate=getValuationDatetime().getJDate(TimeZone.getDefault());
        TransferReport xferReport=new TransferReport();
        xferReport.setValuationDatetime(getValuationDatetime());
        TransferReportTemplate xferTemplate=new TransferReportTemplate();
        Attributes attributes=new Attributes();
        attributes.add("StartDate",getAttribute(startDate));
        attributes.add("Status",getAttribute(xferStatus));
        attributes.add("TransferType","SECURITY");
        attributes.add("ProductType",getAttribute(productType));
        attributes.add("EndDate",getValuationDatetime().getJDate(TimeZone.getDefault()).toString());
        attributes.add("DATETYPE","Value");
        xferTemplate.setAttributes(attributes);
        xferReport.setReportTemplate(xferTemplate);
        DefaultReportOutput output= (DefaultReportOutput) xferReport.load(new Vector<>());
        if(output!=null) {
            for (ReportRow row : output.getRows()) {
                BOTransfer transfer = row.getProperty("BOTransfer");
                processFailedTransfer(transfer,valueDate);
            }
        }
        return true;
    }

    private void parseActionToApply(){
        String actionStr=getAttribute(actionAttr);
        if(!Util.isEmpty(actionStr)){
            this.actionToApply=Action.valueOf(actionStr);
        }
    }
    private void processFailedTransfer(BOTransfer transfer,JDate valueDate){
        CSDRPenaltyPeriod period=getTransferFailedPeriod(transfer,valueDate);
        if(isUpdateNeeded(transfer,period)){
            //update transfer
            try {
                transfer.setAttribute(csdrTransferAttr,period.getAttrValue());
                transfer.setAction(actionToApply);
                DSConnection.getDefault().getRemoteBO().save(transfer,0L,"");
            } catch (CalypsoServiceException exc) {
                Log.error(this,exc.getCause());
            }
        }
    }

    private boolean isUpdateNeeded(BOTransfer transfer, CSDRPenaltyPeriod period){
        String currentAttr=transfer.getAttribute(csdrTransferAttr);
        return period!=null&&!period.getAttrValue().equalsIgnoreCase(currentAttr)
                && BOTransferWorkflow.isTransferActionApplicable(transfer,null,actionToApply,DSConnection.getDefault());

    }


    private CSDRPenaltyPeriod getTransferFailedPeriod(BOTransfer xfer, JDate valueDate) {
        CSDRPenaltyPeriod period=null;
        if (Optional.ofNullable(xfer).map(BOTransfer::getTransferType)
                .map("SECURITY"::equals).orElse(false)) {
            Product security = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), xfer.getProductId());
            CSDRSecurityCategory csdrCategory = Optional.ofNullable(security).map(sec -> sec.getSecCode("CSDR_Penalty_Category"))
                    .map(CSDRSecurityCategory::lookup).orElse(null);
            String isElegible=Optional.ofNullable(security).map(sec -> sec.getSecCode("CSDR_Eligibility")).orElse("");
            if (csdrCategory != null&&"Y".equalsIgnoreCase(isElegible)) {
                period = csdrCategory.getTargetCSDRPeriod(xfer,valueDate);
            }
        }
        return period;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.add(attribute(xferStatus));
        attributeList.add(attribute(productType));
        attributeList.add(attribute(startDate));
        attributeList.add(attribute(actionAttr));
        return attributeList;
    }
}
