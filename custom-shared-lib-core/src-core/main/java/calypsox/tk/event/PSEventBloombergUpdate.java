package calypsox.tk.event;

import com.calypso.tk.event.PSEvent;

//Project: Bloomberg tagging

public class PSEventBloombergUpdate extends PSEvent {

    /**
     * Constant serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constant SEND_OPTIM_POSITION
     */
    private static final String SEND_OPTIM_POSITION = "SEND_OPTIM_POSITION";

    /**
     * tituloId.
     */
    private String tituloId;

    /**
     * tipo.
     */
    private int tipo = 0;

    /**
     * PSEventBloombergUpdate.
     * 
     * @param tituloId
     *            String
     * @param tipo
     *            int
     */
    public PSEventBloombergUpdate(String tituloId, int tipo) {
        super();
        this.tituloId = tituloId;
        this.tipo = tipo;
    }

    @Override
    public String getEventType() {
        return SEND_OPTIM_POSITION;
    }

    @Override
    public String toString() {
        return "Event to be consumed by ";
    }

    /**
     * getTituloId.
     * 
     * @return String
     */
    public String getTituloId() {
        return tituloId;
    }

    /**
     * setTituloId.
     * 
     * @param tituloId
     *            String
     */
    public void setTituloId(String tituloId) {
        this.tituloId = tituloId;
    }

    /**
     * getTipo.
     * 
     * @return int
     */
    public int getTipo() {
        return tipo;
    }

    /**
     * setTipo.
     * 
     * @param tipo
     *            int
     */
    public void setTipo(int tipo) {
        this.tipo = tipo;
    }
}
