package calypsox.tk.pricer;

import java.util.Iterator;
import java.util.Vector;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.pricer.PricerFromDB;
import com.calypso.tk.pricer.PricerInput;
import com.calypso.tk.pricer.PricerMeasureFromDB;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.PricerMeasureUtility;

public class SantPricerMeasureFromDBWithSuffix extends PricerMeasureFromDB {

	public void calculate(Trade trade, JDatetime valDatetime, PricingEnv env, Pricer pricer, PricerInput input)
			throws PricerException {
		String name = this.getRealMeasureName();
		if (Util.isEmpty(name)) {
			throw new PricerException("Cannot Retrieve name of Pricing Measure " + this.getType() + "/Trade "
					+ trade.getLongId() + " Name of PM is not set");
		} else {
			PricerMeasure[] am = new PricerMeasure[1];
			Vector<String> errors = new Vector<String>();
			am[0] = PricerMeasureUtility.makeMeasure(DSConnection.getDefault(), name, errors);

			if (Util.isEmpty(errors)) {
				if (pricer instanceof PricerFromDB) {
					pricer.price(trade, valDatetime, env, am);
				} else {
					this.pricerFromDB.price(trade, valDatetime, env, am);
				}
				this.setDisplayClass(am[0].getDisplayClass());
				this.setDisplayDigits(am[0].getDisplayDigits());
				this.setValue(am[0].getValue());
				this.setCurrency(am[0].getCurrency());
			} else {
				StringBuilder msg = new StringBuilder("Could not create pricer measure:");
				Iterator<String> arg9 = errors.iterator();
				while (arg9.hasNext()) {
					String error = (String) arg9.next();
					msg.append('\n');
					msg.append(error);
				}
				throw new PricerException(msg.toString());
			}
		}
	}

	protected String getRealMeasureName() {
		String name = this.getName();
		if (name.contains("_"))
			return name.substring(0, name.lastIndexOf('_'));
		else
			return name;

	}

	@Override
	public boolean isImplementedByPricer(Pricer pricer) {
		return true;
	}

}
