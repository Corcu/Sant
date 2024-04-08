package calypsox.tk.report.restservices;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;


public abstract class AbstractReportDataWS {

	
	private static final String COMMENT_SEPARATOR = ";";
	
	private  String reportType;
	private  String reportTemplate;
	private  String entity;
	private String defaultOrderBy = "";
	public  int max_conc_req;
	public long timeout;
	protected AbstractReportDataWS(String reportType, String reportTemplate, String entity) {
		this.reportTemplate = reportTemplate;
		this.reportType = reportType;
		this.entity = entity;
		
		try {
			this.max_conc_req = Integer.parseInt(DomainValues.comment("WS.MaxConcurrentReq",reportType));
			this.timeout = Long.parseLong(DomainValues.comment("WS.MaxTimeWaiting",reportType));
		} catch (NumberFormatException  e) {
			Log.error(this, "Can not get the max concurrent number of request for " + reportType + " report",e);
			this.max_conc_req = 0;
			this.timeout = 0;
		}
	}
	
	
	public boolean checkReportSize(Report rep, int pageSize, int pageNumber, List<Object> pagination, Vector<String> errorMsgs) {	
		int limitValue = 0;
		int lastRow = pageSize*pageNumber;
		boolean result = true;
		
		try {
			limitValue = Integer.parseInt(DomainValues.comment("WS.MaxNumRows", reportType));
		} catch (Exception e){
			Log.error(this, "Can not obtain the max number of rows", e);
			return false;
		}
		
		int numRows = (int)rep.getPotentialSize().get(entity);
		int lastPage = (int) Math.ceil((double)numRows/pageSize);
		
		if(lastRow > numRows && pageNumber != 1 && pageNumber != lastPage) {
			errorMsgs.add("The page requested does not exists");
			return false;
		}
		getPaginationInfo(pageNumber, pageSize, numRows, pagination);
		if(limitValue == 0) {
			return true;
		}
		if(!(limitValue >= numRows)) {
			result = false;
			errorMsgs.add("Too many rows found. The max number of rows retrieved has been exceeded");
		}
		return result;
	}
	
	public boolean checkExportSize(Report rep, Vector<String> errorMsgs) {	
		int limitValue = 0;
		boolean result = true;
		
		try {
			limitValue = Integer.parseInt(DomainValues.comment("WS.MaxExportRows", reportType));
		} catch (Exception e){
			Log.error(this, "Can not obtain the max number of rows", e);
			return false;
		}
		
		int numRows = (int)rep.getPotentialSize().get(entity);
		
		
		if(numRows>limitValue) {
			result = false;
			errorMsgs.add("The max number of rows retrieved has been exceeded. The max number of rows to export is "+limitValue);
		}
		return result;
	}
	
	public boolean checkOrderByReport(List<String> orderOptions, Vector<String> errorMsgs) {
		
		boolean result = true;
		String groupValuesDV = DomainValues.comment("WS.OrderBy", reportType+"_"+reportTemplate);
		int i = 1;
		String nextComments = DomainValues.comment("WS.OrderBy", reportType + "_" + reportTemplate + i);
		while (!Util.isEmpty(nextComments)) {
			groupValuesDV = groupValuesDV.concat(COMMENT_SEPARATOR).concat(nextComments);
			i++;
			nextComments = DomainValues.comment("WS.OrderBy", reportType + "_" + reportTemplate + i);
		}
		DomainValues.comment("WS.OrderBy", reportType + "_" + reportTemplate);
		List<String> groupValListDV = Arrays.asList(groupValuesDV.split(COMMENT_SEPARATOR));
		if(orderOptions != null && !orderOptions.isEmpty()) {
			
			for(String elem : orderOptions) {
				//The order criteria will come with the + or - sign  in the last character
				if(!groupValListDV.contains(elem.substring(0, elem.length() - 1))) {
					result= false;
					errorMsgs.add(elem + " is not a valid order criteria");
				}
			}
			
		}

		return result;
	}
	
	
	
	public boolean checkGroupByReport(List<String> groupOptions, Vector<String> errorMsgs) {
		
		boolean result = true;
		if(groupOptions != null && !groupOptions.isEmpty()) {
			String groupValuesDV = DomainValues.comment("WS.groupBy", reportType+"_"+reportTemplate);
			List<String> groupValListDV = Arrays.asList(groupValuesDV.split(COMMENT_SEPARATOR));
			for(String elem : groupOptions) {
				if(!groupValListDV.contains(elem)) {
					result = false;
					errorMsgs.add(elem+" is not a valid grouping criteria");
				}
			}
		}
		return result;
	}
	
