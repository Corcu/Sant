package calypsox.tk.util.json;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONString;

public class JSONAmount extends JSONObject implements JSONString {
    String amount = "";

    public JSONAmount(String value){
        this.amount = value;
    }

    @Override
    public String toJSONString() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getAmount() {
        return amount;
    }
}
