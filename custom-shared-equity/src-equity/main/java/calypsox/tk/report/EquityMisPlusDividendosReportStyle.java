package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.report.util.UtilReport;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Vector;


public class EquityMisPlusDividendosReportStyle extends TradeReportStyle {

  public static final String ORIGIN = "ORIGIN";
  public static final String PROCESSDATE = "PROCESSDATE";
  public static final String DIVIDENDID = "DIVIDENDID";
  public static final String BRANCH = "BRANCH";
  public static final String BRANCH_ID = "BRANCH_ID";
  public static final String ENTITY = "ENTITY";
  public static final String ACCOUNTING_CENTER = "ACCOUNTING_CENTER";
  public static final String SOURCESYSTEM = "SOURCESYSTEM";
  public static final String INSTRUMENT = "INSTRUMENT";
  public static final String INSTRUMENT_ID = "INSTRUMENT_ID";
  public static final String INSTRTYPE = "INSTRTYPE";
  public static final String COMMON = "COMMON";
  public static final String ISIN = "ISIN";
  public static final String ISINDESC = "ISINDESC";
  public static final String STATUSDEAL = "STATUSDEAL";
  public static final String MARKET = "MARKET";
  public static final String ISIN_CURRENCY = "ISIN_CURRENCY";
  public static final String PAYMENT_CURRENCY = "PAYMENT_CURRENCY";
  public static final String ISSUERNIF = "ISSUERNIF";
  public static final String ISSUERGLS = "ISSUERGLS";
  public static final String ISSUERCODE = "ISSUERCODE";
  public static final String ISSUERDESC = "ISSUERDESC";
  public static final String ISSUERSECTOR = "ISSUERSECTOR";
  public static final String ISSUERCOUNTRY = "ISSUERCOUNTRY";
  public static final String RECORD_DATE = "RECORD DATE";
  public static final String PAYMENT_DATE = "PAYMENT DATE";
  public static final String OURCUSTODIANCODE = "OURCUSTODIANCODE";
  public static final String OURCUSTODIANACCOUNT = "OURCUSTODIANACCOUNT";
  public static final String OURCUSTODIANSWIFT = "OURCUSTODIANSWIFT";
  public static final String OURCUSTODIANGLS = "OURCUSTODIANGLS";
  public static final String OURCUSTODIANDESC = "OURCUSTODIANDESC";
  public static final String OURCUSTODIANCOUNTRY = "OURCUSTODIANCOUNTRY";
  public static final String FIXING = "FIXING";
  public static final String UNITGROSSAMOUNT = "UNITGROSSAMOUNT";
  public static final String GROSSDIVIDENDEUR = "GROSSDIVIDENDEUR";
  public static final String GROSSDIVIDENDDIVI = "GROSSDIVIDENDDIVI";
  public static final String EQUITY_TYPE = "EQUITY_TYPE";
  public static final String EQUITY_TYPE_DESC = "EQUITY_TYPE_DESC";
  public static final String SECTOR_CONTABLE_EMISOR = "SECTOR CONTABLE EMISOR";
  public static final String RETENTION = "RETENTION";
  public static final String TOTALRETENTION = "TOTALRETENTION";
  public static final String NETGROSSDIVIDEND = "NETGROSSDIVIDEND";
  public static final String PERSONTYPE = "PERSONTYPE";
  public static final String DIRECTION = "DIRECTION";
  public static final String FILLER1 = "FILLER1";
  public static final String FILLER2 = "FILLER2";
  public static final String FILLER3 = "FILLER3";
  public static final String FILLER4 = "FILLER4";
  public static final String FILLER5 = "FILLER5";

  public static final String LE_ATTR_SECTORCONTABLE = "SECTORCONTABLE";
  public static final long serialVersionUID = 1L;

  @SuppressWarnings("rawtypes")
  @Override
  public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

    if (row == null) {
      return null;
    }

    final Trade trade = row.getProperty(ReportRow.TRADE);
    final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
    final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
    final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
    final Product product = trade.getProduct();

    if(product == null){
      return null;
    }

