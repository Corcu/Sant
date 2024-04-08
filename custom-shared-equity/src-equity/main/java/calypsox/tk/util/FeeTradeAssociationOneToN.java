package calypsox.tk.util;

import java.util.HashMap;
import java.util.List;
import com.calypso.tk.core.Trade;


public class FeeTradeAssociationOneToN {

	
	private HashMap<String,String> line = new HashMap<String,String>();
	private List<Trade> tradeList = null;

	public FeeTradeAssociationOneToN(HashMap<String,String> line, List<Trade> tradeList) {
		this.line = line;
		this.tradeList = tradeList;
	}


	public HashMap<String,String> getLine() {
		return this.line;
	}


	public void setLine(HashMap<String,String> line) {
		this.line = line;
	}


	public List<Trade> getTradeList() {
		return this.tradeList;
	}


	public void setTradeList(List<Trade> tradeList) {
		this.tradeList = tradeList;
	}


}
