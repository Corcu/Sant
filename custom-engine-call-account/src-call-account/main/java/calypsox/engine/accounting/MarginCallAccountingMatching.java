package calypsox.engine.accounting;

import com.calypso.engine.accounting.AccountingMatching;
import com.calypso.engine.accounting.AccountingMatchingUtil;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOPosting;

import java.util.Hashtable;

import static calypsox.engine.accounting.SantAccountingMatchingUtil.getInstance;

public class MarginCallAccountingMatching implements AccountingMatching {
    AccountingMatchingUtil util = new AccountingMatchingUtil();

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
        getInstance().excludeCreAtt(this.getClass().getSimpleName(),oldCreAttributes);
        getInstance().excludeCreAtt(this.getClass().getSimpleName(),newCreAttributes);
        return util.matchCreAttributes(oldCre,oldCreAttributes,newCre,newCreAttributes);
    }
}
