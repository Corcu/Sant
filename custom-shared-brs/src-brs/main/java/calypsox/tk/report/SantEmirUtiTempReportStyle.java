/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import java.util.Vector;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;


public class SantEmirUtiTempReportStyle extends TradeReportStyle {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = -1L;

    
    /** TEMPORARY_UTI. */
    public static final String TEMPORARY_UTI = "Temporary UTI";

    
    /** MATURITY_DATE. */
    public static final String MATURITY_DATE = "Maturity Date";

 
    /** PRODUCT_TYPE. */
    public static final String PRODUCT_TYPE = "Product Type";
    
    /** VALUE_DATE. */
    public static final String VALUE_DATE = "Value Date";

    
    /** PRIMARY LEG AMOUNT. */
    public static final String PRIMARY_LEG_AMOUNT = "Primary Leg Amount";
    
    
    /** PRIMARY LEG CURRENCY. */
    public static final String PRIMARY_LEG_CURRENCY = "Primary Leg Currency";
    
    
    /** SECONDARY LEG AMOUNT. */
    public static final String SECONDARY_LEG_AMOUNT = "Secondary Leg Amount";
 
    
    /** SECONDARY LEG CURRENCY. */
    public static final String SECONDARY_LEG_CURRENCY = "Secondary Leg Currency";
    
    
    /** The Constant DEFAULTS_COLUMNS. */
    public static final String[] DEFAULTS_COLUMNS = { TEMPORARY_UTI, VALUE_DATE, MATURITY_DATE };

    
    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {
        Object value = "";
        final SantEmirUtiTempReportItem item = (SantEmirUtiTempReportItem) row.getProperty(SantEmirUtiTempReportItem.SANT_EMIR_UTI_TEMP_ITEM);

        if (columnName.equals(TEMPORARY_UTI)) {
            value = item.getColumnValue(TEMPORARY_UTI);
        }
        else if (columnName.equals(PRODUCT_TYPE)) {
            value = item.getColumnValue(PRODUCT_TYPE);
        }
        else if (columnName.equals(MATURITY_DATE)) {
            value = item.getColumnValue(MATURITY_DATE);
        }
        else if (columnName.equals(VALUE_DATE)) {
            value = item.getColumnValue(VALUE_DATE);
        }
        else if (columnName.equals(PRIMARY_LEG_AMOUNT)) {
            value = item.getColumnValue(PRIMARY_LEG_AMOUNT);
        }
        else if (columnName.equals(PRIMARY_LEG_CURRENCY)) {
            value = item.getColumnValue(PRIMARY_LEG_CURRENCY);
        }
        else if (columnName.equals(SECONDARY_LEG_AMOUNT)) {
            value = item.getColumnValue(SECONDARY_LEG_AMOUNT);
        }
        else if (columnName.equals(SECONDARY_LEG_CURRENCY)) {
            value = item.getColumnValue(SECONDARY_LEG_CURRENCY);
        }
        else {
            value = super.getColumnValue(row, columnName, errors);
        }

        return value;
    }

}
