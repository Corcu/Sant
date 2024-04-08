package calypsox.tk.report;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.SecLending;

import java.util.Optional;

public class SecLendingInventorySecurityBean extends InventorySecurityPosition {

    public void init(Trade trade, Collateral collateral, JDate valDate){
        if(Optional.ofNullable(trade).isPresent()
                && Optional.ofNullable(collateral).isPresent()){

            SecLending secLendingProduct = (SecLending) trade.getProduct();
            this.setSecurityId(collateral.getSecurityId());
            this.setPositionType("Security");
            this.setMarginCallConfigId(secLendingProduct.getMarginCallContractId(trade));
            this.setTotalSecurity(collateral.getNominal());
            this.setPositionDate(valDate);
            this.setBookId(trade.getBookId());

        }
    }

}
