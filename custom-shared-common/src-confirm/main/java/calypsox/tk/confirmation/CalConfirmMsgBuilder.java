package calypsox.tk.confirmation;

import calypsox.tk.confirmation.model.jaxb.CalypsoConfirmationMsgBean;

import java.util.AbstractMap;

/**
 * @author aalonsop
 */
public abstract class CalConfirmMsgBuilder {

    CalypsoConfirmationMsgBean messageBean;

    public CalConfirmMsgBuilder(){
        this.messageBean=new CalypsoConfirmationMsgBean();
    }

    public abstract CalypsoConfirmationMsgBean build();

     void addFieldToMsgBean(Enum<?> fieldName, String fieldValue){
        AbstractMap.SimpleEntry<String, String> field = new AbstractMap.SimpleEntry<>(fieldName.name(), fieldValue);
        messageBean.addElement(field);
    }
}
