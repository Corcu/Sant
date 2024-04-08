package calypsox.tk.bo.mapping;

import calypsox.tk.bo.mapping.common.CustomSecuritySwiftFieldMapper;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.util.MessageParseException;

public class MT537ExternalMessageFieldMapper extends com.calypso.tk.bo.mapping.MT537ExternalMessageFieldMapper implements CustomSecuritySwiftFieldMapper {
    @Override
    public String getReference(final SwiftMessage swift, final String reference) throws MessageParseException {
        return "ACOW".equals(reference)
                ? parseRelatedMessageReference(super.getReference(swift, reference))
                : super.getReference(swift, reference);
    }
}