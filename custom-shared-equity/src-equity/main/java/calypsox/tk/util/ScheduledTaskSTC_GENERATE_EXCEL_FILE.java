package calypsox.tk.util;


import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.ScheduledTaskREPORT;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.util.Log;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.TimeZone;
import java.util.Vector;


public class ScheduledTaskSTC_GENERATE_EXCEL_FILE extends ScheduledTaskREPORT {


    @Override
    public String getTaskInformation() {
        return "This Scheduled Task generate a excel file";
    }


    @Override
    public boolean process(final DSConnection ds, final PSConnection ps) {
        Log.info("Start the Scheduled Task STC_GENERATE_EXCEL_FILE");
        String ext = "";
        String fileFormat = getAttribute("REPORT FORMAT");
        if (Util.isEmpty(fileFormat)) {
            Log.error("There is not REPORT FORMAT set");
            return false;
        }
        if (fileFormat.equals("Excel")) {
            ext = "xls";
        } else if (fileFormat.equals("csv")) {
            ext = "csv";
        } else if (fileFormat.equals("pdf")) {
            ext = "pdf";
        } else if (fileFormat.equals("xlsx")) {
            ext = "xlsx";
        } else if (fileFormat.equals("html")) {
            ext = "html";
        }

        Vector<String> holidays = getHolidays();
        JDatetime valuationDate = getValuationDatetime();
        JDate valDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        Log.info("Set the Value Date");
        String fileName = getFileName();
        String fileLocation = getRealFilename(valuationDate, this, fileName) + "." + ext;
        Log.info("Set the File Location");
        try {
            PricingEnv pricingEnv = ds.getRemoteMarketData().getPricingEnv(getPricingEnv(), valuationDate);
            Log.info("Set the PricingEnv " + pricingEnv.getName());
            Class<?>[] paramTypes = {JDate.class, PricingEnv.class, Vector.class};
            String className = "calypsox.tk.report." + getAttribute("REPORT TYPE");
            Object myClass = InstantiateUtil.getInstance(className);
            Log.info("Before set the method of refection");
            Method method = myClass.getClass().getMethod("getWorkbook", paramTypes);
            Log.info("Before call the reflection");
            XSSFWorkbook workbook = (XSSFWorkbook) method.invoke(myClass, valDate, pricingEnv, holidays);
            Log.info("After call the reflection");
            FileOutputStream file = new FileOutputStream(fileLocation);
            workbook.write(file);
            file.close();
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

        Log.info("End of the Scheduled Task STC_GENERATE_EXCEL_FILE");
        return true;
    }


}
