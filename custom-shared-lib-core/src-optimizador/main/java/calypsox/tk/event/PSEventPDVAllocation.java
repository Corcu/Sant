package calypsox.tk.event;

import com.calypso.tk.event.PSEvent;

public class PSEventPDVAllocation extends PSEvent {

    /**
     *
     */
    private static final long serialVersionUID = 8661316621767693603L;

    private static final String PDV_ALLOCATION = "PDV_ALLOCATION";

    private boolean isReprocessB = false;
    private boolean isMultiContractB = false;
    private int contractId = 0;
    private long tradeId = 0;
    private String message = null;

    private int collatId = 0;

    public int getCollatId() {
        return collatId;
    }

    public void setCollatId(int collatId) {
        this.collatId = collatId;
    }

    private long taskId = 0;

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public long getTradeId() {
        return tradeId;
    }

    public void setTradeId(long tradeId) {
        this.tradeId = tradeId;
    }

    public String getMessageAlloc() {
        return message;
    }

    public void setMessageAlloc(String messageAlloc) {
        this.message = messageAlloc;
    }

    public int getContractId() {
        return contractId;
    }

    public void setContractId(int contractId) {
        this.contractId = contractId;
    }

    public boolean isReprocessB() {
        return isReprocessB;
    }

    public void setReprocessB(boolean isReprocessB) {
        this.isReprocessB = isReprocessB;
    }

    public boolean isMultiContractB() {
        return isMultiContractB;
    }

    public void setMultiContractB(boolean isMultiContractB) {
        this.isMultiContractB = isMultiContractB;
    }

    public PSEventPDVAllocation(long taskId, int contractId, long tradeId,
                                String allocMessage, int collatId, boolean isReprocess,
                                boolean isMultiContractB) {
        super();
        this.isReprocessB = isReprocess;
        this.isMultiContractB = isMultiContractB;
        this.contractId = contractId;
        this.tradeId = tradeId;
        this.message = allocMessage;
        this.collatId = collatId;
        this.taskId = taskId;
    }

    @Override
    public String getEventType() {
        return PDV_ALLOCATION;
    }

    @Override
    public String toString() {
        return "Event to be consumed by SANT_ImportMessageEngine_PDVCollatEngine to handle Allocation message";
    }
}
