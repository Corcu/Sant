/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.jaxb.xml.bridge.identifiers;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.jaxb.bridge.Codification;
import com.calypso.jaxb.xml.Identifier;
import com.calypso.jaxb.xml.Identifiers;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

/**
 * An implementation of an Identifiers translator for Bond, In fact, the bond will be identified by it's secCodes (ISIN)
 * 
 * @author aela
 * 
 */
public class ProductIdentifiersTranslatorV2 extends
		com.calypso.jaxb.xml.bridge.identifiers.ProductIdentifiersTranslatorV2 {

	//
	private static final String BOND_SEC_CODE_KEY = "bondSecCodeKey";

	@Override
	@SuppressWarnings("unchecked")
	public Identifiers translate(Product bond) throws ErrorMessage {
		System.out.println("*****BondImportInterface : Translator used calypsox ProductIdentifiersTranslatorV2");
		if (!(bond instanceof Bond)) {
			return super.translate(bond);
		}
		Identifiers ids = super.translate(bond);
		// translate the secCode part
		Map<String, String> secCodes = bond.getSecCodes();
		if (secCodes == null) {
			return ids;
		}

		Iterator<String> secCodeIter = secCodes.keySet().iterator();
		String codeName = null;
		while (secCodeIter.hasNext()) {
			codeName = secCodeIter.next();
			if (!isKeyCode(codeName)) {
				continue;
			}
			Identifier localIdentifier = translateAsTradeIdentifier(Codification.getCodifier(codeName),
					Codification.getCode(secCodes.get(codeName)));

			if ((!isDuplicateIdentifier(ids.getIdentifier(), localIdentifier))) {
				ids.getIdentifier().add(localIdentifier);
			}
		}
		return ids;
	}

	// @Override
	// public GDIdentifiers translateAsGDIdentifiers(Product arg0) throws ErrorMessage {
	// // TODO Auto-generated method stub
	// return super.translateAsGDIdentifiers(arg0);
	// }

	/**
	 * @param codeName
	 * @return true if the given secCode must be used to identify the bond
	 */
	private boolean isKeyCode(String codeName) {
		Vector<String> bondSecCodes = LocalCache.getDomainValues(DSConnection.getDefault(), BOND_SEC_CODE_KEY);
		if (!Util.isEmpty(bondSecCodes)) {
			for (String secCode : bondSecCodes) {
				if (secCode.equals(codeName)) {
					return true;
				}
			}
		}
		return false;
	}
}
