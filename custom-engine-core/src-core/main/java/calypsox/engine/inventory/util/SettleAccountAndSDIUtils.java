/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.inventory.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * This is a new logic that, in case there are no error, it will create automatically the recieved account name, with
 * the corresponding SDI Instruction to impact the position. This will avoid to insert all the GD accounts and the SDIs
 * associated
 *
 * @author Guillermo Solano
 * @version 1.0
 * @date 11/09/2013
 */
public class SettleAccountAndSDIUtils {

    /**
     * Reference to DSConnection.
     */

    private static DSConnection dsConnection = DSConnection.getDefault();
    /*
     * CONSTANTS
     */
    private static final String DEFAULT = "Default";
    private static final String PROCESSING_ORG = "BSTE";
    private static final String SETTLE = "Settle";
    private static final String ACTUAL = "Actual";
    private static final String INVENTORY = "INVENTORY";
    private static final String SETTLEMENT = "Settlement";
    private static final String ACCOUNT_NUMBER = "AccountNumber";
    private static final String INVENTORY_AGENT = "InventoryAgent";
    private static final String SDF_NAME_SEPARATOR = "@";
    private static final String SDF_GROUP_TYPE = "SDI";
    private static final String KEYWORD = "KEYWORD.";
    private static final String PROCESSING_ORG_ROLE = "ProcessingOrg";

    /**
     * It will create automatically the received account name, with the corresponding SDI Instruction to impact the
     * position
     *
     * @param counterparty
     * @param AccountName
     * @return the success or not of creating all the set
     */
    public static boolean buildInventoryAccountSDIAndSDF(final LegalEntity agent, final String accountName) {

        if ((agent == null) || Util.isEmpty(accountName)) {
            return false;
        }

        try {
            // just to be sure the account does not exist
            final Account checkAcc = BOCache.getAccount(dsConnection, accountName);
            Integer accId = -1;

            // if the account has been inserted, used it
            if (checkAcc == null) {

                accId = createSettlementAccount(agent, accountName);
            } else {

                accId = checkAcc.getId();
            }

            if (accId > 0) { // account created

                // recover account
                final Account account = BOCache.getAccount(dsConnection, accId);

                // check account belongs to the same custodian
                if (agent.getId() != account.getLegalEntityId()) {
                    Log.error(SettleAccountAndSDIUtils.class, "Account: " + account.getName()
                            + " belongs to another custodian. SDI not created.");
                    return false;
                }

                // create the SDI specific SDF
                final StaticDataFilter accountSDFilter = createSettlementSDIStaticDataFilter(account);

                if (accountSDFilter != null) { // SDF for the SDI created

                    Integer SDIId = createSettlementSDI(account, accountSDFilter.getName());

                    if (SDIId > 0) { // created SDI perfect
                        return true;
                    }
                }
            }
        } catch (Exception e) {

            Log.error(SettleAccountAndSDIUtils.class, "Account, SDI and SDF not created");
            return false;
        }
        return false;
    }

