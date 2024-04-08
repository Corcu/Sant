package calypsox.tk.report;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.NettingMethod;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Vector;

public class NettingMethodsReportStyle extends ReportStyle {

    public static final String ID = "Id";
    public static final String LE = "Legal Entity";
    public static final String ROLE = "Role";
    public static final String PRODUCT_TYPE = "Product Type";
    public static final String CURRENCY = "Currency";
    public static final String PO = "Processing Org";
    public static final String METHOD = "Method";
    public static final String EFF_FROM = "Effective From";
    public static final String EFF_TO = "Effective To";
    public static final String SD_FILTER = "SD Filter";
    public static final String NETTING_TYPE = "Netting Type";

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        NettingMethod nettingMethod = Optional.ofNullable(row).map(ro -> (NettingMethod) ro.getProperty("NettingMethod")).orElse(null);
        LegalEntity legalEntity = Optional.ofNullable(row).map(ro -> (LegalEntity) ro.getProperty("LegalEntity")).orElse(null);
        LegalEntity processingOrg = Optional.ofNullable(row).map(ro -> (LegalEntity) ro.getProperty("ProcessingOrg")).orElse(null);

        if(null!=nettingMethod){
            if (ID.equals(columnName)) {
                return nettingMethod.getId();
            }else if(LE.equals(columnName)){
                return null!=legalEntity ? legalEntity.getCode() : "";
            }else if(ROLE.equals(columnName)){
                return nettingMethod.getLegalEntityRole();
            }else if(PRODUCT_TYPE.equals(columnName)){
                return Util.arrayToString(nettingMethod.getProductTypeList());
            }else if(CURRENCY.equals(columnName)){
                return nettingMethod.getCurrency();
            }else if(PO.equals(columnName)){
                return null!=processingOrg ? processingOrg.getCode() : "";
            }else if(METHOD.equals(columnName)){
                return nettingMethod.getSettleMethod();
            }else if(EFF_FROM.equals(columnName)){
                return nettingMethod.getEffectiveFrom();
            }else if(EFF_TO.equals(columnName)){
                return nettingMethod.getEffectiveTo();
            }else if(SD_FILTER.equals(columnName)){
                return nettingMethod.getStaticDataFilter();
            }else if(NETTING_TYPE.equals(columnName)){
                return nettingMethod.getNettingType();
            }
        }
        return "";
    }
}
