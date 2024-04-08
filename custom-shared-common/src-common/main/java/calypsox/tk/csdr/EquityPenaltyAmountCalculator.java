package calypsox.tk.csdr;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Product;

/**
 * @author aalonsop
 */
public class EquityPenaltyAmountCalculator extends CSDRPenaltyAmountCalculator{

    public EquityPenaltyAmountCalculator(BOTransfer boTransfer, Product product) {
        super(boTransfer,product);
    }


    @Override
    public String getQuoteSet(){
        return "OFFICIAL";
    }

    @Override
    public double getSettlementAmount() {
        return this.boTransfer.getSettlementAmount();
    }
}
