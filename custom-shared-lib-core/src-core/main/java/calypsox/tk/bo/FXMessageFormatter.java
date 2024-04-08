package calypsox.tk.bo;

import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.MimeType;
import com.calypso.tk.service.DSConnection;

public class FXMessageFormatter extends MessageFormatter {
	@SuppressWarnings("rawtypes")
	public String parseTEST(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
			BOTransfer transfer, DSConnection dsCon) {

		return "Prueba";
	}

	@Override
	public AdviceDocument generate(final PricingEnv env, final BOMessage message, final boolean newDocument,
			final DSConnection dsCon) throws MessageFormatException {

		final AdviceDocument ad = super.generate(env, message, newDocument, dsCon);
		final MimeType mt = new MimeType("text/csv");
		mt.setExtension("csv");
		ad.setMimeType(mt);
		return ad;
	}
}
