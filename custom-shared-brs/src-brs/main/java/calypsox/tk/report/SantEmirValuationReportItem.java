/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * SantEmirValuationReportItem contains all values for one report's row.
 * 
 * 
 */
//CAL_EMIR_026
public class SantEmirValuationReportItem extends GenericReportItem {

    /**
     * 
     */
    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;

    /**
     * SANT_EMIR_VALUATION_ITEM key.
     */
    public static final String SANT_EMIR_VALUATION_ITEM = "SANT_EMIR_VALUATION_ITEM";

    /**
     * report type map
     */
    // CAL_EMIR_026
    protected final Map<String, String> reporTypeValues;

    /**
     * Instantiates a new generic report item.
     */
    public SantEmirValuationReportItem() {
        this.reporTypeValues = new HashMap<String, String>();
    }

    /**
     * get the column names
     * 
     * @return column names
     */
    public Set<String> getColumnNames() {
        return this.values.keySet();
    }

    /**
     * Gets the report type value.
     * 
     * @param columnName
     *            the column name
     * @return the report type value
     */
    public String getReportTypeValue(final String columnName) {
        return this.reporTypeValues.get(columnName);
    }

    /**
     * Sets the report type value.
     * 
     * @param columnName
     *            the column name
     * @param columnName
     *            the value
     */
    public void setReportTypeValue(final String columnName,
            final String reportType) {
        this.reporTypeValues.put(columnName, reportType);
    }

}
