package calypsox.tk.export.ack;

import com.calypso.tk.bo.ExternalMessage;

/**
 * @author aalonsop
 */
public interface DUPAckProcessor {
    boolean process(ExternalMessage message);
}
