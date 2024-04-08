package calypsox.tk.bo.fiflow.builder.handler;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.*;

/**
 * @author aalonsop
 */
public class FIFlowTransferNetHandler {

    BOTransfer boTransfer;

    public FIFlowTransferNetHandler(BOTransfer boTransfer) {
        this.boTransfer = boTransfer;

    }

    public BOTransfer getBoTransfer(){
        return this.boTransfer;
    }

    public boolean isPairOffTransferNet() {
        String nettingType = Optional.ofNullable(this.boTransfer).map(BOTransfer::getNettingType).orElse("");
        long nettedTransferId = Optional.ofNullable(this.boTransfer).map(BOTransfer::getNettedTransferLongId).orElse(0L);
        return isPairOffNetType(nettingType) && nettedTransferId == 0L;
    }

    public boolean isPairOffTransferUnderlying() {
        String nettingType = Optional.ofNullable(this.boTransfer).map(BOTransfer::getNettingType).orElse("");
        long nettedTransferId = Optional.ofNullable(this.boTransfer).map(BOTransfer::getNettedTransferLongId).orElse(0L);
        return isPairOffNetType(nettingType) && nettedTransferId != 0L;
    }

    public boolean isNotPairOffTransferUnderlying() {
        return !isPairOffTransferUnderlying();
    }

    public boolean isNotPairOffTransferNet() {
        return !isPairOffTransferNet();
    }

    public TransferArray getTransferUnderlyings() {
        TransferArray underlyingTransfers = this.boTransfer.getUnderlyingTransfers();
        if (Util.isEmpty(underlyingTransfers)) {
            try {
                underlyingTransfers = DSConnection.getDefault().getRemoteBO().getNettedTransfers(this.boTransfer.getLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading Netting Transfer for BOTransfer: " + this.boTransfer.getLongId());
            }
        }
        return underlyingTransfers;
    }

    public BOTransfer getFirstUndTransfer() {
        BOTransfer undTranfer = null;
        TransferArray underlyingTransfers = getTransferUnderlyings();
        if (!Util.isEmpty(underlyingTransfers)) {
            undTranfer = underlyingTransfers.get(0);
        }
        return undTranfer;
    }

    public String getFirstUndTransferProductType() {
        return Optional.ofNullable(getFirstUndTransfer()).map(BOTransfer::getProductType).orElse("");
    }

    public long getFirstUndTransferTradeId() {
        return Optional.ofNullable(getFirstUndTransfer()).map(BOTransfer::getTradeLongId).orElse(0L);
    }

    public Map<String, String> getTradeKwdFromTransfer(String kwdName) {
        Map<String, String> tradeKwds = new HashMap<>();
        long tradeLongId = Optional.ofNullable(this.boTransfer).map(BOTransfer::getTradeLongId).orElse(0L);
        if(tradeLongId==0L){
            tradeLongId= Optional.ofNullable(getFirstUndTransfer()).map(BOTransfer::getTradeLongId).orElse(0L);
        }
        List<String> kwdNames = new ArrayList<>();
        kwdNames.add(kwdName);
        try {
            tradeKwds = DSConnection.getDefault().getRemoteTrade().getTradeKeywords(tradeLongId, kwdNames);
        } catch (CalypsoServiceException exc) {
            Log.error(this, exc.getCause());
        }
        return tradeKwds;
    }

    private boolean isPairOffNetType(String nettingType) {
        String pairOffStr = "pairoff";
        return nettingType.toLowerCase().contains(pairOffStr);
    }
}
