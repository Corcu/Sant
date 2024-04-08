/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.core;

/**
 * Emir Report DB Bean
 *
 * @author Philippe Morange
 *
 */
public class GenericReg_EmirReport extends EmirReport {

  /**
   * serial version ID
   */
  private static final long serialVersionUID = 1L;

  /** Fixed Code */
  protected String fixedCode;

  /**
   * constructor
   */
  public GenericReg_EmirReport() {
    super();
  }

  /**
   * constructor
   */
  public GenericReg_EmirReport(EmirReport er) {
    this();
    setAction(er.getAction());
    setFarLeg(er.getFarLeg());
    setPartenonId(er.getPartenonId());
    setMurexTradeId(er.getMurexTradeId());
    setPo(er.getPo());
    setReportDate(er.getReportDate());
    setReportType(er.getReportType());
    setTag(er.getTag());
    setTradeId(er.getTradeId());
    setTradeVersion(er.getTradeVersion());
    setTranstype(er.getTranstype());
    setValue(er.getValue());

  }

  /**
   * getFixedCode.
   *
   * @return String
   */
  public String getFixedCode() {
    return fixedCode;
  }

  /**
   * setFixedCode.
   *
   * @param fixedCode
   *            String
   */
  public void setFixedCode(final String fixedCode) {
    this.fixedCode = fixedCode;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "[tradeVersion=" + tradeVersion + ", partenonId="
        + partenonId + ", action=" + action + ", transtype="
        + transtype + ", tradeId=" + tradeId
        + ", reportDate=" + reportDate + ", legType="
        + legType + ", tag=" + tag + ", value=" + value
        + ", po=" + po + ", reportType=" + reportType
        + ", fixedCode=" + fixedCode + "]";
  }

}
