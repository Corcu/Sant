package calypsox.tk.util.swiftparser.ccp;


import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TransferArray;
import org.jfree.util.Log;

import java.util.Vector;

/**
 * SENAFMatcher index incoming MT54X by SENAF logic
 * Index by SettlementReferenceInstructed vs MxBankCodeSpain and SRIPlatform = SENAF
 *
 * @author Ruben Garcia
 */
public class SENAFMatcher extends GenericPlatformMatcher {

    private static final String SENAF_PLATFORM = "SENAF";

    @Override
    public TransferArray indexBySettlementReferenceInstructed(ExternalMessage swiftMess, PricingEnv env, DSConnection ds,
                                                              Object dbCon, Vector<String> errors) throws MessageParseException {
        String commRef = getCOMMRef(swiftMess);
        if (!Util.isEmpty(commRef) && commRef.length() >= 14) {
            try {
                return getBOTransfers(filterBySRIValueAndPlatform(String.valueOf(Integer.parseInt(commRef.substring(10, 14))), SENAF_PLATFORM), ds);
            } catch (NumberFormatException e) {
                Log.error(this, e);
            }
        }
        return null;
    }

    @Override
    public TransferArray indexByBOTransferFields(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector<String> errors) {
        return null;
    }
}
