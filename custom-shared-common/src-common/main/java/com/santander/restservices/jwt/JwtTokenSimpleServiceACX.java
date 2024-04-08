package com.santander.restservices.jwt;

/**
 * Jwt service ACX
 *
 * @author x865229
 * date 25/11/2022
 */
public class JwtTokenSimpleServiceACX extends AbstractJwtTokenSimpleService {

    public static final String SERVICE_NAME = "ACX";

    public JwtTokenSimpleServiceACX() {
        super(SERVICE_NAME);
    }
}
