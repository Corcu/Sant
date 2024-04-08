/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: Samuel Bartolome (samuel.bartolome@siag.es)
 * All rights reserved.
 *
 */

package calypsox.tk.core;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.mo.TradeFilterCriterion;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TradePrice;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Siag-Samuel
 */
public class SantanderUtil {

    /**
     * TransferType Principal
     */
    public static final String TRANSFER_TYPE_PRINCIPAL = "PRINCIPAL";

    /**
     * TransferType INTEREST
     */
    public static final String TRANSFER_TYPE_INTEREST = "INTEREST";

    public static final String TRANSFER_TYPE_WTHTAX = "WITHHOLDINGTAX";

    public static final String TERMINATED_TRADE_EVENT = "TERMINATED_TRADE";

    public static final String TRANSFER_TYPE_ACCRUAL = "ACCRUAL_FEE";

    public static final String BO_AMEND_EFFECTIVE_DATE = "BOAmendEffectiveDate";

    public static final String BUY = "B";
    public static final String SELL = "S";
    public static final String NEAR = "N";
    public static final String FAR = "F";
    public static final String YES = "Y";
    public static final String NOT = "N";
    public static final String BAXTER = "BAXTER:";
    public static final String ELECTPLAF = "ELECTPLAF:";
    public static final String ESP = "ESP";
    public static final String INSTRUMENT_ID_SP = "SP";
    public static final String INSTRUMENT_ID_FM = "FM";
    public static final String INSTRUMENT_ID_SW = "SW";
    public static final String COUNTERPARTY_ID_PREFIX = "J";
    public static final String FX_EUR = "FX.EUR.";
    public static final String EUR = "EUR";
    public static final String QUOTE_TYPE = "Price";
    // TODO : to change later if something defined by santander
    public static final String QUOTE_SET_NAME = "default";

    public static final String NEW = "NEW";
    public static final String NEL = "NEL";
    public static final String NOV = "NOV";
    public static final String ERA = "ERA";
    public static final String DEL = "DEL";
    public static final String DEN = "DEN";
    public static final String CAN = "CAN";
    public static final String FIX = "FIX";
    public static final String CALYPSO = "Calypso";
    public static final String MUREX = "Murex";
    public static final String APC = "APC";
    public static final String BPC = "BPC";
    public static final String TELEPHONE = "Telephone";
    public static final String INTERNET = "Internet";
    public static final String VALUE_003 = "003";
    public static final String VALUE_002 = "002";

    public static final String VALUE_508 = "508";
    public static final String VALUE_509 = "509";
    public static final String VALUE_510 = "510";
    public static final String VALUE_511 = "511";
    public static final String VALUE_512 = "512";
    public static final String VALUE_513 = "513";
    public static final String VALUE_514 = "514";
    public static final String VALUE_515 = "515";
    public static final String VALUE_516 = "516";
    public static final String VALUE_517 = "517";
    public static final String SWI = "SWI";
    public static final String SWA = "SWA";
    public static final String INT = "INT";
    public static final String J = "J";

    public static final String TRANSFER_DIRECTION_PAY = "PAY";
    public static final String MADRID_REAL = "MADRID REAL";

    public static final String SANT_CCY_CUTOFF_DAYLAG = "SantCutOffDayLag";
    public static final String SETTLE_CUTOFF_DATETIME = "SettleCutOffTime";
    public static final String SETTLE_CUTOFF_DATETIME_LONG = "SettleCutOffTime_Long";

    public static final String NO = "NO";

    public static final String FIXED_RATE = "F";
    public static final String FLOATING_RATE = "V";
    /**
     * RATE RESET EVENT TYPE
     */
    public static final String RESET_EVENT_TYPE = "RATE_RESET_TRADE";

    /**
     * Matching Status process keywords.
     */
    public static final String TRADE_KEYWORD_MATCHING_STATUS = "MatchingStatus";
    public static final String TRADE_KEYWORD_MATCHING_STATUS_FAR = "MatchingStatusFar";
    public static final String TRADE_KEYWORD_MATCHING_STATUS_FIXING = "MatchingStatusFixing";

    public static final String TRADE_KEYWORD_ETTEVENT_TYPE = "ETTEventType";

    public static final String TRADE_KEYWORD_PTERMINATION_DATE = "PTerminationDate";

    /**
     * Matching Statuses
     */
    public static final String TRADE_KEYWORD_STATUS_MATCHED = "Matched";
    public static final String TRADE_KEYWORD_STATUS_UNMATCHED = "Unmatched";
    public final static String TRADE_KEYWORD_STATUS_MISMATCHED = "Mismatched";

    public static final String INSTRUMENT_ID_NS = "NS";

    public static final String INSTRUMENT_ID_ND = "ND";

    public static final String S = "S";
    public static final String N = "N";

    public static final String RATE_RESET = "RATE_RESET";

    public static final String F = "F";

    public static final String FXNDF_S = "FXNDF-S";
    public static final String FXNDF_F = "FXNDF-F";

    public static final String PLN = "PLN";

    public static final String BRE = "BRE";

    public static final String BNR = "BNR";

    public static final String FAR_LEG = "Far";

    public static final String SI = "SI";

    public static final String R = "R";

    public static final String SETTLEMENT = "Settlement";

    public static final String SWE = "SWE";

    public static SantanderUtil instance;

    private SantanderUtil() {
    }

    public static SantanderUtil getInstance() {
        if (instance == null) {
            instance = new SantanderUtil();
        }
        return instance;
    }

