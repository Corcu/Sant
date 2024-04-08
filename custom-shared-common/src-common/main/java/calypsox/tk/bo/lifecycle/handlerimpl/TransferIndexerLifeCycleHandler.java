package calypsox.tk.bo.lifecycle.handlerimpl;

import calypsox.tk.bo.swift.SantanderSwiftUtil;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.lifecycle.LifeCycleHandler;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.SQLQuery;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventProcessTransfer;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.AbstractObjectSaver;
import com.calypso.tk.util.BulkObjectSaver;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.SwiftParserUtil;
import com.calypso.tk.util.swiftparser.MessageMatcher;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class TransferIndexerLifeCycleHandler extends LifeCycleHandler {

    private PricingEnv env;
    private final Action msgAction;

 //   private static final String EOL_PATTERN = "(\r(?!\n)|(?<!\r)\n)|(\r\n|\r(?!\n))|(\r\n|(?<!\r)\n)";

    @Override
    public Class<? extends PSEvent>[] subscribe() {
        return new Class[]{PSEventTransfer.class, PSEventProcessTransfer.class};
    }

    public TransferIndexerLifeCycleHandler() {
        String actionName = LocalCache.getDomainValueComment(DSConnection.getDefault(), this.getClass().getSimpleName() + ".config", "OutgoingAction");
        this.msgAction = Util.isEmpty(actionName) ? Action.UPDATE : Action.valueOf(actionName);
    }

    @Override
    public BulkObjectSaver handleEvent(PSEvent psEvent) throws Exception {
        BOTransfer xfer = ((PSEventTransfer) psEvent).getBoTransfer();

        BOTransfer latestXfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(xfer.getLongId());
        if (latestXfer.getVersion() > xfer.getVersion())
            return null;

        MessageArray candidates = getMessageCandidates(latestXfer);

        if (candidates != null && !candidates.isEmpty()) {
            BulkObjectSaver saver = new BulkMessageSaver();
            for (BOMessage msg : candidates) {

                MessageMatcher matcher = SwiftParserUtil.getMatcherParserClass(msg.getTemplateName());
                if (matcher == null) {
                    if (SwiftParserUtil.isSwiftTrade(DSConnection.getDefault(), msg.getTemplateName())) {
                        matcher = SwiftParserUtil.getMatcherParserClass("SwiftTrade");
                    } else {
                        matcher = SwiftParserUtil.getMatcherParserClass("Swift");
                    }
                }


                if (matcher == null) {
                    Log.error(this, String.format("Cannot find a valid MessageMatcher Class for MessageType %s, message %s.", msg.getTemplateName(), msg));
                } else {
                    ExternalMessage externalMessage = SantanderSwiftUtil.toExternalMessage(msg);
                    Vector<String> errors = new Vector<>();
                    Object indexedTo = matcher.index(externalMessage, env, DSConnection.getDefault(), null, errors);
                    if (indexedTo instanceof BOTransfer && ((BOTransfer) indexedTo).getLongId() != msg.getTransferLongId()) {
                        BOTransfer indexedXfer = (BOTransfer) indexedTo;
                        boolean matched = matcher.match(externalMessage, indexedTo, null, indexedXfer, env, DSConnection.getDefault(), null, errors);
                        BOMessage cloneMsg = (BOMessage) msg.clone();
                        cloneMsg.setTransferLongId(indexedXfer.getLongId());
                        cloneMsg.setXferVersion(indexedXfer.getVersion());
                        cloneMsg.setBookId(indexedXfer.getBookId());
                        cloneMsg.setTradeLongId(indexedXfer.getTradeLongId());
                        if (indexedXfer.getTradeLongId() > 0)
                            cloneMsg.setTradeVersion( DSConnection.getDefault().getRemoteTrade().getTrade(indexedXfer.getTradeLongId()).getVersion());
                        cloneMsg.setProductFamily(indexedXfer.getProductFamily());
                        cloneMsg.setProductType(indexedXfer.getProductType());
                        cloneMsg.setMatchingB(matched);
                        cloneMsg.setAction(msgAction);
                        saver.add(BOMessage.class, cloneMsg);
                        saver.add(ObjectDescription.class, new ObjectDescription(indexedXfer.getLongId(), ObjectDescription.TRANSFER, indexedXfer.getVersion()));
                    }
                }
            }
            return Util.isEmpty(saver.get(BOMessage.class)) ? null : saver;
        }
        return null;
    }
/*
    private ExternalMessage toExternalMessage(BOMessage msg) throws CalypsoServiceException, MessageParseException {
        AdviceDocument swift = DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(msg.getLongId(), null);
        ExternalMessageParser parser = SwiftParserUtil.getParser(msg.getFormatType());
        return parser.readExternal(reformatSwift(swift.getTextDocument().toString()), msg.getGateway());
    }
*/
    @Override
    public void setPricingEnv(PricingEnv env) {
        this.env = env;
    }

    private MessageArray getMessageCandidates(BOTransfer latestXfer) throws CalypsoServiceException {
        SQLQuery query = new SQLQuery(null, "bo_message.external_b = 1");
        query.appendWhereClause("bo_message.receiver_id = ?", Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, latestXfer.getInternalLegalEntityId())));
        query.appendWhereClauseVector("bo_message.message_type in", new Vector<>(Collections.singletonList("INCOMING")));
        query.appendWhereClause("bo_message.transfer_id != ?", Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.LONG, latestXfer.getLongId())));

        query.appendWhereClause("((bo_message.message_status = 'PEND STAT UPDATE' and bo_message.template_name not in ('MT544','MT545','MT546','MT547')) OR (bo_message.message_status = 'PEND SETT UPDATE' and bo_message.template_name in ('MT544','MT545','MT546','MT547')))");

        if ("PAY".equals(latestXfer.getTransferType())) {
            if ("DAP".equals(latestXfer.getDeliveryType())) {
                query.appendWhereClauseVector("bo_message.template_name in", new Vector<>(Arrays.asList("MT547", "MT543", "MT548")));
            } else {
                query.appendWhereClauseVector("bo_message.template_name in", new Vector<>(Arrays.asList("MT546", "MT542", "MT548")));
            }
        } else {
            if ("DAP".equals(latestXfer.getDeliveryType())) {
                query.appendWhereClauseVector("bo_message.template_name in", new Vector<>(Arrays.asList("MT545", "MT541", "MT548")));
            } else {
                query.appendWhereClauseVector("bo_message.template_name in", new Vector<>(Arrays.asList("MT544", "MT540", "MT548")));
            }
        }
        query.appendWhereClause("bo_message.message_id in (select message_id from mess_attributes, product_sec_code " +
                        "where mess_attributes.attr_name='Security Code' and product_sec_code.sec_code = 'ISIN' and  mess_attributes.attr_value = code_value_ucase and product_sec_code.product_id = ?)",
                Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, latestXfer.getProductId())));

        if ("DAP".equals(latestXfer.getDeliveryType())) {
            query.appendWhereClause("bo_message.message_id in (select message_id from mess_attributes  " +
                            "where mess_attributes.attr_name='Ccy' and  mess_attributes.attr_value  = ?)",
                    Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, latestXfer.getSettlementCurrency())));
        }

        if (latestXfer.getNominalAmount() != 0) {
            String nomText = Util.numberToString(Math.abs(latestXfer.getNominalAmount()), Locale.UK);

            query.appendWhereClause("bo_message.message_id in (select message_id from mess_attributes  " +
                    "where mess_attributes.attr_name='Nominal Amount' and  mess_attributes.attr_value  = ?) " +
                    "or not exists (select 1 from mess_attributes where mess_attributes.message_id = bo_message.message_id and mess_attributes.attr_name='Nominal Amount')", Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, nomText)));
        }

        SettleDeliveryInstruction intSDI = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), latestXfer.getInternalSettleDeliveryId());

        query.appendWhereClause("bo_message.message_id in (select message_id from mess_attributes  " +
                        "where mess_attributes.attr_name='PO Account' and  mess_attributes.attr_value  = ?) "
                , Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, intSDI.getAgentAccount())));


        MessageArray candidates = DSConnection.getDefault().getRemoteBO().getMessages(query.getFromClause(), query.getWhereClause(), query.getWhereBindVariables());

        if (candidates != null && !candidates.isEmpty()) {
            String sdfName = LocalCache.getDomainValueComment(DSConnection.getDefault(), this.getClass().getSimpleName() + ".config", "IncomingSDFilter");
            if (Util.isEmpty(sdfName))
                return candidates;
            StaticDataFilter sdFilter = BOCache.getStaticDataFilter(DSConnection.getDefault(), sdfName);
            return new MessageArray(Arrays.stream(candidates.getMessages()).filter(m -> m != null && sdFilter.accept(m)).collect(Collectors.toList()));

        }
        return null;
    }

    @Override
    public boolean accept(PSEvent psEvent) {
        if (psEvent instanceof PSEventTransfer) {
            BOTransfer xfer = ((PSEventTransfer) psEvent).getBoTransfer();
            if (xfer != null && xfer.isPayment() && !Status.isCanceled(xfer.getStatus()) && !Status.S_SPLIT.equals(xfer.getStatus())
                    && !Status.S_SETTLED.equals(xfer.getStatus()) && "SECURITY".equals(xfer.getTransferType()) && xfer.getInternalSettleDeliveryId() > 0 && xfer.getExternalSettleDeliveryId() > 0) {

                String status = LocalCache.getDomainValueComment(DSConnection.getDefault(), this.getClass().getSimpleName() + ".config", "OutgoingStatus");
                if (!Util.isEmpty(status) && Util.stringToList(status).stream().noneMatch(s -> Status.valueOf(s.trim()).equals(xfer.getStatus())))
                    return false;

                SettleDeliveryInstruction poSDI = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), xfer.getInternalSettleDeliveryId());

                if (poSDI != null && !poSDI.getMessageToAgent()) {
                    String sdfName = LocalCache.getDomainValueComment(DSConnection.getDefault(), this.getClass().getSimpleName() + ".config", "OutgoingSDFilter");
                    return Util.isEmpty(sdfName) || BOCache.getStaticDataFilter(DSConnection.getDefault(), sdfName).accept(xfer);
                }
            }
        }
        return false;
    }
