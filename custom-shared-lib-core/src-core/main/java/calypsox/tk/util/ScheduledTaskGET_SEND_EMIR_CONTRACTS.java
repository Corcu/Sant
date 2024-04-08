/**
 *
 */
package calypsox.tk.util;

import calypsox.util.SantReportingUtil;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.PreviousPositionDTO;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;

import java.rmi.RemoteException;
import java.util.*;

/**
 * Get process date valid contracts to apply action SEND_EMIR
 *
 * @author N2 - v4.3
 * GSM - added option to update additional field EMIR_COLLATERAL_VALUE by activating attribute.
 *  This must be run at least one in PRO with this option activate it.
 *
 */
public class ScheduledTaskGET_SEND_EMIR_CONTRACTS extends ScheduledTask {

    private static final long serialVersionUID = 123L;

    public static List<String> UPDATE_VALID_STATUSES = Arrays.asList("PRICED_NO_CALL", "PRICED_PAY", "PRICED_RECEIVE");

    public static final String SEND_EMIR = "SEND_EMIR";
    public static final String INITIATION_SEND_EMIR = "INITIATION_SEND_EMIR";
    public static final String FREQUENCY_SEND_EMIR = "FREQUENCY_SEND_EMIR";
    public static final String EMIR_DIRECTION = "EMIR_DIRECTION";
    public static final String YES = "YES";

    /*EMIR RTS 9 - calculates and saves CollateralConfigs which EMIR additional field (OneWay, partially, Fully).
     * Needed once in PRO. Just put attribute CALCULATE_EMIR_AF once in PRO to calculate it
     */
    private static final String CALCULATE_EMIR_AF = "Calculate EMIR Add.Field:";
    private static final String IM_GLOBAL_ID = "IM_GLOBAL_ID";
    private static final String IM_SUB_CONTRACTS = "IM_SUB_CONTRACTS";
    private static final String CSA_CONTRACT_TYPE = "CSA";
    private static final String CSD_CONTRACT_TYPE = "CSD";
    private static final String ONE_WAY = "OneWay";
    private static final String FULLY = "Fully";
    private static final String PARTIALLY = "Partially";
    private static final String EMIR_COLLATERAL_VALUE = "EMIR_COLLATERAL_VALUE";
    private static final String CSA_FACADE = "CSA_FACADE";

    // build frequency map (emir_freq_att / Frequency type)
    private final HashMap<String, Frequency> frequencyMap = buildFrequencyMap();

