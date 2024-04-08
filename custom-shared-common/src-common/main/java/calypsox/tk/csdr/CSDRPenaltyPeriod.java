package calypsox.tk.csdr;

public enum CSDRPenaltyPeriod {

    EXTENSION("failed - extension period"),
    BUYIN("failed - buy in period"),
    DEFERRAL("failed - deferral period"),
    COMPENSATION("failed - cash compensation");

    String attrValue;

    CSDRPenaltyPeriod(String attrValue) {
        this.attrValue = attrValue;
    }

    public String getAttrValue(){
        return this.attrValue;
    }
}
