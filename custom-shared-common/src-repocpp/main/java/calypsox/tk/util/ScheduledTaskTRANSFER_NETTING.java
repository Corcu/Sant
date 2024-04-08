package calypsox.tk.util;

import com.calypso.apps.navigator.PSEventHandler;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ComparatorNettedTransfer;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TransferArray;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ScheduledTaskTRANSFER_NETTING
 *
 * @author Ruben Garcia
 */
public class ScheduledTaskTRANSFER_NETTING extends ScheduledTask {

    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 5756760085007417280L;
    /**
     * The transfer counterparty code filter attribute
     */
    private static final String TRANSFER_CPTY = "Transfer Counterparty";
    /**
     * The transfer role filter attribute
     */
    private static final String TRANSFER_ROLE = "Transfer external role";
    /**
     * The transfer status filter attribute
     */
    private static final String TRANSFER_STATUS = "Transfer status";
    /**
     * The transfer static data filter name attribute
     */
    private static final String STATIC_DATA_FILTER = "Transfer SDF";
    /**
     * The extra where clause filter
     */
    private static final String WHERE_CLAUSE = "Extra where clause";
    /**
     * The transfer origin netting type
     */
    private static final String ORIGIN_NETTING_TYPE = "Origin netting type";
    /**
     * The netted transfer type (0 not netted, 1 netted)
     */
    private static final String NETTED_TRANSFER = "Netted Transfer";
    /**
     * The transfer destination netting type
     */
    private static final String DESTINATION_NETTING_TYPE = "Destination netting type";

    /**
     * Attribute to indicate if you want to continue with the netting in the event that there is a transfer to process
     */
    private static final String CONTINUE_IF_A_XFER = "Continue if a transfer?";

    /**
     * Attribute to indicate if you want to continue with the netting in the event that there is a transfer to process
     */
    private static final String CONTINUE_IF_VALDATE_NOT_EQUALS_SETTLE_DATE = "Continue if date not eq?";

