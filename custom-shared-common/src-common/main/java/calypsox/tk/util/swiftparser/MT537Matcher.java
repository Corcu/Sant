package calypsox.tk.util.swiftparser;

import java.util.Optional;
import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;

import calypsox.tk.csdr.CSDRFiFlowTradeBuilder;
import calypsox.tk.swift.formatter.CalypsoAppIdentifier;

/**
 * @author aalonsop
 */
public class MT537Matcher extends com.calypso.tk.util.swiftparser.MT537Matcher {

    private static final String CSDR_MANUAL_XFER_ID = "CSDRManualXferId";
	private final String pRefAttr="PenaltyRef";

    //IN BETA, need to cleanup this
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {

        SwiftFieldMessage isInActive = ((SwiftMessage) swiftMess).getSwiftField("17B", "ACTI", "N");
        if (isInActive != null)
            return null;

        if (isPENA(swiftMess, errors)) {
            deleteRelaIfExists(swiftMess);
            Object indexedObject = super.index(swiftMess, env, ds, dbCon, errors);
            if (indexedObject instanceof Vector) {
                Vector idx = (Vector) indexedObject;
                Vector swifts = ((Vector<?>) idx.get(0));
                Vector objects = ((Vector<?>) idx.get(1));
                for (int i = 0; i < swifts.size(); i++) {
                    SwiftMessage mess = (SwiftMessage) swifts.get(i);
                    Object index = objects.get(i);
                    if (index == null) {
                        BOTransfer indexedXfer = indexCSDRTransfer(mess, ds);
                        if (indexedXfer != null) {
                            objects.set(i, indexedXfer);
                        }
                    }
                }
            }
            return indexedObject;

        } else{

            SwiftMessage swiftMsg  = (SwiftMessage) swiftMess;
            SwiftFieldMessage swiftField = swiftMsg.getSwiftField(swiftMsg.getFields(), ":22H:", ":STST//", (String)null);
            if (swiftField == null) {
                swiftField = swiftMsg.getSwiftField(swiftMsg.getFields(), ":22F:", ":STST//", (String)null);
            }
            String type = swiftField.getValue().substring(swiftField.getValue().lastIndexOf(47) + 1);
            Vector v = this.explode(swiftMsg, type);
            if (Util.isEmpty(v)) {
                Log.debug("MT537Matcher", "Cannot explode message");
                return null;
            } else {
                Log.debug("MT537Matcher", "Nb  exploded messages:" + v.size());
                Vector objects = new Vector();
                Vector indexErrors = new Vector();
                for (Object swiftObj : v ) {

                    Vector<String> localErrors = new Vector<>();
                    SwiftMessage swift = (SwiftMessage)swiftObj;
                    Object obj =   SwiftStatementItemIndexer.indexStatementItem(swift, ds, localErrors);
                    if (obj ==null )
                        obj = this.indexGeneric(swift, ds, dbCon, localErrors);

                    objects.add(obj);
                    indexErrors.add(localErrors);
                }

                Vector ret = new Vector();
                ret.add(v);
                ret.add(objects);
                ret.add(indexErrors);
                return ret;
            }

        }
    }

	@Override
	public Vector explode(SwiftMessage mess, String type) throws MessageParseException {
		//Only when the transfer id is added in a message attribute by user 
		if(mess.getProperty(CSDR_MANUAL_XFER_ID) != null) {
			return addCSDRManualXferId(mess, type);
		}
		
		return super.explode(mess, type);	
	}

	private Object indexPenaltyTrade(SwiftMessage swift, DSConnection ds) {
        try {
            String penaltyRefValue = swift.getReferenceByName("PREF");
            String penalyComRefValue = swift.getReferenceByName("PCOM");
            return getPenaltyTrade(penaltyRefValue, penalyComRefValue, ds);
        } catch (MessageParseException var5) {
            Log.error("MT537Matcher", var5);
            return null;
        }
    }


    public void deleteRelaIfExists(ExternalMessage extMessage) throws MessageParseException {
        if(extMessage instanceof SwiftMessage){
            SwiftMessage msg= (SwiftMessage) extMessage;
            String rela = "RELA";
            if(!Util.isEmpty(msg.getReferenceByName(rela))){
                Vector<SwiftFieldMessage> fields=msg.getFields();
                SwiftFieldMessage fieldToRemove=null;
                for(SwiftFieldMessage field:fields){
                    String relaTag = ":20C:";
                    if(field!=null&& relaTag.equals(field.getTAG())
                    &&field.getValue().contains(rela)){
                        fieldToRemove=field;
                        break;
                    }
                }
                fields.remove(fieldToRemove);
                msg.setFields(fields);
            }
        }
    }

