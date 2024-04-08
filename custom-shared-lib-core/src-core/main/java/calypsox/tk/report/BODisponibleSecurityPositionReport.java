package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.bo.mapping.BODisponibleIssueTypeProductMapper;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author acd
 */
public class BODisponibleSecurityPositionReport extends BOSecurityPositionReport {
    final BODisponibleIssueTypeProductMapper issueTypeProductMapper = new BODisponibleIssueTypeProductMapper();
    static final String DEFAULT_BOOKS_FILTER = "DefaultBODisponibleExcludeBooks";
    static final String DEFAULT_ACCOUNTS_FILTER = "DefaultBODisponibleExcludeAccounts";
    static final String DEFAULT_ISIN_FILTER = "DefaultBODisponibleExcludeIsin";
    static final String INVENTORY = "Inventory";
    private Map <String, List<ReportRow>> aggregatedXfers;
    HashMap<String, List<ReportRow>> actualInvPositions;
    HashMap<String, List<ReportRow>> theoInvPositions;
    @Override
    public ReportOutput load(Vector errorMsgs) {
        final boolean calculateRealPosition = Util.isTrue(getReportTemplate().get("CalculateRealPosition"));

        DefaultReportOutput reportOutput = null;

        if(calculateRealPosition){
            Future<DefaultReportOutput> futureTransferReportOutput = executeTransferReport();
            reportOutput = (DefaultReportOutput) super.load(errorMsgs);
            processTransferReport(reportOutput,futureTransferReportOutput);
        }else {
            reportOutput = (DefaultReportOutput) super.load(errorMsgs);
        }

        processReportRow(reportOutput);

        return reportOutput;
    }


    /**
     * @param reportOutput
     */
    private void processReportRow(DefaultReportOutput reportOutput){
        final boolean unitSwiftFormat = isUnitSwiftFormat();
        Optional.ofNullable(reportOutput).map(DefaultReportOutput::getRows)
                .ifPresent(reportRows -> Arrays.stream(reportRows).parallel()
                        .forEach(row -> {
                            LegalEntity issuer = loadIssuer(row);
                            if(unitSwiftFormat){
                                row.setProperty(BODisponibleSecurityPositionReportStyle.POSITION_VALUE,getPositionValue(row));
                            }
                            String agentCode = Optional.ofNullable(row.getProperty(INVENTORY)).filter(InventorySecurityPosition.class::isInstance)
                                    .map(InventorySecurityPosition.class::cast).map(InventorySecurityPosition::getAgent).map(LegalEntity::getCode).orElse("");
                            int productId = Optional.ofNullable(row.getProperty(BODisponibleSecurityPositionReportStyle.INV_PRODUCT)).map(p -> (Product) p).map(Product::getId).orElse(0);
                            String issueType = getIssueType(row, issuer);
                            String productType = issueTypeProductMapper.getProductType(issueType);
                            String productSubType = issueTypeProductMapper.getProductSubtype(issueType);
                            String centerByAccountType = getCenterByAccountType(row);

                            //Key for matching with partenon contract from mic file
                            StringBuilder matchingKey = new StringBuilder();
                            matchingKey.append(productId).append(agentCode);
                            matchingKey.append(null!=issuer ? issuer.getCode() : "");
                            matchingKey.append(productType).append(productSubType);
                            matchingKey.append(centerByAccountType);

                            row.setProperty(BODisponibleSecurityPositionReportStyle.ISSUER,issuer);
                            row.setProperty(BODisponibleSecurityPositionReportStyle.JISSUER,getJIssuer(issuer));
                            row.setProperty(BODisponibleSecurityPositionReportStyle.ISSUE_TYPE,issueType);
                            row.setProperty(BODisponibleSecurityPositionReportStyle.PRODUCT,productType);
                            row.setProperty(BODisponibleSecurityPositionReportStyle.PRODUCT_SUBTYPE,productSubType);
                            row.setProperty(BODisponibleSecurityPositionReportStyle.PARTENON_MATCHING_KEY,matchingKey.toString());
                            row.setProperty(BODisponibleSecurityPositionReportStyle.CENTRO_BY_ACCOUNT_TYPE,centerByAccountType);

                        }));

        mergePositionIncludingCenter(reportOutput);
        setRowNumber(reportOutput);
    }

