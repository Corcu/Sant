package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class PirumTransferReportTemplate extends ReportTemplate {

  private static final long serialVersionUID = 3338034850955955520L;


  @Override
  public void setDefaults() {
    super.setDefaults();
    this.put("Title", "PirumTransferReport");
  }

}