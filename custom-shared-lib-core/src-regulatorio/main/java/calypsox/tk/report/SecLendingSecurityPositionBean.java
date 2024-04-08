package calypsox.tk.report;

import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.SecurityPosition;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Collateral;

import java.util.List;
import java.util.Optional;

public class SecLendingSecurityPositionBean extends SecurityPosition {

    public SecLendingSecurityPositionBean(MarginCallEntry entry) {
        super(entry);
    }

    @Override
    public String getType() {
        return "Security";
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    protected boolean areCompatible(MarginCallAllocation allocation, List<String> breakdownList) {
        return false;
    }

    public void init(Trade trade,Collateral collateral){
        if(Optional.ofNullable(trade).isPresent()
                && Optional.ofNullable(collateral).isPresent()){

            this.setStatus("Calculated");

            this.setProductId(collateral.getSecurityId());
            this.setDescription(collateral.getName());
            this.setCurrency(collateral.getCurrency());
            this.setCleanPrice(collateral.getInitialPrice());
            this.setQuantity(collateral.getQuantity());
            this.setHaircut(collateral.getHaircut());
            this.setNominal(collateral.getNominal());

        }
    }
}
