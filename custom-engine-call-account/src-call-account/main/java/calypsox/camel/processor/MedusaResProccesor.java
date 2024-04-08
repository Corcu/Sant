package calypsox.camel.processor;

import calypsox.engine.medusa.utils.xml.BotransferResult;
import calypsox.engine.medusa.utils.xml.CashManagement;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.TimeZone;
/**
 * @author acd
 */
public class MedusaResProccesor implements Processor {
    private static final String EX_MEDUSA_NACK = "EX_MEDUSA_NACK";

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        try {
            final CashManagement cashManagement = CashManagement
                    .unMarshall(body);
            generateTask(cashManagement.getBotransferResult());
        }catch (CalypsoServiceException s){
            Log.debug(this,"Error saving task: " + s );
        }catch (final Exception e) {
            Log.debug(this, "Error paring message = " + body, e);
        }
        Log.debug("Medusa response message: ", body);
    }


    private void generateTask(BotransferResult botransferResult) throws CalypsoServiceException {
        if(null!=botransferResult){
            if("NACK".equalsIgnoreCase(botransferResult.getGBOStatus())){
                Task task = new Task();
                task.setStatus(Task.NEW);
                task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                task.setEventType(EX_MEDUSA_NACK);
                task.setPriority(Task.PRIORITY_NORMAL);

                final BOTransfer transfer = loadBoTransfer(botransferResult.getTransferId());
                if(null!=transfer){
                    task.setPoId(transfer.getProcessingOrg());
                    task.setBookId(transfer.getBookId());
                    task.setTradeLongId(transfer.getTradeLongId());
                    task.setObjectStatus(transfer.getStatus());
                }
                task.setDatetime( JDate.getNow().getJDatetime(TimeZone.getDefault()));
                task.setComment( botransferResult.getGBOStatusDescription());

                DSConnection.getDefault().getRemoteBackOffice().save(task);
            }
        }

    }

    private BOTransfer loadBoTransfer(long trasnferId){
        try {
            return DSConnection.getDefault().getRemoteBO().getBOTransfer(trasnferId);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading BOTransfer: "  + trasnferId + " " + e );
        }
        return null;
    }
}
