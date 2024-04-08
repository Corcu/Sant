package calypsox.tk.report;

import java.io.Serializable;

import com.calypso.tk.core.JDate;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.HaircutPoint;

public class Opt_HaircutDefinitionItem implements Serializable {

	private static final long serialVersionUID = 123L;

	public static final String OPT_HAIRCUT_DEF_ITEM = "Optimization_HaircutDefinitionItem";

	public Opt_HaircutDefinitionItem(CollateralConfig contract, String filterName, HaircutPoint haircutPoint) {

		this.contract = contract;
		this.filterName = filterName;
		this.tenor = haircutPoint.getTenor().getCode();
		this.maturityStartDate = haircutPoint.getStartMaturity();
		this.maturityEndDate = haircutPoint.getEndMaturity();
		this.haircutValue = haircutPoint.getValue();
		this.collateralAgreement = contract.getName();
		this.owner = contract.getProcessingOrg().getName();
		this.ownerCode = contract.getProcessingOrg().getCode();

	}

	private final CollateralConfig contract;
	private final String filterName;
	private final int tenor;
	private final int maturityStartDate;
	private final int maturityEndDate;
	private final double haircutValue;
	private final String collateralAgreement;
	private final String owner;
	private final String ownerCode;

	public static String getOptHaircutDefItem() {
		return OPT_HAIRCUT_DEF_ITEM;
	}

	public CollateralConfig getContract() {
		return this.contract;
	}

	public String getFilterName() {
		return this.filterName;
	}

	public int getTenor() {
		return this.tenor;
	}
	
	public String getCollateralAgreement() {
		return this.collateralAgreement;
	}

	public int getMaturityStartDate() {
		return this.maturityStartDate;
	}

	public int getMaturityEndDate() {
		return this.maturityEndDate;
	}

	public double getHaircutValue() {
		//JRL 20/04/2016 Migration 14.4
		return Math.abs(this.haircutValue);
	}
	
	public String getOwner() {
		return this.owner;
	}
	
	public String getOwnerCode() {
		return this.ownerCode;
	}

	public String getMaturityStartDateString() {
		return JDate.getNow().addTenor(getMaturityStartDate()).toString();
	}

	public String getMaturityEndDateString() {
		return JDate.getNow().addTenor(getMaturityEndDate()).toString();
	}

	public double getHaircutPercentageValue() {
		return getHaircutValue() * 100;
	}

}
