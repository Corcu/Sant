package calypsox.tk.refdata.sdfilter.criterionimpl;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterionFactory;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/***
 * @author acd
 */
public class TransferAgentSDFilterCriterionFactory extends AbstractSDFilterCriterionFactory {
    private static final String TO_ACCOUNT = "TransferAgent To Account";
    private static final String FROM_ACCOUNT = "TransferAgent From Account";

    private static final String TO_SDI_IDENTIFIER = "TransferAgent To SDI Identifier";
    private static final String FROM_SDI_IDENTIFIER = "TransferAgent From SDI Identifier";

    @Override
    protected Object getValue(String criterionName, SDFilterInput input) {
        TransferAgent transferAgent = Optional.ofNullable(input.getTrade()).map(Trade::getProduct)
                .filter(TransferAgent.class::isInstance)
                .map(TransferAgent.class::cast).orElse(null);

        if(TO_ACCOUNT.equalsIgnoreCase(criterionName)){
            return getToAccount(transferAgent);
        }
        if(FROM_ACCOUNT.equalsIgnoreCase(criterionName)){
            return getFromAccount(transferAgent);
        }

        if(TO_SDI_IDENTIFIER.equalsIgnoreCase(criterionName)){
            int sdiID = Optional.ofNullable(transferAgent).map(TransferAgent::getToSdiId).orElse(0);
            return getAgentIdentifier(sdiID);
        }

        if(FROM_SDI_IDENTIFIER.equalsIgnoreCase(criterionName)){
            int sdiID = Optional.ofNullable(transferAgent).map(TransferAgent::getFromSdiId).orElse(0);
            return getAgentIdentifier(sdiID);
        }

        return "";
    }

    @Override
    public Set<String> getSDFilterCriterionNames() {
        Set<String> criterion = new TreeSet<>();
        criterion.add(TO_ACCOUNT);
        criterion.add(FROM_ACCOUNT);
        criterion.add(TO_SDI_IDENTIFIER);
        criterion.add(FROM_SDI_IDENTIFIER);
        return criterion;
    }

    @Override
    protected Class<?> getValueType(String criterionName) {
        return String.class;
    }

    @Override
    protected SDFilterCategory getCategory(String criterionName) {
        return SDFilterCategory.PRODUCT;
    }

    @Override
    protected boolean isTradeNeeded(String criterionName) {
        return true;
    }
    protected String getSubCategory(String criterionName) {
        return TransferAgent.class.getSimpleName();
    }


    protected String getAgentIdentifier(int sdId){
        if(sdId>0){
            SettleDeliveryInstruction sdi = loadSDI(sdId);
            return null!=sdi ? sdi.getAgentIdentifier() : "";
        }
        return "";
    }

    protected String getFromAccount(TransferAgent transferAgent){
        int fromSdiId = Optional.ofNullable(transferAgent).map(TransferAgent::getFromSdiId).orElse(0);
        SettleDeliveryInstruction fromSdi = loadSDI(fromSdiId);
        Account account = getAccount(fromSdi);
        return null!=account ? account.getName() : "";
    }

    private SettleDeliveryInstruction loadSDI(int sdiId){
        return BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), sdiId);
    }

    protected String getToAccount(TransferAgent transferAgent){
        int toSdiId = Optional.ofNullable(transferAgent).map(TransferAgent::getToSdiId).orElse(0);
        SettleDeliveryInstruction toSdi = loadSDI(toSdiId);
        Account account = getAccount(toSdi);
        return null!=account ? account.getName() : "";
    }


    private Account getAccount(SettleDeliveryInstruction sdiID){
        int accountId = Optional.ofNullable(sdiID).map(SettleDeliveryInstruction::getGeneralLedgerAccount).orElse(0);
        return BOCache.getAccount(DSConnection.getDefault(), accountId);
    }
}
