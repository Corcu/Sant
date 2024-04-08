package calypsox.repoccp;

import calypsox.repoccp.model.ReconCCP;
import calypsox.repoccp.model.lch.*;
import calypsox.repoccp.reader.ReconCCPReader;
import calypsox.repoccp.reader.XmlStaxReader;
import calypsox.tk.util.ScheduledTaskRECONCCP;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.FilterSet;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.LegalEntityTolerance;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.*;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static calypsox.repoccp.ReconCCPConstants.*;

public class ReconCCPUtil {

    private static final String SELECT_TASK_BY_TRADE_ID = "event_type =? AND trade_id  = ? AND task_status NOT IN (1, 2)";
    private static final String SELECT_TASK_BY_REF = "event_type =? AND  trade_id  = 0 AND task_status  NOT IN (1,2) AND int_reference=?";
    private static final String SELECT_GET_TASKS_BY_TRADE_ID = "(event_type =? OR event_type =?) AND trade_id  = ? AND task_status NOT IN (1, 2)";
    private static final String SELECT_GET_TASKS_BY_REF = "event_type =? AND  trade_id  = 0 AND int_reference=? AND task_status NOT IN (1, 2)";

    private static final boolean isSecPartialCash = DefaultsBase.getBooleanProperty("PARTIAL_CASH_FOR_SECURITY_MATCHING", false);
    private static final boolean isSecWriteOff = DefaultsBase.getBooleanProperty("WRITE_OFF_FOR_SECURITY_MATCHING", false);

    public static List<Trade> loadAndFilterTrades(DSConnection ds, JDatetime jdt, TimeZone tz, String tradeFilter, String filterSet) {
        TradeArray trades = new TradeArray();
        JDate valDate = JDate.valueOf(jdt, tz);

        DSConnection roDs = null;
        try {
            roDs = ds.getReadOnlyConnection();
        } catch (ConnectException exc) {
            Log.error("ReconCCPUtil", exc.getCause());
        }
        if (roDs == null) {
            Log.error("ReconCCPUtil", "Could not get a READONLY DS connection");
            return trades.toList();
        } else {
            if (tradeFilter != null) {
                try {
                    TradeFilter tf = BOCache.getTradeFilter(roDs, tradeFilter);
                    if (tf == null) {
                        throw new Exception("Could not load Trade Filter: " + tradeFilter);
                    }

                    trades = ScheduledTask.getTrades(roDs, tf, jdt);
                } catch (Exception e) {
                    Log.error("ReconCCPUtil", e.getCause());
                }

                if (filterSet != null) {
                    TradeArray v = new TradeArray();
                    try {
                        FilterSet fs = roDs.getRemoteMarketData().getFilterSet(filterSet);

                        for (int i = 0; i < trades.size(); ++i) {
                            Trade trade = trades.elementAt(i);
                            if (fs.accept(trade, valDate)) {
                                v.add(trade);
                            }
                        }

                        trades = v;
                    } catch (Exception var30) {
                        Log.error("ReconCCPUtil", var30);
                    }
                }
            } else if (filterSet != null) {
                try {
                    FilterSet fs = roDs.getRemoteMarketData().getFilterSet(filterSet);
                    trades = ScheduledTask.getTrades(roDs, fs, valDate);
                } catch (Exception exc) {
                    Log.error("ReconCCPUtil", exc.getCause());
                }
            }
        }

        return trades.toList();
    }

    /**
     * @return List of mapped trades from file, only TRADE_DATE=PROCESS_DATE trades are retrieved
     */
    public static List<ReconCCP> readAndParseFile(String fileName, String filePath, String orderBy) throws FileNotFoundException {

        File[] files = new File(filePath).listFiles((FileFilter) new WildcardFileFilter(fileName));

        if (!Util.isEmpty(files)) {
            if (files.length > 1) {
                if (Util.isEmpty(orderBy)) {
                    orderBy = ORDER_BY_NAME;
                }
                Log.warn("ReconCCPUtil", "Multiple files match REGEX " + orderBy);

                if (ORDER_BY_DATE.equalsIgnoreCase(orderBy)) {
                    Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                } else {
                    Arrays.sort(files);
                }
            }

            String absFilePath = files[files.length - 1].getAbsolutePath();
            Log.warn("ReconCCPUtil", "Selected file: " + absFilePath);
            //By now it's enough by directly launching this.
            //Will be good to add a factory to instantiate the needed reader for given input
            ReconCCPReader ccpReader;

            try {
                ccpReader = XmlStaxReader.chooseReader(absFilePath);
                return ccpReader != null ? ccpReader.read(absFilePath) : null;
            } catch (XMLStreamException | FileNotFoundException exc) {
                Log.error("ReconCCPUtil", exc.getCause());
            }
        }
        return null;
    }


