package calypsox.tk.bo.mapping;

import java.util.HashMap;
import java.util.Map;

public class BODisponibleIssueTypeProductMapper {
    private static final Map<String, String> issueTypeToProductMap = new HashMap<>();
    private static final Map<String, String> issueTypeToProductSubtypeMap = new HashMap<>();
    private static final String DEFAULT_PRODUCT = "";
    private static final String DEFAULT_SUBTYPE = "520";

    /**
     * PRODUCTO	DESCRIPCIÓN	SUBTIPO	ISSUE_TYPE
     * 783	CEDULAS	520	RFCUSTCD
     * 420	LETRAS	520	RFCUSTLT
     * 482	BONOS	520	RFCUSTBO
     * 492	PAGARES	520	RFCUSTPG
     * 982	AUTOCARTERA BONOS	520	RFCUSTACBO
     * 983	AUTOCARTERA CÉDULAS	520	RFCUSTACCD
     *
     *-----------------------------------------------------------------------
     *
     * 923	GARANTÍAS (BLOQUEOS) BONOS	100	RFCUSTGRBO
     * 923	GARANTÍAS (BLOQUEOS) LETRAS	101	RFCUSTGRLT
     * 923	GARANTÍAS (BLOQUEOS) PAGARÉS	102	RFCUSTGRPG
     * 923	GARANTÍAS (BLOQUEOS) CEDULAS	103	RFCUSTGRCD
     * 924	PIGNORACIONES BONOS	100	RFCUSTPGBO
     * 924	PIGNORACIONES LETRAS	101	RFCUSTPGLT
     * 924	PIGNORACIONES PAGARÉS	102	RFCUSTPGPG
     * 924	PIGNORACIONES CEDULAS	103	RFCUSTPGCD
     *
     * @return
     */

    static {
        // Initialize issue type mapping
        issueTypeToProductMap.put("RFCUSTCD", "783");
        issueTypeToProductMap.put("RFCUSTLT", "420");
        issueTypeToProductMap.put("RFCUSTBO", "482");
        issueTypeToProductMap.put("RFCUSTPG", "492");
        issueTypeToProductMap.put("RFCUSTACBO", "982");
        issueTypeToProductMap.put("RFCUSTACCD", "983");
        issueTypeToProductMap.put("RFCUSTGRBO", "923");
        issueTypeToProductMap.put("RFCUSTGRLT", "923");
        issueTypeToProductMap.put("RFCUSTGRPG", "923");
        issueTypeToProductMap.put("RFCUSTGRCD", "923");
        issueTypeToProductMap.put("RFCUSTPGBO", "924");
        issueTypeToProductMap.put("RFCUSTPGLT", "924");
        issueTypeToProductMap.put("RFCUSTPGPG", "924");
        issueTypeToProductMap.put("RFCUSTPGCD", "924");
        issueTypeToProductMap.put("RFCUSTGRACBO", "923");
        issueTypeToProductMap.put("RFCUSTGRACCD", "923");
        issueTypeToProductMap.put("RFCUSTPGACBO", "924");
        issueTypeToProductMap.put("RFCUSTPGACCD", "924");

        // Initialize product subtype mapping
        issueTypeToProductSubtypeMap.put("RFCUSTGRBO", "100");
        issueTypeToProductSubtypeMap.put("RFCUSTGRLT", "101");
        issueTypeToProductSubtypeMap.put("RFCUSTGRPG", "102");
        issueTypeToProductSubtypeMap.put("RFCUSTGRCD", "103");
        issueTypeToProductSubtypeMap.put("RFCUSTPGBO", "100");
        issueTypeToProductSubtypeMap.put("RFCUSTPGLT", "101");
        issueTypeToProductSubtypeMap.put("RFCUSTPGPG", "102");
        issueTypeToProductSubtypeMap.put("RFCUSTPGCD", "103");
        issueTypeToProductSubtypeMap.put("RFCUSTGRACBO", "100");
        issueTypeToProductSubtypeMap.put("RFCUSTGRACCD", "103");
        issueTypeToProductSubtypeMap.put("RFCUSTPGACBO", "100");
        issueTypeToProductSubtypeMap.put("RFCUSTPGACCD", "103");
    }

    public String getProductType(String issueType) {
        return issueTypeToProductMap.getOrDefault(issueType, DEFAULT_PRODUCT);
    }

    public String getProductSubtype(String productSubtype) {
        return issueTypeToProductSubtypeMap.getOrDefault(productSubtype, DEFAULT_SUBTYPE);
    }
}
