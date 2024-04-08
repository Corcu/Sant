package calypsox.tk.upload.validator;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.mapping.core.UploaderContextProvider;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.util.TradeArray;

public class ValidatorUtil {
	
	public static String EXISTING_TRADE_WHERE_CLAUSE = "trade.external_reference= ? and product_desc.product_type = ? and trade.trade_status NOT IN (" + ioSQL.string2SQLString("CANCELED") + "," + ioSQL.string2SQLString("TERMINATED") +")"; 
	public static String EXISTING_TRADE_WHERE_CLAUSE_TERMINATED = "trade.external_reference= ? and product_desc.product_type = ? and trade.trade_status NOT IN (" + ioSQL.string2SQLString("CANCELED") + ")";

	public static Trade getExistingTrade(CalypsoTrade calypsoTrade, Vector<BOException> errors) {
		return getExistingTrade(calypsoTrade, false, errors);
	}
	
	
	public static Trade getExistingTrade(CalypsoTrade calypsoTrade, boolean includeTerminate, Vector<BOException> errors) {
		return getExistingTrade(calypsoTrade, includeTerminate, null, errors);
	}

	public static Trade getExistingTrade(CalypsoTrade calypsoTrade, boolean includeTerminate, String forceSQL, Vector<BOException> errors) {
		Trade trade = null;
		String externalRef = calypsoTrade.getExternalReference();
		
		if(externalRef==null)
			return null;

		String exp = "TradesByExternalRefAndProduct" + externalRef;

		TradeArray ta = (TradeArray) UploaderContextProvider.getAttributeValue(exp);
		if (ta == null) {
			try {
				CalypsoBindVariable externalRefBindVariable = new CalypsoBindVariable(CalypsoBindVariable.VARCHAR,externalRef);
				CalypsoBindVariable productTypeBindVariable = new CalypsoBindVariable(CalypsoBindVariable.VARCHAR,calypsoTrade.getProductType());
				List<CalypsoBindVariable> bindVariables = Arrays.asList(externalRefBindVariable,productTypeBindVariable);
				if (Util.isEmpty(forceSQL)) {
					ta = DSConnection.getDefault().getRemoteTrade().getTrades2(null, includeTerminate?EXISTING_TRADE_WHERE_CLAUSE_TERMINATED:EXISTING_TRADE_WHERE_CLAUSE,null,0,true,bindVariables );
				}
				else {
					ta = DSConnection.getDefault().getRemoteTrade().getTrades2(null, forceSQL, null, 0, true, bindVariables );
				}
				UploaderContextProvider.addAttributeValue(exp, ta);
			} catch (CalypsoServiceException e) {
				Log.error("UPLOADER", e);
			}

		}
		try {
			if (ta != null && ta.size() == 1) {
				trade = ta.get(0);
			} else if (ta != null && ta.size() > 1) {
				Log.warn("UPLOADER", "More than one Trade found for external ref: " + externalRef);
				trade = getAllocationParent(ta);
				if (trade == null) {
					if (ta.size() == 2) {
						trade = checkMirrorTrade(ta);
					} else if (ta.size() == 3) {
						trade = checkBackToBackTrade(ta);
						if (trade == null) {
							trade = checkLinkedTrades(ta, "Spot_Transfer_To");
						}
					} else if (ta.size() == 4) {
						trade = checkSpotLinkedTrades(ta, "SalesB2BTo");
						if (trade == null) {
							trade = checkSpotLinkedTrades(ta, "Spot_Transfer_To");
						}
					}

				}
				
				if (trade == null) {
					errors.add(ErrorExceptionUtils.createException("21001", "Trade Action", "10017", calypsoTrade.getAction(), 0L));
					return null;
				}
			}

		} catch (Exception arg6) {
			Log.error("UPLOADER", externalRef);
		}

		return trade;

	}

	private static Trade checkMirrorTrade(TradeArray ta) throws Exception {
		Trade trade1 = ta.get(0);
		Trade trade2 = ta.get(1);
		if (trade1 != null && trade2 != null
				&& CalypsoIDAPIUtil.getMirrorTradeId(trade1) == CalypsoIDAPIUtil.getId(trade2)) {
			Log.debug("UPLOADER",
					"More than one Trade found for same External Reference. Mirror Trade exists for external ref: "
							+ trade1.getExternalReference() + ", tradeId: " + CalypsoIDAPIUtil.getId(trade1)
							+ ", Mirrortrade: " + CalypsoIDAPIUtil.getId(trade2));
			return CalypsoIDAPIUtil.getId(trade1) < CalypsoIDAPIUtil.getId(trade2) ? trade1 : trade2;
		} else {
			return null;
		}
	}

