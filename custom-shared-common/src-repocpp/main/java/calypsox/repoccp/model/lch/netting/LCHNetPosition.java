package calypsox.repoccp.model.lch.netting;

public class LCHNetPosition {

    private String isin;

    private String isinName;
    private double nominalAmount;
    private String nominalCurrency;
    private double cashAmount;
    private String cashAmountCurrency;
    private String bondsReceiver;
    private String bondsDeliverer;
    private String cashReceiver;
    private String cashDeliverer;
    private String netPositionType;

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

    public double getNominalAmount() {
        return nominalAmount;
    }

    public void setNominalAmount(double nominalAmount) {
        this.nominalAmount = nominalAmount;
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

    public String getNetPositionType() {
        return netPositionType;
    }

    public void setNetPositionType(String netPositionType) {
        this.netPositionType = netPositionType;
    }

    @Override
    public String toString() {
        return "LCHNetPosition{" +
                "isin='" + isin + '\'' +
                ", isinName='" + isinName + '\'' +
                ", nominalAmount=" + nominalAmount +
                ", nominalCurrency='" + nominalCurrency + '\'' +
                ", cashAmount=" + cashAmount +
                ", cashAmountCurrency='" + cashAmountCurrency + '\'' +
                ", bondsReceiver='" + bondsReceiver + '\'' +
                ", bondsDeliverer='" + bondsDeliverer + '\'' +
                ", cashReceiver='" + cashReceiver + '\'' +
                ", cashDeliverer='" + cashDeliverer + '\'' +
                ", netPositionType='" + netPositionType + '\'' +
                '}';
    }
}
