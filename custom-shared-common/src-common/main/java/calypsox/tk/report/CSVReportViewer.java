/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: Jimmy Ventura (jimmy.ventura@siag.es)
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.calypso.apps.reporting.PivotTableReportViewer;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.ReportView;
import com.calypso.tk.util.CSVUtil;

import calypsox.util.SantReportFormattingUtil;

public class CSVReportViewer extends com.calypso.tk.report.CSVReportViewer {

    private boolean showHeadings = false;
    
    private DefaultReportOutput output = null;

    @Override
    public void init(DefaultReportOutput out, ReportTemplate template, ReportView view, boolean arg3) {
    	this.output = out;
        super.init(out, template, view, arg3);
        String del = template.get("SANTCSV_DELIMITER");
        String showHead = template.get("SANTCSV_SHOWHEADER");
        if (!Util.isEmpty(del)) {
            setDelimiter(del);
            template.remove("SANTCSV_DELIMITER");
        }
        if (!Util.isEmpty(showHead)) {
            setShowHeadings(Boolean.parseBoolean(showHead));
            template.remove("SANTCSV_SHOWHEADER");
        }
    }

    /**
     * Overridden for V16 Migration, core method adds an space after every String's comma.
     *
     * @param type
     * @param value
     */
    @Override
    protected void formatCell(int type, Object value) {
        value = SantReportFormattingUtil.getInstance().formatEmptyCollectionForReporting(value);
        if (value instanceof String) {
            String stringValue = this.csvFormatting(value.toString());
            this.buffer.append(CSVUtil.escape(stringValue, this.delimiter)).append(this.delimiter);
        } else {
            super.formatCell(type, value);
        }
    }

    @Override
    public void setHeading(String[] headings) {
        super.setHeading(headings);

        if (!this.showHeadings) {
            this.buffer = new StringBuffer();
        }
    }

    private String csvFormatting(String toFormat) {
        return toFormat.replaceAll("(\n|\r)+", " ");
    }

    public CSVReportViewer() {
        super();
        super.delimiter = "";
    }

    public void setDelimiter(String delimiter) {
        super.delimiter = delimiter;

    }

    public void setShowHeadings(boolean showHeadings) {
        this.showHeadings = showHeadings;
    }
    
    
	public String getHTML() {
		if ( this.template != null && Util.isTrue(this.template.get("CSV Use Pivot Table")) && this.template.usePivot() && this.output != null && this.output.getReport() != null
				&& !this.output.getReport().isThinClient()) {
			PivotTableReportViewer pivotViewer = new PivotTableReportViewer((ReportPanel) null);
			this.output.format(pivotViewer);
			StringBuffer strBuf = pivotViewer.buildMasterTableModel().generateCSV(false);
			String coreCsvContentasString = strBuf.toString();
			ArrayList<List<String>> csvContent = new ArrayList<List<String>>();
			String[] coreCsvLines = coreCsvContentasString.split("\n");
			for (int i = 0; i < coreCsvLines.length; i++) {
				String[] coreCsvValues = coreCsvLines[i].split("\t");
				csvContent.add(Arrays.asList(coreCsvValues));
			}
			csvContent.remove(0); // delete 1st line.
			List<String> dataLine = csvContent.get(0);
			int nbPivotColumn = 0;
			for (String content : dataLine) { // get the numbe of pivot columns
				if (content.replaceAll(" ", "").equals("Data"))
					break;
				nbPivotColumn++;
			}
			csvContent.remove(0); // delete the data line.
			ArrayList<String> finalCsvContent = new ArrayList<String>();
			if (this.showHeadings) { // handle header
				int currentColumn =0;
				ArrayList<String> headerLine = new ArrayList<String>();
				for(String headerValue : csvContent.get(0)) {
					if (currentColumn < nbPivotColumn) {
						headerLine.add(CSVUtil.escape(headerValue.substring(1),this.delimiter));
					}
					else {
						headerLine.add(CSVUtil.escape(headerValue, this.delimiter));
					}
					currentColumn++;
				}
				finalCsvContent.add(StringUtils.join(headerLine, this.delimiter));
			}
			csvContent.remove(0); // remove header
			String[] lastPivotValue = new String[nbPivotColumn];
			for (List<String> line : csvContent) {
				int currentColumn = 0;
				ArrayList<String> newLine = new ArrayList<String>();
				for (String value : line) {
					String newValue = value;
					if (currentColumn < nbPivotColumn) {
						if (Util.isEmpty(newValue.trim())) {
							newValue = lastPivotValue[currentColumn];
						} else {
							lastPivotValue[currentColumn] = newValue;
						}
					}
					newLine.add(CSVUtil.escape(newValue, this.delimiter));
					currentColumn++;
				}
				finalCsvContent.add(StringUtils.join(newLine, this.delimiter));
			}
			return StringUtils.join(finalCsvContent, Util.LINE_SEPARATOR)+Util.LINE_SEPARATOR;
		} else {
			return super.getHTML();
		}
	}

}