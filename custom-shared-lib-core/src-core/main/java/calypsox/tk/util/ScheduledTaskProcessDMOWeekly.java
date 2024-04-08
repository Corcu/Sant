package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.core.SantanderUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventScheduledTask;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

/**
 * Created by x131958 on 12/07/2023.
 */
public class ScheduledTaskProcessDMOWeekly extends ScheduledTask {

    public static final String CSV_DELIMITER_ATTR = "Csv Delimiter";
    public static final String INPUT_FILEPATH = "Input File Path";
    public static final String OUTPUT_FILEPATH = "Output File Path";
    public static final String INPUT_FILENAME = "Input File Name";
    public static final String OUTPUT_FILENAME = "Output File Name";


    public static final String LOG_CATEGORY_SCHEDULED_TASK = "ScheduledTask";

    public static  HashMap<String,String> LE_ATTRIBUTE_MAP = new HashMap<String, String>();

    protected static final String DESTINATION_EMAIL = "Destination email";
    protected static final String FROM_EMAIL = "From email";

    public static final String MIN_NO_OF_LINES = "Minimum No Of Lines";


    protected boolean process(DSConnection ds, PSConnection ps) {
        boolean ret = true;
        try {
            if(!validateDMOFile()){
                return false;
            }
            deleteCurrOutputFile();
            importDMOFile();
        } catch (Exception e) {
            ret = false;
            e.printStackTrace();
        }

        return ret;
    }

    protected boolean importDMOFile() throws IOException {
        String month = String.valueOf(this.getValuationDatetime().getJDate().getMonth());
        if (month.length() < 2) {
            month = "0".concat(month);
        }

        String day = String.valueOf(this.getValuationDatetime().getJDate().getDayOfMonth());
        if (day.length() < 2) {
            day = "0".concat(day);
        }
        return processFile(this.getAttribute(INPUT_FILEPATH) + "/" + this.getAttribute(INPUT_FILENAME) + "-" + String.valueOf(this.getValuationDatetime().getJDate().getYear())+
                month + day+".csv");

    }

    @Override
    public Vector<String> getDomainAttributes() {
        final Vector<String> attr = new Vector<String>();
        attr.add(CSV_DELIMITER_ATTR);
        attr.add(INPUT_FILEPATH);
        attr.add(INPUT_FILENAME);
        attr.add(OUTPUT_FILENAME);
        attr.add(OUTPUT_FILEPATH);
        attr.add(DESTINATION_EMAIL);
        attr.add(FROM_EMAIL);
        attr.add(MIN_NO_OF_LINES);
        return attr;
    }

