package calypsox.util;

import calypsox.tk.util.log.LogGeneric;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

public class ForexClearSTUtil {

    public static final String DOS = "2";
    public static final String CTPY = "2Z1I";
    public static final String PO = "BSTE";
    public static final String BUYSELL = "Buy/Sell";
    public static final String CCP = "CCP";
    public static final String COLLATERAL = "COLLATERAL";
    public static final String COUPON = "COUPON";
    public static final String CSH_COL_FEE = "CSH COL FEE";
    public static final String CSA = "CSA";
    public static final String CSD = "CSD";
    public static final String CURRENCY = "Currency";
    public static final String CUSTODIAN = "Custodian";
    public static final String CUSTODIAN_CTE = "CUSTODIAN";
    public static final String DF = "DF";
    public static final String FIELD_SEPARATOR = "Field Separator";
    public static final String FILE_NAME = "File Name";
    public static final String FILE_PATH = "File Path";
    public static final String SUMMARY_LOG = "Summary Log";
    public static final String DETAILED_LOG = "Detailed Log";
    public static final String FULL_LOG = "Full Log";
    public static final String STATIC_DATA_LOG = "Static Data Log";
    public static final String FOREXCLEAR = "FOREXCLEAR";
    public static final String FOREXCLEARSTUTIL = "ForexClearSTUtil";
    public static final String IM = "IM";
    public static final String INITMARGIN = "Initmargin";
    public static final String INTEREST = "INTEREST";
    public static final String IM_INTEREST = "IM_INTEREST";
    public static final String ISIN = "ISIN";
    public static final String LCH = "LCH";
    public static final String MARGIN_CALL = "MARGIN_CALL";
    public static final String MC_CONTRACT_NUMBER = "MC_CONTRACT_NUMBER";
    public static final String NONE = "NONE";
    public static final String PAGE = "Page";
    public static final String PAI = "PAI";
    public static final String POSTINGDESCRIPTION = "Postingdescription";
    public static final String POSTINGDEBIT = "Postingdebit";
    public static final String POSTINGCREDIT = "Postingcredit";
    public static final String PPSCALL = "Ppscall";
    public static final String PPSPAY = "Ppspay";
    public static final String PR_AL_INTST = "PR AL INTST";
    public static final String PRICE = "Price";
    public static final String PROCESSINGORG = "ProcessingOrg";
    public static final String REQMDFCONT = "Reqmdfcont";
    public static final String SECURITY = "SECURITY";
    public static final String UNITS = "Units";
    public static final String VM = "VM";
    public static final String OVERNIGHT = "Overnight";

    // JMGC - DDR ForexClear
    public static final String ACCOUNT = "Account";
    public static final String ACCOUNT_H = "H";
    public static final String VM_EOD_POST = "VM EOD POST";
    public static final String IM_VM_ITD = "IM_VM_ITD";
    public static final String CONTRACTTYPE = "Contracttype";
    public static final String CONTRACTTYPE_O = "O";
    public static final String CCY_EUR = "EUR";

    public static final String REPORT_21 = "21";
    public static final String REPORT_22 = "22";
    public static final String REPORT_22A = "22a";
    public static final String REPORT_32 = "32";
    public static final String REPORT_33A = "33a";
    public static final String REPORT_36A = "36a";
    // JMGC - DDR ForexClear - End

    // Report 32: Start and End date
    public static final String COLUMN_NAME_START_DATE = "Startdate";
    public static final String COLUMN_NAME_END_DATE = "Enddate";
    public static final String DATE_FORMAT = "dd/MM/yyyy hh:mm:ss";
    // Report 32: Start and End date - End

    // Divisa
    public static final String USD_CURRENCY = "USD";

    public static LegalEntity processingOrg = null;
    public static LegalEntity counterParty = null;
    public static SimpleDateFormat timeFormat = new SimpleDateFormat(
            "yyyyMMdd_HHmmss");
	
    //  EUR ccy
    public static final String EUR_CURRENCY = "EUR";
    //  GBP ccy
    public static final String GBP_CURRENCY = "GBP";
    
    public static final String IM_INTEREST_USD = "IM_INTEREST_USD";
    public static final String IM_INTEREST_EUR = "IM_INTEREST_EUR";
    public static final String IM_INTEREST_GBP = "IM_INTEREST_GBP";

    /* Metodos de la una subclase de ForexClearFileReader */
    public static CollateralConfig loadDefaultFundCollateralConfig(String value,
                                                                   final ArrayList<String> logErrTrades) {

        CollateralConfig defaultContract = null;

        if (!Util.isEmpty(value)) {
            try {
                defaultContract = ServiceRegistry.getDefault()
                        .getCollateralDataServer()
                        .getMarginCallConfigByCode("NAME", value.trim());
            } catch (RemoteException e) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
            }

            if (defaultContract == null) {
                logErrTrades.add(
                        "Could not load Default Fund Collateral Config " + value
                                + ". Please, check configuration. Importation cannot be done.");
            }
        } else {
            logErrTrades.add("Could not load Default Fund Config " + value
                    + ". Please, check configuration. Importation cannot be done.");
        }