    public BOTransfer indexCSDRTransfer(SwiftMessage swift,DSConnection ds) throws MessageParseException {
        BOTransfer indexedXfer=null;
        String acow = "ACOW";
        String acowRef = swift.getReferenceByName(acow);
        try {
            TradeArray trades = ds.getRemoteTrade().getTradesByKeywordNameAndValue(CSDRFiFlowTradeBuilder.SWIFTSEMETRADEKWD, acowRef);
            for(Object tradeObj:trades) {
                Trade trade= (Trade) tradeObj;
                if (CSDRFiFlowTradeBuilder.isTargetDummyTrade(trade)) {
                    TransferArray xfers = ds.getRemoteBO().getBOTransfers(trade.getLongId(), false);
                    if (xfers!=null) {
                        for(BOTransfer xfer:xfers) {
                            if(!Status.CANCELED.equals(xfer.getStatus().getStatus())) {
                                updatePenaltyTrade(trade, xfer);
                                checkReloadedPenaltyTrade(trade, ds);
                                indexedXfer = xfer;
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        } catch (CalypsoServiceException exc) {
            Log.error(this, exc.getCause());
        }
        return indexedXfer;
    }

    private void updatePenaltyTrade(Trade trade, BOTransfer xfer) throws CalypsoServiceException {
        if(!trade.isMutable()){
            trade=trade.clone();
        }
        Action updateAction = Action.UPDATE;
        if (TradeWorkflow.isTradeActionApplicable(trade, updateAction, DSConnection.getDefault(), null)) {
            trade.setAction(updateAction);
            trade.addKeyword("OriginalTransferId", Optional.ofNullable(xfer)
                    .map(BOTransfer::getLongId).map(String::valueOf).orElse(""));
            DSConnection.getDefault().getRemoteTrade().save(trade);
        }
    }

    private void checkReloadedPenaltyTrade(Trade trade,DSConnection dsConnection) {
        if(trade!=null){
            try {
                Trade reloadedTrade=dsConnection.getRemoteTrade().getTrade(trade.getLongId());
                String pref=Optional.ofNullable(reloadedTrade).map(t->t.getKeywordValue(pRefAttr)).orElse("");
                long tradeId=Optional.ofNullable(reloadedTrade).map(Trade::getLongId).orElse(0L);
                if(!Util.isEmpty(pref)){
                    Log.debug(this,"External Penalty SimpleTransfer: "+tradeId+
                            " penalty reference succesfully updated");
                }
            } catch (CalypsoServiceException exc) {
                Log.error(this,exc.getCause());
            }
        }
    }

    @Override
    protected long getNumeric(String ref) {
        long res=0L;
        if(isNotExternalACOW(ref)){
            res=super.getNumeric(ref);
        }
        return res;
    }


    private boolean isNotExternalACOW(String ref){
        String cyoStr=CalypsoAppIdentifier.CYO.toString();
        return ref!=null&& ref.contains(cyoStr);
    }


	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean isPENA(ExternalMessage swiftMess, Vector errors) {
        SwiftMessage mess = (SwiftMessage) swiftMess;
        SwiftFieldMessage swiftField = mess.getSwiftField(mess.getFields(), ":22H:", ":STST//", (String)null);
        if (swiftField == null) {
            swiftField = mess.getSwiftField(mess.getFields(), ":22F:", ":STST//", (String)null);
        }
        if (swiftField == null) {
            errors.add("Tag 22F:STST not found");
            return false;
        }
        String type = swiftField.getValue().substring(swiftField.getValue().lastIndexOf(47) + 1);
        return "PENA".equalsIgnoreCase(type) ? true : false;
    }

	@SuppressWarnings("rawtypes")
	@Override
	protected BOTransfer getTransferForACOWRef(SwiftMessage mess, DSConnection ds) {
		Vector errors = new Vector();
		String xferId = (String) mess.getProperty(CSDR_MANUAL_XFER_ID);
		if (isPENA(mess, errors) && !Util.isEmpty(xferId)) {
			try {
				return ds.getRemoteBackOffice().getBOTransfer(Long.valueOf(xferId));
			} catch (CalypsoServiceException e) {
				Log.error(this, "Error getting xfer: " + xferId, e);
			} catch (NumberFormatException e) {
				Log.error(this, "Error parsing number value for xfer id: " + xferId, e);
			}
		}
		if (!Util.isEmpty(errors)) {
			Log.error(this, errors.toString());
		}
		return super.getTransferForACOWRef(mess, ds);

	}
	
	 private Vector addCSDRManualXferId(SwiftMessage mess, String type) throws MessageParseException {
	    	
    //Add the Manual Xfer id to a Swift property because the object miss de BOMessage reference inside the core explode method
        Vector v =super.explode(mess, type);
        SwiftMessage sw =(SwiftMessage) v.get(0);
        sw.addProperty(CSDR_MANUAL_XFER_ID, mess.getProperty(CSDR_MANUAL_XFER_ID));
        Vector newV = new Vector();
        newV.add(sw);
        return newV;
    }



}
