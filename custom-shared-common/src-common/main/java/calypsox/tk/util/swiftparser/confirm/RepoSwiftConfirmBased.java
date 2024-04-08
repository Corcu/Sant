package calypsox.tk.util.swiftparser.confirm;

import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

/**
 * @author aalonsop
 */
public interface RepoSwiftConfirmBased {

    default Object indexRepo(SwiftMessage swiftMess, DSConnection ds) throws MessageParseException {
        Object indexedObject = null;
        try {
            indexedObject = new RepoSwiftPaymentConfirmMatcher().index(swiftMess, ds);
        } catch (NumberFormatException | CalypsoServiceException | CloneNotSupportedException exc) {
            Log.error(this, exc.getCause());
        }
        return indexedObject;

    }
}
