package calypsox.apps.reporting.util.control;

import com.calypso.tk.report.ReportTemplate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class SantCheckBoxPanel extends JPanel {

	private static final long serialVersionUID = 8155637539068132100L;

	private final JCheckBox checkBox = new JCheckBox();
	private final JLabel label = new JLabel();

	public SantCheckBoxPanel(final String name) {
		init(name);
	}

	public SantCheckBoxPanel(final String name, final int width) {
		init(name);
		this.label.setPreferredSize(new Dimension(width, 24));
	}

	private void init(final String name) {
		setLayout(new BorderLayout());
		// final JLabel label = new JLabel();
		this.label.setText(name);
		this.label.setPreferredSize(new Dimension(80, 24));
		final Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(Box.createRigidArea(new Dimension(10, 0)));
		box.add(this.checkBox);
		box.add(Box.createRigidArea(new Dimension(5, 0)));
		box.add(this.label);
		add(box, BorderLayout.WEST);
	}

	public boolean getValue() {
		return this.checkBox.isSelected();
	}

	public void setValue(final String value) {
		this.checkBox.setSelected(Boolean.valueOf(value));

	}

	public void setValue(final Boolean value) {
		this.checkBox.setSelected(value);

	}

	public void setValue(final ReportTemplate reportTemplate, final String key) {
		setValue(false);
		final Boolean s = (Boolean) reportTemplate.get(key);
		if (s != null) {
			setValue(s);
		}

	}

	public void setListener(String actionCommand, ActionListener l) {
		this.checkBox.setActionCommand(actionCommand);
		this.checkBox.addActionListener(l);

	}
}
