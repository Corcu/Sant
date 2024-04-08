package calypsox.tk.util.swiftparser;

import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.CA;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.ProcessStatusException;

import java.util.Vector;

public class MT564EquityMatcher extends com.calypso.tk.util.swiftparser.MT564EquityMatcher {

    @Override
    protected Product getUnderlyingProduct(SwiftMessage swiftMessage) throws MessageParseException, ProcessStatusException {

        Vector<SwiftFieldMessage> swiftMessageFields = swiftMessage.getFields();
        Vector<SwiftFieldMessage> outputMessageFields = new Vector<SwiftFieldMessage>();
        for(int i=0; i<swiftMessageFields.size(); i++) {
            if (!swiftMessageFields.get(i).getValue().startsWith(":CASH//") && !swiftMessageFields.get(i).getValue().startsWith(":TAX")) {
                outputMessageFields.add(swiftMessageFields.get(i));
            }
        }
        swiftMessage.setFields(outputMessageFields);
        return super.getUnderlyingProduct(swiftMessage);
    }

    @Override
    public CA indexCA(SwiftMessage swiftMessage) throws MessageParseException, ProcessStatusException {
        if(MT56XFilterUtil.acceptSwiftMsg(swiftMessage)){
            return super.indexCA(swiftMessage);
        }else {
            return null;
        }
    }
}
