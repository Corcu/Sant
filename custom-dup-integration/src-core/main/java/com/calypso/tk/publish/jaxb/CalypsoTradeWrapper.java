package com.calypso.tk.publish.jaxb;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * @author aalonsop
 */
public class CalypsoTradeWrapper {

    CalypsoTrade calypsoTrade=new CalypsoTrade();

    public void setAction(String action){
        calypsoTrade.setAction(action);
    }

    public void setExternalRef(String extRef){
        calypsoTrade.setExternalRef(extRef);
    }

    public void setCalypsoTradeId(String tradeId){
        if(!Util.isEmpty(tradeId)) {
            long tradeLongId=0L;
            try {
                 tradeLongId = Long.parseLong(tradeId);
            }catch(NumberFormatException exc){
                Log.debug(this,exc.getCause());
            }
            calypsoTrade.setCalypsoTradeId(tradeLongId);
        }
    }

    public void setStatus(String status){
        calypsoTrade.setStatus(status);
    }

    public void setErrors(String errorDesc){
        List<Error> errors=new ArrayList<>();
        Error error=new Error();
        error.setMessage(errorDesc);
        errors.add(error);
        calypsoTrade.error=errors;
    }

    public CalypsoTrade getCalypsoTrade(){
        return this.calypsoTrade;
    }
}
