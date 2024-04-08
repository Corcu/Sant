package com.calypso.tk.upload.jaxb;

import com.calypso.tk.core.DateRoll;
import com.calypso.tk.refdata.FdnRateIndex;
import com.calypso.tk.refdata.RateIndex;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class RIndexDateRollToStringConverter extends RateIndexToStringConverter{

    public RIndexDateRollToStringConverter() {
        super();
    }

    public String extractRateIndexField(RateIndex rateIndex){
        return Optional.ofNullable(rateIndex).map(FdnRateIndex::getDateRoll)
                .map(DateRoll::toString).orElse("");
    }
}