package calypsox.tk.bo.cremapping.util;


import calypsox.tk.bo.cremapping.BOCreMappingFactory;
import calypsox.tk.bo.cremapping.event.SantBOCre;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.apps.appkit.presentation.format.JDatetimeFormat;
import com.calypso.tk.bo.*;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.pricer.PricerMeasureCumulativeCash;
import com.calypso.tk.product.*;
import com.calypso.tk.product.corporateaction.CASwiftCodeDescription;
import com.calypso.tk.product.corporateaction.CASwiftEventCode;
import com.calypso.tk.product.util.RepoCashFlowLayout;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.FdnUtilProvider;
import com.calypso.tk.util.TransferArray;
import org.apache.commons.lang.StringUtils;

import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static calypsox.tk.bo.swift.SantanderSwiftUtil.padStringZero;

public class BOCreUtils {
    private static BOCreUtils instance = new BOCreUtils();
    private static final String SEND_AGREGO = "SendAgrego";
    private static final String SEND_REVALUATION = "SendRevaluation";
    public static final String DATE_FORMAT = "yyyy-MM-dd";


    public synchronized static BOCreUtils getInstance() {

        if (instance == null) {
            instance = new BOCreUtils();
        }

        return instance;
    }

    private static final String CRE_CONTRACT_ATT = "MARGIN_CALL_CONTRACT";
    private static final String IB_KEYWORD = "INTEREST_TRANSFER_FROM";
    private static final String KEYCRE = "cre";
    private static final String SDI_TCCORSPO = "TCCORSPO";
    private static final String REVERSAL = "REVERSAL";
    private static final String STM = "STM";
    private static final String COUPON_TYPE = "CouponType";
    private static final String SEC_CODE_CENTRO_CONTABLE = "CENTRO_CONTABLE";


    /**
     * @param cre
     * @return BOCre message for MIC
     */
    public StringBuilder getBOCreMessage(BOCre cre) {
        StringBuilder message = new StringBuilder();

        if (null != cre) {
            Trade trade = getTrade(cre.getTradeLongId());
            final SantBOCre creType = BOCreMappingFactory.getFactory().getCreType(cre, trade);
            if (creType != null) {
                message = creType.getCreLine();
            }
        }

        return message;
    }

    public boolean isStm(CollateralConfig config) {
        return config != null && "True".equalsIgnoreCase(config.getAdditionalField(STM));
    }

    public String formatStringWithBlankOnLeft(final String value,
                                              final int length) {
        if (!Util.isEmpty(value)) {
            final String pattern = "%" + length + "." + length + "s";
            return String.format(pattern, value).substring(0, length);
        }
        return formatBlank("", length);
    }

    /**
     * Add blanks on the right of a String
     *
     * @param value  String to add blanks
     * @param length Total length of the returned String
     * @return The a String with a length equals to the parameter length
     */
    public String formatStringWithBlankOnRight(final String value,
                                               final int length) {
        if (!Util.isEmpty(value)) {
            final String pattern = "%-" + length + "." + length + "s";
            return String.format(pattern, value).substring(0, length);
        }
        return formatBlank("", length);
    }

    /**
     * Add blanks on the left of a String
     *
     * @param value  String to add blanks
     * @param length Total length of the returned String
     * @return The a String with a length equals to the parameter length
     */
    public String formatStringWithZeroOnLeft(final Long value,
                                             final int length) {
        final String format;
        final String pattern = "%0" + length + "d";
        if (value != null && value != 0) {
            format = String.format(pattern, value);
        } else {
            format = formatBlank("", length);
        }
        return format;
    }

    public String formatStringWithZeroOnLeft(final Integer value,
                                             final int length) {
        final String format;
        final String pattern = "%0" + length + "d";
        if (value != null && value != 0) {
            format = String.format(pattern, value);
        } else {
            format = formatBlank("", length);
        }
        return format;
    }

    public String formatStringWithZeroOnRight(final String value,
                                              final int length) {
        String result = "";

        if (!Util.isEmpty(value)) {
            final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols();
            decimalSymbol.setDecimalSeparator('.');
            final DecimalFormat df = new DecimalFormat("0.00000", decimalSymbol);
            result = df.format(Double.valueOf(value)).toString();
        } else {
            result = formatBlank(value, length);
        }

        return result;
    }

    public String truncateString(final String value, final int length) {
        if (!Util.isEmpty(value)) {
            return value.substring(0, Math.min(length, value.length()));
        } else {
            return formatBlank(value, length);
        }
    }


    public String formatDate(JDate date, int length) {
        if (null != date) {
            JDateFormat format = new JDateFormat("yyyy-MM-dd");
            return format.format(date);
        }
        return formatBlank("", length);
    }

    public String formatDateTime(JDatetime date, int length) {
        if (null != date) {
            JDatetimeFormat format = new JDatetimeFormat("yyyy-MM-dd HH:mm:ss");
            return format.format(date);
        }
        return formatBlank("", length);
    }

    /**
     * Format unsigned number
     *
     * @param value    value
     * @param length   length
     * @param decimals number of decimals
     * @return formatted number
     */
    public String formatUnsignedNumber(final Double value, int length,
                                       final int decimals, String separator) {
        if (null != value && value != 0.0) {
            if (decimals != 0) {
                if (Util.isEmpty(separator)) {
                    length = length + 1;
                }
                final String pattern = "%0" + (length) + "." + decimals + "f";
                final DecimalFormatSymbols symbols = new DecimalFormatSymbols();

                return String.format(pattern, Math.abs(value))
                        .replace(symbols.getDecimalSeparator() + "", separator);
            }

            final String pattern = "%0" + (length) + "." + decimals + "f";
            return String.format(pattern, Math.abs(value));
        }
        return formatBlank("", length);
    }

    /**
     * Format unsigned number zero
     *
     * @param value    value
     * @param length   length
     * @param decimals number of decimals
     * @return formatted number
     */
    public String formatUnsignedNumberZero(final Double value, int length,
                                           final int decimals, String separator) {
        if (null != value) {
            if (decimals != 0) {
                if (Util.isEmpty(separator)) {
                    length = length + 1;
                }
                final String pattern = "%0" + (length) + "." + decimals + "f";
                final DecimalFormatSymbols symbols = new DecimalFormatSymbols();

                return String.format(pattern, Math.abs(value))
                        .replace(symbols.getDecimalSeparator() + "", separator);
            }

            final String pattern = "%0" + (length) + "." + decimals + "f";
            return String.format(pattern, Math.abs(value));
        }
        return formatBlank("", length);
    }

    /**
     * Format unsigned number
     *
     * @param value    value
     * @param length   length
     * @param decimals number of decimals
     * @return formatted number
     */
    public String formatAmountValue(final Double value, int length,
                                    final int decimals, String separator) {
        if (null != value) {
            if (decimals != 0) {
                if (Util.isEmpty(separator)) {
                    length = length + 1;
                }
                final String pattern = "%0" + (length) + "." + decimals + "f";
                final DecimalFormatSymbols symbols = new DecimalFormatSymbols();

                return String.format(pattern, Math.abs(value))
                        .replace(symbols.getDecimalSeparator() + "", separator);
            }

            final String pattern = "%0" + (length) + "." + decimals + "f";
            return String.format(pattern, Math.abs(value));
        }
        return formatBlank("", length);
    }

    /**
     * @param value
     * @param length number of blanks
     * @return blanks values defined
     */
    public String formatBlank(String value, int length) {
        final StringBuilder str = new StringBuilder();

        for (int i = 0; i < length; ++i) {
            str.append(' ');
        }

        return str.toString();
    }

