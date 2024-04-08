package calypsox.apps.reporting.util;

import java.awt.*;

public class SantLegalEntityChooser extends com.calypso.apps.util.LegalEntityChooser {
    public SantLegalEntityChooser(Frame parent) {
        super(parent);
    }

    public String getCurrentSelection() {
        return this.isCanceled() ? null : this._selection;
    }
}
