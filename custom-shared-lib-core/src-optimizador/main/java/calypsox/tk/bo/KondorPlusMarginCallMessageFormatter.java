package calypsox.tk.bo;

import calypsox.tk.collateral.pdv.importer.PDVConstants;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.collateral.util.SantMarginCallUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.collateral.CashPosition;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.MarginCallPosition;
import com.calypso.tk.collateral.SecurityPosition;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TradeArray;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

// Project: Poland Online Enhancement

public class KondorPlusMarginCallMessageFormatter extends MessageFormatter {

    protected static final String ISIN = "ISIN";
    protected static final String VERIFIED = "VERIFIED";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    protected MarginCallEntryDTO entryDTO = null;
    protected MarginCallEntry entry = null;
    public static final String ALIAS_BOOK_K_NO_REHYP = "ALIAS_BOOK_K+_NO_REHYP";
    public static final String ALIAS_BOOK_K_REHYP = "ALIAS_BOOK_K+_REHYP";
    public static final String ALIAS_BOOK_EQUITY = "ALIAS_BOOK_EQUITY";
    public static final String ALIAS_BOOK_KONDOR = "ALIAS_BOOK_KONDOR";
    private static final String POINT_SEPARATOR = ".";
    private static final String COMMA_SEPARATOR = ",";
    public static final String ALIAS_KPLUS = "ALIAS_KPLUS";
    public static final String KONDOR_PLUS = "KondorPlus";
    public static final String CUSTODIAN = "CUSTODIAN";
    public static final String FATHER_FRONT_ID = "Father ID";
    public static final String MC_TRIPARTY = "MC_TRIPARTY";
    public static final String CLEARING_MODE = "CLEARING_MODE";

    //Reuso Accounts
    public static final String TK_FROM_TRIPARTY = "FromTripartyAllocation";
    public static final String TK_REVERSED_ALLOC = "ReversedAllocationTrade";
    public static final String TK_COLLATERAL_GIVER = "Collateral Giver";
    public static final String DV_REUSE_ACCOUNTS = "MT569_REUSE_ACCOUNTS";
    public static final String AD_B_TRIPARTY_REUSE_BOOK = "BOND_TRIPARTY_REUSE_BOOK";
    public static final String AD_E_TRIPARTY_REUSE_BOOK = "EQUITY_TRIPARTY_REUSE_BOOK";

    // This field is to avoid the calculation every time.
    protected String returnDealType = "";

    // MOVEMENT_TYPE
    private static final String IM = "IM";
    private static final String VM = "VM";
    private static final String CONTRACT_TYPE_CSD = "CSD";

    private static final String PO_SOVEREIGN = "SBWO";
    private static final String IM_CSD_TYPE = "IM_CSD_TYPE";

