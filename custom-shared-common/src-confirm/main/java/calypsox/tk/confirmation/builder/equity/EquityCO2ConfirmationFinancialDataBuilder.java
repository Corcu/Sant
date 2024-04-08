package calypsox.tk.confirmation.builder.equity;


import calypsox.tk.confirmation.builder.CalConfirmationFinantialDataBuilder;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Locale;
import java.util.Optional;
import java.util.Vector;


public class EquityCO2ConfirmationFinancialDataBuilder extends CalConfirmationFinantialDataBuilder {


    Equity equity;
    protected LEContact poContact;
    protected LEContact cptyContact;


    public EquityCO2ConfirmationFinancialDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        if (trade.getProduct() instanceof Equity) {
            this.equity = (Equity) trade.getProduct();
        }
        int senderContactId = Optional.ofNullable(boMessage).map(BOMessage::getSenderContactId).orElse(0);
        int receiverContactId = Optional.ofNullable(boMessage).map(BOMessage::getReceiverContactId).orElse(0);
        this.poContact = BOCache.getLegalEntityContact(DSConnection.getDefault(), senderContactId);
        this.cptyContact = BOCache.getLegalEntityContact(DSConnection.getDefault(), receiverContactId);
    }


    public String buildReturnStatus() {
        return String.valueOf(1);
    }


    public String buildOperationDate() {
        return Optional.ofNullable(trade).map(Trade::getTradeDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }


    public String buildEntryDate() {
        return Optional.ofNullable(trade).map(Trade::getEnteredDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }


    public String buildPortfolio() {
        return trade.getBook().getName();
    }


    public String buildDirection() {
        String buySell = "Buy";
        int buySellInd = Optional.ofNullable(equity).map(equity -> equity.getBuySell(trade)).orElse(1);
        if (buySellInd != 1) {
            buySell = "Sell";
        }
        return buySell;
    }


    public String buildSettlementDate() {
        return Optional.ofNullable(trade).map(Trade::getSettleDate).map(JDate::valueOf).map(JDate::toString).orElse("");
    }


    public String buildNominalValueAmount() {
        return Optional.ofNullable(trade).map(Trade::getQuantity).map(this::formatNumberAbs).orElse("");
    }


    public String buildTradeCurrency() {
        return Optional.ofNullable(trade).map(Trade::getTradeCurrency).orElse("");
    }


    public String buildSettlementCurrency() {
        return Optional.ofNullable(trade).map(Trade::getSettleCurrency).orElse("");
    }


    public String buildAllowanceType() {
        return equity.getSecCode("CO2_FACTURA_DESCRIPTION");
    }


    public String buildCompliancePeriod() {
        String currency = "";
        CurrencyDefault ccyDefault = LocalCache.getCurrencyDefault(equity.getCurrency());
        if(ccyDefault != null){
            currency = ccyDefault.getCode();
        }
        if(!Util.isEmpty(currency) && "EUR".equalsIgnoreCase(currency)) {
            return "Fourth";
        }
        else if(!Util.isEmpty(currency) && "GBP".equalsIgnoreCase(currency)) {
            return "First";
        }
        return "";
    }


    public String buildAllowanceNumber() {
        return String.valueOf(trade.getQuantity());
    }


    public String buildAllowancePurchPrice() {
        return String.valueOf(trade.getNegociatedPrice());
    }


    public String buildAllowancePurchCurr() {
        return trade.getTradeCurrency();
    }


    public String buildTotalPurchPrice() {
        return String.format(Locale.ENGLISH, "%.6f", trade.getQuantity() * trade.getNegociatedPrice());
    }


    public String buildTotalPurchCurr() {
        return Optional.ofNullable(equity).map(Equity::getCurrency).orElse("");
    }


    public String buildBusinessDays() {
        String currency = "";
        CurrencyDefault ccyDefault = LocalCache.getCurrencyDefault(equity.getCurrency());
        if(ccyDefault != null){
            Vector<String> holidays = ccyDefault.getDefaultHolidays();
            if(!Util.isEmpty(holidays) && holidays.size()>0){
                currency = holidays.get(0);
            }
        }
        return currency;
    }


    public String buildBuyVatJurisdiction() {
        int buySellInd = Optional.ofNullable(equity).map(equity -> equity.getBuySell(trade)).orElse(1);
        //Buy
        if (buySellInd == 1) {
            return trade.getBook().getLegalEntity().getCountry();
        }
        //Sell
        else{
            return trade.getCounterParty().getCountry();
        }
    }


    public String buildSellVatJurisdiction() {
        int buySellInd = Optional.ofNullable(equity).map(equity -> equity.getBuySell(trade)).orElse(1);
        //Buy
        if (buySellInd == 1) {
            return trade.getCounterParty().getCountry();
        }
        //Sell
        else{
            return trade.getBook().getLegalEntity().getCountry();
        }
    }


    public String buildBuyerLocation() {
        return "";
    }


    public String buildSellerLocation() {
        return "";
    }


    public String buildPaymentDate() {
        if (trade.getProduct() instanceof Equity) {
            return ((Equity) trade.getProduct()).getCashDate(trade.getSettleDate(), trade).toString();
        }
        return "";
    }


    public String buildDeliveryDate() {
        return Optional.ofNullable(trade).map(Trade::getSettleDate).map(JDate::valueOf).map(JDate::toString).orElse("");
    }


    public String buildCptyDeliveryBussDayLoc() {
        String currency = "";
        String country = trade.getCounterParty().getCountry();
        if(!Util.isEmpty(country)) {
            Vector<String> holidays = BOCache.getCountry(DSConnection.getDefault(), country).getDefaultHolidays();
            if (!Util.isEmpty(holidays) && holidays.size() > 0) {
                currency = holidays.get(0);
            }
        }
        return currency;
    }


    public String buildBrAccount() {
        return "BSCHESMM ES3500495493362919999999";
    }


    public String buildBrHoldingAccount() {
        return "EU-100-5035894-0-15";
    }


    public String buildCptyAccount() {
        return "";
    }


    public String buildCptyHoldingAccount() {
        return "";
    }


    protected String formatNumberAbs(double number) {
        return String.format(Locale.ENGLISH, "%.6f", Math.abs(number));
    }


    public String buildMifidAmount() {
        return "";
    }


    public String buildMifidCurrency() {
        return "";
    }


    public String buildMifidClasification() {
        return "";
    }


    public String buildCarbonStandard() {
        if(trade.getProduct() instanceof Equity){
            this.equity = (Equity) trade.getProduct();
            String labelMx3Eqty = this.equity.getSecCode("LABEL_MX3_EQTY");
            if(!Util.isEmpty(labelMx3Eqty)){
                String[] splitLabel = labelMx3Eqty.split("_");
                if(splitLabel!=null && splitLabel.length==3){
                    return splitLabel[0];
                }
            }
        }
        return "";
    }


    public String buildCarbonRegistry() {
        if(trade.getProduct() instanceof Equity){
            this.equity = (Equity) trade.getProduct();
            String labelMx3Eqty = this.equity.getSecCode("LABEL_MX3_EQTY");
            if(!Util.isEmpty(labelMx3Eqty)){
                String[] splitLabel = labelMx3Eqty.split("_");
                if(splitLabel!=null && splitLabel.length==3){
                    return "VCS".equalsIgnoreCase(splitLabel[0]) ? "Verra Registry" : "" ;
                }
            }
        }
        return "";
    }


    public String buildCarbonProject() {
        if(trade.getProduct() instanceof Equity){
            this.equity = (Equity) trade.getProduct();
            return this.equity.getCorporateName();
        }
        return "";
    }


    public String buildCarbonVintage() {
        if(trade.getProduct() instanceof Equity){
            this.equity = (Equity) trade.getProduct();
            String labelMx3Eqty = this.equity.getSecCode("LABEL_MX3_EQTY");
            if(!Util.isEmpty(labelMx3Eqty)){
                String[] splitLabel = labelMx3Eqty.split("_");
                if(splitLabel!=null && splitLabel.length==3){
                    return splitLabel[2];
                }
            }
        }
        return "";
    }


}
