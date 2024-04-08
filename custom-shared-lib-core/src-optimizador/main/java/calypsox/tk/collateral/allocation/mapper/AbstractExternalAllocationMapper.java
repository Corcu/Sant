/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.collateral.allocation.mapper;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.util.TradeInterfaceUtils;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author aela
 */
public abstract class AbstractExternalAllocationMapper implements
        ExternalAllocationMapper {

    protected ExternalAllocationImportContext context = null;

    /**
     *
     */
    public AbstractExternalAllocationMapper(
            ExternalAllocationImportContext context) {
        this.context = context;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.collateral.allocation.optimizer.importer.mapper.
     * ExternalAllocationMapper
     * #mapAllocation(calypsox.tk.collateral.allocation.optimizer
     * .importer.beans.OptimAllocationBean, java.util.List)
     */
    @Override
    public MarginCallAllocation mapAllocation(
            ExternalAllocationBean allocBean,
            List<AllocImportErrorBean> messages) throws Exception {

        if (!isValidAllocation(allocBean, null, messages)) {
            return null;
        }

        // if it's an allocation amendment, just delete the old draft and create
        // the new one
        // In the case of a cancel action, delete the draft if it exists, otherwise create a reversal allocation
        // MarginCallEntry mce = allocBean.getEntry();
        MarginCallEntry mce = allocBean.getEntry();
        allocBean.setAllocationDirection(("Loan".equalsIgnoreCase(allocBean.getCollateralDirection()) ? -1 : 1));
        if (mce != null) {
            if ("AMEND".equals(allocBean.getAction())) {
                deleteDraftAllocation(mce, allocBean);
            } else
                // in the case of a cancel, the amount sign should be reversed
                if ("CANCEL".equals(allocBean.getAction())) {

                    boolean allocationDeleted = deleteDraftAllocation(mce, allocBean);
                    if (allocationDeleted) {
                        // no need to create a new allocation
                        return null;
                    }
                    // check if one allocation has already been executed
                    if (PDVUtil.checkExistsMarginCallTrade(allocBean)) {
                        // in the cancel case, inverse the allocation direction
                        allocBean.setAllocationDirection(-1 * allocBean.getAllocationDirection());
                        //allocBean.setAssetAmount(allocBean.getAssetAmount()*sign);
                    } else {
                        // DO NOTHING
                        return null;
                    }
                }
        }

        // map the bean to an actual margin call allocation
        MarginCallAllocation mappedAllocation = mapActualAllocation(allocBean, messages);
        //add allocation attributes
        addAllocationAttributes(allocBean, mappedAllocation);
        return mappedAllocation;
    }


    /**
     * @param allocBean
     * @param mappedAllocation
     */
    protected void addAllocationAttributes(ExternalAllocationBean allocBean,
                                           MarginCallAllocation mappedAllocation) {
        if (mappedAllocation == null)
            return;

        Map<String, String> attributes = allocBean.getAttributes();
        if (attributes != null && attributes.size() > 0) {
            for (String attrName : attributes.keySet()) {
                mappedAllocation.addAttribute(attrName, attributes.get(attrName));
            }
        }
    }

    /**
     * @param allocBean
     * @param messages
     * @return
     */
    abstract protected MarginCallAllocation mapActualAllocation(
            ExternalAllocationBean allocBean,
            List<AllocImportErrorBean> messages);

    /**
     * Map a list of beans into a Calypso MarginCallAllocation allocations
     *
     * @param listAllocBean list of allocation beans to map
     * @param messages,     any error occurred during the mapping
     * @return a list of Calypso MarginCallAllocation corresponding to the given
     * beans
     * @throws Exception any thrown exception
     */
    public List<MarginCallAllocation> mapListAllocation(
            List<? extends ExternalAllocationBean> listAllocBean,
            List<AllocImportErrorBean> messages) throws Exception {
        List<MarginCallAllocation> marginCallAllocs = new ArrayList<MarginCallAllocation>();

        for (ExternalAllocationBean allocBean : listAllocBean) {
            List<AllocImportErrorBean> errors = new ArrayList<AllocImportErrorBean>();
            MarginCallAllocation mrgAlloc = mapAllocation(allocBean,
                    errors);
            if (mrgAlloc != null) {
                marginCallAllocs.add(mrgAlloc);
            }
            messages.addAll(errors);
        }
        return marginCallAllocs;
    }

    /**
     * @param listAllocBeans
     * @param messages
     * @return
     * @throws Exception
     */
    public List<ExternalAllocationBean> getValidListAllocation(
            List<ExternalAllocationBean> listAllocBeans, MarginCallEntry entry,
            List<AllocImportErrorBean> messages) throws Exception {
        List<ExternalAllocationBean> validAllocs = new ArrayList<ExternalAllocationBean>();
        for (ExternalAllocationBean allocBean : listAllocBeans) {
            List<AllocImportErrorBean> errors = new ArrayList<AllocImportErrorBean>();
            if (isValidAllocation(allocBean, entry, errors)) {
                validAllocs.add(allocBean);
            } else {
                messages.addAll(errors);
            }
        }
        return validAllocs;
    }

    /**
     * get the collateral config corresponding to the passed allocation bean
     *
     * @param bean
     * @return
     */
    protected CollateralConfig getCollateralConfigForAllocBean(
            ExternalAllocationBean allocBean) {
        Double contractId = context.getContractsNameForId().get(
                allocBean.getContractName());
        int mccId = (contractId == null ? 0 : contractId.intValue());
        return CacheCollateralClient.getCollateralConfig(
                DSConnection.getDefault(), mccId);
    }

    /**
     * get the margin call entry corresponding to the passed allocation bean
     *
     * @param allocBean
     * @return
     */
    protected MarginCallEntry getMCEntryForAllocBean(
            ExternalAllocationBean allocBean, List<String> errors) {
        Double contractId = context.getContractsNameForId().get(
                allocBean.getContractName());
        int mccId = (contractId == null ? 0 : contractId.intValue());
        MarginCallEntry entry = null;
        try {
            entry = CollateralManagerUtil.loadEntry(mccId,
                    context.getExecutionContext(), errors);
        } catch (RemoteException e) {
            errors.add("Unable to load the margin call entry for the contract "
                    + allocBean.getContractName());
            Log.error(this, e);
        }
        return entry;
    }


    /**
     * @param mce
     * @param bean
     */
    private boolean deleteDraftAllocation(MarginCallEntry mce,
                                          ExternalAllocationBean bean) {
        boolean allocationDeleted = false;
        List<MarginCallAllocation> mcallocations = mce
                .getPendingMarginAllocations();
        //Allocation te be removed
        if (!Util.isEmpty(mcallocations)) {
            for (MarginCallAllocation allocation : mcallocations) {
                if (allocation.getAttribute(PDVUtil.COLLAT_COLLAT_ID_FIELD) != null
                        && allocation.getAttribute(
                        PDVUtil.COLLAT_COLLAT_ID_FIELD).equals(
                        bean.getExternalId())) {
                    mce.removeAllocation(allocation);
                    allocationDeleted = true;
                }
            }
        }
        return allocationDeleted;
    }


    /**
     * @param keywordName
     * @param keywordValue
     * @return
     */
    public static TradeArray getTradeByKeyword(String keywordName, String keywordValue) {
        TradeArray existingTrades = null;
        try {
            existingTrades = DSConnection
                    .getDefault()
                    .getRemoteTrade()
                    .getTrades(
                            "trade, trade_keyword kwd",
                            "trade.trade_id=kwd.trade_id "
                                    + " and trade.trade_status<>'CANCELED' and "
                                    + "kwd.keyword_name='" + keywordName + "' and kwd.keyword_value='" + keywordValue + "'",
                            null, null);
        } catch (RemoteException e) {
            Log.error(TradeInterfaceUtils.class, e);
            existingTrades = null;
        }
        return existingTrades;
    }

//	/**
//	 * @param messages
//	 * @return
//	 */
//	public List<AllocImportErrorBean> toAllocImportErrorBean(
//			List<String> messages) {
//		List<AllocImportErrorBean> allocImportErrors = new ArrayList<AllocImportErrorBean>();
//		if (!Util.isEmpty(messages)) {
//			for (String message : messages) {
//				allocImportErrors.add(new AllocImportErrorBean(message,allocBean));
//			}
//		}
//		return allocImportErrors;
//	}
}
