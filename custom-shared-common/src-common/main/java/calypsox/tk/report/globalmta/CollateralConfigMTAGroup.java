package calypsox.tk.report.globalmta;

import calypsox.tk.report.quotes.FXQuoteHelper;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class CollateralConfigMTAGroup {

    int mtaThreshold=500000;
    int bsnyId;

    List<CollateralConfig> configs;
    boolean containsBSNY=false;
    boolean containsLiveTrades=false;


    double totalMTAOwnerUSD=0.0D;
    double totalMTACptyUSD=0.0D;

    FXQuoteHelper quoteHelper;

    JDate valDate;


    public CollateralConfigMTAGroup(JDate valDate){
        this.configs=new ArrayList<>();
        this.quoteHelper=new FXQuoteHelper("OFFICIAL");
        this.valDate=valDate;
        this.bsnyId= Optional.ofNullable(BOCache.getLegalEntity(DSConnection.getDefault(),"BSNY"))
                .map(LegalEntity::getId).orElse(-1);
    }

    /**
     *
     * @param cc
     */
    public void addConfigToGroup(CollateralConfig cc){
        this.configs.add(cc);
        setContainsBSNY(cc);
        this.totalMTAOwnerUSD=totalMTAOwnerUSD+getOwnerMTAAmountInUSD(cc);
        this.totalMTACptyUSD=totalMTACptyUSD+getCptyMTAAmountInUSD(cc);
        if(!this.containsLiveTrades){
            this.containsLiveTrades=hasDetailEntries(cc);
        }

    }

    private void setContainsBSNY(CollateralConfig cc){
        if(!this.containsBSNY) {
            this.containsBSNY=Optional.ofNullable(cc.getAdditionalPOIds()).map(add->add.contains(bsnyId)).orElse(false);
        }
    }

    public boolean isContainsLiveTrades(){
        return this.containsLiveTrades;
    }

    public boolean hasDetailEntries(CollateralConfig cc){
        boolean hasDetailEntries=false;
        List<MarginCallEntryDTO> entries;
        try {
            entries = CollateralManagerUtil.loadMarginCallEntriesDTO(Collections.singletonList(cc.getId()),valDate);
            if(!Util.isEmpty(entries)){
                hasDetailEntries=!Util.isEmpty(entries.get(0).getDetailEntries());
            }
        } catch (CollateralServiceException exc) {
            Log.error(this, "Error loading entries", exc);
        }
        return hasDetailEntries;
    }
    public List<CollateralConfig> getCollateralConfigs(){
        return this.configs;
    }

    public boolean isContainsBSNY(){
        return this.containsBSNY;
    }

   public double getTotalMTAOwner(){
        return totalMTAOwnerUSD;
   }

   public double getTotalMTACpty(){
        return totalMTACptyUSD;
   }

    public double getTotalMTAOwnerEUR(){
        double mta=0.0D;
        try {
            mta=quoteHelper.convertAmountInEUR(totalMTAOwnerUSD,"USD",valDate);
        } catch (MarketDataException exc) {
            Log.warn(exc.getMessage(),exc.getCause());
        }
        return mta;
    }

    public double getTotalMTACptyEUR(){
        double mta=0.0D;
        try {
            mta=quoteHelper.convertAmountInEUR(totalMTACptyUSD,"USD",valDate);
        } catch (MarketDataException exc) {
            Log.warn(exc.getMessage(),exc.getCause());
        }
        return mta;
    }

    private Double getOwnerMTAAmountInUSD(CollateralConfig cc){
        return getMTAAmountInUSD(cc.getPoMTAAmount(),cc.getPoMTACurrency());
    }

    private Double getCptyMTAAmountInUSD(CollateralConfig cc){
        return getMTAAmountInUSD(cc.getLeMTAAmount(),cc.getLeMTACurrency());
    }

   private Double getMTAAmountInUSD(Double mtaAmount, String ccy){
        Double mta=0.0D;
        if("USD".equals(ccy)){
            mta=mtaAmount;
        }else if(!Util.isEmpty(ccy)){
            try {
                mta=quoteHelper.convertAmountInUSD(mtaAmount,ccy,valDate);
            } catch (MarketDataException exc) {
                Log.warn(exc.getMessage(),exc.getCause());
            }
        }
        return mta;
   }

   public boolean isThresholdCptyExceeded(){
        return this.totalMTACptyUSD>this.mtaThreshold;
   }

    public boolean isThresholdCptyExceeded(double currentContractMTA,String mtaCurrency){
        double currentMTAUSD=getMTAAmountInUSD(currentContractMTA,mtaCurrency);
        return this.totalMTACptyUSD+currentMTAUSD>this.mtaThreshold;
    }

    public boolean isThresholdPOExceeded(double currentContractMTA,String mtaCurrency){
        double currentMTAUSD=getMTAAmountInUSD(currentContractMTA,mtaCurrency);
        return this.totalMTAOwnerUSD+currentMTAUSD>this.mtaThreshold;
    }
}