        return defaultContract;
    }

    public static boolean additionalFieldFilter(
            CollateralConfig collateralConfig, String additionalField,
            String valueField) {
        if (collateralConfig == null)
            return false;
        Map<String, String> additionalMap = collateralConfig
                .getAdditionalFields();
        if (additionalMap == null)
            return false;
        String additionalString = additionalMap.get(additionalField);
        if (additionalString == null)
            return false;
        return additionalString.equals(valueField);
    }

    public static CollateralConfig getCollateralConfig(String contractType,
                                                       int legalEntity, int processingOrg,
                                                       HashMap<String, String> additionalFields, String currency) {

        CollateralConfig filteredContract = null;

        try {
            final CollateralServiceRegistry srvReg = ServiceRegistry
                    .getDefault();

            MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
            ArrayList<String> cts = new ArrayList<String>();
            ArrayList<Integer> les = new ArrayList<Integer>();
            ArrayList<Integer> pos = new ArrayList<Integer>();
            cts.add(contractType);
            les.add(legalEntity);
            pos.add(processingOrg);
            mcFilter.setContractTypes(cts);
            mcFilter.setLegalEntityIds(les);
            mcFilter.setProcessingOrgIds(pos);

            Vector<CollateralConfig> contracts = new Vector<CollateralConfig>();
            Vector<CollateralConfig> filteredContracts = new Vector<CollateralConfig>();

            contracts.addAll(
                    srvReg.getCollateralDataServer().getMarginCallConfigs(
                            mcFilter, ServiceRegistry.getDefaultContext()));

            String key;
            String value;
            for (CollateralConfig contract : contracts) {
                boolean flag = true;
                for (Map.Entry<String, String> entry : additionalFields
                        .entrySet()) {
                    key = entry.getKey();
                    value = entry.getValue();
                    if (!additionalFieldFilter(contract, key, value)) {
                        flag = false;
                    }
                }
                if (flag) {
                    if (currency != null && contract.getCurrency() != null
                            && contract.getCurrency().equals(currency)) {
                        filteredContracts.add(contract);
                    } else if (currency == null) {
                        filteredContracts.add(contract);
                    }
                }
            }

            if (filteredContracts.size() >= 1) {
                filteredContract = filteredContracts.get(0);
            }

        } catch (CollateralServiceException e) {
            // Log error
            Log.error(FOREXCLEARSTUTIL, e);// sonar 03/01/2018
            return null;
        }
        return filteredContract;
    }

    /* Metodos compartidos por las clases */
    public static void initLegalEntities(DSConnection ds,
                                         ArrayList<String> error) {
        try {
            processingOrg = ds.getRemoteReferenceData().getLegalEntity(PO);
            if (processingOrg == null) {
                error.add("No se ha encontrado la legal entity " + PO);
            }
            counterParty = ds.getRemoteReferenceData().getLegalEntity(CTPY);
            if (counterParty == null) {
                error.add("No se ha encontrado la legal entity " + CTPY);
            }
        } catch (CalypsoServiceException e) {
            Log.error(FOREXCLEARSTUTIL, e);// Sonar 03/01/2018
            error.add(e.getMessage());
        }
    }

    public static boolean checkAndSaveTrade(int fila, Trade trade,
                                            ArrayList<String> error) {
        if (trade == null) {
            error.add("Failed to save trade (" + fila + ")");
            return false;
        }
        long idTrade = -1;
        try {
            idTrade = DSConnection.getDefault().getRemoteTrade().save(trade);
        } catch (CalypsoServiceException e) {
            Log.error(FOREXCLEARSTUTIL, e);
            error.add("Failed to save the Margin Call Security Trade");
            return false;
        }
        if (idTrade <= 0) {
            error.add("Failed to save the Margin Call Security Trade");
            return false;
        } else {
            Log.info(FOREXCLEARSTUTIL,
                    "Margin Call Security Trade saved with id " + idTrade);
            return true;
        }
    }

    public static void checkAtributes(String separator, String path,
                                      String fileName, JDate date, ArrayList<String> error) {
        if (separator == null || separator.equals(""))
            error.add("El atributo de Separator esta vacio");
        if (Util.isEmpty(path))
            error.add("La ruta del archivo esta vacia");
        if (Util.isEmpty(fileName))
            error.add("El nombre del archivo esta vacio");
    }

    public static CollateralConfig findContract(DSConnection ds,
                                                String currency, String contractType, String forexClear) {
        // Buscamos el contrato
        CollateralConfig contract = null;
        HashMap<String, String> additionalFields = new HashMap<String, String>();
        additionalFields.put(FOREXCLEAR, forexClear);
        // additionalFields.put(CCP, LCH);
        contract = getCollateralConfig(contractType, counterParty.getId(),
                processingOrg.getId(), additionalFields, currency);
        return contract;
    }

    public static String getFileName(JDate date, String name) {

        String year = String.format("%04d", date.getYear());
        String month = String.format("%02d", date.getMonth());
        String day = String.format("%02d", date.getDayOfMonth());
        final String fileName = year + month + day + "-000000" + name;

        return fileName;
    }

    public static void returnErrorLog(LogGeneric logGen, boolean status,
                                      JDate date, String fileName, String path, String summaryLog,
                                      String className) {

        try {
            ForexClearFileReader.postProcess(status, date, fileName, path);
        } catch (Exception e1) {
            Log.error(className, e1); // sonar
        }
        try {
            logGen.feedGenericLogProcess(fileName, summaryLog, className,
                    logGen.getNumberTotal() - 1);
            logGen.feedFullLog(0);
            logGen.feedDetailedLog(0);
            logGen.closeLogFiles();
        } catch (Exception e) {
            Log.error(ForexClearSTUtil.class, e);
        }
    }
}
