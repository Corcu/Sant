package calypsox.tk.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.calypso.tk.core.Trade;


public class FeeTradeAssociationNtoOne {

	
	private List<HashMap<String,String>> lineList = new ArrayList<HashMap<String,String>>();
	private Trade trade = null;


	public FeeTradeAssociationNtoOne(List<HashMap<String,String>> lineList, Trade trade) {
		this.lineList = lineList;
		this.trade = trade;
	}


	public List<HashMap<String,String>> getLineList() {
		return this.lineList;
	}


	public void setLine(List<HashMap<String,String>> lineList) {
		this.lineList = lineList;
	}


	public Trade getTrade() {
		return this.trade;
	}


	public void setTrade(Trade trade) {
		this.trade = trade;
	}


}
