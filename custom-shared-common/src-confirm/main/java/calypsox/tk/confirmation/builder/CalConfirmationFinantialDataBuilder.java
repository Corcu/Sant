package calypsox.tk.confirmation.builder;

import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Security;
import com.calypso.tk.product.flow.CashFlowCompound;
import com.calypso.tk.product.flow.CashFlowSimple;
import com.calypso.tk.service.DSConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class CalConfirmationFinantialDataBuilder extends CalypsoConfirmationConcreteBuilder{

    private static final String MX_SEC_DISPLAY_MX_LABEL="Mx_SecurityDisplayLabel";
    protected Security referenceSecurity;

    public CalConfirmationFinantialDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }

    public String buildSettlementCurrency() {
        return Optional.ofNullable(trade).map(Trade::getSettleCurrency).orElse("");
    }

    public String buildBondTradeId() {
        return Optional.ofNullable(trade)
                .map(trade -> trade.getKeywordValue(MX_SEC_DISPLAY_MX_LABEL))
                .orElse("");
    }

    public String buildBondName(){
        return Optional.ofNullable(referenceSecurity)
                .map(Security::getSecurity)
                .map(Product::getName)
                .orElse("");
    }

    public String buildInternalReferenceIsin() {
        return Optional.ofNullable(referenceSecurity)
                .map(Security::getSecurity)
                .map(sec -> sec.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN))
                .orElse("");
    }

    public String buildBondIssuer() {
        int issuerId = Optional.ofNullable(referenceSecurity)
                .map(Security::getIssuerId)
                .orElse(0);
        return Optional.ofNullable(BOCache.getLegalEntity(DSConnection.getDefault(), issuerId))
                .map(LegalEntity::getName)
                .orElse("");
    }

    public String buildBondMaturityDate() {
        return Optional.ofNullable(referenceSecurity)
                .map(Security::getSecurity)
                .map(Product::getMaturityDate).map(JDate::toString).orElse("");
    }

    public String buildUnderlyingSecCurrency(){
        return Optional.ofNullable(referenceSecurity)
                .map(Security::getSecurity)
                .map(Product::getCurrency).orElse("");
    }
    public String buildReturnStatus() {
        return String.valueOf(1);
    }


    protected String getRateFromPeriod(CashFlowSet cashFlows){
        JDate creationDate=JDate.valueOf(boMessage.getCreationDate());
        for (CashFlow flow : cashFlows) {
            if (CashFlow.INTEREST.equals(flow.getType())
                    &&creationDate.before(flow.getEndDate())) {
                BigDecimal spreadBasisPoints = BigDecimal.valueOf(getFlowRate(flow));
                return spreadBasisPoints.setScale(8, RoundingMode.HALF_EVEN).toString();
            }
        }
        return "";
    }

    protected double getFlowRate(CashFlow cashFlow){
        double rate=0.0D;
        if(cashFlow instanceof CashFlowSimple) {
            CashFlowSimple cfSimple= (CashFlowSimple) cashFlow;
            if (cfSimple.getRateIndex() != null) {
                rate = cfSimple.getSpread();
            } else {
                rate = cfSimple.getRate();
            }
        }else if(cashFlow instanceof CashFlowCompound){
            CashFlowCompound cfCompound= (CashFlowCompound) cashFlow;
            if (cfCompound.getRateIndex() != null) {
                rate = cfCompound.getSpread();
            } else {
                rate = cfCompound.getRate();
            }
        }
        return rate * 100.0D;
    }

    public String buildMifidCostsExpensesAmount() {
        return "0";
    }

    public String buildMifidCostsExpensesCurr() {
        return "";
    }

    protected String formatNumber(Double number) {
        return String.format(Locale.ENGLISH, "%.6f", number);
    }
}
