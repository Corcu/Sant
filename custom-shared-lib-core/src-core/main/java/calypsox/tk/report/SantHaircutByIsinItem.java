package calypsox.tk.report;

import java.io.Serializable;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.refdata.CollateralConfig;

public class SantHaircutByIsinItem implements Serializable {

	private static final long serialVersionUID = 123L;

	public static final String SANT_HAIRCUT_BY_ISIN_ITEM = "SantHaircutByIsinItem";

	public SantHaircutByIsinItem(CollateralConfig agreement, LegalEntity issuer, Product security) {
		this.agreement = agreement;
		this.issuer = issuer;
		this.security = security;
	}

	private CollateralConfig agreement;
	private LegalEntity issuer;
	private Product security;

	public CollateralConfig getAgreement() {
		return this.agreement;
	}

	public void setAgreement(CollateralConfig agreement) {
		this.agreement = agreement;
	}

	public LegalEntity getIssuer() {
		return this.issuer;
	}

	public void setIssuer(LegalEntity issuer) {
		this.issuer = issuer;
	}

	public Product getSecurity() {
		return this.security;
	}

	public void setSecurity(Product security) {
		this.security = security;
	}

	public static String getSantHaircutByIsinItem() {
		return SANT_HAIRCUT_BY_ISIN_ITEM;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public static String getSantHaircutByIssuerItem() {
		return SANT_HAIRCUT_BY_ISIN_ITEM;
	}

}
