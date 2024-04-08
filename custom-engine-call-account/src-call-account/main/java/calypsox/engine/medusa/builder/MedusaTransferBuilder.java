package calypsox.engine.medusa.builder;

import calypsox.engine.medusa.utils.MedusaCashManagementUtil;
import calypsox.engine.medusa.utils.MedusaKeywordConstantsUtil;
import calypsox.engine.medusa.utils.XmlDateUtil;
import calypsox.engine.medusa.utils.xml.Botransfer;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOProductHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 * Methods directly extracted from MedusaManagementEngine
 */
public class MedusaTransferBuilder {

    private static final String XFER_TYPE_SEC_DELIVERY = "SEC_DELIVERY";
    private static final String XFER_TYPE_SEC_RECEIPT = "SEC_RECEIPT";

    private static final String XFER_TYPE_PAYMENT = "PAYMENT";
    private static final String XFER_TYPE_RECEIPT = "RECEIPT";

    private static final String XFER_TYPEFOR_FEES = "FEE";

    private final Vector<String> listFeeTypesFromDomainValues = new Vector<>();

    private final List<String> businessReasonResult = Arrays.asList(Action.SPLIT.toString(), Action.PARTIAL_SETTLE.toString());

    private final MedusaCashManagementUtil medusaCashManagementUtil = MedusaCashManagementUtil.getInstance();

    final BOTransfer transfer;
    final Trade trade;

    public MedusaTransferBuilder(BOTransfer transfer,Trade trade){
        this.transfer=transfer;
        this.trade=trade;
    }

