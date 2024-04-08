package calypsox.tk.report;

import calypsox.tk.collateral.service.RemotePLMarkHist;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.entitlement.DataEntitlementCheckProxy;
import com.calypso.tk.marketdata.MarketDataEntitlementController;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.DataServer;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

import static calypsox.tk.core.CollateralStaticAttributes.MC_CONTRACT_NUMBER;
import static calypsox.util.TradeInterfaceUtils.*;

public class SantTradeBrowserReport extends Report {

    private boolean countOnly = false;

    private static final long serialVersionUID = 1331104288730965711L;

    private static final String COMMA = ",";

    // ARCHIVE. Para saber si se ha seleccionado o no el tick Include Archive
    private boolean includeArchived;


    @SuppressWarnings("unchecked")
    @Override
    public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

        if (this._reportTemplate == null) {
            return null;
        }
        final Vector<String> errorMsgs = errorMsgsP;

        if (!hasMTMDateRangeWeekDay(getMTMValDateFrom(), getMTMValDateTo())) {
            return null;
        }

        // ARCHIVE Para almacenar si esta seleccionado o no el tick Include
        // Archive (para incluir los historificados)
        includeArchived = includeArchived();

        // initDates();
        final DefaultReportOutput output = new DefaultReportOutput(this);

        // Get All user Info and create UserAuditItem objects
        Collection<SantTradeBrowserItem> tradeBrowserItems = null;
        try {
            // GSM 05/08/15. SBNA Multi-PO filter
            tradeBrowserItems = getTradeBrowserItems(errorMsgs);

        } catch (final RemoteException e) {
            Log.error(SantTradeBrowserReport.class, "Error loading MTM Audit Items", e);
        }

        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

