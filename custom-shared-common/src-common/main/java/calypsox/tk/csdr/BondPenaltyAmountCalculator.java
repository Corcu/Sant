package calypsox.tk.csdr;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Product;

/**
 * @author aalonsop
 */
public class BondPenaltyAmountCalculator extends CSDRPenaltyAmountCalculator {


    public BondPenaltyAmountCalculator(BOTransfer boTransfer, Product product) {
        super(boTransfer,product);
    }

    @Override
    public String getQuoteSet() {
        return "DirtyPrice";
    }

    @Override
    public double getSettlementAmount() {
        return this.boTransfer.getNominalAmount();
    }
}
