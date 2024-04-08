/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting.util.control;

import com.calypso.apps.util.AppUtil;

public class SantDatePanel extends SantTextFieldPanel {

	private static final long serialVersionUID = 1L;

	public SantDatePanel(String name) {
		super(name);
		AppUtil.addDateListener(this.field);
	}

	// public JDate getDateValue() {
	// return this.field.getText();
	// }
	//
	// public void setDateValue(final JDate value) {
	// this.field.setText(value);
	// }
	//

}
