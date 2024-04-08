package calypsox.tk.confirmation.builder.bond;

import com.calypso.tk.bo.*;
import com.calypso.tk.bo.swift.SwiftAgent;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.flow.CashFlowCoupon;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.PartySDIInfo;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Vector;

public class BondSpotConfirmFinantialDataBuilder extends BondConfirmFinantialDataBuilder{

    public BondSpotConfirmFinantialDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }

    @Override
    public String buildOperationDate() {
        return Optional.ofNullable(trade).map(Trade::getTradeDate)
                .map(tradeDate -> LocalDateTime.ofInstant(tradeDate.toInstant(), ZoneId.of("Europe/Madrid")))
                .map(tradeDateTime -> tradeDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .orElse("");
    }

    public String getNumberOfDaysAccrued(){
        try {
            CashFlowSet cashFlows = bond.getFlows(boMessage.getSettleDate());
            if (cashFlows != null){
                JDate date = boTransfer != null ? boTransfer.getSettleDate() : trade.getSettleDate();
                CashFlow cpn = cashFlows.findEnclosingCoupon(date);
                if (cpn != null){
                    int days;
                    if (cpn instanceof CashFlowCoupon){
                        days = ((CashFlowCoupon)cpn).getAccrualDays(date);
                        if (days < 0){
                            return "";
                        }

                        return String.valueOf(days);
                    }
                }
            }
        } catch (FlowGenerationException e) {
            Log.error(this, e);
        }

        return "";
    }

    public String getReferenceSubtype(){
        String secValue = bond.getSecCode("IS MARKET SECTOR DESCRIPTION");
        String res;
        if (secValue == null){
            return "";
        }

        switch (secValue){
            case "Muni":
                res = "MU";
                break;
            case "Corp":
            case "Mtge":
                res = "CO";
                break;
            case "Govt":
                res = "GO";
                break;
            default:
                res = "";
        }

        return res;
    }

    public String getDatedDate(){
        return Optional.ofNullable(bond).map(Bond::getDatedDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }

    public String getDescriptionOfTheSecurity(){
        return bond.getName();
    }

    public String getInterestRateEmisionClass(){
        if (!bond.isFlipperBondInFloatingPeriod(JDate.getNow()) || bond.getFixedB()){
            return "M";
        }else {
            return "D";
        }
    }

    public String getBasis(){
        return Optional.ofNullable(bond.getDaycount()).map(DayCount::toString).orElse("");
    }

    public String getQuantityType(){
        String unit = bond.getSecCode("UNIT_SWIFT_FORMAT");
        String res;

        if (Util.isEmpty(unit)){
            res = "Face Amount";
        }else {
            res = "Unit";
        }

        String pset = getPset();

        if (bond.getSecCode("ISIN").startsWith("PE")){
            Vector<String> domainValuesPSetDomesticoPeruano = LocalCache.getDomainValues(DSConnection.getDefault(), "pSetDomesticoPeruano");
            Vector<String> domainValuesPSetEuroclear = LocalCache.getDomainValues(DSConnection.getDefault(), "pSetEuroclear");

            if (domainValuesPSetDomesticoPeruano.contains(pset)){
                res = "Unit";
            }else if (domainValuesPSetEuroclear.contains(pset)){
                res = "Face Amount";
            }
        }
        return res;
    }

    private String getPset(){
        String pset= "";
        BOBondHandler bondHandler = new BOBondHandler();
        Vector transferRules = Optional.ofNullable(trade.getTransferRules())
                .orElse(bondHandler.generateTransferRules(trade, trade.getProduct(), new Vector(), DSConnection.getDefault()));

        if (transferRules != null){
            for (Object xferRule: transferRules){
                if (acceptTransferRule(xferRule)){
                    SettleDeliveryInstruction internalSDI = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), ((TradeTransferRule) xferRule).getProcessingOrgSDId());
                    SettleDeliveryInstruction externalSDI = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), ((TradeTransferRule) xferRule).getCounterPartySDId());
                    PartySDIInfo sdiInfo = getPSETSDIInfo(internalSDI, externalSDI, (TradeTransferRule) xferRule, trade.getProductType(), internalSDI.getProcessingOrgBasedId());
                    if (sdiInfo != null){
                        pset = sdiInfo.getPsetSwift();
                    }
                    break;
                }
            }
        }
        return pset;
    }

    private boolean acceptTransferRule(Object targetRule){
        return targetRule instanceof TradeTransferRule && "SECURITY".equals(((TradeTransferRule) targetRule).getTransferType());
    }

    private PartySDIInfo getPSETSDIInfo(SettleDeliveryInstruction posi, SettleDeliveryInstruction cptysi,TradeTransferRule rule, String productType, int poId) {
        int psetId = 0;
        SwiftAgent sa = null;
        String psetContactType = null;
        String psetSwift = "UNKNOWN";
        Vector listSDI = null;
        if (cptysi != null) {
            listSDI = getFullSDIRoute(posi, cptysi,rule);
            sa = SwiftUtil.getBridgeAgent(listSDI);
            if (sa != null) {
                psetId = sa.getAgentId();
                psetContactType = sa.getContactType();
            } else {
                sa = SwiftUtil.getLastCptyAgent(listSDI);
                SwiftAgent spo = SwiftUtil.getLastPOAgent(listSDI);
                if (sa != null && spo != null && sa.getAgentId() > 0 && spo.getAgentId() > 0) {
                    LEContact cptyT2s = BOCache.getContact(DSConnection.getDefault(), "Agent", BOCache.getLegalEntity(DSConnection.getDefault(), sa.getAgentId()), sa.getContactType(), productType, poId);
                    LEContact poT2s = BOCache.getContact(DSConnection.getDefault(), "Agent", BOCache.getLegalEntity(DSConnection.getDefault(), spo.getAgentId()), spo.getContactType(), productType, poId);
                    if (cptyT2s != null && poT2s != null && com.calypso.tk.core.Util.isTrue(cptyT2s.getAddressCode("IsTarget2S")) && com.calypso.tk.core.Util.isTrue(poT2s.getAddressCode("IsTarget2S"))) {
                        psetId = sa.getAgentId();
                        psetContactType = sa.getContactType();
                    }
                }
            }

            psetSwift = SwiftUtil.getSwiftCode(psetId, "Agent", psetContactType, productType, poId, this.trade, this.boTransfer, this.boMessage, DSConnection.getDefault());
        }

        PartySDIInfo partyInfo = new PartySDIInfo(psetId, "Agent", psetContactType, "PSET", productType, psetSwift, "", "");
        return partyInfo;
    }

    private Vector getFullSDIRoute(SettleDeliveryInstruction posi, SettleDeliveryInstruction cptysi, TradeTransferRule rule) {
        Vector v;
        if (rule != null) {
            v = rule.getFullRoute();
            if (!com.calypso.tk.core.Util.isEmpty(v) && v.size() > 2) {
                return v;
            }
        }
        v = SDISelectorUtil.getFullRoute(cptysi, posi, this.trade, rule, this.trade.getSettleDate(), DSConnection.getDefault());
        return v;
    }

    @Override
    protected String formatNumber(Double number) {
        return String.format(Locale.ENGLISH, "%.8f", number);
    }
}
