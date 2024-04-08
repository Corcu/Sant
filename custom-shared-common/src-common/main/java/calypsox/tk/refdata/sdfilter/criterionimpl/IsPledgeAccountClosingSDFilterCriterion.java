package calypsox.tk.refdata.sdfilter.criterionimpl;

import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.refdata.sdfilter.SDFilterOperatorType;
import com.calypso.tk.service.DSConnection;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class IsPledgeAccountClosingSDFilterCriterion extends AbstractSDFilterCriterion<Boolean> {

    private static final String CRIT_NAME = "IsPledgeAccountClosing";


    public IsPledgeAccountClosingSDFilterCriterion() {
        setName(CRIT_NAME);
        setCategory(SDFilterCategory.TRADE);
        setTradeNeeded(true);
    }

    @Override
    public List<SDFilterOperatorType> getOperatorTypes() {
        return Collections.singletonList(SDFilterOperatorType.IS);
    }

    public SDFilterCategory getCategory() {
        return SDFilterCategory.TRADE;
    }

    @Override
    public Class<Boolean> getValueType() {
        return Boolean.class;
    }

    @Override
    public Boolean getValue(SDFilterInput sdFilterInput) {
        final Trade trade = sdFilterInput.getTrade();
        return Optional.ofNullable(trade).map(this::isAccountingClosingDoneInCurrentVersion).orElse(false);
    }

    private boolean isAccountingClosingDoneInCurrentVersion(Trade trade){
        boolean isACDone=false;
        List<CalypsoBindVariable> bindVariables= CustomBindVariablesUtil.createNewBindVariable(trade.getLongId());
        CustomBindVariablesUtil.addNewBindVariableToList(trade.getVersion(),bindVariables);
        try {
            String tradeAuditWhere = "ENTITY_ID=? AND VERSION_NUM=? AND ENTITY_CLASS_NAME='Trade' AND ENTITY_FIELD_NAME='Product._endDate'";
            Vector<?> auditValues= DSConnection.getDefault().getRemoteTrade().getAudit(tradeAuditWhere,null,bindVariables);
            isACDone=!Util.isEmpty(auditValues);
        } catch (CalypsoServiceException e) {
            Log.error(this,e.getCause());
        }
        return isACDone;
    }
}