    public Botransfer buildCMTransfer() throws RemoteException {
        final DSConnection dsCon = DSConnection.getDefault();

        final Botransfer cmXfer = new Botransfer();

        cmXfer.setTransferId(transfer.getLongId());

        String eventType = transfer.getEventType();
        if(medusaCashManagementUtil.isRepoOrEquityOrBondDAP(transfer)){
            if(XFER_TYPE_SEC_DELIVERY.equalsIgnoreCase(eventType)){
                eventType = XFER_TYPE_RECEIPT;
            }
            else if(XFER_TYPE_SEC_RECEIPT.equalsIgnoreCase(eventType)){
                eventType = XFER_TYPE_PAYMENT;
            }
        }
        cmXfer.setEventType(eventType);

        // set the entity field
        // Load the book to get the PO
        final Book book = BOCache.getBook(dsCon, transfer.getBookId());
        if ((book != null) && (book.getLegalEntity() != null)) {
            cmXfer.setBook(book.getName());
            cmXfer.setEntity(book.getLegalEntity().getCode());
        } else {
            cmXfer.setEntity("");
        }

        cmXfer.setTransferStatus(transfer.getStatus().toString());

        if(medusaCashManagementUtil.isMedusaProductType(transfer) || medusaCashManagementUtil.isSecLendingFee(transfer)){
            cmXfer.setMurexId(medusaCashManagementUtil.getMurexRootContract(transfer));
        }
        else if(medusaCashManagementUtil.isCorporateAction(transfer) && trade!=null){
            if("CounterParty".equalsIgnoreCase(trade.getRole())) {
                String caSource = trade.getKeywordValue("CASource");
                Trade refTrade = dsCon.getRemoteTrade().getTrade(Long.parseLong(caSource));
                if (refTrade != null) {
                    cmXfer.setMurexId(refTrade.getKeywordValue("MurexRootContract"));
                }
            }
            else if("Agent".equalsIgnoreCase(trade.getRole())) {
                cmXfer.setMurexId("");
            }
        }
        else{
            cmXfer.setMurexId(medusaCashManagementUtil.getMurexID(transfer));
        }

        setTradeId(cmXfer,transfer);

       setProductType(cmXfer,transfer);
        // if the transfer type is a fee, then set the transfer type to FEE
        final String xferType = transfer.getTransferType();
        if (!Util.isEmpty(xferType)) {
            if (!Util.isEmpty(listFeeTypesFromDomainValues)
                    && listFeeTypesFromDomainValues.contains(xferType)) {
                cmXfer.setTransferType(XFER_TYPEFOR_FEES);
            } else {
                cmXfer.setTransferType(xferType);
            }
        }

        cmXfer.setSettleAmount(getMedusaXferSettleAmount());

        cmXfer.setSettleCurrency(transfer.getSettlementCurrency());

        cmXfer.setValueDate(XmlDateUtil.createXmlDate(transfer.getValueDate()));
        cmXfer.setSettleDate(
                XmlDateUtil.createXmlDate(transfer.getSettleDate()));

        final SettleDeliveryInstruction payerSDI = BOCache
                .getSettleDeliveryInstruction(dsCon, transfer.getPayerSDId());

        final SettleDeliveryInstruction receiverSDI = BOCache
                .getSettleDeliveryInstruction(dsCon,
                        transfer.getReceiverSDId());

        if(payerSDI!=null){
            if(medusaCashManagementUtil.isRepoOrEquityOrBondDAP(transfer)){
                cmXfer.setReceiverCode(BOCache.getLegalEntityCode(dsCon, payerSDI.getBeneficiaryId()));
                cmXfer.setReceiverRole(payerSDI.getRole());
                cmXfer.setReceiverInst(payerSDI.getDescription());
            }
            else{
                cmXfer.setPayerCode(BOCache.getLegalEntityCode(dsCon, payerSDI.getBeneficiaryId()));
                cmXfer.setPayerRole(payerSDI.getRole());
                cmXfer.setPayerInst(payerSDI.getDescription());
                cmXfer.setPoSettleMethod(payerSDI.getSettlementMethod());
            }
        }

        if(receiverSDI!=null){
            if(medusaCashManagementUtil.isRepoOrEquityOrBondDAP(transfer)){
                cmXfer.setPayerCode(BOCache.getLegalEntityCode(dsCon, receiverSDI.getBeneficiaryId()));
                cmXfer.setPayerRole(receiverSDI.getRole());
                cmXfer.setPayerInst(receiverSDI.getDescription());
                cmXfer.setPoSettleMethod(receiverSDI.getSettlementMethod());
            }
            else {
                cmXfer.setReceiverCode(BOCache.getLegalEntityCode(dsCon, receiverSDI.getBeneficiaryId()));
                cmXfer.setReceiverRole(receiverSDI.getRole());
                cmXfer.setReceiverInst(receiverSDI.getDescription());
            }
        }

        long nettedTransfer = 0;
        if (transfer.getNettedTransferLongId() != 0 && !transfer.getProductType().equalsIgnoreCase("SecLending")) {
            nettedTransfer = transfer.getNettedTransferLongId();
        }
        cmXfer.setNettedTransfer(nettedTransfer);

        if (trade != null) {
            cmXfer.setCounterparty(trade.getCounterParty().getCode());
            // Cash management T99A Counterparty Description
            cmXfer.setCounterpartyDescription(
                    trade.getCounterParty().getName());
            // Cash management T99A Counterparty Description - End
            cmXfer.setOriginalCpty(trade.getCounterParty().getCode());
        } else {
            cmXfer.setCounterparty(BOCache.getLegalEntityCode(dsCon,
                    transfer.getExternalLegalEntityId()));
            // Cash management T99A Counterparty Description
            LegalEntity counterparty = BOCache.getLegalEntity(dsCon,
                    transfer.getExternalLegalEntityId());
            cmXfer.setCounterpartyDescription(counterparty.getName());
            // Cash management T99A Counterparty Description - End
            cmXfer.setOriginalCpty(BOCache.getLegalEntityCode(dsCon,
                    transfer.getExternalLegalEntityId()));

            // Checks those fields for netted Transfers of Equity
            if("Equity".equalsIgnoreCase(transfer.getProductType())) {
                //if("Equity".equalsIgnoreCase(transfer.getProductType()) || "Repo".equalsIgnoreCase(transfer.getProductType())){
                counterparty = getNettingCptyByUnderlying(transfer);
                if(counterparty != null){
                    cmXfer.setCounterparty(BOCache.getLegalEntityCode(dsCon, counterparty.getId()));
                    cmXfer.setCounterpartyDescription(counterparty.getName());
                    cmXfer.setOriginalCpty(BOCache.getLegalEntityCode(dsCon, counterparty.getId()));
                }
            }
        }

        cmXfer.setTimestamp(
                XmlDateUtil.createXmlDatetimeObj(medusaCashManagementUtil.getXferCreationDate(
                        transfer.getLongId(), trade, transfer.getVersion())));
        cmXfer.setTransferVersion(transfer.getVersion());

        cmXfer.setTag20(medusaCashManagementUtil.getTag20(transfer, trade));
        // set InternalExternal field
        final SettleDeliveryInstruction sdi = BOCache
                .getSettleDeliveryInstruction(dsCon,
                        transfer.getExternalSettleDeliveryId());

        cmXfer.setInternalExternal("");
        // check if we should use isDDATransfer instead of isDIRECT custom check
        final boolean isDIRECT = medusaCashManagementUtil.isDirectSettleMethod(transfer.getSettlementMethod());

        if (medusaCashManagementUtil.isTransactional(trade, null)) {
            // CAL_240_
            String platform = "";
            if (trade != null) {
                platform = trade
                        .getKeywordValue(MedusaKeywordConstantsUtil.KEYWORD_PLATFORM);
            }
            cmXfer.setInternalExternal(platform);

        } else if (medusaCashManagementUtil.isSDCTransfer(trade, transfer)) {
            // CAL_715
            final String commentGBMC = LocalCache.getDomainValueComment(dsCon,
                    MedusaKeywordConstantsUtil.DOMAIN_VALUE_TRANSACTIONAL_GBMC,
                    MedusaKeywordConstantsUtil.DOMAIN_VALUE_GBMC);

            if (!Util.isEmpty(commentGBMC)) {
                cmXfer.setInternalExternal(commentGBMC);
            }

        } else {

            if ((sdi != null)) {
                if (isDIRECT) {
                    cmXfer.setInternalExternal("Internal");
                } else {
                    cmXfer.setInternalExternal("External");
                }
            }
        }

        // set IBAN field
        cmXfer.setIban("");
        if ((sdi != null)) {
            // if (transfer.isDDATransfer()) {
            if (isDIRECT) {
                cmXfer.setIban(medusaCashManagementUtil.buildTransferExternalSDIGLAccount(sdi));
            }
        }

        // Product desc
        if(transfer.getProductType().equalsIgnoreCase("Bond") &&  trade!=null ){
            String bondForward = trade.getKeywordValue("BondForward");
            if(!Util.isEmpty(bondForward) && "true".equalsIgnoreCase(bondForward)){
                cmXfer.setProduct_desc("BondFwd");
            }
            else{
                cmXfer.setProduct_desc("“BondSpot”");
            }
        }
        else {
            cmXfer.setProduct_desc(medusaCashManagementUtil.getProducDesc(trade));
        }

        cmXfer.setTrader(medusaCashManagementUtil.getTrader(trade));
        cmXfer.setParent_id(transfer.getParentLongId());
        cmXfer.setBusinessReason(getBusinessReason(transfer));

        return cmXfer;
    }


