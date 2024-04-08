package calypsox.repoccp.model.lch.netting;

public class LCHNettingSetIdentifier extends LCHIdentifier {
    /*
    <ns2:dealerId>RDBSL</ns2:dealerId>
				<ns2:dealerName>Banco Santander Madrid</ns2:dealerName>
				<ns2:isin>GB00BNNGP775</ns2:isin>
				<ns2:isinName>UKT 0 7/8 01/31/46</ns2:isinName>
				<ns2:lchMarketCode>GB</ns2:lchMarketCode>
				<ns1:houseClient>H</ns1:houseClient>
				<ns1:settlementDate>2023-06-09</ns1:settlementDate>
				<ns1:settlementCurrency>GBP</ns1:settlementCurrency>
				<ns1:membersCsdIcsdTriPartySystem>G</ns1:membersCsdIcsdTriPartySystem>
     */

    private String dealerId;
    private String dealerName;


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

    @Override
    public String toString() {
        return "LCHNettingSetIdentifier{" +
                "dealerId='" + dealerId + '\'' +
                ", dealerName='" + dealerName + '\'' +
                '}';
    }
}
