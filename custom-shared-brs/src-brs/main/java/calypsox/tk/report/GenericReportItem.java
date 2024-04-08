/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class GenericReportItem is used to store a row for a report
 */
public abstract class GenericReportItem {

    /** The values. */
    //CAL_WP_033. Needs to be an object in order to be sorted later
    protected final Map<String, Object> values;

    /**
     * Instantiates a new generic report item.
     */
    public GenericReportItem() {
        this.values = new HashMap<String, Object>();
    }

    /**
     * Gets the column value.
     * 
     * @param columnName
     *            the column name
     * @return the column value
     */
    public Object getColumnValue(final String columnName) {
        return this.values.get(columnName);
    }

    /**
     * Sets the column value.
     * 
     * @param columnName
     *            the column name
     * @param value
     *            the value
     */
    public void setColumnValue(final String columnName, final Object value) {
        this.values.put(columnName, value);
    }

}
