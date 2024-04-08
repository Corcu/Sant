package calypsox.util.product;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.CustomerTransferSDISelector;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class CustomerTransferUtil {

    private static final String CONTRACT_ID_ACC_ATTR = "MARGIN_CALL_CONTRACT";
    private static final String INTEREST_TRANSFER_FROM_STR = "INTEREST_TRANSFER_FROM";

    private CustomerTransferUtil() {
        //EMPTY
    }


    /**
     * @param account
     * @return
     */
    public static int getMarginCallContractIdFromAccount(Account account) {
        int mccId = 0;
        if (account != null) {
            mccId = Integer.parseInt(account.getAccountProperty(CONTRACT_ID_ACC_ATTR));
        }
        return mccId;
    }

    /**
     * @param account
     * @return
     */
    public static CollateralConfig getMarginCallContractFromAccountId(int accountId) {
        Account account = BOCache.getAccount(DSConnection.getDefault(), accountId);
        return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), getMarginCallContractIdFromAccount(account));
    }

    /**
     * @param customerTransfer
     * @return
     * @throws CalypsoServiceException
     */
    public static int getIbAccountIdFromCustomerTransfer(Trade trade) {
        long interestBearingId = trade.getKeywordAsLongId(INTEREST_TRANSFER_FROM_STR);
        Optional<Trade> interestBearingTrade;
        try {
            interestBearingTrade = Optional.ofNullable(DSConnection.getDefault().getRemoteTrade().getTrade(interestBearingId));
        } catch (CalypsoServiceException exc) {
            interestBearingTrade = Optional.empty();
            Log.error(CustomerTransferSDISelector.class.getSimpleName(), exc.getCause());
        }
        return interestBearingTrade.map(t -> ((InterestBearing) t.getProduct()).getAccountId()).orElseGet(() -> 0);
    }
}
