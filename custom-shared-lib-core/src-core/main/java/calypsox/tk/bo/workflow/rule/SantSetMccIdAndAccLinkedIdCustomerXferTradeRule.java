package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.product.CustomerTransferSDIHandler;
import calypsox.util.product.CustomerTransferUtil;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TaskArray;

import java.util.Vector;


/**
 * @author Anonimous
 */
public class SantSetMccIdAndAccLinkedIdCustomerXferTradeRule implements WfTradeRule {

    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                         final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "If the trade is a CustomerTransfer of transferType=INTEREST, gets the AccountName-which is an Account Name. \n"
                + "then gets the Contract id from the account attribute and sets it to the CustomerTransfer trade as a KeyWord.";
    }

    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                          final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

        boolean res = false;
        Log.debug("SantSetMccIdToInterestSimpleTransfer", "Update - Start");

        if (acceptProductType(trade)) {
            CustomerTransfer customerTransfer = (CustomerTransfer) trade.getProduct();
            int accountId = CustomerTransferUtil.getIbAccountIdFromCustomerTransfer(trade);
            if (accountId != 0) {
                CollateralConfig contract = CustomerTransferUtil.getMarginCallContractFromAccountId(accountId);
                setMcContractNumberAsTradeKeyword(contract, trade, messages, dsCon);
                customerTransfer.setLinkedLongId(accountId);
                res = assignCustomerTransferSDI(trade);
            } else {
                messages.add("CustomerTransfer SDI's GLAccount doesn't match any existing account:" + customerTransfer.getCustomerAccountSDI());
            }
        } else {
            messages.add("SantSetMccIdAndAccLinkedId only applies to CustomerTransfer trades");
        }
        Log.debug("SantSetMccIdToInterestSimpleTransfer", "Update - End");
        return res;
    }


    /**
     * @param contract
     * @param trade
     * @param messages
     * @param dsCon
     * @return
     */
    private int setMcContractNumberAsTradeKeyword(CollateralConfig contract, Trade trade, final Vector messages, DSConnection dsCon) {
        int mccId = 0;
        if (contract != null) {
            mccId = contract.getId();
        }
        CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(dsCon, mccId);
        if (marginCallConfig != null) {
            trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, marginCallConfig.getId());
        } else {
            messages.add("Invalid contract id specified");
        }
        return mccId;
    }

    /**
     * @param trade
     * @return
     */
    private boolean acceptProductType(Trade trade) {
        return trade != null && trade.getProduct() instanceof CustomerTransfer;
    }

    /**
     * @param trade
     * @return
     */
    private boolean assignCustomerTransferSDI(Trade trade) {
        boolean successfullAssignment = new CustomerTransferSDIHandler().assignSDIsByCallAccount(trade);
        if (!successfullAssignment) {
            createAndPublishExceptionTask(trade);
        }
        return successfullAssignment;
    }

    /**
     * @param trade
     * @throws CalypsoServiceException
     */
    private void createAndPublishExceptionTask(Trade trade) {
        Task task = new Task(trade, new BOException(trade.getLongId(), this.getClass().getSimpleName(), "Client SDI not assigned"));// 550
        TaskArray v = new TaskArray();
        v.add(task);
        try {
            DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(v, 0L, (String) null);
        } catch (CalypsoServiceException exc) {
            Log.warn(this.getClass().getSimpleName(), exc.getMessage(), exc.getCause());
        }
    }
}
