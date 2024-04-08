package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreMarginCallINTEREST extends SantBOCre {

    public BOCreMarginCallINTEREST(BOCre cre, Trade trade) {
        super(cre, trade);
    }
    CollateralConfig vmContract = null;
    @Override
    protected void fillValues() {
        this.creDescription = "Interest Margin Call Cash CCP";
        this.settlementMethod = getInstance().getSettleMethod(this.creBoTransfer);
        this.transferAccount = getInstance().getTransferAccountSM(this.settlementMethod,this.creBoTransfer);
    }


    @Override
    protected void init() {
        this.book = BOCache.getBook(DSConnection.getDefault(),this.boCre.getBookId());
        this.clientBoTransfer = getClientBoTransfer();
        this.creBoTransfer = getCreBoTransfer();
        this.collateralConfig = getContract();
        this.vmContract = getVMCashContract();
        this.account = getAccount();
    }

    @Override
    protected Double getPosition() {
        if(null!=vmContract){
            JDate valDate = addBusinessDays(this.trade.getTradeDate().getJDate(TimeZone.getDefault()),-1);
            return getInstance().getInvLastCashPosition(vmContract.getId(),this.trade, BOCreConstantes.DATE_TYPE_TRADE,BOCreConstantes.THEORETICAL,valDate);
        }
        return 0.0D;
    }

    @Override
    protected JDate getCancelationDate() {
        return getInstance().isCanceledEvent(this.boCre) ? getInstance().getActualDate() : null;
    }

    @Override
    protected CollateralConfig getContract() {
        if(null!=trade && trade.getProduct() instanceof MarginCall){
            final MarginCallConfig marginCallConfig = ((MarginCall) this.trade.getProduct()).getMarginCallConfig();
            final int contractId = null!= marginCallConfig ? marginCallConfig.getId() : 0;
            return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
        }
        return null;

    }

    protected Integer loadContractID(){
        return null!=vmContract ? vmContract.getId() : 0;
    }

    @Override
    protected Account getAccount() {
        return getInstance().getAccount(this.vmContract,this.boCre.getCurrency(0));
    }

    private CollateralConfig getVMCashContract(){
        final String contractId = Optional.ofNullable(this.collateralConfig).map(c -> c.getAdditionalField("CCP_INTEREST_VM_CONTRACT_ID")).orElse("");
        try {
            return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), Integer.parseInt(contractId));
        }catch (Exception e){
            Log.error(this,"Error parsing ContractId: " + contractId + " : " + e);
        }
        return null;
    }

    private JDate addBusinessDays(JDate date,int num){
        return null!=date ? date.addBusinessDays(-1, Util.string2Vector("SYSTEM")) : null;
    }
    @Override
    protected String getSubType() {
        return "COLLATERAL";
    }

    @Override
    protected String loadProductType() {
        return getInstance().getProductTypeMarginCall(this.trade);
    }
}
