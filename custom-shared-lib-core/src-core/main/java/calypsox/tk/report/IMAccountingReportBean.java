package calypsox.tk.report;

import java.io.Serializable;

public class IMAccountingReportBean implements Serializable{
	
	/**
	 * 
	 */
	public static final String IM_ACCOUNTING_BEAN = "IMAccountingReportBean";
	
	private static final long serialVersionUID = 1L;
	private String contractid;
	private String valueDate;
	private String valueTradeDate;
	private String maturityDate;
	private String reference;
	private String branchoffice;
	private String po;
	private String direction;
	private String amount;
	private String imp_eur;
	private String currency;
	private String origne_amount;
	private String origne_currency;
	private String cpty;
	private String ume_sign;
	private String ume_product;
	private String ume_tipoper;
	private String product;
	private String type_seat;
	private String mis;
	private String operation;
	private String security;
	private String folder;
	private String proccesDate;
	
	public IMAccountingReportBean(){
		
	}
	
	
	public String getProduct(){
		return product;
	}
	public void setProduct(String product){
		this.product = product;
	}
	public String getProccesDate() {
		return proccesDate;
	}
	public void setProccesDate(String proccesDate) {
		this.proccesDate = proccesDate;
	}
	public String getValueDate() {
		return valueDate;
	}
	public void setValueDate(String valueDate) {
		this.valueDate = valueDate;
	}
	public String getValueTradeDate() {
		return valueTradeDate;
	}
	public void setValueTradeDate(String valueTradeDate) {
		this.valueTradeDate = valueTradeDate;
	}
	public String getMaturityDate() {
		return maturityDate;
	}
	public void setMaturityDate(String maturityDate) {
		this.maturityDate = maturityDate;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	public String getBranchoffice() {
		return branchoffice;
	}
	public void setBranchoffice(String branchoffice) {
		this.branchoffice = branchoffice;
	}
	public String getPo() {
		return po;
	}
	public void setPo(String po) {
		this.po = po;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public String getAmount() {
		return amount;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	public String getDivisa() {
		return imp_eur;
	}
	public void setDivisa(String imp_eur) {
		this.imp_eur = imp_eur;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getOrigne_amount() {
		return origne_amount;
	}
	public void setOrigne_amount(String origne_amount) {
		this.origne_amount = origne_amount;
	}
	public String getOrigne_currency() {
		return origne_currency;
	}
	public void setOrigne_currency(String origne_currency) {
		this.origne_currency = origne_currency;
	}
	public String getCpty() {
		return cpty;
	}
	public void setCpty(String cpty) {
		this.cpty = cpty;
	}
	public String getUme_sign() {
		return ume_sign;
	}
	public void setUme_sign(String ume_sign) {
		this.ume_sign = ume_sign;
	}
	public String getUme_product() {
		return ume_product;
	}
	public void setUme_product(String ume_product) {
		this.ume_product = ume_product;
	}
	public String getUme_tipoper() {
		return ume_tipoper;
	}
	public void setUme_tipoper(String ume_tipoper) {
		this.ume_tipoper = ume_tipoper;
	}
	public String getType_seat() {
		return type_seat;
	}
	public void setType_seat(String type_seat) {
		this.type_seat = type_seat;
	}
	public String getMis() {
		return mis;
	}
	public void setMis(String mis) {
		this.mis = mis;
	}
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public String getSecurity() {
		return security;
	}
	public void setSecurity(String security) {
		this.security = security;
	}
	public String getFolder() {
		return folder;
	}
	public void setFolder(String folder) {
		this.folder = folder;
	}
	
	public String getContractid() {
		return contractid;
	}
	public void setContractid(String contractid) {
		this.contractid = contractid;
	}

	public String getImp_eur() {
		return imp_eur;
	}
	public void setImp_eur(String imp_eur) {
		this.imp_eur = imp_eur;
	}
	
	
}
