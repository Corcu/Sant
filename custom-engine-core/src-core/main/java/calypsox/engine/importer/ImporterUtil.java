/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
/**
 *
 */
package calypsox.engine.importer;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.report.exception.SantExceptionType;
import calypsox.tk.report.exception.SantExceptions;
import calypsox.engine.medusa.utils.xml.XPathHelper;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class ImporterUtil.
 *
 * @author ramous
 */
public class ImporterUtil {
    private static final ImporterUtil INSTANCE = new ImporterUtil();
    private static ImporterUtil instance = INSTANCE;

    private ImporterUtil() {
        // Do nothing
    }

    /**
     * Gets the single instance of ImporterUtil.
     *
     * @return single instance of ImporterUtil
     */
    public static ImporterUtil getInstance() {
        return instance;
    }

    /**
     * This method gets keyword from the message in case of a NACK.
     *
     * @param text
     *            the text
     * @param keywordName
     *            the keyword name
     * @return the keyword from mesg
     */
    protected String getKeywordFromMesg(final String text,
                                        final String keywordName) {
        String res = "";
        XPathHelper helper;
        try {
            helper = new XPathHelper(text, false);

            res = helper.getValueAsString("//keyword[name=\"" + keywordName
                    + "\"]/value");
        } catch (final Exception e) {
            Log.error(this, e);
        }

        return res;

    }

    /**
     * This method gets the unique reference used to choose the right thread.
     *
     * @param text
     *            the text
     * @return the unique reference from mesg
     */
    protected String getUniqueReferenceFromMesg(final String text) {
        String res = "";
        XPathHelper helper;
        try {
            helper = new XPathHelper(text, false);
            // first look for the BlockTradeID
            res = helper
                    .getValueAsString(
                            "//keyword[name=\""
                                    + KeywordConstantsUtil.KEYWORD_BLOCK_TRADE_ID
                                    + "\"]/value").replace("MX", "")
                    .replace("MI", "");
            if (Util.isEmpty(res)) {// if null look to the MxDealId
                res = helper
                        .getValueAsString("//keyword[name=\""
                                + KeywordConstantsUtil.KEYWORD_MX_DEAL_ID
                                + "\"]/value");
            }

        } catch (final Exception e) {
            Log.error(this, e);
        }

        return res;

    }

    /**
     * Gets the xML tag value.
     *
     * @param message
     *            the message
     * @param tag
     *            the tag
     * @return the xML tag value
     */
    public String getXMLTagValue(final ExternalMessage message, final String tag) {
        final String text = message.getText();

        if (text == null) {
            return null;
        }

        int startIndex = text.indexOf(tag);
        if (startIndex < 0) {
            return null;
        }
        int endIndex = text.indexOf(tag, startIndex + 1);
        if (endIndex < 0) {
            return null;
        }
        final String substring = text.substring(startIndex, endIndex);
        startIndex = substring.indexOf('>') + 1;
        endIndex = substring.lastIndexOf('<');
        return substring.substring(startIndex, endIndex);
    }

    // Traded FX

    /**
     * Gets the xML tag value.
     *
     * @param tag
     *            the tag
     * @return the xML tag value
     */
    public String getCodeFromIdentifierTag(final String tag) {
        if (tag == null) {
            return "";
        }
        final Pattern pattern = Pattern
                .compile(".*code=\"([A-Za-z0-9&_ ]+)\".*");

        final Matcher matcher = pattern.matcher(tag);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    /**
     * Publish routing import exception task.
     *
     * @param exceptionType
     *            the exception type
     * @param message
     *            the message
     * @param comment
     *            the comment
     * @param engineName
     *            the engine name
     */
    public void publishJMSImportExceptionTask(
            final SantExceptionType exceptionType, final BOMessage message,
            final String comment, final String engineName) {

        long tradeId = 0;
        long objectId = 0L;
        if (message != null) {
            tradeId = message.getTradeLongId();
            objectId = message.getLongId();
        }

        int bookId = 0;
        try {
            if (tradeId > 0) {
                final Trade trade = DSConnection.getDefault().getRemoteTrade()
                        .getTrade(tradeId);
                final Book book = trade.getBook();
                if (book != null) {
                    bookId = book.getId();
                }
            }
        } catch (final RemoteException e) {
            Log.error(this, "Couldn't get the trade: " + e.getMessage());
        }

        final SantExceptions exceptions = new SantExceptions();
        exceptions.addException(exceptionType, engineName, comment, tradeId, 0,
                objectId, "BOMessage", bookId);

        exceptions.publishTasks(DSConnection.getDefault(), 0, engineName);
    }
}
