/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.medusa.utils;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TransferArray;

import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;

public class MedusaCashManagementUtil {

    private static final String MUREX_ID = "MurexID";
    private static final String ADDRESS_METHOD_SWIFT = "SWIFT";
    private static final String TAG20 = ":20:";
    private static final String MUREX_ROOT_CONTRACT = "MurexRootContract";
    private static final String MEDUSA_DOMAIN_VALUE = "MedusaProductTypes";
    final public static MedusaCashManagementUtil meddusaCashManagmentUtil = null;

    synchronized public static MedusaCashManagementUtil getInstance() {
        if (meddusaCashManagmentUtil != null) {
            return meddusaCashManagmentUtil;
        } else {
            return new MedusaCashManagementUtil();
        }
    }

    public boolean isDirectSettleMethod(final String settleMethod) {
        boolean isDirect = Boolean.FALSE;
        if(!Util.isEmpty(settleMethod) && "direct".equalsIgnoreCase(settleMethod)){
                    isDirect = Boolean.TRUE;
                }
        return isDirect;
    }

    @SuppressWarnings("rawtypes")
    public Vector<String> getDirectSettleMethods() {
        final Vector<String> settleMethods = new Vector<String>();

        final Vector domainValues = LocalCache.getDomainValues(
                DSConnection.getDefault(), "settlementMethodDirect");

        if (domainValues != null) {
            for (final Object value : domainValues) {
                settleMethods.add((String) value);
            }
        }

        return settleMethods;
    }


    public String buildTransferExternalSDIGLAccount(
            final SettleDeliveryInstruction sdi) {
        String rst = "";

        if (sdi == null) {
            return "";
        }
        final int account = sdi.getGeneralLedgerAccount();
        Account acc;
        acc = BOCache.getAccount(DSConnection.getDefault(), account);

        if (acc != null) {
            rst = acc.getName();
        }

        return rst;
    }

    @SuppressWarnings("unchecked")
    public JDatetime getXferCreationDate(final long xferId, final Trade trade,
                                         final int version) throws RemoteException {
        if (version != 0) {
            final Vector<AuditValue> audit = DSConnection.getDefault().getRemoteTrade()
                    .getAudit(
                            "entity_class_name='BOTransfer' and entity_id="
                                    + xferId + " and version_num=" + version,
                            null, null);
            if (!Util.isEmpty(audit)) {
                return audit.get(0).getModifDate();
            } else {
                return new JDatetime();
            }
        } else if (trade != null) {
            return trade.getUpdatedTime();
        } else {
            return new JDatetime();
        }
    }

    /**
     * Get the product desc value for the xfer
     *
     * @param trade trade
     * @return product_desc value
     */
    public String getProducDesc(final Trade trade) {
        String description = "";
        if (trade != null) {
            final Product product = trade.getProduct();
            if (product != null) {
                description = product.getDescription();
            }
        }
        return description;
    }

    /**
     * Get the trader value for the xfer
     *
     * @param trade trade
     * @return trader value
     */
    public String getTrader(final Trade trade) {
        String trader = "";

        if (trade != null) {
            trader = trade.getTraderName();
        }
        return trader;
    }

    public String getTag20(final BOTransfer xfer, final Trade trade) { //Message SEND?
       try {
           StringBuilder where = new StringBuilder();
           where.append(" bo_message.transfer_id=" + xfer.getLongId());
           where.append(" AND template_name IN ('MT202','MT202COV','MT202XferAgent') ");
           where.append(" and bo_message.linked_id=0 and bo_message.message_id not in (select distinct linked_id from bo_message where linked_id<>0 and transfer_id=");
           where.append(xfer.getLongId() + ")");
           final String orderby = " bo_message.message_Id Desc";

           final MessageArray messages = DSConnection.getDefault().getRemoteBO()
                    .getMessages(null, where.toString(), orderby,null);

            if(null!= messages && !Util.isEmpty(messages.getMessages())){
                SwiftMessage swiftMessage = getSwiftMessage(messages.get(0).getLongId(), DSConnection.getDefault());
                if(null!=swiftMessage){
                    SwiftFieldMessage swiftField = swiftMessage.getSwiftField(TAG20, null, null);
                    return null!=swiftField ? swiftField.getValue() : "";
                }
            }
        } catch (final Exception exc) {
            Log.error(MedusaCashManagementUtil.class.getName(), exc);
        }

        return "";
    }

