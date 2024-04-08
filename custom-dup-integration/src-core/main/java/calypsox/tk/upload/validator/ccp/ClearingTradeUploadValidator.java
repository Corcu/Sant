package calypsox.tk.upload.validator.ccp;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Action;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.services.ErrorExceptionUtils;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 *
 */
public interface ClearingTradeUploadValidator {


    default void validateTerminateClearingTrade(CalypsoTrade calypsoTrade, Vector<BOException> errors) {
        String action=Optional.ofNullable(calypsoTrade).map(CalypsoTrade::getAction).orElse("");
        //BETA CPTY check, need to use an SDFilter
        String cpty=Optional.ofNullable(calypsoTrade).map(CalypsoTrade::getTradeCounterParty).orElse("");
        if(cpty.equalsIgnoreCase("LV4V")&& Action.S_TERMINATE.equals(action)){
            errors.add(ErrorExceptionUtils.createException("21001", "Trade Action", "00005", "Cannot auto apply TERMINATE action on a cleared Trade, manual reprocess needed.", null!=calypsoTrade.getTradeId() ? calypsoTrade.getTradeId() : 0L));
        }
    }
}
