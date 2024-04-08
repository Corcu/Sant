package calypsox.engine.im.export.output;

import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;

public class SantInitialMarginExportOutputFactory {
    public static SantInitialMarginExportOutput getSantInitialMarginExportOutput(StaticDataFilter sdf, Trade trade,
                                                                                 MarginCallDetailEntryDTO entry,
                                                                                 CollateralConfig contractCSA,
                                                                                 CollateralConfig contractCSDPO,
                                                                                 CollateralConfig contractCSDCPTY,
                                                                                 String pricingEnvName) {

        if (trade.getProduct() == null) {
            return null;
        }
        if (trade.getProduct() instanceof CollateralExposure) {
            return new SantInitialMarginExportCE(sdf, trade, entry, contractCSA, contractCSDPO, contractCSDCPTY, pricingEnvName);
        } else if (trade.getProduct() instanceof PerformanceSwap) {
            return new SantInitialMarginExportBRS(sdf, trade, entry, contractCSA, contractCSDPO, contractCSDCPTY, pricingEnvName);
        }else if (trade.getProduct() instanceof Bond) {
            return new SantInitialMarginExportBond(sdf, trade, entry, contractCSA, contractCSDPO, contractCSDCPTY, pricingEnvName);
        } else {
            throw new IllegalStateException("Unexpected value: " + trade.getProduct());
        }
    }

}
