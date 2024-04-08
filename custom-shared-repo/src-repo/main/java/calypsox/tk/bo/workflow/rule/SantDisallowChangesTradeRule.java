package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.PARTENONMSGRepoMessageFormatter;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;

import java.util.Optional;

public class SantDisallowChangesTradeRule
extends SantDisallowCommonChangesTradeRule {

	protected String getRuleName() {
		return "SantDisallowChanges";
	}

	@Override
	protected boolean validatePARTENONMSGMessageFormatter(Trade trade) {
		//Check alias of PartenonID
		String partenonAliasKW = trade.getKeywordValue(KW_PARTENON_ALIAS);
		if(!Util.isEmpty(partenonAliasKW)){
			PARTENONMSGRepoMessageFormatter formatter = new PARTENONMSGRepoMessageFormatter();
			if (!partenonAliasKW.equals(formatter.getAlias(trade))) {
				Log.info(this, getRuleName() + " Partenon Alias has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
				return true;
			}
		}
		return false;
	}

	@Override
	protected void initValues(Trade trade, Trade oldTrade) {
		if(Optional.of(trade.getProduct()).filter(Repo.class::isInstance).isPresent()){ //Repo Check
			Repo newRepo = (Repo)trade.getProduct();
			Repo oldRepo = (Repo)oldTrade.getProduct();
			Collateral newCollateral = Optional.of(trade).map(Trade::getProduct).filter(s -> s instanceof SecFinance).map(sec -> ((SecFinance) sec).getCollaterals()).map(v -> v.get(0)).orElse(null);
			Collateral oldCollateral = Optional.of(oldTrade).map(Trade::getProduct).filter(s -> s instanceof SecFinance).map(sec -> ((SecFinance) sec).getCollaterals()).map(v -> v.get(0)).orElse(null);
			//Common values
			newIsin = newRepo.getSecurity().getSecCode(ISIN);
			oldIsin = oldRepo.getSecurity().getSecCode(ISIN);
			tradeDate = trade.getTradeDate();
			oldTradeDate = oldTrade.getTradeDate();
			quantity = Optional.ofNullable(newCollateral).map(Collateral::getQuantity).orElse(0.0);
			oldQuantity = Optional.ofNullable(oldCollateral).map(Collateral::getQuantity).orElse(0.0);
			nominal = Optional.ofNullable(newCollateral).map(Collateral::getNominal).orElse(0.0);
			oldNominal = Optional.ofNullable(oldCollateral).map(Collateral::getNominal).orElse(0.0);
			currency = trade.getProduct().getCurrency();
			oldCurrency = oldTrade.getProduct().getCurrency();
			startDate = newRepo.getStartDate();
			oldStartDate = oldRepo.getStartDate();
			direction = newRepo.getDirection(Repo.REPO, newRepo.getSign());
			oldDirection = oldRepo.getDirection(Repo.REPO, newRepo.getSign());
			principal = newRepo.getCash().getPrincipal();
			oldPrincipal = oldRepo.getCash().getPrincipal();
		}else {
			super.initValues(trade,oldTrade);
		}

	}

}
