package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.mmoo.ImportMMOOUtilities;
import calypsox.tk.util.mmoo.jaxb.eurex.*;
import calypsox.util.DOMUtility;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

/**
 * Title: ScheduledTaskImportXMLEurex
 * <p>
 * Description: Import Eurex from a XML file
 *
 * @author Juan Angel Torija & Guillermo Solano
 * @version 2.1 -> small changes
 * @date 15/10/2014
 */

public class ScheduledTaskImportXMLMMOOEurex extends AbstractProcessFeedScheduledTask {

    private static final long serialVersionUID = 123L;

    /**
     * ST attributes
     */
    private static final String TASK_INFORMATION = "Import Eurex from a XML file";
    private static final String MAN_DEFAULT_FUND_EXT_REF = "Ext. Ref. Default Fund";
    private static final String MAN_INITIAL_EXT_REF = "Ext. Ref. Initial Guarantee";
    private static final String EUREX_DEFAULT_FUND_CONTRACT_NAME = "Default Fund Contract Name";
    private static final String EUREX_GARANTIA_INITIAL_CONTRACT_NAME = "Garantia Inicial Contract Name";
    private static final String EUREX_CUENTA_PROPIA_TYPE = "Cuenta Propia sub-type";
    private static final String EUREX_CUENTA_TERCEROS_TYPE = "Cuenta Terceros sub-type";
    private static final String LOG_DIR = "Log Directoy";
    private static final String TRADES_LOG_NAME = "Trades Log File Name";
    private static final String POSITIONS_LOG_NAME = "Positions Log File Name";

    /**
     * Class static predefined data
     */
    private static final String CAMARA = "EUREX";
    // default external reference names
    private static String TRADE_ID_EUREX_INI = "XXX_MMOO_" + CAMARA + "_INI";
    private static String TRADE_ID_EUREX_DEF = "XXX_MMOO_" + CAMARA + "_DEF";
    // default LE
    private static String EUREX_LE = "ECAG";
    // default cuenta propia exposure subtype
    private static String TRADE_TYPE_CUENTA_PROPIA = "MMOO.CUENTAPROPIA";
    // default cuenta terceros exposure subtype
    private static String TRADE_TYPE_CUENTA_TERCEROS = "MMOO.CUENTATERCEROS";

    /**
     * Class constants
     */
    private static final String FILE_00RPTCC060BSAMD = "00RPTCC060BSAMD";
    private static final String FILE_00RPTCD031BSAMD = "00RPTCD031BSAMD";
    /**
     * File name under process
     */
    private final String file = "";
    /**
     * Eurex ECAG Default Fund Collateral Contract
     */
    private CollateralConfig contractDefaultFund = null;
    /**
     * Eurex ECAG Garant?a Inicial Collateral Contract
     */
    private CollateralConfig contractGarantiaInicial = null;

