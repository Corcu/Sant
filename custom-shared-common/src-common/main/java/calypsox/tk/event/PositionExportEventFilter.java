package calypsox.tk.event;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;

public class PositionExportEventFilter extends GenericPositionEventFilter {
	
	public String getPositionType() {
		return InventorySecurityPosition.THEORETICAL_TYPE;
	}
	
	public String getInternalExternal() {
		return InventorySecurityPosition.MARGIN_CLASS;
	}
	
	public String getDateType() {
		return InventorySecurityPosition.SETTLE_DATE;
	}
	
	public JDate getPositionDate() {
		return null;
	}

	public Integer getConfigId() {
		return 0;
	}

}
