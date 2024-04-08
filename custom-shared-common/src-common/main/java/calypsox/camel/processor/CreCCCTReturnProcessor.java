package calypsox.camel.processor;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Optional;

public class CreCCCTReturnProcessor extends CreMicReturnProcessor {

    public static final int CRE_ID_START = 0;
    public static final int CRE_ID_LENGTH = 15;
    public static final int RETURN_CODE_START = 15;
    public static final int RETURN_CODE_LENGTH = 2;
    public static final int RETURN_DESC_START = 17;
    public static final int RETURN_DESC_LENGTH = 60;


    @Override
    public void process(Exchange exchange) throws Exception {
        String returnMessage = exchange.getIn().getBody(String.class);
        Long creId = (Long.parseLong(returnMessage.substring(
                        CRE_ID_START, CRE_ID_START + CRE_ID_LENGTH).trim()));

        String status = (returnMessage.substring(
                RETURN_CODE_START, RETURN_CODE_START + RETURN_CODE_LENGTH));

        String comment = (returnMessage.substring(
                RETURN_DESC_START, RETURN_DESC_START +RETURN_DESC_LENGTH));

        final Optional<BOCre> boCre = Optional.ofNullable(loadCre(creId));
        boCre.ifPresent(creToSave -> {
            creToSave.addAttribute(BOCreConstantes.SENT, status);
            if (!Util.isEmpty(comment)) {
                creToSave.addAttribute("COMMENT:", comment);
            }
            Log.system(this.getClass().getName(), "Saving response from MIC for Cre id: " + creToSave.getId() + " with accountBalance: " + creToSave.getAttributeValue("accountBalance"));
            saveBOCre(creToSave);
        });


    }
}
