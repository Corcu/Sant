/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.product;

import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.ProductDescriptionGenerator;
import com.calypso.tk.product.Equity;

public class EquityProductDescriptionGenerator implements ProductDescriptionGenerator {

	@Override
	public String getDescription(Product product) {
		return getCustomName((Equity) product);
	}

	@Override
	public String getSubtype(Product product) {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getSubtypes(Product product) {
		return null;
	}

	@Override
	public String getQuoteName(Product product) {
		return getCustomName((Equity) product);

	}

	private String getCustomName(Equity equity) {
		StringBuilder sb = new StringBuilder();
		sb.append(equity.getType());
		sb.append(".ISIN_");
		sb.append(equity.getSecCode("ISIN"));
		sb.append("_");
		sb.append(equity.getCurrency());
		sb.append("_");
		if (!(null == equity.getMarketPlace())) {
			sb.append(equity.getMarketPlace().getAuthName());
		}
		else {
			sb.append("NONE");
		}
		
		return sb.toString();
	}

	@Override
	public String getExtendedType(Product arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector<?> getExtendedTypes(Product arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
