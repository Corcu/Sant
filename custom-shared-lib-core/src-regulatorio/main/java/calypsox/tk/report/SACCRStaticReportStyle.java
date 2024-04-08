/**
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.report.ReportRow;

/**
 * Style SA CCR BALANCES. Extends all columns for positions (cash & securities), Collateral Configs and MarginCall Entries
 * 
 * @author Guillermo Solano
 *
 * @version 1.01
 * @Date 02/01/2017
 */
public class SACCRStaticReportStyle extends calypsox.tk.report.CollateralConfigReportStyle{
		

	
	private static final long serialVersionUID = 8886628563678939426L;
	

	private static final String ENTITY_PREFIX = "Entity.";

	
	/**
	 * Override method to get columns values for the style
	 */
	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {
		
		final Object valueCol = super.getColumnValue(row, columnName, errors);
		
		if (valueCol != null)
			return valueCol;
			
		if (columnName.equals(SACCRStaticReportTemplate.PRODUCT)){
			
			return row.getProperty(SACCRStaticReportTemplate.PRODUCT);
		
		} else if (getLegalEntityStyle().isLegalEntityColumn(ENTITY_PREFIX, columnName)) {
		
			LegalEntity le = row.getProperty(SACCRStaticReportTemplate.ENTITY);
			
			if (le != null)
				return getLegalEntityStyle().getColumnValue(le, ENTITY_PREFIX, row, columnName, errors);	
		}
		
		return null;

	}
	
	/**
	 * Recovers the tree list. Add Entity as LE style for BRANCH or CPTY file.
	 */
	
	@SuppressWarnings("deprecation")
	@Override
	public TreeList getTreeList() {
		
		final TreeList treeList = super.getTreeList();
		
		if (getLegalEntityStyle() != null) {
			addSubTreeList(treeList, new Vector<String>(), ENTITY_PREFIX, getLegalEntityStyle().getTreeList());
		}
		
		treeList.add(SACCRStaticReportTemplate.PRODUCT);

		return treeList;
	}

}