    /**
     * Only to JUnit. You can use that to replace the instance with a mockito
     * instance
     */
    public static void setInstance(final SantanderUtil mockInstance) {
        instance = mockInstance;
    }

    // /**
    // * Gets the currency used. Only for FX,FWFORWARD and FXSWAP
    // *
    // * @author Samuel Bartolome (samuel.bartolome@siag-management.com)
    // * @param isBuy
    // * true if you want the buy's currency and false if you want the
    // * sell's currency
    // *
    // * @param isNear
    // * true if you want the near leg currency, false if you want the
    // * far leg currency.Ignored if product is FXSWAP
    // * @param errors
    // * Error's Vector
    // * @param columnName
    // * Name of the column that need the currency could be "";
    // *
    // * @return Returns the currency used. return null if trade or product are
    // * null and when the product is not an instance of FX or FXSWAP
    // */
    // public String getCurrency(final Trade trade, final boolean isBuy,
    // final boolean isNear, final JDate valDate) {
    // if (trade == null) {
    // return null;
    // }
    // final Product product = trade.getProduct();
    // double tradeQuantity = trade.getQuantity();
    // if (product instanceof FX) {// is not a swap
    // if ((valDate != null)
    // && TradeUtil.getInstance().isFixed(trade, valDate)) {
    // final FXNDF fxndf = ((FXNDF) product);
    // tradeQuantity = fxndf.getSettlementAmount(trade, null);
    // if (tradeQuantity >= 0) {
    // if (isBuy) {
    // return fxndf.getSettleCurrency();
    // } else {
    // if (fxndf.getSettleCurrency().equals(
    // fxndf.getPrimaryCurrency())) {
    // return fxndf.getQuotingCurrency();
    // } else {
    // return fxndf.getPrimaryCurrency();
    // }
    // }
    // } else {
    // if (isBuy) {
    // if (fxndf.getSettleCurrency().equals(
    // fxndf.getPrimaryCurrency())) {
    // return fxndf.getQuotingCurrency();
    // } else {
    // return fxndf.getPrimaryCurrency();
    // }
    // } else {
    // return fxndf.getSettleCurrency();
    // }
    // }
    // } else {
    // final FX fxProduct = (FX) product;
    // if (tradeQuantity > 0) {// the trade buys
    // if (isBuy) {// buy
    // return fxProduct.getPrincipalCurrency();
    // } else {// sell
    // return fxProduct.getQuotingCurrency();
    // }
    // } else {// the trade sells
    // if (isBuy) {// buy
    // return fxProduct.getQuotingCurrency();
    // } else {// sell
    // return fxProduct.getPrincipalCurrency();
    // }
    // }
    // }
    // } else if (product instanceof FXSwap) {// is a swap
    // final FXSwap fxSwapProduct = (FXSwap) product;
    // if (tradeQuantity > 0) {// the trade buys "today" sells "tomorrow"
    // if (isNear) {// near leg
    // if (isBuy) {// buy
    // return fxSwapProduct.getPrincipalCurrency();
    // } else {// sell
    // return fxSwapProduct.getQuotingCurrency();
    // }
    // } else {// far leg
    // if (isBuy) {// buy
    // return fxSwapProduct.getQuotingCurrency();
    // } else {// sell
    // return fxSwapProduct.getPrincipalCurrency();
    // }
    // }
    // } else {// the trade sells "today" buys "tomorrow"
    // if (isNear) {// near leg
    // if (isBuy) {// buy
    // return fxSwapProduct.getQuotingCurrency();
    // } else {// sell
    // return fxSwapProduct.getPrincipalCurrency();
    // }
    // } else {// far leg
    // if (isBuy) {// buy
    // return fxSwapProduct.getPrincipalCurrency();
    // } else {// sell
    // return fxSwapProduct.getQuotingCurrency();
    // }
    // }
    // }
    // } else if (product instanceof Cash) {
    // final Cash cashProduct = (Cash) product;
    // return cashProduct.getCurrency();
    // } else {
    // return null;
    // }
    //
    // }

    /**
     * Gets the currency used. Only for FX,FWFORWARD and FXSWAP
     *
     * @param isBuy      true if you want the buy's currency and false if you want the
     *                   sell's currency
     * @param isNear     true if you want the near leg currency, false if you want the
     *                   far leg currency.Ignored if product is FXSWAP
     * @param errors     Error's Vector
     * @param columnName Name of the column that need the currency could be "";
     * @return Returns the currency used. return null if trade or product are
     * null and when the product is not an instance of FX or FXSWAP
     * @author Samuel Bartolome (samuel.bartolome@siag-management.com)
     */
    public String getCurrency(final Trade trade, final boolean isBuy, final boolean isNear) {
        if (trade == null) {
            return null;
        }
        final Product product = trade.getProduct();
        final double tradeQuantity = trade.getQuantity();
        if (product instanceof FX) {// is not a swap

            final FX fxProduct = (FX) product;
            if (tradeQuantity > 0) {// the trade buys
                if (isBuy) {// buy
                    return fxProduct.getPrincipalCurrency();
                } else {// sell
                    return fxProduct.getQuotingCurrency();
                }
            } else {// the trade sells
                if (isBuy) {// buy
                    return fxProduct.getQuotingCurrency();
                } else {// sell
                    return fxProduct.getPrincipalCurrency();
                }
            }

        } else if (product instanceof FXSwap) {// is a swap
            final FXSwap fxSwapProduct = (FXSwap) product;
            if (tradeQuantity > 0) {// the trade buys "today" sells "tomorrow"
                if (isNear) {// near leg
                    if (isBuy) {// buy
                        return fxSwapProduct.getPrincipalCurrency();
                    } else {// sell
                        return fxSwapProduct.getQuotingCurrency();
                    }
                } else {// far leg
                    if (isBuy) {// buy
                        return fxSwapProduct.getQuotingCurrency();
                    } else {// sell
                        return fxSwapProduct.getPrincipalCurrency();
                    }
                }
            } else {// the trade sells "today" buys "tomorrow"
                if (isNear) {// near leg
                    if (isBuy) {// buy
                        return fxSwapProduct.getQuotingCurrency();
                    } else {// sell
                        return fxSwapProduct.getPrincipalCurrency();
                    }
                } else {// far leg
                    if (isBuy) {// buy
                        return fxSwapProduct.getPrincipalCurrency();
                    } else {// sell
                        return fxSwapProduct.getQuotingCurrency();
                    }
                }
            }
        } else if (product instanceof Cash) {
            final Cash cashProduct = (Cash) product;
            return cashProduct.getCurrency();
        } else {
            return null;
        }

    }

