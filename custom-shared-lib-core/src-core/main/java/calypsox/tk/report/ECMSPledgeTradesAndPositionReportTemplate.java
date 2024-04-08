package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Locale;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;

public class ECMSPledgeTradesAndPositionReportTemplate extends ECMSDisponibilidadReportTemplate {
	private static final long serialVersionUID = -8104571377702470215L;
	@Override
	public void setDefaultDateColumns() {

	}
	@Override
	public String[] getColumns(boolean forConfig) {
		String[] ret = super.getColumns(forConfig);
		ArrayList<String> arr=new ArrayList<String>();
		for (String stringVal : ret) {
			JDate dateObj = null;
			dateObj = Util.MStringToDate(stringVal,Locale.getDefault(),true);
			if(dateObj==null) {
				arr.add(stringVal);
			}
		}
		return arr.toArray(new String[arr.size()]);
	}
}
