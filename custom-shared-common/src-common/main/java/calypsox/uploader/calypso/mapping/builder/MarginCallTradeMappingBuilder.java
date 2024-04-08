package calypsox.uploader.calypso.mapping.builder;

import com.github.dozermapper.core.loader.api.FieldsMappingOption;
import com.github.dozermapper.core.loader.api.FieldsMappingOptions;

public class MarginCallTradeMappingBuilder
        extends com.calypso.uploader.calypso.mapping.builder.MarginCallTradeMappingBuilder{


    @Override
    protected void configure(){
        super.configure();
        FieldsMappingOption[] tradeId=
                new FieldsMappingOption[]{FieldsMappingOptions.customConverter(CustomTradeIdConverter.class)};
        this.tradeBuilder.fields("tradeId", "this", tradeId);

    }

    @Override
    protected void setInternalReference() {
        FieldsMappingOption[] mappingOptions=new FieldsMappingOption[]{FieldsMappingOptions.customConverter(CustomInternalReferenceConverter.class)};
        this.tradeBuilder.fields("internalReference", "internalReference",
                mappingOptions);
    }
}
