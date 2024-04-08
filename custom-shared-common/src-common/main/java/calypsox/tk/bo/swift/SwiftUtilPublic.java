/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo.swift;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.bo.document.Document;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.bo.swift.TagValue;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.PartySDI;
import com.calypso.tk.refdata.SDI;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Optional;
import java.util.Vector;

/**
 * This class allows access to protected methods of class SwiftUtil.
 */
public class SwiftUtilPublic extends SwiftUtil {
    /**
     * return same value than the protected method in the calypso's SwiftUtil
     * class
     *
     * @param paramString            string
     * @param paramSDI               sdi
     * @param paramTrade             trade
     * @param paramBOTransfer        transfer
     * @param paramBOMessage         message
     * @param paramTradeTransferRule transfer rule
     * @param paramBoolean           boolean
     * @param paramDSConnection      dsconnection
     * @return same value than the protected method in the calypso's SwiftUtil
     * class
     */
    public static TagValue getTagValuePublic(final String paramString, final SDI paramSDI, final Trade paramTrade,
                                             final BOTransfer paramBOTransfer, final BOMessage paramBOMessage,
                                             final TradeTransferRule paramTradeTransferRule, final boolean paramBoolean,
                                             final DSConnection paramDSConnection) {
        return getTagValue(paramString, paramSDI, paramTrade, paramBOTransfer, paramBOMessage, paramTradeTransferRule,
                paramBoolean, paramDSConnection);
    }

    /**
     * return same value than the protected method in the calypso's SwiftUtil
     * class
     *
     * @param paramString            string
     * @param paramSDI               sdi
     * @param paramTrade             trade
     * @param paramBOTransfer        transfer
     * @param paramBOMessage         message
     * @param paramTradeTransferRule transfer rule
     * @param paramDSConnection      dsconnection
     * @return same value than the protected method in the calypso's SwiftUtil
     * class
     */
    public static TagValue getTagValuePublic(final String paramString, final SDI paramSDI, final Trade paramTrade,
                                             final BOTransfer paramBOTransfer, final BOMessage paramBOMessage,
                                             final TradeTransferRule paramTradeTransferRule, final DSConnection paramDSConnection) {
        return getTagValue(paramString, paramSDI, paramTrade, paramBOTransfer, paramBOMessage, paramTradeTransferRule,
                paramDSConnection);
    }

    /**
     * return same value than the protected method in the calypso's SwiftUtil
     * class
     *
     * @param paramPartySDI     party sdi
     * @param paramTrade        trade
     * @param paramBOTransfer   transfer
     * @param paramBOMessage    message
     * @param paramDSConnection dsconnection
     * @param paramObject       object
     * @return same value than the protected method in the calypso's SwiftUtil
     * class
     */
    public static TagValue getTagValuePublic(final PartySDI paramPartySDI, final Trade paramTrade,
                                             final BOTransfer paramBOTransfer, final BOMessage paramBOMessage, final DSConnection paramDSConnection,
                                             final Object paramObject) {
        return getTagValue(paramPartySDI, paramTrade, paramBOTransfer, paramBOMessage, paramDSConnection, paramObject);
    }

    /**
     * return same value than the protected method in the calypso's SwiftUtil
     * class
     *
     * @param paramPartySDI     party sdi
     * @param paramTrade        trade
     * @param paramBOTransfer   transfer
     * @param paramBOMessage    message
     * @param paramBoolean      boolean
     * @param paramString       string
     * @param paramDSConnection dsconnection
     * @param paramObject       object
     * @return same value than the protected method in the calypso's SwiftUtil
     * class
     */
    public static TagValue getTagValuePublic(final PartySDI paramPartySDI, final Trade paramTrade,
                                             final BOTransfer paramBOTransfer, final BOMessage paramBOMessage, final Boolean paramBoolean, final boolean use59OptionF,
                                             final String paramString, final DSConnection paramDSConnection, final Object paramObject) {
        return getTagValue(paramPartySDI, paramTrade, paramBOTransfer, paramBOMessage, paramBoolean, use59OptionF, paramString,
                paramDSConnection, paramObject);
    }

    /**
     * return same value than the protected method in the calypso's SwiftUtil
     * class
     *
     * @param paramTrade   trade
     * @param paramBoolean boolean
     * @return same value than the protected method in the calypso's SwiftUtil
     * class
     */
    public static String getPayRecPublic(final Trade paramTrade, final boolean paramBoolean) {
        return getPayRec(paramTrade, paramBoolean);
    }

    /**
     * return same value than the protected method in the calypso's SwiftUtil
     * class
     *
     * @param paramDSConnection dsconnection
     * @param paramObject       object
     * @param paramBook         book
     * @return same value than the protected method in the calypso's SwiftUtil
     * class
     */
    public static LegalEntity getProcessingOrgPublic(final DSConnection paramDSConnection, final Object paramObject,
                                                     final Book paramBook) {
        return getProcessingOrg(paramDSConnection, paramObject, paramBook);
    }

    /**
     * Gets the document version.
     *
     * @param ds  the ds
     * @param msg the msg
     * @return the document version
     */
    @SuppressWarnings("unchecked")
    public static int getDocumentVersion(final DSConnection ds, final BOMessage msg) {
        int version=0;
        if(msg.getLongId()>0) {
            String adviceIdWhere = new StringBuilder("advice_id = ").append(msg.getLongId()).toString();
            try {
                Vector<Document> documents = ds.getRemoteBO().getAdviceDocumentsDataOnly(adviceIdWhere,null,null);
                if(!Util.isEmpty(documents)){
                    version=documents.size();
                }
            } catch (final CalypsoServiceException exc) {
                Log.error("Swift", "Can not calculate AdviceConfig version because a RemoteException: " + exc.getMessage());
                Log.error(SwiftUtilPublic.class, exc); //sonar
            }
        }
        return version;
    }
}
