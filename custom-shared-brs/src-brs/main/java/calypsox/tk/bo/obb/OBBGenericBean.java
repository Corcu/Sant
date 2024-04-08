package calypsox.tk.bo.obb;

import calypsox.util.OBBReportUtil;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.refdata.Account;

import java.util.Optional;

/**
 * @author acd
 */
public abstract class OBBGenericBean implements OBBGenericMapInterface {

    private String branch = "";
    private String postingID = "";
    private String appIndetifier = "";
    private JDate accountingDate;
    private String description = "";
    private String entity = "";
    private String center = "";
    private String product = "";
    private String contract = "";
    private String subProduct = "";
    private String subAccount = "";
    private String currency = "";
    private String ccyCounterValue = "";
    private Double amount = 0D;
    private Double amuntCounterValue = 0D;
    private String operatingPosition = "";
    private String sign = "";
    private String type = "";
    private String originContract = "";
    private String topic = "";
    private String ccySource = "";
    private Double amountSource = 0D;
    private String accIdentifier = "";

    private Trade trade;
    private BOPosting boPosting;
    private String creditDebit;
    private JDate processDate;
    private BOPosting boPostingConvert;
    private Account account;
    private Boolean doNotSetAgrego;


    public void setDoNotSetAgrego(Boolean doNotSetAgrego) {
         this.doNotSetAgrego = doNotSetAgrego;
    }

