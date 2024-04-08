package calypsox.tk.util.bean;

public class CpInternalRatingsBean {
	
	// variables
	private String legalEntity;
	private String agency;
	private String ratingType;
	private String value;
	private String fromDate;
	private int nElem;

	// constructor
	public CpInternalRatingsBean ( String[] values){

		this.setLegalEntity(values[0]);
		this.setAgency(values[1]);
		this.setRatingType(values[2]);
		this.setNElem(3);
		if(values.length > 3){
			this.setValue(values[3]);
			this.setFromDate(values[4]);
			this.setNElem(5);
		}		
	}

	// getters and setters
	public String getLegalEntity() {
		return legalEntity;
	}

	public void setLegalEntity(String legalEntity) {
		this.legalEntity = legalEntity;
	}

	public String getAgency() {
		return agency;
	}

	public void setAgency(String agency) {
		this.agency = agency;
	}

	public String getRatingType() {
		return ratingType;
	}

	public void setRatingType(String ratingType) {
		this.ratingType = ratingType;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}
	
	public int getNElem() {
		return nElem;
	}

	public void setNElem(int nElem) {
		this.nElem = nElem;
	}
	
}
