package calypsox.ctm.camel;

import calypsox.ctm.util.CTMUploaderConstants;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEventUpload;
import com.calypso.tk.service.DSConnection;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * @author aalonsop
 */
public abstract class PSEventUploadCamelPublisher<T extends PSEventUpload> implements Processor {


    @Override
    public void process(Exchange exchange) throws Exception {
        Object publishedObject = Optional.ofNullable(exchange.getIn())
                .map(in -> in.getBody(String.class))
                .filter(msg -> !msg.isEmpty())
                .map(this::createEvent)
                .map(this::publishEvent)
                .orElseGet(() -> UploaderExternalErrorHandler.createAndPublishTask(exchange));
        logPublishResult(publishedObject);
    }

    //Calc
    private T createEvent(String xmlData) {
        Class<T> eventUploadClass = getPSEventUploadClassFromType();
        T eventUploadInstance = null;
        try {
            eventUploadInstance = eventUploadClass.newInstance();
            eventUploadInstance.setMessage(xmlData);
            eventUploadInstance.setGateway(getGateway());
            eventUploadInstance.setMessageSource(eventUploadInstance.getGateway());
            eventUploadInstance.setMessageFormat(CTMUploaderConstants.UPLOADERXML_STR);
        } catch (InstantiationException | IllegalArgumentException | IllegalAccessException exc) {
            Log.error(this, exc.getCause());
        }
        return eventUploadInstance;
    }

    //Action
    private Object publishEvent(PSEventUpload event) {
        try {
            long evId = DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
            event.setLongId(evId);
        } catch (CalypsoServiceException exc) {
            Log.error(this.getClass().getSimpleName(), "Couldn't publish event for msg: "
                    + event.getMessage());
        }
        return event;
    }

    private CalypsoServiceException throwException(Exchange exchange) {
        String msg = "Couldn't publish PSEventUpload event for the following msg:"
                + System.lineSeparator()
                + Optional.of(exchange).map(Exchange::getIn)
                .map(in -> in.getBody(String.class)).orElse("");
        return new CalypsoServiceException(msg);
    }

    final String getGateway() {
        return Optional.of(getPSEventUploadClassFromType()).map(Class::getSimpleName)
                .map(name -> name.replace(PSEventUpload.class.getSimpleName(), ""))
                .orElse("");
    }

    /**
     * IDE's says is unchecked,
     * but any Class<?> taken from its generic type is also Class<T>
     *
     * @return
     */
    private Class<T> getPSEventUploadClassFromType() {
        Type superType = getClass().getGenericSuperclass();
        ParameterizedType paramType = (ParameterizedType) superType;
        return (Class<T>) Optional.ofNullable(paramType)
                .map(ParameterizedType::getActualTypeArguments)
                .map(types -> types[0])
                .filter(type -> type instanceof Class)
                .orElse(PSEventUpload.class);

    }

    private void logPublishResult(Object publishedObject) {
        Optional.ofNullable(publishedObject)
                .ifPresent(obj -> Log.system("UPLOADER",
                        "New " + obj.getClass().getSimpleName() + " published with id: " + getIdFromCreatedObject(obj)));
    }

    private long getIdFromCreatedObject(Object createdObject) {
        long id = 0;
        if (createdObject instanceof PSEventUpload) {
            id = ((PSEventUpload) createdObject).getLongId();
        } else if (createdObject instanceof Task) {
            id = ((Task) createdObject).getId();
        }
        return id;
    }
}
