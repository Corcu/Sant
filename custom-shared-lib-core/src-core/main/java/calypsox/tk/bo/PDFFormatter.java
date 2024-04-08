/*
 *
 * Copyright (c) 2000 by Calypso Technology, Inc. 595 Market Street, Suite 1980,
 * San Francisco, CA 94105, U.S.A. All rights reserved.
 *
 * This software is the confidential and proprietary information of Calypso
 * Technology, Inc. ("Confidential Information"). You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with Calypso Technology.
 */
package calypsox.tk.bo;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.MimeType;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.DisplayInBrowser;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderers.pdf.ITextRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;

/**
 * <b>PDF</b> formatter to enable <i>PDF</i> document archives, plus avoiding
 * <b>HTML</b> confirmations edition <br />
 * DisplayInBrowser.display is not used directly, but with a workaround to
 * enable Mime type
 * v14.4 adaptation by Alvaro Alonso
 *
 * @author Robert S.C.
 * @author aalonsop
 * @version 1.1
 * @since 03/22/2016
 */
public class PDFFormatter implements Formatter {
    /**
     * Title attribute constant
     */
    private static final String TITLE = "title";
    private static final String FILE_URL_START = "file://";
    private static final String PDF_EXT = "PDF";

    // AAP 29/03 GET RESOURCES PATH
    private static final String TEMPLATES_RELATIVE_PATH = "calypsox/templates/";
    private static final String TARGET_JAR = "custom-shared-lib-core";

    public PDFFormatter() {
        super();
    }

    /**
     * Displays the <i>AdviceDocument</i> confirmation in <b>PDF</b> format in
     * web browser
     *
     * @param env     Pricing Environment used to generate BOMessage
     * @param message BOMessage generated
     * @param dsCon   Data Server connection
     * @param doc     advice document containing the generated PDF
     * @return true when no exception
     * @throws MessageFormatException when exception is raised when parsing template
     */
    @Override
    public boolean display(final PricingEnv env, final BOMessage message, final DSConnection dsCon,
                           final AdviceDocument doc) {
        MimeType mimeType = doc.getMimeType();
        String ext = null;
        if (mimeType != null) {
            ext = mimeType.getExtension();
            // If it's .PDF change it to .pdf
            if (PDF_EXT.equals(mimeType.getType())) {
                ext = PDF_EXT.toLowerCase();
            }
        }
        display(doc.getBinaryDocument(), ext, message.getAttribute(TITLE));
        return true;
    }

    /**
     * Generates the <b>PDF</b> confirmation : - replaces the <i>HTML</i>
     * template keywords with MessageFormatter - generates the <i>PDF</i>
     * AdviceDocument
     *
     * @param env         Pricing Environment used to generate BOMessage
     * @param message     BOMessage generated
     * @param newDocument newly generated BOMessage or not
     * @param dsCon       Data Server connection
     * @return Generated adviceDocument
     * @throws MessageFormatException when exception is raised when parsing template
     */
    @Override
    public AdviceDocument generate(final PricingEnv env, final BOMessage message, final boolean newDocument,
                                   final DSConnection dsCon) throws MessageFormatException {
        final String title = message.getMessageType() + "_" + message.getLongId() + "_TradeId_" + message.getTradeLongId();
        message.setAttribute(TITLE, title);
        message.setFormatType(FormatterUtil.HTML);
        String htmlString = MessageFormatter.format(env, message, true, dsCon);
        // spanish adaptation
        htmlString = parserSpecialSpanishCharacters(htmlString);
        message.setFormatType(PDF_EXT);
        return generateAdviceDocument(message, dsCon, htmlString);
    }

    /**
     * @param htmlString that might contain the chars "?" or "?", not recognized by
     *                   the PDF parser
     * @return a replacement for the UNICODE representation for these chars.
     */
    private String parserSpecialSpanishCharacters(final String htmlString) {

        if ((htmlString == null) || htmlString.isEmpty()) {
            return "";
        }

        String parser = htmlString;

        while (parser.indexOf("ñ") > 0) {
            parser = parser.replaceAll("ñ", "&ntilde;");
        }

        while (parser.indexOf("Ñ") > 0) {
            parser = parser.replaceAll("Ñ", "&Ntilde;");
        }

        return parser;

    }

