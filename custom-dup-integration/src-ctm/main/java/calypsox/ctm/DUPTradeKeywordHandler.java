package calypsox.ctm;

import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Keyword;
import com.calypso.tk.upload.jaxb.TradeKeywords;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author aalonsop
 */
public class DUPTradeKeywordHandler {


    public static Optional<Keyword> getKwdFromCalypsoTrade(CalypsoTrade dupTrade, String originalKwdName){
        return Optional.of(dupTrade)
                .map(CalypsoTrade::getTradeKeywords)
                .map(TradeKeywords::getKeyword)
                .map(List::stream)
                .map(stream -> stream.filter(kwd -> kwd.getKeywordName().equals(originalKwdName)))
                .flatMap(Stream::findAny);
    }

}
