package calypsox.tk.upload.validator;

import calypsox.ctm.DUPTradeKeywordHandler;
import calypsox.ctm.util.CTMTradeFinder;
import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.tk.product.BondAllocator;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.TradeRoleAllocation;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.ProductAllocator;
import com.calypso.tk.product.allocation.Allocatable;
import com.calypso.tk.product.allocation.AllocatorUtil;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Keyword;
import com.calypso.tk.upload.services.ErrorExceptionUtils;

import java.util.Optional;
import java.util.Vector;
import java.util.function.Function;

/**
 * @author aalonsop
 */
public class ValidateCalypsoTradeBondCTM extends ValidateCalypsoTradeBond {

    /**
     * validate : unique external reference per product.
     */
    public void validate(CalypsoObject object, Vector<BOException> errors) {
        validateAllocationBlockTrade((CalypsoTrade) object, errors);
        super.validate(object, errors);
    }

    protected void validateAllocationBlockTrade(CalypsoTrade dupTrade, Vector<BOException> errors) {
        Trade blockTrade = getBlockTrade(dupTrade,
                CTMTradeFinder::findCTMBlockTrade, CTMUploaderConstants.ALLOCATED_FROM_EXT_REF);
        checkBlockTradeExistance(blockTrade, errors);
        checkOverAllocation(dupTrade, blockTrade, errors);
        matchAllocationAndBlockTradePlatform(dupTrade, blockTrade, errors);
    }

    /**
     * If blockTrade is not found (either not existing reference or trade not matching target statuses)
     * incoming msg has to be stopped avoiding trade generation
     */
    protected final void checkBlockTradeExistance(Trade blockTrade, Vector<BOException> errors) {
        if (blockTrade == null) {
            errors.add(ErrorExceptionUtils.createException(
                    ErrorExceptionUtils.ERROR_SUBTYPE_TAG, "External Reference", "50887", "Block trade not found, it may not be in the expected status", 0L));
        }
    }

    protected final void matchAllocationAndBlockTradePlatform(CalypsoTrade dupTrade, Trade blockTrade, Vector<BOException> errors) {
        boolean isPlatformMatched = Optional.ofNullable(blockTrade)
                .map(bTrade -> bTrade.getKeywordValue(CTMUploaderConstants.TRADE_KEYWORD_BLOCK_TRADE_DETAIL))
                .map(tradePlatform -> tradePlatform.equals(mapTradeTypeAsPlatformValue(dupTrade.getTradeType())))
                .orElse(true);
        if (!isPlatformMatched) {
            errors.add(ErrorExceptionUtils.createException(
                    ErrorExceptionUtils.ERROR_SUBTYPE_TAG, "External Reference", "50887", "Allocation platform does not match block's trade one (CTM/ION)", 0L));
        }
    }

    protected void checkOverAllocation(CalypsoTrade trade, Trade blockTrade, Vector<BOException> errors){
        Trade tradeToCompare = blockTrade;
        if ("DUMMY_FULL_ALLOC".equals(blockTrade.getStatus().toString())){
            tradeToCompare = getDummyTrade(blockTrade);
        }
        if (oversizedAllocation(tradeToCompare, trade)){
            errors.add(ErrorExceptionUtils.createException(
                    ErrorExceptionUtils.ERROR_SUBTYPE_TAG, "External Reference", "50898",
                    "Trade "+blockTrade.getLongId()+" cannot be overallocated", 0L));
        }
    }

    private Trade getDummyTrade(Trade blockTrade){
        Vector roleAllocations = blockTrade.getRoleAllocations();
        for (Object o: roleAllocations){
            TradeRoleAllocation tr = (TradeRoleAllocation) o;
            Trade relatedTrade = tr.getRelatedTrade();
            String kw = relatedTrade.getKeywordValue("DummyAllocation");
            if (!Util.isEmpty(kw) && Boolean.parseBoolean(kw)){
                return relatedTrade;
            }
        }
        return null;
    }

    /** Check if the allocation will oversize the blocktrade or not **/
    private boolean oversizedAllocation(Trade blockTrade, CalypsoTrade trade){
        return trade.getTradeNotional() > Math.abs(blockTrade.computeNominal());
    }

    protected Trade getBlockTrade(CalypsoTrade dupTrade, Function<String, Trade> finderFunction, String referenceKwdName) {
        return Optional.ofNullable(dupTrade)
                .flatMap(t -> DUPTradeKeywordHandler.getKwdFromCalypsoTrade(t, referenceKwdName))
                .map(Keyword::getKeywordValue)
                .map(finderFunction)
                .orElse(null);
    }

    private String mapTradeTypeAsPlatformValue(String tradeType) {
        String mappedString = tradeType;
        if (CTMUploaderConstants.CTM_TRADETYPE_STR.equals(tradeType)) {
            mappedString = CTMUploaderConstants.CTM_STR;
        } else if (CTMUploaderConstants.PLATFORM_TRADETYPE_STR.equals(tradeType)) {
            mappedString = CTMUploaderConstants.PLATFORM_STR;
        }
        return mappedString;
    }
}
