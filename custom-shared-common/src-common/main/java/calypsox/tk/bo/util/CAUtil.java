package calypsox.tk.bo.util;

import java.util.Vector;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

/**
 * Class with all necessary stuff to manage ELBE & KGR contracts files
 * 
 * @author Jose Maria Salamanca
 */
public class CAUtil {

	private static String CA_SUBTYPE_DV = "CARefSubtype";

	public static String getCASwiftEventCode(DSConnection dsCon, Product product) {
		String swiftEventCodeName = null;
		Vector<String> subtypes = LocalCache.getDomainValues(dsCon, CA_SUBTYPE_DV);
		if (subtypes != null && !subtypes.isEmpty()) {
			String subtype = subtypes.contains(product.getSubType()) ? product.getSubType() : "default";
			swiftEventCodeName = LocalCache.getDomainValueComment(dsCon, CA_SUBTYPE_DV, subtype);
		} else {
			Log.error(CAUtil.class, "Unable to get domainName " + CA_SUBTYPE_DV);
		}
		return swiftEventCodeName;
	}
}
