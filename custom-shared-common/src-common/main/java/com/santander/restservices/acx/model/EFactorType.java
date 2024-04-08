package com.santander.restservices.acx.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.stream.Stream;

/**
 * factorType parameter enum
 *
 * @author x865229
 * date 25/11/2022
 */
public enum EFactorType {
    BND,
    CRV,
    DIV,
    IDX,
    IDX_BO("IDX (BO)"),
    ISSUERS,
    RPO,
    SPT;

    final String value;

    EFactorType() {
        value = name();
    }

    EFactorType(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

    public static EFactorType ofValueName(String valName) {
        return Stream.of(EFactorType.values())
                .filter(v -> v.value.equals(valName))
                .findFirst()
                .orElse(null);
    }
}
