/**
 *
 */
package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.DeadLockException;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.service.DSConnection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author aalonsop
 *
 */
public class SantAdjustFeeDateTradeRule implements WfTradeRule {

    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
                         Vector excps, Task task, Object dbCon, Vector events) {

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.bo.workflow.WfTradeRule#getDescription()
     */
    @Override
    public String getDescription() {
        return "Should be configured in the UNPRICE action to adjust the wrong Fee dates of the trade";
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
                          Vector excps, Task task, Object dbCon, Vector events) {
        Log.debug("SantSaveTradeIndAmountFeeASMarkTradeRule", "check - Start");

        if (trade.getProductType().equals(CollateralStaticAttributes.SEC_LENDING)
                || trade.getProductType().equals(CollateralStaticAttributes.REPO)
                || trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)) {

            Fee intAmountFee = CollateralUtilities.getTradeFee(trade, CollateralStaticAttributes.FEE_TYPE_IND_AMOUNT);
            Fee intAmountPOFee = CollateralUtilities.getTradeFee(trade,
                    CollateralStaticAttributes.FEE_TYPE_IND_AMOUNT_PO);

            if ((intAmountFee != null) && (intAmountPOFee != null)) {
                messages.add("Both " + CollateralStaticAttributes.FEE_TYPE_IND_AMOUNT + " and "
                        + CollateralStaticAttributes.FEE_TYPE_IND_AMOUNT_PO
                        + " are present for the Trade.[SantSaveTradeIndAmountFeeASMarkTradeRule]");
                return true;
            }
            try {
                adjustFeeDate(intAmountFee, intAmountPOFee, trade, messages);
            } catch (Exception e) {
                Log.error(this, "Trade with id:" + trade.getLongId() + " threw a error during saving: " + e.getMessage());
                Log.error(this, e);//Sonar
                return false;
            }
            Log.debug(this, "Fee were successfuly updated");
        }
        return true;
    }

    /**
     *
     * @param intAmountFee
     * @param intAmountPOFee
     * @param trade
     * @param processDate
     * @return Original Fee objects are updated by reference
     * @throws CalypsoServiceException
     * @throws PersistenceException
     * @throws SQLException
     * @throws DeadLockException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void adjustFeeDate(Fee intAmountFee, Fee intAmountPOFee, Trade trade, Vector messages)
            throws SQLException, PersistenceException {
        Vector<Fee> fees = new Vector<Fee>();
        List<String> feeTypes = new ArrayList<String>();
        boolean hasChanged = false;
        if (intAmountFee != null) {
            hasChanged = CollateralUtilities.isFeeDateBeforeGivenDate(intAmountFee,
                    JDate.valueOf(trade.getTradeDate()));
            if (hasChanged) {
                fees.add(intAmountFee);
                feeTypes.add(intAmountFee.getType());
            }
        }
        if (intAmountPOFee != null) {
            hasChanged = CollateralUtilities.isFeeDateBeforeGivenDate(intAmountPOFee,
                    JDate.valueOf(trade.getTradeDate()));
            if (hasChanged) {
                fees.add(intAmountPOFee);
                feeTypes.add(intAmountPOFee.getType());
            }
        }
        if (!fees.isEmpty()) {
            Vector<Fee> tradeFees = trade.getFees();
            for (Fee tradeFee : tradeFees) {
                for (String type : feeTypes)
                    if (tradeFee.getType().equals(type)) {
                        tradeFees.remove(tradeFee);
                    }
                tradeFees.addAll(fees);
                trade.setFees(tradeFees);
            }
        }
    }
}
