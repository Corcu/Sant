package calypsox.tk.refdata;

import com.calypso.tk.refdata.SettleDeliveryInstruction;

public class CustomSDIDescription extends com.calypso.tk.refdata.CustomSDIDescription {

    @Override
    public String calcDescription(SettleDeliveryInstruction sl) {
        String calcDescription = super.calcDescription(sl);
        return !"NONE".equalsIgnoreCase(calcDescription) ? calcDescription + "/" + sl.getId() : calcDescription;
    }
}
