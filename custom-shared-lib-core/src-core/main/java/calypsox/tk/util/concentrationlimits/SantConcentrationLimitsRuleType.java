package calypsox.tk.util.concentrationlimits;

import java.util.Arrays;
import java.util.List;

public enum SantConcentrationLimitsRuleType {
    SECURITY, COUNTRY, ISSUER, BONDTYPE, COUNTRY_EXP, ISSUER_EXP;

    public static List<SantConcentrationLimitsRuleType> STANDARD_TYPES = Arrays
            .asList(SECURITY, COUNTRY, ISSUER, BONDTYPE);
    public static List<SantConcentrationLimitsRuleType> EXPOSURE_TYPES = Arrays
            .asList(COUNTRY_EXP, ISSUER_EXP);

}