    /**
     * @param account
     * @return Contract defined on MARGIN_CALL_CONTRACT account property
     */
    public CollateralConfig getContract(Account account) {
        if (account != null) {
            final String margin_call_contract = account.getAccountProperty(CRE_CONTRACT_ATT);
            if (!Util.isEmpty(margin_call_contract)) {
                return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), Integer.valueOf(margin_call_contract));
            } else {
                Log.error(this, "Error finding attribute " + CRE_CONTRACT_ATT + " on account: " + account.getId() + " " + account.getName());
            }
        }
        return null;
    }

    public CollateralConfig getContract(String contractId) {
        if (!Util.isEmpty(contractId)) {
            return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), Integer.valueOf(contractId));
        } else {
            Log.error(this, "Error finding attribute " + CRE_CONTRACT_ATT + " on account: " + contractId);
        }
        return null;
    }

    /**
     * @param trade
     * @return Transfer type "Client" from a trade, BOTransfer status "CANCELED" included.
     */
    public BOTransfer getClientTransfer(Trade trade) {
        try {
            if (trade != null) {
                final TransferArray boTransfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(trade.getLongId(), true);
                if (!Util.isEmpty(boTransfers)) {
                    return boTransfers.stream().filter(transfer -> transfer.getExternalRole().equals("Client"))
                            .filter(t -> !t.getStatus().equals(Status.CANCELED))
                            .max(Comparator.comparing(BOTransfer::getLongId)).orElse(getLastClientTransfer(boTransfers));
                }
            }
        } catch (final RemoteException e) {
            Log.error("Error loading the transfer for the trade= " +
                    "                    + trade.getLongId()", e.getCause());
            Log.error(this, e); //sonar
        }
        return null;
    }

    public BOTransfer getLastClientTransfer (TransferArray boTransfers){

        if (!Util.isEmpty(boTransfers)) {
            return boTransfers.stream().filter(transfer -> transfer.getExternalRole().equals("Client"))
                    .max(Comparator.comparing(BOTransfer::getLongId)).orElse(null);
        }
        return null;
    }

    /**
     * @param trade
     * @return Transfer type "Client" from a trade, BOTransfer status "CANCELED" included.
     */
    public List<BOTransfer> getClientRuleTransfer(Trade trade) {
        List<BOTransfer> clients = new ArrayList<>();
        try {
            if (trade != null) {
                final TransferArray boTransfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(trade.getLongId(), false);
                if (!Util.isEmpty(boTransfers)) {
                    clients = boTransfers.stream().filter(transfer -> transfer.getExternalRole().equalsIgnoreCase("Client")).collect(Collectors.toList());
                }
            }
        } catch (final RemoteException e) {
            Log.error("Error loading the transfer for the trade= " +
                    "                    + trade.getLongId()", e.getCause());
            Log.error(this, e); //sonar
        }
        return clients;
    }

    /**
     * @param boCre
     * @return BOtransfer from BOCre
     */
    public BOTransfer getTransfer(BOCre boCre) {
        if (null != boCre && boCre.getTransferLongId() > 0) {
            try {
                return DSConnection.getDefault().getRemoteBO().getBOTransfer(boCre.getTransferLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading transfer for CRE: " + boCre.getId());
            }
        }
        return null;
    }

    public BOTransfer getNettedTransfer(BOCre boCre) {
        if (null != boCre && boCre.getNettedTransferLongId() > 0) {
            try {
                return DSConnection.getDefault().getRemoteBO().getBOTransfer(boCre.getNettedTransferLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading transfer for CRE: " + boCre.getId());
            }
        }
        return null;
    }

    /**
     * @param boPosting
     * @return BOtransfer from BOPosting
     */
    public BOTransfer getTransfer(BOPosting boPosting) {
        if (null != boPosting && boPosting.getTransferLongId() > 0) {
            try {
                return DSConnection.getDefault().getRemoteBO().getBOTransfer(boPosting.getTransferLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading transfer for CRE: " + boPosting.getId());
            }
        }
        return null;
    }

    /**
     * @param boTransfer
     * @return Account from a BOTransfer.
     */
    public Account getAccount(BOTransfer boTransfer) {
        if (null != boTransfer) {
            return BOCache.getAccount(DSConnection.getDefault(), boTransfer.getGLAccountNumber());
        }
        return null;
    }


    /**
     * @param config
     * @param currency
     * @return Find Account from contract and currency by property MARGIN_CALL_CONTRACT
     */
    public Account getAccount(CollateralConfig config, String currency) {
        if (null != config) {
            final List<Account> accounts = BOCache.getAccountByAttribute(DSConnection.getDefault(), CRE_CONTRACT_ATT, String.valueOf(config.getId()));
            if (!Util.isEmpty(accounts)) {
                return accounts.stream().filter(account -> account.getCurrency().equalsIgnoreCase(currency)).findFirst().orElse(null);
            }
        }
        return null;
    }

    public Trade getTrade(Long tradeId) {
        if (tradeId != null) {
            try {
                return DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading trade: " + e);
            }
        }
        return null;
    }

    /**
     * @param value
     * @return direction of BOCre just for ACCRUAL accounting event.
     */
    public String getDirectionACC(Double value) {
        return value < 0.0D ? "REC" : "PAY";
    }

    public String getDirection(Double value) {
        return value < 0.0D ? "PAY" : "REC";
    }

    public String getDebitCredit(double value) {
        if (value == 0.0) {
            return "NULL";
        } else {
            return value < 0.0D ? "DBT" : "CRD";
        }
    }

    /**
     * Valid if BOCre coming from events CANCELED_RECEIPT or CANCELED_PAYMENT
     *
     * @param boCre
     * @return
     */
    public Boolean isCanceledTransfer(BOCre boCre) {
        return null != boCre && (BOCreConstantes.CANCELED_RECEIPT_EVENT.equalsIgnoreCase(boCre.getOriginalEventType())
                || BOCreConstantes.CANCEL_PAYMENT_EVENT.equalsIgnoreCase(boCre.getOriginalEventType()));
    }

    /**
     * Valid if BOCre coming from event CANCELED_TRADE
     *
     * @param boCre
     * @return
     */
    public Boolean isCanceledEvent(BOCre boCre) {
        return null != boCre && BOCreConstantes.CANCELED_TRADE_EVENT.equalsIgnoreCase(boCre.getOriginalEventType());

    }

    /**
     * Valid if BOPosting coming from event CANCELED_TRADE
     *
     * @param boPosting
     * @return
     */
    public Boolean isCanceledEvent(BOPosting boPosting, Trade trade) {
        if (trade != null) {
            return null != boPosting && BOCreConstantes.CANCELED_TRADE.equalsIgnoreCase(trade.getStatus().getStatus()) &&
                    (BOCreConstantes.CANCEL_PAYMENT_EVENT.equalsIgnoreCase(boPosting.getOriginalEventType()) ||
                            BOCreConstantes.CANCELED_RECEIPT_EVENT.equalsIgnoreCase(boPosting.getOriginalEventType()));
        } else {
            return null != boPosting &&
                    (BOCreConstantes.CANCEL_PAYMENT_EVENT.equalsIgnoreCase(boPosting.getOriginalEventType()) ||
                            BOCreConstantes.CANCELED_RECEIPT_EVENT.equalsIgnoreCase(boPosting.getOriginalEventType()));
        }
    }


    /**
     * Valid if Trade if CANCELED
     *
     * @param trade
     * @return
     */
    public Boolean isCanceledEvent(Trade trade) {
        return null != trade && BOCreConstantes.CANCELED_TRADE.equalsIgnoreCase(trade.getStatus().toString());
    }

    /**
     * @param marginCall
     * @param dateType
     * @param positionType
     * @param valDate      (null = D)
     * @return
     */
    public Double getInvLastCashPosition(CollateralConfig marginCall, Trade trade, String dateType, String positionType, JDate valDate) {
        if (null == valDate) {
            valDate = getActualDate();
        }
        if (null != marginCall && !Util.isEmpty(dateType) && !Util.isEmpty(positionType) && null != trade) {
            //position_date D-1
            //date_type = 'TRADE' || SETTLE
            StringBuilder where = new StringBuilder("internal_external = 'MARGIN_CALL' ");
            where.append(" AND position_date <= " + Util.date2SQLString(valDate));
            where.append(" AND position_type =  '" + positionType + "' AND date_type = '" + dateType + "' AND config_id = 0");
            where.append(" AND mcc_id = " + marginCall.getId());
            where.append(" AND currency_code = " + Util.string2SQLString(trade.getTradeCurrency()));

            try {
                Vector<InventoryCashPosition> v = DSConnection.getDefault().getRemoteInventory().getLastInventoryCashPositions(where.toString(), null);
                if (!Util.isEmpty(v)) {
                    InventoryCashPosition allBookAggregatePos = null;
                    allBookAggregatePos = v.firstElement();
                    for (int i = 1; i < v.size(); i++) {
                        allBookAggregatePos.addToTotal(v.get(i));
                    }
                    return allBookAggregatePos.getTotal();
                }
            } catch (Exception var5) {
                Log.error(this, var5);
            }
        }
        return 0.0;
    }

    /**
     * @param contractid
     * @param dateType
     * @param positionType
     * @param valDate      (null = D)
     * @return
     */
    public Double getInvLastCashPosition(int contractid, Trade trade, String dateType, String positionType, JDate valDate) {
        if (null == valDate) {
            valDate = getActualDate();
        }
        if (!Util.isEmpty(dateType) && !Util.isEmpty(positionType) && null != trade) {
            //position_date D-1
            //date_type = 'TRADE' || SETTLE
            StringBuilder where = new StringBuilder("internal_external = 'MARGIN_CALL' ");
            where.append(" AND position_date <= " + Util.date2SQLString(valDate));
            where.append(" AND position_type =  '" + positionType + "' AND date_type = '" + dateType + "' AND config_id = 0");
            where.append(" AND mcc_id = " + contractid);
            where.append(" AND currency_code = " + Util.string2SQLString(trade.getTradeCurrency()));

            try {
                Vector<InventoryCashPosition> v = DSConnection.getDefault().getRemoteInventory().getLastInventoryCashPositions(where.toString(), null);
                if (!Util.isEmpty(v)) {
                    InventoryCashPosition allBookAggregatePos = null;
                    allBookAggregatePos = v.firstElement();
                    for (int i = 1; i < v.size(); i++) {
                        allBookAggregatePos.addToTotal(v.get(i));
                    }
                    return allBookAggregatePos.getTotal();
                }
            } catch (Exception var5) {
                Log.error(this, var5);
            }
        }
        return 0.0;
    }

    //TODO need filter by currency

    /**
     * Bilding where for load all SENT Cres on status NEW and REVERSAL.
     *
     * @param creType
     * @param contractID
     * @param valDate
     * @return
     */
    public String buildWhere(String creType, int contractID, JDate valDate) {
        final StringBuilder where = new StringBuilder();

        if (!Util.isEmpty(creType) && null != valDate) {
            where.append(" bo_cre_type = '" + creType + "' ");
            where.append(" AND ");
            where.append(" ( cre_status = 'NEW' OR cre_status = 'REVERSAL' ) ");
            where.append(" AND ");
            where.append(" ( sent_status = 'SENT' ) ");
            where.append(" AND ");
            //COT_REV settle date del Cre / COT trade date
            if (BOCreConstantes.COT_REV.equalsIgnoreCase(creType)) {
                where.append(" trunc(settlement_date) >= ");
            } else if (BOCreConstantes.COT.equalsIgnoreCase(creType) ||
                    BOCreConstantes.COLLATERAL.equalsIgnoreCase(creType) ||
                    BOCreConstantes.CST.equalsIgnoreCase(creType) ||
                    BOCreConstantes.CST_FAILED.equalsIgnoreCase(creType)) {
                where.append(" trade.trade_id = bo_cre.trade_id ");
                where.append(" AND ");
                where.append(" trunc(trade.trade_date_time) >= ");
            }
            where.append(Util.date2SQLString(valDate));
            where.append(" AND ");
            where.append(" bo_cre.bo_cre_id = att.cre_id ");
            where.append(" AND ");
            where.append(" att.attribute_name = '" + BOCreConstantes.CONTRAT_ATT + "' ");
            where.append(" AND ");
            where.append(" att.attribute_value = '" + contractID + "' ");
        }

        return where.toString();
    }


    public String buildFrom(String creType) {
        StringBuilder from = new StringBuilder();
        from.append("cre_attribute att");
        if (BOCreConstantes.COT.equalsIgnoreCase(creType) ||
                BOCreConstantes.COLLATERAL.equalsIgnoreCase(creType) ||
                BOCreConstantes.CST.equalsIgnoreCase(creType) ||
                BOCreConstantes.CST_FAILED.equalsIgnoreCase(creType)) {
            from.append(", trade");
        }
        return from.toString();
    }

    /**
     * @param where
     * @return BOCres defined on where
     */
    public Double getBOCresAmount(String from, String where, BOCre cre, boolean predate) {
        Double result = 0.0;

        if (!Util.isEmpty(where)) {
            try {
                CreArray boCres = DSConnection.getDefault().getRemoteBackOffice().getBOCres(from, where, null);
                /*
                if(predate){
                    boCres = removePredates(boCres,cre);
                }
                */
                String currency = cre.getCurrency(0);
                result = calcCreAmt(boCres, currency);

            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading BOCres: " + e);
            }
        }
        return result;
    }

    /**
     * Remove processed cres whit valDate distinct to D
     *
     * @param boCres
     * @param cre
     * @return
     */
    private CreArray removePredates(CreArray boCres, BOCre cre) {
        String type = cre.getEventType();
        CreArray resultCres = new CreArray();
        JDate valDate = BOCreUtils.getInstance().getActualDate();
        if (null != boCres) {
            for (BOCre bean : boCres) {

                if ((BOCreConstantes.COT.equalsIgnoreCase(type) && valDate.compareTo(bean.getTradeDate().getDate(TimeZone.getDefault())) == 0)
                        || BOCreConstantes.COT_REV.equalsIgnoreCase(type) && valDate.compareTo(bean.getSettlementDate()) == 0) {
                    resultCres.add(bean);
                }
            }
        }
        return resultCres;
    }

    /**
     * Sum cres by currency (Reversal*-1)
     *
     * @param boCres
     * @return
     */
    private Double calcCreAmt(CreArray boCres, String currency) { //TODO check Cre Currency
        Double result = 0.0;
        if (null != boCres && !Util.isEmpty(currency)) {
            for (BOCre cre : boCres) {
                if (currency.equalsIgnoreCase(cre.getCurrency(0))) {
                    if (REVERSAL.equalsIgnoreCase(cre.getCreType())) {
                        result += (cre.getAmount(0) * -1);
                    } else {
                        result += cre.getAmount(0);
                    }
                }
            }
        }
        return result;
    }

    /**
     * @param cre
     * @param trade
     * @return Retrun false if trade settle date is before D
     */
    public boolean isPredate(BOCre cre, Trade trade) {
        JDate valDate = getActualDate();
        return (BOCreConstantes.COT.equalsIgnoreCase(cre.getEventType()) && trade.getTradeDate().getJDate(TimeZone.getDefault()).before(valDate))
                || (BOCreConstantes.COT_REV.equalsIgnoreCase(cre.getEventType()) && trade.getSettleDate().before(valDate));
    }

    /**
     * @param cre
     * @param trade
     * @return Retrun false if trade/ settle date is before D
     */
    public boolean isRepoAccrualPredate(BOCre cre, Trade trade) {
        JDate enteredDate = trade.getEnteredDate().getJDate(TimeZone.getDefault());
        JDate startDate = ((Repo) trade.getProduct()).getStartDate();
        return null != startDate && cre.getEffectiveDate().equals(enteredDate) && startDate.before(enteredDate);
    }

    /**
     * @param trade
     * @return Return true if exist any Action Event whit creationDate = today and effective Date before today
     */
    public boolean isActionPredate(Trade trade) {
        JDate now = JDate.getNow();
        Vector<EventTypeAction> eventTypeActions = Optional.ofNullable(trade).map(Trade::getProduct)
                .filter(SecFinance.class::isInstance)
                .map(SecFinance.class::cast)
                .map(SecFinance::getEventTypeActions).orElse(new Vector<>());
        return eventTypeActions.stream().filter(event -> event.getCreationDate().getJDate(TimeZone.getDefault()).equals(now) && !"Creation".equalsIgnoreCase(event.getActionType()))
                .anyMatch(event -> event.getEffectiveDate().before(now));
    }

    /**
     * @param cre
     * @param trade
     * @return Retrun false if trade/ settle date is before D
     */
    public boolean isRepoPartenonChange(BOCre cre, Trade trade) {
        return checkEffDateRequestDate(cre, trade);
    }

    //InterestBearing
    public String getInterestName(AccountInterestConfig config) {
        if (null != config)
            return config.getName();
        return "";
    }

    /**
     * @param interest
     * @return
     */
    public Double getInterestRate(InterestBearingEntry interest) {
        if (null != interest)
            return interest.getRate();
        return 0.0;
    }

    /**
     * @param interest
     * @return NEGATIVE || POSITIVE
     */
    public String getPositiveNegative(InterestBearingEntry interest) {
        return interest != null && interest.getRate() < 0.0D ? "NEGATIVE" : "POSITIVE";
    }

    /**
     * @return @{@link AccountInterestConfig}
     */
    public AccountInterestConfig getAccountInterest(Account account) {
        final AccountInterests accountInterests = account.getAccountInterests().stream().findFirst().orElse(null);
        if (accountInterests != null) {
            try {
                return DSConnection.getDefault().getRemoteAccounting().getAccountInterestConfig(accountInterests.getConfigId());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading AccountInterestConfig id: " + accountInterests.getConfigId());
            }
        }
        return null;
    }

    /**
     * @param config @{@link AccountInterestConfig}
     * @return
     */
    public String getFixedFloating(AccountInterestConfig config) {
        if (config != null) {
            final Vector ranges = config.getRanges();
            if (!Util.isEmpty(ranges)) {
                try {
                    final AccountInterestConfigRange accountInterestConfigRange = (AccountInterestConfigRange) ranges.stream().findFirst().orElse(null);
                    if (accountInterestConfigRange != null && !accountInterestConfigRange.isFixed()) {
                        return BOCreConstantes.FLOAT;
                    }
                } catch (Exception e) {
                    Log.error(this, "Error casting AccountingInterestConfigRange." + e);
                }
            }
        }

        return BOCreConstantes.FIXED;
    }


    /**
     * @param trade
     * @return InterestBearing from CST
     */
    public Trade getIBFromCT(Trade trade) {
        if (null != trade) {
            final long interest_transfer_from = trade.getKeywordAsLongId(IB_KEYWORD);
            try {
                return DSConnection.getDefault().getRemoteTrade().getTrade(interest_transfer_from);
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading trade: " + interest_transfer_from + " " + e);
            }
        }
        return null;
    }

    /**
     * @param trade
     * @return last COT_REV from Trade in status SENT
     */
    public BOCre getCRfromTrade(Trade trade) {
        HashMap<String, BOCre> cotRev = new HashMap<>();
        cotRev.put(KEYCRE, null);

        if (null != trade) {
            try {
                final CreArray boCres = DSConnection.getDefault().getRemoteBO().getBOCres(trade.getLongId());
                if (null != boCres && !Util.isEmpty(boCres.getCres())) {
                    for (BOCre cre : boCres.getCres()) {
                        if (BOCreConstantes.COT_REV.equalsIgnoreCase(cre.getEventType())
                                && BOCreConstantes.SENT.equalsIgnoreCase(cre.getSentStatus())) {
                            if (cotRev.get(KEYCRE) != null) {
                                if (cre.getId() > cotRev.get(KEYCRE).getId()) {
                                    cotRev.put(KEYCRE, cre);
                                }
                            } else {
                                cotRev.put(KEYCRE, cre);
                            }
                        }
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading Cre - COT_REV from trade: " + trade.getLongId() + " " + e);
            }
        }
        return cotRev.get(KEYCRE);
    }

    public Double getAccountBalancefromCre(BOCre cre) {
        if (null != cre) {
            final String attributeValue = cre.getAttributeValue(BOCreConstantes.ACCOUNT_BALACE_ATT);
            if (null != attributeValue && !"null".equalsIgnoreCase(attributeValue)) {
                try {
                    return Double.parseDouble(attributeValue);
                } catch (Exception e) {
                    Log.error(this, "Error parsing accountBalance to Double: " + e);
                }
            }
        }
        return 0.0;
    }

    public String getTransferAccount(String type, BOTransfer creBoTransfer) {
        SettleDeliveryInstruction sdi;
        if (null != creBoTransfer) {
            if (BOCreConstantes.DIRECT.equalsIgnoreCase(type)) {
                sdi = getSDI(creBoTransfer.getExternalSettleDeliveryId());
                final Account account = getAccount(sdi.getGeneralLedgerAccount());
                return null != sdi && null != account ? account.getName() : sdi.getAgentAccount();

            } else if (BOCreConstantes.SWIFT.equalsIgnoreCase(type)) {
                sdi = getSDI(creBoTransfer.getInternalSettleDeliveryId());
                return getSDITcc(sdi);
            }
        }
        return "";
    }

    public String getTransferAccountSM(String type, BOTransfer creBoTransfer) {
        SettleDeliveryInstruction sdi;
        if (null != creBoTransfer) {
            if (BOCreConstantes.DIRECT.equalsIgnoreCase(type)) {
                sdi = getSDI(creBoTransfer.getExternalSettleDeliveryId());
                final Account account = getAccount(sdi.getGeneralLedgerAccount());
                return null != sdi && null != account ? account.getName() : sdi.getAgentAccount();

            } else if (BOCreConstantes.SWIFT.equalsIgnoreCase(type)) {
                sdi = getSDI(creBoTransfer.getInternalSettleDeliveryId());
                return getSDITcc(sdi);
            } else {
                sdi = getSDI(creBoTransfer.getInternalSettleDeliveryId());
                if (sdi.getAttribute("SETTLEMENTMETHODMIC") != null && sdi.getAttribute("SETTLEMENTMETHODMIC").equalsIgnoreCase(type)) {
                    return getSDITcc(sdi);
                }
            }
        }
        return "";
    }

    public String getTransferAccountEquity(String type, BOTransfer creBoTransfer) {
        SettleDeliveryInstruction sdi;
        if (null != creBoTransfer) {
            if (BOCreConstantes.DIRECT.equalsIgnoreCase(type)) {
                sdi = getSDI(creBoTransfer.getExternalSettleDeliveryId());
                final Account account = getAccount(sdi.getGeneralLedgerAccount());
                return null != sdi && null != account ? account.getName() : sdi.getAgentAccount();

            } else {
                sdi = getSDI(creBoTransfer.getInternalSettleDeliveryId());
                return getSDITcc(sdi);
            }
        }
        return "";
    }

    /**
     * @param trasnferid
     * @return
     */
    public String getSettleMethod(long trasnferid) {
        try {
            final BOTransfer boTransfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(trasnferid);
            return null != boTransfer ? boTransfer.getSettlementMethod() : "";
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading BOTransfer for CRE: " + e);
        }
        return "";
    }

    /**
     * @param transfer
     * @return
     */
    public String getSettleMethod(BOTransfer transfer) {
        return null != transfer ? transfer.getSettlementMethod() : "";
    }

    public String getSettleMethodFromSdi(BOTransfer transfer) {
        SettleDeliveryInstruction sdiPO = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), transfer.getInternalSettleDeliveryId());
        return null != sdiPO && !Util.isEmpty(sdiPO.getAttribute("SETTLEMENTMETHODMIC")) ? sdiPO.getAttribute("SETTLEMENTMETHODMIC") : "";
    }

    private Account getAccount(int accountiD) {
        return BOCache.getAccount(DSConnection.getDefault(), accountiD);
    }

    private SettleDeliveryInstruction getSDI(int sdiID) {
        return BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), sdiID);
    }


    private String getSDITcc(SettleDeliveryInstruction sdi) {
        return null != sdi && BOCreConstantes.SDI_PROCCESINGORG.equalsIgnoreCase(sdi.getRole()) ? sdi.getAttribute(SDI_TCCORSPO) : "";
    }

    private String getGL(SettleDeliveryInstruction sdi) {
        return null != sdi && BOCreConstantes.SDI_COUNTERPARTY.equalsIgnoreCase(sdi.getRole()) ? sdi.getAttribute(SDI_TCCORSPO) : "";
    }

    public String getProductTypeMarginCall(Trade trade) {
        return isNoCouponType(trade) ? "MarginCall" : "Coupon";
    }

    public String getProductTypeEquity() {
        return "Equity";
    }

    public boolean isNoCouponType(Trade trade) {
        return null != trade && Util.isEmpty(trade.getKeywordValue(COUPON_TYPE));
    }

    public boolean isCouponType(BOTransfer transfer) {
        return null != transfer && transfer.getTransferType().contains("COUPON");
    }

    public JDate getActualDate() {
        /*JDate jDate = DSConnection.getDefault().getServerCurrentDatetime().getJDate(TimeZone.getDefault());
        Log.system("Cre Date","Time Zone: " + TimeZone.getDefault());
        Log.system("Cre Date","Server Date: " + JDate.getNow());
        Log.system("Cre Date","Server JDatetime: " + JDate.getNow().getJDatetime());
        Log.system("Cre Date","Date TimeZone: " + JDate.getNow().getJDatetime().getJDate(TimeZone.getDefault()));
        Log.system("Cre Date","Server Current Date: " + jDate);
        Log.system("Cre Date","Server Current DateTime: "  + DSConnection.getDefault().getServerCurrentDatetime());
         */
        return JDate.getNow();
    }

    public Product loadSecurity(BOCre boCre) {
        int securityId = null != boCre ? boCre.getProductId() : 0;
        if (securityId != 0) {
            try {
                return DSConnection.getDefault().getRemoteProduct().getProduct(securityId);
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading security: " + securityId);
            }
        }
        return null;
    }

    public Product loadSecurityFromSecLending(Trade trade) {
        if (null != trade) {
            Product product = trade.getProduct();
            if (product instanceof SecLending) {
                return ((SecLending) product).getSecurity();
            }
        }
        return null;
    }

    public Product loadSecurityFromEquity(Trade trade) {
        if (null != trade) {
            Product product = trade.getProduct();
            if (product instanceof Equity) {
                return ((Equity) product).getSecurity();
            }
        }
        return null;
    }

    public Product loadSecurityFromBond(Trade trade) {
        if (null != trade) {
            Product product = trade.getProduct();
            if (product instanceof Bond) {
                return ((Bond) product).getSecurity();
            }
        }
        return null;
    }

    public Product loadSecurityFromCA(Trade trade) {
        if (null != trade) {
            Product product = trade.getProduct();
            if (product instanceof CA) {
                return ((CA) product).getSecurity();
            }
        }
        return null;
    }
    public Product loadSecurity( Trade trade) {
        return loadSecurity(trade, null);
    }
    public Product loadSecurity( Trade trade, BOTransfer transfer) {
        if (null != transfer && transfer.getProductId()>0) {
            Product product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), transfer.getProductId());
            if (product instanceof Security) {
                return ((Security)product).getSecurity();
            }
        }
        if (null != trade) {
            Product product = trade.getProduct();
            if (product instanceof Security) {
                return ((Security)product).getSecurity();
            }
        }
        return null;
    }

    public String loadIsin(Product security) {
        return null != security ? security.getSecCode("ISIN") : "";
    }

    public Long loadProductID(Product security) {
        return null != security ? security.getLongId() : 0;
    }

    public String loadUnderlyingType(Product security) {
        String type = null != security && null != security.getUnderlyingProduct() ? security.getUnderlyingProduct().getType() : "";
        if (!Util.isEmpty(type)) {
            if ("Equity".equalsIgnoreCase(type))
                return "RV";
            else
                return "RF";
        }
        return "";
    }

    public String loadPortfolioStrategy(BOCre boCre) {
        return null != boCre ? boCre.getAttributeValue("AccountingBook") : "";
    }

    public String loadPartenonId(Trade trade) {
        return null != trade ? trade.getKeywordValue("PartenonAccountingID") : "";
    }

    public String loadPartenonIdCCCT(Trade trade) {
        String partenonAccountingID = trade.getKeywordValue("PartenonAccountingID");
        if (partenonAccountingID != null && partenonAccountingID.length() >= 21) {
            String empresaCentro = partenonAccountingID.substring(0, 8);
            String contrato = partenonAccountingID.substring(11, 18);
            partenonAccountingID = empresaCentro +  contrato;
            return partenonAccountingID;
        }
        return "";
    }

    public String loadPartenonId(Trade trade, String partenonKeyword) {
        return null != trade ? trade.getKeywordValue(partenonKeyword) : "";
    }

    public String loadfieldOfPartenonId(Trade trade, int init, int fin) {
       String partenonAccountingID = trade.getKeywordValue("PartenonAccountingID");
        if (partenonAccountingID != null && partenonAccountingID.length() >= 21) {
            return null != trade ? partenonAccountingID.substring(init, fin) : "";
        }
        return "";
    }

    public String loadClassification(Trade trade) {
        return null != trade ? trade.getKeywordValue("TradeClassification") : "";
    }

    public String loadDeliveryType(Trade trade) {
        return null != trade ? trade.getKeywordValue("DeliveryType") : "";
    }

    public String loadIssuerName(Trade trade) {
        if (trade.getProduct() instanceof Equity) {
            Equity product = (Equity) trade.getProduct();
            return null != product && null != product.getIssuer() ? product.getIssuer().getExternalRef() : "";
        } else if (trade.getProduct() instanceof Bond) {
            Bond product = (Bond) trade.getProduct();
            LegalEntity issuer = BOCache.getLegalEntity(DSConnection.getDefault(), product.getIssuerId());
            return null != product && null != issuer ? issuer.getExternalRef() : "";
        }
        return null;
    }

    public String loadProductCurrency(Trade trade) {
        if (trade.getProduct() instanceof Equity) {
            Equity product = (Equity) trade.getProduct();
            return null != product ? product.getCurrency() : "";
        } else if (trade.getProduct() instanceof Bond) {
            Bond product = (Bond) trade.getProduct();
            return null != product ? product.getCurrency() : "";
        }
        return null;
    }

    public String loadBuySell(Trade trade) {
        if (trade.getProduct() instanceof Equity) {
            Equity product = (Equity) trade.getProduct();
            return product.getBuySell(trade) == 1 ? "BUY" : "SELL";
        } else if (trade.getProduct() instanceof Bond) {
            Bond product = (Bond) trade.getProduct();
            return product.getBuySell(trade) == 1 ? "BUY" : "SELL";
        }
        return null;
    }

    public String loadBS(Trade trade) {
        if (trade.getProduct() instanceof Bond) {
            Bond product = (Bond) trade.getProduct();
            return product.getBuySell(trade) == 1 ? "B" : "S";
        }
        return null;
    }

    public String isInternal(Trade trade) {
        return Optional.ofNullable(trade.getMirrorBook()).isPresent() ? "Y" : "N";
    }

    public String isOwnIssuance(Trade trade) {
        String issuerName = "";
        if (trade.getProduct() instanceof Equity) {
            Equity product = (Equity) trade.getProduct();
            if (product.getIssuer() != null) {
                issuerName = product.getIssuer().getCode() != null ? product.getIssuer().getCode() : "";
            }
        } else if (trade.getProduct() instanceof Bond) {
            Bond product = (Bond) trade.getProduct();
            LegalEntity issuer = BOCache.getLegalEntity(DSConnection.getDefault(), product.getIssuerId());
            issuerName = null != issuer ? issuer.getCode() : "";
        }
        return "BSTE".equals(issuerName) ? "SI" : "NO";
    }

    public boolean isFirstWorkingDateOfMonth(JDate jdate) {

        if (null != jdate) {
            final JDate prevBusDate = DateUtil.getPrevBusDate(jdate, Util.string2Vector("SYSTEM"));
            return prevBusDate.getMonth() < jdate.getMonth();
        }
        return false;
    }

    public JDate getLastWorkingDateOfMonth(int month, JDate valdate) {
        Calendar lastDayCal = valdate.getEOM().asCalendar();
        lastDayCal.add(Calendar.MONTH, month);
        lastDayCal.set(Calendar.DATE, lastDayCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        JDate.valueOf(lastDayCal);
        if (CollateralUtilities.isBusinessDay(JDate.valueOf(lastDayCal), Util.string2Vector("SYSTEM"))) {
            return JDate.valueOf(lastDayCal);
        } else {
            return DateUtil.getPrevBusDate(JDate.valueOf(lastDayCal), Util.string2Vector("SYSTEM"));
        }
    }

    public JDate getMaturityPDVFee(BOCre boCre, Trade trade) {
        JDate maturityDate = null;
        if (null != boCre) {
            maturityDate = boCre.getEffectiveDate();
            Trade tradeToProcess = trade;
            if (null == tradeToProcess) {
                tradeToProcess = this.getTrade(boCre.getTradeLongId());
            }
            JDate endDate = ((SecLending) tradeToProcess.getProduct()).getEndDate();
            if (endDate != null && endDate.before(boCre.getEffectiveDate())) { //antes de effective date
                maturityDate = endDate;
            }
        }
        return maturityDate;
    }


    public BOCre getPreviousFeeAccrualCre(JDate effectiveDate, Trade trade) {
        if (null != trade) {
            JDate prevEffectiveDate = effectiveDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
            if (effectiveDate.getMonth() == prevEffectiveDate.getMonth()) {
                final StringBuilder where = new StringBuilder();
                where.append(" bo_cre_type = 'FEE_ACCRUAL' ");
                where.append(" AND ");
                where.append(" ( cre_status = 'NEW') ");
                where.append(" AND ");
                where.append(" ( sent_status = 'SENT' ) ");
                where.append(" AND ");
                where.append(" trunc(effective_date) = ");
                where.append(Util.date2SQLString(prevEffectiveDate));
                where.append(" AND ");
                where.append(" trade.trade_id = bo_cre.trade_id ");
                where.append(" AND ");
                where.append(" trade.trade_id = " + trade.getLongId());

                try {
                    CreArray boCres = DSConnection.getDefault().getRemoteBackOffice().getBOCres("trade", where.toString(), null);
                    if (null != boCres && !Util.isEmpty(boCres.getCres())) {
                        if (Arrays.stream(boCres.getCres()).anyMatch(x -> "REVERSAL".equalsIgnoreCase(x.getCreType()))) {
                            return null;
                        }
                        return boCres.get(0);
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Error loading previous FEE_ACCTRUAL Cre for trade: " + e.getCause());
                }
            }
        }
        return null;
    }

    public BOCre getLinkCre(long linkedId) {
        try {
            return DSConnection.getDefault().getRemoteBackOffice().getBOCre(linkedId);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading boCre " + e.getCause());
        }
        return null;
    }

    public String getCcpName(CollateralConfig config) {
        String adValue = null != config && !Util.isEmpty(config.getAdditionalField("GUARANTEE_TYPE")) ? config.getAdditionalField("GUARANTEE_TYPE") : "";
        return Arrays.asList("FEE", "PAI").contains(adValue) ? adValue : "";
    }

    public String getTomadoPrestado(Trade trade) {
        if (null != trade) {
            Product product = trade.getProduct();
            if (product instanceof SecLending) {
                String direction = ((SecLending) product).getDirection();
                if ("Borrow".equalsIgnoreCase(direction)) {
                    return "T";
                } else if ("Lend".equalsIgnoreCase(direction)) {
                    return "P";
                }
            } else if (product instanceof Repo) {
                int directionSign = ((Repo) product).getSign();
                if (directionSign < 0) {
                    return "T";
                } else {
                    return "P";
                }
            }
        }
        return "";
    }

    public boolean sendAgrego() {
        Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(), SEND_AGREGO);
        if (!Util.isEmpty(domainValues)) {
            try {
                return Boolean.valueOf(domainValues.get(0));
            } catch (Exception e) {
                Log.error(this, "Error parsing to boolean. " + e.getCause());
            }
        }
        return false;
    }

    public boolean isOpen(Trade trade) {
        if (trade != null && trade.getProduct() instanceof SecLending) {
            return "OPEN".equalsIgnoreCase(((SecLending) trade.getProduct()).getMaturityType());
        }
        return false;
    }

    public boolean isSecRepo(Trade trade) {
        return null != trade && Arrays.asList("Repo", "SecLending").contains(trade.getKeywordValue("CASourceProductType"));
    }

    public String getPartenonCA(Trade trade) {
        String caSource = null != trade ? trade.getKeywordValue("CASource") : "";
        if (!Util.isEmpty(caSource)) {
            try {
                Trade secLending = DSConnection.getDefault().getRemoteTrade().getTrade(Long.parseLong(caSource));
                return null != secLending ? secLending.getKeywordValue("PartenonAccountingID") : "";

            } catch (CalypsoServiceException e) {
                Log.error(this, "Can't retrieve any trade with tradeId: " + caSource, e);
            }
        }
        return "";
    }

    public String getTomadoPrestadoCA(Trade trade) {
        String caSource = null != trade ? trade.getKeywordValue("CASource") : "";
        if (!Util.isEmpty(caSource)) {
            try {
                Trade tradeCaSource = DSConnection.getDefault().getRemoteTrade().getTrade(Long.parseLong(caSource));
                return null != tradeCaSource ? getTomadoPrestado(tradeCaSource) : "";

            } catch (CalypsoServiceException e) {
                Log.error(this, "Can't retrieve any trade with tradeId: " + caSource, e);
            }
        }
        return "";
    }

    /**
     * Valid if BOCre coming from event COT_REV.
     *
     * @param boCre
     * @return
     */
    public Boolean isEventCOT_REV(BOCre boCre) {
        return null != boCre && BOCreConstantes.COT_REV.equalsIgnoreCase(boCre.getEventType());
    }

    /**
     * Load accountingRule
     *
     * @param cre
     * @return
     */
    public String loadAccountingRule(BOCre cre) {
        if ("NOM_FULL".equalsIgnoreCase(cre.getEventType()) || "NOM_FULL_REV".equalsIgnoreCase(cre.getEventType()) ||
                "REALIZED_PL".equalsIgnoreCase(cre.getEventType()) || "UNREALIZED_PL".equalsIgnoreCase(cre.getEventType()) ||
                "ADDITIONAL_FEE".equalsIgnoreCase(cre.getEventType()) || "WRITE_OFF".equalsIgnoreCase(cre.getEventType())) {

            int ruleId = cre.getAccountingRuleId();
            try {
                if (ruleId > 0) {
                    AccountingRule accRule = DSConnection.getDefault().getRemoteAccounting().getAccountingRule(ruleId);
                    return accRule.getName();
                }
            } catch (Exception e) {
                Log.error(this, "Could not get the accounting rule " + ruleId);
            }
        }
        return "";
    }

    public String loadIsinForRepo(Trade trade) {
        return Optional.ofNullable(trade)
                .filter(t -> t.getProduct() instanceof Repo)
                .map(p -> ((Repo) p.getProduct()).getSecurity())
                .map(sec -> sec.getSecCode("ISIN")).orElse("");
    }

    /**
     * Return calcelation Date for a Trade.
     *
     * @param trade
     * @return
     */
    public JDate getCancelationDateFromTradeAudit(Trade trade) {
        if (null != trade) {
            StringBuilder whereBuilder = new StringBuilder();
            whereBuilder.append(" entity_class_name LIKE 'Trade'");
            whereBuilder.append(" AND entity_id = " + trade.getLongId());
            whereBuilder.append(" AND new_value LIKE 'CANCELED'");
            try {
                final Vector<?> rawAudit = DSConnection.getDefault().getRemoteTrade()
                        .getAudit(whereBuilder.toString(), "modif_date DESC", null);
                if (!Util.isEmpty(rawAudit)) {
                    return Optional.ofNullable(rawAudit)
                            .filter(v -> v.get(0) instanceof AuditValue)
                            .map(ve -> ve.get(0))
                            .map(audit -> ((AuditValue) audit).getModifDate())
                            .map(date -> date.getJDate(TimeZone.getDefault())).get();
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading audit for trade: " + trade.getLongId() + " " + e.getCause());
            }
        }
        return null;
    }


    public CashFlow getRepoCashFlow(Trade trade, BOCre boCre) {
        if (null != trade && trade.getProduct() instanceof Repo) {
            Repo product = (Repo) trade.getProduct();
            final JDate repoEndDate = product.getEndDate();
            CashFlowSet cashFlows = null;
            try {
                cashFlows = product.generateMoneyMarketFlows(boCre.getEffectiveDate());
                if (null != cashFlows) {
                    RepoCashFlowLayout layout = new RepoCashFlowLayout();
                    layout.sortFlows(cashFlows);
                    if (boCre.getEffectiveDate().equals(repoEndDate)) {
                        return Arrays.stream(cashFlows.getFlows())
                                .filter(flow -> "INTEREST".equalsIgnoreCase(flow.getType()) && flow.getDate().gte(boCre.getEffectiveDate()))
                                .findFirst().orElse(null);
                    } else {
                        return Arrays.stream(cashFlows.getFlows())
                                .filter(flow -> "INTEREST".equalsIgnoreCase(flow.getType()) && flow.getDate().after(boCre.getEffectiveDate()))
                                .findFirst().orElse(null);
                    }
                }
            } catch (FlowGenerationException e) {
                Log.error(this, "Error generating Flows for trade: " + trade.getLongId());
            }
        }
        return null;
    }

    public CashFlowSet getRepoCashFlows(Trade trade, BOCre boCre) {
        if (null != trade && trade.getProduct() instanceof Repo) {
            Repo product = (Repo) trade.getProduct();
            try {
                CashFlowSet cashFlows = product.generateMoneyMarketFlows(boCre.getEffectiveDate());
                if (null != cashFlows) {
                    RepoCashFlowLayout layout = new RepoCashFlowLayout();
                    layout.sortFlows(cashFlows);
                    return cashFlows;
                }
            } catch (FlowGenerationException e) {
                Log.error(this, "Error generating Flows for trade: " + trade.getLongId());
            }
        }
        return new CashFlowSet();
    }

    public CashFlow getCashFlowForRepoACCRUAL(CashFlowSet cashFlows, Trade trade, BOCre boCre) {
        if (null != trade && trade.getProduct() instanceof Repo && null != cashFlows) {
            Repo product = (Repo) trade.getProduct();
            final JDate repoEndDate = product.getEndDate();
            if (boCre.getEffectiveDate().equals(repoEndDate)) {
                return Arrays.stream(cashFlows.getFlows())
                        .filter(flow -> "INTEREST".equalsIgnoreCase(flow.getType()) && flow.getDate().gte(boCre.getEffectiveDate()))
                        .findFirst().orElse(null);
            } else {
                return Arrays.stream(cashFlows.getFlows())
                        .filter(flow -> "INTEREST".equalsIgnoreCase(flow.getType()) && flow.getDate().after(boCre.getEffectiveDate()))
                        .findFirst().orElse(null);
            }
        }
        return null;
    }


    public String getNettingNumber(BOTransfer botransfer, Trade trade) {
        long nettingNumber = 1;
        if (trade != null && null != botransfer) {
            BOTransfer dapTransfer = botransfer.getCashTransfer();
            try {
                PricingEnv env = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("OFFICIAL_ACCOUNTING");
                if (null != dapTransfer && null != env) {
                    List<CashFlow> cashFlows = dapTransfer.getDAPCashFlows(trade, env);
                    nettingNumber = cashFlows.stream().map(obj -> obj.getClass().getSimpleName()).filter("CashFlow"::equalsIgnoreCase).count();
                }
            } catch (Exception e) {
                Log.error(this, "Error setting NettingNumber" + e);
            }
        }
        return String.valueOf(nettingNumber);
    }

    public String getNettingType(BOTransfer botransfer) {
        if (Optional.ofNullable(botransfer).isPresent()) {
            if ("NONE".equalsIgnoreCase(botransfer.getNettingType())
                    || botransfer.getNettingType().contains("Trade")
                    || botransfer.getNettingType().contains("FeeParent")
                    || botransfer.getNettingType().contains("SecLendingFeeCashPoolDAP")) {
                return "N";
            }
        }
        return "Y";
    }

    public JDate loadSettlemetDateUnliqued(BOCre boCre) {
        try {
            if (validatePositionLong(boCre.getAmount(0))) {
                return DSConnection.getDefault().getRemoteTrade().getTrade(boCre.getLinkedTradeLongId()).getSettleDate();
            } else {
                return boCre.getSettlementDate();
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading Trade: " + boCre.getLinkedTradeLongId() + ": " + e);
        }
        return null;
    }

    public JDate loadTradeDateUnliqued(BOCre boCre) {
        try {
            if (validatePositionLong(boCre.getAmount(0))) {
                return JDate.valueOf(DSConnection.getDefault().getRemoteTrade().getTrade(boCre.getLinkedTradeLongId()).getTradeDate());
            } else {
                return boCre.getTradeDate();
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading Trade: " + boCre.getLinkedTradeLongId() + ": " + e);
        }
        return null;
    }

    public Long loadTradeIdUnliqued(BOCre boCre) {
        try {
            if (validatePositionLong(boCre.getAmount(0))) {
                return DSConnection.getDefault().getRemoteTrade().getTrade(boCre.getLinkedTradeLongId()).getLongId();
            } else {
                return boCre.getTradeLongId();
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading Trade: " + boCre.getLinkedTradeLongId() + ": " + e);
        }
        return null;
    }

    public String loadPartenonIdUnliqued(BOCre boCre, Trade trade) {
        try {
            if (validatePositionLong(boCre.getAmount(0))) {
                Trade linkedTrade = DSConnection.getDefault().getRemoteTrade().getTrade(boCre.getLinkedTradeLongId());
                return null != linkedTrade ? BOCreUtils.getInstance().loadPartenonId(linkedTrade) : "";
            } else {
                return null != trade ? BOCreUtils.getInstance().loadPartenonId(trade) : "";
            }

        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading Trade: " + boCre.getLinkedTradeLongId() + ": " + e);
        }
        return null;
    }

    private boolean validatePositionLong(double amount) {
        if (amount > 0) {
            return true;
        }
        return false;

    }

    public String generateAlias(Product security, String accountingLink, String subType, String isInternal, String bookName) {
        Equity product = (Equity) security;
        String issuerName = product.getIssuer().getCode() != null ? product.getIssuer().getCode() : "";
        String result = "";
        String entity = getEntity(bookName);
        String entityCod = getEntityCod(entity, false);
        String centro = getCentroContable(product, entity, false);

        if ("Negociacion".equalsIgnoreCase(accountingLink)) {
            if ("CS".equalsIgnoreCase(subType)) {
                if ("BSTE".equalsIgnoreCase(issuerName)) {
                    if ("Y".equalsIgnoreCase(isInternal)) {
                        result = entityCod + centro + "_RVAUINAONE";
                    } else {
                        result = entityCod + centro + "_RVAUCOAONE";
                    }
                } else {
                    if ("Y".equalsIgnoreCase(isInternal)) {
                        result = entityCod + centro + "_RVCAINAONE";
                    } else {
                        result = entityCod + centro + "_RVCACOAONE";
                    }
                }
            } else if ("DERSUS".equalsIgnoreCase(subType)) {
                if ("BSTE".equalsIgnoreCase(issuerName)) {
                    if ("Y".equalsIgnoreCase(isInternal)) {
                        result = entityCod + centro + "_RVCACINDSNE";
                    } else {
                        result = entityCod + centro + "_RVAUCODSNE";
                    }
                } else {
                    if ("Y".equalsIgnoreCase(isInternal)) {
                        result = entityCod + centro + "_RVCAINDSNE";
                    } else {
                        result = entityCod + centro + "_RVCACODSNE";
                    }
                }
            } else if ("PS".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINPPNE";
                } else {
                    result = entityCod + centro + "_RVCACOPPNE";
                }
            } else if ("INSW".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINCINE";
                } else {
                    result = entityCod + centro + "_RVCACOCINE";
                }
            } else if ("ADR".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINADNE";
                } else {
                    result = entityCod + centro + "_RVCACOADNE";
                }
            } else if ("PFI".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINPFAM";
                } else {
                    result = entityCod + centro + "_RVCACOPFAM";
                }
            } else if ("CO2".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAIND2NE";
                } else {
                    result = entityCod + centro + "_RVCACOD2NE";
                }
            } else if ("VCO2".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINV2NE";
                } else {
                    result = entityCod + centro + "_RVCACOV2NE";
                }
            } else if ("ETF".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINETNE";
                } else {
                    result = entityCod + centro + "_RVCACOETNE";
                }
            }
        } else if ("Inversion crediticia".equalsIgnoreCase(accountingLink)) {
            if ("PS".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINPPAM";
                } else {
                    result = entityCod + centro + "_RVCACOPPAM";
                }
            } else if ("PEGROP".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINEGAM";
                } else {
                    result = entityCod + centro + "_RVCACOEGAM";
                }
            }
        } else if ("Otros a valor razonable".equalsIgnoreCase(accountingLink)) {
            if ("CS".equalsIgnoreCase(subType)) {
                if ("BSTE".equalsIgnoreCase(issuerName)) {
                    if ("Y".equalsIgnoreCase(isInternal)) {
                        result = entityCod + centro + "_RVAUINAOOV";
                    } else {
                        result = entityCod + centro + "_RVAUCOAOOV";
                    }
                } else {
                    if ("Y".equalsIgnoreCase(isInternal)) {
                        result = entityCod + centro + "_RVCAINAOOV";
                    } else {
                        result = entityCod + centro + "_RVCACOAOOV";
                    }
                }
            } else if ("DERSUS".equalsIgnoreCase(subType)) {
                if ("BSTE".equalsIgnoreCase(issuerName)) {
                    if ("Y".equalsIgnoreCase(isInternal)) {
                        result = entityCod + centro + "_RVCACINDSOV";
                    } else {
                        result = entityCod + centro + "_RVAUCODSOV";
                    }
                } else {
                    if ("Y".equalsIgnoreCase(isInternal)) {
                        result = entityCod + centro + "_RVCAINDSOV";
                    } else {
                        result = entityCod + centro + "_RVCACODSOV";
                    }
                }
            } else if ("PS".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINPPOV";
                } else {
                    result = entityCod + centro + "_RVCACOPPOV";
                }
            } else if ("INSW".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINCIOV";
                } else {
                    result = entityCod + centro + "_RVCACOCIOV";
                }
            } else if ("ADR".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINADOV";
                } else {
                    result = entityCod + centro + "_RVCACOADOV";
                }
            } else if ("PFI".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINPFOV";
                } else {
                    result = entityCod + centro + "_RVCACOPFOV";
                }
            } else if ("CO2".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAIND2OV";
                } else {
                    result = entityCod + centro + "_RVCACOD2OV";
                }
            } else if ("VCO2".equalsIgnoreCase(subType)) {
                if ("Y".equalsIgnoreCase(isInternal)) {
                    result = entityCod + centro + "_RVCAINV2OV";
                } else {
                    result = entityCod + centro + "_RVCACOV2OV";
                }
            }

        }

        return result;
    }

    public String loadAccountingRuleProduc(BOCre boCre) {
        int ruleId = boCre.getAccountingRuleId();
        try {
            if (ruleId > 0) {
                AccountingRule accRule = DSConnection.getDefault().getRemoteAccounting().getAccountingRule(ruleId);
                return accRule.getName();
            }
        } catch (Exception e) {
            Log.error(this, "Could not get the accounting rule " + ruleId);
        }
        return null;
    }

    public JDate getRepoMaturityDate(Trade trade) {
        if (null != trade && trade.getProduct() instanceof Repo) {
            Repo repo = (Repo) trade.getProduct();
            JDate endDate = repo.getEndDate();
            if (endDate != null) {
                return endDate;
            }

            JDate projectedEndDate = repo.getProjectedEndDate(JDate.getNow());
            if (projectedEndDate != null) {
                return projectedEndDate;
            }
        }
        return null;
    }

    public void setRepoPartenonAccId(BOCre boCre, Trade trade, SantBOCre creMessage) {
        if (isPartenonChange(boCre, trade) && null != creMessage) {
            creMessage.setOriginalEvent("PARTENON_CHANGE");
            creMessage.setPartenonId(loadPartenonId(trade, "OldPartenonAccountingID"));
        } else {
            creMessage.setPartenonId(loadPartenonId(trade));
        }
    }

    public boolean isPartenonChange(BOCre boCre, Trade trade) {
        return isReversal(boCre) && checkEffDateRequestDate(boCre, trade);
    }

    public boolean checkEffDateRequestDate(BOCre boCre, Trade trade) {
        try {
            JDate effectiveDate = null;
            JDate partenonRequestDate = null;
            String partenonDate = null != trade ? trade.getKeywordValue("PartenonRequestDate") : null;

            if (!Util.isEmpty(partenonDate)) {
                if (partenonDate.contains("-")) {
                    partenonDate = partenonDate.replace("-", "/");
                }
                Date date = FdnUtilProvider.getDateFormattingUtil().stringToDate(partenonDate, Locale.forLanguageTag("ES"));
                partenonRequestDate = JDate.valueOf(date);
            }

            String eventType = boCre.getEventType();
            if (BOCreConstantes.ACCRUAL.equalsIgnoreCase(eventType)
                    || BOCreConstantes.MTM_NET.equalsIgnoreCase(eventType)) {
                effectiveDate = boCre.getEffectiveDate();
            } else {
                effectiveDate = JDate.getNow();
            }

            return null != partenonRequestDate && null != effectiveDate && partenonRequestDate.equals(effectiveDate);
        } catch (Exception e) {
            Log.error(this, "Error loading date from " + trade.getLongId() + " " + e);
        }
        return false;
    }

    public boolean isReversal(BOCre boCre) {
        return null != boCre && REVERSAL.equalsIgnoreCase(boCre.getCreType());
    }

    public String loadClaimProductType(Trade trade) {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue("CASourceProductType")).orElse("");
    }

    public String loadWithholdingTaxConfigId(Trade trade) {
        String configId = trade.getKeywordValue("WithholdingTaxConfigId");
        if (StringUtils.isNotEmpty(configId)) {
            List<WithholdingTaxConfig> allConfigs = BOCache.getWithholdingTaxConfigs(DSConnection.getDefault());
            for (int i = 0; i < allConfigs.size(); i++) {
                WithholdingTaxConfig config = allConfigs.get(i);
                if (Integer.valueOf(configId) == config.getId()) {
                    return config.getIssuerCountry().getISOCode();
                }
            }
        }
        return "";
    }

    public Integer loadContractID(Trade trade) {
        return trade != null ? trade.getKeywordAsInt("CASource") : 0;
    }

    public JDate loadRepoInterestMaturityDate(BOTransfer creBoTransfer) {
        try {
            String end_date = Optional.ofNullable(creBoTransfer).map(t -> t.getAttribute("EndDate")).orElse("");
            if (!Util.isEmpty(end_date)) {
                if (end_date.contains("-")) {
                    end_date = end_date.replace("-", "/");
                }
                Date date = FdnUtilProvider.getDateFormattingUtil().stringToDate(end_date, Locale.ENGLISH);
                return JDate.valueOf(date);
            }
        } catch (Exception e) {
            Log.error(this, "Error parsing End Date form transfer: " + e);
        }
        return null;
    }

    public String getCentroContable(Product product, String entity, boolean isALM) {
        String out = "";
        if (entity != null && entity.equals("BSTE")) {
            out = "1999";
            if (product != null && product.getSecCode(SEC_CODE_CENTRO_CONTABLE) != null) {
                out = product.getSecCode(SEC_CODE_CENTRO_CONTABLE);
            }
        } else if (entity != null && entity.equals("BDSD")) {
            out = !isALM ? "1111" : "2286";
        }
        return out;
    }

    public String getEntityCod(String entity, boolean isALM) {
        String out = "";
        if (entity != null && entity.equals("BSTE")) {
            out = "0049";
        } else if (entity != null && entity.equals("BDSD")) {
            out = !isALM ? "0306" : "0049";
        }
        return out;
    }

    public String getEntity(String bookName) {
        Book book = null;
        LegalEntity le = null;
        try {
            book = BOCache.getBook(DSConnection.getDefault(), bookName);
            if (book != null) {
                le = book.getLegalEntity();
                return le != null ? le.getCode() : "";
            }
        } catch (Exception e) {
            Log.error(this, "Cannot retrieve book with name = " + bookName, e);
        }
        return "";
    }

    public JDate addBusinessDays(JDate date, int num) {
        return null != date ? date.addBusinessDays(-1, Util.string2Vector("SYSTEM")) : null;
    }

    public void generatePositionLog(BOCre boCre, Long tradeid, Double cashPosition, Double boCresAmount, Double creAmount) {
        StringBuilder log = new StringBuilder();
        log.append("CreID: " + boCre.getId());
        log.append(" | TradeID: " + tradeid);
        log.append(" | CreType: " + boCre.getEventType());
        log.append(" | Position: " + cashPosition);
        log.append(" | SumCres: " + boCresAmount);
        log.append(" | CreAmount: " + creAmount + " |");
        Log.system("Saldo Cre", log.toString());
    }

    public Double calculateSettlement(Trade trade, String direction) {
        Double fee = 0.0;
        //Suma de todas las fees menos ADDITIONAL_FEE
        if(trade != null && trade.getFeesList() != null) {
            for (int i = 0; i < trade.getFeesList().size(); i++) {
                if (!"ADDITIONAL_FEE".equalsIgnoreCase(trade.getFeesList().get(i).getType())) {
                    fee = fee + trade.getFeesList().get(i).getAmount();
                }
            }
        }
        fee = "PAY".equalsIgnoreCase(direction) ? Math.abs(fee) : fee;

        return Math.abs(trade.getNegociatedPrice() * trade.getQuantity() * trade.getSplitBasePrice()) + fee;
    }

    public PricerMeasure calculatePM(JDatetime valDatetime, Trade trade, int pricerMeasure, String pEnv) {
        PricerMeasure pm = new PricerMeasure(pricerMeasure);
        PricerMeasureCumulativeCash cumulativeCash = new PricerMeasureCumulativeCash(pricerMeasure);
        if (311 == pricerMeasure) {
            cumulativeCash.setName(pm.getName());
            pm = cumulativeCash;
        }
        try {
            PricingEnv pricingEnv = PricingEnv.loadPE(pEnv, valDatetime);
            Pricer pricer = pricingEnv.getPricerConfig().getPricerInstance(trade.getProduct());
            PricerMeasure[] pricerMeasures = new PricerMeasure[]{pm};
            pricer.price(trade, valDatetime, pricingEnv, pricerMeasures);
            return pm;
        } catch (Exception ex) {
            Log.error(this, ex);
        }
        return pm;
    }

    public BOCre getPreviousReversalCre(JDate effectiveDate, String creType, Trade trade) {
        if (null != trade) {
            JDate date = effectiveDate;

            final StringBuilder where = new StringBuilder();
            where.append(" bo_cre_type = '" + creType + "' ");
            where.append(" AND ");
            where.append(" ( cre_status = 'NEW') ");
            where.append(" AND ");
            where.append(" ( sent_status = 'SENT' ) ");
            where.append(" AND ");
            where.append(" trunc(effective_date) = ");
            where.append(Util.date2SQLString(date));
            where.append(" AND ");
            where.append(" trade.trade_id = bo_cre.trade_id ");
            where.append(" AND ");
            where.append(" trade.trade_id = " + trade.getLongId());

            try {
                CreArray boCres = DSConnection.getDefault().getRemoteBackOffice().getBOCres("trade", where.toString(), null);
                if (null != boCres && !Util.isEmpty(boCres.getCres())) {
                    BOCre finalCre = null;
                    List<BOCre> collect = Arrays.stream(boCres.getCres()).filter(x -> "REVERSAL".equalsIgnoreCase(x.getCreType())).collect(Collectors.toList());
                    for (BOCre cre : collect) {
                        if (null == finalCre) {
                            finalCre = cre;
                        } else if (cre.getId() > finalCre.getId()) {
                            finalCre = cre;
                        }
                    }
                    return finalCre;
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading previous " + creType + " Cre for trade: " + e.getCause());
            }

        }
        return null;
    }

    public String getCAReference(Trade trade) {
        if (null != trade) {

            // CA RF start
            if(trade.getProduct() instanceof CA) {
                CA ca = (CA) trade.getProduct();
                if(ca != null && ca.getUnderlyingProduct() != null && ca.getUnderlyingProduct() instanceof Bond) {
                    return trade.getKeywordValue("CARefConciDate");
                }
            }
            // CA RF end

            String caRef = trade.getKeywordValue("CARefConci");
            if (Util.isEmpty(caRef)) {
                return generateCARefConci(trade);
            }
            return caRef;
        }
        return "";
    }


    private String generateCARefConci(Trade trade) {
        String key = "";
        if (null != trade && trade.getProduct() instanceof CA) {
            String swiftEventCodeName = Optional.ofNullable(((CA) trade.getProduct()).getSwiftEventCode())
                    .map(CASwiftEventCode::getSwiftCodeDescription)
                    .map(CASwiftCodeDescription::toString)
                    .orElse("");
            int id = trade.getProduct().getId();
            if (!Util.isEmpty(swiftEventCodeName)) {
                key = swiftEventCodeName + id;
            }
        }
        return key;
    }

    /**
     * Get UnderlyingType for Repo Cres
     *
     * @param trade
     * @return
     */
    public String getMaturityType(Trade trade) {
        String mattype = "";
        Product product = Optional.ofNullable(trade).map(Trade::getProduct).orElse(null);
        if (product instanceof Repo) {
            Repo repo = (Repo) product;
            mattype = repo.getMaturityType();
            if ("OPEN".equals(mattype)) {
                mattype = "OP";
            } else if ("TERM".equals(mattype)) {
                mattype = "TR";
            } else if (!Util.isEmpty(mattype)) {
                mattype = "TR";
            }
        }
        return mattype;
    }


    /**
     * get xfer SECURITY and  xferType  by effectiveDate
     *
     * @param boCre
     * @param trade
     * @param xferType
     * @return
     */
    public TransferArray getXfersByEffectiveDate(BOCre boCre, Trade trade, String xferType) {
        TransferArray boTransfers = new TransferArray();
        JDate effectiveDate = boCre.getEffectiveDate();
        StringBuilder where = new StringBuilder();
        where.append("bo_transfer.trade_id =" + trade.getLongId());
        where.append("and trunc(bo_transfer.value_date) = " + Util.date2SQLString(effectiveDate));
        where.append(" and bo_transfer.transfer_status<>'CANCELED' ");
        where.append(" and bo_transfer.transfer_type IN ( 'SECURITY','" + xferType + "')");
        try {
            boTransfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(null, where.toString(), "bo_transfer.transfer_id DESC", 0, null);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading transfers: " + e);
        }
        return boTransfers;
    }


    public boolean isIncreaseRepo(Trade trade, BOTransfer xfer) {
        AtomicBoolean isIncrease = new AtomicBoolean(false);
        String repoDirection = Optional.ofNullable(trade.getProduct()).filter(Repo.class::isInstance).map(Repo.class::cast).map(repo -> repo.getDirection(Repo.REPO, repo.getSign())).orElse("");
        Optional.ofNullable(xfer).ifPresent(xf -> {
            if ("SECURITY".equalsIgnoreCase(xf.getTransferType())) {
                if (("Reverse".equalsIgnoreCase(repoDirection) && "RECEIVE".equalsIgnoreCase(xf.getPayReceive()))
                        || ("Repo".equalsIgnoreCase(repoDirection) && "PAY".equalsIgnoreCase(xf.getPayReceive()))) {
                    isIncrease.set(true);
                }
            }
        });
        return isIncrease.get();
    }

    public boolean isXferSplitReason(BOTransfer xfer) {
        return Optional.ofNullable(xfer).filter(xf -> "PartialSettlement".equalsIgnoreCase(xf.getSplitReasonFrom())).isPresent();
    }


    public BOTransfer getLastXferByType(Trade trade, String type) {
        StringBuilder where = new StringBuilder();
        where.append("bo_transfer.trade_id =" + trade.getLongId());
        where.append(" AND bo_transfer.transfer_status = '" + type + "'");
        //builder.append(" AND trunc(bo_transfer.value_date) = " + Util.date2SQLString(effectiveDate));
        try {
            TransferArray boTransfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(null, where.toString(), "bo_transfer.transfer_id DESC", 0, null);
            if (!Util.isEmpty(boTransfers)) {
                return boTransfers.get(0);
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading transfers: " + e);
        }
        return null;
    }


    public BOCre getLastBoCreACCRUAL(JDate effectiveDate, Trade trade, String eventType, String creType, boolean onDate) {
        BOCre lastBoCre = null;
        if (null != trade && !Util.isEmpty(eventType) && !Util.isEmpty(creType)) {
            String where = buildWhereLastBoCre(effectiveDate, trade, eventType, creType, onDate);
            try {
                CreArray array = DSConnection.getDefault().getRemoteBO().getBOCres(null, where, null);
                if (array != null && !array.isEmpty()) {
                    lastBoCre = Arrays.stream(array.getCres()).filter(s -> s.getAmount(0) != 0D).max(Comparator.comparing(s -> s.getEffectiveDate().getJDatetime())).orElse(null);
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "Could not retrieve CREs from Trade: " + e.toString());
            }
        }
        return lastBoCre;
    }

    public BOCre getLastBoCreINTEREST(JDate effectiveDate, Trade trade, String eventType, String creType, boolean onDate) {
        BOCre lastBoCre = null;
        if (null != trade && !Util.isEmpty(eventType) && !Util.isEmpty(creType)) {
            String where = buildWhereLastBoCre(effectiveDate, trade, eventType, creType, onDate);
            try {
                CreArray array = DSConnection.getDefault().getRemoteBO().getBOCres(null, where, null);
                if (array != null && !array.isEmpty()) {
                    lastBoCre = Arrays.stream(array.getCres()).max(Comparator.comparing(s -> s.getEffectiveDate().getJDatetime())).orElse(null);
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "Could not retrieve CREs from Trade: " + e.toString());
            }
        }
        return lastBoCre;
    }

    private String buildWhereLastBoCre(JDate effectiveDate, Trade trade, String eventType, String creType, boolean onDate) {
        final StringBuilder where = new StringBuilder();
        where.append(" bo_cre_type = '" + eventType + "' ");
        where.append(" AND ");
        where.append(" cre_type = '" + creType + "' ");
        where.append(" AND ");
        where.append(" ( cre_status = 'NEW') ");
        where.append(" AND ");
        where.append(" ( sent_status = 'SENT' ) ");
        where.append(" AND ");
        where.append(" trade_id = " + trade.getLongId());
        if (null != effectiveDate) {
            where.append(" AND ");
            if (onDate) {
                where.append(" trunc(effective_date) = ");
            } else {
                where.append(" trunc(effective_date) <= ");
            }
            where.append(Util.date2SQLString(effectiveDate));
        }
        return where.toString();
    }

    /**
     * Get BoCre INTEREST enviado antes de 1h ese mismo día
     *
     * @param effectiveDate
     * @param trade
     * @param eventType
     * @param creType
     * @return
     */
    public BOCre getPreviousInterestSend(JDatetime effectiveDate, Trade trade, String eventType, String creType){

        JDatetime startOfDay = new JDatetime(effectiveDate.getJDate(TimeZone.getDefault()), 0, 0, 0, TimeZone.getDefault());

        BOCre lastBoCre = null;
        if (null != trade && null!=effectiveDate && !Util.isEmpty(eventType) && !Util.isEmpty(creType)) {
            final StringBuilder where = new StringBuilder();
            where.append(" bo_cre_type = '"+eventType+"' ");
            where.append(" AND ");
            where.append(" cre_type = '"+creType+"' ");
            where.append(" AND ");
            where.append(" cre_status = 'NEW' ");
            where.append(" AND ");
            where.append(" sent_status = 'SENT' ");
            where.append(" AND ");
            where.append(" trade_id = " + trade.getLongId());
            where.append(" AND ");
            where.append("sent_date BETWEEN " + Util.datetime2SQLString(startOfDay)
                    + " AND " + Util.datetime2SQLString(effectiveDate.add(0, -1, 0, 0, 0)));
            try {
                CreArray array = DSConnection.getDefault().getRemoteBO().getBOCres(null, where.toString(), null);
                if (array != null && !array.isEmpty()) {
                    lastBoCre = Arrays.stream(array.getCres()).max(Comparator.comparing(s -> s.getSentDate())).orElse(null);
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "Could not retrieve CREs from Trade: " + e.toString());
            }
        }
        return lastBoCre;
    }

    public String getCSTCAReference(Trade trade, BOTransfer transfer){
        String cstCAReference = "";
        String caReference = getCAReference(trade);
        if (!caReference.isEmpty()) {
            cstCAReference = caReference;
            int caRefLong = caReference.length();
            if (16 > caRefLong) {
                int transferIdLength = 16 - caRefLong;
                long transferId = transfer.getLongId();
                String sTransferID = Long.toString(transferId);
                String subTransferId = sTransferID.substring(sTransferID.length() - transferIdLength);
                cstCAReference += subTransferId;
            }
        }
        return cstCAReference;
    }

    public String getNetCAReference (BOTransfer transfer){
        //se compone como el prefijo NCA + transferId
        String prefix = "NCA";
        long transferId = transfer.getLongId();
        String sTransferId = Long.toString(transferId);
        int totalLong = prefix.length() + sTransferId.length();
        int fillnumber = sTransferId.length() + (16 - totalLong);
        String zerosPlusTransferId = padStringZero(sTransferId, fillnumber);
        return (prefix + zerosPlusTransferId);

    }

    /**
     *  Reevaluación: SI (effectiveDate > settlementDate) Y (no hay flujo de INTEREST en effective date) entonces “Y” sino “N”
     *
     * @param trade
     * @param effectiveDate
     * @return
     */
    public String getRevaluation( Trade trade, JDate effectiveDate){
        final JDate settlementDate = Optional.ofNullable(trade).map(Trade::getProduct).filter(Repo.class::isInstance).map(Repo.class::cast).map(Repo::getStartDate).orElse(null);
        if(null!=effectiveDate && effectiveDate.after(settlementDate)){
            return "Y";
        }
        return "N";
    }

    public boolean sendRevaluation() {
        return !Util.isEmpty(Optional.ofNullable(LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", SEND_REVALUATION )).orElse(""));
    }

    public LegalEntity getLegalEntity(Product security) {
        LegalEntity le = new LegalEntity();
        if (security instanceof Equity) {
            Equity equity = (Equity) security;
            le = equity.getIssuer();
        } else if (security instanceof Bond) {
            Bond bond = (Bond) security;
            if (bond.getIssuerId() != 0) {
                le = BOCache.getLegalEntity(DSConnection.getDefault(), bond.getIssuerId());
            }
        }
        return le;
    }

    public String getIssuerCode(LegalEntity le) {
        return le.getCode() != null ? le.getCode() : "";
    }

    public String getIssuerName(LegalEntity le) {
        return le.getName() != null ? le.getExternalRef() : "";
    }

    public String getProductTypeBond() {
        return "Bond";
    }

    public String getProductTypeBondForwardSpot(Trade trade) {
        return "true".equalsIgnoreCase(trade.getKeywordValue("BondForward")) ? "BondForward" : "Bond";
    }

    public String getSubTypeBond(Trade trade) {
        if ("true".equalsIgnoreCase(trade.getKeywordValue("BondForward"))) {
            return trade.getKeywordValue("BondForwardType");
        } else if(null!=trade){
            return trade.getProductSubType();
        }
        return "";
    }

    public String loadProductBondCurrency(Trade trade) {
        Bond product = (Bond) trade.getProduct();
        return null != product ? product.getCurrency() : "";
    }

    public String loadFixingDate(Trade trade) {
        return null != trade ? trade.getKeywordValue("BF_FixingDate") : "";
    }

    public Boolean isBFWDelivery(Trade trade) {
        return "Delivery".equalsIgnoreCase(trade.getKeywordValue("BondForwardType")) ? true : false;
    }

    public static JDate calculateforwardDate(Trade trade, Product security) {
        return trade.getSettleDate().addBusinessDays( (-1) * Integer.valueOf(trade.getKeywordValue("BondSettleDays")), security.getHolidays());
    }


    public String loadUnderlyingSubType(Trade trade){
        if(trade!=null && trade.getProduct() instanceof Equity) {
            Equity equity = (Equity) trade.getProduct();
            String equityType = equity.getSecCode("EQUITY_TYPE");
            if (!Util.isEmpty(equityType) && ("CO2".equalsIgnoreCase(equityType) || "VCO2".equalsIgnoreCase(equityType))) {
                String productSubType = trade.getKeywordValue("Mx_Product_SubType");
                if (!Util.isEmpty(productSubType) && "SPOT".equalsIgnoreCase(productSubType)) {
                    return "spot";
                } else if (!Util.isEmpty(productSubType) && "FORWARD".equalsIgnoreCase(productSubType)) {
                    return "forward";
                }
            }
            return "spot";
        }
        return "";
    }


    public String loadUnderlyingDeliveryType(Trade trade){
        Equity equity = (Equity) trade.getProduct();
        String equityType = equity.getSecCode("EQUITY_TYPE");
        if(!Util.isEmpty(equityType) && ("CO2".equalsIgnoreCase(equityType) || "VCO2".equalsIgnoreCase(equityType))){
            String mxDeliveryType = trade.getKeywordValue("MX_Delivery_Type");
            if(!Util.isEmpty(mxDeliveryType) && "Physical".equalsIgnoreCase(mxDeliveryType)){
                return "physical";
            }
            else if(!Util.isEmpty(mxDeliveryType) && "Cash".equalsIgnoreCase(mxDeliveryType)){
                return "cash";
            }
        }
        return "";
    }

    public String loadKeyword(Trade trade, String keywordName) {
        return null != trade ? trade.getKeywordValue(keywordName) : "";
    }

    public JDate loadDefaultCCCTDate() {
        return JDate.valueOf(0001, 1, 1);
    }

    public static String formatUnsignedNumber(final double value,
                                              final int length, final int decimals) {
        if (decimals != 0) {
            final String pattern = "%0" + (length + 1) + "." + decimals + "f";
            final DecimalFormatSymbols symbols = new DecimalFormatSymbols();

            return String.format(pattern, Math.abs(value)).replace(
                    String.valueOf(symbols.getDecimalSeparator()), "");
        } else {
            final String pattern = "%0" + (length) + "." + decimals + "f";
            return String.format(pattern, Math.abs(value));
        }
    }

    public BOCre getPreviousCST_S_SETTLED(BOTransfer boTransfer) {

        final StringBuilder where = new StringBuilder();
        where.append(" bo_cre_type = 'CST_S_SETTLED' ");
        where.append(" AND ");
        where.append(" (cre_status = 'NEW') ");
        where.append(" AND ");
        where.append(" (sent_status = 'SENT') ");
        where.append(" AND ");
        where.append(" bo_cre.transfer_id = ?");

        List<CalypsoBindVariable> bindVariables = new ArrayList<>();
        bindVariables.add(new CalypsoBindVariable(3000, Math.abs(boTransfer.getLinkedLongId())));

        try {
            CreArray boCres = DSConnection.getDefault().getRemoteBackOffice().getBOCres(null, where.toString(), bindVariables);
            if (null != boCres && !Util.isEmpty(boCres.getCres())) {
                Optional<BOCre> cre = Arrays.stream(boCres.getCres()).max(Comparator.comparing(BOCre::getId));
                return cre.orElse(null);
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading previous FEE_ACCTRUAL Cre for trade: " + e.getCause());
        }
        return null;
    }

    public String loadPartenonId (Trade trade, BOCre boCre){
        Vector<String> date = LocalCache.getDomainValues(DSConnection.getDefault(), "ActivateAccountingDate");
        if (trade.getEnteredDate().getJDate(TimeZone.getDefault()).gte(JDate.valueOf(date.get(0)))){
            return loadPartenonIdFromCre(boCre);
        } else {
            return loadPartenonId(trade);
        }
    }

    public String loadPartenonIdFromCre(BOCre boCre){
        return boCre.getAttributeValue("PartenonAccountingID");
    }


    public String getCaType(Trade trade){
        String caType = "";
        if(trade != null && trade.getProduct() != null && trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            if(ca != null && ca.getUnderlyingProduct() != null && ca.getUnderlyingProduct() instanceof Bond) {
                String subType = trade.getProductSubType();
                if(!Util.isEmpty(subType) && "INTEREST".equalsIgnoreCase(subType)){
                    caType = "CUPON";
                }
                else if (!Util.isEmpty(subType) && ("AMORTIZATION".equalsIgnoreCase(subType) || "REDEMPTION".equalsIgnoreCase(subType))){
                    caType = "AMORTIZACION";
                }
            }
        }
        return caType;
    }


    public String loadEquityMulticcy(Trade trade) {
        if(trade.getProduct() instanceof Equity){
            String equityCcy = trade.getTradeCurrency();
            String tradeSettleCcy = trade.getSettleCurrency();
            return equityCcy.equalsIgnoreCase(tradeSettleCcy) ? "N" : "Y";
        }
        return "N";
    }


    public Double loadEquityMulticcyAmount2(Trade trade, Equity equity) {
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

    public String loadEquityMulticcyCurrency2(Trade trade) {
        return trade.getTradeCurrency();
    }


    public Double loadEquityMulticcyAmount3(Trade trade, Equity equity) {
       return equity.calcSettlementAmount(trade);
    }


    public String loadEquityMulticcyCurrency3(Trade trade) {
        return trade.getSettleCurrency();
    }


}