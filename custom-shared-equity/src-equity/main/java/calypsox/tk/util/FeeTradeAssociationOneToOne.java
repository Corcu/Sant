package calypsox.tk.util;

import java.util.HashMap;
import com.calypso.tk.core.Trade;


public class FeeTradeAssociationOneToOne {

	
	private HashMap<String,String> line = new HashMap<String,String>();
	private Trade trade = null;


	public FeeTradeAssociationOneToOne(HashMap<String,String> line, Trade trade) {
		this.line = line;
		this.trade = trade;
	}


	public HashMap<String,String> getLine() {
		return this.line;
	}


	public void setLine(HashMap<String,String> line) {
		this.line = line;
	}


	public Trade getTrade() {
		return this.trade;
	}


	public void setTrade(Trade trade) {
		this.trade = trade;
	}


}
