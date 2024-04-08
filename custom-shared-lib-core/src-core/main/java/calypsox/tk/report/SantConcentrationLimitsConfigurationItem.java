package calypsox.tk.report;

import java.io.Serializable;

import com.calypso.tk.refdata.CollateralConfig;

public class SantConcentrationLimitsConfigurationItem implements Serializable {

	private static final long serialVersionUID = 12345L;

	public static final String CONC_LIM_CONF_ITEM = "SantConcentrationLimitsConfigurationItem";

	public SantConcentrationLimitsConfigurationItem(CollateralConfig contract, String ruleType, String minMax, String ruleParameter, double parameterValue) {

		this.contract = contract;
		this.processingOrgCode = contract.getProcessingOrg().getCode();
		this.ruleType = ruleType;
		this.minMax = minMax;
		this.ruleParameter = ruleParameter;
		this.parameterValue = parameterValue;
	}

	private final CollateralConfig contract;
	private final String processingOrgCode;
	private final String ruleType;
	private final String minMax;
	private final String ruleParameter;
	private final double parameterValue;
	

	public static String getConcLimConfItem() {
		return CONC_LIM_CONF_ITEM;
	}

	public CollateralConfig getContract() {
		return this.contract;
	}
	
	public String getProcessingOrgCode() {
		return processingOrgCode;
	}

	public String getRuleType() {
		return this.ruleType;
	}

	public String getMinMax() {
		return this.minMax;
	}

	public String getRuleParameter() {
		return this.ruleParameter;
	}

	public double getParameterValue() {
		return this.parameterValue;
	}

	
}
