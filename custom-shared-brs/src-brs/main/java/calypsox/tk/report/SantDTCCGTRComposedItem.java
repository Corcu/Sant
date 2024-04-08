/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.core.Trade;

import java.util.Vector;

/**
 * SantDTCCGTRComposedItem contains all the common fields in the Snapshot and
 * Valuation reports.
 * 
 * @author Adrian Anton
 * 
 */
public abstract class SantDTCCGTRComposedItem extends SantDTCCGTRItem {

    /**
     * Rows included in this item
     */
    public static final int DTCCGTR_COMPOSED_ITEM_ROW_NUMBER = 7;

    /**
     * Contains all common fields for Snapshot and valuation.
     * 
     * @author Adrian Anton
     * 
     */
    protected enum ComposedItem implements SantDTCCGTREnumItem {

        /**
         * COMMENT field.
         */
        COMMENT(9, "COMMENT"),

        /**
         * ACTION field.
         */
        ACTION_TAG(10, "ACTION"),

        /**
         * MESSAGETYPE field.
         */
        MESSAGE_TYPE_TAG(11, "MESSAGETYPE"),

        /**
         * LEIPREFIX field.
         */
        PO_PREFIX(12, "LEIPREFIX"),

        /**
         * LEIVALUE field.
         */
        PO_VALUE(13, "LEIVALUE"),

        /**
         * USIPREFIX field.
         */
        USI_PREFIX(14, "USIPREFIX"),

        /**
         * USIVALUE field.
         */
        USI_VALUE(15, "USIVALUE");

        private final int position;
        private final String tag;

