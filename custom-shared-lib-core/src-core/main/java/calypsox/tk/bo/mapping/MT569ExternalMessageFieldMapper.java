package calypsox.tk.bo.mapping;

import com.calypso.tk.bo.ExternalMessageField;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

public class MT569ExternalMessageFieldMapper extends com.calypso.tk.bo.mapping.MT569ExternalMessageFieldMapper{

    public Set<ExternalMessageField> getMappableFields(){
        Set<ExternalMessageField> fields = new HashSet();
        fields = super.getMappableFields();
        fields.add(new ExternalMessageField(0, "Exposure Amount"));

        return fields;
    }

}
