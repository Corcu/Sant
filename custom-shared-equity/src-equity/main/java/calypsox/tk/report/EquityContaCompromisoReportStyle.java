package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.core.SantanderUtil;
import calypsox.tk.report.quotes.FXQuoteHelper;
import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

public class EquityContaCompromisoReportStyle extends TradeReportStyle {

  public static final String LOG_CATEGORY = "EquityContaCompromisoReportStyle";
  private static final List<String> contactTypes = Arrays.asList("Default", "ALL");

  public static final String ENTIDAD = "ENTIDAD";
  public static final String CENTRO_CONTABLE = "CENTRO_CONTABLE";
  public static final String DEAL_ID = "DEAL_ID";
  public static final String DIRECTION = "DIRECTION";
  public static final String MUREX_TRADEID = "MUREX_TRADEID";
  public static final String TRADE_DATE = "TRADE_DATE";
  public static final String SETTLE_DATE = "SETTLE_DATE";
  public static final String GLS_COUNTERPARTY = "GLS_COUNTERPARTY";
  public static final String COUNTERPARTY_DESC = "COUNTERPARTY_DESC";
  public static final String COUNTERPARTY_SECTOR = "COUNTERPARTY_SECTOR";
  public static final String COUNTERPARTY_COUNTRY = "COUNTERPARTY_COUNTRY";
  public static final String NIF_COUNTERPARTY = "NIF_COUNTERPARTY";
  public static final String QUANTITY = "QUANTITY";
  public static final String SETTLEMENT_AMOUNT = "SETTLEMENT_AMOUNT";
  public static final String CURRENCY = "CURRENCY";
  public static final String SETTLE_CURR = "SETTLE_CURR";
  public static final String ISIN = "ISIN";
  public static final String BOOKING_DATE = "BOOKING_DATE";
  public static final String TRADE_PRICE = "TRADE_PRICE";
  public static final String MARKET_VALUE_MUREX = "MARKET_VALUE_MUREX";
  public static final String MARKET_VALUE_MUREX_EUR = "MARKET_VALUE_MUREX_EUR";
  public static final String MARKET_VALUE_MUREX_PRODUCT_CCY = "MARKET_VALUE_MUREX_PRODUCT_CCY";
  public static final String PORTFOLIO = "PORTFOLIO";
  public static final String STRATEGY = "STRATEGY";
  public static final String EOD_PRICE = "EOD_PRICE";
  public static final String EQUITY_TYPE = "EQUITY_TYPE";
  public static final String EQUITY_TYPE_DESC = "EQUITY_TYPE_DESC";
  public static final String COMMON = "COMMON";
  public static final String MARKET = "MARKET";
  public static final String DUALCCY = "DUALCCY";
  public static final String DUALCCY_RATE = "DUALCCY_RATE";
  public static final String PRODUCT_AMOUNT = "PRODUCT_AMOUNT";

  public static final String LE_ATTR_SECTORCONTABLE = "SECTORCONTABLE";

  public static final ArrayList<String> emptyColumns = new ArrayList<>();
  public static final HashMap<String, String> columnToKeyword = new HashMap<>();
  public static final HashMap<String, String> columnToColumn = new HashMap<>();

  static {
    columnToKeyword.put(MUREX_TRADEID, "MurexTradeID");
    columnToColumn.put(PORTFOLIO, TradeReportStyle.BOOK);
  }

  public static final long serialVersionUID = 1L;

  @SuppressWarnings("rawtypes")
  @Override
  public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

    if (emptyColumns.contains(columnName)) {
      return "";
    }
    if (columnToKeyword.containsKey(columnName)) {
      return getKeywordValue(row, columnToKeyword.get(columnName), errors);
    }
    if (columnToColumn.containsKey(columnName)) {
      return super.getColumnValue(row, columnToColumn.get(columnName), errors);
    }

