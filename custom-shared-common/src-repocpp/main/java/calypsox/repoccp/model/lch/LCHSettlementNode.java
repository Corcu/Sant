package calypsox.repoccp.model.lch;

import java.util.ArrayList;
import java.util.List;

public class LCHSettlementNode {

    private final LCHSettlement settlement;

    private final List<LCHSettlementNode> spawns = new ArrayList<>();

    public LCHSettlementNode(LCHSettlement settlement) {
        this.settlement = settlement;
    }

    public LCHSettlement getSettlement() {
        return settlement;
    }

    public List<LCHSettlementNode> getSpawns() {
        return spawns;
    }

    public boolean addSpawn(LCHSettlementNode spawn) {
        if (spawns.stream().anyMatch(s->s.getSettlement().getSettlementReferenceInstructed().equals(spawn.getSettlement().getSettlementReferenceInstructed())))
            return false;
       return spawns.add(spawn);

    }
}
