package calypsox.tk.report;

import calypsox.tk.report.generic.loader.interestnotif.SantInterestNotificationEntry;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;

public class AccountingNotificationEntry extends SantInterestNotificationEntry {

    JDate proccesDate;
    Book book;
    String murexID = "";
    String interstBearingDirection = "";
    String accountSector;
    String product;
    String accountInterestConfigName;
    String accountBalance;
    Double interetanual = 0.0;
    String calc;
    Double currentLiveBalance = 0.0;
    Double unliquidatedAccumulatedPeriodic = 0.0;
    Double cumulativeAnnualInterest = 0.0;
    Double annualPositiveInterest = 0.0;
    Double annualNegativeInterest = 0.0;
    JDate lastInterestDate;
    Trade trade;
    Double adjustement = 0.0;
    String key = "";
    String couponType = "";
    String guaranteeType = "";


    public JDate getProccesDate() {
        return proccesDate;
    }

    public void setProccesDate(JDate proccesDate) {
        this.proccesDate = proccesDate;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public Double getCurrentLiveBalance() {
        return currentLiveBalance;
    }

    public void setCurrentLiveBalance(Double currentLiveBalance) {
        this.currentLiveBalance = currentLiveBalance;
    }

    public Double getUnliquidatedAccumulatedPeriodic() {
        return unliquidatedAccumulatedPeriodic;
    }

    public void setUnliquidatedAccumulatedPeriodic(Double unliquidatedAccumulatedPeriodic) {
        this.unliquidatedAccumulatedPeriodic = unliquidatedAccumulatedPeriodic;
    }

    public Double getCumulativeAnnualInterest() {
        return cumulativeAnnualInterest;
    }

    public void setCumulativeAnnualInterest(Double cumulativeAnnualInterest) {
        this.cumulativeAnnualInterest = cumulativeAnnualInterest;
    }

    public Double getAnnualPositiveInterest() {
        return annualPositiveInterest;
    }

    public void setAnnualPositiveInterest(Double annualPositiveInterest) {
        this.annualPositiveInterest = annualPositiveInterest;
    }

    public Double getAnnualNegativeInterest() {
        return annualNegativeInterest;
    }

    public void setAnnualNegativeInterest(Double annualNegativeInterest) {
        this.annualNegativeInterest = annualNegativeInterest;
    }

    public Double getInteretanual() {
        return interetanual;
    }

    public void setInteretanual(Double interetanual) {
        this.interetanual = interetanual;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public String getMurexID() {
        return murexID;
    }

    public void setMurexID(String murexID) {
        this.murexID = murexID;
    }

    public String getInterstBearingDirection() {
        return interstBearingDirection;
    }

    public void setInterstBearingDirection(String interstBearingDirection) {
        this.interstBearingDirection = interstBearingDirection;
    }

    public String getAccountSector() {
        return accountSector;
    }

    public void setAccountSector(String accountSector) {
        this.accountSector = accountSector;
    }

    public String getAccountInterestConfigName() {
        return accountInterestConfigName;
    }

    public void setAccountInterestConfigName(String accountInterestConfigName) {
        this.accountInterestConfigName = accountInterestConfigName;
    }

    public String getCalc() {
        return calc;
    }

    public void setCalc(String calc) {
        this.calc = calc;
    }

    public String getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(String accountBalance) {
        this.accountBalance = accountBalance;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public JDate getLastInterestDate() {
        return lastInterestDate;
    }

    public void setLastInterestDate(JDate lastInterestDate) {
        this.lastInterestDate = lastInterestDate;
    }

    public Double getAdjustement() {
        return adjustement;
    }

    public void setAdjustement(Double adjustement) {
        this.adjustement = adjustement;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCouponType() {
        return couponType;
    }

    public void setCouponType(String couponType) {
        this.couponType = couponType;
    }

    public String getGuaranteeType() {
        return guaranteeType;
    }

    public void setGuaranteeType(String guaranteeType) {
        this.guaranteeType = guaranteeType;
    }
}
