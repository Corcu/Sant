package calypsox.tk.bo.fiflow.taskenrichment;

import calypsox.tk.bo.fiflow.staticdata.FIFlowStaticData;
import com.calypso.taskenrichment.data.enrichment.DefaultTaskEnrichmentCustom;
import com.calypso.taskenrichment.data.enrichment.TaskEnrichmentFieldConfig;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.util.ProcessTaskUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author various
 */
public class GDisponibleTaskEnrichment extends DefaultTaskEnrichmentCustom {

    public String getGDisponibleResponse(BOMessage message) {
        return Optional.ofNullable(message)
                .map(m -> m.getAttribute(FIFlowStaticData.GDISPONIBLE_RESPONSE_MSG_ATTR)).orElse("");
    }

    public String getGdisponibleFlowId(BOMessage message) {
        return Optional.ofNullable(message)
                .map(m -> m.getAttribute(FIFlowStaticData.FLOW_ID_KEYWRD_NAME)).orElse("");
    }

    public String getMessageGDisponiblePartenonId(BOMessage message) {
        return Optional.ofNullable(message)
                .map(m -> m.getAttribute(FIFlowStaticData.PARTENON_ID_KEYWRD_NAME)).orElse("");
    }

    public String getTradePartenonGdisponibleId(Task task, TaskEnrichmentFieldConfig config) {
        Trade trade = getTradeFromMessageTask(task, config);
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue(FIFlowStaticData.PARTENON_ID_KEYWRD_NAME)).orElse("");
    }

    public String getTradeId(Task task, TaskEnrichmentFieldConfig config) {
        Trade trade = getTrade(task, config);
        return Optional.ofNullable(trade).map(Trade::getLongId).map(l -> l.toString()).orElse("0");
    }

    public String getMessageCustodian(BOMessage boMessage) throws CalypsoServiceException {
        BOTransfer xfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(boMessage.getTransferLongId());
        LEContact contact = null;
        if(xfer!=null) {
            LegalEntity custodian = BOCache.getLegalEntity(DSConnection.getDefault(), xfer.getInternalAgentId());
            contact=BOCache.getContact(DSConnection.getDefault(), xfer.getInternalRole(), custodian, LEContact.SWIFT, xfer.getProductType(), xfer.getProcessingOrg());
        }
        return Optional.ofNullable(contact).map(LEContact::getSwift).orElse("");
    }

    public String getBuySell(Task task, TaskEnrichmentFieldConfig config){
        String res = "";
        Trade trade = getTradeFromMessageTask(task, config);
        int buySell = Optional.ofNullable(trade).map(Trade::getProduct)
                .map(product -> product.getBuySell(trade)).orElse(0);
        if (buySell < 0) {
            res = FIFlowStaticData.FlowDirection.VENTA.toString();
        } else if (buySell > 0) {
            res = FIFlowStaticData.FlowDirection.COMPRA.toString();
        }
        return res;
    }

    public String getTransferEventType(BOMessage boMessage) throws CalypsoServiceException {
        BOTransfer xfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(boMessage.getTransferLongId());
        return Optional.ofNullable(xfer).map(BOTransfer::getEventType).orElse("");
    }

    public String getMessageTradeBook(Task task, TaskEnrichmentFieldConfig config) {
        Trade trade = getTrade(task, config);
        return Optional.ofNullable(trade).map(Trade::getBook).map(Book::getName).orElse("");
    }

    public String getTradeNominal(Task task, TaskEnrichmentFieldConfig config) {
        Trade trade = getTrade(task, config);
        return Optional.ofNullable(trade).map(Trade::computeNominal)
                .map(nominal -> nominal.toString()).orElse("");

    }

    public Trade getTradeFromMessageTask(Task task, TaskEnrichmentFieldConfig config) {
        Trade targetTrade = null;
        try {
            ProcessTaskUtil.ObjectDesc objectDesc = new ProcessTaskUtil.ObjectDesc(task);
            if ("Message".equals(objectDesc.type)) {
                BOMessage message = objectDesc.getMessage();
                if (message == null) {
                    message = this.getMessage(objectDesc.id);
                }
                targetTrade = DSConnection.getDefault().getRemoteTrade().getTrade(message.getTradeLongId());
            }
        } catch (Exception var9) {
            Log.error(this, var9);
        }
        return targetTrade;
    }

    public Trade getTradeFromTransferTask(Task task, TaskEnrichmentFieldConfig config) {
        Trade targetTrade = null;
        try {
            ProcessTaskUtil.ObjectDesc objectDesc = new ProcessTaskUtil.ObjectDesc(task);
            BOTransfer transfer = objectDesc.getTransfer();
            if (transfer == null) {
                transfer = this.getTransfer(objectDesc.id);
            }
            targetTrade = DSConnection.getDefault().getRemoteTrade().getTrade(transfer.getTradeLongId());
        } catch (Exception var9) {
            Log.error(this, var9);
        }
        return targetTrade;
    }

    public Trade getTrade(Task task, TaskEnrichmentFieldConfig config) {
        Trade targetTrade = null;
        try {
            switch (getWorkflowType(task)){
                case "Trade":
                    targetTrade = getTradeFromTask(task,config);
                    break;
                case "Transfer":
                    targetTrade = getTradeFromTransferTask(task,config);
                    break;
                case "Message":
                    targetTrade = getTradeFromMessageTask(task,config);
                    break;
            }
        } catch (Exception var9) {
            Log.error(this, var9);
        }
        return targetTrade;
    }

    public String getWorkflowType(Task task){
        ProcessTaskUtil.ObjectDesc objectDesc = new ProcessTaskUtil.ObjectDesc(task);
        return objectDesc.type;
    }

    public Trade getTradeFromTask(Task task, TaskEnrichmentFieldConfig config) {
        Trade targetTrade = null;
        try {
            ProcessTaskUtil.ObjectDesc objectDesc = new ProcessTaskUtil.ObjectDesc(task);
            targetTrade  = objectDesc.getTrade();
        } catch (Exception var9) {
            Log.error(this, var9);
        }
        return targetTrade;
    }
}
