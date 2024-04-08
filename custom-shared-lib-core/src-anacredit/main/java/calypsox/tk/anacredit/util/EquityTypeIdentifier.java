package calypsox.tk.anacredit.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Security;
import com.calypso.tk.service.DSConnection;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class EquityTypeIdentifier {
    private boolean isEQ = false;
    private boolean isEQDES = false;
    private boolean isEQPRF = false;
    private boolean isEQNOT = false;
    private boolean isEQPLZ = false;
    private boolean isEQPreferente = false;

    public  static final String EQ  = "EQ";
    public  static final String EQDES = "EQDES";
    public  static final String EQPRF = "EQPRF";
    public  static final String EQNOT = "EQNOT";

    private Trade trade =  null;
    private InventorySecurityPosition position =  null;

    public static List<String> getEquityPositionBasedTypes  () {
        return Arrays.asList(EQ, EQDES, EQPRF, EQNOT);
    }

    public EquityTypeIdentifier(InventorySecurityPosition position) {
        if (position != null) {
            initPosition(position);
        }
    }

    public Trade getTrade() {
        return trade;
    }

    public InventorySecurityPosition getPosition() {
        return position;
    }

    public EquityTypeIdentifier(JDatetime valDate, Trade trade) {
        this.trade = trade;
        if (trade != null && valDate != null) {
            this.trade = trade;
            if (trade.getTradeDate().before(valDate)
                    && trade.getSettleDate().after(valDate.getJDate(TimeZone.getDefault()))) {
                this.isEQPLZ = true;
            }
        }
    }

    public Product getProduct() {
        if (trade != null) {
            return trade.getProduct();
        }
        else if (position != null) {
            return position.getProduct();
        }
        return null;
    }


    public String getISIN() {
        if (getTrade() !=null) {
            return getTrade().getProduct().getSecCode("ISIN");
        } else if (getPosition() != null) {
            return getPosition().getProduct().getSecCode("ISIN");
        }
        return null;
    }

    public Double getNominal() {
        if (position!= null) {
            return position.getTotal();
        }
        else if (trade != null){
            return trade.computeNominal();
        }
        return null;
    }

    public String getCcy() {
        if (position!= null) {
            return position.getSettleCurrency();
        }
        else if (trade != null){
            return trade.getSettleCurrency();
        }
        return null;
    }

    private void initPosition(InventorySecurityPosition position) {
        this.position = position;
        this.isEQPreferente =
                ("PS".equalsIgnoreCase(position.getProduct().getSecCode("EQUITY_TYPE")));

        Double nominal = getNominal();

        if (nominal == 0.0d) {
            this.isEQNOT = true;
        }
        else if (isEQPreferente) {
            if (nominal < 0.00d) {
                this.isEQDES = true;
            }
            else if (nominal > 0.0d) {
                this.isEQPRF = true;
            }
        }
        else if (nominal < 0.00d ) {
            this.isEQDES = true;
        }
        else if (nominal > 0.0d) {
            this.isEQ = true;
       }
    }

    public Double computeTradeSettlementAmount() {
        if (getTrade() != null) {
            Double dbl = getTrade().getProduct().calcSettlementAmount(getTrade());
            if (dbl != null) {
                return dbl;
            }
        }
        return null;
    }

    public boolean isEQPreferente() {
        return isEQPreferente;
    }

    public boolean isEQ() {
        return isEQ;
    }

    public boolean isEQDES() {
        return isEQDES;
    }

    public boolean isEQPRF() {
        return isEQPRF;
    }

    public boolean isEQNOT() {
        return isEQNOT;
    }

    public boolean isEQPLZ() {
        return isEQPLZ;
    }

    public LegalEntity getCounterPartyOrAgent()  {
        if (position != null) {
            return position.getAgent() ;
        } else {
            return trade.getCounterParty();
        }
    }

    public LegalEntity getProductIssuer()  {
        if (position != null) {
            final int securityId = position.getSecurityId(); // we retrieve the product
            final Product product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), securityId);
            return getProductIssuer(product);
        } else {
            return getProductIssuer(trade.getProduct());
        }
    }

    private LegalEntity getProductIssuer(Product product) {
        if (product != null) {
            if (product instanceof Security) {
                Security security = (Security) product;
                return BOCache.getLegalEntity(DSConnection.getDefault(),security.getIssuerId());
            }
        }
        return null;
    }

    public Product getUnderlying() {
        if ( position!= null) {
           return position.getProduct();
        }
        else   if (trade!= null) {
            return trade.getProduct();
        }
        return null;
    }

    public Security getSecurity() {
        if ( position!= null && position.getProduct() instanceof Security) {
                return (Security) position.getProduct();
        }
        else if (trade!= null && trade.getProduct() instanceof  Security) {
            return (Security) trade.getProduct();
        }
        return null;
    }

    public Book getBook() {
        if ( position!= null) {
            return position.getBook();
        }
        else   if (trade!= null) {
            return trade.getBook();
        }
        return null;
    }


}
