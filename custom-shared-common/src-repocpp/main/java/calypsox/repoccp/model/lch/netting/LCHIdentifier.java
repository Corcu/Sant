package calypsox.repoccp.model.lch.netting;

import com.calypso.tk.core.JDate;

import java.util.Objects;

public class LCHIdentifier {
    private String isin;
    private String isinName;
    private String lchMarketCode;
    private String houseClient;
    private JDate settlementDate;
    private String settlementCurrency;
    private String membersCsdIcsdTriPartySystem;


    public String getIsin( ) {
      return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getLchMarketCode() {
        return lchMarketCode;
    }

    public void setLchMarketCode(String lchMarketCode) {
        this.lchMarketCode = lchMarketCode;
    }

    public String getIsinName() {
        return isinName;
    }

    public void setIsinName(String isinName) {
        this.isinName = isinName;
    }

    public String getHouseClient() {
        return houseClient;
    }

    public void setHouseClient(String houseClient) {
        this.houseClient = houseClient;
    }

    public JDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(JDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getSettlementCurrency() {
        return settlementCurrency;
    }

    public void setSettlementCurrency(String settlementCurrency) {
        this.settlementCurrency = settlementCurrency;
    }

    public String getMembersCsdIcsdTriPartySystem() {
        return membersCsdIcsdTriPartySystem;
    }

    public void setMembersCsdIcsdTriPartySystem(String membersCsdIcsdTriPartySystem) {
        this.membersCsdIcsdTriPartySystem = membersCsdIcsdTriPartySystem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LCHIdentifier)) return false;
        LCHIdentifier that = (LCHIdentifier) o;
        return isin.equals(that.isin)
             //   && lchMarketCode.equals(that.lchMarketCode)
                && houseClient.equals(that.houseClient)
                && settlementDate.equals(that.settlementDate)
                && settlementCurrency.equals(that.settlementCurrency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isin,
                //lchMarketCode,
                houseClient, settlementDate, settlementCurrency);
    }

    @Override
    public String toString() {
        return "LCHIdentifier{" +
                "isin='" + isin + '\'' +
                ", isinName='" + isinName + '\'' +
                ", lchMarketCode='" + lchMarketCode + '\'' +
                ", houseClient='" + houseClient + '\'' +
                ", settlementDate=" + settlementDate +
                ", settlementCurrency='" + settlementCurrency + '\'' +
                ", membersCsdIcsdTriPartySystem='" + membersCsdIcsdTriPartySystem + '\'' +
                '}';
    }
}
