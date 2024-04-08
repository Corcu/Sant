package calypsox.tk.upload.validator;

import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Keyword;

import java.util.Optional;

/**
 * @author aalonsop
 */
public interface ReprocessKwdValidator {


    default boolean isReprocess(CalypsoTrade calypsoTrade){
        return Optional.ofNullable(calypsoTrade).map(this::isReprocessKwdFound).orElse(false);
    }

    default boolean isReprocessKwdFound(CalypsoTrade calypsoTrade){
        boolean res=false;
        for (Keyword keyword : calypsoTrade.getTradeKeywords().getKeyword()) {
            if("IsReprocess".equalsIgnoreCase(keyword.getKeywordName())){
                res=Boolean.parseBoolean(keyword.getKeywordValue());
                break;
            }
        }
        return res;
    }
}
