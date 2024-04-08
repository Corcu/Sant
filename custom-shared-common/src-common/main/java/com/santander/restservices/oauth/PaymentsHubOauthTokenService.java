package com.santander.restservices.oauth;

import com.calypso.tk.core.Util;

public class PaymentsHubOauthTokenService extends AbstractOauthTokenService {

  public static final String SERVICE_NAME = "PaymentsHub";

  public PaymentsHubOauthTokenService() {
    super(SERVICE_NAME);
  }

  @Override
  public boolean validateParameters() {
    return (jwt != null) && !Util.isEmpty(scope) && !Util.isEmpty(grant) && !Util.isEmpty(credentialType);
  }

}
