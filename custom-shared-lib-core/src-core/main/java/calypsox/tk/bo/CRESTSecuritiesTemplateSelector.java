package calypsox.tk.bo;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class CRESTSecuritiesTemplateSelector extends com.calypso.tk.bo.SecuritiesTemplateSelector {

    private final List<String> CREST_TEMPLATES = Arrays.asList("MT540","MT541","MT542","MT543");

    public String getTemplate(Trade trade, BOMessage message, String name) {
        String template = null;
        String coreTemplate = super.getTemplate(trade, message, name);

        if (!Util.isEmpty(coreTemplate)) {
            if(CREST_TEMPLATES.contains(coreTemplate)){
                template = coreTemplate.concat("CREST");
            }
        }
        if (!Util.isEmpty(template)) {
            return template;
        }
        return coreTemplate;
    }

}