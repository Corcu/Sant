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
import java.util.Collection;
import java.util.Vector;

import javax.swing.CellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;



import com.calypso.apps.jide.grid.CalypsoJIDEProperty;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
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
public class CreditRatingHelper {

	public static final String EXISTS = "Exists";

	public static final EditorContext EDITOR_CONTEXT = new EditorContext("CreditRatingMethod");

	public static final ConverterContext CONVERTER_CONTEXT = new ConverterContext("CreditRatingMethod");

	public static final Collection<?> SIGN_VALUES = Util.string2Vector("<=,>=,=");
	public static final Collection<?> SIGN_VALUES_WITH_EXISTS = Util.string2Vector("<=,>=,=," + EXISTS);
	// public static final Collection<?> SIGN_VALUES_STRICTLY_SC = Util.string2Vector("=," + EXISTS);
	public static final Collection<?> HIGHEST_LOWEST_VALUES = Util.string2Vector(",HIGHEST,LOWEST");
	public static final Collection<?> ISSUE_ISSUER_VALUES = Util.string2Vector("Issue,Issuer");

	private String creditRatingType = null;
	private final String ratingAgency;
	private final boolean isStrict;
	private boolean isInternal = false;

	public CreditRatingHelper(String creditRatingType, String ratingAgencyName, boolean isStrict) {
		this.creditRatingType = creditRatingType;
		this.ratingAgency = ratingAgencyName;
		this.isStrict = isStrict;
	}

	public CreditRatingHelper(String creditRatingType, String ratingAgencyName, boolean isStrict, boolean isInternal) {
		this(creditRatingType, ratingAgencyName, isStrict);
		this.isInternal = isInternal;
	}

	public Property getCreditRatingProperty() {
		Property property = null;

		CellEditorManager.registerEditor(CreditRatingMethod.class, new CreditRatingMethodCellEditorFactory(),
				CreditRatingHelper.EDITOR_CONTEXT);

		ObjectConverterManager.registerConverter(CreditRatingMethod.class, new CreditRatingMethodConverter(),
				CreditRatingHelper.CONVERTER_CONTEXT);
		property = new CalypsoJIDEProperty(this.creditRatingType, this.ratingAgency, CreditRatingMethod.class);

		property.setValue(buildEmptyCreditRatingMethod(this.ratingAgency, this.isStrict, this.isInternal));

		property.setCategory(this.creditRatingType);
		property.setConverterContext(CreditRatingHelper.CONVERTER_CONTEXT);
		property.setEditorContext(CreditRatingHelper.EDITOR_CONTEXT);
		return property;

	}

	private CreditRatingMethod buildEmptyCreditRatingMethod(String ratingAgency, boolean isStrict, boolean isInternal) {
		CreditRatingMethod method = new CreditRatingMethod();
		method.setRatingAgency(ratingAgency);
		method.setStrict(isStrict);
		// method.setInternal(isInternal);
		return method;
	}

	private class CreditRatingMethodConverter implements ObjectConverter {

		public CreditRatingMethodConverter() {
		}

		@Override
		public Object fromString(String string, ConverterContext context) {
			CreditRatingMethod result = new CreditRatingMethod();

			String rating = string.substring(result.getSign().length() + 1);
			result.setSign(result.getSign());
			result.setCreditRating(rating);
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

			if ((object != null) && (object instanceof CreditRatingMethod)) {
				CreditRatingMethod method = (CreditRatingMethod) object;
				result = method.toString();
			}

			return result;
		}
	}

	private class CreditRatingMethodComboBox extends AbstractComboBox {

		private static final long serialVersionUID = 2991780435497447663L;

		public CreditRatingMethodComboBox() {
			initComponent();
			setType(CreditRatingMethod.class);
			setPopupVolatile(true);
		}

		@Override
		public EditorComponent createEditorComponent() {
			return new DefaultTextFieldEditorComponent(String.class);
		}

		@Override
		public PopupPanel createPopupComponent() {
			return new CreditRatingMethodDefinitionPanel(this);
		}

	}

	private class CreditRatingMethodDefinitionPanel extends PopupPanel {

		private static final long serialVersionUID = 2411578304025300586L;

		private final CreditRatingMethodComboBox methodComboBox;

		private CalypsoComboBox issueOrIssuerComboBox;
		private CalypsoComboBox highestOrLowestComboBox;
		private CalypsoComboBox signComboBox;
		private CalypsoComboBox creditRatingComboBox;
		private JButton setButton;

		private boolean isStrict = false;//Sonar
		@SuppressWarnings("unused")
		private boolean isInternal = false;//Sonar
		private String agency = null;//Sonar

		public CreditRatingMethodDefinitionPanel(CreditRatingMethodComboBox methodComboBox) {
			this.methodComboBox = methodComboBox;
			setTitle("Define Credit Rating");
			initComponent();
			setCreditRatingMethod();
		}

		private void initComponent() {
			setLayout(new FlowLayout(FlowLayout.LEFT));
			CalypsoComboBox highestOrLowestCombo = getHighestOrLowestComboBox();
			add(highestOrLowestCombo);
			add(getIssueOrIssuerComboBox());
			add(getSignComboBox());
			add(getCreditRatingComboBox());
			add(getSetButton());
		}

