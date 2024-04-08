package calypsox.tk.util.optimizer.position;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;

public class TestOptimizerPositions {
	
	public static void main(String[] args) {
		OptimizerPositions optPos = new OptimizerPositions(JDate.getNow(), "BSTE", 30, 80);
		
		InventorySecurityPosition invSecPos = new InventorySecurityPosition();
		invSecPos.setSecurityId(30);
		invSecPos.setPositionType(InventorySecurityPosition.THEORETICAL_TYPE);
		invSecPos.setPositionDate(JDate.getNow().addDays(1));
		invSecPos.setTotalSecurity(500);
		invSecPos.setTotalPledgedOut(50);
		
		optPos.addPosition(invSecPos, JDate.getNow());
		
		System.out.println(optPos.toString());
	}

}
