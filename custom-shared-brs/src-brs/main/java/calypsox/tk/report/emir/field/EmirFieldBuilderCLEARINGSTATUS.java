
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class EmirFieldBuilderCLEARINGSTATUS implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        return EmirFieldBuilderUtil.getInstance().getLogicCLEARINGSTATUS(trade);
    }

}
