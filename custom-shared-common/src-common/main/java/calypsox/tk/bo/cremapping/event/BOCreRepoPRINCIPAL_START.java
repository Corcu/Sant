package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.Trade;

/**
 * @author aalonsop
 */
public class BOCreRepoPRINCIPAL_START extends BOCreRepoPRINCIPAL{

    public BOCreRepoPRINCIPAL_START(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void fillValues() {
       super.fillValues();
       this.eventType= BOCreConstantes.PRINCIPAL;
        this.nettingType = "None"; //TODO ??? none por defecto
    }
}
