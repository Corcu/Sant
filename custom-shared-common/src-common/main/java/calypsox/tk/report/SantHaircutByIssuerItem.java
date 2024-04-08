package calypsox.tk.report;

import java.io.Serializable;

public class SantHaircutByIssuerItem implements Serializable {

	private static final long serialVersionUID = 123L;

	public static final String SANT_HAIRCUT_BY_ISSUER_ITEM = "SantHaircutByIssuerItem";

	public SantHaircutByIssuerItem() {

	}

	private int agreementId;
	private String issuerName;
	private int tenor;
	private double value;

	public int getAgreement() {
		return this.agreementId;
	}

	public void setAgreement(int agreementId) {
		this.agreementId = agreementId;
	}

	public String getIssuer() {
		return this.issuerName;
	}

	public void setIssuer(String issuerName) {
		this.issuerName = issuerName;
	}

	public int getTenor() {
		return this.tenor;
	}

	public void setTenor(int tenor) {
		this.tenor = tenor;
	}

	public double getValue() {
		return this.value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public static String getSantHaircutByIssuerItem() {
		return SANT_HAIRCUT_BY_ISSUER_ITEM;
	}

}
