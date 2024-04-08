package calypsox.tk.confirmation.builder.repo;

import calypsox.tk.confirmation.builder.CalConfirmationCounterpartyBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.util.CollateralBasedUtil;

/**
 * @author aalonsop
 */
public class RepoConfirmCounterpartyBuilder extends CalConfirmationCounterpartyBuilder {

    public RepoConfirmCounterpartyBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }

    public String buildInstrumentSubType() {
        String lang=this.buildLanguage();
        String subtype="Standard";
        if(CollateralBasedUtil.isBSB(trade)){
            subtype = "Simultanea";
        }else if(lang.toLowerCase().startsWith("esp")||lang.toLowerCase().startsWith("spa")){
            subtype="other";
        }
        return subtype;
    }
}
