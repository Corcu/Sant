package calypsox.tk.bo;

import com.calypso.tk.core.Book;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Pledge;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.io.StringWriter;
import java.util.Optional;

public class PARTENONMSGPledgeMessageFormatter extends PARTENONMSGRepoMessageFormatter{

    @Override
    protected String getIRD() {
        return "IRDREPOREPO";
    }

    @Override
    protected String getDirection(Product product) {


        final double quantity = Optional.ofNullable(product).filter(Pledge.class::isInstance).map(Pledge.class::cast).map(Pledge::getQuantity).orElse(0.0);

        if(isNewCodeEnabled()){
            return getDirectionNew(quantity);
        }else{
            return getDirectionOld(quantity);
        }
    }

    public boolean isNewCodeEnabled(){
        String activationFlag = LocalCache.getDomainValueComment(DSConnection.getDefault(),"CodeActivationDV","ACTIVATE_PLEDGE_PARTENON");
        return Boolean.parseBoolean(activationFlag);
    }

    private String getDirectionOld(double quantity){
        if(quantity>=0.0d){
            return "SELL";
        }else {
            return "BUY";
        }
    }

    private String getDirectionNew(double quantity){
        if(quantity<0.0d){
            return "SELL";
        }else {
            return "BUY";
        }
    }
    /**
     * 0:Alta 1:Baja 2:Modificacion
     * @param trade
     * @return
     */
    public String generateMsg(Trade trade){
        if(isEquity(trade)){
            return generateEquitySwiftMessage(trade);
        }else {
            return super.generateMsg(trade);
        }
    }

    @Override
    protected String getSecCode(Trade trade, String code) {
        return Optional.ofNullable(trade)
                .filter(t -> t.getProduct() instanceof Pledge)
                .map(p -> ((Pledge)p.getProduct()).getSecurity())
                .map(product -> product.getSecCode(code)).orElse("");
    }

    @Override
    protected Product getSecProduct(Trade trade) {
        return Optional.ofNullable(trade)
                .filter(t -> t.getProduct() instanceof Pledge)
                .map(p -> ((Pledge) p.getProduct()).getSecurity()).orElse(null);
    }

    /**
     * Check if product underlying is Equity or not
     *
     * @param trade
     * @return
     */
    private boolean isEquity(Trade trade){
        return Optional.ofNullable(trade)
                .filter(t -> t.getProduct() instanceof Pledge)
                .map(p -> ((Pledge) p.getProduct()).getSecurity()).filter(Equity.class::isInstance).isPresent();
    }

    private String generateEquitySwiftMessage(Trade trade){
            StringWriter sw = new StringWriter();
            sw.append("CALYPSO").append(";");
            sw.append("0").append(";");
            sw.append(String.valueOf(trade.getLongId())).append(";");
            sw.append(trade.getBook().getName()).append(";");
            sw.append(trade.getCounterParty().getExternalRef()).append(";");
            sw.append(getProductSubType(trade)).append(";");
            return sw.toString();
    }

    /**
     * Get Product SubType
     *
     * @return
     */
    private String getProductSubType(Trade trade){
        String result = "";
        Pledge pledge = (Pledge) trade.getProduct();
        Equity equity = pledge.getSecurity() instanceof Equity ? (Equity)pledge.getSecurity() : null;
        if(null!=equity){
            String type = equity.getType();
            String equityType = equity.getSecCode("EQUITY_TYPE");
            Book book = trade.getBook();
            String accountingLink = "";
            if(null!=book) {
                accountingLink = trade.getBook().getAccountingBook().getName();
                if("Equity".equalsIgnoreCase(type)){
                    if("Negociacion".equalsIgnoreCase(accountingLink)) {
                        if ("CS".equalsIgnoreCase(equityType)) {
                            if(isATA(trade)){
                                return "TRIVAATAONE";
                            }else {
                                return "TRIVACTAONE";
                            }
                        } else if ("DERSUS".equalsIgnoreCase(equityType)) {
                            if(isATA(trade)){
                                return "TRIVAATDSNE";
                            }else {
                                return "TRIVACTDSNE";
                            }
                        } else if ("PS".equalsIgnoreCase(equityType)) {
                            if(isATA(trade)){
                                return "TRIVAATPSNE";
                            }else {
                                return "TRIVACTPSNE";
                            }
                        } else if ("INSW".equalsIgnoreCase(equityType)) {
                            if(isATA(trade)){
                                return "TRIVAATCINE";
                            }else {
                                return "TRIVACTCINE";
                            }
                        } else if ("ADR".equalsIgnoreCase(equityType)) {
                            if(isATA(trade)){
                                return "TRIVAATADNE";
                            }else {
                                return "TRIVACTADNE";
                            }
                        }
                    } else if ("Coste Amortizado".equalsIgnoreCase(accountingLink)) {
                        if ("PS".equalsIgnoreCase(equityType)) {
                            if(isATA(trade)){
                                return "TRIVAATPSAM";
                            }else {
                                return "TRIVACTPSAM";
                            }
                        } else if ("PFI".equalsIgnoreCase(equityType)) {
                            if(isATA(trade)){
                                return "TRIVAATPFAM";
                            }else {
                                return "TRIVACTPFAM";
                            }
                        }
                    }else if("CS".equalsIgnoreCase(equityType)){
                        if(isATA(trade)){
                            return "TRIEMATAONE";
                        }else {
                            return "TRIEMCTAONE";
                        }
                    }else if("DERSUS".equalsIgnoreCase(equityType)){
                        if(isATA(trade)){
                            return "TRIEMATDSNE";
                        }else {
                            return "TRIEMCTDSNE";
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean isATA(Trade trade){
        return "BUY".equalsIgnoreCase(getDirection(trade.getProduct()));
    }

}
