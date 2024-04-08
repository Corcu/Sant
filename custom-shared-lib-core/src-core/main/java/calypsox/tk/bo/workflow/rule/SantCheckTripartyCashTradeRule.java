package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Vector;

public class SantCheckTripartyCashTradeRule implements WfTradeRule {

    private static final String CASH = "CASH";
    private static final String COLLATERAL_TYPE = "CollateralType";
    private static final String MARGIN_TYPE = "COLLATERAL";
    private static final String TRADE_KEYWORD = "FromTripartyAllocation";


    @Override
    public boolean check(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade oldTrade, Vector paramVector1,
                         DSConnection paramDSConnection, Vector paramVector2, Task paramTask, Object paramObject,
                         Vector paramVector3) {

        if(CASH.equals(trade.getKeywordValue(COLLATERAL_TYPE))) {//Checking that it is an Margin Call Cash

            if (trade.getProduct() instanceof MarginCall) {

                MarginCall marginCallProduct = (MarginCall) trade.getProduct();

                if (marginCallProduct != null) {//Checking that the product is not null

                    CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                            marginCallProduct.getMarginCallId());

                    if (mcc != null) { //Checking that mcc is not null
                        if (Boolean.valueOf(mcc.getInterestConfig().isUseCashCallAccount())) { //Chechinkg that Contract field "Cash Margin Call Account" is True

                            {
                                if (MARGIN_TYPE.equals(trade.getProductSubType()) &&  //Checking that Margin type is "Collateral"
                                        Boolean.valueOf(trade.getKeywordValue(TRADE_KEYWORD))) //Checking that the Trade Keyword "FromTripartyAllocation" is True
                                {
                                    return true;
                                }
                            }
                        }
                    }
                }

            }
        }else{//If the trade keyword is not cash it won't make any checking
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Check values FromTripartyAllocation, trade Type COLLATERAL and Call account defined in the contract";
    }

    @Override
    public boolean update(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade oldTrade, Vector message,
                          DSConnection paramDSConnection, Vector paramVector2, Task paramTask, Object paramObject,
                          Vector paramVector3) {


        return true;
    }

}

