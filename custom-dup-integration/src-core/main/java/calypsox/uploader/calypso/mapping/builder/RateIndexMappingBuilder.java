package calypsox.uploader.calypso.mapping.builder;

import com.calypso.tk.refdata.RateIndexDefaults;
import com.calypso.tk.upload.jaxb.CalypsoRateIndex;
import com.calypso.tk.upload.jaxb.ListToHolidayCodeTypeConverter;
import com.calypso.tk.upload.jaxb.ListToStringListTypeConverter;
import com.calypso.tk.upload.jaxb.RFDTenorToListConverter;
import com.calypso.tk.upload.jaxb.RIndexDateRollToStringConverter;
import com.calypso.tk.upload.jaxb.RIndexDayCountToStringConverter;
import com.calypso.tk.upload.jaxb.RateIndexAttributeConverter;
import com.calypso.uploader.calypso.mapping.converter.StaticDataActionConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToDateRollConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToDateRuleConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToDayCountConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToFrequencyConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToIntegerConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToPeriodRuleConverter;
import com.calypso.uploader.calypso.mapping.converter.StringToTimeZoneConverter;
import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import com.github.dozermapper.core.loader.api.FieldsMappingOption;
import com.github.dozermapper.core.loader.api.FieldsMappingOptions;
import com.github.dozermapper.core.loader.api.TypeMappingBuilder;
import com.github.dozermapper.core.loader.api.TypeMappingOption;
import com.github.dozermapper.core.loader.api.TypeMappingOptions;

/**
 * @author aalonsop
 */
public class RateIndexMappingBuilder extends BeanMappingBuilder {

    TypeMappingBuilder mappingBuilder = null;

    public RateIndexMappingBuilder() {
    }

    protected void configure() {
        this.mappingBuilder = this.mapping(CalypsoRateIndex.class, RateIndexDefaults.class, new TypeMappingOption[]{TypeMappingOptions.wildcard(false)});
        this.mappingBuilder.fields(this.field("action").accessible(), this.field("__version").accessible(), new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StaticDataActionConverter.class)});
        this.mappingBuilder.fields("currency","currency",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("index","name",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("dayCount","dayCount",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToDayCountConverter.class)});
        this.mappingBuilder.fields("sources","sources",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(ListToStringListTypeConverter.class)});
        this.mappingBuilder.fields("timeZone","resetTimeZone",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToTimeZoneConverter.class)});
        this.mappingBuilder.fields("resetHour","resetHour",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("dateRoll","dateRoll",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToDateRollConverter.class)});
        this.mappingBuilder.fields("periodRule","periodRule",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToPeriodRuleConverter.class)});
        this.mappingBuilder.fields("publishFreq","publishFrequency",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToFrequencyConverter.class)});
        this.mappingBuilder.fields("defaultSource","defaultSource",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("publishDateRule","publishDateRule",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToDateRuleConverter.class)});
        this.mappingBuilder.fields("payHoliday","paymentHolidays",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(ListToHolidayCodeTypeConverter.class)});
        this.mappingBuilder.fields("resetHoliday","resetHolidays",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(ListToHolidayCodeTypeConverter.class)});
        this.mappingBuilder.fields("payDays","paymentDays",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("resetDays","resetDays",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("payBusLagB","paymentBusLagB",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("payInArrearsB","paymentInArrearB",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("resetBusLagB","resetBusLagB",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("resetInArrearsB","resetInArrearB",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("compoundFreq","compoundFrequency",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToFrequencyConverter.class)});
        this.mappingBuilder.fields("indexType","indexType",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("rateRounding","interpolatedRateRoundingMethod",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("quoteType","quoteType",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("noAutoInterpB","noAutomaticInterpolation",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("comment","comments",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("formula","formula",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("tenor","this",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(RFDTenorToListConverter.class)});
        this.mappingBuilder.fields("dateRollTenor","this",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(RIndexDateRollToStringConverter.class)});
        this.mappingBuilder.fields("dayCountTenor","this",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(RIndexDayCountToStringConverter.class)});
        this.mappingBuilder.fields("inflationCalculationMethod","inflationCalculationMethod",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("inflationInterpMethod","inflationInterpMethod",new FieldsMappingOption[0]);
        this.mappingBuilder.fields("publicationLag","publicationLag",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(StringToIntegerConverter.class)});
        this.mappingBuilder.fields("this","this",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(RateIndexAttributeConverter.class)});
    }
}
