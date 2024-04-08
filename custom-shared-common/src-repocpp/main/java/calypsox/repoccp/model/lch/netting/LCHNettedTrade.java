package calypsox.repoccp.model.lch.netting;

public class LCHNettedTrade {
    /*
    <ns1:trade>
		<ns1:registeredDate>2023-06-07</ns1:registeredDate>
        <ns1:registeredTime>06:16:50</ns1:registeredTime>
        <ns1:buyerSellerReference>R0024483690</ns1:buyerSellerReference>
        <ns1:tradeSourceName>Broker Tec</ns1:tradeSourceName>
        <ns1:lchNovatedTradeReference>S1Psa7Ex9e3PUu7v</ns1:lchNovatedTradeReference>
        <ns1:buyerSeller>B</ns1:buyerSeller>
        <ns1:tradeType>REPO</ns1:tradeType>
        <ns1:nominal>90000000.0</ns1:nominal>
        <ns1:nominalCurrency>GBP</ns1:nominalCurrency>
        <ns1:cashAmount>44366396.24</ns1:cashAmount>
        <ns1:cashAmountCurrency>GBP</ns1:cashAmountCurrency>
    </ns1:trade>
     */

    private String registeredDate;
    private String registeredTime;
    private String buyerSellerReference;
    private String tradeSourceName;
    private String lchNovatedTradeReference;
    private String buyerSeller;
    private String tradeType;
    private double nominal;
    private String nominalCurrency;
    private double cashAmount;
    private String cashAmountCurrency;

    public String getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(String registeredDate) {
        this.registeredDate = registeredDate;
    }

    public String getRegisteredTime() {
        return registeredTime;
    }

    public void setRegisteredTime(String registeredTime) {
        this.registeredTime = registeredTime;
    }

    public String getBuyerSellerReference() {
        return buyerSellerReference;
    }

    public void setBuyerSellerReference(String buyerSellerReference) {
        this.buyerSellerReference = buyerSellerReference;
    }

    public String getTradeSourceName() {
        return tradeSourceName;
    }

    public void setTradeSourceName(String tradeSourceName) {
        this.tradeSourceName = tradeSourceName;
    }

    public String getLchNovatedTradeReference() {
        return lchNovatedTradeReference;
    }

    public void setLchNovatedTradeReference(String lchNovatedTradeReference) {
        this.lchNovatedTradeReference = lchNovatedTradeReference;
    }

    public String getBuyerSeller() {
        return buyerSeller;
    }

    public void setBuyerSeller(String buyerSeller) {
        this.buyerSeller = buyerSeller;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public double getNominal() {
        return nominal;
    }

    public void setNominal(double nominal) {
        this.nominal = nominal;
    }

    public String getNominalCurrency() {
        return nominalCurrency;
    }

    public void setNominalCurrency(String nominalCurrency) {
        this.nominalCurrency = nominalCurrency;
    }

    public double getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(double cashAmount) {
        this.cashAmount = cashAmount;
    }

    public String getCashAmountCurrency() {
        return cashAmountCurrency;
    }

    public void setCashAmountCurrency(String cashAmountCurrency) {
        this.cashAmountCurrency = cashAmountCurrency;
    }

    @Override
    public String toString() {
        return "LCHNettedTrade{" +
                "registeredDate='" + registeredDate + '\'' +
                ", registeredTime='" + registeredTime + '\'' +
                ", buyerSellerReference='" + buyerSellerReference + '\'' +
                ", tradeSourceName='" + tradeSourceName + '\'' +
                ", lchNovatedTradeReference='" + lchNovatedTradeReference + '\'' +
                ", buyerSeller='" + buyerSeller + '\'' +
                ", tradeType='" + tradeType + '\'' +
                ", nominal=" + nominal +
                ", nominalCurrency='" + nominalCurrency + '\'' +
                ", cashAmount=" + cashAmount +
                ", cashAmountCurrency='" + cashAmountCurrency + '\'' +
                '}';
    }
}
