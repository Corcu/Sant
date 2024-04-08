package calypsox.tk.util.concentrationlimits;

import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.ConcentrationLimit;
import com.calypso.tk.refdata.ConcentrationRule;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;

import java.util.*;
import java.util.Map.Entry;

// Project: Concentration Limits - Phase II

public class SantConcentrationLimitsRulesManager {

  private static final String CONCENTRATION_LIMIT_PREFIX = "CL_TECH";
  private static final String CONCENTRATION_RULE_PREFIX = "CR_TECH";

  private static final Map<SantConcentrationLimitsRuleType, String> RULE_TYPE_CATEGORY_MAP =
      new HashMap<>();

  static {
    RULE_TYPE_CATEGORY_MAP.put(
        SantConcentrationLimitsRuleType.SECURITY, ConcentrationRule.TYPE_CATEGORY);
    RULE_TYPE_CATEGORY_MAP.put(
        SantConcentrationLimitsRuleType.COUNTRY, ConcentrationRule.TYPE_CATEGORY);
    RULE_TYPE_CATEGORY_MAP.put(
        SantConcentrationLimitsRuleType.ISSUER, ConcentrationRule.TYPE_ISSUER);
    RULE_TYPE_CATEGORY_MAP.put(
        SantConcentrationLimitsRuleType.BONDTYPE, ConcentrationRule.TYPE_CATEGORY);
    RULE_TYPE_CATEGORY_MAP.put(
        SantConcentrationLimitsRuleType.COUNTRY_EXP, ConcentrationRule.TYPE_CATEGORY);
    RULE_TYPE_CATEGORY_MAP.put(
        SantConcentrationLimitsRuleType.ISSUER_EXP, ConcentrationRule.TYPE_ISSUER);
  }

  /**
   * Deletes all concentration rules generated automatically.
   *
   * @return A list that contains all id of the rules that have been deleted.
   */
  public static List<Integer> deleteAllConcentrationRules() {
    List<Integer> deletedRulesIds = new ArrayList<>();

    try {
      List<ConcentrationRule> allRules =
          ServiceRegistry.getDefault().getCollateralDataServer().loadAllConcentrationRule();
      List<String> allPrefixes = getAllConcentrationRulePrefixes();
      loopAndDeleteRules(allRules, allPrefixes, deletedRulesIds);
    } catch (CollateralServiceException e) {
      Log.error(
          SantConcentrationLimitsRulesManager.class.getCanonicalName(),
          "Could not delete concentration rules",
          e);
    }

    return deletedRulesIds;
  }

  public static ConcentrationRule createRule(
      SantConcentrationLimitsRuleType ruleType,
      Set<String> sdfNames,
      int contractId,
      double maxPercentage,
      double maxValue) {
    ConcentrationRule rule = new ConcentrationRule();

    String name = getConcentrationRuleName(ruleType, contractId);
    rule.setName(name);
    // V16 DOUBT
    // rule.setType(RULE_TYPE_CATEGORY_MAP.get(ruleType));
    List<ConcentrationLimit> limits =
        getConcentrationLimits(ruleType, sdfNames, contractId, maxPercentage, maxValue);
    rule.setLimits(limits);

    saveConcentrationRule(rule);

    rule = getConcentrationRule(name);

    return rule;
  }

  // Concentration Limits - Phase II
  public static List<Integer> getNewConcentrationRulesIds(
      CollateralConfig contract,
      List<Product> allProducts,
      Map<Integer, Map<SantConcentrationLimitsRuleType, String>> productSDFNames,
      JDate date) {
    List<Integer> concentrationRulesIds = new ArrayList<>();

    List<Product> products = getContractProducts(contract, allProducts);
    Map<SantConcentrationLimitsRuleType, Set<String>> sdfsByRuleType =
        getSDFsByRuleType(products, productSDFNames);
    Map<SantConcentrationLimitsRuleType, SantConcentrationLimitsValue> concentrationLimits =
        SantConcentrationLimitsUtil.getConcentrationLimits(contract, date);

    for (Entry<SantConcentrationLimitsRuleType, SantConcentrationLimitsValue> concentrationLimit :
        concentrationLimits.entrySet()) {
      SantConcentrationLimitsRuleType ruleType = concentrationLimit.getKey();
      SantConcentrationLimitsValue value = concentrationLimit.getValue();

      ConcentrationRule rule =
          SantConcentrationLimitsRulesManager.createRule(
              ruleType,
              sdfsByRuleType.get(ruleType),
              contract.getId(),
              value.getPercentage(),
              value.getAmount());
      if (rule != null) {
        concentrationRulesIds.add(rule.getId());
      }
    }

    return concentrationRulesIds;
  }
  // Concentration Limits - Phase II - End

