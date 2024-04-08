package calypsox.tk.event;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.event.*;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Optional;
import java.util.Vector;

public class BlockBOCreCCCTEventFilter implements EventFilter {
    /**
     * Block BOCre Events by Event Type
     * @param event
     * @return
     */
    @Override
    public boolean accept(PSEvent event) {
        boolean res = true;
        String engineName = event.getEngineName();
        if (event instanceof PSEventCre) {
            BOCre boCre = ((PSEventCre) event).getBOCre();
            res = BlockBOCreUtil.checkEvent(boCre,"CreOnlineCCCTSenderEngine",engineName,"CreAccountingRulesCCCT");
        }
        return res;
    }

}