    @Override
    public String getTaskInformation() {
        return "Get process date valid contracts to apply action SEND_EMIR";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {

        List<AttributeDefinition> attributes = new ArrayList<AttributeDefinition>();
        attributes.add(attribute(CALCULATE_EMIR_AF).booleanType());

        return attributes;
    }

    @Override
    /**
     * Main process
     */
    public boolean process(DSConnection ds, PSConnection ps) {

        final Collection<CollateralConfig> openContracts = loadOpenContracts();

        // get contracts to send
        HashMap<CollateralConfig, MarginCallEntryDTO> contracts = getContractsToSend(openContracts);

        // mark entries to send
        markEntries(contracts.values());

        // publish contracts on task station
        publishContractsOnTaskStation(contracts.keySet());

        //new for EMIR RTS 9 - if activated, will update EMIR Additional Field (run at least once in PRO)
        if (!getAttribute(CALCULATE_EMIR_AF).isEmpty() && getAttribute(CALCULATE_EMIR_AF).equals("true")) {
            updateEMIR_COLLATERAL_VALUE(openContracts);
        }

        return true;
    }


    /**
     * Build a map with all emir frequency types and their Frequency types in Java
     *
     * @param dateRuleNames
     * @return
     */
    public HashMap<String, Frequency> buildFrequencyMap() {

        HashMap<String, Frequency> frequencyMap = new HashMap<String, Frequency>();

        Vector<String> freqTypes = LocalCache.getDomainValues(getDSConnection(),
                "mccAdditionalField.FREQUENCY_SEND_EMIR");

        for (String freqType : freqTypes) {
            if (freqType.equals("Daily")) {
                frequencyMap.put("Daily", Frequency.F_DAILY);
            } else if (freqType.equals("Weekly")) {
                frequencyMap.put("Weekly", Frequency.F_WEEKLY);
            } else if (freqType.equals("Fortnightly")) {
                frequencyMap.put("Fortnightly", Frequency.F_BIWEEKLY);
            } else if (freqType.equals("Monthly")) {
                frequencyMap.put("Monthly", Frequency.F_MONTHLY);
            } else if (freqType.equals("Quarterly")) {
                frequencyMap.put("Quarterly", Frequency.F_QUARTERLY);
            } else if (freqType.equals("Annual")) {
                frequencyMap.put("Annual", Frequency.F_ANNUAL);
            }
        }

        return frequencyMap;

    }

    /**
     * Load all contracts in the system with status OPEN
     *
     * @return
     */
    private Collection<CollateralConfig> loadOpenContracts() {

        ArrayList<Integer> contractsIds = new ArrayList<Integer>();
        Map<Integer, CollateralConfig> contractsMap = new HashMap<Integer, CollateralConfig>();
        String query = "select mrg_call_def from mrgcall_config where agreement_status = 'OPEN'";

        try {
            // get contract ids
            contractsIds = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getMarginCallConfigIds(
                    query);
            // get contracts
            contractsMap = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                    .getMarginCallConfigByIds(contractsIds);
        } catch (RemoteException e) {
            Log.error("Cannot get contract ids from DB", e);
        } catch (PersistenceException e) {
            Log.error("Cannot get contracts from DB", e);
        }
        return contractsMap.values();

    }

    /**
     * Get contracts to send and its process date margin call entry
     *
     * @param {contracts,mcentries}
     * @return
     */
    public HashMap<CollateralConfig, MarginCallEntryDTO> getContractsToSend(Collection<CollateralConfig> contracts) {

        HashMap<CollateralConfig, MarginCallEntryDTO> contractsToSend = new HashMap<CollateralConfig, MarginCallEntryDTO>();

        for (CollateralConfig contract : contracts) {
            // get contract
            if (isContractToSend(contract)) {
                final List<Integer> mccID = new ArrayList<Integer>();
                mccID.add(contract.getId());
                // get entry
                try {
                    final List<MarginCallEntryDTO> entries = CollateralManagerUtil.loadMarginCallEntriesDTO(mccID, new JDatetime(getValuationDatetime(false)).getJDate(TimeZone.getDefault()));
                    if ((entries != null) && (entries.size() > 0)) {
                        // save both
                        contractsToSend.put(contract, entries.get(0));
                    }
                } catch (RemoteException e) {
                    Log.error(this, "Cannot get marginCallEntry for the contract = " + contract.getId(), e);
                }
            }
        }

        return contractsToSend;

    }

    /**
     * Mark marginCallEntries to send
     *
     * @param contracts
     * @return
     */
    public void markEntries(Collection<MarginCallEntryDTO> entries) {

        for (MarginCallEntryDTO entry : entries) {
            if ((entry.getAttribute("IS_SEND_EMIR") == null) || "".equals(entry.getAttribute("IS_SEND_EMIR"))) {
                // check if status is valid for update
                if (UPDATE_VALID_STATUSES.contains(entry.getStatus())) {
                    // mark attribue and save
                    markEntry(entry, "UPDATE");
                }
            }
        }

    }

    /**
     * save the entry with optimize sending attributes
     *
     * @param mcEntryDTO
     */
    private void markEntry(MarginCallEntryDTO entry, String actionToApply) {

        if (entry != null) {

            markSendEmirAttribute(entry);

            int entryId = 0;
            try {
                entryId = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer()
                        .save(entry, actionToApply, TimeZone.getDefault());
                Log.info(
                        this,
                        "Entry with id " + entryId + " successfully saved for the contract "
                                + entry.getCollateralConfigId());

            } catch (RemoteException e) {
                Log.error(this, e);
                MarginCallEntryDTO reloadedEntry = null;
                // TODO limit the second save just to the mismatch version
                // error
                try {
                    reloadedEntry = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer()
                            .loadEntry(entry.getId());

                    // check if status has not changed
                    if (UPDATE_VALID_STATUSES.contains(reloadedEntry.getStatus())) {
                        // add optimizer send status
                        markSendEmirAttribute(reloadedEntry);

                        entryId = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer()
                                .save(reloadedEntry, actionToApply, TimeZone.getDefault());
                        Log.info(this, "Entry with id " + entryId + " successfully saved for the contract "
                                + reloadedEntry.getCollateralConfigId());
                    }
                } catch (RemoteException re) {
                    Log.error(this, re);
                }
            }
        }
    }

    /**
     * Add optimize sending attributes
     *
     * @param mcEntryDTO
     */
    private void markSendEmirAttribute(MarginCallEntryDTO entry) {

        if (entry != null) {
            entry.addAttribute("IS_SEND_EMIR", "YES");

            // TODO: delete with upgrade 1.6.3
            if (Util.isEmpty(entry.getCashPositions())) {
                entry.setCashPosition(new PreviousPositionDTO<CashPositionDTO>());
            }
        }

    }

    /**
     * Check if a contract is valid to be sent
     *
     * @param contract
     * @return
     */
    public boolean isContractToSend(CollateralConfig contract) {

        // get emir attributes
        String sendEmir = contract.getAdditionalField(SEND_EMIR);
        String sendEmirFreq = contract.getAdditionalField(FREQUENCY_SEND_EMIR);
        String sendEmirInitiation = contract.getAdditionalField(INITIATION_SEND_EMIR);

        // check sendEmir
        if (Util.isEmpty(sendEmir) || sendEmir.equals("NO")) {
            return false;
        }

        // check sendEmirInitiation
        if (Util.isEmpty(sendEmirInitiation) || (JDate.valueOf(sendEmirInitiation) == null)) {
            return false;
        }

        // check sendEmirFreq
        if (Util.isEmpty(sendEmirFreq)) {
            return false;
        }

        // check if contract has to be sent
        JDate emirIniDate = JDate.valueOf(sendEmirInitiation);
        JDate valueDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        Frequency frequency = this.frequencyMap.get(sendEmirFreq);

        if (isDateToSend(frequency, emirIniDate, valueDate)) {
            return true;
        }

        return false;

    }

    /**
     * Check if a date is valid to send the contract
     *
     * @param dateRule
     * @param emirIniDate
     * @param actualDate
     * @return
     */
    public boolean isDateToSend(Frequency freq, JDate emirIniDate, JDate actualDate) {

        // we have to check:
        // 1 - today is a valid date
        // 2 - yesterday was a valid date (to cover cases of batch holidays)

        boolean isTodayDateToSend = checkDay(freq, emirIniDate, actualDate);
        boolean isYesterdayDateToSend = checkYesterday(freq, emirIniDate, actualDate);

        return isTodayDateToSend || isYesterdayDateToSend;

    }

    /**
     * Check if actualDate is a valid date to send
     *
     * @param freq
     * @param emirIniDate
     * @param actualDate
     * @return
     */
    public boolean checkDay(Frequency freq, JDate emirIniDate, JDate actualDate) {

        JDate nextValidDate = emirIniDate.addFrequency(freq);
        while (JDate.diff(nextValidDate, actualDate) > 0) {
            nextValidDate = nextValidDate.addFrequency(freq);
        }
        if (JDate.diff(nextValidDate, actualDate) == 0) {
            return true;
        }
        return false;

    }

    /**
     * Get yesterday date and check if is a valid date to send
     *
     * @param freq
     * @param emirIniDate
     * @param actualDate
     * @return
     */
    public boolean checkYesterday(Frequency freq, JDate emirIniDate, JDate actualDate) {

        // get yesterday
        JDate yesterday = null;
        if (actualDate.getDayOfWeekAsString().equals("MON")) {
            yesterday = actualDate.addDays(-3);
        } else {
            yesterday = actualDate.addDays(-1);
        }

        // check if yesterday was batch holiday
        List<String> messages = new ArrayList<String>();
        if (!isHoliday(yesterday, messages)) {
            return false;
        }

        // check if yesterday was valid date to send
        return checkDay(freq, emirIniDate, yesterday);

    }

    // --- TASK STATION STUFF --- //

    /**
     * Publish contracts info on a task station window tab
     *
     * @param contracts
     */
    public void publishContractsOnTaskStation(Collection<CollateralConfig> contracts) {

        for (CollateralConfig contract : contracts) {
            Task task = buildContractTask(contract);
            try {
                DSConnection.getDefault().getRemoteBO().save(task);
            } catch (RemoteException e) {
                Log.error("Error saving task for contract = " + contract.getId() + "\n", e);
            }
        }

    }

    /**
     * Build a contract info task
     *
     * @param contract
     * @return
     */
    private Task buildContractTask(CollateralConfig contract) {

        Task task = new Task();
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setEventType("EX_SEND_EMIR");
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setDatetime(getValuationDatetime());
        // contract id
        task.setObjectLongId(contract.getId());
        // contract name
        task.setObjectClassName(contract.getName());
        // owner full name
        task.setOwner(contract.getProcessingOrg().getName());
        // owner short name
        task.setUserComment(contract.getProcessingOrg().getCode());
        // cpty full name
        task.setSource(contract.getLegalEntity().getName());
        // cpty short name
        task.setComment(contract.getLegalEntity().getCode());
        // emir direction
        task.setAttribute(contract.getAdditionalField(EMIR_DIRECTION));

        return task;

    }

    //EMIR RTS 9
    /*
     * Run at least once in PRO to update all contracts
     */
    private void updateEMIR_COLLATERAL_VALUE(final Collection<CollateralConfig> CollateralConfigCollection) {

        for (CollateralConfig contract : CollateralConfigCollection) {

            if (checkContractTypeIsCSAorCSD(contract)) {

                CollateralConfig update = updateEMIRCollateralValue(contract);
                if (update != null) {
                    try {
                        update = (CollateralConfig) DSConnection.getDefault().getRemoteReferenceData()
                                .applyNoSave(update);
                        if (update.isValid(new ArrayList<String>())) {
                            ServiceRegistry.getDefault().getCollateralDataServer().save(update);
                        }

                    } catch (CalypsoServiceException e) {
                        Log.error(this, "Couldn't apply action Update on old Parent Contract: " + e.getMessage());
                        Log.error(this, e); //sonar
                    } catch (CollateralServiceException e) {
                        Log.error(this, "Couldn't save old Parent Contract: " + e.getMessage());
                        Log.error(this, e); //sonar
                    }
                }
            }
        }

    }

    /*
     * EMIR RTS 9 - updates the additional field EMIR_COLLATERAL_VALUE of the contract, with values oneWay, Partially or Fully, depending
     * on the IM facade contract relation.
     */
    private CollateralConfig updateEMIRCollateralValue(CollateralConfig contract) {

        if (contract == null) return null;

        CollateralConfig facade = getFacade(contract);
        if (facade != null) {
            CollateralConfig csd = getCSDfromFacade(facade);
            if (csd == null) {
                contract.setAdditionalField(EMIR_COLLATERAL_VALUE, PARTIALLY);
            } else {
                String direction = csd.getContractDirection();
                if (!Util.isEmpty(direction)) {
                    if (direction.equals(CollateralConfig.NET_BILATERAL)) {
                        contract.setAdditionalField(EMIR_COLLATERAL_VALUE, FULLY);
                    } else if (direction.equals(CollateralConfig.NET_UNILATERAL)) {
                        contract.setAdditionalField(EMIR_COLLATERAL_VALUE, ONE_WAY);
                    } else {
                        contract.setAdditionalField(EMIR_COLLATERAL_VALUE, PARTIALLY);
                    }
                }
            }
        } else {
            contract.setAdditionalField(EMIR_COLLATERAL_VALUE, PARTIALLY);
        }
        return contract;
    }

    /**
     *
     * @param colConfig
     * @return FACADE contract from additional field IM_GLOBAL_ID
     */
    private static CollateralConfig getFacade(CollateralConfig colConfig) {
        String globalIdString = colConfig.getAdditionalField(IM_GLOBAL_ID);
        CollateralConfig facade = null;
        if (!Util.isEmpty(globalIdString)) {
            int globalId = Integer.parseInt(globalIdString);
            try {
                facade = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(globalId);
            } catch (CollateralServiceException e) {
                Log.error(ScheduledTaskGET_SEND_EMIR_CONTRACTS.class,
                        "Could not get FACADE contract with ID: " + globalIdString + "\n" + e);
            }
        }
        return facade;
    }

    /**
     * @param FACADE contract
     * @return first CSD contract assign to the facade one
     */
    private static CollateralConfig getCSDfromFacade(CollateralConfig facade) {

        if (CSA_FACADE.equals(facade.getContractType())) {
            if (!Util.isEmpty(facade.getAdditionalField(IM_SUB_CONTRACTS))) {
                String[] ids = facade.getAdditionalField(IM_SUB_CONTRACTS).split(",");
                List<Integer> contractIds = new ArrayList<Integer>();
                for (String id : ids) {
                    contractIds.add(Integer.valueOf(id));
                }

                MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
                mcFilter.setContractIds(contractIds);
                List<CollateralConfig> listCC = loadCollConfigFromFilter(mcFilter);

                for (CollateralConfig cc : listCC) {
                    if (CSD_CONTRACT_TYPE.equals(cc.getContractType())) {
                        return cc;
                    }
                }
            }
        }
        return null;
    }

    /**
     *
     * @param mcFilter
     * @return List of Collateral configs with mcFilter
     */
    private static List<CollateralConfig> loadCollConfigFromFilter(MarginCallConfigFilter mcFilter) {
        List<CollateralConfig> listCC = new ArrayList<>();
        try {
            listCC = CollateralManagerUtil.loadCollateralConfigs(mcFilter);
        } catch (CollateralServiceException e) {
            Log.error(ScheduledTaskGET_SEND_EMIR_CONTRACTS.class, e); //sonar
        }
        return listCC;
    }


    /**
     *
     * @param contract
     * @return true if contract type is CSA or CSD, false otherwise
     */
    private static boolean checkContractTypeIsCSAorCSD(CollateralConfig cc) {
        String contractType = cc.getContractType();
        return CSA_CONTRACT_TYPE.equals(contractType) || CSD_CONTRACT_TYPE.equals(contractType);
    }

}
