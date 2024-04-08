package calypsox.tk.util.swiftparser.ccp;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import java.util.Vector;

public interface ClearingBasedMatcher {

    default Object indexClearingSwift(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        Object indexedObject= new ClearingIncomingSwiftMatcher().index(swiftMess, env, ds, dbCon, errors);
        if(indexedObject==null){
            indexedObject=new MTSClearingIncomingSwiftMatcher().index(swiftMess,env,ds,dbCon,errors);
        }
        return indexedObject;
    }

    default boolean matchClearingSwift(ExternalMessage externalMessage, Object object, BOMessage indexedMessage, BOTransfer indexedTransfer, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        return new ClearingIncomingSwiftMatcher().match(externalMessage,object,indexedMessage,indexedTransfer,env,ds,dbCon,errors);
    }
}
