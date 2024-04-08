package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.util.SantDateUtil;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.FXBased;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.TimeZone;

import static calypsox.tk.report.FXPLMarkReportStyle.getOtherMultiCcyTrade;

public class BOCreFXBOOKING extends SantBOCre {

    private static final String DOWNLOADKEYDEFAULT = "BAXTER:";
    private static final int EXCHANGE_RATE_LENGTH = 17;
    private static final int EXCHANGE_RATE_DECIMALS = 11;

    public BOCreFXBOOKING(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected String loadCreEventType() {
        if (this.boCre.getCreType() != null &&
                this.boCre.getCreType().equals("REVERSAL") &&
                this.boCre.getEventType() != null &&
                this.boCre.getEventType().equalsIgnoreCase("BOOKING"))
            return "CANCELED";
        return super.loadCreEventType();
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
        return null;
    }

    @Override
    protected CollateralConfig getContract() {
        return null;
    }

    @Override
    protected Account getAccount() {
        return null;
    }

    @Override
    protected void fillValues() {
        boolean predated = false;
        if (this.trade.getTradeDate().getJDate().before(JDate.getNow())){
            this.effectiveDate = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault()).getJDate();
            predated = true;
        }
        this.foTradeId = loadFoTradeId();
        this.partenonId = BOCreUtils.getInstance().loadPartenonIdCCCT(this.trade);
        this.mirrorTradeId = loadMirrorTradeId();
        this.mirrorBook = loadTradeMirrorBookName();
        this.trader = loadTrader();
        this.intragrupoId = BOCreUtils.getInstance().loadKeyword(this.trade, "RIG");
        this.roiId = BOCreUtils.getInstance().loadKeyword(this.trade, "ROI");
        //this.foPlatformId = BOCreUtils.getInstance().loadKeyword(this.trade, "PlatformTradeID");
        //this.proposito = BOCreUtils.getInstance().loadKeyword(this.trade, "Proposito");
        //this.downloadKey = loadDownloadKey();
        this.fx48HDate = loadFx48HDate(predated);
        this.maturityDate = loadMaturityDate();
        this.buyCcy = loadCurrency(true);
        this.buyAmount = loadCreAmount(true);
        this.sellCcy = loadCurrency(false);
        this.sellAmount = loadCreAmount(false);
        this.plCcy = loadPlCcy();
        this.exchangeRate = loadExchangeRate();
        this.forexrate = loadExchangeRate(); //Same logic
        this.buyAgent = loadBuyAgent(); //Only apply in maturity cre
        this.buySettleMethod = loadBuySettleMethod(); //Only apply in maturity cre
        this.sellAgent = loadSellAgent(); //Only apply in maturity cre
        this.sellSettleMethod = loadSellSettleMethod(); //Only apply in maturity cre
        this.feeStartDate = BOCreUtils.getInstance().loadDefaultCCCTDate();
        this.feeEndDate = BOCreUtils.getInstance().loadDefaultCCCTDate();
        this.ndfFixingDate = BOCreUtils.getInstance().loadDefaultCCCTDate();
        this.dcProducto = BOCreUtils.getInstance().loadfieldOfPartenonId(this.trade, 8, 11);
        this.dcSubproducto = BOCreUtils.getInstance().loadfieldOfPartenonId(this.trade, 18, 21);
    }

    private boolean isBuyCre() {
        return this.boCre.getAttributeValue("BUY/SELL").equals("BUY");
    }

    private String buildIdContr(Trade trade) {
        String idContr = "";
        long mirrorId = trade.getMirrorTradeId();
        if ((mirrorId > 0) && (mirrorId < trade.getLongId())) {
            idContr = trade.getKeywordValue("Partenon-IDCONTR_MIRROR");
        } else {
            idContr = trade.getKeywordValue("Partenon-IDCONTR");
        }
        return idContr;
    }

    public String getPartenonTradeIdFxMM(final Trade trade) {
        String partenonTradeId = "";
        final long mirrorId = trade.getMirrorTradeId();
        final long tradeId = trade.getLongId();

        if ((mirrorId > 0) && (mirrorId < tradeId)) {
            partenonTradeId = trade
                    .getKeywordValue("Partenon-TRADE_ID_MIRROR");
        } else {
            partenonTradeId = trade
                    .getKeywordValue("Partenon-TRADE_ID");
        }

        if (Util.isEmpty(partenonTradeId)) {
            partenonTradeId = "";
        }

        return partenonTradeId;
    }

    private String loadFoTradeId() {
        return getTrade() != null ? getTrade().getExternalReference() : "";
    }

    private Long loadMirrorTradeId() {
        return getTrade() != null ? getTrade().getMirrorTradeId() : 0;
    }

    private String loadTradeMirrorBookName() {
        Trade trade = getTrade();
        if (trade != null) {
            return getTrade().getMirrorBook() != null ? getTrade().getMirrorBook().getName() : "";
        }
        return "";
    }

    private String loadTrader() {
        Trade trade = getTrade();
        if (trade != null) {
            return getTrade().getTraderName() != null ? getTrade().getTraderName() : "";
        }
        return "";
    }

    protected JDate loadFx48HDate(boolean predated) {
        return predated ? JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault()).getJDate() : SantDateUtil.addBusinessDays(this.trade.getSettleDate(), -2);
    }

    private JDate loadMaturityDate() {
        return this.trade.getSettleDate();
    }

    protected String loadCurrency(boolean isBuyCcy) {
        int index = isBuyCcy ? (isBuyCre() ? 0 : 1) : (isBuyCre() ? 1 : 0);
        return this.boCre.getCurrency(index);
    }

    protected Double loadCreAmount(boolean isBuyCcy) {
        int index = isBuyCcy ? (isBuyCre() ? 0 : 1) : (isBuyCre() ? 1 : 0);
        return this.boCre.getAmount(index);
    }
    private Double loadSellAmount() {
        return this.boCre.getAmount(1);
    }

    private double loadExchangeRate() {
        return this.trade.getNegociatedPrice();
    }

    //Only apply in maturity Cre
    protected String loadBuyAgent() {
        return "";
    }

    protected String loadBuySettleMethod() {
        return "";
    }

    protected String loadSellAgent() {
        return "";
    }

    protected String loadSellSettleMethod() {
        return "";
    }

    private String loadFeeDirection() {
        return "";
    }

    private Double loadFeeAmount() {
        return 0.0;
    }

    private String loadFeeCcy() {
        return "";
    }

    private double loadNdfFixingRate() {
        return 0.0;
    }

    private String loadNdfFixingDirection() {
        return "";
    }

    private double loadNdfFixingAmount() {
        return 0.0;
    }

    private String loadNdfFixingCcy() {
        return "";
    }

    private String loadPlCcy() {
        Trade trade = getTrade();
        Trade fxTrade = getOtherMultiCcyTrade(trade);
        String result = null;
        if (fxTrade != null) {
            FXBased fx = (FXBased) fxTrade.getProduct();
            result = fx.getCurrencyPair().getAttribute("PLCurrency");
        }
        return result != null ? result : "";
    }
}
