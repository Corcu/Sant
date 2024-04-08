package com.santander.restservices.acx.model;

/**
 * valueField element
 *
 * @author x865229
 * date 25/11/2022
 */
public class ACXValueField {
    private int valueType;
    private double value;

    public int getValueType() {
        return valueType;
    }

    public void setValueType(int valueType) {
        this.valueType = valueType;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ACXValueField{" +
                "valueType=" + valueType +
                ", value=" + value +
                '}';
    }
}
