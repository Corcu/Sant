package calypsox.tk.event;

import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventUpload;

/**
 * @author aalonsop
 *
 * This class must be untouched. It must be extended with the needed name to filter the desired gateway's events.
 * F.e. If we want to allow MCLyncs gateway events to be consumed, a class called PSEventUploadMCLyncsEventFilter
 * and extending this one has to be created. This new class wont contain any logic.
 *
 */
public abstract class PSEventUploadEventFilter implements EventFilter {


    @Override
    public boolean accept(PSEvent event) {
       boolean res=false;
       if(event instanceof PSEventUpload){
           PSEventUpload eventUpload=(PSEventUpload) event;
            res=findGatewayFromName().equals(eventUpload.getGateway());
       }
       return res;
    }


    /**
     *
     * @return ClassName's embedded gateway's name
     */
    private String findGatewayFromName(){
        String className=this.getClass().getSimpleName();
        className=className.replace(PSEventUpload.class.getSimpleName(),"");
        className=className.replace(EventFilter.class.getSimpleName(),"");
        return className;
    }
}
