package calypsox.apps.reporting.util.control;

import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

import javax.swing.*;
import java.awt.*;

public class SantTextFieldPanel extends JPanel {

	private static final long serialVersionUID = 160150900678636655L;

	protected JLabel label;
	protected final JTextField field = new JTextField();

	public SantTextFieldPanel(final String name) {
		init(name, null);
	}

	public SantTextFieldPanel(final String name, final Color color) {
		init(name, color);
	}

	private void init(final String name, final Color color) {
		setLayout(new BorderLayout());

		this.label = new JLabel();
		this.label.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.label.setText(name);
		if (color != null) {
			this.label.setForeground(color);
		}

		this.label.setPreferredSize(new Dimension(86, 24));
		this.label.setMaximumSize(new Dimension(86, 24));
		this.label.setMinimumSize(new Dimension(86, 24));
		this.field.setPreferredSize(new Dimension(149, 24));

		final Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(this.label, null);
		box.add(Box.createRigidArea(new Dimension(5, 0)));
		box.add(this.field);

		add(box, BorderLayout.CENTER);

		setPreferredSize(new Dimension(240, 24));

	}

	public void setLabelName(String name) {
		this.label.setText(name);
	}

	public String getValue() {
		return this.field.getText();
	}

	public Double getDoubleValue() {
		return Util.stringToNumber(getValue());
	}

	public void setValue(final String value) {
		this.field.setText(value);
	}

	public void setValue(final ReportTemplate reportTemplate, final String key) {
		setValue("");
		Object value = reportTemplate.get(key);
		if (value != null) {
			String s = "";
			if (value instanceof Double) {
				s = Util.numberToString((Double) value);
			} else {
				s = (String) reportTemplate.get(key);
			}
			if (s != null) {
				setValue(s);
			}
		}
	}

	public JTextField getJTextField() {
		return this.field;
	}
}
