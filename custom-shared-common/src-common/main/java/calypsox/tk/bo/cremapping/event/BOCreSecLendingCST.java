package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.TransferArray;

import java.util.Arrays;
import java.util.Optional;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreSecLendingCST extends BOCreMarginCallCST {
    public BOCreSecLendingCST(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    public void fillValues() {
        super.fillValues();
        this.creDescription = this.creBoTransfer != null ? this.creBoTransfer.getTransferType() : "";
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.nettingType = "";
        this.nettingParent = 0L;

    }

    public CollateralConfig getContract() {
        if (null != this.trade && this.trade.getProduct() instanceof SecLending) {
            Integer contractId = ((SecLending) this.trade.getProduct()).getMarginCallContractId(this.trade);
            return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
        }
        return null;
    }


    @Override
    protected Double getPosition() {
        if("SecLendingFeeCashPoolDAP".equalsIgnoreCase(this.creBoTransfer.getNettingType()) && existCst()){
            final Double creAmount = getCreAmount();
            final Double cashPosition = getCashPosition();
            final Double boCresAmount = getBOCresAmount();
            getInstance().generatePositionLog(this.boCre,this.tradeId,cashPosition,boCresAmount ,creAmount );
            return null!=cashPosition && null!=boCresAmount ? cashPosition + boCresAmount + creAmount : 0.0;
        }
        final BOCre cRfromTrade = getInstance().getCRfromTrade(this.trade);
        return getInstance().getAccountBalancefromCre(cRfromTrade);
    }


    private boolean existCst(){
        try {
            CreArray boCres = DSConnection.getDefault().getRemoteBO().getBOCres(this.trade.getLongId());
            return Arrays.stream(boCres.getCres()).anyMatch(cre -> cre.getEventType().equalsIgnoreCase("CST"));
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error getting trades: " + e);
        }
        return false;
    }

    protected Double getBOCresAmount(){
        String from = getInstance().buildFrom(this.boCre.getEventType());
        String where = buildWhere();
        return getInstance().getBOCresAmount(from,where,this.boCre,false);
    }


    protected String buildWhere(){
        JDate valDate = this.boCre.getTradeDate();
        if(null!=this.collateralConfig){
            return getInstance().buildWhere(this.boCre.getEventType(), this.collateralConfig.getId(),valDate);
        }
        return "";
    }

    @Override
    public Double getCashPosition() {
        JDate valDate = null;
        valDate = getInstance().addBusinessDays(this.boCre.getEffectiveDate(),-1);
        return getInstance().getInvLastCashPosition(this.collateralConfig,this.trade, BOCreConstantes.DATE_TYPE_TRADE,BOCreConstantes.THEORETICAL,valDate);
    }

    @Override
    protected String loadOriginalEventType() {
        String originalEventType = super.loadOriginalEventType();
        boolean failed = Boolean.parseBoolean(Optional.ofNullable(this.creBoTransfer).map(boTransfer -> boTransfer.getAttribute("Failed")).orElse("false"));
        if(failed && Arrays.asList("SETTLED_SEC_DELIVERY", "SETTLED_SEC_RECEIPT").contains(originalEventType)){
            originalEventType = "RE".concat(originalEventType);
        }
        return originalEventType;
    }

    @Override
    protected String loadProductType() {
        return "SecLending";
    }

    @Override
    protected String loadCounterParty() {
        LegalEntity le = null;
        int externalLegalEntityId = 0;
        externalLegalEntityId = null != this.creBoTransfer ? this.creBoTransfer.getExternalLegalEntityId() : 0;
        le = BOCache.getLegalEntity(DSConnection.getDefault(), externalLegalEntityId);
        return null != le ? le.getExternalRef() : "";
    }

    protected String loadCreEventType(){
        String eventType = null != this.boCre ? this.boCre.getEventType() : "";
        return "CST_FAILED".equalsIgnoreCase(eventType) ? "CST" : eventType;
    }

    @Override
    protected String getSubType() {
        if(null!=this.creBoTransfer){
            try {
                TransferArray nettedTransfers = DSConnection.getDefault().getRemoteBO().getNettedTransfers(this.creBoTransfer.getLongId());
                if(!Util.isEmpty(nettedTransfers)){
                    BOTransfer boTransfer = nettedTransfers.stream().filter(transfer -> "COLLATERAL".equalsIgnoreCase(transfer.getTransferType())).findFirst().orElse(null);
                    if(null!=boTransfer){
                        String eventTypeActionName = boTransfer.getAttribute("EventTypeActionName");
                        if("Partial Return".equalsIgnoreCase(eventTypeActionName)){
                            return eventTypeActionName;
                        }
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading Netted Transfers for transfer: " + this.creBoTransfer.getLongId());
            }
        }

        return this.trade!=null ? trade.getProductSubType() : "";
    }

    @Override
    protected String getDebitCredit(double value) {
        return BOCreUtils.getInstance().getDebitCredit(value);
    }

    @Override
    protected String loadAccountCurrency() {
        return null != this.account ? this.account.getCurrency() : "";
    }

}
