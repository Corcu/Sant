package calypsox.uploader.calypso.mapping.builder;

import com.calypso.uploader.calypso.mapping.converter.InternalReferenceConverter;

public class CustomInternalReferenceConverter extends InternalReferenceConverter {

    @Override
    public Object convert(Object o, Object o1, Class<?> aClass, Class<?> aClass1) {
        Object internalRef=super.convert(o,o1,aClass,aClass1);
        if(internalRef==null){
            internalRef="";
        }
        return internalRef;
    }
}
