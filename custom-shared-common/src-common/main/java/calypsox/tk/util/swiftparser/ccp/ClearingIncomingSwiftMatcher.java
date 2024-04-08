package calypsox.tk.util.swiftparser.ccp;

import calypsox.repoccp.ReconCCPConstants;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TransferArray;
import com.calypso.tk.util.swiftparser.SecurityMatcher;

import java.util.*;
import java.util.stream.Stream;

import static calypsox.tk.util.swiftparser.ccp.PlatformUtil.loadClearingCounterParties;

public class ClearingIncomingSwiftMatcher extends SecurityMatcher {

    protected String ccpRefXferAttrName = ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST;

    /**
     * The clearing counterparties IDs list (LGWM,LV4V,5MSR,ECAG)
     */
    protected final List<Integer> clearingCtpy = loadClearingCounterParties();


    @Override
    public Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        Object indexedXfer = null;
        try {
            indexedXfer = Optional.ofNullable(indexWithCOMM(swiftMess)).orElse(indexWithRELA(swiftMess));
        } catch (MessageParseException exc) {
            Log.error(this, exc.getCause());
        }
        return indexedXfer;
    }

    @Override
    public boolean match(ExternalMessage externalMessage, Object object, BOMessage indexedMessage, BOTransfer indexedTransfer, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        return object instanceof BOTransfer;
    }

    @Override
    public Vector getIndexingFields(ExternalMessage externalMessage) {
        return null;
    }


    public Object indexWithCOMM(ExternalMessage swiftMess) throws MessageParseException {
        return Optional.ofNullable(getCOMMRef(swiftMess))
                .map(ref -> this.getTransferForRef(ref, ccpRefXferAttrName, DSConnection.getDefault()))
                .orElse(null);
    }

    public String getCOMMRef(ExternalMessage swiftMess) throws MessageParseException {
        return getRef(swiftMess, "COMM");
    }

    public Object indexWithRELA(ExternalMessage swiftMess) throws MessageParseException {
        return Optional.ofNullable(getRELARef(swiftMess))
                .map(ref -> this.getTransferForRef(ref, ccpRefXferAttrName, DSConnection.getDefault()))
                .orElse(null);
    }

    public String getRELARef(ExternalMessage swiftMess) throws MessageParseException {
        return getRef(swiftMess, "RELA");
    }

    public String getRef(ExternalMessage swiftMess, String refName) throws MessageParseException {
        return ((SwiftMessage) swiftMess).getReferenceByName(refName);
    }

    /**
     * Core method can't choose in case of having multiple transfers for the same reference.
     * This one can.
     *
     * @param ref       the message reference
     * @param attribute the attribute name
     * @param ds        the Data Server connection
     * @return matched transfer
     */
    @Override
    protected BOTransfer getTransferForRef(String ref, String attribute, DSConnection ds) {
        BOTransfer xferForRef = null;
        if (!Util.isEmpty(ref)) {
            try {
                TransferArray transfers = ds.getRemoteBO().getBOTransfers(buildSQLWhere(), buildBindVariableList(attribute, ref));
                if (isSingletonList(transfers)) {
                    xferForRef = transfers.firstElement();
                } else {
                    xferForRef = matchNonSettledTransfer(transfers);
                }
            } catch (CalypsoServiceException exc) {
                Log.error(this, exc);
            }
        }
        return xferForRef;
    }

    private boolean isSingletonList(TransferArray transfers) {
        return Optional.ofNullable(transfers).map(xfers -> xfers.size() == 1)
                .orElse(false);
    }

    private BOTransfer matchNonSettledTransfer(TransferArray transfers) {
        return transferArrayToStream(transfers)
                .filter(this::isNonSettledStatusMatched)
                .findFirst().orElse(null);
    }

    private boolean isNonSettledStatusMatched(BOTransfer xfer) {
        return xfer.getStatus().equals(Status.S_FAILED)
                || xfer.getStatus().equals(Status.S_VERIFIED)
                || xfer.getStatus().equals(Status.S_PENDING);
    }

    protected String buildSQLWhere() {
        String where = " bo_transfer.is_payment = 1 AND transfer_status <> 'CANCELED' AND ";
        where = where + " Exists ( Select 1 FROM xfer_attributes ";
        where = where + " WHERE xfer_attributes.transfer_id = bo_transfer.transfer_id ";
        where = where + " AND xfer_attributes.attr_name = ? ";
        where = where + " AND xfer_attributes.attr_value = ? ";
        where = where + " ) ";
        if (!Util.isEmpty(clearingCtpy)) {
            where += " AND bo_transfer.orig_cpty_id IN " + Util.collectionToSQLString(clearingCtpy) + " ";
        }
        return where;
    }

    private List<CalypsoBindVariable> buildBindVariableList(String attrName, String attrValue) {
        List<CalypsoBindVariable> bindVariables = new ArrayList<>();
        bindVariables.add(new CalypsoBindVariable(12, attrName));
        bindVariables.add(new CalypsoBindVariable(12, attrValue));
        return bindVariables;
    }

    public Stream<BOTransfer> transferArrayToStream(TransferArray transfer) {
        return Optional.ofNullable(transfer)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }
}
