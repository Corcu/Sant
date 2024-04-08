package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.Vector;

/*
 * For SimpleTransfer trades of transferType=INTEREST, gets the linkedId-which is CallAccount Id,
 * then gets the Contract id from the account attribute and sets it to SimpleTransfer trade as KeyWord.
 */
public class SantSetMccIdToIntSimpleXferTradeRule implements WfTradeRule {

    public static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";

    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                         final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        final String desc = "If the trade is SimpleTransfer of transferType=INTEREST, gets the linkedId-which is CallAccount Id . \n"
                + "then gets the Contract id from the account attribute and sets it to SimpleTransfer trade as KeyWord. ";
        return desc;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                          final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

        Log.debug("SantSetMccIdToInterestSimpleTransfer", "Update - Start");

        if ((trade == null) || !acceptProductType(trade)
                || !((SimpleTransfer) trade.getProduct()).getFlowType().equals("INTEREST")) {
            return true;
        }

        try {
            SimpleTransfer simpleTransfer = (SimpleTransfer) trade.getProduct();
            if (!simpleTransfer.getFlowType().equals("INTEREST")) {
                return true;
            }
            int accountId = Math.toIntExact(simpleTransfer.getLinkedLongId());
            Account account = dsCon.getRemoteAccounting().getAccount(accountId);
            if (account == null) {
                messages.add("No Account found with linked id=" + accountId);
                return false;
            }

            CollateralConfig marginCallConfig = null;
            try {
                int mccId = Integer.parseInt(account.getAccountProperty(MARGIN_CALL_CONTRACT));
                marginCallConfig = CacheCollateralClient.getCollateralConfig(dsCon, mccId);
                if (marginCallConfig == null) {
                    messages.add("Invalid contract id specified");
                    return false;
                }
            } catch (Exception exc) {
                messages.add("Invalid contract id specified");
                Log.error(this, exc); //sonar
                return false;
            }

            trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, marginCallConfig.getId());
        } catch (RemoteException e) {
            messages.add(e.getMessage());
            Log.error(this, e); //sonar
            return false;
        }

        Log.debug("SantSetMccIdToInterestSimpleTransfer", "Update - End");

        return true;
    }

    /**
     * @param trade
     * @return
     */
    private boolean acceptProductType(Trade trade) {
        return trade != null && (SimpleTransfer.SIMPLETRANSFER.equals(trade.getProductType()) || SimpleTransfer.CUSTOMERTRANSFER.equals(trade.getProductType()));

    }

}
