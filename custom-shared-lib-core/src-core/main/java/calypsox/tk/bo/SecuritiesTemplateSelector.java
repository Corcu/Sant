package calypsox.tk.bo;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.util.Arrays;
import java.util.List;

public class SecuritiesTemplateSelector extends com.calypso.tk.bo.SecuritiesTemplateSelector {

    private final List<String> CORETEMPLATES = Arrays.asList("MT542", "MT543");

    public String getTemplate(Trade trade, BOMessage message, String name) {
        String template = null;
        String coreTemplate = super.getTemplate(trade, message, name);

        if (!Util.isEmpty(coreTemplate)) {
            if (message.getProductType().equalsIgnoreCase("SecLending") && CORETEMPLATES.contains(coreTemplate)) {
                template = coreTemplate.concat(message.getProductType());
            }
        }
        if (!Util.isEmpty(template)) {
            return template;
        }
        return coreTemplate;
    }

}