    Product underlyingProduct = product.getUnderlyingProduct();
    if (underlyingProduct instanceof Equity && product instanceof CA) {
      CA ca = (CA) product;
      Equity equity = (Equity) underlyingProduct;

      if (columnName.equals(ORIGIN)) {
            return "800018693";
        }

        if (columnName.equals(PROCESSDATE)) {
            return formatDate(valDate);
        }

        if (columnName.equals(DIVIDENDID)) {
            return trade.getLongId();
        }

        if (columnName.equals(BRANCH)) {
            return trade.getBook().getLegalEntity().getCode();
        }

      if (columnName.equals(BRANCH_ID)) {
        return trade.getBook().getLegalEntity().getLongId();
      }

      if (columnName.equals(ENTITY)) {
          return BOCreUtils.getInstance().getEntityCod(trade.getBook().getLegalEntity().getCode(), false);
      }

      if (columnName.equals(ACCOUNTING_CENTER)) {
          return BOCreUtils.getInstance().getCentroContable(equity, trade.getBook().getLegalEntity().getCode(), false);
      }

      if (columnName.equals(SOURCESYSTEM)) {
        return "MUREX EQ";
      }

      if (columnName.equals(INSTRUMENT)) {
        return trade.getProductType();
      }

      if (columnName.equals(INSTRUMENT_ID)) {
          if ("CounterParty".equals(trade.getRole())) {
              Long tradeId = Long.parseLong(trade.getKeywordValue("CASource"));
              String productType = trade.getKeywordValue("CASourceProductType");
              if(tradeId!=null && tradeId!=0 && !Util.isEmpty(productType) && ("Repo".equalsIgnoreCase(productType) || "SecLending".equals(productType))) {
                  Trade sourceTrade = null;
                  try {
                    sourceTrade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
                    if (sourceTrade != null) {
                        return sourceTrade.getKeywordValue("PartenonAccountingID");
                    }
                  } catch (CalypsoServiceException e) {
                    Log.error(this, e);
                  }
              }
          }
          return "";
      }

      if (columnName.equals(INSTRTYPE)) {
          if("CounterParty".equals(trade.getRole())){
              return trade.getKeywordValue("CASourceProductType");
          }
          return "Equity";
      }

      if (columnName.equals(COMMON)) {
          return equity.getSecCode("Common");
      }

      if (columnName.equals(ISIN)) {
        return equity.getSecCode(SecCode.ISIN);
      }

      if (columnName.equals(ISINDESC)) {
        return equity.getDescription();
      }
      
      if(columnName.equals(STATUSDEAL)) {
    	return "VIVA";
      }

      if(columnName.equals(DIRECTION)) {
          return ca.getBuySell(trade)  == 1 ? "Receive" : "Pay";
      }

      if (columnName.equals(MARKET)) {
        return equity.getExchange();
      }

      if (columnName.equals(ISIN_CURRENCY)) {
        return equity.getCurrency();
      }

      if (columnName.equals(PAYMENT_CURRENCY)) {
        return ca.getCurrency();
      }
      
      if(columnName.equals(ISSUERNIF)) {
    	LegalEntity le = equity.getIssuer();
		if(le!=null)
			return le.getExternalRef();
      }

      if (columnName.equals(ISSUERGLS)) {
        LegalEntity le = equity.getIssuer();
        return le!=null ? le.getCode() : "";
      }

      if (columnName.equals(ISSUERCODE)) {
        LegalEntity le = equity.getIssuer();
        return le!=null ? le.getLongId() : "";
      }

      if (columnName.equals(ISSUERDESC)) {
        LegalEntity le = equity.getIssuer();
        return le!=null ? le.getName() : "";
      }

      if (columnName.equals(ISSUERSECTOR)) {
        final LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, equity.getIssuerId(), "ALL", LE_ATTR_SECTORCONTABLE);
        return attr!=null ? attr.getAttributeValue() : "";
      }

      if (columnName.equals(ISSUERCOUNTRY)) {
        LegalEntity le = equity.getIssuer();
        if(le!=null) {
        	Country country =BOCache.getCountry(DSConnection.getDefault(), le.getCountry());
			if(country!=null)
				return country.getISOCode();
        }
        return "";
      }

