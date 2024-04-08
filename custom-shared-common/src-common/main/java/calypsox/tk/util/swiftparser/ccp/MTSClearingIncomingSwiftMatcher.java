package calypsox.tk.util.swiftparser.ccp;

import calypsox.repoccp.ReconCCPConstants;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class MTSClearingIncomingSwiftMatcher extends ClearingIncomingSwiftMatcher {

    private int mtsEndIndex;
    private boolean useRawReference = false;


    /**
     * Need to check if there's in the swift that shows the productType. Or any generic separator to directly extract
     * MTS ref with the right size.
     * Bond refs have 6 chars
     * Repo refs have 5 chars
     *
     * @param swiftMess the current swift message
     * @param env       the current Pricing Environment
     * @param ds        the connection to Data Server
     * @param dbCon     the connection to Data Base
     * @param errors    the errors
     * @return the index object
     * @throws MessageParseException error
     */
    @Override
    public Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        return Optional.ofNullable(
                        indexRepo(swiftMess, env, ds, dbCon, errors))
                .orElse(
                        Optional.ofNullable(indexBond(swiftMess, env, ds, dbCon, errors))
                                .orElse(indexWithRawReference(swiftMess, env, ds, dbCon, errors))
                );
    }

    public Object indexRepo(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        this.mtsEndIndex = 5;
        return super.index(swiftMess, env, ds, dbCon, errors);
    }

    public Object indexBond(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        this.mtsEndIndex = 6;
        return super.index(swiftMess, env, ds, dbCon, errors);
    }

    public Object indexWithRawReference(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        this.useRawReference = true;
        this.ccpRefXferAttrName = ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST_2;
        return super.index(swiftMess, env, ds, dbCon, errors);
    }

    @Override
    public String getCOMMRef(ExternalMessage swiftMess) throws MessageParseException {
        return extractMTSRef(super.getCOMMRef(swiftMess), mtsEndIndex);
    }

    @Override
    public String getRELARef(ExternalMessage swiftMess) throws MessageParseException {
        return extractMTSRef(super.getRELARef(swiftMess), mtsEndIndex);
    }

    @Override
    public String getRef(ExternalMessage swiftMess, String refName) throws MessageParseException {
        return ((SwiftMessage) swiftMess).getReferenceByName(refName);
    }

    private String extractMTSRef(String rawReference, int endIndex) {
        String mtsRef = "";
        if (!useRawReference && !Util.isEmpty(rawReference) && rawReference.length() > endIndex - 1) {
            mtsRef = rawReference.substring(0, endIndex);
        } else if (useRawReference) {
            mtsRef = rawReference;
        }
        return mtsRef;
    }

}