	private static Trade getAllocationParent(TradeArray ta) {
		if (ta != null && ta.size() > 1) {
			for (int i = 0; i < ta.size(); ++i) {
				Trade t = ta.get(i);
				if (t != null && (t.isAllocationParent() || "true".equalsIgnoreCase(t.getKeywordValue("IsRolledUp")))) {
					return t;
				}
			}
		}

		return null;
	}

	private static Trade checkBackToBackTrade(TradeArray ta) throws Exception {
		return checkLinkedTrades(ta, "SalesB2BTo");
	}

	private static Trade checkLinkedTrades(TradeArray ta, String keywordName) throws Exception {
		Trade tradeToReturn = null;
		Trade t1 = ta.get(0);
		Trade t2 = ta.get(1);
		Trade t3 = ta.get(2);
		if (CalypsoIDAPIUtil.getId(t1) < CalypsoIDAPIUtil.getId(t2)
				&& CalypsoIDAPIUtil.getId(t1) < CalypsoIDAPIUtil.getId(t3)) {
			tradeToReturn = t1;
		} else if (CalypsoIDAPIUtil.getId(t2) < CalypsoIDAPIUtil.getId(t1)
				&& CalypsoIDAPIUtil.getId(t2) < CalypsoIDAPIUtil.getId(t3)) {
			tradeToReturn = t2;
		} else if (CalypsoIDAPIUtil.getId(t3) < CalypsoIDAPIUtil.getId(t1)
				&& CalypsoIDAPIUtil.getId(t3) < CalypsoIDAPIUtil.getId(t2)) {
			tradeToReturn = t3;
		}

		if (tradeToReturn != null) {
			String keyVal = tradeToReturn.getKeywordValue(keywordName);
			if (!Util.isEmpty(keyVal)) {
				return tradeToReturn;
			}
		}

		return null;
	}

	private static Trade checkSpotLinkedTrades(TradeArray ta, String keywordName) throws Exception {
		Trade tradeToReturn = null;
		Trade t1 = ta.get(0);
		Trade t2 = ta.get(1);
		Trade t3 = ta.get(2);
		Trade t4 = ta.get(3);
		if (CalypsoIDAPIUtil.getId(t1) < CalypsoIDAPIUtil.getId(t2)
				&& CalypsoIDAPIUtil.getId(t1) < CalypsoIDAPIUtil.getId(t3)
				&& CalypsoIDAPIUtil.getId(t1) < CalypsoIDAPIUtil.getId(t4)) {
			tradeToReturn = t1;
		} else if (CalypsoIDAPIUtil.getId(t2) < CalypsoIDAPIUtil.getId(t1)
				&& CalypsoIDAPIUtil.getId(t2) < CalypsoIDAPIUtil.getId(t3)
				&& CalypsoIDAPIUtil.getId(t2) < CalypsoIDAPIUtil.getId(t4)) {
			tradeToReturn = t2;
		} else if (CalypsoIDAPIUtil.getId(t3) < CalypsoIDAPIUtil.getId(t1)
				&& CalypsoIDAPIUtil.getId(t3) < CalypsoIDAPIUtil.getId(t2)
				&& CalypsoIDAPIUtil.getId(t3) < CalypsoIDAPIUtil.getId(t4)) {
			tradeToReturn = t3;
		} else if (CalypsoIDAPIUtil.getId(t4) < CalypsoIDAPIUtil.getId(t1)
				&& CalypsoIDAPIUtil.getId(t4) < CalypsoIDAPIUtil.getId(t2)
				&& CalypsoIDAPIUtil.getId(t4) < CalypsoIDAPIUtil.getId(t1)) {
			tradeToReturn = t1;
		}

		if (tradeToReturn != null) {
			String keyVal = tradeToReturn.getKeywordValue(keywordName);
			if (!Util.isEmpty(keyVal)) {
				return tradeToReturn;
			}
		}

		return null;
	}

}
