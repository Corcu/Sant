package com.calypso.tk.upload.jaxb;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Tenor;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.refdata.RateIndexDefaults;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.github.dozermapper.core.DozerConverter;

import java.util.Vector;

/**
 * @author aalonsop
 */
public abstract class RateIndexToStringConverter extends DozerConverter<RateIndexDefaults, String> {

    public RateIndexToStringConverter() {
        super(RateIndexDefaults.class, String.class);
    }

    @Override
    public String convertTo(RateIndexDefaults rateIndexDefaults, String calypsoRateIndex) {
        String riDateRoll = "";
        Vector<String> tenors=LocalCache.getRateIndexTenors(DSConnection.getDefault(),rateIndexDefaults.getCurrency(),rateIndexDefaults.getName());
        if(!Util.isEmpty(tenors)) {
            Tenor tenor=new Tenor(tenors.get(0));
            try {
                RateIndex rateIndex = DSConnection.getDefault().getRemoteReferenceData().getRateIndex(rateIndexDefaults.getCurrency(), rateIndexDefaults.getName(), tenor, rateIndexDefaults.getDefaultSource());
                riDateRoll= extractRateIndexField(rateIndex);
            } catch (CalypsoServiceException exc) {
                Log.error(this.getClass().getSimpleName(),exc.getMessage());
            }
        }
       return riDateRoll;
    }

    @Override
    public RateIndexDefaults convertFrom(String dateRoll, RateIndexDefaults rateIndexDefaults) {
        return null;
    }

    abstract String extractRateIndexField(RateIndex rateIndex);
}