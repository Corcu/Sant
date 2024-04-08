package com.santander.restservices;

import com.santander.restservices.paymentshub.PaymentsHubApiRestAdapter;

public class FactoryApiRestAdapter
{
  public static final String HTTPS_APIREST_ADAPTER = "HTTPS_APIREST_ADAPTER";
  public static final String PAYMENTSHUB_APIREST_ADAPTER = "PAYMENTSHUB_APIREST_ADAPTER";

  private FactoryApiRestAdapter()
  {
  }

  public static ApiRestAdapter getApiRestService()
  {
    return getApiRestService(HTTPS_APIREST_ADAPTER);
  }

  public static ApiRestAdapter getApiRestService(final String adapter) {

    if (PAYMENTSHUB_APIREST_ADAPTER.equals(adapter)) {
      return new PaymentsHubApiRestAdapter();
    } else if (HTTPS_APIREST_ADAPTER.equals(adapter)) {
      return new HttpApiRestAdapter();
    }

    return new HttpApiRestAdapter();
  }
}
