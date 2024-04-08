package calypsox.tk.lch.util;


import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;

/**
 * @author acd
 */
public class LchBean {

    public static final String UTI = "uti";
    public static final String USI = "usi";

    private String utivalue;
    private String usivalue;
    private String lchRef;
    private Long tradeid;
    private Double npv;
    private String currency;

    private Trade trade;

    public LchBean() {
    }

    public Double getNpv() {
        return npv;
    }

    public void setNpv(Double npv) {
        this.npv = npv;
    }

    public LchBean(String lchRef, String npv,String currency) {
        this.lchRef = lchRef;
        this.currency = currency;
        try{
            this.npv = Double.parseDouble(npv);
        }catch (Exception e){
            Log.error(this,"Cannot cast value: " +npv + " to double."  );
        }
    }

    public LchBean(String uti, String usi, String lchRef,String currency) {
        this.utivalue = uti;
        this.usivalue = usi;
        this.lchRef = lchRef;
        this.currency = currency;
    }

    public String getUti() {
        return utivalue;
    }

    public void setUti(String uti) {
        this.utivalue = uti;
    }

    public String getUsi() {
        return usivalue;
    }

    public void setUsi(String usi) {
        this.usivalue = usi;
    }

    public String getLchRef() {
        return lchRef;
    }

    public void setLchRef(String lchRef) {
        this.lchRef = lchRef;
    }

    public Long getTradeid() {
        return tradeid;
    }

    public void setTradeid(Long tradeid) {
        this.tradeid = tradeid;
    }

    public String getType(String type){

        if(type.equalsIgnoreCase("uti")){
            return this.utivalue;
        }else if(type.equalsIgnoreCase("usi")){
            return this.usivalue;
        }
        return "";
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }
}
