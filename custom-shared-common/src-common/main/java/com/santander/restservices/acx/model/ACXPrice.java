package com.santander.restservices.acx.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

/**
 * prices collection element
 *
 * @author x865229
 * date 25/11/2022
 */
public class ACXPrice {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX")
    private Date recordDate;

    private List<ACXPriceField> fields;

    public Date getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(Date recordDate) {
        this.recordDate = recordDate;
    }

    public List<ACXPriceField> getFields() {
        return fields;
    }

    public void setFields(List<ACXPriceField> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "ACXPrice{" +
                "recordDate=" + recordDate +
                ", priceFields=" + fields +
                '}';
    }
}