  public static String getConcentrationRuleName(
      SantConcentrationLimitsRuleType ruleType, int contractId) {
    return CONCENTRATION_RULE_PREFIX + '_' + ruleType.name() + '_' + contractId;
  }

  private static List<ConcentrationLimit> getConcentrationLimits(
      SantConcentrationLimitsRuleType ruleType,
      Set<String> sdfNames,
      int contractId,
      double maxPercentage,
      double maxValue) {
    List<ConcentrationLimit> limitList = new ArrayList<>();

    int limitId = 1;
    for (String sdfName : sdfNames) {
      ConcentrationLimit limit =
          newLimit(ruleType, contractId, limitId, sdfName, maxPercentage, maxValue);
      limitList.add(limit);
      limitId++;
    }

    return limitList;
  }

  private static ConcentrationLimit newLimit(
      SantConcentrationLimitsRuleType ruleType,
      int contractId,
      int limitId,
      String sdfName,
      double maxPercentage,
      double maxValue) {
    ConcentrationLimit limit = new ConcentrationLimit();

    limit.setName(getConcentrationLimitName(ruleType, contractId, limitId));
    limit.setUnderlyingType(ConcentrationLimit.UNDERLYING_SECURITY);
    limit.setFilterName(sdfName);
    limit.setMaximumPercentage(maxPercentage);
    limit.setMaximumValue(maxValue);

    return limit;
  }

  private static String getConcentrationLimitName(
      SantConcentrationLimitsRuleType ruleType, int contractId, int limitId) {
    return CONCENTRATION_LIMIT_PREFIX + '_' + ruleType.name() + '_' + contractId + '_' + limitId;
  }

  public static void saveConcentrationRule(ConcentrationRule rule) {
    try {
      ServiceRegistry.getDefault(DSConnection.getDefault())
          .getCollateralDataServer()
          .saveWithoutAuthorization(rule, true);
    } catch (CollateralServiceException e) {
      Log.error(
          SantConcentrationLimitsRulesManager.class.getCanonicalName(),
          String.format("Cannot save concentration rule \"%s\"", rule.getName()),
          e);
    }
  }

  private static List<String> getAllConcentrationRulePrefixes() {
    List<String> allPrefixes = new ArrayList<>();

    List<SantConcentrationLimitsRuleType> allRuleTypes =
        Arrays.asList(SantConcentrationLimitsRuleType.values());
    for (SantConcentrationLimitsRuleType ruleType : allRuleTypes) {
      allPrefixes.add(CONCENTRATION_RULE_PREFIX + '_' + ruleType.name());
    }

    // Delete Exposure Concentration Rules too
    allPrefixes.addAll(getAllExposureConcentrationRulePrefixes());

    return allPrefixes;
  }

  private static ConcentrationRule getConcentrationRule(String name) {
    ConcentrationRule concentrationRule = null;
    try {
      concentrationRule =
          ServiceRegistry.getDefault().getCollateralDataServer().loadConcentrationRule(name);
    } catch (CollateralServiceException e) {
      Log.error(
          SantConcentrationLimitsRulesManager.class.getCanonicalName(),
          String.format("Cannot load concentration rule \"%s\"", name),
          e);
    }

    return concentrationRule;
  }

  private static List<Product> getContractProducts(
      CollateralConfig contract, List<Product> allProducts) {
    List<Product> eligibleProducts = new ArrayList<>();
    List<Product> productsToChoose = new ArrayList<>(allProducts);
    List<Product> productsToRemove = new ArrayList<>();

    List<StaticDataFilter> eligibilityFilters = contract.getEligibilityFilters();
    for (StaticDataFilter sdf : eligibilityFilters) {
      for (Product product : productsToChoose) {
        if (sdf.accept(product)) {
          eligibleProducts.add(product);
          productsToRemove.add(product);
        }
      }
      productsToChoose.removeAll(productsToRemove);
      productsToRemove.clear();
    }

    return eligibleProducts;
  }

