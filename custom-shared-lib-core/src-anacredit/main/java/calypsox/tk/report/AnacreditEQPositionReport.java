package calypsox.tk.report;

import calypsox.apps.reporting.AnacreditEQPositionReportTemplatePanel;
import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.processor.AnacreditProcessorEquityBase;
import calypsox.tk.anacredit.util.AnacreditMapper;
import calypsox.tk.anacredit.util.EquityTypeIdentifier;
import calypsox.tk.anacredit.util.EurBasedAmount;
import calypsox.tk.anacredit.util.positionkeeper.PositionKeeperPO;
import calypsox.tk.collateral.service.RemoteSantReportingService;
import calypsox.tk.core.SantanderUtil;
import calypsox.util.SantReportingUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.BOSecurityPositionReport;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static calypsox.tk.report.AnacreditOperacionesReportStyle.IMPORTE_LIQ_COMPRAS_MES;
import static calypsox.tk.report.AnacreditOperacionesReportStyle.IMPORTE_LIQ_VENTAS_MES;

public class AnacreditEQPositionReport extends Report implements  IAnacreditReport {

    public static final String DIRTY_PRICE_STR = "DirtyPrice";
    public static final String OFFICIAL = "OFFICIAL";

    public static final String PROPERTY_ACTIVOS_VALOR_RAZOBL = "PROPERTY_ACTIVOS_VALOR_RAZOBL";
    private static final long serialVersionUID = 1L;
    private static final String LOG_CAT = "AnacreditEQPositionReport";
    public static final String ANACREDIT_INVENTORY_RV_EQ = "ANACREDIT_INVENTORY_RV_EQ";

    public static final String PROPERTY_KEEPER_AMOUNT = "PROPERTY_KEEPER_Amount";
    public static final String PROPERTY_KEEPER_UNREALIZED = "PROPERTY_KEEPER_Unrealized";
    public static final String PROPERTY_KEEPER_NOMINAL = "PROPERTY_KEEPER_Nominal";
    public static final String PROPERTY_SALDO_DEUDOR_NOVENC = "PROPERTY_SALDO_DEUDOR_NOVENC";
    public static final String PROPERTY_TOTAL_BUY = "PROPERTY_TOTAL_BUY";
    public static final String PROPERTY_TOTAL_SELL = "PROPERTY_TOTAL_SELL";
    public static final String PROPERTY_TOTAL_DIVIDEND = "PROPERTY_TOTAL_DIVIDEND";
    public static final String PROPERTY_CLOSE_PRICE = "PROPERTY_CLOSE_PRICE";
    public static final String PROPERTY_OFFICIAL_PENV = "PROPERTY_OFFICIAL_PENV";
    public static final String PROPERTY_AGGREGO = "PROPERTY_AGGREGO";

    private HashMap<String, PricingEnv> pEnvs = new HashMap<String, PricingEnv>();


    private RemoteSantReportingService _reportingService = null;

