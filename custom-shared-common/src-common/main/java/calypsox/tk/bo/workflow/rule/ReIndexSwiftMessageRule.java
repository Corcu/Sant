package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;
import com.calypso.tk.util.swiftparser.MessageMatcher;


import java.sql.Connection;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.IntStream;

public class ReIndexSwiftMessageRule implements WfMessageRule {

    private static final String CSDR_MANUAL_XFER_ID = "CSDRManualXferId";
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (!"SWIFT".equals(message.getFormatType())) {
            messages.add(String.format("ReIndexing is not supported for %s %s messages.", message.getFormatType(), message.getTemplateName()));
            return false;
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "Calls Message Matcher to re-index message.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        try {

            AdviceDocument ad = DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(message.getLongId(), new JDatetime());
            if (ad == null) {
                messages.add("Advice document not found, cannot re-index.");
                return false;
            }

            SwiftMessage sw = new SwiftMessage(message, message.getTemplateName());
            sw.parse(ad.getTextDocument().toString(), "SWIFT");

            MessageMatcher matcher = SwiftParserUtil.getMatcherParserClass(sw.getType());
            if (matcher == null) {
                messages.add( String.format("Message Matcher not found for message type %s.",sw.getType()));
                return false;
            }

            Vector<String> errors = new Vector<>();
            PricingEnv env = PricingEnv.loadPE(dsCon.getDefaultPricingEnv(), new JDatetime());
            if(message.getAttribute(CSDR_MANUAL_XFER_ID) != null) {
                sw.addProperty(CSDR_MANUAL_XFER_ID, message.getAttribute(CSDR_MANUAL_XFER_ID));
            }
            Object indexedObjects = matcher.index(sw, env, DSConnection.getDefault(), null, new Vector());
            if (!Util.isEmpty(errors)) {
                messages.addAll(errors);
                return false;
            }

            message.setTradeLongId(0L);
            message.setTradeVersion(0);
            message.setProductFamily(null);
            message.setProductType(null);
            message.setTransferLongId(0L);
            message.setXferVersion(0);
            message.setLinkedLongId(0L);
            message.setMatchingB(false);

            Optional<Object> obj = getIndexedObject(indexedObjects);
            if (obj.isPresent()) {
                updateMessage(matcher, sw, message, obj.get(), env, dsCon, (Connection) dbCon, errors);
            }
            return true;

        } catch (Exception e) {
            messages.add(String.format("Error re-indexing message %s. %s: %s", message, e.getClass().getSimpleName(), e.getMessage()));
            Log.error(this, String.format("Error re-indexing message %s.", message), e);
            return false;
        }
    }

    private void updateMessage(MessageMatcher matcher, SwiftMessage sw, BOMessage message, Object indexedObj, PricingEnv env, DSConnection dsCon, Connection dbCon, Vector<String> errors) throws CalypsoServiceException, PersistenceException, MessageParseException {
        Trade indexedTrade = null;
        BOTransfer indexedTransfer = null;
        BOMessage indexedMessage = null;

        if (indexedObj instanceof Trade) {
            indexedTrade = (Trade) indexedObj;
        } else if (indexedObj instanceof BOTransfer) {
            indexedTransfer = (BOTransfer) indexedObj;
            if (indexedTransfer.getTradeLongId() > 0) {
                indexedTrade = dbCon == null ? dsCon.getRemoteTrade().getTrade(indexedTransfer.getTradeLongId()) : TradeSQL.getTrade(indexedTransfer.getTradeLongId(), dbCon);
            }


        } else if (indexedObj instanceof BOMessage) {
            indexedMessage = (BOMessage) indexedObj;
            if (indexedMessage.getTransferLongId() > 0) {
                indexedTransfer = dbCon == null ? dsCon.getRemoteBO().getBOTransfer(indexedMessage.getTransferLongId()) : BOTransferSQL.getTransfer(indexedMessage.getTransferLongId(), dbCon);
            }

            long tradeId = indexedMessage.getTradeLongId() > 0 ? indexedMessage.getTradeLongId() : indexedTransfer != null ? indexedTransfer.getTradeLongId() : 0L;
            if (tradeId > 0) {
                indexedTrade = dbCon == null ? dsCon.getRemoteTrade().getTrade(tradeId) : TradeSQL.getTrade(tradeId, dbCon);
            }
        }

        if (indexedTrade != null) {
            message.setTradeLongId(indexedTrade.getLongId());
            message.setTradeVersion(indexedTrade.getVersion());
            message.setProductFamily(indexedTrade.getProductFamily());
            message.setProductType(indexedTrade.getProductType());
            message.setBookId(indexedTrade.getBookId());
            message.setMatchingB(true);
        }

        if (indexedTransfer != null) {

            message.setTransferLongId(indexedTransfer.getLongId());
            message.setXferVersion(indexedTransfer.getVersion());
            message.setBookId(indexedTransfer.getBookId());
            message.setProductFamily(indexedTransfer.getProductFamily());
            message.setProductType(indexedTransfer.getProductType());
            message.setMatchingB(true);
        }

        if (indexedMessage != null) {
            message.setLinkedLongId(indexedMessage.getLongId());
        }
        message.setMatchingB(matcher.match(sw, indexedObj, message, indexedTransfer, env, dsCon, dbCon, errors));
    }

    private Optional<Object> getIndexedObject(Object indexedObject) {
        if (indexedObject instanceof Vector) {
            Vector idx = (Vector<?>) indexedObject;
            Vector swifts = ((Vector<?>) idx.get(0));
            Vector objects = ((Vector<?>) idx.get(1));
            return IntStream.range(0, swifts.size()).mapToObj(i -> objects.get(i)).filter(Objects::nonNull).findFirst();

        }
        return indexedObject == null ? Optional.empty() : Optional.of(indexedObject);
    }
}
