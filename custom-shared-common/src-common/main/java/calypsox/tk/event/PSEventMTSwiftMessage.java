package calypsox.tk.event;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.event.PSEvent;

/**
 *
 * Contiene el mensaje swift ya parseado que nos envia GestorSTP.
 *
 * @author acd
 */
public class PSEventMTSwiftMessage extends PSEvent {
    ExternalMessage mtSwiftExternalMessage;
    String mtType;
    String mtSwiftMessage;
    String originalMessage;
    String creatorEngineName;

    public String getMtSwiftMessage() {
        return mtSwiftMessage;
    }

    public void setMtSwiftMessage(String mtSwiftMessage) {
        this.mtSwiftMessage = mtSwiftMessage;
    }

    public String getMtType() {
        return mtType;
    }

    public void setMtType(String mtType) {
        this.mtType = mtType;
    }

    public ExternalMessage getMtSwiftExternalMessage() {
        return mtSwiftExternalMessage;
    }

    public void setMtSwiftExternalMessage(ExternalMessage mtSwiftExternalMessage) {
        this.mtSwiftExternalMessage = mtSwiftExternalMessage;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public void setOriginalMessage(String originalMessage) {
        this.originalMessage = originalMessage;
    }

    public String getCreatorEngineName() {
        return creatorEngineName;
    }

    public void setCreatorEngineName(String creatorEngineName) {
        this.creatorEngineName = creatorEngineName;
    }
}
