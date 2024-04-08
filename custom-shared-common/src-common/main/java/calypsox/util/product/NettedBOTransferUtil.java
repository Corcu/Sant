package calypsox.util.product;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class NettedBOTransferUtil {

    private NettedBOTransferUtil() {
        //EMPTY
    }


    public static TransferArray getNettedBOTransferUnderlyings(Optional<BOTransfer> nettedBoTransfer) {
        TransferArray underlyings = nettedBoTransfer.map(BOTransfer::getUnderlyingTransfers).orElseGet(TransferArray::new);
        if (Util.isEmpty(underlyings)) {
            try {
                underlyings = DSConnection.getDefault().getRemoteBO().getNettedTransfers(nettedBoTransfer.map(BOTransfer::getLongId).orElse((long)0));
            } catch (CalypsoServiceException exc) {
                Log.error(NettedBOTransferUtil.class, exc.getCause());
            }
        }
        return underlyings;
    }
}
