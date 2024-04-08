package calypsox.tk.report;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Pledge;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.*;
import com.calypso.tk.secfinance.SecFinanceTradeEntry;
import com.calypso.tk.secfinance.SecFinanceTradeEntryContext;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.fieldentry.FieldEntry;
import com.google.common.collect.Lists;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * @author acd
 */
public class RepoSMMDReport extends TradeReport {
    public static final String[] ACTION_AMND_REPORTABLE = { "mxContractEventICANCEL_REISSUE","mxContractEventRatesIACCOUNT_CLOSING"};
    public static final String[] ACTION_NO_REPORTABLE = { "mxContractEventIPORTFOLIO_MODIFICATION","mxContractEventIMODIFY_UDF","mxContractEventRatesIADDITIONAL_FLOW_AMENDMENT"};
    public static final String[] ACTIONS = { "REPRICE","RERATE","PARTIAL_RETURN"};
    public static final String[] ACTION_NEW_REPORTABLE = {"mxContractEventIRESTRUCTURE"};


    public static final String NEWT = "NEWT";
    public static final String AMND = "AMND";
    public static final String CANC = "CANC";
    public static final String ACCOUNT_CLOSING = "mxContractEventRatesIACCOUNT_CLOSING";


    JDatetime startDateTime = null;
    JDatetime endDateTime = null;

    @Override
    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput defaultReportOutput = null;
        ConcurrentLinkedQueue<ReportRow> finalRows = new ConcurrentLinkedQueue<>();
        ReportOutput load = super.load(errorMsgs);

        if (load instanceof DefaultReportOutput) {
            defaultReportOutput = (DefaultReportOutput) load;
            ReportRow[] rows = defaultReportOutput.getRows();
            JDate finalValDate = getReportValDate();;

            HashMap<Long, Trade> tradesById = getTradesbyId(rows);
            HashMap<Trade, List<AuditValue>> tradeAndAuditList = loadTradesAudit(tradesById);

            tradeAndAuditList.entrySet()
                    .stream().parallel().forEach(r -> {
                        Trade trade = r.getKey();
                List<AuditValue> auditValueList = r.getValue();

                Product product = trade.getProduct();

                if (acceptTrade(trade, product)) {
                    RepoSMMDBean bean = new RepoSMMDBean();
                    boolean acceptedStatus = checkAndSetStatus(bean,auditValueList,trade);
                    if(acceptedStatus){
                        bean.init(trade, product, finalValDate);
                        ReportRow row = new ReportRow(trade);
                        row.setProperty("Trade",trade);
                        row.setProperty("Default",trade);
                        row.setProperty("PricingEnv",getPricingEnv());
                        row.setProperty("ValuationDatetime",getValuationDatetime());
                        row.setProperty(RepoSMMDBean.class.getName(), bean);
                        finalRows.add(row);
                    }
                }
            });

            final ReportRow[] finalReportRows = finalRows.toArray(new ReportRow[finalRows.size()]);
            defaultReportOutput.setRows(finalReportRows);
        }

