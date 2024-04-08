package calypsox.tk.report.restservices;

import java.util.Vector;

import com.calypso.tk.report.ReportOutput;

public interface WebServiceReport {
	
	public ReportOutput loadFromWS(String query, Vector<String> errorMsgs);
}