      if (columnName.equals(RECORD_DATE)) {
        return formatDate(ca.getRecordDate());
      }

      if (columnName.equals(PAYMENT_DATE)) {
        return formatDate(ca.getValueDate());
      }

      if (columnName.equals(OURCUSTODIANCODE)) {
        if("Agent".equals(trade.getRole())){
            LegalEntity le = trade.getCounterParty();
            return le!=null ? le.getLongId() : "";
        }
        return "";
      }

      if (columnName.equals(OURCUSTODIANACCOUNT)) {
          if("Agent".equals(trade.getRole())){
            return trade.getKeywordValue("CAAgentAccountId");
          }
          return "";
      }

      if (columnName.equals(OURCUSTODIANSWIFT)) {
          if("Agent".equals(trade.getRole())){
              LegalEntity le = trade.getCounterParty();
              Vector contacts = null;
              LEContact contact = null;
              try {
                  contacts = DSConnection.getDefault().getRemoteReferenceData().getLEContacts(le.getId());
                  if (null != contacts) {
                      for (int i = 0; i < contacts.size(); i++) {
                          contact = (LEContact) contacts.get(i);
                          Vector<String> products = contact.getProductTypeList();
                          if ("Agent".equals(contact.getContactType()) || "ALL".equals(contact.getContactType()) &&
                                  contact.getProductTypeList().contains("Equity") || contact.getProductTypeList().contains("ALL") &&
                                  !Util.isEmpty(contact.getSwift())) {
                              return contact.getSwift();
                          }
                      }
                  }
              } catch (final RemoteException e) {
                  Log.error(this, e);
              }

          }
          return "";
      }

      if (columnName.equals(OURCUSTODIANGLS)) {
          if("Agent".equals(trade.getRole())){
              LegalEntity le = trade.getCounterParty();
              return le!=null ? le.getCode() : "";
          }
          return "";
      }

      if (columnName.equals(OURCUSTODIANDESC)) {
          if("Agent".equals(trade.getRole())){
              LegalEntity le = trade.getCounterParty();
              return le!=null ? le.getName() : "";
          }
          return "";
      }

      if (columnName.equals(OURCUSTODIANCOUNTRY)) {
          if("Agent".equals(trade.getRole())){
              LegalEntity le = trade.getCounterParty();
              if(le!=null) {
                  Country country =BOCache.getCountry(DSConnection.getDefault(), le.getCountry());
                  if(country!=null)
                      return country.getISOCode();
              }
              return "";
          }
          return "";
      }

      if (columnName.equals(EQUITY_TYPE)) {
        return equity.getSecCode(EQUITY_TYPE);
      }

      if (columnName.equals(EQUITY_TYPE_DESC)) {
        String equityType = equity.getSecCode(EQUITY_TYPE);
            if(!Util.isEmpty(equityType)){
                switch (equityType) {
                    case "ADR":
                        return "RV ADR";
                    case "CS":
                        return "RV Acciones Ordinarias";
                    case "PROP":
                        return "RV Acciones Propias";
                    case "DERSUS":
                        return "RV Derechos de Suscripción";
                    case "PEGROP":
                        return "RV Participaciones Empresas del Grupo";
                    case "PFI":
                        return "RV Participaciones Fondos";
                    case "PS":
                        return "RV Participaciones: Preferentes";
                    default:
                        return "";
                }
            }
        return "";
      }

      if (columnName.equals(RETENTION)) {
        if ("Agent".equals(trade.getRole())) {
            Double settlementAmount = ((SignedAmount) super.getColumnValue(row, "SettlementAmount",errors)).get();
            Double otherAmount = trade.getQuantity()*ca.getAmount();
            Double totalAmount = Math.abs((settlementAmount-otherAmount)/otherAmount*100);
            return formatResult(round(totalAmount,8).toString()) + "%";
        }
        else if ("CounterParty".equals(trade.getRole())) {
            return "0%";
        }
        else if ("ProcessingOrg".equals(trade.getRole())) {
            Double settlementAmount = ((SignedAmount) super.getColumnValue(row, "SettlementAmount",errors)).get();
            Double grossAmount = trade.getQuantity()*ca.getAmount();
            Double totalAmount = Math.abs((settlementAmount-grossAmount)/grossAmount*100);
            return formatResult(round(totalAmount,8)).toString() + "%";
        }
        return "";
      }

