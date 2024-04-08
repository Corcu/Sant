package calypsox.tk.util;

import calypsox.tk.util.swiftparser.util.SwiftMessageProcessorUtil;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.util.UploaderSQLBindAPI;
import com.calypso.tk.util.*;
import com.calypso.tk.util.swiftparser.MessageMatcher;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ScheduledTaskREPROCESS_SWIFT_MESSAGE reprocesses MT messages by creating a
 * new one and deleting or archiving the original message
 *
 * @author Ruben Garcia
 */
public class ScheduledTaskREPROCESS_SWIFT_MESSAGE extends ScheduledTask {

    /**
     * The default serial version UID
     */
    private static final long serialVersionUID = 5313836060948047338L;

    /**
     * ST attribute msg status. Filter BOMessages by status
     */
    private static final String MSG_STATUS = "Attr. MESSAGE_STATUS";

    /**
     * ST attribute msg type. Filter BOMessages by type
     */
    private static final String MSG_TYPE = "Attr. MESSAGE_TYPE";

    /**
     * ST attribute format type. Filter BOMessages by format type
     */
    private static final String FORMAT_TYPE = "Attr. FORMAT_TYPE";

    /**
     * ST attribute template name. Filter BOMessages by template name
     */
    private static final String TEMPLATE_NAME = "Attr. TEMPLATE_NAME";

    /**
     * ST attribute receiver. Filter BOMessages by receiver
     */
    private static final String RECEIVER = "Attr. RECEIVER";

    /**
     * ST attribute Creation date offset. The offset to filter by creation date
     */
    private static final String CREATION_DATE_OFFSET = "Creation date offset";

    /**
     * ST attribute SDF. Name of the SDF using to post-filter BOMessages
     */
    private static final String MSG_SDF = "Msg SDF";

    /**
     * ST attribute Gateway. Used for parse SWIFT document
     */
    private static final String GATEWAY = "GATEWAY";

    /**
     * ST attribute ExternalMessageType. Used to get the handler and Parser. By default, SWIFT.
     */
    private static final String EXTERNAL_MSG_TYPE = "ExternalMessageType";

    /**
     * ST attributes After reprocessing. Used to indicate the action after processing the BOMessage.
     */
    private static final String AFTER_REPROCESSING_BOMESSAGES = "After reprocessing BOMessages";

    /**
     * After reprocessing attribute value Do nothing. It does not delete the messages.
     */
    private static final String AFTER_REPROCESSING_DO_NOTHING = "Do nothing";

    /**
     * After reprocessing attribute value Delete. Delete the processing BOMessages.
     */
    private static final String AFTER_REPROCESSING_DELETE = "Delete";

    /**
     * After reprocessing attribute Archive. Archive the processing BOMessages.
     */
    private static final String AFTER_REPROCESSING_ARCHIVE = "Archive";

    /**
     * After reprocessing attribute. Actions to do in message tasks.
     */
    private static final String AFTER_REPROCESSING_TASKS = "After reprocessing Tasks";

    /**
     * After reprocessing move message tasks to Complete.
     */
    private static final String COMPLETE = "Complete";

    /**
     * After reprocessing move message task to Complete and Archive.
     */
    private static final String COMPLETE_AND_ARCHIVE = "Complete & Archive";

    /**
     * After reprocessing move message task to Complete and Delete.
     */
    private static final String COMPLETE_AND_DELETE = "Complete & Delete";

    /**
     * ST attribute Num of Workers. Indicates the number of threads to process the BOMessages.
     */
    private static final String NUM_OF_WORKERS = "Num of workers";

    /**
     * ST attribute Pre-match filter. Indicates if you want to do a pre-matching of the candidate messages to be processed.
     */
    private static final String PRE_MATCH_FILTER = "Pre-match filter";

    /**
     * ST attribute, the original EOL pattern. To process SWIFT text document
     */
    private static final String ORIGINAL_EOL_PATTERN = "Original EOL Pattern";

    /**
     * ST attribute list separator.
     */
    private static final String SEPARATOR = ",";

    /**
     * The default EOL Swift patern
     */
    private static final String DEFAULT_EOL_PATTERN = "\\r?\\n";

    /**
     * The message partition size to complete task and archive/remove
     */
    private static final String PARTITION_SIZE = "Partition Size";

