package calypsox.tk.bo.workflow.rule;

import calypsox.tk.csdr.BondPenaltyAmountCalculator;
import calypsox.tk.csdr.CSDRPenaltyAmountCalculator;
import calypsox.tk.csdr.CSDRXferPSETHandler;
import calypsox.tk.csdr.EquityPenaltyAmountCalculator;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class CalculateCSDRPenaltyTransferRule implements WfTransferRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Calculates transfer's potential penalty amounts";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        CSDRPenaltyAmountCalculator calculator=getCSDRCalculatorForProduct(transfer);
        if(calculator!=null&& new CSDRXferPSETHandler().isElegiblePSET(transfer,trade)){
            calculator.calculatePenalties();
        }
        return true;
    }

    private CSDRPenaltyAmountCalculator getCSDRCalculatorForProduct(BOTransfer transfer){
        CSDRPenaltyAmountCalculator calculator=null;
        Product underlying=BOCache.getExchangedTradedProduct(DSConnection.getDefault(), transfer.getProductId());
        if(Optional.ofNullable(underlying).map(p-> p instanceof Equity).orElse(false)){
            calculator=new EquityPenaltyAmountCalculator(transfer,underlying);
        }else if(Optional.ofNullable(underlying).map(p-> p instanceof Bond).orElse(false)){
            calculator=new BondPenaltyAmountCalculator(transfer,underlying);
        }
        return calculator;
    }
}
