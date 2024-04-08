package calypsox.repoccp.model.lch;

import calypsox.repoccp.MTSPlatformReferenceHandler;
import calypsox.repoccp.ReconCCPConstants;
import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.ReconCCPMatchingResult;
import calypsox.repoccp.model.ReconCCPPosition;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Objects;

import static calypsox.repoccp.ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST;
import static calypsox.repoccp.ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST_2;

public class LCHSettlement extends ReconCCPPosition {

    private String dealerId;
    private String dealerName;
    private String clearerId;
    private String clearerName;
    private String houseClient;
    private String intendedSettlementDate;
    private String settlementReferenceInstructed;
    private String isin;
    private String isinName;
    private String lchMarketCode;
    private String memberCsdIcsdTriPartySystem;
    private String memberAccount;
    private String lchCsdIcsdTriPartySystem;
    private String lchAccount;
    private double nominalInstructed;
    private double nominalRemaining;
    private String nominalCurrency;
    private double cashAmountInstructed;
    private double cashAmountRemaining;
    private String cashAmountCurrency;
    private String bondsReceiver;
    private String bondsDeliverer;
    private String cashReceiver;
    private String cashDeliverer;
    private String settlementType;
    private String settlementStatus;
    private String parentInstructionReference;

    public String getDealerId() {
        return dealerId;
    }

    public void setDealerId(String dealerId) {
        this.dealerId = dealerId;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }

    public String getClearerId() {
        return clearerId;
    }

    public void setClearerId(String clearerId) {
        this.clearerId = clearerId;
    }

    public String getClearerName() {
        return clearerName;
    }

    public void setClearerName(String clearerName) {
        this.clearerName = clearerName;
    }

    public String getHouseClient() {
        return houseClient;
    }

    public void setHouseClient(String houseClient) {
        this.houseClient = houseClient;
    }

    public String getIntendedSettlementDate() {
        return intendedSettlementDate;
    }

    public void setIntendedSettlementDate(String intendedSettlementDate) {
        this.intendedSettlementDate = intendedSettlementDate;
    }

    public String getSettlementReferenceInstructed() {
        return settlementReferenceInstructed;
    }

