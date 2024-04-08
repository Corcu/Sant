package calypsox.tk.event;

import com.calypso.tk.event.PSEvent;

public class PSEventSendOptimizerMarginCall extends PSEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5879868207200440690L;
	
	private static final String SEND_OPTIM_MARGIN_CALL = "SEND_OPTIM_MARGIN_CALL";

	public PSEventSendOptimizerMarginCall() {
		super();
	}

	@Override
	public String toString() {
		return "Event to be consumed by SANT_ImportMessageEngine_OptimizerMarginCall to inform that MCE have to be sent to Optimizer";
	}

	@Override
	public String getEventType() {
		return SEND_OPTIM_MARGIN_CALL;
	}
}
