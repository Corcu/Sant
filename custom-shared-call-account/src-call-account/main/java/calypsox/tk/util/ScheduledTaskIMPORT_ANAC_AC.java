package calypsox.tk.util;

import calypsox.tk.anacredit.util.AnacreditBean;
import calypsox.tk.anacredit.util.AnacreditFileReader;
import calypsox.tk.anacredit.util.acesscontrol.AnacreditACBean;
import calypsox.tk.anacredit.util.acesscontrol.AnacreditFileACReader;
import com.calypso.etrading.base.enums.EProductIdType;
import com.calypso.infra.authentication.userdetails.CalypsoUserDetails;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.ScheduledTask;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;


public class ScheduledTaskIMPORT_ANAC_AC extends ScheduledTask {


    //ST Attributes Names
    private static final String ATT_FILE_PATH = "File Path";
    private static final String ATT_FILE_NAME = "File Name";
    private static final String ATT_MOVE_TO_COPY = "Move to Copy";

    private static final String ATT_ANACREDIT_COTIZA = "ANACREDIT_COTIZA";
    private static final String ATT_ANACREDIT_JERARQUIA = "ANACREDIT_JERARQUIA";
    private static final String ATT_RF_RV  = "RF_RV";

    private static final String FULLPATHFILE = "/main/interfaces_pro/asset_control/import/anacredit/";
    private static final String ANACREDIT_COTIZA = "ANACREDIT_COTIZA";
    private static final String ANACREDIT_JERARQUIA = "ANACREDIT_JERARQUIA";
    private static final String ISIN = "ISIN";

    //Attributes
    private String attANACREDITKeywordValue;

    private boolean isImportCotiza  = false;
    private boolean isImportJerarquia  = false;
    private String  importType = null;

    private String attACFilePath;
    private String attACFileName;

    private Boolean moveToCopy;

    public ScheduledTaskIMPORT_ANAC_AC() {
    }

    @Override
    public String getTaskInformation() {
        return "Import ANACREDIT instruments attributes COTIZA e JERARQUIA for RF and RV";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(ATT_FILE_PATH).description("File Path"));
        attributeList.add(attribute(ATT_FILE_NAME).description("File Name"));
        attributeList.add(attribute(ATT_ANACREDIT_COTIZA).description("LOAD_COTIZA"));
        attributeList.add(attribute(ATT_ANACREDIT_JERARQUIA).description("LOAD_JERARQUIA"));
        attributeList.add(attribute(ATT_RF_RV).description("RF or RV"));

        attributeList.add(attribute(ATT_MOVE_TO_COPY).description("Move file to copy ").booleanType());
        return attributeList;
    }

    public Vector getAttributeDomain(String attr, Hashtable currentAttr) {
        Vector v = new Vector();
        if (attr.equals(ATT_ANACREDIT_COTIZA)) {
            v.addElement("True");
            v.addElement("False");

        }
        if (attr.equals(ATT_ANACREDIT_JERARQUIA)) {
            v.addElement("True");
            v.addElement("False");
        }

        if (attr.equals(ATT_RF_RV)) {
            v.addElement("RF");
            v.addElement("RV");
        }

        if (attr.equals(ATT_MOVE_TO_COPY)) {
            v.addElement(true);
            v.addElement(false);
        }
        return v;
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {

        //Init attributes
        init();

        if ("RV".equals(importType)) {
            return processRentaVariableFile(ds);
        }

        if ("RF".equals(importType)) {
            return processRentaFijaFile(ds);
        }

        return true;
    }

    private boolean processRentaFijaFile(DSConnection ds) {
        //Read RV file
        final List<AnacreditACBean> rfFile = AnacreditFileACReader.getInstance().readFileRF(attACFilePath, attACFileName, moveToCopy );
        if (Util.isEmpty(rfFile)) {
            Log.warn(this, "input file is empty or not present :" + attACFilePath + " " + attACFileName);
            return false;
        }
        rfFile.stream().forEach(n -> {updateInstrument(n);});
        Log.system(LOG_CATEGORY, "### File RF processed successfully.");

        return true;
    }

    public boolean processRentaVariableFile(DSConnection ds)  {
        //Read RV file
        final List<AnacreditACBean> rvFile = AnacreditFileACReader.getInstance().readFileRV(attACFilePath, attACFileName, moveToCopy );
        if (Util.isEmpty(rvFile)) {
            Log.warn(this, "input file is empty :" + attACFilePath + " " + attACFileName);
            return false;
        }
        rvFile.stream().forEach(n -> {updateInstrument(n);});
        Log.system(LOG_CATEGORY, "### File RV processed successfully.");
        return true;
    }

    private synchronized boolean updateInstrument(AnacreditACBean n) {
        Product p = null;
        try {
            p = getDSConnection().getRemoteProduct().getProductByCode(ISIN, n.getIsin());
            if (p==null) {
                return true;
            }
            Object obj = p.clone();
            boolean needUpdate = false;
            final Product clone = (Product) obj;
            if (isImportCotiza) {
                if (!Util.isEmpty(n.getCotiza())) {
                    if (null == clone.getSecCode(ANACREDIT_COTIZA)
                        || (!n.getCotiza().equals(clone.getSecCode(ANACREDIT_COTIZA)))) {
                        clone.setSecCode(ANACREDIT_COTIZA, n.getCotiza());
                        needUpdate = true;
                    }
                }
            }
            if (isImportJerarquia) {
                if (!Util.isEmpty(n.getJerarquia())) {
                    if (null == clone.getSecCode(ANACREDIT_JERARQUIA)
                            || (!n.getJerarquia().equals(clone.getSecCode(ANACREDIT_JERARQUIA)))) {
                        clone.setSecCode(ANACREDIT_JERARQUIA, n.getJerarquia());
                        needUpdate = true;
                    }
                }

            }
            if (needUpdate) {
                getDSConnection().getRemoteProduct().saveProduct(clone);
                Log.system(LOG_CATEGORY, "### PRODUCT Updated successfully :" + n.toString());
            } else {
                // supress System.out.println("skipped:" + n.toString());
            }
    } catch (CalypsoServiceException | CloneNotSupportedException e) {
        Log.error(LOG_CATEGORY, e);
        return false;
    }
        return true;
    }

    /**
     * Init attributes
     */
    public void init() {
        attACFilePath = !Util.isEmpty(getAttribute(ATT_FILE_PATH)) ? getAttribute(ATT_FILE_PATH) : "";
         attACFileName = !Util.isEmpty(getAttribute(ATT_FILE_NAME)) ? getAttribute(ATT_FILE_NAME) : "";
        isImportCotiza = !Util.isEmpty(getAttribute(ATT_ANACREDIT_COTIZA)) &&  getAttribute(ATT_ANACREDIT_COTIZA).equalsIgnoreCase("True");
        isImportJerarquia = !Util.isEmpty(getAttribute(ATT_ANACREDIT_JERARQUIA)) &&  getAttribute(ATT_ANACREDIT_JERARQUIA).equalsIgnoreCase("True");
        importType =  !Util.isEmpty(getAttribute(ATT_RF_RV)) ? getAttribute(ATT_RF_RV) : "";
        moveToCopy = getBooleanAttribute(ATT_MOVE_TO_COPY, true);
    }

}