/*
    private String reformatSwift(String swiftText) {

        String[] lines = swiftText.split(EOL_PATTERN);
        if (!Util.isEmpty(lines)) {
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line).append(SwiftMessage.END_OF_LINE);
            }
            return sb.toString();
        }

        return "";
    }
*/
    public static class BulkMessageSaver extends AbstractObjectSaver {

        public Map<Class<?>, List<Long>> saveInDataServer(long eventId, String engineName, Connection con, Vector<PSEvent> events) throws Exception {
            Map<Class<?>, List<Long>> ids = new HashMap<>();
            List<BOMessage> messageList = this.get(BOMessage.class);
            List<ObjectDescription> xferDescList = this.get(ObjectDescription.class);

            if (!Util.isEmpty(messageList)) {
                for (int i = 0; i < messageList.size(); i++) {
                    BOMessage msg = messageList.get(i);
                    ObjectDescription xferDesc = xferDescList.get(i);

                    BOTransfer xfer = con == null ? DSConnection.getDefault().getRemoteBO().getBOTransfer(xferDesc.getLongId()) : BOTransferSQL.getTransfer(xferDesc.getLongId(), con);
                    if (xfer == null || xfer.getVersion() > xferDesc.getVersion())
                        return ids;

                    if (BOMessageWorkflow.isMessageActionApplicable(msg, xfer, msg.getTradeLongId() > 0 ? (con == null ? DSConnection.getDefault().getRemoteTrade().getTrade(msg.getTradeLongId()) : TradeSQL.getTrade(msg.getTradeLongId(), con)) : null, msg.getAction(), DSConnection.getDefault(), con)) {
                        long msgId = con == null ? DSConnection.getDefault().getRemoteBO().save(msg, eventId, engineName) : BackOfficeServerImpl.save(msg, eventId, engineName, null, con, events, false);
                        ids.computeIfAbsent(BOMessage.class, k -> new ArrayList<>()).add(msgId);
                    } else {
                        Log.error(this, String.format("Action %s is not applicable to message %s.", msg.getAction(), msg));
                    }
                }
            }
            return ids;
        }
    }
}
