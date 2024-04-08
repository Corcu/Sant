package calypsox.tk.util.swiftparser;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;
import com.calypso.tk.util.TransferArray;
import com.calypso.tk.util.swiftparser.MessageMatcher;

import java.util.*;
import java.util.stream.Collectors;

import static com.calypso.tk.util.swiftparser.SecurityMatcher.getTradeType;

public class SwiftStatementItemIndexer {

    private static final String LOG_CATEGORY = SwiftStatementItemIndexer.class.getName();



    public static BOMessage indexStatementItem(SwiftMessage mess, DSConnection ds,   Vector<String> errors) throws MessageParseException {
        if (isMutliReference(mess, "RELA")) {
            errors.add("More than one Reference");
            return null;
        } else {
            String ref = mess.getReferenceByName("RELA");
            long id = getNumeric(ref);

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

                                String delType = mess.getFieldByType("Del Type");
                                String status = mess.getFieldByType("ALL_STATUS");

                                String transStatus = "MT537".equals(mess.getType())?("IPRC//CAND".equals(status)?"CANCELED":"PENDING"):"SETTLED";

                                xfer = findTransfer(xfer, msgSettleDate,  msgNom, delType, transStatus, errors, ds);
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
                            Log.error(LOG_CATEGORY, "Error getting transfer from matched msg", e);
                        }
                    }

                    return mesg;
                } catch (Exception e) {
                    Log.error(LOG_CATEGORY, "Error getting messages", e);
                    errors.add(String.format("Error getting messages %s: %s.", e.getClass().getSimpleName(), e.getMessage()));
                    return null;
                }
            }

            return null;
        }
    }

    private static BOTransfer getTripartyXfer(SwiftMessage mess, long tradeId, DSConnection dsCon) throws CalypsoServiceException, MessageParseException {
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

    private static BOMessage indexTriparty(SwiftMessage message, long tradeId, DSConnection dsCon) {
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


    public static boolean isMutliReference(SwiftMessage swift, String reference) {
        if (swift == null) {
            return false;
        } else {
            List<SwiftFieldMessage> fields = swift.getFields();
            if (Util.isEmpty(fields)) {
                return false;
            } else {
               return fields.stream().filter(f->f.accept(":20C:", ":" + reference + "//", null)).count() >1;
            }
        }
    }

    private static long getNumeric(String ref) {
        if (!Util.isEmpty(ref)) {
            List<Character> digits = ref.chars().mapToObj(i -> (Character) (char) i).filter(Character::isDigit).collect(Collectors.toList());
            if (!Util.isEmpty(digits)) {
                final char[] chars = new char[digits.size()];
                int i = 0;
                for (char c : digits) {
                    chars[i++] = c;
                }
                try {
                    return Util.toLong(new String(chars));
                } catch (NumberFormatException ignore) {
                }
            }
        }
        return 0L;
    }

    private static BOTransfer findTransfer(BOTransfer xfer, JDate msgSettleDate, DisplayValue nom, String delType, String expectedStatus, Vector<String> errors, DSConnection ds) {

        TransferArray candidateXfers = new TransferArray();
        if (Status.S_SPLIT.equals(xfer.getStatus())) {
            try {
                TransferArray siblings = getSiblings(xfer, expectedStatus, new HashSet<>(), errors, ds);
                if (siblings.isEmpty())
                    return xfer;  //index to SPLIT by default
                else {
                    candidateXfers.addAll(siblings);
                }
            } catch (Exception e) {
                String error = String.format("Error getting siblings for SPLIT transfer %s.", xfer);
                Log.error(LOG_CATEGORY, error);
                errors.add(error);
                return xfer;
            }

        } else {
            candidateXfers.add(xfer);
        }
        candidateXfers.sort((t1, t2) -> {
            int comp ="PENDING".equals(expectedStatus)? t1.getValueDate().compareTo(t2.getValueDate()):t1.getSettleDate().compareTo(t2.getSettleDate());
            if (comp == 0) {
                double a = t1.getSettlementAmount() - t2.getSettlementAmount();
                return a < 0 ? -1 : a > 0 ? 1 : 0;
            }
            return comp;
        });

        BOTransfer found = xfer;
        for (BOTransfer candidate : candidateXfers) {
            String xferDelType = "PAY".equals(candidate.getPayReceive()) ? "DAP".equals(candidate.getDeliveryType()) ? "DAP" : "DFP" : "DAP".equals(candidate.getDeliveryType()) ? "RAP" : "RFP";
            if (!Util.isEmpty(delType) && !xferDelType.equals(delType))
                continue;

            if ("PENDING".equals(expectedStatus) && msgSettleDate != null && !candidate.getValueDate().equals(msgSettleDate))
                continue;

            if (!"PENDING".equals(expectedStatus) && msgSettleDate != null && candidate.getSettleDate().after(msgSettleDate))
                break;

            found = candidate;

            if ("PENDING".equals(expectedStatus) && nom != null && Math.abs(nom.get()) == Math.abs(candidate.getNominalAmount()))
                break;

            if (!"PENDING".equals(expectedStatus) && msgSettleDate != null && candidate.getSettleDate().equals(msgSettleDate) && nom != null && Math.abs(nom.get()) == Math.abs(candidate.getNominalAmount()))
                break;


        }
        return found;

    }

    private static TransferArray getSiblings(BOTransfer splitXfer, String expectedStatus, Set<Long> parentIds, Vector<String> errors, DSConnection dsCon) throws CalypsoServiceException {
        if (!parentIds.add(splitXfer.getLongId())) {
            String error = String.format("Cyclical reference detected for SPLIT transfer %s.", splitXfer);
            Log.error(LOG_CATEGORY, error);
            errors.add(error);
            return new TransferArray();
        }
        TransferArray siblingTransfers = dsCon.getRemoteBO()
                .getBOTransfers("start_time_limit = ? AND IS_PAYMENT = 1 AND TRANSFER_TYPE = 'SECURITY'" + ("CANCELED".equals(expectedStatus)?"":" AND TRANSFER_STATUS!='CANCELED'"),
                        Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.LONG, splitXfer.getLongId())));

        TransferArray found = new TransferArray();
        if (siblingTransfers != null && !siblingTransfers.isEmpty()) {
            for (BOTransfer sibling : siblingTransfers) {
                if (Status.S_SPLIT.equals(sibling.getStatus())) {
                    found.addAll(getSiblings(sibling, expectedStatus, parentIds, errors, dsCon));
                } else
                    found.add(sibling);
            }
        } else {
            String error = String.format("Sibling transfer not found for for SPLIT transfer %s.", splitXfer);
            Log.error(LOG_CATEGORY, error);
            errors.add(error);
        }
        return found;
    }

}
