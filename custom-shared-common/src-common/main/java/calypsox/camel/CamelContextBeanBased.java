package calypsox.camel;

import org.apache.camel.builder.RouteBuilder;

/**
 * @author aalonsop
 */
public interface CamelContextBeanBased {

    RouteBuilder getCamelRouteBuilder();

     default String getCamelContextBeanName(){
         String routeBuilderName=RouteBuilder.class.getSimpleName();
         return getCamelRouteBuilder().getClass().getSimpleName()
                 .replaceAll(routeBuilderName,"")
                 .toUpperCase();
    }
}