    /**
     * Generates a <b>PDF</b> confirmation, converting the <i>HTML</i>
     * confirmation (document) to a <i>PDF</i> <i>confirmation</i>
     * (binaryDocument)
     *
     * @param message    BOMessage generated
     * @param dsCon      Data Server connection
     * @param htmlString string containing the <i>HTML</i> document
     * @return the adviceDocument containing the <i>PDF</i> binary document
     */
    private AdviceDocument generateAdviceDocument(final BOMessage message, final DSConnection dsCon,
                                                  final String htmlString) throws MessageFormatException {

        /*
         * AdviceDocument doc = null; try { doc =
         * this.remoteSantColService.generateAdviceDocument(message,
         * htmlString); } catch (RemoteException e) { Log.error("PDFFormatter",
         * "Unable to generate the advice document for message " +
         *message.getLongId()); throw new MessageFormatException(e); } return doc;
         */

        if (htmlString == null) {
            Log.error("PDFFormatter", "Empty Generated Message " + "for message " + message.getLongId());
            return null;
        }
        final AdviceDocument doc = new AdviceDocument(message, dsCon.getServerCurrentDatetime());
        doc.setDocument(null);
        doc.setUserName(dsCon.getUser());
        //
        try {
            final ByteArrayInputStream htmlIn = new ByteArrayInputStream(htmlString.getBytes());
            final ByteArrayOutputStream xhtmlErr = new ByteArrayOutputStream();
            final PrintWriter printErr = new PrintWriter(xhtmlErr);

            final Tidy tidy = new Tidy();
            tidy.setErrout(printErr);
            final Document w3cDoc = tidy.parseDOM(htmlIn, null);

            final ITextRenderer renderer = new ITextRenderer();
            final ByteArrayOutputStream osIText = new ByteArrayOutputStream();

            String resourcesPath = this.getClass().getClassLoader().getResource(TEMPLATES_RELATIVE_PATH + message.getTemplateName()).toExternalForm();
            renderer.setDocument(w3cDoc, resourcesPath);
            renderer.layout();
            renderer.createPDF(osIText);

            doc.setBinaryDocument(osIText.toByteArray());
        } catch (final Exception e) {
            throw new MessageFormatException(e);
        }

        return doc;
    }

    /**
     * Creates a document with a given extension and title, and displays it in
     * the default browser. It comes from DisplayInBrowser with Acrobat Reader
     * MIME type working
     *
     * @param bytes a document content (byte array)
     * @param ext   a document extension (String)
     * @param title a document title (String)
     */
    public static void display(final byte[] bytes, final String ext, final String title) {
        try {
            String url = DisplayInBrowser.buildDocument(bytes, ext, title);
            if (!Util.isEmpty(url)) {
                final String browserCommand = Defaults.getProperty(Defaults.BROWSER_PATH);
                if (Util.isEmpty(browserCommand) && url.startsWith(FILE_URL_START)) {
                    url = url.substring(FILE_URL_START.length());
                }
                DisplayInBrowser.displayURL(url);
            }
        } catch (final Exception e) {
            Log.error(Log.CALYPSOX, e);
        }
    }

    /**
     * @return url ExternalForm
     * @throws IOException
     * @author aalonsop AAP-Loads resources from classpath
     */
    public static String getTemplatesPathForm() {
        String path = "";
        try {
            Enumeration<URL> urls =
                    CollateralUtilities.class.getClassLoader().getResources(TEMPLATES_RELATIVE_PATH);
            while (urls.hasMoreElements() && Util.isEmpty(path)) {
                URL auxUrl = urls.nextElement();
                if (auxUrl.toExternalForm().contains(TARGET_JAR)) {
                    path = auxUrl.toExternalForm();
                }
            }
        } catch (IOException e) {
            Log.error(
                    CollateralUtilities.class,
                    "An exception ocurred while trying to get template resources: " + e.getMessage());
        }
        return path;
    }
    // public static void main(String pepe[]) {
    // ConnectionUtil.connect("calypso_user", "calypso", "Launcher", "cert");
    // DSConnection dsCon=DSConnection.getDefault();
    // BOMessage message=dsCon.getRemoteBO().getMessage(311578);
    // final AdviceDocument doc = new AdviceDocument(message,
    // dsCon.getServerCurrentDatetime());
    // doc.setDocument(null);
    // doc.setUserName(dsCon.getUser());
    //
    // try {
    // final ByteArrayInputStream htmlIn = new
    // ByteArrayInputStream(htmlString.getBytes());
    // final ByteArrayOutputStream xhtmlErr = new ByteArrayOutputStream();
    // final PrintWriter printErr = new PrintWriter(xhtmlErr);
    //
    // final Tidy tidy = new Tidy();
    // tidy.setErrout(printErr);
    // final Document w3cDoc = tidy.parseDOM(htmlIn, null);
    //
    // final ITextRenderer renderer = new ITextRenderer();
    // renderer.setDocument(w3cDoc, BASE_URL_TEMPLATES);
    // renderer.layout();
    //
    // final ByteBuffer osIText = new ByteBuffer();
    // renderer.createPDF(osIText);
    // final byte[] convertHTML2PDF = osIText.getBuffer();
    //
    // doc.setBinaryDocument(convertHTML2PDF);
    // } catch (final Exception e) {
    // throw new MessageFormatException(e);
    // }
    //
    // }
}
