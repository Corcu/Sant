package calypsox.tk.confirmation.builder.equity;


import java.util.Locale;
import java.util.Optional;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOProductHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;

import calypsox.tk.confirmation.builder.CalConfirmationFinantialDataBuilder;
import calypsox.tk.report.quotes.FXQuoteHelper;


public class EquityETFConfirmationFinancialDataBuilder extends CalConfirmationFinantialDataBuilder {


    Equity equity;
    protected LEContact poContact;
    protected LEContact cptyContact;
	FXQuoteHelper fxQuoteHelper;
	protected LEContact agentCptyContact;
	protected LEContact agentPOContact;
	protected LegalEntity agentPO;
	protected LegalEntity agentCpty;
	protected SettleDeliveryInstruction agentSDIPO;
	protected SettleDeliveryInstruction agentSDICpty;


    public EquityETFConfirmationFinancialDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        if (trade.getProduct() instanceof Equity) {
            this.equity = (Equity) trade.getProduct();
        }
        int senderContactId = Optional.ofNullable(boMessage).map(BOMessage::getSenderContactId).orElse(0);
        int receiverContactId = Optional.ofNullable(boMessage).map(BOMessage::getReceiverContactId).orElse(0);
        this.poContact = BOCache.getLegalEntityContact(DSConnection.getDefault(), senderContactId);
        this.cptyContact = BOCache.getLegalEntityContact(DSConnection.getDefault(), receiverContactId);
        this.fxQuoteHelper = new FXQuoteHelper("OFFICIAL_ACCOUNTING");
		calculateAgentInfo();
    }

	@SuppressWarnings("rawtypes")
	private void calculateAgentInfo() {
		BOTransfer boXfer = boTransfer;

		if (boXfer == null) {
			TransferArray tArr;
			try {
				tArr = DSConnection.getDefault().getRemoteBackOffice().getBOTransfers(trade.getLongId());
				boXfer = Optional.ofNullable(tArr).flatMap(
						arr -> arr.stream().filter(boTr -> "SECURITY".equals(boTr.getTransferType())).findFirst())
						.get();
			} catch (Exception e) {
				Log.info(this, "No transfer was found. ", e);
			}
		}
		if (boXfer != null) {
			agentCptyContact = getAgentContactFromSDI(boXfer.getExternalSettleDeliveryId(), boXfer);
			agentCpty = getAgentFromSDI(boXfer.getExternalSettleDeliveryId(), boXfer);
			agentSDICpty = getSDI(boXfer.getExternalSettleDeliveryId());
			agentPOContact = getAgentContactFromSDI(boXfer.getInternalSettleDeliveryId(), boXfer);
			agentPO = getAgentFromSDI(boXfer.getInternalSettleDeliveryId(), boXfer);
			agentSDIPO = getSDI(boXfer.getInternalSettleDeliveryId());
		} else {
			Product product = trade.getProduct();
			Vector exceptions = new Vector();
			BOProductHandler handler = BOProductHandler.getHandler(product);
			Vector vXferRules = handler.generateTransferRules(trade, product, exceptions, DSConnection.getDefault());
			for (Object object : vXferRules) {
				TradeTransferRule tt = ((TradeTransferRule) object);
				tt.setReceiverSDStatus("Default");
				tt.setPayerSDStatus("Default");
				handler.setSettlementDeliveryInstructions(trade, tt, trade.getSettleDate(), new Vector(),
						DSConnection.getDefault());
				agentCptyContact = getAgentContactFromSDI(tt.getCounterPartySDId(), boXfer);
				agentCpty = getAgentFromSDI(tt.getCounterPartySDId(), boXfer);
				agentSDICpty = getSDI(tt.getCounterPartySDId());
				agentPOContact = getAgentContactFromSDI(tt.getProcessingOrgSDId(), boXfer);
				agentPO = getAgentFromSDI(tt.getProcessingOrgSDId(), boXfer);
				agentSDIPO = getSDI(tt.getProcessingOrgSDId());
			}
		}
	}

	private SettleDeliveryInstruction getSDI(int sdiId) {
		SettleDeliveryInstruction sdiLe = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), sdiId);
		return sdiLe;
	}

	private LegalEntity getAgentFromSDI(int sdiId, BOTransfer boXfer) {
		LegalEntity leAgent = null;
		SettleDeliveryInstruction sdiLe = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), sdiId);
		leAgent = BOCache.getLegalEntity(DSConnection.getDefault(), sdiLe.getAgentId());
		return leAgent;
	}

	private LEContact getAgentContactFromSDI(int sdiId, BOTransfer boXfer) {
		LEContact contact = null;
		SettleDeliveryInstruction sdiLe = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), sdiId);
		if (sdiLe != null && boXfer != null) {
			contact = BOCache.getContact(DSConnection.getDefault(), "Agent",
					BOCache.getLegalEntity(DSConnection.getDefault(), sdiLe.getAgent().getPartyId()),
					sdiLe.getAgentContactType(), trade.getProductType(), sdiLe.getAgentId(), trade.getSettleDate(),
					trade, boXfer);
		} else if (sdiLe != null) {
			contact = BOCache.getContact(DSConnection.getDefault(), "Agent",
					BOCache.getLegalEntity(DSConnection.getDefault(), sdiLe.getAgent().getPartyId()),
					sdiLe.getAgentContactType(), trade.getProductType(), trade.getBook().getLegalEntity().getId(),
					trade.getSettleDate());
		}
		return contact;
	}

    public String buildReturnStatus() {
        return String.valueOf(1);
    }


    public String buildOperationDate() {
        return Optional.ofNullable(trade).map(Trade::getTradeDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }


    public String buildEntryDate() {
        return Optional.ofNullable(trade).map(Trade::getEnteredDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }


    public String buildPortfolio() {
        return trade.getBook().getName();
    }


    public String buildDirection() {
        String buySell = "Buy";
        int buySellInd = Optional.ofNullable(equity).map(equity -> equity.getBuySell(trade)).orElse(1);
        if (buySellInd != 1) {
            buySell = "Sell";
        }
        return buySell;
    }


    public String buildSettlementDate() {
        return Optional.ofNullable(trade).map(Trade::getSettleDate).map(JDate::valueOf).map(JDate::toString).orElse("");
    }


    public String buildNominalValueAmount() {
        return Optional.ofNullable(trade).map(Trade::getQuantity).map(this::formatNumberAbs).orElse("");
    }


    public String buildTradeCurrency() {
        return Optional.ofNullable(trade).map(Trade::getTradeCurrency).orElse("");
}


    public String buildSettlementCurrency() {
        return Optional.ofNullable(trade).map(Trade::getSettleCurrency).orElse("");
    }


    public String buildCompliancePeriod() {
        String currency = "";
        CurrencyDefault ccyDefault = LocalCache.getCurrencyDefault(equity.getCurrency());
        if(ccyDefault != null){
            currency = ccyDefault.getCode();
        }
        if(!Util.isEmpty(currency) && "EUR".equalsIgnoreCase(currency)) {
            return "Fourth";
        }
        else if(!Util.isEmpty(currency) && "GBP".equalsIgnoreCase(currency)) {
            return "First";
        }
        return "";
    }


    public String buildAllowanceNumber() {
        return String.valueOf(trade.getQuantity());
    }


    public String buildAllowancePurchPrice() {
        return String.valueOf(trade.getNegociatedPrice());
    }


    public String buildAllowancePurchCurr() {
        return trade.getTradeCurrency();
    }


    public String buildTotalPurchPrice() {
        return String.valueOf(trade.getQuantity() * trade.getNegociatedPrice());
    }


    public String buildTotalPurchCurr() {
        return Optional.ofNullable(equity).map(Equity::getCurrency).orElse("");
    }


    public String buildBusinessDays() {
        String currency = "";
        CurrencyDefault ccyDefault = LocalCache.getCurrencyDefault(equity.getCurrency());
        if(ccyDefault != null){
            Vector<String> holidays = ccyDefault.getDefaultHolidays();
            if(!Util.isEmpty(holidays) && holidays.size()>0){
                currency = holidays.get(0);
            }
        }
        return currency;
    }


    public String buildBuyVatJurisdiction() {
        int buySellInd = Optional.ofNullable(equity).map(equity -> equity.getBuySell(trade)).orElse(1);
        //Buy
        if (buySellInd == 1) {
            return trade.getBook().getLegalEntity().getCountry();
        }
        //Sell
        else{
            return trade.getCounterParty().getCountry();
        }
    }


    public String buildSellVatJurisdiction() {
        int buySellInd = Optional.ofNullable(equity).map(equity -> equity.getBuySell(trade)).orElse(1);
        //Buy
        if (buySellInd == 1) {
            return trade.getCounterParty().getCountry();
        }
        //Sell
        else{
            return trade.getBook().getLegalEntity().getCountry();
        }
    }


    public String buildPaymentDate() {
        return Optional.ofNullable(trade).map(Trade::getSettleDate).map(JDate::valueOf).map(JDate::toString).orElse("");
    }


    public String buildDeliveryDate() {
        return Optional.ofNullable(trade).map(Trade::getSettleDate).map(JDate::valueOf).map(JDate::toString).orElse("");
    }


    public String buildCptyDeliveryBussDayLoc() {
        String currency = "";
        String country = trade.getCounterParty().getCountry();
        if(!Util.isEmpty(country)) {
            Vector<String> holidays = BOCache.getCountry(DSConnection.getDefault(), country).getDefaultHolidays();
            if (!Util.isEmpty(holidays) && holidays.size() > 0) {
                currency = holidays.get(0);
            }
        }
        return currency;
    }


	public String buildBrAccount() {
		String code = Optional.ofNullable(agentPO).map(LegalEntity::getCode).orElse("");
		String bic = Optional.ofNullable(agentPOContact).map(LEContact::getSwift).orElse("");
		return code.trim() + ((!Util.isEmpty(code) && !Util.isEmpty(bic)) ? " - " : "") + bic.trim();
	}

	public String buildBrHoldingAccount() {
		Account acc = BOCache.getAccount(DSConnection.getDefault(), agentSDIPO.getGeneralLedgerAccount());
		String name = Optional.ofNullable(acc).map(Account::getName).orElse("");
		String acAccount = Optional.ofNullable(agentSDIPO).map(SettleDeliveryInstruction::getAgentAccount).orElse("");
		return name.trim() + ((!Util.isEmpty(name) && !Util.isEmpty(acAccount)) ? " - " : "") + acAccount.trim();
	}

	public String buildCptyAccount() {
		String code = Optional.ofNullable(agentCpty).map(LegalEntity::getCode).orElse("");
		String bic = Optional.ofNullable(agentCptyContact).map(LEContact::getSwift).orElse("");
		return code.trim() + ((!Util.isEmpty(code) && !Util.isEmpty(bic)) ? " - " : "") + bic.trim();
	}

	public String buildCptyHoldingAccount() {
		Account acc = BOCache.getAccount(DSConnection.getDefault(), agentSDICpty.getGeneralLedgerAccount());
		String name = Optional.ofNullable(acc).map(Account::getName).orElse("");
		String acAccount = Optional.ofNullable(agentSDICpty).map(SettleDeliveryInstruction::getAgentAccount).orElse("");
		return name.trim() + ((!Util.isEmpty(name) && !Util.isEmpty(acAccount)) ? " - " : "") + acAccount.trim();
	}


    protected String formatNumberAbs(double number) {
        return String.format(Locale.ENGLISH, "%.6f", Math.abs(number));
    }

    public String buildCorporate() {
        return Optional.ofNullable(this.equity).map(etf -> etf.getCorporateName()).orElse("");
    }
    
    public String buildIsin() {
        return Optional.ofNullable(this.equity).map(etf -> etf.getSecCode("ISIN")).orElse("");
    }
    
    public String buildBroker() {
        return Optional.ofNullable(trade).map(etf -> trade.getBroker()).orElse("");
    }
    
    public String buildExchange() {
        return Optional.ofNullable(this.equity).map(etf -> etf.getExchange()).orElse("");
    }


    public String buildGrossAmount() {
        return formatNumberAbs(trade.getQuantity() * trade.getTradePrice());
    }


    private Double getAllFeesAmount(Trade trade){
        Double sumFeeAmount = 0.0;
        Vector<Fee> fees= Optional.ofNullable(trade).map(Trade::getFeesList).orElse(new Vector<>());
        if (!Util.isEmpty(fees)) {
            for (Fee fee : fees) {
                sumFeeAmount += convertFeeAmountToTradeSettleCurrency(trade.getSettleCurrency(), fee.getCurrency() ,fee.getAmount(), fee.getFeeDate());
            }
        }
        return sumFeeAmount;
    }


	public String buildFeeAmount(String feeType) {
		Double feeAmount= 0.0d;
		Vector<Fee> feeList = trade.getFeesList();
		if(feeList!=null && feeList.size()>0) {
			for(Fee fee : trade.getFeesList()) {
				if(fee != null && feeType.equalsIgnoreCase(fee.getType())) {
					feeAmount += convertFeeAmountToTradeSettleCurrency(trade.getSettleCurrency(), fee.getCurrency(), fee.getAmount(), fee.getFeeDate());
				}
			}
		}
		return String.valueOf(feeAmount);
	}


    public String buildNetAmount() {
        return formatNumberAbs(equity.calcSettlementAmount(trade));
    }


    protected Double convertFeeAmountToTradeSettleCurrency(String tradeCcy, String ccy, Double amount, JDate jDate) {
    	Double amountEur = amount;
        if (!tradeCcy.equalsIgnoreCase(ccy)) {
            QuoteValue quote = null;
            try {
                quote = fxQuoteHelper.getFXQuote(tradeCcy, ccy, jDate);
                if ((quote != null) && !Double.isNaN(quote.getClose())) {
                	amountEur = amount / quote.getClose();
                } else {
                    quote = fxQuoteHelper.getFXQuote(ccy, tradeCcy, jDate);
                    if ((quote != null) && !Double.isNaN(quote.getClose())) {
                        amountEur = amount * quote.getClose();
                    } else {
                        Log.error(this.getClass().getSimpleName(), "There is no quote on " + jDate + " for EUR/" + ccy);
                    }
                }
            }
            catch(MarketDataException e){
                    Log.error(this.getClass().getSimpleName(), "Could not get quote.");
            }
        }
        return amountEur;
    }


    public String buildRegTradetime() {
        return trade.getKeywordValue("TIME_EFX");
    }


    public String buildExecutionCenter() {
        return "BMTF";
    }


    public String buildCptyCountry() {
        return Optional.ofNullable(trade).map(amt -> String.valueOf(trade.getCounterParty().getCountry())).orElse("");
    }


}
