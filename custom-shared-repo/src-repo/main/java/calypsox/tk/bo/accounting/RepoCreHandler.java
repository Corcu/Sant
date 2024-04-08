package calypsox.tk.bo.accounting;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

public class RepoCreHandler extends com.calypso.tk.bo.accounting.RepoCreHandler {

    private static final String PARTENON_ID = "PartenonAccountingID";
    private static final String OLD_PARTENON_ID = "OldPartenonAccountingID";
    @Override
    public void fillAttributes(BOCre cre, Trade trade, PSEvent event, AccountingEventConfig eventConfig, AccountingRule rule, AccountingBook accountingBook) {
        super.fillAttributes(cre, trade, event, eventConfig, rule, accountingBook);
        try{
            final List<String> acceptedEventList = Util.stringToList("COT,COT_REV");
            if(Optional.ofNullable(cre).isPresent()){

                final String activatePartenon = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "CrePartenonIdAttribute");
                if(!Util.isEmpty(activatePartenon) && Boolean.parseBoolean(activatePartenon)){
                    cre.addAttribute(PARTENON_ID,trade.getKeywordValue(PARTENON_ID));
                }

                final JDate effectiveDate = cre.getEffectiveDate();
                final String date = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "RepoAttributesActivationDate");
                if(!Util.isEmpty(date)){
                    final JDate activationDate = JDate.valueOf(date);
                    if(acceptedEventList.stream().anyMatch(Optional.of(cre).map(BOCre::getEventType).orElse("")::equalsIgnoreCase) && Optional.ofNullable(trade).isPresent() && effectiveDate.after(activationDate)){
                        final JDatetime repoTradeDate = trade.getTradeDate();
                        final JDate repoStartDate = Optional.of(trade.getProduct()).filter(Repo.class::isInstance).map(Repo.class::cast).map(Repo::getStartDate).orElse(null);
                        if(null!=repoTradeDate && null!=repoStartDate ){
                            cre.addAttribute("TradeDate",Util.dateToString(repoTradeDate.getJDate(TimeZone.getDefault())));
                            cre.addAttribute("StartDate",Util.dateToString(repoStartDate));
                        }
                    }
                }
            }
        }catch (Exception e){
            Log.error(this,"Error set TradeDate and StartDate on Cre " + e);
        }
        if (cre != null & trade!=null &&  "WRITE_OFF".equals(cre.getEventType())) {
           cre.addAttribute(PARTENON_ID, trade.getKeywordValue(PARTENON_ID));
           if( Util.isEmpty(trade.getKeywordValue(PARTENON_ID)) )
               cre.addAttribute(PARTENON_ID, trade.getKeywordValue(OLD_PARTENON_ID));

        }
    }

    public void getWRITE_OFF(Trade trade, Trade otherTrade, PSEvent event, AccountingEventConfig eventConfig, BOCre cre, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        if (event instanceof PSEventTransfer && !Status.S_SETTLED.equals (((PSEventTransfer)event).getStatus() )) {
            cre.initAmounts();
        }
    }
}
