package calypsox.tk.refdata;

import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.HedgeRelationship;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class SantCAAgentRelatedFailedTrasnferStaticDataFilter implements StaticDataFilterInterface {

	/** TreeList Parent reference for custom PARENT_CUSTOM_REFERENCE. */
	private static final String PARENT_CUSTOM_REFERENCE = "Custom";

	/** CA_AGENT. */
	private static final String CA_AGENT = "CA Agent Related Failed Transfer";

	@Override
	public boolean fillTreeList(DSConnection con, TreeList tl) {
		Vector<String> nodes = new Vector<>();
		nodes.add(PARENT_CUSTOM_REFERENCE);
		tl.add(nodes, CA_AGENT);
		return false;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getDomain(DSConnection con, String attributeName) {
		Vector vect = new Vector();
		Vector<LegalEntity> leList = BOCache.getLegalEntitiesForRole(con, "Agent");
		for (LegalEntity le : leList) {
			vect.add(le.getCode());
		}
		return vect;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void getDomainValues(DSConnection con, Vector v) {
		v.add(CA_AGENT);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {
		Vector<String> v = new Vector<String>();
		if (attributeName.equals(CA_AGENT)) {
			v.addElement(StaticDataFilterElement.S_IN);
			v.addElement(StaticDataFilterElement.S_NOT_IN);
		}
		return v;
	}

	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {
		String result = "";
		if (trade != null) {
			String idsFailed = trade.getKeywordValue("CAFailedTransfer");
			if (!Util.isEmpty(idsFailed)) {
				String[] strIds = idsFailed.split(",");
				for (String strId : strIds) {
					try {
						long id = Long.valueOf(strId);
						BOTransfer transferOrig = DSConnection.getDefault().getRemoteBO().getBOTransfer(id);
						if (transferOrig != null) {
							LegalEntity entity = BOCache.getLegalEntity(DSConnection.getDefault(),
									transferOrig.getInternalAgentId());
							if (Util.isEmpty(result) && entity != null) {
								result = entity.getCode();
							} else if (entity == null || !result.equals(entity.getCode())) {
								return "";
							}
						}
					} catch (CalypsoServiceException | NumberFormatException e) {
						Log.error(this, "Unable to obtain Id transfer: " + idsFailed + " or its Agent");
					}
				}
			}
		}
		return result;
	}

	@Override
	public boolean isTradeNeeded(String attributeName) {
		return true;
	}

}
