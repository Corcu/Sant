/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.product;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import com.calypso.tk.core.Product;
import com.calypso.tk.core.ProductDescriptionGenerator;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class BondProductDescriptionGenerator implements ProductDescriptionGenerator {
	
	private  static final String LOCALE_FOR_BOND_QUOTES = "LocaleForBondQuotes";

	@Override
	public String getDescription(Product product) {
		Bond bond = (Bond) product;

		String desc = bond.calcDescription();
		if (desc.startsWith("Bond")) {
			desc = desc.substring(4);
		}
		desc = getPrefix(bond) + desc;

		return desc;
	}

	@Override
	public String getSubtype(Product product) {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getSubtypes(Product product) {
		return new Vector<>();
	}

	@Override
	public String getQuoteName(Product product) {
		Bond bond = (Bond) product;

		String matDate;
		if (bond.getMaturityDate() != null) {
			matDate = Util.idateToString(bond.getMaturityDate());
		} else {
			matDate = Util.idateToString(bond.getIssueDate());
		}
		String rate;
		if (bond.isWIBond()) {
			rate = Bond.WI;
		} else {
			rate = bond.getRateIndex() != null ? bond.getRateIndex().getName()
					: rateToString(bond.getCoupon(), 5);
		}
		if (bond.isRIBond()) {
			rate += "." + Bond.RI;
		}

		if (bond.isExotic()) {
			rate = "exotic";
			if (bond.getConfigurableType() != null) {
				rate = bond.getConfigurableType().getConfigurableTypeName();
			}
		}
		
		return getPrefix(bond) + bond.getName() + "." + matDate + "." + rate;
	}
	
	/**
	 * Apply decimal format to a rate depending on locale stored at the Domain Value LocaleForBondQuotes
	 * 
	 * @param rate
	 * @param numberOfdec
	 * @return
	 */
	private String rateToString(final double rate, final int numberOfdec){
		List<String> names = LocalCache.getDomainValues(DSConnection.getDefault(), LOCALE_FOR_BOND_QUOTES);
		String name = "";
		if(!Util.isEmpty(names)){
			name = names.get(0);
		}
		if(!Util.isEmpty(name)){
			Locale locale = Util.getLocale(name);
			DecimalFormatSymbols df = new DecimalFormatSymbols(locale);
			String pattern = "0.";
			for (int i = 0; i < numberOfdec; ++i){
				pattern = new StringBuilder().append(pattern).append("0").toString();
			}
			DecimalFormat format = new DecimalFormat(pattern, df);
			synchronized (format) {
				return format.format(rate * 100.0D);
			}
		}
	return "";
	}

	private String getPrefix(Bond bond) {
		return "Bond.ISIN_" + bond.getSecCode("ISIN") + "_" + bond.getCurrency() + ".";
	}

	@Override
	public String getExtendedType(Product p) {
		return "None";
	}

	@Override
	public Vector<?> getExtendedTypes(Product arg0) {
		return new Vector<>();
	}
}
