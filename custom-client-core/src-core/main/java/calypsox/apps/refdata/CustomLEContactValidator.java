/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.refdata;

import java.awt.Frame;
import java.util.Vector;

import com.calypso.apps.refdata.LEContactValidator;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.refdata.LEContact;

public class CustomLEContactValidator implements LEContactValidator {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isValidInput(LEContact leContact, Frame frame, Vector messages) {
	     java.awt.Component[] c = frame.getComponents();
	    
		if ((leContact != null)
				&& ("Settlement".equals(leContact.getContactType()) && (!"Agent".equals(leContact.getLegalEntityRole()))
						&& !AppUtil.displayQuestion(
								"Are you sure you want to save current contact with Contact Type=’Settlement’ and Role="+leContact.getLegalEntityRole() + "?",
								frame))) {
			return false;
		}

		return true;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean isValidRemove(LEContact leContact, Frame frame, Vector messages) {
		return true;
	}

}
