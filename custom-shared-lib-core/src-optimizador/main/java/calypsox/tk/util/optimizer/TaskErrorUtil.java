package calypsox.tk.util.optimizer;

import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.interfaces.optimizer.importstatus.Error;
import calypsox.tk.interfaces.optimizer.importstatus.ImportStatusList;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskErrorUtil {

    private static final String ACK_OK = "ACK_OK";
    private static final String ISIN = "ISIN";
    private static String PATTERN_POSITION = "[\\.[^|]]*|$";

    public enum EnumOptimProcessType {
        OPTIMIZER_MARGIN_CALL, OPTIMIZER_ALLOCATION, OPTIMIZER_POSITION;

        public String toString(EnumOptimProcessType elem) {
            switch (elem) {
                case OPTIMIZER_MARGIN_CALL:
                    return "OPTIM_MARGIN_CALL";
                case OPTIMIZER_ALLOCATION:
                    return "OPTIM_ALLOCS_IMPORT";
                case OPTIMIZER_POSITION:
                    return "OPTIM_POSITION";
                default:
                    break;
            }
            return "";
        }
    }

    public static List<Task> getTaskErrors(EnumOptimProcessType processType,
                                           ExternalMessage message) {
        List<Task> listErrorTasks = new ArrayList<Task>();
        JDatetime currentDatetime = new JDatetime();
        if (message != null && !Util.isEmpty(message.getText())) {
            StreamSource ss = new StreamSource(new StringReader(
                    message.getText()));
            ImportStatusList importStatusList = null;
            try {
                importStatusList = ImportStatusUtil.parseStream(ss);
            } catch (Exception e) {
                Log.error(TaskErrorUtil.class.getName(), e);
                return listErrorTasks;
            }
            if (importStatusList != null) {
                for (ImportStatusList.ImportStatus importStatusElt : importStatusList
                        .getImportStatuses()) {
                    if (importStatusElt != null
                            && importStatusElt.getErrors() != null) {
                        for (Error errorElt : importStatusElt.getErrors()
                                .getErrors()) {
                            if (ACK_OK.equals(errorElt.getCode())) {
                                Log.warn(TaskErrorUtil.class.getName(), "Skipping ACK_OK message: " + importStatusElt.getImportKey());
                                continue;
                            }
                            Task task = buildTask(processType, currentDatetime);
                            String correlationId = getCorrelationId(message);
                            task.setComment(correlationId + errorElt.getCode() + ": "
                                    + errorElt.getDescription());
                            switch (processType) {
                                case OPTIMIZER_MARGIN_CALL:
                                    task.setObjectLongId(getObjectIdFromCorrelationId(correlationId));
                                    // new correlationId: correlationId + getImportKey
                                    correlationId = getCorrelationId(message, importStatusElt.getImportKey());
                                    task.setComment(correlationId + errorElt.getCode() + ": "
                                            + errorElt.getDescription());
                                    break;
                                case OPTIMIZER_ALLOCATION:
                                    task.setObjectLongId(0);
                                    break;
                                case OPTIMIZER_POSITION:
                                    // security Id
                                    int productId = getProductId(message);
                                    task.setObjectLongId(productId);
                                    task.setProductId(productId);
                                    task.setBookId(getBookId(message));
                                default:
                                    break;
                            }
                            listErrorTasks.add(task);
                        }
                    }
                }
            }
        }
        return listErrorTasks;
    }

    private static String getCorrelationId(ExternalMessage message,
                                           String importKey) {
        if (message == null || !(message instanceof JMSQueueMessage)) {
            return Util.isEmpty(importKey) ? "" : importKey;
        }
        return Util.isEmpty(((JMSQueueMessage) message).getCorrelationId()) ? "" : "[" + ((JMSQueueMessage) message).getCorrelationId() + " : " + importKey + "] ";
    }

    private static long getObjectIdFromCorrelationId(String correlationId) {
        if (Util.isEmpty(correlationId)) {
            return 0;
        }
        String regex = "(\\d+)";
        String num = "";
        Matcher matcher = Pattern.compile(regex).matcher(correlationId);
        while (matcher.find()) {
            num += matcher.group();
        }
        if (!Util.isEmpty(num)) {
            try {
                return Long.valueOf(num);
            } catch (NumberFormatException e) {
                Log.error(TaskErrorUtil.class.getName(), e);
                return 0;
            }
        }
        return 0;
    }

    private static String getCorrelationId(ExternalMessage message) {
        if (message == null || !(message instanceof JMSQueueMessage)) {
            return "";
        }
        return Util.isEmpty(((JMSQueueMessage) message).getCorrelationId()) ? "" : "[" + ((JMSQueueMessage) message).getCorrelationId() + "] ";
    }

    private static int getBookId(ExternalMessage message) {
        if (!(message instanceof JMSQueueMessage)) {
            return 0;
        }
        String bookValue = getFieldFromMessage((JMSQueueMessage) message, 2);
        if (!Util.isEmpty(bookValue)) {
            return LocalCache.getBookId(DSConnection.getDefault(), bookValue);
        }
        return 0;
    }

    private static String getMessageKey(ExternalMessage message) {
        if (message instanceof JMSQueueMessage) {
            return ((JMSQueueMessage) message).getCorrelationId();
        } else {
            return "";
        }
    }

    private static Task buildTask(EnumOptimProcessType processType,
                                  JDatetime currentDatetime) {
        Task task = new Task();
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setNewDatetime(currentDatetime);
        task.setUnderProcessingDatetime(currentDatetime);
        task.setUndoTradeDatetime(currentDatetime);
        task.setDatetime(currentDatetime);
        task.setPriority(Task.PRIORITY_HIGH);
        task.setId(0);
        task.setSource(processType.toString(processType));
        task.setOwner(processType.toString(processType));
        task.setEventType("EX_" + processType.toString(processType));
        return task;
    }

    @SuppressWarnings("unchecked")
    public static int getProductId(ExternalMessage message) {
        if (!(message instanceof JMSQueueMessage)) {
            return 0;
        }
        int productId = 0;
        String isinValue = getFieldFromMessage((JMSQueueMessage) message, 3);
        String ccyValue = getFieldFromMessage((JMSQueueMessage) message, 4);

        try {
            Vector<Product> products = DSConnection.getDefault()
                    .getRemoteProduct().getProductsByCode(ISIN, isinValue);

            if (!Util.isEmpty(products)) {
                if (products.size() == 1) {
                    productId = (products.get(0) != null) ? products.get(0)
                            .getId() : 0;
                } else if (products.size() > 1) {
                    Log.warn(TaskErrorUtil.class.getName(),
                            "ISIN used for more than one product: " + isinValue);
                    for (Product product : products) {
                        if (product != null
                                && ccyValue.equals(product.getCurrency())) {
                            productId = product.getId();
                        }
                    }
                }
            }
            return productId;
        } catch (RemoteException e) {
            Log.error(TaskErrorUtil.class.getName(), e);
            return 0;
        }
    }

    private static String getFieldFromMessage(JMSQueueMessage message,
                                              int idxPos) {
        String fieldValue = "";
        Pattern r = Pattern.compile(PATTERN_POSITION);
        Matcher m = r.matcher(getMessageKey(message));
        int fieldIdx = 0;
        while (m.find() && fieldIdx < idxPos) {
            if (!Util.isEmpty(m.group())) {
                fieldValue = m.group();
                fieldIdx++;
            }
        }
        return fieldValue;
    }
}
