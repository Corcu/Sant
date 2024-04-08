package com.calypso.tk.publish.jaxb;

import java.util.List;

/**
 * @author aalonsop
 */
public class CalypsoTradesWrapper {

    CalypsoTrades calypsoTrades=new CalypsoTrades();

    public void setCalypsoTradeList(List<CalypsoTrade> tradeList){
        this.calypsoTrades.calypsoTrade=tradeList;
    }

    public CalypsoTrades getCalypsoTrades(){
        return this.calypsoTrades;
    }
}
