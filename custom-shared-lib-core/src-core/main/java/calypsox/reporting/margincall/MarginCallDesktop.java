package calypsox.reporting.margincall;


import calypsox.apps.collateral.manager.controller.CustomMarginCallDesktopController;
import com.calypso.apps.collateral.manager.controller.MarginCallDesktopController;
import com.calypso.apps.navigator.NavigatorAwareLauncher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 */
public class MarginCallDesktop extends com.calypso.apps.reporting.margincall.MarginCallDesktop implements NavigatorAwareLauncher {

    private static final long serialVersionUID = 1L;

    /**
     * @return
     */
    @Override
    protected MarginCallDesktopController initController() {
        return new CustomMarginCallDesktopController(this);
    }

    @Override
    public void launchApp() {

    }

    @Override
    public void launchAppWithState(Element state) {

    }

    @Override
    public Element getAppState(Document document) {
        return null;
    }
}
