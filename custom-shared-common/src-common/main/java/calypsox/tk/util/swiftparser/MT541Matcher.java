package calypsox.tk.util.swiftparser;

import calypsox.tk.util.swiftparser.ccp.BilateralBasedMatcher;

import calypsox.tk.util.swiftparser.ccp.ClearingBasedMatcher;
import calypsox.tk.util.swiftparser.ccp.PlatformMatcher;
import calypsox.tk.util.swiftparser.ccp.PlatformMatcherFactory;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;

import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import java.util.Optional;
import java.util.Vector;

/**
 * MT541Matcher
 *
 * @author Ruben Garcia
 */
public class MT541Matcher extends com.calypso.tk.util.swiftparser.MT541Matcher implements ClearingBasedMatcher, BilateralBasedMatcher {

    @Override
    public Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        Object obj = super.index(swiftMess, env, ds, dbCon, errors);
        if(obj == null) {
            obj = Optional.ofNullable(indexClearingSwift(swiftMess, env, ds, dbCon, errors)).
                    orElse(indexBilateralSwift(swiftMess, env, ds, dbCon, errors));
        }
        if(obj == null){
            PlatformMatcher matcher = PlatformMatcherFactory.getInstance(swiftMess);
            if(matcher != null){
                obj = matcher.index(swiftMess, env, ds, dbCon, errors);
            }
        }
        return obj;
    }



}
