package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.util.CollateralBasedUtil;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.TransferArray;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreRepoPRINCIPAL extends SantBOCre {

    TransferArray boTransfers = new TransferArray();


    public BOCreRepoPRINCIPAL(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void fillValues() {
        this.creDescription = "Principal";
        BOCreUtils.getInstance().setRepoPartenonAccId(this.boCre, this.trade, this);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
        this.maturityDate = null != this.trade && this.trade.getProduct() instanceof Repo ? ((Repo) this.trade.getProduct()).getEndDate() : null;
        this.nettingType = getInstance().getNettingType(this.creBoTransfer);

        //boolean increaseRepo = BOCreUtils.getInstance().isIncreaseRepo(this.trade, this.creBoTransfer);
        this.nettingParent = null != this.creBoTransfer ? this.creBoTransfer.getLongId() : this.tradeId;
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        checkAndSetRLPrincipal();
        this.nettingNumber = "1";
        this.underlyingType = BOCreUtils.getInstance().getMaturityType(this.trade);
        if(CollateralBasedUtil.isBSB(trade)){
            setCouponAmountFields();
        }

    }

    @Override
    protected BOTransfer getCreBoTransfer() {
        if (!(boCre.getTransferLongId() > 0)) {
            boTransfers = BOCreUtils.getInstance().getXfersByEffectiveDate(this.boCre, this.trade, "PRINCIPAL");
            if (boTransfers.size() > 1) {
                BOTransfer transfer = boTransfers.stream().filter(xfer -> "PRINCIPAL".equalsIgnoreCase(xfer.getTransferType())
                        && !xfer.getNettedTransfer()).findFirst().orElse(boTransfers.get(0));
                return transfer;
            } else if (!boTransfers.isEmpty()) {
                return boTransfers.get(0);
            }
        } else {
            return super.getCreBoTransfer();
        }
        return null;
    }


    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
        String originalEventType = super.loadOriginalEventType();
        List<String> listevents = Util.stringToList("CANCELED_SEC_PAYMENT,CANCELED_SEC_RECEIPT,CANCELED_RECEIPT,CANCELED_PAYMENT");

        if (listevents.contains(originalEventType)) {
            return JDate.getNow();
        }

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
    protected String loadIdentifierIntraEOD() {
        return "INTRADAY";
    }

    @Override
    protected String loadSettlementMethod() {
        return "";
    }


    /**
     * Check if the eventType/creDescription need to be change, only in case effectiveDate and productStart date
     */
    private boolean checkAndSetRLPrincipal() {
        JDate effectiveDate = this.boCre.getEffectiveDate();
        JDate productStartDate = Optional.ofNullable(this.trade.getProduct()).filter(p -> p instanceof Repo).map(product -> (Repo) product).map(Repo::getStartDate).orElse(null);
        if (!effectiveDate.equals(productStartDate)) {
            this.creDescription = "Return Leg Principal";
            this.eventType = "RL_PRINCIPAL";
            return true;
        }
        return false;
    }

    /**
     * @return Sum of all Repo's collat (Security FI) COUPON cashflows for the given period
     */
    public void setCouponAmountFields() {
        double couponAmount = 0.0d;
        Repo repo = (Repo) trade.getProduct();
        this.currency2=repo.getUnderlyingProduct().getCurrency();
        CashFlowSet collatFlows = repo.getCollateralFlows(repo.getEndDate());
        try {
            JDate repoEndDate=Optional.ofNullable(repo.getEndDate()).orElse(JDate.getNow());
            repo.calculateAll(collatFlows, PricingEnv.loadPE("OFFICIAL_ACCOUNTING", repoEndDate.getJDatetime()), repoEndDate);
            for (CashFlow flow : collatFlows) {
                if (flow != null && flow.getType().equals("INTEREST")) {
                    this.currency2 = flow.getCurrency();
                    couponAmount += CurrencyUtil.roundAmount(flow.getAmount(), flow.getCurrency());
                }
            }
        } catch (FlowGenerationException exc) {
            Log.error(this, exc.getCause());
        }
        this.amount2 = couponAmount;
        this.payReceiveAmt2 = BOCreUtils.getInstance().getDirection(this.amount2);
    }
}
