package calypsox.tk.upload.mapper;

import java.util.Vector;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Keyword;
import com.calypso.tk.upload.jaxb.TradeKeywords;
import com.calypso.tk.upload.mapper.DefaultMapper;

public class MurexTransferAgentMapper  extends DefaultMapper  {
	
	public static final String TRANSFER_TRIPARTY_AGENT_KW = "TransferAgentTripartyAgent";
	public static final String BOOK_TO_AGENT = "TransferAgent_BookToAgent";
	
	
	private String source = null;
	
	CalypsoTrade calypsoTrade = null;
	
	
	public String getSource() {
		return this.source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	public void reMap(CalypsoObject calypObject, String source, Vector<BOException> errors) {
		setSource(source);
		if(calypObject instanceof CalypsoTrade) {
			calypsoTrade = (CalypsoTrade)calypObject;
			mapKeywords(calypsoTrade.getTradeKeywords());
		}
	
	}
	
	public void mapKeywords(TradeKeywords keywords) {
		if(keywords!=null) {
			Keyword kw = getKeyword(TRANSFER_TRIPARTY_AGENT_KW);
			if(kw!=null)
			{
				kw.setKeywordValue(getCalypsoValue(BOOK_TO_AGENT,kw.getKeywordValue()));
			}
		}
		
	}
	
	public Keyword getKeyword(String keywordName) {
		if(calypsoTrade.getTradeKeywords()!=null) {
			for(Keyword keyword : calypsoTrade.getTradeKeywords().getKeyword()) {
				if(keyword.getKeywordName().equals(keywordName)) {
					return keyword;
				}
			}
		}
		return null;
	}
	
	public String getCalypsoValue(String typeName, String interfaceValue) {
		return getCalypsoValue(this.getSource(),typeName,interfaceValue,null);
	}
	
}