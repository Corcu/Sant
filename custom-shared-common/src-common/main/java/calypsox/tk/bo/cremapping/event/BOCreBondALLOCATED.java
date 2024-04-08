package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.TimeZone;

/**
 * @author x983373
 */

public class BOCreBondALLOCATED extends SantBOCre{

    private Product security;

    public BOCreBondALLOCATED(BOCre cre, Trade trade){super(cre, trade);}

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromBond(this.trade);
    }

    public void fillValues(){
        //String id = this.creId.toString();
        //id = "0"+id;
        //this.creId = Long.parseLong(id);
        this.aliasIdentifier = "ALLOCATED";
        this.isin = BOCreUtils.getInstance().loadIsin(this.security);
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade, this.boCre);
        this.internal = "N";
        this.deliveryType = BOCreUtils.getInstance().loadDeliveryType(this.trade);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.issuerName = BOCreUtils.getInstance().loadIssuerName(this.trade);
        this.productCurrency = BOCreUtils.getInstance().loadProductBondCurrency(this.trade);
        this.buySell = BOCreUtils.getInstance().loadBuySell(this.trade);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
    }

    @Override
    public String getCreLineJSon() {
        JSONObject json = null;
        try {
            json = new JSONObject(super.getCreLineJSon());
        } catch (JSONException e) {
            Log.error(this, e);
        }
        if (json != null){
            json.remove("payReceive");
            json.remove("amount");
            json.remove("currency");
            return json.toString();
        }
        return super.getCreLineJSon();
    }

    @Override
    protected String loadProductType() {
        return "Bond";
    }

    @Override
    protected String loadCreEventType() {
        if ("ALLOCATED".equals(this.trade.getStatus().toString())||"DUMMY_FULL_ALLOC".equals(this.trade.getStatus().toString())){
            return this.trade.getStatus().toString();
        }
        return "";
    }

    @Override
    protected String loadCreType() {
        return "NEW";
    }

    @Override
    protected Double getPosition() {
        return 0.0;
    }

    @Override
    protected JDate getCancelationDate() {
        return null;
    }

    @Override
    protected CollateralConfig getContract() {
        return null;
    }

    @Override
    protected Account getAccount() {
        return null;
    }
}
