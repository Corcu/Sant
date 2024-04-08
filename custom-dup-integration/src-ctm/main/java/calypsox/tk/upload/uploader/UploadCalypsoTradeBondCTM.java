package calypsox.tk.upload.uploader;

import calypsox.ctm.DUPTradeKeywordHandler;
import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.tk.product.CTMAllocatorHandler;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Keyword;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class UploadCalypsoTradeBondCTM extends UploadCalypsoTradeBond{

    private final CTMAllocatorHandler ctmAllocatorHandler = new CTMAllocatorHandler();

    /**
     * Copies blockTrade's kwds to child by using BondAllocator's logic.
     * It'll be performing the same steps when the trade is externally allocated (DUP) and when is manually done
     * @param trade
     * @param calypsoTrade
     */
    @Override
    public void addKeyWord(Trade trade, CalypsoTrade calypsoTrade) {
        renameKwdToAllocatedFromAndCopyIntoTrade(trade,calypsoTrade,CTMUploaderConstants.ALLOCATED_FROM_EXT_REF);
        super.addKeyWord(trade,calypsoTrade);
        ctmAllocatorHandler.enrichCTMExternalAllocatedTrade(trade, calypsoTrade);
        ctmAllocatorHandler.copyBlockTradeBook(trade);
    }

    @Override
    protected String getKeywordValueByName(String keywordName) {
        return Optional.ofNullable(this.trade)
                .filter(t -> CTMUploaderConstants.ALLOCATED_FROM_STR.equals(keywordName))
                .map(t -> t.getKeywordValue(keywordName))
                .orElseGet(()-> super.getKeywordValueByName(keywordName));
    }
    /**
     * Empty overloaded method to avoid externalRef modification.
     * For further details:
     * @see UploadCalypsoTradeBond#setExternalReferenceFromKwd(com.calypso.tk.core.Trade)
     * @param trade
     */
    @Override
    protected final void setExternalReferenceFromKwd(Trade trade) {}


    void renameKwdToAllocatedFromAndCopyIntoTrade(Trade trade, CalypsoTrade dupTrade, String originalKwdName){
        DUPTradeKeywordHandler.getKwdFromCalypsoTrade(dupTrade,originalKwdName)
                .map(this::mapKeyword)
                .ifPresent(allocFromValue -> trade.addKeyword(CTMUploaderConstants.ALLOCATED_FROM_STR,allocFromValue));
    }

    /**
     * Overridable in case of kwd mapping needed
     * @param kwd
     * @return mapped kwd
     */
    String mapKeyword(Keyword kwd){
        return kwd.getKeywordValue();
    }
}
