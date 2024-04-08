package calypsox.tk.report;

import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

public interface IAnacreditReport {

     ReportOutput buildReportOutputFrom(ReportRow[] items, Vector errorMsgs);
}
