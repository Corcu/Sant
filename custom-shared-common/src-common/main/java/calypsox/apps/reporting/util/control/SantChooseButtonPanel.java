package calypsox.apps.reporting.util.control;

import java.awt.BorderLayout;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.ui.component.dialog.DualListDialog;

public class SantChooseButtonPanel extends JPanel {

	private static final long serialVersionUID = 376014758158782547L;

	private final JTextField field = new JTextField();

	private Vector<String> domains = null;

	private int maxSelectableItems;

	public SantChooseButtonPanel(final String name, final String domainValue) {
		initDomains(domainValue);
		init(name);
	}

	public SantChooseButtonPanel(final String name, final Collection<String> domains) {
		initDomains(domains);
		init(name);
	}

	public SantChooseButtonPanel(final String name, final Collection<String> domains, final int maxSelectableItems) {
		this(name, domains);
		this.maxSelectableItems = maxSelectableItems;
	}

	private void initDomains(final String domainValue) {
		this.domains = LocalCache.getDomainValues(DSConnection.getDefault(), domainValue);
	}

	private void initDomains(final Collection<String> domains) {
		this.domains = new Vector<String>(domains);
	}

	public int getMaxSelectableItems() {
		return this.maxSelectableItems;
	}

	public void setMaxSelectableItems(int maxSelectableItems) {
		this.maxSelectableItems = maxSelectableItems;
	}

	private void init(final String name) {
		setLayout(new BorderLayout());
		final JLabel label = new JLabel();
		final JButton button = new JButton();

		label.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		label.setText(name);
		label.setMinimumSize(new Dimension(86, 24));
		label.setPreferredSize(new Dimension(86, 24));
		this.field.setPreferredSize(new Dimension(117, 24));
		button.setText("...");
		button.setActionCommand("...");
		button.setMinimumSize(new Dimension(32, 24));
		button.setPreferredSize(new Dimension(32, 24));
		button.setMaximumSize(new Dimension(32, 24));

		final Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(label);
		box.add(Box.createRigidArea(new Dimension(5, 0)));
		box.add(this.field);
		box.add(button);

		add(box, BorderLayout.CENTER);

		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent evt) {
				try {
					Vector<String> myVector = Util.string2Vector(SantChooseButtonPanel.this.field.getText());
					if (SantChooseButtonPanel.this.maxSelectableItems != 0) {
						// MIGRATION V14.4
						myVector = (Vector<String>)DualListDialog.chooseList(new Vector<String>(), SantChooseButtonPanel.this, SantChooseButtonPanel.this.domains,
								myVector, true, null, "Agreement Chooser",
								SantChooseButtonPanel.this.maxSelectableItems, true, true);
					} else {
						myVector = (Vector<String>)DualListDialog.chooseList(new Vector<String>(), SantChooseButtonPanel.this, SantChooseButtonPanel.this.domains,
								myVector, null, true, true);
					}
					if (myVector != null) {
						SantChooseButtonPanel.this.field.setText(Util.collectionToString(myVector));
					}
				} catch (final Exception e) {
					Log.error(this, e);
				}

			}
		});
	}

	public String getValue() {
		return this.field.getText();
	}

	public void setValue(final String value) {
		this.field.setText(value);
	}

	public void setValue(final ReportTemplate reportTemplate, final String key) {
		setValue("");
		final String s = (String) reportTemplate.get(key);
		if (s != null) {
			setValue(s);
		}
	}

	public void setValue(final ReportTemplate reportTemplate, final String key, final Map<Integer, String> maps) {
		setValue("");
		final String s = (String) reportTemplate.get(key);
		if (s != null) {
			final Vector<String> names = new Vector<String>();
			final Vector<String> ids = Util.string2Vector(s);
			for (final String id : ids) {
				names.add(maps.get(Integer.valueOf(id)));
			}
			setValue(Util.collectionToString(names));
		}
	}
}
