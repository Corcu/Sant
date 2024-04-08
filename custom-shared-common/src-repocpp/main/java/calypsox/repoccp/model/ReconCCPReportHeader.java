package calypsox.repoccp.model;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;

public interface ReconCCPReportHeader {
    String getReportName();
    JDate getBusinessDate();
    int getTotalNoOfRecords();
    JDatetime getCreationTimestamp();
}