    /**
     * Make the String of a Date
     *
     * @param date The date to convert to string
     * @return The String of the date
     * @author Samuel Bartolome (samuel.bartolome@siag.es)
     */
    public String getDateString(final Date date, final SimpleDateFormat sdt, final boolean emptyWhenNull) {
        if (date != null) {
            return sdt.format(date);
        } else {
            if (emptyWhenNull) {
                return formatStringWithBlankOnLeft("", sdt.toLocalizedPattern().length());
            } else {
                final Calendar calendar = Calendar.getInstance();
                calendar.set(1, 0, 1, 0, 0);
                return sdt.format(calendar.getTime());
            }
        }
    }

    /**
     * Make the String of a JDatetime
     *
     * @param date The date to convert to string
     * @return The String of the date
     * @author Samuel Bartolome (samuel.bartolome@siag.es)
     */
    public String getDateString(final JDatetime date, final SimpleDateFormat sdt, final boolean emptyWhenNull) {
        if (date != null) {
            return sdt.format(date);
        } else {
            if (emptyWhenNull) {
                return formatStringWithBlankOnLeft("", sdt.toLocalizedPattern().length());
            } else {
                final Calendar calendar = Calendar.getInstance();
                calendar.set(1, 0, 1, 0, 0);
                return sdt.format(calendar.getTime());
            }
        }
    }

    /**
     * Make the String of a JDate
     *
     * @param date The date to convert to string
     * @return The String of the date
     * @author Samuel Bartolome (samuel.bartolome@siag.es)
     */
    public String getDateString(final JDate date, final SimpleDateFormat sdt, final boolean emptyWhenNull) {
        if (date != null) {
            final Calendar calendar = Calendar.getInstance();
            calendar.set(date.getYear(), date.getMonth() - 1, date.getDayOfMonth(), 0, 0);
            return sdt.format(calendar.getTime());
        } else {
            if (emptyWhenNull) {
                return formatStringWithBlankOnLeft("", sdt.toLocalizedPattern().length());
            } else {
                final Calendar calendar = Calendar.getInstance();
                calendar.set(1, 0, 1, 0, 0);
                return sdt.format(calendar.getTime());
            }
        }
    }

    /**
     * Add blanks on the left of a String
     *
     * @param value  String to add blanks
     * @param length Total length of the returned String
     * @return The a String with a length equals to the parameter length
     */
    public String formatStringWithBlankOnLeft(final String value, final int length) {
        final String pattern = "%" + length + "." + length + "s";
        return String.format(pattern, value).substring(0, length);
    }

    /**
     * Add blanks on the right of a String
     *
     * @param value  String to add blanks
     * @param length Total length of the returned String
     * @return The a String with a length equals to the parameter length
     */
    public String formatStringWithBlankOnRight(final String value, final int length) {
        final String pattern = "%-" + length + "." + length + "s";
        return String.format(pattern, value).substring(0, length);
    }

    /**
     * Finds the TradePrice object from the list passed as a parameter.
     */
    @Deprecated
    public TradePrice getTradePrice(final Trade trade, final int measure, final JDate valDate,
                                    final List<TradePrice> tradePrices) {

        if (Util.isEmpty(tradePrices)) {
            return null;
        }

        for (final TradePrice tradePrice : tradePrices) {
            if (tradePrice.getTradeLongId() != trade.getLongId()) {
                continue;
            }
            if (tradePrice.getMeasureId() != measure) {
                continue;
            }
            if (tradePrice.getValDate().compareTo(valDate) != 0) {
                continue;
            }
            return tradePrice;
        }

        return null;
    }

    /**
     * Gets the amount used
     *
     * @param isBuy  true if it is for a buy and false if it is for a sell
     * @param isNear true if you want the amount of the near leg , false if you
     *               want the amount of the far leg,
     * @param errors Error's Vector
     * @return Returns the leg's amount.
     */
    public Double getAmount(final Trade trade, final boolean isBuy, final boolean isNear) {
        final Product product = trade.getProduct();
        if (product == null) {
            return null;
        }
        double tradeQuantity = trade.getQuantity();
        if (product instanceof FXSwap) {// is a swap
            final FXSwap fxSwapProduct = (FXSwap) product;
            if (!isNear) {
                tradeQuantity = fxSwapProduct.getForwardQuantity();
                if (tradeQuantity > 0) {// the trade buys
                    if (isBuy) {// buy
                        return tradeQuantity;
                    } else {// sell
                        return fxSwapProduct.getForwardAmount();
                    }
                } else {// the trade sells
                    if (isBuy) {// buy
                        return fxSwapProduct.getForwardAmount();
                    } else {// sell
                        return tradeQuantity;
                    }
                }
            }
        }
        // is not a swap or is the near leg
        if (tradeQuantity > 0) {// the trade buys
            if (isBuy) {// buy
                return tradeQuantity;
            } else {// sell
                return trade.getAccrual();
            }
        } else {// the trade sells
            if (isBuy) {// buy
                return trade.getAccrual();
            } else {// sell
                return tradeQuantity;
            }
        }
    }

