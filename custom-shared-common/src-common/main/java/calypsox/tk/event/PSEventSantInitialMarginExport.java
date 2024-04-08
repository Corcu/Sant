package calypsox.tk.event;

import com.calypso.tk.core.JDate;
import com.calypso.tk.event.PSEvent;

public class PSEventSantInitialMarginExport extends PSEvent {


    /**
     * serialVersion id
     */
    private static final long serialVersionUID = 1L;

    /**
     * margin call contract
     */
    protected int contractid = 0;
    protected JDate processDate = null;
    protected int facadeEntryId=0;

    public PSEventSantInitialMarginExport(int id, JDate processDate,int facadeEntryId) {
        super();
        this.contractid = id;
        this.processDate = processDate;
        this.facadeEntryId=facadeEntryId;

    }

    public PSEventSantInitialMarginExport() {
        super();
    }

    public JDate getProcessDate() {
        return processDate;
    }

    public void setProcessDate(JDate processDate) {
        this.processDate = processDate;
    }

    public int getContractid() {
        return contractid;
    }

    public void setContractid(int contractid) {
        this.contractid = contractid;
    }

    public int getFacadeEntryId(){
        return this.facadeEntryId;
    }

}
