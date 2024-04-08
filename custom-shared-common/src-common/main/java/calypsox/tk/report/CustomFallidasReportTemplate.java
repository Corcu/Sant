package calypsox.tk.report;

import java.util.List;
import com.calypso.tk.core.Attribute;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.Util;

public class CustomFallidasReportTemplate extends com.calypso.tk.report.TransferReportTemplate {
	/**
	 * Serial number asigned by Calypso.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * This class override this get method in order to correct a ClassCastException launched by Calypso core.
	 * @param key Name of the key used to check filters from accept method of the superclass.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(String key) {
		T ret = null;
		ret = super.get(key);
		if (key.equals("XferAttributes") && ret instanceof Attributes) {
			List<Attribute> lstAttr = ((Attributes) ret).getAttributeList();
			if (!Util.isEmpty(lstAttr)) {
				StringBuilder retValList = new StringBuilder();
				for (Attribute attribute : lstAttr) {
					if (retValList.length() > 0) {
						retValList.append(",");
					}
					retValList.append(attribute.getName());
				}
				return (T) retValList.toString();
			}
			return null;
		}
		return super.get(key);
	}
}
