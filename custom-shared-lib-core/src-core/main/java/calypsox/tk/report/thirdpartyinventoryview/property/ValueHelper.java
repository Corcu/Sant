/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.thirdpartyinventoryview.property;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.Collection;

import javax.swing.CellEditor;
import javax.swing.JButton;
import javax.swing.JTextField;

import com.calypso.apps.jide.grid.CalypsoJIDEProperty;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.apps.util.CalypsoLabel;
import com.calypso.tk.core.Log;
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
public class ValueHelper {

	public static final EditorContext EDITOR_CONTEXT = new EditorContext("CreditRatingMethod");

	public static final ConverterContext CONVERTER_CONTEXT = new ConverterContext("CreditRatingMethod");

	public static final Collection<?> SIGN_VALUES = Util.string2Vector("<=,>=");

	public static final Collection<?> NAME_VALUES = Util.string2Vector("Quantity,Nominal");

	private final String value;

	public ValueHelper(String value) {
		this.value = value;
	}

	public Property getValueProperty() {
		Property property = null;
		CellEditorManager.registerEditor(Value.class, new ValueCellEditorFactory(), ProductHelper.EDITOR_CONTEXT);
		ObjectConverterManager.registerConverter(Value.class, new ValueConverter(), ProductHelper.CONVERTER_CONTEXT);
		property = new CalypsoJIDEProperty(this.value, "", Value.class);

		property.setCategory(this.value);
		property.setConverterContext(ValueHelper.CONVERTER_CONTEXT);
		property.setEditorContext(ProductHelper.EDITOR_CONTEXT);
		return property;
	}

	private class ValueConverter implements ObjectConverter {

		public ValueConverter() {
		}

		@Override
		public Object fromString(String string, ConverterContext context) {
			if (!Util.isEmpty(string)) {
				Value result = new Value();
				BigDecimal value = new BigDecimal(string.substring(result.getSign().length() + 1));
				result.setName(result.getName());
				result.setSign(result.getSign());
				result.setValue(value);

				String numberOfDays = string.substring(string.indexOf("in") + 3, string.indexOf("days") - 1);
				result.setDays(Integer.parseInt(numberOfDays));
				return result;
			} else {
				return null;
			}
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

			if ((object != null) && (object instanceof Value)) {
				Value value = (Value) object;
				result = value.toString();
			}

			return result;
		}
	}

	private class ValueComboBox extends AbstractComboBox {

		private static final long serialVersionUID = 2991780435497447663L;

		public ValueComboBox() {
			initComponent();
			setType(Value.class);
			setPopupVolatile(true);
		}

		@Override
		public EditorComponent createEditorComponent() {
			return new DefaultTextFieldEditorComponent(String.class);
		}

		@Override
		public PopupPanel createPopupComponent() {
			return new ValueDefinitionPanel(this);
		}

	}

	private class ValueDefinitionPanel extends PopupPanel {

		private static final long serialVersionUID = 2411578304025300586L;

		private final ValueComboBox valueComboBox;

		private CalypsoComboBox nameComboBox;
		private CalypsoComboBox signComboBox;
		private JTextField valueTextField;
		private CalypsoLabel daysLabel;
		private JTextField daysTextField;

		private JButton setButton;

		public ValueDefinitionPanel(ValueComboBox valueComboBox) {
			this.valueComboBox = valueComboBox;
			setTitle("Define Position value");
			initComponent();
			setValue();
		}

		private void initComponent() {
			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(getNameComboBox());
			add(getSignComboBox());
			add(getValueTextField());
			add(getDaysLabel());
			add(getDaysTextField());
			add(getSetButton());
			setDefaultFocusComponent(getValueTextField());
		}

		private void setValue() {
			Value value = (Value) this.valueComboBox.getSelectedItem();
			if (value != null) {
				getNameComboBox().setSelectedItem(value.getName());
				getSignComboBox().setSelectedItem(value.getSign());
				this.daysTextField.setText(Integer.toString(value.getDays()));
				this.valueTextField.setText(value.getValue().toString());
			}
		}

		private CalypsoComboBox getSignComboBox() {
			if (this.signComboBox == null) {
				this.signComboBox = new CalypsoComboBox();
				AppUtil.set(this.signComboBox, ValueHelper.SIGN_VALUES);
			}
			return this.signComboBox;
		}

		private CalypsoComboBox getNameComboBox() {
			if (this.nameComboBox == null) {
				this.nameComboBox = new CalypsoComboBox();
				AppUtil.set(this.nameComboBox, ValueHelper.NAME_VALUES);
			}
			return this.nameComboBox;
		}

		private JTextField getValueTextField() {
			if (this.valueTextField == null) {
				this.valueTextField = new JTextField();
				this.valueTextField.setEditable(true);
				this.valueTextField.setColumns(8);
				// AppUtil.addNumberListener(this.rateTextField, 4);
			}
			return this.valueTextField;
		}

		private JTextField getDaysTextField() {
			if (this.daysTextField == null) {
				this.daysTextField = new JTextField();
				this.daysTextField.setEditable(true);
				this.daysTextField.setColumns(8);
				// AppUtil.addNumberListener(this.rateTextField, 4);
			}
			return this.daysTextField;
		}

		private CalypsoLabel getDaysLabel() {
			if (this.daysLabel == null) {
				this.daysLabel = new CalypsoLabel("DAYS IN POSITION");
			}
			return this.daysLabel;
		}

		private JButton getSetButton() {
			if (this.setButton == null) {
				this.setButton = new JButton("Set");
				this.setButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setSelectedObject(buildValue());
						hidePopup();
					}
				});
			}
			return this.setButton;
		}

		private Value buildValue() {
			Value value = new Value();
			value.setSign((String) getSignComboBox().getSelectedItem());
			try {
				value.setValue(new BigDecimal(getValueTextField().getText()));
				value.setName((String) getNameComboBox().getSelectedItem());
				value.setDays(Integer.parseInt(getDaysTextField().getText()));
			} catch (NumberFormatException nfe) {
				Log.info(this, nfe.getMessage());
				return null;
			}

			return value;
		}

		private void hidePopup() {
			if (this.valueComboBox != null) {
				this.valueComboBox.hidePopup();
			}
		}
	}

	private class ValueCellEditorFactory implements CellEditorFactory {

		@Override
		public CellEditor create() {
			return new ValueCellEditor();
		}

	}

	private class ValueCellEditor extends AbstractComboBoxCellEditor {

		private static final long serialVersionUID = 1L;

		@Override
		public AbstractComboBox createAbstractComboBox() {
			ValueComboBox comboBox = new ValueComboBox();
			return comboBox;
		}
	}

}
