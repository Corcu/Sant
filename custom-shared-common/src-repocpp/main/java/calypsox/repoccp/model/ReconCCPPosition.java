package calypsox.repoccp.model;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

import java.util.Optional;

import static calypsox.repoccp.ReconCCPConstants.TOLERANCE;

/**
 * @author aalonsop
 */
public abstract class ReconCCPPosition implements ReconCCP<BOTransfer> {

    public final ReconCCPMatchingResult match(BOTransfer transfer) {
        return match(transfer, TOLERANCE);
    }

    public final ReconCCPMatchingResult match(BOTransfer transfer, double cashTolerance) {
        return Optional.ofNullable(transfer).map(x -> matchFields(x, cashTolerance))
                .orElse(ReconCCPMatchingResult.buildEmptyUnmatchedResult());
    }

    public abstract boolean matchReference(BOTransfer transfer);

    public ReconCCPMatchingResult matchFields(BOTransfer transfer) {
        return matchFields(transfer, TOLERANCE);
    }

    public abstract ReconCCPMatchingResult matchFields(BOTransfer transfer, double cashTolerance);
}
