package calypsox.tk.util.swiftparser.confirm;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.product.Repo;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

/**
 * @author aalonsop
 *
 * For Repo only
 */
public class RepoSwiftPaymentConfirmMatcher extends CustomSwiftPaymentConfirmMatcher {


    /**
     * Filters out any matched done over a non Repo xfer
     * @param mess
     * @param ds
     * @return
     * @throws MessageParseException
     * @throws CalypsoServiceException
     * @throws CloneNotSupportedException
     */
    @Override
    public Object index(SwiftMessage mess, DSConnection ds) throws MessageParseException, CalypsoServiceException, CloneNotSupportedException {
        Object indexedObject=super.index(mess,ds);
        if(indexedObject instanceof BOMessage){
            boolean isRepo= Repo.class.getSimpleName().equalsIgnoreCase(((BOMessage)indexedObject).getProductType());
            if(!isRepo){
                indexedObject=null;
            }
        }
        return indexedObject;
    }
}
