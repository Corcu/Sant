package calypsox.apps.reporting.util.control;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.calypso.infra.util.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class SantComboBoxPanel<K, V> extends JPanel {

	private static final long serialVersionUID = -6357868061600389016L;

	private final FilterComboBox choice = new FilterComboBox(false);

	protected final JLabel agrTypeLabel = new JLabel();

	private Map<K, V> map = null;

	public SantComboBoxPanel(final String name, final String domainValue, final boolean flag) {
		init(name);
		initDomains(domainValue, flag);
	}

	public SantComboBoxPanel(final String name, final Collection<V> domains) {
		init(name);
		initDomains(domains);
	}

	public SantComboBoxPanel(final String name, final Map<K, V> map) {
		this.map = map;
		init(name);
		initDomains(map.values());
	}

	private void initDomains(final String domainValue, final boolean flag) {
		final Vector<String> domains = LocalCache.getDomainValues(DSConnection.getDefault(), domainValue);
		if (Util.isEmpty(domains)) {
			domains.insertElementAt("", 0);
		}
		if (flag && !domains.get(0).equals("")) {
			domains.insertElementAt("", 0);
		}

		final Object[] objTab = domains.toArray(new Object[domains.size()]);
		this.choice.setTheWholeItems(objTab);
	}

	private void initDomains(final Collection<V> domains) {
		final Object[] objTab = domains.toArray(new Object[domains.size()]);
		this.choice.setTheWholeItems(objTab);
	}

	private void init(final String name) {
		setLayout(new BorderLayout());
		this.agrTypeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.agrTypeLabel.setText(name);
		this.agrTypeLabel.setPreferredSize(new Dimension(86, 24));
		this.agrTypeLabel.setMaximumSize(new Dimension(86, 24));
		this.agrTypeLabel.setMinimumSize(new Dimension(86, 24));
		this.choice.setPreferredSize(new Dimension(149, 24));

		final Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(this.agrTypeLabel, null);
		box.add(Box.createRigidArea(new Dimension(5, 0)));
		box.add(this.choice);

		add(box, BorderLayout.CENTER);
	}

	public void setLabelSize(final Dimension dim) {
		this.agrTypeLabel.setPreferredSize(dim);
		this.agrTypeLabel.setMaximumSize(dim);
		this.agrTypeLabel.setMinimumSize(dim);
	}

	public void setCombolSize(final Dimension dim) {
		this.choice.setPreferredSize(dim);
		this.choice.setMaximumSize(dim);
		this.choice.setMinimumSize(dim);
	}

	@SuppressWarnings("unchecked")
	public V getValue() {
		return (V) this.choice.getSelectedItem();
	}

	public FilterComboBox getChoice() {
		return choice;
	}

	public void setValue(final V value) {
		this.choice.setSelectedItem(value);
	}

	@SuppressWarnings("unchecked")
	public void setValue(final ReportTemplate reportTemplate, final String key) {
		setValue(null);
		if (this.map == null) {
			final V value = (V) reportTemplate.get(key);
			if (value != null) {
				setValue(value);
			}
		} else {
			final K keyMap = (K) reportTemplate.get(key);
			if (keyMap != null) {
				setValue(this.map.get(keyMap));
			}
		}
	}

	public void setEditable(final boolean editable) {
		this.choice.setEditable(editable);
	}
}