    public void setSettlementReferenceInstructed(String settlementReferenceInstructed) {
        this.settlementReferenceInstructed = settlementReferenceInstructed;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getIsinName() {
        return isinName;
    }

    public void setIsinName(String isinName) {
        this.isinName = isinName;
    }

    public String getLchMarketCode() {
        return lchMarketCode;
    }

    public void setLchMarketCode(String lchMarketCode) {
        this.lchMarketCode = lchMarketCode;
    }

    public String getMemberCsdIcsdTriPartySystem() {
        return memberCsdIcsdTriPartySystem;
    }

    public void setMemberCsdIcsdTriPartySystem(String memberCsdIcsdTriPartySystem) {
        this.memberCsdIcsdTriPartySystem = memberCsdIcsdTriPartySystem;
    }

    public String getMemberAccount() {
        return memberAccount;
    }

    public void setMemberAccount(String memberAccount) {
        this.memberAccount = memberAccount;
    }

    public String getLchCsdIcsdTriPartySystem() {
        return lchCsdIcsdTriPartySystem;
    }

    public void setLchCsdIcsdTriPartySystem(String lchCsdIcsdTriPartySystem) {
        this.lchCsdIcsdTriPartySystem = lchCsdIcsdTriPartySystem;
    }

    public String getLchAccount() {
        return lchAccount;
    }

    public void setLchAccount(String lchAccount) {
        this.lchAccount = lchAccount;
    }

    public double getNominalInstructed() {
        return nominalInstructed;
    }

    public void setNominalInstructed(double nominalInstructed) {
        this.nominalInstructed = nominalInstructed;
    }

    public double getNominalRemaining() {
        return nominalRemaining;
    }

    public void setNominalRemaining(double nominalRemaining) {
        this.nominalRemaining = nominalRemaining;
    }

    public String getNominalCurrency() {
        return nominalCurrency;
    }

    public void setNominalCurrency(String nominalCurrency) {
        this.nominalCurrency = nominalCurrency;
    }

    public double getCashAmountInstructed() {
        return cashAmountInstructed;
    }

    public void setCashAmountInstructed(double cashAmountInstructed) {
        this.cashAmountInstructed = cashAmountInstructed;
    }

    public double getCashAmountRemaining() {
        return cashAmountRemaining;
    }

    public void setCashAmountRemaining(double cashAmountRemaining) {
        this.cashAmountRemaining = cashAmountRemaining;
    }

    public String getCashAmountCurrency() {
        return cashAmountCurrency;
    }

    public void setCashAmountCurrency(String cashAmountCurrency) {
        this.cashAmountCurrency = cashAmountCurrency;
    }

    public String getBondsReceiver() {
        return bondsReceiver;
    }

    public void setBondsReceiver(String bondsReceiver) {
        this.bondsReceiver = bondsReceiver;
    }

    public String getBondsDeliverer() {
        return bondsDeliverer;
    }

    public void setBondsDeliverer(String bondsDeliverer) {
        this.bondsDeliverer = bondsDeliverer;
    }

    public String getCashReceiver() {
        return cashReceiver;
    }

    public void setCashReceiver(String cashReceiver) {
        this.cashReceiver = cashReceiver;
    }

    public String getCashDeliverer() {
        return cashDeliverer;
    }

    public void setCashDeliverer(String cashDeliverer) {
        this.cashDeliverer = cashDeliverer;
    }

    public String getSettlementType() {
        return settlementType;
    }

    public void setSettlementType(String settlementType) {
        this.settlementType = settlementType;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public String getParentInstructionReference() {
        return parentInstructionReference;
    }

    public void setParentInstructionReference(String parentInstructionReference) {
        this.parentInstructionReference = parentInstructionReference;
    }

    @Override
    public boolean matchReference(BOTransfer transfer) {
        boolean res = false;
        if (!Util.isEmpty(transfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST))){
            res = this.getSettlementReferenceInstructed().equalsIgnoreCase(transfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST));
        }
        if(!res && !Util.isEmpty(transfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST_2))){
            res =  this.getSettlementReferenceInstructed().equalsIgnoreCase(transfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST_2));
        }
        return res;
    }

    @Override
    public ReconCCPMatchingResult matchFields(BOTransfer transfer, double cashTolerance) {
        ReconCCPMatchingResult result = new ReconCCPMatchingResult(true, null, new ArrayList<>(), new ArrayList<>());
        result.setTransfer(transfer);
        Product product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), transfer.getProductId());

        if ("SECURITY".equalsIgnoreCase(transfer.getTransferType())) {
            if (!this.getIsin().equalsIgnoreCase(product.getSecCode("ISIN"))) {
                result.addError("ISIN");
            }

            if (this.getNominalRemaining() != Math.abs(transfer.getNominalAmount())) {
                result.addError("Nominal Remaining");
            }

        }

        final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols();
        decimalSymbol.setDecimalSeparator('.');
        NumberFormat numberFormat = new DecimalFormat("0.00000", decimalSymbol);
        numberFormat.setGroupingUsed(false);
        numberFormat.setRoundingMode(RoundingMode.DOWN);

        String extCashAmountRemaining = numberFormat.format(this.getCashAmountRemaining());
        String calOtherAmount = numberFormat.format(Math.abs((transfer.getOtherAmount())));

        if (!Objects.equals(extCashAmountRemaining, calOtherAmount)) {
            Log.warn(this, "Cash Amount Remaining not equals for transfer " + transfer.getLongId() +
                    ", Other Xfer Amount: [" + new Amount(transfer.getOtherAmount()) + "]. Apply tolerance "
                    + cashTolerance);
            if (!ReconCCPUtil.applyTolerance(numberFormat, extCashAmountRemaining, calOtherAmount, cashTolerance)) {
                result.addError("Cash Amount Remaining");
            } else {
                result.addWarning("Cash Amount Remaining");
            }
        }

        return result;
    }
}
