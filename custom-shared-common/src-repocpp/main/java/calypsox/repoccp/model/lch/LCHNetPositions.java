package calypsox.repoccp.model.lch;

import calypsox.repoccp.ReconCCPConstants;
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
import java.util.*;

/**
 * @author aalonsop
 */

public class LCHNetPositions extends ReconCCPPosition {

    private double nominal;
    private String nominalCurrency;
    private double cashAmount;
    private String cashCurrency;
    private String bondsReceiver;
    private String bondsDeliverer;
    private String cashReceiver;
    private String cashDeliverer;
    private String netPositionType;

    private List<LCHObligations> obligations = new ArrayList<>();

    private LCHSetIdentifier obligationSetIdentifier;

    private LCHSetIdentifier nettingSetIdentifier;

    private final String uniqueID = UUID.randomUUID().toString();

    private boolean obligationSet = false;

    private boolean nettingSet = false;

    public LCHSetIdentifier getNettingSetIdentifier() {
        return nettingSetIdentifier;
    }

    public void setNettingSetIdentifier(LCHSetIdentifier nettingSetIdentifier) {
        this.nettingSetIdentifier = nettingSetIdentifier;
    }

    public LCHSetIdentifier getObligationSetIdentifier() {
        return obligationSetIdentifier;
    }

    public void setObligationSetIdentifier(LCHSetIdentifier identifier) {
        this.obligationSetIdentifier = identifier;
    }

    public List<LCHObligations> getObligations() {
        return obligations;
    }

    public void setObligations(List<LCHObligations> obligations) {
        this.obligations = obligations;
    }

    public void addObligation(LCHObligations obligation) {
        Optional.ofNullable(obligation).map(o -> obligations.add(o));
    }

    public String getNetPositionType() {
        return netPositionType;
    }

    public void setNetPositionType(String netPositionType) {
        this.netPositionType = netPositionType;
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

    public String getCashCurrency() {
        return cashCurrency;
    }

    public void setCashCurrency(String cashCurrency) {
        this.cashCurrency = cashCurrency;
    }

    public double getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(double cashAmount) {
        this.cashAmount = cashAmount;
    }

    public String getNominalCurrency() {
        return nominalCurrency;
    }

    public void setNominalCurrency(String nominalCurrency) {
        this.nominalCurrency = nominalCurrency;
    }

    public double getNominal() {
        return nominal;
    }

    public void setNominal(double nominal) {
        this.nominal = nominal;
    }

    @Override
    public ReconCCPMatchingResult matchFields(BOTransfer transfer, double cashTolerance) {
        ReconCCPMatchingResult result = new ReconCCPMatchingResult(true, null, new ArrayList<>(), new ArrayList<>());
        result.setTransfer(transfer);

        final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols();
        decimalSymbol.setDecimalSeparator('.');
        NumberFormat numberFormat = new DecimalFormat("0.00", decimalSymbol);
        numberFormat.setGroupingUsed(false);
        numberFormat.setRoundingMode(RoundingMode.DOWN);

        String extCashAmount = numberFormat.format(this.getCashAmount());
        String calOtherCashAmount = numberFormat.format(Math.abs(transfer.getOtherAmount()));


        if (!Objects.equals(extCashAmount, calOtherCashAmount)) {
            Log.warn(this, "Net Position Cash Amount  not equals for transfer " + transfer.getLongId() +
                    ", Other Xfer Amount: [" + new Amount(transfer.getOtherAmount()) + "]. Apply tolerance "
                    + cashTolerance);
            if (!ReconCCPUtil.applyTolerance(numberFormat, extCashAmount, calOtherCashAmount,cashTolerance)) {
                result.addError("Net Position Cash Amount NOT matched, Calypso Transfer ID " + transfer.getLongId() + ", Other Xfer Amount: [" + new Amount(transfer.getOtherAmount()) + "]");
            } else {
                result.addWarning("Net Position Cash Amount is within the tolerance of " + cashTolerance
                        + ", Calypso Transfer ID " + transfer.getLongId() + ", Other Xfer Amount: ["
                        + new Amount(transfer.getOtherAmount()) + "]");
            }
        }

        return result;
    }

    public boolean matchReference(BOTransfer transfer) {
        return this.getNominal() == Math.abs(transfer.getNominalAmount());
    }


    public String getUniqueID() {
        return uniqueID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LCHNetPositions that = (LCHNetPositions) o;
        return Double.compare(that.nominal, nominal) == 0 && Double.compare(that.cashAmount, cashAmount) == 0 &&
                Objects.equals(nominalCurrency, that.nominalCurrency) &&
                Objects.equals(cashCurrency, that.cashCurrency) &&
                Objects.equals(bondsReceiver, that.bondsReceiver) &&
                Objects.equals(bondsDeliverer, that.bondsDeliverer) &&
                Objects.equals(cashReceiver, that.cashReceiver) &&
                Objects.equals(cashDeliverer, that.cashDeliverer) &&
                Objects.equals(netPositionType, that.netPositionType) &&
                Objects.equals(obligations, that.obligations) &&
                Objects.equals(obligationSetIdentifier, that.obligationSetIdentifier) &&
                Objects.equals(uniqueID, that.uniqueID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nominal, nominalCurrency, cashAmount, cashCurrency, bondsReceiver, bondsDeliverer,
                cashReceiver, cashDeliverer, netPositionType, obligations, obligationSetIdentifier, uniqueID);
    }

    public boolean isObligationSet() {
        return obligationSet;
    }

    public void setObligationSet(boolean obligationSet) {
        this.obligationSet = obligationSet;
    }

    public boolean isNettingSet() {
        return nettingSet;
    }

    public void setNettingSet(boolean nettingSet) {
        this.nettingSet = nettingSet;
    }

    @Override
    public String toString() {
        return "LCHNetPositions{" +
                "nominal=" + nominal +
                ", nominalCurrency='" + nominalCurrency + '\'' +
                ", cashAmount=" + cashAmount +
                ", cashCurrency='" + cashCurrency + '\'' +
                ", bondsReceiver='" + bondsReceiver + '\'' +
                ", bondsDeliverer='" + bondsDeliverer + '\'' +
                ", cashReceiver='" + cashReceiver + '\'' +
                ", cashDeliverer='" + cashDeliverer + '\'' +
                ", netPositionType='" + netPositionType + '\'' +
                ", uniqueID='" + uniqueID + '\'' +
                ", obligationSet=" + obligationSet +
                ", nettingSet=" + nettingSet +
                '}';
    }
}
