package calypsox.tk.util;

import calypsox.tk.util.concentrationlimits.*;
import calypsox.util.SantCalypsoUtilities;
import calypsox.util.collateral.SantMCConfigFilteringUtil;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.collateral.filter.MarginCallConfigDescFilter;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.sql.CollateralConfigDesc;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import static calypsox.tk.util.ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES.TASK_MODE.*;

// Project: Concentration Limits - Phase II

/**
 *
 */
public class ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES extends ScheduledTask {

    private static final String DESCRIPTION =
            "Generates Concentration Rules from values in Additional Fields";
    private static final String SDF_ALL_PRODUCTS = "SDF_CL_TECH_ALL_PRODUCTS";
    private static final String ST_ATTRIBUTE_DELETE_SDFS = "Delete Product SDFs";
    private static final String ST_ATTRIBUTE_TASK_MODE = "Task Mode";
    private static final int CONTRACTS_PER_STEP = 10000;
    private static final long serialVersionUID = 3996822550200209216L;
    private TASK_MODE taskMode;

    /**
     *
     */
    public enum TASK_MODE {
        STANDARD,
        EXPOSURE
    }

    /*
     * @see com.calypso.tk.util.ScheduledTask#getTaskInformation()
     */
    @Override
    public String getTaskInformation() {
        return DESCRIPTION;
    }

    private TASK_MODE initTaskMode() {
        TASK_MODE taskModeLocal = STANDARD;
        String taskModeAttribute = getAttribute(ST_ATTRIBUTE_TASK_MODE);
        if (!Util.isEmpty(taskModeAttribute)) {
            try {
                taskModeLocal = valueOf(taskModeAttribute);
            } catch (IllegalArgumentException exc) {
                Log.warn(
                        this, "TaskMode attribute is not correctly set. " + exc.getMessage(), exc.getCause());
            }
        }
        return taskModeLocal;
    }

    /**
     * @param ds
     * @param ps
     * @return
     */
    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        boolean processOk = false;

