package calypsox.apps.collateral;


import com.calypso.apps.refdata.clearing.ClearingMemberConfigWindow;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.CollateralCacheUtil;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.SubTemplateHolder;
import com.calypso.tk.service.DSConnection;
import com.calypso.ui.image.ImageUtilities;
import com.jidesoft.swing.JideSwingUtilities;

import calypsox.apps.refdata.BOMarginCallConfigWindow;
import calypsox.tk.collateral.service.RemoteSantCollateralService;

import java.awt.Image;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

public class MarginCallUtil {
	public static String LOG_CATEGORY = "MarginCallUtil";
	public static final int INTEREST_TYPE_INTEREST = 0;
	public static final int INTEREST_TYPE_INTEREST_EXPOSURE = 1;

	public static void showContract(MarginCallEntry entry, ReportPanel reportPanel) {
		if (entry != null)
			showContract(entry.getCollateralConfig(), reportPanel);
	}

	public static void showContract(CollateralConfig config, ReportPanel reportPanel) {
		if (config != null)
			if (config.getClearingMemberConfiguration() == null) {
				BOMarginCallConfigWindow w = new BOMarginCallConfigWindow();
				w.setLocationRelativeTo(JideSwingUtilities.getFrame(reportPanel));

				w.showMarginCallConfig(config.getId());
				w.setVisible(true);
			} else {
				ClearingMemberConfigWindow w = new ClearingMemberConfigWindow();
				w.setLocationRelativeTo(JideSwingUtilities.getFrame(reportPanel));

				w.showMarginCallConfig(config.getId());
				w.setVisible(true);
			}
	}

	public static void addReportMenu(JMenu menu, ActionListener listener) {
		menu.add(instanciateMenuItem("Preferences...", "MC_ACTION_LOAD_TEMPLATE", listener));
	}

	public static JMenuItem instanciateMenuItem(String name, String iconName, String action,
			ActionListener actionListener) {
		JMenuItem menuItem = new JMenuItem(name, ImageUtilities.getIcon(iconName));

		menuItem.setActionCommand(action);
		menuItem.addActionListener(actionListener);
		return menuItem;
	}

	public static JMenuItem instanciateMenuItem(String name, String action, ActionListener actionListener) {
		JMenuItem menuItem = new JMenuItem(name);
		menuItem.setActionCommand(action);
		menuItem.addActionListener(actionListener);
		return menuItem;
	}

	public static JRadioButtonMenuItem instanciateJRadioButtonMenuItem(String name, String action,
			ActionListener actionListener) {
		return instanciateJRadioButtonMenuItem(name, action, false, actionListener);
	}

	public static JRadioButtonMenuItem instanciateJRadioButtonMenuItem(String name, String action, boolean state,
			ActionListener actionListener) {
		JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(name, state);
		menuItem.setActionCommand(action);
		menuItem.addActionListener(actionListener);
		return menuItem;
	}

	public static JCheckBoxMenuItem instanciateCheckBoxMenuItem(String name, String action,
			ActionListener actionListener) {
		return instanciateCheckBoxMenuItem(name, action, false, actionListener);
	}

	public static JCheckBoxMenuItem instanciateCheckBoxMenuItem(String name, String action, boolean state,
			ActionListener actionListener) {
		JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(name);
		menuItem.setActionCommand(action);
		menuItem.addActionListener(actionListener);
		return menuItem;
	}

	public static ReportTemplate getSubReportTemplate(SubTemplateHolder template, String name) {
		ReportTemplate result = null;
		if (template != null) {
			result = template.getTemplate(name);

			if (result == null) {
				Object value = template.get(name);

				if (value instanceof Integer) {
					Integer templateId = (Integer) value;
					if ((templateId != null) && (templateId.intValue() > 0)) {
						result = CollateralCacheUtil.getReportTemplate(Integer.valueOf(templateId.intValue()));
					}

				}

				template.setSubTemplate(name, result);
			}
		}
		return result;
	}

	public static void setCalypsoIcon(JDialog dialog) {
		Image calypsoIcon = ImageUtilities.getImage(dialog, "com/calypso/icons/16icon.gif");

		if (calypsoIcon != null)
			dialog.setIconImage(calypsoIcon);
	}
	
	/**
	 * @param addtitionalFields
	 * @return List of contracts {@link CollateralConfig}
	 */
	public static List<CollateralConfig> loadContractsFilterByAdditionalField(HashMap<String, String> addtitionalFields) {
		
		List<CollateralConfig> contracts = new ArrayList<>();
		if(addtitionalFields!=null && addtitionalFields.size()>0) {
			RemoteSantCollateralService remoteColService = (RemoteSantCollateralService) DSConnection.getDefault().getRMIService("baseSantCollateralService",
					RemoteSantCollateralService.class);
			
			if(remoteColService!=null) {
				try {
					contracts = remoteColService.getMarginCallConfigByAdditionalField(addtitionalFields);
				} catch (PersistenceException e) {
					Log.error(MarginCallUtil.class,"Cannot load contracts: " + e);
				}
			}
		}
		return contracts;
	}
	
}
