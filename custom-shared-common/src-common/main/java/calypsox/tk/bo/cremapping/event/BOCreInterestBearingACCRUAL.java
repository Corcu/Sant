package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.InterestBearingEntry;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountInterestConfig;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;
/**
 * @author acd
 */
public class BOCreInterestBearingACCRUAL extends SantBOCre {

    public BOCreInterestBearingACCRUAL(BOCre cre, Trade trade) {
        super(cre,trade);
    }

    public void fillValues() {

        this.creDescription = "Accrued Interest";

        if(this.account!=null){
            final AccountInterestConfig accountInterest = getInstance().getAccountInterest(this.account);
            this.indexDefinition = getInstance().getInterestName(accountInterest);
            this.fixedFloating = getInstance().getFixedFloating(accountInterest);
        }

        if(this.trade!=null){
            final InterestBearingEntry interest = ((InterestBearing) this.trade.getProduct()).getEntry(BOCreConstantes.INTEREST,this.boCre.getEffectiveDate());
            this.rate = getInstance().getInterestRate(interest);
            this.startDate = ((InterestBearing)this.trade.getProduct()).getStartDate();
            this.ratePositiveNegative = getInstance().getPositiveNegative(interest);
            this.expiryDate = ((InterestBearing)this.trade.getProduct()).getEndDate();
        }
    }

    @Override
    public String getDirection() {
        return getInstance().getDirectionACC(this.amount1);
    }

    /**
     * Need Override, load collateralConfig from Account
     */
    @Override
    protected void init() {
        this.book = BOCache.getBook(DSConnection.getDefault(),this.boCre.getBookId());
        this.clientBoTransfer = getClientBoTransfer();
        this.creBoTransfer = getCreBoTransfer();
        this.account = getAccount();
        this.collateralConfig = getContract();
    }

    public CollateralConfig getContract() {
        return getInstance().getContract(this.account);
    }

    protected Account getAccount() {
        if(this.trade!=null && this.trade.getProduct()!=null && this.trade.getProduct() instanceof InterestBearing){
            return BOCache.getAccount(DSConnection.getDefault(), ((InterestBearing)this.trade.getProduct()).getAccountId());
        }
        return null;
    }

    /**
     * @return @{@link JDate}
     */
    @Override
    public JDate getCancelationDate() {
        return getInstance().isCanceledEvent(this.boCre) ? getInstance().getActualDate() : null;
    }

    @Override
    protected Double getPosition() {
        return getCashPosition();
    }

    public Double getCashPosition() {
        return getInstance().getInvLastCashPosition(this.collateralConfig,this.trade, BOCreConstantes.DATE_TYPE_SETTLE,BOCreConstantes.THEORETICAL,null);
    }


}
