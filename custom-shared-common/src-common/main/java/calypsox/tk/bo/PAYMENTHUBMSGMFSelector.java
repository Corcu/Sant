package calypsox.tk.bo;


import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.bo.MessageFormatterUtil;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.InstantiateUtil;
import org.apache.commons.io.FilenameUtils;


public class PAYMENTHUBMSGMFSelector extends MessageFormatterUtil {


    @Override
    public MessageFormatter getMessageFormatter(BOMessage message) {
        final String messageType = message.getMessageType();
        String template = message.getTemplateName();
        MessageFormatter formatter = null;
        // Search for template with extension
        final String ext = FilenameUtils.getExtension(template);
        if (!Util.isEmpty(ext)) {
            template = template.substring(0, template.lastIndexOf(ext)).replaceAll("\\.", "");
            formatter = this.getMessageFormatter(template, "");
        } else {
            // Search for template
            template = PaymentsHubUtil.getTransformedTemplateName(template);
            formatter = this.getMessageFormatter(template, "");
            if (!(formatter != null && formatter instanceof PAYMENTHUBMSGMessageFormatter)) {
                // Search from messageType
                formatter = this.getMessageFormatter("", messageType);
            }
        }
        return formatter;
    }


    @Override
    public MessageFormatter getMessageFormatter(final String productType, final String messageType) {
        MessageFormatter instance = null;
        final String name = messageType + productType;
        synchronized (_messageFormatters) {
            instance = (MessageFormatter) _messageFormatters.get(name);
        }
        if (instance != null) {
            return instance;
        } else {
            final String classname = "tk.bo.paymentshub." + name + "MessageFormatter";
            instance = instantiate(name, classname);
            if (instance != null) {
                return instance;
            }
        }
        return super.getMessageFormatter(productType, messageType);
    }


    @SuppressWarnings("unchecked")
    private static MessageFormatter instantiate(final String name, final String classname) {
        MessageFormatter instance = null;
        try {
            instance = (MessageFormatter) InstantiateUtil.getInstance(classname, true);
        } catch (final Exception arg5) {
            Log.error(PAYMENTHUBMSGMFSelector.class, "Error instantiating MessageFormatter " + classname);
        }
        if (instance != null) {
            synchronized (_messageFormatters) {
                _messageFormatters.put(name, instance);
            }
        }
        return instance;
    }


}
