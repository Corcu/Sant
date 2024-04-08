package calypsox.repoccp.model.lch.settlement;

import calypsox.repoccp.model.lch.LCHHeader;
import calypsox.repoccp.model.lch.LCHSettlement;

import java.util.List;

public class LCHSettlementReport {
    private LCHHeader header;
    private List<LCHSettlement> settlements;

    public void setHeader(LCHHeader header) {
       this. header=header;
    }

    public void setSettlements(List<LCHSettlement> settlements) {
        this. settlements=settlements;
    }

    public LCHHeader getHeader() {
        return header;
    }

    public List<LCHSettlement> getSettlements() {
        return settlements;
    }
}
