package calypsox.tk.util.swiftparser;

import calypsox.tk.bo.mapping.triparty.SantTripartyAllocationMultiObjectSaver;
import calypsox.tk.report.SantMissingIsinUtil;
import calypsox.tk.report.SantMissingIsinUtilContract;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.BulkObjectSaver;
import com.calypso.tk.util.MessageParseException;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MT569MessageProcessor extends com.calypso.tk.util.swiftparser.MT569MessageProcessor {

    // START CALYPCROSS-435 - fperezur
    private static final String ATTR_COMMENT_ID = "CommentId";
    private static final String ATTR_MC_ENTRY_ID = "marginCallEntryId";
    private static final String ATTR_MC_CONFIG_ID = "marginCallConfigId";
    // END CALYPCROSS-435 - fperezur

    @Override
    protected void setMessageAttributes(
            Trade trade,
            BOMessage message,
            SwiftMessage swift,
            BOMessage indexedMessage,
            java.util.Vector errors,
            DSConnection ds)
            throws MessageParseException {
        super.setMessageAttributes(trade, message, swift, indexedMessage, errors, ds);
        try {
            message.setAttribute("CustomPD", getDate(swift));
            String tripartyAccount = getJPTripartyAccount(swift, message);
            if (!Util.isEmpty(tripartyAccount)){
                message.setAttribute("Triparty Account Id", tripartyAccount);
            }
        } catch (ParseException e) {
            Log.error(this, "Cannot parse string to date " + e);
        }

        // Project: MISSING_ISIN
        SantMissingIsinUtilContract contract = addMissingIsinAttributes(message, swift);

        if(null!=contract && !contract.isRepoTriparty()){
            // START CALYPCROSS-435 - fperezur
            addCommentIdAttributes(message);
            // END CALYPCROSS-435 - fperezur
        }



    }

    private String getDate(SwiftMessage message) throws ParseException {
        SwiftFieldMessage swiftField =
                message.getSwiftField(message.getFields(), ":98E:", ":PREP//", null);
        JDate jdate = null;
        if (swiftField != null) {
            String line = swiftField.getValue();
            if (line.contains(":PREP//")) {
                String[] values = line.split("//");
                if (values[1].length() >= 8) {
                    String tagdate = values[1].substring(0, 8);
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
                    jdate = JDate.valueOf(formatter.parse(tagdate));
                }
            }
        }
        if (jdate == null) {
            jdate = JDate.getNow();
        }
        return jdate.toString();
    }

    public String getJPTripartyAccount(SwiftMessage swiftMessage, BOMessage message) throws ParseException {

        SwiftFieldMessage swiftField = swiftMessage.getSwiftField(swiftMessage.getFields(), ":20C:", ":TCTR//", null);
        if (swiftField != null && isJPTripartySender(message)){
            String line = swiftField.getValue();
            if (line.contains(":TCTR//")){
                String [] values = line.split("//");
                return values[1];
            }
        }
        return null;
    }

    // Project: MISSING_ISIN
    /**
     * Adds the attributes needed by this message to generate MISSING_ISIN tasks for every ISIN code
     * that is missing in the system.
     *
     * @param message Message the attributes are going to be added to.
     * @param swiftMessage Text of the MT569 input message.
     */
    private SantMissingIsinUtilContract addMissingIsinAttributes(BOMessage message, SwiftMessage swiftMessage) {
        SantMissingIsinUtil.SwiftFieldsMap fieldsMap = SantMissingIsinUtil.getInstance().getAllFields(swiftMessage);

        final SantMissingIsinUtilContract contract = SantMissingIsinUtil.getInstance().getContractId(fieldsMap, message);
        String counterparty = SantMissingIsinUtil.getInstance().getCounterparty(contract);
        Collection<String> isins = SantMissingIsinUtil.getInstance().getIsins(fieldsMap);

        message.setAttribute(SantMissingIsinUtil.MESSAGE_ATTRIBUTE_CONTRACTID, contract.toString());
        message.setAttribute(SantMissingIsinUtil.MESSAGE_ATTRIBUTE_COUNTERPARTY, counterparty);
        if(contract.isRepoTriparty()){
            message.setAttribute(SantMissingIsinUtil.MESSAGE_ATTRIBUTE_ISREPOTRIPARTY, "true");
        }

        int isinCount = 1;
        for (String isin : isins) {
            String isinMessageAttributeName =
                    SantMissingIsinUtil.getInstance().getIsinMessageAttributeName(isinCount);
            message.setAttribute(isinMessageAttributeName, isin);
            isinCount++;
        }
        return contract;
    }

    // START CALYPCROSS-435 - fperezur
    /**
     * Add the new attribute CommentId
     *
     * @param message
     */
    private void addCommentIdAttributes(BOMessage message) {
        List<Integer> ids = new ArrayList<Integer>();
        ids.add(getContractId(message));
        List<MarginCallEntryDTO> entries;
        JDate processDate = message.getCreationDate().getJDate(TimeZone.getDefault());
        int entryId = 0;

        try {
            int defaultContextId = ServiceRegistry.getDefaultContext().getId();
            entries =
                    ServiceRegistry.getDefault()
                            .getCollateralServer()
                            .loadEntries(ids, processDate, defaultContextId, true);
            if (!Util.isEmpty(entries)) {
                entryId = entries.get(0).getId();
                message.setAttribute(ATTR_COMMENT_ID, String.valueOf(entryId));
                message.setAttribute(ATTR_MC_ENTRY_ID, String.valueOf(entryId));
                message.setAttribute(ATTR_MC_CONFIG_ID, String.valueOf(ids.get(0)));
            } else {
                Log.error(this, "Cannot find any marginCallDTO with contract id: " + ids.get(0));
            }
        } catch (RemoteException e) {
            Log.error(this, "Error with entry: " + ids.get(0) + "Error: " + e);
        }
    }

    /**
     * Get the contract Id from a specific attribute of the message
     *
     * @param message
     * @return entryId
     */
    private int getContractId(BOMessage message) {
        return Optional.ofNullable(message.getAttribute(SantMissingIsinUtil.MESSAGE_ATTRIBUTE_CONTRACTID)).map(Integer::parseInt).orElse(0);
    }


    // END CALYPCROSS-435 - fperezur

    public String getValueFieldFromSwift(SwiftMessage swiftMessage, String field){
        SwiftFieldMessage swiftFieldMessage = null;
        for (SwiftFieldMessage swiftField: swiftMessage.getFields()){
            if (swiftField.getValue().contains(field + "//")){
                swiftFieldMessage = swiftField;
                break;
            }
        }

        if (swiftFieldMessage != null) {
            String[] values = swiftFieldMessage.getValue().split("//");
            if (values.length != 1 && !values[1].isEmpty()) {
                return values[1];
            }
        }

        return null;
    }

    public JDate parseToJDate(String field){

        try {
            if (field != null && field.length() >= 8) {
                String tagdate = field.substring(0, 8);
                SimpleDateFormat formatter = new SimpleDateFormat(
                        "yyyyMMdd");
                return JDate.valueOf(formatter.parse(tagdate));
            }
        } catch (Exception e) {
            Log.error("Error getting "+ field + " as date", e);
        }
        return null;
    }

    public boolean processExternalMessage(Object object, Trade indexedTrade, BOMessage indexedMessage, BOTransfer indexedTransfer, SwiftMessage swiftMessage, boolean indexed, boolean matched, List<String> errors, PSEvent event, String engineName, DSConnection ds, Object dbCon) throws MessageParseException {

        try {
            if (swiftMessage != null) {
                JDate prep = parseToJDate(getValueFieldFromSwift(swiftMessage, "PREP"));
                JDate valn = parseToJDate(getValueFieldFromSwift(swiftMessage, "VALN"));

                if (valn == null || prep == null || !valn.equals(prep)) {
                    //Log.system(MT569MessageProcessor.class.getName(),"Swift Message MT569 has not been processed because VALN and PREP fields are different, empty or wrong format");
                    return true;
                }
            }

        } catch (Exception e) {
            Log.error("Error trying to compare PREP and VALUE fields", e);
        }

        return super.processExternalMessage(object, indexedTrade, indexedMessage, indexedTransfer, swiftMessage, indexed, matched, errors, event, engineName, ds, dbCon);
    }

    //TripartyJP
    protected boolean isJPTripartySender(BOMessage message) {
        LegalEntityAttribute isJPMorganTripartyAgent = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),0, message.getSenderId(), "Agent", "IsJPMorganTripartyAgent");
        return Optional.ofNullable(isJPMorganTripartyAgent).map(LegalEntityAttribute::getAttributeValue).map(s->s.equalsIgnoreCase("true")).orElse(false);
    }

    @Override
    protected BulkObjectSaver createSaver() {
        return new SantTripartyAllocationMultiObjectSaver();
    }
}
