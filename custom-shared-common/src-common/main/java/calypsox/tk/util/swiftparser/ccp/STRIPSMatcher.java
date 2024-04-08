package calypsox.tk.util.swiftparser.ccp;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TransferArray;

import java.util.Vector;

/**
 * STRIPSMatcher index incoming MT54X by STRIPS logic
 *
 * @author Ruben Garcia
 */
public class STRIPSMatcher extends GenericPlatformMatcher {

    @Override
    public TransferArray indexBySettlementReferenceInstructed(ExternalMessage swiftMess, PricingEnv env, DSConnection ds,
                                                              Object dbCon, Vector<String> errors) throws MessageParseException {
        return null;
    }

    @Override
    public TransferArray indexByBOTransferFields(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector<String> errors) {
        if (swiftMess instanceof SwiftMessage && ds != null && !Util.isEmpty(swiftMess.getType())
                && "MT540,MT541,MT542,MT543".contains(swiftMess.getType())) {
            return indexMT540toMT543Transfers((SwiftMessage) swiftMess, ds);
        }
        return null;
    }
}
