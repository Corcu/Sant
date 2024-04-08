package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

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
public class SantMCRequestSubstitAllocationReportStyle extends SantMCSubstitAllocationReportStyle {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see calypsox.tk.report.SantMCSubstitAllocationReportStyle#getColumnValue(
	 * com.calypso.tk.collateral.MarginCallAllocationFacade, java.lang.String, java.util.Vector)
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
		} else if (VALUE_DATE.equals(columnName)) {
			return allocation.getSettlementDate();
		}

		return super.getColumnValue(allocation, columnName, errors);
	}

}
