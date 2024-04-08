/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview.property;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;

import com.calypso.apps.product.ProductChooserWindow;
import com.calypso.tk.core.Util;
import com.jidesoft.swing.NullPanel;

public class ProductListCellEditor extends DefaultCellEditor implements TableCellEditor {

	private static final long serialVersionUID = 1L;

	private JTextField editorField;
	private final JFrame owner;
	private final Vector<String> types;
	private List<Integer> securityIds;

	public ProductListCellEditor(JFrame owner, Vector<String> types) {
		super(new JTextField());
		this.owner = owner;
		this.types = types;
	}

	@Override
	public Object getCellEditorValue() {
		if (this.securityIds != null) {
			return this.securityIds;
		}
		return Util.string2IntVector(this.editorField.getText());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		NullPanel nullPanel = new NullPanel(new BorderLayout());
		this.editorField = (JTextField) this.editorComponent;
		this.editorField.setText(value == null ? "" : Util.collectionToString((List<Integer>) value));
		nullPanel.add(this.editorField, BorderLayout.CENTER);
		nullPanel.add(new JButton(new ProductChooserAction(this.owner)), BorderLayout.EAST);

		return nullPanel;

	}

	class ProductChooserAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		private final JFrame owner;

		public ProductChooserAction(JFrame owner) {
			super("...");
			this.owner = owner;
			setEnabled(true);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				ProductChooserWindow win = new ProductChooserWindow(this.owner, ProductListCellEditor.this.types, true);
				ProductListCellEditor.this.securityIds = win.getSelectedIds();
				win.setVisible(true);
			} finally {
				stopCellEditing();
			}
		}
	}

	class ProductChooserButtonBorder implements Border {
		private JButton button;//Sonar
		private int buttonWidth;

		ProductChooserButtonBorder(JButton button) {
			this.button = button;
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			this.button.setBounds(width - this.buttonWidth, 0, this.buttonWidth, height);
			((Container) c).add(this.button);
		}

		@Override
		public Insets getBorderInsets(Component c) {
			this.buttonWidth = this.button.getPreferredSize().width;
			return new Insets(0, 0, 0, 20);
		}

		@Override
		public boolean isBorderOpaque() {
			return true;
		}
	}
}
