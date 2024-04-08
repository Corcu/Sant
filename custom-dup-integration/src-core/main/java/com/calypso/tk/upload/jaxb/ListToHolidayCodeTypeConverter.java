package com.calypso.tk.upload.jaxb;

import com.calypso.tk.core.Util;
import com.github.dozermapper.core.DozerConverter;

import java.util.List;

/**
 * @author aalonsop
 */
public class ListToHolidayCodeTypeConverter extends DozerConverter<List, HolidayCodeType> {

    public ListToHolidayCodeTypeConverter() {
        super(List.class, HolidayCodeType.class);
    }

    @Override
    public HolidayCodeType convertTo(List source, HolidayCodeType destination) {
        HolidayCodeType result=new HolidayCodeType();
        if(isStringTypeList(source)){
            result.holiday=source;
        }
        return result;
    }

    @Override
    public List convertFrom(HolidayCodeType source, List destination) {
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
