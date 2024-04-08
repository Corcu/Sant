package calypsox.util;

import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class SantDomainValuesUtil {

    public static final String MT569_DESTINATION_FOLDER_MAPPING = "MT569DestinationFolderMapping";
    public static final String SCF_DESTINATION_FOLDER_MAPPING = "SCF_MT569DestinationFolderMapping";
    public static final String SLB_DESTINATION_FOLDER_MAPPING = "SLB_MT569DestinationFolderMapping";
    public static final String LEGAL_AGGR_TYPE = "legalAgreementType";

    public static final String MOV_SENDER_ENG_MSG_TYPE = "MovementSenderEngine_messageType";
    public static final String SLB_MOV_SENDER_ENG_MSG_TYPE = "SLBMovementSenderEngine_messageType";
    public static final String NOTIF_SENDER_ENG_MSG_TYPE = "NotificationSenderEngine_messageType";
    public static final String SWIFT_SENDER_ENG_MSG_TYPE = "SWIFTSenderEngine_messageType";
    public static final String SENDER_ENG_MSG_TYPE = "SenderEngine_messageTypes";

    public static boolean getBooleanDV(String domainName) {
        boolean res = false;
        Vector<String> dvs = LocalCache.getDomainValues(DSConnection.getDefault(), domainName);
        if (!Util.isEmpty(dvs)) {
            String dv = dvs.get(0);
            if (!Util.isEmpty(dv)) {
                res = Boolean.parseBoolean(dv);
            }
        }
        return res;
    }
}
