package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;
import org.jfree.util.Log;

import java.lang.reflect.InvocationTargetException;

public class EmailDataFactory {

    public EmailDataBuilder getEmailData(BOMessage message) {
        String className = message.getTemplateName();
        className = className.substring(0, className.lastIndexOf('.'));
        EmailDataBuilder emailDataBuilder = null;
        try {
            Object o = Class.forName("calypsox.tk.emailnotif." + className).getDeclaredConstructor(BOMessage.class).newInstance(message);
            if (o instanceof EmailDataBuilder) {
                emailDataBuilder = (EmailDataBuilder) o;
            }

        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            String error = "[" + this.getClass() + "]" + " Error, cant find any " + message.getTemplateName() + " class ";
            Log.error(error, e);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            String error = "[" + this.getClass() + "]" + " Error, cant not instantiate " + className + " class";
            Log.error(error, e);
        }
        return emailDataBuilder;
    }
}
