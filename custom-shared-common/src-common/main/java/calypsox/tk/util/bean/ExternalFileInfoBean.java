package calypsox.tk.util.bean;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ExternalFileInfoBean {

    private static final long serialVersionUID = 7007351893006930178L;
    private boolean success;

    private List<String> messages = null;
    public ExternalFileInfoBean() {
        this.success = false;
        this.messages = new LinkedList<>();
    }

    public void addMessage(String s) {
        messages.add(s);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean b) {
        success = b;
    }

    public String getMessage() {
        return messages.toString();
    }
}
