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

public class SantChooserListProperty<T> extends DefaultProperty {

	private static final long serialVersionUID = 1L;

	private ConverterContext converterContext;
	private EditorContext editorContext;
	private ObjectConverter converter;
	private final Vector<T> allValues;
	private final JFrame frame;

	private SantChooserListCellEditor<T> cellEditor;

	public SantChooserListProperty(String name, String displayName, String category, Container owner,
			Vector<T> allValues) {
		this.allValues = allValues;
		this.frame = (JFrame) JideSwingUtilities.getFrame(owner);
		setName(name);
		setDisplayName(displayName);
		setCategory(category);

		makeConverterAndContexts();
		preparePropertyPanelCell();
		configureProperty();
	}

	private void configureProperty() {
		setType(Vector.class);
		setConverterContext(this.converterContext);
		setEditorContext(this.editorContext);
	}

	private void makeConverterAndContexts() {
		this.converterContext = new ConverterContext(getName());
		this.editorContext = new EditorContext(getName());
		this.converter = new SantChooserListConverter<T>();
	}

	public void preparePropertyPanelCell() {
		preparePropertyPanelCell(this.frame, this.allValues);
	}

	public void preparePropertyPanelCell(final JFrame frame, final Vector<T> types) {
		ObjectConverterManager.registerConverter(List.class, this.converter, this.converterContext);
		CellEditorManager.registerEditor(List.class, new CellEditorFactory() {
			@Override
			public CellEditor create() {
				return getSantChooserListCellEditor();
			}

		}, this.editorContext);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Vector<T> getValue() {
		return (Vector<T>) super.getValue();

	}

	private SantChooserListCellEditor<T> getSantChooserListCellEditor() {
		if (this.cellEditor == null) {
			this.cellEditor = new SantChooserListCellEditor<T>(this.frame, this.allValues);
		}
		return this.cellEditor;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setValue(Object obj) {
		super.setValue(obj);
		// Vector<T> v = (Vector<T>) obj;
		// if (v == null) {
		// v = new Vector<T>();
		// }
		getSantChooserListCellEditor().setSelectedValues((Vector<T>) obj);
	}
}