        String taskModeAttribute = getAttribute(ST_ATTRIBUTE_TASK_MODE);
        try {
            taskMode = initTaskMode();
            if (STANDARD.equals(taskMode)) {
                processOk = processStandard();
            } else if (EXPOSURE.equals(taskMode)) {
                processOk = processExposure();
            } else {
                Log.error(
                        this, String.format("Method not defined for task mode \"%s\"", taskModeAttribute));
            }
        } catch (NullPointerException exc) {
            Log.error(this, "A null pointer exception was thrown: " + exc.getMessage(), exc.getCause());
        }
        return processOk;
    }

    /**
     * @return
     */
    private boolean processStandard() {
        // Concentration Limits - Phase II
        JDate processDate = getValuationDatetime().getJDate(TimeZone.getDefault());

        Log.info(this, "Retrieving contracts...");
        long getContractsBegin = System.currentTimeMillis();
        // Concentration Limits - Phase II
        List<CollateralConfig> allContracts =
                SantConcentrationLimitsUtil.getContracts(SantMCConfigFilteringUtil.getInstance().getInstance().buildMCConfigFilter(), processDate);
        // Concentration Limits - Phase II - End
        long getContractsEnd = System.currentTimeMillis();
        Log.info(this, "Time to get contracts: " + (getContractsEnd - getContractsBegin) + "ms");
        Log.info(this, "Number of contracts: " + allContracts.size());

        Log.info(this, "Retrieving products...");
        long getProductsBegin = System.currentTimeMillis();
        List<Product> allProducts = getAllProducts();
        long getProductsEnd = System.currentTimeMillis();
        Log.info(this, "Time to get products: " + (getProductsEnd - getProductsBegin) + "ms");
        Log.info(this, "Number of products: " + allProducts.size());

        Log.info(this, "Mapping products...");
        Map<Integer, Map<SantConcentrationLimitsRuleType, String>> productConcentrationLimitValues =
                SantConcentrationLimitsProductMapper.mapProducts(allProducts, taskMode);

        deleteSDFs();

        Log.info(this, "Obtaining Static Data Filters...");
        long getSdfsBegin = System.currentTimeMillis();
        Map<Integer, Map<SantConcentrationLimitsRuleType, String>> productSDFNames =
                getProductSDFNames(allProducts, productConcentrationLimitValues);
        long getSdfsEnd = System.currentTimeMillis();
        Log.info(this, "Time to obtain SDFs: " + (getSdfsEnd - getSdfsBegin) + "ms");

        // Delete all concentration rules generated automatically
        Log.info(this, "Deleting Concentration Rules...");
        long deleteRulesBegin = System.currentTimeMillis();
        List<Integer> deletedRulesIds =
                SantConcentrationLimitsRulesManager.deleteAllConcentrationRules();
        long deleteRulesEnd = System.currentTimeMillis();
        Log.info(
                this, "Time to delete Concentration Rules: " + (deleteRulesEnd - deleteRulesBegin) + "ms");

        long saveRulesBegin = System.currentTimeMillis();
        for (CollateralConfig contract : allContracts) {
            Log.info(
                    this,
                    "Creating Concentration Rules for contract "
                            + contract.getName()
                            + " ("
                            + contract.getId()
                            + ")");
            // Concentration Limits - Phase II
            List<Integer> newConcentrationRuleIds =
                    SantConcentrationLimitsRulesManager.getNewConcentrationRulesIds(
                            contract, allProducts, productSDFNames, processDate);
            // Concentration Limits - Phase II - End

            try {
                CollateralConfig newContract = contract.clone();
                for (int deletedRuleId : deletedRulesIds) {
                    newContract.getConcentrationRuleIds().remove(Integer.valueOf(deletedRuleId));
                }
                for (int ruleId : newConcentrationRuleIds) {
                    newContract.addConcentrationRuleId(Integer.valueOf(ruleId));
                }

                Log.info(this, "Saving contract...");
                saveContract(newContract);
                Log.info(this, "Number of rules: " + newConcentrationRuleIds.size());
            } catch (CloneNotSupportedException e) {
                Log.error(this, String.format("Could not clone contract %d to save", contract.getId()), e);
            }
        }
        long saveRulesEnd = System.currentTimeMillis();
        Log.info(this, "Time to save Concentration Rules: " + (saveRulesEnd - saveRulesBegin) + "ms");

        Log.info(this, "Processing finished OK");

        return true;
    }

    /**
     * @return
     */
    private boolean processExposure() {
        JDate processDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        int contractsPerStep = getContractsPerStep();

        List<Integer> deletedRulesIds =
                SantConcentrationLimitsRulesManager.deleteAllExposureConcentrationRules();

        // Retrieve and map products
        List<Product> allProducts = null;
        Map<Integer, Map<SantConcentrationLimitsRuleType, String>> productConcentrationLimitValues =
                null;
        Map<Integer, Map<SantConcentrationLimitsRuleType, String>> productSDFNames = null;

        Queue<Integer> allContractsIdsQueue = getAllContractIds();
        while (!allContractsIdsQueue.isEmpty()) {
            List<Integer> partialContractId = new LinkedList<>();
            while (partialContractId.size() < contractsPerStep && !allContractsIdsQueue.isEmpty()) {
                partialContractId.add(allContractsIdsQueue.poll());
            }
            if (!partialContractId.isEmpty()) {
                List<CollateralConfig> contracts = getContracts(partialContractId);
                for (CollateralConfig contract : contracts) {
                    try {
                        // Will be true if this contract has been changed
                        boolean saveContract = false;
                        CollateralConfig newContract = contract.clone();

                        // Delete old exposure concentration rules
                        List<Integer> contractConcentrationRules = newContract.getConcentrationRuleIds();
                        if (!Util.isEmpty(contractConcentrationRules)) {
                            for (int deletedRuleId : deletedRulesIds) {
                                if (contractConcentrationRules.contains(deletedRuleId)) {
                                    saveContract = true;
                                    newContract.getConcentrationRuleIds().remove(Integer.valueOf(deletedRuleId));
                                }
                            }
                        }

                        if (SantConcentrationLimitsUtil.exceedsMaxGlobalRequiredMargin(
                                newContract, processDate)) {
                            saveContract = true;

                            // Retrieve and map products
                            if (Util.isEmpty(allProducts)) {
                                allProducts = getAllProducts();
                            }
                            if (Util.isEmpty(productConcentrationLimitValues)) {
                                productConcentrationLimitValues =
                                        SantConcentrationLimitsProductMapper.mapProducts(allProducts, taskMode);
                            }
                            if (Util.isEmpty(productSDFNames)) {
                                productSDFNames = getProductSDFNames(allProducts, productConcentrationLimitValues);
                            }

                            List<Integer> newRules =
                                    SantConcentrationLimitsRulesManager.getNewExposureConcentrationRulesIds(
                                            contract, allProducts, productSDFNames);
                            for (int ruleId : newRules) {
                                newContract.addConcentrationRuleId(Integer.valueOf(ruleId));
                            }
                        }

                        if (saveContract) {
                            saveContract(newContract);
                        }
                    } catch (CloneNotSupportedException e) {
                        String errorMessage = String.format("Could not clone contract %d", contract.getId());
                        Log.error(this, errorMessage, e);
                    }
                }
            }
        }

        return true;
    }

    /**
     * @return
     */
    @Override
    public Vector<String> getDomainAttributes() {
        Vector<String> attributes = new Vector<>();

        attributes.add(ST_ATTRIBUTE_DELETE_SDFS);
        attributes.add(ST_ATTRIBUTE_TASK_MODE);

        return attributes;
    }

    /**
     * @param attribute
     * @param hashtable
     * @return
     */
    @Override
    public Vector<String> getAttributeDomain(
            final String attribute, final Hashtable<String, String> hashtable) {
        Vector<String> attributeDomain = null;

        if (ST_ATTRIBUTE_DELETE_SDFS.equals(attribute)) {
            attributeDomain = new Vector<>();
            attributeDomain.add(Boolean.TRUE.toString());
            attributeDomain.add(Boolean.FALSE.toString());
        } else if (ST_ATTRIBUTE_TASK_MODE.equals(attribute)) {
            attributeDomain = new Vector<>();
            attributeDomain.add(STANDARD.name());
            attributeDomain.add(EXPOSURE.name());
        }

        return attributeDomain;
    }

    /**
     * @return
     */
    private List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();

        try {
            Set<String> sdFilterNames = new HashSet<>();
            sdFilterNames.add(SDF_ALL_PRODUCTS);
            // MIG V16
            // products = cds.loadProducts(null, allProductsFilters, null);
            Vector<Bond> bonds = SantCalypsoUtilities.getBondAndBondAssetBackedProducts(null, null, null);
            Vector<Bond> equities = DSConnection.getDefault().getRemoteProduct().getProducts(Product.EQUITY, null, null, true, null);
            products.addAll(bonds);
            products.addAll(equities);
        } catch (ExecutionException | CalypsoServiceException exc) {
            Log.error(this, "Could not get SDF for all products", exc);
        }

        return products;
    }

    /**
     * @param products
     * @param productConcentrationLimitValues
     * @return
     */
    private Map<Integer, Map<SantConcentrationLimitsRuleType, String>> getProductSDFNames(
            List<Product> products,
            Map<Integer, Map<SantConcentrationLimitsRuleType, String>> productConcentrationLimitValues) {
        Map<Integer, Map<SantConcentrationLimitsRuleType, String>> productSDFNames = new HashMap<>();
        SantConcentrationLimitsCache cache = new SantConcentrationLimitsCache();
        cache.initSDFCache();

        for (Product product : products) {
            Map<SantConcentrationLimitsRuleType, String> productValues =
                    productConcentrationLimitValues.get(product.getId());
            productSDFNames.put(product.getId(), new HashMap<>());
            for (Entry<SantConcentrationLimitsRuleType, String> mapEntry : productValues.entrySet()) {
                SantConcentrationLimitsRuleType ruleType = mapEntry.getKey();
                String value = mapEntry.getValue();
                String sdfName =
                        SantConcentrationLimitsSDFManager.obtainStaticDataFilterName(ruleType, value, cache);
                productSDFNames.get(product.getId()).put(ruleType, sdfName);
            }
        }

        return productSDFNames;
    }

    /**
     * @param contract
     */
    private void saveContract(CollateralConfig contract) {
        try {
            ServiceRegistry.getDefault().getCollateralDataServer().save(contract);
        } catch (CollateralServiceException e) {
            Log.error(this, String.format("Could not save contract id %d", contract.getId()), e);
        }
    }

    /**
     *
     */
    private void deleteSDFs() {
        if (haveToDeleteSDFs()) {
            Log.info(this, "Deleting Static Data Filters...");
            long deleteSdfsBegin = System.currentTimeMillis();
            SantConcentrationLimitsSDFManager.removeStaticDataFilters(
                    Arrays.asList(SantConcentrationLimitsRuleType.values()));
            long deleteSdfsEnd = System.currentTimeMillis();
            Log.info(this, "Time to delete SDFs: " + (deleteSdfsEnd - deleteSdfsBegin) + "ms");
        }
    }

    /**
     * @return
     */
    private boolean haveToDeleteSDFs() {
        boolean deleteSDFs = false;

        String deleteSDFsString = getAttribute(ST_ATTRIBUTE_DELETE_SDFS);
        if (!Util.isEmpty(deleteSDFsString)) {
            deleteSDFs = Boolean.valueOf(deleteSDFsString);
        }

        return deleteSDFs;
    }

    /**
     * @return
     */
    private Queue<Integer> getAllContractIds() {
        Queue<Integer> allContractIds = new LinkedList<>();

        try {
            MarginCallConfigDescFilter mccdFilter = new MarginCallConfigDescFilter();
            //JDate processDate = getValuationDatetime().getJDate(TimeZone.getDefault());
            //mccdFilter.setProcessDate(processDate);
            List<CollateralConfigDesc> allContractDescs =
                    ServiceRegistry.getDefault()
                            .getCollateralDataServer()
                            .getMarginCallConfigsDesc(mccdFilter);

            for (CollateralConfigDesc desc : allContractDescs) {
                allContractIds.add(desc.getId());
            }
        } catch (CollateralServiceException exc) {
            Log.error(this, exc.getMessage(), exc.getCause());
        }

        return allContractIds;
    }

    /**
     * @param contractIds
     * @return
     */
    private List<CollateralConfig> getContracts(List<Integer> contractIds) {
        List<CollateralConfig> contracts = new LinkedList<>();

        MarginCallConfigFilter mccFilter = SantMCConfigFilteringUtil.getInstance().buildMCConfigFilter(contractIds);


        try {
            contracts = CollateralManagerUtil.loadCollateralConfigs(mccFilter);
        } catch (CollateralServiceException exc) {
            Log.error(this, exc.getMessage(), exc.getCause());
        }

        return contracts;
    }

    /**
     * @return
     */
    private int getContractsPerStep() {
        return CONTRACTS_PER_STEP;
    }
}
