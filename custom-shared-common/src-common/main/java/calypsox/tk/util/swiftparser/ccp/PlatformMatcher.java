package calypsox.tk.util.swiftparser.ccp;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TransferArray;

import java.util.Vector;

/**
 * PlatformMatcher interface
 *
 * @author Ruben Garcia
 */
public interface PlatformMatcher {

    /**
     * Index the incoming MT54X by SettlementReferenceInstructed and SRIPlatform or by BOTransfer fields vs MT54X fields
     *
     * @param swiftMess the incoming MT54X
     * @param env       the Pricing Environment
     * @param ds        the Data Server connection
     * @param dbCon     the Data Base connection
     * @param errors    the error messages
     * @return the index BOTransfer
     * @throws MessageParseException error
     */
    Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector<String> errors)
            throws MessageParseException;

    /**
     * Index the incoming MT54X by SettlementReferenceInstructed and SRIPlatform
     *
     * @param swiftMess the incoming MT54X
     * @param env       the Pricing Environment
     * @param ds        the Data Server connection
     * @param dbCon     the Data Base connection
     * @param errors    the error messages
     * @return the index BOTransfer
     * @throws MessageParseException error
     */
    TransferArray indexBySettlementReferenceInstructed(ExternalMessage swiftMess, PricingEnv env, DSConnection ds,

                                                       Object dbCon, Vector<String> errors) throws MessageParseException;


    /**
     * Index the incoming MT54X by BOTransfer fields vs MT54X fields
     *
     * @param swiftMess the incoming MT54X
     * @param env       the Pricing Environment
     * @param ds        the Data Server connection
     * @param dbCon     the Data Base connection
     * @param errors    the error messages
     * @return the index BOTransfer
     * @throws MessageParseException error
     */
    TransferArray indexByBOTransferFields(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon,
                                          Vector<String> errors) throws MessageParseException;

}
