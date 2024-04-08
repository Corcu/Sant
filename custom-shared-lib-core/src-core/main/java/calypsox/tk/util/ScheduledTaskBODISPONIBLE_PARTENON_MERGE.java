package calypsox.tk.util;

import calypsox.tk.report.BODisponibleSecurityPositionReportStyle;
import calypsox.tk.util.bean.BODisponiblePartenonBean;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author acd
 */
public class ScheduledTaskBODISPONIBLE_PARTENON_MERGE extends ScheduledTaskCSVREPORT {

    private static final String MIC_FILE_PATH = "Mic File Path";
    private static final String MIC_FILE_DATE_FORMAT = "Mic File DateFormat";

    @Override
    public Vector getDomainAttributes() {
        final Vector result = super.getDomainAttributes();
        result.add(MIC_FILE_PATH);
        result.add(MIC_FILE_DATE_FORMAT);
        return result;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(MIC_FILE_PATH).description("File path without extension. ( File name must be contain the date )"));
        attributeList.add(attribute(MIC_FILE_DATE_FORMAT).description("File Date Format."));
        return attributeList;
    }

    /**
     * Call AFTER generating the Report
     */
    @Override
    protected String saveReportOutput(ReportOutput reportOutput, String type, String reportName, String[] errors, StringBuffer notifications) {
        String micFilePath = getMicFilePath();
        if(!Util.isEmpty(micFilePath)) {
            matchPartenonProcess(micFilePath, reportOutput);
        }
        return super.saveReportOutput(reportOutput, type, reportName, errors, notifications);
    }

    /**
     * Match partenon contracts with position lines
     */
    public void matchPartenonProcess(String micFilePath, ReportOutput reportOutput){
        List<BODisponiblePartenonBean> contracts = loadPartenonContracts(micFilePath);
        HashMap<String, BODisponiblePartenonBean> partenonContracts = new HashMap<>();

        contracts.forEach(partenon -> partenonContracts.computeIfAbsent(partenon.getKey(), v -> partenon));
        if(null!=reportOutput){
            List<ReportRow> reportRows = Arrays.stream(((DefaultReportOutput) reportOutput).getRows()).collect(Collectors.toList());
            reportRows.parallelStream().forEach( row -> {
                String matchingKey = row.getProperty(BODisponibleSecurityPositionReportStyle.PARTENON_MATCHING_KEY);
                if(partenonContracts.containsKey(matchingKey)){
                    BODisponiblePartenonBean partenonContract = partenonContracts.get(matchingKey);
                    row.setProperty(BODisponibleSecurityPositionReportStyle.MIC_PARTENON_CONTRACT,partenonContract);
                }
            });
        }
    }

    /**
     * @return Name of the partenon contracts file with .json extension
     */
    private String getMicFilePath(){
        String micFilePath = getAttribute(MIC_FILE_PATH);
        return micFilePath + getValDate() + ".json";
    }
    private String getValDate(){
        JDate jDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        String micDateFormat = Optional.ofNullable(getAttribute(MIC_FILE_DATE_FORMAT)).orElse("yyyyMMdd");
        JDateFormat dateFormat = new JDateFormat(micDateFormat);
        return dateFormat.format(jDate);
    }

    /**
     * Process and load Partenon Contracts from json file.
     */
    protected List<BODisponiblePartenonBean> loadPartenonContracts(String filePath){
        List<BODisponiblePartenonBean> partenonContracts = new ArrayList<>();
        try {
            Path path = Paths.get(filePath);
            ObjectMapper mapper = new ObjectMapper();
            InputStream json = Files.newInputStream(path);
            partenonContracts = Arrays.stream(mapper.readValue(json, BODisponiblePartenonBean[].class)).collect(Collectors.toList());
        } catch (IOException e) {
            Log.error(this,"Error reading json file: " + e.getMessage());
        }
        return partenonContracts;
    }

    public static void main(String[] args) {
        ScheduledTaskBODISPONIBLE_PARTENON_MERGE test = new ScheduledTaskBODISPONIBLE_PARTENON_MERGE();
        test.matchPartenonProcess("/Users/N521459/Documents/Santander/Proyectos/Disponible/PRE_20230901_MIC_Contracts.json",null);
    }

}
