package calypsox.tk.report;

import java.io.Serializable;

import com.calypso.tk.core.Product;

public class Opt_AcceptableCollateralMMOOItem implements Serializable {

	private static final long serialVersionUID = 123L;

	public static final String OPT_ACCEPTABLE_COLLAT_MMOO_ITEM = "Opt_AcceptableCollateralMMOOItem";

	private final Product product;
	private final String cptyName;
	private final double haircutValue;

	public Opt_AcceptableCollateralMMOOItem(Product product, String cptyName, double haircutValue) {

		this.product = product;
		this.cptyName = cptyName;
		this.haircutValue = haircutValue;

	}

	public static String getOptAcceptableCollatMmooItem() {
		return OPT_ACCEPTABLE_COLLAT_MMOO_ITEM;
	}

	public Product getProduct() {
		return this.product;
	}

	public String getCptyName() {
		return this.cptyName;
	}

	public double getHaircutValue() {
		return this.haircutValue;
	}

}
