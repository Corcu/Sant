/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

/**
 * The Enum DateKeyword.
 */
public enum DateKeyword {

    /** The keyword acc ndf refixed date. */
    KEYWORD_ACC_NDF_REFIXED_DATE("NDFReReset", "dd/MM/yyyy", "DD/MM/YYYY"),

    /** The keyword acc ndf fixed date. */
    KEYWORD_ACC_NDF_FIXED_DATE("NDFFixedDate", "dd/MM/yyyy", "DD/MM/YYYY"),

    /** The keyword acc cancellation date. */
    KEYWORD_ACC_CANCELLATION_DATE(
            "CancellationDate",
            "dd/MM/yyyy",
            "DD/MM/YYYY"),

    /** The keyword acc booking date. */
    KEYWORD_ACC_BOOKING_DATE("Partenon-BOOKINGDATE", "dd/MM/yyyy", "DD/MM/YYYY"),

    /** The keyword acc termination date. */
    KEYWORD_ACC_TERMINATION_DATE(
            "AccTerminationDate",
            "dd/MM/yyyy",
            "DD/MM/YYYY"),

    // CAL_ACC 1279
    /** The keyword partial termination date. */
    KEYWORD_PARTIAL_TERMINATION_DATE(
            "PTerminationDate",
            "MM-dd-yyyy",
            "MM-DD-YYYY"),

    /** The keyword transfer date. */
    KEYWORD_TRANSFER_DATE("TransferDate", "MM-dd-yyyy", "MM-DD-YYYY"),

    /** The keyword acc late trade murex. */
    KEYWORD_ACC_LATE_TRADE_MUREX("LateTradeMurex", "yyyyMMdd", "YYYYMMDD"),

    /** The keyword flow rate reset date. */
    KEYWORD_FLOW_RATE_RESET_DATE(
            "FlowRateResetDate",
            "dd/MM/yyyy",
            "DD/MM/YYYY"),

    /** The le attribute legal contract date. */
    LE_ATTRIBUTE_LEGAL_CONTRACT_DATE(
            "legalContractDate",
            "yyyy-MM-dd",
            "YYYY-MM-DD");

    private final String name;
    private final String format;
    private final String sqlFormat;

    private DateKeyword(final String name, final String format,
            final String sqlFormat) {
        this.name = name;
        this.format = format;
        this.sqlFormat = sqlFormat;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the format.
     * 
     * @return the format
     */
    public String getFormat() {
        return this.format;
    }

    /**
     * Gets the sql format.
     * 
     * @return the sqlFormat
     */
    public String getSqlFormat() {
        return this.sqlFormat;
    }

    /**
     * Gets the date.
     * 
     * @param trade
     *            the trade
     * @return the date
     */
    public JDate getDate(final Trade trade) {
        JDate date = null;
        if (trade != null) {
            final String dateKeyword = trade.getKeywordValue(getName());
            if (!Util.isEmpty(dateKeyword)) {
                final SimpleDateFormat sdf = new SimpleDateFormat(getFormat(),
                        Locale.getDefault());
                try {
                    date = JDate.valueOf(sdf.parse(dateKeyword));
                } catch (final ParseException e) {
                    Log.error(this.getClass().getName(),
                            "Cannot parse the keyword '" + getName()
                                    + "' for the trade '" + trade.getLongId()
                                    + "': wrong format", e);
                }
            }
        }
        return date;
    }

    /**
     * Gets the date.
     * 
     * @param le
     *            the le
     * @return the date
     */
    public JDate getDate(final LegalEntity le) {
        JDate date = null;
        if (le != null) {
            final String dateKeyword = SantanderUtil.getInstance()
                    .getLegalEntityAttribute(le, getName());
            if (!Util.isEmpty(dateKeyword)) {
                final SimpleDateFormat sdf = new SimpleDateFormat(getFormat(),
                        Locale.getDefault());
                try {
                    date = JDate.valueOf(sdf.parse(dateKeyword));
                } catch (final ParseException e) {
                    Log.error(this.getClass().getName(),
                            "Cannot parse the attribute '" + getName()
                                    + "' for the legal entity '" + le.getId()
                                    + "': wrong format", e);
                }
            }
        }
        return date;
    }
}