    /**
     * Load Report Output Rows
     *
     * @param errorMsgs
     *
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ReportOutput load(final Vector errorMsgs) {
        StringBuilder error = new StringBuilder();
        try {
            errorMsgs.clear();
            return getReportOutput(errorMsgs);
        } catch (Exception e3) {
            e3.printStackTrace();
            error.append("Error generating Report.\n");
            error.append(e3.getLocalizedMessage());
            Log.error(this, e3);//Sonar
        }
        Log.error(this, error.toString());
        errorMsgs.add(error.toString());
        printLog(errorMsgs);
        return null;
    }

    /**
     * Generates report output by running
     * @param errorMsgs error messages
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ReportOutput getReportOutput(Vector errorMsgs) {

        try {
            String s = getReportTemplate().get(AnacreditConstants.ANACREDIT_EXTRACTION_TYPE);
            if (Util.isEmpty(s))   {
                errorMsgs.add("Extraction Type not selected.");
                return null;
            }
            loadPEnvs();

            List<ReportRow> rows = loadEquityPositionsFromInventory(errorMsgs);
            final ReportRow[] reportRows = rows.stream().map(ReportRow::new).toArray(ReportRow[]::new);
            DefaultReportOutput dro = new DefaultReportOutput(this);
            dro.setRows(reportRows);
            processRows(dro, rows, errorMsgs);
            printLog(errorMsgs);
            return dro;
        } catch (Exception e3) {
            Log.error(this, e3);//Sonar
        }
        return null;
    }

    private List<ReportRow> loadEquityPositionsFromInventory(Vector errorMsgs) throws CalypsoServiceException {
         List<ReportRow> inventoryRows = loadDataFromInventory(errorMsgs);
        inventoryRows = createNewAggregation(inventoryRows, errorMsgs);
        inventoryRows = filterEQNOTPositions(inventoryRows);
        inventoryRows = filterEQDESPositions(inventoryRows);
        inventoryRows = enrichFromPositionKeeper(inventoryRows, errorMsgs);
        return filterAndLoad(inventoryRows, errorMsgs);
    }

    private List<ReportRow> filterEQDESPositions(List<ReportRow> inventoryRows) {
        AnacreditEQAggregationService  service = new AnacreditEQAggregationService();
        inventoryRows = service.sumEQDESPositionSameISIN(inventoryRows);
        return inventoryRows;
    }

    private List<ReportRow> filterEQNOTPositions(List<ReportRow> inventoryRows) {
        AnacreditEQAggregationService  service = new AnacreditEQAggregationService();
        inventoryRows = service.filterEQNOTforIsinWithPositions(inventoryRows);
        return inventoryRows;
    }

    private List<ReportRow> enrichFromPositionKeeper(List<ReportRow> inventoryRows, Vector errorMsgs) {

        Log.debug(this,"#### Start to get info from Position Keeper ####");

        HashMap<String, HashMap<String, Object>> cachedPositions = new HashMap<String, HashMap<String, Object>>();
        HashSet<String> fails = new HashSet<>();
        inventoryRows.stream().forEach(reportRow -> {
           InventorySecurityPosition pos = reportRow.getProperty(ReportRow.INVENTORY);
           String key = pos.getBook().getLegalEntity().getId() +"_"+ pos.getProduct().getId();
           if (fails.contains(key)) {
               return;
           }
           if (!cachedPositions.containsKey(key)) {
                PositionKeeperPO keeper = new PositionKeeperPO();
                ArrayList<HashMap<String, Object>> positionKeeperContent = keeper.getPositionKeeperContent(this, pos.getProduct());
                if (positionKeeperContent != null
                        && !positionKeeperContent.isEmpty()
                        &&  null != positionKeeperContent.get(0)) {
                    cachedPositions.put(key, positionKeeperContent.get(0));
                }
                else {
                    errorMsgs.add("### ISIN with no Position in Position Keeper : " + pos.getProduct().getSecCode("ISIN"));
                    fails.add(key);
                    return;
                }
            }

            HashMap<String, Object> fieldValues = cachedPositions.get(key);
            if (fieldValues != null && !fieldValues.isEmpty()) {
                Amount amount = (Amount) fieldValues.get("Amount");
                Amount unrealized = (Amount) fieldValues.get("Unrealized");
                Amount nominal = (Amount) fieldValues.get("Nominal");
                reportRow.setProperty(PROPERTY_KEEPER_AMOUNT, amount);
                reportRow.setProperty(PROPERTY_KEEPER_UNREALIZED, unrealized);
                reportRow.setProperty(PROPERTY_KEEPER_NOMINAL, nominal);
            }

        });

        fails.stream().forEach(s -> Log.debug(this, "ISIN failed from Position Keeper."));

        return inventoryRows;
    }

    private List<ReportRow> loadDataFromInventory(Vector errorMsgs) throws CalypsoServiceException {
        DefaultReportOutput dro = executeInventoryReport(errorMsgs);
        List<ReportRow> resultows = extractRowsFromInventory(dro, errorMsgs);
        return resultows;
    }

    private List<ReportRow> extractRowsFromInventory(DefaultReportOutput dro, Vector<String> errorMsgs) {
        List<ReportRow> result = new ArrayList<ReportRow>();
        final com.calypso.tk.report.BOSecurityPositionReportStyle style = new com.calypso.tk.report.BOSecurityPositionReportStyle();

        Arrays.asList(dro.getRows()).stream().forEach(reportRow -> {

            try {
                InventorySecurityPosition invSec = reportRow.getProperty(ReportRow.INVENTORY);
                if (invSec != null) {
                    HashMap<JDate, Vector<Inventory>> positions = (HashMap)reportRow.getProperty("POSITIONS");
                    if (positions != null && positions.size() > 0) {
                        JDate firstPosDate = (JDate)positions.keySet().toArray()[0];
                        String firstPosDateString = formatSaldoDate(firstPosDate);

                        Amount saldo = (Amount)style.getColumnValue(reportRow, firstPosDateString, errorMsgs);
                        invSec.setTotalSecurity(saldo.get());

                        ReportRow out = new ReportRow(System.currentTimeMillis());
                        out.setProperty(ReportRow.VALUATION_DATETIME, getValuationDatetime());
                        out.setProperty(ReportRow.INVENTORY, invSec);
                        result.add(out);
                    }
                }

            }catch (Exception ex) {
                Log.error(this, ex);
            }
        });
        return result;

    }

    public String formatSaldoDate(JDate date) {
        String pattern = "dd-MMM-yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

        return simpleDateFormat.format(date.getDate()).toLowerCase();
    }

    protected DefaultReportOutput executeInventoryReport(Vector errorMsgs) throws CalypsoServiceException {
        Report report = getAndConfigureInventoryReport(errorMsgs);
        if (report == null) {
            errorMsgs.add("Error loading Inventory Report for Anacredit EQ Posiciones");
            return null;
        }
        Log.system(LOG_CAT, "### Loading Inventory positions ");

        DefaultReportOutput dro = report.load(errorMsgs);
        if (dro == null) {
            errorMsgs.add("Error executing Inventory Report ..");
            return null;
        }

        Log.system(LOG_CAT, "### Total positions loaded  : " + dro.getNumberOfRows());
        return dro;
    }

    protected Report getAndConfigureInventoryReport(Vector errorMsgs) throws CalypsoServiceException {
        Report report = getInventoryReport();
        String templateName = ANACREDIT_INVENTORY_RV_EQ;
        ReportTemplate template = getReportTemplate(report.getType(), templateName, errorMsgs);
        if (template == null) {
            throw new CalypsoServiceException("Error loading report template ");
        }
        report.setReportTemplate(template);
        configureTemplate(template);
        return report;
    }

    /**
     * Setup variables into Inventory Param template.
     * @param template
     */
    protected void configureTemplate(ReportTemplate template) {

        template.put("SEC_CODE", "ISIN");
        // first param overrides the one from Inventory is there any
        String isinParam =  getReportTemplate().get(AnacreditEQPositionReportTemplatePanel.SELECTED_ISIN);
        if (!Util.isEmpty(isinParam)) {
            template.put("SEC_CODE_VALUE", isinParam.trim());
        }
        template.callBeforeLoad();
    }