	public DefaultReportOutput executeReport(Report report, List<String> orderByOptions, List<String> groupByOptions,  int page,int pageSize,List<Object> pagination, Vector<String> errorMsgs ) throws InterruptedException, TimeoutException {
		DefaultReportOutput dro = null;
		try {
			if (adquireSemaphore()) {
				if(!checkReportSize(report,pageSize,page,pagination, errorMsgs) || !checkOrderByReport(orderByOptions, errorMsgs) || !checkGroupByReport(groupByOptions,errorMsgs)
						 || !checkPageSize(pageSize, errorMsgs)) {
					releaseSemaphore();
					return dro;
				}
				if(!checkBookFilter(report)) {
					return dro;
				}
				modifyTemplate(report);
				
				try {
					dro =(DefaultReportOutput) report.getClass().getMethod("loadFromWS", String.class, Vector.class).invoke(report, performPagination(page,pageSize, report, orderByOptions, groupByOptions,errorMsgs), errorMsgs);
				} catch (Exception e) {
					Log.error(this, "An error has been produced while executing report", e);
					errorMsgs.add(e.getMessage());
					return dro;
				} finally {
					releaseSemaphore();
				}
			} else {
				errorMsgs.add("Too many request enqueued. Maximum waiting time reached");
				throw new TimeoutException("Too many request enqueued. Maximum waiting time reached");
			}
			
		} catch (InterruptedException e) {
			
			Log.error(this, "The thread has been interrupted.", e);
			throw e;
			
		} catch (Exception e) {
			Log.error(this, "An error has been produced while checking report", e);
			return dro;
		}

		
		
		return dro;
	}
	
	public String performPagination(int page,int pageSize, Report report, List<String> orderByOptions, List<String> groupOptions, Vector<String> errorMsgs) {
		
		int offset;
		StringBuilder sb = new StringBuilder();
		if(page < 1) {
			page = 1;
		}
		if(Integer.parseInt(DomainValues.comment("WS.MaxPageSize", reportType+"_"+reportTemplate))==0){
			return "";
		}
		
		offset = (page-1) * pageSize;

		if(orderByOptions != null && !orderByOptions.isEmpty()) {
			String orderString = String.join(", ", orderByOptions);
			sb.append(" ORDER BY ");
			sb.append(orderString.replace("+", " ASC").replace("-", " DESC"));
		} else if (!Util.isEmpty(defaultOrderBy)) {
			sb.append(" ORDER BY ");
			sb.append(defaultOrderBy);
		}
		if(pageSize != 0) {
			sb.append(" OFFSET ");
			sb.append(offset);
			sb.append(" ROWS FETCH NEXT ");
			sb.append(pageSize);
			sb.append(" ROWS ONLY");
		}
		
		
		
		return sb.toString();
	}
	
	public boolean checkPageSize(int pageSize, Vector<String> errorMsgs) {
		boolean result;
		try {
			int maxPageSize = Integer.parseInt(DomainValues.comment("WS.MaxPageSize", reportType+"_"+reportTemplate));
			if(maxPageSize == 0 || maxPageSize > pageSize) {
				result = true;
			} else {
				result = false;
				errorMsgs.add("Max page size permitted is " + maxPageSize + ". Page size provided is: " + pageSize);
						
			}
			return result;
		} catch(Exception e) {
			Log.error(this, "Cannot check the page size",e);
			return false;
		}
		
	}
	
	private void getPaginationInfo(int page, int pageSize, int numRows, List<Object> list) {
		
		int lastPage = 1;
		int nextPage = 0;
		int previousPage = 0;
		double auxLast = (double)numRows/pageSize;
		int maxPageSize = Integer.parseInt(DomainValues.comment("WS.MaxPageSize", reportType+"_"+reportTemplate));

		if(maxPageSize == 0) {
			
			return;
		}
		 Map<String, Integer> pages = new HashMap<>();
		lastPage = (int) Math.ceil(auxLast);
		
		if(lastPage > page) {
			nextPage = page +1;
		}
		if(page != 1) {
			previousPage = page -1;
		}
		pages.put("self",page);
		pages.put("_first", 1);
		pages.put("_prev",previousPage);
		pages.put("_next", nextPage);
		pages.put("_last", lastPage);

		pages.put("_count", numRows);
		list.add(pages);
		
	}
	
	public DefaultReportOutput exportReport(Report report, Vector<String> errorMsgs ) throws InterruptedException, TimeoutException {
		DefaultReportOutput dro = null;
		try {
			if(adquireSemaphore()) {
				
				if(!checkExportSize(report, errorMsgs)) {
					releaseSemaphore();
					return dro;
				}
				modifyTemplate(report);
				
				try {
				dro = report.load(errorMsgs);
				} catch ( IllegalArgumentException  | SecurityException e) {
					Log.error(this, "An error has been produced while exporting report", e);
					return dro;
				} finally {
					releaseSemaphore();
				}

			} else {
				errorMsgs.add("Too many request enqueued. Maximum waiting time reached");
				throw new TimeoutException("Too many request enqueued. Maximum waiting time reached");
			}
			
		} catch (InterruptedException e) {
			
			Log.error(this, "The trhead has been interrupted.",e);
			throw e;
			
		}

		
		
		return dro;
	}
	
	abstract boolean adquireSemaphore() throws InterruptedException;
	abstract void releaseSemaphore();
	
	public boolean checkBookFilter(Report report) {
		return true;
	}
	
	public void modifyTemplate(Report report) {
		return;
	}
	

	public void setReportTemplate(String reportTemplate) {
		this.reportTemplate = reportTemplate;
	}

	public String getReportTemplate() {
		return reportTemplate;
	}

	public String getEntity() {
		return entity;
	}
	public String getReportType() {
		return reportType;
	}


	public void setReportType(String reportType) {
		this.reportType = reportType;
	}


	public String getDefaultOrderBy() {
		return defaultOrderBy;
	}


	public void setDefaultOrderBy(String defaultOrderBy) {
		this.defaultOrderBy = defaultOrderBy;
	}


	public void setEntity(String entity) {
		this.entity = entity;
	}
}
