package calypsox.tk.anacredit.util;

import com.calypso.tk.core.Defaults;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class AnacreditFilenameUtil {

    private static final String ORW12 = "ORW12";
    private static final String EXW12 = "EXW12";
    private static final String FILE_OPERACI = ".BY.LR.BDK.ANACCOL.OPERACI.F";
    private static final String FILE_SALDOPS = ".BY.LR.BDK.ANACCOL.SALDOPS.F";
    private static final String FILE_RELPEOP = ".BY.LR.BDK.ANACCOL.RELPEOP.F";
    private static final String FILE_GARREAL = ".BY.LR.BDK.ANACCOL.GARREAL.F";
    private static final String FILE_ACTIFIN = ".BY.LR.BDK.ANACCOL.ACTIFIN.F";

    public static final String OPERACI = "OPERACI";
    public static final String SALDOPS = "SALDOPS";
    public static final String RELPEOP = "RELPEOP";
    public static final String GARREAL = "GARREAL";
    public static final String ACTIFIN = "ACTIFIN";

    public static final String REPO = "REPO";
    public static final String EQUITY = "REPO";
    public static final String PDV = "PDV";
    public static final String COLLATERAL = "Collateral";
    public static final String TITULOS_RV = "Titulos_RV";
    public static final String TITULOS_RF = "Titulos_RF";


    public static HashMap<String, String> getOutputFilesMap() {
        String  environment =
                (Defaults.getEnvName().contains("PRO") ?
                        EXW12 : ORW12);
        HashMap<String, String> reports = new HashMap<>();
        reports.put(OPERACI, environment+ FILE_OPERACI);
        reports.put(SALDOPS, environment+ FILE_SALDOPS);
        reports.put(RELPEOP, environment+ FILE_RELPEOP);
        reports.put(GARREAL, environment+ FILE_GARREAL);
        reports.put(ACTIFIN, environment+ FILE_ACTIFIN);

        return reports;
    }

    public static HashMap<String, String> getReportIterationsMap(String report_type) {
        // change it later
        // Report Type is the Key
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        if (report_type.startsWith("AnacreditOperaciones") ) {
            map.put("AnacreditOperaciones" , OPERACI);
            map.put("AnacreditImportesOperacion" , SALDOPS);
            map.put("AnacreditPersonaOperaciones" , RELPEOP);
        } else  if (report_type.startsWith("AnacreditPdv")) {
            map.put("Copy3_AnacreditPdv" , OPERACI);
            map.put("Copy4_AnacreditPdv" , SALDOPS);
            map.put("Copy4A_AnacreditPdv" , RELPEOP);
            map.put("Copy11_AnacreditPdv" , GARREAL);
            map.put("Copy13_AnacreditPdv" , ACTIFIN);

        } else  if (report_type.startsWith("AnacreditRepo")){
            map.put("Copy3_AnacreditRepo" , OPERACI);
            map.put("Copy4_AnacreditRepo" , SALDOPS);
            map.put("Copy4A_AnacreditRepo" , RELPEOP);
            map.put("Copy11_AnacreditRepo" , GARREAL);
            map.put("Copy13_AnacreditRepo" , ACTIFIN);

        } else  if (report_type.startsWith("AnacreditEQPosition")){
            map.put("Copy3_AnacreditEQPosition" , OPERACI);
            map.put("Copy4_AnacreditEQPosition" , SALDOPS);
            map.put("Copy4A_AnacreditEQPosition" , RELPEOP);
        } else  if (report_type.startsWith("AnacreditEquity")){
            map.put("Copy3_AnacreditEquity" , OPERACI);
            map.put("Copy4_AnacreditEquity" , SALDOPS);
            map.put("Copy4A_AnacreditEquity" , RELPEOP);

        } else  {
            map.put("AnacreditInventoryOper" , OPERACI);
            map.put("AnacreditInventoryImportes" , SALDOPS);
            map.put("AnacreditInventoryPersona" , RELPEOP);
        }
        return map;
    }
}