    private ReportTemplate getReportTemplate(String reportTYpe, String templateName, Vector errorMsgs) throws CalypsoServiceException {
        final ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
                .getReportTemplate(ReportTemplate.getReportName(reportTYpe), templateName);
        if (template == null) {
            Log.error(this, "Template " + templateName + "NOT Found.");
            errorMsgs.add("Error Loading template : " + templateName);
            return null;
        }
        return template;
    }

    private com.calypso.tk.report.BOSecurityPositionReport getInventoryReport() {
        final com.calypso.tk.report.BOSecurityPositionReport report = new BOSecurityPositionReport();
        report.setPricingEnv(getPricingEnv());
        report.setValuationDatetime(getValuationDatetime());
        return report;
    }

    protected void loadPEnvs() {
        pEnvs.put(OFFICIAL, AppUtil.loadPE(OFFICIAL, getValuationDatetime()));
        pEnvs.put(DIRTY_PRICE_STR, AppUtil.loadPE(DIRTY_PRICE_STR, getValuationDatetime()));
    }

    private List<ReportRow>  createNewAggregation(List<ReportRow> reportRows, Vector<String> errorMsgs) {
        AnacreditEQAggregationService service = new AnacreditEQAggregationService();
        List<ReportRow> result = service.doAggregation(reportRows, errorMsgs);
        return result;
    }

