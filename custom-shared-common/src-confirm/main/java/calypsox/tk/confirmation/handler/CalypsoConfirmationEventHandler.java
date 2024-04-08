package calypsox.tk.confirmation.handler;

import com.calypso.tk.bo.BOMessage;
import org.apache.commons.lang.WordUtils;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class CalypsoConfirmationEventHandler {

    public static final String KEYWORD_MX_LAST_EVENT = "MxLastEvent";
    public static final String KEYWORD_MX_CONTRACT_ID = "Contract ID";
    public static final String KEYWORD_MX_ROOT_CONTRACT_ID="MurexRootContract";

    private static final String COSTS_EXPENSES_EVENT="MODIFY_UDF";
    private static final char EVENT_NAME_DELIMITER='I';
    private static final String EARLY_TERMINATION="EARLY_TERMINATION";

    private static final String CANCEL_PREFIX="C";
    private static final String CANCELED_EVENT_TYPE="CANCELED_";

    EventType eventType;
    EventAction eventAction;
    InstrumentSubType instrumentSubType;

    String mxCurrentContractId;

    String mxRootContractId="";


    public CalypsoConfirmationEventHandler(BOMessage boMessage){
        if(boMessage!=null){
            setEventType(boMessage);
            setEventAction(boMessage);
            setInstrumentSubType(boMessage);
            setMxContractIds(boMessage);
        }
    }

    void setEventType(BOMessage boMessage){
        this.eventType= Optional.ofNullable(boMessage.getAttribute(KEYWORD_MX_LAST_EVENT))
                .map(this::getCroppedEventType)
                .map(EventType::lookUp)
                .orElse(EventType.REGISTRY);
    }

    void setEventAction(BOMessage boMessage){
        if(this.eventType!=null) {
            this.eventAction = Optional.ofNullable(boMessage)
                    .filter(this::isCancelMsg)
                    .map(type -> EventAction.CANCEL).orElse(EventAction.NEW);
        }
    }

    void setInstrumentSubType(BOMessage boMessage){
        this.instrumentSubType= Optional.ofNullable(boMessage.getAttribute(KEYWORD_MX_LAST_EVENT))
                .filter(lastEvent->lastEvent.contains(COSTS_EXPENSES_EVENT))
                .map(mxEvent->InstrumentSubType.MODIF_COST_EXPENSES)
                .orElse(InstrumentSubType.STANDARD);
    }

    void setMxContractIds(BOMessage boMessage){
        this.mxCurrentContractId= boMessage.getAttribute(KEYWORD_MX_CONTRACT_ID);
       /* if(this.eventType.needsOriginalOperId){
            this.mxRootContractId=cropCancelPrefix(boMessage.getAttribute(KEYWORD_MX_ROOT_CONTRACT_ID));
        }*/
        if(this.eventType.needsOriginalOperId){
            this.mxCurrentContractId=boMessage.getAttribute(KEYWORD_MX_ROOT_CONTRACT_ID);
            //An space is demanded here
            this.mxRootContractId=" ";
        }
    }

    public String getEventType() {
        return eventType.name;
    }

    public String getEventTypeId() {
        return eventType.id;
    }

    public String getEventAction() {
        return WordUtils.capitalize(this.eventAction.name().toLowerCase());
    }

    public String getInstrumentSubType() {
        return WordUtils.capitalize(instrumentSubType.name().toLowerCase());
    }

    public String getMxCurrentContractId() {
        return mxCurrentContractId;
    }

    public String getMxRootContractId() {
        return mxRootContractId;
    }

    public int mustBeSigned(){
        return eventType.mustBeSigned ? 1 : 0;
    }

    String getCroppedEventType(String eventType){
        String croppedEventType= Optional.ofNullable(eventType)
                .map(type->type.substring(type.indexOf(EVENT_NAME_DELIMITER)+1))
                .orElse("");
        if(croppedEventType.contains(EARLY_TERMINATION)){
            croppedEventType=EARLY_TERMINATION;
        }
        return croppedEventType;
    }

    String cropCancelPrefix(String rootContractId){
        return Optional.ofNullable(rootContractId)
                .filter(id->id.startsWith(CANCEL_PREFIX))
                .map(id->id.replace(CANCEL_PREFIX,""))
                .orElse(rootContractId);
    }

    enum EventType{
        REGISTRY("Registry",false),
        CANCEL("Registry",false),
        CANCEL_REISSUE("Registry",false),
        RESTRUCTURE("Restructure",true),
        RATE_AMENDMENT("Rate Amendment",true),
        SHARES_MODIFICATION("Shares Modification",true),
        EARLY_TERMINATION("Early Termination",false),
        MATURITY_EXTENSION("Maturity Extension",true),
        ADDITIONAL_FLOW_AMENDMENT("Additional Flow Amendment",true),
        MODIFY_UDF("Modification",false,InstrumentSubType.MODIF_COST_EXPENSES,false),
        MODIFICATION("Modification",false,"03"),
        ACCOUNT_CLOSING("Account Closing",false),
        NONE("",false);

        String name;
        InstrumentSubType subType;
        boolean needsOriginalOperId;
        boolean mustBeSigned;
        String id="01";

        EventType(String eventName,boolean needsOriginalOperId){
            this(eventName,needsOriginalOperId,InstrumentSubType.STANDARD,true);
        }

        EventType(String eventName,boolean needsOriginalOperId, String id){
            this(eventName,needsOriginalOperId,InstrumentSubType.STANDARD,true);
            this.id=id;
        }

        EventType(String eventName,boolean needsOriginalOperId,InstrumentSubType subType,boolean mustBeSigned){
            this.name=eventName;
            this.needsOriginalOperId=needsOriginalOperId;
            this.subType=subType;
            this.mustBeSigned=mustBeSigned;
        }

        static EventType lookUp(String name){
            EventType type;
            try{
                type=EventType.valueOf(name);
            }catch(IllegalArgumentException exc){
                type=EventType.NONE;
            }
            return type;
        }

    }

    /**
     * isCancelMsf
     *
     * @param boMessage
     * @return
     */
    boolean isCancelMsg(BOMessage boMessage){
        return Optional.ofNullable(boMessage).map(BOMessage::getEventType)
                .map(evenType -> evenType.contains(CANCELED_EVENT_TYPE))
                .orElse(false);
    }

    enum EventAction{
        NEW,
        CANCEL
    }

    enum InstrumentSubType{
        STANDARD,
        MODIF_COST_EXPENSES,
        MODIF_TRADE_TIME,
        MODIF_BREAK_CLAUSE
    }

}
