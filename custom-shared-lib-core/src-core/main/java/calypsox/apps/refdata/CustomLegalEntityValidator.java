/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.refdata;

import java.awt.Frame;
import java.util.Vector;

import com.calypso.apps.refdata.LegalEntityValidator;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

public class CustomLegalEntityValidator implements LegalEntityValidator {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isValidInput(LegalEntity legalEntity, Frame frame, Vector messages) {
		if ((legalEntity != null) && !Util.isEmpty(legalEntity.getRoleList())
				&& legalEntity.getRoleList().contains(LegalEntity.PROCESSINGORG)) {
			LegalEntityAttribute bookBundleAttr = CustomBookValidator.getLEBookBundleAttribute(BOCache
					.getLegalEntityAttributes(DSConnection.getDefault(), legalEntity.getId()));
			if (bookBundleAttr == null) {
				messages.addElement(LegalEntity.PROCESSINGORG + " legalEntities must have a "
						+ CustomBookValidator.BOOK_BUNDLE + " attribute defined\n");
				return false;
			}

		}
		return true;
	}
}
