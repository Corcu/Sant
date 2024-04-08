package calypsox.engine.accounting;

import com.calypso.engine.accounting.AccountingMatching;
import com.calypso.engine.accounting.AccountingMatchingUtil;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.DomainValues;

import java.util.Hashtable;
import java.util.List;

import static calypsox.engine.accounting.SantAccountingMatchingUtil.getInstance;

public class RepoAccountingMatching implements AccountingMatching {
    AccountingMatchingUtil util = new AccountingMatchingUtil();
    private static final String PARTENON_ID = "PartenonAccountingID";

    @Override
    public boolean match(BOPosting oldPosting, BOPosting newPosting) {
        return util.match(oldPosting,newPosting);
    }

    @Override
    public boolean matchPostingAttributes(BOPosting oldPosting, Hashtable oldPostingAttributes, BOPosting newPosting, Hashtable newPostingAttributes) {
        return util.matchPostingAttributes(oldPosting,oldPostingAttributes,newPosting,newPostingAttributes);
    }

    @Override
    public boolean match(BOCre oldCre, BOCre newCre) {
        return util.match(oldCre,newCre);
    }

    @Override
    public boolean matchCreAttributes(BOCre oldCre, Hashtable oldCreAttributes, BOCre newCre, Hashtable newCreAttributes) {
        List<String> excludeAttributes = getInstance().getExcludeAttributes(this.getClass().getSimpleName());

        final String activated = DomainValues.comment("CodeActivationDV", "CreMatchingPartenonChange");
        if(!Util.isEmpty(activated) && Boolean.parseBoolean(activated)
                && oldCreAttributes.containsKey(PARTENON_ID)
                && !"null".equalsIgnoreCase(oldCreAttributes.get(PARTENON_ID).toString())){
            excludeAttributes.remove(PARTENON_ID);
        }


        getInstance().excludeCreAtt(oldCreAttributes,excludeAttributes);
        getInstance().excludeCreAtt(newCreAttributes,excludeAttributes);

        return util.matchCreAttributes(oldCre,oldCreAttributes,newCre,newCreAttributes);

    }
}
