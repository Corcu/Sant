package calypsox.tk.upload.uploader;


import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Fee;
import com.calypso.tk.upload.jaxb.TradeFee;
import com.calypso.tk.upload.util.UploaderTradeUtil;

import java.sql.Connection;
import java.util.*;


/**
 * @author aalonsop
 */
public class UploadCalypsoTradeEquity extends com.calypso.tk.upload.uploader.UploadCalypsoTradeEquity {


    //CO2
    private static final String TRADE_KEYWORD_UPI_REFERENCE = "UPI_REFERENCE";
    private static final String UPI_VALUE_CO2_PHYSICAL = "COMMODITY:ENVIRONMENTAL:EMISSIONS:SPOTFWD:PHYSICAL";
    private static final String UPI_VALUE_CO2_CASH = "COMMODITY:ENVIRONMENTAL:EMISSIONS:SWAP:CASH";
    private static final String PRODUCT_SEC_CODE_EQUITY_TYPE = "EQUITY_TYPE";
    private static final String CO2 = "CO2";
    private static final String VCO2 = "VCO2";
    private static final String TRADE_KWRD_DELIVERY_TYPE = "DeliveryType";
    private static final String TRADE_KWRD_DELIVERY_TYPE_DAP = "DAP";
    private static final String TRADE_KWRD_MX_DELIVERY_TYPE = "MX_Delivery_Type";
    private static final String MX_DELIVERY_TYPE_PHYSICAL = "Physical";
    private static final String MX_DELIVERY_TYPE_CASH = "Cash";

    // ETF
    private static final String ACTION_AMEND = "AMEND";
    private static final String FEE_TYPE_BRK = "BRK";
    private static final String FEE_TYPE_ETF_ADDITIONAL_FEE = "ETF_ADDITIONAL_FEE";
    private static final String EQUITY_TYPE = "EQUITY_TYPE";
    private static final String EQUITY_TYPE_ETF = "ETF";


    @Override
    public void upload(CalypsoObject object, Vector<BOException> errors, Object dbCon, boolean saveToDB1) {
        super.upload(object,errors,dbCon,saveToDB1);
        this.addCO2Keywords();
        this.addSLBKeyword();
    }


    private void addCO2Keywords() {
        if (this.trade.getProduct() instanceof Equity) {
            Equity equity = (Equity) this.trade.getProduct();
            String equityType = equity.getSecCode(PRODUCT_SEC_CODE_EQUITY_TYPE);
            String mxDeliveryType = this.trade.getKeywordValue(TRADE_KWRD_MX_DELIVERY_TYPE);
            boolean isCO2 = !Util.isEmpty(equityType) && (CO2.equalsIgnoreCase(equityType) || VCO2.equalsIgnoreCase(equityType));
            boolean isPhysical = !Util.isEmpty(mxDeliveryType) && MX_DELIVERY_TYPE_PHYSICAL.equalsIgnoreCase(mxDeliveryType);
            boolean isCash = !Util.isEmpty(mxDeliveryType) && MX_DELIVERY_TYPE_CASH.equalsIgnoreCase(mxDeliveryType);
            if(isCO2) {
                this.trade.addKeyword(TRADE_KWRD_DELIVERY_TYPE,TRADE_KWRD_DELIVERY_TYPE_DAP);
                if (isPhysical) {
                    this.trade.addKeyword(TRADE_KEYWORD_UPI_REFERENCE, UPI_VALUE_CO2_PHYSICAL);
                } else if (isCash) {
                    this.trade.addKeyword(TRADE_KEYWORD_UPI_REFERENCE, UPI_VALUE_CO2_CASH);
                }
            }
        }
    }


    private void addSLBKeyword(){
        String poCode=Optional.ofNullable(this.trade).map(Trade::getBook)
                .map(Book::getLegalEntity).map(LegalEntity::getCode).orElse("");
        if("BDSD".equals(poCode)){
            this.trade.addKeyword("FO_SYSTEM","MX EQ");
        }
    }


