package calypsox.apps.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.PricingEnvUtil;
import com.calypso.tk.util.SwiftParserUtil;
import com.calypso.tk.util.swiftparser.MessageMatcher;

import static calypsox.tk.bo.swift.SantanderSwiftUtil.toExternalMessage;

/**
 * @author aalonsop
 */
public class CustomTaskMenuActions extends com.calypso.apps.reporting.DefaultTaskMenuActions {

    private static final String CSDR_MANUAL_XFER_ID = "CSDRManualXferId";
    private static final String REPROCESS_MENU = "Reprocess Mx Message";
    private static final String REJECT_MENU = "Reject Mx Message";
    private static final String MANUAL_ASSIGN = "CSDR Manual Assign ACOW MT537 ";

    private static final String REPROCESS_ACTION = "REPROCESS";
    private static final String DUP_TASK_EVTYPE = "EX_GATEWAYMSG_ERROR";
    private static final String MAN_ACOW_ASSIGN = "MAN_ACOW_ASSIGN";

    private static final String MATCH_MESSAGE = "Match";

    public List<TaskMenuAction> getActionNames(String taskWorkflowType) {
        TaskMenuAction reprocessAction = new TaskMenuAction(REPROCESS_MENU);
        TaskMenuAction rejectAction = new TaskMenuAction(REJECT_MENU);
        List<TaskMenuAction> actions = new ArrayList<>();
        if (taskWorkflowType.equals("Message")) {
            TaskMenuAction assignAction = new TaskMenuAction(MANUAL_ASSIGN);
            actions.add(assignAction);
            actions.add(new TaskMenuAction(MATCH_MESSAGE));
        }
        actions.add(reprocessAction);
        actions.add(rejectAction);

        return actions;
    }