    /**
     * Creates a new SDI for the settlement of the SimpleTransfer trades to impact the positions
     *
     * @param counterparty
     * @param accountName
     * @return @return the id of the Account if created, -1 othercase
     * @throws RemoteException
     * @throws CalypsoException
     */
    public static int createSettlementAccount(final LegalEntity counterparty, final String accountName)
            throws RemoteException, CalypsoException {

        final Account account = new Account();

        account.setCreationDate(new JDatetime());
        // set the account name and description
        account.setName(accountName);
        account.setDescription("Account created automatically - online positions.");
        account.setExternalName(accountName);

        final LegalEntity processingOrg = BOCache.getLegalEntity(dsConnection, PROCESSING_ORG);
        if (processingOrg == null) {
            Log.error(SettleAccountAndSDIUtils.class, "Not found PO " + PROCESSING_ORG + ". Account not created");
            return -1;
        } // not used, does not find the account
        account.setProcessingOrgId(processingOrg.getId());
        // processing org ALL
        // account.setProcessingOrgId(0);
        // any currency
        account.setCurrency(Account.ANY);

        // type settle and configuration
        account.setAccountType(Account.SETTLE);
        account.setCallAccountB(false);

        // counterparty and role
        account.setLegalEntityId(counterparty.getId());
        account.setLegalEntityRole(LegalEntity.AGENT);
        // other options

        account.setAccountStatus("");
        account.setBalanceFrequency(Frequency.valueOf(Frequency.DAILY));
        account.setBalanceDateRoll(DateRoll.valueOf(DateRoll.END_MONTH));
        account.setBalanceDay(0);
        account.setAttributes(null);
        account.setUser(dsConnection.getUser());

        // statement configs
        Vector<AccountStatementConfig> statementConfigVector = new Vector<AccountStatementConfig>();
        statementConfigVector.add(createAccountStatementConfig());
        account.setStatementConfigs(statementConfigVector);
        // save new account
        final int accountId = dsConnection.getRemoteAccounting().save(account);

        if (accountId <= 0) {
            Log.error(SettleAccountAndSDIUtils.class, "Could not save Account " + account);
            return -1;
        }
        return accountId;
    }

    /**
     * Creates a new SDI for the settlement of the SimpleTransfer trades to impact the positions
     *
     * @param account
     * @param accountSDFilter
     * @return the id of the SDI if created, -1 othercase
     */
    public static int createSettlementSDI(final Account account, final String accountSDFilter) {

        if ((account == null) || (account.getName() == null) || account.getName().isEmpty()) {
            return -2;
        }

        try {
            final SettleDeliveryInstruction findSdi = findSDIForAgent(account, accountSDFilter);

            // if is on the system already, just return the id
            if (findSdi != null) {
                return findSdi.getId();
            }

        } catch (RemoteException e1) {
            Log.error(SettleAccountAndSDIUtils.class, "Could not access DB to search SDI ");
            return -1;
        } catch (CalypsoException e2) {
            Log.warn(SettleAccountAndSDIUtils.class, e2.getLocalizedMessage());
            return -3;
        }
        // Build a new SDI for the liquidation instruction for positions online
        final SettleDeliveryInstruction sdi = new SettleDeliveryInstruction();

        // role ProcessingOrg
        sdi.setRole(account.getLegalEntityRole());
        // BSTE beneficiary
        final LegalEntity processingOrg = BOCache.getLegalEntity(dsConnection, PROCESSING_ORG);
        if (processingOrg == null) {
            Log.error(SettleAccountAndSDIUtils.class, "Not found PO " + PROCESSING_ORG + ". Account not created");
            return -1;
        }
        sdi.setBeneficiaryId(processingOrg.getId());
        // ccy = ANY (same as empty)
        // Pay/Rec = BOTH
        sdi.setPayReceive(SettleDeliveryInstruction.BOTH);
        // Method = INVENTORY
        sdi.setMethod(INVENTORY);
        // Cash/Security = BOTH
        sdi.setType(SettleDeliveryInstruction.BOTH);
        // Contact = Settlement
        sdi.setBeneficiaryContactType(SETTLEMENT);
        // Processing Org = ALL
        sdi.setProcessingOrg(null);
        sdi.setRole(PROCESSING_ORG_ROLE);
        // set products: SimpleTransfer
        Vector<String> v = new Vector<String>();
        v.add("SimpleTransfer");
        sdi.setProductList(v);
        // Agent configuration
        sdi.setAgentId(account.getLegalEntityId());
        // Contact type = Settlement
        sdi.setAgentContactType(SETTLEMENT);
        // agent a/c
        final LegalEntity agent = BOCache.getLegalEntity(dsConnection, account.getLegalEntityId());
        if (agent == null) {
            return -1;
        }
        sdi.setAgentName(agent.getName());
        sdi.setComments(account.getName() + SDF_NAME_SEPARATOR + agent.getAuthName());

        // account name, G/L account and description
        sdi.setGeneralLedgerAccount(account.getId());
        if (Util.isEmpty(account.getName())) {
            sdi.setDescription(String.valueOf(account.getId()));
            sdi.setAgentAccount(String.valueOf(account.getId()));
        } else {
            sdi.setDescription(account.getName());
            sdi.setAgentAccount(account.getName());
        }
        if (!Util.isEmpty(accountSDFilter)) {
            sdi.setStaticFilterSet(accountSDFilter);
        }
        // more options
        sdi.setPriority(0);
        sdi.setPreferredB(true);

        int sdiId = -1;
        try {
            // try to save it
            sdiId = dsConnection.getRemoteReferenceData().save(sdi);

        } catch (RemoteException e) {
            Log.error(SettleAccountAndSDIUtils.class, "Could not save SDI " + sdi);
            return -1;
        }

        return sdiId <= 0 ? -1 : sdiId;
    }

