package calypsox.engine.accounting;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author acd
 */
public class SantAccountingMatchingUtil {

    private static SantAccountingMatchingUtil instance;

    private static final String EXCLUDE_CRE_ATT_VALIDATION = "ExcludeCreAttValidation";
    private static final String ACCOUNTING_MATCHING = "AccountingMatching";

    private static final String DELIMITER = ",";

    public static SantAccountingMatchingUtil getInstance() {
        if (instance == null) {
            instance = new SantAccountingMatchingUtil();
        }
        return instance;
    }

    /**
     * @param classname
     * @param oldCreAttributes
     */
    public void excludeCreAtt(String classname , Hashtable oldCreAttributes){
        final List<String> excludeAttributes = getExcludeAttributes(classname);
        if(!Util.isEmpty(oldCreAttributes) && !Util.isEmpty(excludeAttributes)){
            try{
                Map<?, ?> oldAttributes = ((Hashtable<?, ?>) oldCreAttributes).entrySet().stream()
                        .filter(entry -> !excludeAttributes.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                //Replace Attributes
                if(!Util.isEmpty(oldAttributes)){
                    oldCreAttributes.clear();
                    oldCreAttributes.putAll(oldAttributes);
                }
            }catch (Exception e){
                Log.error(this,"Error excluding attributes");
            }
        }
    }

    /**
     * @param oldCreAttributes
     */
    public void excludeCreAtt(Hashtable oldCreAttributes,List<String> customExcludeAttributes){
        final List<String> excludeAttributes = customExcludeAttributes;

        if(!Util.isEmpty(oldCreAttributes) && !Util.isEmpty(excludeAttributes)){
            try{
                Map<?, ?> oldAttributes = ((Hashtable<?, ?>) oldCreAttributes).entrySet().stream()
                        .filter(entry -> !excludeAttributes.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                //Replace Attributes
                if(!Util.isEmpty(oldAttributes)){
                    oldCreAttributes.clear();
                    oldCreAttributes.putAll(oldAttributes);
                }
            }catch (Exception e){
                Log.error(this,"Error excluding attributes");
            }
        }
    }
    /**
     * @param productType
     * @return Exclude Attributes define on DV - ExcludeCreAttValidation
     */
    public List<String> getExcludeAttributes(String productType){
        List<String> excludeAtt = new ArrayList<>();
        if(productType.contains(ACCOUNTING_MATCHING)){
            String type = productType.replace(ACCOUNTING_MATCHING, "").trim();
            String domainName = EXCLUDE_CRE_ATT_VALIDATION + "." + type;
            Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(), domainName);
            if(!Util.isEmpty(domainValues)){
                excludeAtt.addAll(domainValues);
            }
        }
        return excludeAtt;
    }
}