    /**
     * Main process ST EUREX MMOO
     */
    /*
     * Process cd031 Eurex xml schemas - v2.11. Complex logic is implemented: 1?
     * Trade CuentaPropia & PLMark for contract Default Fund (MTM =
     * garantiasCd031[0]) ; 2? same amount for that MtM is the cash final
     * position for same contract (position = garantiasCd031[0]); 3? Cash
     * position is read for contract Garantia Initial (position =
     * garantiasCd031[1]).
     */
    /*
     * Process cc006 Eurex xml schemas - 1? Trade CuentaPropia & PLMark for
     * contract Garantia Inicial (MTM = garantiasCc060[0]) ; 2? Trade
     * CuentaTerceros & PLMark for contract Garantia Inicial (MTM =
     * garantiasCc060[1]). Where Cuenta terceros = label sumClgMbrTolMgnClgCurr
     * - Mtm cuenta propia
     */
    @Override
    public boolean process(final DSConnection ds, final PSConnection ps) {

        final String path = getAttribute(FILEPATH);
        final String startFileName = getAttribute(STARTFILENAME);
        final JDate processDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        final JDate valueDate = getValuationDatetime().getJDate(TimeZone.getDefault()).addBusinessDays(-1, getHolidays());
        ArrayList<String> logErrTrades = new ArrayList<String>();
        ArrayList<String> logPositions = new ArrayList<String>();
        long tradeIdMtm1, tradeIdMtm2, tradeIdPos1, tradeIdPos2;
        tradeIdMtm1 = tradeIdMtm2 = tradeIdPos1 = tradeIdPos2 = 0;

        // get data file: processDate must be same than file attribute date.
        final File file = getFile(path, startFileName, processDate, logErrTrades);

        // creating file failed
        if (file == null) {
            scheduledTaskTradesLog(tradeIdMtm1, tradeIdMtm2, logErrTrades, valueDate, file);
            return false;
        }

        // parse the input file
        final Document document = readDOMDocumentfromFile(file, path, logErrTrades);

        // creating document failed
        if (!logErrTrades.isEmpty()) {
            scheduledTaskTradesLog(tradeIdMtm1, tradeIdMtm2, logErrTrades, valueDate, file);
            return false;
        }

        // read alternative options
        fillOptionalVariable();

        // load EUREX contracts
        if (!loadMMOOEurexCollateralConfigs(logErrTrades)) {
            scheduledTaskTradesLog(tradeIdMtm1, tradeIdMtm2, logErrTrades, valueDate, file);
            return false;
        }

        /*
         * Process cd031 Eurex xml schemas - v2.11. Complex logic is
         * implemented: 1? Trade CuentaPropia & PLMark for contract Default Fund
         * (MTM = garantiasCd031[0]) ; 2? same amount for that MtM is the cash
         * final position for same contract (position = garantiasCd031[0]); 3?
         * Cash position is read for contract Garantia Initial (position =
         * garantiasCd031[1]).
         */
        if (startFileName.equals(FILE_00RPTCD031BSAMD)) {

            // CD031 XML proccesing:
            // Default Fund Cuenta Propia Trade MTM = garantiasCd031[0]
            // Default Fund final cash position = garantiasCd031[0]
            // Garantias Iniciales final cash position = garantiasCd031[1]
            final List<Double> garantiasCd031 = processEurexCd031(document, logErrTrades);

            if (!garantiasCd031.isEmpty() && logErrTrades.isEmpty()) {
                /*
                 * 1?-First Trade CuentaPropia & PLMark for contract Default
                 * Fund
                 */
                // get trade amount
                final Double defFundMtm = -garantiasCd031.get(0);

                // get trade
                final Trade trade = ImportMMOOUtilities.getTrade(CAMARA, TRADE_TYPE_CUENTA_PROPIA, TRADE_ID_EUREX_DEF,
                        EUREX_LE, valueDate, this.contractDefaultFund.getBook());

                // update PlMark
                tradeIdMtm1 = ImportMMOOUtilities.updateTradeAndPLMark(trade, defFundMtm, valueDate, getPricingEnv(),
                        logErrTrades);

                /*
                 * 2?-Second same amount is final cash position for Default Fund
                 */
                // get the Cash Position
                final Double defaultFundCashPosition = ImportMMOOUtilities.fecthTodayCashMarginCallPosition(processDate,
                        this.contractDefaultFund, logErrTrades);

                // movement to be done
                Double posDelta = calculateDeltaMov(defaultFundCashPosition, defFundMtm); // defFundMtm
                // -
                // defaultFundCashPosition;


                /*
                 * 3?-Third Cash position is read from 031 and insert for
                 * contract Garantia Initial (position = garantiasCd031[1]).
                 */
                // get the Cash Position
                final Double garInicialCashPosition = ImportMMOOUtilities.fecthTodayCashMarginCallPosition(processDate,
                        this.contractGarantiaInicial, logErrTrades);

                // movement to be done
                posDelta = calculateDeltaMov(garInicialCashPosition, -garantiasCd031.get(1));


            }

            /*
             * Process cd006 Eurex xml - v2.11. get trade amount (MtM) + update
             * trades & PLMarks -> CUENTAPROPIA + CUENTATERCEROS for contract
             * Garantia Inicial
             */
            /*
             * Process cc006 Eurex xml schemas - 1? Trade CuentaPropia & PLMark
             * for contract Garantia Inicial (MTM = garantiasCc060[0]) ; 2?
             * Trade CuentaTerceros & PLMark for contract Garantia Inicial (MTM
             * = garantiasCc060[1]). Where Cuenta terceros = label
             * sumClgMbrTolMgnClgCurr - Mtm cuenta propia
             */
        } else if (startFileName.equals(FILE_00RPTCC060BSAMD)) {

            // CC060 XML proccesing:
            // Garantia Inicial Cuenta Propia Trade MTM = garantiasCc060[0]
            // Garantia Inicial Cuenta Terceros Trade MTM = garantiasCc060[1]
            final List<Double> garantiasCc060 = processEurexCd060(document, logErrTrades);

            if (logErrTrades.isEmpty()) {

                // get trades amount MtM for cuenta propia & cuenta
                // clientes/Terceros
                double garantiasCuentaPropia = -garantiasCc060.get(0);
                double garantiasCuentaTercer = -garantiasCc060.get(1);

                // get trades
                final Trade tradeCuentaPropia = ImportMMOOUtilities.getTrade(CAMARA, TRADE_TYPE_CUENTA_PROPIA,
                        TRADE_ID_EUREX_INI + "_1", EUREX_LE, valueDate, this.contractGarantiaInicial.getBook());
                // added tag "_1" to differentiate references
                // from second one
                final Trade tradeCuentaTerceros = ImportMMOOUtilities.getTrade(CAMARA, TRADE_TYPE_CUENTA_TERCEROS,
                        TRADE_ID_EUREX_INI + "_2", EUREX_LE, valueDate, this.contractGarantiaInicial.getBook());

                // update PlMarks
                tradeIdMtm1 = ImportMMOOUtilities.updateTradeAndPLMark(tradeCuentaPropia, garantiasCuentaPropia,
                        valueDate, getPricingEnv(), logErrTrades);
                tradeIdMtm2 = ImportMMOOUtilities.updateTradeAndPLMark(tradeCuentaTerceros, garantiasCuentaTercer,
                        valueDate, getPricingEnv(), logErrTrades);
            }
        }

        // log details trades
        scheduledTaskTradesLog(tradeIdMtm1, tradeIdMtm2, logErrTrades, valueDate, file);
        // details positions if processed
        if (startFileName.equals(FILE_00RPTCD031BSAMD)) {
            scheduledTaskPositionsLog(tradeIdPos1, tradeIdPos2, logPositions, valueDate, file);
        }
        ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");

        if (!logErrTrades.isEmpty()) {
            return false;
        }
        // everything OK
        return true;
    }

    /**
     * @param logErrTrades
     * @return true if MMOO Eurex Collateral Configs where loaded
     */
    private boolean loadMMOOEurexCollateralConfigs(final ArrayList<String> logErrTrades) {

        boolean retVal = true;
        String value = super.getAttribute(EUREX_DEFAULT_FUND_CONTRACT_NAME);
        if (!Util.isEmpty(value)) {

            try {
                this.contractDefaultFund = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfigByCode("NAME", value.trim());
            } catch (RemoteException e) {

                this.contractDefaultFund = null;
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
            }

            if (this.contractDefaultFund == null) {
                logErrTrades.add("Could not load EUREX Collateral Config " + value + ". Please, check configuration");
                retVal = false;
            }
        }

        value = super.getAttribute(EUREX_GARANTIA_INITIAL_CONTRACT_NAME);
        if (!Util.isEmpty(value)) {

            try {
                this.contractGarantiaInicial = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfigByCode("NAME", value.trim());
            } catch (RemoteException e) {

                this.contractGarantiaInicial = null;
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
            }

            if (this.contractGarantiaInicial == null) {
                logErrTrades.add("Could not load EUREX Collateral Config " + value + ". Please, check configuration");
                retVal = false;
            }
        }
        return retVal;
    }

