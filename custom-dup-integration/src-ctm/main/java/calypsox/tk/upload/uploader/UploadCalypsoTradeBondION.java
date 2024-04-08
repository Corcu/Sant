package calypsox.tk.upload.uploader;

import calypsox.ctm.util.CTMTradeFinder;
import calypsox.ctm.util.CTMUploaderConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Keyword;
import com.calypso.tk.upload.uploader.UploadCalypsoTrade;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class UploadCalypsoTradeBondION extends UploadCalypsoTradeBondCTM {


    /**
     * ION and CTM children doesn't share the same ExternalReference kwd
     *
     * @param trade
     * @param dupTrade
     */
    @Override
    void renameKwdToAllocatedFromAndCopyIntoTrade(Trade trade, CalypsoTrade dupTrade, String originalKwdName) {
        super.renameKwdToAllocatedFromAndCopyIntoTrade(trade, dupTrade,
                CTMUploaderConstants.ALLOCATED_FROM_MX_GLOBALID);
    }


    /**
     * In this case, incoming kwd matches block trade's MxGlobalId but target
     * AllocatedFrom kwd needs block trade's externalRef before DUP allocation linking process.
     *
     * @param kwd
     * @return Mapped kwd
     * @see UploadCalypsoTrade#linkParentTradeByKeyword(com.calypso.tk.core.Trade)
     */
    @Override
    String mapKeyword(Keyword keyword) {
        return Optional.ofNullable(keyword)
                .map(Keyword::getKeywordValue)
                .map(this::getBlockTradeExtRef)
                .orElse("");
    }

    private String getBlockTradeExtRef(String ionRef){
        return Optional.ofNullable(CTMTradeFinder.findIONBlockTrade(ionRef))
                .map(Trade::getExternalReference)
                .orElse(ionRef);
    }
}
