package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventScheduledTask;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by x131958
 */
public class ScheduledTaskProcessDMO extends ScheduledTask {

    public static final String CSV_DELIMITER_ATTR = "Csv Delimiter";
    public static final String INPUT_FILEPATH = "Input File Path";
    public static final String OUTPUT_FILEPATH = "Output File Path";
    public static final String INPUT_FILENAME = "Input File Name";
    public static final String OUTPUT_FILENAME = "Output File Name";
    public static final String STATIC_DATA_FILTER = "Static Data Filter";

    public static final String LOG_CATEGORY_SCHEDULED_TASK = "ScheduledTask";

    public static final String MIN_NO_OF_LINES = "Minimum No Of Lines";

    protected static final String DESTINATION_EMAIL = "Destination email";
    protected static final String FROM_EMAIL = "From email";


    protected boolean process(DSConnection ds, PSConnection ps) {
        boolean ret = true;
        try {
            if(!validateDMOFile()){
                return false;
            }
            deleteCurrOutputFile();
            Vector<DMOPosition> UN01Rows =calculateUN01();
            importDMOFile(UN01Rows);
        } catch (Exception e) {
            ret = false;
            e.printStackTrace();
        }

        return ret;
    }

    protected boolean importDMOFile(Vector<DMOPosition> UN01Rows) throws Exception {

        DMOPositionUtil dmoUtil = new DMOPositionUtil();
        String month = String.valueOf(this.getValuationDatetime().getJDate().getMonth());
        if (month.length() < 2) {
            month = "0".concat(month);
        }

        String day = String.valueOf(this.getValuationDatetime().getJDate().getDayOfMonth());
        if (day.length() < 2) {
            day = "0".concat(day);
        }
        String inputFilePath = this.getAttribute(INPUT_FILEPATH) + "/" + this.getAttribute(INPUT_FILENAME) + "-" + String.valueOf(this.getValuationDatetime().getJDate().getYear())+
                month + day+".csv";
        String outputFilePath = this.getAttribute(OUTPUT_FILEPATH) + "/" + this.getAttribute(OUTPUT_FILENAME) + "-" + String.valueOf(this.getValuationDatetime().getJDate().getYear())+
                month + day +".csv";

        return dmoUtil.processFile(inputFilePath, outputFilePath, UN01Rows , this.getValuationDatetime().getJDate(this.getTimeZone()));
    }

    @Override
    public Vector<String> getDomainAttributes() {
        final Vector<String> attr = new Vector<String>();
        attr.add(CSV_DELIMITER_ATTR);
        attr.add(INPUT_FILEPATH);
        attr.add(INPUT_FILENAME);
        attr.add(OUTPUT_FILEPATH);
        attr.add(OUTPUT_FILENAME);
        attr.add(STATIC_DATA_FILTER);
        attr.add(MIN_NO_OF_LINES);
        attr.add(DESTINATION_EMAIL);
        attr.add(FROM_EMAIL);
        return attr;
    }

