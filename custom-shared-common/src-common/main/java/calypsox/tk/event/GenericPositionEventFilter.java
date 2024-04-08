package calypsox.tk.event;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventInventorySecPosition;

public abstract class GenericPositionEventFilter implements EventFilter {
	
	public abstract String getPositionType();
	
	public abstract String getInternalExternal();
	
	public abstract String getDateType();
	
	public abstract JDate getPositionDate();
	
	public abstract Integer getConfigId();

	@Override
	public boolean accept(PSEvent psevent) {
		
		if (psevent instanceof PSEventInventorySecPosition) {
			PSEventInventorySecPosition psInvSecPos = (PSEventInventorySecPosition) psevent;
			InventorySecurityPosition invSecPos = psInvSecPos.getPosition();
			
			if((getPositionType()!=null) && !getPositionType().equals(invSecPos.getPositionType()))
				return false;
			if((getInternalExternal()!=null) && !getInternalExternal().equals(invSecPos.getInternalExternal()))
				return false;
			if((getDateType()!=null) && !getDateType().equals(invSecPos.getDateType()))
				return false;
			if((getPositionDate()!=null) && !getPositionDate().equals(invSecPos.getPositionDate()))
				return false;
			if((getConfigId()!=null) && !getConfigId().equals(invSecPos.getConfigId()))
				return false;
			
			return true;
		}
		else
			return true;
	}

}
