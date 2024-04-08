package calypsox.tk.bo.workflow.rule;

import java.util.Optional;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Repo;
import com.calypso.tk.service.DSConnection;

public class SLCheckMCConfigTradeRule implements WfTradeRule {
	private final String KW_MARGIN_CALL_CONFIG_ID = "MARGIN_CALL_CONFIG_ID";
	private final String COLLATERAL_EXCLUDE_KWD="CollateralExclude";
	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
						 Vector excps, Task task, Object dbCon, Vector events) {
		if (trade == null || isTripartyNoGcPooling(trade) || isInternalDeal(trade) || isNotCollateralizable(trade)) {
			return true;
		}

		int contractId = trade.getKeywordAsInt(KW_MARGIN_CALL_CONFIG_ID);
		return contractId > 0;
	}

	@Override
	public String getDescription() {

		return "Check if the Trade has an assigned Margin Call Contract";
	}

	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
						  Vector excps, Task task, Object dbCon, Vector events) {
		if(isNotCollateralizable(trade)){
			trade.addKeyword(KW_MARGIN_CALL_CONFIG_ID,"");
		}
		return true;
	}

	public boolean isNotCollateralizable(Trade trade){
		return Optional.ofNullable(trade).map(t->t.getKeywordValue(COLLATERAL_EXCLUDE_KWD))
				.map(Boolean::parseBoolean).orElse(false);
	}
	public boolean isInternalDeal(Trade trade) {
		LegalEntity le = trade.getCounterParty();
		if(le==null)
			return false;
		if (le.equals(trade.getBook().getLegalEntity()))
			return true;
		else
			return trade.getMirrorBook() != null
					&& trade.getMirrorBook().getLegalEntity().equals(le);
	}

	//Condition linked to UploadCalypsoTradeRepo:
	//SCIBCALLAC-3938 & SCIBCALLAC-3963: Do not add collateral info on triparty trades nor gc pooling
	//Thus this rule should not check collateral info on triparty repos that have not been assigned collateral info on their upload
	public boolean isTripartyNoGcPooling(Trade trade) {
		boolean ret=false;
		if(trade.getProduct() instanceof Repo){
			Repo repo = (Repo) trade.getProduct();
			String isGCPooling = trade.getKeywordValue("isGCPooling");
			if (repo.isTriparty() && (Util.isEmpty(isGCPooling) || "false".equalsIgnoreCase(isGCPooling) || trade.getMirrorBook() != null)) {
				ret=true;
			}
		}
		return ret;
	}
}
