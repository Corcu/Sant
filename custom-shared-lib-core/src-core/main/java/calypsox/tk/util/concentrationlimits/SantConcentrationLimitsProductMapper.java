package calypsox.tk.util.concentrationlimits;

import calypsox.tk.util.ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static calypsox.tk.util.ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES.TASK_MODE.EXPOSURE;

public class SantConcentrationLimitsProductMapper {
    private static final String ISIN = "ISIN";

    public static Map<SantConcentrationLimitsRuleType, String> mapProduct(
            Product product, SantConcentrationLimitsCache cache, ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES.TASK_MODE taskMode) {
        Map<SantConcentrationLimitsRuleType, String> productMap = new HashMap<>();

        String isin = product.getSecCode(ISIN);
        String country = null;
        String issuer = null;
        String bondType = product.getType() + "." + product.getSubType();

        int issuerId = 0;
        if (product instanceof Bond) {
            Bond bond = (Bond) product;
            issuerId = bond.getIssuerId();
            country = getCountryFromIssuerId(issuerId);
        } else if (product instanceof Equity) {
            Equity equity = (Equity) product;
            issuerId = equity.getIssuerId();
            country = getCountryFromIssuerId(issuerId);
        }

        if (issuerId > 0) {
            issuer = cache.getLegalEntityName(issuerId);
            if (Util.isEmpty(issuer)) {
                issuer = BOCache.getLegalEntityCode(DSConnection.getDefault(),
                        issuerId);
                cache.addLegalEntityName(issuerId, issuer);
            }
        }
        if (!Util.isEmpty(isin)) {
            productMap.put(SantConcentrationLimitsRuleType.SECURITY, isin);
        }
        if (!Util.isEmpty(country)) {
            productMap.put(getCountryType(taskMode), country);
        }
        if (!Util.isEmpty(issuer)) {
            productMap.put(getIssuerType(taskMode), issuer);
        }
        if (!Util.isEmpty(bondType)) {
            productMap.put(SantConcentrationLimitsRuleType.BONDTYPE, bondType);
        }

        return productMap;
    }


    /**
     * @param taskMode
     * @return
     */
    private static SantConcentrationLimitsRuleType getIssuerType(ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES.TASK_MODE taskMode) {
        SantConcentrationLimitsRuleType issuer = SantConcentrationLimitsRuleType.ISSUER;
        if (EXPOSURE.equals(taskMode)) {
            issuer = SantConcentrationLimitsRuleType.ISSUER_EXP;
        }
        return issuer;
    }

    /**
     * @param taskMode
     * @return
     */
    private static SantConcentrationLimitsRuleType getCountryType(ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES.TASK_MODE taskMode) {
        SantConcentrationLimitsRuleType country = SantConcentrationLimitsRuleType.COUNTRY;
        if (EXPOSURE.equals(taskMode)) {
            country = SantConcentrationLimitsRuleType.COUNTRY_EXP;
        }
        return country;
    }

    public static Map<Integer, Map<SantConcentrationLimitsRuleType, String>> mapProducts(
            List<Product> products, ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES.TASK_MODE taskMode) {
        Map<Integer, Map<SantConcentrationLimitsRuleType, String>> productConcentrationLimitValues = new HashMap<>();

        SantConcentrationLimitsCache cache = new SantConcentrationLimitsCache();
        for (Product product : products) {
            productConcentrationLimitValues.put(product.getId(),
                    SantConcentrationLimitsProductMapper.mapProduct(product,
                            cache, taskMode));
        }

        return productConcentrationLimitValues;
    }

    public static String getCountryFromIssuerId(int issuerId) {
        String country = null;

        if (issuerId > 0) {
            LegalEntity issuer = BOCache
                    .getLegalEntity(DSConnection.getDefault(), issuerId);
            if (issuer != null) {
                country = issuer.getCountry();
            }
        }

        return country;
    }
}
