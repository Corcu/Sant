package calypsox.tk.util;

import calypsox.tk.core.KeywordConstantsUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.Action;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class AccordSWIFTXMLHandler it is used to read an incoming SwiftAccord's
 * xml.
 */
public class AccordSWIFTXMLHandler extends XMLMessageHandler implements
        BoMessageContainer {

    private static final String EMIR_JURISDICTION = "ESMA";

    private static final String DFA_JURISDICTION = "CFTC";

    /**
     * The is status element.
     */
    private boolean isStatusElement = false;

    /**
     * The is message id element.
     */
    private boolean isMessageIdElement = false;

    /** UTI for EMIR **/
    /**
     * The is jurisdiction element.
     */
    private boolean isJurisElement = false;

    /**
     * The is jurisdiction element.
     */
    private boolean isNamespaceElement = false;

    /**
     * The is jurisdiction element.
     */
    private boolean isValueElement = false;

    /**
     * The is jurisdiction element.
     */
    private boolean isPriorNamespaceElement = false;

    /**
     * The is jurisdiction element.
     */
    private boolean isPriorValueElement = false;

    /**
     * is data for EMIR jurisdiction
     **/
    private boolean isEmir = false;

    /**
     * is data for Dodd-Frank jurisdiction
     **/
    private boolean isDfa = false;

    /**
     * The Constant NUMERIC_PATTERN.
     */
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+");

    // CAL_EMIR_019
    /**
     * Temporal prefix
     */
    private String prefix = "";

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        final String content = new String(ch).substring(start, start + length);
        if (this.isMessageIdElement) {
            try {
                final Matcher messageIdMatcher = NUMERIC_PATTERN
                        .matcher(content);
                if (messageIdMatcher.find()) {
                    getMessage().setLongId(
                            Integer.parseInt(messageIdMatcher.group(0)));
                }
            } catch (final NumberFormatException e) {
                throw new SAXException(
                        "element \"mDealTrnS\" is expected to be of type \"xs:decimal\"",
                        e);
            }
        } else if (this.isStatusElement) {
            if (KeywordConstantsUtil.TRADE_KEYWORD_STATUS_MATCHED
                    .equals(content)) {
                getMessage().setAction(Action.MATCH);
            } else if (KeywordConstantsUtil.TRADE_KEYWORD_STATUS_UNMATCHED
                    .equals(content)) {
                getMessage().setAction(Action.UNMATCH);
            } else if (KeywordConstantsUtil.TRADE_KEYWORD_STATUS_MISMATCHED
                    .equals(content)) {
                getMessage().setAction(Action.MISMATCH);
            } else {
                throw new SAXException(
                        "element \"mCnfStatusC\" is expected to be either \"Matched\" or  \"Unmatched\" or  \"Mismatched\"");
            }
        } else if (this.isJurisElement) {
            this.isEmir = EMIR_JURISDICTION.equalsIgnoreCase(content);
            this.isDfa = DFA_JURISDICTION.equalsIgnoreCase(content);
        } else if (this.isEmir && this.isNamespaceElement) {
            if (!"\n".equals(content)) {
                this.prefix = content;
            }
        } else if (this.isEmir && this.isValueElement) {
            if (!Util.isEmpty(this.prefix)) {
                // CAL_EMIR_019
                getMessage().setAttribute(
                        KeywordConstantsUtil.KEYWORD_UTI_TRADE_ID,
                        this.prefix + content);
            } else {
                throw new SAXException(
                        "element \"Unamespace\" is expected to be filled");
            }
        } else if (this.isEmir && this.isPriorNamespaceElement) {
            if (!"\n".equals(content)) {
                this.prefix = content;
            }
        } else if (this.isEmir && this.isPriorValueElement) {
            if (!Util.isEmpty(this.prefix)) {
                // CAL_EMIR_019
                getMessage().setAttribute(
                        KeywordConstantsUtil.KEYWORD_PRIOR_UTI_TRADE_ID,
                        this.prefix + content);
            } else {
                throw new SAXException(
                        "element \"Pnamespace\" is expected to be filled");
            }
        } else if (this.isDfa && this.isNamespaceElement) {
            if (!"\n".equals(content)) {
                this.prefix = content;
            }
        } else if (this.isDfa && this.isValueElement) {
            if (!Util.isEmpty(this.prefix)) {
                // CAL_DODD_097
                getMessage().setAttribute(
                        KeywordConstantsUtil.KEYWORD_USI_TRADE_ID,
                        this.prefix + content);
            } else {
                throw new SAXException(
                        "element \"Unamespace\" is expected to be filled");
            }
        } else if (this.isDfa && this.isPriorNamespaceElement) {
            if (!"\n".equals(content)) {
                this.prefix = content;
            }
        } else if (this.isDfa && this.isPriorValueElement) {
            if (!Util.isEmpty(this.prefix)) {
                // CAL_DODD_097
                getMessage().setAttribute(
                        KeywordConstantsUtil.KEYWORD_PRIOR_USI_TRADE_ID,
                        this.prefix + content);
            } else {
                throw new SAXException(
                        "element \"Pnamespace\" is expected to be filled");
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName,
                           final String qName) throws SAXException {
        if (localName.equalsIgnoreCase("mDealTrnS")) {
            this.isMessageIdElement = false;
        } else if (localName.equalsIgnoreCase("mCnfStatusC")) {
            this.isStatusElement = false;
        } else if (localName.equalsIgnoreCase("Jurisdiction")) {
            // UTI for EMIR reporting Jurisdiction
            this.isJurisElement = false;
        } else if (localName.equalsIgnoreCase("Unamespace")) {
            // USI/UTI namespace
            this.isNamespaceElement = false;
        } else if (localName.equalsIgnoreCase("Uvalue")) {
            // USI/UTI value
            this.isValueElement = false;
        } else if (localName.equalsIgnoreCase("Pnamespace")) {
            // PriorUSI/UTI namespace
            this.isPriorNamespaceElement = false;
        } else if (localName.equalsIgnoreCase("Pvalue")) {
            // PriorUSI/UTI value
            this.isPriorValueElement = false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     * java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName,
                             final String qName, final Attributes attributes)
            throws SAXException {
        if (localName.equalsIgnoreCase("mDealTrnS")) {
            this.isMessageIdElement = true;
        } else if (localName.equalsIgnoreCase("mCnfStatusC")) {
            this.isStatusElement = true;
        } else if (localName.equalsIgnoreCase("Jurisdiction")) {
            // UTI Bock for EMIR reporting Jurisdiction
            this.isJurisElement = true;
        } else if (localName.equalsIgnoreCase("Unamespace")) {
            // USI/UTI namespace
            this.isNamespaceElement = true;
        } else if (localName.equalsIgnoreCase("Uvalue")) {
            // USI/UTI value
            this.isValueElement = true;
        } else if (localName.equalsIgnoreCase("Pnamespace")) {
            // PriorUSI/UTI namespace
            this.isPriorNamespaceElement = true;
        } else if (localName.equalsIgnoreCase("Pvalue")) {
            // PriorUSI/UTI value
            this.isPriorValueElement = true;
        }
    }
}
