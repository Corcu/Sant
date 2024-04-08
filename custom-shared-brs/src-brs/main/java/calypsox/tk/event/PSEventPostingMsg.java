package calypsox.tk.event;

import com.calypso.tk.event.PSEvent;

public class PSEventPostingMsg extends PSEvent {

    String message;

    public PSEventPostingMsg() {
        super();
    }

    public PSEventPostingMsg(String postingMessage) {
        super();
        this.message = postingMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
