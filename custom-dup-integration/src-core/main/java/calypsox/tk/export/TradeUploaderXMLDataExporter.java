package calypsox.tk.export;

import java.util.List;
import java.util.function.Predicate;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;

public class TradeUploaderXMLDataExporter implements AbstractUploaderXMLDataExporter {
    //Trade Keywords
    private static final String MESSAGE_ID = "MessageID";
    private static final String PO = "PO";
    private static final String MOVEMENT_TYPE = "MovementType";
    private static final String MUREX_ID = "MurexID";

    // MOVEMENT_TYPE
    private static final String IM = "IM";
    private static final String VM = "VM";
    private static final String CONTRACT_TYPE_CSD = "CSD";

    //Account Attribute
    private static final String CRE_CONTRACT_ATT = "MARGIN_CALL_CONTRACT";

	@Override
	public String export(Object sourceObject, UploaderXMLDataExporter exporter) {
		return null;
	}
	
	@Override
	public void linkBOMessage(Object sourceObject, BOMessage boMessage) {
	}

	@Override
	public void fillInfo(Object sourceObject, UploaderXMLDataExporter exporter, BOMessage boMessage) {
		Trade trade;
        if (sourceObject instanceof Trade) {
            trade = (Trade) sourceObject;
            if (null != boMessage) {
                trade.addKeyword(MESSAGE_ID, String.valueOf(boMessage.getLongId()));
            }
            trade.addKeyword(PO, getPOCode(trade.getBook()));
            trade.addKeyword(MOVEMENT_TYPE, getMovementType(trade));
            trade.addKeyword(MUREX_ID, getMurexId(trade));
        }
	}

    /**
     * @param trade
     * @return MurexId from Account found from the contract and MarginCall currency
     */
    private String getMurexId(Trade trade) {
        if (trade.getProduct() instanceof MarginCall) {
            final MarginCall product = (MarginCall) trade.getProduct();
            final int contractId = product.getMarginCallConfig().getId();
            final String currency = product.getCurrency();
            final int tradeBookId = trade.getBookId();
            final List<Account> accounts = BOCache.getAccountByAttribute(DSConnection.getDefault(), CRE_CONTRACT_ATT, String.valueOf(contractId));
            if (!Util.isEmpty(accounts)) {
                Predicate<Account> filteringPredicate = a -> a.getCurrency().equalsIgnoreCase(currency) && tradeBookId == a.getCallBookId();
                final Account account = accounts.stream().filter(filteringPredicate).findFirst().orElse(null);
                if (null != account) {
                    String murexID = account.getAccountProperty(MUREX_ID);
                    return !Util.isEmpty(murexID) ? murexID : "";
                }
            }
        }
        return "";
    }

    private String getPOCode(Book book) {
        return book != null ? book.getLegalEntity().getCode() : "";
    }

    private String getMovementType(Trade trade) {
        String movementType = VM;
        if (trade.getProduct() instanceof MarginCall) {
            MarginCall marginCall = (MarginCall) trade.getProduct();
            if (null != marginCall) {
                MarginCallConfig marginCallConf = marginCall
                        .getMarginCallConfig();
                if (null != marginCallConf && CONTRACT_TYPE_CSD
                        .equals(marginCallConf.getContractType())) {
                    movementType = IM;
                }
            }
        }
        return movementType;
    }
    
    @Override
	public String getIdentifier(Object sourceObject) {
    	Trade trade;
        if (sourceObject instanceof Trade) {
            trade = (Trade) sourceObject;
            return String.valueOf(trade.getLongId());
        }
		return "";
	}
}
