package calypsox.tk.util.concentrationlimits;

import calypsox.tk.refdata.SantIssuerCountryStaticDataFilter;
import calypsox.tk.util.ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.service.DSConnection;

import java.util.*;

public class SantConcentrationLimitsSDFManager {

    private static final String SDF_NAME_PREFIX = "SDF_CL_TECH";
    private static final String SDF_PROTOTYPE_SUFIX = "PROTOTYPE";
    private static final String CL_SDF_GROUP = "ConcentrationLimitGroup";
    private static final Set<String> SDF_GROUPS = new HashSet<>();

    private static final Map<SantConcentrationLimitsRuleType, String> SDF_ELEMENT_NAME_MAP = new EnumMap<>(SantConcentrationLimitsRuleType.class);

    static {
        SDF_ELEMENT_NAME_MAP.put(SantConcentrationLimitsRuleType.BONDTYPE,
                StaticDataFilterElement.PRODUCT_SUB_TYPE);
        SDF_ELEMENT_NAME_MAP.put(SantConcentrationLimitsRuleType.COUNTRY,
                SantIssuerCountryStaticDataFilter.SDF_ATTRIBUTE_ISSUER_COUNTRY);
        SDF_ELEMENT_NAME_MAP.put(SantConcentrationLimitsRuleType.ISSUER,
                StaticDataFilterElement.SEC_ISSUER);
        SDF_ELEMENT_NAME_MAP.put(SantConcentrationLimitsRuleType.SECURITY,
                "SEC_CODE.ISIN");
        SDF_ELEMENT_NAME_MAP.put(SantConcentrationLimitsRuleType.COUNTRY_EXP,
                SantIssuerCountryStaticDataFilter.SDF_ATTRIBUTE_ISSUER_COUNTRY);
        SDF_ELEMENT_NAME_MAP.put(SantConcentrationLimitsRuleType.ISSUER_EXP,
                StaticDataFilterElement.SEC_ISSUER);
        //SDF_GROUP filling
        SDF_GROUPS.add(CL_SDF_GROUP);
    }

    private SantConcentrationLimitsSDFManager() {
        //EMPTY
    }

    private static StaticDataFilter obtainStaticDataFilter(
            SantConcentrationLimitsRuleType ruleType, String value) {
        String sdfName = getStaticDataFilterName(ruleType, value);
        StaticDataFilter sdf = getStaticDataFilter(sdfName);
        if (sdf == null) {
            sdf = createStaticDataFilter(ruleType, value);
            saveStaticDataFilter(sdf);
        }

        return sdf;
    }

    public static String obtainStaticDataFilterName(
            SantConcentrationLimitsRuleType ruleType, String value,
            SantConcentrationLimitsCache cache) {
        String sdfName = getStaticDataFilterName(ruleType, value);
        if (!cache.isSDFPresent(sdfName)) {
            StaticDataFilter sdf = obtainStaticDataFilter(ruleType, value);
            if (sdf != null) {
                cache.setSDFPresent(sdfName);
            }
        }

        return sdfName;
    }

    public static void removeStaticDataFilters(
            List<SantConcentrationLimitsRuleType> ruleTypes) {
        List<String> allNames = getAllStaticDataFilterNames();
        List<String> allPrefixes = getAllNamePrefixes(ruleTypes);

        for (String name : allNames) {
            for (String prefix : allPrefixes) {
                if (name.startsWith(prefix)) {
                    removeStaticDataFilter(name);
                }
            }
        }
    }

    private static String getPrototypeName(
            SantConcentrationLimitsRuleType ruleType) {
        String ruleTypeName = getRuleTypeName(ruleType);
        return '_' + SDF_NAME_PREFIX + '_' + ruleTypeName + '_'
                + SDF_PROTOTYPE_SUFIX + '_';
    }

    private static String getStaticDataFilterName(
            SantConcentrationLimitsRuleType ruleType, String value) {
        String ruleTypeName = getRuleTypeName(ruleType);
        value = value.replace(".", "_");
        return SDF_NAME_PREFIX + '_' + ruleTypeName + '_' + value;
    }

    private static StaticDataFilter getStaticDataFilter(String name) {
        StaticDataFilter sdf = null;

        try {
            sdf = DSConnection.getDefault().getRemoteReferenceData()
                    .getStaticDataFilter(name);
        } catch (CalypsoServiceException e) {
            Log.error(
                    SantConcentrationLimitsSDFManager.class.getCanonicalName(),
                    "Cannot load Static Data Filter \"" + name + "\"", e);
        }

        return sdf;
    }

