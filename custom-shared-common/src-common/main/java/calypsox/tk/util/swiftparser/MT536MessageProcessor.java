package calypsox.tk.util.swiftparser;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.swiftparser.SecurityMatcher;

import java.util.Vector;

import static com.calypso.tk.util.swiftparser.SecurityMatcher.getTradeType;

public class MT536MessageProcessor extends com.calypso.tk.util.swiftparser.MT536MessageProcessor {

    @Override
    protected void beforeSave(BOMessage message, Object indexedObject, Trade indexedTrade, BOMessage indexedMessage, BOTransfer indexedTransfer, SwiftMessage swiftMessage, boolean indexed, boolean matched, Vector errors, DSConnection ds, Object dbCon) {
        if (indexedObject != null) {
            Vector v = (Vector) indexedObject;

            if (!Util.isEmpty(v)) {
                this.getObjectSaver().clear();
                Vector swifts = (Vector) v.get(0);
                Vector indexedObjects = (Vector) v.get(1);
                Vector indexedErrors = (Vector) v.get(2);

                for (int i = 0; i < swifts.size(); ++i) {
                    SwiftMessage swift = (SwiftMessage) swifts.get(i);
                    indexedTransfer = null;
                    indexedMessage = null;
                    indexedTrade = null;
                    Object indexObj = indexedObjects.get(i);
                    Vector localErrors = (Vector) indexedErrors.get(i);
                    if (indexObj instanceof BOMessage) {
                        indexedMessage = (BOMessage) indexObj;
                        if (indexedMessage.getTradeLongId() != 0L) {
                            try {
                                if (dbCon == null) {
                                    indexedTrade = ds.getRemoteTrade().getTrade(indexedMessage.getTradeLongId());
                                } else {
                                    indexedTrade = TradeSQL.getTrade(indexedMessage.getTradeLongId());
                                }
                            } catch (Exception var27) {
                                Log.error("SwiftParserUtil", var27);
                                errors.add(var27.getMessage());
                            }
                        }

                        if (indexedMessage.getTransferLongId() != 0L) {
                            try {
                                if (dbCon == null) {
                                    indexedTransfer = ds.getRemoteBO().getBOTransfer(indexedMessage.getTransferLongId());
                                } else {
                                    indexedTransfer = BOTransferSQL.getTransfer(indexedMessage.getTransferLongId());
                                }
                            } catch (Exception var26) {
                                Log.error("SwiftParserUtil", var26);
                                errors.add(var26.getMessage());
                            }
                        }
                    }
                    try {
                        message = this.buildBOMessage(indexedTrade, swift, indexedTransfer, indexedMessage, localErrors, ds, dbCon);

                        setAttributes(indexedTrade, message, swift, indexedMessage, indexedTransfer, localErrors, ds);

                        if (indexedTransfer != null) {
                            String xferStatus = indexedTransfer.getStatus().getStatus();
                            if (!"SETTLED".equalsIgnoreCase(xferStatus) && !"SPLIT".equalsIgnoreCase(xferStatus)) {
                                createCustomTask(indexedMessage, indexedTransfer, ds);
                            }
                        }

                        message.setMatchingB(checkPrepDate(swift, indexedTransfer, localErrors));
                        message.setMessageClass(1);
                        AdviceDocument doc = SwiftParserUtil.buildAdviceDocument(message, swift, ds, dbCon);
                        this.getObjectSaver().add(BOMessage.class, message);
                        this.getObjectSaver().add(AdviceDocument.class, doc);
                        TaskArray array = this.createTasks(message, localErrors);
                        this.getObjectSaver().add(TaskArray.class, array);
                    } catch (Exception e) {
                        Log.error("MT536MessageProcessor", e);
                        errors.add(e.getMessage());
                    }
                }
            }
        } else {
            try {
                setAttributes(null, message, swiftMessage,null, null,errors, ds);
            } catch (MessageParseException e) {
                Log.error(this, e);
                errors.add(String.format("%s:%s.",e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }


    private void setAttributes(Trade trade, BOMessage boMessage, SwiftMessage swiftMessage, BOMessage indexedMessage, BOTransfer indexedTransfer, Vector errors, DSConnection ds) throws MessageParseException {
       // super.setInternalAttributes(trade, boMessage, swiftMessage, indexedMessage, indexedTransfer, errors, ds);
        SwiftFieldMessage isActive = swiftMessage.getSwiftField(":17B:", "ACTI", null);
        if (isActive != null) {
            String[] parts = isActive.getValue().split("//");
            if (parts.length >= 1)
                boMessage.setAttribute("IsActive", parts[1]);
        }
        boMessage.setAttribute("PORef", swiftMessage.getReferenceByName("PORef"));

        String tradeType = getTradeType(swiftMessage);
        boMessage.setAttribute("Trade_Type", Util.isEmpty(tradeType) ? null : tradeType);

        SwiftFieldMessage field = SwiftFieldMessage.findSwiftField(swiftMessage.getFields(), ":22F:", ":SFRE/", (String) null);

        if (field != null) {
            boMessage.setAttribute("Frequency_Indicator", field.getValue().substring(field.getValue().lastIndexOf('/') + 1));
        }

        //direction

        String delType = swiftMessage.getFieldByType("Del Type");
        boMessage.setAttribute("Del_Type", Util.isEmpty(delType) ? null : delType);
        Object date = swiftMessage.getDate("Settle Date");
        if (date instanceof JDate) {
            boMessage.setSettleDate((JDate) date);
            boMessage.setAttribute("Settle Date", Util.idateToString((JDate) swiftMessage.getDate("Settle Date")));
        }

        boMessage.setAttribute("Message_Function",  SecurityMatcher.getMessageFunction(swiftMessage));
    }

    private void createCustomTask(BOMessage boMess, BOTransfer boTransfer, DSConnection ds) {

      if (Util.isTrue(LocalCache.getDomainValueComment(ds, "matcherCreateCustomException", "MT536"), true)) {

          String taskComment = "According to the agent '" + boMess.getReceiverAddressCode() + "', the " + boTransfer.getProductType() + " transfer should be settled. Transfer "
                  + boTransfer.getLongId() + " is currently on status '" + boTransfer.getStatus().getStatus() + "'";

          Task taskException = new Task();
          taskException.setEventType("EX_RECON_MT536_MATCHED");
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
          taskException.setSource("MT536 matching");

          TaskArray task = new TaskArray();
          task.add(taskException);

          try {
              ds.getRemoteBackOffice().saveAndPublishTasks(task, 0L, null);
          } catch (CalypsoServiceException e) {
              Log.error(this, "Could not save the exception task.");
          }
      }
    }

    private boolean checkPrepDate(SwiftMessage swift, BOTransfer indexedTransfer, Vector errors) {
        if (indexedTransfer == null) {
            return false;
        } else {
            JDatetime extPrepDate = SecurityMatcher.getPreparationDate(swift);
            if (extPrepDate != null) {
                String oriDateStr = indexedTransfer.getAttribute("PREP_Date");
                if (oriDateStr != null) {
                    JDatetime oriPrepDate = new JDatetime(Util.istringToMTimestamp(oriDateStr));
                    if (extPrepDate.before(oriPrepDate)) {
                        Log.debug(this, "Message out of sequence " + extPrepDate + " / " + oriPrepDate);
                        errors.add("Message out of sequence " + extPrepDate + " / " + oriPrepDate);
                        return false;
                    }
                }
            }

            return true;
        }
    }
}
