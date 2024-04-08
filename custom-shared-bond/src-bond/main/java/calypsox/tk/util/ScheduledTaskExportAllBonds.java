package calypsox.tk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import calypsox.tk.event.PSEventProduct;

public class ScheduledTaskExportAllBonds extends ScheduledTask {
    private static final String WHERE_FIXED_PART = " PRODUCT_DESC.product_family = 'Bond'AND PRODUCT_DESC.CURRENCY ";
    private static final String WHERE_MATDATE = " AND PRODUCT_DESC.MATURITY_DATE >= CURRENT_DATE ";
    public static final String INCLUDE_MATURED = "Include Matured Bonds (Default = FALSE)";
    
    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        boolean res = true;
        
        String[] currencies = new String[] {"EUR", "USD"};
		String from = null;
		
		String[] whereClauses = buildBondsWhereClause(currencies);

	    Vector<Bond> allBonds = null;
		List<PSEvent> events = null;
	    for (int i = 0; i < whereClauses.length; i++) {
	    	events = new ArrayList<PSEvent>();
	    	
	    	String whereClause = whereClauses[i];
	    	try {
	    		allBonds = ds.getRemoteProduct().getAllProducts(from, whereClause, null);
	    		
	    		Log.info(this, "Query: " + whereClause);
	    		Log.info(this, "Saving a total of Bonds for this query: " + allBonds.size());
	    		for (int j = 0; j < allBonds.size(); j++) {
	    			PSEventProduct event = new PSEventProduct();
	    			Bond bond = allBonds.get(j);
	    			event.setProduct(bond);
	    			events.add(event);

	    			if ((j % 1000) == 0) {
	    				Log.info(this, "Current Bond index: " + j + "/" + allBonds.size());
	    			}
	    		}
	    		ds.getRemoteTrade().saveAndPublish(events);

	    	} catch (CalypsoServiceException e) {
	    		Log.error(this, e.toString());
	    	}
	    }
        
        return res;
    }
    
	private String[] buildBondsWhereClause(String[] currencies) {
		String whereFixedPart = WHERE_FIXED_PART;
		Boolean includeMaturedBondsB = getBooleanAttribute(INCLUDE_MATURED, false);
		if (includeMaturedBondsB) {
			whereFixedPart += WHERE_MATDATE;
		}
		
		String[] whereClauses = new String[currencies.length + 1];
		String invertedWhereClause = "";
		for (int i = 0; i < currencies.length; i++) {
			whereClauses[i] = whereFixedPart + " = '" + currencies[i] + "'";
			
			invertedWhereClause += " '" + currencies[i] + "' ";
			if (i < currencies.length - 1) {
				invertedWhereClause += ", ";
			}
		}
		whereClauses[currencies.length] = whereFixedPart + " NOT IN (" + invertedWhereClause + ")";
		
		return whereClauses;
	}

    
    @Override
    public Vector getDomainAttributes() {
        final Vector result = super.getDomainAttributes();
        result.add(INCLUDE_MATURED);
        return result;
    }

    @Override
    public String getTaskInformation() {
        return "Generates a PSEventProduct for each Bond in the system (either ONLY non-matured of ALL), to be exported by the export engine.";
    }
}
