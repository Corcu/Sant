package calypsox.tk.report;

import com.calypso.tk.collateral.dto.SecurityPositionDTO;
import com.calypso.tk.core.Product;
import java.util.List;

import com.calypso.tk.collateral.dto.SecurityAllocationDTO;

public class ELBEIsinCollatPositionItem {

	private Product product = null;
	private double value = 0.0;
	private String currency = null;
	private double  balanceTitulo = 0.0;
	
	public ELBEIsinCollatPositionItem(SecurityPositionDTO securityPositionDTO) {
		super();
		product = securityPositionDTO.getProduct();
		value = securityPositionDTO.getValue();
		currency = securityPositionDTO.getProduct().getCurrency();
		balanceTitulo = securityPositionDTO.getContractValue();
	
	}
	
	public ELBEIsinCollatPositionItem(SecurityAllocationDTO securityAllocationDTO) {
		super();
		product = securityAllocationDTO.getProduct();
		value = securityAllocationDTO.getValue();
		currency = securityAllocationDTO.getCurrency();
		balanceTitulo = securityAllocationDTO.getContractValue();
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public double getBalanceTitulo() {
		return balanceTitulo;
	}

	public void setBalanceTitulo(double balanceTitulo) {
		this.balanceTitulo = balanceTitulo;
	}
	
	
	
}