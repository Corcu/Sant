/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo.workflow.rule;

import calypsox.tk.collateral.util.SantMarginCallUtil;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.ELBEandKGRutilities;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.*;

/**
 * @author xIS15793
 */
public class SantCheckGlobalMTACollateralRule extends BaseCollateralWorkflowRule {

    private static final String IM_SUB_CONTRACTS_AF = "IM_SUB_CONTRACTS";
    private static final String ACTION = "BLOCKED";
    private static final String PRICED_STATES = "PRICED_";

    @Override
    public String getDescription() {
        return "Check if every child is PRICED_* and if Margin Required > Global MTA";
    }

    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
                                   DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
                                   List<PSEvent> paramList2) {
        Log.info(SantCheckGlobalMTACollateralRule.class, "SantCheckGlobalMTACollateralRule Check - Start");
        boolean result = true;

        CollateralConfig contract = entry.getCollateralConfig();

//		boolean poMtaDefined = (CollateralConfig.GLOBAL_RATING.equalsIgnoreCase(contract.getPoMTAType())
//				|| (CollateralConfig.AMOUNT.equalsIgnoreCase(contract.getPoMTAType())
//						&& contract.getPoMTAAmount() != 0.0));
//
//		boolean cptyMtaDefined = (CollateralConfig.GLOBAL_RATING.equalsIgnoreCase(contract.getLeMTAType())
//				|| (CollateralConfig.AMOUNT.equalsIgnoreCase(contract.getLeMTAType())
//						&& contract.getLeMTAAmount() != 0.0));

        // we need to check if contract is Subtype = Facade and it has MTA
        // defined.
        if ((CollateralConfig.SUBTYPE_FACADE.equalsIgnoreCase(contract.getSubtype()))
            /* & (poMtaDefined || cptyMtaDefined)*/) {
            // and if all the children are in PRICING_* status
            Vector<Integer> childrenIds = getChildrenIds(contract);

            if (!Util.isEmpty(childrenIds)) {
                // get entries and check its status
                List<MarginCallEntryDTO> childrenEntries = null;
                try {
                    childrenEntries = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer()
                            .loadEntries(childrenIds, entry.getProcessDate(),
                                    Integer.valueOf(entry.getCollateralContext().getId()));

                    // if num of entries != num of ids means there is a child in
                    // NONE status. No need to check anything
                    if (childrenEntries.size() == childrenIds.size()) {
                        for (MarginCallEntryDTO childEntry : childrenEntries) {
                            String childStatus = childEntry.getStatus();
                            if (!Util.isEmpty(childStatus) && !childStatus.contains(PRICED_STATES)) {
                                result = false;
                                break;
                            }
                        }
                    } else {
                        Log.info(SantCheckGlobalMTACollateralRule.class,
                                "SantCheckGlobalMTACollateralRule Num of Entries != num of children");
                        result = false;
                    }

                } catch (RemoteException e) {
                    Log.error(this, "Couldn't get today Entries for contracts " + childrenIds.toString());
                    Log.error(this, e); //sonar
                }
            }
        }

        Log.info(SantCheckGlobalMTACollateralRule.class,
                "SantCheckGlobalMTACollateralRule Check - End with result: " + result);
        return result;

    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
        Log.info(SantCheckGlobalMTACollateralRule.class, "SantCheckGlobalMTACollateralRule Update - Start");
        WorkflowResult wfr = new WorkflowResult();

        // entry will be always the Facade contract, filtered on isApplicable
        // method
        CollateralConfig contract = entry.getCollateralConfig();

        double poMta = getMta(contract, entry, "PO");
        double cptyMta = getMta(contract, entry, "CPTY");

        // double poMta = contract.getPoMTAAmount();
        // double cptyMta = contract.getLeMTAAmount();

        Vector<Integer> childrenIds = getChildrenIds(contract);

        if (!Util.isEmpty(childrenIds)) {
            Vector<CollateralConfig> childrenContracts = getContracts(childrenIds);

            // get entries and check its status
            List<MarginCallEntryDTO> childrenEntries = null;
            try {
                Log.info(SantCheckGlobalMTACollateralRule.class,
                        "SantCheckGlobalMTACollateralRule Update - Getting entries [today] for contracts: "
                                + childrenIds.toString());

                childrenEntries = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer()
                        .loadEntries(childrenIds, entry.getProcessDate(),
                                Integer.valueOf(entry.getCollateralContext().getId()));

                Log.info(SantCheckGlobalMTACollateralRule.class,
                        "SantCheckGlobalMTACollateralRule Update - Num of entries [today] loaded: "
                                + childrenEntries.size());

                // get negative entries and check it with PO MTA
                if ((poMta != 0.0) && !Util.isEmpty(childrenEntries)) {
                    Map<MarginCallEntry, Double> entriesRelated = getEntriesAndAmountRelated(childrenContracts,
                            childrenEntries, true);
                    Log.info(SantCheckGlobalMTACollateralRule.class,
                            "SantCheckGlobalMTACollateralRule Update - Num of entries [today] related to PO: "
                                    + entriesRelated.size());

                    if (!Util.isEmpty(entriesRelated)) {
                        double totalMargin = getTotalMargin(entriesRelated, contract.getCurrency());

                        if (poMta > totalMargin) {
                            applyAction(entriesRelated);
                        }
                    }
                }

                // get positive entries and check it with Cpty MTA
                if ((cptyMta != 0.0) && !Util.isEmpty(childrenEntries)) {
                    Map<MarginCallEntry, Double> entriesRelated = getEntriesAndAmountRelated(childrenContracts,
                            childrenEntries, false);
                    Log.info(SantCheckGlobalMTACollateralRule.class,
                            "SantCheckGlobalMTACollateralRule Update - Num of entries [today] related to Cpty: "
                                    + entriesRelated.size());

                    if (!Util.isEmpty(entriesRelated)) {
                        double totalMargin = getTotalMargin(entriesRelated, contract.getCurrency());

                        if (cptyMta > totalMargin) {
                            applyAction(entriesRelated);
                        }
                    }
                }

            } catch (RemoteException e) {
                StringBuilder msg = new StringBuilder("SantCheckGlobalMTACollateralRule Update - ");
                msg.append("Couldn't get today Entries for contracts ");
                msg.append(childrenIds.toString());
                msg.append(". Error: ").append(e.getMessage());

                Log.error(this, msg.toString());
                Log.error(this, e); //sonar
            }
        }

        Log.info(SantCheckGlobalMTACollateralRule.class, "SantCheckGlobalMTACollateralRule Update- End");
        wfr.success();
        return wfr;
    }

    /**
     * get contracts given its id
     *
     * @param childrenIds ids
     * @return vector of contracts
     */
    private Vector<CollateralConfig> getContracts(Vector<Integer> childrenIds) {
        Vector<CollateralConfig> contracts = new Vector<CollateralConfig>();

        for (Integer child : childrenIds) {
            CollateralConfig contract = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), child);

            if (contract != null) {
                contracts.add(contract);
            }
        }

        return contracts;
    }

    /**
     * Look for IM_ SUB_CONTRACTS additional field to get children contracts
     *
     * @param contract facade contract
     * @return vector of children
     */
    private Vector<Integer> getChildrenIds(CollateralConfig contract) {
        Vector<Integer> childrenConfig = new Vector<Integer>();

        String childrenIds = contract.getAdditionalField(IM_SUB_CONTRACTS_AF);

        if (!Util.isEmpty(childrenIds)) {
            String[] children = childrenIds.trim().split(",");

            for (String child : children) {
                try {
                    Integer id = Integer.valueOf(child);

                    childrenConfig.add(id);
                } catch (NumberFormatException ex) {
                    Log.error(this, "Couldn't get the child id from Facade contract: " + ex.getMessage());
                }

            }
        }

        return childrenConfig;
    }

    /**
     * Get the entries related to those children with positive or negative
     * amounts in order to check it with master contract MTA
     *
     * @param childrenContracts    children collateral configs
     * @param childrenEntries      children today entries
     * @param checkNegativeAmounts take only negative entries into account
     * @return hashmap of entries applicable
     */
    private HashMap<MarginCallEntry, Double> getEntriesAndAmountRelated(Vector<CollateralConfig> childrenContracts,
                                                                        List<MarginCallEntryDTO> childrenEntries, boolean checkNegativeAmounts) {
        HashMap<MarginCallEntry, Double> entriesRelated = new HashMap<>();

        for (MarginCallEntryDTO childEntry : childrenEntries) {
            double childAmount = childEntry.getConstitutedMargin();

            if (((childAmount < 0.0) && checkNegativeAmounts) || ((childAmount > 0.0) && !checkNegativeAmounts)) {

                for (CollateralConfig contract : childrenContracts) {
                    if (contract.getId() == childEntry.getCollateralConfigId()) {
                        MarginCallEntry paramMarginCallEntry;

                        paramMarginCallEntry = SantMarginCallUtil.getMarginCallEntry(childEntry, contract,false);
                        entriesRelated.put(paramMarginCallEntry, Math.abs(childAmount));
                    }
                }
            }
        }

        return entriesRelated;
    }

    /**
     * Sum all margincallentry amounts inside the map
     *
     * @param entriesRelated map of entries
     * @param facadeCcy      facade contract currency
     * @return sum of mta of the children related
     */
    @SuppressWarnings("rawtypes")
    private double getTotalMargin(Map<MarginCallEntry, Double> entriesRelated, String facadeCcy) {
        double totalMargin = 0.0;

        Iterator it = entriesRelated.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            MarginCallEntry mccEntry = (MarginCallEntry) entry.getKey();

            try {
                double amountConverted = CollateralUtilities.convertCurrency(mccEntry.getContractCurrency(),
                        (Double) entry.getValue(), facadeCcy, mccEntry.getValueDate(), mccEntry.getPricingEnv());

                Log.info(SantCheckGlobalMTACollateralRule.class,
                        "SantCheckGlobalMTACollateralRule Update - Amount converted for mccEntry: " + mccEntry.getId());

                totalMargin += amountConverted;
            } catch (MarketDataException e) {
                StringBuilder msg = new StringBuilder("SantCheckGlobalMTACollateralRule Update - ");
                msg.append("Couldn't convert amount for mccEntry: ");
                msg.append(mccEntry.getId());
                msg.append(". Error: ").append(e.getMessage());

                Log.error(SantCheckGlobalMTACollateralRule.class, msg.toString());
                Log.error(this, e);//sonar
            }
        }

        return Math.abs(totalMargin);
    }

    /**
     * Apply action to a set of entries
     *
     * @param entriesRelated entries to apply action
     */
    @SuppressWarnings("rawtypes")
    private void applyAction(Map<MarginCallEntry, Double> entriesRelated) {
        Iterator it = entriesRelated.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            MarginCallEntry mcEntry = (MarginCallEntry) entry.getKey();

            int entryId = 0;
            try {
                entryId = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralServer()
                        .save(mcEntry.toDTO(), ACTION, TimeZone.getDefault());

                Log.info(this, "Action " + ACTION + " successfully applied on Entry with id " + entryId);
            } catch (RemoteException e) {
                StringBuffer msg = new StringBuffer("Action ");
                msg.append(ACTION).append(" could not be applied for Entry with id ");
                msg.append(entryId).append(". Error: ").append(e.getMessage());
                Log.error(this, msg.toString());
                Log.error(this, e); //sonar
            }
        }
    }

    // MTA
    public static double getMta(final CollateralConfig marginCall, MarginCallEntry entry, String poCpty) {
        // GLOBAL RATING
        if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.GLOBAL_RATING)) {
            // get po/cpty credit ratings
            Vector<String> agencies = marginCall.getEligibleAgencies();

            Vector<CreditRating> poCreditRatings = new Vector<CreditRating>();
            Vector<CreditRating> cptyCreditRatings = new Vector<CreditRating>();

            MarginCallCreditRatingConfiguration mccRatingConfigPo = null;
            MarginCallCreditRatingConfiguration mccRatingConfigCpty = null;
            try {
                mccRatingConfigPo = CollateralUtilities.getMCRatingConfiguration(marginCall.getLeRatingsConfigId());
                poCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(marginCall, agencies,
                        marginCall.getLegalEntity().getId(), entry.getValueDate(), mccRatingConfigPo.getRatingType());

                mccRatingConfigCpty = CollateralUtilities.getMCRatingConfiguration(marginCall.getLeRatingsConfigId());
                cptyCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(marginCall, agencies,
                        marginCall.getLegalEntity().getId(), entry.getValueDate(), mccRatingConfigCpty.getRatingType());
            } catch (Exception e) {
                Log.error("Cannot get Cpty ratingMatrix for contract = " + marginCall.getName(), e);
            }

            if (!Util.isEmpty(poCpty) && poCpty.equalsIgnoreCase("PO")) {
                MarginCallCreditRating mccCreditRating = ELBEandKGRutilities.getMccrt(poCreditRatings,
                        marginCall.getPoRatingsConfigId(), marginCall.getPoMTARatingDirection(), entry.getValueDate());
                if (mccCreditRating != null) {
                    return ELBEandKGRutilities.getMtaDependingOnRating(marginCall, mccCreditRating,
                            marginCall.getPoRatingsConfigId(), entry.getProcessDate(), entry.getValueDate());
                }
            } else {
                MarginCallCreditRating mccCreditRating = ELBEandKGRutilities.getMccrt(cptyCreditRatings,
                        marginCall.getLeRatingsConfigId(), marginCall.getLeMTARatingDirection(), entry.getValueDate());
                if (mccCreditRating != null) {
                    return ELBEandKGRutilities.getMtaDependingOnRating(marginCall, mccCreditRating,
                            marginCall.getLeRatingsConfigId(), entry.getProcessDate(), entry.getValueDate());
                }
            }
        }
        // AMOUNT
        if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.AMOUNT)) {
            return ELBEandKGRutilities.getMtaDependingOnAmount(marginCall, poCpty, entry.getValueDate());
        }
        // PERCENT
        if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.PERCENT)) {
            return ELBEandKGRutilities.getMtaDependingOnPercent(marginCall, poCpty, entry.getProcessDate());
        }
        // BOTH
        if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.BOTH)) {
            return ELBEandKGRutilities.getMtaDependingOnBoth(marginCall, poCpty, entry.getProcessDate(),
                    entry.getValueDate());
        }

        return 0.00;
    }
}
