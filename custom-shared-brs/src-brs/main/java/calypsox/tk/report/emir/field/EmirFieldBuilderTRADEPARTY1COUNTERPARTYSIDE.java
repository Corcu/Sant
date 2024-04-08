
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.FX;
import com.calypso.tk.product.FXForward;
import com.calypso.tk.product.FXNDF;
import com.calypso.tk.product.FXSwap;
import com.calypso.tk.refdata.CurrencyPair;

public class EmirFieldBuilderTRADEPARTY1COUNTERPARTYSIDE
        implements EmirFieldBuilder {

    @Override
    public String getValue(Trade trade) {
        return EmirFieldBuilderUtil.getInstance().getLogicTRADEPARTY1COUNTERPARTYSIDE(trade);
    }

}
