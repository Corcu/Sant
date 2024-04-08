package calypsox.tk.bo;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.AdviceConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Collections;
import java.util.Vector;

public class BondRoleFinder extends com.calypso.tk.bo.BondRoleFinder {

    @Override
    public Vector getRolesInTransfer(String role, BOTransfer transfer, Trade trade, Vector exceptions, AdviceConfig config, DSConnection dsCon) {
        if (role.equals("Agent") &&  LocalCache.getDomainValues(dsCon, "ignoreMsgToAgentInactive").contains(config.getMessageType()) && transfer.getInternalAgentId() >0) {
            return new Vector(Collections.singleton(BOCache.getLegalEntity(dsCon, transfer.getInternalAgentId())));
        }
        return super.getRolesInTransfer(role, transfer, trade, exceptions, config, dsCon);
    }
}
