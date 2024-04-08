package com.calypso.tk.upload.jaxb;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class TradeKeywordsAdapter {

    private final TradeKeywords tradeKeywords;


    public TradeKeywordsAdapter(CalypsoTrade calypsoTrade){
        this.tradeKeywords= Optional.ofNullable(calypsoTrade)
                .map(CalypsoTrade::getTradeKeywords)
                .orElse(new TradeKeywords());
    }

    public void addKeywordToTradeKeywordList(Keyword kwd){
        this.tradeKeywords.keyword.add(kwd);
    }

    public TradeKeywords getTradeKeywords(){
        return this.tradeKeywords;
    }

}