    @Override
    public String getTaskInformation() {
        return "ScheduledTask to reprocess swift messages that were not matched";
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        List<BOMessage> messages = getBoMessages(ds);
        processBOMessages(ds, messages);
        afterReprocess(ds, messages);
        return super.process(ds, ps);
    }

    @Override
    public boolean isValidInput(Vector<String> messages) {
        boolean ret = super.isValidInput(messages);

        if (messages == null) {
            messages = new Vector<>();
        }

        if (Util.isEmpty(getPricingEnv())) {
            messages.add("Pricing Env cannot be empty.");
            ret = false;
        }
        return ret;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        return Arrays.asList(
                attribute(MSG_STATUS).mandatory().domainName("messageStatus").description("Used as a filter. Value of the MESSAGE_STATUS attribute. Separate values with , no spaces.").multipleSelection(true),
                attribute(MSG_TYPE).domainName("messageType").description("Used as a filter. Comma separated values of the MESSAGE_TYPE attribute (for example INCOMING). Separate values with , no spaces.").multipleSelection(true),
                attribute(FORMAT_TYPE).domainName("formatType").description("Used as a filter. Comma separated values of the FORMAT_TYPE attribute (for example SWIFT). Separate values with , no spaces.").multipleSelection(true),
                attribute(TEMPLATE_NAME).description("Used as a filter. Comma separated values of the TEMPLATE_NAME attribute (for example MT546,MT47). Separate values with , no spaces."),
                attribute(RECEIVER).description("Used as a filter. Comma separated values of the RECEIVER attribute (for example BSCHE).Separate values with , no spaces."),
                attribute(CREATION_DATE_OFFSET).description("Used as a filter. Offset on the message creation date based on the ValDate in days").integer(),
                attribute(MSG_SDF).domain(getSDFNames(this.getDSConnection())).description("Static data filter for filter BOMessages"),
                attribute(GATEWAY).domainName("gateway").description("Used to process the message. Gateway for the new generated message."),
                attribute(EXTERNAL_MSG_TYPE).domainName("formatType").description("Used for parser and handle selection. By default SWIFT."),
                attribute(AFTER_REPROCESSING_BOMESSAGES).domain(Arrays.asList(AFTER_REPROCESSING_DO_NOTHING, AFTER_REPROCESSING_DELETE, AFTER_REPROCESSING_ARCHIVE)).mandatory().description("Indicate what you want to do with the reprocessed messages."),
                attribute(NUM_OF_WORKERS).mandatory().integer().description("Indicates the number of threads to process the BOMessages."),
                attribute(PRE_MATCH_FILTER).description("Indicates if you want to do a pre-matching of the candidate messages to be processed.").booleanType(),
                attribute(ORIGINAL_EOL_PATTERN).description("Original end-of-line pattern for SWIFT file depending on the Origin Operating System. Default \\r?\\n"),
                attribute(PARTITION_SIZE).description("The partition size to complete task and archive or remove messages. Default 50.").integer(),
                attribute(AFTER_REPROCESSING_TASKS).description("Indicate what you want to do with the reprocessed messages tasks.").mandatory().domain(Arrays.asList(AFTER_REPROCESSING_DO_NOTHING, COMPLETE, COMPLETE_AND_ARCHIVE, COMPLETE_AND_DELETE))
        );

    }

