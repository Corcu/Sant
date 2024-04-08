package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.util.FileUtility;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.analytics.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;

public class ScheduledTaskGenerateActionLagoMessage extends ScheduledTask {

    private static final long serialVersionUID = -8555289355017289210L;

    public static final String LAGO_FILEPATH = "Lago File Path";
    public static final String STARTFILENAME_LAGO = "Start of File Name LAGO";
    public static final String SWAPONE_FILEPATH = "Swap One File Path";
    public static final String STARTFILENAME_SWAPONE = "Start of File Name Swap One";
    private static final String SOURCE_SYSTEM = "SOURCE_SYSTEM";
    public static final String LAGOBACKUP_FILEPATH = "Lago Backup File Path";
    public static final String SWAPONEBACKUP_FILEPATH = "Swap One Backup File Path";
    public static final String MERGED_FILEPATH = "Merged File Path";
    protected static final String SUMMARY_LOG = "Summary Log";
    private final static String DO_NOT_USE_CONTROL_LINE = "Do not use Control Line";
    private String file = "";
    private String lastLine = "";
    private static final Logger logger = Logger.getLogger(ScheduledTaskGenerateActionLagoMessage.class.getName());
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd");


    @Override
    public String getTaskInformation() {
        return "Merge LAGO - SWAP ONE files";
    }

    public String getFileName() {
        return this.file;
    }

    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.add(attribute(LAGO_FILEPATH));
        attributeList.add(attribute(SWAPONE_FILEPATH));
        attributeList.add(attribute(LAGOBACKUP_FILEPATH));
        attributeList.add(attribute(SWAPONEBACKUP_FILEPATH));
        attributeList.add(attribute(SOURCE_SYSTEM));
        attributeList.add(attribute(MERGED_FILEPATH));
        attributeList.add(attribute(SUMMARY_LOG));
        attributeList.add(attribute(STARTFILENAME_LAGO));
        attributeList.add(attribute(STARTFILENAME_SWAPONE));
        attributeList.add(attribute(DO_NOT_USE_CONTROL_LINE).booleanType());

