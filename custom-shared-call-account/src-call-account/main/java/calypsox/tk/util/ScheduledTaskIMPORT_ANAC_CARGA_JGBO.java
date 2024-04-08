package calypsox.tk.util;

import calypsox.tk.anacredit.util.AnacreditFileReader;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import calypsox.tk.anacredit.util.AnacreditBean;

import java.io.InputStream;
import java.util.*;


public class ScheduledTaskIMPORT_ANAC_CARGA_JGBO extends ScheduledTask {


    //ST Attributes Names
    private static final String ATT_FILE_PATH = "File Path";
    private static final String ATT_FILE_NAME = "File Name";
    private static final String ATT_MOVE_TO_COPY = "Move to Copy";


    private static final String ATT_J_MINORISTA = "J_MINORISTA";
    private static final String ATT_J_MIN_PADRE = "J_MIN_PADRE";
    private static final String ATT_LOAD_J_MINORISTA = "LOAD_J_MINORISTA";
    private static final String WHERE = "external_ref=";
    private static final String FULLPATHFILE = "/calypsox/templates/fichero_carga_pro_jmay_jmin_v1.txt";


    //Attributes
    private String attANACREDITKeywordValue;

    private InputStream attFullPath;

    private String attGBOFilePath;
    private String attGBOFileName;
    private Boolean moveToCopy;


    public ScheduledTaskIMPORT_ANAC_CARGA_JGBO() {
    }

    @Override
    public String getTaskInformation() {
        return "Import ANACREDIT files";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(ATT_FILE_PATH).description("File Path"));
        attributeList.add(attribute(ATT_FILE_NAME).description("File Name"));
        attributeList.add(attribute(ATT_LOAD_J_MINORISTA).description("LOAD_J_MINORISTA"));
        attributeList.add(attribute(ATT_MOVE_TO_COPY).description("Move file to copy ").booleanType());


        return attributeList;
    }

    public Vector getAttributeDomain(String attr, Hashtable currentAttr) {
        Vector v = new Vector();
        if (attr.equals(ATT_LOAD_J_MINORISTA)) {
            v.addElement("True");
            v.addElement("False");

        }
        if (attr.equals(ATT_MOVE_TO_COPY)) {
            v.addElement("True");
            v.addElement("False");
        }
        return v;
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {

        //Init attributes
        init();

        //Check ANACREDIT attribute to load JMinorista attribute or not.
        if (("True").equalsIgnoreCase(attANACREDITKeywordValue)) {
            if (loadJMinoristaFile(ds)) {
                Log.info(this, "fichero_carga_pro_jmay_jmin_v1 file loaded");
            } else {
                return false;
            }
        }

        if (loadGBOFile(ds)) {
            Log.info(this, "RELACIONJ_ANACREDIT_PRO_GBO file loaded");
        } else {
            return false;
        }

        return true;
    }


    public boolean loadJMinoristaFile(DSConnection ds) {

        AnacreditBean bean = new AnacreditBean();
        //Read lch file
        final List<AnacreditBean> jMinoristasFile = AnacreditFileReader.getInstance().readFile(attFullPath);

        if (Util.isEmpty(jMinoristasFile)) {
            Log.warn(this, "fichero_carga_pro_jmay_jmin_v1 file is empty.");
            return false;
        }

        for (int i = 0; i < jMinoristasFile.size(); i++) {
            try {
                Collection<Integer> ids = ds.getRemoteReferenceData().getLegalEntityIds(null, WHERE + "'" + jMinoristasFile.get(i).getColumnaA() + "'");

                if (ids.size() != 1) {
                    Log.warn("There are different legalEntities with the same external reference: ", ids.stream().toString());
                    continue;
                }
                if (null == ids) {
                    Log.warn("There isn't legalEntities with this external reference: ", jMinoristasFile.get(i).getColumnaA());
                    continue;
                }

                LegalEntity le = ds.getRemoteReferenceData().getLegalEntity(ids.stream().findFirst().get());
                if (le != null) {
                    LegalEntityAttribute attribute = ds.getRemoteReferenceData().getAttribute(le.getProcessingOrgBasedId(),
                            le.getLegalEntityId(), "ALL", ATT_J_MINORISTA);

                    if (attribute != null) {
                        attribute.setAttributeValue(jMinoristasFile.get(i).getcolumnaJMIN());
                        ds.getRemoteReferenceData().save(attribute);
                    } else {
                        LegalEntityAttribute attribute_JMinorista = new LegalEntityAttribute();
                        attribute_JMinorista.setAttributeValue(jMinoristasFile.get(i).getcolumnaJMIN());
                        attribute_JMinorista.setAttributeType(ATT_J_MINORISTA);
                        attribute_JMinorista.setLegalEntityId(le.getLegalEntityId());
                        attribute_JMinorista.setLegalEntityRole("ALL");
                        ds.getRemoteReferenceData().save(attribute_JMinorista);
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "JMINORISTAs file is empty" + e);
            }
        }
        return true;
    }

    public boolean loadGBOFile(DSConnection ds) {

        final List<AnacreditBean> gBOFile = AnacreditFileReader.getInstance().readFile(attGBOFilePath, attGBOFileName, moveToCopy);

        if (Util.isEmpty(gBOFile)) {
            Log.warn(this, "RELACIONJ_ANACREDIT_PRO_GBO file is empty.");
            return false;
        }

        for (int i = 0; i < gBOFile.size(); i++) {
            try {
                LegalEntity le = ds.getRemoteReferenceData().getLegalEntity(ATT_J_MINORISTA, gBOFile.get(i).getcolumnaJMIN());
                if (le != null) {
                    LegalEntityAttribute attribute = ds.getRemoteReferenceData().getAttribute(le.getProcessingOrgBasedId(),
                            le.getLegalEntityId(), "ALL", ATT_J_MIN_PADRE);

                    if (null != attribute && attribute.getAttributeValue().equalsIgnoreCase(gBOFile.get(i).getColumnaA())) {
                        continue;
                    }
                    if (null != attribute && !attribute.getAttributeValue().equalsIgnoreCase(gBOFile.get(i).getColumnaA())) {
                        attribute.setAttributeValue(gBOFile.get(i).getColumnaA());
                        ds.getRemoteReferenceData().save(attribute);
                    }
                    if (null == attribute) {
                        LegalEntityAttribute JMinPadre = new LegalEntityAttribute();
                        JMinPadre.setAttributeValue(gBOFile.get(i).getColumnaA());
                        JMinPadre.setAttributeType(ATT_J_MIN_PADRE);
                        JMinPadre.setLegalEntityId(le.getLegalEntityId());
                        JMinPadre.setLegalEntityRole("ALL");
                        ds.getRemoteReferenceData().save(JMinPadre);
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "GBO file is empty" + e);
            }
        }
        return true;
    }

    /**
     * Init attributes
     */
    public void init() {

        attFullPath = this.getClass().getResourceAsStream(FULLPATHFILE);

        attGBOFilePath = !Util.isEmpty(getAttribute(ATT_FILE_PATH)) ? getAttribute(ATT_FILE_PATH) : "";
        attGBOFileName = !Util.isEmpty(getAttribute(ATT_FILE_NAME)) ? getAttribute(ATT_FILE_NAME) : "";

        attANACREDITKeywordValue = !Util.isEmpty(getAttribute(ATT_LOAD_J_MINORISTA)) ? getAttribute(ATT_LOAD_J_MINORISTA) : "";
        moveToCopy = getBooleanAttribute(ATT_MOVE_TO_COPY, true);
    }


}