    // public Double getAmount(final Trade trade, final boolean isBuy,
    // final boolean isNear, final JDate valDate) {
    // final Product product = trade.getProduct();
    // if (product == null) {
    // return null;
    // }
    // double tradeQuantity = trade.getQuantity();
    // if (product instanceof FXSwap) {// is a swap
    // final FXSwap fxSwapProduct = (FXSwap) product;
    // if (!isNear) {
    // tradeQuantity = fxSwapProduct.getForwardQuantity();
    // if (tradeQuantity > 0) {// the trade buys
    // if (isBuy) {// buy
    // return tradeQuantity;
    // } else {// sell
    // return fxSwapProduct.getForwardAmount();
    // }
    // } else {// the trade sells
    // if (isBuy) {// buy
    // return fxSwapProduct.getForwardAmount();
    // } else {// sell
    // return tradeQuantity;
    // }
    // }
    // }
    // }
    // if (TradeUtil.getInstance().isFixed(trade, valDate)) {
    // final FXNDF fxndf = ((FXNDF) product);
    // final double amount = fxndf.getSettlementAmount(trade, null);
    // if (amount >= 0) {
    // if (isBuy) {
    // return amount;
    // } else {
    // return 0.0;
    // }
    // } else {
    // if (isBuy) {
    // return 0.0;
    // } else {
    // return amount;
    // }
    // }
    // } else {
    // // is not a swap or is the near leg
    // if (tradeQuantity > 0) {// the trade buys
    // if (isBuy) {// buy
    // return tradeQuantity;
    // } else {// sell
    // return trade.getAccrual();
    // }
    // } else {// the trade sells
    // if (isBuy) {// buy
    // return trade.getAccrual();
    // } else {// sell
    // return tradeQuantity;
    // }
    // }
    // }
    // }

    // CAL_ACC 415
    public boolean isSpot(final JDate effectiveDate, final JDate settleDate, final Vector<String> holidays,
                          final CurrencyPair pair) {
        return effectiveDate.gte(settleDate.addBusinessDays(0 - getSpotDays(pair), holidays));
    }

    // CAL_ACC 415
    public boolean isForward(final JDate effectiveDate, final JDate settleDate, final Vector<String> holidays,
                             final CurrencyPair pair) {
        return effectiveDate.before(settleDate.addBusinessDays(0 - getSpotDays(pair), holidays));
    }

    // CAL_ACC 415
    public boolean is48H(final JDate effectiveDate, final JDate settleDate, final JDate bookingDate,
                         final Vector<String> holidays, final CurrencyPair pair) {
        return effectiveDate.equals(settleDate.addBusinessDays(0 - getSpotDays(pair), holidays))
                && isForward(bookingDate, settleDate, holidays, pair);
    }

    // CAL_ACC 415
    public JDate get48HDate(final JDate settleDate, final JDate bookingDate, final Vector<String> holidays,
                            final CurrencyPair pair) {
        if (bookingDate != null) {
            if (isForward(bookingDate, settleDate, holidays, pair)) {
                return settleDate.addBusinessDays(0 - getSpotDays(pair), holidays);
            } else {
                return null;
            }
        } else {
            return settleDate.addBusinessDays(0 - getSpotDays(pair), holidays);
        }
    }

    public int getSpotDays(final CurrencyPair pair) {
        if (pair != null) {
            final int spotDays = pair.getSpotDays();
            if (spotDays < 0) {
                final int primarySpotDays = pair.getPrimary().getDefaultSpotDays();
                final int secundarySpotDays = pair.getQuoting().getDefaultSpotDays();
                if (primarySpotDays > secundarySpotDays) {
                    return primarySpotDays;
                } else {
                    return secundarySpotDays;
                }
            } else {
                return spotDays;
            }
        } else {
            return 2;
        }
    }

    /**
     * Maps PAYMENT, RECEIPT, SEC_DELIVERY and SEC_RECEIPT of BOTransfer into
     * their String version
     *
     * @param i BOTransfer.PAYMENT, BOTransfer.RECEIPT,
     *          BOTransfer.SEC_DELIVERY and BOTransfer.SEC_RECEIPT
     * @return return:<br>
     * BOTransfer.PAYMENT-->"PAYMENT"<br>
     * BOTransfer.RECEIPT-->"RECEIPT"<br>
     * BOTransfer.SEC_DELIVERY-->"SEC_DELIVERY"<br>
     * BOTransfer.SEC_RECEIPT-->"SEC_RECEIPT"<br>
     */
    public String getTransferEventType(final int i) {
        switch (i) {
            case BOTransfer.PAYMENT:
                return "PAYMENT";
            case BOTransfer.RECEIPT:
                return "RECEIPT";
            case BOTransfer.SEC_DELIVERY:
                return "SEC_DELIVERY";
            case BOTransfer.SEC_RECEIPT:
                return "SEC_RECEIPT";
            default:
                return "";
        }

    }

