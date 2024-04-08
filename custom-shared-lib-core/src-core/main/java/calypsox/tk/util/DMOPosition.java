package calypsox.tk.util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;

import java.io.Serializable;


public class DMOPosition implements Serializable {
    public static JDate valuation_Date ;
    public int processing_Org;
    public String position_Type;
    public String product_Code;
    public int future_Month;
    public int future_Year;
    public long entity_Id;

    public float nominal;
    public float market_Value;

    public String cpty_full_name;

    public String direction;

    public String trade_settle_date;

    public String nominal_in_1000;

    public String getCpty_full_name() {
        return cpty_full_name;
    }

    public void setCpty_full_name(String cpty_full_name) {
        this.cpty_full_name = cpty_full_name;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getTrade_settle_date() {
        return trade_settle_date;
    }

    public void setTrade_settle_date(String trade_settle_date) {
        this.trade_settle_date = trade_settle_date;
    }

    public String getNominal_in_1000() {
        return nominal_in_1000;
    }

    public void setNominal_in_1000(String nominal_in_1000) {
        this.nominal_in_1000 = nominal_in_1000;
    }



    public static JDate getValuation_Date() {
        return valuation_Date;
    }

    public static void setValuation_Date(JDate valuation_Date) {
        DMOPosition.valuation_Date = valuation_Date;
    }

    public int getProcessing_Org() {
        return processing_Org;
    }

    public void setProcessing_Org(int processing_Org) {
        this.processing_Org = processing_Org;
    }

    public String getPosition_Type() {
        return position_Type;
    }

    public void setPosition_Type(String position_Type) {
        this.position_Type = position_Type;
    }

    public String getProduct_Code() {
        return product_Code;
    }

    public void setProduct_Code(String product_Code) {
        this.product_Code = product_Code;
    }

    public int getFuture_Month() {
        return future_Month;
    }

    public void setFuture_Month(int future_Month) {
        this.future_Month = future_Month;
    }

    public int getFuture_Year() {
        return future_Year;
    }

    public void setFuture_Year(int future_Year) {
        this.future_Year = future_Year;
    }

    public long getEntity_Id() {
        return entity_Id;
    }

    public void setEntity_Id(long entity_Id) {
        this.entity_Id = entity_Id;
    }

    public float getNominal() {
        return nominal;
    }

    public void setNominal(float nominal) {
        this.nominal = nominal;
    }

    public float getMarket_Value() {
        return market_Value;
    }

    public void setMarket_Value(float market_Value) {
        this.market_Value = market_Value;
    }
}
