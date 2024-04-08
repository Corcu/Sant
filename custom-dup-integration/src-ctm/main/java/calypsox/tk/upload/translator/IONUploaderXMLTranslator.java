package calypsox.tk.upload.translator;

import calypsox.ctm.util.CTMUploaderConstants;
import com.calypso.tk.upload.jaxb.Keyword;

import java.util.List;

/**
 * @author aalonsop
 */
public class IONUploaderXMLTranslator extends CTMUploaderXMLTranslator{

    @Override
    String getAllocatedFromDetailsKwdFromList(List<Keyword> keywordList) {
        return getKwdFromList(keywordList, CTMUploaderConstants.ALLOCATED_FROM_MX_GLOBALID);

    }
}
