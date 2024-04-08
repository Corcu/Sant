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

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.Util;
import com.jidesoft.swing.NullPanel;

public class SantChooserListCellEditor<T> extends DefaultCellEditor implements
		TableCellEditor {

	private static final long serialVersionUID = 1L;

	private JTextField editorField;
	private final JFrame owner;
	private Vector<T> selectedValues = new Vector<T>();

	private final Vector<T> allValues;

	public SantChooserListCellEditor(JFrame owner, Vector<T> allValues) {
		super(new JTextField());
		this.owner = owner;
		this.allValues = allValues;
	}

	@Override
	public Object getCellEditorValue() {
		return this.selectedValues;
	}

	public void setSelectedValues(Vector<T> selectedValues) {
		this.selectedValues = selectedValues;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		NullPanel nullPanel = new NullPanel(new BorderLayout());
		this.editorField = (JTextField) this.editorComponent;
		this.editorField.setEditable(false);
		this.editorField.setText(value == null ? "" : Util
				.collectionToString((List<String>) value));
		nullPanel.add(this.editorField, BorderLayout.CENTER);
		nullPanel.add(new JButton(new ChooserListAction(this.owner)),
				BorderLayout.EAST);

		return nullPanel;
	}

	class ChooserListAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		private final JFrame owner;

		public ChooserListAction(JFrame owner) {
			super("...");
			this.owner = owner;
			setEnabled(true);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				// MIGRATION V14.4 18/01/2015
				SantChooserListCellEditor.this.selectedValues = AppUtil
						.chooseList(this.owner,
								SantChooserListCellEditor.this.allValues,
								SantChooserListCellEditor.this.selectedValues,
								false, null, "",
								SantChooserListCellEditor.this.allValues
										.size(), true, true, true);
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
		public void paintBorder(Component c, Graphics g, int x, int y,
				int width, int height) {
			this.button.setBounds(width - this.buttonWidth, 0,
					this.buttonWidth, height);
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
