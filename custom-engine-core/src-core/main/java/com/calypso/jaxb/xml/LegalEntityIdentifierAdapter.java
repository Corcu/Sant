package com.calypso.jaxb.xml;

import com.calypso.tk.core.LegalEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author aalonsop
 */
public class LegalEntityIdentifierAdapter {

    private final LegalEntityIdentifiers legalEntityIdentifiers;

    public LegalEntityIdentifierAdapter(String leCode){
        Identifier identifier=new Identifier();
        identifier.setCode(leCode);
        identifier.setCodifier("convention");
        List<Identifier> identifierList=new ArrayList<>();
        identifierList.add(identifier);

        this.legalEntityIdentifiers=new LegalEntityIdentifiers();
        this.legalEntityIdentifiers.identifier=identifierList;
        this.legalEntityIdentifiers.type= LegalEntity.class.getName();
    }


    public LegalEntityIdentifiers getLegalEntityIdentifiers() {
        return this.legalEntityIdentifiers;
    }
}
