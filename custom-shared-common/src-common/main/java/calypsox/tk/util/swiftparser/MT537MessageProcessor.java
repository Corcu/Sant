package calypsox.tk.util.swiftparser;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TaskArray;

import java.util.*;


/**
 * @author aalonsop
 */
public class MT537MessageProcessor extends com.calypso.tk.util.swiftparser.MT537MessageProcessor {

    /**
     * There's a bug withing Calypso's v16 MR85, where the objectSaver class need to be cleared before every
     * save attempt. Otherwise, it will crash throwing an ArrayOutOfBoundsException.
     * @param message
     * @param indexedObject
     * @param indexedTrade
     * @param indexedMessage
     * @param indexedTransfer
     * @param swiftMessage
     * @param indexed
     * @param matched
     * @param errors
     * @param ds
     * @param dbCon
     */
    @Override
    protected void beforeSave(BOMessage message, Object indexedObject, Trade indexedTrade, BOMessage indexedMessage, BOTransfer indexedTransfer, SwiftMessage swiftMessage, boolean indexed, boolean matched, Vector errors, DSConnection ds, Object dbCon) {
        if (isNotEmptyMessageVector(indexedObject)) {
            this.getObjectSaver().clear();
        }
        super.beforeSave(message, indexedObject, indexedTrade, indexedMessage, indexedTransfer, swiftMessage, indexed, matched, errors, ds, dbCon);

        if (!isPENA(swiftMessage, errors)) {
            Vector idx = (Vector) indexedObject;
            Vector swifts = ((Vector<?>) idx.get(0));
            Vector indexedObjects = ((Vector<?>) idx.get(1));
            for (int i = 0; i < swifts.size(); i++) {
                Object index = indexedObjects.get(i);
                if (index instanceof BOMessage) {
                    BOMessage boMess = (BOMessage) index;
                    Long xferId = boMess.getTransferLongId();
                    if (xferId != 0) {
                        BOTransfer boTransfer = null;
                        try {
                            boTransfer = ds.getRemoteBackOffice().getBOTransfer(xferId);
                        } catch (CalypsoServiceException e) {
                            Log.error(this.getClass().getSimpleName(), "Cannot retrieve boTransfer with id: " + xferId, e);
                        }
                        if (boTransfer != null) {
                            String xferStatus = boTransfer.getStatus().getStatus();
                            String xferProductType = boTransfer.getProductType();
                            if ((!Util.isEmpty(xferProductType)) && "SETTLED".equalsIgnoreCase(xferStatus)) {
                                createCustomTask(boMess, boTransfer, ds);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void setMessageAttributes(Trade trade, BOMessage message, SwiftMessage swiftMessage, BOMessage indexedMessage, BOTransfer indexedTransfer, Vector errors, DSConnection ds) throws MessageParseException {
        SwiftFieldMessage field = SwiftFieldMessage.findSwiftField(swiftMessage.getFields(), ":22F:", ":SFRE/", (String) null);

        if (field != null) {
            message.setAttribute("Frequency_Indicator", field.getValue().substring(field.getValue().lastIndexOf(47) + 1));
        }
        String delType = swiftMessage.getFieldByType("Del Type");
        message.setAttribute("Del_Type", Util.isEmpty(delType) ? null : delType);

        super.setMessageAttributes(trade, message, swiftMessage, indexedMessage, indexedTransfer, errors, ds);
    }

    private boolean isNotEmptyMessageVector(Object indexedObject) {
        return Optional.ofNullable(indexedObject)
                .map(idx -> ((Vector) idx).get(0))
                .map(swifts -> ((Vector) swifts).size() > 0)
                .orElse(false);
    }

    private boolean isPENA(ExternalMessage swiftMess, Vector errors) {
        SwiftMessage mess = (SwiftMessage) swiftMess;
        SwiftFieldMessage swiftField = mess.getSwiftField(mess.getFields(), ":22H:", ":STST//", (String) null);
        if (swiftField == null) {
            swiftField = mess.getSwiftField(mess.getFields(), ":22F:", ":STST//", (String) null);
        }
        if (swiftField == null) {
            errors.add("Tag 22F:STST not found");
            return false;
        }
        String type = swiftField.getValue().substring(swiftField.getValue().lastIndexOf(47) + 1);
        return "PENA".equalsIgnoreCase(type) ? true : false;
    }

    private void createCustomTask(BOMessage boMess, BOTransfer boTransfer, DSConnection ds) {
        if (Util.isTrue(LocalCache.getDomainValueComment(ds, "matcherCreateCustomException", "MT537"), true)) {
            String taskComment = "According to the agent '" + boMess.getReceiverAddressCode() + "', the " + boTransfer.getProductType() + " transfer should not be settled. Transfer id: "
                    + boTransfer.getLongId();

            Task taskException = new Task();
            taskException.setEventType("EX_RECON_MT537_MATCHED");
            taskException.setEventClass(Task.EXCEPTION_EVENT_CLASS);
            taskException.setStatus(Task.NEW);
            taskException.setComment(taskComment);
            taskException.setObjectLongId(boTransfer.getLongId());
            taskException.setObjectStatus(boTransfer.getStatus());
            taskException.setLinkId(boMess.getLongId());
            taskException.setTradeLongId(boMess.getTradeLongId());
            taskException.setDatetime(JDate.getNow().getJDatetime());
            taskException.setNewDatetime(new JDatetime());
            taskException.setPriority(2);
            taskException.setSource("MT537 matching");

            TaskArray task = new TaskArray();
            task.add(taskException);

            try {
                ds.getRemoteBackOffice().saveAndPublishTasks(task, 0L, null);
            } catch (CalypsoServiceException e) {
                Log.error(this, "Could not save the exception task.");
            }
        }
    }
    @Override
    protected void setPenaltyAttributes(BOMessage message, SwiftMessage swift) throws MessageParseException {
        if (swift != null) {
            //Get the PENA sequece
            List<SwiftFieldMessage> fields = SwiftFieldMessage.getSwiftSequence(swift.getFields(), ":16R:", ":16S:", "PENA", 1);
            SwiftFieldMessage field = SwiftFieldMessage.findSwiftField(fields, ":95P:", ":CASD/", (String) null);
            if (field != null) {
                message.setAttribute("CSDRPenaltyPSET", field.getValue().substring(field.getValue().lastIndexOf(47) + 1));
            }
            //Get the PENACOUNT subsequence
            List<List<SwiftFieldMessage>> list = SwiftFieldMessage.splitBySwiftSequence(fields, "PENACOUNT");
            if (list != null && !list.isEmpty()) {
                field = SwiftFieldMessage.findSwiftField(list.get(0), ":22F:", ":TRCA/", (String) null);
            }
            if (field != null) {
                message.setAttribute("CSDRPartyCapacityIndicator", field.getValue().substring(field.getValue().lastIndexOf(47) + 1));
            }

            super.setPenaltyAttributes(message, swift);
        }
    }
}