  private static Map<SantConcentrationLimitsRuleType, Set<String>> getSDFsByRuleType(
      List<Product> products,
      Map<Integer, Map<SantConcentrationLimitsRuleType, String>> productSDFNames) {
    Map<SantConcentrationLimitsRuleType, Set<String>> sdfsByRuleType = new HashMap<>();
    for (SantConcentrationLimitsRuleType ruleType :
        Arrays.asList(SantConcentrationLimitsRuleType.values())) {
      sdfsByRuleType.put(ruleType, new TreeSet<String>());
    }

    for (Product product : products) {
      Map<SantConcentrationLimitsRuleType, String> productSDFName =
          productSDFNames.get(product.getId());
      for (Entry<SantConcentrationLimitsRuleType, String> productSDFNameEntry :
          productSDFName.entrySet()) {
        SantConcentrationLimitsRuleType ruleType = productSDFNameEntry.getKey();
        String sdfName = productSDFNameEntry.getValue();
        sdfsByRuleType.get(ruleType).add(sdfName);
      }
    }

    return sdfsByRuleType;
  }

  public static List<Integer> deleteAllExposureConcentrationRules() {
    List<Integer> deletedRulesIds = new ArrayList<>();

    try {
      List<ConcentrationRule> allRules =
          ServiceRegistry.getDefault().getCollateralDataServer().loadAllConcentrationRule();
      List<String> allPrefixes = getAllExposureConcentrationRulePrefixes();
      loopAndDeleteRules(allRules, allPrefixes, deletedRulesIds);
    } catch (CollateralServiceException e) {
      Log.error(
          SantConcentrationLimitsRulesManager.class.getCanonicalName(),
          "Could not delete concentration rules",
          e);
    }

    return deletedRulesIds;
  }

  private static void loopAndDeleteRules(
      List<ConcentrationRule> allRules, List<String> allPrefixes, List<Integer> deletedRulesIds)
      throws CollateralServiceException {
    for (ConcentrationRule rule : allRules) {
      for (String prefix : allPrefixes) {
        if (rule.getName().startsWith(prefix)) {
          int ruleId = rule.getId();
          ServiceRegistry.getDefault().getCollateralDataServer().delete(rule);
          deletedRulesIds.add(ruleId);
        }
      }
    }
  }

  private static List<String> getAllExposureConcentrationRulePrefixes() {
    List<String> allPrefixes = new ArrayList<>();

    List<SantConcentrationLimitsRuleType> allExposureRuleTypes =
        SantConcentrationLimitsRuleType.EXPOSURE_TYPES;
    for (SantConcentrationLimitsRuleType ruleType : allExposureRuleTypes) {
      allPrefixes.add(CONCENTRATION_RULE_PREFIX + '_' + ruleType.name());
    }

    return allPrefixes;
  }

  public static List<Integer> getNewExposureConcentrationRulesIds(
      CollateralConfig contract,
      List<Product> allProducts,
      Map<Integer, Map<SantConcentrationLimitsRuleType, String>> productSDFNames) {
    List<Integer> concentrationRulesIds = new ArrayList<>();

    List<Product> products = getContractProducts(contract, allProducts);
    Map<SantConcentrationLimitsRuleType, Set<String>> sdfsByRuleType =
        getSDFsByRuleType(products, productSDFNames);

    // Build Concentration limits
    Map<SantConcentrationLimitsRuleType, SantConcentrationLimitsValue> concentrationLimits =
        new HashMap<>();
    SantConcentrationLimitsValue countryLimit =
        new SantConcentrationLimitsValue(
            SantConcentrationLimitsUtil.getDefaultCountryPercentageLimit(), 0.0);
    concentrationLimits.put(SantConcentrationLimitsRuleType.COUNTRY_EXP, countryLimit);
    SantConcentrationLimitsValue issuerLimit =
        new SantConcentrationLimitsValue(
            SantConcentrationLimitsUtil.getDefaultIssuerPercentageLimit(), 0.0);
    concentrationLimits.put(SantConcentrationLimitsRuleType.ISSUER_EXP, issuerLimit);

    for (Entry<SantConcentrationLimitsRuleType, SantConcentrationLimitsValue> concentrationLimit :
        concentrationLimits.entrySet()) {
      SantConcentrationLimitsRuleType ruleType = concentrationLimit.getKey();
      SantConcentrationLimitsValue value = concentrationLimit.getValue();

      ConcentrationRule rule =
          SantConcentrationLimitsRulesManager.createRule(
              ruleType,
              sdfsByRuleType.get(ruleType),
              contract.getId(),
              value.getPercentage(),
              value.getAmount());
      if (rule != null) {
        concentrationRulesIds.add(rule.getId());
      }
    }

    return concentrationRulesIds;
  }
}
