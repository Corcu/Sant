package com.calypso.tk.upload.jaxb;

import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.RateIndexDefaults;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.github.dozermapper.core.DozerConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class RateIndexAttributeConverter extends DozerConverter<RateIndexDefaults, CalypsoRateIndex> {

    public RateIndexAttributeConverter() {
        super(RateIndexDefaults.class, CalypsoRateIndex.class);
    }

    @Override
    public CalypsoRateIndex convertTo(RateIndexDefaults rateIndexDefaults, CalypsoRateIndex calypsoRateIndex) {
        if(calypsoRateIndex!=null) {
            List<Attribute> attrList=new ArrayList<>();
            Vector<String> attributeNames = LocalCache.getDomainValues(DSConnection.getDefault(), "rateIndexAttributes");
            for (String attrName : attributeNames) {
                Attribute attribute=new Attribute();
                attribute.setAttributeName(attrName);
                attribute.setAttributeValue(rateIndexDefaults.getAttribute(attrName));
                if(!Util.isEmpty(attribute.getAttributeValue())) {
                    attrList.add(attribute);
                }
            }
            Attributes attributes=new Attributes();
            attributes.attribute=attrList;
            calypsoRateIndex.setAttributes(attributes);
        }
        return  calypsoRateIndex;
    }

    @Override
    public RateIndexDefaults convertFrom(CalypsoRateIndex calypsoRateIndex, RateIndexDefaults rateIndexDefaults) {
        return null;
    }
}

