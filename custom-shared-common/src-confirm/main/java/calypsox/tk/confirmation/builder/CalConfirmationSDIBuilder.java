package calypsox.tk.confirmation.builder;

import com.calypso.tk.bo.*;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

public class CalConfirmationSDIBuilder extends CalypsoConfirmationConcreteBuilder{

    LegalEntity cptyAgent;
    LegalEntity poAgent;
    SettleDeliveryInstruction poSDI;
    SettleDeliveryInstruction cptySDI;

    public CalConfirmationSDIBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        initAgentLEs();
    }

    private void initAgentLEs(){
        Vector<TradeTransferRule> xferRules=trade.getTransferRules();
        if(Util.isEmpty(xferRules)){
            xferRules= BOProductHandler.buildTransferRules(trade, new Vector(), DSConnection.getDefault(), trade.isArchived());
        }
        TradeTransferRule secXferRule = xferRules.stream()
                .filter(tr->"SECURITY".equals(tr.getTransferType()))
                .findFirst().orElse(null);
        if(secXferRule!=null) {
            this.cptySDI=BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), secXferRule.getCounterPartySDId());
            this.cptyAgent = findAgentFromXferRules(this.cptySDI);
            this.poSDI=BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), secXferRule.getProcessingOrgSDId());
            this.poAgent = findAgentFromXferRules(this.poSDI);
        }
    }

    private LegalEntity findAgentFromXferRules(SettleDeliveryInstruction sdi){
        int agentId=Optional.ofNullable(sdi)
                .map(SettleDeliveryInstruction::getAgentId).orElse(0);
        return Optional.ofNullable(BOCache.getLegalEntity(DSConnection.getDefault(),agentId)).orElse(null);
    }

    public String getTheirAgentName() {
        return Optional.ofNullable(cptyAgent).map(LegalEntity::getName).orElse("");
    }

    public String getTheirAgentCountry() {
        return Optional.ofNullable(cptyAgent).map(LegalEntity::getCountry).orElse("");
    }

    public String getTheirAgentCity() {
        int leId=Optional.ofNullable(cptyAgent).map(LegalEntity::getId).orElse(0);
        return getLEContacts(leId).stream().filter(leContact -> !Util.isEmpty(leContact.getCityName())).findFirst()
                .map(LEContact::getCityName).orElse("");
    }

    public String getOurAgentName() {
        return Optional.ofNullable(poAgent).map(LegalEntity::getName).orElse("");
    }

    public String getOurAgentCountry() {
        return Optional.ofNullable(poAgent).map(LegalEntity::getCountry).orElse("");
    }

    public String getOurAgentCity() {
        int leId=Optional.ofNullable(poAgent).map(LegalEntity::getId).orElse(0);
        return getLEContacts(leId).stream().filter(leContact -> !Util.isEmpty(leContact.getCityName())).findFirst()
                .map(LEContact::getCityName).orElse("");
    }


    public String getOurSDIAccount() {
        return Optional.ofNullable(poSDI).map(SettleDeliveryInstruction::getAgentAccount)
                .orElse("");
    }

    public String getTheirSDIAccount() {
        return Optional.ofNullable(cptySDI).map(SettleDeliveryInstruction::getAgentAccount)
                .orElse("");
    }

    public String getOurSDIBICS() {
        return getSDIBICS(this.poSDI,this.poAgent);
    }

    public String getOurSDICity() {
        return Optional.ofNullable(poAgent).map(LegalEntity::getCountry)
                .orElse("");
    }

    public String getTheirSDIBICS() {
        return getSDIBICS(this.cptySDI,this.cptyAgent);
    }

    public String getSDIBICS(SettleDeliveryInstruction sdi, LegalEntity agent){
        String psetBic=Optional.ofNullable(sdi).map(SettleDeliveryInstruction::getIntermediaryId)
                .map(id -> BOCache.getLegalEntity(DSConnection.getDefault(), id))
                .map(pset -> BOCache.getContact(DSConnection.getDefault(), null, pset, sdi.getIntermediaryContactType(), null, 0))
                .map(LEContact::getSwift)
                .map(bic -> bic+" // ")
                .orElse("");
        String agentContactType=Optional.ofNullable(sdi).map(SettleDeliveryInstruction::getAgentContactType).orElse("ALL");
        String agentBic=Optional.ofNullable(agent)
                .map(ag -> BOCache.getContact(DSConnection.getDefault(), null, ag, agentContactType, null, 0))
                .map(LEContact::getSwift)
                .orElse("");
        return psetBic+agentBic;
    }
    public String getTheirSDICity() {
        return Optional.ofNullable(cptyAgent).map(LegalEntity::getCountry)
                .orElse("");
    }

    public String getOurAgentAccAndName() {
        String name=Optional.ofNullable(poSDI).map(SettleDeliveryInstruction::getAgentName)
                .map(this::replaceAgentName).orElse("");
        String acc=Optional.ofNullable(poSDI).map(SettleDeliveryInstruction::getAgentAccount).orElse("");
        return name+" / "+ acc;

    }

    public String getTheirAgentAccAndName() {
        String name=Optional.ofNullable(cptySDI).map(SettleDeliveryInstruction::getAgentName)
                .map(this::replaceAgentName).orElse("");
        String acc=Optional.ofNullable(cptySDI).map(SettleDeliveryInstruction::getAgentAccount).orElse("");
        return name+" / "+ acc;

    }

    private String replaceAgentName(String agentName){
        String croppedName=agentName;
        if(agentName.contains("EUROCLEAR")){
            croppedName="ECLR";
        }else if(agentName.contains("CDEL")){
            croppedName="CEDE";
        }
        return croppedName;
    }

    private Vector<LEContact> getLEContacts(int leId){
        Vector<LEContact> contacts=new Vector<>();
        if(leId>0) {
            try {
                contacts= DSConnection.getDefault().getRemoteReferenceData().getLEContacts(leId);
            } catch (CalypsoServiceException exc) {
                Log.error(this.getClass().getSimpleName(),exc.getMessage());
            }
        }
        return contacts;
    }
}
