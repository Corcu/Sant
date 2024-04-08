package calypsox.tk.util.bean;

public class CpExternalRatingsBean {

	// variables
	private String legalEntity;
	private String agency;
	private String valueOR;
	private String valueSU;
	private String valueST;
	private String valueLT;
	private String fromDateOR;
	private String fromDateSU;
	private String fromDateST;
	private String fromDateLT;

	// constructor
	public CpExternalRatingsBean(String[] values) {

		setLegalEntity(values[0]);
		setAgency(values[1]);
		setValueOR(values[2]);
		setValueSU(values[3]);
		setValueST(values[4]);
		setValueLT(values[5]);
		setFromDateOR(values[6]);
		setFromDateSU(values[7]);
		setFromDateST(values[8]);
		setFromDateLT(values[9]);

	}

	public String getValueLT() {
		return this.valueLT;
	}

	public void setValueLT(String valueLT) {
		this.valueLT = valueLT;
	}

	public String getFromDateLT() {
		return this.fromDateLT;
	}

	public void setFromDateLT(String fromDateLT) {
		this.fromDateLT = fromDateLT;
	}

	public String getLegalEntity() {
		return this.legalEntity;
	}

	public void setLegalEntity(String legalEntity) {
		this.legalEntity = legalEntity;
	}

	public String getAgency() {
		return this.agency;
	}

	public void setAgency(String agency) {
		this.agency = agency;
	}

	public String getValueOR() {
		return this.valueOR;
	}

	public void setValueOR(String valueOR) {
		this.valueOR = valueOR;
	}

	public String getValueSU() {
		return this.valueSU;
	}

	public void setValueSU(String valueSU) {
		this.valueSU = valueSU;
	}

	public String getValueST() {
		return this.valueST;
	}

	public void setValueST(String valueST) {
		this.valueST = valueST;
	}

	public String getFromDateOR() {
		return this.fromDateOR;
	}

	public void setFromDateOR(String fromDateOR) {
		this.fromDateOR = fromDateOR;
	}

	public String getFromDateSU() {
		return this.fromDateSU;
	}

	public void setFromDateSU(String fromDateSU) {
		this.fromDateSU = fromDateSU;
	}

	public String getFromDateST() {
		return this.fromDateST;
	}

	public void setFromDateST(String fromDateST) {
		this.fromDateST = fromDateST;
	}

}