    /**
     * Merge position with original key and CENTRO.
     * Only can merge by 1 Day tenor range.
     * @param reportOutput
     */
    private void mergePositionIncludingCenter(DefaultReportOutput reportOutput) {
        boolean mergeByCenter = Util.isTrue(getReportTemplate().get("MergeByCenter"));

        if (null!=reportOutput && !Util.isEmpty(reportOutput.getRows()) && mergeByCenter) {
            if (isOneDayTenorRange(reportOutput)) {
                Map<String, List<ReportRow>> rowsByCenter = groupRowsByCenter(reportOutput);

                List<ReportRow> finalReportRowList = calculateInventoryAndCreateMergedRows(rowsByCenter);

                reportOutput.setRows(finalReportRowList.toArray(new ReportRow[0]));
            } else {
                AppUtil.displayError(this.getReportPanel(), " 'Merge By Centro' only can work with 1 Day tenor range.\n " +
                        "Please uncheck or select just 1 Day tenor range.");
            }
        }
    }

    private boolean isOneDayTenorRange(DefaultReportOutput reportOutput) {
        return Optional.ofNullable(reportOutput)
                .map(DefaultReportOutput::getRows).flatMap(reportRows -> Arrays.stream(reportRows).findFirst())
                .map(row -> row.getProperty("POSITIONS"))
                .filter(HashMap.class::isInstance)
                .map(HashMap.class::cast)
                .filter(map -> map.size() <= 1)
                .isPresent();
    }

    private Map<String, List<ReportRow>> groupRowsByCenter(DefaultReportOutput reportOutput) {
        Map<String, List<ReportRow>> rowsByCenter = new HashMap<>();

        Optional.ofNullable(reportOutput)
                .map(DefaultReportOutput::getRows)
                .ifPresent(reportRows ->
                        Arrays.stream(reportRows).forEach(row -> {
                            String center = row.getProperty(BODisponibleSecurityPositionReportStyle.CENTRO_BY_ACCOUNT_TYPE);
                            String bookId = Optional.ofNullable(row.getProperty("Book"))
                                    .filter(Book.class::isInstance)
                                    .map(Book.class::cast)
                                    .map(Book::getId)
                                    .orElse(0)
                                    .toString();
                            String keyToMerge = row.getUniqueKey().toString().replace(bookId, center);
                            rowsByCenter.computeIfAbsent(keyToMerge, k -> new ArrayList<>()).add(row);
                        })
                );
        return rowsByCenter;
    }

    private List<ReportRow> calculateInventoryAndCreateMergedRows(Map<String, List<ReportRow>> rowsByCenter) {
        List<ReportRow> finalReportRowList = new ArrayList<>();

        rowsByCenter.forEach((key, row) -> {
            List<ReportRow> theoreticalRows = new ArrayList<>(row);
            double inventory = theoreticalRows.stream()
                    .map(inventoryRow -> ((InventorySecurityPosition) inventoryRow.getProperty(INVENTORY)).getTotalSecurity())
                    .reduce(0.0, Double::sum);

            ((InventorySecurityPosition) theoreticalRows.get(0).getProperty(INVENTORY)).setTotalSecurity(inventory);
            finalReportRowList.add(theoreticalRows.get(0));
        });

        return finalReportRowList;
    }

    /**
     * Wait for transfer report and process lines
     * @param reportOutput default report output
     * @param futureTransferReportOutput future transfer report execution
     */
    private void processTransferReport(DefaultReportOutput reportOutput, Future<DefaultReportOutput> futureTransferReportOutput){
        try {
            DefaultReportOutput transferReportOutput = futureTransferReportOutput.get(1L, TimeUnit.DAYS);
            if(null!=transferReportOutput){
                final String aggregation = (String)getReportTemplate().get("AGGREGATION");
                createTransferGrouping(transferReportOutput, aggregation);
                calculateRealPosition(reportOutput,aggregation);
            }
        } catch (Exception e) {
            Log.error(this.getClass().getSimpleName(),"Error launching transfer report: " + e);
        } finally {
            futureTransferReportOutput.cancel(true);
        }
    }

