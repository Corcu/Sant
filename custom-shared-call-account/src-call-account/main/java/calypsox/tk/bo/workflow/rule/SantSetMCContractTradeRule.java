package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Created by XI334330 on 05/06/2019.
 */

public class SantSetMCContractTradeRule implements WfTradeRule {
    public static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";
    public static final String INTEREST_BEARING = "InterestBearing";


    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                         final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        final String desc = "If the trade is InterestBearing or CustomerTransfer, gets the CallAccount Id . \n"
                + "then gets the Contract id from the account attribute and sets it to SimpleTransfer trade as KeyWord. ";
        return desc;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                          final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

        Log.debug("SantSetMCContractTradeRule", "Update - Start");

        if ((trade == null) || !acceptProductType(trade)){
            return true;
        }

        try {

            int accountId = 0;
            Account account = null;
            InterestBearing interestBearingCT = null;

            if (trade.getProduct() instanceof CustomerTransfer){
                String[] interestBearings = trade.getKeywordValue("INTEREST_TRANSFER_FROM").split(",");
                if (INTEREST_BEARING.equalsIgnoreCase(trade.getKeywordValue("TradeSource"))) {
                    interestBearingCT = (InterestBearing) (dsCon.getRemoteTrade().getTrade(Long.parseLong(interestBearings[0]))).getProduct();
                } else {
                    messages.add("No InterestBearing found for the Customer Transfer" + trade.getLongId());
                    return false;
                }
            }

            if (interestBearingCT == null) {
                if (trade.getProduct() instanceof InterestBearing) {
                    InterestBearing interestBearing = (InterestBearing) trade.getProduct();
                    accountId = Math.toIntExact(interestBearing.getAccountId());
                    account = dsCon.getRemoteAccounting().getAccount(accountId);
                }
            } else {
                accountId = Math.toIntExact(interestBearingCT.getAccountId());
                account = dsCon.getRemoteAccounting().getAccount(accountId);
            }

            if (account == null) {
                messages.add("No Account found with linked id=" + account.getLongId());
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
        return trade != null && (SimpleTransfer.CUSTOMERTRANSFER.equals(trade.getProductType()) || INTEREST_BEARING.equalsIgnoreCase(trade.getProductType()));
    }

}


