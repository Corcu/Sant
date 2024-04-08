package calypsox.repoccp.model.lch.netting;

public class LCHObligation {
    /*
    <ns1:settlementReferenceInstructed>100000NL6MZ2000</ns1:settlementReferenceInstructed>
					<ns1:lchCsdIcsdTriPartySystem>CRE</ns1:lchCsdIcsdTriPartySystem>
					<ns1:membersAccount>NBUAG</ns1:membersAccount>
					<ns1:lchAccount>GIKAV</ns1:lchAccount>
					<ns1:nominalInstructed>50000000.0</ns1:nominalInstructed>
					<ns1:nominalCurrency>GBP</ns1:nominalCurrency>
					<ns1:cashAmountInstructed>24644278.08</ns1:cashAmountInstructed>
					<ns1:cashAmountCurrency>GBP</ns1:cashAmountCurrency>
					<ns1:bondsReceiver>RDBSL</ns1:bondsReceiver>
					<ns1:bondsDeliverer>LCH</ns1:bondsDeliverer>
					<ns1:cashReceiver>LCH</ns1:cashReceiver>
					<ns1:cashDeliverer>RDBSL</ns1:cashDeliverer>
					<ns1:settlementType>DvP</ns1:settlementType>
     */

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
    public LCHObligation(){

    }
    public LCHObligation(LCHObligation other) {
        this.settlementReferenceInstructed = other.settlementReferenceInstructed;
        this.lchCsdIcsdTriPartySystem = other.lchCsdIcsdTriPartySystem;
        this.membersAccount = other.membersAccount;
        this.lchAccount = other.lchAccount;
        this.nominalInstructed = other.nominalInstructed;
        this.nominalCurrency = other.nominalCurrency;
        this.cashAmountInstructed = other.cashAmountInstructed;
        this.cashAmountCurrency = other.cashAmountCurrency;
        this.bondsReceiver = other.bondsReceiver;
        this.bondsDeliverer = other.bondsDeliverer;
        this.cashReceiver = other.cashReceiver;
        this.cashDeliverer = other.cashDeliverer;
        this.settlementType = other.settlementType;
    }

    public String getSettlementReferenceInstructed() {
        return settlementReferenceInstructed;
    }

    public void setSettlementReferenceInstructed(String settlementReferenceInstructed) {
        this.settlementReferenceInstructed = settlementReferenceInstructed;
    }

    public String getLchCsdIcsdTriPartySystem() {
        return lchCsdIcsdTriPartySystem;
    }

    public void setLchCsdIcsdTriPartySystem(String lchCsdIcsdTriPartySystem) {
        this.lchCsdIcsdTriPartySystem = lchCsdIcsdTriPartySystem;
    }

    public String getMembersAccount() {
        return membersAccount;
    }

    public void setMembersAccount(String membersAccount) {
        this.membersAccount = membersAccount;
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

    @Override
    public String toString() {
        return "LCHObligation{" +
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
                '}';
    }
}
