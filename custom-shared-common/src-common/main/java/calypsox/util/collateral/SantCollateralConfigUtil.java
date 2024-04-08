package calypsox.util.collateral;

import com.calypso.tk.collateral.client.registry.ClientServiceRegistry;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author aalonsop
 * CollateralConfig related methods utilities class
 */
public class SantCollateralConfigUtil {

    private static final String OLD_UNILATERAL_STR = "UNILATERAL";
    private static final String OLD_BILATERAL_STR = "BILATERAL";
    private static final Map<String, String> CONTRACT_DIRECTION_MAP = new HashMap<>();

    static {
        CONTRACT_DIRECTION_MAP.put(CollateralConfig.NET_UNILATERAL, OLD_UNILATERAL_STR);
        CONTRACT_DIRECTION_MAP.put(CollateralConfig.NET_BILATERAL, OLD_BILATERAL_STR);
    }

    private SantCollateralConfigUtil() {
        //HIDDEN CONSTRUCTOR
    }


    /**
     * @param collateralConfig
     * @return Empty if attribute does not exists
     */
    public static String getAdditionalField(CollateralConfig collateralConfig, String additionalFieldName) {
        String additionalField = collateralConfig.getAdditionalField(additionalFieldName);
        if (additionalField == null) {
            additionalField = "";
        }
        return additionalField;
    }

    /**
     * @param collateralConfig
     * @return All defined additionalFields, even null ones
     */
    public static Map<String, String> getAllAdditionalFields(CollateralConfig collateralConfig) {
        Map<String, String> allAFs = new HashMap<>();

        List<String> domainValueDefinedAdditionalFields = ClientServiceRegistry.getInstance().getDomainValuesClient().findValuesByName(CollateralConfig.DOMAIN_ADDITIONAL_FIELD);// 7276
        Map<String, String> contractAdditionalFields = collateralConfig.getAdditionalFields();

        if (!Util.isEmpty(contractAdditionalFields)) {
            allAFs.putAll(contractAdditionalFields);
        }
        if (!Util.isEmpty(domainValueDefinedAdditionalFields)) {
            for (String additionalField : domainValueDefinedAdditionalFields) {
                if (!contractAdditionalFields.containsKey(additionalField)) {
                    allAFs.put(additionalField, null);
                }
            }
        }
        return allAFs;
    }

    /**
     * @param currentContractDirectionValue
     * @return Previous contractDirection value. Report outputs must remain the same as before v16 version.
     */
    public static String getContractDirectionV14Value(CollateralConfig collateralConfig) {
        String oldContractDirectionValue = "";
        if (collateralConfig != null && !Util.isEmpty(collateralConfig.getContractDirection())) {
            oldContractDirectionValue = CONTRACT_DIRECTION_MAP.get(collateralConfig.getContractDirection());
        }
        return oldContractDirectionValue;
    }

    /**
     * @param config
     * @param columnName
     * @param reportStyle
     * @return
     */
    public static String overrideBookAndContractDirectionReportColumnValue(CollateralConfig config, String columnName, CollateralConfigReportStyle reportStyle) {
        String value = "";
        if (CollateralConfigReportStyle.BOOK.equals(columnName)) {
            value = reportStyle.getBookName(config, CollateralConfigReportStyle.INCOMING_CASH_BOOK);
        } else if (CollateralConfigReportStyle.CONTRACT_DIRECTION.equals(columnName)) {
            value = SantCollateralConfigUtil.getContractDirectionV14Value(config);
        }
        return value;
    }

    /**
     * @param row
     * @param columnName
     * @param reportStyle
     * @return
     */
    public static String overrideBookAndContractDirectionReportColumnValue(ReportRow row, String columnName, CollateralConfigReportStyle reportStyle) {
        String value = "";
        if (row != null) {
            CollateralConfig config = row.getProperty("MarginCallConfig");
            if (config != null) {
                value = overrideBookAndContractDirectionReportColumnValue(config, columnName, reportStyle);
            }
        }
        return value;
    }


}
