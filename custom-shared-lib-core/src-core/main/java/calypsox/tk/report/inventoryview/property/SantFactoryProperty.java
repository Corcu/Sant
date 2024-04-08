/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview.property;

import java.awt.Container;
import java.util.Vector;

import com.jidesoft.grid.Property;

public class SantFactoryProperty {

	public static Property makeChooserListPorperty(String name, String displayName, String category, Container owner,
			Vector<String> allValues) {
		return new SantChooserListProperty<String>(name, displayName, category, owner, allValues);
	}

	public static Property makeProductChooserListPorperty(String name, String displayName, String category,
			Container owner, Vector<String> allValues) {
		return new ProductChooserListProperty("Securities", null, null, null, allValues);
	}

}
