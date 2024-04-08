package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.rule.DisallowChangesTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

public class SantDisallowCommonChangesTradeRule
        extends DisallowChangesTradeRule {
    protected static final String LOG_CATEGORY = "SantDisallowChangesTradeRule";
    protected static final String KW_PARTENON_REQUEST = "PartenonRequest";
    protected static final String KW_PARTENON_MODIF = "PartenonModif";
    protected static final String KW_PARTENON_ALIAS = "PartenonAlias";
    protected static final String KW_PARTENON_REQUESTDATE = "PartenonRequestDate";
    protected static final String S_TRUE = "true";
    public static final String REPROCESS_KW = "Reprocess";
    protected static final String ISIN = "ISIN";
    String partenonRequestValue = "false";

    protected String getLogCategory() {
        return LOG_CATEGORY;
    }
    protected String getRuleName() {
        return "SantDisallowChanges";
    }

    //Values to compare
    String newIsin = "";
    String oldIsin = "";
    JDatetime tradeDate = null;
    JDatetime oldTradeDate = null;
    double quantity = 0.0;
    double oldQuantity = 0.0;
    double nominal = 0.0;
    double oldNominal = 0.0;
    String currency = "";
    String oldCurrency = "";
    double principal = 0.0;
    double oldPrincipal = 0.0;
    JDate startDate = null;
    JDate oldStartDate = null;
    String direction = "";
    String oldDirection = "";
    double dirtyPrice = 0.0;
    double oldDirtyPryce = 0.0;
    double cleanPrice = 0.0;
    double oldCleanPrice = 0.0;


    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
                         Vector excps, Task task, Object dbCon, Vector events) {
        if (trade == null) {
            return true;
        }

        logRuleParams(wc, getRuleName());

        // If it is a reprocess, accept always
        String reprocessKW = trade.getKeywordValue(REPROCESS_KW);
        if (!Util.isEmpty(reprocessKW) && reprocessKW.equalsIgnoreCase(S_TRUE)) {
            Log.info(this, getRuleName() + " " + REPROCESS_KW + " KW is set to TRUE, ignoring all changes in Trade " + trade.getLongId());
            return true;
        }

        // If no Xfer has been settled yet, accept always
        if (!atLeastOneXFerIsSettled(trade, dsCon)) {
            Log.info(this, getRuleName() + " " + " None of the XFers is Settled, ignoring all changes in Trade " + trade.getLongId());
            return true;
        }

        boolean res = super.check(wc, trade, oldTrade, messages, dsCon, excps, task, dbCon, events);
        if (res) {
            Log.info(this, getRuleName() + " Rule has accepted Trade " + trade.getLongId());
        }
        else {
            Log.info(this, getRuleName() + " Rule has NOT accepted Trade " + trade.getLongId());
        }
        return res;
    }

    private boolean atLeastOneXFerIsSettled(Trade trade, DSConnection dsCon) {
        try {
            TransferArray xFers = dsCon.getRemoteBO().getBOTransfers(trade.getLongId());
            for (int i = 0; i < xFers.size(); i++) {
                BOTransfer xfer = xFers.get(i);
                if (xfer.getStatus().equals(Status.S_SETTLED)) {
                    return true;
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, getRuleName() + " Cannot get XFers of Trade " + trade.getLongId());
        }
        return false;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        String reprocessKW = trade.getKeywordValue(REPROCESS_KW);
        if (!Util.isEmpty(reprocessKW)) {
            trade.removeKeyword(REPROCESS_KW);
            Log.info(this, getRuleName() + " Removing " + REPROCESS_KW + " KW in  Trade " + trade.getLongId());
        }

        String partenonModifValue = "";
        boolean addPartenonRequestDate = false;

        //Common for all products check counterparty
        if (trade.getCounterParty().getId() != oldTrade.getCounterParty().getId()) {
            this.partenonRequestValue = S_TRUE;
            if (Util.isEmpty(partenonModifValue)) {
                partenonModifValue = "Counterparty";
            }
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " Counterparty has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
            Log.info(this, getRuleName() + " Counterparty has changed, setting " + KW_PARTENON_MODIF + " to true in Trade " + trade.getLongId());
        }

        //Check PARTENONMSG*Prodcut*MessageFormatter
        if(validatePARTENONMSGMessageFormatter(trade)){
            this.partenonRequestValue = S_TRUE;
            if (Util.isEmpty(partenonModifValue)) {
                partenonModifValue = "PartenonAlias";
            }
            addPartenonRequestDate = true;
        }

        initValues(trade,oldTrade);

        //Check ISIN
        if(!newIsin.equalsIgnoreCase(oldIsin)){
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "Isin";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " Isin has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
        }

        //Check TradeDate
        if(tradeDate!=null && !tradeDate.equals(oldTradeDate)){
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "TradeDate";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " TradeDate has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
        }

        //Check ValueDate/StartDate
        if(startDate!=null && !startDate.equals(oldStartDate)){
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "ValueDate";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " ValueDate has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
        }

        //Check Direction
        if(!direction.equalsIgnoreCase(oldDirection)){
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "Direction";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " Direction has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
        }

        //Check Sec. Quantity
        if(!compareDouble(quantity,oldQuantity)){
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "Quantity";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " Quantity has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
        }

        //Check Nominal
        if(!compareDouble(nominal,oldNominal)){
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "Nominal";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " Nominal has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
        }

        //Check Currency
        if(!currency.equalsIgnoreCase(oldCurrency)){
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "Currency";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " Currency has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
        }

        //Check Principal
        if(!compareDouble(principal,oldPrincipal)){
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "Principal";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " Principal has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
            if (Util.isEmpty(partenonModifValue)) {
                partenonModifValue = "Principal";
            }
        }

        //Check Book common for all prodcuts
        if (trade.getBookId() != oldTrade.getBookId()) {
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "Book";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " Book has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
        }

        //Check dirty pryce
        if (!compareDouble(dirtyPrice, oldDirtyPryce)){
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "Price";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " Dirty price has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
        }

        //Check clean price
        if (!compareDouble(cleanPrice, oldCleanPrice)){
            this.partenonRequestValue = S_TRUE;
            partenonModifValue = "Price";
            addPartenonRequestDate = true;
            Log.info(this, getRuleName() + " Clean price has changed, setting " + KW_PARTENON_REQUEST + " to true in Trade " + trade.getLongId());
        }

        trade.addKeyword(KW_PARTENON_REQUEST, this.partenonRequestValue);
        trade.addKeyword(KW_PARTENON_MODIF, partenonModifValue);

        if (addPartenonRequestDate) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            final String date = dateFormat.format(new Date());
            trade.addKeyword(KW_PARTENON_REQUESTDATE, date);
            Log.info(this, getRuleName() + " Adding " + KW_PARTENON_REQUESTDATE + " KW in Trade " + trade.getLongId());
        }

        if (this.partenonRequestValue.equals(S_TRUE )) {
            // save old PartenonAccountingID
            String partenonAccountingID = trade.getKeywordValue("PartenonAccountingID");
            trade.addKeyword("OldPartenonAccountingID",partenonAccountingID);
            Log.info(this, getRuleName() + " Adding OldPartenonAccountingID KW in Trade " + trade.getLongId());
        }

        return true;
    }

    /**
     * @param trade
     * @param oldTrade
     */
    protected void initValues(Trade trade, Trade oldTrade){
        //Common values
        newIsin =  trade.getProduct().getSecCode(ISIN);
        oldIsin =  oldTrade.getProduct().getSecCode(ISIN);
        tradeDate = trade.getTradeDate();
        oldTradeDate = oldTrade.getTradeDate();
        quantity = trade.getQuantity();
        oldQuantity = oldTrade.getQuantity();
        nominal = trade.computeNominal();
        oldNominal = oldTrade.computeNominal();
        currency = trade.getProduct().getCurrency();
        oldCurrency = oldTrade.getProduct().getCurrency();
        startDate = trade.getSettleDate();
        oldStartDate = oldTrade.getSettleDate();
        direction = mapDirection(quantity);
        oldDirection = mapDirection(oldQuantity);
        principal = 0.0;
        oldPrincipal = 0.0;
        dirtyPrice = trade.getNegociatedPrice();
        oldDirtyPryce = oldTrade.getNegociatedPrice();
        cleanPrice = trade.getTradePrice();
        oldCleanPrice = oldTrade.getTradePrice();
    }

    /**
     * Override for check especific products
     * @param trade
     * @return
     */
    protected boolean validatePARTENONMSGMessageFormatter(Trade trade){
        return false;
    }

    protected void logRuleParams(TaskWorkflowConfig wc, String ruleName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getRuleName());
        sb.append(" in ").append(wc.getCurrentWorkflow()).append(" Workflow").append(" from ")
                .append(wc.getStatus()).append(" to ").append(wc.getResultingStatus())
                .append(" applying ").append(wc.getPossibleAction());
        Log.info(this, sb.toString());
        Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc, ruleName);
        if (!Util.isEmpty(map)) {
            for (String key : map.keySet()) {
                Log.info(this, getRuleName() + " Rule param : " + key + "=" + (String)map.get(key));
            }
        }
        else {
            Log.info(this, getRuleName() + " Rule has no parameters set.");
        }
    }

    protected boolean compareDouble(double first, double second) {
        DecimalFormat dec = new DecimalFormat("#0.00");
        return dec.format(first).equalsIgnoreCase(dec.format(second));
    }


    protected String mapDirection(double quantity){
        if(quantity>0.0){
            return "BUY";
        }else {
            return "SELL";
        }
    }
}
