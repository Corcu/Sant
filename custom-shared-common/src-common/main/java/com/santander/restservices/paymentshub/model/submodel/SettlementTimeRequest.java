package com.santander.restservices.paymentshub.model.submodel;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides information on the requested settlement time(s) of the payment instruction.
 */
public class SettlementTimeRequest implements PaymentsHubSenderModel {

  private static final String REGEX_TIME_FORMAT = "([01]?[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])Z'";

  private static final String CLS_TIME = "Clearing and settlement time";
  private static final String TILL_TIME = "Till time";
  private static final String FROM_TIME = "From time";
  private static final String REJECT_TIME = "Reject time";

  /** Clearing and settlement time - format: 'HH24:MM:SSZ' */
  @JsonProperty("clsTm")
  private String clsTm;

  /** Till time - format: 'HH24:MM:SSZ' */
  @JsonProperty("tillTm")
  private String tillTm;

  /** From time - format: 'HH24:MM:SSZ' */
  @JsonProperty("frTm")
  private String frTm;

  /** Reject time - format: 'HH24:MM:SSZ' */
  @JsonProperty("rjctTm")
  private String rjctTm;

  public String getClsTm() {
    return clsTm;
  }

  public void setClsTm(String clsTm) {
    this.clsTm = clsTm;
  }

  public String getTillTm() {
    return tillTm;
  }

  public void setTillTm(String tillTm) {
    this.tillTm = tillTm;
  }

  public String getFrTm() {
    return frTm;
  }

  public void setFrTm(String frTm) {
    this.frTm = frTm;
  }

  public String getRjctTm() {
    return rjctTm;
  }

  public void setRjctTm(String rjctTm) {
    this.rjctTm = rjctTm;
  }

  @Override
  public boolean checkModelData() {
    return checkFormatTime(CLS_TIME, clsTm) && checkFormatTime(TILL_TIME, tillTm) && checkFormatTime(FROM_TIME, frTm)
        && checkFormatTime(REJECT_TIME, rjctTm);
  }

  private boolean checkFormatTime(final String name, final String valueToCheck) {
    if (!Util.isEmpty(valueToCheck)) {
      final Pattern pattern = Pattern.compile(valueToCheck);
      final Matcher matcher = pattern.matcher(REGEX_TIME_FORMAT);
      if (!(matcher.find())) {
        final String error = String.format(
            "The field '%s' is incorrect. The field value '%s' does not have the correct format '%s'.", name,
            valueToCheck, REGEX_TIME_FORMAT);
        errors.add(error);
        Log.error(this, error);
      }
    }

    return true;

  }

}
