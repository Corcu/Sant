package com.santander.restservices.acx.model;

import java.util.List;

/**
 * riskfactor param type
 *
 * @author x865229
 * date 25/11/2022
 */
public class ACXRiskFactor {
    List<String> underlyings;

    public List<String> getUnderlyings() {
        return underlyings;
    }

    public void setUnderlyings(List<String> underlyings) {
        this.underlyings = underlyings;
    }
}
