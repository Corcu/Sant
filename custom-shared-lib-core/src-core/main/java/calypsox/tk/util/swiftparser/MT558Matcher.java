package calypsox.tk.util.swiftparser;

import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.DisplayValue;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import calypsox.tk.collateral.util.SantMarginCallUtil;

public class MT558Matcher extends com.calypso.tk.util.swiftparser.MT558Matcher {
	private static final String TRIPARTY_TRANSACTION_GROUP_REFERENCE = "TripartyTransactionGroupReference";
	private static final String ATTRIBUTE_TRIPARTY_AGREED_AMOUNT = "Triparty Agreed Amount";

	@Override
	public boolean match(ExternalMessage extMsg, Object object, BOMessage indexedMessage, BOTransfer indexedTransfer,
			PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {

		boolean status = super.match(extMsg, object, indexedMessage, indexedTransfer, env, ds, dbCon, errors);

		if (!status) {
			cleanErrors(errors);
			try {
				MarginCallEntryDTO entry = SantMarginCallUtil.getMarginCallEntryDTO(indexedMessage, ds);
				if (entry != null) {
					return compare(entry, extMsg, errors);
				}
				errors.add("Cannot find Entry");
			} catch (Exception e) {
				Log.error(this, "Cannot load Margin Call Entries Error: " + e);
			}
		}
		return status;
	}

	public static int decodeInteger(String integer) {
		if (!Util.isEmpty(integer)) {
			return Integer.parseInt(integer, 36);
		}
		return 0;
	}

	// Compare swift message amount with entry.Attribute Triparty Agreed Amount
	public boolean compare(MarginCallEntryDTO entry, ExternalMessage extMsg, Vector errors) {
		if (entry != null && extMsg != null) {
			Double entryAmount = getEntryAmount(entry);
			Double messageAmount = getMessageAmount(extMsg);
			if (!entryAmount.equals(0.0) && entryAmount.equals(messageAmount)) {
				return true;
			}
			errors.add("Triparty Agreed Amount " + entryAmount + " Not equal to Message TRAA Amount " + messageAmount);
		}
		return false;
	}

	private void cleanErrors(Vector errors) {
		for (int i = 0; i < errors.size(); i++) {
			if (errors.get(i).toString().contains("Exsposure")) {
				errors.remove(i);
			}
		}
	}

	public Double getMessageAmount(ExternalMessage extMsg) {
		if (extMsg != null && extMsg instanceof SwiftMessage) {
			SwiftMessage mess = (SwiftMessage) extMsg;
			try {
				DisplayValue value = mess.getDisplayAmount("Money Amount");

				if (value != null) {
					return Math.abs(value.get());
				}
			} catch (MessageParseException e) {
				Log.error(this, "Cannot get Money Amount from Swift Message " + " Error: " + e);
			}
		}
		return 0.0;
	}

	public Double getEntryAmount(MarginCallEntryDTO dto) {
		if (dto != null) {
			Object rawAttribute = dto.getAttribute(ATTRIBUTE_TRIPARTY_AGREED_AMOUNT);
			Double tripartyAgreedAmount = 0.0;
			if (rawAttribute instanceof Double) {
				tripartyAgreedAmount = (Double) rawAttribute;
			} else if (rawAttribute instanceof String) {
				try {
					tripartyAgreedAmount = Double.valueOf(rawAttribute.toString());
				} catch (NumberFormatException e) {
					Log.error(this, "Cannot convert attribute Triparty Agreed Amount Error: " + e);
				}
			}

			if (tripartyAgreedAmount != null && !tripartyAgreedAmount.equals(0.0)) {
				return Math.abs(tripartyAgreedAmount);
			}
		}
		return 0.0;
	}

}
