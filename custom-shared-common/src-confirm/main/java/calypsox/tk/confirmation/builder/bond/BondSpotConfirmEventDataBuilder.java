package calypsox.tk.confirmation.builder.bond;

import calypsox.ctm.util.PlatformAllocationTradeFilterAdapter;
import calypsox.ctm.util.CTMTradeFinder;
import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.tk.bo.fiflow.builder.trade.TradePartenonBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author aalonsop
 */
public class BondSpotConfirmEventDataBuilder extends BondConfirmEventDataBuilder implements PlatformAllocationTradeFilterAdapter {

    private final String kwdMxElectPlat = "Mx Electplatf";
    private final String kwdMxRootContract = "MurexRootContract";
    private final String kwdMxGID = "Mx GID";
    private final String kwdMxGlobalID = "Mx Global ID";
    TradePartenonBuilder partenonBuilder;
    final Trade blockTrade;

    public BondSpotConfirmEventDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        partenonBuilder = new TradePartenonBuilder(trade);
        this.blockTrade=getBlockTradeFromAllocationChild(CTMTradeFinder::findVanillaBlockTrade);
    }

    public String buildActionFlow() {
        String eventAction = confirmationEventInfo.getEventAction();
        String eventActionFlow = "A";
        if ("CANCEL".equalsIgnoreCase(eventAction)) {
            eventActionFlow = "C";
        }
        return eventActionFlow;
    }

    public String buildEntityId() {
        return partenonBuilder.buildCodEmprField();
    }

    public String buildAccountingCenterId() {
        return partenonBuilder.buildCodCentField();
    }

    public String buildProductId() {
        return partenonBuilder.buildCodProdField();
    }

    public String buildSubProductId() {
        return partenonBuilder.buildCodsProdField();
    }

    public String buildPartenonTradeId() {
        return partenonBuilder.buildCodEmprField()
                + partenonBuilder.buildCodCentField()
                + partenonBuilder.buildCodProdField()
                + partenonBuilder.buildCdoPerboField();
    }

    public String buildContractId() {
        return partenonBuilder.buildCdoPerboField();
    }

    public String buildEventIdFlow() {
        String partenon = buildPartenonTradeId();
        return partenon + this.boMessage.getLongId();
    }

    public String buildEventTypeId() {
        return this.confirmationEventInfo.getEventTypeId();
    }

    public String buildElectPlat() {
        return Optional.ofNullable(trade)
                .map(t -> t.getKeywordValue(kwdMxElectPlat))
                .orElse("");
    }

    public String buildFOCode() {
        return Optional.ofNullable(trade)
                .map(t -> t.getKeywordValue(kwdMxRootContract))
                .orElse("");
    }

    public String buildGistGlobalId() {
        return Optional.ofNullable(trade)
                .map(t -> t.getKeywordValue(kwdMxGID))
                .orElse("");
    }

    public String buildGistIdCreator() {
        return Optional.of(getKwdMxGlobalID(this.trade))
                .filter(id -> !id.isEmpty())
                .orElseGet(()-> getKwdMxGlobalID(this.blockTrade));
    }

    private String getKwdMxGlobalID(Trade trade){
        return Optional.ofNullable(trade)
                .map(t -> t.getKeywordValue(kwdMxGlobalID))
                .orElse("");
    }

    public String buildRetailIndicator() {
        return String.valueOf(0);
    }

    /**
     * @return 1 in case of NOT being a CTMChild
     */
    public String buildBlockIdentifier() {
        return Optional.ofNullable(this.trade)
                .map(this::isChildTrade)
                .map(isChild -> !isChild)
                .map(this::booleanToIntString)
                .orElseGet(() -> booleanToIntString(true));
    }

    public String buildBlockOperationNumber() {
        return Optional.ofNullable(this.blockTrade)
                .map(Trade::getExternalReference)
                .orElse("");
    }

    public String buildCorporateEventId() {
        return "0";
    }

    /**
     * @return 1 in case of BEING a CTMChild
     */
    public String buildAllocationIndicator() {
        return Optional.ofNullable(this.trade)
                .map(this::isChildTrade)
                .map(this::booleanToIntString)
                .orElseGet(() -> booleanToIntString(false));
    }

    public String buildBlockAllocationInd() {
        return String.valueOf(1);
    }

    @Override
    public String buildInstrumentType() {
        return "Fixed Income Spot";
    }


    private String booleanToIntString(boolean value) {
        return value ? String.valueOf(1) : String.valueOf(0);
    }

    private Trade getBlockTradeFromAllocationChild(Function<String, Trade> finderFunction) {
        return Optional.ofNullable(this.trade)
                .map(t -> t.getKeywordValue(CTMUploaderConstants.ALLOCATED_FROM_STR))
                .filter(value -> !value.isEmpty())
                .map(finderFunction)
                .orElse(null);
    }
}
