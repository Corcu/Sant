package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.FilterSet;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.positionkeeping.PositionKeepingRoutingHelperServiceUtil;
import com.calypso.tk.positionkeeping.PositionKeepingServerUtil;
import com.calypso.tk.product.*;
import com.calypso.tk.product.fx.FXSupport;
import com.calypso.tk.product.util.FXTradeUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class ScheduledTaskMATURE_TRADE extends com.calypso.tk.util.ScheduledTaskMATURE_TRADE {

    private int __blockSize;
    private Set<Long> __notToSaveIds = Collections.newSetFromMap(new ConcurrentHashMap());
    private Set<Long> __mirrorIds = Collections.newSetFromMap(new ConcurrentHashMap());
    private TaskArray __tasks = new TaskArray();
    ProcessTradeJobResult __res = null;

    public boolean process(DSConnection ds, PSConnection ps) {
        boolean ret = true;// 96
        if (this._publishB || this._sendEmailB) {// 97
            ret = super.process(ds, ps);// 98
        }

        TaskArray v = new TaskArray();// 100
        Task task = new Task();// 101
        task.setObjectLongId((long) this.getId());// 102
        task.setEventClass("Exception");// 103
        task.setNewDatetime(this.getDatetime());// 104
        task.setUnderProcessingDatetime(this.getDatetime());// 105
        task.setUndoTradeDatetime(this.getDatetime());// 106
        task.setDatetime(this.getDatetime());// 107
        task.setPriority(1);// 108
        task.setId(0L);// 109
        task.setStatus(0);// 110
        task.setEventType("EX_INFORMATION");// 111
        task.setSource(this.getType());// 112
        task.setAttribute("ScheduledTask Id=" + String.valueOf(this.getId()));// 114
        task.setComment(this.toString());// 115
        v.add(task);// 117
        boolean error = false;// 118
        if (this._executeB) {// 120
            if (this.handleMature(ds, ps)) {// 121
                task.setCompletedDatetime(new JDatetime());// 122
            } else {
                task.setEventType("EX_EXCEPTION");// 126
                error = true;// 127
            }
        }

        try {
            getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(v, 0L, (String) null);// 131
        } catch (Exception var9) {// 133
            Log.error(this, var9);
        }

        try {
            this.__trades.clear();// 137
            this.__notToSaveIds.clear();// 138
            this.__mirrorIds.clear();// 139
            this.__tasks.clear();// 140
            this.__res = null;// 141
        } catch (Exception var8) {// 142
            Log.error(this, var8);// 143
        }

        return ret && !error;// 146
    }

    protected boolean handleMature(DSConnection ds, PSConnection ps) {
        boolean ret = false;// 192
        String exec = null;// 194
        TaskArray tasks = null;// 195

        try {
            if (this.loadAndFilterTrades(ds, ps)) {// 197
                ProcessTradeJobResult res = (ProcessTradeJobResult) this.parallelRun(ds, ps);// 198
                if (Log.isCategoryLogged("ScheduledTask") && Log.isDebug()) {// 201
                    Log.debug("ScheduledTask", "__notToSaveIds " + this.__notToSaveIds);// 202
                    Log.debug("ScheduledTask", "__mirrorIds " + this.__mirrorIds);// 203
                }

                if (res != null) {// 206
                    exec = res.getFirst();// 207
                    tasks = res.getSecond();// 208
                }

                if (Log.isCategoryLogged("ScheduledTask") && Log.isDebug()) {// 211
                    Log.debug("ScheduledTask", "parallelRun return exec " + exec);// 212
                    Log.debug("ScheduledTask", "parallelRun return tasks " + tasks);// 213
                }

                if (tasks == null || tasks.size() == 0) {// 216
                    ret = true;// 217
                }
            }
        } catch (ParallelExecutionException var8) {// 219
            Log.error("ScheduledTask", "Exception in ParallelRun", var8);// 220
            new ProcessTradeJobResult("Exception in ParallelRun", new TaskArray());// 221
        }

        return ret;// 225
    }

    @Override
    protected boolean loadAndFilterTrades(DSConnection ds, PSConnection ps) {
        ArrayList<Trade> tradeList = new ArrayList<>();
        TradeArray trades = null;// 239
        JDatetime valDatetime = this.getValuationDatetime();// 240
        JDate valDate = JDate.valueOf(valDatetime, this._timeZone);// 241
        JDate startDate = this.getFromDate(valDate);// 242
        JDatetime startDateTime = this.getValuationDatetime(startDate, false);// 243
        this.getToDate(valDate);// 244
        String actionS = this.getAttribute("APPLY ACTION");// 246
        Action action = Action.MATURE;// 247
        if (!Util.isEmpty(actionS)) {// 248
            action = Action.valueOf(actionS);// 249
        }

        String statusS = this.getAttribute("FILTER_TRADE_STATUS");// 251
        Status status = Status.S_MATURED;// 252
        if (!Util.isEmpty(statusS)) {// 253
            status = Status.valueOf(statusS);// 254
        }

        this.__blockSize = Util.stringToInteger(this.getAttribute("BLOCK_SIZE"));// 256
        if (this.__blockSize < 1) {// 258
            this.__blockSize = 1000;// 259
        }

        if (Log.isCategoryLogged(Log.OLD_TRACE)) {// 261
            Log.debug(Log.OLD_TRACE, "ScheduledTask Mature " + this.getId() + " ValDate: " + valDate + " Valuation Datetime " + valDatetime + " Start Date " + startDate + " action " + action + " status " + status);// 262
        }

        DSConnection roDs = getReadOnlyDS("MATURE_TRADE", ds, this.getExecuteInServerB());// 272 273
        if (roDs == null) {// 274
            Log.error(this, "Could not get a READONLY DS connection");// 275
            return false;// 276
        } else {
            int count = 0;// 278
            if (this._tradeFilter != null) {// 279
                try {
                    TradeFilter tf = BOCache.getTradeFilter(roDs, this._tradeFilter);// 281
                    if (tf == null) {// 282
                        throw new Exception("Could not load Trade Filter: " + this._tradeFilter);// 283
                    }

                    trades = ScheduledTask.getTrades(roDs, tf, valDatetime, (JDatetime) null, true);// 286

                    tradeList = getFilterTrades(trades);
                    trades.clear();
                    if (null != tradeList && tradeList.size() > 0) {
                        trades.addAll(tradeList);
                    }
                    count = trades.size();// 290
                } catch (Exception var27) {// 291
                    Log.error(this, var27);// 292
                }

                if (this._filterSet != null) {// 294
                    TradeArray v = new TradeArray();// 295

                    try {
                        FilterSet fs = roDs.getRemoteMarketData().getFilterSet(this._filterSet);// 297

                        for (int i = 0; i < trades.size(); ++i) {// 299
                            Trade trade = trades.elementAt(i);// 300
                            if (fs.accept(trade, valDate) && isCheckSettlementStatus(trade)) {// 301
                                v.add(trade);// 302
                            }
                        }

                        trades = v;// 304
                    } catch (Exception var28) {// 305
                        Log.error(this, var28);// 306
                    }
                }
            } else if (this._filterSet != null) {// 309
                trades = null;// 310

                try {
                    FilterSet fs = roDs.getRemoteMarketData().getFilterSet(this._filterSet);// 312
                    trades = ScheduledTask.getTrades(roDs, fs, valDate);// 314
                    tradeList = getFilterTrades(trades);
                    trades.clear();
                    if (null != tradeList && tradeList.size() > 0) {
                        trades.addAll(tradeList);
                    }
                    count = trades.size();// 315
                } catch (Exception var26) {// 316
                    Log.error(this, var26);// 317
                }
            }

            if (Log.isCategoryLogged(Log.OLD_TRACE)) {// 320
                Log.debug(Log.OLD_TRACE, "Trades " + count);// 321
            }

            if (count == 0) {// 322
                if (Log.isCategoryLogged(Log.OLD_TRACE)) {// 323
                    Log.debug(Log.OLD_TRACE, "Nothing to process for MATURE TRADES: ");// 324
                }

                this.__trades = new TradeArray();// 326
                return true;// 329
            } else {
                Map<Long, Trade> tradesToProcessMap = null;// 333
                Trade[] tradesToProcess = trades.getTrades();// 334
                TradeArray reloadedTrades = new TradeArray();// 335
                int i;
                if (tradesToProcess != null && tradesToProcess.length > 0) {// 337
                    tradesToProcessMap = new HashMap((int) ((double) tradesToProcess.length * 1.5D));// 338
                    Trade[] var34 = tradesToProcess;
                    i = tradesToProcess.length;

                    for (int var20 = 0; var20 < i; ++var20) {// 340
                        Trade tradeToProcess = var34[var20];
                        Trade trade = this.reloadTradeFromDS(tradeToProcess, ds);// 343
                        reloadedTrades.add(trade);// 344
                        tradesToProcessMap.put(trade.getLongId(), trade);// 345
                    }
                }

                boolean isDebug = Log.isCategoryLogged(Log.OLD_TRACE);// 351

                for (i = 0; i < reloadedTrades.size(); ++i) {// 352
                    Trade trade = reloadedTrades.get(i);// 353
                    if (isDebug) {// 354
                        Log.debug(Log.OLD_TRACE, "Processing trade: " + trade);// 355
                    }

                    if (this.getProcessingOrg() != null && this.getProcessingOrg().getId() != trade.getBook().getProcessingOrgBasedId()) {// 357 358 359
                        if (isDebug) {// 360
                            Log.debug(Log.OLD_TRACE, "Trade: " + trade + " filtered out due to it's PO");// 361
                        }

                        this.__notToSaveIds.add(trade.getLongId());// 364
                    } else if (trade.getStatus().equals(status)) {// 368
                        if (isDebug) {// 369
                            Log.debug(Log.OLD_TRACE, "Trade: " + trade + " filtered out due to it's status: " + trade.getStatus());// 370 373
                        }

                        this.__notToSaveIds.add(trade.getLongId());// 375
                    } else {
                        boolean isPKSProductSupported = PositionKeepingServerUtil.isProductSupported(trade.getProductType());// 393
                        Vector putcallDateSchedule;
                        if (!isPKSProductSupported) {// 395
                            if (isDebug) {// 396
                                Log.debug(Log.OLD_TRACE, "Trade: " + trade + " is not the FX trade supported by PKS. ");// 397
                            }
                        } else {
                            long originalTradeId = FXSupport.Instance.get().getOriginalTradeLongId(trade);// 402
                            if (isDebug) {// 404
                                Log.debug(Log.OLD_TRACE, "Trade: " + trade + " has FX original trade Id: " + originalTradeId);// 405
                            }

                            if (originalTradeId > 0L && trade.getLongId() != originalTradeId || FXSupport.Instance.get().isGeneratedLinkedTrade(trade)) {// 408 409
                                if (isDebug) {// 410
                                    Log.debug(Log.OLD_TRACE, "Trade: " + trade + " filtered out because it is a internally generated trade");// 411
                                }

                                this.__notToSaveIds.add(trade.getLongId());// 417
                                continue;// 418
                            }

                            if (this.hasChildTradeLaterMaturityCheckWithMirrorTradeChildCheck(trade, tradesToProcessMap, startDate, ds)) {// 420
                                if (isDebug) {// 422
                                    Log.debug(Log.OLD_TRACE, "Trade: " + trade + " filtered out because it has a child trade with later maturity !!");// 423
                                }

                                this.__notToSaveIds.add(trade.getLongId());// 429
                                continue;// 430
                            }

                            putcallDateSchedule = new Vector();// 434
                            if (FXTradeUtil.skipGeneratedTradeModification(trade, putcallDateSchedule)) {// 435
                                if (isDebug) {// 436
                                    Log.debug(Log.OLD_TRACE, "Trade: " + trade + " skipped because it is a internally generated trade.");// 437
                                    Log.debug(Log.OLD_TRACE, Util.collectionToString(putcallDateSchedule));// 442
                                }

                                this.__notToSaveIds.add(trade.getLongId());// 444
                                continue;// 445
                            }
                        }

                        JDate matDate = null;// 450
                        if (!trade.getProduct().hasSecondaryMarket()) {// 451
                            matDate = trade.getProduct().getMaturityDate();// 452
                            if (matDate == null) {// 454
                                if (trade.getProduct() instanceof FXBased) {// 455
                                    matDate = trade.getMaxSettleDate();// 456
                                } else if (trade.getProduct() instanceof SimpleTransfer && !(trade.getProduct() instanceof Pledge)) {// 457 458
                                    matDate = trade.getSettleDate();// 459
                                }
                            } else {
                                if (trade.getProduct() instanceof ListedFRA) {// 466
                                    String futureNameMonth = ((ListedFRA) trade.getProduct()).getFutureNameMonth();// 467
                                    ListedFRAContract listedcontract = ((ListedFRA) trade.getProduct()).getListedFRAContract();// 468
                                    ListedFRAContractInfo listedContractInfo = listedcontract.getListedFRAContractInfo(futureNameMonth);// 469
                                    ((ListedFRA) trade.getProduct()).setDefaultAttributes(listedContractInfo);// 470
                                }

                                if (!(trade.getProduct() instanceof SimpleTransfer)) {// 472
                                    matDate = trade.getMaturityDateInclFees();// 473
                                }
                            }

                            if (matDate != null && trade.getProduct() instanceof Option && !trade.getProductType().equals("CancellableSwap")) {// 476
                                Option option = (Option) trade.getProduct();// 477
                                int time = option.getExpiryTime();// 478
                                JDatetime expiryDateTime = new JDatetime(matDate, time / 100, time % 100, 0, 0, option.getExpiryTimeZone());// 479
                                if (startDateTime.before(expiryDateTime)) {// 480
                                    matDate = null;
                                }
                            }
                        } else if (trade.getProduct().getMaturityDate() == null && !(trade.getProduct() instanceof Bond)) {// 483 484
                            if (!(trade.getProduct() instanceof Warrant)) {// 521
                                matDate = trade.getMaxSettleDate();// 522
                            }
                        } else {
                            matDate = trade.getProduct().getMaturityDate();// 485
                            if (trade.getProduct() instanceof BondAssetBacked) {// 496
                                BondAssetBacked bondAssetBacked = (BondAssetBacked) trade.getProduct();// 499
                                matDate = bondAssetBacked.getEffectiveMaturityDate(valDate);// 501
                            }

                            if (trade.getProduct() instanceof Bond) {// 503
                                Bond bond = (Bond) trade.getProduct();// 504
                                putcallDateSchedule = bond.getSchedule();// 506
                                if (!Util.isEmpty(putcallDateSchedule)) {// 507
                                    PutCallDate pc = (PutCallDate) putcallDateSchedule.lastElement();// 508
                                    if (pc.getIsExercised() && ("Full".equals(bond.getAllowedRedemptionType()) || "Full and Partial".equals(bond.getAllowedRedemptionType()) && bond.getNotional(pc.getExpiryDate()) == 0.0D)) {// 509 511 512 513 514 515
                                        matDate = pc.getExpiryDate();// 516
                                    }
                                }
                            }
                        }

                        if (isDebug) {// 526
                            Log.debug(Log.OLD_TRACE, "Trying Trade " + trade.getLongId() + " MatDate " + matDate + " Start " + startDate);// 527
                        }

                        if (matDate == null) {// 530
                            this.__notToSaveIds.add(trade.getLongId());// 531
                        } else if (matDate.lte(startDate)) {// 534
                            trade.setAction(action);// 535
                            trade.setEnteredUser(ds.getUser());// 537
                        } else {
                            this.__notToSaveIds.add(trade.getLongId());// 549
                        }
                    }
                }

                this.__trades = reloadedTrades;// 557
                return true;// 558
            }
        }
    }

    private Trade reloadTradeFromDS(Trade trade, DSConnection ds) {
        boolean isDebug = Log.isCategoryLogged(Log.OLD_TRACE);// 162
        if (!PositionKeepingServerUtil.isProductSupported(trade.getProductType())) {// 164
            return trade;// 165
        } else {
            try {
                if (isDebug) {// 168
                    Log.debug(Log.OLD_TRACE, "Method : reloadTradeFromDS , Get PKS Processed Trade : " + trade + " - Start");// 169
                }

                trade = ds.getRemoteTrade().getPositionKeepingProcessedTrade(trade);// 173
                if (isDebug) {// 175
                    Log.debug(Log.OLD_TRACE, "Method : reloadTradeFromDS , PKS Processed  Trade  : " + trade + " has Transient Keywords : " + trade.getTransientKeywords());// 176 177
                    Log.debug(Log.OLD_TRACE, "Method : reloadTradeFromDS , Get PKS Processed Trade : " + trade + " - End");// 179
                }
            } catch (CalypsoServiceException var5) {// 182
                Log.debug(Log.OLD_TRACE, "Method : reloadTradeFromDS , Exception when creating PKS Processed Trade : ");// 183
            }

            return trade;// 186
        }
    }

    private boolean hasChildTradeLaterMaturityCheckWithMirrorTradeChildCheck(Trade trade, Map<Long, Trade> tradesToProcessMap, JDate startDate, DSConnection ds) {
        boolean isDebug = Log.isCategoryLogged(Log.OLD_TRACE);// 572
        if (isDebug) {// 573
            Log.debug(Log.OLD_TRACE, "Method : hasChildTradeLaterMaturityCheckWithMirrorTradeChildCheck , Check if Trade : " + trade + " has child trade later maturity - Start");// 574
        }

        if (isDebug) {// 579
            Log.debug(Log.OLD_TRACE, "Method : hasChildTradeLaterMaturityCheckWithMirrorTradeChildCheck , Check if Parent Trade : " + trade + " has child trade later maturity - Start");// 580
        }

        Collection<Trade> routedTrades = this.getRoutedTrades(ds, trade, tradesToProcessMap);// 586
        boolean hasAnyChildTradeWithLaterMaturityDate = this.hasAnyChildTradeWithLaterMaturity(trade, routedTrades, startDate);// 588
        if (isDebug) {// 590
            Log.debug(Log.OLD_TRACE, "Method : hasChildTradeLaterMaturityCheckWithMirrorTradeChildCheck , Check if Parent Trade : " + trade + " has child trade later maturity - End");// 591
        }

        if (!hasAnyChildTradeWithLaterMaturityDate) {// 597
            long mirrorTradeId = trade.getMirrorTradeId();// 599
            if (trade.getMirrorTradeId() > 0L) {// 600
                Trade mirrorTrade = (Trade) tradesToProcessMap.get(mirrorTradeId);// 602
                if (mirrorTrade == null) {// 604
                    if (isDebug) {// 606
                        Log.debug(Log.OLD_TRACE, "Method : hasChildTradeLaterMaturityCheckWithMirrorTradeChildCheck , Check if Mirror Trade : " + trade + " has child trade later maturity - Start");// 607
                    }

                    mirrorTrade = ScheduledTask.getTrade(ds, mirrorTradeId);// 612
                }

                Collection<Trade> mirrorRoutedTrades = this.getRoutedTrades(ds, mirrorTrade, tradesToProcessMap);// 615
                hasAnyChildTradeWithLaterMaturityDate = this.hasAnyChildTradeWithLaterMaturity(mirrorTrade, mirrorRoutedTrades, startDate);// 616
                if (isDebug) {// 618
                    Log.debug(Log.OLD_TRACE, "Method : hasChildTradeLaterMaturityCheckWithMirrorTradeChildCheck , Check if Mirror Trade : " + trade + " has child trade later maturity - End");// 619
                }
            }
        }

        if (isDebug) {// 625
            Log.debug(Log.OLD_TRACE, "Method : hasChildTradeLaterMaturityCheckWithMirrorTradeChildCheck , Check if Trade : " + trade + " has child trade later maturity - End");// 626
        }

        return hasAnyChildTradeWithLaterMaturityDate;// 630
    }

    private Collection<Trade> getRoutedTrades(DSConnection ds, Trade trade, Map<Long, Trade> tradesToProcessMap) {
        boolean isDebug = Log.isCategoryLogged(Log.OLD_TRACE);// 665
        if (isDebug) {// 666
            Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Check if Trade : " + trade + " has routed trades - Start");// 667
        }

        boolean isProductSupported = PositionKeepingServerUtil.isProductSupported(trade.getProductType());// 670
        if (!isProductSupported) {// 671
            return null;// 672
        } else {
            boolean isPrePksTrade = PositionKeepingServerUtil.usePositionKeepingServer() ? PositionKeepingServerUtil.isPrePKS(trade) : true;// 678
            if (isDebug) {// 683
                Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Check if Trade : " + trade + " has PKSRoutedTrades - Start");// 684
            }

            Collection childTrades;
            if (isPrePksTrade) {// 688
                childTrades = this.getPrePKSRoutedTrades(ds, trade.getLongId());// 691
            } else {
                childTrades = this.getPKSRoutedTrades(trade);// 696
            }

            if (isDebug) {// 699
                Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , PKSRoutedTrades : " + childTrades);// 700
            }

            if (isDebug) {// 703
                Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Check if Trade : " + trade + " has PKSRoutedTrades - End");// 704
            }

            if (isDebug) {// 708
                Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Parent Trade  : " + trade + " has Transient Keywords : " + trade.getTransientKeywords());// 709 710
            }

            if (isDebug) {// 713
                Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Parent Trade  : " + trade + " has Trade Keywords : " + trade.getKeywords());// 714 715
            }

            Collection<Trade> routedTrades = new ArrayList();// 718
            if (childTrades != null && childTrades.size() > 0) {// 720
                List<Long> arrRoutedTradeIdsNotFoundInProcessMap = new ArrayList();// 722
                Iterator var11 = childTrades.iterator();// 724

                while (var11.hasNext()) {
                    Long tradeId = (Long) var11.next();
                    Trade childTrade = (Trade) tradesToProcessMap.get(tradeId);// 725
                    if (childTrade != null) {// 728
                        if (isDebug) {// 730
                            Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Child Trade  : " + childTrade + " has Transient Keywords : " + childTrade.getTransientKeywords());// 731 732
                        }

                        if (isDebug) {// 735
                            Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Child Trade  : " + childTrade + " has Trade Keywords : " + childTrade.getKeywords());// 736 737
                        }

                        routedTrades.add(childTrade);// 740
                    } else {
                        arrRoutedTradeIdsNotFoundInProcessMap.add(tradeId);// 745
                    }
                }

                if (isDebug) {// 749
                    Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Before Fetch all the trades which are not found in tradesToProcessMap, value in routedTrades : " + routedTrades);// 750
                }

                if (arrRoutedTradeIdsNotFoundInProcessMap != null && arrRoutedTradeIdsNotFoundInProcessMap.size() > 0) {// 756
                    if (isDebug) {// 758
                        Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Fetch all the trades which are not found in tradesToProcessMap - Start");// 759
                    }

                    TradeArray missedRoutedTradeArray = this.getTrades(ds, Util.toLongPrimitive((Long[]) arrRoutedTradeIdsNotFoundInProcessMap.toArray(new Long[arrRoutedTradeIdsNotFoundInProcessMap.size()])));// 764
                    if (isDebug) {// 766
                        Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Fetch all the trades which are not found in tradesToProcessMap, value in missedRoutedTradeArray : " + missedRoutedTradeArray);// 767
                    }

                    if (missedRoutedTradeArray != null && missedRoutedTradeArray.size() > 0) {// 772
                        Trade[] missedRoutedTrades = missedRoutedTradeArray.getTrades();// 773
                        Trade[] var13 = missedRoutedTrades;
                        int var14 = missedRoutedTrades.length;

                        for (int var15 = 0; var15 < var14; ++var15) {// 774
                            Trade routedTrade = var13[var15];
                            routedTrades.add(routedTrade);// 775
                        }

                        if (isDebug) {// 778
                            Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Fetch all the trades which are not found in tradesToProcessMap, value in routedTrades : " + routedTrades);// 779
                        }
                    }

                    if (isDebug) {// 785
                        Log.debug(Log.OLD_TRACE, "Method : getRoutedTrades , Fetch all the trades which are not found in tradesToProcessMap - End");// 786
                    }
                }
            }

            return routedTrades;// 792
        }
    }

    private Collection<Long> getPKSRoutedTrades(Trade trade) {
        return PositionKeepingRoutingHelperServiceUtil.fetchRiskRoutedRelatedTradeIds(trade);// 801
    }

    private Collection<Long> getPrePKSRoutedTrades(DSConnection ds, long origTradeId) {
        Collection<Long> lstChildTradeIds = Collections.emptyList();// 811
        Object var5 = null;// 812

        try {
            long[] childTradeIds = ds.getRemoteTrade().getTradeIdsByKeywordNameAndValue("FXOriginalTradeID", Long.toString(origTradeId));// 815
            if (childTradeIds != null && childTradeIds.length > 0) {// 816
                lstChildTradeIds = new ArrayList();// 818
                long[] var6 = childTradeIds;
                int var7 = childTradeIds.length;

                for (int var8 = 0; var8 < var7; ++var8) {// 819
                    long tradeId = var6[var8];
                    ((Collection) lstChildTradeIds).add(new Long(tradeId));// 821
                }
            }
        } catch (Exception var11) {// 825
            Log.error("ScheduledTask", var11);// 826
        }

        return (Collection) lstChildTradeIds;// 828
    }

    private boolean hasAnyChildTradeWithLaterMaturity(Trade trade, Collection<Trade> routedTrades, JDate startDate) {
        boolean isDebug = Log.isCategoryLogged(Log.OLD_TRACE);// 634
        if (isDebug) {// 636
            Log.debug(Log.OLD_TRACE, "Checking if the trade has any later maturing child trade: " + trade);// 637
        }

        if (routedTrades != null && routedTrades.size() > 0) {// 639
            Iterator var5 = routedTrades.iterator();

            while (var5.hasNext()) {
                Trade childTrade = (Trade) var5.next();// 640
                if (childTrade.getMaxSettleDate().after(trade.getMaxSettleDate()) && childTrade.getMaxSettleDate().after(startDate) && !childTrade.getStatus().equals(Status.S_CANCELED)) {// 641 642
                    if (isDebug) {// 643
                        Log.debug(Log.OLD_TRACE, " Trade " + trade + " has a later maturing child trade, " + childTrade.getLongId());// 644
                    }

                    return true;// 646
                }
            }
        }

        return false;// 650
    }

    private TradeArray getTrades(DSConnection ds, long[] tradeIds) {
        TradeArray tradeArray = null;// 974

        try {
            tradeArray = ds.getRemoteTrade().getTrades(tradeIds);// 976
        } catch (Exception var5) {// 978
            Log.error("ScheduledTask", var5);// 979
        }

        return tradeArray;// 981
    }

    public IExecutionContext createExecutionContext(DSConnection ds, PSConnection ps) throws ParallelExecutionException {
        ScheduledTaskMATURE_TRADE.MatureTradeExecutionContext res = null;// 991

        try {
            res = new ScheduledTaskMATURE_TRADE.MatureTradeExecutionContext(this, ds, ps, this.__tasks);// 993
        } catch (Exception var5) {// 994
            Log.error(this, var5);// 995
        }

        return res;// 997
    }

    public List<Callable<Boolean>> split(IExecutionContext ctx) throws ParallelExecutionException {
        List<Callable<Boolean>> res = null;// 1003
        if (ctx instanceof ScheduledTaskMATURE_TRADE.MatureTradeExecutionContext) {// 1004
            ScheduledTaskMATURE_TRADE.MatureTradeExecutionContext context = (ScheduledTaskMATURE_TRADE.MatureTradeExecutionContext) ctx;// 1005
            DSConnection ds = context.getDSConnection();// 1006
            PSConnection ps = context.getPSConnection();// 1007
            TaskArray tasks = context.getTasks();// 1008
            int threadCount = context.getThreadPoolSize();// 1009

            try {
                res = new ArrayList();// 1011
                TradeArray filteredTrades = this.__trades;// 1012
                if (filteredTrades != null && filteredTrades.size() > 0) {// 1014
                    if (threadCount > 1) {// 1016
                        List<TradeArray> list = this.getListOfTradeArray(filteredTrades);// 1017
                        if (list != null) {// 1018
                            Iterator var10 = list.iterator();// 1019

                            while (var10.hasNext()) {
                                TradeArray tradeArray = (TradeArray) var10.next();
                                ScheduledTaskMATURE_TRADE.SimpleMatureTradeJob job = new ScheduledTaskMATURE_TRADE.SimpleMatureTradeJob(this, ds, tradeArray, tasks, context);// 1020
                                res.add(job);// 1022
                            }
                        }
                    } else {
                        ScheduledTaskMATURE_TRADE.SimpleMatureTradeJob job = new ScheduledTaskMATURE_TRADE.SimpleMatureTradeJob(this, ds, filteredTrades, tasks, context);// 1026
                        res.add(job);// 1028
                    }
                }
            } catch (Exception var13) {// 1031
                Log.error("ScheduledTask", var13);// 1032
                res = null;// 1033
            }
        } else {
            Log.error(this, "Execution context is not of the correct type (given = " + ctx.getClass().getSimpleName() + ", expected = MatureTradeExecutionContext)");// 1036 1038
            res = null;// 1040
        }

        return res;// 1042
    }

    private List<TradeArray> getListOfTradeArray(TradeArray filteredTrade) throws RemoteException {
        List<TradeArray> listOfTradeArray = new ArrayList();// 1048
        int totalCount = 0;// 1049
        List<Trade> listOfTrades = filteredTrade.asList();// 1052

        int counterOfList;
        for (counterOfList = 0; counterOfList < listOfTrades.size(); counterOfList += this.__blockSize) {// 1055
            List<Trade> blockOfTrades = listOfTrades.subList(counterOfList, Math.min(listOfTrades.size(), counterOfList + this.__blockSize));// 1057
            totalCount += blockOfTrades.size();// 1059
            listOfTradeArray.add(new TradeArray(blockOfTrades));// 1060
        }

        if (totalCount != filteredTrade.size()) {// 1064
            Log.error(this, "getListOfTradeArray doesn't work totalCount " + totalCount + " not equals to filteredTrade.size " + filteredTrade.size());// 1065 1067
            return null;// 1068
        } else {
            counterOfList = 0;// 1072
            Iterator var12 = listOfTradeArray.iterator();// 1073

            while (var12.hasNext()) {
                TradeArray tradeArray = (TradeArray) var12.next();
                ++counterOfList;// 1074
                List<Trade> list = tradeArray.asList();// 1075
                StringBuffer buffer = new StringBuffer();// 1076
                Iterator var10 = list.iterator();// 1077

                while (var10.hasNext()) {
                    Trade trade = (Trade) var10.next();
                    buffer.append(trade.getLongId()).append(",");// 1078
                }

                if (Log.isCategoryLogged("ScheduledTask") && Log.isDebug()) {// 1080
                    Log.debug("ScheduledTask", "list " + counterOfList + " with trades " + buffer.toString());// 1081
                }
            }

            return listOfTradeArray;// 1084
        }
    }

    class MatureTradeExecutionContext implements IExecutionContext {
        private final int threadCount;
        private final DSConnection ds;
        private PSConnection ps;
        private final TaskArray tasks;

        public MatureTradeExecutionContext(ScheduledTask task, final DSConnection ds, PSConnection ps, TaskArray tasks) throws Exception {
            this.ds = ds;// 1101
            this.ps = ps;// 1102
            this.tasks = tasks;// 1103
            this.threadCount = task.getThreadCount();// 1104
        }// 1105

        public int getThreadPoolSize() {
            return this.threadCount;// 1109
        }

        public final DSConnection getDSConnection() {
            return this.ds;// 1117
        }

        public final PSConnection getPSConnection() {
            return this.ps;// 1125
        }

        public final TaskArray getTasks() {
            return this.tasks;// 1132
        }
    }

    abstract class MatureTradeJob implements Callable<Boolean> {
        protected ScheduledTaskMATURE_TRADE superTask;
        protected ScheduledTaskMATURE_TRADE.MatureTradeExecutionContext context;

        public MatureTradeJob(ScheduledTaskMATURE_TRADE task, ScheduledTaskMATURE_TRADE.MatureTradeExecutionContext context) {
            this.superTask = task;// 1145
            this.context = context;// 1146
        }// 1147

        public abstract Boolean call();
    }


    class SimpleMatureTradeJob extends ScheduledTaskMATURE_TRADE.MatureTradeJob {
        private DSConnection ds;
        private TradeArray trades;
        private TaskArray tasks;

        public SimpleMatureTradeJob(ScheduledTaskMATURE_TRADE task, final DSConnection ds, TradeArray trades, TaskArray tasks, ScheduledTaskMATURE_TRADE.MatureTradeExecutionContext context) {
            super(task, context);// 1159
            this.ds = ds;// 1160
            this.trades = trades;// 1161
            this.tasks = tasks;// 1162
        }// 1163

        public Boolean call() {
            Boolean res = Boolean.FALSE;// 1167

            try {
                this.superTask.updateTradesStatus(this.ds, this.trades, this.tasks);// 1169
                res = Boolean.TRUE;// 1170
            } catch (Throwable var3) {// 1171
                res = Boolean.FALSE;// 1172
            }

            return res;// 1174
        }
    }


    private boolean isCheckSettlementStatus(Trade trade) {
        if (trade.getProduct() instanceof SecLending) {
            SecFinance secFinance = (SecFinance) trade.getProduct();
            String settlementStatus = secFinance.getActualSettlementDetails().getSecuritySettlementStatus(trade, getValuationDatetime().getJDate(TimeZone.getDefault()));
            if (!"Close Leg Settled".equalsIgnoreCase(settlementStatus)) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<Trade> getFilterTrades(TradeArray trades) {
        ArrayList<Trade> tradeList = new ArrayList<>();

        for (Object object : trades) {
            Trade trade = (Trade) object;
            if (trade.getProduct() instanceof SecLending) {
                if (isCheckSettlementStatus(trade)) {
                    tradeList.add(trade);
                }
            } else {
                tradeList.add(trade);
            }
        }
        return tradeList.size() > 0 ? tradeList : null;
    }

}
