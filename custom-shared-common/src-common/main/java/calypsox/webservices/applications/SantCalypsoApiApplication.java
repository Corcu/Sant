package calypsox.webservices.applications;

import calypsox.webservices.annotations.SgtEndpoint;
import com.calypso.tk.core.Log;
import org.jboss.security.PicketBoxLogger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@ApplicationPath("/santcalypsoapi")
public class SantCalypsoApiApplication extends Application {

    private HashSet<Object> singletons = new HashSet<>();

    /**
     * Override constructor of Application to add Singleton.
     */
    public SantCalypsoApiApplication() {
        super();

        PicketBoxLogger.LOGGER.info(String.format("[SGT_API] Registering api %s", this.getClass().getAnnotation(ApplicationPath.class).value()));
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(SgtEndpoint.class));

        Set<BeanDefinition> components = provider.findCandidateComponents("calypsox");
        components.addAll(provider.findCandidateComponents("com/isban"));

        Pattern p = Pattern.compile("\\w+.*\\.jar");

        for (BeanDefinition component : components)
        {

            Log.info(SantCalypsoApiApplication.class, "Found class: "+component.getBeanClassName());
            PicketBoxLogger.LOGGER.info(String.format("[SGT_API] Found class endpoint %s", component.getBeanClassName()));

            try {
                PicketBoxLogger.LOGGER.info(String.format("[SGT_API] Registering class endpoint %s", component.getBeanClassName()));
                Object instance = Class.forName(component.getBeanClassName()).getDeclaredConstructor().newInstance();
                String endpoint = instance.getClass().getAnnotation(Path.class).value();
                this.singletons.add(instance);
                URL location = instance.getClass().getResource('/' + instance.getClass().getName().replace('.', '/') + ".class");
                String jarName = "NotFound";
                if(!Objects.isNull(location)) {
                    Matcher m = p.matcher(location.getPath());

                    while (m.find()) {
                        String[] group = m.group().split("/");
                        jarName = group.length > 0 ? group[group.length-1] : "NotFound";
                    }
                }
                PicketBoxLogger.LOGGER.info(String.format("[SGT_API] Registered class %s from %s -> to endpoint %s", component.getBeanClassName(), jarName, endpoint));
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                     | InvocationTargetException | NoSuchMethodException | SecurityException
                     | ClassNotFoundException e) {
                PicketBoxLogger.LOGGER.error(String.format("[SGT_API] Error registering endpoint %s", component.getBeanClassName()), e);
            }

        }

        PicketBoxLogger.LOGGER.info(String.format("[SGT_API] Done registering api %s", this.getClass().getAnnotation(ApplicationPath.class).value()));

    }

}