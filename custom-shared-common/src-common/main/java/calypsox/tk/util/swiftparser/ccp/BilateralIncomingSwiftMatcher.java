package calypsox.tk.util.swiftparser.ccp;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;
import com.calypso.tk.util.TransferArray;
import com.calypso.tk.util.swiftparser.SecurityMatcher;
import org.jfree.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * BilateralIncomingSwiftMatcher gets the transfer of the MT541 and MT543 by ISIN and settle date
 *
 * @author Ruben Garcia
 */
public class BilateralIncomingSwiftMatcher extends SecurityMatcher {

    /**
     * Msg attribute, true if ECCNettingProposal
     */
    public static final String IS_NETTING_ECC_PROPOSAL = "ECCNettingProposal";

    @Override
    public Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        if (isECCNettingProposal(swiftMess)) {
            return getBOTransferByISINAndSettleDate(ds, swiftMess);
        }
        return null;
    }

    @Override
    public boolean match(ExternalMessage externalMessage, Object object, BOMessage indexedMessage, BOTransfer indexedTransfer, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        return object instanceof BOTransfer;
    }

    @Override
    public Vector getIndexingFields(ExternalMessage externalMessage) {
        return null;
    }

    /**
     * Check if swift message is for ECC netting proposal
     *
     * @param externalMessage the swift incoming message
     * @return true if is ECC netting proposal
     */
    public static boolean isECCNettingProposal(ExternalMessage externalMessage) {
        if (externalMessage instanceof SwiftMessage) {
            SwiftMessage sw = (SwiftMessage) externalMessage;
            return sw.getSwiftField(":22F:", ":SETR//NETT", "") != null &&
                    sw.getSwiftField(":22F:", ":CCPT//YCCP", "") != null &&
                    sw.getSwiftField(":95P:", ":TRAG//MEFFESBBCRF", "") != null;
        }
        return false;
    }

    /**
     * Get the transfer related by ISIN and settle date
     *
     * @param dsCon           the connection to Data Server
     * @param externalMessage the incoming swift message
     * @return the BOTransfer
     */
    private BOTransfer getBOTransferByISINAndSettleDate(DSConnection dsCon, ExternalMessage externalMessage) {
        if (dsCon != null && externalMessage instanceof SwiftMessage) {
            SwiftMessage sw = (SwiftMessage) externalMessage;
            String isin = getISINFromSwiftMessage(sw);
            JDate settleDate = getSettleDateFromSwiftMessage(sw);
            JDate tradeDate = getTradeDateFromSwiftMessage(sw);
            if (!Util.isEmpty(isin) && settleDate != null) {
                TransferArray array = getBOTransfersByIsinAndSettleDate(dsCon, isin, settleDate, tradeDate, sw.getType());
                if (!Util.isEmpty(array)) {
                    if (array.size() == 1) {
                        return array.get(0);
                    } else {
                        return filterTransfers(dsCon, sw, array);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Filter transfer array
     * -By status failed, verified or pending
     * -By linked message, transfers that do not have the same message linked
     * -By nominal amount
     *
     * @param dsCon the Data Server connection
     * @param msg   the current SwiftMessage
     * @param array the transfer array to filter
     * @return the BOTransfer
     */
    private BOTransfer filterTransfers(DSConnection dsCon, SwiftMessage msg, TransferArray array) {
        if (!Util.isEmpty(array) && dsCon != null && msg != null) {
            List<BOTransfer> nonSettled = array.stream().filter(this::isNonSettledStatusMatched).collect(Collectors.toList());
            if (!Util.isEmpty(nonSettled)) {
                if (nonSettled.size() == 1) {
                    return nonSettled.get(0);
                } else {
                    List<BOTransfer> noHasMsg = nonSettled.stream().filter(t -> !hasMessagesSameType(dsCon, t, msg)).collect(Collectors.toList());
                    if (!Util.isEmpty(noHasMsg)) {
                        if (noHasMsg.size() == 1) {
                            return noHasMsg.get(0);
                        } else {
                            Double amount = getNominalAmount(msg);
                            if (amount != null) {
                                List<BOTransfer> sameNominalAmt = noHasMsg.stream().filter(t -> t.getNominalAmount() == amount).collect(Collectors.toList());
                                if (!Util.isEmpty(sameNominalAmt)) {
                                    return sameNominalAmt.get(0);
                                }else{
                                    return noHasMsg.get(0);
                                }
                            }
                        }
                    } else {
                        Double amount = getNominalAmount(msg);
                        if (amount != null) {
                            List<BOTransfer> sameNominalAmt = nonSettled.stream().filter(t -> t.getNominalAmount() == amount).collect(Collectors.toList());
                            if (!Util.isEmpty(sameNominalAmt)) {
                                return sameNominalAmt.get(0);
                            }
                        }
                    }
                }
                return nonSettled.get(0);
            }
        }
        return null;
    }

    /**
     * Get if BOTransfer has the same linked message type
     *
     * @param dsCon    the Data Server connection
     * @param transfer the selected BOTransfer
     * @param msg      the current Swift Message
     * @return true if the BOTransfer has the same type linked BOMessage
     */
    private boolean hasMessagesSameType(DSConnection dsCon, BOTransfer transfer, SwiftMessage msg) {
        String templateName = msg.getType();
        if (msg.isIncoming() && !Util.isEmpty(templateName)) {
            String incommingType = LocalCache.getDomainValueComment(dsCon, "incomingType", msg.getType());
            if (!Util.isEmpty(incommingType)) {
                String where = " message_type = ? and template_name = ? and transfer_id = ? ";
                List<CalypsoBindVariable> v = new ArrayList<>();
                v.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, incommingType));
                v.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, templateName));
                v.add(new CalypsoBindVariable(CalypsoBindVariable.LONG, transfer.getLongId()));
                try {
                    MessageArray m = dsCon.getRemoteBO().getMessages(where, v);
                    return m != null && !m.isEmpty();
                } catch (CalypsoServiceException e) {
                    Log.error(this, e);
                }
            }
        }
        return false;
    }

    /**
     * Get Swift Message nominal amount field
     *
     * @param msg the swift message
     * @return the nominal amount
     */
    private Double getNominalAmount(SwiftMessage msg) {
        DisplayValue moneyD;
        try {
            moneyD = msg.getDisplayAmount("Nominal Amount");
        } catch (MessageParseException e) {
            Log.error(this, e);
            return null;
        }
        if (moneyD != null) {
            return moneyD.get();
        }
        return null;
    }

    /**
     * Build the query and get a list of transfers by ISIN and SettleDate with netting
     * type CCP_Counterparty and status not in CANCELED
     *
     * @param dsCon      the Data Server connection
     * @param isin       the ISN
     * @param settleDate the transfer settle date
     * @param tradeDate  the transfer trade date
     * @param type       the message type
     * @return the list of transfers
     */
    private TransferArray getBOTransfersByIsinAndSettleDate(DSConnection dsCon, String isin, JDate settleDate, JDate tradeDate, String type) {
        String where = " bo_transfer.is_payment = 1 AND transfer_status <> 'CANCELED' ";
        List<CalypsoBindVariable> variables = new ArrayList<>();
        if (!Util.isEmpty(type)) {
            if ("MT541".equals(type)) {
                where += " AND payreceive_type = ? ";
                variables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "RECEIVE"));
            } else if ("MT543".equals(type)) {
                where += " AND payreceive_type = ? ";
                variables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "PAY"));
            }
        }
        if (!settleDate.equals(tradeDate)) {
            where += " AND ( netting_key like ? or netting_key like ? ) ";
            variables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "CCP_CounterParty%"));
            variables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "CCP_Counterparty%"));
        }
        where += " AND  settle_date = ? AND trade_date = ? AND bo_transfer.product_id IN (SELECT product_id " +
                "FROM product_sec_code WHERE sec_code = ? AND  code_value_ucase = ? ) ";
        variables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, settleDate));
        variables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, tradeDate));
        variables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "ISIN"));
        variables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, isin));
        try {
            return dsCon.getRemoteBackOffice().getBOTransfers(where, variables);
        } catch (CalypsoServiceException e) {
            Log.error(this, e);
        }
        return null;
    }

    /**
     * Get the ISIN field of the Swift Message
     *
     * @param msg the incoming Swift Message
     * @return the ISIN value
     */
    private String getISINFromSwiftMessage(SwiftMessage msg) {
        SwiftFieldMessage isin = msg.getSwiftField(":35B:", "ISIN", "");
        return isin != null ? isin.getValue().replace("ISIN", "").replaceAll("\\s+", "") : null;
    }

    /**
     * Get the Settle Date field of the Swift Message
     *
     * @param msg the incoming Swift Message
     * @return the Settle Date value
     */
    private JDate getSettleDateFromSwiftMessage(SwiftMessage msg) {
        SwiftFieldMessage settleDate = msg.getSwiftField(":98A:", ":SETT//", "");
        if (settleDate != null) {
            String dateValue = settleDate.getValue().replace(":SETT//", "").replaceAll("\\s+", "");
            try {
                return SwiftParserUtil.getCalypsoDate(dateValue);
            } catch (MessageParseException e) {
                Log.error(this, e);
            }
        }
        return null;
    }

    /**
     * Get trade date from swift message
     *
     * @param msg the swift message
     * @return the trade date
     */
    private JDate getTradeDateFromSwiftMessage(SwiftMessage msg) {
        SwiftFieldMessage settleDate = msg.getSwiftField(":98A:", ":TRAD//", "");
        if (settleDate != null) {
            String dateValue = settleDate.getValue().replace(":TRAD//", "").replaceAll("\\s+", "");
            try {
                return SwiftParserUtil.getCalypsoDate(dateValue);
            } catch (MessageParseException e) {
                Log.error(this, e);
            }
        }
        return null;
    }


    /**
     * Filter non settled transfer
     *
     * @param xfer the current transfer
     * @return true if not settled
     */
    private boolean isNonSettledStatusMatched(BOTransfer xfer) {
        return xfer.getStatus().equals(Status.S_FAILED)
                || xfer.getStatus().equals(Status.S_VERIFIED)
                || xfer.getStatus().equals(Status.S_PENDING);
    }
}
