package calypsox.tk.util.bean;

public class BondExtRatingsBean {
	
	// variables
	private String isin;
	private String agency;
	private String value;
	/*private String valueFLTRO;
	private String valueFSTR;
	private String valueLLTR;
	private String valueLLTRO;
	private String valueLSTR;*/
	private String seniority;
	private String fromDate;
	/*private String fromDateFLTRO;
	private String fromDateFSTR;
	private String fromDateLLTR;
	private String fromDateLLTRO;
	private String fromDateLSTR;*/
	
	// constructor
	public BondExtRatingsBean ( String[] values){
		
		this.setIsin(values[0]);
		this.setAgency(values[1]);
		this.setValue(values[3]);
		/*this.setValueFLTRO(values[3]);
		this.setValueFSTR(values[4]);
		this.setValueLLTR(values[5]);
		this.setValueLLTRO(values[6]);
		this.setValueLSTR(values[7]);
		*/
		this.setSeniority("CURRENT");
		this.setFromDate(values[4]);
		/*this.setFromDateFLTRO(values[9]);
		this.setFromDateFSTR(values[10]);
		this.setFromDateLLTR(values[11]);
		this.setFromDateLLTRO(values[12]);
		this.setFromDateLSTR(values[13]);*/
		
	}

	public String getIsin() {
		return isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public String getAgency() {
		return agency;
	}

	public void setAgency(String agency) {
		this.agency = agency;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getSeniority() {
		return seniority;
	}

	public void setSeniority(String seniority) {
		this.seniority = seniority;
	}

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}

}
