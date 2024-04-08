package calypsox.tk.bo;

import java.util.Iterator;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.SDISelectorUtil;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;


/**
 * SDI selector for sec lending, check the call account margin call id
 * @author CedricAllain
 *
 */
public class SecLendingSDISelector extends SDISelectorUtil {
	
	public static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";
	public static final String ROLE_CLIENT = "Client";
	public static final String CALL_ACCOUNT = "CallAccount";
	
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public Vector getValidSDIList(Trade trade, TradeTransferRule tradeTransferRule, JDate settleDate,
                                  String legalEntity, String legalEntityRole, Vector exceptions, boolean includeNotPreferred,
                                  DSConnection dsCon) {

        Vector validSDI = super.getValidSDIList(trade, tradeTransferRule, settleDate, legalEntity, legalEntityRole,
                exceptions, includeNotPreferred, dsCon);
        
        Iterator<SettleDeliveryInstruction> it = validSDI.iterator();
        
        while(it.hasNext()) {
        	SettleDeliveryInstruction sdi = it.next();
        	if(sdi.getRole().equals(ROLE_CLIENT) && sdi.getAttribute(CALL_ACCOUNT) !=null ) {
        		SecLending secLending = (SecLending)trade.getProduct();
        		int marginCallContractId = secLending.getMarginCallContractId(trade);
        		if(marginCallContractId>0) {
	        		Account acc = BOCache.getAccount(dsCon, Util.toInt(sdi.getAttribute(CALL_ACCOUNT)));
	        		if(acc==null) {
	        			it.remove(); // remove invalid sdi
	        		}
	        		else
	        		{
	        		String acctProp = acc.getAccountProperty(MARGIN_CALL_CONTRACT);
		        	if(!Util.isEmpty(acctProp) && Util.toInt(acctProp) != marginCallContractId) {
		        			it.remove();
		        		}
	        		}
	        		
        		}
        	}
        }

        return validSDI;
        
    }

}