    private String getCenterByAccountType(ReportRow row){
        String centro = "";
        Inventory inventory = Optional.ofNullable(row).map(r -> r.getProperty(ReportRow.INVENTORY))
                .map(Inventory.class::cast).orElse(null);
        Account account = Optional.ofNullable(inventory).map(Inventory::getAccount).orElse(new Account());

        boolean blocAccount = Optional.of(account).filter(acc -> "true".equalsIgnoreCase(acc.getAccountProperty("Bloqueo"))).isPresent() ||
                Optional.of(account).filter(acc -> "true".equalsIgnoreCase(acc.getAccountProperty("Pignoracion"))).isPresent();

        if(blocAccount){
            centro = Optional.ofNullable(inventory).map(Inventory::getBook)
                    .map(book -> book.getAttribute("Centro OPContable GER")).orElse("");
            centro = StringUtils.right(centro, 4);
        }else {
            Product product = Optional.ofNullable(row).map(r -> r.getProperty(BODisponibleSecurityPositionReportStyle.INV_PRODUCT)).filter(Product.class::isInstance).map(Product.class::cast).orElse(null);
            String poBook = Optional.ofNullable(inventory).map(Inventory::getBook).map(Book::getLegalEntity).map(LegalEntity::getCode).orElse("");
            centro = BOCreUtils.getInstance().getCentroContable(product, poBook, false);
        }
        return centro;

    }

    /**
     *
     * Add two new properties on theoretical position row, whit actual position,
     * and the list of not settled xfers loaded by the transfer report, to calculate Real position on ReportStyle side.
     *
     * @param reportOutput default report output
     * @param aggregation aggregation params
     */
    private void calculateRealPosition(DefaultReportOutput reportOutput, String aggregation) {
        if (reportOutput != null) {
            actualInvPositions = new HashMap<>();
            theoInvPositions = new HashMap<>();
            groupPositions(reportOutput, aggregation);

            theoInvPositions.forEach((key, row) -> {
                List<ReportRow> theorethicalRows = new ArrayList<>(row);
                validateRowsSize(theorethicalRows, "ERROR: Can only have one THEORETICAL line per grouping.");

                ReportRow theoreticalRow = theorethicalRows.get(0);
                processActualRow(key, theoreticalRow);
                processAggregatedXfers(key, theoreticalRow);
            });
        }
    }

    private boolean isUnitSwiftFormat(){
        return Optional.ofNullable(getReportTemplate().get("POSITION_VALUE")).map(Object::toString).filter("Nominal (Unit Swift Format)"::equalsIgnoreCase).isPresent();
    }

    /**
     * Add actual position row as a property.
     * @param key aggregation key
     * @param theoreticalRow theoretical position row
     */
    private void processActualRow(String key, ReportRow theoreticalRow) {
        if (actualInvPositions.containsKey(key) && theoreticalRow != null) {
            List<ReportRow> actualRows = new ArrayList<>(actualInvPositions.get(key));
            validateRowsSize(actualRows, "ERROR: Can only have one ACTUAL line per grouping.");

            ReportRow actualRow = actualRows.get(0);
            theoreticalRow.setProperty(BODisponibleSecurityPositionReportStyle.ACTUAL_ROW, actualRow);
        } else {
            Log.info(this.getClass().getSimpleName(), "No Actual Position Found for: " + key);
        }
    }

    /**
     * Add the sum of not settled transfer amounts.
     *
     * @param key aggregation key
     * @param theoreticalRow theoretical position row
     */
    private void processAggregatedXfers(String key, ReportRow theoreticalRow) {
        if (aggregatedXfers.containsKey(key) && theoreticalRow != null) {
            double xferNominal = aggregatedXfers.get(key).stream()
                    .map(transferRow -> (BOTransfer) transferRow.getProperty("BOTransfer"))
                    .mapToDouble(this::getTransferNominal).sum();
            theoreticalRow.setProperty(BODisponibleSecurityPositionReportStyle.REAL_POSITION, new Amount(xferNominal));
        }
    }


