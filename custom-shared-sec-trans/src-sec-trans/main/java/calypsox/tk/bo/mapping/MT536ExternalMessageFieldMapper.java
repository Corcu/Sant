package calypsox.tk.bo.mapping;

import com.calypso.elasticsearch.monitoring.Util;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.util.MessageParseException;

public class MT536ExternalMessageFieldMapper extends com.calypso.tk.bo.mapping.MT536ExternalMessageFieldMapper {

    public String getReferenceByName(Object swiftMsg, String reference) throws MessageParseException {
        if ("AgentRef".equals(reference)) {
            SwiftMessage swift = (SwiftMessage) swiftMsg;
            String poAccount = swift.getFieldByType("PO Account");
            String trrf = this.getReferenceByName(swift, swift.getFields(), "TRRF");
            return poAccount + (Util.isEmpty(trrf) ? "" : trrf);
        }
        return super.getReferenceByName(swiftMsg, reference);
    }
}
