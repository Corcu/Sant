package calypsox.engine.optimizer;

import calypsox.engine.pdv.SantPDVCollatEngine;
import calypsox.engine.pdv.SantPDVLiquidationEngine;
import com.calypso.tk.service.DSConnection;

public class SantOptimizerEngineFactory {

    public static SantOptimizerBaseEngine getOptimizerEngine(String type, String configName, DSConnection ds, String host,
                                                             int port) {
        if (SantOptimizerEngineConstants.POSITION_OPTIMIZER_ENGINE_NAME
                .equals(type)) {
            return new SantOptimizerPositionEngine(ds, host, port);
        } else if (SantOptimizerEngineConstants.MARGIN_CALL_OPTIMIZER_ENGINE_NAME
                .equals(type)) {
            return new SantOptimizerMarginCallEngine(ds, host, port);
        } else if (SantOptimizerEngineConstants.ALLOCATION_OPTIMIZER_ENGINE_NAME
                .equals(type)) {
            return new SantOptimizerAllocationEngine(ds, host, port);
        } else if (SantOptimizerEngineConstants.COLLAT_PDV_ENGINE_NAME
                .equals(type)) {
            return new SantPDVCollatEngine(ds, host, port);
        } else if (SantOptimizerEngineConstants.LIQUIDATION_PDV_ENGINE_NAME
                .equals(type)) {
            return new SantPDVLiquidationEngine(ds, host, port);
        }
        return null;
    }
}
