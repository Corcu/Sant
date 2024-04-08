package calypsox.tk.event;

import com.calypso.tk.event.PSEvent;

public class PSEventResetCustomBOCache extends PSEvent {
	/**
	 * Custom event to reset the custom BOCaches for FailedTransferEngine.
	 */
	private static final long serialVersionUID = -3341595645947060654L;
	private String reportType=null;
	private String reportTemplate=null;
	private Boolean forceReset = true;
	
	public PSEventResetCustomBOCache(String reportType, String reportTemplate, Boolean forceReset) {
		super();
		this.reportType = reportType;
		this.reportTemplate = reportTemplate;
		this.forceReset = forceReset;
	}
	public String getReportType() {
		return reportType;
	}
	public void setReportType(String reportType) {
		this.reportType = reportType;
	}
	public String getReportTemplate() {
		return reportTemplate;
	}
	public void setReportTemplate(String reportTemplate) {
		this.reportTemplate = reportTemplate;
	}
	public Boolean getForceReset() {
		return forceReset;
	}
	public void setReportTemplate(Boolean forceReset) {
		this.forceReset = forceReset;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		if(o instanceof PSEventResetCustomBOCache) {
			PSEventResetCustomBOCache evt=(PSEventResetCustomBOCache)o;
			if(this.reportType!=null && evt.reportType!=null && this.reportType.equals(evt.reportType) &&
					this.reportTemplate!=null && evt.reportTemplate!=null && this.reportTemplate.equals(evt.reportTemplate)) {
				return super.equals(o);
			}else return false;			
		}else return false;		
	}

}
