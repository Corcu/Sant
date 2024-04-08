package calypsox.tk.bo;


import calypsox.tk.bo.util.PHConstants;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.Formatter;
import com.calypso.tk.bo.FormatterSelector;
import com.calypso.tk.bo.MessageFormatterUtil;


public class PAYMENTHUBFormatterUtil implements FormatterSelector {


    @Override
    public Formatter findFormatter(BOMessage message) {
        final String formatType = message.getFormatType();
        if (PHConstants.FORMAT_TYPE_PAYMENTHUB.equals(formatType)) {
            return MessageFormatterUtil.findMessageFormatter(message);
        }
        return null;
    }


}
