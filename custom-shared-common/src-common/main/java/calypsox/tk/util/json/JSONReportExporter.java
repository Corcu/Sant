package calypsox.tk.util.json;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * @author acd
 */
public class JSONReportExporter {

    /**
     * Convert Report lines to JSON format maintaining column order defined in the report template.
     *
     * @param output
     * @return
     */
    public ObjectNode exportJsonReport(ReportOutput output, String reportName, String aliasIdentifier){
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode reportRowsNode = mapper.createObjectNode();

        try{
            if(Optional.ofNullable(output).isPresent()){
                DefaultReportOutput defaultReportOutput = Optional.of(output).filter(DefaultReportOutput.class::isInstance).map(DefaultReportOutput.class::cast).orElse(new DefaultReportOutput(null));
                ReportStyle reportStyle = ReportStyle.getReportStyle(defaultReportOutput.getReport().getType());

                if(!Util.isEmpty(aliasIdentifier)){
                    reportRowsNode.put("aliasIdentifier",aliasIdentifier);
                }

                ArrayNode arrayNode = reportRowsNode.putArray(reportName);
                for (ReportRow row : defaultReportOutput.getRows()){
                    ObjectNode jsonNodes = exportColumns(getReportTemplate(defaultReportOutput), row, reportStyle);
                    arrayNode.add(jsonNodes);
                }

            }

        }catch (Exception e){
            Log.error(this,"Error:" + e);
        }
        return reportRowsNode;
    }






    /**
     * @param reportTemplate
     * @param row
     * @param reportStyle
     * @return
     */
    private ObjectNode exportColumns(ReportTemplate reportTemplate, ReportRow row, ReportStyle reportStyle){
        // create object mapper instance
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode columnNode = mapper.createObjectNode();

        Vector<String> errors = new Vector<>();

        final List<String> column = Arrays.asList(reportTemplate.getColumns());
        final List<String> columnNames = Arrays.asList(reportTemplate.getColumnNames());
        final Hashtable columnFormatsHash = reportTemplate.getColumnFormatsHash();
        if(Optional.ofNullable(reportStyle).isPresent() && !Util.isEmpty(column) && !Util.isEmpty(columnNames) && column.size()==columnNames.size()){
            try {
                for(int i = 0; i <column.size();i++){
                    Object value = Optional.ofNullable(reportStyle.getColumnValue(row, column.get(i), errors)).orElse("");
                    if(!Util.isEmpty(columnFormatsHash) && columnFormatsHash.containsKey(column.get(i)) && value instanceof String){
                        final RawValue test = new RawValue((String) value);
                        columnNode.putRawValue(formatColumnName(columnNames.get(i)),test);
                    }else {
                        setAndMapValue(columnNode,formatColumnName(columnNames.get(i)),value);
                    }
                }
            } catch (Exception e) {
                Log.error(this,"Error: " +e);
            }
        }
        return columnNode;
    }

    /**
     * @param columnName
     * @return
     */
    public static String formatColumnName(String columnName) {
        String formatted = columnName.replaceAll("%", "Perc");
        return formatted.replaceAll("[ ()/]", "_");
    }

    /**
     *
     * Get formats defined on report template
     * @param columnFormatsHash
     * @param columnName
     * @return
     */
    public String[] getColumnFormat(Hashtable columnFormatsHash, String columnName){
        return Optional.ofNullable(columnFormatsHash.get(columnName)).filter(String.class::isInstance).map(String.class::cast).map(v -> v.split(",")).orElse(new String[0]);
    }

    /**
     * @param defaultReportOutput
     * @return
     */
    public ReportTemplate getReportTemplate(DefaultReportOutput defaultReportOutput){
        return Optional.of(defaultReportOutput).map(DefaultReportOutput::getReport).map(Report::getReportTemplate).map(ReportTemplate.class::cast).orElse(new ReportTemplate());
    }

    private File getFileFromResource(String fichero) throws URISyntaxException {
        ClassLoader cargador = getClass().getClassLoader();
        URL resource = cargador.getResource(fichero);
        if (resource == null) {
            throw new IllegalArgumentException("fichero no encontrado" + fichero);
        } else {
            return new File(resource.toURI());
        }
    }

    /**
     * @param columnNode
     * @param name
     * @param value
     */
    private void setAndMapValue(ObjectNode columnNode, String name, Object value){
        if(value instanceof String){
            columnNode.put(name,(String) value);
        }else if(value instanceof Long){
            columnNode.put(name,(Long) value);
        }else if(value instanceof Integer){
            columnNode.put(name,(Integer) value);
        }else if(value instanceof BigInteger){
            columnNode.put(name,(BigInteger) value);
        }else if(value instanceof Double){
            columnNode.put(name,(Double) value);
        }else if(value instanceof BigDecimal){
            columnNode.put(name,(BigDecimal) value);
        }else if(value instanceof Float){
            columnNode.put(name,(Float) value);
        }else if(value instanceof Amount){
            columnNode.put(name,((Amount) value).get());
        }
    }

}
