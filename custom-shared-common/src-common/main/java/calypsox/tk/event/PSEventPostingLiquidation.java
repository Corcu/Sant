package calypsox.tk.event;

import com.calypso.tk.event.PSEvent;

public class PSEventPostingLiquidation extends PSEvent {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	long id;

    public PSEventPostingLiquidation() {
        super();
    }

    public PSEventPostingLiquidation(long postingId) {
        this.id = postingId;
    }

    public long getPostingId() {
        return id;
    }

    public void setPostingId(long postingId) {
        this.id = postingId;
    }
}
