package calypsox.repoccp.model.lch.netting;

import java.util.List;

public class LCHObligationSet {

    private LCHObligationSetIdentifier identifier;
    private List<LCHNetPosition> nettedPositions;
    private List<LCHObligation> obligations;

    public LCHObligationSetIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(LCHObligationSetIdentifier identifier) {
        this.identifier = identifier;
    }

    public List<LCHNetPosition> getNettedPositions() {
        return nettedPositions;
    }

    public void setNettedPositions(List<LCHNetPosition> nettedPositions) {
        this.nettedPositions = nettedPositions;
    }

    public List<LCHObligation> getObligations() {
        return obligations;
    }

    public void setObligations(List<LCHObligation> obligations) {
        this.obligations = obligations;
    }

    @Override
    public String toString() {
        return "LCHObligationSet{" +
                "identifier=" + identifier +
                '}';
    }
}
