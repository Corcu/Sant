package calypsox.tk.report;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.util.TransferArray;

import java.util.*;


public class EquityMisPlusCompromisoReport extends TradeReport {


  public static final String YESTERDAY_DIRTY_PRICE_STR = "YesterdayDirtyPrice";
  public static final String DIRTY_PRICE_STR = "DirtyPrice";
  public static final String OFFICIAL_ACCOUNTING = "OFFICIAL_ACCOUNTING";
  public static final String HOLIDAYS = "Holidays";
  public static final String CASH_COLLATERAL = "CASH_COLLATERAL";
  private static final String WHERE_CLAUSE = "bo_transfer.transfer_status<>'CANCELLED' "
      + "and trade.trade_id = bo_transfer.trade_id and bo_transfer.trade_id IN (";
  private static final long serialVersionUID = 1L;
  HashMap<String, PricingEnv> pEnvs = new HashMap<>();



  public class PriceCache {

    HashMap<Product,HashMap<String,Double>> priceCache = new HashMap<>();

    public void addPrice(Product sec, String type, Double price) {
      HashMap<String,Double>  priceHash = priceCache.get(sec);
      if(priceHash==null) {
        priceHash = new HashMap<>();
      }
      priceHash.put(type, price);
      priceCache.put(sec, priceHash);
    }

    public Double getPrice(Product sec, String type) {
      final HashMap<String,Double>  priceHash = priceCache.get(sec);
      if(priceHash!=null) {
        return priceHash.get(type);
      }
      return null;
    }
  }



  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public ReportOutput load(Vector errorMsgs) {

    pEnvs.put(DIRTY_PRICE_STR, AppUtil.loadPE(DIRTY_PRICE_STR, getValuationDatetime()));

    final DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);
    if(output==null) {
      return null;
    }
    final ReportRow[] rows = output.getRows();

    try {
        ArrayList<Trade> trades = new ArrayList<>();
        final HashMap<Long, ArrayList<BOTransfer>> transfersPerTrades = new HashMap<>();
        for(int i=0; i<rows.length; i++) {
            final Trade trade = rows[i].getProperty(ReportRow.TRADE);
            trades.add(trade);
            if(trades.size()>=ioSQL.MAX_ITEMS_IN_LIST) {
                final HashMap<Long, ArrayList<BOTransfer>> transfers = getTransferForTrades(trades);
                transfersPerTrades.putAll(transfers);
                trades = new ArrayList<>();
            }
        }

        if(trades.size()>0) {
          final HashMap<Long, ArrayList<BOTransfer>> transfers = getTransferForTrades(trades);
          transfersPerTrades.putAll(transfers);
        }

        final PriceCache priceCache = new PriceCache();

        for(int i=0; i<rows.length; i++) {
            final ReportRow row = rows[i];
            if (row != null){
                final Trade trade = row.getProperty(ReportRow.TRADE);
                final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
                final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
                final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
                final Product product = trade.getProduct();

                if (product instanceof Equity) {
                    final PricingEnv dirtyPricePE = getDirtyPricePricingEnv((Equity) product);
                    if (dirtyPricePE != null) {
                        // Calculate Today Dirty Price
                        Double dirtyPrice = priceCache.getPrice(((Equity) product), DIRTY_PRICE_STR);
                        if (dirtyPrice == null) {
                            final QuoteSet quoteSet = dirtyPricePE.getQuoteSet();
                            dirtyPrice = getQuotePrice(((Equity) product), quoteSet, valDate, dirtyPricePE, getHolidays(), errorMsgs);
                            priceCache.addPrice(((Equity) product), DIRTY_PRICE_STR, dirtyPrice);
                        }
                        row.setProperty(DIRTY_PRICE_STR, dirtyPrice);

                        // Calculate Yesterday Dirty Price
                        Double yesterdayDirtyPrice = priceCache.getPrice(((Equity) product), YESTERDAY_DIRTY_PRICE_STR);
                        if (yesterdayDirtyPrice == null) {
                            final QuoteSet quoteSet = dirtyPricePE.getQuoteSet();
                            final JDate valDateMinus1 = valDate.addBusinessDays(-1, getHolidays());
                            yesterdayDirtyPrice = getQuotePrice(((Equity) product), quoteSet, valDateMinus1, dirtyPricePE, getHolidays(), errorMsgs);
                            priceCache.addPrice(((Equity) product), YESTERDAY_DIRTY_PRICE_STR, yesterdayDirtyPrice);
                        }
                        row.setProperty(YESTERDAY_DIRTY_PRICE_STR, yesterdayDirtyPrice);
                    }
                }

                final ArrayList<BOTransfer> transfers = transfersPerTrades.get(trade.getLongId());
                if (transfers != null) {
                    final ArrayList<BOTransfer> secTransfers = getTransfersOfType(transfers, "SECURITY");
                    final ArrayList<BOTransfer> colTransfers = getTransfersOfType(transfers, "COLLATERAL");
                    if (secTransfers != null && secTransfers.size() > 0) {
                        secTransfers.sort(new Comparator<BOTransfer>() {
                            @Override
                            public int compare(BOTransfer o1, BOTransfer o2) {
                              if (o1.getLongId() > o2.getLongId()) {
                                  return 1;
                              }
                              if (o1.getLongId() < o2.getLongId()) {
                                  return -1;
                              }
                              return 0;
                            }
                        });
                        row.setProperty(ReportRow.TRANSFER, secTransfers.get(0));
                        row.setProperty(CASH_COLLATERAL, getTransfersSum(colTransfers, valDate));
                    }
                }
            }
        }

    } catch (final CalypsoServiceException e) {
      Log.error("EquityMISReport", e);
    }