    /**
     * Method to pad a Space on the right of the given String
     *
     * @param string         String that you want to pad Spaces
     * @param finalStrLength final length of the String required after the padding
     * @return returns the resultant String which will have spaces appended to
     * the right
     */
    public String padSpacesToRight(final String string, final int finalStrLength) {
        return String.format("%1$-" + finalStrLength + "." + finalStrLength + "s", string);
    }

    /**
     * Method to build the ordinal number from an integer value.
     *
     * @param int Integer value to be parsed
     * @return returns the resultant String with the ordinal number of the
     * argument
     */
    public String getOrdinalFor(final int value) {
        final int tenRemainder = value % 10;
        switch (tenRemainder) {
            case 1:
                return "ST";
            case 2:
                return "ND";
            case 3:
                return "RD";
            default:
                return "TH";
        }
    }

    /**
     * This method returns the Trade itself in most cases after making sure that
     * the product is indeed FXBased. In order for this Formatter to work with
     * FXSwap, the product is exploded if appropriate and a specific Trade
     * object representing the near leg or the far leg is returned.
     *
     * @param trade   The <code>Trade</code> associated with the message.
     * @param message The <code>BOMessage</code> to render as SWIFT
     */
    public Trade getTrade(final Trade trade, final BOMessage message) {
        final Product product = trade.getProduct();

        if (!(product instanceof FXSwap) && !(product instanceof FXNDF)) {
            return trade;
        }

        final List<Trade> trades = getTrades(trade, product);

        if ((trades == null) || (trades.size() <= 0)) {
            return null;
        }

        final Trade leg = trades.get(message.getStatementId());

        return leg;
    }

    @SuppressWarnings("unchecked")
    protected List<Trade> getTrades(final Trade trade, final Product product) {
        List<Trade> trades = null;

        if (product instanceof FXNDFSwap) {
            final FXNDFSwap ndfSwap = (FXNDFSwap) trade.getProduct();
            trades = ndfSwap.explodeTradeForMsgFormatting(trade);
        } else if (product instanceof FXSwap) {
            final FXSwap fxSwap = (FXSwap) product;
            trades = fxSwap.explodeTrade(trade, null);
        } else if (product instanceof FXNDF) {
            final FXNDF fxNdf = (FXNDF) product;
            trades = ((List<Trade>) fxNdf.explodeTrade(trade, null, true));
        }

        return trades;
    }

    /**
     * @param element
     * @param eventType
     * @param eventClass
     * @param reason
     */
    public void buildAndSaveTask(final Object element, final String eventType, final String eventClass,
                                 final String reason, final String source) {
        Task sdTask = null;
        if (element instanceof BOMessage) {
            final BOMessage message = (BOMessage) element;
            long id = message.getLongId();
            if (id == 0) {
                id = message.getAllocatedLongSeed();
            }
            sdTask = buildTask(reason, id, message.getTradeLongId(), eventType, eventClass, source);
        } else if (element instanceof Trade) {
            final Trade trade = (Trade) element;
            long id = trade.getLongId();
            if (id == 0) {
                id = trade.getAllocatedLongSeed();
            }
            sdTask = buildTask(reason, id, id, eventType, eventClass, source);
        }
        if (sdTask != null) {
            final TaskArray tasks = new TaskArray();
            tasks.add(sdTask);
            try {
                DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(tasks, 0, source);
            } catch (final RemoteException e) {
                Log.error("calypsox.tk.bo.core.SantanderUtil.getInstance().getInstance()", "Cannot publisk tasks", e);
            }
        }
    }

