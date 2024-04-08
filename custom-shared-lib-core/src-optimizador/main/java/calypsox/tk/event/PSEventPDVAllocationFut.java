package calypsox.tk.event;

import com.calypso.tk.core.JDate;
import com.calypso.tk.event.PSEvent;

public class PSEventPDVAllocationFut extends PSEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4433065813573479550L;

	public static final String PDV_ALLOCATION_FUT = "PDV_ALLOCATION_FUTURE";

	// Allocation message to process
	private String allocMessage = null;

	// Allocation task id
	private long taskId = 0;

	// Allocation action
	private String action = null;

	// Allocation valDate
	private JDate valDate = null;

	// Allocation collatId
	private Long collatId = null;

	public PSEventPDVAllocationFut(long taskId, String allocMessage,
			String action, JDate valDate, Long collatId) {
		super();
		this.allocMessage = allocMessage;
		this.taskId = taskId;
		this.action = action;
		this.valDate = valDate;
		this.collatId = collatId;
	}

	public JDate getValDate() {
		return valDate;
	}

	public void setValDate(JDate valDate) {
		this.valDate = valDate;
	}

	public Long getCollatId() {
		return collatId;
	}

	public void setCollatId(Long collatId) {
		this.collatId = collatId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	public String getAllocMessage() {
		return allocMessage;
	}

	public void setAllocMessage(String allocMsg) {
		this.allocMessage = allocMsg;
	}

	@Override
	public String getEventType() {
		return PDV_ALLOCATION_FUT;
	}

	@Override
	public String toString() {
		return "Event to be consumed by SANT_ImportMessageEngine_PDVCollatEngine to handle Allocation message from Task Station";
	}
}
