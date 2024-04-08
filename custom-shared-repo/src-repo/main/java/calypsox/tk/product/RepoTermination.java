package calypsox.tk.product;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.ProductException;
import com.calypso.tk.core.Trade;

/**
 * @author aalonsop
 */
public class RepoTermination extends com.calypso.tk.product.RepoTermination {

    /**
     * Needed to avoid superclass terminate on end date's exception.
     * @param trade
     * @param terminationTradeDate
     * @param terminationDate
     * @param terminationType
     * @throws ProductException
     */
    @Override
    public void canTerminate(Trade trade, JDatetime terminationTradeDate, JDate terminationDate, String terminationType) throws ProductException {

        try {
            super.canTerminate(trade, terminationTradeDate, terminationDate, terminationType);
        }catch(ProductException exc){
            if(!exc.getMessage().contains("trade on End Date")){
                throw exc;
            }
        }
    }
}
