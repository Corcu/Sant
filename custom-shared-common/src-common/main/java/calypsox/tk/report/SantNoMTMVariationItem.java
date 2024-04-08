package calypsox.tk.report;

import com.calypso.tk.core.Trade;

import java.io.Serializable;

public class SantNoMTMVariationItem implements Serializable {

	// START OA 28/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = 8797221650541386881L;
	// END OA OA 28/11/2013

	public static final String SANT_MTM_VARIATION_ITEM = "SantMTMVariationItem";

	public SantNoMTMVariationItem() {

	}

	// private String contractId;
	private String mtmValDate;
	private String markCcy;
	private double markValue;
	private double prevMarkValue;
	private Trade trade;
	private String markName;
	private String marginCallname;

	// public String getContractId() {
	// return contractId;
	// }
	// public void setContractId(String contractId) {
	// this.contractId = contractId;
	// }

	public String getMtmValDate() {
		return this.mtmValDate;
	}

	public void setMtmValDate(String mtmValDate) {
		this.mtmValDate = mtmValDate;
	}

	public String getMarkCcy() {
		return this.markCcy;
	}

	public void setMarkCcy(String markCcy) {
		this.markCcy = markCcy;
	}

	public Trade getTrade() {
		return this.trade;
	}

	public void setTrade(Trade trade) {
		this.trade = trade;
	}

	public String getMarkName() {
		return this.markName;
	}

	public void setMarkName(String markName) {
		this.markName = markName;
	}

	public String getMarginCallname() {
		return this.marginCallname;
	}

	public void setMarginCallname(String marginCallname) {
		this.marginCallname = marginCallname;
	}

	public double getMarkValue() {
		return this.markValue;
	}

	public void setMarkValue(double markValue) {
		this.markValue = markValue;
	}

	public double getPrevMarkValue() {
		return this.prevMarkValue;
	}

	public void setPrevMarkValue(double prevMarkValue) {
		this.prevMarkValue = prevMarkValue;
	}

}