    private static void saveStaticDataFilter(StaticDataFilter sdf) {
        try {
            DSConnection.getDefault().getRemoteReferenceData().save(sdf);
        } catch (CalypsoServiceException e) {
            Log.error(
                    SantConcentrationLimitsSDFManager.class.getCanonicalName(),
                    "Cannot save Static Data Filter \"" + sdf.getName() + "\"",
                    e);
        }
    }

    private static void removeStaticDataFilter(String name) {
        try {
            DSConnection.getDefault().getRemoteReferenceData()
                    .removeStaticDataFilter(name);
        } catch (CalypsoServiceException e) {
            Log.error(
                    SantConcentrationLimitsSDFManager.class.getCanonicalName(),
                    "Cannot remove Static Data Filter \"" + name + "\"", e);
        }
    }

    private static List<String> getAllStaticDataFilterNames() {
        List<String> allNames = new ArrayList<>();

        try {
            Vector<?> rawNames = DSConnection.getDefault()
                    .getRemoteReferenceData().getStaticDataFilterNames();
            for (Object rawName : rawNames) {
                if (rawName instanceof String) {
                    allNames.add((String) rawName);
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(
                    SantConcentrationLimitsSDFManager.class.getCanonicalName(),
                    "Cannot get all Static Data Filter Names", e);
        }

        return allNames;
    }

    private static List<String> getAllNamePrefixes(
            List<SantConcentrationLimitsRuleType> ruleTypes) {
        List<String> allPrefixes = new ArrayList<>();

        for (SantConcentrationLimitsRuleType ruleType : ruleTypes) {
            String prefix = SDF_NAME_PREFIX + '_' + ruleType.name() + '_';
            allPrefixes.add(prefix);
        }

        return allPrefixes;
    }

    private static StaticDataFilter getStaticDataFilterPrototype(
            SantConcentrationLimitsRuleType ruleType) {
        StaticDataFilter sdf = null;

        String prototypeName = getPrototypeName(ruleType);
        StaticDataFilter prototype = getStaticDataFilter(prototypeName);
        if (prototype != null) {
            sdf = prototype.clone();
        }

        return sdf;
    }

    private static StaticDataFilter createStaticDataFilter(
            SantConcentrationLimitsRuleType ruleType, String value) {
        StaticDataFilter sdf = getStaticDataFilterPrototype(ruleType);
        setStaticDataFilterValue(sdf, ruleType, value);

        return sdf;
    }

    private static void setStaticDataFilterValue(StaticDataFilter sdf,
                                                 SantConcentrationLimitsRuleType ruleType, String value) {
        Vector<String> values = new Vector<>();
        values.add(value);
        StaticDataFilterElement sdfElement = getElementByName(sdf,
                SDF_ELEMENT_NAME_MAP.get(ruleType));
        if (sdfElement != null) {
            sdfElement.setValues(values);
        } else {
            Log.error(
                    SantConcentrationLimitsSDFManager.class.getCanonicalName(),
                    String.format(
                            "Cannot find element with name \"%s\" in SDF \"%s\"",
                            SDF_ELEMENT_NAME_MAP.get(ruleType), sdf.getName()));
        }
        sdf.setGroups(SDF_GROUPS);
        sdf.setName(getStaticDataFilterName(ruleType, value));
    }

    /**
     * AAP bug fix
     *
     * @param sdf
     * @param attributeName
     * @return
     */
    private static StaticDataFilterElement getElementByName(
            StaticDataFilter sdf, String attributeName) {
        StaticDataFilterElement element = null;
        if (attributeName != null) {
            Vector<StaticDataFilterElement> elements = sdf.getElements();
            Iterator<StaticDataFilterElement> iElement = elements.iterator();
            while (element == null && iElement.hasNext()) {
                StaticDataFilterElement elementToCheck = iElement.next();
                if (elementToCheck != null) {
                    if (attributeName.equals(elementToCheck.getName())) {
                        element = elementToCheck;
                    }
                } else {
                    Log.warn(ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES.class, sdf.getName() + " has a NULL attribute, please check it at the StaticDataFilter Window.");
                }
            }
        } else {
            Log.warn(ScheduledTaskSANT_GENERATE_CONCENTRATION_RULES.class, "Couldn't get SDF element, attributeName is null (" + sdf.getName() + ")");
        }
        return element;
    }

    /**
     * @param type
     * @return
     */
    private static String getRuleTypeName(SantConcentrationLimitsRuleType type) {
        String name = type.name();

        if (SantConcentrationLimitsRuleType.COUNTRY_EXP == type) {
            name = SantConcentrationLimitsRuleType.COUNTRY.name();
        } else if (SantConcentrationLimitsRuleType.ISSUER_EXP == type) {
            name = SantConcentrationLimitsRuleType.ISSUER.name();
        }

        return name;
    }

}
