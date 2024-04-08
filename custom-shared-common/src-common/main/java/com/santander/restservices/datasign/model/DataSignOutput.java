package com.santander.restservices.datasign.model;

import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class DataSignOutput extends ApiRestModelRoot {

  /** Generated Jwt - SCA Token */
  @JsonProperty("jwt")
  private String jwtToken;

  public DataSignOutput() {
    super();
  }

  public DataSignOutput(final DataSignOutput output) {
    this();
    loadModelData(output);
  }

  public String getJwtToken() {
    return jwtToken;
  }

  public void setJwtToken(String jwtToken) {
    this.jwtToken = jwtToken;
  }

  @Override
  public boolean checkModelDataLoaded() {
    return !Util.isEmpty(jwtToken);
  }

  @Override
  public void loadModelData(ApiRestModel model) {
    if (model != null && model instanceof DataSignOutput) {
      final DataSignOutput dataSignOutput = (DataSignOutput) model;
      setJwtToken(dataSignOutput.getJwtToken());
    }

  }

  @Override
  public Class<DataSignOutput> retriveModelClass() {
    return DataSignOutput.class;
  }

}
