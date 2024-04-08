package calypsox.tk.bo.mapping.triparty;

import calypsox.tk.product.secfinance.triparty.sql.SantTripartyAllocationRecordSQL;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.mapping.triparty.TripartyAllocationMultiObjectSaver;
import com.calypso.tk.bo.sql.AdviceDocumentSQL;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventAdviceDocument;
import com.calypso.tk.event.sql.PSEventSQL;
import com.calypso.tk.product.secfinance.triparty.TripartyAllocationData;
import com.calypso.tk.product.secfinance.triparty.TripartyPersistenceReport;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TaskArray;

import java.sql.Connection;
import java.util.*;

public class SantTripartyAllocationMultiObjectSaver extends TripartyAllocationMultiObjectSaver {

    @Override
    public Map<Class<?>, List<Long>> saveInDataServer(long eventId, String engineName, Connection con, Vector<PSEvent> evts) throws Exception {
        HashMap<Class<?>, List<Long>> ids = new HashMap();
        ids.put(BOMessage.class, new ArrayList());
        ids.put(AdviceDocument.class, new ArrayList());
        ids.put(Trade.class, new ArrayList());
        ids.put(Task.class, new ArrayList());
        List<BOMessage> messageList = this.get(BOMessage.class);
        MessageArray messages = new MessageArray(new Vector(messageList));
        List<Task> tasks = this.get(Task.class);
        TaskArray errors = new TaskArray(tasks);
        List<AdviceDocument> documents = this.get(AdviceDocument.class);
        this.allocateMessageId(messages);

        for(int i = 0; i < messages.size(); ++i) {
            BOMessage message = (BOMessage)messages.get(i);
            AdviceDocument document = (AdviceDocument)documents.get(i);
            long id = message.getAllocatedLongSeed();
            if (id <= 0L) {
                id = message.getLongId();
            }

            ((List)ids.get(BOMessage.class)).add(id);
            document.setAdviceLongId(id);
            AdviceDocumentSQL.save(document, con);
            ((List)ids.get(AdviceDocument.class)).add(document.getId());
            MessageArray messArray = new MessageArray();
            boolean isNew = document.getId() == 0L;
            messArray.add(message);
            Vector<PSEvent> msgEvts = BackOfficeServerImpl.saveMessages(i == 0 ? eventId : 0L, engineName, messArray, errors, (String)null, con);
            errors.clear();
            evts.addAll(msgEvts);
            PSEventAdviceDocument event = new PSEventAdviceDocument(document);
            this.updateEventType(isNew, event);
            if (BackOfficeServerImpl.isEventRequired(event)) {
                PSEventSQL.save(event, con);
            }

            evts.add(event);
        }

        List<TripartyAllocationData> newAllocationRecords = this.get(TripartyAllocationData.class);
        List<Trade> existingAllocationTradesToTerminate = this.get(Trade.class);
        if (newAllocationRecords != null) {
            TripartyPersistenceReport report = SantTripartyAllocationRecordSQL.santSaveNewAllocationRecords(newAllocationRecords, existingAllocationTradesToTerminate, (String)null);
            Set<Long> intIdsSet = report.getNewTradeIds();
            Set<Long> longIdsSet = new HashSet();
            if (!Util.isEmpty(intIdsSet)) {
                Iterator var25 = intIdsSet.iterator();

                while(var25.hasNext()) {
                    Long intId = (Long)var25.next();
                    longIdsSet.add(intId);
                }
            }

            ((List)ids.get(Trade.class)).addAll(longIdsSet);
        }

        return ids;
    }

    private void updateEventType(boolean isNew, PSEventAdviceDocument event) {
        if (isNew) {
            event.setType(1);
        } else {
            event.setType(3);
        }
    }

    private void allocateMessageId(MessageArray messages) throws Exception {
        int msgWithoutId = 0;

        for(int i = 0; i < messages.size(); ++i) {
            BOMessage message = (BOMessage)messages.get(i);
            if (message.getAllocatedLongSeed() <= 0L && message.getLongId() <= 0L) {
                ++msgWithoutId;
            }
        }

        if (msgWithoutId != 0) {
            long preAllocateMessageId = this.preAllocateMessageId(msgWithoutId);

            for(int i = 0; i < messages.size(); ++i) {
                BOMessage message = (BOMessage)messages.get(i);
                if (message.getAllocatedLongSeed() <= 0L && message.getLongId() <= 0L) {
                    message.setAllocatedLongSeed(preAllocateMessageId--);
                }
            }
        }
    }
}