		private void setCreditRatingMethod() {
			CreditRatingMethod method = (CreditRatingMethod) this.methodComboBox.getSelectedItem();
			if (method != null) {
				getSignComboBox().setSelectedItem(method.getSign());
				getCreditRatingComboBox().setSelectedItem(method.getCreditRating());
				getIssueOrIssuerComboBox().setSelectedItem(method.getIssueOrIssuer());
				getHighestOrLowestComboBox().setSelectedItem(method.getHighestOrLowest());
				if (method.isStrict()) {
					getHighestOrLowestComboBox().setVisible(false);
				}
			}
		}

		private CalypsoComboBox getHighestOrLowestComboBox() {
			if (this.highestOrLowestComboBox == null) {
				this.highestOrLowestComboBox = new CalypsoComboBox();
				AppUtil.set(this.highestOrLowestComboBox, CreditRatingHelper.HIGHEST_LOWEST_VALUES);
			}
			return this.highestOrLowestComboBox;
		}

		private CalypsoComboBox getIssueOrIssuerComboBox() {
			if (this.issueOrIssuerComboBox == null) {
				this.issueOrIssuerComboBox = new CalypsoComboBox();
				AppUtil.set(this.issueOrIssuerComboBox, CreditRatingHelper.ISSUE_ISSUER_VALUES);
			}
			return this.issueOrIssuerComboBox;
		}

		private CalypsoComboBox getSignComboBox() {

			if (this.signComboBox == null) {
				this.signComboBox = new CalypsoComboBox();
				AppUtil.set(this.signComboBox, CreditRatingHelper.SIGN_VALUES);
			}

			CreditRatingMethod method = (CreditRatingMethod) this.methodComboBox.getSelectedItem();

			// if (method.isInternal()) {
			// AppUtil.set(this.signComboBox, CreditRatingHelper.SIGN_VALUES_STRICTLY_SC);
			// } else
			if (method.isStrict()) {
				AppUtil.set(this.signComboBox, CreditRatingHelper.SIGN_VALUES_WITH_EXISTS);
			}
			return this.signComboBox;
		}

		@SuppressWarnings("unchecked")
		private JComboBox<?> getCreditRatingComboBox() {
			if (this.creditRatingComboBox == null) {
				this.creditRatingComboBox = new CalypsoComboBox();
				// Object[] theWholeItems = null;
				// String selectedCreditRating = "";
				try {
					CreditRatingMethod method = (CreditRatingMethod) this.methodComboBox.getSelectedItem();
					this.agency = method.getRatingAgency();
					// this.isInternal = method.isInternal();
					this.isStrict = method.isStrict();

					Vector<Object> v = DSConnection.getDefault().getRemoteReferenceData().getRatingValues()
							.getRatingValues(method.getRatingAgency(), "Current");
					if (!Util.isEmpty(v)) {
						v.add(0, "");
						AppUtil.set(this.creditRatingComboBox, v);
					}
				} catch (Exception e) {
					Log.error(this,e);//Sonar
				}

				// this.creditRatingComboBox.setTheWholeItems(theWholeItems);
				// this.creditRatingComboBox.setSelectedItem(selectedCreditRating);
			}
			return this.creditRatingComboBox;
		}

		private JButton getSetButton() {
			if (this.setButton == null) {
				this.setButton = new JButton("Set");
				this.setButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setSelectedObject(buildCreditRatingMethod());
						hidePopup();
					}
				});
			}
			return this.setButton;
		}

		private CreditRatingMethod buildCreditRatingMethod() {
			CreditRatingMethod method = new CreditRatingMethod();
			method.setHighestOrLowest((String) getHighestOrLowestComboBox().getSelectedItem());
			method.setIssueOrIssuer((String) getIssueOrIssuerComboBox().getSelectedItem());
			method.setSign((String) getSignComboBox().getSelectedItem());
			// method.setSign((String) getSignComboBox().getSelectedItem());
			method.setCreditRating((String) getCreditRatingComboBox().getSelectedItem());
			method.setRatingAgency(this.agency);
			// if (CollateralStaticAttributes.SC.equals(method.getRatingAgency())) {
			// method.setInternal(true);
			// }
			method.setStrict(this.isStrict);
			return method;
		}

		private void hidePopup() {
			if (this.methodComboBox != null) {
				this.methodComboBox.hidePopup();
			}
		}
	}

	private class CreditRatingMethodCellEditorFactory implements CellEditorFactory {

		CreditRatingMethodCellEditorFactory() {

		}

		@Override
		public CellEditor create() {
			// df
			return new CreditRatingMethodCellEditor();
		}
	}

	private class CreditRatingMethodCellEditor extends AbstractComboBoxCellEditor {

		private static final long serialVersionUID = 1L;

		@Override
		public AbstractComboBox createAbstractComboBox() {
			CreditRatingMethodComboBox comboBox = new CreditRatingMethodComboBox();
			comboBox.setEditable(false);
			return comboBox;
		}
	}

}
