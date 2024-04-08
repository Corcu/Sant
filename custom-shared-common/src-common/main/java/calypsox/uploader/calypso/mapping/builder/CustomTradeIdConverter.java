package calypsox.uploader.calypso.mapping.builder;

import com.calypso.uploader.calypso.mapping.converter.TradeIdConverter;

public class CustomTradeIdConverter extends TradeIdConverter {

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        Object tradeId = super.convert(existingDestinationFieldValue, sourceFieldValue, destinationClass, sourceClass);
        if (tradeId == null) {
            tradeId = 0L;
        }
        return tradeId;
    }
}