    protected boolean processFile(String file) throws CalypsoServiceException {



        BufferedReader reader = null;
        try {

            reader = new BufferedReader(new FileReader(file));
            String line = null;
            int lineNumber = -1;
            while ((line = reader.readLine()) != null) {

                lineNumber++;
                Log.debug(LOG_CATEGORY_SCHEDULED_TASK, "Processing line " + lineNumber);

                String[] fields = line.split(getDelimiter());

                if (fields.length < 3) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Incorrect line format : " + line);
                } else {

                    String positionType = fields[0];
                    String productCode = fields[1];
                    String nominal = fields[2];

                    DMOPosition dmoPostion = new DMOPosition();
                    dmoPostion.setPosition_Type(positionType);
                    dmoPostion.setProduct_Code(productCode);
                    dmoPostion.setNominal(Integer.parseInt(nominal)*1000);
                }

            }

        } catch (Exception exc) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, exc);
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.error(this, e); // sonar
                }
            }
        }

        return true;
    }


    protected String getDelimiter() {
        String delimiter = this.getAttribute(CSV_DELIMITER_ATTR);
        if (delimiter == null) {
            return ";";
        }
        return delimiter;
    }

    public Vector<DMOPosition> calculateUN01() throws Exception {
        DSConnection ds = DSConnection.getDefault();
        StaticDataFilter sdf = BOCache.getStaticDataFilter(DSConnection.getDefault(), this.getAttribute(STATIC_DATA_FILTER));
        TradeFilter tf = ds.getRemoteReferenceData().getTradeFilter(this.getTradeFilter());
        if(null == tf) {
            return new Vector<DMOPosition>();
        }
        return DMOPositionUtil.addUN01Row(tf , sdf , ds , this.getValuationDatetime());
    }


    @Override
    public String getTaskInformation() {
        return "Import Call Account Attributes from CSV";
    }

    protected boolean validateDMOFile() throws Exception {
        boolean result = true;
        if (Util.isEmpty(this.getAttribute(MIN_NO_OF_LINES))) {
            return true;
        }
        int minNoOfLines = Integer.valueOf(this.getAttribute(MIN_NO_OF_LINES));
        String month = String.valueOf(this.getValuationDatetime().getJDate().getMonth());
        if (month.length() < 2) {
            month = "0".concat(month);
        }

        String day = String.valueOf(this.getValuationDatetime().getJDate().getDayOfMonth());
        if (day.length() < 2) {
            day = "0".concat(day);
        }
        String inputFilePath = this.getAttribute(INPUT_FILEPATH) + "/" + this.getAttribute(INPUT_FILENAME) + "-" + String.valueOf(this.getValuationDatetime().getJDate().getYear()) +
                month + day + ".csv";

        BufferedReader reader = null;
        int counter = 0;
        boolean header = false;
        boolean footer = false;
        try {
            reader = new BufferedReader(new FileReader(inputFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                String positionType = fields[0];
                if (positionType.equalsIgnoreCase("DT01")) {
                    header = true;
                }
                if (positionType.equalsIgnoreCase("TR01")) {
                    footer = true;
                }
                counter = counter + 1;
            }
        } catch (Exception exc) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, exc);
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.error(this, e); // sonar
                }
            }
        }

        String failReason = "";
        if (header == false) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Input file does not contain header");
            result = false;
            failReason = "* Input file does not contain header.";
        }
        if (footer == false) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Input file does not contain footer");
            result = false;
            if(Util.isEmpty(failReason)){
                failReason = "* Input file does not contain footer.";
            } else {
                failReason = failReason + "<br>" +"* Input file does not contain footer.";
            }

        }
        if (minNoOfLines > counter) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Input file has less number of lines than the minimum criteria of : " + this.getAttribute(MIN_NO_OF_LINES));
            result = false;
            if(Util.isEmpty(failReason)){
                failReason = "* Input file has less number of lines than the minimum criteria of : " + this.getAttribute(MIN_NO_OF_LINES);
            } else {
                failReason = failReason +"<br>"+ "* Input file has less number of lines than the minimum criteria of : " + this.getAttribute(MIN_NO_OF_LINES);
            }
        }

        try{
            DMOPositionUtil dmoUtil = new DMOPositionUtil();
            if(result==false) {
                dmoUtil.handleInputFileErrorData(inputFilePath, this.getAttribute(FROM_EMAIL), this.getAttribute(DESTINATION_EMAIL), this.getValuationDatetime().getJDate(), failReason);
            }
        } catch(Exception e){
            return result;
        }

        return result;
    }

    protected boolean deleteCurrOutputFile() throws Exception {

        DMOPositionUtil dmoUtil = new DMOPositionUtil();
        String month = String.valueOf(this.getValuationDatetime().getJDate().getMonth());
        if (month.length() < 2) {
            month = "0".concat(month);
        }

        String day = String.valueOf(this.getValuationDatetime().getJDate().getDayOfMonth());
        if (day.length() < 2) {
            day = "0".concat(day);
        }
        String outputFilePath = this.getAttribute(OUTPUT_FILEPATH) + "/" + this.getAttribute(OUTPUT_FILENAME) + "-" + String.valueOf(this.getValuationDatetime().getJDate().getYear())+
                month + day +".csv";

        try {

            File tmpDir = new File(outputFilePath);
            boolean exists = tmpDir. exists();
            if(exists){
                tmpDir.delete();
            }

        } catch (Exception e) {
            return true;
        }

        return true;
    }

}