    /**
     * Gets the BOMessages using the filter attributes of the ST and the SDF
     *
     * @param dsCon the Data Server connection
     * @return the list of filter BOMessages
     */
    private List<BOMessage> getBoMessages(DSConnection dsCon) {
        if (dsCon == null) {
            Log.error(this, "DSConnection is NULL");
            return new ArrayList<>();
        }
        List<CalypsoBindVariable> bindVariables = new ArrayList<>();
        String status = getAttribute(MSG_STATUS);
        String type = getAttribute(MSG_TYPE);
        String format_type = getAttribute(FORMAT_TYPE);
        String template_name = getAttribute(TEMPLATE_NAME);

        String where = "";

        where = buildListWhere(buildListWhere(buildListWhere(buildListWhere(where,
                                "bo_message.message_type", type),
                        "bo_message.format_type", format_type),
                "bo_message.template_name", template_name), "bo_message.message_status", status);

        List<Integer> receiversIds = this.getMessageReceiver(dsCon);
        if (!Util.isEmpty(receiversIds)) {
            if (!Util.isEmpty(where)) {
                where += " AND ";
            }
            where += " bo_message.receiver_id IN " + Util.collectionToSQLString(receiversIds);
        }

        String offsetS = getAttribute(CREATION_DATE_OFFSET);
        if (!Util.isEmpty(offsetS) && this.getValuationDatetime() != null) {
            int offset = -1;
            try {
                offset = Math.abs(Integer.parseInt(offsetS));
            } catch (NumberFormatException e) {
                Log.warn(this, "Attribute " + CREATION_DATE_OFFSET + " It has to be an integer.");
            }
            if (offset >= 0) {
                JDatetime creationEndDate = this.getValuationDatetime();
                Holiday hol = Holiday.getCurrent();
                TimeZone tz = this.getTimeZone() != null ? this.getTimeZone() : TimeZone.getDefault();
                JDate date = hol.addBusinessDays(creationEndDate.getJDate(tz), this._holidays, -offset, false);
                if (date != null) {
                    JDatetime creationStartDate = new JDatetime(date, tz);
                    creationStartDate = creationStartDate.add(-1, 0, 1, 0, 0);
                    if (!Util.isEmpty(where)) {
                        where += " AND ";
                    }
                    where += " bo_message.creation_sys_date >= ? ";
                    bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATETIME, creationStartDate));
                    where += " AND bo_message.creation_sys_date <= ? ";
                    bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATETIME, creationEndDate));
                }
            }
        }
        MessageArray v;

        try {
            Log.info(this, "Get BOMessages to reprocess where: " + where);
            v = dsCon.getRemoteBO().getMessages(where, bindVariables);
        } catch (Exception e) {
            Log.error(this, e);
            return new ArrayList<>();
        }

        if (v != null) {
            List<BOMessage> messages = Arrays.asList(v.getMessages());
            StaticDataFilter sdf = getMsgSDF(dsCon);
            if (sdf != null) {
                Log.info(this, "Filter BOMessages using SDF: " + sdf);
                return messages.stream().filter(sdf::accept).collect(Collectors.toList());
            }
            return messages;
        }
        return new ArrayList<>();
    }


    /**
     * Constructs the where for lists of elements.
     *
     * @param where  SQL where
     * @param field  current field
     * @param values the list of values
     * @return where SQL
     */
    private String buildListWhere(String where, String field, String values) {
        if (!Util.isEmpty(field) && !Util.isEmpty(values) && !values.contains("ALL")) {
            if (!Util.isEmpty(where)) {
                where += " AND ";
            }
            return where + " " + field + " IN " + Util.collectionToSQLString(
                    Util.stringToCollection(new ArrayList<>(), values, SEPARATOR, true));
        }
        return where;
    }

    /**
     * Processes the list of BOMessages multithreading.
     * Gets the AdviceDocument.
     * Generates the ExternalMessage with the content of the AdviceDocument.
     * Reprocesses the ExternalMessage using native Calypso logic.
     *
     * @param dsCon    the Data Server connection
     * @param messages the List of BOMessages
     */
    public void processBOMessages(DSConnection dsCon, List<BOMessage> messages) {
        if (!Util.isEmpty(messages) && dsCon != null) {
            PricingEnv env = getPricingEnv(dsCon);
            if (env != null) {
                String numOfWorkersS = getAttribute(NUM_OF_WORKERS);
                String gateway = this.getGateway();
                String externalMessageType = this.getExternalMsgType();
                boolean preMatchFilter = getBooleanAttribute(PRE_MATCH_FILTER);
                int numOfWorkers = 1;
                if (!Util.isEmpty(numOfWorkersS)) {
                    try {
                        numOfWorkers = Integer.parseInt(numOfWorkersS);
                    } catch (NumberFormatException e) {
                        Log.warn(this, "Attribute " + NUM_OF_WORKERS + " It has to be an integer, it is " +
                                "reported by default with 1.");
                    }
                }
                numOfWorkers = numOfWorkers <= 0 ? 1 : numOfWorkers;
                int partitionSize = messages.size();
                if (messages.size() > numOfWorkers) {
                    partitionSize = messages.size() / numOfWorkers;
                    if (messages.size() % numOfWorkers != 0) {
                        partitionSize++;
                    }
                }

                List<List<BOMessage>> partitions = new ArrayList<>();
                for (int i = 0; i < messages.size(); i += partitionSize) {
                    partitions.add(messages.subList(i,
                            Math.min(i + partitionSize, messages.size())));
                }

                List<Thread> threads = new ArrayList<>();

                partitions.forEach(p -> threads.add(new Thread(new ProcessBOMessagesWorker(dsCon, p, env, gateway, externalMessageType, preMatchFilter))));

                threads.forEach(Thread::start);

                threads.forEach(t -> {
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        Log.error(this, e);
                    }
                });
            }
        }
    }


    /**
     * Get the ST GATEWAY attribute value
     *
     * @return the GATEWAY value
     */
    private String getGateway() {
        return !Util.isEmpty(this.getAttribute(GATEWAY)) ? this.getAttribute(GATEWAY) : "";
    }

    /**
     * Get the ExternalMessageType attribute value. If empty SWIFT
     *
     * @return the ExternalMessageType value
     */
    private String getExternalMsgType() {
        return !Util.isEmpty(this.getAttribute(EXTERNAL_MSG_TYPE)) ? this.getAttribute(EXTERNAL_MSG_TYPE) : "SWIFT";
    }

    /**
     * Gets the ID of the LegalEntity informed in the attribute of the ST RECEIVER
     *
     * @param dsCon the Data Server connection
     * @return the RECEIVER id
     */
    private List<Integer> getMessageReceiver(DSConnection dsCon) {
        String receiverAttr = this.getAttribute(RECEIVER);
        List<Integer> ids = new ArrayList<>();
        if (!Util.isEmpty(receiverAttr)) {
            List<String> receivers = Util.stringToCollection(new ArrayList<>(), receiverAttr, SEPARATOR, true);
            for (String r : receivers) {
                LegalEntity le = BOCache.getLegalEntity(dsCon, r);
                if (le != null) {
                    ids.add(le.getId());
                }
            }
        }
        return ids;
    }

    /**
     * Gets the SDF associated with the name of the ST attribute
     *
     * @param ds the Data Server connection
     * @return the StaticDataFilter
     */
    private StaticDataFilter getMsgSDF(DSConnection ds) {
        String sdfName = this.getAttribute(MSG_SDF);
        if (!Util.isEmpty(sdfName)) {
            return BOCache.getStaticDataFilter(ds, sdfName);
        }
        return null;
    }

    /**
     * Get the Pricing Environment instance
     *
     * @param dsCon the Data Server connection
     * @return the Pricing Environment
     */
    private PricingEnv getPricingEnv(DSConnection dsCon) {
        if (dsCon != null && !Util.isEmpty(this._pricingEnv)) {
            try {
                return dsCon.getRemoteMarketData().getPricingEnv(this._pricingEnv, this.getValuationDatetime());
            } catch (Exception e) {
                Log.error(this, e);
            }
        }
        return null;
    }

    /**
     * Deletes or archives the processed BOMessages, provided that it is indicated in the ST attribute
     * and a new message has been generated with the Original Message ID attribute reported.
     *
     * @param dsCon    the Data Server connection
     * @param messages the list of BOMessages
     */
    private void afterReprocess(DSConnection dsCon, List<BOMessage> messages) {
        if (dsCon != null && !Util.isEmpty(messages)) {
            String messagesAction = getAttribute(AFTER_REPROCESSING_BOMESSAGES);
            String tasksAction = getAttribute(AFTER_REPROCESSING_TASKS);
            if ((!Util.isEmpty(messagesAction) && !AFTER_REPROCESSING_DO_NOTHING.equals(messagesAction))
                    || (!Util.isEmpty(tasksAction) && !AFTER_REPROCESSING_DO_NOTHING.equals(tasksAction))) {
                Set<Long> idsToRemove = getBOMessageIDsToRemove(dsCon, messages);
                if (!Util.isEmpty(idsToRemove)) {
                    List<List<Long>> partitions = generateIDsPartitions(new ArrayList<>(idsToRemove));
                    boolean isCopyToArchive = AFTER_REPROCESSING_ARCHIVE.equals(messagesAction);
                    for (List<Long> ids : partitions) {
                        if (!Util.isEmpty(tasksAction) && !AFTER_REPROCESSING_DO_NOTHING.equals(tasksAction)) {
                            completeTasks(dsCon, ids, tasksAction);
                        }
                        if (!Util.isEmpty(messagesAction) && !AFTER_REPROCESSING_DO_NOTHING.equals(messagesAction)) {
                            deleteOrArchiveMessages(ids, isCopyToArchive);
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete or archive BOMessages
     *
     * @param ids             the partition message IDs
     * @param isCopyToArchive true id archive BOMessage before delete
     */
    private void deleteOrArchiveMessages(List<Long> ids, boolean isCopyToArchive) {
        String where = " bo_message.message_id IN " + Util.collectionToSQLString(ids);
        Log.info(this, "Remove BOMessages where: " + where + " . Archived: " + isCopyToArchive);
        try {
            int count = UploaderSQLBindAPI.deleteRemoteBOMessages(where, isCopyToArchive, new ArrayList<>());
            Log.info(this, "Remove " + count + " BOMessages. Archived: " + isCopyToArchive);
        } catch (CalypsoServiceException e) {
            Log.error(this, e);
        }
    }

    /**
     * Complete the BOMessages tasks
     *
     * @param dsCon  the Data Server connection
     * @param ids    the message ids
     * @param action the task action COMPLETE, COMPLETE & ARCHIVE , COMPLETE & DELETE
     */
    private void completeTasks(DSConnection dsCon, List<Long> ids, String action) {
        String where = " ( object_classname = ? OR event_class = ? ) and task_status != ? and object_id IN " + Util.collectionToSQLString(ids);
        try {
            Log.info(this, "Completed where: " + where + " . Action: " + action);
            TaskArray tasksToComplete = dsCon.getRemoteBackOffice().getTasks(where, Arrays.asList(
                    new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, BOMessage.class.getSimpleName()),
                    new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, PSEventMessage.class.getSimpleName()),
                    new CalypsoBindVariable(CalypsoBindVariable.INTEGER, Task.COMPLETED)));
            Set<Long> taskIds = new HashSet<>();
            if (tasksToComplete != null && !tasksToComplete.isEmpty()) {
                Set<Long> linkedTasks = tasksToComplete.toArrayList().stream().map(Task::getLinkId)
                        .filter(linkId -> linkId > 0).collect(Collectors.toSet());
                if (!Util.isEmpty(linkedTasks)) {
                    //Get linked task no classname BOMessage
                    where = " task_status != ? and task_id IN " + Util.collectionToSQLString(linkedTasks) +
                            " and object_id IN " + Util.collectionToSQLString(ids);
                    Log.info(this, "Completed where: " + where + " . Action: " + action);
                    TaskArray linkedTasksArray = dsCon.getRemoteBackOffice().getTasks(where,
                            Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, Task.COMPLETED)));
                    if (linkedTasksArray != null && !linkedTasksArray.isEmpty()) {
                        tasksToComplete.addAll(linkedTasksArray);
                    }
                }
                tasksToComplete = new TaskArray(tasksToComplete.toArrayList().stream().distinct().collect(Collectors.toList()));
                for (int i = 0; i < tasksToComplete.size(); i++) {
                    tasksToComplete.get(i).setStatus(Task.COMPLETED);
                    taskIds.add(tasksToComplete.get(i).getId());
                }

                dsCon.getRemoteBackOffice().saveAndPublishTasks(tasksToComplete, 0L, null, true);
                if (!Util.isEmpty(action) && (COMPLETE_AND_ARCHIVE.equals(action) || COMPLETE_AND_DELETE.equals(action))) {
                    //Check if task is completed
                    where = " task_status = ? AND task_id IN " + Util.collectionToSQLString(taskIds);
                    List<CalypsoBindVariable> variables = Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.INTEGER,
                            Task.COMPLETED));
                    dsCon.getRemoteBackOffice().deleteTasks(where, COMPLETE_AND_ARCHIVE.equals(action), variables);
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, e);
        }
    }

    /**
     * Generates a partitioned list of size 500
     *
     * @param ids list to partitioned
     * @return the partitioned list
     */
    private List<List<Long>> generateIDsPartitions(List<Long> ids) {
        List<List<Long>> partitions = new ArrayList<>();
        if (!Util.isEmpty(ids)) {
            int partitionSize = getIntegerAttribute(PARTITION_SIZE, 50);
            if (ids.size() <= partitionSize) {
                partitions.add(ids);
            } else {
                for (int i = 0; i < ids.size(); i += partitionSize) {
                    partitions.add(ids.subList(i,
                            Math.min(i + partitionSize, ids.size())));
                }
            }
        }
        return partitions;
    }


    /**
     * Gets the IDs of the candidate messages to be deleted or archived. For this,
     * a new message with the same Original Message ID must be registered.
     *
     * @param dsCon            the Data Server connection
     * @param originalMessages the list of original BOMessage
     * @return the list of candidate BOMessages IDs
     */
    private Set<Long> getBOMessageIDsToRemove(DSConnection dsCon, List<BOMessage> originalMessages) {
        List<Long> originalIDs = originalMessages.stream().map(BOMessage::getLongId).distinct().collect(Collectors.toList());
        Set<Long> result = new HashSet<>();
        if (!Util.isEmpty(originalIDs)) {
            List<List<Long>> partitions = generateIDsPartitions(originalIDs);
            for (List<Long> ids : partitions) {
                List<CalypsoBindVariable> bindVariables = new ArrayList<>();
                bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, SwiftMessageProcessorUtil.ORIGINAL_MESSAGE_ID));
                String where = " mess_attributes.message_id = bo_message.message_id AND mess_attributes.attr_name = ? ";
                where += " AND mess_attributes.attr_value IN " + Util.collectionToSQLString(ids);
                try {
                    MessageArray array = dsCon.getRemoteBackOffice().getMessages("mess_attributes", where, bindVariables);
                    if (array != null) {
                        result.addAll(Arrays.stream(array.getMessages()).map(m ->
                                Long.parseLong(m.getAttribute(SwiftMessageProcessorUtil.ORIGINAL_MESSAGE_ID))).collect(Collectors.toSet()));
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this, e);
                }
            }
        }
        return result;
    }


    /**
     * Gets the list of registered SDFs
     *
     * @param dsCon the Data Server connection
     * @return the list of SDF names
     */
    private List<String> getSDFNames(DSConnection dsCon) {
        if (dsCon != null) {
            try {
                Vector<String> sdfNames = dsCon.getRemoteReferenceData().getStaticDataFilterNames();
                sdfNames.add("");
                return Util.sort(sdfNames);
            } catch (Exception e) {
                Log.error(this, e);
            }
        }
        return new ArrayList<>();
    }


    /**
     * Worker to process a partition of messages
     *
     * @author Ruben Garcia
     */
    class ProcessBOMessagesWorker implements Runnable {

        /**
         * BOMessages partition
         */
        private final List<BOMessage> messages;

        /**
         * Data Server connection
         */
        private final DSConnection dsCon;

        /**
         * Pricing Environment
         */
        private final PricingEnv env;
        /**
         * The msg gateway
         */
        private final String gateway;

        /**
         * The external message type
         */
        private final String externalMessageType;

        /**
         * Flag pre-match filter
         */
        private final boolean preMatchFilter;

        public ProcessBOMessagesWorker(DSConnection dsCon, List<BOMessage> messages, PricingEnv env, String gateway, String externalMessageType, boolean preMatchFilter) {
            this.messages = messages;
            this.dsCon = dsCon;
            this.env = env;
            this.gateway = gateway;
            this.externalMessageType = externalMessageType;
            this.preMatchFilter = preMatchFilter;
        }

        @Override
        public void run() {
            if (!Util.isEmpty(messages) && dsCon != null && env != null) {
                ExternalMessage ext;
                for (BOMessage m : messages) {
                    Log.info(this, "Process BOMessage: " + m);
                    ext = getExternalMessage(getAdviceDocument(dsCon, m));
                    if (ext != null) {
                        if (!preMatchFilter || preMatchMessagesFilter(dsCon, env, ext)) {
                            SwiftMessageProcessorUtil.addExternalMessageOriginalID(m, ext);
                            processExternalMessage(dsCon, env, ext);
                        }
                    } else {
                        Log.warn(this, "No external message for BOMessage " + m);
                    }
                }
            }
        }

        /**
         * Process the ExternalMessage using native Calypso logic.
         *
         * @param ds              the Data Server connection
         * @param env             the Pricing Environment
         * @param externalMessage the External Message
         */
        private void processExternalMessage(DSConnection ds, PricingEnv env, ExternalMessage externalMessage) {
            if (externalMessage != null) {
                try {
                    ExternalMessageHandler handler = SwiftParserUtil.getHandler(this.externalMessageType, externalMessage.getType(), true);
                    if (handler != null) {
                        handler.handleExternalMessage(externalMessage, env, null, null, ds, null);
                    } else {
                        SwiftParserUtil.processExternalMessage(externalMessage, env, null, null, ds, null);
                    }
                } catch (Exception e) {
                    Log.error(this, e);
                }
            }
        }

        /**
         * Generate the ExternalMessage from the Advice Document
         *
         * @param doc the Advice Document
         * @return the ExternalMessage
         */
        private ExternalMessage getExternalMessage(AdviceDocument doc) {
            if (doc != null && doc.getTextDocument() != null) {
                ExternalMessageParser parser = SwiftParserUtil.getParser(this.externalMessageType);
                try {
                    String parsedText = parseAdviceDocumentText(doc);
                    return parser != null && !Util.isEmpty(parsedText) ? parser.readExternal(parsedText, this.gateway) : null;
                } catch (MessageParseException e) {
                    Log.error(this, e);
                }
            }
            return null;
        }

        /**
         * Modifies line breaks depending on the system it runs on
         *
         * @param doc the AdviceDocument
         * @return the parsed file text
         */
        private String parseAdviceDocumentText(AdviceDocument doc) {
            if (doc != null && doc.getTextDocument() != null) {
                String pattern = getAttribute(ORIGINAL_EOL_PATTERN);
                if (Util.isEmpty(pattern)) {
                    pattern = DEFAULT_EOL_PATTERN;
                }
                String[] lines = doc.getTextDocument().toString().split(pattern);
                if (!Util.isEmpty(lines)) {
                    StringBuilder sb = new StringBuilder();
                    for (String line : lines) {
                        sb.append(line).append(SwiftMessage.END_OF_LINE);
                    }
                    return sb.toString();
                }
            }
            return "";
        }

        /**
         * Get the BOMessage AdviceDocument
         *
         * @param dsCon the Data Server document
         * @param msg   the BOMessage
         * @return the AdviceDocument
         */
        private AdviceDocument getAdviceDocument(DSConnection dsCon, BOMessage msg) {
            if (dsCon != null && msg != null) {
                long id = msg.getLongId();
                if (msg.isChild()) {
                    id = msg.getGroupLongId();
                }
                try {
                    if (!msg.isArchived()) {
                        return dsCon.getRemoteBO().getLatestAdviceDocument(id, null);
                    } else {
                        return dsCon.getRemoteBO().getLatestArchivedAdviceDocument(id, null);
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this, e);
                }
            }
            return null;
        }

        /**
         * Check if the message has an associated candidate object
         *
         * @param dsCon           the Data Server connection
         * @param env             the Pricing Environment
         * @param externalMessage the External Message
         * @return true if it finds an object related to the message
         */
        private boolean preMatchMessagesFilter(DSConnection dsCon, PricingEnv env, ExternalMessage externalMessage) {
            if (dsCon != null && env != null && externalMessage != null) {
                MessageMatcher matcher = SwiftParserUtil.getMatcherParserClass(externalMessage.getType());
                if (matcher == null && externalMessage instanceof SwiftMessage) {
                    if (SwiftParserUtil.isSwiftTrade(dsCon, externalMessage.getType())) {
                        matcher = SwiftParserUtil.getMatcherParserClass("SwiftTrade");
                    } else {
                        matcher = SwiftParserUtil.getMatcherParserClass("Swift");
                    }
                }

                if (matcher != null) {
                    try {
                        return matcher.index(externalMessage, env, dsCon, null, new Vector<String>()) != null;
                    } catch (MessageParseException e) {
                        Log.error(this, e);
                    }
                }
            }
            return false;
        }
    }
}
