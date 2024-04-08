package calypsox.repoccp.model.lch.netting;

import calypsox.repoccp.model.lch.LCHHeader;

public class LCHNettingReport {

    private LCHHeader header;
    private LCHNetting netting;

    public LCHHeader getHeader() {
        return header;
    }

    public void setHeader(LCHHeader header) {
        this.header = header;
    }

    public LCHNetting getNetting() {
        return netting;
    }

    public void setNetting(LCHNetting netting) {
        this.netting = netting;
    }
}
