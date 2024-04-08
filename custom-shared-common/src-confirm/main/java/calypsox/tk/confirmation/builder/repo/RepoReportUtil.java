package calypsox.tk.confirmation.builder.repo;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Vector;

public class RepoReportUtil {

	//FORMATS
	public static final String DATE_FORMAT = "yyyyMMdd";

	public final JDate getCallableOrProjectedDate(Repo repo, JDate valDate) {
		if (repo == null || valDate == null) {
			return null;
		}
		switch(repo.getMaturityType()) {
		case "EVERGREEN":
			return repo.getCallableDate() != null ? repo.getCallableDate() : repo.calcCallableDate(valDate);
			
		case "EXTENDABLE":
		case "TERM":
			return repo.getEndDate();
			
		case "OPEN":
			return valDate.addBusinessDays(1, Util.string2Vector("TARGET"));
			
		default:
			return null;
		}
	}

	public final CashFlowInterest findNextIntFlowByResetDate(CashFlowSet cfSet, JDate valDate) {
		CashFlowInterest nextFlow = null;
		for(CashFlow thisFlow:cfSet) {
			if(thisFlow instanceof CashFlowInterest) {
				CashFlowInterest thisFlowInt=(CashFlowInterest) thisFlow;
				if (thisFlowInt.getType().equals(CashFlow.INTEREST) && thisFlowInt.getResetDate().after(valDate)) {
					if (nextFlow == null) {
						nextFlow = thisFlowInt;
					} else if (thisFlowInt.getResetDate().before(nextFlow.getResetDate())) {
						nextFlow = thisFlowInt;
					}
				}
			}
		}
		return nextFlow;
	}

	public final CashFlowInterest findPreviousIntFlowByResetDate(CashFlowSet cfSet, JDate valDate) {
		CashFlowInterest previousFlow = null;
		for(CashFlow thisFlow:cfSet) {
			if(thisFlow instanceof CashFlowInterest) {
				CashFlowInterest thisFlowInt=(CashFlowInterest) thisFlow;
				if (thisFlowInt.getType().equals(CashFlow.INTEREST) &&
						(thisFlowInt.getResetDate().before(valDate)||thisFlowInt.getResetDate().equals(valDate))){
					if (previousFlow == null) {
						previousFlow = thisFlowInt;
					} else if (thisFlowInt.getResetDate().after(previousFlow.getResetDate())) {
						previousFlow = thisFlowInt;
					}
				}
			}
		}
		return previousFlow;
	}

	public static double getIndexbyQuoteSet(final JDate date, PricingEnv pricingEnv, Repo repo) {

		String quoteSetName = "OFFICIAL";
		if (pricingEnv != null) {
			quoteSetName = pricingEnv.getQuoteSetName();
		}

		if (repo != null) {
			RateIndex rateIndex = repo.getCash().getRateIndex();
			String rate = "MM." + rateIndex.getCurrency() + "." + rateIndex.getName() + "." + rateIndex.getTenor().toString() + "." + rateIndex.getSource();
			String clausule = "quote_name = '" + rate + "' AND quote_set_name ='" + quoteSetName + "'  AND trunc(quote_date) = to_date('" + date + "', 'dd/mm/yy')";
			Vector<QuoteValue> vQuotes;
			try {
				vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
				if ((vQuotes != null) && (vQuotes.size() > 0)) {
					return vQuotes.get(0).getClose();
				}
				return 0.00; // no encuentra rate para fecha
			} catch (final RemoteException e) {
				Log.error(CollateralUtilities.class, e);
				return 0.00;
			}

		}
		return 0.00;
	}

	public static JDate getNextBusinessDay(JDate valDate) {
		Holiday hol = Holiday.getCurrent();
		JDate nextBusinessDay = valDate.addDays(1);
		boolean isBusinessDay = hol.isBusinessDay(nextBusinessDay, Util.string2Vector("SYSTEM"));
		int i = 1;
		while(!isBusinessDay) {
			nextBusinessDay = nextBusinessDay.addDays(i);
			isBusinessDay = hol.isBusinessDay(nextBusinessDay, Util.string2Vector("SYSTEM"));
			i++;
		}
		return nextBusinessDay;
	}

}
