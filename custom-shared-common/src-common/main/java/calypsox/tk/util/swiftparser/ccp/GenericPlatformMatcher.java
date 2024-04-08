package calypsox.tk.util.swiftparser.ccp;

import calypsox.repoccp.ReconCCPConstants;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.SQLQuery;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.TransferArray;
import com.calypso.tk.util.swiftparser.SecurityMatcher;
import org.jfree.util.Log;

import java.util.*;
import java.util.stream.Collectors;

import static calypsox.repoccp.ReconCCPConstants.TOLERANCE;
import static calypsox.tk.util.swiftparser.ccp.PlatformUtil.loadClearingCounterParties;

/**
 * GenericPlatformMatcher index incoming MT54X by generic logic
 *
 * @author Ruben Garcia
 */
public class GenericPlatformMatcher implements PlatformMatcher {

    /**
     * The clearing counterparties IDs list (LGWM,LV4V,5MSR,ECAG)
     */
    protected final List<Integer> clearingCtpy = loadClearingCounterParties();

    @Override
    public Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector<String> errors)
            throws MessageParseException {
        if (swiftMess instanceof SwiftMessage && ds != null) {
            if (errors == null) {
                errors = new Vector<>();
            }
            TransferArray result = indexBySettlementReferenceInstructed(swiftMess, env, ds, dbCon, errors);
            if (Util.isEmpty(result)) {
                result = indexByBOTransferFields(swiftMess, env, ds, dbCon, errors);
            }
            if (!Util.isEmpty(result)) {
                if (result.size() == 1) {
                    return result.get(0);
                } else {
                    StringBuilder errorMsg = new StringBuilder("More than one transfer found that matches [ ");
                    result.forEach(t -> errorMsg.append(t.getLongId()).append(" "));
                    errorMsg.append("]");
                    errors.add(errorMsg.toString());
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public TransferArray indexBySettlementReferenceInstructed(ExternalMessage swiftMess, PricingEnv env, DSConnection ds,
                                                              Object dbCon, Vector<String> errors) throws MessageParseException {
        return null;
    }

    @Override
    public TransferArray indexByBOTransferFields(ExternalMessage swiftMess, PricingEnv env, DSConnection ds,
                                                 Object dbCon, Vector<String> errors) {
        if (swiftMess instanceof SwiftMessage && ds != null && !Util.isEmpty(swiftMess.getType())) {
            SwiftMessage message = (SwiftMessage) swiftMess;
            if ("MT544,MT545,MT546,MT547".contains(message.getType())) {
                if (isPartialSettle(message)) {
                    return indexMT544toMT547PartialSettleTransfers(message, ds);
                } else {
                    return indexMT544toMT547TotalConciliationTransfers(message, ds);
                }
            } else if ("MT540,MT541,MT542,MT543".contains(message.getType())) {
                return indexMT540toMT543Transfers(message, ds);
            }
        }
        return null;
    }

    /**
     * Get the common reference of the swift incoming message MT54X
     *
     * @param swiftMess the incoming MT54X
     * @return the COMM reference
     * @throws MessageParseException error
     */
    protected String getCOMMRef(ExternalMessage swiftMess) throws MessageParseException {
        return getRef(swiftMess, "COMM");
    }

    /**
     * Get the rela reference of the swift incoming message MT54X
     *
     * @param swiftMess the incoming MT54X
     * @return the RELA reference
     * @throws MessageParseException error
     */
    protected String getRELARef(ExternalMessage swiftMess) throws MessageParseException {
        return getRef(swiftMess, "RELA");
    }

    /**
     * Get the reference filed value of the incoming MT54X
     *
     * @param swiftMess the incoming MT54X
     * @param refName   the reference name
     * @return the reference value
     * @throws MessageParseException error
     */
    private String getRef(ExternalMessage swiftMess, String refName) throws MessageParseException {
        return ((SwiftMessage) swiftMess).getReferenceByName(refName);
    }


    /**
     * Filter the BOTransfers by attributes SettlementReferenceInstructed and SRIPlatform
     *
     * @param sriValue    the SettlementReferenceInstructed attribute value
     * @param sriPlatform the SRIPlatform attribute value
     * @return the query
     */
    protected SQLQuery filterBySRIValueAndPlatform(String sriValue, String sriPlatform) {
        SQLQuery query = buildSQLQuery();
        query = filterBySettlementReferenceInstructed(query, sriValue);
        return filterBySettlementReferenceInstructedPlatform(query, sriPlatform);
    }

    /**
     * Build the default SQLQuery, filter settle transfer,
     * status VERIFIED/FAILED and not clearing counterparties
     *
     * @return the default SQLQuery
     */
    protected SQLQuery buildSQLQuery() {
        SQLQuery query = new SQLQuery();
        query = filterBySettleTransfers(query);
        return filterByNotClearingCounterparties(query);
    }

    /**
     * Filter the BOTransfer by attribute SettlementReferenceInstructed
     *
     * @param query    the current query
     * @param sriValue the SettlementReferenceInstructed value
     * @return the query with new filter
     */
    protected SQLQuery filterBySettlementReferenceInstructed(SQLQuery query, String sriValue) {
        return filterByTransferAttribute(query, ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST, sriValue);
    }

    /**
     * Filter by NOT clearing counterparties LGWM,LV4V,5MSR,ECAG, because there are classes that do this logic
     * ClearingIncomingSwiftMatcher
     * MTSClearingIncomingSwiftMatcher
     *
     * @param query the current query
     * @return the query with new filter
     */
    protected SQLQuery filterByNotClearingCounterparties(SQLQuery query) {
        if (!Util.isEmpty(clearingCtpy)) {
            query.appendWhereClause(" bo_transfer.orig_cpty_id NOT IN " + Util.collectionToSQLString(clearingCtpy));
        }
        return query;
    }

    /**
     * Filter the BOTransfer by attribute SRIPlatform
     *
     * @param query       the current query
     * @param sriPlatform the SRIPlatform value
     * @return the query with new filter
     */
    protected SQLQuery filterBySettlementReferenceInstructedPlatform(SQLQuery query, String sriPlatform) {
        return filterByTransferAttribute(query, ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST_PLATFORM, sriPlatform);
    }

    /**
     * Filter BOTransfers by attribute
     *
     * @param query the current query
     * @param name  the BOTransfer attribute name
     * @param value the BOTransfer attribute value
     * @return the query with new filter
     */
    protected SQLQuery filterByTransferAttribute(SQLQuery query, String name, String value) {
        String where = " bo_transfer.transfer_id IN ( select transfer_id from xfer_attributes where " +
                "xfer_attributes.attr_name = ? and xfer_attributes.attr_value = ? ) ";
        query.appendWhereClause(where, Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR,
                name), new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, value)));
        return query;
    }

    /**
     * Filter the settle transfers VERIFIED/FAILED
     *
     * @param query the current query
     * @return the new query with filter
     */
    protected SQLQuery filterBySettleTransfers(SQLQuery query) {
        query.appendWhereClause(" bo_transfer.is_payment = 1 AND bo_transfer.transfer_status IN "
                + Util.collectionToSQLString(Arrays.asList(Status.VERIFIED, Status.FAILED)) + " ");
        return query;
    }

    /**
     * Filter the transfers by SecCode ISIN
     *
     * @param query the current query
     * @param isin  the isin
     * @return the new query with filter
     */
    protected SQLQuery filterByISIN(SQLQuery query, String isin) {
        query.appendWhereClause(" bo_transfer.product_id IN " +
                        "(SELECT product_id FROM product_sec_code WHERE sec_code = ? AND  code_value_ucase = ? ) ",
                Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, "ISIN"),
                        new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, isin)));
        return query;
    }

    /**
     * Filter the transfer by the amount currency
     *
     * @param query the current query
     * @param ccy   the MT54X currency :19A::ESTT//CCY
     * @return the new query with filter
     */
    protected SQLQuery filterByCcy(SQLQuery query, String ccy) {
        query.appendWhereClause(" bo_transfer.amount_ccy = ? ",
                Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, ccy)));
        return query;
    }

    /**
     * Filter the transfers by nominal amount
     *
     * @param query   the current query
     * @param nominal the MT54X nominal :36B::ESTT//FAMT
     * @return the new query with filter
     */
    protected SQLQuery filterByNominalTotal(SQLQuery query, double nominal) {
        query.appendWhereClause(" abs(bo_transfer.nominal_amount) = ?",
                Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.DOUBLE, Math.abs(nominal))));
        return query;
    }

    /**
     * Filter the transfers by pending order nominal: Nominal (xfer) >= :36B::ESTT/FAMT (PAIN/PARC)
     *
     * @param query   the current query
     * @param nominal the MT54X nominal :36B::ESTT/FAMT
     * @return the new query with filter
     */
    protected SQLQuery filterByPendingOrderNominal(SQLQuery query, double nominal) {
        query.appendWhereClause(" abs(bo_transfer.nominal_amount) >= ? ",
                Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.DOUBLE, Math.abs(nominal))));
        return query;
    }

    /**
     * Filter the transfers by total other amount with tolerance +/- 25
     *
     * @param query the current query
     * @param cash  the MT54X cash amount :19A::ESTT//
     * @return the new query with filter
     */
    protected SQLQuery filterByTotalCash(SQLQuery query, double cash) {
        query.appendWhereClause(" abs(abs(bo_transfer.other_amount)- ? ) <= ? ",
                Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.DOUBLE, Math.abs(cash)), new CalypsoBindVariable(
                        CalypsoBindVariable.DOUBLE, TOLERANCE)
                ));
        return query;
    }

    /**
     * Filter the transfers by total delivery type DFP/DAP
     *
     * @param query the current query
     * @param deliveryType DFP if MT540/2 and DAP if MT541/3
     * @return the new query with filter
     */

    protected SQLQuery  filterByDeliveryType (SQLQuery query, String deliveryType) {
        query.appendWhereClause("  bo_transfer.delivery_type = ? ", Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, deliveryType)));
        return query;
    }

    /**
     * Filter the transfers by pending order cash: Other Amount (xfer)>= :19A::ESTT// (PAIN/PARC) +/- 25
     *
     * @param query the current query
     * @param cash  the MT54X cash amount :19A::ESTT//
     * @return the new query with filter
     */
    protected SQLQuery filterByPendingOrderCash(SQLQuery query, double cash) {
        query.appendWhereClause(" ( abs(bo_transfer.other_amount) + ? ) >= ? ",
                Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.DOUBLE, TOLERANCE), new CalypsoBindVariable(
                        CalypsoBindVariable.DOUBLE, Math.abs(cash))
                ));
        return query;
    }

    /**
     * Filter the transfer by Processing Org SDI account value
     *
     * @param query     the current query
     * @param poAccount the MT54X POAccount
     * @return the new query with filter
     */
    protected SQLQuery filterByPOAccount(SQLQuery query, String poAccount) {
        query.appendWhereClause(" bo_transfer.int_sdi IN ( select sdi_id from le_settle_delivery " +
                        "where agent_account = ? and le_role = 'ProcessingOrg' ) ",
                Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, poAccount)));
        return query;
    }

    /**
     * Filter transfer by PAY/RECEIVE
     *
     * @param query  the current query
     * @param payRec the PAY/RECEIVE value
     * @return the new query with filter
     */
    protected SQLQuery filterByPayRec(SQLQuery query, String payRec) {
        query.appendWhereClause(" bo_transfer.payreceive_type = ? ",
                Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, payRec)));
        return query;
    }

    /**
     * Filter transfer by payment type DAP/DFP
     *
     * @param query       the current query
     * @param paymentType the payment type DAP/DFP
     * @return the new query with filter
     */
    protected SQLQuery filterByPaymentType(SQLQuery query, String paymentType) {
        query.appendWhereClause(" bo_transfer.delivery_type = ? ",
                Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, paymentType)));
        return query;
    }

    /**
     * Filter transfers by trade date
     *
     * @param query     the current query
     * @param tradeDate the MT54X trade date value :98A::TRAD
     * @return the query with filter
     */
    protected SQLQuery filterByTradeDate(SQLQuery query, JDate tradeDate) {
        query.appendWhereClause(" bo_transfer.trade_date = ? ",
                Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.JDATE, tradeDate)));
        return query;
    }

    /**
     * Filter transfers by settle date
     *
     * @param query      the current query
     * @param settleDate the MT54X settle date value :98A::SETT//
     * @return the query with filter
     */
    protected SQLQuery filterBySettleDate(SQLQuery query, JDate settleDate) {
        query.appendWhereClause(" bo_transfer.settle_date = ? ",
                Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.JDATE, settleDate)));
        return query;
    }

    /**
     * Get the BOTransfer by SQLQuery
     *
     * @param query the query to filter transfers
     * @param dsCon the Data Server connection
     * @return the transfer array
     */
    protected TransferArray getBOTransfers(SQLQuery query, DSConnection dsCon) {
        if (query != null) {
            try {
                return dsCon.getRemoteBackOffice().getBOTransfers(query.getFromClause(),
                        query.getWhereClause(), query.getOrderByClause(), 200, query.getWhereBindVariables());
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
        }
        return null;
    }

    /**
     * Get the ISIN swift value
     *
     * @param message the current Swift message
     * @return the ISIN value
     */
    protected String getIsinValue(SwiftMessage message) {
        try {
            String isin = message.getFieldByType("Security Code Long");
            return !Util.isEmpty(isin) ? isin.replaceAll("ISIN/", "") : null;
        } catch (MessageParseException e) {
            Log.error(this, e);
        }
        return null;
    }

    /**
     * Get the MT54X currency value :19A::ESTT//CCY
     *
     * @param message the current message
     * @return the MT54X currency value
     */
    protected String getCcyValue(SwiftMessage message) {
        try {
            return message.getFieldByType("Ccy");
        } catch (MessageParseException e) {
            Log.error(this, e);
        }
        return null;
    }


    /**
     * Get the nominal total :36B::ESTT//FAMT
     *
     * @param message the current MT54X
     * @return the nominal total
     */
    protected Double getNominalTotal(SwiftMessage message) {
        DisplayValue moneyD = null;
        try {
            moneyD = message.getDisplayAmount("Nominal Amount");
        } catch (MessageParseException e) {
            Log.error(this, e);
        }
        return moneyD != null ? moneyD.get() : null;
    }

    /**
     * Get the cash total :19A::ESTT//
     *
     * @param message the current MT54X
     * @return the cash total
     */
    protected Double getCashTotal(SwiftMessage message) {
        DisplayValue moneyD = null;
        try {
            moneyD = message.getDisplayAmount("Cash Amount");
        } catch (MessageParseException e) {
            Log.error(this, e);
        }
        return moneyD != null ? moneyD.get() : null;
    }

    /**
     * Get the PAY or RECEIVE
     * PAY: MT542,MT543,MT546,MT547
     * RECEIVE: MT540,MT541,MT544,MT545
     *
     * @param message the current MT54X
     * @return the PAY/RECEIVE
     */
    protected String getPayRec(SwiftMessage message) {
        if ("MT540,MT541,MT544,MT545".contains(message.getType())) {
            return "RECEIVE";
        } else if ("MT542,MT543,MT546,MT547".contains(message.getType())) {
            return "PAY";
        } else if ("MT548".contains(message.getType())) {
            SwiftFieldMessage field = message.getSwiftField(":22H:", "REDE//RECE", null);
            if (field != null) {
                return "RECEIVE";
            } else {
                field = message.getSwiftField(":22H:", "REDE//DELI", null);
                return field != null ? "PAY" : null;
            }
        }
        return null;
    }

    /**
     * Get the payment type
     * DAP: MT541,MT543,MT545,MT547
     * DFP: MT540, MT542, MT544, MT546, MT548
     *
     * @param message the current swift message
     * @return the payment type DAP/DFP
     */
    protected String getPaymentType(SwiftMessage message) {
        if ("MT541,MT543,MT545,MT547".contains(message.getType())) {
            return "DAP";
        }
        return "DFP";
    }

    /**
     * Get the trade date :98A::TRAD//
     *
     * @param message the current swift message
     * @return the trade date
     */
    protected JDate getTradeDate(SwiftMessage message) {
        try {
            Object obj = message.getDate("Trade Date");
            if (obj instanceof JDate) {
                return (JDate) obj;
            } else if (obj instanceof JDatetime) {
                return ((JDatetime) obj).getJDate(TimeZone.getDefault());
            }
        } catch (MessageParseException e) {
            Log.error(this, e);
        }
        return null;
    }

    /**
     * Get the settle date :98A::SETT//
     *
     * @param message the current MT54X
     * @return the settle date
     */
    protected JDate getSettleDate(SwiftMessage message) {
        try {
            Object obj = message.getDate("Settle Date");
            if (obj instanceof JDate) {
                return (JDate) obj;
            } else if (obj instanceof JDatetime) {
                return ((JDatetime) obj).getJDate(TimeZone.getDefault());
            }
        } catch (MessageParseException e) {
            Log.error(this, e);
        }
        return null;
    }

    /**
     * Get if MT544-547 is partial settle :22F::PARS//PARC or :22F::PARS//PAIN
     *
     * @param message the current MT54X
     * @return true if partial settle
     */
    protected boolean isPartialSettle(SwiftMessage message) {
        String partialSettlement = getPartialSettlement(message);
        return !Util.isEmpty(partialSettlement) && ("PARS//PARC".equals(partialSettlement) || "PARS//PAIN".equals(partialSettlement));
    }

    /**
     * Get the partial settlement value of MT54X :22F::PARS//
     *
     * @param message the current MT54X
     * @return the partial settlement value
     */
    protected String getPartialSettlement(SwiftMessage message) {
        try {
            return message.getFieldByType("Partial Settlement");
        } catch (MessageParseException e) {
            Log.error(this, e);
        }
        return null;
    }

    /**
     * Get the remaining nominal of MT544-547 :36B::RSTT//FAMT/ or :36B::PSTT//FAMT/
     *
     * @param message the current MT54X
     * @return the remaining nominal
     */
    protected Double getRemainingNominal(SwiftMessage message) {
        DisplayValue moneyD = null;
        try {
            moneyD = message.getDisplayAmount("Remaining Amount");
        } catch (MessageParseException e) {
            Log.error(this, e);
        }
        return moneyD != null ? moneyD.get() : null;
    }

    /**
     * Get the remaining cash of MT544-547 :19A::RSTT//
     *
     * @param message the current MT54X
     * @return the remaining nominal
     */
    protected Double getRemainingCash(SwiftMessage message) {
        SwiftFieldMessage field = message.getSwiftField(":19A:", "RSTT//", null);
        if (field != null) {
            try {
                Object obj = field.parse(message.getType());
                return obj instanceof DisplayValue ? ((DisplayValue) obj).get() : null;
            } catch (MessageParseException e) {
                Log.error(this, e);
            }
        }
        return null;
    }

    /**
     * Build filters for MT544-MT547 total conciliation messages:
     * ISIN
     * Currency
     * Sender BIC
     * Nominal total
     * Cash total
     * Receiver BIC
     * POAccount
     * Pay or receive
     * DAP or DFP
     * Trade Date
     * Settle Date
     *
     * @param message the current MT54X
     * @return the SQL query
     */
    protected SQLQuery buildTotalConciliationTransfersQuery(SwiftMessage message) {
        String ccy = getCcyValue(message);
        if ("MT545,MT547".contains(message.getType()) && Util.isEmpty(ccy)) {
            return null;
        }
        String isin = getIsinValue(message);
        Double nominalTotal = getNominalTotal(message);
        Double cashTotal = getCashTotal(message);
        String poAccount = SecurityMatcher.getPOAccount(message);
        String payRec = getPayRec(message);
        String paymentType = getPaymentType(message);
        JDate tradeDate = getTradeDate(message);
        JDate settleDate = getSettleDate(message);

        if (!Util.isEmpty(isin) && nominalTotal != null
                && cashTotal != null && !Util.isEmpty(poAccount) && !Util.isEmpty(payRec) && !Util.isEmpty(paymentType)
                && tradeDate != null && settleDate != null) {
            SQLQuery query = buildSQLQuery();
            query = filterByISIN(query, isin);
            if ("MT545,MT547".contains(message.getType())) {
                query = filterByCcy(query, ccy);
            }
            query = filterByNominalTotal(query, nominalTotal);
            query = filterByTotalCash(query, cashTotal);
            query = filterByPOAccount(query, poAccount);
            query = filterByPayRec(query, payRec);
            query = filterByPaymentType(query, paymentType);
            query = filterBySettleDate(query, settleDate);
            return filterByTradeDate(query, tradeDate);

        }
        return null;
    }

    /**
     * Return the MT544-MT547 total conciliation index transfers
     *
     * @param message the current MT54X
     * @param dsCon   the Data Server connection
     * @return the transfer array with index transfers
     */
    protected TransferArray indexMT544toMT547TotalConciliationTransfers(SwiftMessage message, DSConnection dsCon) {
        TransferArray result = getBOTransfers(buildTotalConciliationTransfersQuery(message), dsCon);
        return filterTransfersByBIC(message, result, dsCon);
    }

    /**
     * Build SQL for MT544-MT547 partial settle messages (:22F::PARS//PARC or :22F::PARS//PAIN)
     * ISIN
     * Currency
     * Sender BIC
     * Pending order nominal
     * Pending order cash
     * Nominal total
     * Cash total
     * Receiver BIC
     * POAccount
     * Pay or receive
     * DAP or DFP
     * Trade Date
     * Settle Date
     *
     * @param message the current MT54X
     * @return the SQL query
     */
    protected SQLQuery buildPartialSettleTransfersQuery(SwiftMessage message) {
        String ccy = getCcyValue(message);
        if ("MT545,MT547".contains(message.getType()) && Util.isEmpty(ccy)) {
            return null;
        }

        String partialSettlementType = getPartialSettlement(message);

        //:36B::RSTT//FAMT
        Double remainingNominal = getRemainingNominal(message);
        //:19A::RSTT//
        Double remainingCash = getRemainingCash(message);

        if (!Util.isEmpty(partialSettlementType) && "PARS//PAIN".equals(partialSettlementType) && (remainingNominal == null
                || remainingCash == null)) {
            return null;
        }

        String isin = getIsinValue(message);

        //:36B::ESTT//FAMT
        Double nominalTotal = getNominalTotal(message);

        //:19A::ESTT//
        Double cashTotal = getCashTotal(message);

        String poAccount = SecurityMatcher.getPOAccount(message);
        String payRec = getPayRec(message);
        String paymentType = getPaymentType(message);
        JDate tradeDate = getTradeDate(message);
        JDate settleDate = getSettleDate(message);

        if (!Util.isEmpty(isin) && nominalTotal != null
                && cashTotal != null && !Util.isEmpty(poAccount) && !Util.isEmpty(payRec) && !Util.isEmpty(paymentType)
                && tradeDate != null && settleDate != null) {
            nominalTotal = Math.abs(nominalTotal);
            cashTotal = Math.abs(cashTotal);
            SQLQuery query = buildSQLQuery();
            query = filterByISIN(query, isin);
            if ("MT545,MT547".contains(message.getType())) {
                query = filterByCcy(query, ccy);
            }

            //Nominal pdte. orden >= Nominal recibido (ESET) -> Nominal (xfer) >= :36B::ESTT/FAMT
            query = filterByPendingOrderNominal(query, nominalTotal);
            //Efectivo pdte. (+- tolerancias) >= Efectivo recibido (ESET)-> Other Amount (xfer)>= :19A::ESTT//
            query = filterByPendingOrderCash(query, cashTotal);

            if (!Util.isEmpty(partialSettlementType) && "PARS//PAIN".equals(partialSettlementType) && remainingCash != null
                    && remainingNominal != null) {
                remainingCash = Math.abs(remainingCash);
                remainingNominal = Math.abs(remainingNominal);

                //Nominal total orden = Nominal mensaje SETT + PSTT + RSTT -> Nominal (xfer) = :36B::RSTT//FAMT/ + :36B::ESTT/FAMT
                double totalNominal = nominalTotal + remainingNominal;
                query = filterByNominalTotal(query, totalNominal);

                //Efectivo total orden (+- tolerancias) = Efectivo mensaje SETT + PSTT + RSTT ->  Other Amount (xfer) = :19A::RSTT// + :19A::ESTT//
                double totalCash = cashTotal + remainingCash;
                query = filterByTotalCash(query, totalCash);

            }

            query = filterByPOAccount(query, poAccount);
            query = filterByPayRec(query, payRec);
            query = filterByPaymentType(query, paymentType);
            query = filterBySettleDate(query, settleDate);
            return filterByTradeDate(query, tradeDate);
        }

        return null;
    }

    /**
     * Return the MT544-MT547 partial settle conciliation index transfers
     *
     * @param message the current MT54X
     * @param dsCon   the Data Server connection
     * @return the transfer array with index transfers
     */
    protected TransferArray indexMT544toMT547PartialSettleTransfers(SwiftMessage message, DSConnection dsCon) {
        TransferArray result = getBOTransfers(buildPartialSettleTransfersQuery(message), dsCon);
        return filterTransfersByBIC(message, result, dsCon);
    }

    /**
     * Build SQL for MT540-MT543 messages
     * ISIN
     * Currency
     * Sender BIC
     * Nominal total
     * Cash total
     * Receiver BIC
     * Trade Date
     * Settle Date
     *
     * @param message the current MT54X
     * @return the SQL query
     */
    protected SQLQuery buildMT540toMT543TransfersQuery(SwiftMessage message) {
        String ccy = getCcyValue(message);
        if ("MT541,MT543".contains(message.getType()) && Util.isEmpty(ccy)) {
            return null;
        }
        String isin = getIsinValue(message);
        Double nominalTotal = getNominalTotal(message);
        Double cashTotal = getCashTotal(message);
        JDate tradeDate = getTradeDate(message);
        JDate settleDate = getSettleDate(message);
        if (cashTotal == null  && ("MT541".equals(message.getType()) ||"MT543".equals(message.getType())))
            return null;
        if (!Util.isEmpty(isin) && nominalTotal != null
           //     && cashTotal != null
                && tradeDate != null && settleDate != null) {
            SQLQuery query = buildSQLQuery();
            query = filterByISIN(query, isin);
            if ("MT541,MT543".contains(message.getType())) {
                query = filterByCcy(query, ccy);
            }
            query = filterByNominalTotal(query, nominalTotal);
            query = filterByDeliveryType(query, "MT541".equals(message.getType()) || "MT543".equals(message.getType())? "DAP" : "DFP");

            if ("MT541".equals(message.getType()) || "MT543".equals(message.getType()))
                query = filterByTotalCash(query, cashTotal);

            query = filterBySettleDate(query, settleDate);
            return filterByTradeDate(query, tradeDate);
        }
        return null;
    }

    /**
     * Return the MT540-MT543  index transfers
     *
     * @param message the current MT54X
     * @param dsCon   the Data Server connection
     * @return the transfer array with index transfers
     */
    protected TransferArray indexMT540toMT543Transfers(SwiftMessage message, DSConnection dsCon) {
        TransferArray result = getBOTransfers(buildMT540toMT543TransfersQuery(message), dsCon);
        return filterTransfersByBIC(message, result, dsCon);
    }

    /**
     * Filter BOTransfer by sender/receiver BIC code
     *
     * @param message the current MT54X
     * @param array   the transfer candidates
     * @param ds      the Data Server connection
     * @return the filter transfer array
     */
    protected TransferArray filterTransfersByBIC(SwiftMessage message, TransferArray array, DSConnection ds) {
        String receiverBIC = message.getReceiver();
        String senderBic = message.getSender();
        if (!Util.isEmpty(array) && !Util.isEmpty(receiverBIC) && !Util.isEmpty(senderBic)) {
            return new TransferArray(array.stream().filter(t ->
                    filterTransferByBIC(senderBic, getPOAgentBIC(t, ds))).filter(t ->
                    filterTransferByBIC(receiverBIC, getPOBIC(t, ds))).collect(Collectors.toList()));
        }
        return new TransferArray();
    }

    /**
     * Filter transfer BIC vs MT BIC 7 chars
     *
     * @param msgBic      the message BIC code
     * @param transferBIC the transfer BIC code
     * @return true if transfer BIC code is equals to MT BIC code
     */
    private boolean filterTransferByBIC(String msgBic, String transferBIC) {
        if (!Util.isEmpty(msgBic) && !Util.isEmpty(transferBIC) && msgBic.length() >= 7 && transferBIC.length() >= 7) {
            msgBic = msgBic.substring(0, 7);
            transferBIC = transferBIC.substring(0, 7);
            return msgBic.equals(transferBIC);
        }
        return false;
    }

    /**
     * Get PO Agent BIC code like Transfer Viewer window, except counterparty 5GSR get BIC code from intermediary contact
     *
     * @param transfer the current transfer
     * @param ds       the Data Server connection
     * @return the POAgent BIC code
     */
    private String getPOAgentBIC(BOTransfer transfer, DSConnection ds) {
        LegalEntity ctpy = BOCache.getLegalEntity(ds, transfer.getOriginalCptyId());
        if (ctpy != null && !Util.isEmpty(ctpy.getCode()) && "5GSR".equals(ctpy.getCode())) {
            SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(ds, transfer.getInternalSettleDeliveryId());
            if (sdi != null && sdi.getIntermediaryId() > 0) {
                String productType;
                int poId = transfer.getProcessingOrg();
                if (!Util.isEmpty(sdi.getProductList())) {
                    productType = String.valueOf(sdi.getProductList().get(0));
                } else {
                    productType = "ALL";
                }

                if (sdi.getProcessingOrg() != null) {
                    poId = sdi.getProcessingOrg().getId();
                }
                LegalEntity intermediary = BOCache.getLegalEntity(ds, sdi.getIntermediaryId());
                if (intermediary != null) {
                    LEContact leC = BOCache.getContact(ds, "Agent", intermediary, sdi.getIntermediaryContactType(),
                            productType, poId);
                    return leC != null ? leC.getSwift() : null;
                }
            }
        } else {
            LegalEntity agent = BOCache.getLegalEntity(ds, transfer.getInternalAgentId());
            if (agent != null) {
                SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(ds, transfer.getInternalSettleDeliveryId());
                if (sdi != null) {
                    LEContact leC = BOCache.getContact(ds, "Agent", agent, sdi.getAgentContactType(),
                            transfer.getProductType(), transfer.getProcessingOrg());
                    return leC != null ? leC.getSwift() : null;
                }
            }
        }
        return null;
    }

    /**
     * Get PO BIC code, like SDI window (PO) > contact (beneficiary) > SWIFT
     *
     * @param transfer the current transfer
     * @param ds       the Data Server connection
     * @return PO BIC code
     */
    private String getPOBIC(BOTransfer transfer, DSConnection ds) {
        LegalEntity po = BOCache.getLegalEntity(ds, transfer.getProcessingOrg());
        if (po != null) {
            String poContactType = "Default";
            String productType = "ALL";
            SettleDeliveryInstruction poSDI = BOCache.getSettleDeliveryInstruction(ds,
                    transfer.getInternalSettleDeliveryId());
            if (poSDI != null) {
                poContactType = poSDI.getBeneficiaryContactType();
                if (!Util.isEmpty(poSDI.getProductList())) {
                    productType = String.valueOf(poSDI.getProductList().get(0));
                } else {
                    productType = "ALL";
                }
            }
            if (!Util.isEmpty(poContactType) && !Util.isEmpty(productType)) {
                LEContact leC = BOCache.getContact(ds, "ProcessingOrg", po, poContactType,
                        productType, transfer.getProcessingOrg());
                return leC != null ? leC.getSwift() : null;
            }
        }
        return null;
    }
}
