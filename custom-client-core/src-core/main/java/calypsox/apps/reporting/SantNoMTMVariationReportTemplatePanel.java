package calypsox.apps.reporting;

import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.ui.component.dialog.DualListDialog;

/**
 * Report template to be imported in Delivery Notice template
 * 
 * @author aela
 * 
 */
public class SantNoMTMVariationReportTemplatePanel extends ReportTemplatePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected ReportTemplate reportTemplate = null;

	private final JLabel holidaysNameLabel = new JLabel();
	private final javax.swing.JButton HolidaysButton = new javax.swing.JButton();;
	private final javax.swing.JTextField holidaysText = new javax.swing.JTextField();;

	public SantNoMTMVariationReportTemplatePanel() {

		init();
		this.reportTemplate = null;
	}

	private void init() {
		setLayout(null);
		setSize(new Dimension(1173, 50));

		this.holidaysNameLabel.setText("Holidays: ");
		this.holidaysNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.holidaysNameLabel.setBounds(220, 10, 100, 24);
		add(this.holidaysNameLabel);

		this.holidaysText.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.holidaysText.setBounds(320, 10, 100, 24);
		this.holidaysText.setText("");
		this.holidaysText.setEditable(false);
		add(this.holidaysText);

		this.HolidaysButton.setText("...");
		this.HolidaysButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.HolidaysButton.setBounds(420, 10, 40, 24);
		this.HolidaysButton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				HolidaysButtonActionPerformed(evt);
			}
		});

		add(this.HolidaysButton);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void HolidaysButtonActionPerformed(java.awt.event.ActionEvent evt) {
		Vector sels = Util.string2Vector(this.holidaysText.getText());
		Vector all = Holiday.getCurrent().getHolidayCodeList();
		//MIG_V14
		Vector v = DualListDialog.chooseList(new Vector(), 
				this, all, sels, "Select Holiday(s)");

		if (v == null) {
			return;
		}
		this.holidaysText.setText(Util.collectionToString(v));
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		this.reportTemplate = template;

		this.holidaysText.setText(Util.collectionToString(this.reportTemplate.getHolidays()));

	}

	@Override
	public ReportTemplate getTemplate() {
		return this.reportTemplate;
	}

	public static void main(String... args) throws ConnectException {
		ConnectionUtil.connect(args, "SantMTMAuditReportTemplate");
		JFrame frame = new JFrame();
		frame.setContentPane(new SantMTMAuditReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}

}