      if (columnName.equals(TOTALRETENTION)) {
          if ("Agent".equals(trade.getRole())) {
              Double settlementAmount = ((SignedAmount) super.getColumnValue(row, "SettlementAmount",errors)).get();
              Double grossAmount = trade.getQuantity()*ca.getAmount();
              Double totalAmount = Math.abs(settlementAmount-grossAmount);
              return formatResult(totalAmount).toString();

          }
          else if ("CounterParty".equals(trade.getRole())) {
            return "0";
          }
          else if ("ProcessingOrg".equals(trade.getRole())) {
              Double grossAmount = trade.getQuantity()*ca.getAmount();
              Double settlementAmount = ((SignedAmount) super.getColumnValue(row, "SettlementAmount",errors)).get();
              Double totalAmount = Math.abs(settlementAmount-grossAmount);
              return formatResult(totalAmount).toString();
          }
          return "";
      }

      if (columnName.equals(NETGROSSDIVIDEND)) {
          if ("Agent".equals(trade.getRole())) {
              SignedAmount settlementAmount = (SignedAmount) super.getColumnValue(row, "SettlementAmount",errors);
              return formatResult(settlementAmount.get());
          }
          else if ("CounterParty".equals(trade.getRole())) {
              SignedAmount settlementAmount = (SignedAmount) super.getColumnValue(row, "SettlementAmount",errors);
              return formatResult(settlementAmount.get());
          }
          else if ("ProcessingOrg".equals(trade.getRole())) {
              SignedAmount settlementAmount = (SignedAmount) super.getColumnValue(row, "SettlementAmount",errors);
              return formatResult(settlementAmount.get());
          }
          return "";
      }

      if (columnName.equals(FIXING)) {
          return formatResult(getFixing(trade,ca,pricingEnv));
      }

      if (columnName.equals(UNITGROSSAMOUNT)) {
          if ("Agent".equals(trade.getRole())) {
              return formatResult(ca.getAmount());
          }
          else if ("CounterParty".equals(trade.getRole())) {
              return formatResult(ca.getAmount());
          }
          else if ("ProcessingOrg".equals(trade.getRole())) {
              return formatResult(ca.getAmount());
          }
          return "";
      }

      if (columnName.equals(GROSSDIVIDENDEUR)) {
          Double amount = trade.getQuantity()*ca.getAmount();
          Double amountEur = 0.0;
          try {
              QuoteValue quote = null;
              if ("Agent".equals(trade.getRole())) {
                  if("EUR".equalsIgnoreCase(trade.getTradeCurrency())){
                      amountEur = amount;
                  }
                  else {
                      quote = pricingEnv.getQuoteSet().getFXQuote("EUR", trade.getTradeCurrency(), ca.getRecordDate());
                      if ((quote != null) && !Double.isNaN(quote.getClose())) {
                          amountEur = amount / quote.getClose();
                      } else {
                          quote = pricingEnv.getQuoteSet().getFXQuote(trade.getTradeCurrency(), "EUR", ca.getRecordDate());
                          if ((quote != null) && !Double.isNaN(quote.getClose())) {
                              amountEur = amount * quote.getClose();
                          }
                      }
                  }
              }
              else if ("CounterParty".equals(trade.getRole())) {
                  if("EUR".equalsIgnoreCase(trade.getTradeCurrency())){
                      amountEur = amount;
                  }
                  else {
                      quote = pricingEnv.getQuoteSet().getFXQuote("EUR", trade.getTradeCurrency(), ca.getRecordDate());
                      if ((quote != null) && !Double.isNaN(quote.getClose())) {
                          amountEur = amount / quote.getClose();
                      } else {
                          quote = pricingEnv.getQuoteSet().getFXQuote(trade.getTradeCurrency(), "EUR", ca.getRecordDate());
                          if ((quote != null) && !Double.isNaN(quote.getClose())) {
                              amountEur = amount * quote.getClose();
                          }
                      }
                  }
              }
              else if ("ProcessingOrg".equals(trade.getRole())) {
                  if("EUR".equalsIgnoreCase(trade.getTradeCurrency())){
                      amountEur = amount;
                  }
                  else {
                      quote = pricingEnv.getQuoteSet().getFXQuote("EUR", trade.getTradeCurrency(), ca.getRecordDate());
                      if ((quote != null) && !Double.isNaN(quote.getClose())) {
                          amountEur = amount / quote.getClose();
                      } else {
                          quote = pricingEnv.getQuoteSet().getFXQuote(trade.getTradeCurrency(), "EUR", ca.getRecordDate());
                          if ((quote != null) && !Double.isNaN(quote.getClose())) {
                              amountEur = amount * quote.getClose();
                          }
                      }
                  }
              }
              if(amountEur == 0.0){
                  amountEur = amount;
              }
              return formatResult(amountEur);
          } catch (MarketDataException e) {
              Log.error(this, "Could not get quote.");
          }
          return "";
      }

