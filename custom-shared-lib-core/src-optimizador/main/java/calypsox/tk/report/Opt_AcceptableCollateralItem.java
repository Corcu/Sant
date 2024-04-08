package calypsox.tk.report;

import java.io.Serializable;

import com.calypso.tk.core.Product;

public class Opt_AcceptableCollateralItem implements Serializable {

	private static final long serialVersionUID = 123L;

	public static final String OPT_ACCEPTABLE_COLLAT_ITEM = "Opt_AcceptableCollateralItem";

	private final Product product;
	private final String filterName;

	public Opt_AcceptableCollateralItem(Product product, String filterName) {

		this.product = product;
		this.filterName = filterName;

	}

	public static String getOptAcceptableCollatItem() {
		return OPT_ACCEPTABLE_COLLAT_ITEM;
	}

	public Product getProduct() {
		return this.product;
	}

	public String getFilterName() {
		return this.filterName;
	}

	public String getProductIsin() {
		if ((this.product != null) && (getProduct().getSecCode("ISIN") != null)) {
			return getProduct().getSecCode("ISIN");
		}
		return "";

	}

}
