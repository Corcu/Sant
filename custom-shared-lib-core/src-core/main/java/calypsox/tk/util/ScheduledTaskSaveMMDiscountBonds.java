package calypsox.tk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.BondMMDiscount;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

/**
 * To import StockLending rates
 **/
public class ScheduledTaskSaveMMDiscountBonds  extends ScheduledTask {
	private static final long serialVersionUID = 123L;

	@Override
	public String getTaskInformation() {
		return "ST to Save MMDiscount Bonds in order to update Quotes";
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
			Vector<BondMMDiscount> bonds = (Vector<BondMMDiscount>)DSConnection.getDefault().getRemoteProduct().getAllProducts(null, " product_type = 'BondMMDiscount' ", null);

			int count = 1;
			for (BondMMDiscount bond : bonds) {
				String isin = bond.getSecCode("ISIN");
				Log.error(this, count + "/" + bonds.size() + " - Saving Bond " + isin);
				DSConnection.getDefault().getRemoteProduct().saveBond(bond, true);
				
				// Get Quote Name as it would exist for natural MMDiscount
				String reIssue = "";
				if ((!Util.isEmptyString(bond.getSecCode("When-Issued"))) && 
						(bond.getSecCode("When-Issued").equalsIgnoreCase("Re-Issue"))) {
					reIssue = ".Re-Issue";
				}
				String oldQuoteName = "DISCOUNT." + bond.getName() + "." + Util.idateToString(bond.getMaturityDate()) + reIssue;
				
				int countOldQuotes = DSConnection.getDefault().getRemoteMarketData().countQuotes(oldQuoteName);
				if (countOldQuotes == 0) {
					Log.error(this, "  > No old Quotes found.");
					
					Log.error(null, "  > Removing Old Quote Name");
					DSConnection.getDefault().getRemoteProduct().removeQuoteName(oldQuoteName);
					
					count++;
					continue;
				}

				Log.error(this, "  > Duplicating Quotes");
				DSConnection.getDefault().getRemoteMarketData().saveQuotesFromName(oldQuoteName, bond.getQuoteName());
				
				Log.error(this, "  > Removing Old Quote Name");
				DSConnection.getDefault().getRemoteProduct().removeQuoteName(oldQuoteName);
				
				count++;
			}
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error : " + e.toString());
		}
		
		return true;
	}
}
