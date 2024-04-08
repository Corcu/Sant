package calypsox.camel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author aalonsop
 */
public class CamelContextBeanPool {

    private static final CamelContextBeanPool instance=new CamelContextBeanPool();

    private final ConcurrentMap<String, CamelContextBean> beans = new ConcurrentHashMap<>();

    private CamelContextBeanPool() {
        // private
    }

    public static CamelContextBeanPool getInstance() {
        return instance;
    }

    public CamelContextBean getCamelContextBean(String camelContextName) {
        return beans.get(camelContextName);
    }

    public void putCamelContextBean(String beanName,CamelContextBean camelContextBean) {
        beans.put(beanName, camelContextBean);
    }

    public void removeCamelContextBean(String beanName,CamelContextBean camelContextBean) {
        beans.remove(beanName, camelContextBean);
    }
}
