package calypsox.tk.report.extracontable;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;

import java.util.Optional;

/**
 * @author dmenendd
 */
public class MICExtracontableBRSBuilder extends MICExtracontableTradeBuilder {

    PerformanceSwap brs;
    Bond referenceBond;

    public MICExtracontableBRSBuilder(Trade trade) {
        super(trade);
        Product tradeProduct = Optional.ofNullable(trade).map(Trade::getProduct).orElse(null);
        if (tradeProduct instanceof PerformanceSwap) {
            brs = (PerformanceSwap) tradeProduct;
            referenceBond = getBRSReferenceBond();
        }

    }

    @Override
    public  MICExtracontableBean build() {
        messageBean=super.build();
        messageBean.setCodGLSContrapar(getCptyCode(sourceObject));
        messageBean.setCodContrapar(getCptyCode(sourceObject));
        messageBean.setDescCodContrapar(sourceObject.getCounterParty().getName());
        messageBean.setCodEstrOpe("ESP");
        messageBean.setCodNumOpeFront(getMurexRootContractKwd(sourceObject));
        messageBean.setCodRefInGr(buildInternalReference(getBRSReferenceBond()));
        return messageBean;
    }

    private Bond getBRSReferenceBond() {
        Bond bond = null;
        if (Optional.ofNullable(brs).map(PerformanceSwap::getReferenceProduct)
                .orElse(null) instanceof Bond) {
            bond = (Bond) brs.getReferenceProduct();
        }
        return bond;
    }

    private String buildInternalReference(Bond bond) {
        return Optional.ofNullable(bond)
                .map(bondSecond -> bondSecond.getSecCode(BOND_SEC_CODE_REF_INTERNA))
                .orElse("");
    }

    private String getCptyCode(Trade trade){
        return Optional.ofNullable(trade).map(Trade::getCounterParty).map(LegalEntity::getCode).orElse("");
    }

    private String getMurexRootContractKwd(Trade trade){
        return Optional.ofNullable(trade).map(t->t.getKeywordValue("MurexRootContract")).orElse("");
    }
}