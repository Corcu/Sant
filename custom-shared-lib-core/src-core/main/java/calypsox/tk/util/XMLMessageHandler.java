package calypsox.tk.util;

import org.xml.sax.helpers.DefaultHandler;

import com.calypso.tk.bo.BOMessage;

/**
 * The Class XMLMessageHandler.
 */
public abstract class XMLMessageHandler extends DefaultHandler implements
        BoMessageContainer {
    private final BOMessage message;

    /**
     * Instantiates a new xML message handler.
     */
    public XMLMessageHandler() {
        this.message = new BOMessage();
    }

    /*
     * (non-Javadoc)
     * 
     * @see calypsox.tk.util.BoMessageContainer#getMessage()
     */
    @Override
    public BOMessage getMessage() {
        return this.message;
    }

}