    return output;
  }


  protected HashMap<Long, ArrayList<BOTransfer>> getTransferForTrades (List<Trade> trades) throws CalypsoServiceException {
    final HashMap<Long, ArrayList<BOTransfer>> transfersPerTrades = new HashMap<>();
    final ArrayList<CalypsoBindVariable> bindVariables = new ArrayList<>();
    final String whereClause = getWhereClause(trades,bindVariables);
    final TransferArray transfers = getDSConnection().getRemoteBO().getTransfers("trade", whereClause, bindVariables);
    for(final BOTransfer transfer : transfers) {
      ArrayList<BOTransfer> transferForTrade = transfersPerTrades.get(transfer.getTradeLongId());
      if(transferForTrade==null) {
        transferForTrade = new ArrayList<>();
        transfersPerTrades.put(transfer.getTradeLongId(), transferForTrade);
      }
      transferForTrade.add(transfer);
    }
    return transfersPerTrades;
  }


  protected Double getTransfersSum(ArrayList<BOTransfer> transfers, JDate asofDate) {
    Double cashAmount = 0.0d;
    for(final BOTransfer transfer : transfers) {
      if(transfer.getValueDate().lte(asofDate)) {
        cashAmount+=transfer.getSettlementAmount();
      }
    }
    return cashAmount;
  }


  protected ArrayList<BOTransfer> getTransfersOfType(ArrayList<BOTransfer> transfers, String type) {
    final ArrayList<BOTransfer> result = new ArrayList<>();
    for(final BOTransfer transfer : transfers) {
      if(transfer.getTransferType().equals(type)) {
        result.add(transfer);
      }
    }
    return result;
  }


  protected String getWhereClause(List<Trade> trades, List<CalypsoBindVariable> bindVariables) {
    final StringBuilder strBld = new StringBuilder();
    for(final Trade trade : trades) {
      bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.LONG,trade.getLongId()));
      strBld.append("?,");
    }
    final String tradeIdList = strBld.toString();
    if(tradeIdList.length()==0) {
      return null;
    }
    final String whereClause = WHERE_CLAUSE + tradeIdList.substring(0, tradeIdList.length()-1) + ")";
    return whereClause;
  }


  @SuppressWarnings({ "rawtypes", "unchecked" })
protected Vector getHolidays() {
    Vector holidays = new Vector<>();
    if (getReportTemplate().getHolidays() != null) {
      holidays = getReportTemplate().getHolidays();
    } else {
      holidays.add("SYSTEM");
    }
    return holidays;
  }


  private Double getQuotePrice(final Product product, final QuoteSet quoteSet, JDate valDate, PricingEnv pEnv,Collection<String> holidays, Vector<String> errors) {
    final JDate quoteDate = valDate;
    final QuoteValue productQuote = quoteSet.getProductQuote(product, quoteDate, getPriceType(pEnv.getName()));
    if ((productQuote != null) && (!Double.isNaN(productQuote.getClose()))) {
      return productQuote.getClose();
    }
    final String error = "Quote not available for Product ISIN: " + product.getSecCode("ISIN") + " for date " + quoteDate + " quote name " + product.getQuoteName();
    errors.add(error);
    Log.error(this, error);
    return null;
  }


  public PricingEnv getDirtyPricePricingEnv(Equity product) {
    return pEnvs.get(DIRTY_PRICE_STR);
  }


  public static String getPriceType(String penvName) {
    if(penvName.equals(OFFICIAL_ACCOUNTING)) {
      return "Price";
    } else {
      return penvName;
    }
  }


  public static String getDirtyPricePricingEnvName(SecLending product) {
    return product.getSecurity() instanceof Equity ? OFFICIAL_ACCOUNTING : "";
  }

}