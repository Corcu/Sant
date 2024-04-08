package calypsox.tk.report;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;

public class BalanzaDePagosItem {

    String isin;
    int entityId;
    String currency;
    String inventoryType;
    String positionDetail;
    String positionType;
    Double nominal;
    Double marketValue;
    Product product;
    JDate positionDate;

    public  BalanzaDePagosItem(String isin, Integer entityId, String inventoryType, String positionDetail ) {
        this.isin = isin;
        this.entityId = entityId;
        this.inventoryType = inventoryType;
        this.positionDetail = positionDetail;
        nominal = 0.0;
        marketValue = 0.0;
    }


    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityID(int entityId) {
        this.entityId = entityId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setInventoryType(String inventoryType) {
        this.inventoryType = inventoryType;
    }

    public Double getNominal() {
        return nominal;
    }

    public void setNominal(Double nominal) {
        this.nominal = nominal;
    }

    public Double getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(Double marketValue) {
        this.marketValue = marketValue;
    }

    public JDate getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(JDate positionDate) {
        this.positionDate = positionDate;
    }

    public String getKey() {
        return new StringBuilder()
                .append(isin).append("_")
                .append(entityId)
                .append(inventoryType)
                .append(currency)
                .append(positionDetail).toString();
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public String getInventoryType() {
        return inventoryType;
    }

    public String getPositionDetail() {
        return positionDetail;
    }

    public void setPositionDetail(String positionDetail) {
        this.positionDetail = positionDetail;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getPositionType() {
        return positionType;
    }

    public void setPositionType(String positionType) {
        this.positionType = positionType;
    }
}
