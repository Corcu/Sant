package calypsox.tk.event;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.event.PSEvent;

public class PSEventSendOptimizerMarginCallStatus extends PSEvent {


    /**
     *
     */
    private static final long serialVersionUID = -1409514393757717961L;

    private static final String SEND_OPTIM_MARGIN_CALL_STATUS = "SEND_OPTIM_MARGIN_CALL_STATUS";

    public PSEventSendOptimizerMarginCallStatus() {
        super();
    }

    public PSEventSendOptimizerMarginCallStatus(String fileName,
                                                JDatetime timeStamp, int nbRecords) {
        super();
        this.fileName = fileName;
        this.timeStamp = timeStamp;
        this.nbRecords = nbRecords;
        if (timeStamp != null) {
            setLongId(timeStamp.getTime());
        }
    }

    private String fileName = null;
    private JDatetime timeStamp = null;
    private int nbRecords = 0;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public JDatetime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(JDatetime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getNbRecords() {
        return nbRecords;
    }

    public void setNbRecords(int nbRecords) {
        this.nbRecords = nbRecords;
    }

    @Override
    public String toString() {
        return "PSEventSendOptimizerMarginCallStatus [fileName=" + fileName
                + ", timeStamp=" + timeStamp + ", nbRecords=" + nbRecords + "]";
    }

    @Override
    public String getEventType() {
        return SEND_OPTIM_MARGIN_CALL_STATUS;
    }
}
