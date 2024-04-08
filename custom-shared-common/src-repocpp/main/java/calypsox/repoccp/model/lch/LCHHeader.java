package calypsox.repoccp.model.lch;

import calypsox.repoccp.model.ReconCCPReportHeader;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class LCHHeader implements ReconCCPReportHeader {
    /*
    	<ns1:header>
		<ns2:reportName>NETTING</ns2:reportName>
		<ns2:businessDate>2023-06-09</ns2:businessDate>
		<ns2:emptyReport>false</ns2:emptyReport>
		<ns2:totalNoOfRecords>148</ns2:totalNoOfRecords>
		<ns2:creationTimestamp>2023-06-09T11:45:39.518631</ns2:creationTimestamp>
		<ns2:sendTo>RDBSL</ns2:sendTo>
	</ns1:header>
     */

    private String reportName;
    private JDate businessDate;
    private String emptyReport;
    private int totalNoOfRecords;
    private String creationTimestamp;
    private String sendTo;

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public JDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(JDate businessDate) {
        this.businessDate = businessDate;
    }

    public String getEmptyReport() {
        return emptyReport;
    }

    public void setEmptyReport(String emptyReport) {
        this.emptyReport = emptyReport;
    }

    public int getTotalNoOfRecords() {
        return totalNoOfRecords;
    }

    public void setTotalNoOfRecords(int totalNoOfRecords) {
        this.totalNoOfRecords = totalNoOfRecords;
    }

    public JDatetime getCreationTimestamp() {
        try {
            return new JDatetime((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")).parse(creationTimestamp));
        } catch (ParseException e) {
            Log.error(this, e);
        }
        return null;
    }

    public void setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(String sendTo) {
        this.sendTo = sendTo;
    }

    @Override
    public String toString() {
        return "LCHHeader{" +
                "reportName='" + reportName + '\'' +
                ", businessDate=" + businessDate +
                ", emptyReport='" + emptyReport + '\'' +
                ", totalNoOfRecords=" + totalNoOfRecords +
                ", creationTimestamp='" + creationTimestamp + '\'' +
                ", sendTo='" + sendTo + '\'' +
                '}';
    }
}
