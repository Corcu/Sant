/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.core.Trade;
import com.calypso.tk.risk.TradeItem;

/**
 * SantDTCCGTRItem contains the head values of Snapshot and Valuation reports.
 * The rest of fields will be contained in tag-value pairs.
 * 
 * @author Adrian Anton
 * 
 */
public class SantDTCCGTRItem extends TradeItem {

    /**
     * Key to access this item
     */
    public static final String SANT_DTCC_GTR_ITEM = "SANT_DTCC_GTR_ITEM";

    /**
     * Rows included in this item
     */
    public static final int DTCCGTR_ITEM_ROW_NUMBER = 9;

    /**
     * Contains all the fields.
     * 
     * @author Adrian Anton
     * 
     */
    public enum Item implements SantDTCCGTREnumItem {

        /**
         * EXTERNAL_ID field.
         */
        EXTERNAL_ID(0, "EXTERNAL_ID"),

        /**
         * SOURCESYSTEM field.
         */
        SOURCESYSTEM(1, "SOURCESYSTEM"),

        /**
         * MESSAGE_TYPE field.
         */
        MESSAGE_TYPE(2, "MESSAGE_TYPE"),

        /**
         * ACTION field.
         */
        ACTION(3, "ACTION"),

        /**
         * TRANSACTION_TYPE field.
         */
        TRANSACTION_TYPE(4, "TRANSACTION_TYPE"),

        /**
         * PRIMARY_ASSET_CLASS field.
         */
        PRIMARY_ASSET_CLASS(5, "PRIMARY_ASSET_CLASS"),

        /**
         * TAG field.
         */
        TAG(6, "TAG"),

        /**
         * VALUE field.
         */
        VALUE(7, "VALUE"),

        /**
         * TRANSACTIONIDPARTY1 field.
         */
        TRANSACTIONIDPARTY1(8, "TRANSACTIONIDPARTY1");

        private final int position;
        private final String tag;

        private Item(final int position, final String tag) {
            this.position = position;
            this.tag = tag;
        }

        /**
         * Gets the position.
         * 
         * @return the position.
         */
        @Override
        public int getPosition() {
            return this.position;
        }

        /**
         * Gets the tag.
         * 
         * @return the tag.
         */
        @Override
        public String getTag() {
            return this.tag;
        }

    }

    /**
     * Final row number.
     */
    protected int rowNumber;

    /**
     * Field externalId.
     */
    protected String externalId;

    /**
     * Field sourceSystem.
     */
    protected String sourceSystem;

    /**
     * Field messageType.
     */
    protected String messageType;

    /**
     * Field action.
     */
    protected String action;

    /**
     * Field transactionType.
     */
    protected String transactionType;

    /**
     * Field primaryAssetClass.
     */
    protected String primaryAssetClass;

    /**
     * Field tag.
     */
    protected String tag;

    /**
     * Field value.
     */
    protected String value;

    /**
     * Field transactionIdParty1.
     */
    protected String transactionIdParty1;


    /**
     * SantDTCCGTRItem constructor.
     * 
     * @param trade
     *            Trade.
     */
    protected SantDTCCGTRItem(final Trade trade) {
        super();
        setTrade(trade);
        this.rowNumber = DTCCGTR_ITEM_ROW_NUMBER;
    }

    /**
     * Returns the externalId.
     * 
     * @return the externalId.
     */
    protected final String getExternalId() {
        return this.externalId;
    }

    /**
     * Sets the externalId.
     * 
     * @param externalId
     *            the externalId to set.
     */
    protected final void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    /**
     * Returns the sourceSystem.
     * 
     * @return the sourceSystem.
     */
    protected final String getSourceSystem() {
        return this.sourceSystem;
    }

