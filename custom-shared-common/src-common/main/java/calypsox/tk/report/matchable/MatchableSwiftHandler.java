package calypsox.tk.report.matchable;

import com.calypso.analytics.Util;
import com.calypso.helper.CoreAPI;
import com.calypso.helper.RemoteAPI;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.matching.Matchable;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Optional;
import java.util.Vector;

import static calypsox.tk.report.MatchableReportStyle.*;

/**
 * @author aalonsop
 */
public class MatchableSwiftHandler {

    private final Vector<String> swiftColumns;
    private transient ParsedSwiftCache cachedMsg= new ParsedSwiftCache( null,null);

    public MatchableSwiftHandler(){
        this.swiftColumns=initSwiftColumnList();
    }

    public Object handleSwiftTagColumn(String columnId,BOMessage message){
        Object res=null;
        if(this.swiftColumns.contains(columnId)) {
            res=getSwiftTag(columnId,message);
        }
        return res;
    }

    private Object getSwiftTag(String columnId, BOMessage message) {
        Object result = null;
        SwiftMessage swift = Optional.ofNullable(message).map(this::buildSwiftMessage).orElse(null);
        if (swift != null && !Util.isEmpty(swift.getSwiftText())) {
            if(MSG_CPTY.equals(columnId)){
                result = getCounterparty(swift);
            }else if (SAFEACC_TAG.equals(columnId)) {
                result = get97asafe(swift);
            } else if (PTYB.equals(columnId)) {
                result = get95qptyb(swift);
            } else if (PREP.equals(columnId)) {
                result = get98cprep(swift);
            } else if (TEXA.equals(columnId)) {
                result = get19atexa(swift);
            } else if (SEME.equals(columnId)) {
                result = get20cseme(swift);
            } else if (RELA.equals(columnId)) {
                result = get20crela(swift);
            } else if (TRAD.equals(columnId)) {
                result = get94btrad(swift);
            } else if (PSET.equals(columnId)) {
                result = get95ppset(swift);
            } else if (SETT.equals(columnId)) {
                result = get97bsafe(swift);
            } else if (FUNCTION.equals(columnId)) {
                result = get23gfunction(swift);
            } else if (PREV.equals(columnId)) {
                result = get20cprev(swift);
            } else if (ISIN.equals(columnId)) {
                result = get35bisin(swift);
            } else if (ACOW.equals(columnId)) {
                result = get95pacow(swift);
            } else if (SAFE.equals(columnId)) {
                result = get97bsafe(swift);
            } else if (SETR.equals(columnId)) {
                result = get22fsetr(swift);
            } else if (STCO.equals(columnId)) {
                result = get22fstco(swift);
            } else if (DEAG.equals(columnId)) {
                result = get95pdeag(swift);
            } else if (SELL.equals(columnId)) {
                result = get95pacow(swift);
            } else if (DECU.equals(columnId)) {
                result = get95qdecu(swift);
            } else if (REAG.equals(columnId)) {
                result = get95preag(swift);
            } else if (BUYR.equals(columnId)) {
                result = get95rbuyr(swift);
            } else if (RECU.equals(columnId)) {
                result = get95qrecu(swift);
            } else if (PAYM.equals(columnId)) {
                result = get22hpaym(swift);
            } else if (REDE.equals(columnId)) {
                result = get22hrede(swift);
            }
        }
        return result;
    }

    private Vector<String> initSwiftColumnList(){
        Vector<String> swiftList=new Vector<>();
        swiftList.add(SAFEACC_TAG);
        swiftList.add(PTYB);
        swiftList.add(PREP);
        swiftList.add(TEXA);
        swiftList.add(SEME);
        swiftList.add(RELA);
        swiftList.add(TRAD);
        swiftList.add(PSET);
        swiftList.add(SETT);
        swiftList.add(FUNCTION);
        swiftList.add(PREV);
        swiftList.add(ISIN);
        swiftList.add(ACOW);
        swiftList.add(SAFE);
        swiftList.add(SETR);
        swiftList.add(STCO);
        swiftList.add(DEAG);
        swiftList.add(SELL);
        swiftList.add(DECU);
        swiftList.add(REAG);
        swiftList.add(BUYR);
        swiftList.add(RECU);
        swiftList.add(PAYM);
        swiftList.add(REDE);
        swiftList.add(MSG_CPTY);
        return swiftList;
    }


    private SwiftMessage buildSwiftMessage(BOMessage boMessage){
        SwiftMessage swiftMessage= Optional.ofNullable(cachedMsg.parsedMsg).orElse(new SwiftMessage());
        try {
            if(cachedMsg.parsedMsg==null||cachedMsg.getCurrentMsgLongId()!=boMessage.getLongId()){
                AdviceDocument doc= DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(boMessage.getLongId(),new JDatetime());
                String swiftString="";
                if(doc!=null){
                    swiftString=doc.getDocument().toString();
                }
                swiftMessage.parse(swiftString, boMessage.getGateway());
                this.cachedMsg= new ParsedSwiftCache(swiftMessage,boMessage);
            }
        } catch (CalypsoServiceException exc) {
            Log.warn(this.getClass().getSimpleName(),exc.getMessage(),exc.getCause());
        }
        return swiftMessage;
    }

