package calypsox.uploader.calypso.mapping.builder;

import com.calypso.tk.mapping.core.UploaderContextProvider;
import com.calypso.tk.upload.jaxb.Pledge;
import com.calypso.tk.upload.jaxb.PledgeDataConverter;
import com.calypso.tk.upload.jaxb.PledgeTradeDataConverter;
import com.calypso.uploader.calypso.mapping.builder.TradeMappingBuilder;
import com.github.dozermapper.core.loader.api.*;

/**
 * @author aalonsop
 */
public class PledgeTradeMappingBuilder extends TradeMappingBuilder {

    TypeMappingBuilder productMappingBuilder;

    public PledgeTradeMappingBuilder() {
    }

    protected void setProductMapping() {
        this.tradeBuilder.fields("product.pledge", "product", new FieldsMappingOption[]{FieldsMappingOptions.useMapId("pledge")});
        this.productMappingBuilder = this.mapping(Pledge.class, com.calypso.tk.product.Pledge.class, new TypeMappingOption[]{TypeMappingOptions.mapId("pledge"), TypeMappingOptions.wildcard(false)});
        UploaderContextProvider.addAttributeValue("ProductType", "Repo");
    }


    public void uploadProduct() {
        this.tradeBuilder.fields("this","this",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(PledgeTradeDataConverter.class)});
        this.productMappingBuilder.fields("this","this",new FieldsMappingOption[]{FieldsMappingOptions.customConverter(PledgeDataConverter.class)});

    }
}
