package calypsox.tk.confirmation.builder.bond.ctm;

import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.tk.confirmation.builder.bond.BondConfirmCounterpartyBuilder;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class CTMBondConfirmCounterpartyBuilder extends BondConfirmCounterpartyBuilder {

    public CTMBondConfirmCounterpartyBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }

    public String buildConfirmMode() {
        return Optional.ofNullable(this.cpty)
                .map(LegalEntity::getId)
                .map(id -> BOCache.getLegalEntityAttributes(DSConnection.getDefault(),id))
                .map(attrs -> getLEAttributeValue(attrs, CTMUploaderConstants.CONFIRMATION_TYPE_ATTR))
                .map(attrValue -> attrValue.replaceAll("\\s",""))
                .map(attrValue -> attrValue.replaceAll("(?i)/pdf",mapPdfStringValue()))
                .orElse("");
    }


    /**
     *
     * @return Empty if block trade, EMAIL in any other case
     */
    private String mapPdfStringValue(){
        return Optional.ofNullable(this.trade)
                .map(t -> t.getKeywordValue(CTMUploaderConstants.TRADE_KEYWORD_BLOCK_TRADE_DETAIL))
                .filter(kwdValue -> !kwdValue.isEmpty())
                .map(kwdValue -> "")
                .orElse("/EMAIL");
    }
}
