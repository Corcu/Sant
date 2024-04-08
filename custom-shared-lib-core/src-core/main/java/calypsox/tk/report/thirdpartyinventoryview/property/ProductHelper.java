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
public class ProductHelper {

	public static final EditorContext EDITOR_CONTEXT = new EditorContext("CreditRatingMethod");

	public static final ConverterContext CONVERTER_CONTEXT = new ConverterContext("CreditRatingMethod");

	public static final Collection<?> VALUES = Util.string2Vector("<=,>=");

	private String rate = null;

	public ProductHelper(String rate) {
		this.rate = rate;
	}

	public Property getProductProperty() {
		Property property = null;
		CellEditorManager.registerEditor(Rate.class, new RateCellEditorFactory(), ProductHelper.EDITOR_CONTEXT);
		ObjectConverterManager.registerConverter(Rate.class, new RateConverter(), ProductHelper.CONVERTER_CONTEXT);
		property = new CalypsoJIDEProperty(this.rate, "", Rate.class);

		property.setCategory(this.rate);
		property.setConverterContext(ProductHelper.CONVERTER_CONTEXT);
		property.setEditorContext(ProductHelper.EDITOR_CONTEXT);
		return property;
	}

	private class RateConverter implements ObjectConverter {

		public RateConverter() {
		}

		@Override
		public Object fromString(String string, ConverterContext context) {
			if (!Util.isEmpty(string)) {
				Rate result = new Rate();
				BigDecimal rating = new BigDecimal(string.substring(result.getSign().length() + 1));
				result.setSign(result.getSign());
				result.setRate(rating);
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

			if ((object != null) && (object instanceof Rate)) {
				Rate method = (Rate) object;
				result = method.toString();
			}

			return result;
		}
	}

	private class RateComboBox extends AbstractComboBox {

		private static final long serialVersionUID = 2991780435497447663L;

		public RateComboBox() {
			initComponent();
			setType(Rate.class);
			setPopupVolatile(true);
		}

		@Override
		public EditorComponent createEditorComponent() {
			return new DefaultTextFieldEditorComponent(String.class);
		}

		@Override
		public PopupPanel createPopupComponent() {
			return new RateDefinitionPanel(this);
		}

	}

	private class RateDefinitionPanel extends PopupPanel {

		private static final long serialVersionUID = 2411578304025300586L;

		private final RateComboBox rateComboBox;

		private CalypsoComboBox signComboBox;
		private JTextField rateTextField;

		private JButton setButton;

		public RateDefinitionPanel(RateComboBox rateComboBox) {
			this.rateComboBox = rateComboBox;
			setTitle("Define Stock Lending Rate");
			initComponent();
			setRate();
		}

		private void initComponent() {
			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(getSignComboBox());
			add(getRateTextField());
			add(getSetButton());
			setDefaultFocusComponent(getRateTextField());
		}

		private void setRate() {
			Rate rate = (Rate) this.rateComboBox.getSelectedItem();
			if (rate != null) {
				getSignComboBox().setSelectedItem(rate.getSign());
				this.rateTextField.setText(rate.getRate().toString());
			}
		}

		private CalypsoComboBox getSignComboBox() {
			if (this.signComboBox == null) {
				this.signComboBox = new CalypsoComboBox();
				AppUtil.set(this.signComboBox, ProductHelper.VALUES);
			}
			return this.signComboBox;
		}

		private JTextField getRateTextField() {
			if (this.rateTextField == null) {
				this.rateTextField = new JTextField();
				this.rateTextField.setEditable(true);
				this.rateTextField.setColumns(8);
				// AppUtil.addNumberListener(this.rateTextField, 4);
			}
			return this.rateTextField;
		}

		private JButton getSetButton() {
			if (this.setButton == null) {
				this.setButton = new JButton("Set");
				this.setButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setSelectedObject(buildRate());
						hidePopup();
					}
				});
			}
			return this.setButton;
		}

		private Rate buildRate() {
			Rate rate = new Rate();
			rate.setSign((String) getSignComboBox().getSelectedItem());
			try {
				rate.setRate(new BigDecimal(getRateTextField().getText()));
			} catch (NumberFormatException nfe) {
				Log.warn(this, nfe.getMessage());
				return null;
			}

			return rate;
		}

		private void hidePopup() {
			if (this.rateComboBox != null) {
				this.rateComboBox.hidePopup();
			}
		}
	}

	private class RateCellEditorFactory implements CellEditorFactory {

		@Override
		public CellEditor create() {
			return new RateCellEditor();
		}

	}

	private class RateCellEditor extends AbstractComboBoxCellEditor {

		private static final long serialVersionUID = 1L;

		@Override
		public AbstractComboBox createAbstractComboBox() {
			RateComboBox comboBox = new RateComboBox();
			return comboBox;
		}
	}

}
