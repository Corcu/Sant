package com.santander.restservices.oauth;

/**
 * OAuth service ACX
 *
 * @author x865229
 * date 25/11/2022
 */
public class ACXOauthTokenService extends AbstractOauthTokenService {

  public static final String SERVICE_NAME = "ACX";

  public ACXOauthTokenService() {
    super(SERVICE_NAME);
  }

}
