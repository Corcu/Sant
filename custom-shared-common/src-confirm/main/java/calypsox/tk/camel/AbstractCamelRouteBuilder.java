package calypsox.tk.camel;
import org.apache.camel.builder.RouteBuilder;

/**
 * @author aalonsop
 */
public abstract class AbstractCamelRouteBuilder extends RouteBuilder {

    /**
     *
     * @param Flow direction (SEND,RECEIVE)
     * @return Camel Route's name
     */
    protected final String buildRouteName(String direction){
        String routeBuilderName= RouteBuilder.class.getSimpleName();
        return this.getClass().getSimpleName()
                .replaceAll(routeBuilderName,"_"+direction)
                .toUpperCase();
    }

    public final String getCamelComponentName(){
        String routeBuilderName=RouteBuilder.class.getSimpleName();
        return this.getClass().getSimpleName()
                .replaceAll(routeBuilderName,"")
                .toUpperCase();
    }
}
