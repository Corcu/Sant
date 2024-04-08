/**
 *
 */
package calypsox.tk.collateral.allocation.validator;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocation;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportConstants;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.pdv.importer.PDVConstants;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.collateral.util.ExternalAllocationImportUtils;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.TradeInterfaceUtils;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author aela
 *
 */
public class ExternalAllocationBeanValidator implements AllocationBeanValidator {

    private MarginCallEntry entry;

    private CollateralConfig mcc;

    protected ExternalAllocationImportContext context;

    public ExternalAllocationBeanValidator(MarginCallEntry entry,
                                           ExternalAllocationImportContext context) {
        this.entry = entry;
        this.context = context;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * calypsox.tk.collateral.allocation.validator.AllocationBeanValidatorInterface
     * #
     * validate(calypsox.tk.collateral.allocation.bean.ExternalAllocationInterface
     * , java.util.List)
     */
    @Override
    public boolean validate(ExternalAllocation bean,
                            List<AllocImportErrorBean> messages) {

        ExternalAllocationBean allocBean = (ExternalAllocationBean) bean;

        boolean isValid = true;
        // check mandatory fields
        if (Util.isEmpty(allocBean.getCollateralType())) {
            messages.add(new AllocImportErrorBean(
                    ExternalAllocationImportConstants.ERR_EMPTY_MANDATORY_FIELD,
                    "The mandatory field Collateral Type is not set", allocBean));
            isValid = false;
        }

        if (!isValidCollatType(allocBean.getCollateralType())) {
            messages.add(new AllocImportErrorBean(
                    ExternalAllocationImportConstants.ERR_INVALID_VALUE,
                    "Invalid value for field collateral_type:  " + allocBean.getCollateralType(), allocBean));
            isValid = false;
        }

        // check the action
        if (!isValidAction(allocBean.getAction())) {
            messages.add(new AllocImportErrorBean(
                    ExternalAllocationImportConstants.ERR_INVALID_VALUE,
                    "Invalid value for field action:  " + allocBean.getAction(), allocBean));
            isValid = false;
        }

        // check the existence of the trade PDV and get the corresponding
        // contract
        isValid = isValid && checkCollateralConfig(allocBean, messages);

        if (allocBean.getCollateralConfig() == null) {
            return false;
        }

        mcc = allocBean.getCollateralConfig();

        if (entry == null) {
            // get the entry
            if (allocBean.getCollateralConfig() != null) {
                List<String> errors = new ArrayList<String>();
                try {

                    entry = CollateralManagerUtil.loadEntry(allocBean
                            .getCollateralConfig().getId(), context
                            .getExecutionContext(), errors);
                } catch (RemoteException e) {
                    messages.add(new AllocImportErrorBean(null,
                            "Unable to load the margin call entry for the contract " + mcc.getName(), allocBean));
                    Log.error(this, e);
                    return false;
                }

            }
        }
        if (entry == null || entry.getId() <= 0) {
            messages.add(new AllocImportErrorBean(null,
                    "Unable to load the margin call entry for the contract " + mcc.getName(), allocBean));
            return false;
        }
        allocBean.setEntry(entry);
        // isValid = isValid && checkMarginCallEntry(allocBean, messages);

        // check the book
        isValid = isValid && checkBook(allocBean, messages);

        // check currency
        isValid = isValid && checkCurrency(allocBean, messages);

        // check currency
        //isValid = isValid && checkEligibleCurrency(allocBean, messages);

        return isValid;
    }

    // protected boolean checkMarginCallEntry(ExternalAllocationBean allocBean,
    // List<AllocImportErrorBean> messages) {
    // allocBean.getCollateralConfig()
    // return false;
    // }

    /**
     * @param allocBean
     * @param messages
     * @return
     */
    protected boolean checkCollateralConfig(ExternalAllocationBean allocBean,
                                            List<AllocImportErrorBean> messages) {
        boolean isContractFound = false;

        if (allocBean.getCollateralConfig() != null
                && allocBean.getCollateralConfig().getId() > 0) {
            return true;
        }

        String boSystem = allocBean.getAttributes().get(
                PDVUtil.COLLAT_FO_SYSTEM_FIELD);
        String frontId = allocBean.getAttributes().get(
                PDVUtil.COLLAT_NUM_FRONT_ID_FIELD);
        String collatId = allocBean.getAttributes().get(
                PDVUtil.COLLAT_COLLAT_ID_FIELD);

        TradeArray existingTrades = TradeInterfaceUtils
                .getTradeByBORefAndBOSystem(
                        allocBean.getAttributes().get(
                                PDVUtil.COLLAT_FO_SYSTEM_FIELD),
                        allocBean.getAttributes().get(
                                PDVUtil.COLLAT_NUM_FRONT_ID_FIELD), !"CANCEL".equals(allocBean.getAction()));

        if (existingTrades.size() >= 1) {
            Trade existingTrade = existingTrades.get(0);
            int contractNumber = Math.toIntExact(existingTrade
                    .getKeywordAsLongId(CollateralStaticAttributes.MC_CONTRACT_NUMBER));
            if (contractNumber == 0) {
                List<CollateralConfig> collatConfigs = PDVUtil.getCollateralConfig(existingTrade);
                if (collatConfigs != null && collatConfigs.size() >= 1) {
                    TaskArray tasks = new TaskArray();
                    for (CollateralConfig collateralConfig : collatConfigs) {
                        Task task = PDVUtil.buildTask(Util.isEmpty(allocBean.getPdvMessageContent()) ? "" : allocBean.getPdvMessageContent(), PDVConstants.MORE_THAN_ONE_ELIGIBLE_CONTRACT, ExternalAllocationImportUtils.TASK_EXCEPTION_TYPE,
                                ExternalAllocationImportUtils.TASK_EXCEPTION_TYPE, "Collateral", Long.valueOf(collatId), collateralConfig.getId(), existingTrade.getLongId());
                        tasks.add(task);
                    }
                    if (tasks.size() > 0) {
                        try {
                            DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(tasks, 0, null);
                        } catch (RemoteException e) {
                            messages.add(new AllocImportErrorBean(null,
                                    "Unable to save tasks", allocBean));
                            Log.error(this, e);
                            return false;
                        }
                    }
                    return true;
                } else {
                    messages.add(new AllocImportErrorBean(null,
                            "No contract found for trade " + existingTrade.getLongId(), allocBean));
                    return false;
                }
            }
            CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(
                    DSConnection.getDefault(), contractNumber);
            if (mcc == null) {
                //
                messages.add(new AllocImportErrorBean(
                        "No margin call contract found for the this allocation", allocBean));
                return false;
            }

            allocBean.setCollateralConfig(mcc);
            isContractFound = true;

            // now get information from the trade to use with the allocation
            allocBean.addAttribute(PDVConstants.IS_FINANCEMENT_TRADE_KEYWORD,
                    existingTrade.getKeywordValue(PDVConstants.IS_FINANCEMENT_TRADE_KEYWORD));
            allocBean.addAttribute(PDVConstants.DVP_FOP_TRADE_KEYWORD,
                    existingTrade.getKeywordValue(PDVConstants.DVP_FOP_TRADE_KEYWORD));

        } else if (existingTrades.size() == 0) {
            messages.add(new AllocImportErrorBean(
                    "No trade exists yet with the reference "
                            + (Util.isEmpty(boSystem) ? "null" : boSystem)
                            + " - "
                            + (Util.isEmpty(frontId) ? "null" : frontId), allocBean));
            isContractFound = false;
        } else {
            messages.add(new AllocImportErrorBean(
                    "More than one trade exist for the same BO_SYSTEM and BO_REFERENCE: ", allocBean));
            isContractFound = false;
        }

        return isContractFound;
    }

    /**
     * @param allocBean
     * @param messages
     * @return
     */
    private boolean checkCurrency(ExternalAllocationBean allocBean,
                                  List<AllocImportErrorBean> messages) {
        CurrencyDefault cd = LocalCache.getCurrencyDefault(allocBean
                .getAssetCurrency());
        if (cd == null) {
            messages.add(new AllocImportErrorBean(
                    ExternalAllocationImportConstants.ERR_CCY_NOT_FOUND,
                    " Currency " + allocBean.getAssetCurrency()
                            + " is not valid or not setup in the system", allocBean));
            return false;
        }
        return true;
    }

    /**
     * @param allocBean
     * @param messages
     * @return
     */
    private boolean checkBook(ExternalAllocationBean allocBean,
                              List<AllocImportErrorBean> messages) {

        Book book = BOCache.getBook(DSConnection.getDefault(),
                allocBean.getCollateralBook());

        if (book == null) {
            messages.add(new AllocImportErrorBean(
                    ExternalAllocationImportConstants.ERR_BOOK_NOT_VALID,
                    "Invalid Book "
                            + (Util.isEmpty(allocBean.getCollateralBook()) ? ""
                            : allocBean.getCollateralBook()), allocBean));
            return false;
        }

        /*
         * if(allocBean.getMcc() != null) { allocBean.getMcc().is
         * //allocBean.getMcc().getEligibleBooks(arg0) }
         */
        return true;
    }

    /**
     * @return the entry
     */
    public MarginCallEntry getEntry() {
        return entry;
    }

    /**
     * @return the mcc
     */
    public CollateralConfig getMcc() {
        return mcc;
    }


    private boolean isValidAction(String action) {
        for (PDVConstants.PDV_ACTION act : PDVConstants.PDV_ACTION.values()) {
            if (act.name().equals(action)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidCollatType(String collatType) {
        for (PDVConstants.PDV_COLLAT_TYPE colType : PDVConstants.PDV_COLLAT_TYPE.values()) {
            if (colType.name().equals(collatType)) {
                return true;
            }
        }
        return false;
    }

}
