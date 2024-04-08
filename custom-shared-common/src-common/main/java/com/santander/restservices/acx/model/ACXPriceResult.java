package com.santander.restservices.acx.model;

import java.util.List;

/**
 * ACX prices response element
 *
 * @author x865229
 * date 25/11/2022
 */
public class ACXPriceResult {
    private String underlying;
    private String symbol;
    private String description;
    private List<ACXStaticAttribute> staticAttributes;
    private List<ACXPrice> prices;

    public String getUnderlying() {
        return underlying;
    }

    public void setUnderlying(String underlying) {
        this.underlying = underlying;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ACXStaticAttribute> getStaticAttributes() {
        return staticAttributes;
    }

    public void setStaticAttributes(List<ACXStaticAttribute> staticAttributes) {
        this.staticAttributes = staticAttributes;
    }

    public List<ACXPrice> getPrices() {
        return prices;
    }

    public void setPrices(List<ACXPrice> prices) {
        this.prices = prices;
    }

    @Override
    public String toString() {
        return "ACXPriceResult{" +
                "underlying='" + underlying + '\'' +
                ", symbol='" + symbol + '\'' +
                ", description='" + description + '\'' +
                ", staticAttributes=" + staticAttributes +
                ", prices=" + prices +
                '}';
    }
}
