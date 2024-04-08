package calypsox.tk.event;

import com.calypso.tk.core.Product;
import com.calypso.tk.event.PSEvent;

public class PSEventProduct extends PSEvent {
	private static final long serialVersionUID = 5501923200658522049L;
	String refInterna;
	Product product;
	
	public PSEventProduct() {
		super();
	}

	public String getRefInterna() {
		return refInterna;
	}

	public void setRefInterna(String refInterna) {
		this.refInterna = refInterna;
	}
	
	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

}
