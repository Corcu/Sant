package calypsox.tk.product;

import calypsox.ctm.util.CTMChildTradeExtRefGenerator;
import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.ctm.util.PlatformAllocationTradeFilterAdapter;
import com.calypso.tk.core.*;
import com.calypso.tk.product.allocation.AllocatorUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.upload.jaxb.CalypsoTrade;

import java.util.Hashtable;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

import static calypsox.tk.upload.uploader.util.UploadBondMultiCCyUtil.isDualCcy;


public class CTMAllocatorHandler implements PlatformAllocationTradeFilterAdapter {


    public void performCTMAllocation(Trade blockTrade, Trade allocatedTrade) {
        Trade oldTrade = null;
        if (isPlatformOrCTMBlockTrade(blockTrade)) {
            if (allocatedTrade.getAction().equals(Action.NEW)) {
                deleteCTMAllocatedKeywords(blockTrade, allocatedTrade);
            } else {
                try {
                    oldTrade = DSConnection.getDefault().getRemoteTrade().getTrade(allocatedTrade.getLongId());
                } catch (CalypsoServiceException e) {
                    Log.error(this, e);
                }

                if (oldTrade != null){
                    copyBlockTradeConfigs(allocatedTrade, oldTrade);
                }
            }
            if (Util.isEmpty(allocatedTrade.getExternalReference()) || !isChildExtReferenceSet(allocatedTrade.getExternalReference(), blockTrade.getExternalReference())) {
                allocatedTrade.setExternalReference(CTMChildTradeExtRefGenerator.buildChildExtRef(blockTrade));
            }
        }
    }

    /**
     * Copies blockTrade's kwds to child by using BondAllocator's logic.
     * It'll be performing the same steps when the trade is externally allocated (DUP) and when is manually done
     *
     * @param trade
     */
    public void enrichCTMExternalAllocatedTrade(Trade trade, CalypsoTrade calypsoTrade) {
        Optional.ofNullable(trade)
                .filter(this::isPlatformOrCTMChild)
                .ifPresent(childTrade ->
                        updateCTMAllocatedKeywords(childTrade, calypsoTrade));
    }

    private void updateCTMAllocatedKeywords(Trade allocatedTrade, CalypsoTrade calypsoTrade) {
        try {
            Trade blockTrade = DSConnection.getDefault().getRemoteTrade().getTrade(allocatedTrade.getKeywordAsLongId(CTMUploaderConstants.ALLOCATED_FROM_STR));
            Optional.ofNullable(blockTrade)
                    .ifPresent(bTrade -> {
                        AllocatorUtil.updateTradeKeywords(allocatedTrade, blockTrade, AllocatorUtil.getPreservedKeywords());
                        deleteCTMAllocatedKeywords(bTrade, allocatedTrade);
                        addGLCSBlockTradeKwd(allocatedTrade,blockTrade);
                        copyMultiCcyInfo(calypsoTrade,allocatedTrade,blockTrade);
                    });
        } catch (CalypsoServiceException exc) {
            Log.error(this, exc.getCause());
        }
    }

    private void deleteCTMAllocatedKeywords(Trade mother, Trade allocatedTrade) {
        Vector<String> blankAllocatedKeywords = LocalCache.getDomainValues(DSConnection.getDefault(), CTMUploaderConstants.BLANK_ALLOCATED_KWDS);
        Hashtable<String, String> kwd = allocatedTrade.getKeywords();
        Hashtable<String, String> motherKwd = mother.getKeywords();

        Set<String> motherKeys = motherKwd.keySet();
        Hashtable<String, String> newKwds = (Hashtable<String, String>) kwd.clone();

        for (String s : motherKeys) {
            if (kwd.containsKey(s) && blankAllocatedKeywords.contains(s)) {
                newKwds.remove(s);
            }
        }
        allocatedTrade.setKeywords(newKwds);
    }

    private boolean isChildExtReferenceSet(String extRef, String motherExtRef) {
        return extRef.matches(motherExtRef + "_\\d+") || extRef.matches("C".concat(motherExtRef) + "_\\d+");
    }

    private void copyMultiCcyInfo(CalypsoTrade calypsoTrade, Trade trade, Trade blockTrade) {
            if (blockTrade != null && calypsoTrade!=null && isDualCcy(blockTrade)) {
                trade.setSettleCurrency(blockTrade.getSettleCurrency());
                calypsoTrade.getProduct().getBond().setFxRate(blockTrade.getSplitBasePrice());
            }
    }

    private void addGLCSBlockTradeKwd(Trade allocatedTrade, Trade blockTrade){
        allocatedTrade.addKeyword(CTMUploaderConstants.TRADE_KEYWORD_GLCSBLOCKTRADE,blockTrade.getCounterParty().getCode());
    }

    private void copyBlockTradeConfigs(Trade allocatedTrade, Trade oldTrade){
        allocatedTrade.setTradeDate(oldTrade.getTradeDate());
        allocatedTrade.setSettleDate(oldTrade.getSettleDate());
        allocatedTrade.setTradePrice(oldTrade.getTradePrice());

        allocatedTrade.setTradeCurrency(oldTrade.getTradeCurrency());
        allocatedTrade.setAccrual(oldTrade.getAccrual());
        allocatedTrade.setNegociatedPrice(oldTrade.getNegociatedPrice());
        allocatedTrade.setSettleCurrency(oldTrade.getSettleCurrency());
        allocatedTrade.setSplitBasePrice(oldTrade.getSplitBasePrice());
    }

    public void copyBlockTradeBook(Trade allocatedTrade){
        Trade blockTrade = null;
        if (isPlatformOrCTMChild(allocatedTrade)){
            try {
                blockTrade = DSConnection.getDefault().getRemoteTrade().getTrade(allocatedTrade.getKeywordAsLongId(CTMUploaderConstants.ALLOCATED_FROM_STR));
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
            if (blockTrade != null){
                if (allocatedTrade.getBookId() != blockTrade.getBookId()){
                    allocatedTrade.setBook(blockTrade.getBook());
                }
            }
        }
    }
}