    /*
     * NOT USED private static final String ALT = "ALT"; private static final
     * String CAN = "CAN"; private static final String CANCELED = "CANCELED";
     */
    private static final String format = "dd/MM/yyyy";
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
            format);

    // Poland Online Enhancement
    private static final String DOMAIN_NAME_POLAND_POS = "MARGIN_CALL_INTERFACE_POLAND_POS";
    private static final String CONTRACT_TYPE_THIRDPARTY = "THIRDPARTY";
    private static final String CONTRACT_TYPE_TRIPARTY = "TRIPARTY";
    private static final String[] DEFAULT_POLAND_POS = {"1WBK"};
    // Poland Online Enhancement - End

    public KondorPlusMarginCallMessageFormatter() {
        super();
    }

    // DEAL_TYPE for Security & TYPE for Cash.
    @SuppressWarnings("rawtypes")
    public String parseDEAL_TYPE(BOMessage message, Trade trade, LEContact po,
                                 LEContact cp, Vector paramVector, BOTransfer transfer,
                                 DSConnection dsConn) {
        // First of all, we need to retrieve the Margin Call Contract related to
        // the Trade.
        try {
            MarginCall marginCall = (MarginCall) trade.getProduct();
            int mcId = Math.toIntExact(marginCall.getLinkedLongId());
            CollateralConfig marginCallConfig = CacheCollateralClient
                    .getCollateralConfig(dsConn, mcId);
            if (null != marginCallConfig) {
                if ("".equals(this.returnDealType)) {
                    prepareMarginCallEntry(message, dsConn, marginCallConfig,
                            trade.getTradeDate());
                    if (null != this.entry) {

                        // GSM: 28/10/14. For MMOO, we create directly the MC
                        // Cash o Security without allocations
                        // for this reason, checking the existance of
                        // allocations in the entry is nonsense.

                        // Get Cash allocations
                        // List<CashAllocation> cashAllocations =
                        // this.entry.getCashAllocations();
                        // // Get Securities allocations
                        // List<SecurityAllocation> securityAllocations =
                        // this.entry.getSecurityAllocations();

                        // We call the necessary methods to manage the cash or
                        // the security depending on the allocation
                        // vectors.
                        if (FALSE.equals(
                                parseIS_MARGINCALL_SECURITY(message, trade, po,
                                        cp, paramVector, transfer, dsConn))) { // &&
                            // !Util.isEmpty(cashAllocations))
                            // {
                            // cashPositionsPerCurrency(mcId, cashAllocations,
                            // message, dsConn);
                            cashPositionsPerCurrency(mcId, message, dsConn);
                        }
                        if (TRUE.equals(
                                parseIS_MARGINCALL_SECURITY(message, trade, po,
                                        cp, paramVector, transfer, dsConn))) { // &&
                            // !Util.isEmpty(securityAllocations))
                            // {
                            // securityPositionsPerCurrency(mcId,
                            // securityAllocations, message, dsConn);
                            securityPositionsPerCurrency(mcId, message, dsConn);
                        }
                    }
                }
            }

            // We return always the value that we saved in the variable
            // returnDealType.
            // System.out.println("DealType " + this.returnDealType);
            return this.returnDealType;
        } catch (Exception e) {
            Log.error(this, e);
        }

        return null;
    }

    // TRADE_BOOK for CASH.
    @SuppressWarnings("rawtypes")
    public String parseTRADE_BOOK_CASH(BOMessage message, Trade trade,
                                       LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                       DSConnection dsConn) {
        String bookInCalypso = super.parseTRADE_BOOK(message, trade, po, cp,
                paramVector, transfer, dsConn);
        if ((null != bookInCalypso) && !"".equals(bookInCalypso)) {
            // We check if the mapped is wrong for any reason.
            String bookReturned = CollateralUtilities
                    .getBookAliasMapped(bookInCalypso, ALIAS_BOOK_KONDOR);
            if ((null != bookReturned) && !"".equals(bookReturned)
                    && !bookReturned.startsWith("BOOK_WARNING")) {
                return bookReturned;
            } else {
                return bookInCalypso;
            }
        }

        return null;
    }

    private boolean isReversalTripartyTrade(Trade trade){
        java.util.Optional<Trade> tradeOpt=java.util.Optional.ofNullable(trade);
        return !Util.isEmpty(tradeOpt.map(t -> t.getKeywordValue(TK_REVERSED_ALLOC)).orElse(""));
    }

    private String processReversalTripartyTrade(Trade trade, DSConnection dsConn){
        String book="";
        if(isReversalTripartyTrade(trade)){
            Trade originalAllocationTrade = null;
            try {
                originalAllocationTrade = dsConn.getRemoteTrade().getTrade(Long.parseLong(trade.getKeywordValue(TK_REVERSED_ALLOC)));
            } catch (Exception e) {
                return book;
            }
            if(isReusoAccount(originalAllocationTrade,DSConnection.getDefault())){
                book=getAliasBook(originalAllocationTrade, dsConn);
            }
        }
        return book;
    }

    // TRADE_BOOK for SECURITY.
    @SuppressWarnings("rawtypes")
    public String parseTRADE_BOOK_SECURITY(BOMessage message, Trade trade,
                                           LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                           DSConnection dsConn) {
        if (trade.getProduct() != null && isReusoAccount(trade, dsConn)) {
            return getAliasBook(trade, dsConn);
        } else {
            String bookInCalypso = processReversalTripartyTrade(trade, dsConn);
            if (!Util.isEmpty(bookInCalypso)){
                return bookInCalypso;
            }
            bookInCalypso=  super.parseTRADE_BOOK(message, trade, po, cp,
                        paramVector, transfer, dsConn);

            String aliasForSearch = null;

            if ((null != bookInCalypso) && !"".equals(bookInCalypso)) {
                MarginCall marginCall = (MarginCall) trade.getProduct();
                if (null != marginCall) {
                    Product p = marginCall.getSecurity();
                    if (p != null) {

                        if (p instanceof Bond) {
                            try {
                                // We look at the rehypotecable mark in the
                                // contract.
                                if (parseCONTRACT_IS_REHYPOTHECABLE(message, trade,
                                        po, cp, paramVector, transfer, dsConn)
                                        .equals("1")) {
                                    aliasForSearch = ALIAS_BOOK_K_REHYP;
                                } else {
                                    aliasForSearch = ALIAS_BOOK_K_NO_REHYP;
                                }
                            } catch (Exception e) {
                                Log.error(this, e); //sonar
                            }

                        } else if (p instanceof Equity) {
                            try {
                                aliasForSearch = ALIAS_BOOK_EQUITY;
                            } catch (Exception e) {
                                Log.error(this, e); //sonar
                            }
                        }
                    }
                }

                // We look for the book in the alias included for K+.
                String bookReturned = CollateralUtilities
                        .getBookAliasMapped(bookInCalypso, aliasForSearch);
                if ((null != bookReturned) && !"".equals(bookReturned)
                        && !bookReturned.startsWith("BOOK_WARNING")) {
                    return bookReturned;
                } else {
                    return bookInCalypso;
                }
            }
        }

        return null;
    }


    private boolean isReusoAccount(Trade trade, DSConnection dsConn) {
        int direction = ((MarginCall) trade.getProduct()).getBuySell(trade); //MarginCall direction (<0):Pay / (>=0):Receive
        boolean tripartyAllocation = Boolean.valueOf(trade.getKeywordValue(TK_FROM_TRIPARTY));
        boolean reversedAllocation = Util.isEmpty(trade.getKeywordValue(TK_REVERSED_ALLOC));

        if (direction < 0.0 && tripartyAllocation && reversedAllocation) {
            String collateralGiver = trade.getKeywordValue(TK_COLLATERAL_GIVER);
            if (!Util.isEmpty(collateralGiver)) {
                try {
                    DomainValues.DomainValuesRow mt569_reuse_accounts = dsConn.getRemoteReferenceData().getDomainValuesRow(DV_REUSE_ACCOUNTS, collateralGiver);
                    if (null != mt569_reuse_accounts) {
                        return true;
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Cannot get DomainValue " + DV_REUSE_ACCOUNTS + " : " + collateralGiver + " Error: " + e);
                }
            }
        }
        return false;
    }


    private String getAliasBook(Trade trade, DSConnection dsConn) {
        String bookAlias = "";
        String alias;
        Product product = ((MarginCall) trade.getProduct()).getSecurity();
        MarginCallConfig contract = ((MarginCall) trade.getProduct()).getMarginCallConfig();

        if (null != contract && null != product) {
            if (product instanceof Bond) {
                alias = contract.getAdditionalField(AD_B_TRIPARTY_REUSE_BOOK);
                if (!Util.isEmpty(alias)) {
                    Book book = getBookFromAlias(alias, dsConn);
                    if (null != book) {
                        if (contract.isRehypothecable()) {
                            bookAlias = book.getAttribute(ALIAS_BOOK_K_REHYP);
                        } else {
                            bookAlias = book.getAttribute(ALIAS_BOOK_K_NO_REHYP);
                        }
                    }
                }
            } else if (product instanceof Equity) {
                alias = contract.getAdditionalField(AD_E_TRIPARTY_REUSE_BOOK);
                if (!Util.isEmpty(alias)) {
                    Book book = getBookFromAlias(alias, dsConn);
                    bookAlias = book.getAttribute(ALIAS_BOOK_EQUITY);
                }
            }
        }

        return bookAlias;
    }

    private Book getBookFromAlias(String alias, DSConnection dsConn) {
        Book book = null;
        try {
            book = dsConn.getRemoteReferenceData().getBook(alias);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Cannot load book: " + alias + " Error: " + e);
        }
        return book;
    }


    // INTERNAL_ID.
    @SuppressWarnings("rawtypes")
    public String parseMARGIN_CALL_ID(BOMessage message, Trade trade,
                                      LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                      DSConnection dsConn) {
        int marginCallId = 0;
        if (trade.getProductType().equals(MarginCall.MARGINCALL)) {
            MarginCall marginCall = (MarginCall) trade.getProduct();
            marginCallId = Math.toIntExact(marginCall.getLinkedLongId());
        }

        return "" + marginCallId;
    }

    // OWNER.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseTRADE_PROCESSING_ORGANIZATION(BOMessage message,
                                                     Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                                     BOTransfer transfer, DSConnection dsConn) {
        LegalEntity legalEnt = getProcessingOrg(trade, transfer, dsConn);
        if (null != legalEnt) {
            return legalEnt.getAuthName();
        }
        return null;
    }

    // TRADE_DATE.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseTRADE_DATE(BOMessage message, Trade trade, LEContact po,
                                  LEContact cp, Vector paramVector, BOTransfer transfer,
                                  DSConnection dsConn) {
        JDatetime jdatetime = trade.getTradeDate();
        JDate jdate = jdatetime.getJDate(TimeZone.getDefault());
        try {
            return formatDate(jdate);
        } catch (ParseException e) {
            Log.error(this, e); //sonar
        }
        return null;
    }

    // VALUE_DATE.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseSETTLE_DATE(BOMessage message, Trade trade, LEContact po,
                                   LEContact cp, Vector paramVector, BOTransfer transfer,
                                   DSConnection dsConn) {
        JDate jdate = trade.getSettleDate();
        try {
            return formatDate(jdate);
        } catch (ParseException e) {
            Log.error(this, e); //sonar
        }

        return null;
    }

    // MATURITY_DATE.
    @Override
    @SuppressWarnings("rawtypes")
    public String parsePRODUCT_MATURITYDATE(BOMessage message, Trade trade,
                                            LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                            DSConnection dsConn) {
        MarginCall marginCall = (MarginCall) trade.getProduct();

        if (null != marginCall) {
            Product p = marginCall.getSecurity();
            if (p != null) {
                if (p instanceof Bond) {
                    JDate jdate = p.getMaturityDate();
                    jdate = jdate.addBusinessDays(-1,
                            Util.string2Vector("SYSTEM"));
                    if (null != jdate) {
                        try {
                            return formatDate(jdate);
                        } catch (ParseException e) {
                            Log.error(this, e); //sonar
                        }
                    }

                } else if (p instanceof Equity) {
                    String date = "";
                    return date;
                }
            }
        }
        return null;
    }

    // BOND.
    @SuppressWarnings("rawtypes")
    public String parseISINCODE(BOMessage message, Trade trade, LEContact po,
                                LEContact cp, Vector paramVector, BOTransfer transfer,
                                DSConnection dsConn) {
        MarginCall marginCall = (MarginCall) trade.getProduct();
        if (null != marginCall) {
            return marginCall.getSecurity().getSecCode(ISIN);
        }

        return null;
    }

    // AMOUNT.
    @SuppressWarnings("rawtypes")
    public String parseAMOUNT(BOMessage message, Trade trade, LEContact po,
                              LEContact cp, Vector paramVector, BOTransfer transfer,
                              DSConnection dsConn) {
        MarginCall marginCall = (MarginCall) trade.getProduct();
        if (null != marginCall) {
            Product p = marginCall.getSecurity();
            if (p != null) {
                if (p instanceof Bond) {
                    try {
                        return formatNumber(Math.abs(trade.getQuantity())
                                * marginCall.getPrincipal());
                    } catch (ParseException e) {
                        Log.error(this, e); //sonar
                    }

                } else if (p instanceof Equity) {
                    try {
                        return formatNumber(Math.abs(trade.getQuantity()));
                    } catch (ParseException e) {
                        Log.error(this, e); //sonar
                    }
                }
            }
        }

        return null;
    }

    // BAU 6.1 - New field <mainAmount>
    // MAIN AMOUNT.
    @SuppressWarnings("rawtypes")
    public String parseMAIN_AMOUNT(BOMessage message, Trade trade, LEContact po,
                                   LEContact cp, Vector paramVector, BOTransfer transfer,
                                   DSConnection dsConn) {
        MarginCall marginCall = (MarginCall) trade.getProduct();
        if (null != marginCall) {
            Product p = marginCall.getSecurity();
            if (p != null) {
                if (p instanceof Bond) {
                    try {
                        return formatNumber(Math.abs(trade.getQuantity())
                                * marginCall.getPrincipal()
                                * ((Bond) p).getPoolFactor(trade.getTradeDate()
                                .getJDate(TimeZone.getDefault())));
                    } catch (ParseException e) {
                        Log.error(this, e); //sonar
                    }

                } else if (p instanceof Equity) {
                    try {
                        return formatNumber(Math.abs(trade.getQuantity())
                                * trade.getTradePrice());
                    } catch (ParseException e) {
                        Log.error(this, e); //sonar
                    }
                }
            }

        }

        return null;
    }

    // REHYPOTHECABLE.
    @SuppressWarnings("rawtypes")
    public String parseCONTRACT_IS_REHYPOTHECABLE(BOMessage message,
                                                  Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                                  BOTransfer transfer, DSConnection dsConn) {
        MarginCall marginCall = (MarginCall) trade.getProduct();
        if (null != marginCall) {
            MarginCallConfig marginCallConf = marginCall.getMarginCallConfig();
            if (null != marginCallConf) {
                if (marginCallConf.isRehypothecable()) {
                    return "1";
                }
            }
        }

        return "0";
    }

    // PRICE.
    @Override
    @SuppressWarnings("rawtypes")
    public String parsePRODUCT_PRINCIPAL(BOMessage message, Trade trade,
                                         LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                         DSConnection dsConn) {
        MarginCall marginCall = (MarginCall) trade.getProduct();
        if (null != marginCall) {
            Product product = (Product) marginCall.getSecurity();
            if (null != product) {
                if (product instanceof Bond) {
                    try {
                        return formatNumber(
                                (trade.getTradePrice() + trade.getAccrual())
                                        * 100);
                    } catch (ParseException e) {
                        Log.error(this, e); //sonar
                    }

                } else if (product instanceof Equity) {
                    try {
                        return formatNumber(trade.getTradePrice());
                    } catch (ParseException e) {
                        Log.error(this, e); //sonar
                    }
                }

            }
        }

        return null;
    }

    // USER
    @SuppressWarnings("rawtypes")
    public String parseUSER(BOMessage message, Trade trade, LEContact po,
                            LEContact cp, Vector paramVector, BOTransfer transfer,
                            DSConnection dsConn) {
        try {
            DomainValues kondorPlus = dsConn.getRemoteReferenceData()
                    .getDomains();
            String userKondor = kondorPlus.getDomainValueComment("KONDOR_PLUS",
                    "USER");
            if ((null != userKondor) && !"".equals(userKondor)) {
                return userKondor;
            }
        } catch (RemoteException e) {
            Log.error(this, e); //sonar
        }

        return super.parseTRADE_ENTEREDUSER(message, trade, po, cp, paramVector,
                transfer, dsConn);
    }

    // DEAL_TYPE for the Cash Transfers.
    @SuppressWarnings("rawtypes")
    public String parseDEAL_TYPE_CASH(BOMessage message, Trade trade,
                                      LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                      DSConnection dsConn) {
        // Reuse the method to calculate the TYPE for cash, using the returned
        // value to select the correct result.
        String type = parseDEAL_TYPE(message, trade, po, cp, paramVector,
                transfer, dsConn);
        if (null != type) {
            if (type.equals("0") || type.equals("1")) {
                return "D";
            } else if (type.equals("2") || type.equals("3")) {
                return "I";
            }
        }

        return null;
    }

    // TOMADA_PRESTADA.
    @SuppressWarnings("rawtypes")
    public String parseTOMADA_PRESTADA(BOMessage message, Trade trade,
                                       LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                       DSConnection dsConn) {
        // Reuse the method to calculate the TYPE for cash, using the returned
        // value to select the correct result.
        String type = parseDEAL_TYPE(message, trade, po, cp, paramVector,
                transfer, dsConn);
        if (null != type) {
            if (type.equals("0") || type.equals("3")) {
                return "T";
            } else if (type.equals("1") || type.equals("2")) {
                return "P";
            }
        }

        return null;
    }

    // ALIAS.
    @SuppressWarnings("rawtypes")
    public String parseALIAS(BOMessage message, Trade trade, LEContact po,
                             LEContact cp, Vector paramVector, BOTransfer transfer,
                             DSConnection dsConn) {
        String internalId = parseMARGIN_CALL_ID(message, trade, po, cp,
                paramVector, transfer, dsConn);
        String currency = parseTRADE_CURRENCY(message, trade, po, cp,
                paramVector, transfer, dsConn);
        String tomadaPrestada = parseTOMADA_PRESTADA(message, trade, po, cp,
                paramVector, transfer, dsConn);

        return internalId + "" + currency + "" + tomadaPrestada;
    }

    // TRIPARTY.
    @SuppressWarnings("rawtypes")
    public String parseTRIPARTY(BOMessage message, Trade trade, LEContact po,
                                LEContact cp, Vector paramVector, BOTransfer transfer,
                                DSConnection dsConn) {
        try {
            int mcId = Math.toIntExact(((MarginCall) trade.getProduct()).getLinkedLongId());
            MarginCallConfig marginCallConfig = dsConn.getRemoteReferenceData()
                    .getMarginCallConfig(mcId);
            if (null != marginCallConfig) {
                String mc_triparty = marginCallConfig
                        .getAdditionalField(MC_TRIPARTY);
                if (!"".equals(mc_triparty) && (null != mc_triparty)
                        && (mc_triparty.toLowerCase().startsWith("y")
                        || mc_triparty.toLowerCase().startsWith("t")
                        || mc_triparty.toLowerCase().startsWith("s"))) {
                    return "Y";
                } else {
                    return "N";
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e); //sonar
        }

        return null;
    }

    // FATHER_FRONT_ID.
    @SuppressWarnings("rawtypes")
    public String parseFATHER_FRONT_ID(BOMessage message, Trade trade,
                                       LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                       DSConnection dsConn) {
        String externalRefRepo = trade.getKeywordValue(FATHER_FRONT_ID);
        return externalRefRepo;
    }

    // CUSTODIAN.
    @SuppressWarnings("rawtypes")
    public String parseCUSTODIAN(BOMessage message, Trade trade, LEContact po,
                                 LEContact cp, Vector paramVector, BOTransfer transfer,
                                 DSConnection dsConn) {
        String custodian = "", custodianToReturn = "";
        Trade repoTrade = getRepoTrade(trade, dsConn);
        if (null != repoTrade) {
            custodian = repoTrade.getKeywordValue(CUSTODIAN);
            if ((null != custodian) && !"".equals(custodian)) {
                // Get from the system the LegalEntity associated with the GLCS
                // code above.
                try {
                    LegalEntity leAgent = dsConn.getRemoteReferenceData()
                            .getLegalEntity(custodian);
                    if ((null != leAgent) && !"".equals(leAgent)) {
                        Vector attributesAgent = dsConn.getRemoteReferenceData()
                                .getAttributes(leAgent.getId());
                        for (int numAtt = 0; numAtt < attributesAgent
                                .size(); numAtt++) {
                            LegalEntityAttribute legalEntityAtt = (LegalEntityAttribute) attributesAgent
                                    .get(numAtt);
                            if (ALIAS_KPLUS.equals(
                                    legalEntityAtt.getAttributeType())) {
                                custodianToReturn = legalEntityAtt
                                        .getAttributeValue();
                            }
                        }
                    }
                } catch (RemoteException e) {
                    Log.error(this, e);
                }
            }
        }

        return custodianToReturn;
    }

    // TRADE_SETTLEMENT_AMOUNT
    @Override
    @SuppressWarnings("rawtypes")
    public String parseTRADE_SETTLEMENT_AMOUNT(BOMessage message, Trade trade,
                                               LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                               DSConnection dsConn) {
        String tradeSettleAmount = super.parseTRADE_SETTLEMENT_AMOUNT(message,
                trade, po, cp, paramVector, transfer, dsConn);
        if ((null != tradeSettleAmount) && !"".equals(tradeSettleAmount)) {
            try {
                return formatNumber(Util.stringToNumber(tradeSettleAmount));
            } catch (ParseException e) {
                Log.error(this, e);
            }
        }

        return null;
    }

    /**
     * Method to check if the trade is kind of cash or transfer.
     *
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return The string "true" or "false" depending on the result of the
     * check.
     */
    @SuppressWarnings("rawtypes")
    public String parseIS_MARGINCALL_SECURITY(BOMessage message, Trade trade,
                                              LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                              DSConnection dsConn) {

        if (trade.getProductType().equals(MarginCall.MARGINCALL)) {
            MarginCall marginCall = (MarginCall) trade.getProduct();

            if (marginCall.getSecurity() != null) {
                return TRUE;
            }
        }

        return FALSE;
    }

    /**
     * This method gets the subAction for the message to generate, and if it's
     * CANCEL, we will return FALSE, else TRUE.
     *
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return A String 'true' or 'false' depending on the subAction for the
     * message to generate.
     */
    @SuppressWarnings("rawtypes")
    public String parseIS_NEW_TRADE(BOMessage message, Trade trade,
                                    LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                    DSConnection dsConn) {

        // We check if we have two messages to K+ in the same Trade.
        MessageArray msgArray;
        try {
            msgArray = dsConn.getRemoteBO().getMessages(trade.getLongId());
            if (null != msgArray) {
                for (int numMsg = 0; numMsg < msgArray.size(); numMsg++) {
                    BOMessage msg = msgArray.get(numMsg);
                    if ((null != msg) && (message.getLongId() != msg.getLongId())
                            && KONDOR_PLUS.equals(msg.getMessageType())
                            && "CANCELED".equals(msg.getStatus().getStatus())
                            && ("ACK".equals(msg.getAction().toString())
                            || (("NACK"
                            .equals(msg.getAction().toString()))
                            && msg.getStatus().toString()
                            .equals("WAITING_CANCEL")))) {
                        return FALSE;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }

        return TRUE;
    }

    /**
     * Method to check if the Repo is triparty or not.
     *
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return The string "true" or "false" depending on the result of the
     * check.
     */
    @SuppressWarnings("rawtypes")
    public String parseIS_TRIPARTY(BOMessage message, Trade trade, LEContact po,
                                   LEContact cp, Vector paramVector, BOTransfer transfer,
                                   DSConnection dsConn) {
        // We use the method to retrieve the additional field into the Margin
        // Call Contract.
        String triparty = parseTRIPARTY(message, trade, po, cp, paramVector,
                transfer, dsConn);
        if ((null != triparty) && "Y".equals(triparty)) {
            return TRUE;
        }

        return FALSE;
    }

    /**
     * Method to check if the contract has filled the additional field CLEARING
     * MODE
     *
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return The string "true" or "false" depending on the result of the
     * check.
     */
    @SuppressWarnings("rawtypes")
    public String parseCLEARING_OK(BOMessage message, Trade trade, LEContact po,
                                   LEContact cp, Vector paramVector, BOTransfer transfer,
                                   DSConnection dsConn) {

        String clearing = FALSE;
        try {
            int mcId = Math.toIntExact(((MarginCall) trade.getProduct()).getLinkedLongId());
            MarginCallConfig marginCallConfig = dsConn.getRemoteReferenceData()
                    .getMarginCallConfig(mcId);
            if (null != marginCallConfig) {
                if (!Util.isEmpty(
                        marginCallConfig.getAdditionalField(CLEARING_MODE))) {
                    clearing = TRUE;
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        } catch (Exception e) {
            Log.error(this, e);
        }
        return clearing;
    }

    /**
     * Method to check if the contract has filled the additional field CLEARING
     * MODE
     *
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return The string "true" or "false" depending on the result of the
     * check.
     */
    @SuppressWarnings("rawtypes")
    public String parseCUSTODIAN_CM(BOMessage message, Trade trade,
                                    LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                    DSConnection dsConn) {

        try {
            int mcId = Math.toIntExact(((MarginCall) trade.getProduct()).getLinkedLongId());
            MarginCallConfig marginCallConfig = dsConn.getRemoteReferenceData()
                    .getMarginCallConfig(mcId);
            if (null != marginCallConfig) {
                if (!Util.isEmpty(
                        marginCallConfig.getAdditionalField(CLEARING_MODE))) {
                    return marginCallConfig.getAdditionalField(CLEARING_MODE);
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }
        return "";
    }

    /**
     * Get the Repo Trade related to the Margin Call Trade generated.
     *
     * @param trade  Margin Call Trade generated.
     * @param dsConn Connection with the DataBase.
     * @return The Repo Trade retrieved.
     */
    private Trade getRepoTrade(Trade trade, DSConnection dsConn) {
        // We retrieve the keyword Father_Front_Id.
        String fatherFrontId = trade.getKeywordValue(FATHER_FRONT_ID);
        try {
            if ((null != fatherFrontId) && !"".equals(fatherFrontId)) {
                // Trade repoTrade =
                // dsConn.getRemoteTrade().getTrade(Integer.parseInt(fatherFrontId));
                TradeArray tradeArray = dsConn.getRemoteTrade()
                        .getTradesByExternalRef(fatherFrontId);
                if ((null != tradeArray) && !tradeArray.isEmpty()) {
                    for (int i = 0; i < tradeArray.size(); i++) {
                        Trade tradeWithFather = tradeArray.get(i);
                        if ((null != tradeWithFather) && "Repo"
                                .equals(tradeWithFather.getProductType())) {
                            // Status status = tradeWithFather.getStatus();
                            // if ((null != status) &&
                            // status.getStatus().getStatus().equals(Status.VERIFIED)) {
                            return tradeWithFather;
                            // }
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.error(this, e);
        } catch (RemoteException e) {
            Log.error(this, e);
        }

        return null;
    }

    /**
     * Format the number with the separator depending on the locale property.
     *
     * @param valueToFormat Value to format.
     * @return The number formatted.
     * @throws ParseException Exception if we cannot parse the number.
     */
    public static synchronized String formatNumber(double valueToFormat) throws ParseException {
        BigDecimal bigDecimal = new BigDecimal(valueToFormat);
        // FINDBUGS:
        // the call to the method toPlainString() returns an string. If the
        // result is not stored in a variable it does
        // anything. So we can save CPU if we don't call it
        // bigDecimal.toPlainString();
        DecimalFormat decimalFormat = new DecimalFormat("#0.0000#");
        decimalFormat.setDecimalSeparatorAlwaysShown(true);
        return decimalFormat.format(bigDecimal).replace(COMMA_SEPARATOR,
                POINT_SEPARATOR);
    }

    /**
     * Parse the JDate to String.
     *
     * @param valueToFormat Value to format.
     * @return The date formatted (converted to String).
     * @throws ParseException Exception if we cannot parse the date.
     */
    public static synchronized String formatDate(JDate valueToFormat) throws ParseException {
        return simpleDateFormat.format(valueToFormat.getDate(TimeZone.getDefault()));
    }

    /**
     * Extract the margin call entry for this message.
     *
     * @param message
     * @param dsConn
     * @param jDatetime
     */
    protected void prepareMarginCallEntry(BOMessage message, DSConnection dsConn,
                                          CollateralConfig marginCallConfig, JDatetime jDatetime) {
        if (this.entry == null) {
            prepareMarginCallEntryDTO(message, dsConn, marginCallConfig,
                    jDatetime);
            try {
                if (null != this.entryDTO) {
                    this.entry = SantMarginCallUtil.getMarginCallEntry(
                            ServiceRegistry.getDefault().getCollateralServer()
                                    .loadEntry(this.entryDTO.getId()),
                            marginCallConfig, false);
                }
            } catch (RemoteException e) {
                Log.error(this, e);
            }
        }
    }

    /**
     * Extract the margin call entry dto for this message.
     *
     * @param message
     * @param dsConn
     * @param jDatetime
     */
    private void prepareMarginCallEntryDTO(BOMessage message,
                                           DSConnection dsConn, CollateralConfig marginCallConfig,
                                           JDatetime jDatetime) {
        if (this.entryDTO == null) {
            List<MarginCallEntryDTO> entries;
            try {
                // AAP MIG 14.4
                entries = ServiceRegistry.getDefault().getCollateralServer()
                        .loadEntries(marginCallConfig.getId(),
                                jDatetime.getJDate(TimeZone.getDefault()),
                                jDatetime.getJDate(TimeZone.getDefault()),
                                TimeZone.getDefault(),
                                ServiceRegistry.getDefault()
                                        .getCollateralDataServer()
                                        .loadDefaultContext().getId());

                if (!Util.isEmpty(entries)) {
                    this.entryDTO = entries.get(0);
                }
            } catch (final RemoteException e) {
                Log.error(this, e);
            }
        }
    }

    /**
     * Distribute cash allocation per currency and calculation the previous cash
     * position for each currency
     *
     * @param mcId
     * @param message
     * @param dsConn
     */
    protected void cashPositionsPerCurrency(int mcId, BOMessage message,
                                            DSConnection dsConn) {
        Trade trade = null;
        String posCcy = new String(), movCcy = new String();
        JDatetime jdateTrade = null;
        Double positionPrev = 0.0, currentMovement = 0.0;

        // Get the previous position per currency
        List<MarginCallPosition> prevPos = this.entry.getPositions();

        try {
            // We get the Trade from the message, to avoid use
            // MarginCallAllocation getting the current movement.
            trade = dsConn.getRemoteTrade().getTrade(message.getTradeLongId());
            if (null != trade) {
                Product productTrade = trade.getProduct();
                if (null != productTrade) {
                    currentMovement = productTrade.getPrincipal();
                    movCcy = trade.getTradeCurrency();
                    // Rounding.
                    currentMovement = roundPositionValue(currentMovement,
                            movCcy);
                }

                // Set the JDate in the Trade.
                jdateTrade = trade.getTradeDate();
            }

            // Sum the previous balance to the calculated position.
            MarginCallPosition position = null;
            if (!Util.isEmpty(prevPos)) {
                // We find out the previous position for the currency in the
                // allocation.
                for (int numPos = 0; numPos < prevPos.size(); numPos++) {
                    position = prevPos.get(numPos);
                    if ((null != position) && (position instanceof CashPosition)
                            && position.getCurrency()
                            .equals(trade.getSettleCurrency())) {
                        positionPrev += position.getValue();
                        posCcy = position.getCurrency();
                        // Rounding.
                        positionPrev = roundPositionValue(positionPrev, posCcy);
                        break;
                    } else {
                        position = null;
                    }
                }
            }

            TradeArray existingMrgCallTrades = DSConnection.getDefault()
                    .getRemoteTrade().getTrades("trade, product_simplexfer",
                            "trade.product_id = product_simplexfer.product_id "
                                    + "and trade.trade_status not in ('CANCELED', 'ERROR', 'PENDING') "
                                    + "and trunc(trade.trade_date_time) = trunc("
                                    + Util.date2SQLString(jdateTrade) + ") "
                                    + "and product_simplexfer.linked_id = "
                                    + mcId
                                    + " and product_simplexfer.flow_type = 'COLLATERAL'",
                            null, null);

            if (null != existingMrgCallTrades) {
                // Loop to iterate in the correct position.
                for (Trade tradeAlloc : existingMrgCallTrades.getTrades()) {
                    if ((null != tradeAlloc)
                            && (tradeAlloc.getLongId() != trade.getLongId())
                            && trade.getSettleCurrency()
                            .equals(tradeAlloc.getSettleCurrency())
                            && !VERIFIED.equals(
                            tradeAlloc.getStatus().getStatus())) {
                        Product product = tradeAlloc.getProduct();
                        if (null != product) {
                            positionPrev += product.getPrincipal();
                            movCcy = tradeAlloc.getTradeCurrency();
                            // Rounding
                            positionPrev = roundPositionValue(positionPrev,
                                    movCcy);
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }

        // Call to the method to set the value for the deal type, send in the
        // message to K+.
        setReturnDealTypeVariable(positionPrev, currentMovement);
    }

    /**
     * Distribute security allocation per currency and calculation the previous
     * cash position for each currency
     *
     * @param mcId
     * @param message
     * @param dsConn
     */
    private void securityPositionsPerCurrency(int mcId, BOMessage message,
                                              DSConnection dsConn) {
        Trade trade = null;
        JDatetime jdateTrade = null;
        String description = new String(), posCcy = new String(),
                movCcy = new String();
        Double positionPrev = 0.0, currentMovement = 0.0;

        // Get the previous position per currency
        List<MarginCallPosition> prevPos = this.entry.getPositions();

        try {
            // We get the Trade from the message, to avoid use
            // MarginCallAllocation getting the current movement.
            trade = dsConn.getRemoteTrade().getTrade(message.getTradeLongId());
            if (null != trade) {
                MarginCall productMrgCall = (MarginCall) trade.getProduct();
                if (null != productMrgCall) {/*
                 * Bond bond = (Bond)
                 * productMrgCall.getSecurity(); if
                 * (null != bond) { currentMovement
                 * = trade.getQuantity() *
                 * bond.getFaceValue(); movCcy =
                 * trade.getTradeCurrency(); //
                 * Rounding. currentMovement =
                 * roundPositionValue(
                 * currentMovement, movCcy); // We
                 * concatenate the information for
                 * the description. description =
                 * bond.getSecCode(ISIN) + "_" +
                 * bond.getCurrency(); }
                 */
                    Product p = productMrgCall.getSecurity();
                    if (p != null) {
                        if (p instanceof Bond) {
                            currentMovement = trade.getQuantity()
                                    * ((Bond) p).getFaceValue();
                            movCcy = trade.getTradeCurrency();
                            // Rounding.
                            currentMovement = roundPositionValue(
                                    currentMovement, movCcy);
                            // We concatenate the information for the
                            // description.
                            description = ((Bond) p).getSecCode(ISIN) + "_"
                                    + ((Bond) p).getCurrency();

                        } else if (p instanceof Equity) {
                            currentMovement = trade.getQuantity();
                            movCcy = trade.getTradeCurrency();
                            // Rounding.
                            currentMovement = roundPositionValue(
                                    currentMovement, movCcy);
                            // We concatenate the information for the
                            // description.
                            description = ((Equity) p).getSecCode(ISIN) + "_"
                                    + ((Equity) p).getCurrency();

                        }
                    }

                }

                // Set the JDate in the Trade.
                jdateTrade = trade.getTradeDate();
            }

            // Sum the previous balance to the calculated position.
            MarginCallPosition position = null;
            if (!Util.isEmpty(prevPos)) {
                // We find out the previous position for the currency in the
                // allocation.
                for (int numPos = 0; numPos < prevPos.size(); numPos++) {
                    position = prevPos.get(numPos);
                    if ((null != position)
                            && (position instanceof SecurityPosition)) {
                        if (position.getCurrency()
                                .equals(trade.getSettleCurrency())
                                && position.getDescription()
                                .contains(description)) {
                            positionPrev += position.getValue();
                            posCcy = position.getCurrency();
                            // Rounding.
                            positionPrev = roundPositionValue(positionPrev,
                                    posCcy);
                            break;
                        }
                    } else {
                        position = null;
                    }
                }
            }

            TradeArray existingMrgCallTrades = DSConnection.getDefault()
                    .getRemoteTrade().getTrades("trade, product_simplexfer",
                            "trade.product_id = product_simplexfer.product_id "
                                    + "and trade.trade_status not in ('CANCELED', 'ERROR', 'PENDING') "
                                    + "and trunc(trade.trade_date_time) = trunc("
                                    + Util.date2SQLString(jdateTrade) + ") "
                                    + "and product_simplexfer.linked_id = "
                                    + mcId
                                    + " and product_simplexfer.flow_type = 'SECURITY'",
                            null, null);

            if (null != existingMrgCallTrades) {
                // Loop to iterate in the correct position.
                for (Trade tradeAlloc : existingMrgCallTrades.getTrades()) {
                    if ((null != tradeAlloc)
                            && (tradeAlloc.getLongId() != trade.getLongId())
                            && trade.getSettleCurrency()
                            .equals(tradeAlloc.getSettleCurrency())
                            && !VERIFIED.equals(
                            tradeAlloc.getStatus().getStatus())) {
                        MarginCall product = (MarginCall) tradeAlloc
                                .getProduct();
                        if (null != product) {
                            // new adrian
                            Product p = product.getSecurity();
                            if (p != null) {
                                if (p instanceof Bond) {
                                    if (description.equals(p.getSecCode(ISIN)
                                            + "_" + p.getCurrency())) {
                                        positionPrev += tradeAlloc.getQuantity()
                                                * ((Bond) p).getFaceValue();
                                        movCcy = tradeAlloc.getTradeCurrency();
                                        // Rounding
                                        positionPrev = roundPositionValue(
                                                positionPrev, movCcy);
                                    }

                                } else if (p instanceof Equity) {
                                    if (description.equals(p.getSecCode(ISIN)
                                            + "_" + p.getCurrency())) {
                                        positionPrev += tradeAlloc
                                                .getQuantity();
                                        movCcy = tradeAlloc.getTradeCurrency();
                                        // Rounding
                                        positionPrev = roundPositionValue(
                                                positionPrev, movCcy);
                                    }
                                }
                            }
                            /*
                             * Bond bondAlloc = (Bond) product.getSecurity(); if
                             * ((null != bondAlloc) &&
                             * description.equals(bondAlloc.getSecCode(ISIN) +
                             * "_" + bondAlloc.getCurrency())) { positionPrev +=
                             * tradeAlloc.getQuantity() *
                             * bondAlloc.getFaceValue(); movCcy =
                             * tradeAlloc.getTradeCurrency(); // Rounding
                             * positionPrev = roundPositionValue(positionPrev,
                             * movCcy); }
                             */
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }

        // Call to the method to set the value for the deal type, send in the
        // message to K+.
        setReturnDealTypeVariable(positionPrev, currentMovement);
    }

    /**
     * In this method we set the value in the variable returnDealType, used to
     * send it in the message to Kondor+.
     *
     * @param prevPosition    Previous Position.
     * @param currentMovement Current Movement.
     */
    protected void setReturnDealTypeVariable(double prevPosition,
                                             double currentMovement) {
        // We check the balance for the cash.
        if (prevPosition > 0.0) {
            if (currentMovement < 0.0) {
                this.returnDealType = "0";
            } else {
                this.returnDealType = "3";
            }
        } else if (prevPosition < 0.0) {
            if (currentMovement < 0.0) {
                this.returnDealType = "2";
            } else {
                this.returnDealType = "1";
            }
        } else {
            if (currentMovement < 0.0) {
                this.returnDealType = "2";
            } else {
                this.returnDealType = "3";
            }
        }
    }

    /**
     * @param value    value to round
     * @param currency get the rounding value from the currency
     * @return
     */
    protected double roundPositionValue(Double value, String currency) {
        return BigDecimal.valueOf(value)
                .setScale(CurrencyUtil.getRoundingUnit(currency),
                        BigDecimal.ROUND_HALF_UP)
                .doubleValue();
    }

    // public static void main(String[] args) {
    // DSConnection dsConDevCo4;
    // try {
    // dsConDevCo4 = ConnectionUtil.connect(args, "MainEntry");
    // Trade trade = dsConDevCo4.getRemoteTrade().getTrade(503465);
    // BOMessage message = dsConDevCo4.getRemoteBackOffice().getMessage(22579);
    //
    // KondorPlusMarginCallMessageFormatter formatter = new
    // KondorPlusMarginCallMessageFormatter();
    //
    // formatter.parseDEAL_TYPE_CASH(message, trade, null, null, null, null,
    // dsConDevCo4);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

    public String parseDELIVERY_TYPE(BOMessage message, Trade trade,
                                     LEContact po, LEContact cp,
                                     @SuppressWarnings("rawtypes") Vector paramVector,
                                     BOTransfer transfer, DSConnection dsConn) {
        String deliveryType = "";
        if (trade != null && !Util.isEmpty(
                trade.getKeywordValue(PDVConstants.DVP_FOP_TRADE_KEYWORD))) {
            deliveryType = PDVUtil.getDvpFopValue(
                    trade.getKeywordValue(PDVConstants.DVP_FOP_TRADE_KEYWORD));
        }
        return deliveryType;
    }

    // MOVEMENT_TYPE
    @SuppressWarnings("rawtypes")
    public String parseMOVEMENT_TYPE(BOMessage message, Trade trade,
                                     LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer,
                                     DSConnection dsConn) {
        String movementType = VM;
        if (trade.getProduct() instanceof MarginCall) {
            MarginCall marginCall = (MarginCall) trade.getProduct();
            if (null != marginCall) {
                MarginCallConfig marginCallConf = marginCall
                        .getMarginCallConfig();
                if (null != marginCallConf) {
                    if (CONTRACT_TYPE_CSD
                            .equals(marginCallConf.getContractType())) {
                        if (PO_SOVEREIGN.equals(
                                marginCallConf.getProcessingOrg().getCode())) {
                            return IM + "-" + marginCallConf
                                    .getAdditionalField(IM_CSD_TYPE);
                            // Poland Online Enhancement
                        } else if (isPolandPO(
                                marginCallConf.getProcessingOrg().getCode())) {
                            return getPolandMovementType(marginCallConf);
                            // Poland Online Enhancement - End
                        } else {
                            return IM;
                        }
                    } else {
                        return VM;
                    }
                }
            }
        }

        return movementType;
    }

    // Poland Online Enhancement

    /**
     * Tells if a given PO has to use the special Poland logic for field
     * &lt;movementType&gt;.
     *
     * @param shortName Short Name of the PO to check
     * @return <code>true</code> if this PO has to use Poland logic or
     * <code>false</code> otherwise.
     */
    private boolean isPolandPO(String shortName) {
        Vector<String> polandPOs = null;
        try {
            polandPOs = DSConnection.getDefault().getRemoteReferenceData()
                    .getDomainValues(DOMAIN_NAME_POLAND_POS);
        } catch (CalypsoServiceException e) {
            String message = String.format(
                    "Could not retrieve values from domain %s",
                    DOMAIN_NAME_POLAND_POS);
            Log.error(this, message, e);
        }

        if (polandPOs == null || polandPOs.size() == 0) {
            polandPOs = new Vector<String>();
            polandPOs.addAll(Arrays.asList(DEFAULT_POLAND_POS));
        }
        return polandPOs.contains(shortName);
    }

    /**
     * Gets the value for the &lt;movementType&gt; using Poland logic.
     *
     * @param marginCallConf Margin Call Contract that generated the message
     * @return Value of the field &lt;movementType&gt;
     */
    private String getPolandMovementType(MarginCallConfig marginCallConf) {
        String imCSDType = marginCallConf.getAdditionalField(IM_CSD_TYPE);
        String contractType = getContractType(marginCallConf);

        StringBuilder movementType = new StringBuilder();
        movementType.append(IM);
        movementType.append('-');
        movementType.append(imCSDType);
        movementType.append('-');
        movementType.append(contractType);

        return movementType.toString();
    }

    private String getContractType(MarginCallConfig marginCallConf) {
        String contractType = CONTRACT_TYPE_THIRDPARTY;
        if (isTriparty(marginCallConf)) {
            contractType = CONTRACT_TYPE_TRIPARTY;
        }

        return contractType;
    }

    private boolean isTriparty(MarginCallConfig marginCallConf) {
        CollateralConfig colConf = CacheCollateralClient.getCollateralConfig(
                DSConnection.getDefault(), marginCallConf.getId());

        return colConf.isTriParty();
    }
    // Poland Online Enhancement - End

}
