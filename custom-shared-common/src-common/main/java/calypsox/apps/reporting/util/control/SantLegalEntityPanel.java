package calypsox.apps.reporting.util.control;

import java.awt.Dimension;
import java.util.Vector;

import com.calypso.apps.util.LegalEntityTextPanel;
import com.calypso.tk.report.ReportTemplate;

public class SantLegalEntityPanel extends LegalEntityTextPanel {

    private static final long serialVersionUID = 5324550697194634815L;

    public SantLegalEntityPanel(final String role, final String name,
	    final boolean roleEditable, final boolean allowRoleAll,
	    final boolean allowMultipleSelection, final boolean isEditable) {

	setRole(role, name, roleEditable, allowRoleAll);
	allowMultiple(allowMultipleSelection);
	setEditable(isEditable);

	setPreferredSize(new Dimension(240, 24));
    }

    @SuppressWarnings("rawtypes")
    public void setValue(final ReportTemplate reportTemplate, final String key) {
	setLEIdsStr("");
	final Object value = reportTemplate.get(key);
	if (value == null) {
	    return;
	}

	if (value instanceof String) {
	    setLEIdsStr((String) value);
	} else if (value instanceof Vector) {
	    setLEIds((Vector) value);
	}

    }

}
