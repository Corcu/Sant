package calypsox.tk.bo.fiflow.factory;

/**
 * @author aalonsop
 */
public abstract class FIFlowMsgBuilder<T> {

    T messageBean;
    
    public abstract T build();
}
