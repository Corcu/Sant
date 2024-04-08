package com.santander.restservices.acx.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

import java.util.Date;

/**
 * ACX prices endpoint request data model
 *
 * @author x865229
 * date 25/11/2022
 */
public class ACXPricesInput extends ApiRestModelRoot {

    private EArea area;
    private ELayerType layerType;
    private EUnit unit;
    private EAssetClass assetClass;
    private EFactorType factorType;
    private EUnderlyingIdType underlyingIdType;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private Date startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private Date endDate;
    private ACXRiskFactor riskFactor;

    public EArea getArea() {
        return area;
    }

    public void setArea(EArea area) {
        this.area = area;
    }

    public ELayerType getLayerType() {
        return layerType;
    }

    public void setLayerType(ELayerType layerType) {
        this.layerType = layerType;
    }

    public EUnit getUnit() {
        return unit;
    }

    public void setUnit(EUnit unit) {
        this.unit = unit;
    }

    public EAssetClass getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(EAssetClass assetClass) {
        this.assetClass = assetClass;
    }

    public EFactorType getFactorType() {
        return factorType;
    }

    public void setFactorType(EFactorType factorType) {
        this.factorType = factorType;
    }

    public EUnderlyingIdType getUnderlyingIdType() {
        return underlyingIdType;
    }

    public void setUnderlyingIdType(EUnderlyingIdType underlyingIdType) {
        this.underlyingIdType = underlyingIdType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public ACXRiskFactor getRiskFactor() {
        return riskFactor;
    }

    public void setRiskFactor(ACXRiskFactor riskFactor) {
        this.riskFactor = riskFactor;
    }

    @Override
    public boolean checkModelDataLoaded() {
        return area != null
                && layerType != null
                && unit != null
                && assetClass != null
                && factorType != null
                && underlyingIdType != null
                && startDate != null
                && endDate != null
                && riskFactor != null;
    }

    @Override
    public void loadModelData(ApiRestModel model) {
        if (model instanceof ACXPricesInput) {
            ACXPricesInput data = (ACXPricesInput)model;

            setArea(data.getArea());
            setLayerType(data.getLayerType());
            setUnit(data.getUnit());
            setAssetClass(data.getAssetClass());
            setFactorType(data.getFactorType());
            setUnderlyingIdType(data.getUnderlyingIdType());
            setStartDate(data.getStartDate());
            setEndDate(data.getEndDate());
            setRiskFactor(data.getRiskFactor());
        }
    }

    @Override
    public Class<? extends ApiRestModel> retriveModelClass() {
        return ACXPricesInput.class;
    }
}