    final Trade trade = row.getProperty(ReportRow.TRADE);
    final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
    final Product product = trade.getProduct();
    final Double todayPrice = row.getProperty(EquityMisPlusCompromisoReport.DIRTY_PRICE_STR);
    final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
    final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());

    if (product instanceof Equity) {
      final Equity equity = (Equity) product;

      if (columnName.equals(ENTIDAD)) {
        Book book = trade.getBook();
        if(book!=null) {
          String entity = BOCreUtils.getInstance().getEntity(book.getName());
          return BOCreUtils.getInstance().getEntityCod(entity, false);
        }
        return "";
      }

      if (columnName.equals(CENTRO_CONTABLE)) {
        return BOCreUtils.getInstance().getCentroContable(product, trade.getBook().getLegalEntity().getCode(), false);
      }

      if (columnName.equals(DEAL_ID)) {
        return trade.getLongId();
      }

      if (columnName.equals(DIRECTION)) {
        int buySell = equity.getBuySell(trade);
        return buySell == 1 ? "B" : "S";
      }

      if (columnName.equals(TRADE_DATE)) {
        return formatDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
      }

      if (columnName.equals(SETTLE_DATE)) {
        return formatDate(trade.getSettleDate());
      }

      if (columnName.equals(GLS_COUNTERPARTY)) {
        return trade.getCounterParty().getCode();
      }

      if (columnName.equals(COUNTERPARTY_DESC)) {
        return trade.getCounterParty().getName();
      }

      if (columnName.equals(COUNTERPARTY_SECTOR)) {
        final LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, trade.getCounterParty().getId(), "ALL", LE_ATTR_SECTORCONTABLE);
        return attr!=null ? attr.getAttributeValue() : "";
      }

      if (columnName.equals(COUNTERPARTY_COUNTRY)) {
        if(trade.getCounterParty().getCountry()!=null) {
          Country country =BOCache.getCountry(DSConnection.getDefault(), trade.getCounterParty().getCountry());
          if(country!=null)
            return country.getISOCode();
          return "";
        }
      }

      if (columnName.equals(NIF_COUNTERPARTY)) {
        return trade.getCounterParty().getExternalRef();
      }

      if (columnName.equals(QUANTITY)) {
        return trade.getQuantity();
      }

      if (columnName.equals(DUALCCY)) {
          return trade.getTradeCurrency().equalsIgnoreCase(trade.getSettleCurrency()) ? "N" : "Y";
      }

      if (columnName.equals(DUALCCY_RATE)) {
          return trade.getTradeCurrency().equalsIgnoreCase(trade.getSettleCurrency()) ? "1" : trade.getSplitBasePrice();
      }

      if (columnName.equals(PRODUCT_AMOUNT)) {
          String equityCcy = trade.getTradeCurrency();
          String tradeSettleCcy = trade.getSettleCurrency();
          Double settleAmount = equity.calcSettlementAmount(trade);
          if (equityCcy.equalsIgnoreCase(tradeSettleCcy)) {
              return CurrencyUtil.roundAmount(settleAmount, equityCcy);
          } else {
              Double amount2 = 0.0;
              try {
                  CurrencyPair cp = CurrencyUtil.getCurrencyPair(equityCcy, tradeSettleCcy);
                  if(equityCcy.equalsIgnoreCase(cp.getQuotingCode())){
                      amount2 = CurrencyUtil.roundAmount((settleAmount * trade.getSplitBasePrice()), equityCcy);
                  }
                  else{
                      amount2 = CurrencyUtil.roundAmount((settleAmount / trade.getSplitBasePrice()), equityCcy);
                  }
                  System.out.println();
              } catch (MarketDataException e) {
                  Log.error("Clould not get CurrencyPair for currencies " + equityCcy + " and "+ tradeSettleCcy, e.getCause());
              }
              return amount2;
          }
      }

      if (columnName.equals(SETTLEMENT_AMOUNT)) {
        return equity.calcSettlementAmount(trade);
      }

      if (columnName.equals(CURRENCY)) {
        return trade.getTradeCurrency();
      }

      if (columnName.equals(SETTLE_CURR)) {
        return trade.getSettleCurrency();
      }

      if (columnName.equals(ISIN)) {
        return getISIN(equity);
      }

      if (columnName.equals(BOOKING_DATE)) {
        return formatDate(trade.getEnteredDate().getJDate(pricingEnv.getTimeZone()));
      }

      if (columnName.equals(TRADE_PRICE)) {
        return trade.getTradePrice();
      }

      if (columnName.equals(MARKET_VALUE_MUREX)) {
        Book book = trade.getBook();
        if(book!=null) {
          AccountingBook acctBook = book.getAccountingBook();
          if(acctBook != null) {
            String acctBookName = acctBook.getName();
            if(acctBookName.equals("Inversion crediticia")) {
              return "0";
            }
          }
        }
        final PLMark plMarkValue = getPLMarkValue(pricingEnv, trade, valDate);
        if(plMarkValue!=null){
          Double amount = getPLMark(plMarkValue,"MTM_FULL_LAGO", trade.getSettleCurrency());
          return amount!=null ? formatResult(amount) : null;
        }
        return "";
      }

      if(columnName.equals(MARKET_VALUE_MUREX_EUR)) {
          Book book = trade.getBook();
          if(book!=null) {
            AccountingBook acctBook = book.getAccountingBook();
            if(acctBook != null) {
              String acctBookName = acctBook.getName();
              if(acctBookName.equals("Inversion crediticia")) {
                return "0";
              }
            }
          }
          final PLMark plMarkValue = getPLMarkValue(pricingEnv, trade, valDate);
          if(plMarkValue!=null) {
              Double amount = getPLMark(plMarkValue, "MTM_FULL_LAGO", trade.getSettleCurrency());
              if(amount!=null) {
                  return (!"EUR".equalsIgnoreCase(trade.getSettleCurrency())) ? formatResult(convertWithFxQuote(amount, "EUR", trade.getSettleCurrency(), valDate)) : formatResult(amount);
              }
              return "";
          }
          return "";
      }

      if (columnName.equals(MARKET_VALUE_MUREX_PRODUCT_CCY)) {
          Book book = trade.getBook();
          if(book!=null) {
              AccountingBook acctBook = book.getAccountingBook();
              if(acctBook != null) {
                  String acctBookName = acctBook.getName();
                  if(acctBookName.equals("Inversion crediticia")) {
                      return "0";
                  }
              }
          }
          final PLMark plMarkValue = getPLMarkValue(pricingEnv, trade, valDate);
          if(plMarkValue!=null) {
              Double amount = getPLMark(plMarkValue, "MTM_FULL_LAGO", trade.getSettleCurrency());
              if(amount!=null) {
                  return (!product.getCurrency().equalsIgnoreCase(trade.getSettleCurrency())) ? formatResult(convertWithFxQuote(amount, product.getCurrency(), trade.getSettleCurrency(), valDate)) : formatResult(amount);
              }
              return "";
          }
          return "";
      }

      if (columnName.equals(STRATEGY)) {
        Book book = trade.getBook();
        if(book!=null) {
          AccountingBook acctBook = book.getAccountingBook();
          if(acctBook != null) {
            String acctBookName = acctBook.getName();
            if(acctBookName.equals("Negociacion")) {
              return "NEG";
            }
            else if(acctBookName.equals("Inversion crediticia")) {
              return "COS";
            }
            else if(acctBookName.equals("Otros a valor razonable")) {
              return "OVR";
            }
          }
        }
        return "NEG";
      }

      if (columnName.equals(EOD_PRICE)) {
        return formatResult(todayPrice);
      }

      if (columnName.equals(EQUITY_TYPE)) {
        return equity.getSecCode(EQUITY_TYPE);
      }

      if (columnName.equals(EQUITY_TYPE_DESC)) {
        String equityType = equity.getSecCode(EQUITY_TYPE);
        if(!Util.isEmpty(equityType)) {
          switch (equityType) {
            case "ADR":
              return "RV ADR";
            case "CS":
              return "RV Acciones Ordinarias";
            case "PROP":
              return "RV Acciones Propias";
            case "DERSUS":
              return "RV Derechos de Suscripcion";
            case "PEGROP":
              return "RV Participaciones Empresas del Grupo";
            case "PFI":
              return "RV Participaciones Fondos";
            case "PS":
              return "RV Participaciones: Preferentes";
            case "INSW":
              return "Cesta indices";
            case "CO2":
              return "CO2";
            case "VCO2":
                return "VCO2";
            case "ETF":
                return "ETF";
            default:
              return "";
          }
        }
        return "";
      }

      if (columnName.equals(COMMON)) {
        return equity.getSecCode("Common");
      }

      if (columnName.equals(MARKET)) {
        return equity.getExchange();
      }

    }

    return formatResult(super.getColumnValue(row, columnName, errors));
  }

  public final String getISIN(final Equity equity) {
    return equity.getSecCode(SecCode.ISIN);
  }

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


  private PLMark getPLMarkValue(PricingEnv pricingEnv, Trade trade, JDate date) {
    PLMark plMark = null;
    try {
      plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(), pricingEnv.getName(), date);
      return plMark;
    } catch (RemoteException e) {
      Log.error(this, e);
      return null;

    }
  }

  private Double getPLMark(PLMark plMark, String type, String ccy){
    return null!=plMark && null!=plMark.getPLMarkValue(type, ccy) ? plMark.getPLMarkValue(type, ccy).getMarkValue() : 0.0D;
  }


    private double convertWithFxQuote(Double amount, String currency, String settlementCurrency, JDate valDate) {
        FXQuoteHelper fxQuoteHelper = new FXQuoteHelper("OFFICIAL");
        if (!settlementCurrency.equals(currency)) {
            QuoteValue quote;
            try {
                quote = fxQuoteHelper.getFXQuote(settlementCurrency, currency, valDate);
                if ((quote != null) && !Double.isNaN(quote.getClose())) {
                    return amount * quote.getClose();
                } else {
                    Log.error(this.getClass().getSimpleName(), "There is no quote on " + valDate + " for " + currency + "/" + settlementCurrency);
                }
            }
            catch(MarketDataException e){
                Log.error(this.getClass().getSimpleName(), "Could not get quote.");
            }
        }
        return amount;
    }


}
