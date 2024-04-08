package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.util.SantDomainValuesUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CreArray;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

public class SLReverseCreFeeAccrualTradeRule implements WfTradeRule {
	private static final String S_TRUE = "true";
	private static final String DV_REVERSE_ALL_ACTIVATION = "SLReverseCreFeeAccrualMICReverseALL";
	
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
    	return true;
    }

    @Override
    public String getDescription() {
        return "Reverse FEE_ACCRUAL Cre in case of Partenon change";
    }

	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		if (trade == null) {
			return true;
		}
		
		boolean isReverseALLActivated = SantDomainValuesUtil.getBooleanDV(DV_REVERSE_ALL_ACTIVATION);
		
		String partenonKW = trade.getKeywordValue("PartenonRequest");

		//Add && remove partenon keyword
		if(S_TRUE.equalsIgnoreCase(partenonKW)){
			String partenonAccountingID = trade.getKeywordValue("PartenonAccountingID");
			trade.addKeyword("OldPartenonAccountingID",partenonAccountingID);
			trade.addKeyword("PartenonAccountingID","");
			trade.addKeyword("PartenonRequest","");
		}

		if (!Util.isEmpty(partenonKW) && partenonKW.equalsIgnoreCase(S_TRUE) || trade.getAction().equals(Action.CANCEL)) {
			CreArray cresNEW = null;
			CreArray cresREVERSAL = null;
			try {
				String whereClause = buildWhereClause(trade, BOCre.NEW);
				cresNEW = DSConnection.getDefault().getRemoteBO().getBOCres(null, whereClause, null);
				
				whereClause = buildWhereClause(trade, BOCre.REVERSAL);
				cresREVERSAL = DSConnection.getDefault().getRemoteBO().getBOCres(null, whereClause, null);
			} catch (CalypsoServiceException e) {
				Log.error(this, "Could not retrieve CREs from Trade: " + e.toString());
				return false;
			}
			
			if (cresNEW == null || cresNEW.isEmpty()) {
				return true;
			}
			
			for (int i = cresNEW.size() - 1; i >= 0; i--) {
				BOCre creNEW = cresNEW.get(i);
				
				for (int j = 0; j < cresREVERSAL.size(); j++) {
					BOCre creREVERSAL = cresREVERSAL.get(j);
					
					if (creREVERSAL.getLinkedId() == creNEW.getId()) {
						cresNEW.remove(i);
						break;
					}
				}
			}
			
			if (cresNEW.isEmpty()) {
				return true;
			}
			
			CreArray reversalCres = new CreArray();
			for (int i = cresNEW.size() - 1; i >= 0; i--) {
				BOCre cre = cresNEW.get(i);
				try {
					BOCre reversalCre = (BOCre) cre.clone();
					reversalCre.setId(0L);
					reversalCre.setLinkedId(cre.getId());
					reversalCre.setCreType(BOCre.REVERSAL);
					reversalCre.setSentDate(null);
					reversalCre.setStatus(BOCre.NEW);
					reversalCre.setSentStatus(null);
					
					if (trade.getAction().equals(Action.CANCEL)) {
						reversalCre.setOriginalEventType(BOCreConstantes.CANCELED_TRADE_EVENT);
					}
					
					Hashtable ht = reversalCre.getAttributes();
					if (ht == null) {
						ht = new Hashtable();
					}
					ht.put("createdIn", "SLReverseCreFeeAccrualTradeRule");
					reversalCre.setAttributes(ht);

					reversalCres.add(reversalCre);
					
					if (!isReverseALLActivated) {
						break;
					}
				} catch (CloneNotSupportedException e) {
					Log.error(this, "Could not clone CRE: " + e.toString());
				}
			}
			if (reversalCres.size() > 0) {
				try {
					DSConnection.getDefault().getRemoteBO().saveCres(reversalCres);
				} catch (CalypsoServiceException e) {
					Log.error(this, "Could not save Reversal CREs: " + e.toString());
				}
			}

			//Publish the events FB.
			Arrays.stream(reversalCres.getCres()).forEach(this::publishEvents);

		}
		
		return true;
	}

	public String buildWhereClause(Trade trade, String creType) {
		StringBuilder sb = new StringBuilder();
		sb.append(" bo_cre_type = '");
		sb.append(BOCreConstantes.FEE_ACCRUAL);
		sb.append("'");
		sb.append(" AND trade_id = ");
		sb.append(trade.getLongId());
		sb.append(" AND cre_type = '");
		sb.append(creType);
		sb.append("'");
		return sb.toString();
	}

	/** Publish event for CreOnlineSenderEngine
	 * @param cre
	 */
	private void publishEvents(BOCre cre){
		PSEventCre creEvent = new PSEventCre();
		creEvent.setBoCre(cre);
		try {
			DSConnection.getDefault().getRemoteTrade().saveAndPublish(creEvent);
		} catch (CalypsoServiceException e) {
			Log.error(this,"Error publish the event: " + e);
		}
	}

}
