package calypsox.apps.refdata;

import javax.swing.JFrame;

import com.calypso.apps.refdata.collateral.MarginCallConfigPanel;
import com.calypso.tk.refdata.MarginCallConfig;

import calypsox.apps.refdata.collateral.CollateralConfigPanel;

public class BOMarginCallConfigWindow extends JFrame{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static MarginCallConfigPanel instance;
	
	public BOMarginCallConfigWindow()
	  {
		instance = (MarginCallConfigPanel) new CollateralConfigPanel();
		instance.initialize(this);

	   }
	public void showMarginCallConfig(int mccId) {
		instance.showMarginCallConfig(mccId, true);
	}
	public void setReadOnly() {
		instance.setReadOnly();
	}
	public void showMarginCallConfig(MarginCallConfig mcc) {
		instance.showMarginCallConfig(mcc.getImplementation());
	}
	public MarginCallConfigPanel getMarginCallConfigPanel() {
		return instance;
	}
	
	
}