    public boolean isSDCTransfer(final Trade trade, final BOTransfer transfer) {
        boolean result = Boolean.FALSE;

        if (trade == null) {
            return result;
        }
        String kwToCheck = null;
        // Transfer for near leg
        if (!transfer.getIsReturnB()) {
            kwToCheck = MedusaKeywordConstantsUtil.TRADE_KEYWORD_BUSINESS;
        } else {
            // Transfer for far leg
            kwToCheck = MedusaKeywordConstantsUtil.TRADE_KEYWORD_BUSINESS_FAR;
        }

        final String business = trade.getKeywordValue(kwToCheck);

        // check if the business is SDC
        if (!Util.isEmpty(business)
                && MedusaKeywordConstantsUtil.TRADE_KEYWORD_BUSINESS_SDC
                .equalsIgnoreCase(business)) {
            result = Boolean.TRUE;
        }

        return result;
    }

    public boolean isTransactional(final Trade trade, final String subsetDvName) {
        if (trade == null) {
            return Boolean.FALSE;
        }

        final String platform = trade
                .getKeywordValue(MedusaKeywordConstantsUtil.KEYWORD_PLATFORM);
        String counterparty = "";
        if (trade.getCounterParty() != null) {
            counterparty = trade.getCounterParty().getAuthName();
        }

        return isTransactional(platform, counterparty, subsetDvName);
    }