    /**
     * Recovers the SDI for the agent of the account with that SDFname (if this attribute is not empty and not null)
     *
     * @param account
     * @param accountSDFilter
     * @return
     * @throws RemoteException
     * @throws CalypsoException
     */
    private static SettleDeliveryInstruction findSDIForAgent(final Account account, String accountSDFilter)
            throws RemoteException, CalypsoException {

        final StringBuffer whereSQL = new StringBuffer();

        whereSQL.append("AGENT_LE = ").append(account.getLegalEntityId()).append(" ");
        whereSQL.append("AND AGENT_ACCOUNT = ").append(Util.string2SQLString((account.getName()))).append(" ");
        if ((accountSDFilter != null) && !accountSDFilter.isEmpty()) {
            whereSQL.append("AND SD_FILTER =").append(Util.string2SQLString(accountSDFilter));
        }
        // SELECT * FROM LE_SETTLE_DELIVERY
        // WHERE AGENT_LE = 86268
        // AND AGENT_ACCOUNT = '000000000000000000000000000000139'
        // AND SD_FILTER ='000000000000000000000000139@MGBE'

        @SuppressWarnings("unchecked")
        Vector<SettleDeliveryInstruction> vSdis = dsConnection.getRemoteReferenceData().getSettleDeliveryInstructions(
                "", whereSQL.toString(), null);

        if ((vSdis == null) || vSdis.isEmpty()) {
            return null;
        }

        if (vSdis.size() > 1) {
            throw new CalypsoException("More than one SDI found for the account " + account.getName() + " and agent "
                    + account.getLegalEntityId());
        }

        return vSdis.get(0);
    }

    public static SettleDeliveryInstruction findSDIForAccount(final String accountName, final String leName)
            throws RemoteException, CalypsoException {

        final LegalEntity le = BOCache.getLegalEntity(dsConnection, leName.trim());
        if (le == null) {
            return null;
        }

        final StringBuffer whereSQL = new StringBuffer();
        whereSQL.append("AGENT_LE = ").append(le.getId()).append(" ");
        whereSQL.append("AND AGENT_ACCOUNT = ").append(Util.string2SQLString(accountName)).append(" ");

        @SuppressWarnings("unchecked")
        Vector<SettleDeliveryInstruction> vSdis = dsConnection.getRemoteReferenceData().getSettleDeliveryInstructions(
                "", whereSQL.toString(), null);

        if ((vSdis == null) || vSdis.isEmpty()) {
            return null;
        }

        if (vSdis.size() > 1) {
            // throw new CalypsoException("More than one SDI found for the account Name" + accountName +
            // " and agent Name"
            // + leName);
        }

        return vSdis.get(0);
    }

