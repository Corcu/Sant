package com.calypso.tk.upload.jaxb;

import com.github.dozermapper.core.DozerConverter;

/**
 * @author aalonsop
 */
public class PledgeDataConverter extends DozerConverter<com.calypso.tk.product.Pledge, Pledge> {

    public PledgeDataConverter() {
        super(com.calypso.tk.product.Pledge.class, Pledge.class);
    }

    @Override
    public Pledge convertTo(com.calypso.tk.product.Pledge pledge, Pledge dupPledge) {
        if(pledge!=null&&dupPledge!=null){
            dupPledge.setPledgeDirection(pledge.getQuantity()<=0.0D ? "OUT" : "IN");
            dupPledge.setProductCodeType("ISIN");
            dupPledge.setProductCodeValue(pledge.getSecurity().getSecCode("ISIN"));
            dupPledge.setOpenTermB(false);
            dupPledge.setNoticeDays(pledge.getNoticeDays());
            dupPledge.setQuantity(pledge.getQuantity());
        }
        return dupPledge;
    }

    @Override
    public com.calypso.tk.product.Pledge convertFrom(Pledge dupPledge, com.calypso.tk.product.Pledge pledge) {
        return null;
    }
}
