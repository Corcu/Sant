/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview.property;

import java.awt.Container;
import java.util.List;
import java.util.Vector;

import javax.swing.CellEditor;
import javax.swing.JFrame;

import com.jidesoft.converter.ConverterContext;
import com.jidesoft.converter.ObjectConverter;
import com.jidesoft.converter.ObjectConverterManager;
import com.jidesoft.grid.CellEditorFactory;
import com.jidesoft.grid.CellEditorManager;
import com.jidesoft.grid.DefaultProperty;
import com.jidesoft.grid.EditorContext;
import com.jidesoft.swing.JideSwingUtilities;

public class ProductChooserListProperty extends DefaultProperty {

	private static final long serialVersionUID = 1L;

	private ConverterContext converterContext;
	private EditorContext editorContext;
	private ObjectConverter converter;
	private final Vector<String> types;

	public ProductChooserListProperty(String name, String displayName, String category, Container owner,
			Vector<String> types) {
		this.types = types;
		setName(name);
		setDisplayName(displayName);
		setCategory(category);

		makeConverterAndContexts();
		preparePropertyPanelCell((JFrame) JideSwingUtilities.getFrame(owner));

		configureProperty();
	}

	private void configureProperty() {
		setType(List.class);
		setConverterContext(this.converterContext);
		setEditorContext(this.editorContext);
	}

	private void makeConverterAndContexts() {
		this.converterContext = new ConverterContext("ProductList");
		this.editorContext = new EditorContext("ProductList");
		this.converter = new ProductListConverter();
	}

	public void preparePropertyPanelCell(final JFrame frame) {
		preparePropertyPanelCell(frame, this.types);
	}

	public void preparePropertyPanelCell(final JFrame frame, final Vector<String> types) {
		ObjectConverterManager.registerConverter(List.class, this.converter, this.converterContext);
		CellEditorManager.registerEditor(List.class, new CellEditorFactory() {
			@Override
			public CellEditor create() {
				return new ProductListCellEditor(frame, types);
			}
		}, this.editorContext);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getValue() {
		return (List<String>) super.getValue();

	}
}