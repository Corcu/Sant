package calypsox.repoccp.model.lch;

import java.util.Objects;
import java.util.UUID;

public class LCHSetIdentifier {

    private String clearerId;
    private String clearerName;
    private String isin;
    private String isinName;
    private String lchMarketCode;
    private String houseClient;
    private String settlementDate;
    private String settlementCurrency;
    private String membersCsdIcsdTriPartySystem;
    private String couponIdentifier;

    private final String uniqueID = UUID.randomUUID().toString();


    public String getClearerId() {
        return clearerId;
    }

    public void setClearerId(String clearerId) {
        this.clearerId = clearerId;
    }

    public String getCouponIdentifier() {
        return couponIdentifier;
    }

    public void setCouponIdentifier(String couponIdentifier) {
        this.couponIdentifier = couponIdentifier;
    }

    public String getMembersCsdIcsdTriPartySystem() {
        return membersCsdIcsdTriPartySystem;
    }

    public void setMembersCsdIcsdTriPartySystem(String membersCsdIcsdTriPartySystem) {
        this.membersCsdIcsdTriPartySystem = membersCsdIcsdTriPartySystem;
    }

    public String getSettlementCurrency() {
        return settlementCurrency;
    }

    public void setSettlementCurrency(String settlementCurrency) {
        this.settlementCurrency = settlementCurrency;
    }

    public String getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(String settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getHouseClient() {
        return houseClient;
    }

    public void setHouseClient(String houseClient) {
        this.houseClient = houseClient;
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

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getClearerName() {
        return clearerName;
    }

    public void setClearerName(String clearerName) {
        this.clearerName = clearerName;
    }

    public String getID() {
        return uniqueID + isin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LCHSetIdentifier that = (LCHSetIdentifier) o;
        return Objects.equals(clearerId, that.clearerId) &&
                Objects.equals(clearerName, that.clearerName) &&
                Objects.equals(isin, that.isin) &&
                Objects.equals(isinName, that.isinName) &&
                Objects.equals(lchMarketCode, that.lchMarketCode) &&
                Objects.equals(houseClient, that.houseClient) &&
                Objects.equals(settlementDate, that.settlementDate) &&
                Objects.equals(settlementCurrency, that.settlementCurrency) &&
                Objects.equals(membersCsdIcsdTriPartySystem, that.membersCsdIcsdTriPartySystem) &&
                Objects.equals(couponIdentifier, that.couponIdentifier) &&
                Objects.equals(uniqueID, that.uniqueID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clearerId, clearerName, isin, isinName, lchMarketCode, houseClient, settlementDate,
                settlementCurrency, membersCsdIcsdTriPartySystem, couponIdentifier, uniqueID);
    }

    @Override
    public String toString() {
        return "LCHSetIdentifier{" +
                "clearerId='" + clearerId + '\'' +
                ", clearerName='" + clearerName + '\'' +
                ", isin='" + isin + '\'' +
                ", isinName='" + isinName + '\'' +
                ", lchMarketCode='" + lchMarketCode + '\'' +
                ", houseClient='" + houseClient + '\'' +
                ", settlementDate='" + settlementDate + '\'' +
                ", settlementCurrency='" + settlementCurrency + '\'' +
                ", membersCsdIcsdTriPartySystem='" + membersCsdIcsdTriPartySystem + '\'' +
                ", couponIdentifier='" + couponIdentifier + '\'' +
                ", uniqueID='" + uniqueID + '\'' +
                '}';
    }
}
