package com.calypso.tk.upload.jaxb;

import com.calypso.tk.core.Util;
import com.github.dozermapper.core.DozerConverter;

import java.util.List;

/**
 * @author aalonsop
 */
public class ListToStringListTypeConverter extends DozerConverter<List, StringListType> {

    public ListToStringListTypeConverter() {
        super(List.class, StringListType.class);
    }

    @Override
    public StringListType convertTo(List source, StringListType destination) {
        StringListType result=new StringListType();
        if(isStringTypeList(source)){
            result.value=source;
        }
        return result;
    }

    @Override
    public List convertFrom(StringListType source, List destination) {
        return null;
    }

    private boolean isStringTypeList(List source){
        boolean res=true;
        if(!Util.isEmpty(source)){
            res=source.get(0) instanceof String;
        }
        return res;
    }
}