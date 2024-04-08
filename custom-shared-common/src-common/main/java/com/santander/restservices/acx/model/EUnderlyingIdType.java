package com.santander.restservices.acx.model;

/**
 * underlyingIdType parameter enum
 *
 * @author x865229
 * date 25/11/2022
 */
public enum EUnderlyingIdType {
    ADO,
    ADO_LIST,
    CURRENCY,
    GL_CODE,
    ISIN,
    J_CODE,
    NAME,
    SEDOL,
    TO_CURRENCY,
    UNDERLYING;

    @Override
    public String toString() {
        return name();
    }
}
