package calypsox.tk.util;

import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScheduledTaskCUSTOM_ECMS_DeltaReportLoad extends ScheduledTask {

    private static final String DELTA_FILE_PATH = "/calypso_interfaces/ecms/import/";
    private static final String DELTA_FILE_NAME = "Eligible_Assets_Update.xml";
    public static final String ISIN = "ISIN";

    private DSConnection dsCon;


    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {

        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute("Delta File Path:"));

        return attributeList;
    }

    @Override
    public String getTaskInformation() {
        return "This ScheduledTask loads the delta ECMS File and updates the Calypso information to get the eligibility of the ISINs.";
    }


    public boolean process(DSConnection ds, PSConnection ps) {
        this.dsCon = ds;

        JDatetime date = dsCon.getServerCurrentDatetime();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String formattedDate = formatter.format(date);

        String fileName = DELTA_FILE_PATH + DELTA_FILE_NAME.replace("DDMMYYYY", formattedDate);

        //we read the file line by line and analyze every block
        readFile(fileName);

        //we return true if everything was ok.
        return true;
    }

    private void readFile(String fileName) {

        try {
            if (this.getAttribute("Delta File Path:") != null && !this.getAttribute("Delta File Path:").isEmpty()) {
                fileName = this.getAttribute("Delta File Path:") + DELTA_FILE_NAME;
            }

            File file = new File(fileName);
            DocumentBuilderFactory dBf = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = dBf.newDocumentBuilder();
            Document document = documentBuilder.parse(file);

            NodeList isins = document.getElementsByTagName("isinCode");
            NodeList issuerNames = document.getElementsByTagName("issuerName");
            NodeList nonOwnUseHaircuts = document.getElementsByTagName("nonOwnUseHaircut");
            NodeList ownUseHaircuts = document.getElementsByTagName("ownUseHaircut");
            NodeList pOwnUsableCoveredBonds = document.getElementsByTagName("potentiallyOwnUsableCoveredBond");
            NodeList changes = document.getElementsByTagName("change");

            for (int position = 0; position <= isins.getLength() - 1; position++) {
                String isin = isins.item(position).getTextContent();
                String issuerName = issuerNames.item(position).getTextContent();
                String nonOwnUseHaircut = nonOwnUseHaircuts.item(position).getTextContent();
                String ownUseHaircut = ownUseHaircuts.item(position).getTextContent();
                String pOwnUsableCoveredBond = pOwnUsableCoveredBonds.item(position).getTextContent();
                String change = changes.item(position).getTextContent();
                checkIsinEligibility(isin, issuerName, nonOwnUseHaircut, ownUseHaircut, pOwnUsableCoveredBond, change);
            }


        } catch (IOException | ParserConfigurationException | SAXException e) {
            Log.error(this, "Error reading file: " + fileName + "\n" + e);
        }

    }


    private void checkIsinEligibility(String isin, String issuerName, String nonOwnUseHaircut, String ownUseHaircut, String pOnwUseCoveredBond, String change) {
        //we check the conditions for this isin
        //Revisamos si el ISIN existe en calypso
        Product product = isinExists(isin);
        boolean modified = false;

        if (product != null) {
            final Date d = new Date();
            String dateEcms = "";
            SimpleDateFormat timeFormat = new SimpleDateFormat("ddMMyyyy");
            synchronized (timeFormat) {
                dateEcms = timeFormat.format(d);
            }

            String oldBSTE = this.getEligibility_BSTE(product);
            String oldBFOM = this.getEligibility_BFOM(product);
            String oldHaircut = this.getHaircut(product);
            String oldOwnUseHaircut = this.getHaircutOwnUse(product);

            //si existe el isin, comprobamos el campoChange
            if (change != null && !change.isEmpty() &&
                    (change.equalsIgnoreCase("D") ||
                            (change.equalsIgnoreCase("U")
                                    && pOnwUseCoveredBond.equalsIgnoreCase("N")
                                    && (checkIssuerName(issuerName) || checkConsumerName(issuerName))
                                    && checkWasEligible(oldBFOM, oldBSTE)))) {
                //Si el campo change esta relleno con D se fijan a vacio los SecCode “ECMS_Eligibility_BSTE”, “ECMS_Eligibility_BFOM”, “ECMS_Haircut” y “ECMS_Haircut_Own_Use”.
                if (oldBSTE != null && !oldBSTE.equalsIgnoreCase("")) {
                    setEligibility_BSTE(product, "");
                }

                if (oldBFOM != null && !oldBFOM.equalsIgnoreCase("")) {
                    setEligibility_BFOM(product, "");
                }

                if (oldHaircut != null && !oldHaircut.equalsIgnoreCase("")) {
                    setHaircut(product, "");
                }

                if (oldOwnUseHaircut != null && !oldOwnUseHaircut.equalsIgnoreCase("")) {
                    setHaircutOwnUse(product, "");
                }
                setNonElegibility_DATE(product, dateEcms);
                modified = true;

            } else if (change != null && !change.isEmpty() && (change.equalsIgnoreCase("N") || change.equalsIgnoreCase("U"))) {
                //si el ISIN ya es no elegible y el campo change tiene valor "U", vuelve a ser elegible
                if (checkNonElegibility_DATE(product) && change.equalsIgnoreCase("U")) {
                    if (checkIssuerName(issuerName)){
                        setEligibility_BSTE(product, "Y");
                        setHaircutOwnUse(product, ownUseHaircut);
                    } else if (checkConsumerName(issuerName)){
                        setEligibility_BFOM(product, "Y");
                        setHaircutOwnUse(product, ownUseHaircut);
                    } else{
                        setEligibility_BSTE(product, "Y");
                        setEligibility_BFOM(product, "Y");
                    }
                    setHaircut(product, nonOwnUseHaircut);
                    unsetNonElegibility_DATE(product);
                    modified = true;
                } else {
                    if (checkIssuerName(issuerName)) {
                        //si el issuer es Santander SA
                        if (checkOwnUsableCoveredOwn(pOnwUseCoveredBond)) {
                            //si tiene valor Y,
                            // se fijara el “ECMS_Eligibility_BSTE” con valor “Y”,
                            // el “ECMS_Haircut_Own_Use” se informara con el valor del campo HAIRCUT_OWN_USE
                            // y el ECMS_Haircut se informara con el valor del campo HAIRCUT.
                            // El SecCode “ECMS_Eligibility_BFOM” se informara a vacio.
                            if (oldBSTE == null || !oldBSTE.equalsIgnoreCase("Y")) {
                                setEligibility_BSTE(product, "Y");
                                unsetNonElegibility_DATE(product);
                                modified = true;
                            }

                            if (oldBFOM != null && !oldBFOM.equalsIgnoreCase("")) {
                                setEligibility_BFOM(product, "");
                                modified = true;
                            }

                            if (oldHaircut == null || !oldHaircut.equalsIgnoreCase(nonOwnUseHaircut)) {
                                setHaircut(product, nonOwnUseHaircut);
                                modified = true;
                            }

                            if (oldOwnUseHaircut == null || !oldOwnUseHaircut.equalsIgnoreCase(ownUseHaircut)) {
                                setHaircutOwnUse(product, ownUseHaircut);
                                modified = true;
                            }


                        } else {
                            //si tiene valor N, entonces se fijan a vacio los SecCode “ECMS_Eligibility” , “ECMS_Haircut” y “ECMS_Haircut_Own_Use”.
                            if (oldBSTE != null && !oldBSTE.equalsIgnoreCase("")) {
                                setEligibility_BSTE(product, "");
                                modified = true;
                            }

                            if (oldBFOM != null && !oldBFOM.equalsIgnoreCase("")) {
                                setEligibility_BFOM(product, "");
                                modified = true;
                            }

                            if (oldHaircut != null && !oldHaircut.equalsIgnoreCase("")) {
                                setHaircut(product, "");
                                modified = true;
                            }

                            if (oldOwnUseHaircut != null && !oldOwnUseHaircut.equalsIgnoreCase("")) {
                                setHaircutOwnUse(product, "");
                                modified = true;
                            }
                        }
                    } else if (checkConsumerName(issuerName)) {
                        //si el issuer es Santander Consumer
                        if (checkOwnUsableCoveredOwn(pOnwUseCoveredBond)) {
                            //si tiene valor Y,
                            // “ECMS_Eligibility_BFOM” con valor “Y”,
                            // el “ECMS_Haircut_Own_Use” con el valor del campo HAIRCUT_OWN_USE
                            // y el ECMS_Haircut se informara con el valor del campo HAIRCUT.
                            // El SecCode “ECMS_Eligibility_BSTE” se informara a vacio.
                            if (oldBSTE != null && !oldBSTE.equalsIgnoreCase("")) {
                                setEligibility_BSTE(product, "");
                                modified = true;
                            }

                            if (oldBFOM == null || !oldBFOM.equalsIgnoreCase("Y")) {
                                setEligibility_BFOM(product, "Y");
                                unsetNonElegibility_DATE(product);
                                modified = true;
                            }

                            if (oldHaircut == null || oldHaircut.equalsIgnoreCase(nonOwnUseHaircut)) {
                                setHaircut(product, nonOwnUseHaircut);
                                modified = true;
                            }

                            if (oldOwnUseHaircut == null || !oldOwnUseHaircut.equalsIgnoreCase(ownUseHaircut)) {
                                setHaircutOwnUse(product, ownUseHaircut);
                                modified = true;
                            }
                        } else {
                            //si tiene valor N, entonces se fijan a vacio los SecCode “ECMS_Eligibility_BSTE” , “ECMS_Eligibility_BFOM”, “ECMS_Haircut” y “ECMS_Haircut_Own_Use”.

                            if (oldBSTE != null && !oldBSTE.equalsIgnoreCase("")) {
                                setEligibility_BSTE(product, "");
                                modified = true;
                            }

                            if (oldBFOM != null && !oldBFOM.equalsIgnoreCase("")) {
                                setEligibility_BFOM(product, "");
                                modified = true;
                            }

                            if (oldHaircut != null && !oldHaircut.equalsIgnoreCase("")) {
                                setHaircut(product, "");
                                modified = true;
                            }

                            if(oldOwnUseHaircut != null && !oldOwnUseHaircut.equalsIgnoreCase("")){
                                setHaircutOwnUse(product, "");
                                modified = true;
                            }

                        }
                    } else {
                        //si el isser no es ninguno de los anteriores,
                        // se fijaran los SecCode “ECMS_Eligibility_BSTE” y  “ECMS_Eligibility_BFOM”  con valor “Y”
                        // y el “ECMS_Haircut” se informara con el valor del campo HAIRCUT.
                        // El SecCode “ECMS_Haircut_Own_Use” se fijara a vacio.
                        if(oldBSTE == null || !oldBSTE.equalsIgnoreCase("Y")){
                            setEligibility_BSTE(product, "Y");
                            unsetNonElegibility_DATE(product);
                            modified = true;
                        }

                        if(oldBFOM == null || !oldBFOM.equalsIgnoreCase("Y")){
                            setEligibility_BFOM(product, "Y");
                            unsetNonElegibility_DATE(product);
                            modified = true;
                        }

                        if(oldHaircut == null || !oldHaircut.equalsIgnoreCase(nonOwnUseHaircut)){
                            setHaircut(product, nonOwnUseHaircut);
                            modified = true;
                        }

                        if(oldOwnUseHaircut != null && !oldOwnUseHaircut.equalsIgnoreCase("")) {
                            setHaircutOwnUse(product, "");
                            modified = true;
                        }
                    }
                }

            }

            if (modified) {
                saveProduct(product);
            }

        }

    }

    private boolean checkWasEligible(String oldBFOM, String oldBSTE) {
        if(oldBFOM != null && !oldBFOM.isEmpty() && oldBFOM.equalsIgnoreCase("Y")){
            return true;
        } else if(oldBSTE != null && !oldBSTE.isEmpty() && oldBSTE.equalsIgnoreCase("Y")){
            return true;
        } else return false;
    }

    private Product isinExists(String isinCode) {
        try {
            return dsCon.getDefault().getRemoteProduct()
                    .getProductByCode(ISIN, isinCode);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve products from database", e);
        }

        return null;
    }

    private boolean checkIssuerName(String issuerName) {
        return issuerName.equalsIgnoreCase("Banco Santander, S.A.") || issuerName.equalsIgnoreCase("Banco Santander S.A.");
    }

    private boolean checkConsumerName(String issuerName) {
        return issuerName.equalsIgnoreCase("Santander Consumer Finance SA") || issuerName.equalsIgnoreCase("Santander Consumer Finance, S.A.");
    }

    private boolean checkOwnUsableCoveredOwn(String usable) {
        if (usable != null && !usable.isEmpty()) {
            return usable.equalsIgnoreCase("Y");
        } else return false;
    }

    private void setNonElegibility_DATE(Product product, String value) {
        product.setSecCode("ECMS_NonElegibilityDate", value);
    }

    private void unsetNonElegibility_DATE(Product product) {
        product.setSecCode("ECMS_NonElegibilityDate", null);
    }

    private boolean checkNonElegibility_DATE(Product product) {
        String fecha = product.getSecCode("ECMS_NonElegibilityDate");
        if (fecha == null || fecha.isEmpty()) {
            return false;
        } else return true;
    }

    private void setEligibility_BSTE(Product product, String value) {
        product.setSecCode("ECMS_Eligibility_BSTE", value);
    }

    private void setEligibility_BFOM(Product product, String value) {
        product.setSecCode("ECMS_Eligibility_BFOM", value);
    }

    private void setHaircutOwnUse(Product product, String ownUseHaircut) {
        product.setSecCode("ECMS_Haircut_Own_Use", ownUseHaircut);
    }

    private void setHaircut(Product product, String nonOwnUseHaircut) {
        product.setSecCode("ECMS_Haircut", nonOwnUseHaircut);
    }

    private String getEligibility_BSTE(Product product) {
        return product.getSecCode("ECMS_Eligibility_BSTE");
    }

    private String getEligibility_BFOM(Product product) {
        return product.getSecCode("ECMS_Eligibility_BFOM");
    }

    private String getHaircutOwnUse(Product product) {
        return product.getSecCode("ECMS_Haircut_Own_Use");
    }


    private String getHaircut(Product product) {
        return product.getSecCode("ECMS_Haircut");
    }

    private void saveProduct(Product product) {
        try {
            dsCon.getDefault().getRemoteProduct().saveProduct(product);
        } catch (CalypsoServiceException e) {
            e.printStackTrace();
        }
    }
}
