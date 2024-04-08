package calypsox.tk.report;

import java.io.Serializable;

import com.calypso.tk.refdata.CollateralConfig;

public class SantHaircutConfigurationItem implements Serializable {

	private static final long serialVersionUID = 1234L;

	public static final String HAIRCUT_CONF_ITEM = "SantHaircutConfigurationItem";

	public SantHaircutConfigurationItem(CollateralConfig contract, String currency1, String currency2, String haircutValue) {

		this.contract = contract;
		this.contractName = contract.getName();
		this.processingOrgCode = contract.getProcessingOrg().getCode();
		this.currency1 = currency1;
		this.currency2 = currency2;
		this.haircutValue = haircutValue;
	}

	private final CollateralConfig contract;
	private final String contractName;
	private final String processingOrgCode;
	private final String currency1;
	private final String currency2;
	private final String haircutValue;
	

	public static String getHaircutConfItem() {
		return HAIRCUT_CONF_ITEM;
	}

	public CollateralConfig getContract() {
		return this.contract;
	}

	public String getHaircutValue() {
		return this.haircutValue;
	}
	
	public String getContractName() {
		return this.contractName;
	}

	public String getProcessingOrgCode() {
		return this.processingOrgCode;
	}

	public String getCurrency1() {
		return this.currency1;
	}

	public String getCurrency2() {
		return this.currency2;
	}
}
