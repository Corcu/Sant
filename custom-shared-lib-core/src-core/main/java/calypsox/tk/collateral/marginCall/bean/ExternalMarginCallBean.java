package calypsox.tk.collateral.marginCall.bean;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;

public class ExternalMarginCallBean implements ExternalMarginCall {

	private Map<String, String> attributes;
	private CollateralConfig collateralConfig;
	private String pdvMessageContent;
	private MarginCall marginCall;

	private String action;
	private String collatId;
	private String counterparty;
	private String instrument;
	private String portfolio;
	private Date valueDate;
	private Date tradeDate;
	private String collateralDirection;
	private double amount;
	private String amountCcy;
	private String underlyingType;
	private String underlying;
	private String closingPrice;
	//SLB
	private boolean isSLB=false;
	private String SLB_BUNDLE;//or SLB_MUREX
	/**
	 * @return the attributes
	 */
	public Map<String, String> getAttributes() {
		if (attributes == null) {
			attributes = new HashMap<String, String>();
		}
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public void addAttribute(String name, String value) {
		getAttributes().put(name, value);
	}

	public CollateralConfig getCollateralConfig() {
		return collateralConfig;
	}

	public void setCollateralConfig(CollateralConfig collateralConfig) {
		this.collateralConfig = collateralConfig;
	}

	public String getPdvMessageContent() {
		return pdvMessageContent;
	}

	public void setPdvMessageContent(String pdvMessageContent) {
		this.pdvMessageContent = pdvMessageContent;
	}

	public MarginCall getMarginCall() {
		return marginCall;
	}

	public void setMarginCall(MarginCall marginCall) {
		this.marginCall = marginCall;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getCollatId() {
		return collatId;
	}

	public void setCollatId(String collatId) {
		this.collatId = collatId;
	}

	public String getCounterparty() {
		return counterparty;
	}

	public void setCounterparty(String counterparty) {
		this.counterparty = counterparty;
	}

	public String getInstrument() {
		return instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	public String getPortfolio() {
		return portfolio;
	}

	public void setPortfolio(String portfolio) {
		this.portfolio = portfolio;
	}

	public Date getValueDate() {
		return valueDate;
	}

	public void setValueDate(Date valueDate) {
		this.valueDate = valueDate;
	}

	public Date getTradeDate() {
		return tradeDate;
	}

	public void setTradeDate(Date tradeDate) {
		this.tradeDate = tradeDate;
	}

	public String getCollateralDirection() {
		return collateralDirection;
	}

	public void setCollateralDirection(String collateralDirection) {
		this.collateralDirection = collateralDirection;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public String getAmountCcy() {
		return amountCcy;
	}

	public void setAmountCcy(String amountCcy) {
		this.amountCcy = amountCcy;
	}

	public String getUnderlyingType() {
		return underlyingType;
	}

	public void setUnderlyingType(String underlyingType) {
		this.underlyingType = underlyingType;
	}

	public String getUnderlying() {
		return underlying;
	}

	public void setUnderlying(String underlying) {
		this.underlying = underlying;
	}

	public String getClosingPrice() {
		return closingPrice;
	}

	public void setClosingPrice(String closingPrice) {
		this.closingPrice = closingPrice;
	}
	
	public void setSLB_BUNDLE(String slb_bundle) {
		this.SLB_BUNDLE=slb_bundle;
		
	}
	public String getSLB_BUNDLE() {
		return SLB_BUNDLE;
	}
	
	

	// private String contractName;
	// private double assetAmount;
	// private Date settlementDate;
	// private String collateralBook;
	// private String collateralType;
	// private Double contractValue;

}