    @SuppressWarnings({"rawtypes"})
    public boolean isTransactional(final String platform,
                                   final String counterparty, final String subsetDvName) {
        if (Util.isEmpty(platform)) {
            return Boolean.FALSE;
        }

        final Vector transactionalPlatforms = getTransactionalPlatforms(subsetDvName);
        final Vector transactionalCounterparties = LocalCache.getDomainValues(
                DSConnection.getDefault(),
                MedusaKeywordConstantsUtil.DOMAIN_VALUE_TRANSACTIONAL_CPTY);

        if ((!Util.isEmpty(transactionalPlatforms) && transactionalPlatforms
                .contains(platform))
                && (Util.isEmpty(counterparty)
                || Util.isEmpty(transactionalCounterparties) || transactionalCounterparties
                .contains(counterparty))) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Vector<String> getTransactionalPlatforms(final String subsetDvName) {
        if (!Util.isEmpty(subsetDvName)) {
            return LocalCache.getDomainValues(DSConnection.getDefault(),
                    subsetDvName);
        } else {
            Vector transactionalPlatforms = LocalCache.getDomainValues(
                    DSConnection.getDefault(),
                    MedusaKeywordConstantsUtil.DOMAIN_VALUE_TRANSACTIONAL_PLATFORM);
            if (Util.isEmpty(transactionalPlatforms)) {
                transactionalPlatforms = new Vector();
            }
            Vector transactionalPlatformsSdc = LocalCache
                    .getDomainValues(
                            DSConnection.getDefault(),
                            MedusaKeywordConstantsUtil.DOMAIN_VALUE_TRANSACTIONAL_PLATFORM_SDC);
            if (Util.isEmpty(transactionalPlatformsSdc)) {
                transactionalPlatformsSdc = new Vector();
            }

            return Util.mergeVectors(transactionalPlatforms,
                    transactionalPlatformsSdc);
        }

    }


    public String getMurexID(BOTransfer transfer){
        String murexId = findMurexId(transfer);
        if(Util.isEmpty(murexId)){
            BOTransfer clientXfer = findClientXfer(transfer);
            murexId = findMurexId(clientXfer);
        }
        return !Util.isEmpty(murexId) ? murexId : "";
    }

    public String findMurexId(BOTransfer transfer){
        String murexID = "";
        if(null!=transfer){
            final int glAccountNumber = transfer.getGLAccountNumber();
            final Account account = BOCache.getAccount(DSConnection.getDefault(), glAccountNumber);
            if(null!=account) {
                murexID = account.getAccountProperty(MUREX_ID);
            }
        }
        return !Util.isEmpty(murexID) ? murexID : "";
    }

    public BOTransfer findClientXfer(BOTransfer boTransfer){
        BOTransfer transfer = null;
        if(null!=boTransfer){
            TransferArray transfers = null;
            try {
                transfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(boTransfer.getTradeLongId());
                if(!Util.isEmpty(transfers)){
                    transfer = transfers.stream().filter(t -> "Client".equalsIgnoreCase(t.getExternalRole())).max((trans, t1) -> {
                        if (trans.getLongId() > t1.getLongId()) {
                            return 0;
                        }
                        return 1;
                    }).orElse(null);
                }
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading Trade: " + boTransfer.getTradeLongId());
            }
        }
        return transfer;
    }


    public String getMurexRootContract(BOTransfer xfer){
        Long tradeId = xfer.getTradeLongId();
        if(tradeId==null || (tradeId!=null && tradeId==0)){
            return "0";
        }
        Trade trade=null;
        try {
            trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
            return trade.getKeywordValue(MUREX_ROOT_CONTRACT);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Cannot load trade " + tradeId);
        }
        return "0";
    }


    public String printDouble(final Double value) {
        final NumberFormat nf = NumberFormat.getInstance(Locale.UK);
        nf.setMaximumFractionDigits(6);
        nf.setGroupingUsed(false);
        return nf.format(value);
    }

    /**
     * parse double
     *
     * @param value value to parse
     * @return value parsed
     */
    public Double parseDouble(final String value) {
        return Double.valueOf(value);
    }

    private SwiftMessage getSwiftMessage(long swiftMsgID, DSConnection dsConn) {

        SwiftMessage swiftMessage = new SwiftMessage();

        if (swiftMsgID != 0) {
            Log.info(this, "Loading advice document from message: " + swiftMsgID);
            Vector adviceDocuments = null;
            try {
                adviceDocuments = dsConn.getRemoteBackOffice().getAdviceDocuments("advice_document.advice_id=" + swiftMsgID, null, null);
            } catch (CalypsoServiceException e) {
                Log.error(this, "Cannot load Advice Documents for Swift message id " + swiftMsgID);
            }

            if (Util.isEmpty(adviceDocuments))
                Log.warn(this, "No Advice Documents found for Swift message id " + swiftMsgID);
            else {
                for (Object docObj : adviceDocuments) {
                    AdviceDocument document = (AdviceDocument) docObj;
                    if (document != null
                            && document.getAddressMethod().equalsIgnoreCase(ADDRESS_METHOD_SWIFT)) {
                        Log.info(
                                this, "generate swiftMessage from Advice Document id: " + document.getAdviceId());

                        if (!swiftMessage.parseSwiftText(document.getDocument().toString(), false)) {
                            Log.error("", "Can't parse raw records from Triparty Margin Detail");
                            return null;
                        }
                        return swiftMessage;
                    }
                }
                Log.warn(
                        this,
                        " Advice documents found for Swift message id "
                                + swiftMsgID
                                + ", but none of them are Swift messages");
            }
        }

        return null;
    }

    public boolean isSecDAPXfer(BOTransfer xfer) {
        return null!=xfer && "SECURITY".equalsIgnoreCase(xfer.getTransferType()) && "DAP".equalsIgnoreCase(xfer.getDeliveryType());
    }

    public boolean isMedusaProductType(BOTransfer xfer) {
        String xferProductType = xfer.getProductType();
        Vector<String> productTypes = LocalCache.getDomainValues(DSConnection.getDefault(), MEDUSA_DOMAIN_VALUE);

        return productTypes.contains(xferProductType) || isUnderlying(xfer);
    }

    public boolean isRepoOrEquityOrBondDAP(BOTransfer xfer) {
        return isMedusaProductType(xfer) && isSecDAPXfer(xfer);
    }

    public boolean isSecLendingFee(BOTransfer xfer){
        String xferTransferType = xfer.getTransferType();
        return !Util.isEmpty(xferTransferType) && "SECLENDING_FEE".equalsIgnoreCase(xferTransferType) ? true : false;
    }

    public boolean isCorporateAction(BOTransfer xfer){
        String xferProductType = xfer.getProductType();
        return !Util.isEmpty(xferProductType) && ("CA".equalsIgnoreCase(xferProductType)) ? true : false;
    }


    public boolean isUnderlying(BOTransfer xfer) {
        TransferArray underlyingTransfers = xfer.getUnderlyingTransfers();
        if (Util.isEmpty(underlyingTransfers)) {
            try {
                underlyingTransfers = DSConnection.getDefault().getRemoteBO().getNettedTransfers(xfer.getLongId());
            } catch (Exception e) {
                Log.error(this, "Error loading Netting Transfer for BOTransfer: " + xfer.getLongId() + "Error: " + e.getMessage());
            }
        }
        if (!Util.isEmpty(underlyingTransfers)) {
            BOTransfer underXfer = underlyingTransfers.get(0);
            String xferProductType = underXfer.getProductType();
            return !Util.isEmpty(xferProductType) && ("Repo".equalsIgnoreCase(xferProductType) || "Equity".equalsIgnoreCase(xferProductType) || "Bond".equalsIgnoreCase(xferProductType));
        }
        return false;
    }


}

