package calypsox.tk.anacredit.items;

import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class AnacreditItem {

	CollateralConfig contract;
	HashMap<String, Trade> _tradesInterests = new HashMap<>();
	HashMap<String, InventoryCashPosition> _positions;
	HashSet<String> _currencies = new HashSet<>();

	public Set<String> getCurrencies() {
		return _currencies;
	}

	public HashMap<String, InventoryCashPosition> getPositions()  {
		if (_positions == null)  {
			_positions = new HashMap<String, InventoryCashPosition>();
		}
		return _positions;
	}

	public synchronized  void addPosition(InventoryCashPosition newPos) {
		if (newPos==null){
			return;
		}
		if (!getPositions().containsKey(newPos.getCurrency())) {
			_positions.put(newPos.getCurrency(), newPos);
			_currencies.add(newPos.getCurrency());
			return;
		}
		InventoryCashPosition pos = getPositions().get(newPos.getCurrency());
		if (pos != null) {
			pos.addTotalPosition(newPos);
		}
	}

	public CollateralConfig getContract() {
		return contract;
	}

	public void setContract(CollateralConfig contract) {
		this.contract = contract;
		this._currencies.add(contract.getCurrency());
	}

	public Trade getTradeInterest(String ccy) {
		return this._tradesInterests.get(ccy);
	}

	public void setTrade(Trade trade) {
		this._tradesInterests.put(trade.getTradeCurrency(), trade);
	}

}
