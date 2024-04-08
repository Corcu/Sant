package calypsox.camel.processor;

import com.calypso.tk.bo.ExternalMessage;

public class MexicoExternalMessage implements ExternalMessage {
    String text = "";

    @Override
    public String getType() {
        return "Mexico";
    }

    @Override
    public String getSender() {
        return null;
    }

    @Override
    public String getReceiver() {
        return null;
    }

    @Override
    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
