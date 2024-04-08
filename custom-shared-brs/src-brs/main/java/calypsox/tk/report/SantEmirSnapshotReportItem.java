/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.core.Trade;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * SantEmirSnapshotReportItem contains all values for one report's row.
 * 
 * 
 */
public class SantEmirSnapshotReportItem extends GenericReportItem {
  /**
   * trade
   */
  private Trade trade;

  @SuppressWarnings("unused")
  private static final long serialVersionUID = 1L;
  /**
   * SANT_EMIR_SNAPSHOT_ITEM key.
   */

  public static final String SANT_EMIR_SNAPSHOT_ITEM = "SANT_EMIR_SNAPSHOT_ITEM";

  /**
   * report type map
   */
  // CAL_EMIR_026
  protected final Map<String, String> reporTypeValues;

  /**
   * Instantiates a new generic report item.
   */
  public SantEmirSnapshotReportItem() {
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
   * getter for tradeId
   * 
   * @return trade id
   */
  public Trade getTrade() {
    return this.trade;
  }

  /**
   * setter for tradeId
   * 
   * @param trade  trade id
   */
  public void setTrade(final Trade trade) {
    this.trade = trade;
  }

  /**
   * Gets the report type value.
   * 
   * @param columnName the column name
   * @return the report type value
   */
  public String getReportTypeValue(final String columnName) {
    return this.reporTypeValues.get(columnName);
  }

  /**
   * Sets the report type value.
   * 
   * @param columnName the column name
   * @param value the value
   */
  public void setReportTypeValue(final String columnName, final String reportType) {
    this.reporTypeValues.put(columnName, reportType);
  }

}
