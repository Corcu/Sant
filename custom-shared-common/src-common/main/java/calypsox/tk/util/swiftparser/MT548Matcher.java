package calypsox.tk.util.swiftparser;

import calypsox.tk.util.swiftparser.ccp.ClearingBasedMatcher;
import calypsox.tk.util.swiftparser.ccp.ClearingIncomingSwiftMatcher;
import calypsox.tk.util.swiftparser.ccp.PlatformMatcher;
import calypsox.tk.util.swiftparser.ccp.PlatformMatcherFactory;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class MT548Matcher extends com.calypso.tk.util.swiftparser.MT548Matcher implements ClearingBasedMatcher {

    @Override
    public Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        Object indexedObject = super.index(swiftMess, env, ds, dbCon, errors);
       if(indexedObject==null){
           indexedObject = indexClearingSwift(swiftMess,env,ds,dbCon,errors);
       }

        if (indexedObject == null) {
            PlatformMatcher matcher = PlatformMatcherFactory.getInstance(swiftMess);
            if (matcher != null) {
                indexedObject = matcher.index(swiftMess, env, ds, dbCon, errors);
            }
        }

        return indexedObject;
    }

    @Override
    public boolean match(ExternalMessage externalMessage, Object object, BOMessage indexedMessage, BOTransfer indexedTransfer, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
       boolean res=matchClearingSwift(externalMessage,object,indexedMessage,indexedTransfer,env,ds,dbCon,errors);
       if(!res){
           res=super.match(externalMessage,object,indexedMessage,indexedTransfer,env,ds,dbCon,errors);
       }
       return res;
    }
}
