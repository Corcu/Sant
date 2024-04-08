package calypsox.tk.confirmation.builder.bond;

import calypsox.tk.confirmation.builder.CalConfirmationCounterpartyBuilder;
import com.calypso.analytics.Util;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.swift.SwiftAgent;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.PartySDIInfo;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Collection;
import java.util.Optional;
import java.util.Vector;

/**
 * @author dmenendd
 */
public class BondConfirmCounterpartyBuilder extends CalConfirmationCounterpartyBuilder {

    public BondConfirmCounterpartyBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }

    public String buildInstrumentSubType() {
        String lang=this.buildLanguage();
        String subtype="Standard";
        if(lang.toLowerCase().startsWith("esp")||lang.toLowerCase().startsWith("spa")){
            subtype="other";
        }
        return subtype;
    }

    public String buildCptyType() {
       String cptyExtRef=this.cpty.getExternalRef();
       String cptyType="";
       if(!Util.isEmpty(cptyExtRef)){
           cptyType=cptyExtRef.substring(0,1);
       }
       return cptyType;
    }

    public String buildCptyCodeFlow() {
        String cptyExtRef=this.cpty.getExternalRef();
        String cptyCode="";
        if(!Util.isEmpty(cptyExtRef)&&cptyExtRef.length()>1){
            cptyCode=cptyExtRef.substring(1);
        }
        return cptyCode;
    }

    public String buildSettlementHouseCode(){
        return Optional.ofNullable(getPSETLegalEntityAttributes(this.trade))
                .map(attrs->getLEAttributeValue(attrs,"SETTLEMENT_HOUSE_CODE"))
                .orElse("");
    }

    public Vector<LegalEntityAttribute> getPSETLegalEntityAttributes(Trade trade){
        int psetId=0;
        BOBondHandler bondHandler=new BOBondHandler();
        Vector transferRules= Optional.ofNullable(trade.getTransferRules())
                .orElse(bondHandler.generateTransferRules(trade,trade.getProduct(),new Vector(),DSConnection.getDefault()));
        if(transferRules!=null) {
            for (Object xferRule : transferRules) {
                if (acceptTransferRule(xferRule)) {
                    SettleDeliveryInstruction internalSDI = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), ((TradeTransferRule) xferRule).getProcessingOrgSDId());
                    SettleDeliveryInstruction externalSDI = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), ((TradeTransferRule) xferRule).getCounterPartySDId());
                    if(internalSDI.getMethod().equals("CREST")){
                        psetId = internalSDI.getAgentId();
                    }
                    else {
                        PartySDIInfo sdiInfo = getPSETSDIInfo(internalSDI, externalSDI, (TradeTransferRule) xferRule, this.trade.getProductType(), internalSDI.getProcessingOrgBasedId());
                        if (sdiInfo != null) {
                            psetId = sdiInfo.getPartyId();
                        }
                    }
                    break;
                }
            }
        }
        return BOCache.getLegalEntityAttributes(DSConnection.getDefault(), psetId);
    }

    protected PartySDIInfo getPSETSDIInfo(SettleDeliveryInstruction posi, SettleDeliveryInstruction cptysi,TradeTransferRule rule, String productType, int poId) {
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

    private boolean acceptTransferRule(Object targetRule){
        return targetRule instanceof TradeTransferRule &&"SECURITY".equals(((TradeTransferRule) targetRule).getTransferType());
    }

    public String getLEAttributeValue(final Collection<LegalEntityAttribute> attr, String attributeType) {
        if (attr == null) {
            return "";
        }
        for (final LegalEntityAttribute leAttr : attr) {
            if (leAttr.getAttributeType().equals(attributeType)) {
                final String rst = leAttr.getAttributeValue();
                if (rst != null) {
                    return rst;
                }
                return "";
            }
        }
        return "";
    }

    public String buildAlternEmail() {
        return Optional.ofNullable(cptyContact)
                .map(contact -> contact.getAddressCode("ADDITIONAL_EMAIL"))
                .orElse("");
    }

    public String buildConfirmMode() {
        return "EMAIL";
    }
}
