package calypsox.repoccp.model;

import com.calypso.tk.core.Trade;

import java.util.Optional;

import static calypsox.repoccp.ReconCCPConstants.TOLERANCE;

/**
 * @author aalonsop
 */
public abstract class ReconCCPTrade implements ReconCCP<Trade> {
    /**
     *
     * @param calypsoTrade
     * @return ReconCCPMatchingResult to true if cleared input trade matches cleared calypso's one,
     * if not, unmatched fields should be recoded
     */
    public final ReconCCPMatchingResult match(Trade calypsoTrade){
        return match(calypsoTrade, TOLERANCE);
    }

    public final ReconCCPMatchingResult match(Trade calypsoTrade, double amountTolerance){
        return Optional.ofNullable(calypsoTrade).map(t->matchFields(calypsoTrade, amountTolerance))
                .orElse(ReconCCPMatchingResult.buildEmptyUnmatchedResult());
    }

    public abstract boolean matchReference(Trade calypsoTrade);

    public abstract ReconCCPMatchingResult matchFields(Trade calypsoTrade, double amountTolerance);

}
