/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview.property;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.CellEditor;
import javax.swing.JButton;
import javax.swing.JTextField;

import com.calypso.apps.jide.grid.CalypsoJIDEProperty;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.tk.core.Util;
import com.jidesoft.combobox.AbstractComboBox;
import com.jidesoft.combobox.PopupPanel;
import com.jidesoft.converter.ConverterContext;
import com.jidesoft.converter.ObjectConverter;
import com.jidesoft.converter.ObjectConverterManager;
import com.jidesoft.grid.AbstractComboBoxCellEditor;
import com.jidesoft.grid.CellEditorFactory;
import com.jidesoft.grid.CellEditorManager;
import com.jidesoft.grid.EditorContext;
import com.jidesoft.grid.Property;

@SuppressWarnings("deprecation")
public class HaircutHelper {

	public static final EditorContext EDITOR_CONTEXT = new EditorContext("HaircutMethod");

	public static final ConverterContext CONVERTER_CONTEXT = new ConverterContext("HaircutMethod");

	public static final String[] VALUES = { ">=", "<=" };

	private String haircutType = null;

	public HaircutHelper(String haircutType) {
		this.haircutType = haircutType;
	}

	public Property getHaircutProperty() {
		Property property = null;
		CellEditorManager.registerEditor(HaircutMethod.class, new HaircutMethodCellEditorFactory(),
				HaircutHelper.EDITOR_CONTEXT);
		ObjectConverterManager.registerConverter(HaircutMethod.class, new HaircutMethodConverter(),
				HaircutHelper.CONVERTER_CONTEXT);
		property = new CalypsoJIDEProperty(this.haircutType, "", HaircutMethod.class);

		property.setCategory(this.haircutType);
		property.setConverterContext(HaircutHelper.CONVERTER_CONTEXT);
		property.setEditorContext(HaircutHelper.EDITOR_CONTEXT);
		return property;

	}

	private class HaircutMethodConverter implements ObjectConverter {

		public HaircutMethodConverter() {
		}

		@Override
		public Object fromString(String string, ConverterContext context) {
			HaircutMethod result = new HaircutMethod();
			if (Util.isEmpty(string)) {
				return result;
			}
			//
			// int greaterThan = string.indexOf(VALUES[0]);
			// if(greaterThan == -1){
			// String maxAmountStr = string.substring((VALUES[1]).length()+1;
			// }
			//
			// String amountString = string.substring(result.getLowDirection().length() + 1);
			// double amount = Util.stringToNumber(amountString);
			//
			// result.setLowDirection(result.getLowDirection());
			// if (!"".equals(result.getLowDirection())) {
			// result.setLowAmount(amount);
			// }
			return result;
		}

		@Override
		public boolean supportFromString(String string, ConverterContext context) {
			return true;
		}

		@Override
		public boolean supportToString(Object object, ConverterContext context) {
			return true;
		}

		@Override
		public String toString(Object object, ConverterContext context) {
			String result = "";

			if ((object != null) && (object instanceof HaircutMethod)) {
				HaircutMethod method = (HaircutMethod) object;
				result = method.toString();
			}

			return result;
		}
	}

	private class HaircutMethodComboBox extends AbstractComboBox {

		private static final long serialVersionUID = 2991780435497447663L;

		public HaircutMethodComboBox() {
			initComponent();
			setType(HaircutMethod.class);
			setPopupVolatile(true);
		}

		@Override
		public EditorComponent createEditorComponent() {
			return new DefaultTextFieldEditorComponent(String.class);
		}

		@Override
		public PopupPanel createPopupComponent() {
			return new HaircutMethodDefinitionPanel(this);
		}

	}

	private class HaircutMethodDefinitionPanel extends PopupPanel {

		private static final long serialVersionUID = 2411578304025300586L;

		private final HaircutMethodComboBox methodComboBox;

		private CalypsoComboBox lowHaircutComboBox;
		private JTextField lowAmountTextField;

		private CalypsoComboBox upHaircutComboBox;
		private JTextField upAmountTextField;

