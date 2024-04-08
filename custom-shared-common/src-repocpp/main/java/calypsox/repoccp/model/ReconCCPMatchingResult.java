package calypsox.repoccp.model;

import calypsox.repoccp.model.lch.LCHObligations;
import calypsox.repoccp.model.lch.LCHTrade;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Trade;

import java.util.ArrayList;
import java.util.List;

import static calypsox.repoccp.ReconCCPConstants.TRADE_KWD_BUYER_SELLER_REF;

/**
 * @author aalonsop
 */
public class ReconCCPMatchingResult {

    boolean isMatched;
    Trade trade;
    BOTransfer transfer;
    List<String> matchingErrors;

    List<String> matchingWarnings;

    private String reference;

    private double cashAmount;

    public ReconCCPMatchingResult(boolean isMatched, Trade trade, List<String> matchingErrors, List<String> matchingWarnings) {
        this.isMatched = isMatched;
        this.trade = trade;
        this.matchingErrors = matchingErrors;
        this.matchingWarnings = matchingWarnings;
        this.reference =  trade != null ?trade.getKeywordValue(TRADE_KWD_BUYER_SELLER_REF):null;
    }

    public ReconCCPMatchingResult(boolean isMatched, Trade trade, BOTransfer transfer, List<String> matchingErrors, List<String> matchingWarnings) {
        this.isMatched = isMatched;
        this.transfer = transfer;
        this.trade = trade;
        this.matchingErrors = matchingErrors;
        this.matchingWarnings = matchingWarnings;
        this.reference = trade != null ? trade.getKeywordValue(TRADE_KWD_BUYER_SELLER_REF) : null;
    }

    public static ReconCCPMatchingResult buildEmptyUnmatchedResult() {
        return new ReconCCPMatchingResult(false, (Trade) null, new ArrayList<>(), new ArrayList<>());
    }

    public void addError(String errorMsg) {
        matchingErrors.add(errorMsg);
    }

    public void addWarning(String warnMsg){
        matchingWarnings.add(warnMsg);
    }

    public void addCalypsoTradeNotMatchError(Trade trade) {
        addError( "Calypso Trade " + + trade.getLongId() + " did not match with any trades of the external file");
    }

    public void addObligationNotMatched(LCHObligations obligation) {
        addError( "No transfer found in Calypso for SettlementReferenceInstructed " + obligation.getSettlementReferenceInstructed());
    }

    public boolean isMatched() {
        return isMatched;
    }

    public void unmatch(){
        isMatched = false;
    }

    public boolean hasErrors() {
        return !matchingErrors.isEmpty();
    }

    public boolean hasWarnings(){
        return !matchingWarnings.isEmpty();
    }

    public Trade getTrade() {
        return this.trade;
    }


    public BOTransfer getTransfer() {
        return transfer;
    }

    public void setTransfer(BOTransfer transfer) {
        this.transfer = transfer;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
        this.reference = trade != null ? trade.getKeywordValue(TRADE_KWD_BUYER_SELLER_REF) : null;
    }

    public String getMatchingErrors() {
        StringBuilder sb = new StringBuilder();
        sb.append("RECON CCP for trade id ");
        sb.append(getTrade().getLongId());
        sb.append(" failed, unmatched fields are: ");
        for (String error : this.matchingErrors) {
            sb.append(error);
            sb.append(", ");
        }
        sb.replace(sb.length()-2,sb.length(),"");
        return sb.toString();
    }

    public String getMatchingWarnings() {
        StringBuilder sb = new StringBuilder();
        sb.append("RECON CCP for trade id ");
        sb.append(getTrade().getLongId());
        sb.append(" has warnings, unmatched fields are: ");
        for (String warn : this.matchingWarnings) {
            sb.append(warn);
            sb.append(", ");
        }
        sb.replace(sb.length()-2,sb.length(),"");
        return sb.toString();
    }

    public List<String> getMatchingErrorsList(){
        return matchingErrors;
    }

    public String getTransferMatchingErrors() {
        StringBuilder sb = new StringBuilder();
        sb.append("Settlement recon for transfer ");
        sb.append(transfer.getLongId());
        sb.append(" failed, unmatched fields are: ");
        for (String error : this.matchingErrors) {
            sb.append(error);
            sb.append(", ");
        }
        sb.replace(sb.length()-2,sb.length(),"");
        return sb.toString();
    }

    public String getTransferMatchingWarnings() {
        StringBuilder sb = new StringBuilder();
        sb.append("Settlement recon for transfer ");
        sb.append(transfer.getLongId());
        sb.append(" has warnings, unmatched fields are: ");
        for (String warn : this.matchingWarnings) {
            sb.append(warn);
            sb.append(", ");
        }
        sb.replace(sb.length()-2,sb.length(),"");
        return sb.toString();
    }

    public String getUnmatchedErrors() {
        StringBuilder sb = new StringBuilder();
        for (String error : this.matchingErrors) {
            sb.append(error);
        }
        return sb.toString();
    }

    public String getUnmatchedWarnings() {
        StringBuilder sb = new StringBuilder();
        for (String warn : this.matchingWarnings) {
            sb.append(warn);
        }
        return sb.toString();
    }

    public String getReference() {
        return reference;
    }

    public void addTradeNotFound(ReconCCP trade) {
        if (trade instanceof LCHTrade) {
            LCHTrade lchTrade = (LCHTrade) trade;
            reference = lchTrade.getBuyerSellerReference();
            addError("LCH Trade NOT FOUND in Calypso, Buyer/Seller Reference: " + lchTrade.getBuyerSellerReference());
        }
    }

    public void addNoTransfersFound(BOTransfer transfer) {
        addError("Nominal Instructed NOT matched, Calypso Transfer ID " + transfer.getLongId()
                + ", Nominal [" + new Amount(transfer.getNominalAmount()) + "]");
    }

    public void setReference(String reference) {
        this.reference=reference;
    }

    public void setCashAmount(double cashAmount) {
        this.cashAmount =  cashAmount;
    }

    public double getCashAmount() {
        return cashAmount;
    }
}
