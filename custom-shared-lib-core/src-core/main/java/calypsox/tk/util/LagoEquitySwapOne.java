package calypsox.tk.util;

public class LagoEquitySwapOne {
    String key;
    String action;
    String mtm;

    public LagoEquitySwapOne(String key, String action, String mtm) {
        this.key = key;
        this.action = action;
        this.mtm = mtm;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMtm() {
        return mtm;
    }

    public void setMtm(String mtm) {
        this.mtm = mtm;
    }
}