		private JButton setButton;

		public HaircutMethodDefinitionPanel(HaircutMethodComboBox methodComboBox) {
			this.methodComboBox = methodComboBox;
			setTitle("Define Haircut");
			initComponent();
			setLowHaircutMethod();
			setUpHaircutMethod();
		}

		private void initComponent() {
			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(getLowComboBox());
			add(getLowAmountTextField());
			add(getUpComboBox());
			add(getUpAmountTextField());

			add(getSetButton());
			setDefaultFocusComponent(getLowAmountTextField());
		}

		private void setLowHaircutMethod() {
			HaircutMethod method = (HaircutMethod) this.methodComboBox.getSelectedItem();
			if (method != null) {
				getLowComboBox().setSelectedItem(VALUES[0]);
				getLowAmountTextField().setText(Util.numberToString(method.getMinAmount()));
			}
		}

		private void setUpHaircutMethod() {
			HaircutMethod method = (HaircutMethod) this.methodComboBox.getSelectedItem();
			if (method != null) {
				getUpComboBox().setSelectedItem(VALUES[1]);
				getUpAmountTextField().setText(Util.numberToString(method.getMaxAmount()));
			}
		}

		private CalypsoComboBox getLowComboBox() {
			if (this.lowHaircutComboBox == null) {
				this.lowHaircutComboBox = new CalypsoComboBox();
				List<String> list = new ArrayList<String>();
				list.add(VALUES[0]);
				AppUtil.set(this.lowHaircutComboBox, list);
			}
			return this.lowHaircutComboBox;
		}

		private CalypsoComboBox getUpComboBox() {
			if (this.upHaircutComboBox == null) {
				this.upHaircutComboBox = new CalypsoComboBox();
				List<String> list = new ArrayList<String>();
				list.add(VALUES[1]);
				AppUtil.set(this.upHaircutComboBox, list);
			}
			return this.upHaircutComboBox;
		}

		private JTextField getLowAmountTextField() {
			if (this.lowAmountTextField == null) {
				this.lowAmountTextField = new JTextField();
				this.lowAmountTextField.setColumns(8);
				AppUtil.addNumberListener(this.lowAmountTextField, 4);
			}
			return this.lowAmountTextField;
		}

		private JTextField getUpAmountTextField() {
			if (this.upAmountTextField == null) {
				this.upAmountTextField = new JTextField();
				this.upAmountTextField.setColumns(8);
				AppUtil.addNumberListener(this.upAmountTextField, 4);
			}
			return this.upAmountTextField;
		}

		private JButton getSetButton() {
			if (this.setButton == null) {
				this.setButton = new JButton("Set");
				this.setButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setSelectedObject(buildHaircutMethod());
						hidePopup();
					}
				});
			}
			return this.setButton;
		}

		private HaircutMethod buildHaircutMethod() {
			HaircutMethod method = new HaircutMethod();

			double amount;
			try {
				amount = Util.stringToNumber(getLowAmountTextField().getText());
				method.setMinAmount(amount);
			} catch (NumberFormatException e) {
				method.setMinAmount(0.0);
			}

			try {
				amount = Util.stringToNumber(getUpAmountTextField().getText());
				method.setMaxAmount(amount);
			} catch (NumberFormatException e) {
				method.setMaxAmount(0.0);
			}

			return method;
		}

		private void hidePopup() {
			if (this.methodComboBox != null) {
				this.methodComboBox.hidePopup();
			}
		}
	}

	private class HaircutMethodCellEditorFactory implements CellEditorFactory {

		@Override
		public CellEditor create() {
			return new HaircutMethodCellEditor();
		}

	}

	private class HaircutMethodCellEditor extends AbstractComboBoxCellEditor {

		private static final long serialVersionUID = 1L;

		@Override
		public AbstractComboBox createAbstractComboBox() {
			HaircutMethodComboBox comboBox = new HaircutMethodComboBox();
			// comboBox.setEditable(false);
			return comboBox;
		}

	}

}
