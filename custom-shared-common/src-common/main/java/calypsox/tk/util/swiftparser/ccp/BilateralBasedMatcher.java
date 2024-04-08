package calypsox.tk.util.swiftparser.ccp;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import java.util.Vector;

/**
 * BilateralBasedMatcher get BOTransfer form bilateral incoming Swift MT541 MT543
 *
 * @author Ruben Garcia
 */
public interface BilateralBasedMatcher {

    /**
     * Get BOTransfer from bilateral incoming Swift MT541 MT543
     *
     * @param swiftMess the current swift message
     * @param env       the pricing environment
     * @param ds        the Data Server connection
     * @param dbCon     the Data Base connection
     * @param errors    the vector error
     * @return the bilateral BOTransfer
     * @throws MessageParseException
     */
    default Object indexBilateralSwift(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        return new BilateralIncomingSwiftMatcher().index(swiftMess, env, ds, dbCon, errors);
    }



}