    private double getMedusaXferSettleAmount(){
        double settlementAmount;

        if(medusaCashManagementUtil.isSecDAPXfer(transfer)){
            settlementAmount = transfer.getOtherAmount();
        }else{
            settlementAmount = addSettleAmountSign(transfer.getSettlementAmount());
        }
        return settlementAmount;
    }

    private double addSettleAmountSign(double settlementAmount){
        double sign = 1.;
        if (BOProductHandler.PAY.equals(transfer.getPayReceiveType())) {
            sign = -1.;
        }
        return sign * Math.abs(settlementAmount);
    }


    private LegalEntity getNettingCptyByUnderlying(BOTransfer nettingTransfer){
        TransferArray underlyingTransfers = nettingTransfer.getUnderlyingTransfers();
        if(Util.isEmpty(underlyingTransfers)){
            try {
                underlyingTransfers = DSConnection.getDefault().getRemoteBO().getNettedTransfers(nettingTransfer.getLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading Netting Transfer for BOTransfer: " + nettingTransfer.getLongId());
            }
        }
        if(!Util.isEmpty(underlyingTransfers) && underlyingTransfers.size()>0){
            for (int i = 0; i < underlyingTransfers.size(); i++) {
                BOTransfer transfer = underlyingTransfers.get(i);
                try {
                    Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(transfer.getTradeLongId());
                    if(trade != null){
                        return trade.getCounterParty();
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this, e);
                }
            }
        }
        return null;
    }

    private String getBusinessReason(BOTransfer transfer) {
        String businessReason = transfer.getAttribute("BusinessReason");
        if (!Util.isEmpty(businessReason) && businessReasonResult.contains(businessReason)) {
            return Action.SPLIT.toString();
        }
        return businessReason;
    }

    private void setTradeId(Botransfer cmXfer, BOTransfer xfer){
        if(isPenaltyNetting(xfer)){
            setTradeIdPenalty(cmXfer,xfer);
        }else{
            setTradeIdOriginal(cmXfer);
        }
    }
    private void setTradeIdOriginal(Botransfer cmXfer){
        if (trade != null) {
            if(!transfer.getNettedTransfer()) {
                cmXfer.setTradeId(transfer.getTradeLongId());
            }
            else {
                cmXfer.setTradeId(0);
            }
        } else {
            cmXfer.setTradeId(0);
        }
    }

    private void setTradeIdPenalty(Botransfer cmXfer,BOTransfer boTransfer){
        String penaltyTradeId=Optional.ofNullable(boTransfer).map(xfer->xfer.getAttribute("PenaltyTradeId")).orElse("");
        long tradeIdLong=0L;
        try {
             tradeIdLong = Long.parseLong(penaltyTradeId);
        }catch(NumberFormatException exc){
            Log.warn(this,exc.getCause());
        }
        cmXfer.setTradeId(tradeIdLong);
    }

    private void setProductType(Botransfer cmXfer, BOTransfer xfer){
        if(isPenaltyNetting(xfer)){
            setProductTypePenalty(cmXfer);
        }else{
            setProductTypeOriginal(cmXfer);
        }
    }

    private void setProductTypeOriginal(Botransfer cmXfer){
        if (trade != null) {
            if(!transfer.getNettedTransfer()) {
                if ("Equity".equalsIgnoreCase(trade.getProductType())) {
                    Equity equity = (Equity) trade.getProduct();
                    String equityType = equity.getSecCode("EQUITY_TYPE");
                    if(!Util.isEmpty(equityType) && (equityType.equalsIgnoreCase("CO2") || equityType.equalsIgnoreCase("VCO2"))){
                       String equityCO2SubType = trade.getKeywordValue("Mx_Product_SubType");
                       if(!Util.isEmpty(equityCO2SubType) && equityCO2SubType.equalsIgnoreCase("SPOT")){
                           cmXfer.setProductType("CO2_Spot");
                       }
                       else if(!Util.isEmpty(equityCO2SubType) && equityCO2SubType.equalsIgnoreCase("FORWARD")){
                           cmXfer.setProductType("CO2_Forward");
                       }
                       else{
                           cmXfer.setProductType(transfer.getProductType());
                       }
                    }
                    else if(!Util.isEmpty(equityType) && equityType.equalsIgnoreCase("ETF")) {
                        cmXfer.setProductType("ETF");
                    }
                    else{
                        cmXfer.setProductType(transfer.getProductType());
                    }
                } else {
                    cmXfer.setProductType(transfer.getProductType());
                }
            }
            else {
                cmXfer.setProductType("");
            }
        } else {
            cmXfer.setProductType("");
        }
    }

    private void setProductTypePenalty(Botransfer cmXfer){
        cmXfer.setProductType(SimpleTransfer.class.getSimpleName());
    }

    private boolean isPenaltyNetting(BOTransfer xfer){
        boolean isPenaltyNetting= Optional.ofNullable(xfer).map(BOTransfer::getNettingType).map("Penalty"::equalsIgnoreCase).orElse(false);
        boolean isNotUnderlying=Optional.ofNullable(xfer).map(BOTransfer::getNettedTransfer).orElse(false);
        return isPenaltyNetting && isNotUnderlying;
    }
}