    /**
     * Attribute to indicate the comment in the netting action
     */
    private static final String COMMENT = "Comment";

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        if (ds != null) {
            TransferArray elegibleTransfers = getEligibleTransfers(ds);
            if (!Util.isEmpty(elegibleTransfers)) {
                TransferArray res = checkActionAssingIsApplicable(elegibleTransfers, ds);
                if (!nettingGroups(res, ds)) {
                    Log.warn(this, "The netting process has not been applied. See details in log.");
                }
            }


        }
        return super.process(ds, ps);
    }

    @Override
    public String getTaskInformation() {
        return "This Scheduled Task: \n 1) Gets the netted transfers through filter criteria " +
                "(You can configure the attributes from day and to days to filter by value date) \n 2)Group transfers by netting key." +
                "\n 3) Cancel transfers with origin netting type.\n 4) Creates or reuses (if it already exists) the net transfer of destination netting type.";
    }


    @Override
    public boolean isValidInput(Vector<String> messages) {
        boolean res = super.isValidInput(messages);
        if (_toDays < _fromDays) {
            messages.add("To Days cannot be less than From Days.");
            res = false;
        }
        return res;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        return Arrays.asList(
                attribute(TRANSFER_CPTY).
                        description("Code of the counterparty associated with the transfer to be processed."),
                attribute(TRANSFER_ROLE).domainName("role").
                        description("External legal entity role of the transfer to process."),
                attribute(TRANSFER_STATUS).domainName("transferStatus").mandatory().description("Status of the transfer to process."),
                attribute(STATIC_DATA_FILTER).domain(getSDFNames(DSConnection.getDefault())).description("Transfer Static Data Filter."),
                attribute(NETTED_TRANSFER).booleanType().mandatory().description("Indicates if the transfer to be processed is net (true) or not net (false)"),
                attribute(WHERE_CLAUSE).description("Extra where clause to filter the transfers to process."),
                attribute(ORIGIN_NETTING_TYPE).mandatory().domainName("nettingType").description("Origin Transfer Netting Type."),
                attribute(DESTINATION_NETTING_TYPE).mandatory().domainName("nettingType").description("Destination Transfer Netting Type."),
                attribute(CONTINUE_IF_A_XFER).mandatory().booleanType().description("Indicate TRUE if you want to continue, in the event that there is only one candidate to do the netting."),
                attribute(CONTINUE_IF_VALDATE_NOT_EQUALS_SETTLE_DATE).mandatory().booleanType().description("Indicate TRUE if you want to continue in case there is a transfer candidate for netting with a Value date different from Settle Date."),
                attribute(COMMENT).mandatory().description("Comment for the netting.")

        );
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
     * Obtains the transfer candidates to make the netting
     *
     * @param ds the Data Server connection
     * @return the list of transfer candidates to make the netting
     */
    private TransferArray getEligibleTransfers(DSConnection ds) {
        JDate startDate = getStartDate();
        JDate endDate = getEndDate();
        String originNetting = getAttribute(ORIGIN_NETTING_TYPE);
        String status = getAttribute(TRANSFER_STATUS);
        boolean netted = getBooleanAttribute(NETTED_TRANSFER);
        if (ds != null && startDate != null && endDate != null && !Util.isEmpty(originNetting) && !Util.isEmpty(status)) {
            List<CalypsoBindVariable> whereBindVariables = new ArrayList<>();
            String where = "bo_transfer.value_date >= ? ";
            where += " AND bo_transfer.value_date <= ? ";
            where += " AND bo_transfer.netting_key = ? ";
            where += " AND bo_transfer.transfer_status  = ? ";
            where += " AND bo_transfer.netted_transfer = ? ";
            whereBindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, startDate));
            whereBindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, endDate));
            whereBindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, originNetting));
            whereBindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, status));
            if (netted) {
                whereBindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, 1));
            } else {
                whereBindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, 0));
            }


            LegalEntity ctpy = this.getTransferCpty(ds);
            if (ctpy != null) {
                where += " AND bo_transfer.orig_cpty_id = ? ";
                whereBindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, ctpy.getId()));
            }

            LegalEntity po = this.getProcessingOrg();
            if (po != null) {
                where += " AND bo_transfer.int_le_id = ? ";
                whereBindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, po.getId()));
            }


            String role = getAttribute(TRANSFER_ROLE);
            if (!Util.isEmpty(role)) {
                where = where + " AND bo_transfer.ext_le_role = ? ";
                whereBindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, role));
            }


            String extraWhereClause = getAttribute(WHERE_CLAUSE);
            if (!Util.isEmpty(extraWhereClause)) {
                where = where + " AND " + extraWhereClause;
            }

            Log.info(this, where);
            TransferArray result = null;
            try {
                result = ds.getRemoteBO().getBOTransfers(where, whereBindVariables);
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }

            if (!Util.isEmpty(result)) {
                StaticDataFilter sdf = getStaticDataFilter(ds);
                if (sdf != null) {
                    return new TransferArray(result.stream().filter(sdf::accept).collect(Collectors.toList()));
                } else {
                    return result;
                }
            }
        }
        return null;

    }

    /**
     * Get the Counterparty object from ST attribute
     *
     * @param ds the Data Server connection
     * @return the Counterparty object
     */
    private LegalEntity getTransferCpty(DSConnection ds) {
        String ctpyCode = this.getAttribute(TRANSFER_CPTY);
        return !Util.isEmpty(ctpyCode) ? BOCache.getLegalEntity(ds, ctpyCode) : null;
    }

    /**
     * Get the Static Data Filter object from ST attribute
     *
     * @param ds the Data Server connection
     * @return the Static Data Filter object
     */
    protected StaticDataFilter getStaticDataFilter(DSConnection ds) {
        String sdfName = this.getAttribute(STATIC_DATA_FILTER);
        return !Util.isEmpty(sdfName) ? BOCache.getStaticDataFilter(ds, sdfName) : null;
    }

    /**
     * Get the start date from ST valuation date and from date attribute
     *
     * @return the transfer valuation start date
     */
    public JDate getStartDate() {
        JDate startDate = JDate.valueOf(this.getValuationDatetime());
        Holiday hol = Holiday.getCurrent();
        if (this._fromDays < 0) {
            for (int i = 0; i < Math.abs(this._fromDays); ++i) {
                startDate = hol.previousBusinessDay(startDate, this._holidays);
            }
        } else {
            for (int i = 0; i < Math.abs(this._fromDays); ++i) {
                startDate = hol.nextBusinessDay(startDate, this._holidays);
            }
        }
        return startDate;
    }

    /**
     * Get the end date from ST valuation date and from date attribute
     *
     * @return the transfer valuation end date
     */
    public final JDate getEndDate() {
        JDate endDate = JDate.valueOf(this.getValuationDatetime());
        Holiday hol = Holiday.getCurrent();
        if (this._toDays < 0) {
            for (int i = 0; i < Math.abs(this._toDays); ++i) {
                endDate = hol.previousBusinessDay(endDate, this._holidays);
            }
        } else {
            for (int i = 0; i < Math.abs(this._toDays); ++i) {
                endDate = hol.nextBusinessDay(endDate, this._holidays);
            }
        }
        return endDate;
    }

    /**
     * Check if action assign is applicable on current transfers, if not applicable remove transfer
     *
     * @param currentGroup the current transfers group
     * @return the filter transfer array
     */
    private TransferArray checkActionAssingIsApplicable(TransferArray currentGroup, DSConnection dsCon) {
        Vector<String> possibleActions;
        TransferArray result = new TransferArray();
        for (BOTransfer t : currentGroup) {
            possibleActions = BOTransferWorkflow.getBOTransferActions(t, dsCon);
            if (!Util.isEmpty(possibleActions) && possibleActions.contains(Action.ASSIGN.toString())) {
                if (AccessUtil.isAuthorized("Transfer", t.getProductType(), t.getStatus().toString(), Action.ASSIGN.toString())) {
                    t.setAction(Action.ASSIGN);
                    result.add(t);
                } else {
                    Log.error(this, "Action is not allowed " + Action.ASSIGN + " for the transfer " + t.getLongId() + ", it is removed from the process");
                }
            } else {
                Log.error(this, "The action " + Action.ASSIGN + " cannot be applied to the transfer " + t.getLongId() + ", it is removed from the process");
            }
        }
        return result;
    }

    /**
     * Check transfer group size to net. If empty return false. If size is 1 check user configuration to continue.
     *
     * @param currentGroup the current transfer group
     * @return true if correct size
     */
    private boolean checkGroupSize(TransferArray currentGroup) {
        if (Util.isEmpty(currentGroup)) {
            return false;
        } else {
            if (currentGroup.size() == 1) {
                Log.info(this, "Current transfer group has only 1 transfer to net, transfer ID: " +
                        currentGroup.get(0).getLongId());
                if (!getBooleanAttribute(CONTINUE_IF_A_XFER)) {
                    Log.warn(this, "The process is stopped, there is only one transfer " +
                            "to process and the attribute " + CONTINUE_IF_A_XFER + " is set to FALSE.");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if destination netting type is none and has no net transfers in the group
     *
     * @param currentGroup the current transfers group
     * @param nettingType  the current netting type
     * @return true if netting type not equals none or has only net transfer in the group
     */
    private boolean checkIfNoneNettingType(TransferArray currentGroup, String nettingType) {
        if (!Util.isEmpty(nettingType) && !Util.isEmpty(currentGroup) && nettingType.equals("None")) {
            List<Long> ids = currentGroup.stream().filter(t -> !t.getNettedTransfer()).map(BOTransfer::getLongId).collect(Collectors.toList());
            if (!Util.isEmpty(ids)) {
                Log.error(this, "It is configured as destination netting type NONE, " +
                        "candidate transfers have been detected in the process that are not netted " +
                        "(transfers ID " + ids + " ). Please check the filters or" +
                        " change the destination netting type. The process stops.");
                return false;
            }
        }
        return true;
    }

    /**
     * Check if there is any transfer with value date other than settle date
     *
     * @param currentGroup the current transfer group
     * @return true if there are no transfers with different dates
     */
    private boolean checkTransferValueDateEqSettleDate(TransferArray currentGroup) {
        if (!Util.isEmpty(currentGroup)) {
            List<Long> ids = currentGroup.stream().filter(t -> t.getSettleDate() != null && t.getValueDate() != null &&
                    !t.getSettleDate().equals(t.getValueDate())).map(BOTransfer::getLongId).collect(Collectors.toList());
            if (!Util.isEmpty(ids)) {
                Log.info(this, "The value date is not equals to settle date in these transfers " + ids);
                if (!getBooleanAttribute(CONTINUE_IF_VALDATE_NOT_EQUALS_SETTLE_DATE)) {
                    Log.warn(this, "Processing is stopped because the attribute " + CONTINUE_IF_VALDATE_NOT_EQUALS_SETTLE_DATE + " is FALSE.");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Applies the netting action on a group of transfers.
     *
     * @param currentGroup the current transfer group
     * @return true if the netting action has been executed
     */
    private boolean nettingGroups(TransferArray currentGroup, DSConnection ds) {
        String nettingType = getAttribute(DESTINATION_NETTING_TYPE);
        if(!checkGroupSize(currentGroup)){
            return false;
        }
        if (!Util.isEmpty(nettingType)) {
            boolean isNone = false, isRollOver = false;

            if (nettingType.equals("RollOver")) {
                isRollOver = true;
            }

            if (nettingType.equals("None") && !checkIfNoneNettingType(currentGroup, nettingType)) {
                return false;
            }

            currentGroup = new TransferArray(SortShell.sort(currentGroup.toVector(), new ComparatorNettedTransfer()));

            TransferArray clonedTransfers;
            try {
                clonedTransfers = (TransferArray) currentGroup.clone();
            } catch (CloneNotSupportedException e) {
                Log.error(this, e);
                return false;
            }


            if (!isNone && !checkTransferValueDateEqSettleDate(clonedTransfers)) {
                return false;
            }

            long groupId = 0L;
            if (!isNone) {
                groupId = this.getGroupId(clonedTransfers, nettingType);
                if (groupId > 0L) {
                    nettingType = nettingType + "/" + groupId;
                }
            }

            String comment = getAttribute(COMMENT);

            if (isRollOver && groupId == 0L) {
                nettingType += "/" + clonedTransfers.get(0).getLongId();
            }


            boolean securityCashMix = false;
            boolean securityFound = false;
            boolean cashFound = false;

            for (BOTransfer t : clonedTransfers) {
                if (t.getTransferType().equals("SECURITY")) {
                    securityFound = true;
                } else {
                    cashFound = true;
                }
            }

            if (cashFound && securityFound) {
                securityCashMix = true;
            }

            Vector<Long> errorTransfers = new Vector<>();


            BOTransfer clonedBot;
            Trade trade = null;
            BOTransfer origBot = null;
            String originKey, destinationKey;
            for (int i = 0; i < clonedTransfers.size(); ++i) {
                clonedBot = clonedTransfers.get(i);
                try {
                    origBot = (BOTransfer) clonedBot.clone();
                } catch (Exception e) {
                    Log.error(this, e);
                }

                if (origBot != null) {
                    if (origBot.getTradeLongId() != 0L) {
                        try {
                            trade = ds.getRemoteTrade().getTrade(origBot.getTradeLongId());
                        } catch (Exception e) {
                            Log.error(this, e);
                        }
                    }

                    if (origBot.getNettedTransfer()) {
                        origBot = currentGroup.get(i);
                        originKey = "";
                        destinationKey = "";
                        HashMap<String, String> keys1 = BOCache.getNettingConfig(ds, origBot.getNettingType());
                        if (!Util.isEmpty(keys1)) {
                            originKey = origBot.buildKey(keys1);
                        }

                        HashMap<String, String> keys2 = BOCache.getNettingConfig(ds, nettingType);
                        if (!Util.isEmpty(keys2)) {
                            String oriNettingType = origBot.getNettingType();
                            origBot.setNettingType(nettingType);
                            destinationKey = origBot.buildKey(keys2);
                            origBot.setNettingType(oriNettingType);
                        }

                        if (!Util.isEmpty(originKey) && !Util.isEmpty(destinationKey) &&
                                originKey.equals(destinationKey) &&
                                (!securityCashMix || origBot.getTransferType().equals("SECURITY"))) {
                            if (origBot.getNettingType().equals(nettingType)) {
                                origBot.setAction(Action.CANCEL);
                            }
                        } else {
                            origBot.setAction(Action.CANCEL);
                        }
                    }

                    if (!BOTransferWorkflow.isTransferActionApplicable(origBot, trade, origBot.getAction(), ds, null)) {
                        errorTransfers.addElement(origBot.getLongId());
                    }
                }


            }

            if (!Util.isEmpty(errorTransfers)) {
                Log.warn(this, "There are transfers that cannot be cancelled, they are eliminated from the process. " + errorTransfers);
            }

            for (int i = 0; i < clonedTransfers.size(); ++i) {
                clonedBot = clonedTransfers.get(i);
                if (!errorTransfers.contains(clonedBot.getLongId())) {
                    origBot = currentGroup.get(i);
                    clonedBot.setParentLongId(clonedBot.getLongId());
                    clonedBot.setEnteredUser(getUser());
                    clonedBot.setLongId(0L);
                    clonedBot.setNettingType(nettingType);
                    clonedBot.setStatus(Status.S_NONE);
                    clonedBot.setAction(Action.NEW);
                    origBot.setAction(Action.ASSIGN);
                    origBot.setEnteredUser(getUser());
                    if (i == clonedTransfers.size() - 1 && Defaults.getBooleanProperty("PAIR_OFF_AUTO_PROCESS", false)) {
                        clonedBot.setAttribute("PairOffAutoProcess", "PairOffAutoProcess");
                    } else {
                        clonedBot.setAttribute("PairOffAutoProcess", null);
                    }

                    if (securityCashMix) {
                        if (!clonedBot.getTransferType().equals("SECURITY")) {
                            clonedBot.setAttribute("SecurityCashMix", "true");
                        }
                    } else if (clonedBot.getNettedTransfer() && clonedBot.getNettingType().equals(origBot.getNettingType())) {
                        clonedBot.setAttribute("SecurityCashMix", "true");
                    }

                    TransferArray transferArray = new TransferArray();
                    transferArray.add(clonedBot);
                    clonedBot.setPairOffFrom(comment);
                    clonedBot.setPairOffTo(null);

                    try {
                        if (!Util.isEmpty(transferArray)) {
                            origBot.setPairOffTo(comment);
                        }

                        Vector<PSEvent> events = ds.getRemoteBO().splitTransfers(origBot, transferArray);
                        if (PSEventHandler.getPSConnection() != null) {
                            PSEventHandler.getPSConnection().publish(events);
                        }
                    } catch (Exception e) {
                        Log.error(this, e);
                        return false;
                    }
                }
            }
        }
        return true;
    }


    /**
     * Get the next transfers group id
     *
     * @param transfers   the current transfers
     * @param nettingType the destination netting type
     * @return the group id
     */
    private long getGroupId(TransferArray transfers, String nettingType) {
        long lastGroup = 0L;
        if (!Util.isEmpty(transfers) && !Util.isEmpty(nettingType)) {
            boolean isSameGroupFound = false;
            int index = nettingType.lastIndexOf(47);
            if (index > 0) {
                nettingType = nettingType.substring(0, index);
            }

            for (int i = 0; i < transfers.size(); ++i) {
                BOTransfer bot = transfers.get(i);
                String initNettingType = bot.getNettingType();
                String boNet;
                index = initNettingType.lastIndexOf(47);
                if (index > 0) {
                    boNet = initNettingType.substring(0, index);
                } else {
                    boNet = initNettingType;
                }

                if (nettingType.equals(boNet)) {
                    isSameGroupFound = true;
                    long botGroup = bot.getNettingGroup();
                    if (botGroup > lastGroup) {
                        lastGroup = botGroup;
                    }
                }
            }

            if (lastGroup > 0L || isSameGroupFound) {
                ++lastGroup;
            }

        }
        return lastGroup;
    }

}
