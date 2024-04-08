package calypsox.ctm;

import calypsox.ctm.util.CTMUploaderConstants;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author aalonsop
 * Used to mark CTM/ION block trades.
 * @see calypsox.tk.upload.uploader.UploadCalypsoTradeBond
 */
public class CTMBlockTradeEnricher{

    public static void enrichCTMBlockTrade(Trade trade) {
        Optional.ofNullable(trade)
                .filter(t -> isGestoraLegalEntity(trade.getCounterParty()))
                .ifPresent(CTMBlockTradeEnricher::setCTMBlockTradeKeyword);
    }

    public static String getLegalEntityGestoraType(LegalEntity legalEntity) {
        return Optional.ofNullable(legalEntity)
                .filter(t -> isGestoraLegalEntity(legalEntity))
                .map(CTMBlockTradeEnricher::getGestoraType)
                .orElse("");
    }

    private static void setCTMBlockTradeKeyword(Trade trade){
        if (Util.isEmpty(trade.getKeywordValue("AllocatedFrom"))){
            trade.addKeyword(CTMUploaderConstants.TRADE_KEYWORD_BLOCK_TRADE_DETAIL,getGestoraType(trade.getCounterParty()));
        }
    }

    private static String getGestoraType(LegalEntity legalEntity){
        return  Optional.of(legalEntity)
                .map(LegalEntity::getEntityId)
                .map(leId-> BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, leId, "ALL", CTMUploaderConstants.CONFIRMATION_TYPE_ATTR))
                .map(LegalEntityAttribute::getAttributeValue)
                .filter(confType->confType.contains(CTMUploaderConstants.CTM_STR))
                .map(value->CTMUploaderConstants.CTM_STR)
                .orElse(CTMUploaderConstants.PLATFORM_STR);
    }
    private static boolean isGestoraLegalEntity(LegalEntity legalEntity) {
        return Optional.ofNullable(legalEntity)
                .map(LegalEntity::getEntityId)
                .map(legalEntityId -> BOCache.getLegalEntityAttribute(
                        DSConnection.getDefault(), 0, legalEntityId, "ALL", CTMUploaderConstants.GESTORA_LE_ATTR))
                .map(LegalEntityAttribute::getAttributeValue)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

}