    public void handleFees(Trade trade, CalypsoTrade calypsoTrade, Connection connection) {
        String action = UploaderTradeUtil.isValidAction(connection, calypsoTrade.getAction());
        String strTradeSource = this.getTradeSource(calypsoTrade);
        Vector<com.calypso.tk.bo.Fee> feeVector = this.getFees(trade, calypsoTrade, connection);
        boolean replaceIfEmpty = false;
        Vector<String> replaceFeesIfEmptyProductList = UploaderTradeUtil.getDomainValue("UploaderRemoveFeesForProduct");
        if (replaceFeesIfEmptyProductList != null && replaceFeesIfEmptyProductList.contains(calypsoTrade.getProductType())) {
            replaceIfEmpty = true;
        }

        //// ETF Fee management
        String equityType = ((Equity) trade.getProduct()).getSecCode(EQUITY_TYPE);
        if (EQUITY_TYPE_ETF.equalsIgnoreCase(equityType) && ACTION_AMEND.equalsIgnoreCase(action)) {

            Vector<com.calypso.tk.bo.Fee> tradeFeeList = this.trade.getFeesList();
            TradeFee amendTradeList = this.calypsoTrade.getTradeFee();
            boolean existBrkOnTrade = existeOnFeeList(FEE_TYPE_BRK, tradeFeeList);
            boolean existBrkOnAmend = existeOnFeeList(FEE_TYPE_BRK, amendTradeList);
            boolean existAddFeeOnTrade = existeOnFeeList(FEE_TYPE_ETF_ADDITIONAL_FEE, tradeFeeList);
            boolean existAddFeeOnAmend = existeOnFeeList(FEE_TYPE_ETF_ADDITIONAL_FEE, amendTradeList);

            if (existBrkOnTrade || existBrkOnAmend || existAddFeeOnTrade || existAddFeeOnAmend) {
                Vector<com.calypso.tk.bo.Fee> finalTradeFees = new Vector<com.calypso.tk.bo.Fee>();
                // Vuelca las fees del amenf en el finalTradeList
                if (amendTradeList != null && amendTradeList.getFee() != null && amendTradeList.getFee().size() > 0) {
                    Iterator<Fee> feeIterator = amendTradeList.getFee().iterator();
                    while (feeIterator.hasNext()) {
                        Fee fee = (Fee) feeIterator.next();
                        com.calypso.tk.bo.Fee newFee = this.getFee(trade, calypsoTrade, fee, connection);
                        if (trade != null && newFee != null && !this.isReapperFee(trade, connection, newFee, trade.getKeywordAsJDate("TransferDate"))) {
                            finalTradeFees.add(newFee);
                        }
                    }
                }
                // Si las fees del trade original no estan en la finalTradeList, las incluye
                if (tradeFeeList != null && tradeFeeList.size() > 0) {
                    for (com.calypso.tk.bo.Fee fee : tradeFeeList) {
                        if (!existeOnFeeList(fee.getType(), finalTradeFees)) {
                            finalTradeFees.add(fee);
                        }
                    }
                }

                // si existe Brk en el trade y no existe Brk en el amend
                if (existBrkOnTrade && !existBrkOnAmend) {
                    finalTradeFees = removeFeeOnList(FEE_TYPE_BRK, finalTradeFees);
                }

                // si existe AddFee en el trade y no existe AddFee en el amend
                if (existAddFeeOnTrade && !existAddFeeOnAmend) {
                    finalTradeFees = removeFeeOnList(FEE_TYPE_ETF_ADDITIONAL_FEE, finalTradeFees);
                }

                trade.setFees(finalTradeFees);
            }
        }
        //// ETF Fee management

        else if (!Util.isEmpty(feeVector) || replaceIfEmpty) {
            if (strTradeSource == null) {
                strTradeSource = "DEF";
            }
            String RA = this.replaceOrAppend(strTradeSource, action);
            if ("R".equalsIgnoreCase(RA)) {
                trade.setFees(feeVector);
            } else if ("A".equalsIgnoreCase(RA)) {
                this.appendFee(trade, calypsoTrade, feeVector);
            }
        }
    }


    public boolean existeOnFeeList(String feeType, Vector<com.calypso.tk.bo.Fee> feeList) {
        boolean exist = false;
        if(feeList!=null && feeList.size()>0){
            for (com.calypso.tk.bo.Fee fee : feeList) {
                if (fee!=null && feeType.equalsIgnoreCase(fee.getType())) {
                    return true;
                }
            }
        }
        return exist;
    }


    public boolean existeOnFeeList(String feeType, TradeFee feeList) {
        boolean exist = false;
        if(feeList!=null && feeList.getFee()!=null && feeList.getFee().size()>0) {
            Iterator<Fee> feeIterator = feeList.getFee().iterator();
            while (feeIterator.hasNext()) {
                Fee fee = feeIterator.next();
                if (fee != null && feeType.equalsIgnoreCase(fee.getFeeType())) {
                    return true;
                }
            }
        }
        return exist;
    }


    public Vector<com.calypso.tk.bo.Fee> removeFeeOnList(String feeType, Vector<com.calypso.tk.bo.Fee> feeList) {
        Vector<com.calypso.tk.bo.Fee> finalTradeFees = new Vector<com.calypso.tk.bo.Fee>();
        if(feeList!=null && feeList.size()>0){
            for (com.calypso.tk.bo.Fee fee : feeList) {
                if (fee!=null && !feeType.equalsIgnoreCase(fee.getType())) {
                    finalTradeFees.add(fee);
                }
            }
        }
        return finalTradeFees;
    }


}
