
package calypsox.tk.upload.uploader.margincall;

import com.calypso.tk.core.Action;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.upload.jaxb.MarginCallContract;
import com.calypso.tk.upload.jaxb.MarginCallDetails;

import java.util.Optional;

/**
 * @author aalonsop
 *
 * Used to set CollateralConfig jaxb fields that doesnt need to be overriden by an incoming xml AMEND
 */
public class UploadCollateralConfigMapper {

    static String date1="15/11/2019";
    static String date2="20/02/2020";
    static String date3="23/03/2020";
    static String verdadero="VERDADERO";

    public void map(CollateralConfig persistedContract, MarginCallContract incomingJaxbContract){
        if(incomingJaxbContract.getAction().equals(Action.S_NEW)){
            mapOnNew(persistedContract);
        }else{
            mapOnAmend(persistedContract,incomingJaxbContract);
        }
    }

    public void mapOnNew(CollateralConfig persistedContract){
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.ACCOUNTING);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.ACCOUNTING_SECURITY);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.ACCRUAL_INCLUDE_CALC);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.ALWAYS_ROUND_RETURN_MARGIN);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.ANACREDIT);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.BLOCK_MESSAGE_PAY_CASH);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.BLOCK_MESSAGE_PAY_SECURITIES);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.BLOCK_MESSAGE_PAY_SECURITIES_EQTY);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.BLOCK_MESSAGE_RECEIVE_CASH);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.BLOCK_MESSAGE_RECEIVE_SECURITIES);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.BLOCK_MESSAGE_RECEIVE_SECURITIES_EQTY);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.CALC_AGENT);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.GRM_CONTROL);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.GUARANTEE_TYPE);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.HEAD_CLONE);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.MEDUSA);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.POSITION_EXPORT_BOND_DATE);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.POSITION_EXPORT_EQUITY_DATE);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.SETTLEMENT);
        setAdditionalFieldFromEnum(persistedContract,ContractFixedFields.SETTLEMENT_SEC);
    }

    private void setAdditionalFieldFromEnum(CollateralConfig persistedContract, ContractFixedFields value){
        persistedContract.setAdditionalField(value.name(),value.value);
    }

    private enum ContractFixedFields{

        ACCOUNTING(date1),
        ACCOUNTING_SECURITY(date1),
        ACCRUAL_INCLUDE_CALC("None"),
        ALWAYS_ROUND_RETURN_MARGIN("usingDeliveryRoundingAndTotalRounding"),
        ANACREDIT(verdadero),
        BLOCK_MESSAGE_PAY_CASH(verdadero),
        BLOCK_MESSAGE_PAY_SECURITIES(verdadero),
        BLOCK_MESSAGE_PAY_SECURITIES_EQTY(verdadero),
        BLOCK_MESSAGE_RECEIVE_CASH(verdadero),
        BLOCK_MESSAGE_RECEIVE_SECURITIES(verdadero),
        BLOCK_MESSAGE_RECEIVE_SECURITIES_EQTY(verdadero),
        CALC_AGENT("BSTE"),
        GRM_CONTROL(verdadero),
        GUARANTEE_TYPE("Variation Margin"),
        HEAD_CLONE("HEAD"),
        MEDUSA(date1),
        POSITION_EXPORT_BOND_DATE(date2),
        POSITION_EXPORT_EQUITY_DATE(date2),
        SETTLEMENT(date1),
        SETTLEMENT_SEC(date3);

        String value;


        ContractFixedFields(String value){
            this.value=value;
        }
    }

    public void mapOnAmend(CollateralConfig persistedContract, MarginCallContract incomingJaxbContract){
        incomingJaxbContract.setMarginCallConfigName(persistedContract.getName());
        mapProductsFilter(persistedContract,incomingJaxbContract);
        mapAcceptUndisputedAmt(persistedContract,incomingJaxbContract);
        mapAcceptOnPOFavor(persistedContract,incomingJaxbContract);
        mapComments(persistedContract,incomingJaxbContract);
        mapWfProduct(persistedContract,incomingJaxbContract);
        mapWfSubtype(persistedContract,incomingJaxbContract);


    }

    private void mapProductsFilter(CollateralConfig persistedContract, MarginCallContract incomingJaxbContract){
       String incomingProductFilter=incomingJaxbContract.getMarginCallDetails().getProductFilter();
       if(Optional.ofNullable(incomingProductFilter).map(String::isEmpty).orElse(true)){
           MarginCallDetails details=incomingJaxbContract.getMarginCallDetails();
           details.setProductFilter(persistedContract.getProdStaticDataFilterName());
           incomingJaxbContract.setMarginCallDetails(details);
       }
    }

    private void mapWfProduct(CollateralConfig persistedContract, MarginCallContract incomingJaxbContract){
        if("Collateral".equals(persistedContract.getWorkflowProduct())){
            MarginCallDetails details=incomingJaxbContract.getMarginCallDetails();
            details.setWorkflowProduct(persistedContract.getWorkflowProduct());
            incomingJaxbContract.setMarginCallDetails(details);
        }
    }

    private void mapWfSubtype(CollateralConfig persistedContract, MarginCallContract incomingJaxbContract){
        if("ANY".equals(persistedContract.getWorkflowSubtype())){
            MarginCallDetails details=incomingJaxbContract.getMarginCallDetails();
            details.setWorkflowProduct(persistedContract.getWorkflowSubtype());
            incomingJaxbContract.setMarginCallDetails(details);
        }
    }

    private void mapAcceptUndisputedAmt(CollateralConfig persistedContract, MarginCallContract incomingJaxbContract){
        boolean undisputedAmountB=Optional.ofNullable(incomingJaxbContract.getMarginCallDetails().isAcceptUndisputedAmountB())
                .orElse(false);
        if(!undisputedAmountB){
            MarginCallDetails details=incomingJaxbContract.getMarginCallDetails();
            details.setAcceptUndisputedAmountB(persistedContract.acceptUndisputedAmount());
            incomingJaxbContract.setMarginCallDetails(details);
        }
    }

    private void mapAcceptOnPOFavor(CollateralConfig persistedContract, MarginCallContract incomingJaxbContract){
        boolean acceptPOInFavour=Optional.ofNullable(incomingJaxbContract.getMarginCallDetails().isAcceptOnPOFavorB())
                .orElse(false);
        if(!acceptPOInFavour){
            MarginCallDetails details=incomingJaxbContract.getMarginCallDetails();
            details.setAcceptOnPOFavorB(persistedContract.acceptOnPOFavor());
            incomingJaxbContract.setMarginCallDetails(details);
        }
    }

    private void mapComments(CollateralConfig persistedContract, MarginCallContract incomingJaxbContract){
        String comment=incomingJaxbContract.getComment();
        if(Optional.ofNullable(comment).map(String::isEmpty).orElse(true)){
            incomingJaxbContract.setComment(persistedContract.getComment());
        }
    }
}
