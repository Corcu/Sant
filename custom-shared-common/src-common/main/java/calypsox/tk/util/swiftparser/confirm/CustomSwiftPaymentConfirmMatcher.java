package calypsox.tk.util.swiftparser.confirm;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TransferArray;
import com.calypso.tk.util.swiftparser.SecurityMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 * Only applies for swift payment security confirmation msgs (MT545,MT547)
 * BAs told that core's matching behaviour is not correct, that's why this class was developed
 */
public class CustomSwiftPaymentConfirmMatcher {

    public Object index(SwiftMessage mess, DSConnection ds) throws MessageParseException, CalypsoServiceException, CloneNotSupportedException {
        String msgFunction = SecurityMatcher.getMessageFunction(mess);
        Object indexedObject;
        if ("RVSL".equals(msgFunction)) {
            indexedObject=this.indexRVSL(mess, ds);
        }else{
            indexedObject=index(mess,ds,true);
        }
        return indexedObject;
    }

    /**
     * For reversals where the xfer to match is in SETTLED status
     * @param mess
     * @param ds
     * @return
     * @throws MessageParseException
     * @throws CalypsoServiceException
     * @throws CloneNotSupportedException
     */
    private Object indexRVSL(SwiftMessage mess, DSConnection ds) throws MessageParseException, CalypsoServiceException, CloneNotSupportedException {
        return index(mess,ds,false);
    }

    private Object index(SwiftMessage mess, DSConnection ds, boolean lookForSplittedOfFailedXfer) throws MessageParseException, CalypsoServiceException, CloneNotSupportedException, NumberFormatException {
        String ref = mess.getReferenceByName("RELA");
        long msgId=Long.parseLong(ref);

        BOMessage indexedMsg=ds.getRemoteBO().getMessage(msgId);
        long xferId= Optional.ofNullable(indexedMsg).map(BOMessage::getTransferLongId).orElse(0L);
        if(xferId>0L){
            BOTransfer origXfer=ds.getRemoteBO().getBOTransfer(xferId);
            if(origXfer!=null){
                BOTransfer finalXfer=matchQuantityAndFindTransfer(origXfer,ds,mess,lookForSplittedOfFailedXfer);
                if (finalXfer != null) {
                    if (!indexedMsg.isMutable()) {
                        indexedMsg = (BOMessage)indexedMsg.clone();
                    }
                    indexedMsg.setTransferLongId(finalXfer.getLongId());
                    indexedMsg.setBookId(finalXfer.getBookId());
                }
            }
        }
        return indexedMsg;
    }

    protected BOTransfer matchQuantityAndFindTransfer(BOTransfer orig, DSConnection ds,SwiftMessage mess, boolean lookForSplittedOrFailedXfer) throws MessageParseException {
        BOTransfer parent = orig;
        double msgNominal=Optional.ofNullable(mess.getAmount("Nominal Amount")).orElse(0.0D);
        while(parent != null && Status.S_SPLIT.equals(parent.getStatus())) {
            List<CalypsoBindVariable> bindVariables = new ArrayList<>();
            bindVariables.add(new CalypsoBindVariable(3000, parent.getLongId()));
            String where = "start_time_limit = ? ";
            TransferArray siblingTransfers = null;

            try {
                siblingTransfers = ds.getRemoteBO().getBOTransfers(where, bindVariables);
            } catch (CalypsoServiceException exc) {
                Log.error(this, exc);
            }

            int count = 0;
            boolean isQuantityMatched=false;
            if (siblingTransfers != null) {
                for(int i = 0; i < siblingTransfers.size(); ++i) {
                    BOTransfer siblingTransfer = siblingTransfers.get(i);
                    if (isSameQuantity(msgNominal, siblingTransfer.getNominalAmount())) {
                        if (isXferOnExpectedStatus(siblingTransfer, lookForSplittedOrFailedXfer)) {
                            ++count;
                            parent = siblingTransfer;
                            isQuantityMatched=true;
                        }
                    }else if (isXferOnExpectedStatus(siblingTransfer, true)&&!isQuantityMatched) {
                            ++count;
                            parent = siblingTransfer;
                        }
                    }
            }

            if (count != 1) {
                return orig;
            }
        }

        return parent;

    }

    private boolean isSameQuantity(double msgNominal, double xferNominal){
        double error = 0.0000001d;
        return Math.abs(msgNominal - xferNominal) < error;
    }

    private boolean isXferOnExpectedStatus(BOTransfer xfer,boolean lookForSplittedOfFailedXfer){
        return lookForSplittedOfFailedXfer?this.isSplitOrfailed(xfer):this.isNotSplitOrfailed(xfer);
    }

    protected boolean isNotSplitOrfailed(BOTransfer xfer) {
       return !isSplitOrfailed(xfer);
    }

    protected boolean isSplitOrfailed(BOTransfer xfer) {
        return !xfer.getStatus().equals(Status.S_CANCELED) && (Status.isFailed(xfer.getStatus()) || xfer.getStatus().equals(Status.S_SPLIT));
    }
}
