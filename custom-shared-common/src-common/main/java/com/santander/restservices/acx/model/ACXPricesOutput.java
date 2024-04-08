package com.santander.restservices.acx.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

import java.util.List;

/**
 * ACX prices endpoint response data model
 *
 * @author x865229
 * date 25/11/2022
 */
public class ACXPricesOutput extends ApiRestModelRoot {
    private List<ACXPriceResult> priceResultList;

    @JsonCreator
    public ACXPricesOutput(List<ACXPriceResult> priceResultList) {
        this.priceResultList = priceResultList;
    }

    public List<ACXPriceResult> getPriceResultList() {
        return priceResultList;
    }

    public void setPriceResultList(List<ACXPriceResult> priceResultList) {
        this.priceResultList = priceResultList;
    }

    @Override
    public boolean checkModelDataLoaded() {
        return priceResultList != null;
    }

    @Override
    public void loadModelData(ApiRestModel model) {
        if (model instanceof ACXPricesOutput) {
            setPriceResultList(((ACXPricesOutput) model).getPriceResultList());
        }
    }

    @Override
    public Class<? extends ApiRestModel> retriveModelClass() {
        return ACXPricesOutput.class;
    }
}
