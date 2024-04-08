package calypsox.tk.bo.mapping;


import calypsox.tk.bo.mapping.common.CustomSecuritySwiftFieldMapper;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.util.MessageParseException;

/**
 * @author aalonsop
 */
public class MT546ExternalMessageFieldMapper extends com.calypso.tk.bo.mapping.MT546ExternalMessageFieldMapper
        implements CustomSecuritySwiftFieldMapper {

    @Override
    public String getReference(final SwiftMessage swift, final String reference) throws MessageParseException {
        return parseRelatedMessageReference(super.getReference(swift, reference));
    }
}
