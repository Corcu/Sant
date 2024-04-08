package com.santander.restservices.acx.model;

/**
 * priceType param enum
 *
 * @author x865229
 * date 25/11/2022
 */
public enum EPriceType {
    ALL_DATA,
    BCE,
    CLOSE,
    DISC_FACTOR,
    EOD,
    MATURITY_DATE,
    MKQPrice,
    MKQPrice_Spread,
    MKQPrice_Spread_ZC,
    MKQPrice_ZC,
    N_DAYS,
    RENT,
    ZCR;

    @Override
    public String toString() {
        return name();
    }
}