        if ((tradeBrowserItems != null) && (tradeBrowserItems.size() > 0)) {

            for (final SantTradeBrowserItem item : tradeBrowserItems) {
                // We need to add one row for each MTM Day
                addOneRowPerDay(reportRows, item, getMTMValDateFrom(), getMTMValDateTo());
            }
        }

        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
        return output;
    }

    private boolean hasMTMDateRangeWeekDay(JDate from, JDate to) {
        JDate mtmDate = from;
        while (mtmDate.lte(to)) {
            if (mtmDate.isWeekEndDay()) {
                mtmDate = mtmDate.addDays(1);
                continue;
            }
            return true;
        }
        return false;
    }

    private void addOneRowPerDay(ArrayList<ReportRow> reportRows, SantTradeBrowserItem item, JDate from, JDate to) {

        JDate mtmDate = from;
        while (mtmDate.lte(to)) {
            if (mtmDate.isWeekEndDay()) {
                mtmDate = mtmDate.addDays(1);
                continue;
            }
            // Start Date check
            if (mtmDate.before(item.getTrade().getTradeDate().getJDate(TimeZone.getDefault()))) {
                mtmDate = mtmDate.addDays(1);
                continue;
            }

            // Cancelled Date check
            if ((item.getTrade().getStatus().getStatus().equals(Status.CANCELED)
                    || item.getTrade().getStatus().getStatus().equals(Status.MATURED))
                    && mtmDate.after(item.getTrade().getUpdatedTime().getJDate(TimeZone.getDefault()))) {
                mtmDate = mtmDate.addDays(1);
                continue;
            }

            // End Date/Maturity Date check
            if ((item.getTrade().getMaturityDateInclFees() != null)
                    && mtmDate.after(getSantSettleDate(item.getTrade()))) {

                mtmDate = mtmDate.addDays(1);
                continue;
            }

            final ReportRow row = new ReportRow(item.getTrade(), ReportRow.TRADE);
            row.setProperty(ReportRow.MARGIN_CALL_CONFIG, item.getMarginCall());
            row.setProperty(ReportRow.PL_MARK, item.getPLMark(mtmDate));
            row.setProperty(SantTradeBrowserReportTemplate.VAL_DATE, mtmDate);
            reportRows.add(row);

            mtmDate = mtmDate.addDays(1);
        }
    }

    private JDate getSantSettleDate(Trade trade) {
        JDate matDate = trade.getMaturityDateInclFees();
        @SuppressWarnings("unused")
        JDate productEndDate = null;
        Product product = trade.getProduct();
        if (product instanceof Repo) {
            productEndDate = ((Repo) product).getEndDate();
        } else if (product instanceof SecLending) {
            productEndDate = ((SecLending) product).getEndDate();
        } else if (product instanceof CollateralExposure) {
            productEndDate = ((CollateralExposure) product).getEndDate();
        } else {
            return matDate;
        }

        // if (!matDate.equals(productEndDate)) {
        // matDate = matDate.addBusinessDays(-1,
        // CollateralUtilities.getSystemHolidays());
        // }

        return matDate;
    }

    private JDate getMTMValDateFrom() {
        return (JDate) this._reportTemplate.get(SantTradeBrowserReportTemplate.VAL_DATE_FROM);
    }

    private JDate getMTMValDateTo() {
        return (JDate) this._reportTemplate.get(SantTradeBrowserReportTemplate.VAL_DATE_TO);

    }

    @SuppressWarnings("rawtypes")
    private Collection<SantTradeBrowserItem> getTradeBrowserItems(final Vector errormsgs) throws RemoteException {

        final String contractIdStr = (String) this._reportTemplate.get(SantTradeBrowserReportTemplate.CONTRACT_IDS);
        Vector<Integer> contractIdsVect = new Vector<Integer>();
        if (!Util.isEmpty(contractIdStr)) {
            contractIdsVect = Util.string2IntVector(contractIdStr);
        }

        final String valAgent = (String) this._reportTemplate.get(SantTradeBrowserReportTemplate.VAL_AGENT);
        final String agrType = (String) this._reportTemplate.get(SantTradeBrowserReportTemplate.AGR_TYPE);
        final String cptyIds = (String) this._reportTemplate.get(TradeReportTemplate.CPTYNAME);
        Vector<Integer> cptyIdsVect = Util.string2IntVector(cptyIds);

        // GSM 05/08/15. SBNA Multi-PO filter
        String poAgrStr = "";
        if (CollateralUtilities.isFilteredByST(getReportTemplate())) {

            poAgrStr = CollateralUtilities.filterPoIdsByTemplate(getReportTemplate());
        } else {
            poAgrStr = (String) this._reportTemplate.get(SantTradeBrowserReportTemplate.PROCESSING_ORG_AGR);
        }

        Vector<Integer> poAgrIds = null;
        if (!Util.isEmpty(poAgrStr)) {
            poAgrIds = Util.string2IntVector(poAgrStr);
        }

        final String where = buildWhere();

        final String from = buildFrom();

        return loadTradeBrowserItems(from, where, valAgent, poAgrIds, agrType, contractIdsVect, cptyIdsVect);

    }

    private boolean checkIsMrgCallCriteriaSpecified(final String valAgent, final Vector<Integer> agrOwnerIds,
                                                    final String agrType, Vector<Integer> cptyIdsVect, Vector<Integer> contractIdsVect) throws RemoteException {

        boolean isMrgCallCriteriaSpecified = false;
        if (!Util.isEmpty(valAgent) || !Util.isEmpty(agrOwnerIds) || !Util.isEmpty(agrType)
                || !Util.isEmpty(cptyIdsVect) || !Util.isEmpty(contractIdsVect)) {
            isMrgCallCriteriaSpecified = true;
        }

        return isMrgCallCriteriaSpecified;
    }

    /**
     * 1. If the contract id is specified it loads the contract first otherwise
     * it loads contracts based on the contract criteria or all contracts. 2.
     * Then it filters out contracts not falling into the contract specific
     * criteria specified. 3. If no contract criteria is specified then it loads
     * Trades and then load contracts that those trades belong to. 4. Load PL
     * Marks for all those trades.
     *
     * @param from
     * @param where
     * @param valAgent
     * @param agrOwnerIds
     * @param agrType
     * @param contractIdsVect
     * @return
     * @throws RemoteException
     */
    private Collection<SantTradeBrowserItem> loadTradeBrowserItems(String from, String where, final String valAgent,
                                                                   final Vector<Integer> agrOwnerIds, final String agrType, Vector<Integer> contractIdsVect,
                                                                   Vector<Integer> cptyIdsVect) throws RemoteException {

        final HashMap<Long, SantTradeBrowserItem> tradeItemsMap = new HashMap<>();

        final HashMap<Integer, CollateralConfig> finalContractsMap = new HashMap<>();

        boolean isMrgCallCriteriaSpecified = checkIsMrgCallCriteriaSpecified(valAgent, agrOwnerIds, agrType,
                cptyIdsVect, contractIdsVect);

        int valAgentID = 0;
        if (!Util.isEmpty(valAgent)) {
            valAgentID = BOCache.getLegalEntityId(DSConnection.getDefault(), valAgent);
        }

        List<CollateralConfig> allMCConfigs = new ArrayList<CollateralConfig>();

        if (isMrgCallCriteriaSpecified) {
            MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();

            if (!Util.isEmpty(contractIdsVect)) {
                mcFilter.setContractIds(contractIdsVect);
            }
            if (!Util.isEmpty(agrOwnerIds)) {
                mcFilter.setProcessingOrgIds(agrOwnerIds);
            }

            if (!Util.isEmpty(agrType)) {
                mcFilter.setContractTypes(Util.string2Vector(agrType));
            }

            if (!Util.isEmpty(cptyIdsVect)) {
                mcFilter.setLegalEntityIds(cptyIdsVect);
            }
            // ABOUT 2500 registries
            allMCConfigs = CollateralManagerUtil.loadCollateralConfigs(mcFilter);

            // BAU 6.1 - Filter only OPEN contracts

            List<CollateralConfig> contractsArray = new ArrayList<CollateralConfig>();
            // PERFORMANCE CHECK
            for (int i = 0; i < allMCConfigs.size(); i++) {
                if (allMCConfigs.get(i).getAgreementStatus().equals("OPEN")) {
                    contractsArray.add(allMCConfigs.get(i));
                }
            }

            // Filter out contracts not matched for criteria
            if (contractsArray.size() > 0) {
                for (final CollateralConfig marginCallConfig : contractsArray) {
                    boolean matched = true;
                    if (valAgentID != 0) {
                        if (marginCallConfig.getValuationAgentId() != valAgentID) {
                            matched = false;
                        }
                    }

                    if (matched) {
                        // Add to Map if not in the map already
                        if (finalContractsMap.get(marginCallConfig.getId()) == null) {
                            finalContractsMap.put(marginCallConfig.getId(), marginCallConfig);
                        }
                    }
                }
            }

        } // fin if isMrgCallCriteriaSpecified

        TradeArray trades = new TradeArray();

        if (isMrgCallCriteriaSpecified && (finalContractsMap.size() > 0)) {
            final Set<Integer> keySet = finalContractsMap.keySet();
            List<String> sqlAgrIdsList = CollateralUtilities.getSqlStringList(new ArrayList<Integer>(keySet), 999,
                    true);

            if (this.countOnly) {
                int count = 0;
                if (!Util.isEmpty(sqlAgrIdsList)) {
                    // CHECK PERFORMANCE
                    for (String sqlAgrIds : sqlAgrIdsList) {
                        String countWhere = where;
                        if (countWhere.length() > 0) {
                            countWhere += " and ";
                        }
                        countWhere = countWhere + "  trade.internal_reference in " + sqlAgrIds;

                        int[] countArray = DSConnection.getDefault().getRemoteTrade().countTrades(from, countWhere, null);

                        // ARCHIVE si el tick es true.
                        // DSConnection.getDefault().getRemoteTrade().countArchiveTrades2(from,
                        // where);
                        // sumarlos
                        count = count + countArray[0];
                    }
                }

                // ARCHIVE
                if (includeArchived)
                    count = 1;

                addPotentialSize(Trade.class.getName(), count);
                // return null; // GSM 21/08/15 - PO filter will NEVER WORK!
                if (count < 1) {
                    return null;
                }
            }

            // Load Trades here
            if (!Util.isEmpty(sqlAgrIdsList)) {
                for (String sqlAgrIds : sqlAgrIdsList) {
                    String tmpWhere = where;
                    if (tmpWhere.length() > 0) {
                        tmpWhere += " and ";
                    }
                    tmpWhere = tmpWhere + "  trade.internal_reference in " + sqlAgrIds;

                    TradeArray trades2;

                    // ARCHIVE
                    trades2 = DSConnection.getDefault().getRemoteTrade().getTrades(from, tmpWhere, null, null);


                    //Si hay que incluir los trades archivados, cargarlos y a?adirlos
                    if (includeArchived) {
                        TradeArray tradesArchived = DSConnection.getDefault().getRemoteTrade().getArchivedTrades(null, tmpWhere);
                        trades2.addAll(tradesArchived);

                    }

                    trades.addAll(trades2);
                }
            }
            // else {
            // trades =
            // DSConnection.getDefault().getRemoteTrade().getTrades(from, where,
            // null);
            // }
        } else if (!isMrgCallCriteriaSpecified) {
            // No Agreement criteria
            if (this.countOnly) {

                int[] countArray;

                if (includeArchived) {
                    // hay q poner en el where las tablas historicas, sino da
                    // error.
                    // from="trade_hist, product_desc_hist";

                    countArray = new int[1];
                    // countArray[0]=DSConnection.getDefault().getRemoteTrade().countArchiveTrades2(from,where);

                    countArray[0] = 1;
                } else
                    countArray = DSConnection.getDefault().getRemoteTrade().countTrades(from, where, null);
                addPotentialSize(Trade.class.getName(), countArray[0]);
                return null;
            }
            // CHECK PERFORMANCE
            long a = System.currentTimeMillis();

            //ARCHIVE
            //cargar los trades.
            trades = DSConnection.getDefault().getRemoteTrade().getTrades(from, where, null, null);
            //Si hay que incluir los trades archivados, cargarlos y a?adirlos
            if (includeArchived) {
                TradeArray tradesArchived = new TradeArray();
                tradesArchived = DSConnection.getDefault().getRemoteTrade().getArchivedTrades(null, where, null);
                trades.addAll(tradesArchived);
            }

            long b = System.currentTimeMillis();
            System.out.println("Get remote trade time segs--> " + (b - a) / 60000);
        }

        if ((trades != null) && (trades.size() > 0)) {
            for (int i = 0; i < trades.size(); i++) {
                final SantTradeBrowserItem tradeItem = new SantTradeBrowserItem();
                final Trade trade = trades.get(i);
                tradeItem.setTrade(trade);

                if (finalContractsMap.get(trade.getKeywordAsInt(MC_CONTRACT_NUMBER)) != null) {
                    tradeItem.setMarginCall(finalContractsMap.get(trade.getKeywordAsInt(MC_CONTRACT_NUMBER)));
                } else {
                    // When no mrgCall criteria specified we dont load any
                    // contracts so load the relevant ones here
                    final int contractId = trade.getKeywordAsInt(MC_CONTRACT_NUMBER);
                    if (contractId != 0) {
                        @SuppressWarnings("static-access")
                        // CHECK PERFORMANCE NOT-NECCESARY ACCESS
                        final CollateralConfig marginCallConfig = CacheCollateralClient.getInstance()
                                .getCollateralConfig(getDSConnection(), contractId);

                        finalContractsMap.put(contractId, marginCallConfig);
                        tradeItem.setMarginCall(marginCallConfig);
                    }
                }
                tradeItemsMap.put(trade.getLongId(), tradeItem);
            }
        }

        // 3. Load PLMArks
        if (tradeItemsMap.size() > 0) {
            try {
                // CHECK
                loadPLMarks(tradeItemsMap, getMTMValDateFrom(), getMTMValDateTo());


            } catch (final Exception exc) {
                Log.error(SantTradeBrowserReport.class, "Error retreiving PLMarks. ", exc);
            }
        }

        return tradeItemsMap.values();
    }

    /**
     * This method retreives PLMarks for the trades in SantTradeBrowserItem
     * passed.
     *
     * @param tradeItemsMap
     * @throws RemoteException
     */
    @SuppressWarnings("rawtypes")
    private void loadPLMarks(final HashMap<Long, SantTradeBrowserItem> tradeItemsMap, final JDate from,
                             final JDate to) throws RemoteException {
        // Load PLMarks pl_mark
        final ArrayList<Long> tradeIdsList = new ArrayList<>(tradeItemsMap.keySet());
        String pricingEnv = getPricingEnv().getName();

        final int SQL_IN_ITEM_COUNT = 999;
        int start = 0;

        for (int i = 0; i <= (tradeIdsList.size() / SQL_IN_ITEM_COUNT); i++) {
            int end = (i + 1) * SQL_IN_ITEM_COUNT;
            if (end > tradeIdsList.size()) {
                end = tradeIdsList.size();
            }
            final List<Long> subList = tradeIdsList.subList(start, end);

            if (subList.size() > 0) {
                // To retreive PLMarks we need a map with TradeIds as keys as
                // per
                // the API. So create one.
                Collection<Long> tradeIds = new ArrayList<>();
                for (final Long trade_id : subList) {
                    tradeIds.add(trade_id);

                }

                // Load PLMArks for a date range
                JDate tmpDate = from;

                while (tmpDate.lte(to)) {
                    Collection<PLMark> plMarks = null;
                    try {
                        plMarks = CollateralUtilities.retrievePLMarkBothTypes(tradeIds, pricingEnv, tmpDate);
                        //quizas mejor cargar los pl-marks historificados aqui.

                        //cargar los 2 tipos de pl-marks

                        //cargar los PLMArks de los historificados si es necesario.(mejor dentro del metodo anterior)
                        if (includeArchived) {


                            Collection<PLMark> plMarksHist = null;

                            //hay que pasarle tradeIds, pricingEnv, tmpDate
                            plMarksHist = loadPLMarksHist(tradeIds, pricingEnv, tmpDate);


                            //a?adir los pl-mark hist a los normales..

                            if (plMarksHist != null) plMarks.addAll(plMarksHist);
                        }


                    } catch (PersistenceException e) {
                        Log.error(this, e);//Sonar
                    }
                    // final Set plMarks =
                    // DSConnection.getDefault().getRemoteMarketData().getPLMarks(tradeIdsMap,
                    // pricingEnv, tmpDate);

                    // Set plMarks to SantTradeBrowserItem
                    final Iterator plMarkIter = plMarks.iterator();
                    while (plMarkIter.hasNext()) {
                        final PLMark plMark = (PLMark) plMarkIter.next();
                        if (tradeItemsMap.get(plMark.getTradeLongId()) != null) {
                            final SantTradeBrowserItem santTradeBrowserItem = tradeItemsMap.get(plMark.getTradeLongId());
                            santTradeBrowserItem.addPLMark(tmpDate, plMark);
                        }
                    }

                    tmpDate = tmpDate.addDays(1);
                }
            }
            start = end;

        }

    }


    //metodo que carga los Pl-marks historificados.
    Collection<PLMark> loadPLMarksHist(Collection<Long> tradeIds, String pricingEnv, JDate tmpDate) {
        Collection<PLMark> plMarkHistTradePosCollection = null;
        Collection<PLMark> plMarkHistTradeZeroCollection = null;

        String tradePosParams = "";
        String tradeZeroParams = "";


        //Filtros para busqueda de pl-marks
        //pricingEnv
        if (!Util.isEmpty(pricingEnv)) {
            tradePosParams += "  pricing_env_name = " + ioSQL.string2SQLString(pricingEnv);

            tradeZeroParams += "  pricing_env_name = " + ioSQL.string2SQLString(pricingEnv);
        }

        //fecha

        if (tmpDate != null) {
            if (!Util.isEmpty(tradePosParams)) {
                tradePosParams += " AND ";
            }
            tradePosParams += " valuation_date >= " + Util.date2SQLString(tmpDate);

            tradeZeroParams += " AND valuation_date >= " + Util.date2SQLString(tmpDate);
        }

        if (tmpDate != null) {
            if (!Util.isEmpty(tradePosParams)) {
                tradePosParams += " AND ";
            }
            tradePosParams += " valuation_date <= " + Util.date2SQLString(tmpDate);

            tradeZeroParams += " AND valuation_date <= " + Util.date2SQLString(tmpDate);
        }

        //TradesIDs
        if (tradeIds != null && !tradeIds.isEmpty()) {
            tradePosParams += " AND trade_id IN (";
            String ids = "";
            for (final Long id : tradeIds) {
                ids += id + ",";

            }
            ids = ids.substring(0, ids.length() - 1);
            tradePosParams += ids + ")";
        }


        //
        try {
            RemotePLMarkHist remotePLMarkHist = null;
            try {
                remotePLMarkHist = (RemotePLMarkHist) getDSConnection().getRMIService("PLMarkHistServer",
                        RemotePLMarkHist.class);
                if ((!DataServer._isDataServer)
                        && (MarketDataEntitlementController.getDefault().needToPerformEntitlementCheck())) {
                    remotePLMarkHist = DataEntitlementCheckProxy.newInstance(remotePLMarkHist);
                }
            } catch (final Exception e) {
                Log.error(this, e);
            }

            //tradePosParams hay que montarlo con tradeIds, pricingEnv, tmpDate que hay que pasarlo como param.
            plMarkHistTradePosCollection = remotePLMarkHist.getPLMarksTableSwitch(tradePosParams, false);

            plMarkHistTradeZeroCollection = remotePLMarkHist.getPLMarksTableSwitch(tradeZeroParams, false);


        } catch (final Exception e) {
            Log.error(this, e);
        }

        if (plMarkHistTradePosCollection != null && plMarkHistTradeZeroCollection != null)
            plMarkHistTradePosCollection.addAll(plMarkHistTradeZeroCollection);
        return plMarkHistTradePosCollection;
    }

    @SuppressWarnings({"rawtypes", "deprecation"})
    protected String buildWhere() {
        if (this._reportTemplate == null) {
            return null;
        }
        final ReportTemplate h = this._reportTemplate;
        final StringBuffer where = new StringBuffer("");
        String s = null;

        // Trade ID
        s = (String) h.get(TradeReportTemplate.TRADE_ID);
        if (!Util.isEmpty(s)) {
            // GSM: 28/05/15: change to allow several trades ids separate by
            // comma
            if (!s.contains(COMMA)) {
                long tradeId = 0;
                try {
                    tradeId = Util.getInteger(s);
                } catch (final Exception e) {
                    Log.error(this, e);//Sonar
                    tradeId = 0;
                }
                appendAND(where);
                where.append(" trade.trade_id = " + tradeId);

            } else {
                Vector<Integer> tradeIds = Util.string2IntVector(s);
                appendAND(where);
                where.append(" trade.trade_id IN ");
                where.append(Util.collectionToSQLString(tradeIds));
            }
        }

        // BO_REFERENCE
        s = (String) this._reportTemplate.get(SantTradeBrowserReportTemplate.FRONT_ID);
        if (!Util.isEmpty(s)) {
            appendAND(where);
            where.append(" trade.external_reference=" + Util.string2SQLString(s));
        }

        // FRONT_ID
        s = (String) this._reportTemplate.get(SantTradeBrowserReportTemplate.BO_REFERENCE);
        if (!Util.isEmpty(s)) {
            appendAND(where);
            final String temp = " exists( select 1 from trade_keyword where trade.trade_id=trade_keyword.trade_id "
                    + "AND trade_keyword.keyword_name='" + TRADE_KWD_BO_REFERENCE + "' "
                    + "AND trade_keyword.keyword_value=" + Util.string2SQLString(s) + ")";
            where.append(temp);
        }

        // GSM: RIG Code
        s = (String) this._reportTemplate.get(SantTradeBrowserReportTemplate.RIG_CODE);
        if (!Util.isEmpty(s)) {
            appendAND(where);
            final String temp = " exists( select 1 from trade_keyword where trade.trade_id=trade_keyword.trade_id "
                    + "AND trade_keyword.keyword_name='" + TRADE_KWD_RIG_CODE + "' "
                    + "AND trade_keyword.keyword_value=" + Util.string2SQLString(s) + ")";
            where.append(temp);
        }

        // BuySell
        s = (String) h.get(SantTradeBrowserReportTemplate.BUY_SELL);
        if (!Util.isEmpty(s)) {
            appendAND(where);
            if (s.equals("Buy")) {
                where.append(" trade.quantity>=0 ");
            } else if (s.equals("Sell")) {
                where.append(" trade.quantity<0 ");
            }
        }

        // Instrument
        s = (String) h.get(SantTradeBrowserReportTemplate.INSTRUMENT);
        if (!Util.isEmpty(s)) {
            appendAND(where);
            if (s.equals(CollateralStaticAttributes.INSTRUMENT_TYPE_REPO)) {
                where.append(" (product_desc.product_sub_type=").append(Util.string2SQLString(s));
                where.append(" OR product_desc.product_type='Repo') ");
            } else if (s.equals(CollateralStaticAttributes.INSTRUMENT_TYPE_SEC_LENDING)) {
                where.append(" (product_desc.product_sub_type=").append(Util.string2SQLString(s));
                where.append(" OR product_desc.product_type='SecLending') ");
            } else {
                where.append(" product_desc.product_sub_type=").append(Util.string2SQLString(s));
            }
        }

        // Trade Date
        String dateString = " trade.trade_date_time ";
        JDate date = getDate(TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS,
                TradeReportTemplate.START_TENOR);
        if (date != null) {
            JDatetime startOfDay = new JDatetime(date, h.getTimeZone());
            startOfDay = startOfDay.add(-1, 0, 1, 0, 0);
            appendAND(where);
            where.append(dateString);
            where.append(" >= ");
            where.append(Util.datetime2SQLString(startOfDay));
        }
        date = getDate(TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS, TradeReportTemplate.END_TENOR);
        if (date != null) {
            JDatetime endOfDay = new JDatetime(date, h.getTimeZone());
            endOfDay = endOfDay.add(0, 0, 0, 59, 999);
            appendAND(where);
            where.append(dateString);
            where.append(" <= ");
            where.append(Util.datetime2SQLString(endOfDay));
        }

        // Settlement Date
        dateString = " trade.settlement_date ";
        date = getDate(TradeReportTemplate.SETTLE_START_DATE, TradeReportTemplate.SETTLE_START_PLUS,
                TradeReportTemplate.SETTLE_START_TENOR);
        if (date != null) {
            appendAND(where);
            where.append(dateString);
            where.append(" >= ");
            where.append(Util.date2SQLString(date));
        }
        date = getDate(TradeReportTemplate.SETTLE_END_DATE, TradeReportTemplate.SETTLE_END_PLUS,
                TradeReportTemplate.SETTLE_END_TENOR);
        if (date != null) {
            appendAND(where);
            where.append(dateString);
            where.append(" <= ");
            where.append(Util.date2SQLString(date));
        }

        // Product Maturity Date
        dateString = " product_desc.maturity_date ";
        final JDate minDate = getDate(TradeReportTemplate.MATURITY_START_DATE, TradeReportTemplate.MATURITY_START_PLUS,
                TradeReportTemplate.MATURITY_START_TENOR);

        if (minDate != null) {
            appendAND(where);
            where.append(dateString);
            where.append(" >= ");
            where.append(Util.date2SQLString(minDate));
        }

        final JDate maxDate = getDate(TradeReportTemplate.MATURITY_END_DATE, TradeReportTemplate.MATURITY_END_PLUS,
                TradeReportTemplate.MATURITY_END_TENOR);
        if (maxDate != null) {
            appendAND(where);
            where.append(dateString);
            where.append(" <= ");
            where.append(Util.date2SQLString(maxDate));
        }

        s = (String) h.get(TradeReportTemplate.CPTYNAME);
        if (!Util.isEmpty(s)) {
            final Vector ids = Util.string2Vector(s);

            if (ids.size() > 0) {

                final int SQL_IN_ITEM_COUNT = 999;
                int start = 0;
                StringBuffer tmpCptyWhere = new StringBuffer();
                for (int i = 0; i <= (ids.size() / SQL_IN_ITEM_COUNT); i++) {
                    int end = (i + 1) * SQL_IN_ITEM_COUNT;
                    if (end > ids.size()) {
                        end = ids.size();
                    }

                    List subList = ids.subList(start, end);
                    if (subList.size() > 0) {
                        if (tmpCptyWhere.length() > 0) {
                            tmpCptyWhere.append(" OR ");
                        }
                        tmpCptyWhere.append(" trade.cpty_id IN (").append(Util.collectionToString(subList)).append(")");
                    }
                    start = end;
                }

                appendAND(where);
                where.append(" ( ").append(tmpCptyWhere).append(" )");
            }
        }

        // Book
        s = (String) h.get(TradeReportTemplate.BOOK);
        if (!Util.isEmpty(s)) {
            final Book b = BOCache.getBook(getDSConnection(), s);
            final int book_id = b.getId();
            appendAND(where);
            where.append(" trade.book_id=").append(book_id);
        }

        s = (String) h.get(SantTradeBrowserReportTemplate.PROCESSING_ORG_DEAL);
        if (!Util.isEmpty(s)) {
            final Vector ids = Util.string2Vector(s);

            appendAND(where);
            where.append(" trade.book_id = book.book_id AND");
            where.append(" book.legal_entity_id = legal_entity.legal_entity_id AND");
            where.append(" legal_entity.legal_entity_id IN (");
            where.append(Util.collectionToString(ids));
            where.append(")");
        }

        final String productTypesString = (String) this._reportTemplate.get(TradeReportTemplate.PRODUCT_TYPE);
        if (!Util.isEmpty(productTypesString)) {
            final Vector<String> productTypesVect = Util.string2Vector(productTypesString, ",");
            if ((productTypesVect != null) && (productTypesVect.size() > 0)) {
                appendAND(where);

                where.append(" trade.product_id=product_desc.product_id AND product_desc.product_type in ")
                        .append(Util.collectionToSQLString(productTypesVect));
            }
        }

        // Trade Status
        s = (String) this._reportTemplate.get(SantTradeBrowserReportTemplate.TRADE_STATUS);
        if (!Util.isEmpty(s)) {
            final Vector<String> tradeStatusVect = Util.string2Vector(s, ",");
            if ((tradeStatusVect != null) && (tradeStatusVect.size() > 0)) {
                appendAND(where);
                where.append(" trade.trade_status in ").append(Util.collectionToSQLString(tradeStatusVect));
            }
        }

        // Structure
        s = (String) this._reportTemplate.get(SantTradeBrowserReportTemplate.STRUCTURE);
        if (!Util.isEmpty(s)) {
            appendAND(where);
            // trade_keyword1.keyword_name
            where.append(" trade.trade_id=trade_keyword.trade_id " + "AND trade_keyword.keyword_name='"
                    + TRADE_KWD_STRUCTURE_ID + "' " + "AND trade_keyword.keyword_value='" + s + "' ");
        }

        // Extra filtering
        // we want to exclude if the trade is cancelled/matured and the updated
        // time is not between MTM from and TO
        JDatetime mtmFromDatetime = new JDatetime(getMTMValDateFrom(), 0, 0, 0,
                DSConnection.getDefault().getUserDefaults().getTimeZone());
        JDatetime mtmToDatetime = new JDatetime(getMTMValDateFrom(), 23, 59, 59,
                DSConnection.getDefault().getUserDefaults().getTimeZone());

        appendAND(where);
        where.append("( decode(trade.trade_status, 'CANCELED', 1, 'MATURED', 1,0)=0 OR update_date_time>");
        where.append(Util.datetime2SQLString(mtmFromDatetime));
        // where.append(" and ").append(Util.datetime2SQLString(mtmToDatetime));
        where.append(")");

        // Maturity Date check including Fee End Date
        // Exclude if both MTM From is after maturityDate
        appendAND(where);
        where.append(" ( product_desc.maturity_date is null OR product_desc.maturity_date>")
                .append(Util.datetime2SQLString(mtmFromDatetime)).append(")");

        // If the MTMToDate is before Trade Date then it will be excluded
        appendAND(where);
        where.append(" trade.trade_date_time<").append(Util.datetime2SQLString(mtmToDatetime));

        // "trunc(sysdate-1) and trunc(sysdate) )");

        return where.toString();
    }

    private void appendAND(StringBuffer where) {
        if (where.length() > 0) {
            where.append(" AND ");
        }
    }

    protected String buildFrom() {
        final ReportTemplate h = this._reportTemplate;
        final StringBuffer from = new StringBuffer();

        String str = (String) h.get(SantTradeBrowserReportTemplate.PROCESSING_ORG_DEAL);
        if (!Util.isEmpty(str)) {
            from.append("book,legal_entity");
        }

        str = (String) this._reportTemplate.get(SantTradeBrowserReportTemplate.STRUCTURE);
        if (!Util.isEmpty(str)) {
            if (from.length() > 0) {
                from.append(", ");
            }

            from.append(" trade_keyword ");
        }

        if (!this.countOnly) {
            final String productTypesString = (String) this._reportTemplate.get(TradeReportTemplate.PRODUCT_TYPE);
            if (!Util.isEmpty(productTypesString)) {
                if (from.length() > 0) {
                    from.append(", ");
                }

                from.append(" product_desc ");
            }

            if (from.length() > 0) {
                from.append(",");
            }

            from.append(" trade");
        }

        return from.toString();
    }


    public static void main(final String... args) throws ConnectException {
        ConnectionUtil.connect(args, "Test");

//		new SantTradeBrowserReport();

    }
}