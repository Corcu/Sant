package calypsox.tk.util.swiftparser.ccp;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TransferArray;


import java.util.Vector;

/**
 * SENDMatcher index incoming MT54X by SEND logic
 * Index by SettlementReferenceInstructed vs Mx SentinelID and SRIPlatform = SEND
 *
 * @author Ruben Garcia
 */
public class SENDMatcher extends GenericPlatformMatcher {

    private static final String SEND_PLATFORM = "SEND";

    @Override
    public TransferArray indexBySettlementReferenceInstructed(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector<String> errors) throws MessageParseException {
        String commRef = getCOMMRef(swiftMess);
        if (!Util.isEmpty(commRef) && commRef.length() >= 9) {
            return getBOTransfers(filterBySRIValueAndPlatform(commRef.substring(commRef.length() - 9), SEND_PLATFORM), ds);
        }
        return null;
    }

    @Override
    public TransferArray indexByBOTransferFields(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector<String> errors) {
        return null;
    }
}
