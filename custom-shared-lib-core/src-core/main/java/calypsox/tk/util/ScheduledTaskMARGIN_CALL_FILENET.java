package calypsox.tk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.collateral.CollateralManager;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.MarginCallEntryFactory;
import com.calypso.tk.collateral.command.ACTION;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.OptimizationConfiguration;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskCOLLATERAL_MANAGEMENT;
import com.calypso.tk.util.TimingStatCollector;

import calypsox.tk.report.MarginCallEntryReport;

/**
 * 
 * @author x957355
 *
 */
public class ScheduledTaskMARGIN_CALL_FILENET extends ScheduledTaskCOLLATERAL_MANAGEMENT {

	private static final String ATTRIBUTE = "MarginCallEntry Attribute";
	private static final String ATTRIBUTE_VALUE = "MarginCallEntry Attribute Value";
	
	
	public boolean process(DSConnection ds, PSConnection ps) {
		ReportRow[] rows = null;
		List<MarginCallEntry> entries = new ArrayList<>();
		List<String> errors = new ArrayList<>();
        ExecutionContext context = this.getExecutionContext(this.getReportTemplate(), this.getMCProcessDate(this.getValuationDatetime()), this.getMCValuationDatetime(this.getValuationDatetime()));
        this.initContext(context);
        CollateralManager manager = new CollateralManager(context);
		String action = this.getAttribute(ATTRIBUTE_ACTION);
		try {
			rows = getMCEntries(ds);
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error getting MarginCall entries", e);
		}
		if (rows != null) {
			for (ReportRow row : rows) {
				MarginCallEntryDTO entry = row.getProperty("Default");
				if (entry != null) {
					
					MarginCallEntry ex = MarginCallEntryFactory.getInstance(entry).createMarginCallEntry(entry);
					ex.setAction(action);
					entries.add(ex);
				}

			}
			saveEntries(manager, entries, errors);
		}
		return true;
	}
	
	@Override
	protected List<JDatetime> getMCProcessDatetimes() {
		List<JDatetime> result = new ArrayList<JDatetime>();
		Holiday holiday = Holiday.getCurrent();

		JDate fromDate = holiday.addBusinessDays(this.getCurrentDate(), this._holidays, -this.getFromDays(), false);
		JDate toDate = holiday.addBusinessDays(this.getCurrentDate(), this._holidays, this.getToDays(), false);
		Vector<String> holidays = this.getHolidays();
		Log.debug(this, "Selecting business days (" + Util.collectionToString(holidays) + ") from " + fromDate + " to "
				+ toDate);
		for (; fromDate.lte(toDate); fromDate = fromDate.addDays(1)) {
			if (holiday.isBusinessDay(fromDate, holidays)) {
				result.add(this.getValuationDatetime(fromDate));
				Log.debug(this, fromDate + " is a business day");
			} 
		}

		return result;
	}

	@Override
	public boolean isValidInput(Vector messages) {
		boolean ret = true;

		if (Util.isEmpty(this.getTemplateName())) {
			messages.addElement("Must define Template");
			ret = false;
		}
		if (this.getCollateralContext() == null) {
			messages.addElement("Must define Collateral Context");
			ret = false;
		}

		return ret;
	}
	
	@Override
	protected void fillAdditionalInfos(List<MarginCallEntry> entries) {
		
		List<MarginCallEntry> entryList = new ArrayList<>();
		String attName = this.getAttribute(ATTRIBUTE);
		String attValue = this.getAttribute(ATTRIBUTE_VALUE);
		
		if( attName != null && attValue != null && !attName.isEmpty() && !attValue.isEmpty()) {
			for(MarginCallEntry entry: entries) {
				
				if(!(entry.getAttribute(attName) != null && (entry.getAttribute(attName).equals(attValue) 
						|| Boolean.valueOf(String.valueOf(entry.getAttribute(attName)))))) {
					entryList.add(entry);
				}
			}
		}
		entries.removeAll(entryList);
		super.fillAdditionalInfos(entries);
	}
	
	@Override
    public Vector<String> getDomainAttributes() {
        Vector<String> domainAttrs=super.getDomainAttributes();
        domainAttrs.add(ATTRIBUTE);
        domainAttrs.add(ATTRIBUTE_VALUE);
        return domainAttrs;
    }
	
	
	private ReportRow[] getMCEntries(DSConnection ds) throws CalypsoServiceException {
		
         ReportTemplate rt = ds.getRemoteReferenceData().getReportTemplate("MarginCallEntry", this.getAttribute("Template"));
         rt.put("ProcessStartDate",  this.getValuationDatetime().getJDate(this._timeZone).toString());
         rt.put("ProcessEndDate",  this.getValuationDatetime().getJDate(this._timeZone).toString());
         

         MarginCallEntryReport report = new MarginCallEntryReport();
         report.setReportTemplate(rt);
         DefaultReportOutput ro = (DefaultReportOutput) report.load(new Vector());
         ReportRow[] rows = ro.getRows();
         
         return rows;
	}
	
	public void saveEntries(CollateralManager manager,List<MarginCallEntry> processedMCEntryList,List<String> errors) {
        if (this.isCleanupRun()) {
            manager.getExecutor(ACTION.REMOVE, processedMCEntryList).execute();
        } else {
            if (this.isOptimize()) {
                long start = System.currentTimeMillis();
                OptimizationConfiguration config = this.getOptimizationConfiguration();
                if (config != null) {
                    manager.optimize(config, processedMCEntryList, errors);
                }
                long end = System.currentTimeMillis();
                TimingStatCollector.recordTiming("ScheduledTask-Optimize", end - start);
            }
            this.fillAdditionalInfos(processedMCEntryList);
            if (!this.isDryRun()) {
                manager.getExecutor(ACTION.SAVE, processedMCEntryList).execute();
            }
        }
    }
}
