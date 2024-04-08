package calypsox.repoccp.model.lch.netting;

import java.util.ArrayList;
import java.util.List;

public class LCHNetting {
    private List<LCHNettingSet> nettingSets;
    private List<LCHObligationSet> obligationSets;

    public List<LCHNettingSet> getNettingSets() {
        return nettingSets;
    }

    public void setNettingSets(List<LCHNettingSet> nettingSets) {
        this.nettingSets = nettingSets;
    }

    public List<LCHObligationSet> getObligationSets() {
        return obligationSets;
    }

    public void setObligationSets(List<LCHObligationSet> obligationSets) {
        this.obligationSets = obligationSets;
    }

    public void addNettingSet(LCHNettingSet nettingSet) {
        if (nettingSets == null)
            nettingSets = new ArrayList<>();
        nettingSets.add(nettingSet);
    }

    public void addObligationSet(LCHObligationSet obligationSet) {
        if (obligationSets == null)
            obligationSets = new ArrayList<>();
        obligationSets.add(obligationSet);
    }
}