    public boolean getDoNotSetAgrego() {
        return this.doNotSetAgrego;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getAppIndetifier() {
        return appIndetifier;
    }

    public void setAppIndetifier(String appIndetifier) {
        this.appIndetifier = appIndetifier;
    }

    public String getPostingID() {
        return postingID;
    }

    public void setPostingID(String postingID) {
        this.postingID = postingID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getCenter() {
        return center;
    }

    public void setCenter(String center) {
        this.center = center;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getSubProduct() {
        return subProduct;
    }

    public void setSubProduct(String subProduct) {
        this.subProduct = subProduct;
    }

    public String getSubAccount() {
        return subAccount;
    }

    public void setSubAccount(String subAccount) {
        this.subAccount = subAccount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCcyCounterValue() {
        return ccyCounterValue;
    }

    public void setCcyCounterValue(String ccyCounterValue) {
        this.ccyCounterValue = ccyCounterValue;
    }

    public String getOperatingPosition() {
        return operatingPosition;
    }

    public void setOperatingPosition(String operatingPosition) {
        this.operatingPosition = operatingPosition;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOriginContract() {
        return originContract;
    }

    public void setOriginContract(String originContract) {
        this.originContract = originContract;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getCcySource() {
        return ccySource;
    }

    public void setCcySource(String ccySource) {
        this.ccySource = ccySource;
    }

    public JDate getAccountingDate() {
        return accountingDate;
    }

    public void setAccountingDate(JDate accountingDate) {
        this.accountingDate = accountingDate;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getAmuntCounterValue() {
        return amuntCounterValue;
    }

    public void setAmuntCounterValue(Double amuntCounterValue) {
        this.amuntCounterValue = amuntCounterValue;
    }

    public Double getAmountSource() {
        return amountSource;
    }

    public void setAmountSource(Double amountSource) {
        this.amountSource = amountSource;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public BOPosting getBoPosting() {
        return boPosting;
    }

    public void setBoPosting(BOPosting boPosting) {
        this.boPosting = boPosting;
    }

    public String getCreditDebit() {
        return creditDebit;
    }

    public void setCreditDebit(String creditDebit) {
        this.creditDebit = creditDebit;
    }

    public JDate getProcessDate() {
        return processDate;
    }

    public void setProcessDate(JDate processDate) {
        this.processDate = processDate;
    }

    public BOPosting getBoPostingConvert() {
        return boPostingConvert;
    }

    public void setBoPostingConvert(BOPosting boPostingConvert) {
        this.boPostingConvert = boPostingConvert;
    }

    public String getAccIdentifier() {
        return accIdentifier;
    }

    public void setAccIdentifier(String accIdentifier) {
        this.accIdentifier = accIdentifier;
    }

    public void buildData(){
        this.setBranch(loadBranch());
        this.setAppIndetifier(loadAppIdentifier());
        this.setAccountingDate(loadAccDate());
        this.setPostingID(loadPostingID());
        this.setDescription(loadDescription());
        this.setEntity(loadEntity());
        this.setCenter(loadCenter());
        this.setProduct(loadProduct());
        this.setContract(loadContract());
        this.setSubProduct(loadSubProduct());
        this.setSubAccount(loadSubAccount());
        this.setCurrency(loadCurrency());
        this.setCcyCounterValue(loadCcyCounterValue());
        this.setAmount(loadAmount());
        this.setAmuntCounterValue(loadAmountCounterValue());
        this.setOperatingPosition(loadOperatingPosition());
        this.setSign(loadSign());
        this.setType(loadType());
        this.setTopic(loadTopic());
        this.setOriginContract(loadOriginContract());
        this.setAmountSource(loadAmountSource());
        this.setCcySource(loadCcySource());
        this.setAccIdentifier(loadAccIdentifier());
    }

    @Override
    public String loadBranch() {
        return "CALYPSO";
    }

    @Override
    public String loadAppIdentifier() {
        return "BR";
    }

    @Override
    public String loadPostingID() {
        return String.valueOf(getBoPosting().getId());
    }

    @Override
    public JDate loadAccDate() {
        return getProcessDate();
    }

    @Override
    public String loadDescription() {
        return getBoPosting().getDescription();
    }

    @Override
    public Double loadAmount() {
        return isFxTranslationOnly(getBoPosting()) ? 0D : getBoPosting().getAmount();
    }

    @Override
    public Double loadAmountSource() {
        return loadAmount();
    }

    @Override
    public String loadCurrency() {
        return isFxTranslationOnly(getBoPosting()) ? getOriginalCcy(getBoPosting()) : getBoPosting().getCurrency();
    }

    @Override
    public String loadCcySource() {
        if(null!=getTrade() && null!=getTrade().getProduct()){
            return ((PerformanceSwap)getTrade().getProduct()).getPrimaryLegUnderlyingCurrency();
        }
        return "";
    }

    @Override
    public String loadSubAccount() {
        final String entity = getEntity();
        final String center = getCenter();
        String subAccount = entity+center+"PGC";
        final Optional<Account> account = Optional.ofNullable(getAccount());
        if(account.isPresent()){
            if("SubCuenta".equalsIgnoreCase(OBBReportUtil.getAccountTypeValue(account.get()))){
                final String accountPosicionValue = OBBReportUtil.getAccountPosicionValue(account.get());
                if(Optional.ofNullable(getTrade()).map(trade -> {
                    return 0L!=trade.getMirrorTradeId();
                }).get()){
                    return subAccount+"9999999";
                }
                if(accountPosicionValue.contains(",")){
                    return subAccount+"0"+accountPosicionValue.replace(",","");
                }
                return subAccount+"0"+accountPosicionValue;
            }
        }
        return "";
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        final OBBGenericFormatter intance = OBBGenericFormatter.getInstance();
        builder.append(intance.formatLeftString(branch,10));
        builder.append(intance.formatLeftString(appIndetifier,2));
        builder.append(intance.formatDate(accountingDate,"yyyy/MM/dd"));
        builder.append(intance.formatLeftZeroNumber(postingID,40));
        builder.append(intance.formatLeftString(description,80));
        builder.append(intance.formatLeftZeroNumber(entity,4));
        builder.append(intance.formatLeftZeroNumber(center,4));
        builder.append(intance.formatLeftZeroNumber(product,3));
        builder.append(intance.formatLeftZeroNumber(contract,7));
        builder.append(intance.formatLeftString(subProduct,3));
        builder.append(intance.formatLeftString(subAccount,18));
        builder.append(intance.formatLeftString(currency,3));
        builder.append(intance.formatLeftString(ccyCounterValue,3));
        builder.append(intance.formatDecimal(amount));
        builder.append(intance.formatDecimal(amuntCounterValue));
        builder.append(intance.formatLeftString(operatingPosition,3));
        builder.append(intance.formatLeftString(sign,1));
        builder.append(intance.formatLeftZeroNumber(type,1));
        builder.append(intance.formatLeftString(originContract,7));
        builder.append(intance.formatLeftString(topic,80));
        builder.append(intance.formatDecimal(amountSource));
        builder.append(intance.formatLeftString(ccySource,3));
        builder.append(intance.formatLeftString(accIdentifier,1));

        return builder.toString();
    }

    private boolean isFxTranslationOnly(BOPosting posting){
        return Optional.ofNullable(posting).isPresent() && Util.toBoolean(posting.getAttributeValue("FxTranslationOnly"));
    }

    private String getOriginalCcy(BOPosting posting){
        return Optional.ofNullable(posting).isPresent() && !Util.isEmpty(posting.getAttributeValue("ORIGINAL_CCY"))
                ? posting.getAttributeValue("ORIGINAL_CCY") : getOriginalCCYFromAccount();
    }

    private String getOriginalCCYFromAccount(){
         return Optional.ofNullable(getAccount()).map(Account::getCurrency).orElse("");
    }
}
