package com.calypso.tk.upload.jaxb;

import com.calypso.tk.core.DayCount;
import com.calypso.tk.refdata.FdnRateIndex;
import com.calypso.tk.refdata.RateIndex;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class RIndexDayCountToStringConverter extends RateIndexToStringConverter{

    public RIndexDayCountToStringConverter() {
        super();
    }

    public String extractRateIndexField(RateIndex rateIndex){
        return Optional.ofNullable(rateIndex).map(FdnRateIndex::getDayCount)
                .map(DayCount::toString).orElse("");
    }
}