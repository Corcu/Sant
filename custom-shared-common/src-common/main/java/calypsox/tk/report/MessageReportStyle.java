package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class MessageReportStyle extends com.calypso.tk.report.MessageReportStyle {

    public static String SAFEACC_TAG="97A SAFE";
    public static String PTYB="95Q PTYB";
    public static String PREP="98E PREP";
    public static String TEXA="19A TEXA";

    private transient ParsedSwiftCache cachedMsg= new ParsedSwiftCache(0, null);

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors){
        Object result =  super.getColumnValue(row, columnId, errors);
        if (result==null && null!=row){
            BOMessage message = (BOMessage)row.getProperty("BOMessage");
            result = getCustomValue(message, columnId);
        }
        return result;
    }

    private SwiftMessage buildSwiftMessage(BOMessage boMessage){
        SwiftMessage swiftMessage= Optional.ofNullable(cachedMsg.parsedMsg).orElse(new SwiftMessage());
        try {
            if(cachedMsg.parsedMsg==null||cachedMsg.msgId!=boMessage.getLongId()){
                AdviceDocument doc=DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(boMessage.getLongId(),new JDatetime());
                String swiftString="";
                if(doc!=null){
                    swiftString=doc.getDocument().toString();
                }
                swiftMessage.parse(swiftString, boMessage.getGateway());
                this.cachedMsg= new ParsedSwiftCache(boMessage.getLongId(), swiftMessage);
            }
        } catch (CalypsoServiceException exc) {
            Log.warn(this.getClass().getSimpleName(),exc.getMessage(),exc.getCause());
        }
        return swiftMessage;
    }

    private String get97asafe(BOMessage boMessage){
        SwiftMessage mess=buildSwiftMessage(boMessage);
        SwiftFieldMessage field=mess.getSwiftField(":97A:",":SAFE//",null);
        return formatField(field);
    }

    private String get95qptyb(BOMessage boMessage){
        SwiftMessage mess=buildSwiftMessage(boMessage);
        SwiftFieldMessage field=mess.getSwiftField(":95Q:",":PTYB//",null);
        return formatField(field);
    }

    private String get98eprep(BOMessage boMessage){
        SwiftMessage mess=buildSwiftMessage(boMessage);
        SwiftFieldMessage field=mess.getSwiftField(":98E:",":PREP//",null);
        return formatField(field);
    }

    private String get19atexa(BOMessage boMessage){
        SwiftMessage mess=buildSwiftMessage(boMessage);
        SwiftFieldMessage field=mess.getSwiftField(":19A:",":TEXA//",null);
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
    @Override
    public TreeList getTreeList() {
        TreeList swiftList=new TreeList("Swift");
        swiftList.add(SAFEACC_TAG);
        swiftList.add(PTYB);
        swiftList.add(PREP);
        swiftList.add(TEXA);

        TreeList fullList=super.getTreeList();
        fullList.add(swiftList);
        return fullList;
    }

    private static class ParsedSwiftCache{
        long msgId;
        SwiftMessage parsedMsg;

         ParsedSwiftCache(long msgId,SwiftMessage parsedMsg){
            this.msgId=msgId;
            this.parsedMsg=parsedMsg;
        }
    }

}