    /**
     * the sourceSystem to set.
     * 
     * @param sourceSystem
     *            the sourceSystem to set.
     */
    protected final void setSourceSystem(final String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    /**
     * Returns the messageType.
     * 
     * @return the messageType.
     */
    protected final String getMessageType() {
        return this.messageType;
    }

    /**
     * the messageType to set.
     * 
     * @param messageType
     *            the messageType to set.
     */
    protected final void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    /**
     * Returns the action.
     * 
     * @return the action.
     */
    protected final String getAction() {
        return this.action;
    }

    /**
     * the action to set.
     * 
     * @param action
     *            the action to set.
     */
    protected final void setAction(final String action) {
        this.action = action;
    }

    /**
     * Returns the transactionType.
     * 
     * @return the transactionType.
     */
    protected final String getTransactionType() {
        return this.transactionType;
    }

    /**
     * the transactionType to set.
     * 
     * @param transactionType
     *            the transactionType to set.
     */
    protected final void setTransactionType(final String transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Returns the primaryAssetClass.
     * 
     * @return the primaryAssetClass.
     */
    protected final String getPrimaryAssetClass() {
        return this.primaryAssetClass;
    }

    /**
     * the primaryAssetClass to set.
     * 
     * @param primaryAssetClass
     *            the primaryAssetClass to set.
     */
    protected final void setPrimaryAssetClass(final String primaryAssetClass) {
        this.primaryAssetClass = primaryAssetClass;
    }

    /**
     * returns the transactionIdParty1
     * 
     * @return the transactionIdParty1
     */
    protected String getTransactionIdParty1() {
        return this.transactionIdParty1;
    }

    /**
     * set the transactionIdParty1 field
     * 
     * @param transactionIdParty1
     *            the transactionIdParty1 to set
     */
    protected void setTransactionIdParty1(final String transactionIdParty1) {
        this.transactionIdParty1 = transactionIdParty1;
    }

    /**
     * Returns the tag.
     * 
     * @return the tag.
     */
    public final String getTag() {
        return this.tag;
    }

    /**
     * the tag to set.
     * 
     * @param tag
     *            the tag to set.
     */
    protected final void setTag(final String tag) {
        this.tag = tag;
    }

    /**
     * Returns the value.
     * 
     * @return the value.
     */
    public final String getValue() {
        return this.value;
    }

    /**
     * the value to set.
     * 
     * @param value
     *            the value to set.
     */
    protected final void setValue(final String value) {
        this.value = value;
    }

    /**
     * String representation of this item.
     * 
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuffer str = new StringBuffer();

        str.append(getExternalId());
        str.append('/');
        str.append(getSourceSystem());
        str.append('/');
        str.append(getMessageType());
        str.append('/');
        str.append(getAction());
        str.append('/');
        str.append(getTransactionType());
        str.append('/');
        str.append(getPrimaryAssetClass());
        str.append('/');
        str.append(getTag());
        str.append('/');
        str.append(getValue());
        str.append('/');
        str.append(getTransactionIdParty1());

        return str.toString();
    }

    /**
     * String representation of this item.
     * 
     * @return String representation.
     */
    public String toStringNoTagValue() {
        final StringBuffer str = new StringBuffer();

        str.append(getExternalId());
        str.append('/');
        str.append(getSourceSystem());
        str.append('/');
        str.append(getMessageType());
        str.append('/');
        str.append(getAction());
        str.append('/');
        str.append(getTransactionType());
        str.append('/');
        str.append(getPrimaryAssetClass());
        str.append('/');
        str.append(getTransactionIdParty1());

        return str.toString();
    }

    /**
     * Gets the value associated with the position i.
     * 
     * @param i
     *            position to get the value.
     * @return Value as String.
     */
    protected String getValue(final int i) {
        String rst = null;

        if (Item.EXTERNAL_ID.getPosition() == i) {
            rst = this.externalId;
        } else if (Item.SOURCESYSTEM.getPosition() == i) {
            rst = this.sourceSystem;
        } else if (Item.MESSAGE_TYPE.getPosition() == i) {
            rst = this.messageType;
        } else if (Item.ACTION.getPosition() == i) {
            rst = this.action;
        } else if (Item.TRANSACTION_TYPE.getPosition() == i) {
            rst = this.transactionType;
        } else if (Item.PRIMARY_ASSET_CLASS.getPosition() == i) {
            rst = this.primaryAssetClass;
        } else if (Item.TAG.getPosition() == i) {
            rst = this.tag;
        } else if (Item.VALUE.getPosition() == i) {
            rst = this.value;
        } else if (Item.TRANSACTIONIDPARTY1.getPosition() == i) {
            rst = this.transactionIdParty1;
        }

        return rst;
    }

    /**
     * Gets the value associated with the current tag.
     * 
     * @param tag
     *            current tag to get the value.
     * @return Value as String.
     */
    protected String getValue(final String tag) {
        String rst = null;

        if (Item.EXTERNAL_ID.getTag().equalsIgnoreCase(tag)) {
            rst = this.externalId;
        } else if (Item.SOURCESYSTEM.getTag().equalsIgnoreCase(tag)) {
            rst = this.sourceSystem;
        } else if (Item.MESSAGE_TYPE.getTag().equalsIgnoreCase(tag)) {
            rst = this.messageType;
        } else if (Item.ACTION.getTag().equalsIgnoreCase(tag)) {
            rst = this.action;
        } else if (Item.TRANSACTION_TYPE.getTag().equalsIgnoreCase(tag)) {
            rst = this.transactionType;
        } else if (Item.PRIMARY_ASSET_CLASS.getTag().equalsIgnoreCase(tag)) {
            rst = this.primaryAssetClass;
        } else if (Item.TAG.getTag().equalsIgnoreCase(tag)) {
            rst = this.tag;
        } else if (Item.VALUE.getTag().equalsIgnoreCase(tag)) {
            rst = this.value;
        } else if (Item.TRANSACTIONIDPARTY1.getTag().equalsIgnoreCase(tag)) {
            rst = this.transactionIdParty1;
        }

        return rst;
    }

    /**
     * Gets an array containing all the items.
     * 
     * @return All items.
     */
    protected SantDTCCGTREnumItem[] getAllItems() {
        return Item.values();
    }

}
