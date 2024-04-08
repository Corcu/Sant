package calypsox.tk.report;

public class SantCollateralPositionSendBalanceItem {
	/**
	 * Maximum Decimal places for the control line.
	 */
	public static final int CONTROL_MAX_DECIMAL_LENGTH = 0;
	/**
	 * Length for the control line.
	 */
	public static final int CONTROL_LENGTH = 8;

	/**
	 * Identify the object ReposTradeItem.
	 */
	public static final String SEND_BALANCE_ITEM = "SantCollateralPositionSendBalanceItem";

	// Customized columns.
	private String contractID;
	private String contractType;
	private String name;
	private String positionType;
	private String isin;
	private String nominal;
	private String dirtyPrice;
	private String currency;
	private String haircut;
	private String value;
	private String fxRate;
	private String baseCCY;
	private String valueInBaseCCY;
	private String maturity;
	private String nextCouponDate;

	public String getContractID() {
		return this.contractID;
	}

	public void setContractID(String contractID) {
		this.contractID = contractID;
	}

	public String getContractType() {
		return this.contractType;
	}

	public void setContractType(String contractType) {
		this.contractType = contractType;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPositionType() {
		return this.positionType;
	}

	public void setPositionType(String positionType) {
		this.positionType = positionType;
	}

	public String getIsin() {
		return this.isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public String getNominal() {
		return this.nominal;
	}

	public void setNominal(String nominal) {
		this.nominal = nominal;
	}

	public String getDirtyPrice() {
		return this.dirtyPrice;
	}

	public void setDirtyPrice(String dirtyPrice) {
		this.dirtyPrice = dirtyPrice;
	}

	public String getCurrency() {
		return this.currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getHaircut() {
		return this.haircut;
	}

	public void setHaircut(String haircut) {
		this.haircut = haircut;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getFxRate() {
		return this.fxRate;
	}

	public void setFxRate(String fxRate) {
		this.fxRate = fxRate;
	}

	public String getBaseCCY() {
		return this.baseCCY;
	}

	public void setBaseCCY(String baseCCY) {
		this.baseCCY = baseCCY;
	}

	public String getValueInBaseCCY() {
		return this.valueInBaseCCY;
	}

	public void setValueInBaseCCY(String valueInBaseCCY) {
		this.valueInBaseCCY = valueInBaseCCY;
	}

	public String getMaturity() {
		return this.maturity;
	}

	public void setMaturity(String maturity) {
		this.maturity = maturity;
	}

	public String getNextCouponDate() {
		return this.nextCouponDate;
	}

	public void setNextCouponDate(String nextCouponDate) {
		this.nextCouponDate = nextCouponDate;
	}

}
