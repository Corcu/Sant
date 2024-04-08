package com.santander.restservices.paymentshub.model.submodel;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.DomainValues;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface PaymentsHubSenderModel {

  public static final String DN_PH_VALIDATION = "PaymentsHubValidation.";

  public boolean checkModelData();

  List<String> errors = new ArrayList<String>();

  public static void clearErrors() {
    errors.clear();
  }

  public static String errorsToString() {
    return StringUtils.join(errors, "|");
  }

  /**
   * Check value using DomainValues.
   *
   * @param name
   * @param value
   * @return
   */
  public static boolean checkValue(final String name, final String value) {
    boolean rst = true;

    if (!Util.isEmpty(value)) {
      String error = String.format("The field '%s' is incorrect. The field value '%s' is not a valid value.", name,
          value);

      if (!Util.isEmpty(name)) {
        // Remove whitespaces
        final String nameNoSpaces = name.replaceAll("\\s+", "");

        final String domainName = (nameNoSpaces.startsWith(DN_PH_VALIDATION)) ? nameNoSpaces : DN_PH_VALIDATION
            .concat(nameNoSpaces);

        // If do not exist DomainValue, not validate
        final List<String> domainValues = DomainValues.values(domainName);
        rst = Util.isEmpty(domainValues) ? true : domainValues.contains(value);

      } else {
        error = "The field name is Null.";
        rst = false;
      }

      if (!rst) {
        errors.add(error);
        Log.error(PaymentsHubSenderModel.class, error);
      }
    }

    return rst;

  }

  /**
   * Check value using pattern. If the value is null or empty, return true.
   *
   * @param name
   * @param value
   * @param regexPattern
   *
   * @return
   */
  public static boolean checkValue(final String name, final String value, final String regexPattern) {
    if (!Util.isEmpty(value)) {
      final Pattern pattern = Pattern.compile(regexPattern);
      final Matcher matcher = pattern.matcher(value);
      if (!matcher.find()) {
        final String error = String
            .format("The field '%s' is incorrect. The field value '%s' does not match pattern '%s'.", name, value,
                regexPattern);
        Log.error(PaymentsHubSenderModel.class, error);
        errors.add(error);
        return false;
      }
    }

    return true;

  }

  /**
   * Check Length. If the value is null or empty, return true.
   *
   * @param name
   * @param value
   * @param minLength
   * @param maxLength
   * @return
   */
  public static boolean checkValueLength(final String name, final String value, final int minLength, final int maxLength) {
    if (!Util.isEmpty(value) && !(value.length() >= minLength && value.length() <= maxLength)) {
      final String error = String.format(
          "The field '%s' is incorrect. The field value '%s' does not have the correct length between '%s' and '%s'.",
          name, value, String.valueOf(minLength), String.valueOf(maxLength));
      Log.error(PaymentsHubSenderModel.class, error);
      errors.add(error);
      return false;
    }
    return true;
  }

  /**
   * Check Maximum Length. If the value is null or empty, return true.
   *
   * @param name
   * @param value
   * @param maxLength
   * @return
   */
  public static boolean checkMaximumLength(final String name, final String value, final int maxLength) {
    if (!Util.isEmpty(value) && value.length() > maxLength) {
      final String error = String
          .format(
              "The field '%s' is incorrect. The field value '%s' does not have the correct length: value length '%s'; Max length '%s'.",
              name, value, String.valueOf(value.length()), String.valueOf(maxLength));
      Log.error(PaymentsHubSenderModel.class, error);
      errors.add(error);
      return false;
    }
    return true;
  }

  /**
   * Check list Size. If the value is null or empty, return true.
   *
   * @param name
   * @param value
   * @param minSize
   * @param maxSize
   * @return
   */
  public static boolean checkSize(final String name, final List<?> value, final int minSize, final int maxSize) {
    if (!Util.isEmpty(value) && !(value.size() >= minSize && value.size() <= maxSize)) {
      final String error = String.format(
          "The field '%s' is incorrect. The field size '%s' does not have the correct size between '%s' and '%s'.",
          name, String.valueOf(value.size()), String.valueOf(minSize), String.valueOf(maxSize));
      Log.error(PaymentsHubSenderModel.class, error);
      errors.add(error);
      return false;
    }
    return true;
  }

  /**
   * Check list Size. If the value is null or empty, return true.
   *
   * @param name
   * @param value
   * @param maxSize
   * @return
   */
  public static boolean checkMaximumSize(final String name, final List<?> value, final int maxSize) {
    if (!Util.isEmpty(value) && value.size() > maxSize) {
      final String error = String.format(
          "The field '%s' is incorrect. The field size '%s' does not have the correct size: Max size '%s'.", name,
          String.valueOf(value.size()), String.valueOf(maxSize));
      Log.error(PaymentsHubSenderModel.class, error);
      errors.add(error);
      return false;
    }
    return true;
  }

}