    @Override
    public ReportOutput buildReportOutputFrom(ReportRow[] items, Vector errorMsgs) {
        getReportTemplate().put(AnacreditConstants.USE_CACHED_ROWS, Boolean.TRUE);
        DefaultReportOutput output = new DefaultReportOutput(this);
        List<ReportRow> cachedItems = Arrays.asList(items);
        processRows(output, cachedItems, errorMsgs);
        return output;
    }

    protected void processRows(DefaultReportOutput output, List<ReportRow> reportRows, Vector errorMsgs) {
        AnacreditProcessorEquityBase processor = new AnacreditProcessorEquityBase();
        processor.processReportRows(this, output, reportRows, errorMsgs);
    }

    private List<ReportRow> filterAndLoad(List<ReportRow> reportRows, Vector errorMsgs) {
        List<ReportRow> filteredPositions = filterPositions(reportRows, errorMsgs);
        enrichBuysAndSells(filteredPositions);
        enrichCorporateActions(filteredPositions);
        enrichAndCalculate(filteredPositions);
        return filteredPositions;
    }


    private void enrichBuysAndSells(List<ReportRow> reportRows) {
        ConcurrentHashMap<String,List<BOTransfer>> xfersCache = new ConcurrentHashMap<>();
        reportRows.stream().parallel().forEach(reportRow -> {
            EquityTypeIdentifier identifier = new EquityTypeIdentifier(reportRow.getProperty(ReportRow.INVENTORY));
            if (identifier.isEQ() || identifier.isEQNOT()) {
                final String positionKey = Optional.ofNullable(reportRow.getProperty(PROPERTY_AGGREGO)).filter(String.class::isInstance).map(String.class::cast).orElse("");
                Double monthBuy = getMonthlyBuySell(xfersCache,identifier, positionKey,"SEC_RECEIPT");
                Double monthSell = getMonthlyBuySell(xfersCache,identifier,positionKey, "SEC_DELIVERY");
                reportRow.setProperty(IMPORTE_LIQ_COMPRAS_MES, monthBuy);
                reportRow.setProperty(IMPORTE_LIQ_VENTAS_MES, monthSell);
            }
        });
        /*
        reportRows.stream().forEach(reportRow -> {

            EquityTypeIdentifier identifier = new EquityTypeIdentifier(reportRow.getProperty(ReportRow.INVENTORY));
            if (identifier.isEQ() || identifier.isEQNOT()) {
                final Object property = reportRow.getProperty(PROPERTY_AGGREGO);
                //Double totalBuy = getAllBuySell(identifier, true);
                //Double totalSell = getAllBuySell(identifier, false);
                Double monthBuy = getMonthlyBuySell(identifier, true);
                Double monthSell = getMonthlyBuySell(identifier, false);
                //reportRow.setProperty(PROPERTY_TOTAL_BUY, totalBuy);
                //reportRow.setProperty(PROPERTY_TOTAL_SELL, totalSell);
                reportRow.setProperty(IMPORTE_LIQ_COMPRAS_MES, monthBuy);
                reportRow.setProperty(IMPORTE_LIQ_VENTAS_MES, monthSell);
            }
        });
         */
    }

