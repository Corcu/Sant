package calypsox.tk.util.swiftparser;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;
import com.calypso.tk.util.TransferArray;
import com.calypso.tk.util.swiftparser.SecurityMatcher;

import java.util.*;


public class MT536Matcher extends SecurityMatcher {


    @Override
    public Object index(ExternalMessage extMsg, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        SwiftMessage swiftMess = (SwiftMessage) extMsg;
        SwiftFieldMessage isInActive = ((SwiftMessage) extMsg).getSwiftField(":17B:", "ACTI", "N");
        if (isInActive != null)
            return null;

        Vector<SwiftMessage> v = explode(swiftMess);
        if (Util.isEmpty(v)) {
            Log.debug("MT536Matcher", "Can not explode message");
            return null;
        } else {
            Log.debug("MT536Matcher", "Num exploded messages:" + v.size());
            Vector objects = new Vector();
            Vector indexErrors = new Vector();

            for ( SwiftMessage swiftMsg : v) {

                Vector<String> localErrors = new Vector<>();

                Object obj =   SwiftStatementItemIndexer.indexStatementItem(swiftMsg, ds, errors);
                if (obj ==null )
                    obj = this.indexGeneric(swiftMsg, ds, dbCon, errors);

                objects.add(obj);

            //    Object  obj = this.customIndexMT536(swiftMsg, ds, dbCon, localErrors);

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

    @Override
    public boolean match(ExternalMessage externalMessage, Object object, BOMessage indexedMessage, BOTransfer indexedTransfer, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        return true;
    }

    @Override
    public Vector getIndexingFields(ExternalMessage externalMessage) {
        return null;
    }
/*
    protected BOMessage customIndexMT536(SwiftMessage mess, DSConnection ds, Object dbCon, Vector<String> errors) throws MessageParseException {
        if (this.isMutliReference(mess, "RELA")) {
            errors.add("More than one Reference");
            return null;
        } else {
            String ref = mess.getReferenceByName("RELA");
            long id = 0L;

            try{
                id = Util.toLong(ref);
            } catch(Exception var27){
                id = 0L;
            }
            if (id == 0L){
                return null;
            }

            if (id > 0) {
                try {
                    BOMessage mesg;

                    String tradeType = getTradeType(mess);

                    if ("TRPO".equals(tradeType)) {
                        mesg = indexTriparty(mess, id, ds);
                        if (mesg != null) {
                            BOTransfer xfer = getTripartyXfer(mess, id, ds);
                            if (xfer != null) {
                                mesg.setTransferLongId(xfer.getLongId());
                                mesg.setXferVersion(xfer.getVersion());
                                mesg.setBookId(xfer.getBookId());
                                if (xfer.getTradeLongId() > 0) {
                                    mesg.setTradeLongId(xfer.getTradeLongId());
                                    mesg.setTradeVersion(ds.getRemoteTrade().getTrade(xfer.getTradeLongId()).getVersion());
                                }
                            }
                            return mesg;
                        }
                    }


                    mesg = ds.getRemoteBO().getMessage(id);
                    if (mesg == null || mesg.getExternalB()) {
                        errors.add("No  Msg found for Reference " + ref);
                        return null;
                    }

                    if (mesg.getTransferLongId() > 0L) {
                        try {
                            BOTransfer xfer = ds.getRemoteBO().getBOTransfer(mesg.getTransferLongId());
                            if (xfer != null) {
                                JDate msgSettleDate = (JDate) mess.getDate("Settle Date");
                                DisplayValue msgNom = mess.getDisplayAmount("Nominal Amount");

                              String delType =  mess.getFieldByType("Del Type");

                                xfer = this.findTransfer(xfer, msgSettleDate, msgNom,delType, errors,  ds);
                                if (xfer != null) {
                                    if (!mesg.isMutable()) {
                                        mesg = (BOMessage) mesg.clone();
                                    }

                                    mesg.setTransferLongId(xfer.getLongId());
                                    mesg.setXferVersion(xfer.getVersion());
                                    mesg.setBookId(xfer.getBookId());
                                }
                            }
                        } catch (Exception e) {
                            Log.error(this, "Error getting transfer from matched msg", e);
                        }
                    }

                    return mesg;
                } catch (Exception e) {
                    Log.error(this, "Error getting messages", e);
                    errors.add(String.format("Error getting messages %s: %s.", e.getClass().getSimpleName(), e.getMessage()));
                    return null;
                }
            }
            if (Util.isEmpty(ref)) {
                errors.add("Related reference RELA not found.");
            } else {
                errors.add("Numeric id expected in RELA.");
            }
            return null;
        }
    }

    private BOTransfer getTripartyXfer(SwiftMessage mess, long tradeId, DSConnection dsCon) throws CalypsoServiceException, MessageParseException {
        TransferArray xfers = dsCon.getRemoteBO().getBOTransfers(tradeId, false);

        //  :98A::ESET//20240220

        SwiftFieldMessage eset = mess.getSwiftField("98A", "ESET", null);
        JDate setDate = eset != null ? SwiftParserUtil.getCalypsoDate(eset.getValue()) : null;

        Optional<BOTransfer> tradeXfer = xfers.stream().filter(x -> !Status.S_SPLIT.equals(x.getStatus()) && (!x.getSettleDate().after(setDate))).min((t1, t2) -> t2.getSettleDate().compareTo(t1.getSettleDate()));
        if (tradeXfer.isPresent()) {
            return tradeXfer.get().getNettedTransferLongId() > 0 ? dsCon.getRemoteBO().getBOTransfer(tradeXfer.get().getNettedTransferLongId()) : tradeXfer.get();

        }
        return null;
    }

    private BOMessage indexTriparty(SwiftMessage message, long tradeId, DSConnection dsCon) {
        try {
            MessageArray tradeMessages = dsCon.getRemoteBO().getMessages(
                    "bo_message.trade_id= ? " +
                            " AND bo_message.external_b = 0 " +
                            " AND bo_message.message_status!='CANCELED' " +
                            " AND bo_message.template_name like 'MT527%'", Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.LONG, tradeId)));

            if (tradeMessages == null || tradeMessages.isEmpty())
                return null;

            return Arrays.stream(tradeMessages.getMessages()).sorted((m1, m2) -> m1.getCreationSystemDate().compareTo(m2.getCreationDate())).filter(m -> Arrays.stream(tradeMessages.getMessages()).noneMatch(lm -> lm.getLinkedLongId() == m.getLongId())).findFirst().orElse(null);
        } catch (CalypsoServiceException e) {
            throw new RuntimeException(e);
        }
    }

*/
    protected boolean isMutliReference(SwiftMessage swift, String reference) {
        if (swift == null) {
            return false;
        } else {
            List<SwiftFieldMessage> fields = swift.getFields();
            if (Util.isEmpty(fields)) {
                return false;
            } else {
                int count = 0;
                Iterator it = fields.iterator();

                while (it.hasNext()) {
                    SwiftFieldMessage swiftText = (SwiftFieldMessage) it.next();
                    if (swiftText.accept(":20C:", ":" + reference + "//", (String) null)) {
                        ++count;
                    }
                }

                return count > 1;
            }
        }
    }

    protected long getNumeric(String ref) {
        if (!Util.isEmpty(ref)) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < ref.length(); ++i) {
                if (Character.isDigit(ref.charAt(i))) {
                    sb.append(ref.charAt(i));
                }
            }

            if (sb.length() > 0) {
                try {
                    return Util.toLong(sb.toString());
                } catch (Exception exception) {
                }
            }
        }

        return 0L;
    }
/*
    protected BOTransfer findTransfer(BOTransfer xfer, JDate msgSettleDate , DisplayValue nom, String delType, Vector<String> errors, DSConnection ds) {

        TransferArray candidateXfers = new TransferArray();
        if (Status.S_SPLIT.equals(xfer.getStatus())) {
            try {
                TransferArray siblings = getSiblings(xfer, new HashSet<>(), errors, ds);
                if (siblings.isEmpty())
                    return xfer;  //index to SPLIT by default
                else {
                    candidateXfers.addAll(siblings);
                }
            } catch (Exception e) {
                String error = String.format("Error getting siblings for SPLIT transfer %s.", xfer);
                Log.error(this, error);
                errors.add(error);
                return xfer;
            }

        } else {
             candidateXfers.add(xfer);
        }
        candidateXfers.sort((t1, t2) -> {
            int comp =  t1.getSettleDate().compareTo(t2.getSettleDate());
            if (comp ==0) {
                double a =  t1.getSettlementAmount() -t2.getSettlementAmount();
                return a<0?-1:a>0?1:0;
            }
            return comp;
        });

        BOTransfer found = xfer;
        for (BOTransfer candidate : candidateXfers) {
            String xferDelType = "PAY".equals(candidate.getPayReceive())?"DAP".equals(candidate.getDeliveryType())?"DAP":"DFP":"DAP".equals(candidate.getDeliveryType())?"RAP":"RFP";
            if (!Util.isEmpty(delType) && !xferDelType.equals(xferDelType))
                continue;

            if (msgSettleDate!= null && candidate.getSettleDate().after(msgSettleDate)   )
                break;

            found = candidate;

            if (msgSettleDate!= null && candidate.getSettleDate().equals(msgSettleDate) &&  nom != null && Math.abs(nom.get()) == Math.abs(candidate.getNominalAmount()))
                break;


        }
        return found;

    } */

    /*

    private TransferArray getSiblings(BOTransfer splitXfer, Set<Long> parentIds, Vector<String> errors, DSConnection dsCon) throws CalypsoServiceException {
        if (!parentIds.add(splitXfer.getLongId())) {
            String error = String.format("Cyclical reference detected for SPLIT transfer %s.", splitXfer);
            Log.error(this, error);
            errors.add(error);
            return new TransferArray();
        }
        TransferArray  siblingTransfers = dsCon.getRemoteBO()
                .getBOTransfers("start_time_limit = ? AND transfer_status != 'CANCELED' AND IS_PAYMENT = 1 AND TRANSFER_TYPE = 'SECURITY'",
                        Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.LONG,splitXfer.getLongId())));

        TransferArray found = new TransferArray();
        if (siblingTransfers!=null && !siblingTransfers.isEmpty()) {
            for (BOTransfer sibling: siblingTransfers) {
                if (Status.S_SPLIT.equals(sibling.getStatus())) {
                    found.addAll(getSiblings(sibling, parentIds, errors, dsCon));
                }  else
                    found.add(sibling);
            }
        } else {
            String error = String.format("Sibling transfer not found for for SPLIT transfer %s.", splitXfer);
            Log.error(this, error);
            errors.add(error);
        }
        return found;
    }
*/

    /*
    protected boolean isSplitOrfailed(BOTransfer xfer) {
        return !xfer.getStatus().equals(Status.S_CANCELED) && (Status.isFailed(xfer.getStatus()) || xfer.getStatus().equals(Status.S_SPLIT));
    }
*/
    public Vector<SwiftMessage> explode(ExternalMessage swiftMess) {
        Vector<SwiftMessage> v = new Vector<>();
        SwiftMessage mess = (SwiftMessage) swiftMess;
  //      Hashtable hash = new Hashtable();
        Vector<SwiftFieldMessage> headerFields = new Vector<>();

        for (int i = 0; i < mess.getFields().size(); ++i) {
            SwiftFieldMessage swiftText = mess.getFields().elementAt(i);
            headerFields.add(swiftText);
            if (swiftText.getTAG().equals(":16R:") && swiftText.getValue().equals("FIN")) {
                break;
            }
        }

        Vector<SwiftFieldMessage> trailerFields = new Vector<>();
        trailerFields.add(new SwiftFieldMessage((byte) 79, ":16S:", (String) null, "FIN"));
        trailerFields.add(new SwiftFieldMessage((byte) 79, ":16S:", (String) null, "SUBSAFE"));
        int nbCusip = mess.getNumberOfSequence(mess.getFields(), ":16R:", ":16S:", "FIN");

        for (int i = 0; i < nbCusip; ++i) {
            Vector<SwiftFieldMessage> cusipFields = SwiftMessage.getSwiftSequence(mess.getFields(), ":16R:", ":16S:", "FIN", i + 1);
            SwiftFieldMessage cusipField = cusipFields.get(1);
            int nbTran = mess.getNumberOfSequence(cusipFields, ":16R:", ":16S:", "TRAN");

            for (int j = 0; j < nbTran; ++j) {
                SwiftMessage swift = new SwiftMessage();

                try {
                    swift = (SwiftMessage) mess.clone();
                } catch (CloneNotSupportedException e) {
                    Log.error(this, e);
                }

                Vector<SwiftFieldMessage> fields = new Vector<>();
                swift.setFields(fields);
                fields.addAll(headerFields);
                fields.add(cusipField);
                Vector<SwiftFieldMessage> transactionFields = SwiftMessage.getSwiftSequence(cusipFields, ":16R:", ":16S:", "TRAN", j + 1);
                fields.addAll(transactionFields);
                fields.addAll(trailerFields);
                StringBuffer buff = new StringBuffer(swift.getText());
                SwiftMessage.stripExtraInfo(buff);
                Log.debug(this, "in explode add swift \n" + buff.toString());
                v.add(swift);
            }
        }
        return v;

    }
}
