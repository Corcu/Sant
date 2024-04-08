package calypsox.tk.util;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportViewer;
import com.calypso.tk.util.DisplayInBrowser;

import calypsox.tk.report.StandardReportOutput;

public class ScheduledTaskConsumerFoboIMConciliation extends ScheduledTaskCSVREPORT{
	
	
	private static final String FOBO_REPORT_VIWER = "CONFOBO";
	private static final String FOBO_FORMAT = "Consumer Format";
	private static final String NOM_COLM_NAME = "Nominal Column Name";
	
	
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = super.buildAttributeDefinition();
		attributeList.add(attribute(FOBO_FORMAT).booleanType());
		attributeList.add(attribute(NOM_COLM_NAME));
		return attributeList;
		
	}
	
	@Override 
	public Vector getAttributeDomain(final String attribute, final Hashtable hashtable) { 
		
		Vector vector = super.getAttributeDomain(attribute, hashtable);  
		if (attribute.equals(FOBO_FORMAT)) {
			 vector = new Vector();
			 vector.addElement("true");
			 vector.addElement("false");
			 return vector;
		} 
		return vector; 
	}
	
	@Override
	public Vector getDomainAttributes() {
		final Vector result = super.getDomainAttributes();
		result.add(FOBO_FORMAT);
		result.add(NOM_COLM_NAME);
		return result;
	}
	
	@Override
	protected String saveReportOutput(final ReportOutput reportOutput, String type, final String reportName,
			final String[] errors, final StringBuffer notifications) {
		final String delimiteur = getAttribute(DELIMITEUR);
		final String showheadings = getAttribute(SHOWHEADINGS);
		final String ctrlLine = getAttribute(CTRL_LINE);
		final String fileFormat = getAttribute(REPORT_FORMAT);
		final String nominalColumnName = getAttribute(NOM_COLM_NAME);

		boolean bShowHeadings = false;
		String type2 = "";
		// default will be showHeadings=false
		if ((showheadings != null) && showheadings.equals("true")) {
			bShowHeadings = true;
		} else {
			bShowHeadings = false;
		}

		Log.debug(Log.CALYPSOX, "Entering ScheduledTaskReport::reportViewer");

		if ((delimiteur == null) && !"Excel".equals(fileFormat) && (reportOutput instanceof StandardReportOutput)) {
			((StandardReportOutput) reportOutput).setDelimiteur("@");
			setCheckDelim(1);
		}

		if ((reportOutput instanceof StandardReportOutput) && (delimiteur != null) && !delimiteur.equals("")) {
			((StandardReportOutput) reportOutput).setDelimiteur(delimiteur);
		}
		if (reportOutput instanceof StandardReportOutput) {
			((StandardReportOutput) reportOutput).setShowHeadings(bShowHeadings);
		}
		if (type.equals("txt")) {
			type2 = "txt"; // for KGR export
		}
		if (type.equals("dat")) {
			type2 = "dat"; // for KGR export
		}
		if (type.equals("txt") || type.equals("dat")) {
			type = "csv";
		}

		// a silly workaround to convey the delimiter and the showheader info to
		// the CSV viewer!!!!
		((DefaultReportOutput) reportOutput).getReport().getReportTemplate().put("SANTCSV_DELIMITER", delimiteur);
		((DefaultReportOutput) reportOutput).getReport().getReportTemplate().put("SANTCSV_SHOWHEADER",
				"" + bShowHeadings);

		String reportStr = "";
		String foboformat = "false";
		if (!Util.isEmpty(getAttribute(FOBO_FORMAT)) && !getAttribute(FOBO_FORMAT).equalsIgnoreCase("false")) {
			foboformat = "true";
		}
		
		if(Boolean.parseBoolean(foboformat)) {
			ReportViewer viewer = DefaultReportOutput.getViewer(FOBO_REPORT_VIWER);
			if(!Util.isEmpty(nominalColumnName)) {
				((DefaultReportOutput) reportOutput).getReport().getReportTemplate().put(NOM_COLM_NAME,nominalColumnName);	
			}
			reportOutput.format(viewer);
			if(viewer!=null) {
				reportStr = viewer.toString();
				this.setFileName(DisplayInBrowser.buildDocument(reportStr, type, errors[0], false, 1));
			}
		}else {
			reportStr = super.saveReportOutput(reportOutput, type, reportName, errors, notifications);
		}
	
		// set extension
		String fileName = getFileName();
		if (fileName.startsWith("file://")) {
			fileName = fileName.substring(7);
		}

		if (type2.equals("txt")) {
			final String str1 = fileName.substring(0, fileName.lastIndexOf('.'));
			fileName = str1.concat(".txt");
		}

		if (type2.equals("dat")) {
			final String str1 = fileName.substring(0, fileName.lastIndexOf('.'));
			fileName = str1.concat(".dat");
		}

		// delete control separator for concrete cases (KGR)
		if (getCheckDelim() == 1) {
			reportStr = removeDelimiteurs(reportStr, '@');
		}

		// add header and footer if is required
		if ((getAttribute(START_HEADER) != null) && (!getAttribute(START_HEADER).equals(""))) {
			reportStr = getAttribute(START_HEADER) + "\n" + reportStr;
		}

		if ((getAttribute(FOOTER) != null) && (!getAttribute(FOOTER).equals(""))) {
			reportStr = reportStr + getAttribute(FOOTER);
		}

		// generate report file, with line control if is required
		if ((ctrlLine != null) && (ctrlLine.equals("false"))) {
			return generateReportFile(reportOutput, reportStr, fileName, false);
		} else {
			return generateReportFile(reportOutput, reportStr, fileName, true);
		}
	}
	
	
}
