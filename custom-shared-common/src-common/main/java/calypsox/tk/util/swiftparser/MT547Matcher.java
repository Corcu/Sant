package calypsox.tk.util.swiftparser;

import calypsox.tk.util.swiftparser.ccp.ClearingBasedMatcher;
import calypsox.tk.util.swiftparser.ccp.PlatformMatcher;
import calypsox.tk.util.swiftparser.ccp.PlatformMatcherFactory;
import calypsox.tk.util.swiftparser.confirm.RepoSwiftConfirmBased;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class MT547Matcher extends com.calypso.tk.util.swiftparser.MT547Matcher implements RepoSwiftConfirmBased,ClearingBasedMatcher {

    @Override
    public Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        SwiftMessage mess = (SwiftMessage) swiftMess;
        Object indexedObject = Optional.ofNullable(indexRepo(mess, ds)).orElse(indexClearingSwift(swiftMess,env,ds,dbCon,errors));
        if(indexedObject==null) {
            indexedObject = super.index(swiftMess, env, ds, dbCon, errors);
        }
        if (indexedObject == null) {
            PlatformMatcher matcher = PlatformMatcherFactory.getInstance(swiftMess);
            if (matcher != null) {
                indexedObject = matcher.index(swiftMess, env, ds, dbCon, errors);
            }
        }
        return indexedObject;
    }
}
