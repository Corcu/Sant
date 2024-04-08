package calypsox.tk.util;

import java.util.HashMap;
import com.calypso.tk.core.Trade;


public class FeeTradeAssociation {

	
	private HashMap<String,String> line = new HashMap<String,String>();
	private Trade trade = null;
	private int typeOfCase = 0;


	public FeeTradeAssociation(HashMap<String,String> line, Trade trade, int typeOfCase) {
		this.line = line;
		this.trade = trade;
		this.typeOfCase = typeOfCase;
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


	public int getTypeOfCase() {
		return this.typeOfCase;
	}


	public void setTypeOfcase(int typeOfCase) {
		this.typeOfCase = typeOfCase;
	}


}
