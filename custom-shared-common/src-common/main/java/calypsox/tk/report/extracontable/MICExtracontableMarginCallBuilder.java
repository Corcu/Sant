package calypsox.tk.report.extracontable;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.Optional;


public class MICExtracontableMarginCallBuilder extends MICExtracontableInventoryBuilder {

    public MICExtracontableMarginCallBuilder(Inventory inventory){
        super(inventory);
    }

    @Override
    public MICExtracontableBean build() {
        this.messageBean = super.build();

        LegalEntity issuer = getIssuer();

        this.messageBean.setMonedaContravalor("EUR");
        this.messageBean.setCodPaisContrapar(getCountryISOCode(issuer));

        try {
            CollateralConfig cc = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(sourceObject.getMarginCallConfigId());
            LegalEntity contrapartyContract = Optional.ofNullable(cc).map(CollateralConfig::getLegalEntity).orElse(null);
            Account acc = BOCreUtils.getInstance().getAccount(cc,sourceObject.getSettleCurrency());

            this.messageBean.setAccountId(acc != null ? acc.getId() : 0);
            this.messageBean.setCodSector(contrapartyContract != null ? getLeAttr(contrapartyContract,SECCONT_LE_ATTR) : "");
            this.messageBean.setCdnuopba(acc != null ? acc.getId() : 0);
        } catch (CollateralServiceException e) {
            Log.error(this,e.getCause());
        }

        return this.messageBean;
    }

}
