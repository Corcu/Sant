package calypsox.tk.upload.mapper.repo;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.EventTypeAction;
import com.calypso.tk.product.Repo;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.util.TradeArray;

import java.util.Optional;

/**
 * @author aalonsop
 * After a partial return integration, core's logic breaks collateral amounts and dates. This class tries to avoid that.
 */
public class RepoOnAmendMapper {

    Repo repoToMap;
    Collateral repoCollateral;
    Cash repoCash;


    public RepoOnAmendMapper(Repo repoToMap){
        this.repoToMap=repoToMap;
        this.repoCollateral= Optional.ofNullable(repoToMap).map(Repo::getCollaterals)
                .map(collats->collats.get(0)).orElse(null);
        this.repoCash=Optional.ofNullable(repoToMap).map(Repo::getCash).orElse(null);
    }

    public void mapOnAmendIfActivated(CalypsoTrade jaxbCalypsoTrade) throws CalypsoServiceException{
        String activationFlag = LocalCache.getDomainValueComment(DSConnection.getDefault(),"CodeActivationDV","DEACTIVATE_REPO_SLA");
        if(!Boolean.parseBoolean(activationFlag)){
            mapOnAmend(jaxbCalypsoTrade);
        }
    }
    public void mapOnAmend(CalypsoTrade jaxbCalypsoTrade) throws CalypsoServiceException {
        if(Action.S_AMEND.equals(jaxbCalypsoTrade.getAction()) && this.repoCollateral.getId()==0){
            TradeArray currentTrades= DSConnection.getDefault().getRemoteTrade().getTradesByExternalRef(jaxbCalypsoTrade.getExternalReference());
            if(!Util.isEmpty(currentTrades)){
                Trade currentTrade=currentTrades.get(0);
                if(currentTrade.getProduct() instanceof Repo){
                    Repo currentRepo=(Repo) currentTrade.getProduct();
                    overrideRepoCollateral(currentRepo);
                    overrideAmortSchedule(currentRepo);
                }
            }
        }
    }

    private void overrideRepoCollateral(Repo currentRepo){
        if(isAnyPartialReturnDone(currentRepo)) {
            repoCollateral.setId(currentRepo.getCollaterals().get(0).getId());
            repoCollateral.setInitialTradeVersion(currentRepo.getCollaterals().get(0).getInitialTradeVersion());
            repoCollateral.setStartDate(currentRepo.getCollaterals().get(0).getStartDate());
            repoCollateral.setQuantity(currentRepo.getCollaterals().get(0).getQuantity());
        }
    }
    private void overrideAmortSchedule(Repo currentRepo){
        String scheduleAmortType="Schedule";
        String currentAmortType=Optional.ofNullable(currentRepo.getCash()).map(Cash::getAmortType).orElse("");
        if(scheduleAmortType.equals(currentAmortType)){
            this.repoCash.setAmortType(scheduleAmortType);
        }
    }

    private boolean isAnyPartialReturnDone(Repo repo){
        boolean res=false;
        for(EventTypeAction action:repo.getEventTypeActions()){
            res="Partial Return".equalsIgnoreCase(action.getActionType());
            if(res){
                break;
            }
        }
        return res;
    }
}