    /**
     * @param account
     * @return Creates a SDF for the SDI for the new account
     */
    // Attribute Criteria Filter Value(s)
    public static StaticDataFilter createSettlementSDIStaticDataFilter(final Account account) {

        final String proposeName = account.getName().trim() + SDF_NAME_SEPARATOR
                + BOCache.getLegalEntity(dsConnection, account.getLegalEntityId()).getAuthName().trim();
        final String sdfName = getFilterNameToSave(proposeName);

        StaticDataFilter sdf = null;

        try {
            // check if already exists
            sdf = dsConnection.getRemoteReferenceData().getStaticDataFilter(sdfName);
            if (sdf != null) {
                return sdf;
            }
            // none, create it
            sdf = new StaticDataFilter(sdfName);

        } catch (RemoteException e) {
            Log.error(SettleAccountAndSDIUtils.class, "Could not save SFD " + sdfName);
            return null;
        }

        // put attributes
        if (sdf != null) {
            // put the group SDI
            Set<String> group = new HashSet<String>();
            group.add(SDF_GROUP_TYPE);
            sdf.setGroups(group);
            // comment
            sdf.setComment("SDF created automatically for the SDIs to impact positions");

            // add configuration KEYWORD.accountNumber
            StaticDataFilterElement accountNumberCondition = new StaticDataFilterElement(KEYWORD + ACCOUNT_NUMBER);
            accountNumberCondition.setType(StaticDataFilterElement.LIKE);
            accountNumberCondition.setLikeValue(getLikeAccountNumber(account));
            // add configuration KEYWORD.InventoryAgent
            StaticDataFilterElement invAgentCondition = new StaticDataFilterElement(KEYWORD + INVENTORY_AGENT);
            invAgentCondition.setType(StaticDataFilterElement.IN);
            invAgentCondition.setValues(new Vector<String>(Arrays.asList(getInventoryAgent(account))));

            final Vector<StaticDataFilterElement> elementsFilterSdf = new Vector<StaticDataFilterElement>(2);
            elementsFilterSdf.add(invAgentCondition);
            elementsFilterSdf.add(accountNumberCondition);

            // add new sdf elements to the sdf
            sdf.setElements(elementsFilterSdf);
        } // end sdf
        boolean success = false;
        try {
            // save the sdf
            success = dsConnection.getRemoteReferenceData().save(sdf);

        } catch (RemoteException e) {
            Log.error(SettleAccountAndSDIUtils.class, "Could not save SFD " + sdfName);
            return null;
        }

        return success ? sdf : null;
    }

    /**
     * @param account
     * @return the agent short name
     */
    private static String getInventoryAgent(Account account) {

        final LegalEntity agent = BOCache.getLegalEntity(dsConnection, account.getLegalEntityId());
        if (agent != null) {
            return agent.getAuthName();
        }
        return "";
    }

    /**
     * @param account
     * @return the filter value in the form %ACCOUNT_NAME%
     */
    private static String getLikeAccountNumber(Account account) {

        final StringBuffer sb = new StringBuffer();
        sb.append("%").append(account.getName()).append("%");

        return sb.toString();
    }

    /**
     * @param proposed filter name
     * @return filterName reduced to max length name of 32 chars
     */
    private static String getFilterNameToSave(String filterName) {
        if (Util.isEmpty(filterName)) {
            return filterName;
        }

        if (filterName.length() > 32) {
            return filterName.substring(filterName.length() - 32, filterName.length());

        } else {
            return filterName;
        }
    }

