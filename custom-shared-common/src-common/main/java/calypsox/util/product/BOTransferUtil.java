package calypsox.util.product;

import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TransferArray;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class BOTransferUtil {


    private BOTransferUtil() {
        //EMPTY
    }

    /**
     * @param xferOptional
     * @return
     */
    public static CollateralConfig getCollateralConfig(Optional<BOTransfer> xferOptional) {
        Optional<CollateralConfig> contractOpt = xferOptional.map(BOTransferUtil::findCollateralConfigFromBOTransfer);
        return contractOpt.orElse(null);
    }

    /**
     * @param xfer
     * @return
     */
    public static CollateralConfig findCollateralConfigFromBOTransfer(BOTransfer xfer) {
        return getMarginCallContractFromTradeId(getTradeIdFromBOTransfer(xfer));
    }

    /**
     * @param xfer
     * @return
     */
    public static long getTradeIdFromBOTransfer(BOTransfer xfer) {
        long tradeId = xfer.getTradeLongId();
        //This means that the Xfer is a netting
        if (tradeId < 1) {
            TransferArray underlyings = NettedBOTransferUtil.getNettedBOTransferUnderlyings(Optional.ofNullable(xfer));
            if (underlyings != null && !Util.isEmpty(underlyings.getTransfers())) {
                tradeId = underlyings.getTransfers()[0].getTradeLongId();
            }
        }
        return tradeId;
    }

    /**
     * @param tradeId
     * @return
     * @throws CalypsoServiceException
     */
    private static CollateralConfig getMarginCallContractFromTradeId(long tradeId) {
        Trade trade = null;
        try {
            trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
        } catch (CalypsoServiceException exc) {
            Log.error(BOTransferUtil.class.getSimpleName(), exc.getCause());
        }
        return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), getContractIdFromTrade(trade));
    }

    /**
     * @param trade
     * @return
     */
    private static int getContractIdFromTrade(Trade trade) {
        int contractId = 0;
        if (!java.util.Objects.isNull(trade)) {
            if (trade.getProduct() instanceof MarginCall) {
                contractId = ((MarginCall) trade.getProduct()).getMarginCallId();
            } else {
                Optional<Integer> integerOpt = Optional.ofNullable(trade.getKeywordValue(CollateralStaticAttributes.MC_CONTRACT_NUMBER)).map(Integer::parseInt);
                contractId = integerOpt.orElse(0);
            }
        }
        return contractId;
    }
}
