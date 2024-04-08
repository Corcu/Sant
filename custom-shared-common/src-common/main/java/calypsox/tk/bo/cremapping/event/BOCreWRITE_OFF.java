package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;

import java.util.*;
import java.util.stream.Collectors;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreWRITE_OFF extends SantBOCre {
    private Product security;
    // private BOTransfer securityXfer;


    public BOCreWRITE_OFF(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurity(trade, creBoTransfer);
        //    this.securityXfer = getSecurityXfer(creBoTransfer);
    }
/*
    private BOTransfer getSecurityXfer(BOTransfer creBoTransfer) {
        try {
            if (creBoTransfer.getParentLongId() > 0) {
                BOTransfer parentXfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(creBoTransfer.getParentLongId());
                if (parentXfer != null && Status.isCanceled(parentXfer.getStatus())) {
                    Set<Long> seen = new HashSet<>();
                    while (Status.isCanceled(parentXfer.getStatus())
                            && parentXfer.getParentLongId() > 0
                            && creBoTransfer.getTransferType().equals(parentXfer.getTransferType())
                            && "Xfer Assigned".equals(creBoTransfer.getInternalSDStatus())) {
                        if (seen.contains(parentXfer.getParentLongId())) {
                            Log.error(this, String.format("Cyclical Parent Id dependency detected for %s, Cre %s, Parent Id %d..", creBoTransfer, boCre, parentXfer.getParentLongId()));
                            break;
                        }
                        long parentId = parentXfer.getParentLongId();
                        parentXfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(parentId);
                        if (parentXfer == null) {
                            Log.error(this, String.format("Parent security transfer not found by id %d, transfer %s,  Cre %s.", parentId, creBoTransfer, boCre));
                            break;
                }
                        seen.add(parentXfer.getParentLongId()); //make sure there is cyclical ref
                    }
                }
                return parentXfer;
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, String.format("Failed to get Parent SECURITY Transfer for %s, Cre %s.", creBoTransfer, boCre), e);

        }
        return null;
    }
*/
    @Override
    protected void fillValues() {
        this.settlementMethod = this.creBoTransfer != null ? this.creBoTransfer.getSettlementMethod() : "";
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);

        /*
         Workaround for an issue with trade amends.
         PartenonAccountingID is deleted and new requested, during the period between deletion and
         response with the new id, the trade has empty PartenonAccountingID
         */

        this.partenonId = getPartenonAccountingID(); //BOCreUtils.getInstance().loadPartenonId(this.trade);


        this.ownIssuance = security != null && ((Security) security).getIssuerId() == book.getProcessingOrgBasedId() ? "SI" : "NO";
        this.internal = "N";
        this.deliveryType = BOCreUtils.getInstance().loadDeliveryType(this.trade);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.issuerName = getIssuerExternalRef();
        this.productCurrency = security != null ? security.getCurrency() : trade != null ? BOCreUtils.getInstance().loadProductCurrency(this.trade) : null;
        this.accountingRule = BOCreUtils.getInstance().loadAccountingRule(this.boCre);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.nettingType = getNettingFlag();
        this.nettingParent = creBoTransfer == null
                ? 0
                : creBoTransfer.getNettedTransfer() ? creBoTransfer.getLongId() : creBoTransfer.getNettedTransferLongId();

    }

    private String getPartenonAccountingID() {
        String id = boCre.getAttributeValue("PartenonAccountingID");
        if (Util.isEmpty(id) || "null".equals(id)) {
            id = null != trade ? trade.getKeywordValue("PartenonAccountingID") : null;
            return Util.isEmpty(id)
                    ? (null != trade ? trade.getKeywordValue("OldPartenonAccountingID") : "")
                    : id;
        }
        return id;
    }

    private String getIssuerExternalRef() {
        if (security != null) {
            LegalEntity issuer = BOCache.getLegalEntity(DSConnection.getDefault(), ((Security) security).getIssuerId());
            return issuer != null ? issuer.getExternalRef() : null;
        }
        return null;
    }

    private String getNettingFlag() {
        return creBoTransfer.getNettedTransfer()
                ? BOCache.getNettingConfig(DSConnection.getDefault(), creBoTransfer.getNettingType()).containsKey("TradeId") ? "N" : "Y"
                : "N";
    }


    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected String loadOriginalEventType() {
        return creBoTransfer != null && Util.isTrue(creBoTransfer.getAttribute("Failed"), false)
                ? "RE" + this.boCre.getOriginalEventType()
                : this.boCre.getOriginalEventType();
    }


    @Override
    protected JDate getCancelationDate() {
        String originalEventType = super.loadOriginalEventType();
        if (originalEventType.startsWith("CANCELED_"))
            return boCre.getCreationDate().getJDate(TimeZone.getDefault());


        return null;
    }

    public CollateralConfig getContract() {
        return null;
    }

    @Override
    protected Account getAccount() {
        return creBoTransfer.getGLAccountNumber() > 0 ? BOCache.getAccount(DSConnection.getDefault(), creBoTransfer.getGLAccountNumber()) : null;
    }

    protected String getSubType() {
        if (null != this.trade) {
            if (trade.getProduct() instanceof Bond && Util.isTrue(trade.getKeywordValue("BondForward"), false)) {
                return "Cash";
            }
            return trade.getProduct().getSubType();
        }
        return "";
    }

    @Override
    protected String loadIdentifierIntraEOD() {
        return "INTRADAY";
    }

    @Override
    protected String loadSettlementMethod() {
        return getInstance().getSettleMethod(this.creBoTransfer);
    }

    @Override
    protected String loadProductType() {

        if (creBoTransfer != null && !"NONE".equals(creBoTransfer.getProductType()))
            return creBoTransfer.getProductType().startsWith("Bond") ? "Bond" : creBoTransfer.getProductType();

        if (trade == null && creBoTransfer != null && creBoTransfer.getNettedTransfer()) {
            try {
                TransferArray underlings = Util.isEmpty(creBoTransfer.getUnderlyingTransfers()) ?
                        DSConnection.getDefault().getRemoteBO().getNettedTransfers(creBoTransfer.getLongId()) : creBoTransfer.getUnderlyingTransfers();
                Optional<BOTransfer> first = underlings.stream().filter(x -> x != null && !Status.isCanceled(x.getStatus()) && !Status.S_SPLIT.equals(x.getStatus())).min((t1, t2) -> {
                    long l = t1.getLongId() - t2.getLongId();
                    return l < 0 ? -1 : l > 0 ? 1 : 0;
                });

                if (first.isPresent()) {
                    trade = DSConnection.getDefault().getRemoteTrade().getTrade(first.get().getTradeLongId());
                }

            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }

        }

        return (trade == null ? ""
                : trade.getProduct() instanceof Bond
                ? ("true".equalsIgnoreCase(trade.getKeywordValue("BondForward")) ? "BondForward" : "Bond")
                : trade.getProductType()
        );
    }

    @Override
    protected String loadBookName() {
        try {
            if (this.creBoTransfer != null && this.creBoTransfer.getNettedTransfer()) {
                HashMap<String, String> nettedConfig = BOCache.getNettingConfig(DSConnection.getDefault(), this.creBoTransfer.getNettingType());
                if (!nettedConfig.containsKey("Trade") && !nettedConfig.containsKey("Book")) {
                    TransferArray underlings = creBoTransfer.getUnderlyingTransfers();
                    if (Util.isEmpty(creBoTransfer.getUnderlyingTransfers())) {
                        underlings = DSConnection.getDefault().getRemoteBO().getNettedTransfers(creBoTransfer.getLongId());
                    }
                    if (underlings != null && !underlings.isEmpty()) {
                        int bookId = underlings.get(0).getBookId();
                        if (Arrays.stream(underlings.getTransfers()).anyMatch(t -> t != null && bookId != t.getBookId())) {

                            String defaultBook = getDefaultBook(Arrays.stream(underlings.getTransfers()).filter(Objects::nonNull).map(BOTransfer::getBookId).distinct().collect(Collectors.toList()));
                            if (!Util.isEmpty(defaultBook))
                                return defaultBook;

                        }
                    }
                }
                Book book = creBoTransfer != null ? BOCache.getBook(DSConnection.getDefault(), creBoTransfer.getBookId()) : trade != null ? trade.getBook() : null;
                if (book == null) {
                    Log.error(this, String.format("Book not found for CRe %s", boCre));
                    return null;
                }
                return book.getName();
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error getting underlings transfers  for netted transfer " + creBoTransfer.getLongId() + ".", e);

        }
        return super.loadBookName();
    }

    public static String getDefaultBook(List<Integer> bookIds) {
        Map<String, List<String>> mappings = new HashMap<>();
        bookIds.stream().distinct().forEach(id ->
                {
                    Book b = BOCache.getBook(DSConnection.getDefault(), id);
                    String accCentre = b.getAttribute("Centro OPContable GER");
                    String cde = accCentre != null && accCentre.length() > 3 ? accCentre.substring(accCentre.length() - 4) : null;
                    if (!Util.isEmpty(cde)) {
                        String mappedBook = LocalCache.getDomainValueComment(DSConnection.getDefault(), "AccountingCentreToBook", cde);
                        List<String> mappedBooks = mappings.computeIfAbsent(Util.isEmpty(mappedBook) ? "" : mappedBook, k -> new ArrayList<>());

                        mappedBooks.add(b.getName());
                    }
                }
        );

        if (mappings.keySet().size() == 1 && !mappings.containsKey(""))
            return mappings.keySet().iterator().next();

        List<String> noMapping = mappings.remove("");

        if (!Util.isEmpty(noMapping)) {
            noMapping.forEach(b -> Log.error(BOCreWRITE_OFF.class.getName(), String.format("No mapping for Book %s", b)));
        }
        if (mappings.keySet().size() > 1) {
            Log.error(BOCreWRITE_OFF.class.getName(), String.format("Transfer books are mapped for multiple default books %s", Util.collectionToString(mappings.keySet())));
        }
        return null;
    }
}