    /**
     * Generate a Task
     *
     * @param comment    comment related to the task
     * @param messageId  trade id if the exception is related to a trade
     * @param eventType  task event type
     * @param eventClass task event class
     * @return a new Task
     */
    private Task buildTask(final String comment, final long messageId, final long tradeId, final String eventType,
                           final String eventClass, final String source) {
        final Task task = new Task();
        task.setObjectLongId(messageId);
        task.setTradeLongId(tradeId);
        task.setEventClass(eventClass);
        task.setDatetime(new JDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setStatus(Task.NEW);
        task.setEventType(eventType);
        task.setSource(source);
        task.setComment(comment);
        return task;
    }

    public JDate getNDFResetDate(final Trade trade) {
        if (trade.getProductType().equals(FXNDF.FXNDF)) {
            final FXNDF ndf = (FXNDF) trade.getProduct();
            if (ndf != null) {
                return ndf.getResetDateTime().getJDate(TimeZone.getDefault());
            }
        }
        return null;
    }

    public Vector<String> getHolidaysFromCurrencyPair(final Trade trade) {
        final Vector<String> holidays = new Vector<String>();

        final Product p = trade.getProduct();
        // CAL_ACC 376
        if (p instanceof FXBased) {
            final String primaryCurrency = ((FXBased) p).getCurrencyPair().getPrimaryCode();
            final String quotingCurrency = ((FXBased) p).getCurrencyPair().getQuotingCode();

            final CurrencyDefault ccy1 = LocalCache.getCurrencyDefault(primaryCurrency);
            holidays.addAll(ccy1.getDefaultHolidays());

            final CurrencyDefault ccy2 = LocalCache.getCurrencyDefault(quotingCurrency);
            holidays.addAll(ccy2.getDefaultHolidays());
        }

        return holidays;
    }

    public Vector<String> getHolidaysFromTransfer(final BOTransfer transfer) {
        final Vector<String> holidays = new Vector<String>();

        final CurrencyDefault ccy1 = LocalCache.getCurrencyDefault(transfer.getSettlementCurrency());
        holidays.addAll(ccy1.getDefaultHolidays());

        return holidays;
    }

    /**
     * Convert an int number into a two Char radix 26 String
     *
     * @param num
     * @param base
     * @return
     */
    public String mapIntToChar(int num, final int base) // return string,
    // accept two
    // integers
    {
        String result = "";

        while (num >= base) {
            final int digit = num % base;
            result = result + (char) (digit + 65); // continue
            num = num / base;
        }
        if (num < 0) {
            result = "ZZ";
        } else {
            result = (char) (num + 65) + result; // ""+M makes a string out of a
            if (result.length() == 1) {
                result = (char) 65 + result;
            }
        }
        return result;
    }

    /**
     * Retrieve the Message ID from a received message MUR from GestorSTP. The
     * ID is located in the field tag 108 within the block 3 of Message Header
     * It is the number after the MUR id code GT + Message Version number mapped
     * into two chars code. Example is: {108:GTAA000000010052} GT -> Global
     * Trading; AA -> Version number = 0; 000000010052 -> Message Id;
     *
     * @param serializedMessage
     * @return
     */
    public int getMessageIdFromMur(final String serializedMessage) {
        final String block = "{108:";
        int id = -1;
        boolean found = false;
        int pos = serializedMessage.indexOf(block) + block.length();
        if (pos >= 0) {
            while (!found && (pos < serializedMessage.length())) {
                if (Character.isDigit(serializedMessage.charAt(pos))) {
                    found = true;
                } else {
                    pos = pos + 1;
                }
            }
            final int tagEnd = serializedMessage.indexOf("}", pos);
            if (tagEnd >= 0) {
                id = Integer.parseInt(serializedMessage.substring(pos, tagEnd));
            }
        }
        return id;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public TradeArray mixTradeArrays(final TradeArray trades1, final TradeArray trades2) {
        final TradeArray trades = new TradeArray();
        // Use the set so duplicates Trades will be removed
        final HashSet set = new HashSet();
        if (trades2 != null) {
            set.addAll(trades2);
        }
        if (trades1 != null) {
            set.addAll(trades1);
        }
        trades.addAll(set);
        return trades;
    }

    public String getLEAttributeValue(final Collection<LegalEntityAttribute> attr, final String attrbuteType) {
        if (attr == null) {
            return "";
        }

        for (final LegalEntityAttribute leAttr : attr) {
            if (leAttr.getAttributeType().equals(attrbuteType)) {
                return leAttr.getAttributeValue();
            }
        }
        return "";
    }

    public String getBookAttributeValue(final Vector<BookAttribute> bookAttributes, final String bookAttrbuteName) {
        for (final BookAttribute bookAttr : bookAttributes) {
            if (bookAttr.getName().equals(bookAttrbuteName)) {
                return bookAttr.getValue();
            }
        }
        return "";
    }

    public String getOtherCurrency(final String currency, final Trade trade) {
        if ((trade != null) && (trade.getProduct() instanceof FXBased)) {
            final FXBased product = (FXBased) trade.getProduct();
            final String principal = product.getCurrencyPair().getPrimaryCode();
            final String quoting = product.getCurrencyPair().getQuotingCode();
            if (principal.equals(currency)) {
                return quoting;
            } else {
                return principal;
            }
        }
        return "";
    }

    /**
     * Removes any existing trade prices(apart from the NPV Pricer measures that
     * we recieve from Murex) for the list of trades in TradeArray, for the
     * valuationDate passed in. It is required as this scheduled task is
     * rerunnable.
     *
     * @param ds
     * @param trades
     * @param valDateOffset
     * @throws RemoteException
     * @throws Exception
     */
    // public void removeTradePrices(final DSConnection ds,
    // final TradeArray trades, final JDate valDateOffset)
    // throws RemoteException {
    //
    // final int SQL_IN_TRADES_LIMIT = 1000;
    //
    // final String removeTPWhere = "valuation_date="
    // + Util.date2SQLString(valDateOffset)
    // + " AND MEASURE_ID not in ("
    // + SantanderPricerMeasure.NPV_PAYLEG + ","
    // + SantanderPricerMeasure.NPV_RECLEG + ","
    // + SantanderPricerMeasure.NPV_PAYLEG_FAR + ","
    // + SantanderPricerMeasure.NPV_PAYLEG_NEAR + ","
    // + SantanderPricerMeasure.NPV_RECLEG_FAR + ","
    // + SantanderPricerMeasure.NPV_RECLEG_NEAR + ","
    // // CAL_ACC 694
    // + SantanderPricerMeasure.SANT_MM_MTM_GROSS
    // + ") AND trade_id in (";
    //
    // String tradeListWhere = " ";
    // for (int i = 0; i < trades.size(); i++) {
    // tradeListWhere += trades.get(i).getId();
    //
    // if ((i == (trades.size() - 1))
    // || (((i + 1) % SQL_IN_TRADES_LIMIT) == 0)) {
    // // SQL_IN_TRADES_LIMIT reached or end of tradeArray reached
    // tradeListWhere += ")";
    // final String removeSQL = removeTPWhere + tradeListWhere;
    // // System.out.println("****" + removeSQL);
    // ds.getRemoteTrade().removeTradePrices(removeSQL);
    // tradeListWhere = " ";
    // } else if (i < (trades.size() - 1)) {
    // tradeListWhere += ", ";
    // }
    // }
    //
    // }
    public void roundTradePrices(final ArrayList<TradePrice> pricersToRound) {
        for (final TradePrice tradePrice : pricersToRound) {
            final double roundedValue = CurrencyUtil.roundAmount(tradePrice.getMeasureValue(),
                    tradePrice.getCurrency());
            tradePrice.setMeasureValue(roundedValue);
        }
    }

    public JDatetime getAccBusinessDate(final LegalEntity po) {

        final Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
        JDatetime now;

        // Do it for ALL Po
        int poId = 0;
        if (po != null) {
            poId = po.getId();
        }
        final LegalEntityAttribute leDateAttribute = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), poId,
                poId, LegalEntity.PROCESSINGORG, KeywordConstantsUtil.LE_ATTRIBUTE_SANT_ACC_BUSINESS_DATE);
        if (leDateAttribute != null) {
            final String businessDateS = leDateAttribute.getAttributeValue();
            if (!Util.isEmpty(businessDateS)) {
                final JDate businessDate = Util.istringToJDate(businessDateS);
                if (businessDate != null) {
                    now = new JDatetime(businessDate, 23, 59, 59, TimeZone.getDefault());
                } else {
                    now = new JDatetime(localCalendar.getTime());
                }
            } else {
                now = new JDatetime(localCalendar.getTime());
            }
        } else {
            now = new JDatetime(localCalendar.getTime());
        }
        return now;
    }

