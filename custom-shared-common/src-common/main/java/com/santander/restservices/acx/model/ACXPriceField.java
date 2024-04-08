package com.santander.restservices.acx.model;

/**
 * priceFields collection element
 *
 * @author x865229
 * date 25/11/2022
 */
public class ACXPriceField {
    private String id;
    private String name;
    private ACXValueField valueField;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ACXValueField getValueField() {
        return valueField;
    }

    public void setValueField(ACXValueField valueField) {
        this.valueField = valueField;
    }

    @Override
    public String toString() {
        return "ACXPriceField{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", valueField=" + valueField +
                '}';
    }
}
