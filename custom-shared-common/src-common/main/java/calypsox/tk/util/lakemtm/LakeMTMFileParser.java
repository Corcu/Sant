package calypsox.tk.util.lakemtm;

import calypsox.tk.util.ScheduledTaskImportLakeMtM;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author aalonsop
 */
public class LakeMTMFileParser {

    private final String fileName;
    private final String path;

    private String idTargetColumn;

    private final List<String> productGroupFilter;
    private final List<String> productTypeFilter;
    private final List<String> productFamFilter;

    private boolean errorFlag = false;

    public LakeMTMFileParser(String fileName, String path,String idColumn, String productGroup, String productType, String productFam) {
        this.fileName = fileName;
        this.path = path;
        this.idTargetColumn=idColumn;
        this.productGroupFilter = parseProductFilter(productGroup);
        this.productTypeFilter = parseProductFilter(productType);
        this.productFamFilter = parseProductFilter(productFam);
    }


    public Map<String, ScheduledTaskImportLakeMtM.MTMData> parse() {
        List<MtmLine> lines = readFileAsMap();
        return parseLinesAsMap(lines);
    }

    /**
     * @return Lines of the file
     */
    private List<MtmLine> readFileAsMap() {
        List<MtmLine> lines = new ArrayList<>();
        List<String> header = new ArrayList<>();
        BufferedReader inputFileStream = null;

        if (!Util.isEmpty(fileName) && !Util.isEmpty(path)) {
            try {
                // We read the file.
                inputFileStream = new BufferedReader(new FileReader(path + fileName));
                int i = 0;
                while (inputFileStream.ready()) {
                    List<String> splittedLine = Optional.ofNullable(inputFileStream.readLine())
                            .map(line -> line.split("\t")).map(Arrays::asList).orElse(new ArrayList<>());
                    if (i == 0) {
                        header = splittedLine;
                    } else {
                        MtmLine mappedLine = new MtmLine(header, splittedLine);
                        lines.add(mappedLine);
                    }
                    i++;
                }

                Log.info(Log.CALYPSOX, "Finished reading process of Collateral Prices file");

            } catch (final FileNotFoundException e) {
                Log.error("Error: File didn't found", e);
                errorFlag = true;

            } catch (final Exception e) {
                Log.error("Reading Error", e);
                errorFlag = true;

            } finally {
                try {
                    if (inputFileStream != null) {
                        inputFileStream.close();
                    }
                } catch (final Exception e) {
                    Log.error("File Loader", e);
                    errorFlag = true;
                }
            }
        }

        return lines;

    }

    private Map<String, ScheduledTaskImportLakeMtM.MTMData> parseLinesAsMap(List<MtmLine> lines) {
        Map<String, ScheduledTaskImportLakeMtM.MTMData> mtmAllDataMap = new HashMap<>();

        if (!Util.isEmpty(lines)) {
            for (final MtmLine line : lines) {
                if (!line.fields.isEmpty()) {
                    try {
                        String trngroup = Optional.ofNullable(line.getFieldValue("PROD_GROUP")).orElse("");
                        String trntype = Optional.ofNullable(line.getFieldValue("PROD_TYPE")).orElse("");
                        String trnFam = Optional.ofNullable(line.getFieldValue("PROD_FAMILY")).orElse("");
                        if (!productGroupFilter.contains(trngroup) && !productTypeFilter.contains(trntype) && !productFamFilter.contains(trnFam)) {
                            continue;
                        }

                        if (Util.isEmpty(idTargetColumn)) {
                            idTargetColumn = "CONTRACT_ORI_REF";
                        }
                        String mxID = line.getFieldValue(idTargetColumn);

                        ScheduledTaskImportLakeMtM.MTMData mtmData = mtmAllDataMap.get(mxID);
                        if (mtmData == null) {
                            mtmData = new ScheduledTaskImportLakeMtM.MTMData();
                            mtmData.setDate(stringToDate(line.getFieldValue("PROCESSDATE")));
                            mtmData.setBook(line.getFieldValue("FOLDER"));
                            mtmData.setLine1MTM(parseDouble(line.getFieldValue("MKT_VALUE")));
                            mtmData.setLine1CCY(line.getFieldValue("CURRENCY"));
                            mtmData.setBaseMtM(parseDouble(line.getFieldValue("MV_F")));
                            mtmData.setBaseCCY(mtmData.getLine1CCY());
                            mtmData.setMxID(mxID);
                            mtmData.setCurrencyMtM(parseDouble(line.getFieldValue("MKT_VALUE_CURRENCY")));
                        }

                        mtmData.setLine2MTM(parseDouble(line.getFieldValue("MKT_VALUE")));
                        mtmData.setLine2CCY(line.getFieldValue("CURRENCY"));

                        mtmAllDataMap.put(mtmData.getMxID(), mtmData);
                    } catch (Exception exc) {
                        Log.error(this.getClass().getSimpleName(), "Cannot set line: " + line + "Error: " + exc);
                    }
                }
            }
        }
        return mtmAllDataMap;
    }


    private double parseDouble(String valueStr) {
        double res = 0.0D;
        try {
            res = Optional.ofNullable(valueStr)
                    .map(Double::parseDouble).orElse(0.0D);
        } catch (NumberFormatException exc) {
            Log.debug(this.getClass().getSimpleName(), "Error parsing " + valueStr + " into a Double.");
        }
        return res;
    }

    public static JDate stringToDate(String datetime) {
        String dFormat = "yyyy-MM-dd";
        SimpleDateFormat format = new SimpleDateFormat(dFormat);
        try {
            return JDate.valueOf(format.parse(datetime));
        } catch (ParseException e) {
            Log.warn(Log.LOG, "Error parsing string to JDatetime (" + dFormat + ")" + e.toString());
            return null;
        }
    }

    private List<String> parseProductFilter(String attributeValue) {
        return Optional.ofNullable(attributeValue).map(value -> value.split(";"))
                .map(arr -> new ArrayList<>(Arrays.asList(arr)))
                .orElse(new ArrayList<>());
    }

    public boolean hasErrors() {
        return errorFlag;
    }

    private static class MtmLine {
        Map<String, String> fields;

        public MtmLine(List<String> header, List<String> rawLine) {
            fields = new HashMap<>();
            for (int i = 0; i < header.size(); i++) {
                fields.put(header.get(i), rawLine.get(i));
            }
        }

        String getFieldValue(String key) {
            return fields.get(key);
        }
    }
}