    private Double getMonthlyBuySell(ConcurrentHashMap<String,List<BOTransfer>> xferCache, EquityTypeIdentifier identifier, String positionKey, String eventType) {
        String cachedPositionKey = "";
        Double amount = 0.00d;

        cachedPositionKey = Optional.of(positionKey).filter(s->s.length()>=19).map(k->k.substring(0,19).concat(eventType)).orElse("");

        StringBuilder SQLBuy = new StringBuilder();
        SQLBuy.append(" product_type in ('Equity','TransferAgent') ");
        SQLBuy.append(" and  transfer_type = 'SECURITY'  ");
        SQLBuy.append(" and  transfer_status in ('SETTLED', 'MIGRATED') ");
        SQLBuy.append(" and  netted_transfer = 0 ");
        SQLBuy.append(" and product_id = " + identifier.getUnderlying().getId() + " ");
        SQLBuy.append(" and int_le_id = " +  identifier.getBook().getLegalEntity().getId() + " ");
        SQLBuy.append(" and amount_ccy LIKE '" +  identifier.getCcy() + "' ");
        SQLBuy.append(" and  event_type = '"+eventType+"'  ");
        SQLBuy.append(getBetweenDates());

        try {
            if(xferCache.containsKey(cachedPositionKey)){
                final List<BOTransfer> boTransfers = xferCache.get(cachedPositionKey);
                for(BOTransfer xfer : boTransfers) {
                    String xferKey = generateKey(identifier, xfer);
                    if (!Util.isEmpty(positionKey) && positionKey.equalsIgnoreCase(xferKey)) {
                        if ("TransferAgent".equalsIgnoreCase(xfer.getProductType())) {
                            amount = amount + xfer.getNominalAmount();
                        } else {
                            amount = amount + xfer.getOtherAmount();
                        }
                    }
                }
            }else {
                final TransferArray boTransfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(SQLBuy.toString(), null);
                //Group and get OtherAmount
                for(BOTransfer xfer : boTransfers.getTransfers()){
                    String xferKey = generateKey(identifier,xfer);
                    if(!Util.isEmpty(positionKey) && positionKey.equalsIgnoreCase(xferKey)){
                        if("TransferAgent".equalsIgnoreCase(xfer.getProductType())){
                            amount=amount+xfer.getNominalAmount();
                        }else {
                            amount=amount+xfer.getOtherAmount();
                        }
                    }
                    String cachedKey = Optional.of(xferKey).filter(s->s.length()>=19).map(k->k.substring(0,19).concat(eventType)).orElse("");
                    if(xferCache.containsKey(cachedKey)){
                        final List<BOTransfer> cachedListXfers = xferCache.get(cachedKey);
                        if(!Util.isEmpty(cachedListXfers)){
                            cachedListXfers.add(xfer);
                            xferCache.put(cachedKey,cachedListXfers);
                        }
                    }else {
                        List<BOTransfer> xfers = new ArrayList<>();
                        xfers.add(xfer);
                        xferCache.put(cachedKey,xfers);
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading BOTransfer: " + e );
        }
        return amount;
    }

    //TODO REMOVE Original
    private Double getTotalBuySell(EquityTypeIdentifier identifier, boolean isBUY) {

        Double amount = 0.00d;
        StringBuilder SQLBuy =new StringBuilder(" select cast(sum(other_amount) as float) ");
        SQLBuy.append(" from bo_transfer t ");
        SQLBuy.append(" where t.product_type = 'Equity' ");
        SQLBuy.append(" and  t.transfer_type = 'SECURITY'  ");
        SQLBuy.append(" and  t.transfer_status in ('SETTLED', 'MIGRATED') ");
        SQLBuy.append(" and  t.netted_transfer = 1 ");
        SQLBuy.append(" and t.product_id = " + identifier.getUnderlying().getId() + " ");
        SQLBuy.append(" and int_le_id = " +  identifier.getBook().getLegalEntity().getId() + " ");
        SQLBuy.append(" and amount_ccy LIKE '" +  identifier.getCcy() + "' ");
        //Buy Security sell paper - search xfers Pay security get other amount
        if (!isBUY) {
            SQLBuy.append(" and  t.event_type = 'SEC_DELIVERY'  ");
            //SQLBuy.append("    and  t.payreceive_type = 'PAY' ");
        } else {
            SQLBuy.append(" and  t.event_type = 'SEC_RECEIPT'  ");
            //SQLBuy.append("    and  t.payreceive_type = 'RECEIVE' ");
        }
        final Vector<?> rawResultSet;
        try {
            rawResultSet = getReportingService().executeSelectSQL(SQLBuy.toString());
            if (rawResultSet.size() > 2) {
                final Vector<Vector<String>> result = SantanderUtil.getInstance().getDataFixedResultSetWithType(rawResultSet,
                        String.class);
                for (final Vector<String> v : result) {
                    if (v.get(0) != null) {
                        amount =  Double.parseDouble(v.get(0));;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }
        return amount;
    }


    //TODO TEST
    private Double getAllBuySell(EquityTypeIdentifier identifier, boolean isBUY) {
        String entidad = AnacreditMapper.getEntidadDepositaria(identifier);
        String  tipo_cartera_irfs9 = AnacreditMapper.getTipoCartera(identifier.getBook());
        String positionKey = entidad.concat(tipo_cartera_irfs9);
        Double amount = 0.00d;
        StringBuilder SQLBuy = new StringBuilder();
        SQLBuy.append(" product_type in ('Equity','TransferAgent') ");
        SQLBuy.append(" and  transfer_type = 'SECURITY'  ");
        SQLBuy.append(" and  transfer_status in ('SETTLED', 'MIGRATED') ");
        SQLBuy.append(" and  netted_transfer = 0 ");
        SQLBuy.append(" and product_id = " + identifier.getUnderlying().getId() + " ");
        SQLBuy.append(" and int_le_id = " +  identifier.getBook().getLegalEntity().getId() + " ");
        SQLBuy.append(" and amount_ccy LIKE '" +  identifier.getCcy() + "' ");
        if (!isBUY) {
            SQLBuy.append(" and  event_type = 'SEC_DELIVERY'  ");
        } else {
            SQLBuy.append(" and  event_type = 'SEC_RECEIPT'  ");
        }
        try {
            final TransferArray boTransfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(SQLBuy.toString(), null);
            //Group and get OtherAmount
            for(BOTransfer xfer : boTransfers.getTransfers()){
                final String xferEntidad = AnacreditMapper.getEntidadDepositaria(xfer);
                final String xferCartera = getXferBookTipoCartera(xfer.getBookId());
                String xferKey = xferEntidad.concat(xferCartera);
                if(positionKey.equalsIgnoreCase(xferKey)){
                    amount=amount+xfer.getOtherAmount();
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading BOTransfer: " + e );
        }
        return amount;
    }

    private String generateKey(EquityTypeIdentifier identifier, BOTransfer xfer){
        String entidad = "";
        String tipo_cartera_irfs9 = "";
        if(Optional.ofNullable(xfer).isPresent()){
            entidad = AnacreditMapper.getEntidadDepositaria(xfer);
            tipo_cartera_irfs9 = getXferBookTipoCartera(xfer.getBookId());
        }else {
             entidad = AnacreditMapper.getEntidadDepositaria(identifier);
             tipo_cartera_irfs9 = AnacreditMapper.getTipoCartera(identifier.getBook());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(identifier.getBook().getLegalEntity().getCode());
        sb.append(identifier.getISIN());
        sb.append(identifier.getCcy());
        sb.append(tipo_cartera_irfs9);
        sb.append(entidad);
        return sb.toString();
    }


    private String getXferBookTipoCartera(int bookid){
        String cod_dmn = "";
        try {
            final Book book = BOCache.getBook(DSConnection.getDefault(), bookid);
            if(null!=book){
                cod_dmn = LocalCache.getDomainValueComment(DSConnection.getDefault(), "ANACREDIT.Tipo_Cartera_IFRS9", book.getAccountingBook().getName());
                if(Util.isEmpty(cod_dmn)){
                    cod_dmn = "06_01";
                }
            }
        } catch (Exception e) {
           Log.error(this,"Error getting Acccounting Book for: " + bookid + " " +e);
        }
        return cod_dmn;
    }


    protected RemoteSantReportingService getReportingService() {
        if (_reportingService == null) {
            _reportingService = SantReportingUtil.getSantReportingService(DSConnection.getDefault());
        }
        return _reportingService;

    }

    private String getBetweenDates(){
        String dates = "";
        Calendar firstDayOfMonth = getValDate().asCalendar();
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, firstDayOfMonth.getActualMinimum(Calendar.DAY_OF_MONTH));

        Calendar lastDayOfMonth = getValDate().asCalendar();
        lastDayOfMonth.set(Calendar.DAY_OF_MONTH, lastDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        dates = " and trunc(settle_date) between " + Util.date2SQLString(firstDayOfMonth.getTime()) + " AND " + Util.date2SQLString(lastDayOfMonth.getTime());

        return dates;
    }


    private void enrichCorporateActions(List<ReportRow> reportRows) {

        JDate valDate = getValDate();
        Calendar cal = getValDate().asCalendar();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        JDate startDate = JDate.valueOf(cal.getTime());

        reportRows.stream().forEach(reportRow -> {

            Double totalDividend = 0.0d;

            EquityTypeIdentifier identifier = new EquityTypeIdentifier(reportRow.getProperty(ReportRow.INVENTORY));

            if (identifier.isEQPRF()) {
                //fixme put bind Variables here!
                StringBuilder sql = new StringBuilder();
                sql.append(" bo_transfer.event_type = 'RECEIPT' ");
                sql.append(" and bo_transfer.transfer_type = 'DIVIDEND' ");
                sql.append(" and bo_transfer.transfer_status  IN ('SETTLED', 'MIGRATED') ");
                sql.append(" and bo_transfer.netted_transfer = 0 ");
                sql.append(" and bo_transfer.amount_ccy = '" + identifier.getPosition().getSettleCurrency() + "' ");
                sql.append(" and bo_transfer.product_id = " + identifier.getUnderlying().getId() + " ");
                sql.append(" and bo_transfer.book_id = " + identifier.getBook().getId() + " ");
                sql.append(" and trunc(bo_transfer.value_date ) >= ");
                sql.append( Util.date2SQLString(startDate));
                sql.append(" and trunc(bo_transfer.value_date ) <= ");
                sql.append( Util.date2SQLString(valDate));

                try {
                    TransferArray xfers = DSConnection.getDefault().getRemoteBackOffice().getTransfers(null, sql.toString(), null);
                    for (BOTransfer xfer : xfers) {
                        totalDividend  += xfer.getOtherAmount();

                    }
                    reportRow.setProperty(PROPERTY_TOTAL_DIVIDEND, totalDividend);

                } catch (CalypsoServiceException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void enrichAndCalculate(List<ReportRow> reportRows) {
        PricingEnv pEnv = PricingEnv.loadPE("OFFICIAL", getValuationDatetime());
        reportRows.stream().forEach(reportRow -> {
            InventorySecurityPosition inventory = reportRow.getProperty(ReportRow.INVENTORY);
            if (inventory == null) {
                return;
            }

            EquityTypeIdentifier identifier = new EquityTypeIdentifier(inventory);

            if (identifier.isEQ() || identifier.isEQPRF()) {

                Double posNominal = identifier.getNominal();
                Amount nominalPO = reportRow.getProperty(PROPERTY_KEEPER_NOMINAL);
                Amount amount = reportRow.getProperty(PROPERTY_KEEPER_AMOUNT);
                Amount unrealized = reportRow.getProperty(PROPERTY_KEEPER_UNREALIZED);

                if ( (posNominal != null && !posNominal.isNaN())
                        && (nominalPO != null && !Double.isNaN(nominalPO.get()))
                        && unrealized != null && !Double.isNaN(unrealized.get())) {

                    Double reparto = (posNominal*100)/nominalPO.get();
                    if (reparto.isNaN() || reparto.isInfinite()) {
                        reparto = 1d;
                    }
                    Double dSaldoDeudorNoVenc = (amount.get()*reparto/100);
                    Double dActivosValozRazobl = (unrealized.get()*reparto/100);

                    EurBasedAmount saldoDeudorNovenc  =
                            new EurBasedAmount(inventory.getSettleCurrency(), dSaldoDeudorNoVenc).invoke(getValDate(), pEnvs.get(OFFICIAL));
                    reportRow.setProperty(PROPERTY_SALDO_DEUDOR_NOVENC, saldoDeudorNovenc);

                    EurBasedAmount activosValozRazobl  =
                            new EurBasedAmount(inventory.getSettleCurrency(), dActivosValozRazobl).invoke(getValDate(), pEnvs.get(OFFICIAL));
                    reportRow.setProperty(PROPERTY_ACTIVOS_VALOR_RAZOBL, activosValozRazobl);

                }

            }

            Double closePrice = CollateralUtilities.getDirtyPrice(
                    inventory.getProduct(), getValDate().addBusinessDays(1, getHolidays()), pEnv, getHolidays());
            reportRow.setProperty(PROPERTY_CLOSE_PRICE, closePrice);
            reportRow.setProperty(PROPERTY_OFFICIAL_PENV,  pEnv);

        });
    }

    private List<ReportRow> filterPositions(List<ReportRow> reportRows, Vector errorMsgs) {
        List<ReportRow> filteredPositions = filterEQPositions(reportRows, errorMsgs);
        filteredPositions = filterInvalidStaticData(filteredPositions, errorMsgs);
        return filteredPositions;
    }

    private List<ReportRow> filterEQPositions(List<ReportRow> reportRows, Vector errorMsgs) {
        ConcurrentLinkedQueue<ReportRow> syncList = new ConcurrentLinkedQueue<>();

        String s = getReportTemplate().get(AnacreditConstants.ANACREDIT_EXTRACTION_TYPE);
        List<String> extractionTypes = Util.stringToList(s);
        reportRows.stream().forEach(reportRow -> {
            InventorySecurityPosition position = reportRow.getProperty(ReportRow.INVENTORY);
            EquityTypeIdentifier identifier = new EquityTypeIdentifier(position);
            if ( (identifier.isEQ() && extractionTypes.contains(EquityTypeIdentifier.EQ))
                    || (identifier.isEQDES() && extractionTypes.contains(EquityTypeIdentifier.EQDES))
                    || (identifier.isEQPRF() && extractionTypes.contains(EquityTypeIdentifier.EQPRF))
                    || (identifier.isEQNOT() && extractionTypes.contains(EquityTypeIdentifier.EQNOT)) )  {
                syncList.add(reportRow);
                return;
            }

            errorMsgs.add("### Invalid Equity identification :" + identifier.getISIN());
        });

        List<ReportRow> result = new ArrayList<>();
        result.addAll(syncList);
        return result;
    }

    private List<ReportRow> filterInvalidStaticData(List<ReportRow> reportRows, Vector errorMsgs) {

        ConcurrentLinkedQueue<ReportRow> syncList = new ConcurrentLinkedQueue<>();
        reportRows.stream()
                .parallel().forEach(reportRow -> {
            InventorySecurityPosition position = reportRow.getProperty(ReportRow.INVENTORY);
            EquityTypeIdentifier identifier = new EquityTypeIdentifier(position);
            try {

                LegalEntity productIssuer = identifier.getProductIssuer();
                if (productIssuer == null)  {
                    errorMsgs.add("Invalid Product Issuer for ISIN : " +  identifier.getISIN());
                    return;
                }

                String jminIssuer = AnacreditMapper.getJMin(identifier.getProductIssuer());
                if (Util.isEmpty(jminIssuer))  {
                    errorMsgs.add("ISIN " + identifier.getISIN() + " Attribute JMINORISTA not found for ISSUER " + identifier.getProductIssuer().getCode());
                    return;
                }
                syncList.add(reportRow);

            } catch (Exception e) {
                Log.error(this, "Error processing Inventory position.", e);
            }

        });
        List<ReportRow> result = new ArrayList<>();
        result.addAll(syncList);
        return result;
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


    private void printLog(Vector<String> errorMsgs){
        if(!Util.isEmpty(errorMsgs)){
            for(String line : errorMsgs){
                Log.system("Anacredit: ",line.trim());
            }
        }
    }

    }
