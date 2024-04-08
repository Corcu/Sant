package calypsox.tk.upload.validator;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Util;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Equity;
import com.calypso.tk.upload.jaxb.Keyword;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.util.UploaderTradeUtil;
import java.util.Vector;


public class ValidateCalypsoTradeEquity extends com.calypso.tk.upload.validator.ValidateCalypsoTradeEquity {


    public static final String MUREX_TRADE_VERSION_KW = "MurexVersionNumber";


    /**
     * validate : unique external reference per product.
     */
    public void validate(CalypsoObject object, Vector<BOException> errors) {

        this.calypsoTrade = (CalypsoTrade) object;
        Double rate = this.calypsoTrade.getProduct().getEquity().getBaseFxRate();
        setBaseFxRate(1.0d);
        _trade = ValidatorUtil.getExistingTrade(this.calypsoTrade, isUndoTerminate(), errors);

        String tradeAction = calypsoTrade.getAction();
        String mxLastEventKW = getKeyWordValue("MxLastEvent");
        String mappedValue = "";
        String existingTradeMxStatus = "";
        if(_trade != null) {
            existingTradeMxStatus = _trade.getKeywordValue("MurexStatus");
        }

        if ((calypsoTrade.getTradeId() != null || _trade != null) && "NEW".equalsIgnoreCase(this.calypsoTrade.getAction()) && "1s".equalsIgnoreCase(existingTradeMxStatus)) {
            errors.add(ErrorExceptionUtils.createException("21001", "Trade Action", "00011", UploaderTradeUtil.getID(this.calypsoTrade), _trade != null ? _trade.getLongId() : calypsoTrade.getTradeId()));
            return;
        }
        if ((calypsoTrade.getTradeId() != null || _trade != null) && "NEW".equalsIgnoreCase(this.calypsoTrade.getAction()) && "1".equalsIgnoreCase(existingTradeMxStatus)) {
            if (!Util.isEmpty(calypsoTrade.getAction())) {
                // AMEND is received with NEW but different murex trade version
                //String incomingMurexVersionId = getKeyWordValue(MUREX_TRADE_VERSION_KW);
                //    if(incomingMurexVersionId != null) {
                //        String existingMurexVersionID = _trade.getKeywordValue(MUREX_TRADE_VERSION_KW);
                //        if(incomingMurexVersionId.equals(existingMurexVersionID))
                mappedValue = Action.S_AMEND;
                calypsoTrade.setAction(mappedValue);
                //    }
            }
        }

        if ((calypsoTrade.getTradeId() == null && _trade == null) && !"NEW".equalsIgnoreCase(this.calypsoTrade.getAction())) {
            String productType = Util.isEmpty(this.calypsoTrade.getExternalReference()) ? "TradeId" : "External Reference";
            errors.add(ErrorExceptionUtils.createException("21002", productType, "00002", UploaderTradeUtil.getID(this.calypsoTrade), 0L));
            return;
        }

        if (_trade != null) {
            calypsoTrade.setTradeId(_trade.getLongId());
        }

        String externalReference = calypsoTrade.getExternalReference();
        calypsoTrade.setExternalReference(null);
        super.validate(calypsoTrade, errors);
        calypsoTrade.setExternalReference(externalReference);
        setBaseFxRate(rate);
    }


    public String getKeyWordValue(String keywordName) {
        if(calypsoTrade.getTradeKeywords()!=null) {
            for(final Keyword keyword : calypsoTrade.getTradeKeywords().getKeyword()) {
                if(keyword.getKeywordName().equals(keywordName)) {
                    return keyword.getKeywordValue();
                }
            }
        }
        return null;
    }


    public boolean isUndoTerminate() {
        return calypsoTrade.getAction().equals("UNDO_TERMINATE");
    }


    private void setBaseFxRate(Double rate) {
        Equity equity = this.calypsoTrade.getProduct().getEquity();
        equity.setBaseFxRate(rate);
        calypsoTrade.getProduct().setEquity(equity);
    }


}
