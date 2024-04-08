/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.inventory.util;

import calypsox.engine.inventory.SantPositionBean;
import calypsox.engine.inventory.SantPositionConstants.RESPONSES_CODES;

import java.util.*;

import static calypsox.engine.inventory.SantPositionConstants.EMPTY;

/**
 * Keeps track, for each message, on the row, the generated bean and any error that might had happened. This helper
 * allows to generate later on the response and log data in an easier way.
 *
 * @author Guillermo Solano
 * @version 2.0, added tradeID
 * @date 22/10/2013
 */
public class PositionLogHelper {

    /**
     * Response state
     */
    private RESPONSES_CODES state;

    // GSM: by this way it will be easier to identify which state must return NACK depending on the message errors
    // attach, or ACK however we have a warning (e.a. SDI configuration missing)
    public final static RESPONSES_CODES[] SET_VALUES = {}; // RESPONSES_CODES.WAR_SDI_NOT_CONFIGURED -> Best to send
    // NACK=resend
    public final static Set<RESPONSES_CODES> RESP_WARNING = new HashSet<>(Arrays.asList(SET_VALUES));
    /**
     * Row that has been processed
     */
    private String row;

    /**
     * Position Bean generated from the Row
     */
    private SantPositionBean bean;

    /**
     * != empty if any error occurred during parsing
     */
    private final List<LineStatus> lineParserStatus;

    /**
     * Id of the trade after save it
     */
    private Long tradeId;

    /**
     * Constructor
     */
    public PositionLogHelper() {

        this(null, null);
    }

    /**
     * Constructor
     *
     * @param row
     * @param bean
     */
    public PositionLogHelper(String row, SantPositionBean bean) {
        this.row = row;
        this.bean = bean;
        this.lineParserStatus = new ArrayList<>();
        this.tradeId = 0L;
        this.state = RESPONSES_CODES.ACK_OK;
    }

    /**
     * @param logTrack
     * @param posBeanKey
     * @return the log for the key of the Position bean
     */
    public static PositionLogHelper getLog4PosBean(final List<PositionLogHelper> logTrack, final String posBeanKey) {

        for (PositionLogHelper logInfo : logTrack) {
            if (logInfo.getBean().getBeanKey().equals(posBeanKey)) {
                return logInfo;
            }
        }
        return null;
    }

    /**
     * @param logTrack
     * @param bean
     * @return the log helper for Position bean
     */
    public static PositionLogHelper getLog4PosBean(final List<PositionLogHelper> logTrack, final SantPositionBean bean) {

        return getLog4PosBean(logTrack, bean.getBeanKey());
    }

    /**
     * @return the row
     */
    public String getRow() {
        final String cleanRow = this.row.replaceAll("[\n\r]", EMPTY);
        return cleanRow;
    }

    /**
     * @return the state
     */
    public RESPONSES_CODES getResponseState() {
        return this.state;
    }

    /**
     * @param state the state to set
     */
    public void setResponseState(RESPONSES_CODES state) {
        this.state = state;
    }

    /**
     * @param row the row to set
     */
    public void setRow(String row) {
        this.row = row;
    }

    /**
     * @return the bean
     */
    public SantPositionBean getBean() {
        return this.bean;
    }

    /**
     * @param bean the bean to set
     */
    public void setBean(SantPositionBean bean) {
        this.bean = bean;
    }

    /**
     * @return the tradeId
     */
    public Long getTradeId() {
        return this.tradeId;
    }

    /**
     * @param tradeId the tradeId to set
     */
    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

    /**
     * Adds a new status to this row which is being processing
     *
     * @param string
     * @param errCcy
     * @param string2
     */
    public void addLineStatus(String string, RESPONSES_CODES errCcy, String string2) {

        this.lineParserStatus.add(new LineStatus(string, errCcy, string2));
    }

    /**
     * @return true if the line parsered has format or static data errors
     */
    public boolean hasParserErrors() {

        return !this.lineParserStatus.isEmpty();

    }

    /**
     * @return under which circumstances resend have to be done
     */
    // e.a. SDI error configuration
    public boolean requestResendMessage() {

        if (!hasParserErrors()) {
            return false;

        } else {
            // this will check is a warning, send ACK=no_resend
            if (hasOnlyWarningStates()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @returns set of states of the processed position line
     */
    private boolean hasOnlyWarningStates() {

        for (LineStatus l : this.lineParserStatus) {

            if (!RESP_WARNING.contains(l.getClass())) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the list of LineStatus containing errors if any has occurred
     */
    public List<LineStatus> getLinesParserStatuses() {

        return this.lineParserStatus;
    }

    /**
     * Set the position row bean
     *
     * @param row
     * @param posBean
     */
    public void setRowBean(final String row, final SantPositionBean posBean) {
        this.row = row;
        this.bean = posBean;

    }

    /*
     * Inner class to identify the status of EVERY field in each line, therefore all possible errors will be collected
     * in a Vector of this type
     */
    public class LineStatus {

        private final String field;
        private final RESPONSES_CODES type;
        private final String message;

        public LineStatus(String field, RESPONSES_CODES type, String message) {
            this.field = field;
            this.type = type;
            this.message = message;
        }

        public String getField() {
            return this.field;
        }

        public RESPONSES_CODES getType() {
            return this.type;
        }

        public String getMessage() {
            return this.message;
        }

        @Override
        public String toString() {
            return this.type.getResponseValue() + " " + this.message;
        }
    }

}
