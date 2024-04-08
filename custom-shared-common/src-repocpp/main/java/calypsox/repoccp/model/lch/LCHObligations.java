package calypsox.repoccp.model.lch;

import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.ReconCCPMatchingResult;
import calypsox.repoccp.model.ReconCCPPosition;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Log;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import static calypsox.repoccp.ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST;
import static calypsox.repoccp.ReconCCPConstants.XFER_ATTR_TRADE_SOURCE;

/**
 * @author aalonsop
 */

public class LCHObligations extends ReconCCPPosition {

    private String settlementReferenceInstructed;
    private String lchCsdIcsdTriPartySystem;
    private String membersAccount;
    private String lchAccount;
    private double nominalInstructed;
    private String nominalCurrency;
    private double cashAmountInstructed;
    private String cashAmountCurrency;
    private String bondsReceiver;
    private String bondsDeliverer;
    private String cashReceiver;
    private String cashDeliverer;
    private String settlementType;
    private LCHSetIdentifier identifier = new LCHSetIdentifier();

    private final String uniqueID = UUID.randomUUID().toString();


    public LCHSetIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(LCHSetIdentifier identifier) {
        this.identifier = identifier;
    }

    public String getSettlementType() {
        return settlementType;
    }

    public void setSettlementType(String settlementType) {
        this.settlementType = settlementType;
    }

    public String getCashDeliverer() {
        return cashDeliverer;
    }

    public void setCashDeliverer(String cashDeliverer) {
        this.cashDeliverer = cashDeliverer;
    }

    public String getCashReceiver() {
        return cashReceiver;
    }

    public void setCashReceiver(String cashReceiver) {
        this.cashReceiver = cashReceiver;
    }

    public String getBondsDeliverer() {
        return bondsDeliverer;
    }

    public void setBondsDeliverer(String bondsDeliverer) {
        this.bondsDeliverer = bondsDeliverer;
    }

    public String getBondsReceiver() {
        return bondsReceiver;
    }

    public void setBondsReceiver(String bondsReceiver) {
        this.bondsReceiver = bondsReceiver;
    }

    public String getCashAmountCurrency() {
        return cashAmountCurrency;
    }

    public void setCashAmountCurrency(String cashAmountCurrency) {
        this.cashAmountCurrency = cashAmountCurrency;
    }

    public double getCashAmountInstructed() {
        return cashAmountInstructed;
    }

    public void setCashAmountInstructed(double cashAmountInstructed) {
        this.cashAmountInstructed = cashAmountInstructed;
    }

    public String getNominalCurrency() {
        return nominalCurrency;
    }

    public void setNominalCurrency(String nominalCurrency) {
        this.nominalCurrency = nominalCurrency;
    }

    public double getNominalInstructed() {
        return nominalInstructed;
    }

    public void setNominalInstructed(double nominalInstructed) {
        this.nominalInstructed = nominalInstructed;
    }

    public String getLchAccount() {
        return lchAccount;
    }

    public void setLchAccount(String lchAccount) {
        this.lchAccount = lchAccount;
    }

    public String getMembersAccount() {
        return membersAccount;
    }

    public void setMembersAccount(String membersAccount) {
        this.membersAccount = membersAccount;
    }

    public String getLchCsdIcsdTriPartySystem() {
        return lchCsdIcsdTriPartySystem;
    }

    public void setLchCsdIcsdTriPartySystem(String lchCsdIcsdTriPartySystem) {
        this.lchCsdIcsdTriPartySystem = lchCsdIcsdTriPartySystem;
    }

    public String getSettlementReferenceInstructed() {
        return settlementReferenceInstructed;
    }

    public void setSettlementReferenceInstructed(String settlementReferenceInstructed) {
        this.settlementReferenceInstructed = settlementReferenceInstructed;
    }


