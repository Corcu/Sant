package calypsox.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * @author aalonsop
 */
public enum CamelContextSingleton {

    INSTANCE(new DefaultCamelContext());

    private final CamelContext camelContext;

    CamelContextSingleton(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }
}
