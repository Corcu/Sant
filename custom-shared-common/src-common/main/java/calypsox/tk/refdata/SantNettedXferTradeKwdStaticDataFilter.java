package calypsox.tk.refdata;

import calypsox.tk.bo.fiflow.builder.handler.FIFlowTransferNetHandler;
import com.calypso.apps.util.TreeList;
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
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

/**
 *
 */

public class SantNettedXferTradeKwdStaticDataFilter implements StaticDataFilterInterface{

	private static final String WF_SUBTYPE_KWD="WorkflowSubType";
	public static final String NETTED_XFER_KEYWORD = "NettedXferTradeKeyword."+WF_SUBTYPE_KWD;


	/**
	 * Inserts the SDFilter name
	 */
	@Override
	public void getDomainValues(DSConnection con, Vector v) {
		v.add(NETTED_XFER_KEYWORD);
	}


	@Override
	public boolean fillTreeList(DSConnection con, TreeList tl) {
		Vector<String> nodes = new Vector<>();
		nodes.add("SantTradeKeyword");
		tl.add(nodes, NETTED_XFER_KEYWORD);
		return false;
	}

	// USER CRITERIA OPTIONS:

	/**
	 * Option type of the filter. This is the criteria section in SDF window.
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {
		Vector<String> v = new Vector<>(1);
		if (attributeName.equals(NETTED_XFER_KEYWORD)) {
			v.addElement(StaticDataFilterElement.S_IS);
			v.addElement(StaticDataFilterElement.S_LIKE);
			v.addElement(StaticDataFilterElement.S_IN);
			v.addElement(StaticDataFilterElement.S_NOT_LIKE);
			v.addElement(StaticDataFilterElement.S_NOT_IN);
		}

		return v;
	}

	@Override
	public Vector getDomain(DSConnection con, String attributeName) {
		return null;
	}

	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {
		return Optional.ofNullable(trade).map(t->t.getKeywordValue(WF_SUBTYPE_KWD)).orElse(getTradeKeywordFromNettedTransfer(transfer));
	}

	private String getTradeKeywordFromNettedTransfer(BOTransfer xfer){
		FIFlowTransferNetHandler netHandler=new FIFlowTransferNetHandler(xfer);
		long firstXferLongId=Optional.ofNullable(netHandler.getFirstUndTransfer()).map(BOTransfer::getTradeLongId)
				.orElse(0L);
		return getTradeKeywordFromLongId(firstXferLongId);
	}

	private String getTradeKeywordFromLongId(long tradeId){
		String kwdValue="";
		if(tradeId>0L){
			List<String> kwdNames=new ArrayList<>();
			kwdNames.add(WF_SUBTYPE_KWD);
			try {
				Map<String,String> kwds=DSConnection.getDefault().getRemoteTrade().getTradeKeywords(tradeId,kwdNames);
				kwdValue=Optional.ofNullable(kwds).map(kwdMap->kwdMap.get(WF_SUBTYPE_KWD)).orElse("");
			} catch (CalypsoServiceException exc) {
				Log.error(this.getClass().getSimpleName(),exc.getMessage(),exc.getCause());
			}
		}
		return kwdValue;
	}

	@Override
	public boolean isTradeNeeded(String arg) {
		return false;
	}

}