    @Override
    public ReconCCPMatchingResult matchFields(BOTransfer transfer, double cashTolerance) {
        ReconCCPMatchingResult result = new ReconCCPMatchingResult(true, null, new ArrayList<>(), new ArrayList<>());
        result.setTransfer(transfer);

        String couponIdentifier = this.getIdentifier().getCouponIdentifier();

        final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols();
        decimalSymbol.setDecimalSeparator('.');
        NumberFormat numberFormat = new DecimalFormat("0.00000", decimalSymbol);
        numberFormat.setGroupingUsed(false);
        numberFormat.setRoundingMode(RoundingMode.DOWN);

        String extCashAmountInstructed = numberFormat.format(this.getCashAmountInstructed());
        String calCashAmountInstructed = numberFormat.format(Math.abs(transfer.getOtherAmount()));

        if (!Objects.equals(extCashAmountInstructed, calCashAmountInstructed)) {
            Log.warn(this, "Cash Amount Instructed not equals for transfer " + transfer.getLongId() +
                    ", Other Xfer Amount: [" + new Amount(transfer.getOtherAmount()) +"]. Apply tolerance "
                    + cashTolerance);
            if(!ReconCCPUtil.applyTolerance(numberFormat, extCashAmountInstructed, calCashAmountInstructed, cashTolerance)) {
                result.addError("Cash Amount Instructed NOT matched, Calypso Transfer ID "
                        + transfer.getLongId() + ", Other Xfer Amount: [" + new Amount(transfer.getOtherAmount()) +"]");
            }else{
                result.addWarning("Cash Amount Instructed is within the tolerance of " + cashTolerance
                        + ", Calypso Transfer ID " + transfer.getLongId() + ", Other Xfer Amount: ["
                        + new Amount(transfer.getOtherAmount()) +"]");
            }
        }

        if ("Y".equalsIgnoreCase(couponIdentifier)) {
            result.addError("Transfer id " + transfer.getLongId() + " is linked to a coupon payment");
            transfer.setAttribute(XFER_ATTR_TRADE_SOURCE, "");
            transfer.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, "");
        } else {
            //fill settlement reference instructed for postprocesing
            transfer.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, this.getSettlementReferenceInstructed());
        }

        return result;
    }

    public boolean matchReference(BOTransfer transfer) {
        return this.getNominalInstructed() == Math.abs(transfer.getNominalAmount());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LCHObligations that = (LCHObligations) o;
        return Double.compare(that.nominalInstructed, nominalInstructed) == 0 &&
                Double.compare(that.cashAmountInstructed, cashAmountInstructed) == 0 &&
                Objects.equals(settlementReferenceInstructed, that.settlementReferenceInstructed) &&
                Objects.equals(lchCsdIcsdTriPartySystem, that.lchCsdIcsdTriPartySystem) &&
                Objects.equals(membersAccount, that.membersAccount) &&
                Objects.equals(lchAccount, that.lchAccount) &&
                Objects.equals(nominalCurrency, that.nominalCurrency) &&
                Objects.equals(cashAmountCurrency, that.cashAmountCurrency) &&
                Objects.equals(bondsReceiver, that.bondsReceiver) &&
                Objects.equals(bondsDeliverer, that.bondsDeliverer) &&
                Objects.equals(cashReceiver, that.cashReceiver) &&
                Objects.equals(cashDeliverer, that.cashDeliverer) &&
                Objects.equals(settlementType, that.settlementType) &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(uniqueID, that.uniqueID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settlementReferenceInstructed, lchCsdIcsdTriPartySystem, membersAccount, lchAccount,
                nominalInstructed, nominalCurrency, cashAmountInstructed, cashAmountCurrency, bondsReceiver,
                bondsDeliverer, cashReceiver, cashDeliverer, settlementType, identifier, uniqueID);
    }

    @Override
    public String toString() {
        return "LCHObligations{" +
                "settlementReferenceInstructed='" + settlementReferenceInstructed + '\'' +
                ", lchCsdIcsdTriPartySystem='" + lchCsdIcsdTriPartySystem + '\'' +
                ", membersAccount='" + membersAccount + '\'' +
                ", lchAccount='" + lchAccount + '\'' +
                ", nominalInstructed=" + nominalInstructed +
                ", nominalCurrency='" + nominalCurrency + '\'' +
                ", cashAmountInstructed=" + cashAmountInstructed +
                ", cashAmountCurrency='" + cashAmountCurrency + '\'' +
                ", bondsReceiver='" + bondsReceiver + '\'' +
                ", bondsDeliverer='" + bondsDeliverer + '\'' +
                ", cashReceiver='" + cashReceiver + '\'' +
                ", cashDeliverer='" + cashDeliverer + '\'' +
                ", settlementType='" + settlementType + '\'' +
                ", uniqueID='" + uniqueID + '\'' +
                '}';
    }
}
