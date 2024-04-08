package com.calypso.tk.upload.jaxb;

import com.calypso.tk.refdata.RateIndexDefaults;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.github.dozermapper.core.DozerConverter;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class RFDTenorToListConverter extends DozerConverter<RateIndexDefaults, StringListType> {

    public RFDTenorToListConverter() {
        super(RateIndexDefaults.class, StringListType.class);
    }

    @Override
    public StringListType convertTo(RateIndexDefaults rateIndexDefaults, StringListType calypsoRateIndex) {
       Vector<String> tenors=LocalCache.getRateIndexTenors(DSConnection.getDefault(),rateIndexDefaults.getCurrency(),rateIndexDefaults.getName());
       StringListType listType=new StringListType();
       listType.value=tenors;
       return listType;
    }

    @Override
    public RateIndexDefaults convertFrom(StringListType calypsoRateIndex, RateIndexDefaults rateIndexDefaults) {
        return null;
    }
}