    /**
     * @param defaultFundCashPosition
     * @param defFundMtm
     * @return the appropiate delta impact in the position to ensure that is the
     * same than the desired position
     */
    private Double calculateDeltaMov(final Double iniPos, final Double finalPos) {

        Double delta = 0.0d;
        // delta must be - Position + movement
        delta = -iniPos + finalPos;

        return delta;
    }

    /**
     * Parses the document file and captures errors if any occur
     *
     * @param file
     * @param path
     * @param logErrTrades
     * @return the document processed from the xml file
     */
    private Document readDOMDocumentfromFile(final File file, final String path, final ArrayList<String> logErrTrades) {
        // parse the input file
        Document document = null;
        try {

            document = creaDOMDocumentDesdeFich(file.getAbsolutePath());

        } catch (final FileNotFoundException e) {
            Log.error(this, e); //sonar
            logErrTrades.add("Error while looking for file:" + path + file.getName());
            ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
        } catch (final IOException e) {
            Log.error(this, e); //sonar
            logErrTrades.add("Error while reading file:" + path + file.getName());
            ControlMErrorLogger.addError(ErrorCodeEnum.InputFileCanNotBeRead, "");
        } catch (final SAXException e) {
            Log.error(this, e); //sonar
            logErrTrades.add("Error while reading file:" + path + file.getName());
            ControlMErrorLogger.addError(ErrorCodeEnum.InputXMLFileCanNotBeParsed, "");
        }
        return document;
    }

    /**
     * @param xmlDocument
     * @param errors
     * @return
     */
    private List<Double> processEurexCd060(final Document xmlDocument, final ArrayList<String> errors) {

        Cc060 cc060 = null;
        Unmarshaller unmarshall = null;
        JAXBContext context = null;

        try {

            context = JAXBContext.newInstance(Cc060.class);
            unmarshall = context.createUnmarshaller();
            cc060 = (Cc060) unmarshall.unmarshal(xmlDocument);

        } catch (JAXBException e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.getLocalizedMessage());
            Log.error(this, e); //sonar
            errors.add("Error processing CC060 Eurex file: xml does NOT match the schema v2.11");
            return null;
        }

        List<Double> tradesListMtm = getGuaranteesDataCc060(cc060);

