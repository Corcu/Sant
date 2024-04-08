/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.processing;

import com.calypso.jaxb.xml.Equity;
import com.calypso.processing.PreTranslateProcessor;

public class EquityPreTranslator implements PreTranslateProcessor {

	/**
	 * Pre translation process override. Includes Equities checks
	 * 
	 * @param jaxbObject
	 */
	@Override
	public void process(com.calypso.jaxb.xml.Object jaxbObject) {
		if (jaxbObject instanceof Equity) {

			@SuppressWarnings("unused")
			Equity equity = (Equity) jaxbObject;
			//
			// // check and map the country code
			// String countryCode = bond.getDefinition().getCountry();
			// Country country = BOCache.getCountryByISO(DSConnection.getDefault(), countryCode);
			// if (country == null) {
			// throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION, "Invalid country code "
			// + (countryCode == null ? "null" : countryCode));
			// } else {
			// bond.getDefinition().setCountry(country.getName());
			// }
			// // check the issuer name
			// try {
			// List<Identifier> isserIdentifiers = bond.getDefinition().getIssuer().getIdentifier();
			//
			// for (Identifier id : isserIdentifiers) {
			// LegalEntity isser = BOCache.getLegalEntity(DSConnection.getDefault(), id.getCode());
			// if (isser == null) {
			// throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION, "Invalid Issuer code "
			// + (id.getCode() == null ? "null" : id.getCode()));
			// }
			// }
			// } catch (Exception e) {
			// throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION,
			// "Cannot get the Issuer from the source file");
			// }
			//
			// // check the inflation index
			// try {
			// if (bond.getDefinition().getInflationIndex() != null) {
			// List<Identifier> inflationIdentifiers = bond.getDefinition().getInflationIndex().getIdentifier();
			//
			// for (Identifier id : inflationIdentifiers) {
			// String indexCode = id.getCode();
			// if (!Util.isEmpty(indexCode)) {
			// indexCode = indexCode.replace("#", ".");
			// RateIndex rate = LocalCache.getRateIndex(DSConnection.getDefault(), indexCode);
			// if (rate == null) {
			// throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION,
			// "Invalid RateIndex code " + (id.getCode() == null ? "null" : id.getCode()));
			// }
			//
			// }
			// }
			//
			// }
			// } catch (Exception e) {
			// throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION,
			// "Cannot get the Inflation index from the source file");
			// }
			//
			// // check the bond currencies
			// String cpCcy = bond.getDefinition().getCouponCurrency();
			// if (!isValidCurrency(cpCcy)) {
			// throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION, "Invalid coupon currency "
			// + (Util.isEmpty(cpCcy) ? "null" : cpCcy));
			// }
			//
			// String redemptionCcy = bond.getDefinition().getRedemptionCurrency();
			// if (!isValidCurrency(redemptionCcy)) {
			// throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION, "Invalid redemption currency "
			// + (Util.isEmpty(redemptionCcy) ? "null" : redemptionCcy));
			// }
			//
			// // divide the bond coupon rate and spead by 100
			// if (bond.getDefinition().getCoupon() != null) {
			// BigDecimal couponToSave = new BigDecimal(Util.rateToNumber(bond.getDefinition().getCoupon()
			// .doubleValue()));
			// bond.getDefinition().setCoupon(couponToSave);
			// }
			//
			// if (bond.getDefinition().getSpread() != null) {
			// BigDecimal spreadToSave = new BigDecimal(Util.spreadToNumber(bond.getDefinition().getSpread()
			// .doubleValue()));
			// bond.getDefinition().setSpread(spreadToSave);
			// }
		}
	}

}
