package calypsox.tk.product.secfinance.triparty;

import calypsox.tk.util.SantTradeKeywordUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.product.Pledge;
import com.calypso.tk.product.secfinance.triparty.AllocationTradeListener;
import com.calypso.tk.product.secfinance.triparty.TripartyCollateralAllocation;
import com.calypso.tk.product.secfinance.triparty.TripartyTransaction;
import com.calypso.tk.refdata.Haircut;
import com.calypso.tk.refdata.Haircut.HaircutData;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.sql.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

public class CustomAllocationTradeListener implements AllocationTradeListener {

    private static final String HAIRCUT = "Haircut";
    private static final String DIRTY_PRICE = "DirtyPrice";
    private static final String SECURITY = "SECURITY";
    // TripartyAgent Haircut
    private static final String _92A_VAFC = "92A::VAFC";
    // TripartyAgent DirtyPrice
    private static final String _90A_MRKT = "90A::MRKT";
    // TripartyAgent StatementNumber
    private static final String _22F_SFRE = ":22F::SFRE//";

    private static final String _19A_MKTP = "19A::MKTP";
    private static final String _92B_EXCH = "92B::EXCH";
    private static final String _19A_COVA = "19A::COVA";

    private static final Object LOCALE = "LOCALE";
    private static final String COLON = ":";

    // Values for Triparty Multipage methods

    public static final String KEYWORD_PREP = "PREP";
    public static final String KEYWORD_VALN = "VALN";
    public static final String KEYWORD_TRIPARTYSTOP = "TripartyStop";
    public static final String KEYWORD_CONTINUATION = "ContinuationIndicator";
    public static final String KEYWORD_MT569_MESSAGE_ID = "MT569MessageId";

    private static final String PREP_Q = "PREP";
    private static final String CONTINUATION_TAG = ":28E:"; // ContinuationIndicator field
    private static final String KEY_TRIPARTY_NOT_EXPORTED = "TripartyNotExported";

    @Override
    public Trade newAllocationTrade(
            Trade trade,
            TripartyTransaction tripartyTransaction,
            TripartyCollateralAllocation tripartyCollateralAllocation) {

        Locale locale = new Locale("es", "ES");
        if (trade != null && trade.getBook() != null && trade.getBook().getProcessingOrgBasedId() > 0) {
            LegalEntity processingOrg =
                    BOCache.getLegalEntity(
                            DSConnection.getDefault(), trade.getBook().getProcessingOrgBasedId());
            if (processingOrg != null) {
                LegalEntityAttribute leAttrLocale =
                        getLELocaleAttribute(
                                BOCache.getLegalEntityAttributes(DSConnection.getDefault(), processingOrg.getId()));
                if (leAttrLocale != null) {
                    locale = new Locale(leAttrLocale.toString());
                }
            }
        }

        if (tripartyCollateralAllocation.getCollValPrice() != null) {
            trade.addKeyword(
                    _90A_MRKT, Util.numberToString(tripartyCollateralAllocation.getCollValPrice(), locale));
        }

        if (tripartyCollateralAllocation.getValuationFactor() != null) {
            trade.addKeyword(
                    _92A_VAFC,
                    Util.numberToString(tripartyCollateralAllocation.getValuationFactor(), locale));
        }

        if (tripartyCollateralAllocation.getStatementNumber() != null) {
            trade.addKeyword(
                    SantTradeKeywordUtil.STATEMENT_NUMBER, tripartyCollateralAllocation.getStatementNumber());
        }

        if (trade.getProduct() != null && trade.getProduct() instanceof Pledge) {
            if (tripartyCollateralAllocation.getMarketValue() != null) {
                trade.addKeyword(
                        _19A_MKTP,
                        Util.numberToString(tripartyCollateralAllocation.getMarketValue(), locale));
            }
            if (tripartyCollateralAllocation.getFxRate() != null) {
                trade.addKeyword(
                        _92B_EXCH,
                        Util.numberToString(tripartyCollateralAllocation.getFxRate(), locale));
            }
            if (tripartyTransaction.getCollateralHeldAmount() != null) {
                trade.addKeyword(
                        _19A_COVA,
                        Util.numberToString(tripartyTransaction.getCollateralHeldAmount(), locale));
            }
        }

        addStatementFrequencyIndicator(trade, tripartyCollateralAllocation);

        if (trade.getProduct() != null && trade.getProduct() instanceof MarginCall) {
            MarginCall marginCall = (MarginCall) trade.getProduct();
            if (SECURITY.equals(marginCall.getFlowType()) && marginCall.getMarginCallId() > 0) {
                MarginCallConfig marginCallConfig;
                try {
                    marginCallConfig = marginCall.getMarginCallConfig();

                    double dirtyPrice =
                            CollateralUtilities.getMarginCallDirtyPrice(
                                    ((MarginCall) trade.getProduct()).getSecurity(),
                                    tripartyTransaction.getCreationDate().getJDate(TimeZone.getDefault()),
                                    DSConnection.getDefault()
                                            .getRemoteMarketData()
                                            .getPricingEnv(marginCallConfig.getPricingEnvName()),
                                    null);

                    if (dirtyPrice != 0.0) {
                        trade.addKeyword(DIRTY_PRICE, Util.numberToString(dirtyPrice, locale));
                        if (((MarginCall) trade.getProduct()).getSecurity() instanceof Bond) {
                            trade.setTradePrice(dirtyPrice / 100);
                        } else {
                            trade.setTradePrice(dirtyPrice);
                        }
                        trade.setAccrual(0.0);
                    } else {
                        trade.addKeyword(
                                DIRTY_PRICE,
                                Util.numberToString(tripartyCollateralAllocation.getCollValPrice(), locale));
                        trade.setTradePrice(tripartyCollateralAllocation.getCollValPrice());
                        trade.setAccrual(0.0);
                    }

                    String haircutRuleName = marginCallConfig.getHaircutName();
                    Product underlying = ((MarginCall) trade.getProduct()).getSecurity();

                    double haircutValue =
                            getHaircutValue(
                                    haircutRuleName,
                                    underlying,
                                    tripartyTransaction.getCreationDate().getJDate(TimeZone.getDefault()));

                    trade.addKeyword(HAIRCUT, Util.numberToString(haircutValue, locale));

                } catch (Exception e) {
                    Log.error(CalypsoServiceException.class.getName(), e);
                }
            }
        }

        try {
            setKeywordsForTripartyMultipageTrade(
                    trade, tripartyTransaction, tripartyCollateralAllocation);
        } catch (Exception e) {
            Log.error(CalypsoServiceException.class.getName(), e);
        }

        return trade;
    }

