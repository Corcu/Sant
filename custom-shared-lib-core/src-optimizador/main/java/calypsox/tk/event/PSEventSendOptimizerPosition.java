package calypsox.tk.event;

import com.calypso.tk.event.PSEvent;

public class PSEventSendOptimizerPosition extends PSEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5579863438349049206L;

	private static final String SEND_OPTIM_POSITION = "SEND_OPTIM_POSITION";

	public int productId = 0;

	public int bookId = 0;

	public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public int getBookId() {
		return bookId;
	}

	public void setBookId(int bookId) {
		this.bookId = bookId;
	}

	public PSEventSendOptimizerPosition(int productId, int bookId) {
		super();
		this.productId = productId;
		this.bookId = bookId;
	}

	@Override
	public String getEventType() {
		return SEND_OPTIM_POSITION;
	}

	@Override
	public String toString() {
		return "Event to be consumed by SANT_ImportMessageEngine_OptimizerPosition to inform that position has be sent to Optimizer";
	}
}