    public double convertToEUR(final double amount, final String ccy, final JDate date, final PricingEnv pricingEnv)
            throws MarketDataException {
        if (amount == 0.0) {
            return 0.0;
        }

        if (SantanderUtil.EUR.equals(ccy)) {
            return amount;
        }
        QuoteValue quote = pricingEnv.getQuoteSet().getFXQuote(SantanderUtil.EUR, ccy, date);
        if ((quote != null) && !Double.isNaN(quote.getClose())) {
            return amount / quote.getClose();
        } else {
            quote = pricingEnv.getQuoteSet().getFXQuote(ccy, SantanderUtil.EUR, date);
            if ((quote != null) && !Double.isNaN(quote.getClose())) {
                return amount * quote.getClose();
            } else {

                throw new MarketDataException("FX Quote not found for the currency combination " + ccy + "/"
                        + SantanderUtil.EUR + " or " + SantanderUtil.EUR + "/" + ccy + " on " + date.toString());
            }
        }
    }

    public boolean isFirstBussisnesDayOfYear(final JDate todayDate, final Vector<String> holidays) {
        final JDate yesterday = todayDate.addBusinessDays(-1, holidays);
        return yesterday.getYear() != todayDate.getYear();
    }

    public static JDate getTradeFlowRateResetDate(final Trade trade) {
        JDate resetDate = null;
        if (trade != null) {
            final String resetDateAsString = trade.getKeywordValue(KeywordConstantsUtil.KEYWORD_FLOW_RATE_RESET_DATE);
            if (!Util.isEmpty(resetDateAsString)) {
                try {
                    final String resetDateFormat = LocalCache.getDomainValueComment(DSConnection.getDefault(),
                            "tradeKeyword", KeywordConstantsUtil.KEYWORD_FLOW_RATE_RESET_DATE);
                    final SimpleDateFormat sdf = new SimpleDateFormat(resetDateFormat);
                    resetDate = JDate.valueOf(sdf.parse(resetDateAsString));

                } catch (final ParseException e) {
                    Log.error("calypsox.tk.bo.core.SantanderUtil", "Cannot parse FlowRateReset date: wrong format", e);
                }
            }
        }
        return resetDate;
    }

    public boolean overwritePrices(final DSConnection ds, final ArrayList<TradePrice> pricesTosave)
            throws RemoteException {
        return removeTradePrices(ds, pricesTosave) && saveTradePrices(ds, pricesTosave);
    }

    private boolean removeTradePrices(final DSConnection ds, final ArrayList<TradePrice> pricesToremove)
            throws RemoteException {
        if (!pricesToremove.isEmpty()) {
            final SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmm");
            TradePrice tradePrice = pricesToremove.get(0);
            String where = "(TRADE_ID = " + tradePrice.getTradeLongId();
            where = where + " AND ";
            where = where + "VALUATION_DATE >=TO_DATE('"
                    + sdf.format(new JDatetime(tradePrice.getValDate(), 0, 0, 0, TimeZone.getDefault()))
                    + "','DDMMYYHH24MI')";
            where = where + " AND ";
            where = where + "VALUATION_DATE <=TO_DATE('"
                    + sdf.format(new JDatetime(tradePrice.getValDate(), 59, 59, 59, TimeZone.getDefault()))
                    + "','DDMMYYHH24MI')";
            where = where + " AND ";
            where = where + "MEASURE_ID=" + tradePrice.getMeasureId() + ")";
            for (int i = 1; i < pricesToremove.size(); i++) {
                where = where + " OR ";
                tradePrice = pricesToremove.get(i);
                where = where + "(TRADE_ID = " + tradePrice.getTradeLongId();
                where = where + " AND ";
                where = where + "VALUATION_DATE >=TO_DATE('"
                        + sdf.format(new JDatetime(tradePrice.getValDate(), 0, 0, 0, TimeZone.getDefault()))
                        + "','DDMMYYHH24MI')";
                where = where + " AND ";
                where = where + "VALUATION_DATE <=TO_DATE('"
                        + sdf.format(new JDatetime(tradePrice.getValDate(), 59, 59, 59, TimeZone.getDefault()))
                        + "','DDMMYYHH24MI')";
                where = where + " AND ";
                where = where + "MEASURE_ID=" + tradePrice.getMeasureId() + ")";
            }
            ds.getRemoteTrade().removeTradePrices(where, null);
            return true;

        } else {
            return true;
        }
    }

