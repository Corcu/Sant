package calypsox.tk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

/**
 * To Save Equities
 **/
public class ScheduledTaskSaveEquities  extends ScheduledTask {
	@Override
	public String getTaskInformation() {
		return "ST to Save Equities Bonds in order to update Quotes";
	}

	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();

		return attributeList;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {
		try {
			Vector<Equity> equities = DSConnection.getDefault().getRemoteProduct().getAllProducts(null, " product_desc.product_type = 'Equity' ", null);
			 
			int count = 1;
			for (Equity equity : equities) {
				String isin = equity.getSecCode("ISIN"); 
				String identifier = isin;
				if (Util.isEmpty(identifier)) {
					identifier = String.valueOf(equity.getId());
				}
				Log.error(this, count + "/" + equities.size() + " - Saving Equity " + identifier);
				DSConnection.getDefault().getRemoteProduct().saveProduct(equity, true);
				
				Log.error(this, "  > Saving Quote Name");
				DSConnection.getDefault().getRemoteProduct().saveEquityQuoteNames(equity);
				
				StringBuilder sb = new StringBuilder();
				sb.append(equity.getType());
				sb.append(".ISIN_");
				sb.append(equity.getSecCode("ISIN"));
				sb.append("_");
				sb.append(equity.getCurrency());
				String oldQuoteName = sb.toString();
				
				Log.error(this, "  > Duplicating Quotes");
				DSConnection.getDefault().getRemoteMarketData().saveQuotesFromName(oldQuoteName, equity.getQuoteName());
				
				Log.error(this, "  > Removing Old Quote Name");
				DSConnection.getDefault().getRemoteProduct().removeQuoteName(oldQuoteName);
				DSConnection.getDefault().getRemoteProduct().removeQuoteName(oldQuoteName + "." + "EDSP");
				DSConnection.getDefault().getRemoteProduct().removeQuoteName(oldQuoteName + "." + "VWAP");
				DSConnection.getDefault().getRemoteProduct().removeQuoteName(oldQuoteName + "." + "PDR");
				
				count++;
			}
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error : " + e.toString());
		}
		
		return true;
	}
}