    protected boolean processFile(String file) throws IOException {

        int counter = 0;
        HashMap<String ,DMOWeekly> DMOWeeklyRows = new HashMap<>();
        Vector<DMOWeekly> DMOErrorRows =  new Vector<>();

        JDate valDate = this.getValuationDatetime().getJDate();
        String valdateString = "";
        if (valDate != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy", Locale.UK);
            valdateString = format.format(valDate.getDate());
        }

        String month = String.valueOf(this.getValuationDatetime().getJDate().getMonth());
        if (month.length() < 2) {
            month = "0".concat(month);
        }

        String day = String.valueOf(this.getValuationDatetime().getJDate().getDayOfMonth());
        if (day.length() < 2) {
            day = "0".concat(day);
        }

        BufferedReader reader = null;
        OutputStream outStream = new FileOutputStream(this.getAttribute(OUTPUT_FILEPATH) + "/" + this.getAttribute(OUTPUT_FILENAME) + "-" + String.valueOf(this.getValuationDatetime().getJDate().getYear())+
                month + day +".csv", true);
        String delimiter = ",";
        try {

            reader = new BufferedReader(new FileReader(file));
            String line = null;
            int lineNumber = -1;
            while ((line = reader.readLine()) != null) {

                lineNumber++;
                Log.debug(LOG_CATEGORY_SCHEDULED_TASK, "Processing line " + lineNumber);

                String[] fields = line.split(",");

                String positionType = fields[0];

                if (positionType.equalsIgnoreCase("DT01")) {

                    if (!valdateString.equalsIgnoreCase(fields[1])) {
                        Log.error(this, "Valuation date check failed. Valuation date in file :" + fields[1]);
                        return false;
                    }

                    outStream.write(fields[0].getBytes());
                    outStream.write(delimiter.getBytes());
                    outStream.write(fields[1].getBytes());
                    outStream.write(delimiter.getBytes());
                    outStream.write(fields[2].getBytes());
                    outStream.write("\r\n".getBytes());
                    counter = counter +1;
                }

                if(positionType.equalsIgnoreCase("TO01")){
                    DMOWeekly rowData = new DMOWeekly();
                    rowData.setRowType(fields[0].toString());
                    rowData.setISIN(fields[1].toString());
                    String leCode = fields[2];
                    rowData.setValue1(Double.valueOf(fields[3].toString().trim()));
                    rowData.setValue2(Double.valueOf(fields[4].toString().trim()));
                    if (rowData.getValue1() == 0.0 || rowData.getValue2() == 0.0) {
                        continue;
                    }
                    LegalEntity le = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(leCode);
                    String attr_value = getLegalEntityAttribute(le, "GEMMS_CATEGORY");
                    if(!Util.isEmpty(attr_value)) {
                        rowData.setCategory(attr_value);
                    } else {
                        DMOWeekly errorRowData = new DMOWeekly();
                        errorRowData.setRowType(fields[0].toString());
                        errorRowData.setISIN(fields[1].toString());
                        errorRowData.setCategory(fields[2].toString());
                        errorRowData.setValue1(Double.valueOf(fields[3].toString().trim()));
                        errorRowData.setValue2(Double.valueOf(fields[4].toString().trim()));
                        DMOErrorRows.add(errorRowData);
                        rowData.setCategory("UKOT");
                    }
                    if (null != DMOWeeklyRows.get(rowData.getISIN().concat(rowData.getCategory()))) {
                        DMOWeekly existingRow = DMOWeeklyRows.get(rowData.getISIN().concat(rowData.getCategory()));
                        double exValue1 = existingRow.getValue1();
                        double exValue2 = existingRow.getValue2();
                        exValue1 = exValue1 + rowData.getValue1();
                        exValue2 = exValue2 + rowData.getValue2();
                        existingRow.setValue1(exValue1);
                        existingRow.setValue2(exValue2);
                        DMOWeeklyRows.put(existingRow.getISIN().concat(existingRow.getCategory()), existingRow);
                    }
                    else {
                        DMOWeeklyRows.put(rowData.getISIN().concat(rowData.getCategory()), rowData);
                    }
                }

                if(positionType.equalsIgnoreCase("TR01")){
                    Set<String> keys = DMOWeeklyRows.keySet();
                    for(String key : keys){
                        DMOWeekly row = DMOWeeklyRows.get(key);
                        outStream.write(row.getRowType().toString().getBytes());
                        outStream.write(delimiter.getBytes());
                        outStream.write(row.getISIN().toString().getBytes());
                        outStream.write(delimiter.getBytes());
                        outStream.write(row.getCategory().toString().getBytes());
                        outStream.write(delimiter.getBytes());
                        String value1 = String.valueOf(row.getValue1());
                        if (value1.contains(".")) {
                            value1 = value1.substring(0, value1.indexOf("."));
                        }
                        outStream.write(value1.getBytes());
                        outStream.write(delimiter.getBytes());
                        String value2 = String.valueOf(row.getValue2());
                        if (value2.contains(".")) {
                            value2 = value2.substring(0, value2.indexOf("."));
                        }
                        outStream.write(value2.getBytes());
                        outStream.write("\r\n".getBytes());
                        counter = counter +1;
                    }
                    outStream.write(fields[0].getBytes());
                    outStream.write(delimiter.getBytes());
                    counter = counter+1;
                    String counterValue = String.valueOf(counter);
                    if (counterValue.contains(".")) {
                        counterValue = counterValue.substring(0, counterValue.indexOf("."));
                    }
                    outStream.write(counterValue.trim().getBytes());
                }
            }

            if (!DMOErrorRows.isEmpty()) {
                handleErrorData(DMOErrorRows);
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

            outStream.close();
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



    @Override
    public String getTaskInformation() {
        return "Import Call Account Attributes from CSV";
    }

    public String getLegalEntityAttribute(final LegalEntity le, final String att) {
        String rst = "";
        if (le != null) {
            final Collection<?> atts = le.getLegalEntityAttributes();
            // FIX in case a LE does NOT have attributes
            if (atts == null) {
                return rst;
            }
            LegalEntityAttribute current;
            final Iterator<?> it = atts.iterator();

            while (it.hasNext() ) {
                current = (LegalEntityAttribute) it.next();
                if (current.getAttributeType().equalsIgnoreCase(att)) {
                    return current.getAttributeValue();
                }
            }
        }
        return rst;
    }

    public void handleErrorData(Vector<DMOWeekly> DMOErrorRows) {
        try {
            ArrayList<String> files = initFiles(DMOErrorRows);
            String subject = "GENERACION INFORME TRN PARA DMO - GLCS SIN CATEGORIA DMO";
            boolean proccesOK = true;
            proccesOK = areFilesGenerated(files);

            List<String> to = getEmails();
            String from = getAttribute(FROM_EMAIL);
            try {
                if (!Util.isEmpty(to) && proccesOK) {
                    String body = getTextBody();
                    CollateralUtilities.sendEmail(to, subject, body, from, files);
                }
            } catch (Exception e) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
            }
        } catch (Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
        }
    }

    public ArrayList<String> initFiles(Vector<DMOWeekly> DMOErrorRows) throws IOException {

        ArrayList<String> files = new ArrayList<String>();
        String delimiter = ",";

        String month = String.valueOf(this.getValuationDatetime().getJDate().getMonth());
        if (month.length() < 2) {
            month = "0".concat(month);
        }

        String day = String.valueOf(this.getValuationDatetime().getJDate().getDayOfMonth());
        if (day.length() < 2) {
            day = "0".concat(day);
        }

        BufferedReader reader = null;
        OutputStream outStream = new FileOutputStream(this.getAttribute(OUTPUT_FILEPATH) + "/" + "RCONTROL" + "-" + String.valueOf(this.getValuationDatetime().getJDate().getYear()) +
                month + day + ".csv", true);

        for (int i = 0; i < DMOErrorRows.size(); i++) {

            DMOWeekly row = DMOErrorRows.get(i);
            outStream.write(row.getRowType().toString().getBytes());
            outStream.write(delimiter.getBytes());
            outStream.write(row.getISIN().toString().getBytes());
            outStream.write(delimiter.getBytes());
            outStream.write(row.getCategory().toString().getBytes());
            outStream.write(delimiter.getBytes());
            String value1 = String.valueOf(row.getValue1());
            if (value1.contains(".")) {
                value1 = value1.substring(0, value1.indexOf("."));
            }
            outStream.write(value1.getBytes());
            outStream.write(delimiter.getBytes());
            String value2 = String.valueOf(row.getValue2());
            if (value2.contains(".")) {
                value2 = value2.substring(0, value2.indexOf("."));
            }
            outStream.write(value2.getBytes());
            outStream.write(delimiter.getBytes());
            outStream.write("CODIGO GLCS SIN CATEGORIA DMO".getBytes());
            outStream.write("\r\n".getBytes());
        }

        files.add(this.getAttribute(OUTPUT_FILEPATH) + "/" + "RCONTROL" + "-" + String.valueOf(this.getValuationDatetime().getJDate().getYear()) +
                month + day + ".csv");

        return files;
    }


    private boolean areFilesGenerated(ArrayList<String> files) {
        if (!files.isEmpty()) {
            for (String namefile : files) {
                try {
                    File f = new File(namefile);
                } catch (Exception e) {
                    Log.error(this, e); //sonar
                    return false;
                }

            }
        } else {
            return false;
        }

        return true;
    }

    public List<String> getEmails() {

        List<String> to = null;

        if (Util.isEmpty(to)) {
            to = new ArrayList<String>();
        }

        String emails = getAttribute(DESTINATION_EMAIL);
        if (!Util.isEmpty(emails)) {
            to.addAll(Arrays.asList(emails.split(";")));
        }
        return to;
    }

    public String getTextBody() {

        String month = String.valueOf(this.getValuationDatetime().getJDate().getMonth());
        if (month.length() < 2) {
            month = "0".concat(month);
        }

        String day = String.valueOf(this.getValuationDatetime().getJDate().getDayOfMonth());
        if (day.length() < 2) {
            day = "0".concat(day);
        }
        StringBuffer body = new StringBuffer();

        String date = String.valueOf(this.getValuationDatetime().getJDate().getYear()) +
                month + day;
        body.append("<br>HOLA,");
        body.append("<br>PRODUCCION");
        body.append("<br>EN LA ULTIMA EJECUCIóN SE HAN DETECTADO ALGÚN REGISTRO CON");
        body.append("<br>HCONTRAPARTIDA SIN CATEGORIA DMO ASOCIADA.");
        body.append("<br>");
        body.append("<br>ADJUNTAMOS LOS REGISTROS INCORRECTOS ");
        body.append("<br>");
        body.append("<br>EL REPORTE DMO TRN CON FECHA " + date + " SE HA ENVIADO UTILIZANDO");
        body.append("<br>LA CATEGORIA DMO POR DEFECTO.");
        body.append("<br><br>UN SALUDO.");
        return body.toString();
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
        String  inputFilePath = this.getAttribute(INPUT_FILEPATH) + "/" + this.getAttribute(INPUT_FILENAME) + "-" + String.valueOf(this.getValuationDatetime().getJDate().getYear())+
                month + day+".csv";

        BufferedReader reader = null;
        boolean header = false;
        boolean footer = false;
        int counter = 0;
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
                failReason = failReason + "<br>" + "* Input file does not contain footer.";
            }
        }
        if (minNoOfLines > counter) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Input file has less number of lines than the minimum criteria of : " + this.getAttribute(MIN_NO_OF_LINES));
            result = false;
            if(Util.isEmpty(failReason)){
                failReason = "* Input file has less number of lines than the minimum criteria of : " + this.getAttribute(MIN_NO_OF_LINES);
            } else {
                failReason = failReason + "<br>" + "* Input file has less number of lines than the minimum criteria of : " + this.getAttribute(MIN_NO_OF_LINES);
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