package calypsox.tk.bo.workflow.rule;

import calypsox.tk.util.swiftparser.MT537Matcher;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.MessageParseException;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class ReIndexMT537MessageRule implements WfMessageRule {
	
	private static final String CSDR_MANUAL_XFER_ID = "CSDRManualXferId";

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
    	
    	return true;
    }

    @Override
    public String getDescription() {
        return "Used to reprocess CSDR's pending MT537s";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        try {
            MT537Matcher matcher = new MT537Matcher();
            AdviceDocument ad = DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(message.getLongId(), new JDatetime());
            SwiftMessage sw = new SwiftMessage(message, message.getTemplateName());
            sw.parse(ad.getTextDocument().toString(), "SWIFT");
            addCSDRManualXferId(sw, message);
            Object indexedObjects = matcher.index(sw, PricingEnv.loadPE("OFFICIAL", new JDatetime()), DSConnection.getDefault(), null, new Vector());
            Object indexedObj = getIndexedObject(indexedObjects);
            updateMsg(indexedObj, message, DSConnection.getDefault());
        } catch (MessageParseException | CalypsoServiceException exc) {
            Log.error("this", exc.getCause());
        }
        return true;
    }

    private Object getIndexedObject(Object indexedObject) {
        Object obj = null;
        if (indexedObject instanceof Vector) {
            Vector idx = (Vector) indexedObject;
            Vector swifts = ((Vector<?>) idx.get(0));
            Vector objects = ((Vector<?>) idx.get(1));
            for (int i = 0; i < swifts.size(); i++) {
                if (objects.get(i) != null) {
                    obj = objects.get(i);
                    break;
                }
            }
        }
        return obj;
    }

    private void updateMsg(Object indexed, BOMessage message, DSConnection ds) throws CalypsoServiceException {
        Trade trade = getTradeFromIndexed(indexed, ds);
        cleanUpReferences(message);
        if (indexed instanceof Trade) {
            trade = (Trade) indexed;
            message.setProductFamily(trade.getProductFamily());
            message.setProductType(trade.getProductType());
            message.setMatchingB(true);
        } else if (indexed instanceof BOTransfer) {
            BOTransfer xfer = (BOTransfer) indexed;
            message.setTransferLongId(getXferLongId(indexed));
            message.setBookId(xfer.getBookId());
            message.setProductFamily(xfer.getProductFamily());
            message.setProductType(xfer.getProductType());
            message.setMatchingB(true);
        } else if (indexed instanceof BOMessage) {
            BOMessage msg = (BOMessage) indexed;
            message.setTransferLongId(getXferLongId(indexed));
            message.setLinkedLongId(msg.getLongId());
            message.setBookId(msg.getBookId());
            message.setProductFamily(msg.getProductFamily());
            message.setProductType(msg.getProductType());
            message.setMatchingB(true);
        }
        if (trade != null) {
            message.setTradeLongId(trade.getLongId());
            message.setTradeVersion(trade.getVersion());
            message.setTradeUpdateDatetime(trade.getUpdatedTime());
        }
    }

    private void cleanUpReferences(BOMessage message){
        message.setTradeLongId(0L);
        message.setTransferLongId(0L);
        message.setLinkedLongId(0L);
        message.setTradeVersion(0);
        message.setMatchingB(false);
    }

    private Trade getTradeFromIndexed(Object obj, DSConnection ds) throws CalypsoServiceException {
        long tradeId = 0L;
        Trade trade = null;
        boolean isTrade = obj instanceof Trade;
        if (!isTrade) {
            if (obj instanceof BOMessage) {
                tradeId = ((BOMessage) obj).getTradeLongId();
            } else if (obj instanceof BOTransfer) {
                tradeId = ((BOTransfer) obj).getTradeLongId();
            }
            trade = ds.getRemoteTrade().getTrade(tradeId);
        }
        return trade;
    }

    private long getXferLongId(Object obj) {
        long xferId = 0L;
        if (obj instanceof BOMessage) {
            xferId = ((BOMessage) obj).getTransferLongId();
        } else if (obj instanceof BOTransfer) {
            xferId = ((BOTransfer) obj).getLongId();
        }
        return xferId;
    }
    
    /**
     * Needed because the CSDRManualXferId is lost in MT537Matcher index() method
     * @param sw SwiftMessage ibject
     * @param msg BOMessage object
     */
    private void addCSDRManualXferId(SwiftMessage sw, BOMessage msg) {
    	
    	if(msg.getAttribute(CSDR_MANUAL_XFER_ID) != null) {
    		sw.addProperty(CSDR_MANUAL_XFER_ID, msg.getAttribute(CSDR_MANUAL_XFER_ID));
    	}
    }


}
