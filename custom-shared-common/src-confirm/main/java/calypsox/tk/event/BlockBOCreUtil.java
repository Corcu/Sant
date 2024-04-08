package calypsox.tk.event;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Optional;
import java.util.Vector;

public class BlockBOCreUtil {

    public static boolean checkEvent(BOCre event, String engineName1, String engineName2, String domainName) {
        boolean res = true;
        Vector<String> boCreAccountingRuleKafka = LocalCache.getDomainValues(DSConnection.getDefault(), domainName);
        String accountingRule = Optional.ofNullable(event).map(String -> getAccountingRuleName(event)).orElse("");

        if(!Util.isEmpty(boCreAccountingRuleKafka)) {
            if(validateAccountingRule(accountingRule, boCreAccountingRuleKafka)){
                if(engineName1.equalsIgnoreCase(engineName2))
                    res = true;
                else
                    res = false;
            } else {
                if(engineName1.equalsIgnoreCase(engineName2))
                    res = false;
                else
                    res = true;
            }

        }
        return res;
    }

    private static String getAccountingRuleName(BOCre boCre) {
        int ruleId = boCre.getAccountingRuleId();
        try {
            if (ruleId > 0) {
                AccountingRule accRule = DSConnection.getDefault().getRemoteAccounting().getAccountingRule(ruleId);
                return accRule.getName();
            }
        } catch (Exception e) {
            Log.error(BlockBOCreUtil.class, "Could not get the accounting rule " + ruleId);
        }
        return null;
    }

    private static boolean validateAccountingRule(String accountingRule, Vector<String> boCreAccountingRuleKafka) {
        boolean out = false;
        if(!Util.isEmpty(accountingRule)){
            for(String accountingRuleDV : boCreAccountingRuleKafka){
                if (!out)
                    out = ! (accountingRuleDV ==null) && accountingRuleDV.contains(accountingRule);
            }
        }
        return out;
    }

}
