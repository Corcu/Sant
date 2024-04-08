package calypsox.tk.report.extracontable.filter;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class MrgCallContractPOFilter {

    private final String bdsd="BDSD";
    private final String bste="BSTE";

    public boolean isPOEnabledForTripartyExtracontable(CollateralConfig config){
        return  isBSTEContract(config)||isBDSDContract(config);
    }

    public boolean isBSTEContract(CollateralConfig config){
        return Optional.ofNullable(config).map(CollateralConfig::getProcessingOrg).map(LegalEntity::getCode)
                .map(bste::equals).orElse(false);
    }

    public boolean isBDSDContract(CollateralConfig config){
        boolean isBdsdPo=Optional.ofNullable(config).map(CollateralConfig::getProcessingOrg).map(LegalEntity::getCode)
                .map(bdsd::equals).orElse(false);
        return isBdsdPo;
    }
}