        return attributeList;
    }


    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        boolean processOK = true;
        boolean checkFile = true;
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyyMMdd");
        String pathLago = getAttribute(LAGO_FILEPATH);
        String pathSwapOne = getAttribute(SWAPONE_FILEPATH);
        String pathLagoBackup = getAttribute(LAGOBACKUP_FILEPATH);
        String pathSwapOneBackup = getAttribute(SWAPONEBACKUP_FILEPATH);
        String startFileNameLago = getAttribute(STARTFILENAME_LAGO);
        String startFileNameSwapOne = getAttribute(STARTFILENAME_SWAPONE);
        String date = CollateralUtilities.getValDateString(this.getValuationDatetime());
        String fileToProcessLago = getAndCheckFileToProcess(pathLago, startFileNameLago + date);
        String fileToProcessSwapOne = getAndCheckFileToProcess(pathSwapOne, startFileNameSwapOne + date);
        String getLagoDate = fileToProcessLago.substring(fileToProcessLago.indexOf(".") - 8, fileToProcessLago.indexOf('.'));
        String mergedFile = getAttribute(MERGED_FILEPATH) + "COLLATERAL_LAGO_" + getLagoDate + ".dat";

        String pathLogs = getAttribute(SUMMARY_LOG);

        String separator = System.getProperty("file.separator");

        // Crear el Handler para escribir los logs en un archivo
        FileHandler fileHandler = null;
        //Obtener fecha para el nombre del fichero
        final Date d = new Date();
        String time = "";
        synchronized (timeFormat) {
            time = timeFormat.format(d);
        }

        try {
            fileHandler = new FileHandler(pathLogs + separator + "MergedLagoFileLogs_" + time + ".txt", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Configurar el formato de los logs
        SimpleFormatter formatter = new SimpleFormatter();
        fileHandler.setFormatter(formatter);

        // Agregar el Handler al logger principal
        Logger.getLogger("").addHandler(fileHandler);

        if (!Util.isEmpty(fileToProcessLago) && !Util.isEmpty(fileToProcessLago)) {

            // Just after file verifications, this method will make a copy into the
            // ./import/copy/ directory
            FileUtility.copyFileToDirectory(pathLago + separator + fileToProcessLago, pathLagoBackup);
            File backupLago = new File(pathLagoBackup);
            if (backupLago.exists()) {
                logger.info("Lago file copied to " + pathLagoBackup);
            } else {
                logger.info("LAGO Backup directory " + pathLagoBackup + " does not exist");
            }

            FileUtility.copyFileToDirectory(pathSwapOne + separator + fileToProcessSwapOne, pathSwapOneBackup);
            File backupSwapOne = new File(pathSwapOneBackup);
            if (backupSwapOne.exists()) {
                logger.info("SwapOne file copied to " + pathSwapOneBackup);
            } else {
                logger.info("SwapOne Backup directory " + pathSwapOneBackup + " does not exist");
            }

            HashMap<String, LagoEquitySwap> mapLago;
            HashMap<String, LagoEquitySwapOne> mapSwapOne;
            try {
                mapLago = readDataFromFileLago(pathLago + separator + fileToProcessLago);
                mapSwapOne = readDataFromFileSwapOne(pathSwapOne + separator + fileToProcessSwapOne);
                if (!checkMaps(mapLago, mapSwapOne)) {
                    return false;
                }
                checkBothLeg(mapLago);
                checkLagoVsSwapOneMatch(mapLago, mapSwapOne);
                checkSwapOneVsLagoMatch(mapSwapOne, mapLago);
                setActionFromSwapOne(mapLago, mapSwapOne);
                setMtM(mapLago);
                processOK = writeFileFromLagoMap(mapLago, mergedFile);
                deleteOldFiles(pathLago, fileToProcessLago);
                deleteOldFiles(pathSwapOne, fileToProcessSwapOne);


            } catch (FileNotFoundException e) {
                Log.error(this, e);
                logger.info(String.valueOf(e));
                processOK = false;
            }

        } else {
            ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound,
                    "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
            processOK = false;
            //this.context.stopLogWriterProcess(); // DPM - 4.5 - avoid unlimited wait in next while
        }

        fileHandler.close();

        return processOK;
    }

    private Boolean deleteOldFiles(String path, String fileToProcess) {

        String separator = System.getProperty("file.separator");
        File deleteFile = new File(path + separator + fileToProcess);

        if (deleteFile.exists()) {
            // Eliminar el archivo
            deleteFile.delete();
            logger.info(fileToProcess + " File deleted");
        } else {
            Log.error(this, "The File " + fileToProcess + " wasn't deleted");
            logger.info("The File " + fileToProcess + " wasn't deleted");
            return false;
        }

        return true;
    }

    private String getAndCheckFileToProcess(String path, String startFileName) {
        String fileToProcess = "";
        ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName);
        // We check if the number of matche?s files is 1.
        if (files.size() == 1) {
            fileToProcess = files.get(0);
            this.file = fileToProcess;

        } else {

            Log.error(this, "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
            logger.info("The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
        }
        return fileToProcess;
    }

    public HashMap<String, LagoEquitySwap> readDataFromFileLago(String fileName) throws FileNotFoundException {
        File f = new File(fileName);
        Scanner scanner = new Scanner(f);

        HashMap<String, LagoEquitySwap> dataList = new HashMap<>();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] data = line.split(Pattern.quote("|"), -1);
            if (data.length <= 1) {
                lastLine = line;
                break;
            }
            int contractId = Integer.parseInt(data[1]);
            String markToMarketDate = data[14];

            LagoEquitySwapLeg equityLeg = new LagoEquitySwapLeg("", data[0], contractId, data[2], data[3], data[4], data[5], data[6], data[7],
                    data[8], data[9], data[10], data[11], data[12], data[13], markToMarketDate, data[15], data[16], data[17], data[18], data[19],
                    data[20], data[21], data[22], data[23], data[24], data[25], data[26], data[27], data[28], data[29], data[30], data[31],
                    data[32], data[33], data[34], data[35], data[36], data[37], data[38], data[39], data[40], data[41], data[42], data[43],
                    data[44], data[45], data[46], data[47], data[48], data[49], data[50]);

            String boReference = data[16];
            String direction = data[9];
            LagoEquitySwap equity = dataList.get(boReference);
            if (equity == null) {
                equity = createEquityWithLeg(direction, equityLeg);
                dataList.put(boReference, equity);
            } else {
                if (settedEquityLeg(equity, direction, equityLeg, contractId, markToMarketDate)) {
                    dataList.put(boReference, equity);
                }
            }
        }
        scanner.close();

        return dataList;
    }

    private HashMap<String, LagoEquitySwapOne> readDataFromFileSwapOne(String fileName) throws FileNotFoundException {
        File f = new File(fileName);
        Scanner scannerSwapOne = new Scanner(f);

        HashMap<String, LagoEquitySwapOne> dataListSwapOne = new HashMap<>();

        while (scannerSwapOne.hasNextLine()) {
            String line = scannerSwapOne.nextLine();
            String[] data = line.split(Pattern.quote("|"), -1);
            if (data.length <= 1) {
                break;
            }
            String bo_reference = getValue(data, 0);
            String action = getValue(data, 1);
            String mtm = getValue(data, 2);
            LagoEquitySwapOne swapOne = new LagoEquitySwapOne(bo_reference,action,mtm);
            dataListSwapOne.put(bo_reference, swapOne);
        }

        scannerSwapOne.close();

        return dataListSwapOne;
    }

    private String getValue(String[] array, int index) {
        return index < array.length ? array[index] : "";
    }

    private boolean checkMaps(HashMap mapLago, HashMap mapSwapOne) {
        if (mapLago == null || mapLago.isEmpty()) {
            Log.error(Log.CALYPSOX, "Lago file wasn't read properly");
            logger.info("Lago file wasn't read properly");
            return false;
        }
        if (mapSwapOne == null || mapSwapOne.isEmpty()) {
            Log.error(Log.CALYPSOX, "SwapOne file wasn't read properly");
            logger.info("SwapOne file wasn't read properly");
            return false;
        }
        return true;
    }

    private LagoEquitySwap createEquityWithLeg(String direction, LagoEquitySwapLeg equityLeg) {
        LagoEquitySwap equity = new LagoEquitySwap();
        if (direction.equals("Loan")) {
            equity.setLoanLeg(equityLeg);
        } else {
            equity.setBorrowLeg(equityLeg);
        }
        return equity;
    }

    private boolean settedEquityLeg(LagoEquitySwap equity, String direction, LagoEquitySwapLeg equityLeg, int contractId, String markToMarketDate) {
        if (direction.equals("Loan") &&
                ((equity.getLoanLeg() == null) ||
                        equity.getLoanLeg() != null &&
                                equity.getLoanLeg().getNum_front_id() <= contractId)) {

            equity.setLoanLeg(equityLeg);
            String mtmDateString = equity.getLoanLeg().getMtm_date();
            String pattern = "dd/MM/yyyy";
            SimpleDateFormat mtmDateFormat = new SimpleDateFormat(pattern);
            try {
                Date equityMtmDate = mtmDateFormat.parse(mtmDateString);
                Date markToMarketFormatDate = mtmDateFormat.parse(markToMarketDate);

                if (equityMtmDate.before(markToMarketFormatDate)) {
                    equity.getLoanLeg().setMtm_date(markToMarketDate);

                    equity.setLoanLeg(equityLeg);
                    return true;
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
            return true;

        } else if (direction.equals("Borrower") &&
                ((equity.getBorrowLeg() == null) ||
                        equity.getBorrowLeg() != null &&
                                equity.getBorrowLeg().getNum_front_id() <= contractId)) {

            equity.setBorrowLeg(equityLeg);
            String mtmDateString = equity.getBorrowLeg().getMtm_date();
            String pattern = "dd/MM/yyyy";
            SimpleDateFormat mtmDateFormat = new SimpleDateFormat(pattern);
            try {
                Date equityMtmDate = mtmDateFormat.parse(mtmDateString);
                Date markToMarketFormatDate = mtmDateFormat.parse(markToMarketDate);

                if (equityMtmDate.before(markToMarketFormatDate)) {
                    equity.getBorrowLeg().setMtm_date(markToMarketDate);
                    equity.setBorrowLeg(equityLeg);
                    return true;
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
            return true;

        }
        return false;
    }

    private void checkBothLeg(HashMap<String, LagoEquitySwap> map) {
        ArrayList<String> tradesToRemove = new ArrayList<>();
        for (String key : map.keySet()) {
            LagoEquitySwap equitySwap = map.get(key);
            if (equitySwap.getBorrowLeg() == null) {
                tradesToRemove.add(key);
                Log.error(Log.CALYPSOX, "Borrow leg from " + key + " trade it is missing");
                logger.info("Borrow leg from " + key + " trade it is missing");
            } else if (equitySwap.getLoanLeg() == null) {
                tradesToRemove.add(key);
                Log.error(Log.CALYPSOX, "Loan leg from " + key + " trade it is missing");
                logger.info("Loan leg from " + key + " trade it is missing");
            }
        }
        if (!tradesToRemove.isEmpty()) {
            removeTrades(tradesToRemove, map);
        }
    }

    private void removeTrades(ArrayList<String> tradesToRemove, HashMap<String, LagoEquitySwap> map) {
        for (String key : tradesToRemove) {
            map.remove(key);
        }
    }


    private void checkSwapOneVsLagoMatch(HashMap<String, LagoEquitySwapOne> mapSwapOne,
                                         HashMap<String, LagoEquitySwap> mapLago) {
        ArrayList<String> tradesToRemove = new ArrayList<>();
        for (String key : mapSwapOne.keySet()) {
            if (mapLago.get(key) == null) {
                tradesToRemove.add(key);
                Log.error(Log.CALYPSOX, "Trade " + key + " it is not in Lago file");
                logger.info("Trade " + key + " it is not in Lago file");
            }
        }
        if (!tradesToRemove.isEmpty()) {
            removeTradesSwapOne(tradesToRemove, mapSwapOne);
        }
    }

    private void removeTradesSwapOne(ArrayList<String> tradesToRemove, HashMap<String, LagoEquitySwapOne> map) {
        for (String key : tradesToRemove) {
            map.remove(key);
        }
    }

    private void checkLagoVsSwapOneMatch(HashMap<String, LagoEquitySwap> mapLago,
                                         HashMap<String, LagoEquitySwapOne> mapSwapOne) {

        ArrayList<String> tradesToRemove = new ArrayList<>();
        for (String key : mapLago.keySet()) {
            if (mapSwapOne.get(key) == null) {
                tradesToRemove.add(key);
                Log.error(Log.CALYPSOX, "Trade " + key + " it is not in SwapOne file");
                logger.info("Trade " + key + " it is not in SwapOne file");
            }
        }
        if (!tradesToRemove.isEmpty()) {
            removeTrades(tradesToRemove, mapLago);
        }
    }

    private void setActionFromSwapOne(HashMap<String, LagoEquitySwap> mapLago,
                                      HashMap<String, LagoEquitySwapOne> mapSwapOne) {

        boolean isUpdateSwapOneMTM = isUpdateMTM();

        for (String key : mapSwapOne.keySet()) {
            LagoEquitySwapOne lagoEquitySwapOne = mapSwapOne.get(key);
            LagoEquitySwap t = mapLago.get(key);
            t.getLoanLeg().setAction(lagoEquitySwapOne.getAction());
            t.getBorrowLeg().setAction(lagoEquitySwapOne.getAction());

            if(isUpdateSwapOneMTM){
                t.getLoanLeg().setMtm(lagoEquitySwapOne.getMtm());
                t.getBorrowLeg().setMtm("0.0");
            }

            mapLago.put(key, t);
        }
    }


    private boolean isUpdateMTM(){
        String activated = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "UpdateSwapOneMTM");
        return !Util.isEmpty(activated) && Boolean.parseBoolean(activated);
    }

    //Only apply if the action is CANCEL
    private void setMtM(HashMap<String, LagoEquitySwap> mapLago) {
        for (String key : mapLago.keySet()) {
            LagoEquitySwap trade = mapLago.get(key);
            if (trade.getLoanLeg().getAction().equals("CANCEL") &&
                    trade.getBorrowLeg().getAction().equals("CANCEL")) {
                trade.getLoanLeg().setMtm("0");
                trade.getBorrowLeg().setMtm("0");
                mapLago.put(key, trade);
            }
        }
    }

    private boolean writeFileFromLagoMap(HashMap<String, LagoEquitySwap> mapLago, String filePath) {

        StringBuilder result = new StringBuilder();
        for (String key : mapLago.keySet()) {
            LagoEquitySwap t = mapLago.get(key);
            result.append(t.getLoanLeg().toString());
            result.append(System.getProperty("line.separator"));
            result.append(t.getBorrowLeg().toString());
            result.append(System.getProperty("line.separator"));
        }
        result.append(lastLine);
        File file = new File(filePath);
        FileWriter fr = null;
        try {
            file.createNewFile();
            fr = new FileWriter(file);
            fr.write(result.toString());
        } catch (IOException e) {
            Log.error(this, "An error was happened when try to find the path to write the merge result. Reason :" + e);
            logger.info("An error was happened when try to find the path to write the merge result. Reason :" + e);
            return false;
        } finally {
            try {
                fr.close();
            } catch (IOException e) {
                Log.error(this, "An error was happened when try to close the fileWriter. Reason :" + e);
                logger.info("An error was happened when try to close the fileWriter. Reason :" + e);
                return false;
            }
        }
        logger.info("Lago and SwapOne files have been merged correctly");
        return true;
    }


}