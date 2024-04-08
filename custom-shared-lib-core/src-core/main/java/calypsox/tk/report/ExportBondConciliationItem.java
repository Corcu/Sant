package calypsox.tk.report;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Rate;

/**
 * Class with the item definition for the Bonds Conciliation report.
 * 
 * @author Juan Angel Torija Díaz
 */
public class ExportBondConciliationItem {

	/**
	 * Identify the object ExpTLM_BondPricesItem.
	 */
	public static final String EXPORT_BONDCONCILIATION_ITEM = "ExportBondConciliationItem";

	// Customized columns.

	public ExportBondConciliationItem() {
	}

	private Amount spread;
	private String rateIndexFactor;
	private String paymentRule;
	private String redemptionCurrency;
	private String resetHolidays;
	private String RedemptionPrice;
	private String minPurchaseAmount;
	private String settleDays;
	private String cuponCurrency;
	private String resetDays;
	private String resetBusLag;
	private String resetInArrear;
	private String paymentLag;
	private String frecuency;
	private String recordDays;
	private String flipper;
	private String flipperDate;
	private String flipperFrequency;
	private String dayCount;
	private String poolFactorEfectiveDate;
	private String poolFactorKnowDate;
	private Rate rate;
	private Amount totalIssued;

	// Getters & Setters

	public String getPoolFactorEfectiveDate() {
		return this.poolFactorEfectiveDate;
	}

	public void setPoolFactorEfectiveDate(String poolFactorEfectiveDate) {
		this.poolFactorEfectiveDate = poolFactorEfectiveDate;
	}

	public String getPoolFactorKnowDate() {
		return this.poolFactorKnowDate;
	}

	public void setPoolFactorKnowDate(String poolFactorKnowDate) {
		this.poolFactorKnowDate = poolFactorKnowDate;
	}

	public String getDayCount() {
		return this.dayCount;
	}

	public void setDayCount(String dayCount) {
		this.dayCount = dayCount;
	}

	public String getRateIndexFactor() {
		return this.rateIndexFactor;
	}

	public void setRateIndexFactor(String rateIndexFactor) {
		this.rateIndexFactor = rateIndexFactor;
	}

	public String getPaymentRule() {
		return this.paymentRule;
	}

	public void setPaymentRule(String paymentRule) {
		this.paymentRule = paymentRule;
	}

	public String getRedemptionCurrency() {
		return this.redemptionCurrency;
	}

	public void setRedemptionCurrency(String redemptionCurrency) {
		this.redemptionCurrency = redemptionCurrency;
	}

	public String getResetHolidays() {
		return this.resetHolidays;
	}

	public void setResetHolidays(String resetHolidays) {
		this.resetHolidays = resetHolidays;
	}

	public String getRedemptionPrice() {
		return this.RedemptionPrice;
	}

	public void setRedemptionPrice(String redemptionPrice) {
		this.RedemptionPrice = redemptionPrice;
	}

	public String getMinPurchaseAmount() {
		return this.minPurchaseAmount;
	}

	public void setMinPurchaseAmount(String minPurchaseAmount) {
		this.minPurchaseAmount = minPurchaseAmount;
	}

	public String getSettleDays() {
		return this.settleDays;
	}

	public void setSettleDays(String settleDays) {
		this.settleDays = settleDays;
	}

	public String getCuponCurrency() {
		return this.cuponCurrency;
	}

	public void setCuponCurrency(String cuponCurrency) {
		this.cuponCurrency = cuponCurrency;
	}

	public String getResetDays() {
		return this.resetDays;
	}

	public void setResetDays(String resetDays) {
		this.resetDays = resetDays;
	}

	public String getResetBusLag() {
		return this.resetBusLag;
	}

	public void setResetBusLag(String resetBusLag) {
		this.resetBusLag = resetBusLag;
	}

	public String getResetInArrear() {
		return this.resetInArrear;
	}

	public void setResetInArrear(String resetInArrear) {
		this.resetInArrear = resetInArrear;
	}

	public String getPaymentLag() {
		return this.paymentLag;
	}

	public void setPaymentLag(String paymentLag) {
		this.paymentLag = paymentLag;
	}

	public String getFrecuency() {
		return this.frecuency;
	}

	public void setFrecuency(String frecuency) {
		this.frecuency = frecuency;
	}

	public String getRecordDays() {
		return this.recordDays;
	}

	public void setRecordDays(String recordDays) {
		this.recordDays = recordDays;
	}

	public String getFlipper() {
		return this.flipper;
	}

	public void setFlipper(String flipper) {
		this.flipper = flipper;
	}

	public String getFlipperDate() {
		return this.flipperDate;
	}

	public void setFlipperDate(String flipperDate) {
		this.flipperDate = flipperDate;
	}

	public String getFlipperFrequency() {
		return this.flipperFrequency;
	}

	public void setFlipperFrequency(String flipperFrequency) {
		this.flipperFrequency = flipperFrequency;
	}

	public Amount getSpread() {
		return this.spread;
	}

	public void setSpread(Amount spread) {
		this.spread = spread;
	}
	
	public Rate getRate(){
		return this.rate;
	}
	public void setRate(Rate rate){
		this.rate = rate;
	}

	public Amount getTotalIssued() {
		return totalIssued;
	}

	public void setTotalIssued(Amount totalIssued) {
		this.totalIssued = totalIssued;
	}
	

}
