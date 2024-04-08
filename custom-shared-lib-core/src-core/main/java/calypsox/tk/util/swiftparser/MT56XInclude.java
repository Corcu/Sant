package calypsox.tk.util.swiftparser;

import java.util.ArrayList;
import java.util.List;

public class MT56XInclude {
    String bic = "";
    String type = "";
    List<String> values = new ArrayList<>();

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
