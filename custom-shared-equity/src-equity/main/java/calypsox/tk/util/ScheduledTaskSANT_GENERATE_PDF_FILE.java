package calypsox.tk.util;


import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.ScheduledTaskREPORT;
import org.jfree.util.Log;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderers.pdf.ITextRenderer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


public class ScheduledTaskSANT_GENERATE_PDF_FILE extends ScheduledTaskREPORT {


    private static final String ATTR_CA_CUPONETTO_REPORT = "CA CUPONETTO REPORT";
    private static final String ATTR_REPORT_FILE_NAME = "REPORT FILE NAME";
    private static final String ATTR_REPORT_FORMAT = "REPORT FORMAT";
    private static final String ATTR_TIMESTAMP_FILENAME = "TIMESTAMP FILENAME";
    private static final String ATTR_TIMESTAMP_FORMAT = "TIMESTAMP FORMAT";
    private static final String CA_CUPONETO_REPORTS = "CACuponettoReports";


    @Override
    public String getTaskInformation() {
        return "This Scheduled Task generate a pdf file";
    }

    @Override
    public boolean isValidInput(Vector messages) {
        return true;
    }


    @Override
    public Vector getDomainAttributes() {
        Vector v = new Vector();
        v.addElement(ATTR_CA_CUPONETTO_REPORT);
        v.addElement(ATTR_REPORT_FILE_NAME);
        v.addElement(ATTR_REPORT_FORMAT);
        v.addElement(ATTR_TIMESTAMP_FILENAME);
        v.addElement(ATTR_TIMESTAMP_FORMAT);
        return v;
    }


    public Vector getAttributeDomain(String attr, Hashtable currentAttr) {
        Vector v = new Vector();
        if (attr.equals(ATTR_CA_CUPONETTO_REPORT)) {
            v = LocalCache.getDomainValues(DSConnection.getDefault(), CA_CUPONETO_REPORTS);
        } else if (attr.equals(ATTR_REPORT_FORMAT)) {
            v.addElement("pdf");
        } else if (attr.equals(ATTR_TIMESTAMP_FILENAME)) {
            v = new Vector();
            v.addElement("true");
            v.addElement("false");
        }
        return v;
    }


    @Override
    public boolean process(final DSConnection ds, final PSConnection ps) {
        Vector<String> holidays = getHolidays();
        JDatetime valuationDate = getValuationDatetime();
        String report =  getAttribute(ATTR_CA_CUPONETTO_REPORT);
        String fileName =  getAttribute(ATTR_REPORT_FILE_NAME);
        String fileFormat =  getAttribute(ATTR_REPORT_FORMAT);
        String fileLocation = getRealFilename(valuationDate, this, fileName) + "." + fileFormat;
        try {
            PricingEnv pricingEnv = ds.getRemoteMarketData().getPricingEnv(getPricingEnv(), valuationDate);
            Class<?>[] paramTypes = {DSConnection.class, JDatetime.class, PricingEnv.class, Vector.class};
            String className = "calypsox.tk.report." + report;
            Object myClass = InstantiateUtil.getInstance(className);
            Method method = myClass.getClass().getMethod("getContent", paramTypes);
            String content = (String) method.invoke(myClass,ds,  valuationDate, pricingEnv, holidays);

            StringBuilder htmlInfo = new StringBuilder();
            htmlInfo.append(getHeader());
            htmlInfo.append(content);
            htmlInfo.append(getFooter());

            try {
                final ByteArrayInputStream htmlIn = new ByteArrayInputStream(htmlInfo.toString().getBytes());
                final ByteArrayOutputStream xhtmlErr = new ByteArrayOutputStream();
                final PrintWriter printErr = new PrintWriter(xhtmlErr);
                final Tidy tidy = new Tidy();
                tidy.setErrout(printErr);
                final Document w3cDoc = tidy.parseDOM(htmlIn, null);
                final ITextRenderer renderer = new ITextRenderer();
                final ByteArrayOutputStream osIText = new ByteArrayOutputStream();
                renderer.setDocument(w3cDoc,null);
                renderer.layout();
                FileOutputStream fos = new FileOutputStream(fileLocation);
                renderer.createPDF(fos);
                fos.close();
                return true;
            } catch (final Exception e) {
                throw new MessageFormatException(e);
            }

        } catch (IllegalAccessException e) {
            Log.error(this.getClass().getSimpleName() + "IllegalAccessException", e);
            return false;
        } catch (InstantiationException e) {
            Log.error(this.getClass().getSimpleName() + "InstantiationException",e);
            return false;
        } catch (NoSuchMethodException e) {
            Log.error(this.getClass().getSimpleName() + "NoSuchMethodException", e);
            return false;
        } catch (InvocationTargetException e) {
            Log.error(this.getClass().getSimpleName() + "InvocationTargetException", e);
            return false;
        } catch (Exception e) {
            Log.error(this.getClass().getSimpleName() + "Exception", e);
            return false;
        }

    }


    private StringBuilder getHeader(){
        StringBuilder htmlInfo = new StringBuilder();
        htmlInfo.append("<!DOCTYPE html>");
        htmlInfo.append("<html>");
        htmlInfo.append("<head>");
        htmlInfo.append("<meta charset=\"utf-8\">");
        htmlInfo.append("<title>Sant Generate Pdf File</title>");
        htmlInfo.append("</head>");
        htmlInfo.append("<body>");
        return htmlInfo;
    }


    private StringBuilder getFooter(){
        StringBuilder htmlInfo = new StringBuilder();
        htmlInfo.append("</body>");
        htmlInfo.append("</html>");
        return htmlInfo;
    }


}