        if (tradesListMtm.isEmpty() || (tradesListMtm.size() != 2)) {
            errors.add("cc060 trades couldn't be created. Error parsing xml. See specific log for more details");

        }
        return tradesListMtm;

    }

    /**
     * @param xmlDocument
     * @param errors
     * @return
     */
    private List<Double> processEurexCd031(final Document xmlDocument, final ArrayList<String> errors) {

        Cd031 cd031 = null;
        Unmarshaller unmarshall = null;
        JAXBContext context = null;

        try {

            context = JAXBContext.newInstance(Cd031.class);
            unmarshall = context.createUnmarshaller();
            cd031 = (Cd031) unmarshall.unmarshal(xmlDocument);

        } catch (JAXBException e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.getLocalizedMessage());
            Log.error(this, e); //sonar
            errors.add("Error processing CC060 Eurex file: xml does NOT match the schema v2.11");
            return null;
        }

        List<Double> tradesList = getGuaranteesDataCc031(cd031);
        if (tradesList.isEmpty() || (tradesList.size() != 2)) {
            errors.add("cc060 trades couldn't be created. Error parsing xml. See specific log for more details");

        }

        return tradesList;
    }

    /**
     * @param cc060
     * @return
     */
    private List<Double> getGuaranteesDataCc031(Cd031 cd031) {

        Double cuentaPropiaYPos = 0.0d;
        Double cashPosition = 0.0d;
        Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Starting process cd031");
        // System.out.println("Starting process cd031");

        for (Cd031GrpType g : cd031.getCd031Grps()) {

            if (g.getCd031KeyGrp().getMembClgIdCod().trim().equals("BSAMD")) {

                Log.info(LOG_CATEGORY_SCHEDULED_TASK, g.getCd031KeyGrp().getMembClgIdCod());
                // System.out.println(g.getCd031KeyGrp().getMembClgIdCod());

                for (Cd031Grp1Type t : g.getCd031Grp1s()) {

                    if (t.getCd031KeyGrp1().getPoolId().trim().equals("BSAMDXSTANDARD")) {

                        Log.info(LOG_CATEGORY_SCHEDULED_TASK, t.getCd031KeyGrp1().getPoolId());
                        // System.out.println(t.getCd031KeyGrp1().getPoolId());

                        cashPosition = getNodeCashValue(t);

                    } else if (t.getCd031KeyGrp1().getPoolId().trim().equals("BSAMDXCLEARFUND")) {

                        Log.info(LOG_CATEGORY_SCHEDULED_TASK, t.getCd031KeyGrp1().getPoolId());
                        // System.out.println(t.getCd031KeyGrp1().getPoolId());

                        cuentaPropiaYPos = getNodeCashValue(t);
                    }
                }
            }
        }
        return new ArrayList<Double>(Arrays.asList(new Double[]{cuentaPropiaYPos, cashPosition}));

    }

    /**
     * @param t
     * @return
     */
    private double getNodeCashValue(Cd031Grp1Type t) {

        for (Cd031Grp3Type l : t.getCd031Grp3s()) {

            if (l.getCd031KeyGrp3().getMembExchIdCod().trim().equals("BSAMD")) {

                Log.info(LOG_CATEGORY_SCHEDULED_TASK, l.getCd031KeyGrp3().getMembExchIdCod());
                // System.out.println(l.getCd031KeyGrp3().getMembExchIdCod());

                for (Cd031Grp4Type gr4 : l.getCd031Grp4s()) {

                    if (gr4.getCd031KeyGrp4().getCurrTypCod().trim().equals("EUR")) {

                        Log.info(LOG_CATEGORY_SCHEDULED_TASK, gr4.getCd031KeyGrp4().getCurrTypCod());
                        // System.out.println(gr4.getCd031KeyGrp4().getCurrTypCod());

                        for (Cd031RecType grp5 : gr4.getCd031Recs()) {

                            if (grp5.getIsinCod().trim().equals("CASH")) {

                                Log.info(LOG_CATEGORY_SCHEDULED_TASK, grp5.getIsinCod());
                                // System.out.println(grp5.getIsinCod());

                                return grp5.getSecuCollVal().doubleValue();

                            }
                        }
                    }

                }
            }
        }
        return 0.0d;

    }

    /**
     * @param cc060
     * @return
     */
    private List<Double> getGuaranteesDataCc060(Cc060 cc060) {

        Double cuentaPropia, cuentaTer = 0.0;
        Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Starting process cc060");

        for (Cc060GrpType g : cc060.getCc060Grps()) {

            cuentaTer = g.getSumClgMbrTotMgnClgCurr().doubleValue();

            for (Cc060Grp1Type t : g.getCc060Grp1s()) {

                Log.info(LOG_CATEGORY_SCHEDULED_TASK, t.getCc060KeyGrp1().getPoolId());
                Log.info(LOG_CATEGORY_SCHEDULED_TASK, t.getCc060KeyGrp1().getCurrTypCod());
                // System.out.println(t.getCc060KeyGrp1().getPoolId());
                // System.out.println(t.getCc060KeyGrp1().getCurrTypCod());

                if (t.getCc060KeyGrp1().getPoolId().trim().equals("BSAMDXSTANDARD")
                        && t.getCc060KeyGrp1().getCurrTypCod().trim().equals("EUR")) {

                    for (Cc060Grp3Type n : t.getCc060Grp3s()) {

                        if (n.getCc060KeyGrp3().getMembExchIdCod().trim().equals("BSAMD")) {

                            Log.info(LOG_CATEGORY_SCHEDULED_TASK, n.getCc060KeyGrp3().getMembExchIdCod());
                            // System.out.println(n.getCc060KeyGrp3().getMembExchIdCod());

                            for (Cc060RecType rec : n.getCc060Recs()) {

                                if (rec.getAcctTypGrp().equals(AcctTypGrpType.PP)) {

                                    Log.info(LOG_CATEGORY_SCHEDULED_TASK, n.getCc060KeyGrp3().getMembExchIdCod());
                                    // System.out.println(rec.getAcctTypGrp().toString());

                                    cuentaPropia = rec.getPrtMgnReqt().doubleValue();
                                    cuentaTer = cuentaTer - cuentaPropia; // <sumClgMbrTotMgnClgCurr>+202842102.85</sumClgMbrTotMgnClgCurr>
                                    Log.info(LOG_CATEGORY_SCHEDULED_TASK, "CuentaPropia = " + cuentaPropia);
                                    Log.info(LOG_CATEGORY_SCHEDULED_TASK, "CuentaTerceros = " + cuentaTer);
                                    return new ArrayList<Double>(
                                            Arrays.asList(new Double[]{cuentaPropia, cuentaTer}));

                                } else {
                                    continue;
                                }

                            }
                        } else {
                            continue;
                        }

                    }
                }
            }
        }
        return new ArrayList<Double>(2);
    }

    /**
     * Fills the information of the log
     *
     * @param tradeId1
     * @param tradeId2
     * @param errors
     * @param file2
     * @param file2
     * @param processDate
     */
    private void scheduledTaskTradesLog(long tradeId1, long tradeId2, ArrayList<String> errors, JDate value,
                                        final File file2) {

        if ((errors != null) && (errors.size() > 0)) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, errors.toString());
        }

        String logPath = getAttribute(LOG_DIR);
        final String logName = getAttribute(TRADES_LOG_NAME);
        final String extRef = super.getExternalReference();
        String startFileName = getAttribute(STARTFILENAME);
        if (this.file != null) {
            startFileName = file2.getName();
        }

        if (!logPath.endsWith("/")) {
            logPath = logPath + "/";
        }
        logPath += logName.trim();
        ImportMMOOUtilities.scheduledTaskLog(tradeId1, tradeId2, errors, logPath, startFileName, extRef, value);
    }

    /**
     * Task information
     */
    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        //Gets superclass attributes
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(FILEPATH));
        attributeList.add(attribute(STARTFILENAME));
        attributeList.add(attribute(MAN_DEFAULT_FUND_EXT_REF));
        attributeList.add(attribute(MAN_INITIAL_EXT_REF));
        attributeList.add(attribute(EUREX_DEFAULT_FUND_CONTRACT_NAME));
        attributeList.add(attribute(EUREX_GARANTIA_INITIAL_CONTRACT_NAME));
        attributeList.add(attribute(EUREX_CUENTA_PROPIA_TYPE));
        attributeList.add(attribute(EUREX_CUENTA_TERCEROS_TYPE));
        attributeList.add(attribute(LOG_DIR));
        attributeList.add(attribute(TRADES_LOG_NAME));
        attributeList.add(attribute(POSITIONS_LOG_NAME));

        return attributeList;
    }

    // @SuppressWarnings({ "rawtypes", "unchecked" })
    // @Override
    // public Vector getDomainAttributes() {
    //
    // final Vector v = new Vector();
    // v.addElement(FILEPATH);
    // v.addElement(STARTFILENAME);
    // v.addElement(MAN_DEFAULT_FUND_EXT_REF);
    // v.addElement(MAN_INITIAL_EXT_REF);
    // v.addElement(EUREX_DEFAULT_FUND_CONTRACT_NAME);
    // v.addElement(EUREX_GARANTIA_INITIAL_CONTRACT_NAME);
    // v.addElement(EUREX_CUENTA_PROPIA_TYPE);
    // v.addElement(EUREX_CUENTA_TERCEROS_TYPE);
    // v.addElement(LOG_DIR);
    // v.addElement(TRADES_LOG_NAME);
    // v.addElement(POSITIONS_LOG_NAME);
    //
    // return v;
    // }
    //
    // /**
    // * @param attribute
    // * name
    // * @param hastable
    // * with the attributes declared
    // * @return a vector with the values for the attribute name
    // */
    // @SuppressWarnings({ "rawtypes", "unchecked" })
    // @Override
    // public Vector getAttributeDomain(String attribute, Hashtable hashtable) {
    //
    // Vector<String> vector = super.getAttributeDomain(attribute, hashtable);
    // return vector;
    // }

    /**
     * Ensures that the attributes have a value introduced by who has setup the
     * schedule task
     *
     * @return if the attributes are ok
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean isValidInput(@SuppressWarnings("rawtypes") final Vector messages) {

        boolean retVal = super.isValidInput(messages);
        String value = super.getAttribute(STARTFILENAME);
        if (Util.isEmpty(value)) {
            messages.addElement(STARTFILENAME + " attribute cannot be empty ");
            retVal = false;
        }

        value = super.getAttribute(MAN_DEFAULT_FUND_EXT_REF);
        if (Util.isEmpty(value)) {
            messages.addElement(MAN_DEFAULT_FUND_EXT_REF + " attribute cannot be empty ");
            retVal = false;
        }

        value = super.getAttribute(MAN_INITIAL_EXT_REF);
        if (Util.isEmpty(value)) {
            messages.addElement(MAN_INITIAL_EXT_REF + " attribute cannot be empty ");
            retVal = false;
        }

        // EUREX_DEFAULT_FUND_CONTRACT_NAME,
        // EUREX_GARANTIA_INITIAL_CONTRACT_NAME
        value = super.getAttribute(EUREX_DEFAULT_FUND_CONTRACT_NAME);
        if (!Util.isEmpty(value)) {

            try {
                this.contractDefaultFund = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfigByCode("NAME", value.trim());
            } catch (RemoteException e) {

                this.contractDefaultFund = null;
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
            }

            if (this.contractDefaultFund == null) {
                messages.addElement("Default Fund Contract Name does not exist in the System. Please check attribute "
                        + EUREX_DEFAULT_FUND_CONTRACT_NAME);
                retVal = false;
            }
        }

        value = super.getAttribute(EUREX_GARANTIA_INITIAL_CONTRACT_NAME);
        if (!Util.isEmpty(value)) {

            try {
                this.contractGarantiaInicial = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfigByCode("NAME", value.trim());
            } catch (RemoteException e) {

                this.contractGarantiaInicial = null;
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
            }

            if (this.contractGarantiaInicial == null) {
                messages.addElement(
                        "Garantia Initial Contract Name does not exist in the System. Please check attribute "
                                + EUREX_GARANTIA_INITIAL_CONTRACT_NAME);
                retVal = false;
            }
        }

        // value = super.getAttribute(EUREX_BOOK);
        // if (!Util.isEmpty(value)) {
        //
        // Book b = BOCache.getBook(DSConnection.getDefault(), value.trim());
        // if (b == null) {
        // messages.addElement("BOOK does not exist in the System. Please check
        // attribute " + EUREX_BOOK);
        // }
        // }

        value = super.getAttribute(LOG_DIR);
        if (Util.isEmpty(value)) {

            messages.addElement(LOG_DIR + " MANDATORY attribute not specified.");
            retVal = false;
        }

        value = super.getAttribute(TRADES_LOG_NAME);
        if (Util.isEmpty(value)) {

            messages.addElement(TRADES_LOG_NAME + " MANDATORY attribute not specified.");
            retVal = false;
        }

        value = super.getAttribute(POSITIONS_LOG_NAME);
        if (Util.isEmpty(value)) {

            messages.addElement(POSITIONS_LOG_NAME + " MANDATORY attribute not specified.");
            retVal = false;
        }

        return retVal;
    }

    // ***** FILES & XML PROCESSING STUFF *** //

    /**
     * Fills the information of the log for the Positions trades
     *
     * @param tradeIdPos2
     * @param log
     * @param dataFile
     * @param processDate
     */
    private void scheduledTaskPositionsLog(Long tradeIdPos1, Long tradeIdPos2, List<String> log,
                                           final JDate value, final File dataFile) {

        if (!Util.isEmpty(log)) {
            Log.error(this, log.toString());
        }

        String logPath = getAttribute(LOG_DIR);
        final String logName = getAttribute(POSITIONS_LOG_NAME);
        final String startFileName = getAttribute(STARTFILENAME);
        String fileName = startFileName;
        if (dataFile != null) {
            fileName = dataFile.getName();
        }

        final String extRef = super.getExternalReference();
        if (!logPath.endsWith("/")) {
            logPath = logPath + "/";
        }
        logPath += logName.trim();
        List<Long> tradeIds = new ArrayList<>(Arrays.asList(new Long[]{tradeIdPos1, tradeIdPos2}));
        ImportMMOOUtilities.scheduledTaskLog_Depo(tradeIds, log, logPath, fileName, extRef, value);

    }

    /**
     * This method overrides optional configurations like default LE External
     * reference for Initial and Default fund trades Trades subtypes for cuenta
     * propia and cuenta terceros
     */
    private void fillOptionalVariable() {

        String attribute = super.getAttribute(MAN_DEFAULT_FUND_EXT_REF);
        // fill the external reference
        if (!(Util.isEmpty(attribute))) {
            TRADE_ID_EUREX_DEF = attribute.trim();
        }

        attribute = super.getAttribute(MAN_INITIAL_EXT_REF);
        if (!(Util.isEmpty(attribute))) {
            TRADE_ID_EUREX_INI = attribute.trim();
        }

        // sub type cuenta propia name
        attribute = super.getAttribute(EUREX_CUENTA_PROPIA_TYPE);
        if (!(Util.isEmpty(attribute))) {
            TRADE_TYPE_CUENTA_PROPIA = attribute.trim();
        }

        attribute = super.getAttribute(EUREX_CUENTA_TERCEROS_TYPE);
        if (!(Util.isEmpty(attribute))) {
            TRADE_TYPE_CUENTA_TERCEROS = attribute.trim();
        }
    }

    /**
     * Obtiene el document raiz a partir de una URL a un fichero
     *
     * @param URLArchivo el path del fichero xml
     * @return el document del fichero
     * @throws IOException
     * @throws SAXException
     * @throws AdSysException Fichero no encontrado o error al parsearlo.
     */
    public static Document creaDOMDocumentDesdeFich(final String URLArchivo)
            throws SAXException, IOException, FileNotFoundException {
        InputStream streamXML = null;
        Document retornoDoc;
        streamXML = new FileInputStream(new File(URLArchivo));
        retornoDoc = DOMUtility.createDOMDocument(streamXML);

        return retornoDoc;
    }

    @Override
    public String getFileName() {
        return this.file;
    }

    /**
     * @param path
     * @param fileName
     * @param date
     * @return Get file from path and specific date from file attributes
     */
    public File getFile(String path, String fileName, JDate date, ArrayList<String> errors) {

        final String fileNameFilter = fileName;
        // name filter
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File directory, String fileName) {
                return fileName.startsWith(fileNameFilter);
            }
        };

        final File directory = new File(path);
        final File[] listFiles = directory.listFiles(filter);

        for (File file : listFiles) {

            final Long dateFileMilis = file.lastModified();
            final Date dateFile = new Date(dateFileMilis);
            final JDate jdateFile = JDate.valueOf(dateFile);

            if (JDate.diff(date, jdateFile) == 0) {
                return file;
            }
        }
        if (errors != null) {
            errors.add("Error getting file with start name " + fileName);
        }
        return null;
    }

    // ////////////////////////////////
    // ////// DEPRECATED /////////////
    // //////////////////////////////
    /**
     * Process calculations for obtain Cash Eurex value (report 031)
     *
     * @param document
     * @return
     */
    // private double getCashEurex031(Document document) {
    //
    // double secuCollVal = 0.00;
    //
    // // Check if the root node is OK
    // if (checkRootNode031(document)) {
    //
    // // Get the root node
    // final Node rootNode =
    // DOMUtility.getFirstChildElementRecursivoObligatorio(document,
    // FIELD_CD031GRP, true);
    //
    // // Get first node
    // Node nodoDef = getFatherNodeEurex031(rootNode);
    //
    // // Get secuCollVal
    // if (nodoDef != null) {
    // secuCollVal = getsecuColVal031(nodoDef);
    // }
    //
    // }
    // return secuCollVal;
    //
    // }

    /**
     * Process calculations for obtain guarantees Required from Eurex (report
     * 060)
     *
     * @param document
     * @return
     */
    // private double[] getGuaranteesRequiredEurex060(Document document) {
    //
    // double garantiaCuentaPropia = 0.00;
    // double garantiaCuentaCliente = 0.00;
    // double sumClgMbrTotMgnClgCurr = 0.00;
    //
    // Node secondNodo = null;
    // // Check if the root node is OK
    // if (checkRootNode060(document)) {
    //
    // // Get the root node
    // final Node rootNode =
    // DOMUtility.getFirstChildElementRecursivoObligatorio(document,
    // FIELD_CC060GRP, true);
    //
    // // Get first node
    // Node firstNodo = getFatherNodeEurex060(rootNode);
    //
    // // Get second node
    // if (firstNodo != null) {
    // secondNodo = getSecondNodeEurex060(firstNodo);
    // }
    //
    // // Get total of guarantees Required from Eurex
    // if (secondNodo != null) {
    //
    // // Get node to obtain sumClgMbrTotMgnClgCurr value
    // final Node rootNode2 =
    // DOMUtility.getFirstChildElementRecursivoObligatorio(document,
    // "sumClgMbrTotMgnClgCurr", true);
    //
    // // Obtain sumClgMbrTotMgnClgCurr value
    // sumClgMbrTotMgnClgCurr =
    // Double.parseDouble(rootNode2.getFirstChild().getTextContent());
    //
    // // Get Garantia cuenta propia
    // garantiaCuentaPropia = getTotalGuaranteesRequired060(secondNodo);
    //
    // // Get Garantia cuenta cliente
    // garantiaCuentaCliente = sumClgMbrTotMgnClgCurr - garantiaCuentaPropia;
    //
    // }
    // }
    //
    // double total[] = { garantiaCuentaPropia, garantiaCuentaCliente };
    //
    // return total;
    // }

    /**
     * Get true or false if the rootNode is empty or not
     *
     * @param document
     * @return
     */
    // private boolean checkRootNode031(Document document) {
    // final Node rootNode =
    // DOMUtility.getFirstChildElementRecursivoObligatorio(document, "cd031Grp",
    // true);
    // if (rootNode == null) {
    // Log.error(LOG_CATEGORY_SCHEDULED_TASK, "The XML file is empty");
    // return false;
    // } else {
    // return true;
    // }
    // }

    /**
     * Get true or false if the rootNode is empty or not
     *
     * @param document
     * @return
     */
    // private boolean checkRootNode060(Document document) {
    // final Node rootNode =
    // DOMUtility.getFirstChildElementRecursivoObligatorio(document, "cc060Grp",
    // true);
    // final Node rootNode2 =
    // DOMUtility.getFirstChildElementRecursivoObligatorio(document,
    // "sumClgMbrTotMgnClgCurr",
    // true);
    // if ((rootNode == null) || (rootNode2 == null)) {
    // Log.error(LOG_CATEGORY_SCHEDULED_TASK, "The XML file is empty ");
    // return false;
    // } else {
    // return true;
    // }
    // }

    /**
     * Get first node of Eurex 031
     *
     * @param rootNode
     * @return
     */
    // private Node getFatherNodeEurex031(Node rootNode) {
    //
    // final NodeList listaNodosConectores = rootNode.getChildNodes();
    //
    // List<Node> nodos = new ArrayList<Node>();
    // for (int i = 0; i < listaNodosConectores.getLength(); i++) {
    // if (listaNodosConectores.item(i) != null) {
    // nodos.add(listaNodosConectores.item(i));
    // }
    // }
    //
    // NodeList listaNodosConectores2;
    // List<Node> nodos2 = new ArrayList<Node>();
    //
    // NodeList listaNodosConectores3;
    // List<Node> nodos3 = new ArrayList<Node>();
    // Node nodoDef = null;
    // for (int i = 0; i < nodos.size(); i++) {
    // if (nodos.get(i).toString().equals("[cd031Grp1: null]")) {
    //
    // listaNodosConectores2 = nodos.get(i).getChildNodes();
    //
    // for (int j = 0; j < listaNodosConectores2.getLength(); j++) {
    // if (listaNodosConectores2.item(j) != null) {
    // nodos2.add(listaNodosConectores2.item(j));
    // }
    // }
    //
    // for (int j = 0; j < nodos2.size(); j++) {
    // if (nodos2.get(j).toString().equals("[cd031KeyGrp1: null]")) {
    //
    // listaNodosConectores3 = nodos2.get(j).getChildNodes();
    //
    // for (int x = 0; x < listaNodosConectores3.getLength(); x++) {
    // if (listaNodosConectores3.item(x) != null) {
    // nodos3.add(listaNodosConectores3.item(x));
    // }
    // }
    //
    // for (int x = 0; x < nodos3.size(); x++) {
    // if (nodos3.get(x).toString().equals("[poolId: null]")) {
    // if (nodos3.get(x).getFirstChild().toString().contains("BSAMDXCLEARFUND"))
    // {
    // nodoDef = nodos.get(i);
    // return nodoDef;
    // }
    // }
    // }
    //
    // }
    // }
    // }
    // nodos2.clear();
    // nodos3.clear();
    // }
    //
    // return nodoDef;
    // }

    /**
     * Get first node of Eurex 060
     *
     * @param rootNode
     * @return
     */
    // private Node getFatherNodeEurex060(Node rootNode) {
    //
    // final NodeList listaNodosConectores = rootNode.getChildNodes();
    //
    // List<Node> nodos = new ArrayList<Node>();
    // for (int i = 0; i < listaNodosConectores.getLength(); i++) {
    // if (listaNodosConectores.item(i) != null) {
    // nodos.add(listaNodosConectores.item(i));
    // }
    // }
    //
    // boolean flagPoolID = false;
    // boolean flagCurrency = false;
    // NodeList listaNodosConectores2;
    // NodeList listaNodosConectores3;
    // List<Node> nodos2 = new ArrayList<Node>();
    // List<Node> nodos3 = new ArrayList<Node>();
    //
    // Node group1 = null;
    //
    // for (int i = 0; (i < nodos.size()) && ((flagPoolID == false) &&
    // (flagCurrency == false)); i++) {
    // if (((flagPoolID == false) && (flagCurrency == false))
    // && nodos.get(i).toString().equals("[cc060Grp1: null]")) {
    //
    // listaNodosConectores2 = nodos.get(i).getChildNodes();
    //
    // for (int j = 0; j < listaNodosConectores2.getLength(); j++) {
    // if (listaNodosConectores2.item(j) != null) {
    // nodos2.add(listaNodosConectores2.item(j));
    // }
    // }
    //
    // for (int j = 0; (j < nodos2.size()) && ((flagPoolID == false) &&
    // (flagCurrency == false)); j++) {
    // if (((flagPoolID == false) && (flagCurrency == false))
    // && nodos2.get(j).toString().equals("[cc060KeyGrp1: null]")) {
    //
    // listaNodosConectores3 = nodos2.get(j).getChildNodes();
    //
    // for (int x = 0; x < listaNodosConectores3.getLength(); x++) {
    // if (listaNodosConectores3.item(x) != null) {
    // nodos3.add(listaNodosConectores3.item(x));
    // }
    // }
    //
    // for (int x = 0; (x < nodos3.size()) && ((flagPoolID == false) ||
    // (flagCurrency == false)); x++) {
    // if (nodos3.get(x).toString().equals("[poolId: null]")) {
    // if (nodos3.get(x).getFirstChild().toString().contains("BSAMDXSTANDARD"))
    // {
    // flagPoolID = true;
    // }
    // } else if (nodos3.get(x).toString().equals("[currTypCod: null]")) {
    // if (nodos3.get(x).getFirstChild().toString().contains("EUR")) {
    // flagCurrency = true;
    // }
    // }
    // }
    // if ((flagPoolID == true) && (flagCurrency == true)) {
    // group1 = nodos.get(i);
    // } else {
    // flagPoolID = false;
    // flagCurrency = false;
    // }
    // }
    // }
    // }
    // nodos2.clear();
    // nodos3.clear();
    // }
    // return group1;
    // }

    /**
     * Get second node of Eurex 060
     *
     * @param rootNode
     * @return
     */
    // private Node getSecondNodeEurex060(Node rootNode) {
    //
    // NodeList listaNodosDef = rootNode.getChildNodes();
    // Node group2 = null;
    //
    // boolean flagDef = false;
    //
    // NodeList listaNodosDef2;
    // NodeList listaNodosDef3;
    //
    // List<Node> nodosDef1 = new ArrayList<Node>();
    // List<Node> nodosDef2 = new ArrayList<Node>();
    // List<Node> nodosDef3 = new ArrayList<Node>();
    //
    // for (int i = 0; i < listaNodosDef.getLength(); i++) {
    // if (listaNodosDef.item(i) != null) {
    // nodosDef1.add(listaNodosDef.item(i));
    // }
    // }
    //
    // for (int i = 0; (flagDef == false) && (i < nodosDef1.size()); i++) {
    // if (nodosDef1.get(i).toString().equals("[cc060Grp3: null]")) {
    //
    // listaNodosDef2 = nodosDef1.get(i).getChildNodes();
    //
    // for (int j = 0; j < listaNodosDef2.getLength(); j++) {
    // if (listaNodosDef2.item(j) != null) {
    // nodosDef2.add(listaNodosDef2.item(j));
    // }
    // }
    //
    // for (int j = 0; (flagDef == false) && (j < nodosDef2.size()); j++) {
    // if (nodosDef2.get(j).toString().equals("[cc060KeyGrp3: null]")) {
    //
    // listaNodosDef3 = nodosDef2.get(j).getChildNodes();
    //
    // for (int x = 0; x < listaNodosDef3.getLength(); x++) {
    // if (listaNodosDef3.item(x) != null) {
    // nodosDef3.add(listaNodosDef3.item(x));
    // }
    // }
    //
    // for (int x = 0; (flagDef == false) && (x < nodosDef3.size()); x++) {
    // if (nodosDef3.get(x).toString().equals("[membExchIdCod: null]")) {
    // if (nodosDef3.get(x).getFirstChild().toString().contains("BSAMD")) {
    // flagDef = true;
    // group2 = nodosDef1.get(i);
    // }
    // }
    // }
    // }
    // }
    // }
    // nodosDef2.clear();
    // nodosDef3.clear();
    // }
    // return group2;
    // }

    /**
     * Get and calculate secuColVal from Eurex Document 031
     *
     * @param rootNode
     * @return
     */
    // private double getsecuColVal031(Node nodoDef) {
    //
    // double secuCollVal = 0.00;
    //
    // NodeList listaNodosDef = nodoDef.getChildNodes();
    //
    // List<Node> nodosDef1 = new ArrayList<Node>();
    // for (int i = 0; i < listaNodosDef.getLength(); i++) {
    // if (listaNodosDef.item(i) != null) {
    // nodosDef1.add(listaNodosDef.item(i));
    // }
    // }
    //
    // NodeList listaNodosDef2;
    // List<Node> nodosDef2 = new ArrayList<Node>();
    // NodeList listaNodosDef3;
    // List<Node> nodosDef3 = new ArrayList<Node>();
    // NodeList listaNodosDef4;
    // List<Node> nodosDef4 = new ArrayList<Node>();
    // boolean flagDef = false;
    //
    // for (int i = 0; (flagDef == false) && (i < nodosDef1.size()); i++) {
    // if (nodosDef1.get(i).toString().equals("[cd031Grp3: null]")) {
    //
    // listaNodosDef2 = nodosDef1.get(i).getChildNodes();
    //
    // for (int j = 0; j < listaNodosDef2.getLength(); j++) {
    // if (listaNodosDef2.item(j) != null) {
    // nodosDef2.add(listaNodosDef2.item(j));
    // }
    // }
    //
    // for (int j = 0; j < nodosDef2.size(); j++) {
    // if (nodosDef2.get(j).toString().equals("[cd031Grp4: null]")) {
    //
    // listaNodosDef3 = nodosDef2.get(j).getChildNodes();
    //
    // for (int x = 0; x < listaNodosDef3.getLength(); x++) {
    // if (listaNodosDef3.item(x) != null) {
    // nodosDef3.add(listaNodosDef3.item(x));
    // }
    // }
    //
    // for (int x = 0; x < nodosDef3.size(); x++) {
    // if (nodosDef3.get(x).toString().equals("[cd031Rec: null]")) {
    //
    // listaNodosDef4 = nodosDef3.get(x).getChildNodes();
    //
    // for (int y = 0; y < listaNodosDef4.getLength(); y++) {
    // if (listaNodosDef4.item(y) != null) {
    // nodosDef4.add(listaNodosDef4.item(y));
    // }
    // }
    //
    // for (int y = 0; y < nodosDef4.size(); y++) {
    // if (nodosDef4.get(y).toString().equals("[secuCollVal: null]")) {
    // secuCollVal = Double.parseDouble(nodosDef4.get(y).getFirstChild()
    // .getTextContent());
    // flagDef = true;
    // }
    // }
    // }
    // }
    // }
    // }
    // }
    // nodosDef2.clear();
    // nodosDef3.clear();
    // nodosDef4.clear();
    // }
    //
    // return secuCollVal;
    // }

    /**
     * Get and calculate total of guarantees required from Eurex Document 060
     *
     * @param secondNodo
     * @return
     */
    // private double getTotalGuaranteesRequired060(Node secondNodo) {
    //
    // double prtMgnReqt = 0.00;
    // NodeList listaNodosFin = secondNodo.getChildNodes();
    //
    // boolean flagFin = false;
    // boolean flagPrtMgn = false;
    //
    // NodeList listaNodosFin2;
    //
    // List<Node> nodosFin1 = new ArrayList<Node>();
    // List<Node> nodosFin2 = new ArrayList<Node>();
    //
    // for (int i = 0; i < listaNodosFin.getLength(); i++) {
    // if (listaNodosFin.item(i) != null) {
    // nodosFin1.add(listaNodosFin.item(i));
    // }
    // }
    //
    // for (int i = 0; ((flagFin == false) && (flagPrtMgn == false)) && (i <
    // nodosFin1.size()); i++) {
    // if (nodosFin1.get(i).toString().equals("[cc060Rec: null]")) {
    //
    // listaNodosFin2 = nodosFin1.get(i).getChildNodes();
    //
    // for (int j = 0; j < listaNodosFin2.getLength(); j++) {
    // if (listaNodosFin2.item(j) != null) {
    // nodosFin2.add(listaNodosFin2.item(j));
    // }
    // }
    //
    // for (int j = 0; ((flagFin == false) || (flagPrtMgn == false)) && (j <
    // nodosFin2.size()); j++) {
    // if (nodosFin2.get(j).toString().equals("[acctTypGrp: null]")) {
    // if (nodosFin2.get(j).getFirstChild().toString().contains("PP")) {
    // flagFin = true;
    // }
    // }
    // if (nodosFin2.get(j).toString().equals("[prtMgnReqt: null]")) {
    // if (flagFin == true) {
    // flagPrtMgn = true;
    // prtMgnReqt =
    // Double.parseDouble(nodosFin2.get(j).getFirstChild().getTextContent());
    // }
    // }
    // }
    // if ((flagFin == false) || (flagPrtMgn == false)) {
    // flagFin = false;
    // flagPrtMgn = false;
    // }
    //
    // }
    // nodosFin2.clear();
    // }
    // return prtMgnReqt;
    //
    // }

}