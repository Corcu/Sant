package calypsox.tk.util.swiftparser;

import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 *
 */
public class MT56XMsgFilter {
    String type = "";
    String value = "";
    List<MT56XInclude> includeExceptions = new ArrayList<>();

    /**
     * accept
     *
     * @param swiftMessage swiftMessage
     * @return {@link boolean}
     */
    public boolean accept(SwiftMessage swiftMessage){
        boolean accept = true;

        if("BIC".equalsIgnoreCase(this.type)){
            String bicSenderCode = swiftMessage.getSender();
            if(getBicCode(value).equalsIgnoreCase(getBicCode(bicSenderCode))){
                accept = false;
            }
        }else if("REC_BIC".equalsIgnoreCase(this.type)) {
            String bicReceiverCode = swiftMessage.getReceiver();
            if (getBicCode(value).equalsIgnoreCase(getBicCode(bicReceiverCode))) {
                accept = false;
            }
        } else {
            String swiftFieldValue = getSwiftField(this.type, swiftMessage);
            if(this.value.equalsIgnoreCase(swiftFieldValue)){
                accept = false;
            }
        }

        accept = validateIncludeExceptions(accept,swiftMessage);

        return accept;
    }

    /**
     * get bic code
     *
     * @param code code
     * @return {@link String}
     * @see String
     */
    private String getBicCode(String code){
        return StringUtils.left(code, 8);
    }

    private boolean validateIncludeExceptions(boolean acc, SwiftMessage swiftMessage){
        AtomicBoolean accept = new AtomicBoolean(acc);
        includeExceptions.forEach(incl-> {
            String swiftFieldValue = getSwiftField(incl.getType(), swiftMessage);
            if(incl.getValues().stream().anyMatch(valuesToValidate -> valuesToValidate.equalsIgnoreCase(swiftFieldValue))){
                accept.set(true);
            }
        });
        return accept.get();
    }

    /**
     * get swift field
     *
     * @param tagType tagType
     * @param swiftMessage swiftMessage
     * @return {@link String}
     * @see String
     */
    private static String getSwiftField(String tagType,SwiftMessage swiftMessage){
        if(!Util.isEmpty(tagType)){
            List<String> tagQualifier = Pattern.compile("\\.").splitAsStream(tagType).collect(Collectors.toList());
            SwiftFieldMessage swiftField = swiftMessage.getSwiftField(":"+tagQualifier.get(0)+":", ":"+tagQualifier.get(1), null);
            return getTagValue(swiftField);
        }
        return "";
    }

    /**
     * get tag value
     *
     * @param swiftField swiftField
     * @return {@link String}
     * @see String
     */
    private static String getTagValue(SwiftFieldMessage swiftField){
        if(null!=swiftField){
            try{
                Pattern datePattern = Pattern.compile("//(.*)");
                Matcher murStartMatcher = datePattern.matcher(swiftField.getValue());
                if (murStartMatcher.find()) {
                    return murStartMatcher.group(1);
                }
            }catch (Exception e){
                Log.error(MT56XMsgFilter.class.getName(),"Regex Error: " +e);
            }
        }
        return "";
    }



    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<MT56XInclude> getIncludeExceptions() {
        return includeExceptions;
    }

    public void setIncludeExceptions(List<MT56XInclude> includeExceptions) {
        this.includeExceptions = includeExceptions;
    }

}
