/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.core;

import com.calypso.tk.core.JDate;

import java.io.Serializable;

/**
 * Emir Report DB Bean
 *
 * @author Philippe Morange
 *
 */
public class EmirReport implements Serializable {

  private static final String FAR_LEG = "Far";

  private static final String NEAR_LEG = "Near";

  /**
   * serial version ID
   */
  private static final long serialVersionUID = 1L;

  /** trade version */
  protected int tradeVersion;

  /** Partenon Id */
  protected String partenonId;

  /** Murex Trade Id */
  protected String murexTradeId;


  /** Action */
  protected String action;

  /** trd */
  protected String transtype;

  /** Trade id */
  protected long tradeId;

  /** Report Date */
  protected JDate reportDate;

  /** Leg */
  protected String legType;

  /** Tag */
  protected String tag;

  /** Value */
  protected String value;

  /** PO */
  // CAL_EMIR_006
  protected String po;

  /** Report Type */
  // CAL_EMIR_026
  protected String reportType;

  /**
   * constructor
   */
  public EmirReport() {
    // nothing to do
  }

  /**
   * Getter for trade version
   *
   * @return trade version
   */
  public int getTradeVersion() {
    return tradeVersion;
  }

  /**
   * setter for trade Version
   *
   * @param tradeVersion
   *            trade version
   */
  public void setTradeVersion(final int tradeVersion) {
    this.tradeVersion = tradeVersion;
  }

  /**
   * Getter for trade Id
   *
   * @return trade Id
   */
  public long getTradeId() {
    return tradeId;
  }

  /**
   * setter for trade Id
   *
   * @param tradeId
   *            trade Id
   */
  public void setTradeId(final long tradeId) {
    this.tradeId = tradeId;
  }

  /**
   * returns the reportDate
   *
   * @return the reportDate
   */
  public JDate getReportDate() {
    return reportDate;
  }

  /**
   * set the reportDate field
   *
   * @param reportDate
   *            the reportDate to set
   */
  public void setReportDate(final JDate reportDate) {
    this.reportDate = reportDate;
  }

  /**
   * returns the tag
   *
   * @return the tag
   */
  public String getTag() {
    return tag;
  }

  /**
   * set the tag field
   *
   * @param tag
   *            the tag to set
   */
  public void setTag(final String tag) {
    this.tag = tag;
  }

  /**
   * returns the value
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * set the value field
   *
   * @param value
   *            the value to set
   */
  public void setValue(final String value) {
    this.value = value;
  }

  /**
   * returns the FarLeg value
   *
   * @return the value
   */
  public String getFarLeg() {
    String rst = NEAR_LEG;

    if (Boolean.TRUE.toString().equalsIgnoreCase(legType)) {
      rst = FAR_LEG;
    }

    return rst;
  }

  /**
   * set the leg field
   *
   * @param isFarLeg
   *            value to set
   */
  public void setFarLeg(final String isFarLeg) {
    legType = isFarLeg;
  }

  /**
   * return the Partenon Id value
   *
   * @return partenon Id
   */
  public String getPartenonId() {
    return partenonId;
  }

  /**
   * set the partenon Id field
   *
   * @param partenonId
   *            partenon Id
   */
  public void setPartenonId(final String partenonId) {
    this.partenonId = partenonId;
  }


  /**
   * return the Murex Trade Id value
   *
   * @return Murex Trade Id
   */
  public String getMurexTradeId() {
    return murexTradeId;
  }


  /**
   * set the Murex Trade Id field
   *
   * @param murexTradeId
   *            pmurexTradeId
   */
  public void setMurexTradeId(final String murexTradeId) {
    this.murexTradeId = murexTradeId;
  }

  /**
   * get the action field
   *
   * @return action
   */

  public String getAction() {
    return action;
  }

  /**
   * set the action field
   *
   * @param action
   *            action
   */
  public void setAction(final String action) {
    this.action = action;
  }

  /**
   * get the transtype field
   *
   * @return transtype
   */
  public String getTranstype() {
    return transtype;
  }

  /**
   * set the transtype field
   *
   * @param transtype
   *            transtype
   */
  public void setTranstype(final String transtype) {
    this.transtype = transtype;
  }

  /**
   * get the processing org
   *
   * @return po
   */
  public String getPo() {
    return po;
  }

  /**
   * set the processing org
   *
   * @param po
   *            po
   */
  public void setPo(final String po) {
    this.po = po;
  }

  /**
   * get the reportType
   *
   * @return report type
   */
  public String getReportType() {
    return reportType;
  }

  /**
   * set the report Type
   *
   * @param reportType
   *            report type
   */
  public void setReportType(final String reportType) {
    this.reportType = reportType;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "[tradeVersion=" + tradeVersion + ", partenonId="
        + partenonId + ", murexTradeId="+ murexTradeId + ", action=" + action + ", transtype="
        + transtype + ", tradeId=" + tradeId
        + ", reportDate=" + reportDate + ", legType="
        + legType + ", tag=" + tag + ", value=" + value
        + ", po=" + po + ", reportType=" + reportType + "]";
  }

}
