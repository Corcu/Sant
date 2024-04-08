package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import calypsox.tk.util.OptimizerMarginCallEntryConstants;
import calypsox.tk.util.SantCollateralOptimConstants;

public class OptimizerMarginCallEntryReportStyle extends
		MarginCallEntryReportStyle implements OptimizerMarginCallEntryConstants {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -6602889808584555613L;

	public static final String OPTIMIZE_FIELD = "Optimize";
	public static final String FULL_NAME_LE = "Full Name Legal Entity";
	public static final String FULL_NAME_PO = "Full Name Owner";
	public static final String MC_VALIDATION = "MC Validation";


	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			final Vector errors) throws InvalidParameterException {
		if (row == null) {
			return null;
		}

		MarginCallEntryDTO mce = (MarginCallEntryDTO) row
				.getProperty(DEFAULT_PROPERTY);

		if (columnName.equals(OPTIMIZE_FIELD)) {
			if (Boolean.TRUE.equals(row.getProperty(OPTIMIZE_PROPERTY))) {
				return Boolean.TRUE;
			}
			if (Boolean.FALSE.equals(row.getProperty(OPTIMIZE_PROPERTY))) {
				return Boolean.FALSE;
			}
			if (SELECT_TRUE
					.equals(this
							.getProperty(OPTIMIZE_SELECT_ALL))) {
				return Boolean.TRUE;
			} else if (SELECT_TRUE
					.equals(this
							.getProperty(OPTIMIZE_UNSELECT_ALL))) {
				return Boolean.FALSE;
			}
			if (SantCollateralOptimConstants.OPTIMIZER_TO_BE_SENT_STATUS_VALUE.equals(mce.getAttribute(SantCollateralOptimConstants.OPTIMIZER_SEND_STATUS))) {
				return Boolean.TRUE;
			}
			return Boolean.FALSE;
		
		// GSM 06/05/15 - Feature #184 Inclusi?n de 3 campos en el OptimizerMarginCallEntry Report de Calypso
		} else if (columnName.equals(FULL_NAME_LE)){
			
			final CollateralConfig c = getCollateralConfig(mce);
			if (c != null && c.getLegalEntity() != null){
				return c.getLegalEntity().getName();		
			}
			
		} else if (columnName.equals(FULL_NAME_PO)){
			
			final CollateralConfig c = getCollateralConfig(mce);
			if (c != null && c.getProcessingOrg() != null){
				return c.getProcessingOrg().getName();		
			}
			
		} else if (columnName.equals(MC_VALIDATION)){
			
			final CollateralConfig c = getCollateralConfig(mce);
			if (c != null && c.getAdditionalFields() != null){
				return c.getAdditionalFields().get("MC_VALIDATION");
			}	
		}


		return super.getColumnValue(row, columnName, errors);
	}
	
	/**
	 * @param MarginCallEntryDTO
	 * @return CollateralConfig
	 */
	private CollateralConfig getCollateralConfig(final MarginCallEntryDTO entry) {
		
		if (entry == null) {
			return null;
		}
		CollateralConfig collateralConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
				entry.getCollateralConfigId());
		return collateralConfig;
	}
}