      if (columnName.equals(GROSSDIVIDENDDIVI)) {
          if ("Agent".equals(trade.getRole())) {
              return formatResult(trade.getQuantity()*ca.getAmount());
          }
          else if ("CounterParty".equals(trade.getRole())) {
              return formatResult(trade.getQuantity()*ca.getAmount());
          }
          else if ("ProcessingOrg".equals(trade.getRole())) {
              return formatResult(trade.getQuantity()*ca.getAmount());
          }
          return "";
      }

      if (columnName.equals(FILLER1)) {
        return trade.getRole();
      }

      if (columnName.equals(FILLER2)) {
        return trade.getBook().getName();
      }

      // DIVNEGOCIADO
      if (columnName.equals(FILLER3)) {
          if ("CounterParty".equals(trade.getRole())) {
              if(!Util.isEmpty(trade.getKeywordValue("ContractDivRate"))) {
                  return formatResult(Double.valueOf(trade.getKeywordValue("ContractDivRate")));
              }
          }
          return "";
      }

      // CLAIMDIVREQ
      if (columnName.equals(FILLER4)) {
          if ("CounterParty".equals(trade.getRole())) {
              Double settlementAmount = ((SignedAmount) super.getColumnValue(row, "SettlementAmount",errors)).get();
              Double grossAmount = trade.getQuantity()*ca.getAmount();
              Double totalAmount = settlementAmount-grossAmount;
              return formatResult(round(totalAmount,3));
          }
          return "";
      }

      if (columnName.equals(FILLER5)) {
        return "";
      }

      return formatResult(super.getColumnValue(row, columnName, errors));
    }

    return "";

  }


  @SuppressWarnings("rawtypes")
public String getKeywordValue(ReportRow row, String keyword, Vector errors) {
    final Object value = super.getColumnValue(row, TRADE_KEYWORD_PREFIX + keyword, errors);
    if (value != null) {
      return value.toString();
    }
    return null;
  }


  public static Object formatResult(Object o) {
    return UtilReport.formatResult(o, '.');
  }


  private String formatDate(JDate jDate){
    String date = "";
    if (jDate != null) {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
      date = format.format(jDate.getDate());
    }
    return date;
  }
  
  protected Double getFixing(Trade trade, CA ca, PricingEnv env) {
	  String ccy = trade.getTradeCurrency();
      if ("EUR".equalsIgnoreCase(ccy)) {
        return 1.0d;
      } else {
        QuoteValue quote = null;
        try {
          quote = env.getQuoteSet().getFXQuote("EUR", ccy, "EUR", ca.getRecordDate(), false);
        } catch (MarketDataException e) {
          Log.error(this, "Error while retrieving the FxQuote");
        }
        if (quote != null) {
          return quote.getClose();
        }
        else{
            Log.error(this, "There is no quote for EUR/" + ccy);
        }
      }
      return null;
  }

    public Double round(Double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
            BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public String formatAmount(final Double value) {
        final DecimalFormat myFormatter = new DecimalFormat("#############");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        myFormatter.setRoundingMode(RoundingMode.DOWN);
        if (value != null && value!=0.00) {
            String format = myFormatter.format(value);
            return format;
        } else {
            return "";
        }
    }



}

