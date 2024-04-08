package calypsox.tk.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//Project: Bloomberg tagging

public class BloombergInfoBean {

    private Map<String, String> valueMap = null;

    BloombergInfoBean() {
        valueMap = new HashMap<String, String>();
    }

    public String get(String key) {
        return valueMap.get(key);
    }

    public void set(String key, String value) {
        if (!valueMap.containsKey(key)) {
            valueMap.put(key, value);
        }
    }

    public Set<String> getFieldList() {
        return valueMap.keySet();
    }

}
