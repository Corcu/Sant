package calypsox.tk.bo;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.DefaultNettingHandler;
import com.calypso.tk.core.CashFlow;

import java.sql.Connection;

/**
 * @author aalonsop
 */
public class SecurityNettingHandler extends DefaultNettingHandler {

    /**
     * Splitted transfer's security underlyings mustn't have decimals for its quantity
     * @param xfer
     * @param con
     * @return Always 0
     */
    @Override
    protected int getRoundingUnit(BOTransfer xfer, Connection con) {
        int roundingUnit=super.getRoundingUnit(xfer,con);
        if (CashFlow.SECURITY.equals(xfer.getTransferType())){
            roundingUnit=0;
        }
        return roundingUnit;
    }
}
