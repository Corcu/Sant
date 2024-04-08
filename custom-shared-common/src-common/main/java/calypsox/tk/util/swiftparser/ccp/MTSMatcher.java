package calypsox.tk.util.swiftparser.ccp;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TransferArray;

import java.util.Vector;

/**
 * MTSMatcher index incoming MT54X by MTS logic
 * Index by SettlementReferenceInstructed vs MxElectplatId and SRIPlatform = MTS
 *
 * @author Ruben Garcia
 */
public class MTSMatcher extends GenericPlatformMatcher {

    private static final String MTS_PLATFORM = "MTS";

    @Override
    public TransferArray indexBySettlementReferenceInstructed(ExternalMessage swiftMess, PricingEnv env, DSConnection ds,
                                                              Object dbCon, Vector<String> errors) throws MessageParseException {
        String sender = swiftMess.getSender();
        if (!Util.isEmpty(sender) && sender.contains("IBRCESMMXXXX")) {
            String commRef = getCOMMRef(swiftMess);
            if (!Util.isEmpty(commRef) && commRef.length() >= 6) {
                return getBOTransfers(filterBySRIValueAndPlatform(commRef.substring(0, 6), MTS_PLATFORM), ds);
            }
        } else {
            String relaRef = getRELARef(swiftMess);
            if (!Util.isEmpty(relaRef) && relaRef.length() >= 6) {
                return getBOTransfers(filterBySRIValueAndPlatform(relaRef.substring(0, 6), MTS_PLATFORM), ds);
            }
        }
        return null;
    }
}