    public static BOTransfer splitTransfer(DSConnection dsCon, BOTransfer bo, LCHObligations obligation) throws CloneNotSupportedException {
        double nominalAmount = obligation.getNominalInstructed();
        double otherAmount = obligation.getCashAmountInstructed();

        BigDecimal bigNominal = BigDecimal.valueOf(nominalAmount);

        BOTransfer boSplit = (BOTransfer) bo.clone();
        Product p = BOCache.getExchangedTradedProduct(dsCon, boSplit.getProductId());
        BigDecimal faceValue = BigDecimal.ONE;
        if (p instanceof Bond) {
            faceValue = BigDecimal.valueOf(((Bond) p).getFaceValue());
        } else {
            if (p instanceof Repo) {
                if (p.getUnderlyingProduct() instanceof Bond) {
                    faceValue = BigDecimal.valueOf(((Bond) p.getUnderlyingProduct()).getFaceValue());
                }
            }
        }

        BigDecimal settlementAmount = bigNominal.divide(faceValue);

        if (boSplit.getParentLongId() == 0) {
            boSplit.setParentLongId(boSplit.getLongId());
        }

        //INC50
        if (!Util.isEmpty(bo.getTransferType())) {
            double settlementAmountD = Math.abs(settlementAmount.doubleValue());
            otherAmount = Math.abs(otherAmount);
            nominalAmount = Math.abs(nominalAmount);
            if ("PRINCIPAL".equals(bo.getTransferType()) && !Util.isEmpty(obligation.getCashReceiver())) {
                if ("BS".equals(obligation.getCashReceiver())) {
                    boSplit.setSettlementAmount(settlementAmountD);
                    boSplit.setRealSettlementAmount(settlementAmountD);
                    boSplit.setNominalAmount(nominalAmount);

                    boSplit.setRealCashAmount(otherAmount * -1);
                    boSplit.setOtherAmount(otherAmount * -1);

                    boSplit.setPayReceive("RECEIVE");
                } else {
                    boSplit.setSettlementAmount(settlementAmountD * -1);
                    boSplit.setRealSettlementAmount(settlementAmountD * -1);
                    boSplit.setNominalAmount(nominalAmount * -1);

                    boSplit.setRealCashAmount(otherAmount);
                    boSplit.setOtherAmount(otherAmount);
                    boSplit.setPayReceive("PAY");
                }
            } else if ("SECURITY".equals(bo.getTransferType()) && !Util.isEmpty(obligation.getBondsReceiver())) {
                if ("BS".equals(obligation.getBondsReceiver())) {
                    boSplit.setSettlementAmount(settlementAmountD);
                    boSplit.setRealSettlementAmount(settlementAmountD);
                    boSplit.setNominalAmount(nominalAmount);

                    boSplit.setRealCashAmount(otherAmount * -1);
                    boSplit.setOtherAmount(otherAmount * -1);
                    boSplit.setPayReceive("RECEIVE");
                } else {
                    boSplit.setSettlementAmount(settlementAmountD * -1);
                    boSplit.setRealSettlementAmount(settlementAmountD * -1);
                    boSplit.setNominalAmount(nominalAmount * -1);

                    boSplit.setRealCashAmount(otherAmount);
                    boSplit.setOtherAmount(otherAmount);
                    boSplit.setPayReceive("PAY");
                }
            }
        }
        return boSplit;
    }


    public static TransferArray getSettlementTransfers(DSConnection dsCon, long tradeId) {
        TransferArray tfArray = new TransferArray();
        try {
            TransferArray transfers = DSConnection.getDefault().getRemoteBackOffice().getBOTransfers(tradeId);
            TransferArray settlements = DSConnection.getDefault().getRemoteBackOffice().getNettedTradeTransfers(tradeId);
            for (BOTransfer transfer : transfers) {
                if (!transfer.isPayment()) {
                    if (transfer.getNettedTransferLongId() != 0L) {
                        HashMap keys = BOCache.getNettingConfig(dsCon, transfer.getNettingType());
                        if (keys.get("TradeId") == null && tfArray.stream().noneMatch(t -> t.getLongId() == transfer.getLongId())) {
                            tfArray.add(dsCon.getRemoteBackOffice().getBOTransfer(transfer.getNettedTransferLongId()));
                        }
                    }
                } else {
                    tfArray.add(transfer);
                }
            }
            tfArray.add(settlements.toVector());
        } catch (CalypsoServiceException e) {
            Log.error("ReconCCPUtil", e.getCause());
        }
        return tfArray;
    }

    public static void saveTransferAttribute(DSConnection dsCon, long transferId, String attributeName, String attributeValue) {
        try {
            dsCon.getRemoteBackOffice().saveTransferAttribute(transferId, attributeName, attributeValue);
        } catch (CalypsoServiceException e) {
            Log.error("ScheduledTaskRECONCCP_NETTING_MTINEXTDAY", e.getCause());
        }
    }


    public static Map<String, LCHTrade> separateTrades(List<ReconCCP> fileObjects) {
        Map<String, LCHTrade> trades = new HashMap<>();
        for (ReconCCP object : fileObjects) {
            if (object instanceof LCHTrade) {
                LCHTrade lchTrade = (LCHTrade) object;
                trades.put(lchTrade.getIdentifier().getIsin() + "|" + lchTrade.getBuyerSellerReference(), lchTrade);
            }
        }
        return trades;
    }

    public static Map<String, List<LCHObligations>> separateObligations(List<ReconCCP> fileObjects) {
        Map<String, List<LCHObligations>> obligationsMap = new HashMap<>();
        for (ReconCCP object : fileObjects) {
            if (object instanceof LCHObligations) {
                LCHObligations lchObligations = (LCHObligations) object;
                String isin = lchObligations.getIdentifier().getIsin();
                if (obligationsMap.containsKey(isin)) {
                    List<LCHObligations> obligations = obligationsMap.get(isin);
                    obligations.add(lchObligations);
                    obligationsMap.put(isin, obligations);
                } else {
                    List<LCHObligations> obligations = new ArrayList<>();
                    obligations.add(lchObligations);
                    obligationsMap.put(isin, obligations);
                }

            }
        }
        return obligationsMap;
    }

    public static List<LCHSettlement> extractSettlements(List<ReconCCP> fileObjects) {
        List<LCHSettlement> settlements = new ArrayList<>();
        for (ReconCCP object : fileObjects) {
            if (object instanceof LCHSettlement) {
                settlements.add((LCHSettlement) object);
            }
        }
        return settlements;
    }

