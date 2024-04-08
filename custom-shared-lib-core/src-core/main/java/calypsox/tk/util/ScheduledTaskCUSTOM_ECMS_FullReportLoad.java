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
import java.util.List;

public class ScheduledTaskCUSTOM_ECMS_FullReportLoad extends ScheduledTask {

    private static final String FULL_FILE_PATH = "/calypso_interfaces/ecms/import/";
    private static final String FULL_FILE_NAME = "Eligible_Assets.xml";
    public static final String ISIN = "ISIN";

    private DSConnection dsCon;

    protected List<AttributeDefinition> buildAttributeDefinition() {

        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute("Full File Path:"));

        return attributeList;
    }


    @Override
    public String getTaskInformation() {
        return "This ScheduledTask loads the full ECMS File and updates the Calypso information to get the eligibility of the ISINs.";
    }


    public boolean process(DSConnection ds, PSConnection ps) {
        this.dsCon = ds;

        JDatetime date = dsCon.getServerCurrentDatetime();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String formattedDate = formatter.format(date);

        String fileName = FULL_FILE_PATH + FULL_FILE_NAME.replace("DDMMYYYY", formattedDate);
        //we read the file line by line and analyze every block
        readFile(fileName);

        //we return true if everything was ok.
        return true;
    }

    private void readFile(String fileName) {

        try {
            if(this.getAttribute("Full File Path:") != null && !this.getAttribute("Full File Path:").isEmpty()){
                fileName = this.getAttribute("Full File Path:") + FULL_FILE_NAME;
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

            for (int position = 0; position <= isins.getLength() - 1; position++) {
                String isin = isins.item(position).getTextContent();
                String issuerName = issuerNames.item(position).getTextContent();
                String nonOwnUseHaircut = nonOwnUseHaircuts.item(position).getTextContent();
                String ownUseHaircut = ownUseHaircuts.item(position).getTextContent();
                String pOwnUsableCoveredBond = pOwnUsableCoveredBonds.item(position).getTextContent();
                checkIsinEligibility(isin, issuerName, nonOwnUseHaircut, ownUseHaircut, pOwnUsableCoveredBond);
            }


        } catch (IOException | ParserConfigurationException | SAXException e) {
            Log.error(this, "Error reading file: " + fileName + "\n" + e);
        }

    }


    private void checkIsinEligibility(String isin, String issuerName, String nonOwnUseHaircut, String ownUseHaircut, String pOnwUseCoveredBond) {
        //we check the conditions for this isin
        //Revisamos si el ISIN existe en calypso
        Product product = isinExists(isin);
        boolean modified = false;

        if (product != null) {
            //si existe el isin, comprobamos el issuerName
            if (checkIssuerName(issuerName)) {
                //si el issuer es Santander SA
                if (checkOwnUsableCoveredOwn(pOnwUseCoveredBond)) {
                    //Fijar el ECMS_Eligibility_BSTE a "Y"
                    setEligibility_BSTE(product, "Y");
                    //Fijar el "ECMS_Haircut_Own_Use" se informa con el HAIRCUT_OWN_USE
                    setHaircutOwnUse(product, ownUseHaircut);
                    //ECMS_Haircut se informa con HAIRCUT
                    setHaircut(product, nonOwnUseHaircut);
                    //El SecCode “ECMS_Eligibility_BFOM” se informara a vacio.
                    setEligibility_BFOM(product, "");
                    modified = true;
                } else {
                    //si tiene valor N, entonces
                    // se fijan a vacio los SecCode “ECMS_Eligibility” , “ECMS_Haircut” y “ECMS_Haircut_Own_Use”.
                    setEligibility_BSTE(product, "");
                    setEligibility_BFOM(product, "");
                    setHaircut(product, "");
                    setHaircutOwnUse(product, "");
                    modified = true;
                }
            } else if (checkConsumerName(issuerName)) {
                //si el issuer es Santander Consumer
                if (checkOwnUsableCoveredOwn(pOnwUseCoveredBond)) {
                    //si tiene valor Y, entonces
                    //Fijar el ECMS_Eligibility_BSTE a vacio.
                    setEligibility_BSTE(product, "");
                    //Fijar el "ECMS_Haircut_Own_Use" se informa con el HAIRCUT_OWN_USE
                    setHaircutOwnUse(product, ownUseHaircut);
                    //ECMS_Haircut se informa con HAIRCUT
                    setHaircut(product, nonOwnUseHaircut);
                    //El SecCode “ECMS_Eligibility_BFOM” se informara a "Y".
                    setEligibility_BFOM(product, "Y");
                    modified = true;
                } else {
                    //si tiene valor N, entonces se fijan a vacio los SecCode “ECMS_Eligibility_BSTE” , “ECMS_Eligibility_BFOM”, “ECMS_Haircut” y “ECMS_Haircut_Own_Use”.
                    setEligibility_BSTE(product, "");
                    setEligibility_BFOM(product, "");
                    setHaircut(product, "");
                    setHaircutOwnUse(product, "");
                    modified = true;
                }
            } else {
                //si el isser no es ninguno de los anteriores,
                // “ECMS_Eligibility_BSTE” y  “ECMS_Eligibility_BFOM”  con valor “Y”
                // “ECMS_Haircut” se informara con el valor del campo HAIRCUT. El SecCode “ECMS_Haircut_Own_Use” se fijara a vacío.
                setEligibility_BSTE(product, "Y");
                setEligibility_BFOM(product, "Y");
                setHaircut(product, nonOwnUseHaircut);
                setHaircutOwnUse(product, "");
                modified = true;

            }


            if (modified) {
                saveProduct(product);
            }
        }
        //no se hace nada si no existe este isin
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

    private void saveProduct(Product product) {
        try {
            dsCon.getDefault().getRemoteProduct().saveProduct(product);
        } catch (CalypsoServiceException e) {
            e.printStackTrace();
        }
    }
}
