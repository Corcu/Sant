package calypsox.tk.bo;


import java.util.Vector;

import calypsox.tk.confirmation.EquityCO2ConfirmationMsgBuilder;
import calypsox.tk.confirmation.EquityETFConfirmationMsgBuilder;
import calypsox.tk.confirmation.EquityVCO2ConfirmationMsgBuilder;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;


public class CALYPSOCONFIRMEquityMessageFormatter extends MessageFormatter {


    public String parseCALYPSOCONFIRM_XML(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                          BOTransfer transfer, DSConnection dsConn) {
        Product product = trade.getProduct();
        if(product != null && product instanceof Equity){
            Equity equity = (Equity) product;
            String equityType = equity.getSecCode("EQUITY_TYPE");
            if(!Util.isEmpty(equityType) && equityType.equalsIgnoreCase("CO2")){
                return new EquityCO2ConfirmationMsgBuilder(message, transfer, trade).build().toString();
            }
            else if(!Util.isEmpty(equityType) && equityType.equalsIgnoreCase("ETF")) {
                return new EquityETFConfirmationMsgBuilder(message, transfer, trade).build().toString();
            }
            else if(!Util.isEmpty(equityType) && equityType.equalsIgnoreCase("VCO2")) {
                return new EquityVCO2ConfirmationMsgBuilder(message, transfer, trade).build().toString();
            }
        }
        return EMPTY_STRING;
    }


}