        private ComposedItem(final int position, final String tag) {
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
     * Field comment.
     */
    protected String comment;

    /**
     * Field actionTag.
     */
    protected String actionTag;

    /**
     * Field messageTypeTag.
     */
    protected String messageTypeTag;

    /**
     * Field poPrefix.
     */
    protected String poPrefix;

    /**
     * Field poValue.
     */
    protected String poValue;

    /**
     * Field usiPrefix.
     */
    protected String usiPrefix;

    /**
     * Field usiValue.
     */
    protected String usiValue;

    /**
     * Field transType.
     */
    protected String transType;

    /**
     * SantDTCCGTRComposedItem constructor.
     * 
     * @param trade
     *            Trade.
     */
    protected SantDTCCGTRComposedItem(final Trade trade) {
        super(trade);
        this.rowNumber += DTCCGTR_COMPOSED_ITEM_ROW_NUMBER;
    }

    /**
     * Returns the comment.
     * 
     * @return the comment.
     */
    protected final String getComment() {
        return this.comment;
    }

    /**
     * Gets the comment.
     * 
     * @param comment
     *            the comment to set
     */
    protected final void setComment(final String comment) {
        this.comment = comment;
    }

    /**
     * Returns the actionTag.
     * 
     * @return the actionTag.
     */
    protected final String getActionTag() {
        return this.actionTag;
    }

    /**
     * Gets the actionTag.
     * 
     * @param actionTag
     *            the actionTag to set
     */
    protected final void setActionTag(final String actionTag) {
        this.actionTag = actionTag;
    }

    /**
     * Returns the messageTypeTag.
     * 
     * @return the messageTypeTag.
     */
    protected final String getMessageTypeTag() {
        return this.messageTypeTag;
    }

    /**
     * Gets the messageTypeTag.
     * 
     * @param messageTypeTag
     *            the messageTypeTag to set
     */
    protected final void setMessageTypeTag(final String messageTypeTag) {
        this.messageTypeTag = messageTypeTag;
    }

    /**
     * Returns the uSIPrefix.
     * 
     * @return the uSIPrefix.
     */
    protected final String getUSIPrefix() {
        return this.usiPrefix;
    }

    /**
     * Gets the uSIPrefix.
     * 
     * @param uSIPrefix
     *            the uSIPrefix to set.
     */
    protected final void setUSIPrefix(final String uSIPrefix) {
        this.usiPrefix = uSIPrefix;
    }

    /**
     * Returns the uSIValue.
     * 
     * @return the uSIValue.
     */
    protected final String getUSIValue() {
        return this.usiValue;
    }

    /**
     * Gets the uSIValue to set.
     * 
     * @param uSIValue
     *            the uSIValue to set.
     */
    protected final void setUSIValue(final String uSIValue) {
        this.usiValue = uSIValue;
    }

    /**
     * Returns the poPrefix.
     * 
     * @return the poPrefix.
     */
    protected final String getPoPrefix() {
        return this.poPrefix;
    }

    /**
     * Gets the poPrefix.
     * 
     * @param poPrefix
     *            the poPrefix to set.
     */
    protected final void setPoPrefix(final String poPrefix) {
        this.poPrefix = poPrefix;
    }

    /**
     * Returns the poValue.
     * 
     * @return the poValue.
     */
    protected final String getPoValue() {
        return this.poValue;
    }

    /**
     * Gets the poValue.
     * 
     * @param poValue
     *            the poValue to set.
     */
    protected final void setPoValue(final String poValue) {
        this.poValue = poValue;
    }

    /**
     * Returns the transType.
     * 
     * @return the transType.
     */
    protected final String getTransType() {
        return this.transType;
    }

    /**
     * Sets the transType.
     * 
     * @param transType
     *            the transType to set.
     */
    protected final void setTransType(final String transType) {
        this.transType = transType;
    }

    /**
     * String representation of this item.
     * 
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuffer str = new StringBuffer();

        str.append(super.toStringNoTagValue());

        str.append('/');
        str.append(getMessageTypeTag());
        str.append('/');
        str.append(getActionTag());
        str.append('/');
        str.append(getComment());
        str.append('/');
        str.append(getPoPrefix());
        str.append('/');
        str.append(getPoValue());
        str.append('/');
        str.append(getUSIPrefix());
        str.append('/');
        str.append(getUSIValue());
        str.append('/');
        str.append(getTransType());

        return str.toString();
    }

    /**
     * Splits the composed item into several items.
     * 
     * @return Vector including all single items.
     */
    public Vector<SantDTCCGTRItem> split() {

        final Vector<Integer> skipped = skipFields();

        final Vector<SantDTCCGTRItem> rst = new Vector<SantDTCCGTRItem>();
        SantDTCCGTRItem current = null;

        for (int i = 0; i < this.rowNumber; ++i) {

            if (!skipped.contains(i)) {

                current = new SantDTCCGTRItem(getTrade());

                fillItem(current, i);

                rst.add(current);

            }

        }

        return rst;

    }

    /**
     * Returns all the positions of all the fields that should be skipped. In
     * this case, for all composed item all columns from single item should be
     * skipped.
     * 
     * @return Vector containing the positions to be skipped.
     */
    protected Vector<Integer> skipFields() {

        final Vector<Integer> rst = new Vector<Integer>();
        rst.add(SantDTCCGTRItem.Item.EXTERNAL_ID.getPosition());
        rst.add(SantDTCCGTRItem.Item.SOURCESYSTEM.getPosition());
        rst.add(SantDTCCGTRItem.Item.MESSAGE_TYPE.getPosition());
        rst.add(SantDTCCGTRItem.Item.ACTION.getPosition());
        rst.add(SantDTCCGTRItem.Item.TRANSACTION_TYPE.getPosition());
        rst.add(SantDTCCGTRItem.Item.PRIMARY_ASSET_CLASS.getPosition());
        rst.add(SantDTCCGTRItem.Item.TAG.getPosition());
        rst.add(SantDTCCGTRItem.Item.VALUE.getPosition());
        rst.add(SantDTCCGTRItem.Item.TRANSACTIONIDPARTY1.getPosition());

        return rst;
    }

    /**
     * Fill a single item.
     * 
     * @param item
     *            Item to be filled.
     * @param i
     *            Index of the pair tag-value.
     */
    protected void fillItem(final SantDTCCGTRItem item, final int i) {
        item.setExternalId(getExternalId());
        item.setSourceSystem(getSourceSystem());
        item.setMessageType(getMessageType());
        item.setAction(getAction());
        item.setTransactionType(getTransactionType());
        item.setPrimaryAssetClass(getPrimaryAssetClass());
        item.setTransactionIdParty1(getTransactionIdParty1());

        final SantDTCCGTREnumItem field = getItem(i);

        item.setTag(field.getTag());
        item.setValue(getValue(i));
    }

    /**
     * Gets the located at position i.
     * 
     * @param i
     *            position of the item.
     * @return Proper item.
     */
    protected SantDTCCGTREnumItem getItem(final int i) {
        SantDTCCGTREnumItem rst = null;
        final SantDTCCGTREnumItem[] values = getAllItems();
        if (i < values.length) {
            rst = values[i];
        }
        return rst;
    }

    /**
     * Gets an array containing all the items. It includes super.getAllItems()
     * plus ComposedItem.values().
     * 
     * @return All items.
     */
    @Override
    protected SantDTCCGTREnumItem[] getAllItems() {
        final SantDTCCGTREnumItem[] superItems = super.getAllItems();
        final SantDTCCGTREnumItem[] thisItems = ComposedItem.values();

        SantDTCCGTREnumItem[] rst = null;
        rst = new SantDTCCGTREnumItem[superItems.length + thisItems.length];

        System.arraycopy(superItems, 0, rst, 0, superItems.length);
        System.arraycopy(thisItems, 0, rst, superItems.length, thisItems.length);

        return rst;
    }

    /**
     * Gets the value associated with the position i.
     * 
     * @param i
     *            position to get the value.
     * @return Value as String.
     */
    @Override
    protected String getValue(final int i) {
        String rst = super.getValue(i);

        if (rst == null) {
            if (ComposedItem.COMMENT.getPosition() == i) {
                rst = this.comment;
            } else if (ComposedItem.ACTION_TAG.getPosition() == i) {
                rst = this.actionTag;
            } else if (ComposedItem.MESSAGE_TYPE_TAG.getPosition() == i) {
                rst = this.messageTypeTag;
            } else if (ComposedItem.PO_PREFIX.getPosition() == i) {
                rst = this.poPrefix;
            } else if (ComposedItem.PO_VALUE.getPosition() == i) {
                rst = this.poValue;
            } else if (ComposedItem.USI_PREFIX.getPosition() == i) {
                rst = this.usiPrefix;
            } else if (ComposedItem.USI_VALUE.getPosition() == i) {
                rst = this.usiValue;
            }
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
    @Override
    protected String getValue(final String tag) {
        String rst = super.getValue(tag);

        if (rst == null) {
            if (ComposedItem.COMMENT.getTag().equalsIgnoreCase(tag)) {
                rst = this.comment;
            } else if (ComposedItem.ACTION_TAG.getTag().equalsIgnoreCase(tag)) {
                rst = this.actionTag;
            } else if (ComposedItem.MESSAGE_TYPE_TAG.getTag().equalsIgnoreCase(
                    tag)) {
                rst = this.messageTypeTag;
            } else if (ComposedItem.PO_PREFIX.getTag().equalsIgnoreCase(tag)) {
                rst = this.poPrefix;
            } else if (ComposedItem.PO_VALUE.getTag().equalsIgnoreCase(tag)) {
                rst = this.poValue;
            } else if (ComposedItem.USI_PREFIX.getTag().equalsIgnoreCase(tag)) {
                rst = this.usiPrefix;
            } else if (ComposedItem.USI_VALUE.getTag().equalsIgnoreCase(tag)) {
                rst = this.usiValue;
            }
        }

        return rst;
    }
}
