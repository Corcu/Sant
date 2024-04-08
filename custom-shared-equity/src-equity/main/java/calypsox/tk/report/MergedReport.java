package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;

import calypsox.apps.reporting.MergedReportTemplatePanel;

public class MergedReport extends Report {

	public static final String ROW_REPORT_STYLE = "ROW_REPORT_STYLE";
	public static final String REVERSE_COLUMN_MAP = "REVERSE_COLUMN_MAP";
	public static final String ROW_NUMBER = "ROW_NUMBER";

	static HashSet<String> possibleColumns = new HashSet<String>();

	static {
		MergedReportStyle mergedReportStyle = new MergedReportStyle();
		for(int i = 0; i<mergedReportStyle.getPossibleColumnNames().length;i++) {
			possibleColumns.add(mergedReportStyle.getPossibleColumnNames()[i]);
		}
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	class ReportRowKey {
		HashMap<String, Object> keyValues = new HashMap<String, Object>();

		public ReportRowKey(HashMap<String, Object> keyValues) {
			this.keyValues = keyValues;
		}

		public ReportRowKey() {

		}

		public void addKey(String column, Object value) {
			keyValues.put(column, value);
		}

		public Object getKeyValue(String column) {
			return keyValues.get(column);
		}

		public boolean equals(ReportRowKey key) {
			if(keyValues.size()==0)
				return false;
			for(Map.Entry<String, Object> keyValue: keyValues.entrySet()) {
				if(!keyValue.getValue().equals(key.getKeyValue(keyValue.getKey()))) {
					return false;
				}
			}
			return true;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ReportOutput load(Vector errorMsgs) {


		boolean mergeRows = false;
		String templateType1 = this.getReportTemplate().get(MergedReportTemplatePanel.TEMPLATE_TYPE_1);
		String templateChoice1 = this.getReportTemplate().get(MergedReportTemplatePanel.TEMPLATE_CHOICE_1);
		String templateType2 = this.getReportTemplate().get(MergedReportTemplatePanel.TEMPLATE_TYPE_2);
		String templateChoice2 = this.getReportTemplate().get(MergedReportTemplatePanel.TEMPLATE_CHOICE_2);



		try {

			// generate reports

			Report reportToFormat1 = this.createReport(templateType1, templateChoice1, this.getPricingEnv());
			Report reportToFormat2 = this.createReport(templateType2, templateChoice2, this.getPricingEnv());

			ReportOutput output1 = generateReportOutput(reportToFormat1, this.getValuationDatetime(), errorMsgs );
			ReportOutput output2 = generateReportOutput(reportToFormat2, this.getValuationDatetime(), errorMsgs );


			// Manage column

			List<String> columns1 = Arrays.asList(reportToFormat1.getReportTemplate().getColumns());
			List<String> columns2 = Arrays.asList(reportToFormat2.getReportTemplate().getColumns());
			ArrayList<String> commonColumnNames = new ArrayList<String>();
			commonColumnNames.addAll(columns1);
			commonColumnNames.retainAll(columns2);

			// Manage column names

			Hashtable column2Hash =(Hashtable) reportToFormat2.getReportTemplate().getColumnNamesHash();
			Hashtable column1Hash =(Hashtable) reportToFormat1.getReportTemplate().getColumnNamesHash();
			Hashtable columnHash = new Hashtable();

			if(column2Hash!=null)
				columnHash.putAll(column2Hash);
			if(column1Hash!=null)
				columnHash.putAll(column1Hash);

			Hashtable currentColumnHash = getReportTemplate().getColumnNamesHash();
			if(currentColumnHash!=null)
				currentColumnHash.putAll(columnHash);
			else
				getReportTemplate().setColumnNamesHash(columnHash);


			HashMap<String, String> columnMap = new HashMap<String, String>();

			ArrayList<String> allColumnNames = new ArrayList<String>();
			allColumnNames.addAll(columns1);

			for(String column : columns2) {
				if(!allColumnNames.contains(column))
					allColumnNames.add(column);
			}

			String mergeMode = getReportTemplate().get(MergedReportTemplatePanel.COLUMN_MERGE_MODE);
			if(mergeMode==null)
				mergeMode =MergedReportTemplatePanel.COLUMN_MERGE_MODE_ID;

			if(mergeMode.equals(MergedReportTemplatePanel.COLUMN_MERGE_MODE_ID)) {
				if(getReportTemplate()!=null && getReportTemplate().getColumns()!=null)
					for(int i=0; i<getReportTemplate().getColumns().length; i++) {
						String columnName = getReportTemplate().getColumns()[i];
						if(possibleColumns.contains(columnName))
							allColumnNames.add(0,columnName);
					}
				getReportTemplate().setColumns(allColumnNames.toArray(new String[allColumnNames.size()]));
			}
			else if(mergeMode.equals(MergedReportTemplatePanel.COLUMN_MERGE_MODE_NAME)) {

				for(Object entry : columnHash.entrySet()) {
					Map.Entry<String, String> entryM = (Map.Entry<String, String>)entry;
					columnMap.put(entryM.getValue(), entryM.getKey());
				}

				ArrayList<String> allColumnNamesRenamed = new ArrayList<String>();

				for(String name : allColumnNames ) {
					String renamedColumn =(String) columnHash.get(name);
					if(renamedColumn!=null) {
						if(!allColumnNamesRenamed.contains(renamedColumn))
							allColumnNamesRenamed.add(renamedColumn);
					}
					else {
						if(!allColumnNamesRenamed.contains(name))
							allColumnNamesRenamed.add(name);
					}
				}
				if(getReportTemplate()!=null && getReportTemplate().getColumns()!=null)
					for(int i=0; i<getReportTemplate().getColumns().length; i++) {
						String columnName = getReportTemplate().getColumns()[i];
						if(possibleColumns.contains(columnName))
							allColumnNamesRenamed.add(0,columnName);
					}
				getReportTemplate().setColumns(allColumnNamesRenamed.toArray(new String[allColumnNamesRenamed.size()]));


			}
			else if(mergeMode.equals(MergedReportTemplatePanel.COLUMN_MERGE_MODE_NUMBER)) {

				int id = 0;
				ArrayList<String> columnNames = new ArrayList<String>();
				columnNames.addAll(columns1);
				for(String columnName : columns2) {
					if(columns1.size()>id)
						columnMap.put(columns1.get(id++), columnName);
					else {
						columnNames.add(columnName);
					}
				}

				if(getReportTemplate()!=null && getReportTemplate().getColumns()!=null)
					for(int i=0; i<getReportTemplate().getColumns().length; i++) {
						String columnName = getReportTemplate().getColumns()[i];
						if(possibleColumns.contains(columnName))
							columnNames.add(0,columnName);
					}
				getReportTemplate().setColumns(columnNames.toArray(new String[columnNames.size()]));

			}

			// Manage column format

			Hashtable format2Hash =(Hashtable) reportToFormat2.getReportTemplate().getColumnFormatsHash();
			Hashtable format1Hash =(Hashtable) reportToFormat1.getReportTemplate().getColumnFormatsHash();
			Hashtable formatHash = new Hashtable();

			if(format2Hash!=null)
				formatHash.putAll(format2Hash);
			if(format1Hash!=null)
				formatHash.putAll(format1Hash);

			getReportTemplate().setColumnFormats(formatHash);




			ReportStyle reportStyle1 = ReportStyle.getReportStyle(reportToFormat1.getType());
			ReportStyle reportStyle2 = ReportStyle.getReportStyle(reportToFormat2.getType());


			// sort

			this.getReportTemplate().setSortColumns(reportToFormat1.getReportTemplate().getSortColumns());
			this.getReportTemplate().setSortAscDesc(reportToFormat1.getReportTemplate().getSortAscDesc());


			// Merge rows

			LinkedHashMap<ReportRowKey, ReportRow> mergedRows = new LinkedHashMap<ReportRowKey, ReportRow>();



			if(output1 instanceof DefaultReportOutput && output2 instanceof DefaultReportOutput) {
				DefaultReportOutput defaultReportOutput1 = (DefaultReportOutput)output1;
				for(int i=0; i<defaultReportOutput1.getRows().length; i++) {
					//defaultReportOutput1.getRows()[i].setProperty(REVERSE_COLUMN_MAP, columnMap);
					ReportRow row = addRow(mergedRows,defaultReportOutput1.getRows()[i],reportStyle1,commonColumnNames,errorMsgs, mergeRows);
					//row.setProperty(REVERSE_COLUMN_MAP, columnMap);

				}
				DefaultReportOutput defaultReportOutput2 = (DefaultReportOutput)output2;
				for(int i=0; i<defaultReportOutput2.getRows().length; i++) {
					//defaultReportOutput2.getRows()[i].setProperty(REVERSE_COLUMN_MAP, columnMap);
					ReportRow row = addRow(mergedRows,defaultReportOutput2.getRows()[i],reportStyle2,commonColumnNames,errorMsgs, mergeRows);
					row.setProperty(REVERSE_COLUMN_MAP, columnMap);
				}
			}

			StandardReportOutput standardOutput = new StandardReportOutput(this);

			standardOutput.setRows(mergedRows.values().toArray(new ReportRow[mergedRows.values().size()]));

			standardOutput.sort();

			int rowNumber = 1;

			for(int i=0; i<standardOutput.getRows().length; i++) {
				standardOutput.getRows()[i].setProperty(ROW_NUMBER, rowNumber++);
			}

			return standardOutput;
		} catch (RemoteException e) {
			Log.error(this, e);
		}

		return null;
	}


	@SuppressWarnings("rawtypes")
	protected ReportRow addRow(LinkedHashMap<ReportRowKey, ReportRow> rowList, ReportRow row,  ReportStyle reportStyle, List<String> commonColumnNames , Vector errors, boolean mergeRows) {
		ReportRowKey rowKey = getRowKey(row, reportStyle, commonColumnNames, errors, mergeRows);
		ReportRow existingRow = rowList.get(rowKey);

		if(existingRow==null) {
			ArrayList<ReportRow> existingRows = new ArrayList<ReportRow>();
			existingRow = new ReportRow(existingRows);
			rowList.put(rowKey, existingRow);
		}
		ArrayList<ReportRow> existingRows = existingRow.getProperty(ReportRow.DEFAULT);
		row.setProperty(ROW_REPORT_STYLE, reportStyle);
		existingRows.add(row);
		return existingRow;
	}

	@SuppressWarnings("rawtypes")
	protected ReportRowKey getRowKey(ReportRow row, ReportStyle reportStyle, List<String> commonColumnNames, Vector errors, boolean mergeRows) {
		ReportRowKey key = new ReportRowKey();
		if(mergeRows) {
			for(String columnName : commonColumnNames) {
				key.addKey(columnName, reportStyle.getColumnValue(row, columnName, errors));
			}
		}
		return key;
	}

	@SuppressWarnings("rawtypes")
	private ReportOutput generateReportOutput(Report reportToFormat, JDatetime valDatetime,
											  Vector errorMsgs) throws RemoteException {

		Vector holidays = reportToFormat.getReportTemplate().getHolidays();
		if (!Util.isEmpty(holidays)) {
			reportToFormat.getReportTemplate().setHolidays(holidays);
		}

		if (this.getReportTemplate().getTimeZone() != null) {
			reportToFormat.getReportTemplate().setTimeZone(this.getReportTemplate().getTimeZone());
		}

		return reportToFormat.load(errorMsgs);

	}


	@SuppressWarnings({ "unused", "rawtypes" })
	private ReportOutput generateReportOutput(String type, String templateName, JDatetime valDatetime,
											  Vector errorMsgs) throws RemoteException {

		Report reportToFormat = this.createReport(type, templateName, this.getPricingEnv());
		if (reportToFormat == null) {
			Log.error(this, "Invalid report type: " + type);
			return null;
		} else if (reportToFormat.getReportTemplate() == null) {
			Log.error(this, "Invalid report template: " + type);
			return null;
		} else {
			Vector holidays = reportToFormat.getReportTemplate().getHolidays();
			if (!Util.isEmpty(holidays)) {
				reportToFormat.getReportTemplate().setHolidays(holidays);
			}

			if (this.getReportTemplate().getTimeZone() != null) {
				reportToFormat.getReportTemplate().setTimeZone(this.getReportTemplate().getTimeZone());
			}

			return reportToFormat.load(errorMsgs);
		}
	}

	protected Report createReport(String type, String templateName, PricingEnv env)
			throws RemoteException {
		Report report;
		try {
			String className = "tk.report." + type + "Report";
			report = (Report) InstantiateUtil.getInstance(className, true);
			report.setPricingEnv(env);
			report.setValuationDatetime(this.getValuationDatetime());
		} catch (Exception var8) {
			Log.error(this, var8);
			report = null;
		}

		if (report != null && !Util.isEmpty(templateName)) {
			ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
					.getReportTemplate(ReportTemplate.getReportName(type), templateName);
			if (template == null) {
				Log.error(this, "Template " + templateName + " Not Found for " + type + " Report");
			} else {
				report.setReportTemplate(template);
				template.setValDate(this.getValuationDatetime().getJDate(this.getReportTemplate().getTimeZone()));
				template.callBeforeLoad();
			}
		}

		return report;
	}



}


