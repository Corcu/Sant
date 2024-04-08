package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.CalypsoTextField;
import com.calypso.apps.util.LegalEntityTextPanel;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportTemplate;

import javax.swing.*;
import java.awt.*;

public class NettingMethodsReportTemplatePanel extends ReportTemplatePanel {
    private ReportTemplate _template;
    private LegalEntityTextPanel _lePanel = null;
    private CalypsoTextField _method = null;
    private JLabel _lmethod = null;

    public NettingMethodsReportTemplatePanel() {
        try {
            this.jbInit();
        } catch (Exception var2) {
            Log.error(this, var2);
        }
    }

    @Override
    public void setTemplate(ReportTemplate template) {
        this._template = template;
        String s = (String)this._template.get("LegalEntities");
        String role = (String)this._template.get("Role");
        String method = (String)this._template.get("MethodFilter");
        if (s == null) {
            s = "";
        }
        if (role == null) {
            role = "ProcessingOrg";
        }
        if (method == null) {
            method = "";
        }

        this._lePanel.setSelectedRole(role);
        this._lePanel.setLEIdsStr(s);
        this._method.setText(method);
    }

    @Override
    public ReportTemplate getTemplate() {
        this._template.put("LegalEntities", this._lePanel.getLEIdsStr());
        this._template.put("Role", this._lePanel.getRole());
        this._template.put("MethodFilter", this._method.getText());
        return this._template;
    }

    private void jbInit() {
        this._lePanel = new LegalEntityTextPanel();
        this._lePanel.setRole((String)null, "Legal Entity", true, true);
        this._lePanel.allowMultiple(true);
        this._lePanel.setEditable(true);

        this._lmethod = new JLabel("Method: ");
        this._method = new CalypsoTextField();
        this.setSize(new Dimension(100, 210));


        final JPanel methodFilter = new JPanel();
        methodFilter.setLayout(new GridLayout(1, 1));
        methodFilter.add(this._lmethod);
        methodFilter.add(this._method);

        final JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BorderLayout());
        masterPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        add(masterPanel);

        masterPanel.add(methodFilter, BorderLayout.NORTH);
        masterPanel.add(this._lePanel, BorderLayout.CENTER);



    }
}
