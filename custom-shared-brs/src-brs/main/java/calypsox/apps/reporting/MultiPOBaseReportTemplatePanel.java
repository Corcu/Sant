package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TransferReportTemplate;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

//CAL_EMIR_007

/**
 * This is base Template Panel which will have a PO dropdown box where you can
 * select PO. There are some reprots like TLM, KGR etc which needed to be split
 * based on PO. Those reports should have a template panel which shoud be
 * extending from this class.
 *
 */
public class MultiPOBaseReportTemplatePanel extends ReportTemplatePanel {

  private static final long serialVersionUID = 1L;

  private ReportTemplate template;

  private final JLabel jLabel2 = new JLabel();
  protected JComboBox<Object> processingOrgChoice = new JComboBox<>();

  public MultiPOBaseReportTemplatePanel() {
    try {
      jbInit();
    } catch (final Exception e) {
      Log.error(this, e);
    }
    initDomains();
  }

  private void jbInit() throws Exception {
    setLayout(null);
    setSize(new Dimension(900, 286));

    jLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel2.setText("Processing Org");
    jLabel2.setBounds(new Rectangle(100, 197, 93, 24));
    processingOrgChoice.setBounds(new Rectangle(200, 197, 151, 24));

    this.add(jLabel2, null);
    this.add(processingOrgChoice, null);
  }

  @SuppressWarnings("rawtypes")
  private void initDomains() {
    final Vector pos = AccessUtil.getAccessiblePONames(false, true);
    AppUtil.set(processingOrgChoice, pos);
  }

  public JLabel getJLabel2() {
    return jLabel2;
  }

  public JComboBox<?> getProcessingOrgChoice() {
    return processingOrgChoice;
  }

  @Override
  public ReportTemplate getTemplate() {
    if (!processingOrgChoice.getSelectedItem().equals("ALL")) {
      final String poName = (String) processingOrgChoice
          .getSelectedItem();
      template.put(TransferReportTemplate.PO_NAME, poName);
    } else {
      template.remove(TransferReportTemplate.PO_NAME);
    }
    return template;
  }

  @Override
  public void setTemplate(final ReportTemplate t) {
    final String s = (String) t.get(TransferReportTemplate.PO_NAME);
    if (s != null) {
      processingOrgChoice.setSelectedItem(s);
    } else {
      processingOrgChoice.setSelectedItem("ALL");
    }

    template = t;
  }
}
