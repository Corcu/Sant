package calypsox.apps.refdata;

import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.RemoteReferenceData;

public class BOSettlDeliveryWindow extends com.calypso.apps.refdata.BOSettlDeliveryWindow {
    public SettleDeliveryInstruction oldSdi;

    @Override
    public boolean buildSettleDeliveryInstruction(SettleDeliveryInstruction sl, RemoteReferenceData rf, boolean buildNameB, boolean verbose) {
        if (sl.getId()!=0){
            try {
                this.oldSdi = (SettleDeliveryInstruction) sl.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

        }
        return super.buildSettleDeliveryInstruction(sl, rf, buildNameB, verbose);
    }

    public SettleDeliveryInstruction getOldSdi() {
        return oldSdi;
    }
}