    public static TransferArray getTransfersByAttribute(DSConnection dsCon, String attributeName, String attributeValue, JDate valDate) {
        TransferArray result = new TransferArray();
        String where = "xfer_attributes.transfer_id=bo_transfer.transfer_id AND attr_name = ? AND attr_value = ? AND " +
                "bo_transfer.transfer_status in ('VERIFIED', 'FAILED') AND bo_transfer.value_date <= ?";
        Vector<CalypsoBindVariable> bindVariables = new Vector<>();
        bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, attributeName));
        bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, attributeValue));
        bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, valDate));
        try {
            result = dsCon.getRemoteBO().getTransfers("xfer_attributes", where, bindVariables);
        } catch (CalypsoServiceException e) {
            Log.error("ReconCCPUtil", e.getCause());
        }
        return result;
    }

    public static TransferArray getAllTransfersWithSettlementReferenceInstructed(DSConnection dsCon, JDate valDate) {
        String where = "xfer_attributes.transfer_id=bo_transfer.transfer_id AND (attr_name = 'SettlementReferenceInstructed' " +
                "AND attr_value IS NOT NULL OR attr_name = 'SettlementReferenceInstructed2' AND attr_value IS NOT NULL) AND " +
                "bo_transfer.transfer_status in ('VERIFIED', 'FAILED') AND bo_transfer.value_date <= ?";
        Vector<CalypsoBindVariable> bindVariables = new Vector<>();
        bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, valDate));
        try {
            return Optional.ofNullable(dsCon.getRemoteBO().getTransfers("xfer_attributes", where, bindVariables)).
                    map(t -> new TransferArray(new HashSet<>(t))).orElse(new TransferArray());
        } catch (CalypsoServiceException e) {
            Log.error("ReconCCPUtil", e.getCause());
        }
        return new TransferArray();
    }

    /**
     * Apply tolerance on two values using ReconCCPConstants.TOLERANCE
     *
     * @param formatter the number formatter
     * @param value1    the value 1
     * @param value2    the value 2
     * @return true if the values are within tolerance
     */
    public static boolean applyTolerance(NumberFormat formatter, String value1, String value2) {
        return applyTolerance(formatter, value1, value2, ReconCCPConstants.TOLERANCE);
    }

    public static boolean applyTolerance(NumberFormat formatter, String value1, String value2, double tolerance) {
        if (!Util.isEmpty(value1) && !Util.isEmpty(value2) && formatter != null) {
            try {
                Number number1 = formatter.parse(value1);
                Number number2 = formatter.parse(value2);

                if (number1 != null && number2 != null) {
                    BigDecimal decimal1 = BigDecimal.valueOf(number1.doubleValue());
                    BigDecimal decimal2 = BigDecimal.valueOf(number2.doubleValue());
                    //  BigDecimal tolerance = BigDecimal.valueOf(ReconCCPConstants.TOLERANCE);

                    BigDecimal result = decimal1.subtract(decimal2).abs();

                    BigDecimal compare = result.min(BigDecimal.valueOf(tolerance));
                    return compare.equals(result);
                }

            } catch (ParseException e) {
                Log.error("ReconCCPUtil", e.getCause());
            }
        }
        return false;
    }

    public static LegalEntityTolerance getTolerance(Trade trade, String type) {
        return getTolerance(trade.getCounterParty().getId(), trade.getTradeCurrency(), type);
    }

    public static LegalEntityTolerance getTolerance(int leId, String ccy, String type) {
        List<LegalEntityTolerance> tolerances = BOCache.getLegalEntityTolerances(DSConnection.getDefault(), leId);
        if (Util.isEmpty(tolerances))
            return null;
        Optional<LegalEntityTolerance> exactMatch = tolerances.stream().filter(t -> type.equals(t.getToleranceType())
                && ccy.equals(t.getCurrency())).findFirst();
        if (exactMatch.isPresent())
            return exactMatch.get();

        Optional<LegalEntityTolerance> anyCcy = tolerances.stream().filter(t -> type.equals(t.getToleranceType())
                && "ANY".equals(t.getCurrency())).findFirst();

        return anyCcy.orElseGet(() -> tolerances.stream().filter(t -> type.equals(t.getToleranceType())).findFirst().orElse(null));

    }

    public static double getToleranceAmount(int leId, String ccy, String type, PricingEnv pe, JDate valDate) {
        double toleranceAmt = ReconCCPConstants.TOLERANCE;
        String toleranceAmtCC = "EUR";
        LegalEntityTolerance tolerance = getTolerance(leId, ccy, type);
        if (tolerance != null) {
            toleranceAmt = tolerance.getAmount();
            toleranceAmtCC = tolerance.getCurrency();
        }

        try {
            toleranceAmt = CurrencyUtil.convertAmount(pe, toleranceAmt, toleranceAmtCC, ccy, valDate, pe.getQuoteSet());
        } catch (MarketDataException e) {
            Log.error(ReconCCPUtil.class, String.format("Cannot cover tolerance to from %s to %s, %s", toleranceAmtCC, ccy, e));
        }
        return toleranceAmt;
    }

    public static List<BOTransfer> filterTransfersByAttribute(DSConnection dsConnection, List<BOTransfer> transfers, String attrName, String attrValue) {
        List<BOTransfer> accept = new ArrayList<BOTransfer>();
        if (dsConnection != null && !Util.isEmpty(transfers) && !Util.isEmpty(attrName) && !Util.isEmpty(attrValue)) {
            TransferArray lastVersion = getLastTransfersVersion(dsConnection, transfers);
            if (lastVersion != null) {
                for (BOTransfer transfer : lastVersion) {
                    if (!Util.isEmpty(transfer.getAttribute(attrName)) && attrValue.equals(transfer.getAttribute(attrName))) {
                        accept.add(transfer);
                    }
                }
            }
        }
        return accept;
    }

    public static boolean moveTransfersNextStatus(DSConnection dsCon, List<BOTransfer> transfers, String bondAction,
                                                  String repoAction, String allAction) {
        if (dsCon != null && !Util.isEmpty(transfers) && !Util.isEmpty(bondAction) && !Util.isEmpty(repoAction)
                && !Util.isEmpty(allAction)) {
            TransferArray newTransfers = getLastTransfersVersion(dsCon, transfers);
            if (newTransfers != null) {
                String productFamily = null;
                for (BOTransfer transfer : newTransfers) {
                    productFamily = transfer.getProductType();
                    if (!Util.isEmpty(productFamily)) {
                        if (Product.BOND.equals(productFamily)) {
                            transfer.setAction(Action.valueOf(bondAction));
                            saveTransfer(dsCon, transfer);
                        } else if (Product.REPO.equals(productFamily)) {
                            transfer.setAction(Action.valueOf(repoAction));
                            saveTransfer(dsCon, transfer);
                        } else if ("NONE".equals(productFamily)) {
                            //If NONE use WF all
                            transfer.setAction(Action.valueOf(allAction));
                            saveTransfer(dsCon, transfer);
                        } else {
                            Log.info("ReconCCPUtil", "The transfer id " + transfer.getLongId() +
                                    " cannot be saved, its product family is " + productFamily);
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static TransferArray getLastTransfersVersion(DSConnection dsCon, List<BOTransfer> transfers) {
        if (dsCon != null && !Util.isEmpty(transfers)) {
            //Delete duplicates
            Set<BOTransfer> transfersND = new HashSet<>(transfers);
            long[] ids = new long[transfersND.size()];
            int i = 0;
            for (BOTransfer t : transfersND) {
                ids[i] = t.getLongId();
                i++;
            }
            TransferArray newTransfers = null;
            try {
                return dsCon.getRemoteBackOffice().getTransfers(ids);
            } catch (CalypsoServiceException e) {
                Log.error("ReconCCPUtil", e.getCause());
            }
        }
        return null;
    }

    private static boolean saveTransfer(DSConnection dsCon, BOTransfer transfer) {
        if (transfer != null && dsCon != null) {
            try {
                if (!Util.isEmpty(transfer.getAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED)) && "SECURITY".equals(transfer.getTransferType()) && transfer.getAction().toString().contains(Action.SETTLE.toString()) && isSecPartialCash) {
                    transfer.setRealCashAmount(Util.stringToNumber(transfer.getAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED), Locale.UK));
                    transfer.setAttribute("ExpectedStatus", Status.SETTLED);
                    if (isSecWriteOff) {
                        transfer.setAttribute("WriteOff", "true");
                        if (transfer.getNettedTransfer())
                            transfer.setAttribute("SPLITREASON", "SecurityNetting");
                    }
                    dsCon.getRemoteBO().partialSettleTransfer(transfer);
                } else {
                    dsCon.getRemoteBO().save(transfer, 0L, "");
                }
                return true;
            } catch (CalypsoServiceException e) {
                Log.warn("ReconCCPUtil", e.getCause());
            }
        }
        return false;
    }

    private static TransferArray getSplitTransferFromDB(DSConnection dsCon, List<BOTransfer> splitTransfers) {
        List<BOTransfer> dbSplitTransfers = new ArrayList<BOTransfer>();
        if (!Util.isEmpty(splitTransfers) && dsCon != null) {
            String attr;
            Set<String> attrValues = new HashSet<>();
            Set<Long> parentIds = new HashSet<>();
            for (BOTransfer bo : splitTransfers) {
                attr = bo.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST);
                if (!Util.isEmpty(attr)) {
                    attrValues.add(attr);
                }
                parentIds.add(bo.getParentLongId());
            }
            String where = "xfer_attributes.transfer_id=bo_transfer.transfer_id AND attr_name = " +
                    Util.string2SQLString(XFER_ATTR_SETTLEMENT_REF_INST) + " AND attr_value IN " + Util.collectionToSQLString(attrValues) +
                    " AND bo_transfer.start_time_limit in " + Util.collectionToSQLString(parentIds);
            try {
                return dsCon.getRemoteBO().getTransfers("xfer_attributes", where, new Vector<>());
            } catch (CalypsoServiceException e) {
                Log.error("ReconCCPUtil", e.getCause());
            }
        }
        return new TransferArray();
    }

    public static List<BOTransfer> filterTransfersByStatus(DSConnection dsConnection, List<BOTransfer> transfers,
                                                           Status status) {
        if (dsConnection != null && !Util.isEmpty(transfers) && status != null) {
            TransferArray lastVersion = getLastTransfersVersion(dsConnection, transfers);
            if (lastVersion != null) {
                return lastVersion.stream()
                        .filter(bo -> status.equals(bo.getStatus()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    public static TransferArray splitTransfer(DSConnection dsCon, BOTransfer bo, List<LCHObligations> obligations) {
        if (dsCon != null && bo != null & !Util.isEmpty(obligations)) {
            bo.setAction(Action.SPLIT);
            bo.setAttribute(TRADE_KEYWORD_RECON, RECON_OK);
            TransferArray transferSplits = new TransferArray();
            try {
                for (LCHObligations obligation : obligations) {
                    BOTransfer boSplit = splitTransfer(dsCon, bo, obligation);
                    double theirCashAmount = "LCH".equals(obligation.getCashReceiver()) ? -obligation.getCashAmountInstructed() : obligation.getCashAmountInstructed();
                    double ourCashAmount = "SECURITY".equals(boSplit.getTransferType())
                            ? "PAY".equals(boSplit.getPayReceive()) ? Math.abs(boSplit.getRealCashAmount()) : -Math.abs(boSplit.getRealCashAmount())
                            : "PAY".equals(boSplit.getPayReceive()) ? -Math.abs(boSplit.getRealSettlementAmount()) : Math.abs(boSplit.getRealSettlementAmount());
                    if (Math.abs(theirCashAmount - ourCashAmount) > Math.pow(10, -CurrencyUtil.getCcyDecimals(boSplit.getSettlementCurrency(), 2)))
                        boSplit.setAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED, Util.numberToString(theirCashAmount, Locale.UK, false));
                    boSplit.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, obligation.getSettlementReferenceInstructed());
                    boSplit.setAttribute(TRADE_KEYWORD_RECON, RECON_OK);
                    transferSplits.add(boSplit);
                }
                dsCon.getRemoteBackOffice().splitTransfers(bo, transferSplits);
                return getSplitTransferFromDB(dsCon, transferSplits.toVector());
            } catch (CalypsoServiceException | CloneNotSupportedException e) {
                Log.error("ReconCCPUtil", e.getCause());
            }
        }
        return new TransferArray();
    }

    public static Map<String, List<LCHNetPositions>> groupNetPositionsByISIN(List<ReconCCP> fileObjects) {
        if (!Util.isEmpty(fileObjects)) {
            List<LCHNetPositions> netPositionsObligationSet = fileObjects.stream().filter(LCHNetPositions.class::isInstance).
                    map(LCHNetPositions.class::cast).filter(LCHNetPositions::isObligationSet).collect(Collectors.toList());
            List<LCHObligations> obligations = fileObjects.stream().filter(LCHObligations.class::isInstance).
                    map(LCHObligations.class::cast).collect(Collectors.toList());
            if (!Util.isEmpty(netPositionsObligationSet) && !Util.isEmpty(obligations)) {
                Map<String, List<LCHNetPositions>> groupedNetPositions = netPositionsObligationSet.stream().
                        collect(Collectors.groupingBy(n -> Optional.ofNullable(n.getObligationSetIdentifier()).
                                map(LCHSetIdentifier::getID).orElse("NO_ID")));
                Map<String, List<LCHObligations>> groupedObligations = obligations.stream().
                        collect(Collectors.groupingBy(n -> Optional.ofNullable(n.getIdentifier()).
                                map(LCHSetIdentifier::getID).orElse("NO_ID")));

                List<LCHNetPositions> processedNetPositions = new ArrayList<>();
                groupedNetPositions.forEach((k, v) ->
                        processedNetPositions.addAll(parseObligationSet(v, groupedObligations.get(k))));

                return processedNetPositions.stream().
                        collect(Collectors.groupingBy(n -> Optional.ofNullable(n.getObligationSetIdentifier()).
                                map(LCHSetIdentifier::getIsin).orElse("NO_ISIN")));

            }
        }
        return new HashMap<>();
    }

    private static List<LCHNetPositions> parseObligationSet(List<LCHNetPositions> netPositions,
                                                            List<LCHObligations> obligations) {
        if (!Util.isEmpty(netPositions) && !Util.isEmpty(obligations)) {
            //Caso 1: 1-N
            if (netPositions.size() == 1) {
                netPositions.get(0).setObligations(obligations);
                return netPositions;
            }

            List<LCHNetPositions> processedObligationInputs = new ArrayList<>();
            List<LCHObligations> processedObligationOutputs = new ArrayList<>();
            //Caso 2: N - N
            for (LCHNetPositions n : netPositions) {
                for (LCHObligations o : obligations) {
                    if (n.getNominal() != 0 && o.getNominalInstructed() != 0
                            && n.getNominal() == o.getNominalInstructed()) {
                        n.addObligation(o);
                        processedObligationInputs.add(n);
                        processedObligationOutputs.add(o);
                        break;
                    } else if (n.getNominal() == 0 && o.getNominalInstructed() == 0
                            && n.getCashAmount() == o.getCashAmountInstructed()) {
                        n.addObligation(o);
                        processedObligationInputs.add(n);
                        processedObligationOutputs.add(o);
                        break;
                    }
                }
                obligations.removeAll(processedObligationOutputs);
            }
            netPositions.removeAll(processedObligationInputs);

            if (!Util.isEmpty(netPositions) && !Util.isEmpty(obligations)) {
                //Con split, busca sumatorio
                for (LCHNetPositions n : netPositions) {
                    if (!Util.isEmpty(obligations)) {
                        List<LCHObligations> result = new ArrayList<>();
                        try {
                            findSplitObligations(obligations, 0, 0, n.getNominal(),
                                    new int[obligations.size()], result);
                        } catch (Exception e) {
                            Log.error("ReconCCP", "No solution found for netPosition " + n);
                        }
                        if (!Util.isEmpty(result)) {
                            for (LCHObligations o : result) {
                                n.addObligation(o);
                                processedObligationOutputs.add(o);
                            }
                            obligations.removeAll(processedObligationOutputs);
                            processedObligationInputs.add(n);
                        } else {
                            Log.error("ReconCCPUtil", "No obligations found for the NetPosition: " + n);
                        }
                    } else {
                        Log.error("ReconCCPUtil", "There are no further obligations for netting positions");
                    }
                }
                netPositions.removeAll(processedObligationInputs);
                if (!Util.isEmpty(netPositions)) {
                    Log.error("ReconCCP", "There are NetPositions without processing: " + netPositions);
                    //Se anaden las netPositions sin procesar (sin obligations)
                    processedObligationInputs.addAll(netPositions);
                }

                if (!Util.isEmpty(obligations)) {
                    Log.error("ReconCCP", "There are Obligations without processing: " + obligations);
                }


            }
            return processedObligationInputs;
        }
        return new ArrayList<>();
    }

    private static void findSplitObligations(List<LCHObligations> obligations, double currSum,
                                             int index, double sum,
                                             int[] solution, List<LCHObligations> solutionO) {
        if (currSum == sum) {
            if (Util.isEmpty(solutionO)) {
                for (int i = 0; i < solution.length; i++) {
                    if (solution[i] == 1) {
                        if (obligations.get(i).getNominalInstructed() != 0.0D) {
                            solutionO.add(obligations.get(i));
                        }
                    }
                }
            }
            return;
        } else if (index == obligations.size()) {
            return;
        } else {
            solution[index] = 1;// select the element
            currSum += obligations.get(index).getNominalInstructed();
            findSplitObligations(obligations, currSum, index + 1, sum, solution, solutionO);
            currSum -= obligations.get(index).getNominalInstructed();
            solution[index] = 0;// do not select the element
            findSplitObligations(obligations, currSum, index + 1, sum, solution, solutionO);
        }
        return;
    }

    public static List<LCHTrade> groupByTrade(Map<String, List<LCHNetPositions>> netPositionsObligationSet,
                                              List<ReconCCP> fileObjects) {
        if (netPositionsObligationSet != null && fileObjects != null) {
            List<LCHTrade> trades = fileObjects.stream().filter(LCHTrade.class::isInstance).
                    map(LCHTrade.class::cast).collect(Collectors.toList());
            List<LCHNetPositions> netPositionNettingSet = fileObjects.stream().filter(LCHNetPositions.class::isInstance).
                    map(LCHNetPositions.class::cast).filter(LCHNetPositions::isNettingSet).collect(Collectors.toList());


            for (LCHTrade t : trades) {
                for (LCHNetPositions n : netPositionNettingSet) {
                    if (t.getIdentifier() != null && n.getNettingSetIdentifier() != null &&
                            t.getIdentifier().equals(n.getNettingSetIdentifier())) {
                        t.setNetPositionNettingSet(n);
                        break;
                    }
                }
            }

            LCHNetPositions selectedNetOS;
            List<LCHNetPositions> processedNetPositions = new ArrayList<>();
            for (LCHTrade t : trades) {
                selectedNetOS = null;
                List<LCHNetPositions> netPositionsOS = netPositionsObligationSet.get(Optional.ofNullable(t.getIdentifier()).
                        map(LCHSetIdentifier::getIsin).orElse("NO_ID"));
                if (!Util.isEmpty(netPositionsOS)) {
                    if (netPositionsOS.size() == 1) {
                        //Asignacion directa
                        t.setNetPositionObligationSet(netPositionsOS.get(0));
                    } else if (t.getNetPositionNettingSet() != null) {
                        LCHNetPositions tradeNetPositionNS = t.getNetPositionNettingSet();
                        for (LCHNetPositions nOS : netPositionsOS) {
                            if (isEqual(tradeNetPositionNS, nOS)) {
                                if (!processedNetPositions.contains(nOS)) {
                                    t.setNetPositionObligationSet(nOS);
                                    processedNetPositions.add(nOS);
                                    break;
                                } else {
                                    selectedNetOS = nOS;
                                }
                            }
                        }
                        if (t.getNetPositionObligationSet() == null && selectedNetOS != null) {
                            //NetPosition compartida MTINEXTDAY
                            t.setNetPositionObligationSet(selectedNetOS);
                        }
                    }
                }
            }
            return trades;

        }
        return new ArrayList<>();
    }


    private static boolean isEqual(LCHNetPositions o1, LCHNetPositions o2) {
        if (o1 != null && o2 != null) {
            return Double.compare(o1.getNominal(), o2.getNominal()) == 0 && Double.compare(o1.getCashAmount(),
                    o2.getCashAmount()) == 0 &&
                    Objects.equals(o1.getNominalCurrency(), o2.getNominalCurrency()) &&
                    Objects.equals(o1.getCashCurrency(), o2.getCashCurrency()) &&
                    Objects.equals(o1.getBondsReceiver(), o2.getBondsReceiver()) &&
                    Objects.equals(o1.getBondsDeliverer(), o2.getBondsDeliverer()) &&
                    Objects.equals(o1.getCashReceiver(), o2.getCashReceiver()) &&
                    Objects.equals(o1.getCashDeliverer(), o2.getCashDeliverer()) &&
                    Objects.equals(o1.getNetPositionType(), o2.getNetPositionType());
        }
        return false;
    }

    public static boolean isBondCalypso(Trade trade) {
        return trade != null && trade.getProduct() != null && trade.getProduct() instanceof Bond;
    }

    public static boolean isRepoCalypso(Trade trade) {
        return trade != null && trade.getProduct() != null && trade.getProduct() instanceof Repo;
    }

    public static List<ReconCCP> filterByFileTradeDateAndSTValDate(List<ReconCCP> clearingTrades, JDatetime stValueDatetime, String pattern) {
        if (!Util.isEmpty(clearingTrades) && stValueDatetime != null && !Util.isEmpty(pattern)) {
            JDate stValueDate = JDate.valueOf(stValueDatetime);
            return clearingTrades.stream().filter(r -> filterRecoCCPObjectByTradeDate(r, stValueDate, pattern)).
                    collect(Collectors.toList());
        }
        return clearingTrades;
    }

    private static boolean filterRecoCCPObjectByTradeDate(ReconCCP obj, JDate valDate, String pattern) {
        if (obj instanceof LCHTrade) {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern);
            try {
                Date date = formatter.parse(((LCHTrade) obj).getTradeDate());
                return date != null ? JDate.valueOf(date).equals(valDate) : false;
            } catch (ParseException e) {
                Log.error("ReconCCPUtil", e);
            }
        }
        return false;
    }

    public static boolean matchReferenceByProductType() {
        Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(),
                "ReconCCPMatchRefereceByProductType");
        if (!Util.isEmpty(domainValues)) {
            return domainValues.get(0).equalsIgnoreCase("true");
        }
        return true;
    }

    public static List<ReconCCP> filterFileTradesReconOK(DSConnection dsCon, List<ReconCCP> clearingTrades, String tradeFilter, String filterSet, JDate valDate,
                                                         JDatetime valDateTime) {
        if (dsCon != null && !Util.isEmpty(clearingTrades)) {
            List<String> buyerSellerRefs = clearingTrades.stream().filter(LCHTrade.class::isInstance).map(LCHTrade.class::cast).
                    map(LCHTrade::getBuyerSellerReference).
                    filter(buyerSellerReference -> !Util.isEmpty(buyerSellerReference)).collect(Collectors.toList());
            if (!Util.isEmpty(buyerSellerRefs)) {
                TradeArray trades = null;
                try {

                    // this query will hit
                    StringBuilder where = (new StringBuilder(" TRADE_KEYWORD.TRADE_ID = TRADE.TRADE_ID  AND TRADE.TRADE_STATUS != 'CANCELED' AND trade_keyword.keyword_name = "))
                            .append(Util.string2SQLString(TRADE_KWD_BUYER_SELLER_REF)).append(" AND ");
                    int chunkSize = Util.getMaxItemsInQuery();
                    if (buyerSellerRefs.size() <= chunkSize) {
                        where.append("trade_keyword.keyword_value IN ").append(Util.collectionToSQLString(buyerSellerRefs));
                    } else {

                        for (int x = 0; x < buyerSellerRefs.size(); x += chunkSize) {
                            List<String> chunk = buyerSellerRefs.subList(x, Math.min(x + chunkSize, buyerSellerRefs.size()));
                            if (x == 0)
                                where.append("(trade_keyword.keyword_value IN ").append(Util.collectionToSQLString(chunk));
                            else
                                where.append(" OR trade_keyword.keyword_value IN ").append(Util.collectionToSQLString(chunk));
                        }
                        where.append(")");
                    }

                    List<CalypsoBindVariable> bindVars = new ArrayList<>();
                    TradeFilter tf = Util.isEmpty(tradeFilter) ? null : BOCache.getTradeFilter(dsCon, tradeFilter);
                    String from = "trade_keyword";
                    if (tf != null) {
                        TradeFilter cloneTf = (TradeFilter) tf.clone();
                        cloneTf.setValDate(valDateTime);

                        cloneTf.removeCriterion("KEYWORD_CRITERION"); //remove Recon <> OK

                        String filterFrom = dsCon.getRemoteReferenceData().generateFromClause(cloneTf);
                        Object[] filterWhereAndVars = dsCon.getRemoteReferenceData().generateWhereClause(cloneTf, bindVars);
                        String filterWhere = (String) filterWhereAndVars[0];
                        bindVars = (List<CalypsoBindVariable>) filterWhereAndVars[1];

                        from = mergeFrom(from, filterFrom);

                        if (!Util.isEmpty(filterWhere) && filterWhere.trim().length() > 0)
                            where.append(" AND ").append(filterWhere);
                    }
                    FilterSet fs = Util.isEmpty(filterSet) ? null : BOCache.getFilterSet(dsCon, filterSet);

                    if (fs != null) {
                        String fsWhere = fs.getSQLWhere(valDate, bindVars);
                        if (!Util.isEmpty(fsWhere) && fsWhere.trim().length() > 0) {
                            where.append(" AND ").append(fsWhere);
                        }
                        String fsFrom = fs.getSQLFrom(valDate);
                        from = mergeFrom(from, fsFrom);

                    }

                    trades = dsCon.getRemoteTrade().getTrades(from, where.toString(), "", bindVars);
                } catch (Exception e) {
                    Log.error("ReconCCPUtil", e);
                }


                if (!Util.isEmpty(trades)) {
                    Set<String> filterTradeRefs = trades.asList().stream().
                            filter(t -> !Util.isEmpty(t.getKeywordValue(TRADE_KEYWORD_RECON)) &&
                                    RECON_OK.equals(t.getKeywordValue(TRADE_KEYWORD_RECON))).
                            map(t -> t.getKeywordValue(TRADE_KWD_BUYER_SELLER_REF)).collect(Collectors.toSet());
                    if (!Util.isEmpty(filterTradeRefs)) {
                        List<ReconCCP> result = new ArrayList<>();
                        for (ReconCCP t : clearingTrades) {
                            if (t instanceof LCHTrade) {
                                String buyerSeller = ((LCHTrade) t).getBuyerSellerReference();
                                if (Util.isEmpty(buyerSeller) || !filterTradeRefs.contains(buyerSeller)) {
                                    result.add(t);
                                }
                            } else {
                                result.add(t);
                            }
                        }
                        return result;
                    }
                }
            }

        }
        return clearingTrades;
    }

    private static String mergeFrom(String from, String addFrom) {
        if (Util.isEmpty(addFrom))
            return from;
        List<String> fromList = Util.stringToList(from);
        fromList.addAll(Util.stringToList(addFrom));
        return Util.collectionToString(fromList.stream().filter(f -> !Util.isEmpty(f) && f.trim().length() > 0).map(f -> f.trim().toLowerCase()).collect(Collectors.toList()));
    }

    public static boolean isBOTransferMTS(DSConnection dsCon, BOTransfer transfer) {
        if (transfer != null) {
            String electPlatf = transfer.getAttribute(XFER_ATTR_TRADE_SOURCE);
            if (!Util.isEmpty(electPlatf) && electPlatf.contains("MTS")) {
                return true;
            } else if (dsCon != null) {
                TransferArray nettedTransfers = null;
                try {
                    nettedTransfers = dsCon.getRemoteBackOffice().getNettedTransfers(transfer.getLongId());
                } catch (CalypsoServiceException e) {
                    Log.error("ReconCCPUtil", e);
                }
                if (!Util.isEmpty(nettedTransfers)) {
                    long[] tradeIds = nettedTransfers.stream().filter(t -> t.getTradeLongId() != 0
                                    && !t.getStatus().equals(Status.CANCELED)).
                            map(t -> t.getTradeLongId()).mapToLong(i -> i).toArray();
                    if (tradeIds.length > 0) {
                        TradeArray trades = null;
                        try {
                            trades = dsCon.getRemoteTrade().getTrades(tradeIds);
                        } catch (CalypsoServiceException e) {
                            Log.error("ReconCCPUtil", e);
                        }
                        if (!Util.isEmpty(trades)) {
                            List<Trade> selectedTrades = trades.asList().stream().filter(t ->
                                    !Util.isEmpty(t.getKeywordValue(TRADE_KWD_MX_ELECTPLTF))
                                            && t.getKeywordValue(TRADE_KWD_MX_ELECTPLTF).
                                            contains("MTS")).collect(Collectors.toList());
                            return !Util.isEmpty(selectedTrades);
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Filter trades by Settlement Transfers filter
     *
     * @param dsCon          the Data Server connection
     * @param tradesToFilter the list of trade to be filter
     * @param bySRI          true if filter by SRI empty
     * @param bySettleDate   true if filter by Settlement Date equals Value Date
     * @param sdf            Static Data filter
     * @param valDate        the valuation Date
     * @return filter trade list by transfer criteria
     */
    public static List<Trade> filterTradeBySettlementXfers(DSConnection dsCon, List<Trade> tradesToFilter,
                                                           boolean bySRI, boolean bySettleDate, StaticDataFilter sdf, JDate valDate) {
        if (!Util.isEmpty(tradesToFilter) && dsCon != null) {
            List<Trade> filterTrades = new ArrayList<>();
            TransferArray transfers;
            for (Trade t : tradesToFilter) {
                transfers = getSettlementTransfers(dsCon, t.getLongId());
                if (!Util.isEmpty(transfers) && !Util.isEmpty(transfers.stream().filter(bo -> filterXfer(bySRI, bySettleDate, sdf,
                        bo, t, valDate)).collect(Collectors.toList()))) {
                    filterTrades.add(t);
                }
            }
            return filterTrades;
        }
        return tradesToFilter;
    }

    /**
     * Filter transfer by custom criteria
     *
     * @param bySRI        true if filter by empty SRI
     * @param bySettleDate tru if filter by SettleDate equals ValueDate
     * @param sdf          Static Data Filter
     * @param xfer         transfer to be filter
     * @param valDate      the valuation date
     * @param trade        the trade Object
     * @return true if the transfer meets the criteria
     */
    public static boolean filterXfer(boolean bySRI, boolean bySettleDate, StaticDataFilter sdf, BOTransfer xfer, Trade trade, JDate valDate) {
        if (xfer == null) {
            return false;
        }
        if (bySRI && !Util.isEmpty(xfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST))) {
            return false;
        }
        if (bySettleDate && (xfer.getSettleDate() == null || !xfer.getSettleDate().equals(valDate))) {
            return false;
        }
        return sdf == null || sdf.accept(trade, xfer.toTradeTransferRule(), xfer);
    }

    /**
     * Get the list of SDF names
     *
     * @param dsCon the Data Server connection
     * @return the list of SDF names
     */
    public static List<String> getSDFNames(DSConnection dsCon) {
        if (dsCon != null) {
            try {
                Vector<String> sdfNames = dsCon.getRemoteReferenceData().getStaticDataFilterNames();
                sdfNames.add("");
                return Util.sort(sdfNames);
            } catch (Exception e) {
                Log.error("ReconCCPUtil", e);
            }
        }
        return new ArrayList<>();
    }

    public static TaskArray getTasksToClose(String ref, Function<Task, Boolean> predicate, DSConnection dsCon) throws CalypsoServiceException {
        return getTasksToClose(0, ref, predicate, dsCon);
    }


    public static TaskArray getTasksToClose(long tradeId, String ref, Function<Task, Boolean> predicate, DSConnection sdCon) throws CalypsoServiceException {
        TaskArray tasks = getExceptionTasks(tradeId, ref, sdCon);
        if (tasks == null)
            return new TaskArray();


        return tasks.isEmpty() ? tasks : new TaskArray(Arrays.stream(tasks.getTasks()).filter(t -> t != null && (predicate == null || predicate.apply(t)))
                .map(t -> {
                    try {
                        Task clone = (Task) t.clone();
                        clone.setStatus(Task.COMPLETED);
                        return clone;
                    } catch (CloneNotSupportedException e) {
                        Log.error(ReconCCPUtil.class, String.format("Cannot clone task %s.", t), e);
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public static boolean exceptionTaskNotFound(Task task, DSConnection dsCon) throws CalypsoServiceException {

        TaskArray tasks;
        if (task.getTradeLongId() > 0) {
            String sqlWhere = SELECT_TASK_BY_TRADE_ID;
            if ("WARN".equals(task.getAttribute()))
                sqlWhere += "AND ATTRIBUTE='WARN'";
            else if ("ERROR".equals(task.getAttribute()))
                sqlWhere += "AND ATTRIBUTE='ERROR'";

            tasks = dsCon.getRemoteBO().getTasks(sqlWhere, Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, task.getEventType()), new CalypsoBindVariable(CalypsoBindVariable.LONG, task.getTradeLongId())));
        } else {
            tasks = dsCon.getRemoteBO().getTasks(SELECT_TASK_BY_REF, Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, EXCEPTION_MISSING_TRADE_RECON_CCP), new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, task.getInternalReference())));
        }
        return tasks == null || tasks.isEmpty();

    }

    public static TaskArray getExceptionTasks(Trade trade, String ref, DSConnection dsCon) throws CalypsoServiceException {
        return getExceptionTasks(trade.getLongId(), ref, dsCon);
    }

    public static TaskArray getExceptionTasks(long tradeId, String ref, DSConnection dsCon) throws CalypsoServiceException {
        TaskArray taskArray =  new TaskArray();
        if (tradeId > 0) {
            taskArray.addAll( dsCon.getRemoteBO().getTasks(SELECT_GET_TASKS_BY_TRADE_ID, //"(event_type =? OR event_type =?) AND trade_id  = ? AND task_status NOT IN (1, 2)",
                    Arrays.asList(
                            new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP),
                            new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, EXCEPTION_CALYPSO_TRADE_UNMATCHED_RECON_CCP),
                            new CalypsoBindVariable(CalypsoBindVariable.LONG, tradeId))
            ));
        }

        taskArray.addAll( dsCon.getRemoteBO().getTasks(SELECT_GET_TASKS_BY_REF, //"event_type =? AND  trade_id  = 0 AND int_reference=? AND task_status NOT IN (1, 2)",
                Arrays.asList(

                        new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, EXCEPTION_MISSING_TRADE_RECON_CCP),
                        new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, ref))));

        return  taskArray;

          /*
        return dsCon.getRemoteBO().getTasks(SELECT_GET_TASKS, Arrays.asList(
                new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP),
                new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, EXCEPTION_CALYPSO_TRADE_UNMATCHED_RECON_CCP),
                new CalypsoBindVariable(CalypsoBindVariable.LONG, tradeId),
                new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, EXCEPTION_MISSING_TRADE_RECON_CCP),
                new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, ref)));

*/
    }
}