        return defaultReportOutput;
    }

    /**
     * @param trade
     * @param product
     * @return
     */
    private boolean acceptTrade(Trade trade, Product product) {
        boolean checkCpty = checkCpty(trade);
        boolean checkRepoType = true;
        boolean checkOpen = true;
        boolean isMoreThan = true;
        Product security = null;

        if(Optional.ofNullable(product).isPresent()){
            if(product instanceof Repo){
                Repo repo = (Repo)product;
                security = repo.getSecurity();
                checkRepoType = checkRepoType(repo); //Pledge true
                checkOpen = checkOpen(repo, trade); //Pledge true
                isMoreThan = isMoreThan(trade); //Pledge true
            }else if(product instanceof Pledge){
                Pledge pledge = (Pledge)product;
                security = pledge.getSecurity();
            }
        }
        boolean isGBIsin = isGBIsin(security);
        boolean checkSectorContable = checkSectorContable(trade);
        boolean mxTransferTo = checkMurexTransferTo(trade);

        return checkCpty && checkRepoType && checkOpen && isGBIsin && checkSectorContable && mxTransferTo && isMoreThan;
    }

    /**
     * Load audit between ProcessStartDate && ProcessEndDate and group by trade.
     *
     * @param tradesById
     * @return @{@link HashMap}
     */
    private HashMap<Trade,List<AuditValue>> loadTradesAudit(HashMap<Long, Trade> tradesById){
        HashMap<Trade,List<AuditValue>> tradesAndAudits = new HashMap<>();
        List<List<Long>> tradesSubList = new ArrayList<>();

        if(!Util.isEmpty(tradesById)){
            HashMap<Long,ConcurrentLinkedQueue<AuditValue>> idsAndAudits = new HashMap<>();
            if(null==startDateTime){
                startDateTime = getValuationDatetime().add(-1,0,0,0,0);
            }
            if(null==endDateTime){
                endDateTime = getValuationDatetime();
            }

            final String fromDate = jdateTime2SQLString((Date)startDateTime);
            final String toDate = jdateTime2SQLString((Date)endDateTime);
            String sModifDate = "modif_date >= " + fromDate + " AND modif_date <= " + toDate + " AND ";
            final String orderBy = " VERSION_NUM DESC";

            tradesSubList = generateSubLists(tradesById);

            for(List<Long> listOfTrades : tradesSubList){
                final String whereClauseConf = getWhere(sModifDate,listOfTrades);
                try {
                    final Vector<AuditValue> auditConf = DSConnection.getDefault().getRemoteTrade().getAudit(whereClauseConf, orderBy,null);

                    //GroupAuditByTradeId
                    auditConf.stream().forEach(auditValue -> {
                        long entityId = auditValue.getEntityId();
                        if(idsAndAudits.containsKey(entityId)){
                            ConcurrentLinkedQueue<AuditValue> auditValues = idsAndAudits.get(entityId);
                            auditValues.add(auditValue);
                            idsAndAudits.put(entityId,auditValues);
                        }else {
                            ConcurrentLinkedQueue<AuditValue> auditValues = new ConcurrentLinkedQueue<>();
                            auditValues.add(auditValue);
                            idsAndAudits.put(entityId,auditValues);
                        }
                    });

                    //GroupAuditByTrade
                    idsAndAudits.entrySet()
                            .stream().forEach(entry -> {
                                Long key = entry.getKey();
                                ConcurrentLinkedQueue<AuditValue> value1 = entry.getValue();
                                List<AuditValue> auditValues = Lists.newArrayList(value1.iterator());
                                Trade trade = tradesById.get(key);
                                tradesAndAudits.put(trade,auditValues);
                            });

                } catch (CalypsoServiceException e) {
                    Log.error(this, e);
                }
            }
        }

        return tradesAndAudits;
    }

    protected List<List<Long>> generateSubLists(HashMap<Long, Trade> tradesById){
        List<List<Long>> tradesSubList = new ArrayList<>();
        final List<Long> tradeIdsList = tradesById.keySet().stream().collect(Collectors.toList());
        final int SQL_IN_ITEM_COUNT = 999;
        int start = 0;
        for (int i = 0; i <= (tradeIdsList.size() / SQL_IN_ITEM_COUNT); i++) {
            int end = (i + 1) * SQL_IN_ITEM_COUNT;
            if (end > tradeIdsList.size()) {
                end = tradeIdsList.size();
            }
            final List<Long> subList = tradeIdsList.subList(start, end);
            start = end;
            tradesSubList.add(subList);
        }
        return tradesSubList;
    }

    @Override
    protected String buildQuery(boolean buildQueryForRepoOrSecLending, List<CalypsoBindVariable> bindVariables) {
        setDefaultBSTEPO();
        setDatesRanges();
        removeProcessDateRange();
        String where = super.buildQuery(buildQueryForRepoOrSecLending, bindVariables);
        where = addOtherPO(where, bindVariables);
        return where;
    }

    /**
     * Add Default PO for generate correct query
     */
    private void setDefaultBSTEPO() {
        ReportTemplate reportTemplate = getReportTemplate();
        if (null != reportTemplate) {
            Attributes attributes = reportTemplate.getAttributes();
            LegalEntity bste = BOCache.getLegalEntity(DSConnection.getDefault(), "BSTE");
            if (null != bste) {
                attributes.add("ProcessingOrg", String.valueOf(bste.getId()));
                reportTemplate.setAttributes(attributes);
            }
        }
    }

    /**
     * Add other PO for generate correct query BDSD
     * @param where
     * @param bindVariables
     * @return
     */
    private String addOtherPO(String where, List<CalypsoBindVariable> bindVariables) {
        LegalEntity bdsd = BOCache.getLegalEntity(DSConnection.getDefault(), "BDSD");
        CalypsoBindVariable id = new CalypsoBindVariable(4, null != bdsd ? bdsd.getId() : 0);
        if (where.contains("legal_entity_id IN (?)")) {
            where = where.replace("legal_entity_id IN (?)", "legal_entity_id IN (?,?)");
            bindVariables.add(id);
        }
        return where;
    }

    /**
     * Accept just ISIN on RepoSMMDIsin
     * @param security
     * @return
     */
    private boolean isGBIsin(Product security) {
        Vector<String> repoIsins = LocalCache.getDomainValues(DSConnection.getDefault(), "RepoSMMDIsin");
        if(!Util.isEmpty(repoIsins)){
            for(String isin : repoIsins){
                if(Optional.ofNullable(security)
                        .map(sec -> sec.getSecCode("ISIN")).filter(st -> st.startsWith(isin)).isPresent()){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Filter less nominal than 1000000.00
     * @param trade
     * @return
     */
    private boolean isMoreThan(Trade trade) {
        Product product = trade.getProduct();
        double latestPrincipalAmount = ((Repo) product).getCash().getLatestPrincipalAmount();
        //double nominal = ((Repo) product).getNominal(getValDate());
        return Math.abs(latestPrincipalAmount) >= 1000000.00;
    }

    /**
     * Filter by Repo Subtype, Standard or BSB
     *
     * @param repo
     * @return
     */
    private boolean checkRepoType(Repo repo) {
        List<String> repoTypes = Util.stringToList("Standard,BSB");
        return Optional.ofNullable(repo).map(Product::getSubType).filter(repoTypes::contains).isPresent();
    }


    /**
     * Exclude by Legal Entity Attribute SECTORCONTABLE and DV SectorContable
     *
     * @param trade
     * @return
     */
    private boolean checkSectorContable(Trade trade) {
        String sectorcontable = Optional.ofNullable(trade).map(t -> t.getKeywordValue("SECTORCONTABLE")).orElse("");
        Vector<String> sleepBookNames = LocalCache.getDomainValues(DSConnection.getDefault(), "RepoSMMDSectorContable");
        if (!Util.isEmpty(sleepBookNames)) {
            return !sleepBookNames.contains(sectorcontable);
        }
        return true;
    }

    /**
     * Filter by maturity time.
     *
     * @param repo
     * @param trade
     * @return
     */
    private boolean checkOpen(Repo repo, Trade trade) {
        if (null != repo) {
            List<String> maturityTypes = Util.stringToList("TERM,EXTENDIBLE,EVERGREEN");
            if (!repo.isOpen()) {
                String maturityType = repo.getMaturityType();
                if (maturityTypes.contains(maturityType)) {
                    SecFinanceTradeEntryContext context = new SecFinanceTradeEntryContext();
                    SecFinanceTradeEntry externalSecFinanceTradeEntry = SecFinanceTradeEntry.createSecFinanceTradeEntry(trade, new JDatetime(), null, context);
                    if (null != externalSecFinanceTradeEntry) {
                        Object duration = Optional.ofNullable(externalSecFinanceTradeEntry.get("Duration")).map(FieldEntry::getValue).orElse("");
                        return duration instanceof Integer && (Integer) duration <= 375;
                    }
                } else return true;
            }
            return true;
        }
        return false;
    }

    /**
     * Check CPTY and double book
     *
     * @param trade
     * @return
     */
    private boolean checkCpty(Trade trade) {
        List<String> counterPartyBlackList = Util.stringToList("BSTE,BSDD,BOUK");
        if (null != trade) {
            LegalEntity counterParty = trade.getCounterParty();
            if (null != counterParty && !counterPartyBlackList.contains(counterParty.getCode())) {
                Vector<LegalEntityAttribute> legalEntityAttributes = (Vector<LegalEntityAttribute>) counterParty.getLegalEntityAttributes();
                if (!Util.isEmpty(legalEntityAttributes)) {
                    return !legalEntityAttributes.stream().filter(att -> "INTRAGROUP".equalsIgnoreCase(att.getAttributeType()))
                            .map(LegalEntityAttribute::getAttributeValue).anyMatch(val -> "YES".equalsIgnoreCase(val) || "True".equalsIgnoreCase(val) || "S".equalsIgnoreCase(val)|| "Y".equalsIgnoreCase(val));
                }
            }
        }
        return true;
    }


    private boolean checkMurexTransferTo(Trade trade) {
        return !Optional.ofNullable(trade).map(t -> t.getKeywordValue("MurexTransferTo")).isPresent();
    }


    private boolean checkAndSetStatus(RepoSMMDBean bean,List<AuditValue> auditValueList, Trade trade){
        List<AuditValue> auditConf = auditValueList;
        JDatetime tradeEnteredDate = trade.getEnteredDate();
        JDate reportValDate = getValDate();
        bean.setEventDate(trade.getEnteredDate()); //Set Event Date if != NEW

        if(!Util.isEmpty(auditConf)) {
            JDatetime endDateTimeStart = endDateTime.getJDate(TimeZone.getDefault()).getJDatetime(TimeZone.getDefault());
            endDateTimeStart.setHours(0);
            endDateTimeStart.setMinutes(0);
            endDateTimeStart.setSeconds(0);

            List<AuditValue> auditOnePlus = auditConf.stream()
                    .filter(aud -> aud.getFieldName().equalsIgnoreCase("ADDKEY#MxLastEvent")
                            || aud.getFieldName().equalsIgnoreCase("MODKEY#MxLastEvent")
                            || (aud.getFieldName().equalsIgnoreCase("Product.__eventTypeActions")
                            && Arrays.asList(ACTIONS).contains(aud.getAction().toString())))
                    .collect(Collectors.toList());

            List<AuditValue> ptiModification = auditConf.stream()
                    .filter(aud -> aud.getFieldName().equalsIgnoreCase("ADDKEY#SMMDUpdatePTI")
                            || aud.getFieldName().equalsIgnoreCase("MODKEY#SMMDUpdatePTI"))
                    .collect(Collectors.toList());

            //1. Check terminated
            if(trade.getStatus().toString().equalsIgnoreCase(Status.TERMINATED)){
                if(!Util.isEmpty(auditOnePlus) && ACCOUNT_CLOSING.equalsIgnoreCase(trade.getKeywordValue("MxLastEvent"))){
                    final AuditValue lastAuditValue = auditOnePlus.get(0);
                    JDatetime modifDate = lastAuditValue.getModifDate();
                    if(tradeEnteredDate.getJDate(TimeZone.getDefault()).equals(modifDate.getJDate(TimeZone.getDefault()))){
                        bean.setStatus(NEWT);
                        bean.setEventDate(modifDate);
                        return true;
                    }else {
                        bean.setStatus(AMND);
                        checkPtiModifDate(bean,ptiModification,modifDate);
                        return true;
                    }
                }else if(!Util.isEmpty(trade.getKeywordValue("MxLastEvent"))){
                    bean.setStatus(NEWT);
                    return true;
                }else {
                    return false;
                }
            }

            //2. Check if enterd = valDate(proccesEndDate) (si fecha de creacion es igual a D se reporta como new excepto si la operacion es CANCEL)
            if(tradeEnteredDate.gte(endDateTimeStart) && tradeEnteredDate.lte(endDateTime)){
                if(!trade.getStatus().toString().equalsIgnoreCase(Status.CANCELED)){
                    bean.setStatus(NEWT);
                    return true;
                }else {
                    return false;
                }
            }

            //3. Check if have modif action diferent of enteredDate
            if(!Util.isEmpty(auditOnePlus)){
                AuditValue lastAuditValue = auditOnePlus.get(0);

                if(auditOnePlus.size()>1){
                    for(AuditValue prevAudit : auditOnePlus){
                        String eventToCheck = prevAudit.getField().getNewValue();
                        if(!Arrays.asList(ACTION_NO_REPORTABLE).contains(eventToCheck)
                                || Arrays.asList(ACTIONS).contains(prevAudit.getAction().toString())){
                            lastAuditValue = prevAudit;
                            break;
                        }
                    }
                }

                String lastEvent = lastAuditValue.getField().getNewValue();
                String action = lastAuditValue.getAction().toString();
                JDatetime modifDate = lastAuditValue.getModifDate();
                JDatetime startEndDateTime = startDateTime.getJDate(TimeZone.getDefault()).getJDatetime(TimeZone.getDefault());

                if(Arrays.asList(ACTION_NO_REPORTABLE).contains(lastEvent)){
                    if(!tradeEnteredDate.getJDate(TimeZone.getDefault()).equals(modifDate.getJDate(TimeZone.getDefault()))) {
                        return false;
                    }else {
                        bean.setStatus(NEWT);
                        bean.setEventDate(modifDate);
                        return true;
                    }
                }

                if(tradeEnteredDate.gte(startDateTime)
                        && tradeEnteredDate.lte(startEndDateTime)
                        && modifDate.gte(startDateTime)
                        && modifDate.lte(startEndDateTime)){
                    if(!action.equalsIgnoreCase(Action.CANCEL.toString())){
                        bean.setStatus(NEWT);
                        bean.setEventDate(modifDate);
                        return true;
                    }else {
                        return false;
                    }
                }

                if(Arrays.asList(ACTION_AMND_REPORTABLE).contains(lastEvent)){
                    bean.setStatus(AMND);
                    checkPtiModifDate(bean,ptiModification,modifDate);
                    return true;
                }else if(Arrays.asList(ACTIONS).contains(action) ||
                        Arrays.asList(ACTION_NEW_REPORTABLE).contains(lastEvent)){
                    bean.setStatus(NEWT);
                    bean.setEventDate(modifDate);
                    return true;
                }else if("CANCEL".equalsIgnoreCase(action)){
                    bean.setStatus(CANC);
                    checkPtiModifDate(bean,ptiModification,modifDate);
                    return true;
                }
            }
        }
        //Por defecto no se informa
        return false;
    }

    private void checkPtiModifDate(RepoSMMDBean bean,List<AuditValue> ptiModification, JDatetime modifDate){
        if(!Util.isEmpty(ptiModification)){
            AuditValue value = ptiModification.get(0);
            JDatetime ptiModifDate = value.getModifDate();
            if(ptiModifDate.getJDate(TimeZone.getDefault()).equals(modifDate.getJDate(TimeZone.getDefault()))){
                bean.setPtiUpdatedOnD(true);
                bean.setPtiOldValue(value.getField().getOldValue());
            }
        }
    }

    private String getWhere(String sModifDate, List<Long> tradesIds){
        String listofids = "";
        if(!Util.isEmpty(tradesIds)){
            listofids = tradesIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return sModifDate + " entity_class_name = 'Trade' " + " AND entity_id IN (" + listofids + ")"
                + " AND (audit_action LIKE '%NEW%' "
                + " OR audit_action  LIKE '%REPRICE%'"
                + " OR audit_action  LIKE '%RERATE%'"
                + " OR audit_action  LIKE '%PARTIAL_RETURN%'"
                + " OR audit_action  LIKE '%CANCEL%'"
                + " OR audit_action  LIKE '%TERMINATE%'"
                + " OR audit_action LIKE '%AMEND%')";
    }

    private void removeProcessDateRange(){
        ReportTemplate reportTemplate = getReportTemplate();
        if (null != reportTemplate) {
            Attributes attributes = reportTemplate.getAttributes();
            attributes.add("ProcessStartDate","");
            attributes.add("ProcessStartPlus","");
            attributes.add("ProcessStartTenor","");
            attributes.add("ProcessStratTime","");
            attributes.add("ProcessEndDate","");
            attributes.add("ProcessEndTime","");
            attributes.add("ProcessEndPlus","");
            attributes.add("ProcessEndTenor","");
            reportTemplate.setAttributes(attributes);
        }
    }

    private void setDatesRanges(){
        JDatetime startDate = this.getDateTime("ProcessStartDate", "ProcessStartPlus", "ProcessStartTenor", "ProcessStratTime", TimeZone.getDefault(), true);
        JDatetime endDate = this.getDateTime("ProcessEndDate", "ProcessEndPlus", "ProcessEndTenor", "ProcessEndTime", TimeZone.getDefault(), true);
        startDateTime = null!=startDate ? startDate : startDateTime;
        endDateTime = null!=endDate ? endDate : endDateTime;
    }

    private HashMap<Long,Trade> getTradesbyId(ReportRow[] rows){
        HashMap<Long,Trade> trades = new HashMap<>();
        Arrays.stream(rows).forEach(rw -> {
            Trade trade = (Trade)rw.getProperty("Trade");
            trades.put(trade.getLongId(),trade);
        });
        return trades;
    }

    private JDate getReportValDate(){
        JDate valDate = getProcessStartDate();
        if(null==valDate){
            valDate = getValDate();
        }
        return valDate;
    }

    private JDate getProcessStartDate() {
        JDate valDate = null;
        Object processStartDate = getReportTemplate().get("ProcessStartDate");
        if (processStartDate instanceof JDate) {
            valDate = (JDate) processStartDate;
        }
        return valDate;
    }

    private String jdateTime2SQLString(Date valDate){
        if (valDate == null) {
            return "null";
        } else {
            valDate = toReferenceTZ(valDate);
            Timestamp sqldate = new Timestamp(valDate.getTime());
            return "{ts '" + sqldate.toString() + "'}";
        }
    }

    public static Date toReferenceTZ(Date d) {
        TimeZone def = TimeZone.getDefault();
        GregorianCalendar calendar = new GregorianCalendar();
        synchronized(calendar) {
            GregorianCalendar cal = calendar;
            cal.setTimeZone(def);
            cal.setTime(d);
            return cal.getTime();
        }
    }
}