    private boolean saveTradePrices(final DSConnection ds, final ArrayList<TradePrice> pricesTosave)
            throws RemoteException {
        if (!pricesTosave.isEmpty()) {
            roundTradePrices(pricesTosave);
            ds.getRemoteTrade().saveTradePrices(pricesTosave);
            return true;
        } else {
            return true;
        }
    }

    // CAL_ACC 464
    public String getLegalEntityAttribute(final LegalEntity le, final String att) {
        String rst = "";
        if (le != null) {
            final Collection<?> atts = le.getLegalEntityAttributes();
            // FIX in case a LE does NOT have attributes
            if (atts == null) {
                if (le != null) {
                    Log.error(this.getClass(), le.getName() + " does not have LE attributes configured");
                }
                return rst;
            }
            LegalEntityAttribute current;
            final Iterator<?> it = atts.iterator();

            while (it.hasNext() && (rst == null)) {
                current = (LegalEntityAttribute) it.next();
                if (current.getAttributeType().equalsIgnoreCase(att)) {
                    rst = current.getAttributeValue();
                }
            }
        }
        return rst;
    }

    /**
     * Returns a vector of events defined in Domain Values with a name
     *
     * @param eventType
     * @return vector of events
     */
    public Vector<String> getEvents(final String eventName) {

        final Vector<String> allEvents = LocalCache.getDomainValues(DSConnection.getDefault(), eventName);

        return allEvents;
    }

    /**
     * Look for a list of events that have the comment equal to a eventGroup and
     * belongs to a Event Type
     *
     * @param allEvents
     * @param eventToFind
     * @param eventType
     * @return vector of events
     */
    public Vector<String> lookup(final Vector<String> allEvents, final String eventGroup, final String eventType) {
        final Vector<String> v = new Vector<String>();
        for (final String dv : allEvents) {
            final String comment = LocalCache.getDomainValueComment(DSConnection.getDefault(), eventType, dv);
            if (Util.isEmpty(comment)) {
                continue;
            }
            if (comment.equals(eventGroup)) {
                v.add("'" + dv + "'");
            }
        }
        return v;
    }


    //// EMIR BRS Mappings and Utils

    public static final String PO_T99A = "T99A";

    public static final String PO_BSTE = "BSTE";

    /**
     * Detects if a value is contained in a vector of values
     * @return true if is contained else false
     */
    public boolean containsCaseInsensitive(final String s,
                                           final Vector<String> v) {
        final Iterator<String> i = v.iterator();
        String currentItem = "";
        while (i.hasNext()) {
            currentItem = i.next();
            if (currentItem.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param tradeIdArray
     * @return
     * @throws CalypsoServiceException
     */
    public TradeArray getTradesWithTradeFilter(long[] tradeIdArray) throws CalypsoServiceException {
        Vector<String> tradeIdsVector = new Vector<>();
        if (tradeIdArray != null && tradeIdArray.length > 0) {
            tradeIdsVector = new Vector<String>(Arrays.stream(tradeIdArray).mapToObj(l -> ((Long) l).toString()).collect(Collectors.toList()));
        }
        return getTradesWithTradeFilter(tradeIdsVector);
    }

    /**
     * @param tradeIdsVector
     * @return
     * @throws CalypsoServiceException
     */
    public TradeArray getTradesWithTradeFilter(Vector<String> tradeIdsVector) throws CalypsoServiceException {
        TradeArray result = new TradeArray();
        if (!Util.isEmpty(tradeIdsVector)) {
            TradeFilter tradeFilter = buildTradeIdTradeFilter(tradeIdsVector);
            result = DSConnection.getDefault().getRemoteTrade().getTrades(tradeFilter, null);
        }
        return result;
    }

    /**
     * @param tradeIdsVector
     * @return
     */
    private TradeFilter buildTradeIdTradeFilter(Vector<String> tradeIdsVector) {
        TradeFilter tradeFilter = new TradeFilter();
        tradeFilter.setName(this.getClass().getSimpleName());
        TradeFilterCriterion tIdList = new TradeFilterCriterion("TRADE_ID_LIST");
        tIdList.setValues(tradeIdsVector);
        tIdList.setIsInB(true);
        tradeFilter.addCriterion(tIdList);
        return tradeFilter;
    }
    /**
     * Retrieves a typed (only one type) Vector with the data from a generic Vector returned by the
     * method executeSelectSQL.
     *
     * @param resultSet
     * @param type
     * @return
     */
    public <T> Vector<Vector<T>> getDataFixedResultSetWithType(final Vector<?> resultSet,
                                                               final Class<? extends T> type) {
        return getDataFromGenericResultSetWithType(resultSet, 2, type);
    }


    private <T> Vector<Vector<T>> getDataFromGenericResultSetWithType(final Vector<?> resultSet, final int indexRow,
                                                                      final Class<? extends T> type) {
        final Vector<Vector<T>> rst = new Vector<Vector<T>>();

        if (!Util.isEmpty(resultSet)) {
            for (int iRow = indexRow; iRow < resultSet.size(); iRow++) {
                final Object dataObject = resultSet.get(iRow);
                if (dataObject instanceof Vector<?>) {
                    final Vector<T> r = new Vector<T>();
                    final Vector<?> rowData = (Vector<?>) dataObject;
                    for (final Object columnObject : rowData) {
                        if (type.getName().equals(Long.class.getName())) {
                            r.add((T) new Long(Long.parseLong(columnObject.toString())));
                        } else
                            r.add(type.cast(columnObject));
                        }
                        rst.add(r);
                }
            }
        }
        return rst;
    }

}
