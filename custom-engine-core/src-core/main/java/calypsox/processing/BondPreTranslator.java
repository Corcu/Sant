package calypsox.processing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Vector;

import com.calypso.jaxb.xml.Bond;
import com.calypso.jaxb.xml.BondDefinition;
import com.calypso.jaxb.xml.BondMMDiscount;
import com.calypso.jaxb.xml.Identifier;
import com.calypso.jaxb.xml.Object;
import com.calypso.processing.PreTranslateProcessor;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.processing.error.MessageType;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class BondPreTranslator implements PreTranslateProcessor {

	private Vector<String> ccyList = new Vector<String>();
	private static final String REGEXP_FORBIDEN_CHAR_IN_BOND_NAME = "[<>&'\")|]";

	@Override
	public void process(final Object jaxbObject) throws ErrorMessage {
         if (jaxbObject instanceof Bond) {  
        	BondDefinition definition = null;
        	if (jaxbObject instanceof BondMMDiscount) {
        		definition = ((BondMMDiscount) jaxbObject).getMmDiscountDefinition();
        	}
        	else if (jaxbObject instanceof Bond) {
        		definition = ((Bond) jaxbObject).getBondDefinition();
        	}
        	if (definition == null) {
        		throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION, "Invalid Object type: " + jaxbObject.getClass().toString());
        	}
			
			// delete special characters from bond name
			String bondName = definition.getName();

			if (!Util.isEmpty(bondName)) {
				definition.setName(bondName.replaceAll(REGEXP_FORBIDEN_CHAR_IN_BOND_NAME, ""));
			}

			// check and map the country code
			String countryCode = definition.getCountry();
			Country country = BOCache.getCountryByISO(DSConnection.getDefault(), countryCode);
			if (country == null) {
				throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION, "Invalid country code "
						+ (countryCode == null ? "null" : countryCode));
			} else {
				definition.setCountry(country.getName());
			}
			// check the issuer name
			try {
				List<Identifier> isserIdentifiers = definition.getIssuer().getIdentifier();

				for (Identifier id : isserIdentifiers) {
					LegalEntity isser = BOCache.getLegalEntity(DSConnection.getDefault(), id.getCode());
					if (isser == null) {
						throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION, "Invalid Issuer code "
								+ (id.getCode() == null ? "null" : id.getCode()));
					}
				}
			} catch (Exception e) {
				throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION,
						"Cannot get the Issuer from the source file");
			}

			// check the inflation index
			try {
				if (definition.getInflationIndex() != null) {
					List<Identifier> inflationIdentifiers = definition.getInflationIndex().getIdentifier();

					for (Identifier id : inflationIdentifiers) {
						String indexCode = id.getCode();
						if (!Util.isEmpty(indexCode)) {
							indexCode = indexCode.replace("#", ".");
							RateIndex rate = LocalCache.getRateIndex(DSConnection.getDefault(), indexCode);
							if (rate == null) {
								throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION,
										"Invalid RateIndex code: " + (id.getCode() == null ? "empty" : id.getCode()));
							}

						}
					}

				}
			} catch (Exception e) {
				throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION,
						"Cannot get the Inflation index from the source file");
			}

			// check the bond currencies
			String cpCcy = definition.getCouponCurrency();
			if (!isValidCurrency(cpCcy)) {
				throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION, "Invalid coupon currency: "
						+ (Util.isEmpty(cpCcy) ? "empty" : cpCcy));
			}

			// check the bond couponFrequency
			String cpFreq = definition.getCouponFrequency();
			if (Util.isEmpty(cpFreq)) {
				throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION, "Invalid coupon frequency: "
						+ (Util.isEmpty(cpFreq) ? "empty" : cpFreq));
			}

			String redemptionCcy = definition.getRedemptionCurrency();
			if (!isValidCurrency(redemptionCcy)) {
				throw new ErrorMessage(jaxbObject, MessageType.INVALID_TRANSLATION, "Invalid redemption currency: "
						+ (Util.isEmpty(redemptionCcy) ? "empty" : redemptionCcy));
			}

			// divide the bond coupon rate and spead by 100
			if (definition.getCoupon() != null) {
				BigDecimal couponToSave = new BigDecimal(Util.rateToNumber(definition.getCoupon()
						.doubleValue()));
				definition.setCoupon(couponToSave);
			}

			if (definition.getSpread() != null) {
				BigDecimal spreadToSave = new BigDecimal(Util.spreadToNumber(definition.getSpread()
						.doubleValue()));
				definition.setSpread(spreadToSave);
			}
		}
	}

	private boolean isValidCurrency(String ccy) {
		if (Util.isEmpty(ccy)) {
			return false;
		}

		if (Util.isEmpty(this.ccyList)) {
			this.ccyList = LocalCache.getCurrencies();
		}
		return this.ccyList.contains(ccy);
	}
}