    /**
     * Builds generic Account statements for the SETTLEMENT Account
     *
     * @return Generic AccountStatementConfig for an Account
     * @throws RemoteException
     * @throws CalypsoException
     */
    private static AccountStatementConfig createAccountStatementConfig() throws RemoteException, CalypsoException {

        final AccountStatementConfig accStatmentConfig = new AccountStatementConfig();

        // ticks to true
        accStatmentConfig.setNoMovementB(true);
        accStatmentConfig.setIsPaymentB(true);
        accStatmentConfig.setZeroBalanceB(true);
        // configuration
        accStatmentConfig.setPositionCashSecurityFlag(AccountStatementConfig.CASH);
        accStatmentConfig.setPositionClass(InventorySecurityPosition.CLIENT_CLASS);
        accStatmentConfig.setType(DEFAULT);
        accStatmentConfig.setPositionDateType(SETTLE);
        accStatmentConfig.setPositionType(ACTUAL);
        accStatmentConfig.setPositionValue(InventorySecurityPosition.getCompositeTypePositionValue(ACTUAL));
        accStatmentConfig.setNumbering("");

        return accStatmentConfig;
    }

    // TESTS
    // @SuppressWarnings({ "unused" })
    // public static void main(String[] pepe) throws RemoteException, CalypsoException {
    //
    // final String args[] = { "-env", "dev5-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
    // DSConnection ds = null;
    // try {
    // ds = ConnectionUtil.connect(args, "MainEntry");
    // dsConnection = ds;
    // } catch (ConnectException e) {
    // e.printStackTrace();
    // }
    //
    // final String[] accounts = { "1@DUMMY_2" };
    // final LegalEntity le = BOCache.getLegalEntity(dsConnection, "BDSD");
    //
    // Account account = BOCache.getAccount(ds, accounts[0], 0, "ANY");
    //
    // final LegalEntity le2 = BOCache.getLegalEntity(dsConnection, "BSTE");
    // account = BOCache.getAccount(ds, accounts[0], 0, "ANY");
    //
    // System.out.println();
    //
    // }
    //
    // // final String[] accounts = { "1118149@CITM", "1190399@CITM", "91100", "1@DUMMY",
    // // "00000000000000000000000000000091100" };
    //
    // final LegalEntity le = BOCache.getLegalEntity(dsConnection, "MGBE");
    // final LegalEntity po = BOCache.getLegalEntity(dsConnection, "BSTE");
    //
    // for (String accountName : accounts) {
    //
    // // BOCache
    // Account account = BOCache.getAccount(ds, accountName);
    //
    // // account = BOCache.getAccount(ds, accountName, le.getEntityId(), "ANY");
    //
    // account = BOCache.getAccount(ds, accountName, po.getEntityId(), "ANY"); // ok
    //
    // // account = BOCache.getAccount(ds, accountName, 0, "ANY");
    //
    // // DS
    // // account = ds.getRemoteAccounting().getAccount(accountName);
    //
    // account = ds.getRemoteAccounting().getAccount(accountName, po.getId(), "ANY"); // ok
    //
    // // SDI directly from accName and LE name
    // // SettleDeliveryInstruction accountSdi = SettleAccountAndSDIUtils.findSDIForAccount(accountName, "MGBE");
    //
    // System.out.println();
    // }
    //
    // // final String accountName = "000000000000000000000000000000139";
    // // final String accountNameNew = "000000000000000000000000000000140";
    // // final String accountNameBANESTO = "000000000000000000000000000000141";
    // // final LegalEntity le = BOCache.getLegalEntity(ds, "MGBE");
    // // final LegalEntity le2 = BOCache.getLegalEntity(ds, "BANESTO");
    // // Account account = BOCache.getAccount(ds, accountName);
    //
    // // not working, must return using LE ID! To be finished
    // // test 1
    // // boolean test = buildInventoryAccountSDIAndSDF(le, accountName);
    // //
    // // // test 2
    // // boolean test2 = buildInventoryAccountSDIAndSDF(le, accountNameNew);
    // //
    // // // test 3
    // // boolean test3 = buildInventoryAccountSDIAndSDF(le2, accountNameNew);
    // //
    // // // test 4
    // // boolean test4 = buildInventoryAccountSDIAndSDF(le2, accountNameBANESTO);
    //
    // System.out.println();
    // System.exit(0);
    // // pruebas
    // }
} // END CLASS
