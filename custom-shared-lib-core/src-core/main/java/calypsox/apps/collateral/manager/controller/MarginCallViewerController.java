package calypsox.apps.collateral.manager.controller;

import calypsox.apps.collateral.MarginCallUtil;
import com.calypso.apps.collateral.manager.controller.MarginCallDesktopController;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.Util;

import java.awt.event.ActionEvent;

public class MarginCallViewerController extends com.calypso.apps.collateral.manager.controller.MarginCallViewerController {

    public MarginCallViewerController(MarginCallDesktopController controller) {
        super(controller);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String action = actionEvent.getActionCommand();
        if (Util.isEmpty(action)) {
            return;
        }
        if ("MC_ACTION_OPTIMIZE_CONFIG".equals(action)) {
            displayOptimization();
        } else if ("ACTION_MC_RISK_CONFIG".equals(action)) {
            // MIG V16, commented out by the moment.
            //displayRiskScenario();
        } else {
            MarginCallEntry entry = getSelectedMarginCallEntry();
            if (entry == null) {
                AppUtil.displayAdvice("No entry selected", getDesktop());
                return;
            }
            if ("MC_ACTION_SHOW_CONTRACT".equals(action)) {
                MarginCallUtil.showContract(entry, getReportPanel());
            }
        }
    }


}
