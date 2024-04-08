package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TaskReportStyle;

//Project: MISSING_ISIN

/**
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 * @author Carlos Humberto Cejudo Bermejo <c.cejudo.bermejo@accenture.com >
 *
 */
public class SantMissingIsinReportStyle extends TaskReportStyle {

    private static final long serialVersionUID = -8286895716853692630L;
    private static final String ISIN = "ISIN";
    private static final String AGENT = "Agent";
    private static final String CP_SHORT_NAME = "Counterparty";
    private static final String CURRENT_DATE = "Current date";
    private static final String CONTRACT_TYPE = "Contract Type";
    private static final String CONTRACT_CSD_TYPE = "CSD Type";
    

    public static final String[] DEFAULTS_COLUMNS = { ISIN, AGENT,
            CP_SHORT_NAME, CURRENT_DATE, CONTRACT_TYPE, CONTRACT_CSD_TYPE  };

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.calypso.tk.report.ReportStyle#getColumnValue(com.calypso.tk.report.
     * ReportRow, java.lang.String, java.util.Vector)
     */
    @Override
    public Object getColumnValue(ReportRow row, String columnName,
            final Vector errors) throws InvalidParameterException {
        SantMissingIsinItem item = (SantMissingIsinItem) row
                .getProperty(SantMissingIsinReport.ROW_PROPERTY_MISSING_ISIN);

        if (ISIN.equals(columnName)) {
            return item.getIsin();
        } else if (AGENT.equals(columnName)) {
            return item.getAgent();
        } else if (CP_SHORT_NAME.equals(columnName)) {
            return item.getCpShortName();
        } else if (CURRENT_DATE.equals(columnName)) {
            return item.getCurrentDate();
        }else if(CONTRACT_TYPE.equals(columnName)) {
        		return item.getContractType();
        }else if(CONTRACT_CSD_TYPE.equals(columnName)) {
        		return item.getCsdType();
        }
        return null;
    }

	@Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();
		treeList.add(ISIN);
		treeList.add(AGENT);
		treeList.add(CP_SHORT_NAME);
		treeList.add(CURRENT_DATE);
		treeList.add(CONTRACT_TYPE);
		treeList.add(CONTRACT_CSD_TYPE);
		return treeList;
	}
	
}
