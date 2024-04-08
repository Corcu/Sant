package calypsox.tk.anacredit.util;

import calypsox.tk.anacredit.api.AnacreditConstants;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;

public class ActivoOpValores {


    public static final String PP180 = "PP180";
    public static String RV030 = "RV030";
    public static String RV111 = "RV111";
    public static String RV170 = "RV170";
    public static String RV190 = "RV190";

    public static String RF165 = "RF165";
    public static String RF161 = "RF161";
    public static String RF163 = "RF163";
    public static String RF140 = "RF140";
    public static String RF150 = "RF150";
    public static String RF154 = "RF154";
    public static String RF010 = "RF010";
    public static String RF111 = "RF111";
    public static String RF110 = "RF110";
    public static String RF153 = "RF153";
    public static String RF152 = "RF152";


    private  static ActivoOpValores _instance;

    private ActivoOpValores() {
    }

    public static ActivoOpValores instance()  {
        if ( _instance == null)  {
            _instance = new ActivoOpValores();
        }
        return _instance;
    }

    public String get(Product product) {
        if (product != null)  {
            if (product instanceof Equity) {
                return getFromEquity((Equity) product);
            } else if ( product instanceof Bond) {
                return getFromBond((Bond) product);
            }
        }
        return null;
    }

    private String getFromEquity(Equity equity) {

        if  ("PEGROUP".equalsIgnoreCase(equity.getSecCode("EQUITY_TYPE"))) {
            return RV030;
        }
        else if ("DERSUS".equalsIgnoreCase(equity.getSecCode("EQUITY_TYPE"))) {
            return RV111;
        }
        else if ("PFI".equalsIgnoreCase(equity.getSecCode("EQUITY_TYPE"))) {
            return RV170;
        }
        else if ("PS".equalsIgnoreCase(equity.getSecCode("EQUITY_TYPE"))) {
            return RF165;
        }

        return RV190;
    }

    private String getFromBond(Bond bond) {

        String result = RF150;
        LegalEntity productIssuer = AnacreditMapper.getProductIssuer(bond);
        String isin = bond.getSecCode("ISIN");
        char[]  isinChars = bond.getSecCode("ISIN").toCharArray();

        if ("Y".equalsIgnoreCase(bond.getSecCode("IS_COVERED"))) {
            if ("0019".equalsIgnoreCase(bond.getSecCode("COLLATERAL_DESCRIPTION"))) {
                return RF161;
            }
            return RF163;
        }

        if ("YES".equalsIgnoreCase(bond.getSecCode("IS_SUBORDINATED"))) {
            if ( ("BO".equalsIgnoreCase(bond.getSecCode("ISSUE_TYPE"))
                    || "PG".equalsIgnoreCase(bond.getSecCode("ISSUE_TYPE"))) ) {
                return  RF140;
            }
            else if ("YES".equalsIgnoreCase(bond.getSecCode("IS_CONVERTIBLE"))) {
                return RF140;
            }
        }

        if ("YES".equalsIgnoreCase(bond.getSecCode("IS_OPTIONABLE"))) {
            return RF150;
        }
        else if ("YES".equalsIgnoreCase(bond.getSecCode("IS_EXDIVIDEND"))) {
            return RF150;
        }
        else if ("YES".equalsIgnoreCase(bond.getSecCode("IS_CONVERTIBLE"))) {
            return RF154;
        }

        if ("LT".equalsIgnoreCase(bond.getSecCode("ISSUE_TYPE"))) {
            return RF010;
        }

        if  ("PG".equalsIgnoreCase(bond.getSecCode("ISSUE_TYPE"))) {

            if (isIssuerFTJE(bond)) {
                return RF111;
            }

            if (isin.startsWith("ES")) {
                String sectorContable = AnacreditMapper.getLEAttribute(productIssuer, "SECTORCONTABLE");
                if ( isinChars[3] == '5'
                        && ( "086".equalsIgnoreCase(sectorContable)  || "084".equalsIgnoreCase(sectorContable))) {
                    return RF111;
                }
            }

            if ("Mtge".equalsIgnoreCase(bond.getSecCode("BOND_TIPOLOGY_3"))
                    && ("Mortgate".equalsIgnoreCase(bond.getSecCode("INDUSTRY_SECTOR")))
                            || "Asset Backed Securities".equalsIgnoreCase(bond.getSecCode("INDUSTRY_SECTOR"))) {
                return RF111;

            }
            return RF110;
        }

        if  ("BO".equalsIgnoreCase(bond.getSecCode("ISSUE_TYPE"))) {
            if (isIssuerFTJE(bond)) {
                return RF153;
            }

            if (isin.startsWith("ES")) {
                String sectorContable = AnacreditMapper.getLEAttribute(productIssuer, "SECTORCONTABLE");
                if ( isinChars[3] == '3' && ("086".equalsIgnoreCase(sectorContable))) {
                    return RF152;
                }
                if ( isinChars[3] == '3' && ("084".equalsIgnoreCase(sectorContable))) {
                    return RF153;
                 }
            }

            if ("Mtge".equalsIgnoreCase(bond.getSecCode("BOND_TIPOLOGY_3")))  {
                if ("Mortgate".equalsIgnoreCase(bond.getSecCode("INDUSTRY_SECTOR")))  {
                    return  RF152;
                }
                if ("Asset Backed Securities".equalsIgnoreCase(bond.getSecCode("INDUSTRY_SECTOR")))  {
                    return RF153;
                }
            }
        }

        return result;
    }

    private boolean isIssuerFTJE(Bond bond) {
        LegalEntity le = AnacreditMapper.getProductIssuer(bond);
        return (le != null) && le.getCode().equalsIgnoreCase("FTJE");
    }
}