package calypsox.repoccp.model.lch.netting;

import calypsox.repoccp.model.lch.LCHTrade;

import java.util.List;

public class LCHNettingSet implements Cloneable {

    LCHNettingSetIdentifier identifier;

    List<LCHNetPosition> netPositions;

    List<LCHNettedTrade> nettedTrades;


    public LCHNettingSetIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(LCHNettingSetIdentifier identifier) {
        this.identifier = identifier;
    }

    public    List<LCHNetPosition> getNetPositions() {
        return netPositions;
    }

    public void setNetPositions(   List<LCHNetPosition> netPositions) {
        this.netPositions = netPositions;
    }

    public List<LCHNettedTrade> getNettedTrades() {
        return nettedTrades;
    }

    public void setNettedTrades(List<LCHNettedTrade> nettedTrades) {
        this.nettedTrades = nettedTrades;
    }

    @Override
    public LCHNettingSet clone() {
        try {
           return (LCHNettingSet) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        return "LCHNettingSet{" +
                "identifier=" + identifier +
                ", netPositions=" + netPositions +
                ", nettedTrades=" + nettedTrades +
                '}';
    }
}
