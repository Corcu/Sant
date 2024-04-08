package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.util.MarginCallConstants;

import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallAllocationFacade;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

/**
 * ReportStyle class for allocation list
 * 
 * @author aela
 * 
 */
@SuppressWarnings("rawtypes")
public class SantMCSubstitAllocationReportStyle extends SantMCAllocationReportStyle {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String PARTY = "PARTY";
	public static final String ELIGIBLE_CURRENCY = "ELIGIBLE CURRENCY";
	public static final String ELIGIBLE_COLLATERAL = "ELIGIBLE COLLATERAL";

	protected static final String TO_RETURN = " TO RETURN";
	protected static final String TO_DELIVER = " TO DELIVER";
	protected static final String PLEASE_ADVISE = "PLEASE ADVISE";
	protected static final String TBD = "TBD";

	/*
	 * (non-Javadoc)
	 * 
	 * @see calypsox.tk.report.SantMCAllocationReportStyle#getColumnValue(com.calypso
	 * .tk.collateral.MarginCallAllocationFacade, java.lang.String, java.util.Vector)
	 */
	@Override
	public Object getColumnValue(MarginCallAllocationFacade allocation, String columnName, Vector errors)
			throws InvalidParameterException {

		if (PARTY.equals(columnName)) {
			CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
					allocation.getCollateralConfigId());

			if (MarginCallAllocation.ALLOCATION_SUBSTITUTION.equals(allocation.getType())) {
				if (isPayAllocation(allocation)) {
					return marginCallConfig.getProcessingOrg().getName() + TO_RETURN;
				} else if (isReceiveAllocation(allocation)) {
					return marginCallConfig.getLegalEntity().getName() + TO_RETURN;
				}
			} else {
				if (isPayAllocation(allocation)) {
					return marginCallConfig.getProcessingOrg().getName() + TO_DELIVER;
				} else if (isReceiveAllocation(allocation)) {
					return marginCallConfig.getLegalEntity().getName() + TO_DELIVER;
				}
			}
		} else if (ELIGIBLE_COLLATERAL.equals(columnName)) {
			return super.getColumnValue(allocation, SantMCAllocationReportStyle.ASSET, errors);
		} else if (ELIGIBLE_CURRENCY.equals(columnName)) {
			return super.getColumnValue(allocation, SantMCAllocationReportStyle.CURRENCY, errors);
		}

		return super.getColumnValue(allocation, columnName, errors);
	}

	/**
	 * @param allocation
	 * @return
	 */
	protected boolean isPayAllocation(MarginCallAllocationFacade allocation) {
		return allocation.getContractValue() < 0;
	}

	/**
	 * @param allocation
	 * @return
	 */
	protected boolean isReceiveAllocation(MarginCallAllocationFacade allocation) {
		return allocation.getContractValue() >= 0;
	}

	/**
	 * @param allocation
	 * @return
	 */
	protected boolean isAdviseAllocation(MarginCallAllocationFacade allocation) {
		return MarginCallConstants.ALLOCATION_TYPE_ADVISE_SUBSTITUTION.equals(allocation.getType());
	}

}