    private String getCounterparty(SwiftMessage mess){
        String counterPartyCode;
        String buyrSwift=Optional.ofNullable(mess.getSwiftField(":95P:",":BUYR//",null))
                .map(SwiftFieldMessage::getValue)
                .map(str->str.split("//")).map(strings -> strings[1])
                .orElse("");

        String sellSwift=Optional.ofNullable(mess.getSwiftField(":95P:",":SELL//",null))
                .map(SwiftFieldMessage::getValue)
                .map(str->str.split("//")).map(strings -> strings[1])
                .orElse("");
        LegalEntity buyrLE=Optional.ofNullable(BOCache.getContactsByAddressCode(DSConnection.getDefault(),"SWIFT",buyrSwift))
                .filter(list->!list.isEmpty())
                .map(list->list.get(0)).map(contact->BOCache.getLegalEntity(DSConnection.getDefault(),contact.getLegalEntityId()))
                .orElse(null);
        if(Optional.ofNullable(buyrLE).map(le -> le.getRoleList().contains(LegalEntity.PROCESSINGORG)).orElse(false)){
            LegalEntity sellLE=Optional.ofNullable(BOCache.getContactsByAddressCode(DSConnection.getDefault(),"SWIFT",sellSwift))
                    .filter(list->!list.isEmpty())
                    .map(list->list.get(0)).map(contact->BOCache.getLegalEntity(DSConnection.getDefault(),contact.getLegalEntityId()))
                    .orElse(null);
            counterPartyCode=Optional.ofNullable(sellLE).map(LegalEntity::getCode).orElse("");
        }else{
            counterPartyCode=Optional.ofNullable(buyrLE).map(LegalEntity::getCode).orElse("");
        }
        return counterPartyCode;
    }

    private String get97asafe(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":97A:",":SAFE//",null);
        return formatField(field);
    }

    private String get95qptyb(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":95Q:",":PTYB//",null);
        return formatField(field);
    }

    private String get98cprep(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":98E:",":PREP//",null);
        return formatField(field);
    }

    private String get19atexa(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":19A:",":TEXA//",null);
        return formatField(field);
    }

    private String get20cseme(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":20C:",":SEME//",null);
        return formatField(field);
    }

    private String get20crela(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":20C:",":RELA//",null);
        return formatField(field);
    }

    private String get94btrad(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":94B:",":TRAD//",null);
        return formatField(field);
    }

    private String get95ppset(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":95B:",":PSET//",null);
        return formatField(field);
    }

    private String get19asett(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":19A:",":SETT//",null);
        return formatField(field);
    }

    private String get23gfunction(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":23G:","",null);
        return formatField(field);
    }

    private String get20cprev(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":20C:",":PREV//",null);
        return formatField(field);
    }

    private String get35bisin(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":35B:",":ISIN//",null);
        return formatField(field);
    }

    private String get95pacow(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":95P:",":ACOW//",null);
        return formatField(field);
    }

    private String get97bsafe(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":97B:",":SAFE//",null);
        return formatField(field);
    }

    private String get22fsetr(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":22F:",":SETR//",null);
        return formatField(field);
    }

    private String get22fstco(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":22F:",":STCO//",null);
        return formatField(field);
    }

    private String get95pdeag(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":95P:",":DEAG//",null);
        return formatField(field);
    }

    private String get95preag(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":95P:",":REAG//",null);
        return formatField(field);
    }

    private String get95qsell(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":95Q:",":SELL//",null);
        return formatField(field);
    }

    private String get95qdecu(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":95Q:",":DECU//",null);
        return formatField(field);
    }

    private String get95qrecu(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":95Q:",":RECU//",null);
        return formatField(field);
    }

    private String get95rbuyr(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":95R:",":BUYR//",null);
        return formatField(field);
    }

    private String get22hpaym(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":22H:",":PAYM//",null);
        return formatField(field);
    }

    private String get22hrede(SwiftMessage mess){
        SwiftFieldMessage field=mess.getSwiftField(":22H:",":REDE//",null);
        return formatField(field);
    }


    private String formatField(SwiftFieldMessage field){
        return Optional.ofNullable(field).map(SwiftFieldMessage::getValue)
                .map(value->value.substring(value.lastIndexOf("/") + 1).trim())
                .orElse("");
    }


    private Object getCustomValue(BOMessage message, String columnId){
        Object result=null;
        String methodName="get"+columnId.replaceAll("\\s+", "").toLowerCase();
        try {
            Method method=this.getClass().getDeclaredMethod(methodName,BOMessage.class);
            result=method.invoke(this,message);
        } catch (IllegalAccessException| InvocationTargetException | NoSuchMethodException exc) {
            Log.debug(this.getClass().getSimpleName(),"Custom method for column "+columnId+" does not exist");
        }
        return result;
    }

    public Vector<String> getSwiftColumns() {
        return swiftColumns;
    }

    public BOMessage getBOMessage(Matchable matchable) throws RemoteException {
        long targetMsgId=CoreAPI.getId(matchable.getObjectDescription());
        long cacheMsgId= cachedMsg.getCurrentMsgLongId();
        if(targetMsgId!=cacheMsgId){
            BOMessage targetMsg=RemoteAPI.getMessage(DSConnection.getDefault().getRemoteBackOffice(), targetMsgId);
            this.cachedMsg=new ParsedSwiftCache(null,targetMsg);
        }
        return this.cachedMsg.boMessage;
    }

    private static class ParsedSwiftCache{
        SwiftMessage parsedMsg;
        BOMessage boMessage;

        ParsedSwiftCache(SwiftMessage parsedMsg, BOMessage boMessage){
            this.parsedMsg=parsedMsg;
            this.boMessage=boMessage;
        }

        long getCurrentMsgLongId(){
            return Optional.ofNullable(this.boMessage).map(BOMessage::getLongId).orElse(0L);
        }
    }
}