    private Double getTransferNominal(BOTransfer transfer) {
        Double amount = 1.0;
        if (transfer.getPayReceiveType().equals("PAY")) {
            amount = -1.0;
        }
        double moneyAmount = 0.0;
        if ("UnavailabilityTransfer".equals(transfer.getProductType())) {
            Product product = loadTransferProduct(transfer);
            if(null!=product){
                moneyAmount = amount * Math.abs(transfer.getUnavailabilityQuantity() * product.getPrincipal());
            }
        } else {
            moneyAmount = amount * Math.abs(transfer.getNominalAmount());
        }
        return moneyAmount;
    }


    private Product loadTransferProduct(BOTransfer transfer){
        if(null!=transfer){
            int productId = transfer.getProductId();
            try {
                Product product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), productId);
                if (product == null) {
                    product = DSConnection.getDefault().getRemoteProduct().getProduct(productId);
                }
                return product;
            } catch (Exception var4) {
                Log.error("ProductReportStyle", "Could not get product " + productId, var4);
            }
        }
        return null;
    }
    private void validateRowsSize(List<ReportRow> rows, String errorMsg) {
        if (rows.size() > 1) {
            Log.warn(this.getClass().getSimpleName(),errorMsg);
        }
    }

    /**
     *
     * @param reportOutput
     * @param aggregation
     */
    private void groupPositions(DefaultReportOutput reportOutput,String aggregation){
        Arrays.stream(reportOutput.getRows()).forEach(row -> {
            InventorySecurityPosition inventory = row.getProperty(INVENTORY);
            if(null!=inventory){
                String positionType = inventory.getPositionType();
                String inventoryKey = getInvKey(inventory, aggregation);

                if (InventorySecurityPosition.ACTUAL_TYPE.equalsIgnoreCase(positionType)) {
                    addRowToMap(actualInvPositions, inventoryKey, row);
                } else if (InventorySecurityPosition.THEORETICAL_TYPE.equalsIgnoreCase(positionType)) {
                    addRowToMap(theoInvPositions, inventoryKey, row);
                }
            }
        });
    }
    private void addRowToMap(Map<String, List<ReportRow>> map, String inventoryKey, ReportRow row) {
        map.computeIfAbsent(inventoryKey, k -> new ArrayList<>()).add(row);
    }

    /**
     * Create transfer aggregation by defined key
     *
     * @param xferReportOutput
     * @param aggregation
     */
    private void createTransferGrouping(DefaultReportOutput xferReportOutput, String aggregation){
       aggregatedXfers = Arrays.stream(xferReportOutput.getRows())
                .collect(Collectors.groupingBy(row -> getXferKey(row, aggregation)));
    }

    private String getXferKey(ReportRow row, String aggregation) {
        if(null!=row){
            try {
                TransferReportStyle style = new TransferReportStyle();
                BOTransfer boTransfer = (BOTransfer) row.getProperty("BOTransfer");
                if(null!=boTransfer){
                    String isin = style.getColumnValue(row, "Xfer_SecCode.ISIN", new Vector()).toString();
                    String agent = style.getColumnValue(row, "PO Agent", new Vector()).toString();
                    String accountId = String.valueOf(boTransfer.getGLAccountNumber()) ;
                    String bookId = String.valueOf(boTransfer.getBookId()) ;
                    return buildKey(isin, agent, accountId, bookId, aggregation);
                }
            }catch (Exception e){
                Log.error(this.getClass().getSimpleName(),"Error generating key: " + e.getMessage());
            }
        }
        return "";
    }

    private String getInvKey(InventorySecurityPosition inventory, String aggregation) {
        String isin = null!=inventory.getProduct() ? inventory.getProduct().getSecCode("ISIN") : "";
        String agent = null!=inventory.getAgent() ? inventory.getAgent().getCode() : "";
        String accountId = String.valueOf(inventory.getAccountId());
        String bookId = String.valueOf(inventory.getBookId());

        return buildKey(isin, agent, accountId, bookId, aggregation);
    }

    private String buildKey(String isin, String agent, String accountId, String bookId, String aggregation) {
        StringBuilder key = new StringBuilder();
        key.append(isin).append('/').append(agent);

        appendKeyPart(aggregation, "Account", accountId, key);
        appendKeyPart(aggregation, "Book", bookId, key);

        return key.toString();
    }

    private void appendKeyPart(String aggregation, String part, String value, StringBuilder key) {
        if (aggregation.contains(part)) {
            key.append('/').append(value);
        }
    }

    /**
     * Execute transfer report in parallel
     * @return TransferReport execution
     */
    private Future<DefaultReportOutput> executeTransferReport(){
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CompletionService<DefaultReportOutput> executionService = new ExecutorCompletionService<>(pool);
        TransferReportCallable transferReportCallable = new TransferReportCallable(getReportTemplate());
        return executionService.submit(transferReportCallable);
    }

    /**
     * @param row
     * @return Return "Quantity" if UNIT_SWIFT_FORMAT set as true and "Nominal" if not.  In case of RECON TLM reports (Nominal (Unfactored), and for IBRC Account return Nominal.
     *
     */
    private String getPositionValue(ReportRow row){
        Product security = row.getProperty("Product")!=null ? row.getProperty("Product") : row.getProperty("INV_PRODUCT");
        boolean isUnitSwiftFormat = Optional.ofNullable(security)
                .map(product -> product.getSecCode("UNIT_SWIFT_FORMAT"))
                .filter("true"::equalsIgnoreCase).isPresent();
        String reportTemplateName = Optional.ofNullable(row.getProperty("ReportTemplate")).map(ReportTemplate.class::cast).map(ReportTemplate::getTemplateName).orElse("");
        if(reportTemplateName.contains("RECON")){
            if(isIbrcAccount(row)){
                return "Nominal";
            }
            return isUnitSwiftFormat ? "Quantity" : "Nominal (Unfactored)";
        }else {
            return isUnitSwiftFormat ? "Quantity" : "Nominal";
        }
    }


    private boolean isIbrcAccount(ReportRow row){
        LegalEntity accountLegalEntity = Optional.ofNullable(row.getProperty("Inventory")).map(Inventory.class::cast).map(Inventory::getAccountLE).orElse(new LegalEntity());
        Collection<LegalEntityAttribute> legalEntityAttributes = Optional.ofNullable(accountLegalEntity).map(LegalEntity::getLegalEntityAttributes).orElse(Collections.emptyList());
        return !legalEntityAttributes.isEmpty() && legalEntityAttributes.stream()
                .filter(att -> "Data Source Scheme".equalsIgnoreCase(att.getAttributeType()))
                .map(LegalEntityAttribute::getAttributeValue)
                .anyMatch("IBRC"::equalsIgnoreCase);
    }

    /**
     * PRODUCTO	DESCRIPCIÓN	SUBTIPO	ISSUE_TYPE
     * 783	CEDULAS	520	RFCUSTCD
     * 420	LETRAS	520	RFCUSTLT
     * 482	BONOS	520	RFCUSTBO
     * 492	PAGARES	520	RFCUSTPG
     * 982	AUTOCARTERA BONOS	520	RFCUSTACBO
     * 983	AUTOCARTERA CÉDULAS	520	RFCUSTACCD
     *
     *-----------------------------------------------------------------------
     *
     * 923	GARANTÍAS (BLOQUEOS) BONOS	100	RFCUSTGRBO
     * 923	GARANTÍAS (BLOQUEOS) LETRAS	101	RFCUSTGRLT
     * 923	GARANTÍAS (BLOQUEOS) PAGARÉS	102	RFCUSTGRPG
     * 923	GARANTÍAS (BLOQUEOS) CEDULAS	103	RFCUSTGRCD
     * 924	PIGNORACIONES BONOS	100	RFCUSTPGBO
     * 924	PIGNORACIONES LETRAS	101	RFCUSTPGLT
     * 924	PIGNORACIONES PAGARÉS	102	RFCUSTPGPG
     * 924	PIGNORACIONES CEDULAS	103	RFCUSTPGCD
     *
     * @return
     */
    private String getIssueType(ReportRow row,LegalEntity issuer){
        Product product = row.getProperty(BOSecurityPositionReportStyle.INV_PRODUCT);
        Account account = Optional.ofNullable(row.getProperty(INVENTORY)).map(InventorySecurityPosition.class::cast).map(InventorySecurityPosition::getAccount).orElse(new Account());
        boolean bloqueo = false;
        boolean pignoracion = false;
        if(Optional.ofNullable(account).filter(acc -> "true".equalsIgnoreCase(acc.getAccountProperty("Bloqueo"))).isPresent()){
            bloqueo = true;
        }else if(Optional.ofNullable(account).filter(acc -> "true".equalsIgnoreCase(acc.getAccountProperty("Pignoracion"))).isPresent()){
            pignoracion = true;
        }

        if( product instanceof Security ){
            Security security = (Security) product;
            String isin = product.getSecCode("ISIN");
            boolean isAutocartera = null!=issuer && issuer.getCode().equals("BSTE");
            boolean isCedula = "Y".equalsIgnoreCase(product.getSecCode("IS COVERED"))
                    && (isin.startsWith("ES") || (isin.startsWith("XS")
                    && "SPAIN".equalsIgnoreCase(security.getCountry())));
            String secCodeIssueType = Optional.ofNullable(product.getSecCode("ISSUE_TYPE")).orElse("");

            if(bloqueo){
                if (isAutocartera) {
                    return isCedula ? "RFCUSTGRACCD" : "RFCUSTGRACBO";
                }
                if(isCedula){
                    return "RFCUSTGRCD";
                }
                switch (secCodeIssueType.toUpperCase()){
                    case "BO": return "RFCUSTGRBO";
                    case "LT": return "RFCUSTGRLT";
                    case "PG": return "RFCUSTGRPG";
                    default: return "";
                }
            }

            if(pignoracion){
                if (isAutocartera) {
                    return isCedula ? "RFCUSTPGACCD" : "RFCUSTPGACBO";
                }
                if(isCedula){
                    return "RFCUSTPGCD";
                }
                switch (secCodeIssueType.toUpperCase()){
                    case "BO": return "RFCUSTPGBO";
                    case "LT": return "RFCUSTPGLT";
                    case "PG": return "RFCUSTPGPG";
                    default: return "";
                }
            }

            if (isAutocartera) {
                return isCedula ? "RFCUSTACCD" : "RFCUSTACBO";
            }

            if (isCedula) {
                return "RFCUSTCD";
            }

            if (!Util.isEmpty(secCodeIssueType)){
                switch (secCodeIssueType.toUpperCase()){
                    case "BO": return "RFCUSTBO";
                    case "LT": return "RFCUSTLT";
                    case "PG": return "RFCUSTPG";
                    default: return "";
                }
            }
        }

        return "";
    }

    /**
     * @param issuer
     * @return external ref att from LegalEntity
     */
    public String getJIssuer(LegalEntity issuer) {
        return Optional.ofNullable(issuer).map(LegalEntity::getExternalRef).orElse("");
    }

    private LegalEntity loadIssuer(ReportRow row){
        Product product = row.getProperty(BOSecurityPositionReportStyle.INV_PRODUCT);
        if(product instanceof Security){
            return BOCache.getLegalEntity(DSConnection.getDefault(),((Security)product).getIssuerId());
        }
        return new LegalEntity();
    }

    private void setRowNumber(DefaultReportOutput reportOutput){
        AtomicInteger numbOfRow = new AtomicInteger(0);
        Optional.ofNullable(reportOutput).map(DefaultReportOutput::getRows)
                .ifPresent(reportRows -> Arrays.stream(reportRows).forEach(row -> {
                    row.setProperty(BODisponibleSecurityPositionReportStyle.ROW_NUMBER,numbOfRow.addAndGet(1));
                }));
    }

    @Override
    protected boolean buildWhere(StringBuffer where, StringBuffer from, String inventoryTable, Set<Integer> bookIds, Set<Integer> configIds, Set<Integer> productIds, List<CalypsoBindVariable> bindVariables) throws Exception {
        includeOnlyBloqAccounts();
        boolean b = super.buildWhere(where, from, inventoryTable, bookIds, configIds, productIds, bindVariables);
        List<String> defaultBODisponibleExcludeBooks = DomainValues.values(DEFAULT_BOOKS_FILTER);
        List<String> defaultBODisponibleExcludeAccounts = DomainValues.values(DEFAULT_ACCOUNTS_FILTER);
        List<String> defaultBODisponibleExcludeIsin = DomainValues.values(DEFAULT_ISIN_FILTER);


        String tableName = "inv_sec_balance";
        if ("Book/Agent/Account/CustAcc".equals(this._reportTemplate.get("AGGREGATION")) || "Agent/Account/CustAcc".equals(this._reportTemplate.get("AGGREGATION")) || "Agent/CustAcc".equals(this._reportTemplate.get("AGGREGATION"))) {
            tableName = "inv_cust_sec_balance";
        }

        excludeBloqAccounts(defaultBODisponibleExcludeAccounts);

        if(!Util.isEmpty(defaultBODisponibleExcludeIsin)){
            //Exclude Isin
            where.append(" AND ");
            Vector<ProductDescAndCode> allProductDescAndCode = DSConnection.getDefault().getRemoteProduct().getAllProductDescAndCode("product_sec_code.sec_code = 'ISIN' AND product_sec_code.CODE_VALUE IN "
                    + Util.collectionToSQLString(defaultBODisponibleExcludeIsin), 0, null);
            List<Integer> collect = allProductDescAndCode.stream().map(ProductDescAndCode::getId).collect(Collectors.toList());
            where.append("inv_sec_balance.SECURITY_ID NOT IN ").append(Util.collectionToSQLString(collect));
        }

        if(!Util.isEmpty(defaultBODisponibleExcludeBooks)){
            //Exclude Books
            where.append(" AND ");
            Vector<Book> booksFromBookNames = BOCache.getBooksFromBookNames(DSConnection.getDefault(), new Vector<>(defaultBODisponibleExcludeBooks));
            List<Integer> collect = booksFromBookNames.stream().map(Book::getId).collect(Collectors.toList());
            where.append("inv_sec_balance.book_id NOT IN ").append(Util.collectionToSQLString(collect));
        }

        if(!Util.isEmpty(defaultBODisponibleExcludeAccounts)){
            //Exclude Accounts
            ArrayList<CalypsoBindVariable> accountsBindVariable = new ArrayList<>();
            StringBuilder strBld = new StringBuilder();
            for (String accountName : defaultBODisponibleExcludeAccounts){
                accountsBindVariable.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, accountName));
                strBld.append("?,");
            }
            where.append(" AND ");
            Vector<Account> accounts = DSConnection.getDefault().getRemoteAccounting().getAccounts(" ACC_ACCOUNT_NAME IN (" + strBld.substring(0, strBld.length() - 1) + ")", accountsBindVariable);
            List<Long> accountIds = accounts.parallelStream().map(Account::getLongId).collect(Collectors.toList());
            where.append("inv_sec_balance.account_id NOT IN "+ Util.collectionToSQLString(accountIds));
        }

        String activated = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "BODisponibleFilterMatured");
        if(!Util.isEmpty(activated) && Boolean.parseBoolean(activated.toLowerCase()) && !Util.isEmpty(where.toString()) &&
                !where.toString().contains("(SELECT product_id FROM product_desc WHERE (maturity_date is null or maturity_date >= ? ))")){
            Object o = this._reportTemplate.get("INV_FILTER_MATURED");
            if (Util.isTrue(o, true)) {
                if (where.length() > 0) {
                    where.append(" AND ");
                }

                where.append("").append(tableName).append(".security_id IN (SELECT product_id FROM product_desc WHERE (maturity_date is null or maturity_date >= ? ))");
                bindVariables.add(new CalypsoBindVariable(3001, this.getPositionStartDate()));
            }
        }

        return b;
    }

    private void excludeBloqAccounts(List<String> defaultBODisponibleExcludeAccounts){
        final boolean excludeBloqAccounts = Util.isTrue(getReportTemplate().get("ExcludeBloqAccounts"));
        if(excludeBloqAccounts){
            List<Account> accounts = loadBloqAccounts();
            defaultBODisponibleExcludeAccounts.addAll(accounts.stream().map(Account::getName).collect(Collectors.toList()));
        }
    }

    private void includeOnlyBloqAccounts(){
        final boolean onlyBloqAccounts = Util.isTrue(getReportTemplate().get("OnlyBloqAccounts"));
        if(onlyBloqAccounts){
            List<Account> accounts = loadBloqAccounts();
            List<String> accountIds = accounts.stream().map(Account::getId).map(String::valueOf).collect(Collectors.toList());
            Attributes attributes = getReportTemplate().getAttributes();
            attributes.add("ACCOUNT_ID", Util.collectionToString(accountIds));
            getReportTemplate().setAttributes(attributes);
        }
    }

    private List<Account> loadBloqAccounts(){
        List<Account> acoounts = new ArrayList<>();
        try {
            Vector pignoracion = DSConnection.getDefault().getRemoteAccounting().getAccountByAttribute("Pignoracion", "True");
            Vector bloqueo = DSConnection.getDefault().getRemoteAccounting().getAccountByAttribute("Bloqueo", "True");
            acoounts.addAll(pignoracion);
            acoounts.addAll(bloqueo);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading Bloq Accounts: " + e.getMessage());
        }
        return acoounts;
    }


    /**
     * Callable to load TransferReport using BOPosition Not Settled Movements by default.
     * This class merge the content of the boPosition template with the transfer template
     * in order to load all the related transfers.
     */
    public static class TransferReportCallable implements Callable<DefaultReportOutput> {
        private ReportTemplate positionreportTemplate;

        public TransferReportCallable(ReportTemplate template) {
            this.positionreportTemplate = template;
        }
        public DefaultReportOutput call() throws Exception {

            TransferReport transferReport = new TransferReport();
            try {
                ReportTemplate transferTemplate = DSConnection.getDefault().getRemoteReferenceData()
                        .getReportTemplate(ReportTemplate.getReportName("Transfer"), "BOPosition Not Settled Movements");

                mergeTemplates(transferTemplate);

                transferTemplate.setValDate(new JDatetime().getJDate(TimeZone.getDefault()));
                transferReport.setReportTemplate(transferTemplate);
                return (DefaultReportOutput) transferReport.load(new Vector());

            } catch (CalypsoServiceException e) {
                Log.error(this.getClass().getSimpleName(),"Error: " + e);
            }

            return null;
        }

        private void mergeTemplates(ReportTemplate transferTemplate){
            if(null!=positionreportTemplate){
                Attributes positionAttributes = positionreportTemplate.getAttributes();
                Attributes attributes = transferTemplate.getAttributes();
                attributes.add("StartDate","");
                attributes.add("StartTenor","");
                attributes.add("StartPlus","");
                attributes.add("EndDate",positionAttributes.get("EndDate"));
                attributes.add("EndPlus",positionAttributes.get("EndPlus"));
                attributes.add("EndTenor",positionAttributes.get("EndTenor"));
                attributes.add("Book",positionAttributes.get("INV_POS_BOOKLIST"));
                attributes.add("PoAgent",positionAttributes.get("AGENT_ID"));
                attributes.add("SecCode",positionAttributes.get("SEC_CODE"));
                attributes.add("SecCodeValue",positionAttributes.get("SEC_CODE_VALUE"));
                Optional.ofNullable(positionAttributes.get("ACCOUNT_ID")).ifPresent(obj -> {
                    String glAccountsNames = getGlAccountsNames(String.valueOf(obj));
                    attributes.add("GLAccount",glAccountsNames);
                });
                transferTemplate.setAttributes(attributes);
            }

        }
        private String getGlAccountsNames(String accounts){
            try {
                if(accounts.contains(",")){
                    List<Integer> accountIdlist = Arrays.stream(accounts.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    Collection<Account> accountsFromIds = BOCache.getAccountsFromIds(DSConnection.getDefault(), accountIdlist, false);
                    return accountsFromIds.stream().map(Account::getName).collect(Collectors.joining(","));
                }else {
                    return Optional.ofNullable(BOCache.getAccount(DSConnection.getDefault(),Integer.parseInt(accounts))).map(Account::getName).orElse("");
                }
            } catch (NumberFormatException e) {
                Log.error(this.getClass().getSimpleName(),"Error: " + e.getMessage());
            }
            return "";
        }

    }


}