    @SuppressWarnings("unchecked")
    private double getHaircutValue(String haircutRuleName, Product underlying, JDate currentDate) {
        double haircutValue = 0.0;
        if (underlying != null && !Util.isEmpty(haircutRuleName)) {
            Vector<Haircut> haircutsVect = null;
            try {
                haircutsVect =
                        DSConnection.getDefault().getRemoteReferenceData().getHaircuts(haircutRuleName);
                if (!Util.isEmpty(haircutsVect)) {
                    for (int i = 0; i < haircutsVect.size(); i++) {
                        Haircut haircut = (Haircut) haircutsVect.get(i);
                        StaticDataFilter secFilter = haircut.getSecFilter();
                        if (!secFilter.accept(null, underlying)) {
                            continue;
                        }
                        List<HaircutData> haircutPoints = haircut.getHaircutPoints();
                        for (int j = 0; j < haircutPoints.size(); j++) {
                            HaircutData hcData = haircutPoints.get(j);
                            if (hcData != null && underlying instanceof Bond
                                    ? underlying.getMaturityDate().lte(currentDate.addTenor(hcData.getTenor()))
                                    : true) {
                                haircutValue += Math.abs(hcData.getValue()) * 100.0;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.error("Error while getting haircut rule: " + haircutRuleName, e);
            }
        }
        return haircutValue;
    }

    public static LegalEntityAttribute getLELocaleAttribute(
            Collection<LegalEntityAttribute> leAttributes) {
        if (Util.isEmpty(leAttributes)) {
            return null;
        }
        for (LegalEntityAttribute legalEntityAttribute : leAttributes) {
            if (LOCALE.equals(legalEntityAttribute.getAttributeType())) {
                return legalEntityAttribute;
            }
        }
        return null;
    }

    private void addStatementFrequencyIndicator(
            final Trade trade, final TripartyCollateralAllocation tripartyCollateralAllocation) {
        String[] rawMessage = tripartyCollateralAllocation.getRawRecords();
        if (!Util.isEmpty(rawMessage)) {
            trade.addKeyword(
                    SantTradeKeywordUtil.STATEMENT_FREQUENCY_INDICATOR,
                    getStatementFrequencyIndicator(rawMessage));
        }
    }

    private String getStatementFrequencyIndicator(String[] rawMessage) {
        String message = arrayToString(rawMessage);
        int index = message.lastIndexOf(_22F_SFRE) + _22F_SFRE.length();
        StringBuilder property = new StringBuilder();
        while (index < message.length()
                && !String.valueOf(message.charAt(index)).equalsIgnoreCase(COLON)) {
            property.append(message.charAt(index));
            index++;
        }
        return property.toString();
    }

    private String arrayToString(String[] message) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < message.length; i++) {
            result.append(message[i]);
        }
        return result.toString();
    }

    /**
     * @return Returns false if error
     */
    private boolean setKeywordsForTripartyMultipageTrade(
            final Trade trade,
            TripartyTransaction tripartyTransaction,
            final TripartyCollateralAllocation
                    tripartyCollateralAllocation) // Assign PREP, ContinuationIndicator and BO message id to
    // Triparty
    // trade
    {
        if (trade != null && tripartyTransaction != null && tripartyCollateralAllocation != null && tripartyCollateralAllocation.getRawRecords()!=null) {
            SwiftMessage swiftMsg = null;
            // Swift message of the trade

            swiftMsg = getSwiftMessageFromTripartyCollateralAllocation(tripartyCollateralAllocation);

            if (swiftMsg == null) return false;
            // Get PREP and Continuation										// 3. Parse swift and get PREP and ContinuationIndicator
            // form swift

            String prepValue = getPrepValueFromSwift(swiftMsg, tripartyTransaction);

            Log.debug(this, "prepValue: " + prepValue);
            if (prepValue == null) return false;
            String valnValue = getValnValueFromSwift(swiftMsg, tripartyTransaction);
            Log.debug(this, "valnValue: " + valnValue);
            if (valnValue == null) return false;
            String continuationValue = getContinuationValueFromSwift(swiftMsg, tripartyTransaction);
            Log.debug(this, "continuationValue: " + continuationValue);
            if (continuationValue == null) return false;

            // Set values in the trade											// 4. add PREP, ContinuationIndicator and BO Message Id
            trade.addKeyword(KEYWORD_PREP, prepValue);
            trade.addKeyword(KEYWORD_VALN, valnValue);
            trade.addKeyword(KEYWORD_TRIPARTYSTOP, getTripartyStopKeyword(valnValue, prepValue));
            trade.addKeyword(KEYWORD_CONTINUATION, continuationValue);
            trade.addKeywordAsLong(KEYWORD_MT569_MESSAGE_ID, tripartyTransaction.getMessageLongId());
            trade.addKeyword(KEY_TRIPARTY_NOT_EXPORTED, Boolean.toString(false));

        }

        return true;
    }

    /**
     * @return Returns null if error
     */
    private SwiftMessage getSwiftMessageFromTripartyCollateralAllocation(
            final TripartyCollateralAllocation tripartyCollateralAllocation) {

        // Get plain message from TripartyCollateralAllocation
        String[] rawMsgLines = tripartyCollateralAllocation.getRawRecords();
        String rawMsg = StringUtils.join(rawMsgLines, "\n");
        Log.debug(this, "rawMsg: " + rawMsg);

        // Parse plain message to Swift object
        SwiftMessage swiftMessage = new SwiftMessage();
        if (rawMsg != null && !swiftMessage.parseSwiftText(rawMsg, false)) {
            Log.error(this, "Can't parse raw records from Triparty Margin Detail");
            return null;
        }
        Log.debug(this, "swiftMessage.getSwiftText(): " + swiftMessage.getSwiftText());
        Log.debug(this, "swiftMessage.getText(): " + swiftMessage.getText());
        Log.debug(this, "swiftMessage.toString(): " + swiftMessage.toString());

        return swiftMessage;
    }

    public JDate parseToJDate(String field) {

        try {
            if (field != null && field.length() >= 8) {
                String tagdate = field.substring(0, 8);
                SimpleDateFormat formatter = new SimpleDateFormat(
                        "yyyyMMdd");
                return JDate.valueOf(formatter.parse(tagdate));
            }
        } catch (Exception e) {
            Log.error("Error getting " + field + " as date", e);
        }
        return null;
    }

    private String getTripartyStopKeyword(String valn, String prep) {

        JDate valnDate = parseToJDate(valn);
        JDate prepDate = parseToJDate(prep);

        if (valnDate != null && prepDate != null && valnDate.equals(prepDate)) {
            return "false";
        } else {
            return "true";
        }
    }

    /**
     * TAGS:  Several (probably: :98A:, :98C:)
     * Qualifier: VALN
     *
     * @param swiftMsg {@link SwiftMessage}
     * @return Returns the VALN value. Returns null if error. If the field
     * exists but the value is empty, it is considered an error,
     * so null will be returned.
     */
    public String getValnValueFromSwift(
            SwiftMessage swiftMsg, TripartyTransaction tripartyTransaction) {
        SwiftFieldMessage valnField = null;
        for (SwiftFieldMessage field : swiftMsg.getFields()) {
            if (field.getValue().indexOf(KEYWORD_VALN + "//") >= 0) {
                valnField = field;
                break;
            }
        }

        if (valnField == null)
            Log.error(this,
                    "Cannot get field "
                            + KEYWORD_VALN
                            + " from Swift message id "
                            + tripartyTransaction.getMessageLongId());
        else {
            String[] values = valnField.getValue().split("//");            // example of value:  :VALN//20190613
            if (values.length == 1
                    || values[1].
                    isEmpty())            // split() discards trailing empty strings, so values.length() could be 1
                Log.error(this,
                        "Field "
                                + KEYWORD_VALN
                                + " contains an empty value after '//', in Swift message with id "
                                + tripartyTransaction.getMessageLongId());
            else return values[1];
        }

        return null;
    }

    /**
     * TAGS: Several (probably: :98A:,:98C:,:98E:) Qualifier: PREP
     *
     * @param swiftMsg {@link SwiftMessage}
     * @return Returns the PREP value. Returns null if error. If the field exists but the value is
     * empty, it is considered an error, so null will be returned.
     */
    public String getPrepValueFromSwift(
            SwiftMessage swiftMsg, TripartyTransaction tripartyTransaction) {
        SwiftFieldMessage prepField = null;
        for (SwiftFieldMessage field : swiftMsg.getFields()) {
            if (field.getValue().indexOf(PREP_Q + "//") >= 0) {
                prepField = field;
                break;
            }
        }

        if (prepField == null)
            Log.error(
                    this,
                    "Cannot get field "
                            + PREP_Q
                            + " from Swift message id "
                            + tripartyTransaction.getMessageLongId());
        else {
            String[] values =
                    prepField.getValue().split("//"); // example of value:  :PREP//20100819050000
            if (values.length == 1
                    || values[1]
                    .isEmpty()) // split() discards trailing empty strings, so values.length() could be 1
                Log.error(
                        this,
                        "Field "
                                + PREP_Q
                                + " contains an empty value after '//', in Swift message with id "
                                + tripartyTransaction.getMessageLongId());
            else return values[1];
        }

        return null;
    }

    /**
     * TAG (:28E:) Qualifier (1)
     *
     * @param swiftMsg {@link SwiftMessage}
     * @return Returns the Continuation value. Returns null if error. If the field exists but the
     * value is empty, it is considered an error, so null will be returned.
     */
    public String getContinuationValueFromSwift(
            SwiftMessage swiftMsg, TripartyTransaction tripartyTransaction) {
        SwiftFieldMessage contiField = swiftMsg.getSwiftField(CONTINUATION_TAG, null, null);

        if (contiField == null)
            Log.error(
                    this,
                    "Cannot get Continuation field from Swift message with id "
                            + tripartyTransaction.getMessageLongId());
        else if (!contiField.getValue().contains("/"))
            Log.error(
                    this,
                    "Continuation field doesn't contain '/' in Swift message with id "
                            + tripartyTransaction.getMessageLongId());
        else {
            String[] values = contiField.getValue().split("/");
            if (values.length == 1
                    || values[1]
                    .isEmpty()) // split() discards trailing empty strings, so values.length() could be 1
                Log.error(
                        this,
                        "Continuation field contains an empty value after '/', in Swift message with id "
                                + tripartyTransaction.getMessageLongId());
            else return values[1];
        }

        return null;
    }


}