    @Override
    public void handleAction(String actionName, List<Task> selectedTasks, JComponent component) {
        if (!Util.isEmpty(selectedTasks)) {
            for (Task currentTask : selectedTasks) {

                if (MATCH_MESSAGE.equals(actionName)) {
                    try {
                        handleMatch(currentTask);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(component, e.getMessage(), "Error Matching Message", JOptionPane.ERROR_MESSAGE);
                    }
                    continue;
                }
                if (DUP_TASK_EVTYPE.equals(currentTask.getEventType())) {
                    try {
                        if (REPROCESS_MENU.equals(actionName)) {
                            handleReprocessTrade(currentTask);
                        } else if (REJECT_MENU.equals(actionName)) {
                            handleReject(currentTask);
                        }
                    } catch (CalypsoServiceException e) {
                        Log.error(this, "Could not save message with messageId=" + currentTask.getObjectLongId());
                    }
                } else {

                    if (currentTask.getEventClass().equals(PSEventMessage.class.getSimpleName())
                            && selectedTasks.toString().contains("eventType: PENDING_INC_RECON")) {

                        String userInput = JOptionPane.showInputDialog(null, "Please enter the transfer id:",
                                CSDR_MANUAL_XFER_ID, JOptionPane.PLAIN_MESSAGE);
                        if (userInput != null && !userInput.trim().isEmpty()) {
                            // Logica para procesar

                            Long xferId = Long.valueOf(userInput);

                            try {
                                handleManAssignAcow(currentTask, xferId);
                            } catch (CalypsoServiceException e) {
                                Log.error(this, e);
                            }

                        } else {
                            JOptionPane.showMessageDialog(null, "Nothing was entered, the action is cancelled.",
                                    CSDR_MANUAL_XFER_ID, JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        }
    }

    private void handleMatch(Task messageTask) throws CalypsoServiceException, MessageParseException, CloneNotSupportedException {
        BOMessage msg = DSConnection.getDefault().getRemoteBO().getMessage(messageTask.getObjectLongId());


        MessageMatcher matcher = SwiftParserUtil.getMatcherParserClass(msg.getTemplateName());
        if (matcher == null) {
            if (SwiftParserUtil.isSwiftTrade(DSConnection.getDefault(), msg.getTemplateName())) {
                matcher = SwiftParserUtil.getMatcherParserClass("SwiftTrade");
            } else {
                matcher = SwiftParserUtil.getMatcherParserClass("Swift");
            }
        }
        if (matcher == null) {
            throw new MessageParseException(String.format("Cannot find a valid MessageMatcher Class for MessageType %s, message %s.", msg.getTemplateName(), msg));
        } else {
            ExternalMessage externalMessage = toExternalMessage(msg);
            PricingEnv env = PricingEnv.loadLitePE(DSConnection.getDefault().getDefaultPricingEnv(), new JDatetime());
            Vector<String> errors = new Vector<>();
            Object indexedTo = matcher.index(externalMessage, env, DSConnection.getDefault(), null, errors);
            if (indexedTo != null) {
                if (indexedTo instanceof BOTransfer) {
                    BOTransfer indexedXfer = (BOTransfer) indexedTo;
                    if (indexedXfer.getLongId() != msg.getTransferLongId()) {
                        boolean matched = matcher.match(externalMessage, indexedTo, null, indexedXfer, env, DSConnection.getDefault(), null, errors);
                        BOMessage cloneMsg = (BOMessage) msg.clone();
                        cloneMsg.setTransferLongId(indexedXfer.getLongId());
                        cloneMsg.setXferVersion(indexedXfer.getVersion());
                        cloneMsg.setBookId(indexedXfer.getBookId());
                        cloneMsg.setTradeLongId(indexedXfer.getTradeLongId());
                        if (indexedXfer.getTradeLongId() > 0)
                            cloneMsg.setTradeVersion(DSConnection.getDefault().getRemoteTrade().getTrade(indexedXfer.getTradeLongId()).getVersion());
                        cloneMsg.setProductFamily(indexedXfer.getProductFamily());
                        cloneMsg.setProductType(indexedXfer.getProductType());
                        cloneMsg.setMatchingB(matched);
                        cloneMsg.setAction(Action.UPDATE);
                        DSConnection.getDefault().getRemoteBO().save(cloneMsg, 0L, null);
                    }
                } else if (indexedTo instanceof Trade) {
                    Trade indexedTrade = (Trade) indexedTo;
                    boolean matched = matcher.match(externalMessage, indexedTrade, null, null, env, DSConnection.getDefault(), null, errors);
                    BOMessage cloneMsg = (BOMessage) msg.clone();
                    cloneMsg.setTradeLongId(indexedTrade.getLongId());
                    cloneMsg.setTradeVersion(indexedTrade.getVersion());
                    cloneMsg.setBookId(indexedTrade.getBookId());
                    cloneMsg.setProductFamily(indexedTrade.getProductFamily());
                    cloneMsg.setProductType(indexedTrade.getProductType());
                    cloneMsg.setTransferLongId(0);
                    cloneMsg.setXferVersion(0);
                    cloneMsg.setMatchingB(matched);
                    cloneMsg.setAction(Action.UPDATE);
                    DSConnection.getDefault().getRemoteBO().save(cloneMsg, 0L, null);
                } else if (indexedTo instanceof BOMessage) {
                    BOMessage indexedMessage = (BOMessage) indexedTo;
                    BOTransfer xfer = indexedMessage.getTransferLongId() > 0 ? DSConnection.getDefault().getRemoteBO().getBOTransfer(indexedMessage.getTransferLongId()) : null;
                    boolean matched = matcher.match(externalMessage, indexedMessage, indexedMessage, xfer, env, DSConnection.getDefault(), null, errors);
                    BOMessage cloneMsg = (BOMessage) msg.clone();
                    cloneMsg.setTradeLongId(indexedMessage.getLongId());
                    cloneMsg.setTradeVersion(indexedMessage.getVersion());
                    cloneMsg.setTransferLongId(indexedMessage.getTransferLongId());
                    cloneMsg.setXferVersion(indexedMessage.getXferVersion());
                    cloneMsg.setBookId(indexedMessage.getBookId());
                    cloneMsg.setProductFamily(indexedMessage.getProductFamily());
                    cloneMsg.setProductType(indexedMessage.getProductType());
                    cloneMsg.setMatchingB(matched);
                    cloneMsg.setAction(Action.UPDATE);
                    DSConnection.getDefault().getRemoteBO().save(cloneMsg, 0L, null);
                }
            }
        }
    }

    private void handleReject(Task dupTask) throws CalypsoServiceException {
        handleMessageAction(dupTask, Action.S_CANCEL);
    }

    private void handleReprocessTrade(Task dupTask) throws CalypsoServiceException {
        handleMessageAction(dupTask, REPROCESS_ACTION);
    }

    private void handleManAssignAcow(Task dupTask, Long xferId) throws CalypsoServiceException {
        BOMessage taskMsg = DSConnection.getDefault().getRemoteBO().getMessage(dupTask.getObjectLongId());
        if (taskMsg != null) {
            Action action = Action.valueOf(MAN_ACOW_ASSIGN);
            if (BOMessageWorkflow.isMessageActionApplicable(taskMsg, null, null, action, DSConnection.getDefault(),
                    null)) {

                BOTransfer xfer = DSConnection.getDefault().getRemoteBackOffice().getBOTransfer(Long.valueOf(xferId));

                if ((xfer != null && xfer.getStatus().equals(Status.S_FAILED)) || (xfer != null
                        && xfer.getStatus().equals(Status.S_VERIFIED) && xfer.getValueDate().before(JDate.getNow()))) {

                    taskMsg.setAction(action);

                    taskMsg.setAttribute(CSDR_MANUAL_XFER_ID, String.valueOf(xferId));


                    DSConnection.getDefault().getRemoteBO().save(taskMsg, 0, null);

                } else {
                    if (xfer == null) {
                        JOptionPane.showMessageDialog(null,
                                "The transfer id entered is not associated with any existing transfer.",
                                CSDR_MANUAL_XFER_ID, JOptionPane.INFORMATION_MESSAGE);
                    } else if (!xfer.getStatus().equals(Status.S_FAILED)
                            && !xfer.getStatus().equals(Status.S_VERIFIED)) {
                        JOptionPane.showMessageDialog(null, "Not found any failed transfer with this id.",
                                CSDR_MANUAL_XFER_ID, JOptionPane.INFORMATION_MESSAGE);
                    } else if (xfer.getStatus().equals(Status.S_VERIFIED)
                            && !xfer.getValueDate().before(JDate.getNow())) {
                        JOptionPane.showMessageDialog(null, "Not found any failed transfer with this id.",
                                CSDR_MANUAL_XFER_ID, JOptionPane.INFORMATION_MESSAGE);
                    }
                }

            } else {
                Log.error(this, "Could not apply action " + action + " to message with id: " + taskMsg.getLongId());
            }
        }
    }

    private void handleMessageAction(Task dupTask, String actionName) throws CalypsoServiceException {
        BOMessage taskMsg = DSConnection.getDefault().getRemoteBO().getMessage(dupTask.getObjectLongId());
        if (taskMsg != null) {
            Action action = Action.valueOf(actionName);
            if (BOMessageWorkflow.isMessageActionApplicable(taskMsg, null, null, action, DSConnection.getDefault(),
                    null)) {
                taskMsg.setAction(action);
                DSConnection.getDefault().getRemoteBO().save(taskMsg, 0, null);
            } else {
                Log.error(this, "Could not apply action " + action + " to message with id: " + taskMsg.getLongId());
            }
        }
    }
}
